// #20260712_kpopmodder: LAN Lobby native source commented out for maintenance safety.
// # Do not re-enable without an explicit LAN revive.
// //20260711_kpopmodder: LAN Lobby-only game coordinator implementation.
// #include "LanLadderGame.h"

// #include <algorithm>
// #include <cctype>
// #include <chrono>
// #include <ctime>
// #include <fstream>
// #include <iomanip>
// #include <sstream>

// #include "LanProxy.h"
// #include "Tools.h"

// #include "sc2api/sc2_args.h"
// #include "sc2api/sc2_game_settings.h"
// #include "sc2lib/sc2_lib.h"
// #include "sc2utils/sc2_manage_process.h"

// LanLadderGame::LanLadderGame(int inCoordinatorArgc, char** inCoordinatorArgv, LadderConfig* inConfig)
//     : CoordinatorArgc(inCoordinatorArgc)
//     , CoordinatorArgv(inCoordinatorArgv)
//     , Config(inConfig)
// {
//     const int maxGameTimeInt = Config->GetIntValue("MaxGameTime");
//     MaxGameTime = maxGameTimeInt > 0 ? static_cast<uint32_t>(maxGameTimeInt) : 0;
//     const int maxRealGameTimeInt = Config->GetIntValue("MaxRealGameTime");
//     MaxRealGameTime = maxRealGameTimeInt > 0 ? static_cast<uint32_t>(maxRealGameTimeInt) : 0;
//     RealTime = Config->GetBoolValue("RealTimeMode");
// }

// void LanLadderGame::SetRealTime(bool inRealTime)
// {
//     RealTime = inRealTime;
// }

// void LanLadderGame::LogStartGame(const BotConfig& bot1, const BotConfig& bot2)
// {
//     const auto writeStartLog = [](const BotConfig& bot, const std::string& opponentName) {
//         if (bot.Type == BotType::Human || bot.RootPath.empty())
//         {
//             return;
//         }
//         std::time_t t = std::time(nullptr);
//         std::tm tm = *std::localtime(&t);
//         const std::string filename = bot.RootPath + "/data/stderr.log";
//         std::ofstream outfile;
//         outfile.open(filename, std::ios_base::app);
//         outfile << std::endl << std::put_time(&tm, "%d-%m-%Y %H-%M-%S")
//                 << ": Starting game vs " << opponentName << std::endl;
//         outfile.close();
//     };
//     writeStartLog(bot1, bot2.BotName);
//     writeStartLog(bot2, bot1.BotName);
// }

// GameResult LanLadderGame::StartRemoteHumanGame(
//     const BotConfig& remoteHumanAgent,
//     const BotConfig& botAgent,
//     const std::string& map,
//     const std::string& remoteHumanHost,
//     int remoteHumanClientPort,
//     const std::string& lanGameHostIp)
// {
//     LogStartGame(remoteHumanAgent, botAgent);

//     LanProxy proxyHuman(MaxGameTime, MaxRealGameTime, remoteHumanAgent, lanGameHostIp);
//     LanProxy proxyBot(MaxGameTime, MaxRealGameTime, botAgent, lanGameHostIp);

//     sc2::ProcessSettings processSettings;
//     sc2::GameSettings gameSettings;
//     sc2::ParseSettings(CoordinatorArgc, CoordinatorArgv, processSettings, gameSettings);

//     constexpr int portServerHuman = 5677;
//     constexpr int portServerBot = 5678;
//     constexpr int portClientBot = 5680;
//     constexpr int remoteApiWaitSeconds = 150;
//     constexpr unsigned int remotePingTimeoutMS = 2000U;

//     if (remoteHumanHost.empty() || remoteHumanClientPort <= 0)
//     {
//         PrintThread{} << "[LAN] Missing remote human SC2 endpoint." << std::endl;
//         return GameResult();
//     }
//     if (lanGameHostIp.empty())
//     {
//         PrintThread{} << "[LAN] WARNING: --lan-game-host-ip is empty. Remote JoinGame may fall back to localhost." << std::endl;
//     }

//     PrintThread{} << "[LAN] Starting isolated LAN remote-human clients." << std::endl;
//     PrintThread{} << "[LAN] Remote human SC2 client: " << remoteHumanHost << ":" << remoteHumanClientPort << "." << std::endl;
//     PrintThread{} << "[LAN] Local bot SC2 client port: " << portClientBot << "." << std::endl;
//     PrintThread{} << "[LAN] CreateGame owner: host-local-bot; JoinGame host_ip="
//                   << (lanGameHostIp.empty() ? "<unset>" : lanGameHostIp) << "." << std::endl;

