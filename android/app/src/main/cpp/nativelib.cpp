#include <jni.h>
#include <string>
#include <android/fdsan.h>
#include <unistd.h>
#include <utility>
#include <android/log.h>

extern "C"
JNIEXPORT void JNICALL
Java_ir_filternet_cfscanner_CFScannerApplication_DisableFDSAN(JNIEnv *env, jobject thiz) {
    if (__builtin_available(android 29, *)) {
        android_fdsan_set_error_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED);
        __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "fdsan JNI Disabled.");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", "fdsan JNI Not Implemented.");
    }
}