// #20260712_kpopmodder: LAN Lobby native source commented out for maintenance safety.
// # Do not re-enable without an explicit LAN revive.
// //20260711_kpopmodder: LAN Lobby-only SC2 API proxy implementation.
// #include "LanProxy.h"

// #include <algorithm>
// #include <atomic>
// #include <fstream>
// #include <iomanip>
// #include <sstream>
// #include <thread>

// #include "Tools.h"
// #include "Types.h"

// #include "sc2api/sc2_proto_interface.h"
// #include "sc2utils/sc2_manage_process.h"

// namespace
// {
// constexpr unsigned int kJoinGameResponseTimeoutMS = 30000U;

// std::string BoolString(const bool value)
// {
//     return value ? "true" : "false";
// }

// std::string HexPrefix(const std::string& bytes, const size_t maxBytes)
// {
//     std::ostringstream ss;
//     ss << std::hex << std::setfill('0');
//     const size_t count = std::min(maxBytes, bytes.size());
//     for (size_t i = 0; i < count; ++i)
//     {
//         if (i)
//         {
//             ss << ' ';
//         }
//         ss << std::setw(2)
//            << static_cast<unsigned int>(static_cast<unsigned char>(bytes[i]));
//     }
//     if (bytes.size() > count)
//     {
//         ss << " ...";
//     }
//     return ss.str();
// }

// std::string RequestDebugSummary(
//     const SC2APIProtocol::Request& request,
//     const SC2APIProtocol::Response::ResponseCase expectedResponseCase)
// {
//     std::ostringstream ss;
//     const std::string payload = request.SerializeAsString();
//     ss << "request_case=" << static_cast<int>(request.request_case())
//        << " expected_response=" << responseCaseToString(expectedResponseCase)
//        << " byte_size=" << request.ByteSize()
//        << " has_join_game=" << BoolString(request.has_join_game())
//        << " has_observation=" << BoolString(request.has_observation())
//        << " has_step=" << BoolString(request.has_step())
//        << " has_leave_game=" << BoolString(request.has_leave_game())
//        << " payload_hex_prefix=" << HexPrefix(payload, 96U);
//     if (request.has_join_game())
//     {
//         const auto& joinGame = request.join_game();
//         ss << " join_host_ip="
//            << (joinGame.has_host_ip() ? joinGame.host_ip() : "<unset>")
//            << " join_shared_port="
//            << (joinGame.has_shared_port() ? std::to_string(joinGame.shared_port()) : "<unset>");
//         if (joinGame.has_server_ports())
//         {
//             ss << " join_server_ports="
//                << joinGame.server_ports().game_port()
//                << "/"
//                << joinGame.server_ports().base_port();
//         }
//         else
//         {
//             ss << " join_server_ports=<unset>";
//         }
//         ss << " join_client_ports=";
//         if (joinGame.client_ports_size() == 0)
//         {
//             ss << "<none>";
//         }
//         else
//         {
//             for (int i = 0; i < joinGame.client_ports_size(); ++i)
//             {
//                 if (i)
//                 {
//                     ss << ",";
//                 }
//                 ss << joinGame.client_ports(i).game_port()
//                    << "/"
//                    << joinGame.client_ports(i).base_port();
//             }
//         }
//     }
//     return ss.str();
// }

// std::string ResponseDebugSummary(const SC2APIProtocol::Response& response)
// {
//     std::ostringstream ss;
//     const std::string payload = response.SerializeAsString();
//     ss << "response_case=" << responseCaseToString(response.response_case())
//        << " byte_size=" << response.ByteSize()
//        << " error_count=" << response.error_size()
//        << " has_join_game=" << BoolString(response.has_join_game())
//        << " has_status=" << BoolString(response.has_status())
//        << " payload_hex_prefix=" << HexPrefix(payload, 96U);
//     if (response.has_status())
//     {
//         ss << " status=" << statusToString(response.status());
//     }
//     if (response.has_join_game())
//     {
//         ss << " join_player_id=" << response.join_game().player_id();
//     }
//     return ss.str();
// }

// void ApplyLanHostIp(SC2APIProtocol::RequestJoinGame* joinGame, const std::string& lanGameHostIp)
// {
//     if (joinGame == nullptr || lanGameHostIp.empty())
//     {
//         return;
//     }
//     if (!joinGame->has_host_ip() || joinGame->host_ip().empty())
//     {
//         joinGame->set_host_ip(lanGameHostIp);
//     }
// }

