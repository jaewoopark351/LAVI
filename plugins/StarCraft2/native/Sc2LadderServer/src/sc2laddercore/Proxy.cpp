#include "Proxy.h"

#include <algorithm>
#include <atomic>
#include <cctype>
#include <condition_variable>
#include <exception>
#include <fstream>
#include <iomanip>
#include <mutex>
#include <sstream>
#include <map>
#include <thread>

#include "Tools.h"

#include "sc2utils/sc2_manage_process.h"


bool Proxy::m_mapAlreadyLoaded{false};

namespace
{
constexpr unsigned int kJoinGameResponseTimeoutMS = 30000U;
constexpr auto kLanGameHostIpArg{"--lan-game-host-ip"};

//20260709_kpopmodder: Human slots need an SC2 API client that joins but never issues commands.
class LavNoopHumanAgent : public sc2::Agent
{
};

std::string BoolString(const bool value)
{
    return value ? "true" : "false";
}

std::string HexPrefix(const std::string& bytes, const size_t maxBytes)
{
    std::ostringstream ss;
    ss << std::hex << std::setfill('0');
    const size_t count = std::min(maxBytes, bytes.size());
    for (size_t i = 0; i < count; ++i)
    {
        if (i)
        {
            ss << ' ';
        }
        ss << std::setw(2)
           << static_cast<unsigned int>(static_cast<unsigned char>(bytes[i]));
    }
    if (bytes.size() > count)
    {
        ss << " ...";
    }
    return ss.str();
}

std::string RequestDebugSummary(
    const SC2APIProtocol::Request& request,
    const SC2APIProtocol::Response::ResponseCase expectedResponseCase)
{
    std::ostringstream ss;
    const std::string payload = request.SerializeAsString();
    ss << "request_case=" << static_cast<int>(request.request_case())
       << " expected_response=" << responseCaseToString(expectedResponseCase)
       << " byte_size=" << request.ByteSize()
       << " has_join_game=" << BoolString(request.has_join_game())
       << " has_observation=" << BoolString(request.has_observation())
       << " has_step=" << BoolString(request.has_step())
       << " has_leave_game=" << BoolString(request.has_leave_game())
       << " payload_hex_prefix=" << HexPrefix(payload, 96U);
    if (request.has_join_game())
    {
        const SC2APIProtocol::RequestJoinGame& joinGame = request.join_game();
        ss << " join_host_ip="
           << (joinGame.has_host_ip() ? joinGame.host_ip() : "<unset>")
           << " join_shared_port="
           << (joinGame.has_shared_port() ? std::to_string(joinGame.shared_port()) : "<unset>");
        if (joinGame.has_server_ports())
        {
            ss << " join_server_ports="
               << joinGame.server_ports().game_port()
               << "/"
               << joinGame.server_ports().base_port();
        }
        else
        {
            ss << " join_server_ports=<unset>";
        }
        ss << " join_client_ports=";
        if (joinGame.client_ports_size() == 0)
        {
            ss << "<none>";
        }
        else
        {
            for (int i = 0; i < joinGame.client_ports_size(); ++i)
            {
                if (i)
                {
                    ss << ",";
                }
                ss << joinGame.client_ports(i).game_port()
                   << "/"
                   << joinGame.client_ports(i).base_port();
            }
        }
    }
    return ss.str();
}

std::string ResponseDebugSummary(const SC2APIProtocol::Response& response)
{
    std::ostringstream ss;
    const std::string payload = response.SerializeAsString();
    ss << "response_case=" << responseCaseToString(response.response_case())
       << " byte_size=" << response.ByteSize()
       << " error_count=" << response.error_size()
       << " has_join_game=" << BoolString(response.has_join_game())
       << " has_status=" << BoolString(response.has_status())
       << " payload_hex_prefix=" << HexPrefix(payload, 96U);
    if (response.has_status())
    {
        ss << " status=" << statusToString(response.status());
    }
    if (response.has_join_game())
    {
        ss << " join_player_id=" << response.join_game().player_id();
    }
    return ss.str();
}

std::string LowerString(std::string value)
{
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return value;
}

std::string BotArgValue(const std::string& args, const std::string& name, const std::string& fallback)
{
    const std::string prefix = name + "=";
    std::istringstream stream(args);
    std::string token;
    while (stream >> token)
    {
        if (token == name)
        {
            std::string value;
            if (stream >> value)
            {
                return value;
            }
            return fallback;
        }
        if (token.rfind(prefix, 0) == 0)
        {
            return token.substr(prefix.size());
        }
    }
    return fallback;
}

std::string LanGameHostIp(const BotConfig& botConfig)
{
    return BotArgValue(botConfig.Args, kLanGameHostIpArg, "");
}

bool ApplyLanJoinGameHostIp(SC2APIProtocol::Request& request, const BotConfig& botConfig)
{
    if (!request.has_join_game())
    {
        return false;
    }
    SC2APIProtocol::RequestJoinGame* joinGame = request.mutable_join_game();
    if (joinGame->has_host_ip() && !joinGame->host_ip().empty())
    {
        return false;
    }
    const std::string hostIp = LanGameHostIp(botConfig);
    if (hostIp.empty())
    {
        return false;
    }
    //20260711_kpopmodder: Only LAN Lobby injects this internal arg. Local Match
    // keeps the default SC2 API localhost behavior.
    joinGame->set_host_ip(hostIp);
    return true;
}

bool UseRemoteHumanClientJoinPortRole(const BotConfig& botConfig)
{
    //20260711_kpopmodder: In LAN Lobby, the remote human is the second SC2
    // participant. Its raw JoinGame request must advertise the opposite port
    // set from the local bot: server=start+4/+5, client=start+2/+3.
    const std::string role = LowerString(BotArgValue(
        botConfig.Args,
        "--remote-human-join-port-role",
        "server"));
    return role == "client" || role == "second" || role == "flipped" || role == "inverse";
}
}



Proxy::Proxy(const uint32_t maxGameLoops, const uint32_t maxRealGameTime, const BotConfig& botConfig):
    m_maxGameLoops(maxGameLoops)
  , m_maxRealGameTime(maxRealGameTime)
  , m_botConfig(botConfig)
{
    m_lastResponseSendTime = clock::now();

}

