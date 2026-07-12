//20260701_kpopmodder: Probes the Samase/LAV read-only snapshot path without game control.
#include "LAVBWAPIRM/FileBridge.h"

#include <cstdlib>
#include <iostream>
#include <memory>
#include <string>

namespace {

std::string envValue(const char* name)
{
    const char* value = std::getenv(name);
    return value ? std::string(value) : std::string();
}

bool startsWith(const std::string& text, const std::string& prefix)
{
    return text.rfind(prefix, 0) == 0;
}

std::string boolText(bool value)
{
    return value ? "true" : "false";
}

void printUsage()
{
    std::cout
        << "Usage: scr_readonly_runtime.exe [--snapshot=<path>]\n"
        << "\n"
        << "Reads a lav_bwapi_rm_snapshot_v1 JSON file and prints the BWAPI-RM\n"
        << "state surface. It does not send commands or attach to StarCraft.\n";
}

} // namespace

int main(int argc, char** argv)
{
    std::string snapshotPath = envValue("LAV_BWAPI_RM_SNAPSHOT_PATH");
    if (snapshotPath.empty()) {
        snapshotPath = "..\\..\\..\\logs\\starcraft_bwapi_rm_snapshot.json";
    }

    for (int index = 1; index < argc; ++index) {
        const std::string arg = argv[index] ? argv[index] : "";
        if (arg == "--help" || arg == "-h") {
            printUsage();
            return 0;
        }
        if (startsWith(arg, "--snapshot=")) {
            snapshotPath = arg.substr(std::string("--snapshot=").size());
        }
    }

    LAVBWAPIRM::FileBridge bridge(snapshotPath, "");
    const bool bridgeConnected = bridge.connect();
    const LAVBWAPIRM::GameSnapshot snapshot = bridge.snapshot();

    std::cout << "[SCR readonly] snapshot=" << snapshotPath << "\n";
    std::cout << "[SCR readonly] bridgeConnected=" << boolText(bridgeConnected)
              << " connected=" << boolText(snapshot.connected)
              << " inGame=" << boolText(snapshot.inGame)
              << " singlePlayer=" << boolText(snapshot.singlePlayer)
              << " battleNet=" << boolText(snapshot.battleNetScreen)
              << " multiplayer=" << boolText(snapshot.multiplayerScreen)
              << " frame=" << snapshot.frameCount << "\n";
    std::cout << "[SCR readonly] map=" << snapshot.mapName
              << " size=" << snapshot.mapWidth << "x" << snapshot.mapHeight << "\n";
    std::cout << "[SCR readonly] self name=" << snapshot.self.name
              << " race=" << snapshot.self.race
              << " minerals=" << snapshot.self.minerals
              << " gas=" << snapshot.self.gas
              << " supply=" << snapshot.self.supplyUsed
              << "/" << snapshot.self.supplyTotal
              << " start=" << snapshot.self.startLocation.x
              << "," << snapshot.self.startLocation.y << "\n";
    std::cout << "[SCR readonly] enemy name=" << snapshot.enemy.name
              << " race=" << snapshot.enemy.race
              << " supply=" << snapshot.enemy.supplyUsed
              << "/" << snapshot.enemy.supplyTotal
              << " start=" << snapshot.enemy.startLocation.x
              << "," << snapshot.enemy.startLocation.y << "\n";
    std::cout << "[SCR readonly] units my=" << snapshot.myUnits.size()
              << " enemy=" << snapshot.enemyUnits.size()
              << " neutral=" << snapshot.neutralUnits.size() << "\n";
    if (!snapshot.myUnits.empty()) {
        const LAVBWAPIRM::UnitSnapshot& unit = snapshot.myUnits.front();
        std::cout << "[SCR readonly] firstMyUnit id=" << unit.id
                  << " type=" << unit.type
                  << " pos=" << unit.position.x << "," << unit.position.y
                  << " hp=" << unit.hitPoints << "\n";
    }
    std::cout << "[SCR readonly] commands=disabled\n";
    std::cout << "[SCR readonly] ok=" << boolText(snapshot.connected) << "\n";
    return snapshot.connected ? 0 : 2;
}