//     proxyHuman.StartProxyServer(portServerHuman);
//     proxyBot.StartSC2Instance(processSettings, portServerBot, portClientBot);

//     const bool remoteReady = proxyHuman.ConnectToSC2ClientReady(
//         remoteHumanHost,
//         remoteHumanClientPort,
//         remoteApiWaitSeconds,
//         remotePingTimeoutMS);
//     const bool botClientReady = proxyBot.ConnectToSC2Instance(processSettings, portServerBot, portClientBot);
//     if (!remoteReady || !botClientReady)
//     {
//         PrintThread{} << "[LAN] Failed to start the StarCraft II clients. remote_ready="
//                       << (remoteReady ? "true" : "false")
//                       << " bot_ready=" << (botClientReady ? "true" : "false") << std::endl;
//         return GameResult();
//     }

//     PrintThread{} << "[LAN] Creating the game on host-local-bot SC2 with map " << map << "." << std::endl;
//     const bool setupBotGame = proxyBot.SetupGameAsHost(processSettings, map, RealTime, remoteHumanAgent.Race, botAgent.Race);
//     if (!setupBotGame)
//     {
//         PrintThread{} << "[LAN] Failed to create the game on host-local-bot SC2." << std::endl;
//         return GameResult();
//     }

//     PrintThread{} << "[LAN] Starting remote human API bridge on port " << portServerHuman << "." << std::endl;
//     const bool startHumanBridge = proxyHuman.StartBot(portServerHuman, LAN_PORT_START, botAgent.PlayerId);
//     if (!startHumanBridge)
//     {
//         PrintThread{} << "[LAN] Failed to start remote human API bridge for " << remoteHumanAgent.BotName << "." << std::endl;
//         return GameResult();
//     }

//     PrintThread{} << "[LAN] Starting bot " << botAgent.BotName << " on port " << portServerBot << "." << std::endl;
//     const bool startBot = proxyBot.StartBot(portServerBot, LAN_PORT_START, remoteHumanAgent.PlayerId);
//     if (!startBot)
//     {
//         PrintThread{} << "[LAN] Failed to start " << botAgent.BotName << "." << std::endl;
//         return GameResult();
//     }

//     PrintThread{} << "[LAN] Starting the match." << std::endl;
//     proxyHuman.StartGameLoop();
//     proxyBot.StartGameLoop();

//     while (!proxyHuman.GameFinished() || !proxyBot.GameFinished())
//     {
//         sc2::SleepFor(1000);
//     }

//     std::string replayDir = Config->GetStringValue("LocalReplayDirectory");
//     if (!replayDir.empty() && replayDir.back() != '/')
//     {
//         replayDir += "/";
//     }
//     std::string replayFile = replayDir + remoteHumanAgent.BotName + "v" + botAgent.BotName + "-" + RemoveMapExtension(map) + ".SC2Replay";
//     replayFile.erase(std::remove_if(replayFile.begin(), replayFile.end(), isspace), replayFile.end());
//     if (!(proxyHuman.SaveReplay(replayFile) || proxyBot.SaveReplay(replayFile)))
//     {
//         PrintThread{} << "[LAN] Saving replay failed." << std::endl;
//     }
//     ChangeBotNames(replayFile, remoteHumanAgent.BotName, botAgent.BotName);

//     GameResult result;
//     const auto resultHuman = proxyHuman.GetResult();
//     const auto resultBot = proxyBot.GetResult();

//     result.Result = getEndResultFromProxyResults(resultHuman, resultBot);
//     result.Bot1AvgFrame = proxyHuman.Stats().avgLoopDuration;
//     result.Bot2AvgFrame = proxyBot.Stats().avgLoopDuration;
//     result.GameLoop = proxyHuman.Stats().gameLoops;

//     std::time_t t = std::time(nullptr);
//     std::tm tm = *std::gmtime(&t);
//     std::ostringstream oss;
//     oss << std::put_time(&tm, "%d-%m-%Y %H-%M-%S") << "UTC";
//     result.TimeStamp = oss.str();
//     return result;
// }

// void LanLadderGame::ChangeBotNames(const std::string& replayFile, const std::string& bot1Name, const std::string& bot2Name)
// {
//     std::string cmdLine = Config->GetStringValue("ReplayBotRenameProgram");
//     if (cmdLine.size() > 0)
//     {
//         cmdLine = cmdLine + " " + replayFile + " " + LAN_FIRST_PLAYER_NAME + " " + bot1Name + " " + LAN_SECOND_PLAYER_NAME + " " + bot2Name;
//         StartExternalProcess(cmdLine);
//     }
// }