Proxy::~Proxy()
{
    // Set it back to false for the match after this one.
    m_mapAlreadyLoaded = false;

    // Check if the bot is still running.
    const auto start = clock::now();
    std::chrono::duration<double> elapsedSeconds{0};
    std::future_status botProgStatus{std::future_status::deferred};
    // toDo: add to config?
    constexpr auto maxWaitTime{20};
    if (m_botProgramThread.valid())
    {
        while (elapsedSeconds.count() < maxWaitTime)
        {
            botProgStatus = m_botProgramThread.wait_for(std::chrono::seconds(1));
            if (botProgStatus == std::future_status::ready)
            {
                PrintThread{} << m_botConfig.BotName << " : Bot terminated properly." << std::endl;
                break;
            }
            elapsedSeconds = clock::now() - start;
        }
        if (botProgStatus != std::future_status::ready)
        {
            PrintThread{} << m_botConfig.BotName << " : Bot is still running after " << maxWaitTime << " seconds. Sending kill signal." << std::endl;
            if (m_botThreadId != 0)
            {
                KillBotProcess(m_botThreadId);
            }
        }
        sc2::SleepFor(5000);
    }
    if (m_gameClientPid)
    {
        if (!sc2::TerminateProcess(m_gameClientPid))
        {
            PrintThread{} << m_botConfig.BotName << " : Terminating SC2 failed!" << std::endl;
        }
        sc2::SleepFor(5000);
    }
}
void Proxy::startSC2Instance(const sc2::ProcessSettings& processSettings, const int portServer, const int portClient)
{
    startProxyServer(portServer);

    m_gameClientPid = sc2::StartProcess(processSettings.process_path,
        { "-listen", m_localHost,
          "-port", std::to_string(portClient),
          "-displayMode", "0",
          "-dataVersion", processSettings.data_version });
}

bool Proxy::ConnectToSC2Instance(const sc2::ProcessSettings& processSettings, const int portServer, const int portClient)
{
    return ConnectToSC2Client(m_localHost, portClient);
}

void Proxy::startProxyServer(const int portServer)
{
    // magic numbers
    m_server.Listen(std::to_string(portServer).c_str(), "100000", "100000", "5");
}

bool Proxy::ConnectToSC2Client(const std::string& host, const int portClient)
{
    // Depending on the hardware the client sometimes needs a second or two.
    size_t connectionAttempts = 0;
    constexpr size_t abandonConnectionAttemptAfter = 60;  // sec
    constexpr bool withDebugOutput = false;
    const std::string connectHost = host.empty() ? m_localHost : host;
    PrintThread{} << "Connecting proxy for " << m_botConfig.BotName << " to SC2 client "
                  << connectHost << ":" << portClient << std::endl;
    while (!m_client.Connect(connectHost, portClient, withDebugOutput))
    {
        ++connectionAttempts;
        sc2::SleepFor(1000);
        if (connectionAttempts > abandonConnectionAttemptAfter)
        {
            PrintThread{} << "Failed to connect to client (" << m_botConfig.BotName << ") at "
                          << connectHost << ":" << portClient << std::endl;
            return false;
        }
    }

    // Check if client is reacting
    sc2::ProtoInterface proto;
    sc2::GameRequestPtr request = proto.MakeRequest();
    request->mutable_ping();
    m_client.Send(request.get());
    auto* response = receiveResponse(SC2APIProtocol::Response::ResponseCase::kPing);
    return response;
}

bool Proxy::ConnectToSC2ClientReady(const std::string& host, const int portClient, const int maxWaitSeconds, const unsigned int pingTimeoutMS)
{
    //20260711_kpopmodder: LAN remote-human clients can open the TCP port before
    // the SC2 API websocket is ready to answer. Keep this retry path separate
    // from the Local Match launcher so local startup semantics stay unchanged.
    constexpr bool withDebugOutput = false;
    const std::string connectHost = host.empty() ? m_localHost : host;
    const int waitSeconds = maxWaitSeconds > 0 ? maxWaitSeconds : 120;
    const unsigned int pingTimeout = pingTimeoutMS > 0U ? pingTimeoutMS : 2000U;
    const auto start = clock::now();
    size_t attempt = 0U;

    PrintThread{} << "[LAN] Waiting for SC2 API for " << m_botConfig.BotName
                  << " at " << connectHost << ":" << portClient
                  << " max_wait_sec=" << waitSeconds
                  << " ping_timeout_ms=" << pingTimeout << std::endl;

    while (std::chrono::duration_cast<std::chrono::seconds>(clock::now() - start).count() <= waitSeconds)
    {
        ++attempt;
        if (!m_client.HasConnection())
        {
            PrintThread{} << "[LAN] TCP connect attempt " << attempt << " for "
                          << m_botConfig.BotName << " to SC2 client "
                          << connectHost << ":" << portClient << std::endl;
            if (!m_client.Connect(connectHost, portClient, withDebugOutput))
            {
                sc2::SleepFor(1000);
                continue;
            }
            PrintThread{} << "[LAN] TCP connected for " << m_botConfig.BotName
                          << " to SC2 client " << connectHost << ":" << portClient << std::endl;
        }

        sc2::ProtoInterface proto;
        sc2::GameRequestPtr request = proto.MakeRequest();
        request->mutable_ping();
        m_client.Send(request.get());
        SC2APIProtocol::Response* response = receiveResponse(SC2APIProtocol::Response::ResponseCase::kPing, pingTimeout);
        if (response && response->response_case() == SC2APIProtocol::Response::ResponseCase::kPing)
        {
            PrintThread{} << "[LAN] SC2 API ping ok for " << m_botConfig.BotName
                          << " at " << connectHost << ":" << portClient << std::endl;
            return true;
        }

        PrintThread{} << "[LAN] SC2 API ping not ready for " << m_botConfig.BotName
                      << " at " << connectHost << ":" << portClient
                      << " attempt=" << attempt << "; reconnecting." << std::endl;
        if (m_client.HasConnection())
        {
            m_client.Disconnect();
        }
        sc2::SleepFor(1000);
    }

    PrintThread{} << "[LAN] Failed to get SC2 API ping for " << m_botConfig.BotName
                  << " at " << connectHost << ":" << portClient
                  << " within " << waitSeconds << " seconds." << std::endl;
    return false;
}

