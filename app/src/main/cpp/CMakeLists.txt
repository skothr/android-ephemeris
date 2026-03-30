cmake_minimum_required(VERSION 3.22.1)
project("ephemeris")

file(GLOB SWISSEPH_SOURCES "swisseph/*.c")

add_library(swisseph_jni SHARED
    swisseph_wrapper.c
    ${SWISSEPH_SOURCES}
)

target_include_directories(swisseph_jni PRIVATE
    swisseph
)

# Disable thread-local storage in Swiss Ephemeris.
# TLS causes init on one thread (IO) to be invisible to calculations on another (Main).
# Our Kotlin Mutex already serializes all access, so TLS is unnecessary.
target_compile_definitions(swisseph_jni PRIVATE TLSOFF)

target_link_libraries(swisseph_jni
    android
    log
)