// void ConfigureSecondPlayerPorts(SC2APIProtocol::RequestJoinGame* joinGame, const int startPort)
// {
//     const int sharedPort = startPort + 1;
//     const int firstServerGamePort = startPort + 2;
//     const int firstServerBasePort = startPort + 3;
//     const int secondServerGamePort = startPort + 4;
//     const int secondServerBasePort = startPort + 5;

//     joinGame->set_shared_port(sharedPort);
//     auto* serverPorts = joinGame->mutable_server_ports();
//     serverPorts->set_game_port(secondServerGamePort);
//     serverPorts->set_base_port(secondServerBasePort);

//     joinGame->clear_client_ports();
//     auto* clientPorts = joinGame->add_client_ports();
//     clientPorts->set_game_port(firstServerGamePort);
//     clientPorts->set_base_port(firstServerBasePort);
// }
// }

// LanProxy::LanProxy(
//     const uint32_t maxGameLoops,
//     const uint32_t maxRealGameTime,
//     const BotConfig& botConfig,
//     const std::string& lanGameHostIp)
//     : m_maxGameLoops(maxGameLoops)
//     , m_maxRealGameTime(maxRealGameTime)
//     , m_botConfig(botConfig)
//     , m_lanGameHostIp(lanGameHostIp)
// {
//     m_lastResponseSendTime = clock::now();
// }

// LanProxy::~LanProxy()
// {
//     constexpr auto maxWaitTime{20};
//     const auto start = clock::now();
//     std::chrono::duration<double> elapsedSeconds{0};
//     std::future_status botProgStatus{std::future_status::deferred};
//     if (m_botProgramThread.valid())
//     {
//         while (elapsedSeconds.count() < maxWaitTime)
//         {
//             botProgStatus = m_botProgramThread.wait_for(std::chrono::seconds(1));
//             if (botProgStatus == std::future_status::ready)
//             {
//                 PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Bot terminated properly." << std::endl;
//                 break;
//             }
//             elapsedSeconds = clock::now() - start;
//         }
//         if (botProgStatus != std::future_status::ready)
//         {
//             PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Bot is still running after "
//                           << maxWaitTime << " seconds. Sending kill signal." << std::endl;
//             if (m_botThreadId != 0)
//             {
//                 KillBotProcess(m_botThreadId);
//             }
//         }
//         sc2::SleepFor(5000);
//     }
//     if (m_gameClientPid)
//     {
//         if (!sc2::TerminateProcess(m_gameClientPid))
//         {
//             PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Terminating SC2 failed!" << std::endl;
//         }
//         sc2::SleepFor(5000);
//     }
// }

// void LanProxy::StartProxyServer(const int portServer)
// {
//     m_server.Listen(std::to_string(portServer).c_str(), "100000", "100000", "5");
// }

// void LanProxy::StartSC2Instance(const sc2::ProcessSettings& processSettings, const int portServer, const int portClient)
// {
//     StartProxyServer(portServer);
//     m_gameClientPid = sc2::StartProcess(processSettings.process_path,
//         { "-listen", m_localHost,
//           "-port", std::to_string(portClient),
//           "-displayMode", "0",
//           "-dataVersion", processSettings.data_version });
// }

// bool LanProxy::ConnectToSC2Instance(const sc2::ProcessSettings&, const int, const int portClient)
// {
//     return ConnectToSC2Client(m_localHost, portClient);
// }

// bool LanProxy::ConnectToSC2Client(const std::string& host, const int portClient)
// {
//     size_t connectionAttempts = 0;
//     constexpr size_t abandonConnectionAttemptAfter = 60;
//     constexpr bool withDebugOutput = false;
//     const std::string connectHost = host.empty() ? m_localHost : host;
//     PrintThread{} << "[LAN] Connecting proxy for " << m_botConfig.BotName
//                   << " to SC2 client " << connectHost << ":" << portClient << std::endl;
//     while (!m_client.Connect(connectHost, portClient, withDebugOutput))
//     {
//         ++connectionAttempts;
//         sc2::SleepFor(1000);
//         if (connectionAttempts > abandonConnectionAttemptAfter)
//         {
//             PrintThread{} << "[LAN] Failed to connect to client (" << m_botConfig.BotName
//                           << ") at " << connectHost << ":" << portClient << std::endl;
//             return false;
//         }
//     }