// Technically, we only need opponents race. But I think it looks clearer on the caller side with both races.
bool Proxy::setupGame(const sc2::ProcessSettings& processSettings, const std::string& map, const bool realTimeMode, const sc2::Race bot1Race, const sc2::Race bot2Race)
{
    m_realTimeMode = realTimeMode;
    // Only one client needs to / is allowed to send the create game request.
    if (m_mapAlreadyLoaded)
    {
        return true;
    }
    sc2::ProtoInterface proto;
    sc2::GameRequestPtr request = proto.MakeRequest();

    SC2APIProtocol::RequestCreateGame* requestCreateGame = request->mutable_create_game();

    // Player 1
    SC2APIProtocol::PlayerSetup* playerSetup = requestCreateGame->add_player_setup();
    playerSetup->set_type(SC2APIProtocol::PlayerType::Participant);
    playerSetup->set_race(SC2APIProtocol::Race(static_cast<int>(bot1Race) + 1));  // Ugh
    playerSetup->set_difficulty(SC2APIProtocol::Difficulty::VeryEasy);

    // Player 2
    playerSetup = requestCreateGame->add_player_setup();
    playerSetup->set_type(SC2APIProtocol::PlayerType::Participant);
    playerSetup->set_race(SC2APIProtocol::Race(static_cast<int>(bot2Race) + 1));
    playerSetup->set_difficulty(SC2APIProtocol::Difficulty::VeryEasy);

    // Map
    // BattleNet map
    if (!sc2::HasExtension(map, ".SC2Map"))
    {
        requestCreateGame->set_battlenet_map_name(map);
    }
    else
    {
        // Local map file
        SC2APIProtocol::LocalMap* const localMap = requestCreateGame->mutable_local_map();
        // Absolute path
        if (sc2::DoesFileExist(map))
        {
            localMap->set_map_path(map);
        }
        else
        {
            // Relative path - Game maps directory
            const std::string gameRelative = sc2::GetGameMapsDirectory(processSettings.process_path) + map;
            if (sc2::DoesFileExist(gameRelative))
            {
                localMap->set_map_path(map);
            }
            else
            {
                // Relative path - Library maps directory
                const std::string libraryRelative = sc2::GetLibraryMapsDirectory() + map;
                if (sc2::DoesFileExist(libraryRelative))
                {
                    localMap->set_map_path(libraryRelative);
                }
                else
                {
                    return false;
                }
            }
        }
    }

    // Real time mode
    requestCreateGame->set_realtime(realTimeMode);

    // Send the request
    m_client.Send(request.get());
    SC2APIProtocol::Response* createGameResponse = receiveResponse(SC2APIProtocol::Response::ResponseCase::kCreateGame);

    // Check if the request was successful
    if (!createGameResponse || createGameHasErrors(createGameResponse->create_game()))
    {
        return false;
    }
    m_mapAlreadyLoaded = true;
    return true;
}

