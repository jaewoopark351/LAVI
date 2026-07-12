//20260701_kpopmodder: Defines the safe bridge contract behind the BWAPI-compatible shim.
#pragma once

#include <cstdint>
#include <map>
#include <memory>
#include <ostream>
#include <string>
#include <vector>

namespace LAVBWAPIRM {

enum class CommandType {
    Train,
    Build,
    Move,
    Attack,
    Stop,
    HoldPosition,
    RightClick,
    Gather,
    Repair,
    Research,
    Upgrade,
    UseTech,
    LogOnly
};

struct Position {
    int x = 0;
    int y = 0;
};

struct UnitSnapshot {
    int id = 0;
    std::string type;
    std::string owner;
    int ownerId = 0;
    Position position;
    int hitPoints = 0;
    int shields = 0;
    int energy = 0;
    int resources = 0;
    bool completed = false;
    bool visible = true;
    bool selected = false;
    bool flying = false;
    bool idle = false;
    std::string order;
};

struct PlayerSnapshot {
    int id = 0;
    std::string name;
    std::string race;
    int minerals = 0;
    int gas = 0;
    int supplyUsed = 0;
    int supplyTotal = 0;
    Position startLocation;
    std::vector<std::string> researchedTechs;
    std::vector<std::string> researchingTechs;
    std::vector<std::string> upgradingUpgrades;
    std::map<std::string, int> upgradeLevels;
};

struct GameSnapshot {
    bool connected = false;
    bool inGame = false;
    bool singlePlayer = true;
    bool battleNetScreen = false;
    bool multiplayerScreen = false;
    int frameCount = 0;
    std::string mapName;
    int mapWidth = 128;
    int mapHeight = 128;
    PlayerSnapshot self;
    PlayerSnapshot enemy;
    std::vector<UnitSnapshot> myUnits;
    std::vector<UnitSnapshot> enemyUnits;
    std::vector<UnitSnapshot> neutralUnits;
};

struct Command {
    CommandType type = CommandType::LogOnly;
    std::vector<int> unitIds;
    int targetUnitId = 0;
    bool hasTargetUnit = false;
    Position targetPosition;
    bool hasTargetPosition = false;
    std::string unitName;
    std::string buildingName;
    std::string abilityName;
    std::string payload;
};

class Bridge {
public:
    virtual ~Bridge() = default;
    virtual bool connect() = 0;
    virtual void disconnect() = 0;
    virtual GameSnapshot snapshot() = 0;
    virtual bool sendCommand(const Command& command) = 0;
    virtual void stopAllControl() = 0;
    virtual void advanceFrame() {}
};

class NullBridge final : public Bridge {
public:
    bool connect() override;
    void disconnect() override;
    GameSnapshot snapshot() override;
    bool sendCommand(const Command& command) override;
    void stopAllControl() override;

private:
    GameSnapshot snapshot_;
};

std::shared_ptr<Bridge> getBridge();
void setBridge(std::shared_ptr<Bridge> bridge);
std::string commandTypeName(CommandType type);
std::string formatCommandLog(const Command& command, int frameCount);

} // namespace LAVBWAPIRM
