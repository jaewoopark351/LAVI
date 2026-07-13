//20260709_kpopmodder: Qt-free local launcher so LAV can open a human-vs-bot StarCraft II ladder game.
#include <algorithm>
#include <cctype>
#include <iostream>
#include <cstdlib>
#include <string>
#include <vector>

#include "AgentsConfig.h"
#include "LadderConfig.h"
#include "LadderGame.h"
#include "Types.h"

namespace
{
std::string toLower(std::string value)
{
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return value;
}

bool hasArg(int argc, char** argv, const std::string& name)
{
    const std::string prefix = name + "=";
    for (int i = 1; i < argc; ++i)
    {
        const std::string arg = argv[i] ? argv[i] : "";
        if (arg == name || arg.rfind(prefix, 0) == 0)
        {
            return true;
        }
    }
    return false;
}

std::string normalizeArgName(const std::string& name)
{
    return name + "=";
}

bool shouldExcludeFromSc2Args(const std::string& arg)
{
    static const std::vector<std::string> excluded = {
        "--human-name",
        "--bot",
        "--map",
        "--race",
        "--bot-race",
        "--bot-dir",
        "--config",
        "--remote-human-host",
        "--remote-human-client-port",
        "--no-realtime",
        "--realtime",
    };
    for (const auto& excluded_name : excluded)
    {
        if (arg == excluded_name || arg.rfind(normalizeArgName(excluded_name), 0) == 0)
        {
            return true;
        }
    }
    return false;
}

bool isBooleanArgValue(const std::string& arg)
{
    return arg == "--no-realtime" || arg == "--realtime";
}

std::vector<std::string> coordinatorArgs(int argc, char** argv)
{
    std::vector<std::string> args;
    if (argc <= 0)
    {
        return args;
    }
    args.reserve(static_cast<size_t>(argc));
    args.push_back(argv[0] ? argv[0] : "");
    for (int i = 1; i < argc; ++i)
    {
        const std::string current_arg = argv[i] ? argv[i] : "";
        if (current_arg.empty())
        {
            continue;
        }
        if (shouldExcludeFromSc2Args(current_arg))
        {
            if (!isBooleanArgValue(current_arg) && current_arg.find('=') == std::string::npos && i + 1 < argc)
            {
                ++i;
            }
            continue;
        }
        args.push_back(current_arg);
    }
    return args;
}

std::string argValue(int argc, char** argv, const std::string& name, const std::string& fallback)
{
    const std::string prefix = name + "=";
    for (int i = 1; i < argc; ++i)
    {
        const std::string arg = argv[i] ? argv[i] : "";
        if (arg == name && i + 1 < argc)
        {
            return argv[i + 1] ? argv[i + 1] : fallback;
        }
        if (arg.rfind(prefix, 0) == 0)
        {
            return arg.substr(prefix.size());
        }
    }
    return fallback;
}

void printUsage()
{
    std::cout
        << "LavHumanVsBot - local human vs ProBots launcher\n"
        << "Usage:\n"
        << "  LavHumanVsBot.exe --human-name LAVHuman --bot changeling --map IncorporealAIE_v4 --race Terran --bot-race Random --bot-dir Bots --config HumanLadder.json --realtime\n\n"
        << "  Human race: --race Terran|Zerg|Protoss|Random\n"
        << "  Bot race override: --bot-race Terran|Zerg|Protoss|Random\n"
        << "  Remote human slot: --remote-human-host 192.168.0.20 --remote-human-client-port 5679\n\n"
        << "SC2PATH should point to the full SC2_x64.exe path. LAV injects that environment value before launch.\n";
}
}

int main(int argc, char** argv)
{
    if (hasArg(argc, argv, "--help") || hasArg(argc, argv, "-h"))
    {
        printUsage();
        return 0;
    }

    const std::string humanName = argValue(argc, argv, "--human-name", "LAVHuman");
    const std::string botName = argValue(argc, argv, "--bot", "changeling");
    const std::string mapName = argValue(argc, argv, "--map", "IncorporealAIE_v4");
    const std::string raceName = argValue(argc, argv, "--race", "Terran");
    const std::string botRaceName = argValue(argc, argv, "--bot-race", "");
    const std::string botDirectory = argValue(argc, argv, "--bot-dir", "Bots");
    const std::string configPath = argValue(argc, argv, "--config", "HumanLadder.json");
    const std::string remoteHumanHost = argValue(argc, argv, "--remote-human-host", "");
    const int remoteHumanClientPort = std::atoi(argValue(argc, argv, "--remote-human-client-port", "5679").c_str());
    const bool realtime = !hasArg(argc, argv, "--no-realtime");

    LadderConfig config(configPath);
    if (!config.ParseConfig())
    {
        std::cerr << "[LavHumanVsBot] Unable to parse config: " << configPath << std::endl;
        return 2;
    }

    AgentsConfig agents(&config);
    agents.ReadBotDirectories(botDirectory);

    BotConfig bot;
    if (!agents.FindBot(botName, bot))
    {
        std::cerr << "[LavHumanVsBot] Unable to find bot: " << botName << " in " << botDirectory << std::endl;
        return 3;
    }
    if (!botRaceName.empty())
    {
        //20260710_kpopmodder: Let LAV override Changeling's runtime race without editing the bot's source config.
        bot.Race = GetRaceFromString(botRaceName);
    }

    BotConfig human;
    human.Type = BotType::Human;
    human.BotName = humanName;
    human.Race = GetRaceFromString(raceName);
    human.PlayerId = "HUMAN";
    human.Skeleton = false;
    human.Enabled = true;

    std::cout << "[LavHumanVsBot] Starting " << human.BotName << " vs " << bot.BotName
              << " on " << mapName
              << " human-race=" << GetRaceString(human.Race)
              << " bot-race=" << GetRaceString(bot.Race)
              << " realtime=" << (realtime ? "true" : "false");
    if (!remoteHumanHost.empty() && remoteHumanClientPort > 0)
    {
        std::cout << " remote-human=" << remoteHumanHost << ":" << remoteHumanClientPort;
    }
    std::cout << std::endl;

    std::vector<std::string> coordinator_args = coordinatorArgs(argc, argv);
    std::vector<char*> coordinator_argv;
    coordinator_argv.reserve(coordinator_args.size());
    for (auto& arg : coordinator_args)
    {
        coordinator_argv.push_back(&arg[0]);
    }
    LadderGame game(
        static_cast<int>(coordinator_argv.size()),
        coordinator_argv.data(),
        &config
    );
    game.SetRealTime(realtime);
    GameResult result = game.StartGame(human, bot, mapName, remoteHumanHost, remoteHumanClientPort);
    std::cout << "[LavHumanVsBot] Finished with result: " << GetResultType(result.Result) << std::endl;

    if (result.Result == ResultType::InitializationError || result.Result == ResultType::Error)
    {
        return 4;
    }
    return 0;
}