void Proxy::runHumanAgent(const int gamePort, const int startPort, const std::string opponentPlayerId)
{
    //20260711_kpopmodder: LAN remote-human uses a lightweight raw SC2 API
    // client instead of sc2::Coordinator. Coordinator owns/validates a local
    // SC2 process id, which is unsafe here because the real human SC2 process
    // lives on the HumanJoiner PC.
    sc2::Connection bridgeConnection;
    std::atomic_bool bridgeClosed{false};
    bridgeConnection.SetConnectionClosedCallback([&bridgeClosed]() {
        bridgeClosed.store(true);
    });

    PrintThread{} << "Starting raw human API bridge for " << m_botConfig.BotName
                  << " on proxy port " << gamePort << std::endl;
    if (!bridgeConnection.Connect(m_localHost, gamePort))
    {
        PrintThread{} << "ERROR: Human API bridge could not connect to local proxy for "
                      << m_botConfig.BotName
                      << " at " << m_localHost << ":" << gamePort << std::endl;
        return;
    }

    sc2::ProtoInterface proto;
    sc2::GameRequestPtr joinRequest = proto.MakeRequest();
    SC2APIProtocol::RequestJoinGame* requestJoinGame = joinRequest->mutable_join_game();
    requestJoinGame->set_race(SC2APIProtocol::Race(static_cast<int>(m_botConfig.Race) + 1));

    // Mirrors sc2::Coordinator::SetupPorts(2, startPort, false). The first
    // participant uses server=start+2/+3 and client=start+4/+5. The second
    // participant must use the inverse role.
    const int sharedPort = startPort + 1;
    const bool useClientJoinPortRole = UseRemoteHumanClientJoinPortRole(m_botConfig);
    const int firstServerGamePort = startPort + 2;
    const int firstServerBasePort = startPort + 3;
    const int secondServerGamePort = startPort + 4;
    const int secondServerBasePort = startPort + 5;
    const int serverGamePort = useClientJoinPortRole ? secondServerGamePort : firstServerGamePort;
    const int serverBasePort = useClientJoinPortRole ? secondServerBasePort : firstServerBasePort;
    const int clientGamePort = useClientJoinPortRole ? firstServerGamePort : secondServerGamePort;
    const int clientBasePort = useClientJoinPortRole ? firstServerBasePort : secondServerBasePort;
    requestJoinGame->set_shared_port(sharedPort);
    SC2APIProtocol::PortSet* serverPorts = requestJoinGame->mutable_server_ports();
    serverPorts->set_game_port(serverGamePort);
    serverPorts->set_base_port(serverBasePort);
    SC2APIProtocol::PortSet* clientPorts = requestJoinGame->add_client_ports();
    clientPorts->set_game_port(clientGamePort);
    clientPorts->set_base_port(clientBasePort);
    const bool appliedLanHostIp = ApplyLanJoinGameHostIp(*joinRequest, m_botConfig);
    const std::string lanGameHostIp = LanGameHostIp(m_botConfig);

    SC2APIProtocol::InterfaceOptions* options = requestJoinGame->mutable_options();
    options->set_raw(true);
    options->set_score(true);

    PrintThread{} << "Human API bridge sending raw JoinGame for " << m_botConfig.BotName
                  << " context: game_port=" << gamePort
                  << " start_port=" << startPort
                  << " shared_port=" << sharedPort
                  << " join_port_role=" << (useClientJoinPortRole ? "client" : "server")
                  << " server_ports=" << serverGamePort << "/" << serverBasePort
                  << " client_ports=" << clientGamePort << "/" << clientBasePort
                  << " lan_game_host_ip=" << (lanGameHostIp.empty() ? "<none>" : lanGameHostIp)
                  << " host_ip_applied=" << BoolString(appliedLanHostIp)
                  << " opponent_id=" << opponentPlayerId
                  << " race=" << GetRaceString(m_botConfig.Race)
                  << " timeout_ms=" << kJoinGameResponseTimeoutMS
                  << " " << RequestDebugSummary(*joinRequest, SC2APIProtocol::Response::ResponseCase::kJoinGame)
                  << std::endl;
    bridgeConnection.Send(joinRequest.get());

    SC2APIProtocol::Response* joinResponse{nullptr};
    if (!bridgeConnection.Receive(joinResponse, kJoinGameResponseTimeoutMS))
    {
        PrintThread{} << "ERROR: Human API bridge raw JoinGame timeout/closed for "
                      << m_botConfig.BotName
                      << " after " << kJoinGameResponseTimeoutMS
                      << "ms. bridge_closed=" << BoolString(bridgeClosed.load())
                      << " game_port=" << gamePort
                      << " start_port=" << startPort
                      << " join_port_role=" << (useClientJoinPortRole ? "client" : "server")
                      << " opponent_id=" << opponentPlayerId << std::endl;
        return;
    }
    if (joinResponse == nullptr)
    {
        PrintThread{} << "ERROR: Human API bridge raw JoinGame returned null response for "
                      << m_botConfig.BotName << std::endl;
        return;
    }
    PrintThread{} << "Human API bridge raw JoinGame response for " << m_botConfig.BotName
                  << ". " << ResponseDebugSummary(*joinResponse) << std::endl;
    if (!joinResponse->has_join_game() || joinResponse->error_size() > 0)
    {
        PrintThread{} << "ERROR: Human API bridge raw JoinGame failed for "
                      << m_botConfig.BotName
                      << " has_join_game=" << BoolString(joinResponse->has_join_game())
                      << " error_count=" << joinResponse->error_size() << std::endl;
        return;
    }

    PrintThread{} << "Human API bridge joined game for " << m_botConfig.BotName
                  << " player_id=" << joinResponse->join_game().player_id() << std::endl;

    uint32_t lastLoggedObservationLoop = 0U;
    bool hasLoggedObservation = false;
    while (!bridgeClosed.load())
    {
        sc2::GameRequestPtr observationRequest = proto.MakeRequest();
        observationRequest->mutable_observation();
        bridgeConnection.Send(observationRequest.get());

        SC2APIProtocol::Response* observationResponse{nullptr};
        if (!bridgeConnection.Receive(observationResponse, 10000U))
        {
            PrintThread{} << "Human API bridge observation wait ended for "
                          << m_botConfig.BotName
                          << ". bridge_closed=" << BoolString(bridgeClosed.load()) << std::endl;
            break;
        }
        if (observationResponse == nullptr)
        {
            PrintThread{} << "Human API bridge observation returned null for "
                          << m_botConfig.BotName << std::endl;
            break;
        }
        if (observationResponse->error_size() > 0)
        {
            PrintThread{} << "ERROR: Human API bridge observation response has "
                          << observationResponse->error_size()
                          << " error(s) for " << m_botConfig.BotName
                          << ". " << ResponseDebugSummary(*observationResponse) << std::endl;
            break;
        }
        if (observationResponse->has_observation())
        {
            const auto& observation = observationResponse->observation().observation();
            const uint32_t gameLoop = observation.game_loop();
            if (!hasLoggedObservation || gameLoop - lastLoggedObservationLoop >= 224U)
            {
                PrintThread{} << "Human API bridge observation ok for "
                              << m_botConfig.BotName
                              << " game_loop=" << gameLoop
                              << " response_case=" << responseCaseToString(observationResponse->response_case())
                              << std::endl;
                lastLoggedObservationLoop = gameLoop;
                hasLoggedObservation = true;
            }
            if (observationResponse->observation().player_result_size() > 0)
            {
                PrintThread{} << "Human API bridge observed game result for "
                              << m_botConfig.BotName
                              << " player_result_count="
                              << observationResponse->observation().player_result_size()
                              << std::endl;
                break;
            }
        }
        if (observationResponse->has_status()
            && observationResponse->status() == SC2APIProtocol::Status::ended)
        {
            PrintThread{} << "Human API bridge observed ended status for "
                          << m_botConfig.BotName << std::endl;
            break;
        }
        sc2::SleepFor(1000);
    }

    bridgeConnection.Disconnect();
    PrintThread{} << "Human API bridge exiting for " << m_botConfig.BotName << std::endl;
}
bool Proxy::startBot(const int portServer, const int portStart, const std::string & opponentPlayerId)
{
    if (m_botConfig.Type == BotType::Human)
    {
        m_botProgramThread = std::async(std::launch::async, &Proxy::runHumanAgent, this, portServer, portStart, opponentPlayerId);
        constexpr size_t maxStartUpTime = 10U;
        for (auto waitedFor(0U); waitedFor < maxStartUpTime; ++waitedFor)
        {
            if (!m_server.connections_.empty())
            {
                return true;
            }
            sc2::SleepFor(1000);
        }
        return false;
    }
    const std::string botStartCommand = getBotCommandLine(portServer, portStart, opponentPlayerId);
    if (botStartCommand == m_botConfig.executeCommand)
    {
        return false;
    }
    m_botProgramThread = std::async(std::launch::async, &StartBotProcess, m_botConfig, botStartCommand, &m_botThreadId);
    if (m_botProgramThread.wait_for(std::chrono::seconds(2)) == std::future_status::ready)
    {
        return false;
    }
    constexpr size_t maxStartUpTime = 10U; // The bot gets 10 seconds to connect to the proxy. This is NOT the first game loop time.
    for (auto waitedFor(0U); waitedFor < maxStartUpTime; ++waitedFor)
    {
        if (!m_server.connections_.empty())
        {
            return true;
        }
        sc2::SleepFor(1000);
    }
    return false;
}

void Proxy::startGame()
{
    m_gameUpdateThread = std::async(std::launch::async, &Proxy::gameUpdate, this);
}

bool Proxy::gameFinished() const
{
    return std::future_status::ready == m_gameUpdateThread.wait_for(std::chrono::seconds(0));
}

ExitCase Proxy::getResult() const
{
    return m_result;
}

