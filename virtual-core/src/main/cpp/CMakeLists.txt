cmake_minimum_required(VERSION 3.22.1)
project("sandboxr_native" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enforce 16KB ELF page alignment for Android 15 (API 35), 16 (API 36), and 17 (API 37) compatibility
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Configure Prefab packages
find_package(shadowhook REQUIRED CONFIG)
find_package(bytehook REQUIRED CONFIG)

add_library(sandboxr_native SHARED
    sandboxr_native.cpp
    shadowhook_props.cpp
    bytehook_fs.cpp
)

find_library(log-lib log)

target_include_directories(sandboxr_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Explicitly enforce 16KB ELF max page size on target for Android 15, 16, and 17 compatibility
target_link_options(sandboxr_native PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(sandboxr_native
    ${log-lib}
    shadowhook::shadowhook
    bytehook::bytehook
)
