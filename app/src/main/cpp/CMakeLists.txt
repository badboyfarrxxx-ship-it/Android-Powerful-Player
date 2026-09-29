cmake_minimum_required(VERSION 3.22.1)
project("vpn_player_core")

# Add the local include directory for OpenSSL stubs
include_directories("../../../../jni/vpn/include")

add_library(vpn_player_core SHARED
    ../../../../jni/vpn/WireGuardCore.cpp
)

find_library(log-lib log)

target_link_libraries(vpn_player_core ${log-lib} crypto)
