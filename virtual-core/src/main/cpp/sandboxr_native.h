#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_initHooks(JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_setSpoofedProperty(
    JNIEnv* env, jclass clazz, jstring key, jstring value);

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_getSpoofedProperty(
    JNIEnv* env, jclass clazz, jstring key);

JNIEXPORT void JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_clearSpoofedProperties(
    JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_setSpoofedMacAddress(
    JNIEnv* env, jclass clazz, jstring mac);

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_getSpoofedMacAddress(
    JNIEnv* env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_isHooked(JNIEnv* env, jclass clazz);

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_nativeGetProp(
    JNIEnv* env, jclass clazz, jstring key);

JNIEXPORT jstring JNICALL
Java_com_sandboxr_virtual_hardware_NativeHookBridge_nativeReadSysfs(
    JNIEnv* env, jclass clazz, jstring path);

#ifdef __cplusplus
}
#endif
