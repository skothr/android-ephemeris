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

target_link_libraries(swisseph_jni
    android
    log
)
