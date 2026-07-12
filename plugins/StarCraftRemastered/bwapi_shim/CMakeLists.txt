#20260701_kpopmodder: Builds only the safe source-level BWAPI shim, not an injector.
cmake_minimum_required(VERSION 3.15)
project(LAV_BWAPI_RM LANGUAGES CXX)

set(LAV_SAIDA_SOURCE_DIR "" CACHE PATH "Local SAIDA source root, for example C:/Vtuber_Souorce_Code/SAIDA-saida-aiide2018")
set(LAV_DEFAULT_SAIDA_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../SAIDA-saida-aiide2018")
if(NOT LAV_SAIDA_SOURCE_DIR AND EXISTS "${LAV_DEFAULT_SAIDA_SOURCE_DIR}/src/SAIDA")
    set(LAV_SAIDA_SOURCE_DIR "${LAV_DEFAULT_SAIDA_SOURCE_DIR}" CACHE PATH "Local SAIDA source root" FORCE)
endif()

option(LAV_BUILD_SAIDA_MOCK "Build the local SAIDA source against lav_bwapi_rm and MockBridge." OFF)

add_library(lav_bwapi_rm STATIC
    src/BWAPI.cpp
    src/Bridge.cpp
    src/CompatRunner.cpp
    src/FileBridge.cpp
    src/MockBridge.cpp
    src/MockGameStateProvider.cpp
)

target_include_directories(lav_bwapi_rm
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(lav_bwapi_rm PUBLIC cxx_std_17)

add_library(minimal_saida_style_bot MODULE
    examples/minimal_saida_style_bot.cpp
)

target_link_libraries(minimal_saida_style_bot PRIVATE lav_bwapi_rm)

set_target_properties(minimal_saida_style_bot PROPERTIES
    PREFIX ""
)

add_executable(mock_runtime_probe
    examples/mock_runtime_probe.cpp
)

target_link_libraries(mock_runtime_probe PRIVATE lav_bwapi_rm)

add_executable(scr_readonly_runtime
    examples/scr_readonly_runtime.cpp
)

target_link_libraries(scr_readonly_runtime PRIVATE lav_bwapi_rm)

if(LAV_BUILD_SAIDA_MOCK)
    if(NOT EXISTS "${LAV_SAIDA_SOURCE_DIR}/src/SAIDA")
        message(FATAL_ERROR "LAV_BUILD_SAIDA_MOCK=ON requires LAV_SAIDA_SOURCE_DIR to point at the SAIDA source root.")
    endif()

    file(GLOB_RECURSE LAV_SAIDA_SOURCES CONFIGURE_DEPENDS
        "${LAV_SAIDA_SOURCE_DIR}/src/SAIDA/*.cpp"
    )

    set(LAV_SAIDA_RUNTIME_SOURCES "")
    foreach(LAV_SAIDA_SOURCE ${LAV_SAIDA_SOURCES})
        get_filename_component(LAV_SAIDA_SOURCE_NAME "${LAV_SAIDA_SOURCE}" NAME)
        if(NOT LAV_SAIDA_SOURCE_NAME STREQUAL "main.cpp"
            AND NOT LAV_SAIDA_SOURCE_NAME STREQUAL "dllmain.cpp"
            AND NOT LAV_SAIDA_SOURCE_NAME STREQUAL "MyBotModule.cpp"
            AND NOT LAV_SAIDA_SOURCE_NAME STREQUAL "EasyBMPsample.cpp")
            list(APPEND LAV_SAIDA_RUNTIME_SOURCES "${LAV_SAIDA_SOURCE}")
        endif()
    endforeach()

    add_executable(saida_mock_runtime
        examples/saida_mock_runtime.cpp
        src/SaidaMockMyBotModule.cpp
        ${LAV_SAIDA_RUNTIME_SOURCES}
    )

    target_include_directories(saida_mock_runtime
        PRIVATE
            "${LAV_SAIDA_SOURCE_DIR}/src/SAIDA"
    )

    target_link_libraries(saida_mock_runtime PRIVATE lav_bwapi_rm)
    target_compile_features(saida_mock_runtime PRIVATE cxx_std_17)
    target_compile_definitions(saida_mock_runtime
        PRIVATE
            NOMINMAX
            WIN32
            _CONSOLE
            _CRT_SECURE_NO_WARNINGS
            NDEBUG
            LAV_BWAPI_RM_SAIDA_MOCK
    )
    if(MSVC)
        target_compile_options(saida_mock_runtime
            PRIVATE
                /EHa
                /Zc:threadSafeInit-
                /FIeh.h
                "/FI${CMAKE_CURRENT_SOURCE_DIR}/include/LAVBWAPIRM/SaidaBuildCompat.h"
        )
    endif()
endif()
