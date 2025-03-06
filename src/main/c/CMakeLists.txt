cmake_minimum_required(VERSION 3.14)
project(sha3_test)

find_package(beethoven REQUIRED)

set(CMAKE_CXX_STANDARD 20)

# We wrap `add_executable` with utilities for linking
# everything together. If you need additional CMake
# commands you can use `vec_add_test` as you would
# any other target
beethoven_build(sha3_test
        SOURCES sha3WrapperTest.cc)