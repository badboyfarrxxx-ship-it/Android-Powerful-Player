cmake_minimum_required(VERSION 3.10.2)

project("powerful_player_jni")

# FFmpeg shared libraries to link against
set(FFMPEG_LIBS
    avcodec
    avformat
    avutil
    swscale
)

# Find FFmpeg headers and libraries
# Note: In a real Android environment, these paths are typically provided
# via environment variables or predefined NDK paths.
set(FFMPEG_INCLUDE_DIR ${CMAKE_SOURCE_DIR}/include)
set(FFMPEG_LIB_DIR ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})

add_library(powerful_player_jni SHARED FFmpegBridge.cpp)

target_include_directories(powerful_player_jni PRIVATE ${FFMPEG_INCLUDE_DIR})

target_link_libraries(powerful_player_jni
    ${FFMPEG_LIBS}
    log # Android logging library
    android
)