//     sc2::ProtoInterface proto;
//     sc2::GameRequestPtr request = proto.MakeRequest();
//     request->mutable_ping();
//     m_client.Send(request.get());
//     auto* response = ReceiveResponse(SC2APIProtocol::Response::ResponseCase::kPing);
//     return response != nullptr;
// }

// bool LanProxy::ConnectToSC2ClientReady(
//     const std::string& host,
//     const int portClient,
//     const int maxWaitSeconds,
//     const unsigned int pingTimeoutMS)
// {
//     constexpr bool withDebugOutput = false;
//     const std::string connectHost = host.empty() ? m_localHost : host;
//     PrintThread{} << "[LAN] Waiting for SC2 API for " << m_botConfig.BotName
//                   << " at " << connectHost << ":" << portClient
//                   << " max_wait_sec=" << maxWaitSeconds
//                   << " ping_timeout_ms=" << pingTimeoutMS << std::endl;
//     for (int attempt = 1; attempt <= maxWaitSeconds; ++attempt)
//     {
//         if (!m_client.HasConnection())
//         {
//             PrintThread{} << "[LAN] TCP connect attempt " << attempt << " for "
//                           << m_botConfig.BotName << " to SC2 client "
//                           << connectHost << ":" << portClient << std::endl;
//             if (!m_client.Connect(connectHost, portClient, withDebugOutput))
//             {
//                 sc2::SleepFor(1000);
//                 continue;
//             }
//             PrintThread{} << "[LAN] TCP connected for " << m_botConfig.BotName
//                           << " to SC2 client " << connectHost << ":" << portClient << std::endl;
//         }

//         sc2::ProtoInterface proto;
//         sc2::GameRequestPtr request = proto.MakeRequest();
//         request->mutable_ping();
//         m_client.Send(request.get());
//         auto* response = ReceiveResponse(SC2APIProtocol::Response::ResponseCase::kPing, pingTimeoutMS);
//         if (response != nullptr)
//         {
//             PrintThread{} << "[LAN] SC2 API ping ok for " << m_botConfig.BotName
//                           << " at " << connectHost << ":" << portClient << std::endl;
//             return true;
//         }
//         if (m_client.HasConnection())
//         {
//             m_client.Disconnect();
//         }
//         sc2::SleepFor(1000);
//     }
//     PrintThread{} << "[LAN] Failed to get SC2 API ping for " << m_botConfig.BotName
//                   << " at " << connectHost << ":" << portClient
//                   << " within " << maxWaitSeconds << " seconds." << std::endl;
//     return false;
// }

// bool LanProxy::SetupGameAsHost(
//     const sc2::ProcessSettings& processSettings,
//     const std::string& map,
//     const bool realTimeMode,
//     const sc2::Race player1Race,
//     const sc2::Race player2Race)
// {
//     m_realTimeMode = realTimeMode;

//     sc2::ProtoInterface proto;
//     sc2::GameRequestPtr request = proto.MakeRequest();
//     auto* requestCreateGame = request->mutable_create_game();

//     auto* playerSetup = requestCreateGame->add_player_setup();
//     playerSetup->set_type(SC2APIProtocol::PlayerType::Participant);
//     playerSetup->set_race(SC2APIProtocol::Race(static_cast<int>(player1Race) + 1));
//     playerSetup->set_difficulty(SC2APIProtocol::Difficulty::VeryEasy);

//     playerSetup = requestCreateGame->add_player_setup();
//     playerSetup->set_type(SC2APIProtocol::PlayerType::Participant);
//     playerSetup->set_race(SC2APIProtocol::Race(static_cast<int>(player2Race) + 1));
//     playerSetup->set_difficulty(SC2APIProtocol::Difficulty::VeryEasy);

