#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "swisseph/swephexp.h"

#define LOG_TAG "SwissEphemeris"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeInit(
    JNIEnv *env, jobject thiz, jstring ephe_path) {
    const char *path = (*env)->GetStringUTFChars(env, ephe_path, NULL);
    swe_set_ephe_path((char *)path);
    (*env)->ReleaseStringUTFChars(env, ephe_path, path);
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

    int rc = swe_calc_ut(jd, body_id, flags, result, err);
    if (rc < 0) {
        LOGE("swe_calc_ut error for body %d: %s", body_id, err);
        return NULL;
    }

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 4);
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
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 16, out);
    return jresult;
}