bool Proxy::createGameHasErrors(const SC2APIProtocol::ResponseCreateGame& createGameResponse) const
{
    bool hasError = false;
    if (createGameResponse.has_error())
    {
        std::string errorCode = "Unknown";
        switch (createGameResponse.error())
        {
        case SC2APIProtocol::ResponseCreateGame::MissingMap:
        {
            errorCode = "Missing Map";
            break;
        }
        case SC2APIProtocol::ResponseCreateGame::InvalidMapPath:
        {
            errorCode = "Invalid Map Path";
            break;
        }
        case SC2APIProtocol::ResponseCreateGame::InvalidMapData:
        {
            errorCode = "Invalid Map Data";
            break;
        }
        case SC2APIProtocol::ResponseCreateGame::InvalidMapName:
        {
            errorCode = "Invalid Map Name";
            break;
        }
        case SC2APIProtocol::ResponseCreateGame::InvalidMapHandle:
        {
            errorCode = "Invalid Map Handle";
            break;
        }
        case SC2APIProtocol::ResponseCreateGame::MissingPlayerSetup:
        {
            errorCode = "Missing Player Setup";
            break;
        }
        case SC2APIProtocol::ResponseCreateGame::InvalidPlayerSetup:
        {
            errorCode = "Invalid Player Setup";
            break;
        }
        default:
        {
            break;
        }
        }

        PrintThread{} << m_botConfig.BotName << " : CreateGame request returned an error code: " << errorCode << " (" << createGameResponse.error() << ")" << std::endl;
        hasError = true;
    }
    if (createGameResponse.has_error_details() && createGameResponse.error_details().length() > 0)
    {
        PrintThread{} << m_botConfig.BotName << " : CreateGame request returned error details: " << createGameResponse.error_details() << std::endl;
        hasError = true;
    }
    return hasError;
}

std::string Proxy::getBotCommandLine(const int gamePort, const int startPort, const std::string& opponentID) const
{
    // Add universal arguments
    std::string ReturnCmd = m_botConfig.executeCommand + " --GamePort " + std::to_string(gamePort) + " --StartPort " + std::to_string(startPort) + " --LadderServer " + m_localHost + " --OpponentId " + opponentID;
    if (m_realTimeMode)
    {
        ReturnCmd += " --RealTime";
    }
    return ReturnCmd;

}



void Proxy::gameUpdate()
{
    PrintThread{} << "Starting proxy for " << m_botConfig.BotName << std::endl;
    // toDo: somehow check if the other functions were already used.


    // Actually, the game still loads...
    const auto gameStartTime = clock::now();
    // The bot has 1 minute + time out time to send the first request.
    bool alreadySurrendered = false;

    while (m_gameStatus == SC2APIProtocol::Status::in_game || m_gameStatus == SC2APIProtocol::Status::init_game || m_gameStatus == SC2APIProtocol::Status::launched)
    {
        // If we know that the bot crashed we surrender for it.
        if (m_result == ExitCase::BotCrashed || m_result == ExitCase::BotStepTimeout || m_result == ExitCase::GameTimeOver)
        {
            // The bot is dead. So we will surrender on its behalf
            if (!alreadySurrendered)
            {
                terminateGame();
                alreadySurrendered = true;
                continue;
            }
            // and step the simulation until the match has officially ended.
            if (m_result == ExitCase::BotCrashed || m_result == ExitCase::BotStepTimeout)
            {
                doAStep();
                continue;
            }
        }

        if (m_server.HasRequest())
        {
            const sc2::RequestData& request = m_server.PeekRequest();
            // Analyse request
            // Returns false if a quit request was made.
            const bool validRequest = processRequest(request);
            // A quit request is handled as if the bot crashed.
            // Especially, we do not want to forward the request to the client.
            // We still need it for the replay.
            if (!validRequest)
            {
                m_result = ExitCase::BotCrashed;
                continue;
            }
            // Forward the valid request
            // The cast puts a lot of trust in Blizzard
            const auto expectedResponseCase = static_cast<SC2APIProtocol::Response::ResponseCase>(request.second->request_case());
            const unsigned int responseTimeoutMS =
                    expectedResponseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame
                    ? kJoinGameResponseTimeoutMS
                    : m_responseTimeOutMS;
            if (expectedResponseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame)
            {
                PrintThread{} << m_botConfig.BotName
                              << " : sending JoinGame request to remote SC2 websocket. "
                              << "server_connections=" << m_server.connections_.size()
                              << " client_connected=" << BoolString(m_client.connection_ != nullptr)
                              << " timeout_ms=" << responseTimeoutMS
                              << std::endl;
            }
            m_server.SendRequest(m_client.connection_);

            // Block for sc2's response then queue it.
            SC2APIProtocol::Response* response = receiveResponse(expectedResponseCase, responseTimeoutMS);
            const bool validResponse = processResponse(response);

            if (!validResponse)
            {
                m_result = ExitCase::Error;
                break;
            }
            // Send the response back to the client.
            if (!m_server.connections_.empty() && m_client.connection_ != nullptr)
            {
                m_server.QueueResponse(m_client.connection_, response);
                m_server.SendResponse();
            }
            else
            {
                // This usually happens if the bot crashed.
                // Check if the bot thread has send the crashed signal aka ready signal.
                if (isBotCrashed(1000))
                {
                    PrintThread{} << m_botConfig.BotName << " : crashed." << std::endl;
                    m_result = ExitCase::BotCrashed;
                    continue;
                }
                // Maybe it is the client ?
                if (isClientCrashed(1000))
                {
                    PrintThread{} << m_botConfig.BotName << " : crashed." << std::endl;
                }
                // toDo: Are there other cases when this happens?
                if (m_server.connections_.empty())
                {
                    PrintThread{} << m_botConfig.BotName << " : Response: m_server.connections_.empty()" << std::endl;
                }
                else
                {
                    PrintThread{} << m_botConfig.BotName << " : Response: m_client.connection_ == nullptr" << std::endl;
                }
                m_result = ExitCase::Error;
                break;
            }
        }
        else
        {
            const uint32_t maxStepTime = getMaxStepTime();  // ms
            const auto timeSinceLastResponse = std::chrono::duration_cast<std::chrono::milliseconds>(clock::now() - m_lastResponseSendTime).count();
            if ( !m_realTimeMode && maxStepTime && timeSinceLastResponse > static_cast<int>(maxStepTime))
            {
                PrintThread{} << m_botConfig.BotName << " : bot is too slow. " << timeSinceLastResponse << " milliseconds passed. Max step time: " << static_cast<int>(maxStepTime) << " milliseconds." << std::endl;
                // ToDo: Make a chat announcement
                // ToDo: Can we handle this better. It
                m_result = ExitCase::BotStepTimeout;
            }

            // Check if the bot thread has send the crashed signal.
            // This is a fast check to not slow down the game
            if (isBotCrashed(0))
            {
                PrintThread{} << m_botConfig.BotName << " : crashed." << std::endl;
                m_result = ExitCase::BotCrashed;
                continue;
            }
            if (m_server.connections_.empty() || m_client.connection_ == nullptr)
            {
                // Time for a serious check if the bot crashed.
                if (isBotCrashed(1000))
                {
                    PrintThread{} << m_botConfig.BotName << " : crashed." << std::endl;
                    m_result = ExitCase::BotCrashed;
                    continue;
                }
                // Maybe it is the client ?
                if (isClientCrashed(1000))
                {
                    PrintThread{} << m_botConfig.BotName << " : crashed." << std::endl;
                }
                if (m_server.connections_.empty())
                {
                    PrintThread{} << m_botConfig.BotName << " : Receive: server->connections_.empty()" << std::endl;
                    m_result = ExitCase::Error;
                    break;
                }

                // If there is no connection to the client it probably crashed.
                if (m_client.connection_ == nullptr)
                {
                    PrintThread{} << m_botConfig.BotName << " :  Receive: m_client.connection_ == nullptr" << std::endl;
                    m_result = ExitCase::Error;
                    break;
                }
            }
        }
        const auto gameDurationRealTime = std::chrono::duration_cast<std::chrono::seconds>(clock::now() - gameStartTime).count();
        if (m_maxRealGameTime && gameDurationRealTime > m_maxRealGameTime)
        {
            m_result = ExitCase::GameTimeOver;
        }
    }

    if (m_result == ExitCase::Unknown)
    {
        // The game ended normally for this bot. Get the result from the observation.
        const SC2APIProtocol::Result result = getGameResult();
        switch (result)
        {
        case SC2APIProtocol::Result::Victory:
        {
            m_result = ExitCase::GameEndVictory;
            break;
        }
        case SC2APIProtocol::Result::Defeat:
        {
            m_result = ExitCase::GameEndDefeat;
            break;
        }
        case SC2APIProtocol::Result::Tie:
        {
            m_result = ExitCase::GameEndTie;
            break;
        }
        case SC2APIProtocol::Result::Undecided:
        default:
        {
            m_result = ExitCase::Error;
            break;
        }
        }
    }
    m_stats.avgLoopDuration = std::chrono::duration_cast<std::chrono::milliseconds>(m_totalTime).count()/static_cast<float>(m_currentGameLoop);
    m_stats.gameLoops = m_currentGameLoop;
    PrintThread{} << m_botConfig.BotName << " : Exiting with " << GetExitCaseString(m_result) << " Average step time " << m_stats.avgLoopDuration << " microseconds, total time: " << std::chrono::duration_cast<std::chrono::seconds>(m_totalTime).count() << " seconds, game loops: " << m_currentGameLoop << std::endl;
}

