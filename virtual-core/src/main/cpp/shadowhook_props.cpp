#include "shadowhook_props.h"
#include <shadowhook.h>
#include <android/log.h>
#include <sys/system_properties.h>
#include <cstring>
#include <shared_mutex>

#define TAG "SandboxrShadowHook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace sandboxr {
namespace props {

static std::shared_mutex g_prop_mutex;
static std::unordered_map<std::string, std::string> g_spoofed_props;
static void* g_stub_system_property_get = nullptr;
static void* g_stub_system_property_read = nullptr;
static void* g_stub_system_property_read_callback = nullptr;
static bool g_hook_active = false;

typedef int (*orig_system_property_get_fn)(const char* name, char* value);
static orig_system_property_get_fn g_orig_system_property_get = nullptr;

typedef int (*orig_system_property_read_fn)(const prop_info* pi, char* name, char* value);
static orig_system_property_read_fn g_orig_system_property_read = nullptr;

typedef void (*orig_system_property_read_callback_fn)(
    const prop_info* pi,
    void (*callback)(void* cookie, const char* name, const char* value, uint32_t serial),
    void* cookie
);
static orig_system_property_read_callback_fn g_orig_system_property_read_callback = nullptr;

struct CallbackCookieWrapper {
    void (*orig_callback)(void* cookie, const char* name, const char* value, uint32_t serial);
    void* orig_cookie;
};

struct PropNameExtractor {
    char name_buf[PROP_NAME_MAX];
    bool found;
};

static void name_extractor_callback(void* cookie, const char* name, const char* /*value*/, uint32_t /*serial*/) {
    auto* ext = reinterpret_cast<PropNameExtractor*>(cookie);
    if (name != nullptr) {
        std::strncpy(ext->name_buf, name, PROP_NAME_MAX - 1);
        ext->name_buf[PROP_NAME_MAX - 1] = '\0';
        ext->found = true;
    }
}

static void proxy_callback_wrapper(void* cookie, const char* name, const char* value, uint32_t serial) {
    auto* wrapper = reinterpret_cast<CallbackCookieWrapper*>(cookie);
    if (name != nullptr) {
        std::shared_lock<std::shared_mutex> lock(g_prop_mutex);
        auto it = g_spoofed_props.find(name);
        if (it != g_spoofed_props.end()) {
            wrapper->orig_callback(wrapper->orig_cookie, name, it->second.c_str(), serial);
            return;
        }
    }
    wrapper->orig_callback(wrapper->orig_cookie, name, value, serial);
}

static void proxy_system_property_read_callback(
    const prop_info* pi,
    void (*callback)(void* cookie, const char* name, const char* value, uint32_t serial),
    void* cookie
) {
    if (callback != nullptr) {
        CallbackCookieWrapper wrapper{callback, cookie};
        if (g_orig_system_property_read_callback != nullptr) {
            g_orig_system_property_read_callback(pi, proxy_callback_wrapper, &wrapper);
            return;
        }
    } else if (g_orig_system_property_read_callback != nullptr) {
        g_orig_system_property_read_callback(pi, callback, cookie);
    }
}

static int proxy_system_property_get(const char* name, char* value) {
    if (name != nullptr && value != nullptr) {
        std::shared_lock<std::shared_mutex> lock(g_prop_mutex);
        auto it = g_spoofed_props.find(name);
        if (it != g_spoofed_props.end()) {
            const std::string& spoofed_val = it->second;
            size_t len = spoofed_val.length();
            if (len >= PROP_VALUE_MAX) {
                len = PROP_VALUE_MAX - 1;
            }
            std::memcpy(value, spoofed_val.c_str(), len);
            value[len] = '\0';
            return static_cast<int>(len);
        }
    }

    if (g_orig_system_property_get != nullptr) {
        return g_orig_system_property_get(name, value);
    }
    return 0;
}

static int proxy_system_property_read(const prop_info* pi, char* name, char* value) {
    int res = 0;
    if (g_orig_system_property_read != nullptr) {
        res = g_orig_system_property_read(pi, name, value);
    }

    const char* prop_name = name;
    char extracted_name[PROP_NAME_MAX] = {0};

    // If caller passed NULL name to bypass name-based hooks, extract the name via callback
    if (prop_name == nullptr && value != nullptr && pi != nullptr && g_orig_system_property_read_callback != nullptr) {
        PropNameExtractor ext;
        ext.found = false;
        ext.name_buf[0] = '\0';
        g_orig_system_property_read_callback(pi, name_extractor_callback, &ext);
        if (ext.found) {
            std::strncpy(extracted_name, ext.name_buf, sizeof(extracted_name) - 1);
            extracted_name[sizeof(extracted_name) - 1] = '\0';
            prop_name = extracted_name;
        }
    }

    if (prop_name != nullptr && value != nullptr) {
        std::shared_lock<std::shared_mutex> lock(g_prop_mutex);
        auto it = g_spoofed_props.find(prop_name);
        if (it != g_spoofed_props.end()) {
            const std::string& spoofed_val = it->second;
            size_t len = spoofed_val.length();
            if (len >= PROP_VALUE_MAX) {
                len = PROP_VALUE_MAX - 1;
            }
            std::memcpy(value, spoofed_val.c_str(), len);
            value[len] = '\0';
            return static_cast<int>(len);
        }
    }
    return res;
}

bool init_hooks() {
    if (g_hook_active) {
        return true;
    }

    int init_res = shadowhook_init(SHADOWHOOK_MODE_SHARED, false);
    if (init_res != 0) {
        LOGW("shadowhook_init returned %d", init_res);
    }

    g_stub_system_property_get = shadowhook_hook_sym_name(
        "libc.so",
        "__system_property_get",
        reinterpret_cast<void*>(proxy_system_property_get),
        reinterpret_cast<void**>(&g_orig_system_property_get)
    );

    if (g_stub_system_property_get == nullptr) {
        LOGE("Failed to hook __system_property_get, errno: %d", shadowhook_get_errno());
    } else {
        LOGI("Successfully hooked __system_property_get in libc.so");
    }

    g_stub_system_property_read = shadowhook_hook_sym_name(
        "libc.so",
        "__system_property_read",
        reinterpret_cast<void*>(proxy_system_property_read),
        reinterpret_cast<void**>(&g_orig_system_property_read)
    );

    if (g_stub_system_property_read != nullptr) {
        LOGI("Successfully hooked __system_property_read in libc.so");
    }

    g_stub_system_property_read_callback = shadowhook_hook_sym_name(
        "libc.so",
        "__system_property_read_callback",
        reinterpret_cast<void*>(proxy_system_property_read_callback),
        reinterpret_cast<void**>(&g_orig_system_property_read_callback)
    );

    if (g_stub_system_property_read_callback != nullptr) {
        LOGI("Successfully hooked __system_property_read_callback in libc.so");
    }

    g_hook_active = (g_stub_system_property_get != nullptr);
    return g_hook_active;
}

void cleanup_hooks() {
    if (g_stub_system_property_get != nullptr) {
        shadowhook_unhook(g_stub_system_property_get);
        g_stub_system_property_get = nullptr;
    }
    if (g_stub_system_property_read != nullptr) {
        shadowhook_unhook(g_stub_system_property_read);
        g_stub_system_property_read = nullptr;
    }
    if (g_stub_system_property_read_callback != nullptr) {
        shadowhook_unhook(g_stub_system_property_read_callback);
        g_stub_system_property_read_callback = nullptr;
    }
    g_hook_active = false;
}

void set_spoofed_property(const std::string& key, const std::string& value) {
    std::unique_lock<std::shared_mutex> lock(g_prop_mutex);
    g_spoofed_props[key] = value;
}

std::string get_spoofed_property(const std::string& key) {
    std::shared_lock<std::shared_mutex> lock(g_prop_mutex);
    auto it = g_spoofed_props.find(key);
    if (it != g_spoofed_props.end()) {
        return it->second;
    }
    return "";
}

void clear_spoofed_properties() {
    std::unique_lock<std::shared_mutex> lock(g_prop_mutex);
    g_spoofed_props.clear();
}

bool is_hook_active() {
    return g_hook_active;
}

int test_system_property_get(const char* name, char* value) {
    return __system_property_get(name, value);
}

} // namespace props
} // namespace sandboxr
