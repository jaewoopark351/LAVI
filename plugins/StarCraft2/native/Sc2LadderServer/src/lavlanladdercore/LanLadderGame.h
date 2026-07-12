// #20260712_kpopmodder: LAN Lobby native source commented out for maintenance safety.
// # Do not re-enable without an explicit LAN revive.
// //20260711_kpopmodder: LAN Lobby-only game coordinator separated from Local Match LadderGame.
// #pragma once

// #include "LadderConfig.h"
// #include "Types.h"

// #include <string>

// #define LAN_PORT_START 5690
// #define LAN_FIRST_PLAYER_NAME "foo5679"
// #define LAN_SECOND_PLAYER_NAME "foo5680"

// class LanLadderGame
// {
// public:
//     LanLadderGame(int inCoordinatorArgc, char** inCoordinatorArgv, LadderConfig* inConfig);
//     void SetRealTime(bool inRealTime);
//     GameResult StartRemoteHumanGame(
//         const BotConfig& remoteHumanAgent,
//         const BotConfig& botAgent,
//         const std::string& map,
//         const std::string& remoteHumanHost,
//         int remoteHumanClientPort,
//         const std::string& lanGameHostIp);

// private:
//     void LogStartGame(const BotConfig& bot1, const BotConfig& bot2);
//     void ChangeBotNames(const std::string& replayFile, const std::string& bot1Name, const std::string& bot2Name);

//     int CoordinatorArgc;
//     char** CoordinatorArgv;
//     LadderConfig* Config;
//     uint32_t MaxGameTime{0U};
//     uint32_t MaxRealGameTime{0U};
//     bool RealTime{false};
// };