bool Proxy::isBotCrashed(const int milliseconds) const
{
    return m_botProgramThread.wait_for(std::chrono::milliseconds(milliseconds)) == std::future_status::ready;
}

bool Proxy::isClientCrashed(const int) const
{
    // toDo
    // Big effort due to cross compatibility,
    // little gain since it is only needed to make a clearer error message.
    return false;
}

bool Proxy::processRequest(const sc2::RequestData& request)
{
    if (request.second)
    {
        const auto expectedResponseCase = static_cast<SC2APIProtocol::Response::ResponseCase>(request.second->request_case());
        if (request.second->has_join_game())
        {
            const bool appliedLanHostIp = ApplyLanJoinGameHostIp(*request.second, m_botConfig);
            PrintThread{} << m_botConfig.BotName
                          << " : forwarding JoinGame request to SC2 client. "
                          << RequestDebugSummary(*request.second, expectedResponseCase)
                          << " host_ip_applied=" << BoolString(appliedLanHostIp)
                          << " server_connections=" << m_server.connections_.size()
                          << " client_connected=" << BoolString(m_client.connection_ != nullptr)
                          << " timeout_ms=" << kJoinGameResponseTimeoutMS
                          << std::endl;
        }
        if (request.second->has_quit())
        {
            // Intercept quit requests, we want to keep game alive to save replays.
            // If a s2client-api (c++) throws an exception a quit request gets issued.
            // So check if it crashed.
            if (isBotCrashed(1000))
            {
                PrintThread{} << m_botConfig.BotName << " : crashed." << std::endl;
            }
            else
            {
                PrintThread{} << m_botConfig.BotName << " HAS ISSUED A QUIT REQUEST. Please tell the author not to." << std::endl;
            }
            return false;
        }
        if (request.second->has_leave_game())
        {
            // Leave game requests are also a problem.
            PrintThread{} << m_botConfig.BotName << " has issued a leave game request. Please don't do that." << std::endl;
            // return false;
        }
        else if (request.second->has_debug() && !m_usedDebugInterface)
        {
            PrintThread{} << m_botConfig.BotName << " : IS USING DEBUG INTERFACE.  POSSIBLE CHEAT! Please tell them not to." << std::endl;
            m_usedDebugInterface = true;
        }
        else if (request.second->has_step() && m_currentGameLoop)
        {
            m_totalTime += clock::now() - m_lastResponseSendTime;
        }
    }
    return true;
}

