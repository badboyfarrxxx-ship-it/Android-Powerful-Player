cmake_minimum_required(VERSION 3.22.1)
project("vpn_player_core")

# The path is relative to the directory containing this CMakeLists.txt
# Current: app/src/main/cpp/CMakeLists.txt
# Target: jni/vpn/WireGuardCore.cpp
# Relative path: ../../../../jni/vpn/WireGuardCore.cpp

add_library(vpn_player_core SHARED
    ../../../../jni/vpn/WireGuardCore.cpp
)

find_library(log-lib log)

target_link_libraries(vpn_player_core ${log-lib})