//     if (!sc2::HasExtension(map, ".SC2Map"))
//     {
//         requestCreateGame->set_battlenet_map_name(map);
//     }
//     else
//     {
//         auto* const localMap = requestCreateGame->mutable_local_map();
//         if (sc2::DoesFileExist(map))
//         {
//             localMap->set_map_path(map);
//         }
//         else
//         {
//             const std::string gameRelative = sc2::GetGameMapsDirectory(processSettings.process_path) + map;
//             if (sc2::DoesFileExist(gameRelative))
//             {
//                 localMap->set_map_path(map);
//             }
//             else
//             {
//                 const std::string libraryRelative = sc2::GetLibraryMapsDirectory() + map;
//                 if (sc2::DoesFileExist(libraryRelative))
//                 {
//                     localMap->set_map_path(libraryRelative);
//                 }
//                 else
//                 {
//                     PrintThread{} << "[LAN] " << m_botConfig.BotName
//                                   << " : map not found for CreateGame: " << map << std::endl;
//                     return false;
//                 }
//             }
//         }
//     }

//     requestCreateGame->set_realtime(realTimeMode);
//     PrintThread{} << "[LAN] " << m_botConfig.BotName
//                   << " creating multiplayer game as host. lan_game_host_ip="
//                   << (m_lanGameHostIp.empty() ? "<unset>" : m_lanGameHostIp)
//                   << std::endl;
//     m_client.Send(request.get());
//     auto* createGameResponse = ReceiveResponse(SC2APIProtocol::Response::ResponseCase::kCreateGame);
//     if (!createGameResponse || CreateGameHasErrors(createGameResponse->create_game()))
//     {
//         return false;
//     }
//     return true;
// }

// void LanProxy::RunHumanAgent(const int gamePort, const int startPort, const std::string opponentPlayerId)
// {
//     sc2::Connection bridgeConnection;
//     std::atomic_bool bridgeClosed{false};
//     bridgeConnection.SetConnectionClosedCallback([&bridgeClosed]() {
//         bridgeClosed.store(true);
//     });

//     PrintThread{} << "[LAN] Starting raw human API bridge for " << m_botConfig.BotName
//                   << " on proxy port " << gamePort << std::endl;
//     if (!bridgeConnection.Connect(m_localHost, gamePort))
//     {
//         PrintThread{} << "ERROR: [LAN] Human API bridge could not connect to local proxy for "
//                       << m_botConfig.BotName
//                       << " at " << m_localHost << ":" << gamePort << std::endl;
//         return;
//     }

//     sc2::ProtoInterface proto;
//     sc2::GameRequestPtr joinRequest = proto.MakeRequest();
//     auto* requestJoinGame = joinRequest->mutable_join_game();
//     requestJoinGame->set_race(SC2APIProtocol::Race(static_cast<int>(m_botConfig.Race) + 1));
//     ConfigureSecondPlayerPorts(requestJoinGame, startPort);
//     ApplyLanHostIp(requestJoinGame, m_lanGameHostIp);

//     auto* options = requestJoinGame->mutable_options();
//     options->set_raw(true);
//     options->set_score(true);

//     PrintThread{} << "[LAN] Human API bridge sending raw JoinGame for " << m_botConfig.BotName
//                   << " context: game_port=" << gamePort
//                   << " start_port=" << startPort
//                   << " lan_game_host_ip=" << (m_lanGameHostIp.empty() ? "<unset>" : m_lanGameHostIp)
//                   << " opponent_id=" << opponentPlayerId
//                   << " race=" << GetRaceString(m_botConfig.Race)
//                   << " timeout_ms=" << kJoinGameResponseTimeoutMS
//                   << " " << RequestDebugSummary(*joinRequest, SC2APIProtocol::Response::ResponseCase::kJoinGame)
//                   << std::endl;
//     bridgeConnection.Send(joinRequest.get());

//     SC2APIProtocol::Response* joinResponse{nullptr};
//     if (!bridgeConnection.Receive(joinResponse, kJoinGameResponseTimeoutMS))
//     {
//         PrintThread{} << "ERROR: [LAN] Human API bridge raw JoinGame timeout/closed for "
//                       << m_botConfig.BotName
//                       << " after " << kJoinGameResponseTimeoutMS
//                       << "ms. bridge_closed=" << BoolString(bridgeClosed.load())
//                       << " game_port=" << gamePort
//                       << " start_port=" << startPort
//                       << " opponent_id=" << opponentPlayerId << std::endl;
//         return;
//     }
//     if (joinResponse == nullptr)
//     {
//         PrintThread{} << "ERROR: [LAN] Human API bridge raw JoinGame returned null response for "
//                       << m_botConfig.BotName << std::endl;
//         return;
//     }
//     PrintThread{} << "[LAN] Human API bridge raw JoinGame response for " << m_botConfig.BotName
//                   << ". " << ResponseDebugSummary(*joinResponse) << std::endl;
//     if (!joinResponse->has_join_game() || joinResponse->error_size() > 0)
//     {
//         PrintThread{} << "ERROR: [LAN] Human API bridge raw JoinGame failed for "
//                       << m_botConfig.BotName
//                       << " has_join_game=" << BoolString(joinResponse->has_join_game())
//                       << " error_count=" << joinResponse->error_size() << std::endl;
//         return;
//     }