bool Proxy::processResponse(SC2APIProtocol::Response* const response)
{
    if (response == nullptr)
    {
        PrintThread{} << m_botConfig.BotName << " : Waiting for a response had a timeout or was invalid." << std::endl;
        return false;
    }
    if (response->has_join_game())
    {
        PrintThread{} << m_botConfig.BotName << " : JoinGame response received player_id="
                      << response->join_game().player_id() << std::endl;
    }
    if (response->has_observation())
    {
        const SC2APIProtocol::Observation& observation = response->observation().observation();
        m_currentGameLoop = observation.game_loop();
        //20260710_kpopmodder: Read the already-received SC2 observation for
        // passive AI commentary. Human proxies are intentionally excluded.
        if (m_botConfig.Type != BotType::Human
            && observation.has_player_common()
            && m_currentGameLoop > 0U
            && (m_lastTelemetryGameLoop == 0U
                || m_currentGameLoop - m_lastTelemetryGameLoop >= 224U))
        {
            const auto& common = observation.player_common();
            uint32_t selfUnits = 0U;
            uint32_t visibleEnemyUnits = 0U;
            uint32_t underConstructionUnits = 0U;
            std::map<uint32_t, uint32_t> selfUnitTypes;
            //20260711_kpopmodder: Preserve type-level construction and completed
            // upgrade state while keeping the existing aggregate telemetry fields.
            std::map<uint32_t, uint32_t> underConstructionUnitTypes;
            std::vector<uint32_t> completedUpgradeIds;
            if (observation.has_raw_data())
            {
                const auto& rawData = observation.raw_data();
                for (int i = 0; i < rawData.units_size(); ++i)
                {
                    const auto& unit = rawData.units(i);
                    if (!unit.has_alliance())
                    {
                        continue;
                    }
                    if (unit.alliance() == SC2APIProtocol::Alliance::Self)
                    {
                        ++selfUnits;
                        ++selfUnitTypes[unit.unit_type()];
                        if (unit.has_build_progress() && unit.build_progress() < 1.0f)
                        {
                            ++underConstructionUnits;
                            ++underConstructionUnitTypes[unit.unit_type()];
                        }
                    }
                    else if (unit.alliance() == SC2APIProtocol::Alliance::Enemy)
                    {
                        ++visibleEnemyUnits;
                    }
                }
                //20260711_kpopmodder: PlayerRaw upgrade_ids contains upgrades that
                // are already completed for the observed AI player.
                if (rawData.has_player())
                {
                    const auto& player = rawData.player();
                    for (int i = 0; i < player.upgrade_ids_size(); ++i)
                    {
                        completedUpgradeIds.push_back(player.upgrade_ids(i));
                    }
                }
            }
            std::ostringstream telemetry;
            telemetry << "LAV_OBSERVATION {\"schema\":1"
                << ",\"bot\":\"" << m_botConfig.BotName << "\""
                << ",\"role\":\"ai\""
                << ",\"player_id\":" << common.player_id()
                << ",\"game_loop\":" << m_currentGameLoop
                << ",\"minerals\":" << common.minerals()
                << ",\"vespene\":" << common.vespene()
                << ",\"food_used\":" << common.food_used()
                << ",\"food_cap\":" << common.food_cap()
                << ",\"food_workers\":" << common.food_workers()
                << ",\"food_army\":" << common.food_army()
                << ",\"army_count\":" << common.army_count()
                << ",\"idle_workers\":" << common.idle_worker_count()
                << ",\"self_units\":" << selfUnits
                << ",\"visible_enemy_units\":" << visibleEnemyUnits
                << ",\"under_construction_units\":" << underConstructionUnits
                << ",\"under_construction_type_counts\":{";
            bool firstUnderConstructionType = true;
            for (const auto& entry : underConstructionUnitTypes)
            {
                if (!firstUnderConstructionType)
                {
                    telemetry << ",";
                }
                telemetry << "\"" << entry.first << "\":" << entry.second;
                firstUnderConstructionType = false;
            }
            telemetry << "}"
                << ",\"unit_type_counts\":{";
            bool firstUnitType = true;
            for (const auto& entry : selfUnitTypes)
            {
                if (!firstUnitType)
                {
                    telemetry << ",";
                }
                telemetry << "\"" << entry.first << "\":" << entry.second;
                firstUnitType = false;
            }
            telemetry << "}"
                << ",\"upgrade_ids\":[";
            for (std::size_t i = 0; i < completedUpgradeIds.size(); ++i)
            {
                if (i > 0U)
                {
                    telemetry << ",";
                }
                telemetry << completedUpgradeIds[i];
            }
            telemetry << "]"
                << "}";
            PrintThread{} << telemetry.str() << std::endl;
            m_lastTelemetryGameLoop = m_currentGameLoop;
        }
        // forced tie situation
        if (m_result == ExitCase::GameTimeOver && response->observation().player_result_size() > 0)
        {
            auto* const obs = response->mutable_observation();
            std::vector<uint32_t> allPlayerIDs;
            for (int i(0); i < obs->player_result_size(); ++i)
            {
                allPlayerIDs.push_back(obs->player_result(i).player_id());
            }
            obs->clear_player_result();
            for (const auto& playerID : allPlayerIDs)
            {
                auto* const result = obs->add_player_result();
                result->set_player_id(playerID);
                result->set_result(SC2APIProtocol::Result::Tie);
            }
        }
        for (int i(0); i < response->observation().chat_size(); ++i)
        {
            const auto& chat = response->observation().chat(i);
            if (observation.player_common().player_id() == chat.player_id())
            {
                if (chat.has_message() && chat.message() == m_botConfig.SurrenderPhrase)
                {
                    m_surrenderLoop = m_currentGameLoop + 68; // ~3 in-game sec
                }
            }
        }
        if (m_surrenderLoop && m_currentGameLoop >= m_surrenderLoop)
        {
            terminateGame();
        }

        if (m_maxGameLoops && m_currentGameLoop > m_maxGameLoops)
        {
            m_result = ExitCase::GameTimeOver;
        }
    }
    if (response->has_step())
    {
        m_lastResponseSendTime = clock::now();
    }
    return true;
}


void Proxy::terminateGame()
{
    PrintThread{} << m_botConfig.BotName << " : surrender." << std::endl;
    sc2::ProtoInterface proto;
    sc2::GameRequestPtr request = proto.MakeRequest();

    SC2APIProtocol::RequestDebug* debugRequest = request->mutable_debug();

    auto debugCommand = debugRequest->add_debug();
    auto endGame = debugCommand->mutable_end_game();
    // If the proxy has to end the game it is because the bot failed somehow (crash, too slow, etc), aka lost.
    endGame->set_end_result(SC2APIProtocol::DebugEndGame_EndResult::DebugEndGame_EndResult_Surrender);

    m_client.Send(request.get());
    SC2APIProtocol::Response* debugResponse = receiveResponse(SC2APIProtocol::Response::ResponseCase::kDebug);
    if (debugResponse)
    {
        if (debugResponse->has_status())
        {
            updateStatus(debugResponse->status());
        }
    }
}

