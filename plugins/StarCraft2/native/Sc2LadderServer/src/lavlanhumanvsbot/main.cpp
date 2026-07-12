// #20260712_kpopmodder: LAN Lobby native source commented out for maintenance safety.
// # Do not re-enable without an explicit LAN revive.
// //20260711_kpopmodder: Dedicated LAN Lobby launcher so remote-human behavior can evolve without changing Local Match.
// #include <algorithm>
// #include <cctype>
// #include <cstdlib>
// #include <iostream>
// #include <string>

// #include "AgentsConfig.h"
// #include "LadderConfig.h"
// #include "LanLadderGame.h"
// #include "Types.h"

// namespace
// {
// bool hasArg(int argc, char** argv, const std::string& name)
// {
//     const std::string prefix = name + "=";
//     for (int i = 1; i < argc; ++i)
//     {
//         const std::string arg = argv[i] ? argv[i] : "";
//         if (arg == name || arg.rfind(prefix, 0) == 0)
//         {
//             return true;
//         }
//     }
//     return false;
// }

// std::string argValue(int argc, char** argv, const std::string& name, const std::string& fallback)
// {
//     const std::string prefix = name + "=";
//     for (int i = 1; i < argc; ++i)
//     {
//         const std::string arg = argv[i] ? argv[i] : "";
//         if (arg == name && i + 1 < argc)
//         {
//             return argv[i + 1] ? argv[i + 1] : fallback;
//         }
//         if (arg.rfind(prefix, 0) == 0)
//         {
//             return arg.substr(prefix.size());
//         }
//     }
//     return fallback;
// }

// void printUsage()
// {
//     std::cout
//         << "LavLanHumanVsBot - LAN Lobby remote human vs ProBots launcher\n"
//         << "Usage:\n"
//         << "  LavLanHumanVsBot.exe --human-name RemoteHuman --bot changeling --map PersephoneLE.SC2Map --race Protoss --bot-dir Bots/ --config HumanLadder.json --remote-human-host 192.168.0.20 --remote-human-client-port 5679 --lan-game-host-ip 192.168.0.10\n\n"
//         << "Required LAN arguments:\n"
//         << "  --remote-human-host <ip-or-hostname>\n"
//         << "  --remote-human-client-port <port>\n\n"
//         << "Recommended LAN arguments:\n"
//         << "  --lan-game-host-ip <host-lan-ip>  Host PC IP that remote SC2 should use for JoinGame host_ip.\n\n"
//         << "LAN JoinGame port role:\n"
//         << "  --remote-human-join-port-role client|server (default: client)\n\n"
//         << "SC2PATH should point to the full SC2_x64.exe path. LAV injects that environment value before launch.\n";
// }
// }

// int main(int argc, char** argv)
// {
//     if (hasArg(argc, argv, "--help") || hasArg(argc, argv, "-h"))
//     {
//         printUsage();
//         return 0;
//     }

//     const std::string humanName = argValue(argc, argv, "--human-name", "RemoteHuman");
//     const std::string botName = argValue(argc, argv, "--bot", "changeling");
//     const std::string mapName = argValue(argc, argv, "--map", "PersephoneLE.SC2Map");
//     const std::string raceName = argValue(argc, argv, "--race", "Protoss");
//     const std::string botRaceName = argValue(argc, argv, "--bot-race", "");
//     const std::string botDirectory = argValue(argc, argv, "--bot-dir", "Bots");
//     const std::string configPath = argValue(argc, argv, "--config", "HumanLadder.json");
//     const std::string remoteHumanHost = argValue(argc, argv, "--remote-human-host", "");
//     const int remoteHumanClientPort = std::atoi(argValue(argc, argv, "--remote-human-client-port", "0").c_str());
//     const std::string lanGameHostIp = argValue(argc, argv, "--lan-game-host-ip", "");
//     const std::string remoteHumanJoinPortRole = argValue(argc, argv, "--remote-human-join-port-role", "client");
//     const bool realtime = !hasArg(argc, argv, "--no-realtime");

//     if (remoteHumanHost.empty() || remoteHumanClientPort <= 0)
//     {
//         std::cerr << "[LavLanHumanVsBot] Missing required remote human endpoint. "
//                   << "Use --remote-human-host and --remote-human-client-port." << std::endl;
//         printUsage();
//         return 2;
//     }

//     LadderConfig config(configPath);
//     if (!config.ParseConfig())
//     {
//         std::cerr << "[LavLanHumanVsBot] Unable to parse config: " << configPath << std::endl;
//         return 2;
//     }

//     AgentsConfig agents(&config);
//     agents.ReadBotDirectories(botDirectory);

//     BotConfig bot;
//     if (!agents.FindBot(botName, bot))
//     {
//         std::cerr << "[LavLanHumanVsBot] Unable to find bot: " << botName << " in " << botDirectory << std::endl;
//         return 3;
//     }
//     if (!botRaceName.empty())
//     {
//         bot.Race = GetRaceFromString(botRaceName);
//     }

//     BotConfig human;
//     human.Type = BotType::Human;
//     human.BotName = humanName;
//     human.Race = GetRaceFromString(raceName);
//     human.PlayerId = "HUMAN";
//     human.Skeleton = false;
//     human.Enabled = true;
//     human.Args = "--remote-human-join-port-role " + remoteHumanJoinPortRole;

//     std::cout << "[LavLanHumanVsBot] Starting LAN remote human " << human.BotName
//               << " vs " << bot.BotName
//               << " on " << mapName
//               << " human-race=" << GetRaceString(human.Race)
//               << " bot-race=" << GetRaceString(bot.Race)
//               << " realtime=" << (realtime ? "true" : "false")
//               << " remote-human=" << remoteHumanHost << ":" << remoteHumanClientPort
//               << " lan-game-host-ip=" << (lanGameHostIp.empty() ? "<unset>" : lanGameHostIp)
//               << " join-port-role=" << remoteHumanJoinPortRole
//               << std::endl;

//     LanLadderGame game(argc, argv, &config);
//     game.SetRealTime(realtime);
//     GameResult result = game.StartRemoteHumanGame(human, bot, mapName, remoteHumanHost, remoteHumanClientPort, lanGameHostIp);
//     std::cout << "[LavLanHumanVsBot] Finished with result: " << GetResultType(result.Result) << std::endl;

//     if (result.Result == ResultType::InitializationError || result.Result == ResultType::Error)
//     {
//         return 4;
//     }
//     return 0;
// }
