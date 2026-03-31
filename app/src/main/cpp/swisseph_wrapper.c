#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "swisseph/swephexp.h"

#define LOG_TAG "SwissEphemeris"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeInit(
    JNIEnv *env, jobject thiz, jstring ephe_path) {
    const char *path = (*env)->GetStringUTFChars(env, ephe_path, NULL);
    if (path == NULL) {
        LOGE("Failed to get ephe_path string");
        return;
    }
    LOGI("Setting ephemeris path to: %s", path);
    swe_set_ephe_path((char *)path);
    (*env)->ReleaseStringUTFChars(env, ephe_path, path);
    LOGI("Ephemeris path set successfully");
}

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeClose(
    JNIEnv *env, jobject thiz) {
    swe_close();
}

JNIEXPORT jdouble JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeJulianDay(
    JNIEnv *env, jobject thiz,
    jint year, jint month, jint day,
    jdouble hour) {
    return swe_julday(year, month, day, hour, SE_GREG_CAL);
}

JNIEXPORT jdoubleArray JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeCalculateBody(
    JNIEnv *env, jobject thiz,
    jdouble jd, jint body_id) {
    double result[6];
    char err[256];
    int flags = SEFLG_SPEED | SEFLG_SWIEPH;

    LOGI("swe_calc_ut: body=%d, jd=%.6f, flags=%d", body_id, jd, flags);
    int rc = swe_calc_ut(jd, body_id, flags, result, err);
    if (rc < 0) {
        LOGE("swe_calc_ut error for body %d (rc=%d): %s", body_id, rc, err);
        return NULL;
    }

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 4);
    if (jresult == NULL) return NULL;
    double out[4] = { result[0], result[1], result[2], result[3] };
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 4, out);
    return jresult;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeCalculateHouses(
    JNIEnv *env, jobject thiz,
    jdouble jd, jdouble lat, jdouble lon, jchar house_system) {
    double cusps[13];
    double ascmc[10];
    char sys = (char)house_system;

    int rc = swe_houses(jd, lat, lon, sys, cusps, ascmc);
    if (rc < 0) {
        LOGE("swe_houses error for system %c", sys);
        return NULL;
    }

    double out[16];
    for (int i = 0; i < 12; i++) {
        out[i] = cusps[i + 1];
    }
    out[12] = ascmc[0]; /* ASC */
    out[13] = ascmc[1]; /* MC */
    double dsc = ascmc[0] + 180.0;
    if (dsc >= 360.0) dsc -= 360.0;
    out[14] = dsc;      /* DSC */
    double ic = ascmc[1] + 180.0;
    if (ic >= 360.0) ic -= 360.0;
    out[15] = ic;       /* IC */

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 16);
    if (jresult == NULL) return NULL;
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 16, out);
    return jresult;
}

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeSetSiderealMode(
    JNIEnv *env, jobject thiz, jint sid_mode) {
    swe_set_sid_mode(sid_mode, 0, 0);
    LOGI("Set sidereal mode to: %d", sid_mode);
}

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeSetTopographicPosition(
    JNIEnv *env, jobject thiz,
    jdouble lon, jdouble lat, jdouble alt) {
    swe_set_topo(lon, lat, alt);
    LOGI("Set topographic position to: lon=%.4f, lat=%.4f, alt=%.1f", lon, lat, alt);
}

JNIEXPORT jdoubleArray JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeCalculateBodyWithFlags(
    JNIEnv *env, jobject thiz,
    jdouble jd, jint body_id, jint flags) {
    double result[6];
    char err[256];

    LOGI("swe_calc_ut: body=%d, jd=%.6f, flags=%d", body_id, jd, flags);
    int rc = swe_calc_ut(jd, body_id, flags, result, err);
    if (rc < 0) {
        LOGE("swe_calc_ut error for body %d (rc=%d): %s", body_id, rc, err);
        return NULL;
    }

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 4);
    if (jresult == NULL) return NULL;
    double out[4] = { result[0], result[1], result[2], result[3] };
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 4, out);
    return jresult;
}