// If the bot is dead and the opponent bot has a step_size so that
// the current loop is an 'off step' loop the proxy needs to step on behalf of the bot.
void Proxy::doAStep()
{
    sc2::ProtoInterface proto;
    sc2::GameRequestPtr request = proto.MakeRequest();

    SC2APIProtocol::RequestStep* stepRequest = request->mutable_step();

    stepRequest->set_count(1);  // ToDo: this can maybe made smarter
    m_client.Send(request.get());
    SC2APIProtocol::Response* response = receiveResponse(SC2APIProtocol::Response::ResponseCase::kStep);
    if (response)
    {
        if (response->has_status())
        {
            updateStatus(response->status());
        }
    }
}

uint32_t Proxy::getMaxStepTime() const
{
    if (m_currentGameLoop)
    {
        return 20000U;  // ToDo: Add this to config file.
    }
    return 300000U;
}

void Proxy::updateStatus(const SC2APIProtocol::Status newStatus)
{
    if (newStatus != m_gameStatus && (m_gameStatus == SC2APIProtocol::Status::unknown || static_cast<int>(newStatus) > static_cast<int>(m_gameStatus)))
    {
        PrintThread{} << m_botConfig.BotName << " : Client changed status from "<< statusToString(m_gameStatus) << " to " << statusToString(newStatus) << std::endl;
        m_gameStatus = newStatus;
    }
}

bool Proxy::saveReplay(const std::string& replayFile)
{
    if (m_result == ExitCase::Error)
    {
        PrintThread{} << m_botConfig.BotName << " : Match ended in error. Can not save replay." << std::endl;
        // Maybe we could. But most likely we will get an assertion failed or even exception. Better safe than sorry.
        return false;
    }
    PrintThread{} << m_botConfig.BotName << " : Saving replay." << std::endl;
    sc2::ProtoInterface proto;
    sc2::GameRequestPtr request = proto.MakeRequest();
    request->mutable_save_replay();
    m_client.Send(request.get());
    SC2APIProtocol::Response* response = receiveResponse(SC2APIProtocol::Response::ResponseCase::kSaveReplay);
    if (!response || !response->has_save_replay())
    {
        PrintThread{} << m_botConfig.BotName << " : Failed to receive replay response." << std::endl;
        return false;
    }

    const SC2APIProtocol::ResponseSaveReplay& replay = response->save_replay();

    if (replay.data().empty())
    {
        PrintThread{} << m_botConfig.BotName << " : Replay data empty." << std::endl;
        return false;
    }

    std::ofstream file;
    file.open(replayFile, std::fstream::binary);
    if (!file.is_open())
    {
        PrintThread{} << m_botConfig.BotName << " : Could not open replay file: " << replayFile << std::endl;
        return false;
    }

    file.write(&replay.data()[0], static_cast<std::streamsize>(replay.data().size()));
    return true;
}

SC2APIProtocol::Response* Proxy::receiveResponse(const SC2APIProtocol::Response::ResponseCase responseCase, const unsigned int timeoutMS)
{
    SC2APIProtocol::Response* response{nullptr};
    if (!m_client.Receive(response, timeoutMS))
    {
        PrintThread{} << "ERROR: " << m_botConfig.BotName << " : timeout/closed/error waiting for "
                      << responseCaseToString(responseCase)
                      << " after " << timeoutMS << "ms."
                      << " client_connected=" << BoolString(m_client.connection_ != nullptr)
                      << " server_connections=" << m_server.connections_.size()
                      << std::endl;
        if (responseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame)
        {
            PrintThread{} << "ERROR: " << m_botConfig.BotName
                          << " : JoinGame response timeout/closed/error. Remote SC2 websocket did not return JoinGame;"
                          << " check remote SC2 screen, map load state, and whether the API socket closed after CreateGame."
                          << std::endl;
        }
        return nullptr;
    }
    if (responseCase == SC2APIProtocol::Response::ResponseCase::kJoinGame
        || response->response_case() == SC2APIProtocol::Response::ResponseCase::kJoinGame)
    {
        PrintThread{} << m_botConfig.BotName << " : received JoinGame response from remote SC2 websocket. "
                      << ResponseDebugSummary(*response) << std::endl;
    }
    bool hasErrors = false;
    if (responseCase != response->response_case())
    {
        PrintThread{} << m_botConfig.BotName << " : expected " << responseCaseToString(responseCase) << " but got " << responseCaseToString(response->response_case()) <<std::endl;
        hasErrors = true;
    }
    if (response->error_size())
    {
        std::ostringstream ss;
        ss << m_botConfig.BotName << " : response " << responseCaseToString(response->response_case()) << " has " << response->error_size() << " error(s)!" << std::endl;
        for (int i(0); i < response->error_size(); ++i)
        {
            ss << "\t \t \t * " << response->error(i) << std::endl;
        }
        PrintThread{} << ss.str();
        hasErrors = true;
    }
    if (hasErrors)
    {
        updateStatus(SC2APIProtocol::Status::ended);
    }

    // During the game we only update the status if the bot also gets the update at the same time.
    // The bot gets the update via the observation, so we can only update if the response has an observation.
    // If we wouldn't do this, the LM would know the game ended and wouldn't proxy anymore steps.
    // Bad if the bot has an 'off step' due to step size != 1.
    if (response->has_status() && (response->has_observation() || m_gameStatus != SC2APIProtocol::Status::in_game))
    {
        updateStatus(response->status());
    }
    return response;
}

const Stats& Proxy::stats() const
{
    return m_stats;
}

SC2APIProtocol::Result Proxy::getGameResult()
{
    sc2::ProtoInterface proto;
    sc2::GameRequestPtr request = proto.MakeRequest();

    request->mutable_observation();
    m_client.Send(request.get());
    SC2APIProtocol::Response* observationResponse = receiveResponse(SC2APIProtocol::Response::ResponseCase::kObservation);
    if (observationResponse)
    {
        const SC2APIProtocol::ResponseObservation& obs = observationResponse->observation();
        const SC2APIProtocol::Observation& observation = obs.observation();
        const auto playerID = observation.player_common().player_id();
        for (int i(0); i < obs.player_result_size(); ++i)
        {
            if (obs.player_result(i).player_id() == playerID)
            {
                return obs.player_result(i).result();
            }
        }
    }
    return SC2APIProtocol::Result::Undecided;
}
