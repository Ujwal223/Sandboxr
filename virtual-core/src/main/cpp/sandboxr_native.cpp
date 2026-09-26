#include "sandboxr_native.h"
#include "shadowhook_props.h"
#include "bytehook_fs.h"
#include <android/log.h>
#include <sys/system_properties.h>
#include <string>

#define TAG "SandboxrNativeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::string jstring2string(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_initHooks(JNIEnv* env, jclass clazz) {
    LOGI("Initializing NativeHookBridge: ShadowHook and ByteHook...");
    bool shadow_ok = sandboxr::props::init_hooks();
    bool byte_ok = sandboxr::fs::init_hooks();
    LOGI("NativeHookBridge init result: ShadowHook=%d, ByteHook=%d", shadow_ok, byte_ok);
    return static_cast<jboolean>(shadow_ok && byte_ok);
}

JNIEXPORT void JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_setSpoofedProperty(
    JNIEnv* env, jclass clazz, jstring key, jstring value) {
    if (!key || !value) return;
    std::string k = jstring2string(env, key);
    std::string v = jstring2string(env, value);
    sandboxr::props::set_spoofed_property(k, v);
}

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_getSpoofedProperty(
    JNIEnv* env, jclass clazz, jstring key) {
    if (!key) return nullptr;
    std::string k = jstring2string(env, key);
    std::string v = sandboxr::props::get_spoofed_property(k);
    if (v.empty()) return nullptr;
    return env->NewStringUTF(v.c_str());
}

JNIEXPORT void JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_clearSpoofedProperties(
    JNIEnv* env, jclass clazz) {
    sandboxr::props::clear_spoofed_properties();
}

JNIEXPORT void JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_setSpoofedMacAddress(
    JNIEnv* env, jclass clazz, jstring mac) {
    if (!mac) return;
    std::string m = jstring2string(env, mac);
    sandboxr::fs::set_spoofed_mac_address(m);
}

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_getSpoofedMacAddress(
    JNIEnv* env, jclass clazz) {
    std::string m = sandboxr::fs::get_spoofed_mac_address();
    if (m.empty()) return nullptr;
    return env->NewStringUTF(m.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_isHooked(JNIEnv* env, jclass clazz) {
    return static_cast<jboolean>(sandboxr::props::is_hook_active() && sandboxr::fs::is_hook_active());
}

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_nativeGetProp(
    JNIEnv* env, jclass clazz, jstring key) {
    if (!key) return nullptr;
    std::string k = jstring2string(env, key);
    char buf[PROP_VALUE_MAX] = {0};
    int len = sandboxr::props::test_system_property_get(k.c_str(), buf);
    if (len <= 0) {
        std::string fallback = sandboxr::props::get_spoofed_property(k);
        if (!fallback.empty()) {
            return env->NewStringUTF(fallback.c_str());
        }
        return nullptr;
    }
    return env->NewStringUTF(buf);
}

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_nativeReadSysfs(
    JNIEnv* env, jclass clazz, jstring path) {
    if (!path) return nullptr;
    std::string p = jstring2string(env, path);
    std::string content = sandboxr::fs::test_read_file(p);
    if (content.empty()) {
        std::string fallback = sandboxr::fs::get_spoofed_mac_address();
        return env->NewStringUTF(fallback.c_str());
    }
    return env->NewStringUTF(content.c_str());
}

} // extern "C"