//     PrintThread{} << "[LAN] Human API bridge joined game for " << m_botConfig.BotName
//                   << " player_id=" << joinResponse->join_game().player_id() << std::endl;

//     while (!bridgeClosed.load())
//     {
//         sc2::GameRequestPtr observationRequest = proto.MakeRequest();
//         observationRequest->mutable_observation();
//         bridgeConnection.Send(observationRequest.get());

//         SC2APIProtocol::Response* observationResponse{nullptr};
//         if (!bridgeConnection.Receive(observationResponse, 10000U))
//         {
//             PrintThread{} << "[LAN] Human API bridge observation wait ended for "
//                           << m_botConfig.BotName
//                           << ". bridge_closed=" << BoolString(bridgeClosed.load()) << std::endl;
//             break;
//         }
//         if (observationResponse == nullptr)
//         {
//             PrintThread{} << "[LAN] Human API bridge observation returned null for "
//                           << m_botConfig.BotName << std::endl;
//             break;
//         }
//         if (observationResponse->error_size() > 0)
//         {
//             PrintThread{} << "ERROR: [LAN] Human API bridge observation response has "
//                           << observationResponse->error_size()
//                           << " error(s) for " << m_botConfig.BotName
//                           << ". " << ResponseDebugSummary(*observationResponse) << std::endl;
//             break;
//         }
//         if (observationResponse->has_observation()
//             && observationResponse->observation().player_result_size() > 0)
//         {
//             PrintThread{} << "[LAN] Human API bridge observed game result for "
//                           << m_botConfig.BotName
//                           << " player_result_count="
//                           << observationResponse->observation().player_result_size()
//                           << std::endl;
//             break;
//         }
//         if (observationResponse->has_status()
//             && observationResponse->status() == SC2APIProtocol::Status::ended)
//         {
//             PrintThread{} << "[LAN] Human API bridge observed ended status for "
//                           << m_botConfig.BotName << std::endl;
//             break;
//         }
//         sc2::SleepFor(1000);
//     }

//     bridgeConnection.Disconnect();
//     PrintThread{} << "[LAN] Human API bridge exiting for " << m_botConfig.BotName << std::endl;
// }

// bool LanProxy::StartBot(const int portServer, const int portStart, const std::string& opponentPlayerId)
// {
//     if (m_botConfig.Type == BotType::Human)
//     {
//         m_botProgramThread = std::async(std::launch::async, &LanProxy::RunHumanAgent, this, portServer, portStart, opponentPlayerId);
//         constexpr size_t maxStartUpTime = 10U;
//         for (auto waitedFor(0U); waitedFor < maxStartUpTime; ++waitedFor)
//         {
//             if (!m_server.connections_.empty())
//             {
//                 return true;
//             }
//             sc2::SleepFor(1000);
//         }
//         return false;
//     }

//     const std::string botStartCommand = GetBotCommandLine(portServer, portStart, opponentPlayerId);
//     if (botStartCommand == m_botConfig.executeCommand)
//     {
//         return false;
//     }
//     m_botProgramThread = std::async(std::launch::async, &StartBotProcess, m_botConfig, botStartCommand, &m_botThreadId);
//     if (m_botProgramThread.wait_for(std::chrono::seconds(2)) == std::future_status::ready)
//     {
//         return false;
//     }
//     constexpr size_t maxStartUpTime = 10U;
//     for (auto waitedFor(0U); waitedFor < maxStartUpTime; ++waitedFor)
//     {
//         if (!m_server.connections_.empty())
//         {
//             return true;
//         }
//         sc2::SleepFor(1000);
//     }
//     return false;
// }

