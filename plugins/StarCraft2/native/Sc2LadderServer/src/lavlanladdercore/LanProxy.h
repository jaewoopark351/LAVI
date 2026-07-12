// #20260712_kpopmodder: LAN Lobby native source commented out for maintenance safety.
// # Do not re-enable without an explicit LAN revive.
// //20260711_kpopmodder: LAN Lobby-only proxy path isolated from sc2laddercore::Proxy so Local Match stays untouched.
// #pragma once

// #include "AgentsConfig.h"

// #include <chrono>
// #include <future>
// #include <string>

// #include "s2clientprotocol/sc2api.pb.h"
// #include "sc2api/sc2_connection.h"
// #include "sc2api/sc2_game_settings.h"
// #include "sc2api/sc2_server.h"

// struct LanStats
// {
//     float avgLoopDuration{0.0f};
//     size_t gameLoops{0U};
// };

// class LanProxy
// {
//     sc2::Server m_server{};
//     sc2::Connection m_client{};
//     uint64_t m_gameClientPid{0UL};

//     const uint32_t m_maxGameLoops{0U};
//     const uint32_t m_maxRealGameTime{0U};
//     uint32_t m_currentGameLoop{0U};
//     SC2APIProtocol::Status m_gameStatus{SC2APIProtocol::Status::unknown};
//     std::future<void> m_gameUpdateThread{};
//     ExitCase m_result{ExitCase::Unknown};
//     bool m_realTimeMode{false};

//     const BotConfig m_botConfig{};
//     const std::string m_lanGameHostIp{};
//     std::future<void> m_botProgramThread{};
//     unsigned long m_botThreadId{0};
//     bool m_usedDebugInterface{false};

//     LanStats m_stats{};
//     using clock = std::chrono::system_clock;
//     clock::time_point m_lastResponseSendTime{};
//     clock::duration m_totalTime{std::chrono::seconds(0)};

//     static constexpr auto m_localHost{"127.0.0.1"};
//     static constexpr int m_responseTimeoutMS{100000};

//     std::string GetBotCommandLine(const int gamePort, const int startPort, const std::string& opponentID) const;
//     void RunHumanAgent(const int gamePort, const int startPort, const std::string opponentPlayerId);
//     bool IsBotFinished(const int milliseconds) const;
//     bool ProcessRequest(const sc2::RequestData& request);
//     bool ProcessResponse(SC2APIProtocol::Response* const response);
//     void GameUpdate();
//     void UpdateStatus(const SC2APIProtocol::Status newStatus);
//     bool CreateGameHasErrors(const SC2APIProtocol::ResponseCreateGame& createGameResponse) const;
//     SC2APIProtocol::Response* ReceiveResponse(SC2APIProtocol::Response::ResponseCase responseCase, unsigned int timeoutMS = m_responseTimeoutMS);

// public:
//     LanProxy() = delete;
//     ~LanProxy();
//     LanProxy(const uint32_t maxGameLoops, const uint32_t maxRealGameTime, const BotConfig& botConfig, const std::string& lanGameHostIp);

//     void StartProxyServer(const int portServer);
//     void StartSC2Instance(const sc2::ProcessSettings& processSettings, const int portServer, const int portClient);
//     bool ConnectToSC2Instance(const sc2::ProcessSettings& processSettings, const int portServer, const int portClient);
//     bool ConnectToSC2Client(const std::string& host, const int portClient);
//     bool ConnectToSC2ClientReady(const std::string& host, const int portClient, const int maxWaitSeconds, const unsigned int pingTimeoutMS);
//     bool SetupGameAsHost(const sc2::ProcessSettings& processSettings, const std::string& map, const bool realTimeMode, const sc2::Race player1Race, const sc2::Race player2Race);
//     bool StartBot(const int portServer, const int portStart, const std::string& opponentPlayerId);
//     void StartGameLoop();

//     bool GameFinished() const;
//     bool SaveReplay(const std::string& replayFile);
//     ExitCase GetResult() const;
//     const LanStats& Stats() const;
// };
