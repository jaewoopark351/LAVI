//20260701_kpopmodder: Seeds a deterministic local StarCraft-like snapshot for BWAPICompat tests.
#include "LAVBWAPIRM/MockGameStateProvider.h"

#include <cstdlib>
#include <string>
#include <utility>

namespace LAVBWAPIRM {
namespace {
UnitSnapshot makeUnit(
    int id,
    std::string type,
    int ownerId,
    std::string owner,
    int x,
    int y,
    int hitPoints,
    bool completed = true
)
{
    UnitSnapshot unit;
    unit.id = id;
    unit.type = std::move(type);
    unit.ownerId = ownerId;
    unit.owner = std::move(owner);
    unit.position = {x, y};
    unit.hitPoints = hitPoints;
    unit.completed = completed;
    unit.visible = true;
    unit.idle = true;
    return unit;
}

bool isCombatScenario()
{
    const char* scenario = std::getenv("LAV_SAIDA_MOCK_SCENARIO");
    if (!scenario) {
        return false;
    }

    const std::string value = scenario;
    return value == "combat" || value == "active-combat" || value == "combat-units"
        || value == "combat-tech" || value == "combat_tech";
}

bool isTechSurfaceScenario()
{
    const char* scenario = std::getenv("LAV_SAIDA_MOCK_SCENARIO");
    if (!scenario) {
        return false;
    }

    const std::string value = scenario;
    return value == "tech" || value == "tech-surface" || value == "tech_surface"
        || value == "tech-buildings" || value == "tech_buildings"
        || value == "combat-tech" || value == "combat_tech";
}

bool isResearchUpgradeScenario()
{
    const char* scenario = std::getenv("LAV_SAIDA_MOCK_SCENARIO");
    if (!scenario) {
        return false;
    }

    const std::string value = scenario;
    return value == "research" || value == "research-upgrade" || value == "research_upgrade"
        || value == "upgrade" || value == "upgrades" || value == "research-surface"
        || value == "research_surface";
}

void addResourceCluster(
    GameSnapshot& snapshot,
    int firstMineralId,
    int mineralStartX,
    int mineralY,
    int geyserId,
    int geyserX,
    int geyserY
)
{
    for (int index = 0; index < 6; ++index) {
        UnitSnapshot mineral = makeUnit(
            firstMineralId + index,
            "Resource Mineral Field",
            0,
            "Neutral",
            mineralStartX + index * 32,
            mineralY,
            1
        );
        mineral.resources = 1500;
        snapshot.neutralUnits.push_back(mineral);
    }

    UnitSnapshot geyser = makeUnit(
        geyserId,
        "Resource Vespene Geyser",
        0,
        "Neutral",
        geyserX,
        geyserY,
        1
    );
    geyser.resources = 5000;
    snapshot.neutralUnits.push_back(geyser);
}
} // namespace

MockGameStateProvider::MockGameStateProvider()
{
    seedInitialState();
}

GameSnapshot MockGameStateProvider::snapshot() const
{
    return snapshot_;
}

void MockGameStateProvider::advanceFrame()
{
    ++snapshot_.frameCount;
    if (snapshot_.frameCount % 24 == 0) {
        snapshot_.self.minerals += 8;
    }
}

void MockGameStateProvider::reset()
{
    seedInitialState();
}

void MockGameStateProvider::seedInitialState()
{
    snapshot_ = {};
    snapshot_.connected = true;
    snapshot_.inGame = true;
    snapshot_.singlePlayer = true;
    snapshot_.mapName = "Mock Fighting Spirit";
    snapshot_.mapWidth = 64;
    snapshot_.mapHeight = 64;
    snapshot_.frameCount = 0;

    snapshot_.self.id = 1;
    snapshot_.self.name = "SAIDA";
    snapshot_.self.race = "Terran";
    snapshot_.self.minerals = 50;
    snapshot_.self.gas = 0;
    snapshot_.self.supplyUsed = 8;
    snapshot_.self.supplyTotal = 20;
    snapshot_.self.startLocation = {9, 15};

    snapshot_.enemy.id = 2;
    snapshot_.enemy.name = "MockEnemy";
    snapshot_.enemy.race = "Zerg";
    snapshot_.enemy.minerals = 50;
    snapshot_.enemy.gas = 0;
    snapshot_.enemy.supplyUsed = 4;
    snapshot_.enemy.supplyTotal = 18;
    snapshot_.enemy.startLocation = {24, 7};

    snapshot_.myUnits.push_back(
        makeUnit(1, "Terran Command Center", 1, "SAIDA", 288, 480, 1500)
    );
    snapshot_.myUnits.push_back(makeUnit(2, "Terran SCV", 1, "SAIDA", 288, 560, 60));
    snapshot_.myUnits.push_back(makeUnit(3, "Terran SCV", 1, "SAIDA", 304, 560, 60));
    snapshot_.myUnits.push_back(makeUnit(4, "Terran SCV", 1, "SAIDA", 320, 560, 60));
    snapshot_.myUnits.push_back(makeUnit(5, "Terran SCV", 1, "SAIDA", 336, 560, 60));

    snapshot_.enemyUnits.push_back(
        makeUnit(101, "Zerg Hatchery", 2, "MockEnemy", 768, 224, 1250)
    );
    snapshot_.myUnits.push_back(
        makeUnit(6, "Terran Command Center", 1, "SAIDA", 800, 1456, 1500)
    );
    snapshot_.enemyUnits.push_back(
        makeUnit(102, "Zerg Hatchery", 2, "MockEnemy", 1568, 704, 1250)
    );

    //20260701_kpopmodder: Seed extra gas bases so SAIDA can calculate secondExpansion/third multi candidates.
    addResourceCluster(snapshot_, 200, 128, 672, 250, 384, 672);
    addResourceCluster(snapshot_, 300, 672, 128, 350, 672, 320);
    addResourceCluster(snapshot_, 400, 672, 1536, 450, 960, 1664);
    addResourceCluster(snapshot_, 500, 1472, 640, 550, 1760, 832);
    addResourceCluster(snapshot_, 600, 1472, 1664, 650, 1760, 1856);
    addResourceCluster(snapshot_, 700, 128, 192, 750, 384, 352);

    if (isTechSurfaceScenario() || isResearchUpgradeScenario()) {
        seedTechSurfaceScenario();
    }
    if (isResearchUpgradeScenario()) {
        seedResearchUpgradeScenario();
    }
    if (isCombatScenario()) {
        seedCombatScenario();
    }
}

void MockGameStateProvider::seedTechSurfaceScenario()
{
    snapshot_.myUnits.push_back(makeUnit(70, "Terran Refinery", 1, "SAIDA", 384, 672, 750));
    snapshot_.myUnits.push_back(makeUnit(71, "Terran Refinery", 1, "SAIDA", 960, 1664, 750));
    snapshot_.myUnits.push_back(makeUnit(72, "Terran Barracks", 1, "SAIDA", 512, 480, 1000));
    snapshot_.myUnits.push_back(makeUnit(73, "Terran Engineering Bay", 1, "SAIDA", 640, 480, 850));
    snapshot_.myUnits.push_back(makeUnit(74, "Terran Academy", 1, "SAIDA", 768, 480, 600));
    snapshot_.myUnits.push_back(makeUnit(75, "Terran Factory", 1, "SAIDA", 512, 608, 1250));
    snapshot_.myUnits.push_back(makeUnit(76, "Terran Machine Shop", 1, "SAIDA", 640, 640, 750));
    snapshot_.myUnits.push_back(makeUnit(77, "Terran Armory", 1, "SAIDA", 704, 608, 750));
    snapshot_.myUnits.push_back(makeUnit(78, "Terran Starport", 1, "SAIDA", 832, 608, 1300));
    snapshot_.myUnits.push_back(makeUnit(79, "Terran Control Tower", 1, "SAIDA", 960, 640, 500));
    snapshot_.myUnits.push_back(makeUnit(80, "Terran Science Facility", 1, "SAIDA", 512, 736, 850));
    UnitSnapshot comsat = makeUnit(81, "Terran Comsat Station", 1, "SAIDA", 416, 512, 500);
    comsat.energy = 150;
    snapshot_.myUnits.push_back(comsat);
}

void MockGameStateProvider::seedResearchUpgradeScenario()
{
    snapshot_.self.minerals = 800;
    snapshot_.self.gas = 800;
    snapshot_.self.supplyUsed = 76;
    snapshot_.self.supplyTotal = 140;
    snapshot_.self.researchedTechs = {
        "Tank Siege Mode",
        "Spider Mines",
        "Irradiate",
        "EMP Shockwave"
    };
    snapshot_.self.researchingTechs = {"Cloaking Field"};
    snapshot_.self.upgradingUpgrades = {"Terran Vehicle Plating"};
    snapshot_.self.upgradeLevels = {
        {"Terran Vehicle Weapons", 2},
        {"Terran Vehicle Plating", 1},
        {"Ion Thrusters", 1},
        {"Charon Boosters", 1}
    };

    snapshot_.myUnits.push_back(makeUnit(82, "Terran Armory", 1, "SAIDA", 704, 736, 750));

    for (UnitSnapshot& unit : snapshot_.myUnits) {
        if (unit.type == "Terran Control Tower") {
            unit.order = "research Cloaking Field";
            unit.idle = false;
        } else if (unit.id == 77 && unit.type == "Terran Armory") {
            unit.order = "upgrade Terran Vehicle Plating";
            unit.idle = false;
        }
    }
}

void MockGameStateProvider::seedCombatScenario()
{
    snapshot_.self.minerals = 500;
    snapshot_.self.gas = 300;
    snapshot_.self.supplyUsed = 58;
    snapshot_.self.supplyTotal = 120;
    snapshot_.enemy.supplyUsed = 30;
    snapshot_.enemy.supplyTotal = 60;

    snapshot_.myUnits.push_back(makeUnit(20, "Terran Marine", 1, "SAIDA", 384, 544, 40));
    snapshot_.myUnits.push_back(makeUnit(21, "Terran Marine", 1, "SAIDA", 408, 544, 40));
    snapshot_.myUnits.push_back(makeUnit(30, "Terran Siege Tank Tank Mode", 1, "SAIDA", 448, 512, 150));
    snapshot_.myUnits.push_back(makeUnit(31, "Terran Siege Tank Tank Mode", 1, "SAIDA", 480, 512, 150));
    snapshot_.myUnits.push_back(makeUnit(40, "Terran Vulture", 1, "SAIDA", 512, 496, 80));
    snapshot_.myUnits.push_back(makeUnit(41, "Terran Vulture", 1, "SAIDA", 544, 496, 80));
    snapshot_.myUnits.push_back(makeUnit(42, "Terran Vulture", 1, "SAIDA", 576, 496, 80));
    snapshot_.myUnits.push_back(makeUnit(50, "Terran Goliath", 1, "SAIDA", 512, 544, 125));
    snapshot_.myUnits.push_back(makeUnit(51, "Terran Goliath", 1, "SAIDA", 544, 544, 125));

    UnitSnapshot wraith = makeUnit(60, "Terran Wraith", 1, "SAIDA", 608, 448, 120);
    wraith.flying = true;
    wraith.energy = 90;
    snapshot_.myUnits.push_back(wraith);

    UnitSnapshot vessel = makeUnit(61, "Terran Science Vessel", 1, "SAIDA", 576, 448, 200);
    vessel.flying = true;
    vessel.energy = 180;
    snapshot_.myUnits.push_back(vessel);

    UnitSnapshot dropship = makeUnit(62, "Terran Dropship", 1, "SAIDA", 416, 448, 150);
    dropship.flying = true;
    snapshot_.myUnits.push_back(dropship);

    snapshot_.enemyUnits.push_back(makeUnit(120, "Zerg Hydralisk", 2, "MockEnemy", 704, 320, 80));
    snapshot_.enemyUnits.push_back(makeUnit(121, "Zerg Hydralisk", 2, "MockEnemy", 736, 320, 80));
    UnitSnapshot mutalisk = makeUnit(122, "Zerg Mutalisk", 2, "MockEnemy", 736, 288, 120);
    mutalisk.flying = true;
    snapshot_.enemyUnits.push_back(mutalisk);

    UnitSnapshot overlord = makeUnit(123, "Zerg Overlord", 2, "MockEnemy", 704, 256, 200);
    overlord.flying = true;
    snapshot_.enemyUnits.push_back(overlord);
}

} // namespace LAVBWAPIRM
