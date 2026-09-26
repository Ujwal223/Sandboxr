#include "bytehook_fs.h"
#include <bytehook.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <cstring>
#include <android/log.h>

#define TAG "SandboxrByteHook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace sandboxr {
namespace fs {

static std::mutex g_fs_mutex;
static std::string g_spoofed_mac = "02:00:00:00:00:01";
static std::string g_virtual_sysfs_dir = "";
static bool g_hook_active = false;

static bytehook_stub_t g_stub_open = nullptr;
static bytehook_stub_t g_stub_openat = nullptr;
static bytehook_stub_t g_stub_readlink = nullptr;
static bytehook_stub_t g_stub_readlinkat = nullptr;

static inline bool is_sensitive_dev_socket(const char* path) {
    if (!path) return false;
    return (std::strstr(path, "/dev/socket") != nullptr || std::strstr(path, "dev/socket") != nullptr);
}

static inline bool is_sysfs_net_address_path(const char* path) {
    if (!path) return false;
    return ((std::strncmp(path, "/sys/", 5) == 0 ||
             std::strncmp(path, "/devices/", 9) == 0 ||
             std::strstr(path, "sys/class/net") != nullptr) &&
            std::strstr(path, "/address") != nullptr);
}

static inline bool is_sysfs_net_path(const char* path) {
    if (!path) return false;
    return (std::strncmp(path, "/sys/class/net/", 15) == 0 ||
            std::strstr(path, "sys/class/net/") != nullptr);
}

static bool resolve_at_path(int dirfd, const char* pathname, char* out_buf, size_t out_size) {
    if (!pathname || out_size == 0) return false;
    if (pathname[0] == '/') {
        size_t len = std::strlen(pathname);
        if (len >= out_size) len = out_size - 1;
        std::memcpy(out_buf, pathname, len);
        out_buf[len] = '\0';
        return true;
    }

    char base_dir[1024] = {0};
    ssize_t base_len = -1;
    if (dirfd == AT_FDCWD) {
        base_len = readlink("/proc/self/cwd", base_dir, sizeof(base_dir) - 1);
    } else if (dirfd >= 0) {
        char fd_proc[64];
        snprintf(fd_proc, sizeof(fd_proc), "/proc/self/fd/%d", dirfd);
        base_len = readlink(fd_proc, base_dir, sizeof(base_dir) - 1);
    }

    if (base_len <= 0) {
        size_t len = std::strlen(pathname);
        if (len >= out_size) len = out_size - 1;
        std::memcpy(out_buf, pathname, len);
        out_buf[len] = '\0';
        return true;
    }
    base_dir[base_len] = '\0';

    int written = snprintf(out_buf, out_size, "%s/%s", base_dir, pathname);
    return (written > 0 && static_cast<size_t>(written) < out_size);
}

static int proxy_open(const char* pathname, int flags, mode_t mode) {
    BYTEHOOK_STACK_SCOPE();

    if (__builtin_expect(!pathname, 0)) {
        return BYTEHOOK_CALL_PREV(proxy_open, pathname, flags, mode);
    }

    if (is_sensitive_dev_socket(pathname)) {
        errno = ENOENT;
        return -1;
    }

    if (is_sysfs_net_address_path(pathname)) {
        std::lock_guard<std::mutex> lock(g_fs_mutex);
        if (!g_virtual_sysfs_dir.empty()) {
            std::string redirect_file = g_virtual_sysfs_dir + "/wlan0_address";
            if (access(redirect_file.c_str(), R_OK) == 0) {
                return BYTEHOOK_CALL_PREV(proxy_open, redirect_file.c_str(), flags, mode);
            }
        }

        // Pipe-based synthetic sysfs address injection:
        int pipefd[2];
        if (pipe(pipefd) == 0) {
            std::string mac_line = g_spoofed_mac + "\n";
            write(pipefd[1], mac_line.c_str(), mac_line.length());
            close(pipefd[1]);
            return pipefd[0];
        }
    }

    return BYTEHOOK_CALL_PREV(proxy_open, pathname, flags, mode);
}

static int proxy_openat(int dirfd, const char* pathname, int flags, mode_t mode) {
    BYTEHOOK_STACK_SCOPE();

    if (__builtin_expect(!pathname, 0)) {
        return BYTEHOOK_CALL_PREV(proxy_openat, dirfd, pathname, flags, mode);
    }

    char resolved[1024] = {0};
    const char* target_path = pathname;
    if (resolve_at_path(dirfd, pathname, resolved, sizeof(resolved))) {
        target_path = resolved;
    }

    if (is_sensitive_dev_socket(target_path)) {
        errno = ENOENT;
        return -1;
    }

    if (is_sysfs_net_address_path(target_path)) {
        std::lock_guard<std::mutex> lock(g_fs_mutex);
        if (!g_virtual_sysfs_dir.empty()) {
            std::string redirect_file = g_virtual_sysfs_dir + "/wlan0_address";
            if (access(redirect_file.c_str(), R_OK) == 0) {
                return BYTEHOOK_CALL_PREV(proxy_openat, AT_FDCWD, redirect_file.c_str(), flags, mode);
            }
        }

        int pipefd[2];
        if (pipe(pipefd) == 0) {
            std::string mac_line = g_spoofed_mac + "\n";
            write(pipefd[1], mac_line.c_str(), mac_line.length());
            close(pipefd[1]);
            return pipefd[0];
        }
    }

    return BYTEHOOK_CALL_PREV(proxy_openat, dirfd, pathname, flags, mode);
}

static ssize_t proxy_readlink(const char* pathname, char* buf, size_t bufsiz) {
    BYTEHOOK_STACK_SCOPE();

    if (__builtin_expect(pathname != nullptr && is_sysfs_net_path(pathname), 0)) {
        const char* virtual_target = "/devices/virtual/net/wlan0";
        size_t len = std::strlen(virtual_target);
        if (len > bufsiz) len = bufsiz;
        std::memcpy(buf, virtual_target, len);
        return static_cast<ssize_t>(len);
    }

    return BYTEHOOK_CALL_PREV(proxy_readlink, pathname, buf, bufsiz);
}

static ssize_t proxy_readlinkat(int dirfd, const char* pathname, char* buf, size_t bufsiz) {
    BYTEHOOK_STACK_SCOPE();

    if (__builtin_expect(pathname != nullptr, 1)) {
        char resolved[1024] = {0};
        const char* target_path = pathname;
        if (resolve_at_path(dirfd, pathname, resolved, sizeof(resolved))) {
            target_path = resolved;
        }

        if (is_sysfs_net_path(target_path)) {
            const char* virtual_target = "/devices/virtual/net/wlan0";
            size_t len = std::strlen(virtual_target);
            if (len > bufsiz) len = bufsiz;
            std::memcpy(buf, virtual_target, len);
            return static_cast<ssize_t>(len);
        }
    }

    return BYTEHOOK_CALL_PREV(proxy_readlinkat, dirfd, pathname, buf, bufsiz);
}

bool init_hooks() {
    if (g_hook_active) {
        return true;
    }

    int init_res = bytehook_init(BYTEHOOK_MODE_AUTOMATIC, false);
    if (init_res != 0) {
        LOGW("bytehook_init returned %d", init_res);
    }

    g_stub_open = bytehook_hook_all(
        nullptr,
        "open",
        reinterpret_cast<void*>(proxy_open),
        nullptr,
        nullptr
    );

    g_stub_openat = bytehook_hook_all(
        nullptr,
        "openat",
        reinterpret_cast<void*>(proxy_openat),
        nullptr,
        nullptr
    );

    g_stub_readlink = bytehook_hook_all(
        nullptr,
        "readlink",
        reinterpret_cast<void*>(proxy_readlink),
        nullptr,
        nullptr
    );

    g_stub_readlinkat = bytehook_hook_all(
        nullptr,
        "readlinkat",
        reinterpret_cast<void*>(proxy_readlinkat),
        nullptr,
        nullptr
    );

    g_hook_active = (g_stub_open != nullptr || g_stub_openat != nullptr);
    LOGI("ByteHook filesystem hooks initialized: open=%p, openat=%p, readlink=%p",
         g_stub_open, g_stub_openat, g_stub_readlink);
    return g_hook_active;
}

void cleanup_hooks() {
    if (g_stub_open != nullptr) {
        bytehook_unhook(g_stub_open);
        g_stub_open = nullptr;
    }
    if (g_stub_openat != nullptr) {
        bytehook_unhook(g_stub_openat);
        g_stub_openat = nullptr;
    }
    if (g_stub_readlink != nullptr) {
        bytehook_unhook(g_stub_readlink);
        g_stub_readlink = nullptr;
    }
    if (g_stub_readlinkat != nullptr) {
        bytehook_unhook(g_stub_readlinkat);
        g_stub_readlinkat = nullptr;
    }
    g_hook_active = false;
}

void set_spoofed_mac_address(const std::string& mac_address) {
    std::lock_guard<std::mutex> lock(g_fs_mutex);
    g_spoofed_mac = mac_address;
}

std::string get_spoofed_mac_address() {
    std::lock_guard<std::mutex> lock(g_fs_mutex);
    return g_spoofed_mac;
}

void set_virtual_sysfs_dir(const std::string& path) {
    std::lock_guard<std::mutex> lock(g_fs_mutex);
    g_virtual_sysfs_dir = path;
}

bool is_hook_active() {
    return g_hook_active;
}

std::string test_read_file(const std::string& path) {
    int fd = open(path.c_str(), O_RDONLY);
    if (fd < 0) {
        return "";
    }
    char buf[128] = {0};
    ssize_t bytes = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (bytes <= 0) {
        return "";
    }
    buf[bytes] = '\0';
    // Trim newline
    if (bytes > 0 && (buf[bytes - 1] == '\n' || buf[bytes - 1] == '\r')) {
        buf[bytes - 1] = '\0';
    }
    return std::string(buf);
}

} // namespace fs
} // namespace sandboxr