// void LanProxy::StartGameLoop()
// {
//     m_gameUpdateThread = std::async(std::launch::async, &LanProxy::GameUpdate, this);
// }

// bool LanProxy::GameFinished() const
// {
//     return std::future_status::ready == m_gameUpdateThread.wait_for(std::chrono::seconds(0));
// }

// ExitCase LanProxy::GetResult() const
// {
//     return m_result;
// }

// const LanStats& LanProxy::Stats() const
// {
//     return m_stats;
// }

// std::string LanProxy::GetBotCommandLine(const int gamePort, const int startPort, const std::string& opponentID) const
// {
//     std::string returnCmd = m_botConfig.executeCommand
//         + " --GamePort " + std::to_string(gamePort)
//         + " --StartPort " + std::to_string(startPort)
//         + " --LadderServer " + m_localHost
//         + " --OpponentId " + opponentID;
//     if (m_realTimeMode)
//     {
//         returnCmd += " --RealTime";
//     }
//     return returnCmd;
// }

// bool LanProxy::CreateGameHasErrors(const SC2APIProtocol::ResponseCreateGame& createGameResponse) const
// {
//     bool hasError = false;
//     if (createGameResponse.has_error())
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : CreateGame request returned error code "
//                       << createGameResponse.error() << std::endl;
//         hasError = true;
//     }
//     if (createGameResponse.has_error_details() && createGameResponse.error_details().length() > 0)
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : CreateGame request returned error details: "
//                       << createGameResponse.error_details() << std::endl;
//         hasError = true;
//     }
//     return hasError;
// }

// bool LanProxy::IsBotFinished(const int milliseconds) const
// {
//     return m_botProgramThread.valid()
//         && m_botProgramThread.wait_for(std::chrono::milliseconds(milliseconds)) == std::future_status::ready;
// }

// void LanProxy::GameUpdate()
// {
//     PrintThread{} << "[LAN] Starting proxy for " << m_botConfig.BotName << std::endl;
//     const auto gameStartTime = clock::now();

//     while (m_gameStatus == SC2APIProtocol::Status::in_game
//         || m_gameStatus == SC2APIProtocol::Status::init_game
//         || m_gameStatus == SC2APIProtocol::Status::launched)
//     {
//         if (m_server.HasRequest())
//         {
//             const sc2::RequestData& request = m_server.PeekRequest();
//             if (!ProcessRequest(request))
//             {
//                 m_result = ExitCase::BotCrashed;
//                 continue;
//             }
//             const auto expectedResponseCase = static_cast<SC2APIProtocol::Response::ResponseCase>(request.second->request_case());
//             const unsigned int responseTimeoutMS =
//                 expectedResponseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame
//                     ? kJoinGameResponseTimeoutMS
//                     : m_responseTimeoutMS;
//             if (expectedResponseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame)
//             {
//                 PrintThread{} << "[LAN] " << m_botConfig.BotName
//                               << " : sending JoinGame request to SC2 websocket. "
//                               << "server_connections=" << m_server.connections_.size()
//                               << " client_connected=" << BoolString(m_client.connection_ != nullptr)
//                               << " timeout_ms=" << responseTimeoutMS << std::endl;
//             }
//             m_server.SendRequest(m_client.connection_);

//             SC2APIProtocol::Response* response = ReceiveResponse(expectedResponseCase, responseTimeoutMS);
//             if (!ProcessResponse(response))
//             {
//                 m_result = ExitCase::Error;
//                 break;
//             }
//             if (!m_server.connections_.empty() && m_client.connection_ != nullptr)
//             {
//                 m_server.QueueResponse(m_client.connection_, response);
//                 m_server.SendResponse();
//             }
//             else
//             {
//                 m_result = ExitCase::Error;
//                 break;
//             }
//         }
//         else
//         {
//             if (IsBotFinished(0))
//             {
//                 PrintThread{} << "[LAN] " << m_botConfig.BotName << " : bot thread ended." << std::endl;
//                 if (m_result == ExitCase::Unknown)
//                 {
//                     m_result = ExitCase::BotCrashed;
//                 }
//                 break;
//             }
//             if (m_server.connections_.empty() || m_client.connection_ == nullptr)
//             {
//                 if (IsBotFinished(1000))
//                 {
//                     PrintThread{} << "[LAN] " << m_botConfig.BotName << " : bot thread ended." << std::endl;
//                     if (m_result == ExitCase::Unknown)
//                     {
//                         m_result = ExitCase::BotCrashed;
//                     }
//                     break;
//                 }
//                 if (m_server.connections_.empty())
//                 {
//                     PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Receive: server->connections_.empty()" << std::endl;
//                     m_result = ExitCase::Error;
//                     break;
//                 }
//                 if (m_client.connection_ == nullptr)
//                 {
//                     PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Receive: m_client.connection_ == nullptr" << std::endl;
//                     m_result = ExitCase::Error;
//                     break;
//                 }
//             }
//         }

