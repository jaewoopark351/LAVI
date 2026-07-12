# Source files
file(GLOB SOURCES_SC2LADDERSERVER "sc2ladderserver/*.cpp" "sc2ladderserver/*.h")
file(GLOB SOURCES_LAVHUMANVSBOT "lavhumanvsbot/*.cpp" "lavhumanvsbot/*.h")
# #20260712_kpopmodder: LAN Lobby native targets are commented out for maintenance safety.
# # Do not re-enable without an explicit LAN revive.
# #20260711_kpopmodder: Keep LAN Lobby remote-human experiments in a dedicated launcher target so local matches stay isolated.
# file(GLOB SOURCES_LAVLANHUMANVSBOT "lavlanhumanvsbot/*.cpp" "lavlanhumanvsbot/*.h")
# #20260711_kpopmodder: LAN Lobby server entrypoint copied away from Sc2LadderServer for future LAN-only changes.
# file(GLOB SOURCES_LAVLANLADDERSERVER "lavlanladderserver/*.cpp" "lavlanladderserver/*.h")
# #20260711_kpopmodder: LAN Lobby-only ladder core separated from sc2laddercore so Local Match behavior stays isolated.
# file(GLOB SOURCES_LAVLANLADDERCORE "lavlanladdercore/*.cpp" "lavlanladdercore/*.h")
file(GLOB SOURCES_SC2LADDERCORE "sc2laddercore/*.cpp" "sc2laddercore/*.h")

# Include directories
include_directories(SYSTEM
    ${PROJECT_SOURCE_DIR}/src/sc2laddercore
#     ${PROJECT_SOURCE_DIR}/src/lavlanladdercore
    ${SC2_API_SOURCE_DIR}/include
    ${SC2_API_SOURCE_DIR}/contrib/protobuf/src
        ${SC2_API_SOURCE_DIR}/contrib/civetweb/include
${SC2_API_GENERATED_DIR}
    ${PROJECT_SOURCE_DIR}/rapidjson
)

if (WIN32)
    include_directories(SYSTEM
        ${SC2_API_SOURCE_DIR}/src/sc2utils/
    )
endif ()

# Link directories
link_directories(${PROJECT_BINARY_DIR} ${PROJECT_BINARY_DIR}/cpp-sc2/bin)


# Create the executable.
add_executable(Sc2LadderServer ${SOURCES_SC2LADDERSERVER})
#20260709_kpopmodder: Small local launcher for LAV human-vs-Changeling tests without Qt/SC2AIApp UI automation.
add_executable(LavHumanVsBot ${SOURCES_LAVHUMANVSBOT})
# #20260712_kpopmodder: LAN Lobby native targets are archived/commented out.
# #20260711_kpopmodder: LAN Lobby launcher uses the same core but is a separate executable boundary from Local Match.
# add_executable(LavLanHumanVsBot ${SOURCES_LAVLANHUMANVSBOT})
# #20260711_kpopmodder: Preferred LAN Lobby executable. LavLanHumanVsBot remains as a compatibility fallback.
# add_executable(LavLanLadderServer ${SOURCES_LAVLANLADDERSERVER})
add_library(Sc2LadderCore ${SOURCES_SC2LADDERCORE})
# add_library(LavLanLadderCore ${SOURCES_LAVLANLADDERCORE})
# target_include_directories(LavLanHumanVsBot PRIVATE
#     ${PROJECT_SOURCE_DIR}/src/lavlanladdercore
# )
# target_include_directories(LavLanLadderServer PRIVATE
#     ${PROJECT_SOURCE_DIR}/src/lavlanladdercore
# )
# target_include_directories(LavLanLadderCore PRIVATE
#     ${PROJECT_SOURCE_DIR}/src/lavlanladdercore
#     ${PROJECT_SOURCE_DIR}/src/sc2laddercore
# )
target_link_libraries(Sc2LadderServer
    Sc2LadderCore
)
target_link_libraries(LavHumanVsBot
    Sc2LadderCore sc2api sc2lib sc2utils sc2protocol civetweb libprotobuf ipv6-parse
)
# target_link_libraries(LavLanHumanVsBot
#     LavLanLadderCore Sc2LadderCore sc2api sc2lib sc2utils sc2protocol civetweb libprotobuf ipv6-parse
# )
# target_link_libraries(LavLanLadderServer
#     LavLanLadderCore Sc2LadderCore sc2api sc2lib sc2utils sc2protocol civetweb libprotobuf ipv6-parse
# )
target_link_libraries(Sc2LadderCore
    sc2api sc2lib sc2utils sc2protocol civetweb libprotobuf ipv6-parse
)
# target_link_libraries(LavLanLadderCore
#     Sc2LadderCore sc2api sc2lib sc2utils sc2protocol civetweb libprotobuf ipv6-parse
# )


# Set working directory as the project root
set_target_properties(Sc2LadderServer PROPERTIES VS_DEBUGGER_WORKING_DIRECTORY "${CMAKE_SOURCE_DIR}")