//         const auto gameDurationRealTime = std::chrono::duration_cast<std::chrono::seconds>(clock::now() - gameStartTime).count();
//         if (m_maxRealGameTime && gameDurationRealTime > m_maxRealGameTime)
//         {
//             m_result = ExitCase::GameTimeOver;
//             break;
//         }
//     }

//     if (m_result == ExitCase::Unknown)
//     {
//         m_result = ExitCase::Error;
//     }
//     if (m_currentGameLoop > 0U)
//     {
//         m_stats.avgLoopDuration = std::chrono::duration_cast<std::chrono::milliseconds>(m_totalTime).count()
//             / static_cast<float>(m_currentGameLoop);
//     }
//     m_stats.gameLoops = m_currentGameLoop;
//     PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Exiting with "
//                   << GetExitCaseString(m_result)
//                   << " Average step time " << m_stats.avgLoopDuration
//                   << " microseconds, total time: "
//                   << std::chrono::duration_cast<std::chrono::seconds>(m_totalTime).count()
//                   << " seconds, game loops: " << m_currentGameLoop << std::endl;
// }

// bool LanProxy::ProcessRequest(const sc2::RequestData& request)
// {
//     if (request.second)
//     {
//         const auto expectedResponseCase = static_cast<SC2APIProtocol::Response::ResponseCase>(request.second->request_case());
//         if (request.second->has_join_game())
//         {
//             auto* joinGame = request.second->mutable_join_game();
//             ApplyLanHostIp(joinGame, m_lanGameHostIp);
//             PrintThread{} << "[LAN] " << m_botConfig.BotName
//                           << " : forwarding JoinGame request to SC2 client. "
//                           << RequestDebugSummary(*request.second, expectedResponseCase)
//                           << " server_connections=" << m_server.connections_.size()
//                           << " client_connected=" << BoolString(m_client.connection_ != nullptr)
//                           << " timeout_ms=" << kJoinGameResponseTimeoutMS
//                           << std::endl;
//         }
//         if (request.second->has_quit())
//         {
//             PrintThread{} << "[LAN] " << m_botConfig.BotName << " issued a quit request." << std::endl;
//             return false;
//         }
//         if (request.second->has_leave_game())
//         {
//             PrintThread{} << "[LAN] " << m_botConfig.BotName << " issued a leave game request." << std::endl;
//         }
//         else if (request.second->has_debug() && !m_usedDebugInterface)
//         {
//             PrintThread{} << "[LAN] " << m_botConfig.BotName << " : IS USING DEBUG INTERFACE." << std::endl;
//             m_usedDebugInterface = true;
//         }
//         else if (request.second->has_step() && m_currentGameLoop)
//         {
//             m_totalTime += clock::now() - m_lastResponseSendTime;
//         }
//     }
//     return true;
// }

// bool LanProxy::ProcessResponse(SC2APIProtocol::Response* const response)
// {
//     if (response == nullptr)
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Waiting for a response had a timeout or was invalid." << std::endl;
//         return false;
//     }
//     if (response->has_join_game())
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : JoinGame response received player_id="
//                       << response->join_game().player_id() << std::endl;
//     }
//     if (response->has_observation())
//     {
//         const auto& observation = response->observation().observation();
//         m_currentGameLoop = observation.game_loop();
//         if (response->observation().player_result_size() > 0)
//         {
//             const auto result = response->observation().player_result(0).result();
//             switch (result)
//             {
//             case SC2APIProtocol::Result::Victory:
//                 m_result = ExitCase::GameEndVictory;
//                 break;
//             case SC2APIProtocol::Result::Defeat:
//                 m_result = ExitCase::GameEndDefeat;
//                 break;
//             case SC2APIProtocol::Result::Tie:
//                 m_result = ExitCase::GameEndTie;
//                 break;
//             default:
//                 break;
//             }
//         }
//     }
//     if (response->has_step())
//     {
//         m_lastResponseSendTime = clock::now();
//     }
//     if (response->has_status() && (response->has_observation() || m_gameStatus != SC2APIProtocol::Status::in_game))
//     {
//         UpdateStatus(response->status());
//     }
//     return true;
// }

// void LanProxy::UpdateStatus(const SC2APIProtocol::Status newStatus)
// {
//     if (newStatus != m_gameStatus
//         && (m_gameStatus == SC2APIProtocol::Status::unknown
//             || static_cast<int>(newStatus) > static_cast<int>(m_gameStatus)))
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : Client changed status from " << statusToString(m_gameStatus)
//                       << " to " << statusToString(newStatus) << std::endl;
//         m_gameStatus = newStatus;
//     }
// }

// bool LanProxy::SaveReplay(const std::string& replayFile)
// {
//     if (m_result == ExitCase::Error)
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : Match ended in error. Can not save replay." << std::endl;
//         return false;
//     }
//     sc2::ProtoInterface proto;
//     sc2::GameRequestPtr request = proto.MakeRequest();
//     request->mutable_save_replay();
//     m_client.Send(request.get());
//     auto* response = ReceiveResponse(SC2APIProtocol::Response::ResponseCase::kSaveReplay);
//     if (!response || !response->has_save_replay())
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : Failed to receive replay response." << std::endl;
//         return false;
//     }
//     const auto& replay = response->save_replay();
//     if (replay.data().empty())
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName << " : Replay data empty." << std::endl;
//         return false;
//     }
//     std::ofstream file;
//     file.open(replayFile, std::fstream::binary);
//     if (!file.is_open())
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : Could not open replay file: " << replayFile << std::endl;
//         return false;
//     }
//     file.write(&replay.data()[0], static_cast<std::streamsize>(replay.data().size()));
//     return true;
// }

// SC2APIProtocol::Response* LanProxy::ReceiveResponse(
//     const SC2APIProtocol::Response::ResponseCase responseCase,
//     const unsigned int timeoutMS)
// {
//     SC2APIProtocol::Response* response{nullptr};
//     if (!m_client.Receive(response, timeoutMS))
//     {
//         PrintThread{} << "ERROR: [LAN] " << m_botConfig.BotName
//                       << " : timeout/closed/error waiting for "
//                       << responseCaseToString(responseCase)
//                       << " after " << timeoutMS << "ms."
//                       << " client_connected=" << BoolString(m_client.connection_ != nullptr)
//                       << " server_connections=" << m_server.connections_.size()
//                       << std::endl;
//         return nullptr;
//     }
//     if (responseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame
//         || response->response_case() == SC2APIProtocol::Response::ResponseCase::kJoinGame)
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : received JoinGame response from SC2 websocket. "
//                       << ResponseDebugSummary(*response) << std::endl;
//     }
//     bool hasErrors = false;
//     if (responseCase != response->response_case())
//     {
//         PrintThread{} << "[LAN] " << m_botConfig.BotName
//                       << " : expected " << responseCaseToString(responseCase)
//                       << " but got " << responseCaseToString(response->response_case()) << std::endl;
//         hasErrors = true;
//     }
//     if (response->error_size())
//     {
//         std::ostringstream ss;
//         ss << "[LAN] " << m_botConfig.BotName << " : response "
//            << responseCaseToString(response->response_case())
//            << " has " << response->error_size() << " error(s)!" << std::endl;
//         for (int i = 0; i < response->error_size(); ++i)
//         {
//             ss << "\t \t \t * " << response->error(i) << std::endl;
//         }
//         PrintThread{} << ss.str();
//         hasErrors = true;
//     }
//     if (hasErrors)
//     {
//         UpdateStatus(SC2APIProtocol::Status::ended);
//     }
//     if (response->has_status() && (response->has_observation() || m_gameStatus != SC2APIProtocol::Status::in_game))
//     {
//         UpdateStatus(response->status());
//     }
//     return response;
// }
