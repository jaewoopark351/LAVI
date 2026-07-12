//20260701_kpopmodder: Implements the source-level BWAPI facade over the safe bridge contract.
#include "BWAPI.h"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <iostream>
#include <initializer_list>
#include <string>
#include <utility>

namespace BWAPI {
namespace {
Game defaultGame;

std::string normalizedName(std::string value)
{
    std::replace(value.begin(), value.end(), '_', ' ');
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return value;
}

bool matchesAny(const std::string& value, std::initializer_list<const char*> names)
{
    const std::string normalized = normalizedName(value);
    for (const char* name : names) {
        if (normalized == normalizedName(name)) {
            return true;
        }
    }
    return false;
}

bool containsText(const std::string& value, const std::string& needle)
{
    return normalizedName(value).find(normalizedName(needle)) != std::string::npos;
}

bool isMockBlockedTile(int tileX, int tileY, int width, int height)
{
    if (tileX < 0 || tileY < 0 || tileX >= width || tileY >= height) {
        return true;
    }
    if (tileX <= 1 || tileY <= 1 || tileX >= width - 2 || tileY >= height - 2) {
        return true;
    }

    //20260701_kpopmodder: Give BWEM a simple local terrain chain so SAIDA sees first/second choke points.
    const bool inGateway = tileY >= 31 && tileY <= 32;
    const bool firstChokeWall = tileX == 16 && !inGateway;
    const bool secondChokeWall = tileX == 40 && !inGateway;
    return firstChokeWall || secondChokeWall;
}

WeaponType makeWeapon(
    std::string name,
    int maxRange,
    int minRange,
    bool targetsGround,
    bool targetsAir,
    int damage = 0,
    int cooldown = 1
)
{
    WeaponType weapon(std::move(name), maxRange, minRange);
    weapon.setTargets(targetsGround, targetsAir);
    weapon.setDamage(damage, cooldown);
    return weapon;
}

LAVBWAPIRM::Position toBridge(Position position)
{
    return {position.x, position.y};
}

LAVBWAPIRM::Position toBridge(TilePosition position)
{
    return {position.x, position.y};
}

Position toBWAPI(LAVBWAPIRM::Position position)
{
    return {position.x, position.y};
}

TilePosition toBwapTile(LAVBWAPIRM::Position position)
{
    return {position.x, position.y};
}

bool sendUnitCommand(int unitId, LAVBWAPIRM::Command command)
{
    command.unitIds.push_back(unitId);
    return LAVBWAPIRM::getBridge()->sendCommand(command);
}

bool unitTypeMatches(UnitType current, UnitType requested)
{
    const std::string requestedName = requested.getName();
    return requestedName == "Unknown" || requestedName == "None" || requestedName == "All Units" || current == requested;
}

bool namedListContains(const std::vector<std::string>& values, const std::string& value)
{
    const std::string normalizedValue = normalizedName(value);
    for (const std::string& current : values) {
        if (normalizedName(current) == normalizedValue) {
            return true;
        }
    }
    return false;
}

int namedLevelValue(const std::map<std::string, int>& values, const std::string& value)
{
    const std::string normalizedValue = normalizedName(value);
    for (const auto& item : values) {
        if (normalizedName(item.first) == normalizedValue) {
            return item.second;
        }
    }
    return 0;
}

Player resolveOwner(
    const LAVBWAPIRM::UnitSnapshot& unit,
    Player self,
    Player enemy,
    Player neutral
)
{
    if (unit.ownerId > 0) {
        if (self && unit.ownerId == self->getID()) {
            return self;
        }
        if (enemy && unit.ownerId == enemy->getID()) {
            return enemy;
        }
    }

    if (containsText(unit.owner, "enemy")) {
        return enemy;
    }
    if (containsText(unit.owner, "neutral") || containsText(unit.type, "mineral")
        || containsText(unit.type, "vespene")) {
        return neutral;
    }
    if (!unit.owner.empty() && self && containsText(unit.owner, self->getName())) {
        return self;
    }
    return nullptr;
}
} // namespace

Game* BroodwarPtr = &defaultGame;
GameHandle Broodwar;

namespace Races {
const Race Terran("Terran");
const Race Protoss("Protoss");
const Race Zerg("Zerg");
const Race None("None");
const Race Unknown("Unknown");
const Race Random("Random");
} // namespace Races

namespace UnitSizeTypes {
const UnitSizeType Independent("Independent");
const UnitSizeType Small("Small");
const UnitSizeType Medium("Medium");
const UnitSizeType Large("Large");
const UnitSizeType Unknown("Unknown");
} // namespace UnitSizeTypes

namespace GameTypes {
const GameType Melee("Melee");
const GameType Use_Map_Settings("Use Map Settings");
const GameType Unknown("Unknown");
} // namespace GameTypes

namespace UnitTypes {
const UnitType None("None", -1);
const UnitType AllUnits("All Units", -2);
const UnitType Men("Men", -3);
const UnitType Buildings("Buildings", -4);
const UnitType Terran_SCV("Terran SCV", 1);
const UnitType Terran_Command_Center("Terran Command Center", 2);
const UnitType Terran_Comsat_Station("Terran Comsat Station", 3);
const UnitType Terran_Nuclear_Silo("Terran Nuclear Silo", 4);
const UnitType Terran_Supply_Depot("Terran Supply Depot", 5);
const UnitType Terran_Barracks("Terran Barracks", 6);
const UnitType Terran_Bunker("Terran Bunker", 7);
const UnitType Terran_Missile_Turret("Terran Missile Turret", 8);
const UnitType Terran_Refinery("Terran Refinery", 9);
const UnitType Terran_Engineering_Bay("Terran Engineering Bay", 10);
const UnitType Terran_Academy("Terran Academy", 11);
const UnitType Terran_Armory("Terran Armory", 12);
const UnitType Terran_Factory("Terran Factory", 13);
const UnitType Terran_Machine_Shop("Terran Machine Shop", 14);
const UnitType Terran_Starport("Terran Starport", 15);
const UnitType Terran_Control_Tower("Terran Control Tower", 16);
const UnitType Terran_Science_Facility("Terran Science Facility", 17);
const UnitType Terran_Covert_Ops("Terran Covert Ops", 18);
const UnitType Terran_Physics_Lab("Terran Physics Lab", 19);
const UnitType Terran_Marine("Terran Marine", 20);
const UnitType Terran_Firebat("Terran Firebat", 21);
const UnitType Terran_Medic("Terran Medic", 22);
const UnitType Terran_Ghost("Terran Ghost", 23);
const UnitType Terran_Vulture("Terran Vulture", 24);
const UnitType Terran_Vulture_Spider_Mine("Terran Vulture Spider Mine", 25);
const UnitType Terran_Siege_Tank_Tank_Mode("Terran Siege Tank Tank Mode", 26);
const UnitType Terran_Siege_Tank_Siege_Mode("Terran Siege Tank Siege Mode", 27);
const UnitType Terran_Goliath("Terran Goliath", 28);
const UnitType Terran_Wraith("Terran Wraith", 29);
const UnitType Terran_Dropship("Terran Dropship", 30);
const UnitType Terran_Science_Vessel("Terran Science Vessel", 31);
const UnitType Terran_Battlecruiser("Terran Battlecruiser", 32);
const UnitType Terran_Valkyrie("Terran Valkyrie", 33);
const UnitType Terran_Nuclear_Missile("Terran Nuclear Missile", 34);
const UnitType Zerg_Larva("Zerg Larva", 101);
const UnitType Zerg_Egg("Zerg Egg", 102);
const UnitType Zerg_Drone("Zerg Drone", 103);
const UnitType Zerg_Zergling("Zerg Zergling", 104);
const UnitType Zerg_Hydralisk("Zerg Hydralisk", 105);
const UnitType Zerg_Lurker("Zerg Lurker", 106);
const UnitType Zerg_Mutalisk("Zerg Mutalisk", 107);
const UnitType Zerg_Guardian("Zerg Guardian", 108);
const UnitType Zerg_Scourge("Zerg Scourge", 109);
const UnitType Zerg_Queen("Zerg Queen", 110);
const UnitType Zerg_Defiler("Zerg Defiler", 111);
const UnitType Zerg_Ultralisk("Zerg Ultralisk", 112);
const UnitType Zerg_Overlord("Zerg Overlord", 113);
const UnitType Zerg_Broodling("Zerg Broodling", 114);
const UnitType Zerg_Infested_Terran("Zerg Infested Terran", 115);
const UnitType Zerg_Lurker_Egg("Zerg Lurker Egg", 116);
const UnitType Zerg_Cocoon("Zerg Cocoon", 117);
const UnitType Zerg_Devourer("Zerg Devourer", 118);
const UnitType Zerg_Hatchery("Zerg Hatchery", 119);
const UnitType Zerg_Lair("Zerg Lair", 120);
const UnitType Zerg_Hive("Zerg Hive", 121);
const UnitType Zerg_Creep_Colony("Zerg Creep Colony", 122);
const UnitType Zerg_Sunken_Colony("Zerg Sunken Colony", 123);
const UnitType Zerg_Spore_Colony("Zerg Spore Colony", 124);
const UnitType Zerg_Extractor("Zerg Extractor", 125);
const UnitType Zerg_Spawning_Pool("Zerg Spawning Pool", 126);
const UnitType Zerg_Evolution_Chamber("Zerg Evolution Chamber", 127);
const UnitType Zerg_Hydralisk_Den("Zerg Hydralisk Den", 128);
const UnitType Zerg_Spire("Zerg Spire", 129);
const UnitType Zerg_Greater_Spire("Zerg Greater Spire", 130);
const UnitType Zerg_Queens_Nest("Zerg Queens Nest", 131);
const UnitType Zerg_Nydus_Canal("Zerg Nydus Canal", 132);
const UnitType Zerg_Defiler_Mound("Zerg Defiler Mound", 133);
const UnitType Zerg_Ultralisk_Cavern("Zerg Ultralisk Cavern", 134);
const UnitType Zerg_Infested_Command_Center("Zerg Infested Command Center", 135);
const UnitType Protoss_Probe("Protoss Probe", 201);
const UnitType Protoss_Zealot("Protoss Zealot", 202);
const UnitType Protoss_Dragoon("Protoss Dragoon", 203);
const UnitType Protoss_High_Templar("Protoss High Templar", 204);
const UnitType Protoss_Dark_Templar("Protoss Dark Templar", 205);
const UnitType Protoss_Archon("Protoss Archon", 206);
const UnitType Protoss_Reaver("Protoss Reaver", 207);
const UnitType Protoss_Scarab("Protoss Scarab", 208);
const UnitType Protoss_Shuttle("Protoss Shuttle", 209);
const UnitType Protoss_Scout("Protoss Scout", 210);
const UnitType Protoss_Observer("Protoss Observer", 211);
const UnitType Protoss_Carrier("Protoss Carrier", 212);
const UnitType Protoss_Interceptor("Protoss Interceptor", 213);
const UnitType Protoss_Arbiter("Protoss Arbiter", 214);
const UnitType Protoss_Corsair("Protoss Corsair", 215);
const UnitType Protoss_Dark_Archon("Protoss Dark Archon", 216);
const UnitType Protoss_Nexus("Protoss Nexus", 217);
const UnitType Protoss_Pylon("Protoss Pylon", 218);
const UnitType Protoss_Assimilator("Protoss Assimilator", 219);
const UnitType Protoss_Gateway("Protoss Gateway", 220);
const UnitType Protoss_Forge("Protoss Forge", 221);
const UnitType Protoss_Photon_Cannon("Protoss Photon Cannon", 222);
const UnitType Protoss_Cybernetics_Core("Protoss Cybernetics Core", 223);
const UnitType Protoss_Robotics_Facility("Protoss Robotics Facility", 224);
const UnitType Protoss_Stargate("Protoss Stargate", 225);
const UnitType Protoss_Citadel_of_Adun("Protoss Citadel of Adun", 226);
const UnitType Protoss_Robotics_Support_Bay("Protoss Robotics Support Bay", 227);
const UnitType Protoss_Fleet_Beacon("Protoss Fleet Beacon", 228);
const UnitType Protoss_Templar_Archives("Protoss Templar Archives", 229);
const UnitType Protoss_Observatory("Protoss Observatory", 230);
const UnitType Protoss_Arbiter_Tribunal("Protoss Arbiter Tribunal", 231);
const UnitType Protoss_Shield_Battery("Protoss Shield Battery", 232);
const UnitType Spell_Scanner_Sweep("Spell Scanner Sweep", 290);
const UnitType Special_Pit_Door("Special Pit Door", 291);
const UnitType Special_Right_Pit_Door("Special Right Pit Door", 292);
const UnitType Special_Power_Generator("Special Power Generator", 293);
const UnitType Resource_Mineral_Field("Resource Mineral Field", 301);
const UnitType Resource_Vespene_Geyser("Resource Vespene Geyser", 302);
const UnitType Unknown("Unknown", 0);

std::vector<UnitType> allUnitTypes()
{
    return {
        Terran_SCV,
        Terran_Command_Center,
        Terran_Comsat_Station,
        Terran_Nuclear_Silo,
        Terran_Supply_Depot,
        Terran_Barracks,
        Terran_Bunker,
        Terran_Missile_Turret,
        Terran_Refinery,
        Terran_Engineering_Bay,
        Terran_Academy,
        Terran_Armory,
        Terran_Factory,
        Terran_Machine_Shop,
        Terran_Starport,
        Terran_Control_Tower,
        Terran_Science_Facility,
        Terran_Covert_Ops,
        Terran_Physics_Lab,
        Terran_Marine,
        Terran_Firebat,
        Terran_Medic,
        Terran_Ghost,
        Terran_Vulture,
        Terran_Vulture_Spider_Mine,
        Terran_Siege_Tank_Tank_Mode,
        Terran_Siege_Tank_Siege_Mode,
        Terran_Goliath,
        Terran_Wraith,
        Terran_Dropship,
        Terran_Science_Vessel,
        Terran_Battlecruiser,
        Terran_Valkyrie,
        Zerg_Drone,
        Zerg_Zergling,
        Zerg_Hydralisk,
        Zerg_Lurker,
        Zerg_Mutalisk,
        Zerg_Guardian,
        Zerg_Scourge,
        Zerg_Queen,
        Zerg_Defiler,
        Zerg_Ultralisk,
        Zerg_Overlord,
        Zerg_Broodling,
        Zerg_Infested_Terran,
        Zerg_Lurker_Egg,
        Zerg_Cocoon,
        Zerg_Devourer,
        Zerg_Hatchery,
        Zerg_Lair,
        Zerg_Hive,
        Zerg_Creep_Colony,
        Zerg_Sunken_Colony,
        Zerg_Spore_Colony,
        Zerg_Extractor,
        Zerg_Spawning_Pool,
        Zerg_Evolution_Chamber,
        Zerg_Hydralisk_Den,
        Zerg_Spire,
        Zerg_Greater_Spire,
        Zerg_Queens_Nest,
        Zerg_Nydus_Canal,
        Zerg_Defiler_Mound,
        Zerg_Ultralisk_Cavern,
        Zerg_Infested_Command_Center,
        Protoss_Probe,
        Protoss_Zealot,
        Protoss_Dragoon,
        Protoss_High_Templar,
        Protoss_Dark_Templar,
        Protoss_Archon,
        Protoss_Reaver,
        Protoss_Shuttle,
        Protoss_Scout,
        Protoss_Observer,
        Protoss_Carrier,
        Protoss_Arbiter,
        Protoss_Nexus,
        Protoss_Pylon,
        Protoss_Assimilator,
        Protoss_Gateway,
        Protoss_Forge,
        Protoss_Photon_Cannon,
        Protoss_Cybernetics_Core,
        Protoss_Robotics_Facility,
        Protoss_Stargate,
        Protoss_Arbiter_Tribunal,
        Protoss_Shield_Battery,
        Special_Pit_Door,
        Special_Right_Pit_Door,
        Special_Power_Generator,
        Resource_Mineral_Field,
        Resource_Vespene_Geyser
    };
}
} // namespace UnitTypes

namespace TechTypes {
const TechType None("None");
const TechType Unknown("Unknown");
const TechType Tank_Siege_Mode("Tank Siege Mode");
const TechType Spider_Mines("Spider Mines");
const TechType Cloaking_Field("Cloaking Field");
const TechType EMP_Shockwave("EMP Shockwave");
const TechType Irradiate("Irradiate");
const TechType Defensive_Matrix("Defensive Matrix");
const TechType Lurker_Aspect("Lurker Aspect");
const TechType Scanner_Sweep("Scanner Sweep");

std::vector<TechType> allTechTypes()
{
    return {
        Tank_Siege_Mode,
        Spider_Mines,
        Cloaking_Field,
        EMP_Shockwave,
        Irradiate,
        Defensive_Matrix,
        Lurker_Aspect,
        Scanner_Sweep
    };
}
} // namespace TechTypes

namespace UpgradeTypes {
const UpgradeType None("None");
const UpgradeType Unknown("Unknown");
const UpgradeType Terran_Vehicle_Weapons("Terran Vehicle Weapons");
const UpgradeType Terran_Vehicle_Plating("Terran Vehicle Plating");
const UpgradeType Ion_Thrusters("Ion Thrusters");
const UpgradeType Charon_Boosters("Charon Boosters");
const UpgradeType Metabolic_Boost("Metabolic Boost");
const UpgradeType Muscular_Augments("Muscular Augments");
const UpgradeType Grooved_Spines("Grooved Spines");
const UpgradeType Singularity_Charge("Singularity Charge");
const UpgradeType Leg_Enhancements("Leg Enhancements");
const UpgradeType Ventral_Sacs("Ventral Sacs");
const UpgradeType U_238_Shells("U-238 Shells");

std::vector<UpgradeType> allUpgradeTypes()
{
    return {
        Terran_Vehicle_Weapons,
        Terran_Vehicle_Plating,
        Ion_Thrusters,
        Charon_Boosters,
        Metabolic_Boost,
        Muscular_Augments,
        Grooved_Spines,
        Singularity_Charge,
        Leg_Enhancements,
        Ventral_Sacs,
        U_238_Shells
    };
}
} // namespace UpgradeTypes

namespace WeaponTypes {
const WeaponType None("None", 0, 0);
const WeaponType Gauss_Rifle = makeWeapon("Gauss Rifle", 5 * TILE_SIZE, 0, true, false, 6, 15);
const WeaponType C_10_Canister_Rifle = makeWeapon("C-10 Canister Rifle", 7 * TILE_SIZE, 0, true, true, 10, 22);
const WeaponType Fragmentation_Grenade = makeWeapon("Fragmentation Grenade", 5 * TILE_SIZE, 0, true, false, 8, 22);
const WeaponType Spider_Mines = makeWeapon("Spider Mines", 3 * TILE_SIZE, 0, true, false, 125, 1);
const WeaponType Twin_Autocannons = makeWeapon("Twin Autocannons", 5 * TILE_SIZE, 0, true, false, 12, 22);
const WeaponType Hellfire_Missile_Pack = makeWeapon("Hellfire Missile Pack", 5 * TILE_SIZE, 0, false, true, 10, 22);
const WeaponType Arclite_Cannon = makeWeapon("Arclite Cannon", 7 * TILE_SIZE, 0, true, false, 30, 37);
const WeaponType Arclite_Shock_Cannon = makeWeapon("Arclite Shock Cannon", 12 * TILE_SIZE, 2 * TILE_SIZE, true, false, 70, 75);
const WeaponType Burst_Lasers = makeWeapon("Burst Lasers", 5 * TILE_SIZE, 0, true, false, 8, 30);
const WeaponType Gemini_Missiles = makeWeapon("Gemini Missiles", 5 * TILE_SIZE, 0, false, true, 20, 22);
const WeaponType ATS_Laser_Battery = makeWeapon("ATS Laser Battery", 6 * TILE_SIZE, 0, true, false, 25, 30);
const WeaponType ATA_Laser_Battery = makeWeapon("ATA Laser Battery", 6 * TILE_SIZE, 0, false, true, 25, 30);
const WeaponType Needle_Spines = makeWeapon("Needle Spines", 4 * TILE_SIZE, 0, true, true, 10, 15);
const WeaponType Subterranean_Spines = makeWeapon("Subterranean Spines", 6 * TILE_SIZE, 0, true, false, 20, 37);
const WeaponType Glave_Wurm = makeWeapon("Glave Wurm", 3 * TILE_SIZE, 0, true, true, 9, 30);
const WeaponType Seeker_Spores = makeWeapon("Seeker Spores", 7 * TILE_SIZE, 0, false, true, 15, 15);
const WeaponType Phase_Disruptor = makeWeapon("Phase Disruptor", 4 * TILE_SIZE, 0, true, true, 20, 30);
const WeaponType Psi_Blades = makeWeapon("Psi Blades", TILE_SIZE, 0, true, false, 16, 22);
const WeaponType Photon_Cannon = makeWeapon("Photon Cannon", 7 * TILE_SIZE, 0, true, true, 20, 22);
const WeaponType Unknown("Unknown", 0, 0);
} // namespace WeaponTypes

namespace Colors {
const Color Green(7);
const Color Purple(16);
const Color Red(111);
const Color Blue(165);
const Color White(255);
const Color Yellow(135);
const Color Orange(149);
const Color Cyan(164);
const Color Brown(19);
const Color Grey(74);
} // namespace Colors

namespace Text {
const char White = '\x04';
const char Yellow = '\x03';
const char Green = '\x07';
const char Red = '\x08';
namespace Size {
const int Small = 0;
const int Large = 2;
} // namespace Size
} // namespace Text

namespace Positions {
const Position None(-1, -1);
const Position Unknown(-2, -2);
const Position Invalid(-3, -3);
const Position Origin(0, 0);
} // namespace Positions

namespace TilePositions {
const TilePosition None(-1, -1);
const TilePosition Unknown(-2, -2);
const TilePosition Invalid(-3, -3);
const TilePosition Origin(0, 0);
} // namespace TilePositions

namespace WalkPositions {
const WalkPosition None(-1, -1);
const WalkPosition Unknown(-2, -2);
const WalkPosition Invalid(-3, -3);
const WalkPosition Origin(0, 0);
} // namespace WalkPositions

namespace UnitCommandTypes {
const UnitCommandType None("None");
const UnitCommandType Move("Move");
const UnitCommandType Patrol("Patrol");
const UnitCommandType Attack_Unit("Attack Unit");
const UnitCommandType Attack_Move("Attack Move");
const UnitCommandType Right_Click_Unit("Right Click Unit");
const UnitCommandType Build("Build");
const UnitCommandType Build_Addon("Build Addon");
const UnitCommandType Train("Train");
const UnitCommandType Research("Research");
const UnitCommandType Upgrade("Upgrade");
const UnitCommandType Repair("Repair");
const UnitCommandType Stop("Stop");
const UnitCommandType Hold_Position("Hold Position");
const UnitCommandType Gather("Gather");
const UnitCommandType Return_Cargo("Return Cargo");
const UnitCommandType Use_Tech("Use Tech");
const UnitCommandType Use_Tech_Unit("Use Tech Unit");
const UnitCommandType Use_Tech_Position("Use Tech Position");
const UnitCommandType Land("Land");
} // namespace UnitCommandTypes

namespace BulletTypes {
const BulletType None("None");
const BulletType Fusion_Cutter_Hit("Fusion Cutter Hit");
const BulletType EMP_Missile("EMP Missile");
const BulletType Gauss_Rifle_Hit("Gauss Rifle Hit");
const BulletType Invisible("Invisible");
const BulletType Psionic_Storm("Psionic Storm");
} // namespace BulletTypes

UnitFilter::UnitFilter()
    : predicate_([](Unit) { return true; })
{
}

UnitFilter::UnitFilter(std::function<bool(Unit)> predicate)
    : predicate_(std::move(predicate))
{
}

bool UnitFilter::operator()(Unit unit) const
{
    return predicate_ ? predicate_(unit) : true;
}

UnitFilter UnitFilter::operator!() const
{
    UnitFilter self = *this;
    return UnitFilter([self](Unit unit) {
        return !self(unit);
    });
}

UnitFilter operator&&(UnitFilter left, UnitFilter right)
{
    return UnitFilter([left, right](Unit unit) {
        return left(unit) && right(unit);
    });
}

UnitFilter operator||(UnitFilter left, UnitFilter right)
{
    return UnitFilter([left, right](Unit unit) {
        return left(unit) || right(unit);
    });
}

namespace Filter {
const UnitFilter IsEnemy([](Unit unit) {
    return unit && unit->getPlayer() && !unit->getPlayer()->isNeutral()
        && (unit->getPlayer()->getID() > 1 || containsText(unit->getPlayer()->getName(), "enemy"));
});
const UnitFilter IsAlly([](Unit unit) {
    return unit && unit->getPlayer() && !unit->getPlayer()->isNeutral()
        && !containsText(unit->getPlayer()->getName(), "enemy") && unit->getPlayer()->getID() <= 1;
});
const UnitFilter IsNeutral([](Unit unit) {
    return unit && unit->getPlayer() && unit->getPlayer()->isNeutral();
});
const UnitFilter IsOwned([](Unit unit) {
    return unit && unit->getPlayer() && !unit->getPlayer()->isNeutral();
});
const UnitFilter IsBuilding([](Unit unit) {
    return unit && unit->isBuilding();
});
const UnitFilter IsWorker([](Unit unit) {
    return unit && unit->getType().isWorker();
});
const UnitFilter IsMineralField([](Unit unit) {
    return unit && unit->isMineralField();
});
const UnitFilter IsRefinery([](Unit unit) {
    return unit && unit->isRefinery();
});
const UnitFilter IsFlying([](Unit unit) {
    return unit && unit->isFlying();
});
const UnitFilter IsFlyingBuilding([](Unit unit) {
    return unit && unit->isFlyingBuilding();
});
const UnitFilter IsSpell([](Unit unit) {
    return unit && unit->getType().isSpell();
});
const UnitFilter IsCompleted([](Unit unit) {
    return unit && unit->isCompleted();
});
const UnitFilter IsVisible([](Unit unit) {
    return unit && unit->isVisible();
});
const UnitFilter CanAttack([](Unit unit) {
    return unit && unit->canAttack();
});
} // namespace Filter

Race UnitType::getRace() const
{
    if (containsText(name_, "Terran")) {
        return Races::Terran;
    }
    if (containsText(name_, "Zerg")) {
        return Races::Zerg;
    }
    if (containsText(name_, "Protoss")) {
        return Races::Protoss;
    }
    return Races::Unknown;
}

bool UnitType::isWorker() const
{
    return matchesAny(name_, {"Terran SCV", "Zerg Drone", "Protoss Probe"});
}

bool UnitType::isBuilding() const
{
    return matchesAny(
        name_,
        {
            "Terran Command Center",
            "Terran Comsat Station",
            "Terran Nuclear Silo",
            "Terran Supply Depot",
            "Terran Barracks",
            "Terran Bunker",
            "Terran Missile Turret",
            "Terran Refinery",
            "Terran Engineering Bay",
            "Terran Academy",
            "Terran Armory",
            "Terran Factory",
            "Terran Machine Shop",
            "Terran Starport",
            "Terran Control Tower",
            "Terran Science Facility",
            "Terran Covert Ops",
            "Terran Physics Lab",
            "Zerg Hatchery",
            "Zerg Lair",
            "Zerg Hive",
            "Zerg Creep Colony",
            "Zerg Sunken Colony",
            "Zerg Spore Colony",
            "Zerg Extractor",
            "Zerg Spawning Pool",
            "Zerg Evolution Chamber",
            "Zerg Hydralisk Den",
            "Zerg Spire",
            "Zerg Greater Spire",
            "Zerg Queens Nest",
            "Zerg Nydus Canal",
            "Zerg Defiler Mound",
            "Zerg Ultralisk Cavern",
            "Zerg Infested Command Center",
            "Protoss Nexus",
            "Protoss Pylon",
            "Protoss Assimilator",
            "Protoss Gateway",
            "Protoss Forge",
            "Protoss Photon Cannon",
            "Protoss Cybernetics Core",
            "Protoss Robotics Facility",
            "Protoss Stargate",
            "Protoss Citadel of Adun",
            "Protoss Robotics Support Bay",
            "Protoss Fleet Beacon",
            "Protoss Templar Archives",
            "Protoss Observatory",
            "Protoss Arbiter Tribunal",
            "Protoss Shield Battery"
        }
    ) || isResourceDepot() || isRefinery() || isMineralField();
}

bool UnitType::isAddon() const
{
    return matchesAny(
        name_,
        {
            "Terran Comsat Station",
            "Terran Nuclear Silo",
            "Terran Machine Shop",
            "Terran Control Tower",
            "Terran Covert Ops",
            "Terran Physics Lab"
        }
    );
}

bool UnitType::isResourceDepot() const
{
    return matchesAny(
        name_,
        {"Terran Command Center", "Zerg Hatchery", "Zerg Lair", "Zerg Hive", "Protoss Nexus"}
    );
}

bool UnitType::isMineralField() const
{
    return containsText(name_, "Mineral Field");
}

bool UnitType::isRefinery() const
{
    return matchesAny(
        name_,
        {"Terran Refinery", "Zerg Extractor", "Protoss Assimilator", "Resource Vespene Geyser"}
    );
}

bool UnitType::isFlyer() const
{
    return matchesAny(
        name_,
        {
            "Terran Wraith",
            "Terran Dropship",
            "Terran Science Vessel",
            "Terran Battlecruiser",
            "Terran Valkyrie",
            "Zerg Mutalisk",
            "Zerg Guardian",
            "Zerg Scourge",
            "Zerg Queen",
            "Zerg Overlord",
            "Zerg Devourer",
            "Protoss Shuttle",
            "Protoss Scout",
            "Protoss Observer",
            "Protoss Carrier",
            "Protoss Interceptor",
            "Protoss Arbiter",
            "Protoss Corsair"
        }
    );
}

bool UnitType::isFlyingBuilding() const
{
    return false;
}

bool UnitType::isSpell() const
{
    return containsText(name_, "Spell") || containsText(name_, "Scanner Sweep") || containsText(name_, "Nuclear Missile");
}

bool UnitType::isSpellcaster() const
{
    return matchesAny(
        name_,
        {
            "Terran Science Vessel",
            "Terran Ghost",
            "Zerg Queen",
            "Zerg Defiler",
            "Protoss High Templar",
            "Protoss Arbiter",
            "Protoss Dark Archon"
        }
    );
}

bool UnitType::isDetector() const
{
    return matchesAny(
        name_,
        {
            "Terran Missile Turret",
            "Terran Science Vessel",
            "Zerg Overlord",
            "Zerg Spore Colony",
            "Protoss Photon Cannon",
            "Protoss Observer",
            "Protoss Observatory"
        }
    );
}

bool UnitType::isSpecialBuilding() const
{
    return containsText(name_, "Special");
}

bool UnitType::isCritter() const
{
    return containsText(name_, "Critter");
}

bool UnitType::isBurrowable() const
{
    return matchesAny(
        name_,
        {
            "Zerg Drone",
            "Zerg Zergling",
            "Zerg Hydralisk",
            "Zerg Lurker",
            "Zerg Defiler",
            "Zerg Ultralisk",
            "Terran Vulture Spider Mine"
        }
    );
}

bool UnitType::isMechanical() const
{
    return getRace() == Races::Terran && !isWorker() && !matchesAny(name_, {"Terran Marine", "Terran Firebat", "Terran Medic", "Terran Ghost"});
}

bool UnitType::isNeutral() const
{
    return isMineralField() || isRefinery() || isSpecialBuilding() || isCritter();
}

bool UnitType::isTwoUnitsInOneEgg() const
{
    return matchesAny(name_, {"Zerg Zergling", "Zerg Scourge"});
}

bool UnitType::requiresPsi() const
{
    return getRace() == Races::Protoss && isBuilding() && !matchesAny(name_, {"Protoss Nexus", "Protoss Pylon", "Protoss Assimilator"});
}

bool UnitType::isInvincible() const
{
    return false;
}

bool UnitType::canProduce() const
{
    return isResourceDepot()
        || matchesAny(
            name_,
            {
                "Terran Barracks",
                "Terran Factory",
                "Terran Starport",
                "Zerg Hatchery",
                "Zerg Lair",
                "Zerg Hive",
                "Protoss Nexus",
                "Protoss Gateway",
                "Protoss Robotics Facility",
                "Protoss Stargate"
            }
        );
}

bool UnitType::canAttack() const
{
    return groundWeapon() != WeaponTypes::None || airWeapon() != WeaponTypes::None;
}

int UnitType::mineralPrice() const
{
    if (matchesAny(name_, {"Terran SCV", "Zerg Drone", "Protoss Probe"})) {
        return 50;
    }
    if (matchesAny(name_, {"Terran Marine"})) {
        return 50;
    }
    if (matchesAny(name_, {"Terran Supply Depot", "Terran Refinery"})) {
        return 100;
    }
    if (matchesAny(name_, {"Terran Barracks"})) {
        return 150;
    }
    if (matchesAny(name_, {"Terran Command Center"})) {
        return 400;
    }
    return 0;
}

int UnitType::gasPrice() const
{
    if (matchesAny(name_, {"Terran Medic", "Terran Siege Tank Tank Mode"})) {
        return 25;
    }
    return 0;
}

int UnitType::supplyRequired() const
{
    if (matchesAny(name_, {"Terran SCV", "Terran Marine", "Zerg Drone", "Protoss Probe"})) {
        return 2;
    }
    if (matchesAny(name_, {"Terran Vulture", "Terran Goliath", "Terran Wraith", "Protoss Dragoon"})) {
        return 4;
    }
    if (matchesAny(name_, {"Terran Siege Tank Tank Mode", "Terran Siege Tank Siege Mode", "Terran Dropship"})) {
        return 4;
    }
    return 0;
}

int UnitType::supplyProvided() const
{
    if (matchesAny(name_, {"Terran Supply Depot", "Zerg Overlord", "Protoss Pylon"})) {
        return 16;
    }
    return 0;
}

int UnitType::maxHitPoints() const
{
    if (matchesAny(name_, {"Terran SCV", "Zerg Drone", "Protoss Probe"})) {
        return 60;
    }
    if (matchesAny(name_, {"Terran Marine", "Zerg Zergling"})) {
        return 40;
    }
    if (matchesAny(name_, {"Terran Vulture"})) {
        return 80;
    }
    if (matchesAny(name_, {"Terran Goliath", "Terran Wraith", "Terran Dropship"})) {
        return 125;
    }
    if (matchesAny(name_, {"Terran Siege Tank Tank Mode", "Terran Siege Tank Siege Mode"})) {
        return 150;
    }
    if (matchesAny(name_, {"Terran Command Center", "Zerg Hatchery", "Zerg Lair", "Zerg Hive", "Protoss Nexus"})) {
        return 1500;
    }
    if (matchesAny(name_, {"Terran Barracks", "Terran Factory", "Terran Starport", "Protoss Gateway"})) {
        return 1000;
    }
    if (isBuilding()) {
        return 500;
    }
    return 100;
}

int UnitType::maxShields() const
{
    if (getRace() != Races::Protoss) {
        return 0;
    }
    if (isBuilding()) {
        return 500;
    }
    if (matchesAny(name_, {"Protoss Zealot", "Protoss Dragoon"})) {
        return 80;
    }
    return 60;
}

int UnitType::buildTime() const
{
    if (matchesAny(name_, {"Terran SCV", "Zerg Drone", "Protoss Probe"})) {
        return 20 * 24;
    }
    if (matchesAny(name_, {"Terran Marine", "Zerg Zergling", "Protoss Zealot"})) {
        return 24 * 24;
    }
    if (matchesAny(name_, {"Terran Supply Depot", "Protoss Pylon"})) {
        return 30 * 24;
    }
    if (matchesAny(name_, {"Terran Barracks", "Protoss Gateway"})) {
        return 60 * 24;
    }
    if (matchesAny(name_, {"Terran Command Center", "Zerg Hatchery", "Protoss Nexus"})) {
        return 120 * 24;
    }
    return isBuilding() ? 60 * 24 : 30 * 24;
}

bool UnitType::canBuildAddon() const
{
    return matchesAny(
        name_,
        {
            "Terran Command Center",
            "Terran Factory",
            "Terran Starport",
            "Terran Science Facility"
        }
    );
}

int UnitType::spaceProvided() const
{
    if (matchesAny(name_, {"Terran Dropship"})) {
        return 8;
    }
    if (matchesAny(name_, {"Terran Bunker"})) {
        return 4;
    }
    if (matchesAny(name_, {"Protoss Shuttle"})) {
        return 8;
    }
    if (matchesAny(name_, {"Zerg Overlord"})) {
        return 8;
    }
    return 0;
}

int UnitType::spaceRequired() const
{
    if (matchesAny(name_, {"Terran Siege Tank Tank Mode", "Terran Siege Tank Siege Mode", "Terran Goliath"})) {
        return 4;
    }
    if (matchesAny(name_, {"Terran Vulture", "Protoss Dragoon"})) {
        return 2;
    }
    if (!isBuilding() && !isFlyer() && !isSpell() && !isMineralField()) {
        return 1;
    }
    return 0;
}

int UnitType::width() const
{
    return tileWidth() * TILE_SIZE;
}

int UnitType::height() const
{
    return tileHeight() * TILE_SIZE;
}

bool UnitType::isCloakable() const
{
    return matchesAny(
        name_,
        {
            "Terran Wraith",
            "Terran Ghost",
            "Protoss Dark Templar",
            "Protoss Observer",
            "Protoss Arbiter"
        }
    );
}

int UnitType::tileWidth() const
{
    if (isResourceDepot()) {
        return 4;
    }
    if (matchesAny(name_, {"Terran Barracks", "Terran Factory", "Terran Starport"})) {
        return 4;
    }
    if (matchesAny(name_, {"Terran Supply Depot", "Terran Refinery", "Terran Engineering Bay", "Terran Academy", "Terran Armory"})) {
        return 3;
    }
    if (matchesAny(name_, {"Terran Science Facility"})) {
        return 4;
    }
    if (isAddon()) {
        return 2;
    }
    return 1;
}

int UnitType::tileHeight() const
{
    if (isResourceDepot()) {
        return 3;
    }
    if (matchesAny(name_, {"Terran Barracks", "Terran Factory", "Terran Starport"})) {
        return 3;
    }
    if (matchesAny(name_, {"Terran Supply Depot", "Terran Refinery", "Terran Engineering Bay", "Terran Academy", "Terran Armory"})) {
        return 2;
    }
    if (matchesAny(name_, {"Terran Science Facility"})) {
        return 3;
    }
    if (isAddon()) {
        return 2;
    }
    return 1;
}

TilePosition UnitType::tileSize() const
{
    return {tileWidth(), tileHeight()};
}

int UnitType::sightRange() const
{
    if (isDetector()) {
        return 10 * TILE_SIZE;
    }
    if (isFlyer()) {
        return 8 * TILE_SIZE;
    }
    return 7 * TILE_SIZE;
}

int UnitType::seekRange() const
{
    return sightRange();
}

int UnitType::maxEnergy() const
{
    return isSpellcaster() || matchesAny(name_, {"Terran Comsat Station"}) ? 200 : 0;
}

double UnitType::topSpeed() const
{
    if (isBuilding() || isMineralField()) {
        return 0.0;
    }
    if (matchesAny(name_, {"Zerg Zergling", "Terran Vulture"})) {
        return 6.0;
    }
    if (isFlyer()) {
        return 5.0;
    }
    return 4.0;
}

UnitSizeType UnitType::size() const
{
    if (matchesAny(name_, {"Terran Siege Tank Tank Mode", "Terran Siege Tank Siege Mode", "Terran Goliath", "Protoss Dragoon", "Protoss Archon"})) {
        return UnitSizeTypes::Large;
    }
    if (matchesAny(name_, {"Terran Marine", "Terran SCV", "Zerg Zergling", "Zerg Drone", "Protoss Probe"})) {
        return UnitSizeTypes::Small;
    }
    return UnitSizeTypes::Medium;
}

int UnitType::dimensionLeft() const { return tileWidth() * TILE_SIZE / 2; }
int UnitType::dimensionRight() const { return tileWidth() * TILE_SIZE / 2; }
int UnitType::dimensionUp() const { return tileHeight() * TILE_SIZE / 2; }
int UnitType::dimensionDown() const { return tileHeight() * TILE_SIZE / 2; }

WeaponType UnitType::airWeapon() const
{
    if (matchesAny(name_, {"Terran Goliath"})) {
        return WeaponTypes::Hellfire_Missile_Pack;
    }
    if (matchesAny(name_, {"Terran Wraith"})) {
        return WeaponTypes::Gemini_Missiles;
    }
    if (matchesAny(name_, {"Terran Battlecruiser"})) {
        return WeaponTypes::ATA_Laser_Battery;
    }
    if (matchesAny(name_, {"Zerg Hydralisk"})) {
        return WeaponTypes::Needle_Spines;
    }
    if (matchesAny(name_, {"Zerg Mutalisk", "Zerg Guardian"})) {
        return WeaponTypes::Glave_Wurm;
    }
    if (matchesAny(name_, {"Zerg Spore Colony"})) {
        return WeaponTypes::Seeker_Spores;
    }
    if (matchesAny(name_, {"Protoss Dragoon"})) {
        return WeaponTypes::Phase_Disruptor;
    }
    if (matchesAny(name_, {"Protoss Photon Cannon"})) {
        return WeaponTypes::Photon_Cannon;
    }
    return WeaponTypes::None;
}

WeaponType UnitType::groundWeapon() const
{
    if (matchesAny(name_, {"Terran SCV"})) {
        return WeaponTypes::Gauss_Rifle;
    }
    if (matchesAny(name_, {"Terran Marine"})) {
        return WeaponTypes::Gauss_Rifle;
    }
    if (matchesAny(name_, {"Terran Ghost"})) {
        return WeaponTypes::C_10_Canister_Rifle;
    }
    if (matchesAny(name_, {"Terran Firebat"})) {
        return WeaponTypes::Fragmentation_Grenade;
    }
    if (matchesAny(name_, {"Terran Vulture"})) {
        return WeaponTypes::Fragmentation_Grenade;
    }
    if (matchesAny(name_, {"Terran Vulture Spider Mine"})) {
        return WeaponTypes::Spider_Mines;
    }
    if (matchesAny(name_, {"Terran Siege Tank Tank Mode"})) {
        return WeaponTypes::Arclite_Cannon;
    }
    if (matchesAny(name_, {"Terran Siege Tank Siege Mode"})) {
        return WeaponTypes::Arclite_Shock_Cannon;
    }
    if (matchesAny(name_, {"Terran Goliath"})) {
        return WeaponTypes::Twin_Autocannons;
    }
    if (matchesAny(name_, {"Terran Wraith"})) {
        return WeaponTypes::Burst_Lasers;
    }
    if (matchesAny(name_, {"Terran Battlecruiser"})) {
        return WeaponTypes::ATS_Laser_Battery;
    }
    if (matchesAny(name_, {"Terran Bunker", "Terran Missile Turret"})) {
        return WeaponTypes::Gauss_Rifle;
    }
    if (matchesAny(name_, {"Zerg Hydralisk"})) {
        return WeaponTypes::Needle_Spines;
    }
    if (matchesAny(name_, {"Zerg Lurker"})) {
        return WeaponTypes::Subterranean_Spines;
    }
    if (matchesAny(name_, {"Zerg Mutalisk"})) {
        return WeaponTypes::Glave_Wurm;
    }
    if (matchesAny(name_, {"Zerg Sunken Colony"})) {
        return WeaponTypes::Subterranean_Spines;
    }
    if (matchesAny(name_, {"Protoss Zealot"})) {
        return WeaponTypes::Psi_Blades;
    }
    if (matchesAny(name_, {"Protoss Dragoon"})) {
        return WeaponTypes::Phase_Disruptor;
    }
    if (matchesAny(name_, {"Protoss Photon Cannon"})) {
        return WeaponTypes::Photon_Cannon;
    }
    return WeaponTypes::None;
}

std::pair<UnitType, int> UnitType::whatBuilds() const
{
    if (matchesAny(name_, {"Terran SCV"})) {
        return {UnitTypes::Terran_Command_Center, 1};
    }
    if (getRace() == Races::Terran && isBuilding() && !isAddon()) {
        return {UnitTypes::Terran_SCV, 1};
    }
    if (getRace() == Races::Terran && !isBuilding()) {
        if (matchesAny(name_, {"Terran Marine", "Terran Firebat", "Terran Medic", "Terran Ghost"})) {
            return {UnitTypes::Terran_Barracks, 1};
        }
        if (matchesAny(name_, {"Terran Vulture", "Terran Siege Tank Tank Mode", "Terran Siege Tank Siege Mode", "Terran Goliath"})) {
            return {UnitTypes::Terran_Factory, 1};
        }
        if (isFlyer()) {
            return {UnitTypes::Terran_Starport, 1};
        }
    }
    if (isAddon()) {
        if (matchesAny(name_, {"Terran Comsat Station", "Terran Nuclear Silo"})) {
            return {UnitTypes::Terran_Command_Center, 1};
        }
        if (matchesAny(name_, {"Terran Machine Shop"})) {
            return {UnitTypes::Terran_Factory, 1};
        }
        if (matchesAny(name_, {"Terran Control Tower"})) {
            return {UnitTypes::Terran_Starport, 1};
        }
        return {UnitTypes::Terran_Science_Facility, 1};
    }
    if (getRace() == Races::Zerg) {
        return {isBuilding() ? UnitTypes::Zerg_Drone : UnitTypes::Zerg_Larva, 1};
    }
    if (getRace() == Races::Protoss && isBuilding()) {
        return {UnitTypes::Protoss_Probe, 1};
    }
    if (getRace() == Races::Protoss && !isBuilding()) {
        return {UnitTypes::Protoss_Gateway, 1};
    }
    return {UnitTypes::None, 0};
}

std::vector<UnitType> UnitType::buildsWhat() const
{
    if (matchesAny(name_, {"Terran Command Center"})) {
        return {UnitTypes::Terran_SCV, UnitTypes::Terran_Comsat_Station, UnitTypes::Terran_Nuclear_Silo};
    }
    if (matchesAny(name_, {"Terran Factory"})) {
        return {UnitTypes::Terran_Vulture, UnitTypes::Terran_Siege_Tank_Tank_Mode, UnitTypes::Terran_Goliath, UnitTypes::Terran_Machine_Shop};
    }
    if (matchesAny(name_, {"Terran Starport"})) {
        return {UnitTypes::Terran_Wraith, UnitTypes::Terran_Dropship, UnitTypes::Terran_Science_Vessel, UnitTypes::Terran_Control_Tower};
    }
    if (matchesAny(name_, {"Terran Science Facility"})) {
        return {UnitTypes::Terran_Covert_Ops, UnitTypes::Terran_Physics_Lab};
    }
    return {};
}

const std::map<UnitType, int>& UnitType::requiredUnits() const
{
    static const std::map<UnitType, int> empty;
    return empty;
}

TechType UnitType::requiredTech() const
{
    return TechTypes::None;
}

Race TechType::getRace() const
{
    if (containsText(name_, "Lurker")) {
        return Races::Zerg;
    }
    return Races::Terran;
}

int TechType::mineralPrice() const
{
    return name_ == "None" || name_ == "Unknown" ? 0 : 100;
}

int TechType::gasPrice() const
{
    return name_ == "None" || name_ == "Unknown" ? 0 : 100;
}

int TechType::energyCost() const
{
    if (matchesAny(name_, {"Scanner Sweep"})) {
        return 50;
    }
    if (matchesAny(name_, {"Defensive Matrix", "Irradiate", "EMP Shockwave"})) {
        return 100;
    }
    return 0;
}

UnitType TechType::whatResearches() const
{
    if (matchesAny(name_, {"Tank Siege Mode", "Spider Mines"})) {
        return UnitTypes::Terran_Machine_Shop;
    }
    if (matchesAny(name_, {"Cloaking Field"})) {
        return UnitTypes::Terran_Control_Tower;
    }
    if (matchesAny(name_, {"EMP Shockwave", "Irradiate", "Defensive Matrix"})) {
        return UnitTypes::Terran_Science_Facility;
    }
    if (matchesAny(name_, {"Lurker Aspect"})) {
        return UnitTypes::Zerg_Hydralisk;
    }
    return UnitTypes::None;
}

UnitType TechType::requiredUnit() const
{
    return whatResearches();
}

Race UpgradeType::getRace() const
{
    if (matchesAny(name_, {"Metabolic Boost", "Muscular Augments", "Grooved Spines", "Ventral Sacs"})) {
        return Races::Zerg;
    }
    if (matchesAny(name_, {"Singularity Charge", "Leg Enhancements"})) {
        return Races::Protoss;
    }
    return Races::Terran;
}

int UpgradeType::mineralPrice() const
{
    return name_ == "None" || name_ == "Unknown" ? 0 : 100;
}

int UpgradeType::gasPrice() const
{
    return name_ == "None" || name_ == "Unknown" ? 0 : 100;
}

int UpgradeType::upgradeTime() const
{
    return 24 * 120;
}

UnitType UpgradeType::whatUpgrades() const
{
    if (matchesAny(name_, {"Terran Vehicle Weapons", "Terran Vehicle Plating", "Charon Boosters"})) {
        return UnitTypes::Terran_Armory;
    }
    if (matchesAny(name_, {"Ion Thrusters"})) {
        return UnitTypes::Terran_Machine_Shop;
    }
    return UnitTypes::None;
}

UnitType UpgradeType::whatsRequired(int) const
{
    return whatUpgrades();
}

PlayerInterface::PlayerInterface(LAVBWAPIRM::PlayerSnapshot snapshot)
    : snapshot_(std::move(snapshot))
{
}

int PlayerInterface::getID() const { return snapshot_.id; }
std::string PlayerInterface::getName() const { return snapshot_.name; }
Race PlayerInterface::getRace() const { return Race(snapshot_.race); }
int PlayerInterface::minerals() const { return snapshot_.minerals; }
int PlayerInterface::gas() const { return snapshot_.gas; }
int PlayerInterface::supplyUsed() const { return snapshot_.supplyUsed; }
int PlayerInterface::supplyTotal() const { return snapshot_.supplyTotal; }
int PlayerInterface::gatheredMinerals() const { return snapshot_.minerals; }
int PlayerInterface::gatheredGas() const { return snapshot_.gas; }
bool PlayerInterface::hasResearched(TechType tech) const
{
    return namedListContains(snapshot_.researchedTechs, tech.getName());
}

bool PlayerInterface::isResearching(TechType tech) const
{
    return namedListContains(snapshot_.researchingTechs, tech.getName());
}

bool PlayerInterface::isUpgrading(UpgradeType upgrade) const
{
    return namedListContains(snapshot_.upgradingUpgrades, upgrade.getName());
}

int PlayerInterface::getUpgradeLevel(UpgradeType upgrade) const
{
    return namedLevelValue(snapshot_.upgradeLevels, upgrade.getName());
}

int PlayerInterface::getMaxUpgradeLevel(UpgradeType upgrade) const
{
    if (upgrade == UpgradeTypes::None || upgrade == UpgradeTypes::Unknown) {
        return 0;
    }
    if (matchesAny(
            upgrade.getName(),
            {
                "Terran Vehicle Weapons",
                "Terran Vehicle Plating"
            }
        )) {
        return 3;
    }
    return 1;
}
int PlayerInterface::weaponMaxRange(WeaponType weapon) const { return weapon.maxRange(); }
int PlayerInterface::weaponDamageCooldown(WeaponType weapon) const { return weapon.damageCooldown(); }
int PlayerInterface::weaponDamageCooldown(UnitType type) const { return type.groundWeapon().damageCooldown(); }
Color PlayerInterface::getTextColor() const { return isEnemy(Broodwar ? Broodwar->self() : nullptr) ? Colors::Red : Colors::Green; }
Force PlayerInterface::getForce() const
{
    static ForceInterface force;
    return &force;
}
bool PlayerInterface::isDefeated() const { return false; }
bool PlayerInterface::leftGame() const { return false; }
TilePosition PlayerInterface::getStartLocation() const { return toBwapTile(snapshot_.startLocation); }
Unitset PlayerInterface::getUnits() const { return units_; }

int PlayerInterface::allUnitCount(UnitType type) const
{
    int count = 0;
    for (Unit unit : units_) {
        if (unit && unitTypeMatches(unit->getType(), type)) {
            ++count;
        }
    }
    return count;
}

int PlayerInterface::completedUnitCount(UnitType type) const
{
    int count = 0;
    for (Unit unit : units_) {
        if (unit && unit->isCompleted() && unitTypeMatches(unit->getType(), type)) {
            ++count;
        }
    }
    return count;
}

int PlayerInterface::incompleteUnitCount(UnitType type) const
{
    int count = 0;
    for (Unit unit : units_) {
        if (unit && !unit->isCompleted() && unitTypeMatches(unit->getType(), type)) {
            ++count;
        }
    }
    return count;
}

int PlayerInterface::visibleUnitCount(UnitType type) const
{
    int count = 0;
    for (Unit unit : units_) {
        if (unit && unit->isVisible() && unitTypeMatches(unit->getType(), type)) {
            ++count;
        }
    }
    return count;
}

int PlayerInterface::maxEnergy(UnitType type) const
{
    return type.maxEnergy();
}

double PlayerInterface::topSpeed(UnitType type) const
{
    return type.topSpeed();
}

bool PlayerInterface::isEnemy(Player player) const
{
    return player && player->getID() != getID() && !player->isNeutral() && !isNeutral();
}

bool PlayerInterface::isAlly(Player player) const
{
    return player && player->getID() == getID();
}

bool PlayerInterface::isNeutral() const
{
    return snapshot_.id == 0 || containsText(snapshot_.name, "Neutral");
}

void PlayerInterface::update(LAVBWAPIRM::PlayerSnapshot snapshot)
{
    snapshot_ = std::move(snapshot);
}

void PlayerInterface::setUnits(Unitset units)
{
    units_ = std::move(units);
}

UnitInterface::UnitInterface(LAVBWAPIRM::UnitSnapshot snapshot, Player player)
    : snapshot_(std::move(snapshot))
    , player_(player)
{
}

int UnitInterface::getID() const { return snapshot_.id; }
UnitType UnitInterface::getType() const { return UnitType(snapshot_.type); }
Player UnitInterface::getPlayer() const { return player_; }
Position UnitInterface::getPosition() const { return toBWAPI(snapshot_.position); }
TilePosition UnitInterface::getTilePosition() const
{
    return {snapshot_.position.x / 32, snapshot_.position.y / 32};
}
int UnitInterface::getHitPoints() const { return snapshot_.hitPoints; }
int UnitInterface::getInitialHitPoints() const { return getType().maxHitPoints(); }
Position UnitInterface::getInitialPosition() const { return getPosition(); }
TilePosition UnitInterface::getInitialTilePosition() const { return getTilePosition(); }
UnitType UnitInterface::getInitialType() const { return getType(); }
int UnitInterface::getShields() const { return snapshot_.shields; }
int UnitInterface::getEnergy() const { return snapshot_.energy; }
int UnitInterface::getResources() const { return snapshot_.resources; }
int UnitInterface::getInitialResources() const { return snapshot_.resources; }
int UnitInterface::getSpaceRemaining() const { return getType().spaceProvided(); }
Unitset UnitInterface::getLoadedUnits() const { return {}; }

int UnitInterface::getDistance(Unit target) const
{
    if (!target) {
        return 0;
    }
    return getDistance(target->getPosition());
}

int UnitInterface::getDistance(Position position) const
{
    return getPosition().getApproxDistance(position);
}

bool UnitInterface::exists() const { return snapshot_.id != 0; }
bool UnitInterface::isCompleted() const { return snapshot_.completed; }
bool UnitInterface::isVisible(Player) const { return snapshot_.visible; }
bool UnitInterface::isSelected() const { return snapshot_.selected; }
bool UnitInterface::isIdle() const { return snapshot_.idle || snapshot_.order.empty(); }
bool UnitInterface::isFlying() const { return snapshot_.flying; }
bool UnitInterface::isGatheringMinerals() const { return containsText(snapshot_.order, "mineral"); }
bool UnitInterface::isGatheringGas() const { return containsText(snapshot_.order, "gas") || containsText(snapshot_.order, "vespene"); }
bool UnitInterface::isCarryingMinerals() const { return containsText(snapshot_.order, "carry mineral"); }
bool UnitInterface::isCarryingGas() const { return containsText(snapshot_.order, "carry gas") || containsText(snapshot_.order, "carry vespene"); }
bool UnitInterface::isBeingGathered() const { return isMineralField() || getType() == UnitTypes::Resource_Vespene_Geyser; }
bool UnitInterface::isConstructing() const { return containsText(snapshot_.order, "construct"); }
bool UnitInterface::isBeingConstructed() const { return !snapshot_.completed && getType().isBuilding(); }
bool UnitInterface::isTraining() const { return containsText(snapshot_.order, "train"); }
bool UnitInterface::isResearching() const { return containsText(snapshot_.order, "research"); }
bool UnitInterface::isUpgrading() const { return containsText(snapshot_.order, "upgrade"); }
bool UnitInterface::isPowered() const { return true; }
bool UnitInterface::isLifted() const { return false; }
bool UnitInterface::isCloaked() const { return containsText(snapshot_.order, "cloak"); }
bool UnitInterface::isUnderAttack() const { return false; }
bool UnitInterface::isMorphing() const { return containsText(snapshot_.order, "morph"); }
bool UnitInterface::isDetected() const { return true; }
bool UnitInterface::isAttacking() const { return containsText(snapshot_.order, "attack"); }
bool UnitInterface::isDefenseMatrixed() const { return containsText(snapshot_.order, "defensive matrix"); }
bool UnitInterface::isBurrowed() const { return containsText(snapshot_.order, "burrow"); }
bool UnitInterface::isHoldingPosition() const { return containsText(snapshot_.order, "hold"); }
bool UnitInterface::isMoving() const { return containsText(snapshot_.order, "move"); }
bool UnitInterface::isRepairing() const { return containsText(snapshot_.order, "repair"); }
bool UnitInterface::isPatrolling() const { return containsText(snapshot_.order, "patrol"); }
bool UnitInterface::isUnderDarkSwarm() const { return false; }
bool UnitInterface::isStasised() const { return false; }
bool UnitInterface::isIrradiated() const { return false; }
bool UnitInterface::isStuck() const { return false; }
bool UnitInterface::isUnderDisruptionWeb() const { return false; }
bool UnitInterface::isUnderStorm() const { return false; }
bool UnitInterface::isInWeaponRange(Unit target) const
{
    if (!target) {
        return false;
    }
    const WeaponType weapon = target->isFlying() ? getType().airWeapon() : getType().groundWeapon();
    return weapon != WeaponTypes::None && getDistance(target) <= weapon.maxRange();
}
bool UnitInterface::canMove() const { return !getType().isBuilding() && !getType().isMineralField(); }
bool UnitInterface::canAttack() const { return exists() && !getType().isMineralField(); }
bool UnitInterface::canGather() const { return getType().isWorker(); }
bool UnitInterface::canBuildAddon() const { return getType().canBuildAddon() && !isConstructing() && getAddon() == nullptr; }
bool UnitInterface::canResearch(TechType tech) const
{
    if (!exists() || !isCompleted() || isResearching()) {
        return false;
    }
    if (player_ && (player_->hasResearched(tech) || player_->isResearching(tech))) {
        return false;
    }
    return tech != TechTypes::None && tech != TechTypes::Unknown && getType() == tech.whatResearches();
}

bool UnitInterface::canUpgrade(UpgradeType upgrade) const
{
    if (!exists() || !isCompleted() || isUpgrading()) {
        return false;
    }
    if (player_ && (player_->isUpgrading(upgrade) || player_->getUpgradeLevel(upgrade) >= player_->getMaxUpgradeLevel(upgrade))) {
        return false;
    }
    return upgrade != UpgradeTypes::None && upgrade != UpgradeTypes::Unknown && getType() == upgrade.whatUpgrades();
}
bool UnitInterface::canTrain(UnitType) const { return exists(); }
bool UnitInterface::canPatrol() const { return canMove(); }
bool UnitInterface::canHoldPosition() const { return exists(); }
bool UnitInterface::canCancelConstruction() const { return isConstructing() || isBeingConstructed(); }
bool UnitInterface::canCancelAddon() const { return false; }
bool UnitInterface::canCancelTrain() const { return isTraining(); }
bool UnitInterface::canReturnCargo() const { return isCarryingMinerals() || isCarryingGas(); }
bool UnitInterface::canCommand() const { return exists(); }
bool UnitInterface::isInterruptible() const { return exists(); }
bool UnitInterface::canLift() const { return getType().isBuilding() && getType().getRace() == Races::Terran; }
bool UnitInterface::canSiege() const { return getType() == UnitTypes::Terran_Siege_Tank_Tank_Mode; }
bool UnitInterface::canUnsiege() const { return getType() == UnitTypes::Terran_Siege_Tank_Siege_Mode || isSieged(); }
bool UnitInterface::isSieged() const { return getType() == UnitTypes::Terran_Siege_Tank_Siege_Mode; }
bool UnitInterface::canCloak() const { return matchesAny(getType().getName(), {"Terran Wraith", "Terran Ghost"}); }
bool UnitInterface::isMineralField() const { return getType().isMineralField(); }
bool UnitInterface::isFlyingBuilding() const { return getType().isFlyingBuilding(); }
bool UnitInterface::isRefinery() const { return getType().isRefinery(); }
bool UnitInterface::isBuilding() const { return getType().isBuilding(); }
Unit UnitInterface::getTarget() const { return nullptr; }
Unit UnitInterface::getOrderTarget() const { return nullptr; }
Unit UnitInterface::getBuildUnit() const { return nullptr; }
Unit UnitInterface::getAddon() const
{
    const UnitType parentType = getType();
    if (!player_ || !parentType.canBuildAddon()) {
        return nullptr;
    }

    const TilePosition expectedPosition = getTilePosition() + TilePosition(parentType.tileWidth(), 1);
    for (Unit unit : player_->getUnits()) {
        if (!unit || unit == this) {
            continue;
        }

        const UnitType addonType = unit->getType();
        if (!addonType.isAddon() || addonType.whatBuilds().first != parentType) {
            continue;
        }
        if (unit->getTilePosition() == expectedPosition) {
            return unit;
        }
    }
    return nullptr;
}
Unit UnitInterface::getClosestUnit(UnitFilter filter, int radius) const
{
    return Broodwar->getClosestUnit(getPosition(), std::move(filter), radius);
}
Position UnitInterface::getTargetPosition() const { return getPosition(); }
double UnitInterface::getAngle() const { return 0.0; }
int UnitInterface::getLeft() const { return getPosition().x - getType().dimensionLeft(); }
int UnitInterface::getRight() const { return getPosition().x + getType().dimensionRight(); }
int UnitInterface::getTop() const { return getPosition().y - getType().dimensionUp(); }
int UnitInterface::getBottom() const { return getPosition().y + getType().dimensionDown(); }
double UnitInterface::getVelocityX() const { return 0.0; }
double UnitInterface::getVelocityY() const { return 0.0; }
int UnitInterface::getAirWeaponCooldown() const { return 0; }
int UnitInterface::getGroundWeaponCooldown() const { return 0; }
int UnitInterface::getSpellCooldown() const { return 0; }
int UnitInterface::getSpiderMineCount() const { return 0; }
int UnitInterface::getRemainingBuildTime() const { return 0; }
int UnitInterface::getRemainingTrainTime() const { return 0; }
int UnitInterface::getRemainingResearchTime() const { return 0; }
int UnitInterface::getRemainingUpgradeTime() const { return 0; }
std::vector<UnitType> UnitInterface::getTrainingQueue() const { return {}; }
UnitType UnitInterface::getBuildType() const { return UnitTypes::None; }
UnitCommand UnitInterface::getLastCommand() const
{
    if (containsText(snapshot_.order, "patrol")) {
        return UnitCommand(UnitCommandTypes::Patrol);
    }
    if (containsText(snapshot_.order, "repair")) {
        return UnitCommand(UnitCommandTypes::Repair);
    }
    if (containsText(snapshot_.order, "gather")) {
        return UnitCommand(UnitCommandTypes::Gather);
    }
    if (containsText(snapshot_.order, "return")) {
        return UnitCommand(UnitCommandTypes::Return_Cargo);
    }
    if (containsText(snapshot_.order, "build")) {
        return UnitCommand(UnitCommandTypes::Build);
    }
    if (containsText(snapshot_.order, "tech")) {
        return UnitCommand(UnitCommandTypes::Use_Tech);
    }
    if (containsText(snapshot_.order, "move")) {
        return UnitCommand(UnitCommandTypes::Move);
    }
    if (containsText(snapshot_.order, "attack")) {
        return UnitCommand(UnitCommandTypes::Attack_Unit);
    }
    return UnitCommand(UnitCommandTypes::None);
}
int UnitInterface::getLastCommandFrame() const { return 0; }

bool UnitInterface::train(UnitType type)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Train;
    command.unitName = type.getName();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::build(UnitType type, TilePosition position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Build;
    command.buildingName = type.getName();
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::buildAddon(UnitType type)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Build;
    command.buildingName = type.getName();
    command.payload = "addon";
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::research(TechType tech)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Research;
    command.abilityName = tech.getName();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::upgrade(UpgradeType upgrade)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Upgrade;
    command.abilityName = upgrade.getName();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::move(Position position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Move;
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::attack(Unit target)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Attack;
    command.targetUnitId = target ? target->getID() : 0;
    command.hasTargetUnit = target != nullptr;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::attack(Position position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Attack;
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::rightClick(Unit target)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::RightClick;
    command.targetUnitId = target ? target->getID() : 0;
    command.hasTargetUnit = target != nullptr;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::rightClick(Position position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::RightClick;
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::gather(Unit target)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Gather;
    command.targetUnitId = target ? target->getID() : 0;
    command.hasTargetUnit = target != nullptr;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::repair(Unit target)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Repair;
    command.targetUnitId = target ? target->getID() : 0;
    command.hasTargetUnit = target != nullptr;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::holdPosition(bool)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::HoldPosition;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::returnCargo()
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::RightClick;
    command.payload = "return_cargo";
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::stop()
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Stop;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::patrol(Position position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Move;
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    command.payload = "patrol";
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::lift()
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Move;
    command.payload = "lift";
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::siege()
{
    return useTech(TechType("Tank Siege Mode"));
}

bool UnitInterface::unsiege()
{
    return useTech(TechType("Tank Unsiege"));
}

bool UnitInterface::cloak()
{
    return useTech(TechType("Cloak"));
}

bool UnitInterface::decloak()
{
    return useTech(TechType("Decloak"));
}

bool UnitInterface::land(TilePosition position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::Move;
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    command.payload = "land";
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::canLand() const { return getType().isFlyingBuilding(); }
bool UnitInterface::canLand(bool) const { return canLand(); }
bool UnitInterface::canLand(TilePosition position) const { return position.isValid(); }
bool UnitInterface::unload(Unit) { return true; }
bool UnitInterface::unloadAll() { return true; }
bool UnitInterface::cancelConstruction() { return true; }
bool UnitInterface::cancelAddon() { return true; }
bool UnitInterface::cancelTrain(int) { return true; }
bool UnitInterface::cancelUpgrade() { return true; }
bool UnitInterface::canUnload(Unit) const { return false; }
bool UnitInterface::isLoaded() const { return false; }
Unit UnitInterface::getTransport() const { return nullptr; }
bool UnitInterface::canBurrow(bool) const { return false; }
bool UnitInterface::canUnburrow(bool) const { return false; }
bool UnitInterface::isAttackFrame() const { return false; }

bool UnitInterface::useTech(TechType tech)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::UseTech;
    command.abilityName = tech.getName();
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::useTech(TechType tech, Unit target)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::UseTech;
    command.abilityName = tech.getName();
    command.targetUnitId = target ? target->getID() : 0;
    command.hasTargetUnit = target != nullptr;
    return sendUnitCommand(snapshot_.id, command);
}

bool UnitInterface::useTech(TechType tech, Position position)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::UseTech;
    command.abilityName = tech.getName();
    command.targetPosition = toBridge(position);
    command.hasTargetPosition = position.isValid();
    return sendUnitCommand(snapshot_.id, command);
}

void UnitInterface::update(LAVBWAPIRM::UnitSnapshot snapshot)
{
    snapshot_ = std::move(snapshot);
}

void UnitInterface::setPlayer(Player player)
{
    player_ = player;
}

Game::Game()
    : bridge_(LAVBWAPIRM::getBridge())
{
}

Game::Game(std::shared_ptr<LAVBWAPIRM::Bridge> bridge)
    : bridge_(std::move(bridge))
{
}

bool Game::isConnected()
{
    refresh();
    return snapshot_.connected;
}

bool Game::isInGame()
{
    refresh();
    return snapshot_.inGame;
}

int Game::getFrameCount()
{
    refresh();
    return snapshot_.frameCount;
}

std::string Game::mapName()
{
    refresh();
    return snapshot_.mapName;
}

std::string Game::mapFileName()
{
    refresh();
    return snapshot_.mapName.empty() ? "mock_map.scx" : snapshot_.mapName;
}

int Game::mapWidth()
{
    refresh();
    return snapshot_.mapWidth > 0 ? snapshot_.mapWidth : 128;
}

int Game::mapHeight()
{
    refresh();
    return snapshot_.mapHeight > 0 ? snapshot_.mapHeight : 128;
}

Player Game::self()
{
    refresh();
    return &self_;
}

Player Game::enemy()
{
    refresh();
    return &enemy_;
}

Player Game::neutral()
{
    refresh();
    return &neutral_;
}

Playerset Game::getPlayers()
{
    refresh();
    return {&self_, &enemy_, &neutral_};
}

Forceset Game::getForces()
{
    static ForceInterface force;
    return {&force};
}

Unitset Game::getAllUnits()
{
    refresh();
    Unitset unitSet;
    for (auto& unit : units_) {
        unitSet.insert(unit.get());
    }
    return unitSet;
}

Unitset Game::getMinerals()
{
    refresh();
    Unitset unitSet;
    for (auto& unit : units_) {
        if (unit && unit->isMineralField()) {
            unitSet.insert(unit.get());
        }
    }
    return unitSet;
}

Unitset Game::getNeutralUnits()
{
    refresh();
    return neutral_.getUnits();
}

Unitset Game::getStaticNeutralUnits()
{
    return getNeutralUnits();
}

Bulletset Game::getBullets()
{
    return {};
}

std::vector<Position> Game::getNukeDots()
{
    return {};
}

Unitset Game::getUnitsInRadius(Position center, int radius)
{
    refresh();
    Unitset unitSet;
    const int radiusSquared = radius * radius;
    for (auto& unit : units_) {
        Position position = unit->getPosition();
        const int dx = position.x - center.x;
        const int dy = position.y - center.y;
        if ((dx * dx + dy * dy) <= radiusSquared) {
            unitSet.insert(unit.get());
        }
    }
    return unitSet;
}

Unitset Game::getUnitsInRadius(Position center, int radius, UnitFilter filter)
{
    Unitset filtered;
    for (Unit unit : getUnitsInRadius(center, radius)) {
        if (filter(unit)) {
            filtered.insert(unit);
        }
    }
    return filtered;
}

Unitset Game::getUnitsInRectangle(Position leftTop, Position rightBottom)
{
    refresh();
    Unitset unitSet;
    for (auto& unit : units_) {
        if (!unit) {
            continue;
        }
        const Position position = unit->getPosition();
        if (position.x >= leftTop.x && position.y >= leftTop.y
            && position.x <= rightBottom.x && position.y <= rightBottom.y) {
            unitSet.insert(unit.get());
        }
    }
    return unitSet;
}

Unitset Game::getUnitsInRectangle(Position leftTop, Position rightBottom, UnitFilter filter)
{
    Unitset filtered;
    for (Unit unit : getUnitsInRectangle(leftTop, rightBottom)) {
        if (filter(unit)) {
            filtered.insert(unit);
        }
    }
    return filtered;
}

Unitset Game::getUnitsInRectangle(int left, int top, int right, int bottom)
{
    return getUnitsInRectangle(Position(left, top), Position(right, bottom));
}

Unit Game::getClosestUnit(Position center, UnitFilter filter, int radius)
{
    refresh();
    Unit closest = nullptr;
    int bestDistance = 0;
    for (auto& unit : units_) {
        if (!unit || !filter(unit.get())) {
            continue;
        }
        const int distance = unit->getDistance(center);
        if (radius > 0 && distance > radius) {
            continue;
        }
        if (!closest || distance < bestDistance) {
            closest = unit.get();
            bestDistance = distance;
        }
    }
    return closest;
}

Unitset Game::getUnitsOnTile(TilePosition position)
{
    refresh();
    Unitset unitSet;
    for (auto& unit : units_) {
        if (unit && unit->getTilePosition() == position) {
            unitSet.insert(unit.get());
        }
    }
    return unitSet;
}

Unitset Game::getUnitsOnTile(int tileX, int tileY)
{
    return getUnitsOnTile(TilePosition(tileX, tileY));
}

std::vector<TilePosition> Game::getStartLocations()
{
    refresh();
    std::vector<TilePosition> locations;
    const TilePosition selfStart = self_.getStartLocation();
    const TilePosition enemyStart = enemy_.getStartLocation();
    if (selfStart.isValid()) {
        locations.push_back(selfStart);
    }
    if (enemyStart.isValid() && enemyStart != selfStart) {
        locations.push_back(enemyStart);
    }
    return locations;
}

bool Game::canBuildHere(TilePosition position, UnitType type, Unit, bool)
{
    return position.isValid() && type.isBuilding();
}

bool Game::isReplay()
{
    return false;
}

GameType Game::getGameType()
{
    return GameTypes::Melee;
}

bool Game::isPaused()
{
    return false;
}

void Game::pauseGame()
{
}

void Game::resumeGame()
{
}

bool Game::isVisible(TilePosition position)
{
    return position.isValid();
}

bool Game::isVisible(int tileX, int tileY)
{
    return isVisible(TilePosition(tileX, tileY));
}

bool Game::isExplored(TilePosition position)
{
    return position.isValid();
}

bool Game::isExplored(int tileX, int tileY)
{
    return isExplored(TilePosition(tileX, tileY));
}

bool Game::hasCreep(TilePosition)
{
    return false;
}

bool Game::hasCreep(int, int)
{
    return false;
}

bool Game::isBuildable(int tileX, int tileY, bool)
{
    return !isMockBlockedTile(tileX, tileY, mapWidth(), mapHeight());
}

bool Game::isBuildable(TilePosition position, bool includeBuildings)
{
    return isBuildable(position.x, position.y, includeBuildings);
}

bool Game::isWalkable(TilePosition position)
{
    return isBuildable(position, false);
}

bool Game::isWalkable(WalkPosition position)
{
    const int width = mapWidth();
    const int height = mapHeight();
    if (position.x < 0 || position.y < 0 || position.x >= width * 4 || position.y >= height * 4) {
        return false;
    }
    return !isMockBlockedTile(position.x / 4, position.y / 4, width, height);
}

bool Game::isWalkable(int walkX, int walkY)
{
    return isWalkable(WalkPosition(walkX, walkY));
}

bool Game::hasPath(Position source, Position destination)
{
    return source.isValid() && destination.isValid();
}

bool Game::canMake(UnitType type, Unit)
{
    return type != UnitTypes::None && type != UnitTypes::Unknown;
}

bool Game::canResearch(TechType tech, Unit unit)
{
    if (tech == TechTypes::None || tech == TechTypes::Unknown) {
        return false;
    }
    if (unit) {
        return unit->canResearch(tech);
    }

    Player player = self();
    if (!player || player->hasResearched(tech) || player->isResearching(tech)) {
        return false;
    }
    for (Unit candidate : player->getUnits()) {
        if (candidate && candidate->canResearch(tech)) {
            return true;
        }
    }
    return false;
}

bool Game::canUpgrade(UpgradeType upgrade, Unit unit)
{
    if (upgrade == UpgradeTypes::None || upgrade == UpgradeTypes::Unknown) {
        return false;
    }
    if (unit) {
        return unit->canUpgrade(upgrade);
    }

    Player player = self();
    if (!player || player->isUpgrading(upgrade) || player->getUpgradeLevel(upgrade) >= player->getMaxUpgradeLevel(upgrade)) {
        return false;
    }
    for (Unit candidate : player->getUnits()) {
        if (candidate && candidate->canUpgrade(upgrade)) {
            return true;
        }
    }
    return false;
}

int Game::getDamageFrom(UnitType fromType, UnitType toType, Player, Player)
{
    const WeaponType weapon = toType.isFlyer() ? fromType.airWeapon() : fromType.groundWeapon();
    return weapon == WeaponTypes::None ? 0 : weapon.damageAmount();
}

bool Game::sendText(const char* text)
{
    LAVBWAPIRM::Command command;
    command.type = LAVBWAPIRM::CommandType::LogOnly;
    command.payload = text ? text : "";
    return bridge_->sendCommand(command);
}

bool Game::sendText(const char* format, const char* text)
{
    std::string payload = format ? format : "";
    const std::string replacement = text ? text : "";
    const size_t marker = payload.find("%s");
    if (marker != std::string::npos) {
        payload.replace(marker, 2, replacement);
    } else if (!replacement.empty()) {
        payload += replacement;
    }
    return sendText(payload.c_str());
}

void Game::printf(const char* text)
{
    sendText(text);
}

void Game::drawTextScreen(int, int, const char*, ...)
{
}

void Game::drawTextMap(Position, const char*, ...)
{
}

void Game::drawTextMap(int, int, const char*, ...)
{
}

void Game::drawBoxMap(Position, Position, Color, bool)
{
}

void Game::drawBoxMap(int, int, int, int, Color, bool)
{
}

void Game::drawCircleMap(Position, int, Color, bool)
{
}

void Game::drawDotMap(Position, Color)
{
}

void Game::drawLineMap(Position, Position, Color)
{
}

void Game::drawLineMap(Position, Position)
{
}

void Game::drawLineMap(int, int, int, int, Color)
{
}

void Game::drawTriangleMap(Position, Position, Position, Color, bool)
{
}

void Game::setTextSize(int)
{
}

void Game::setTextSize()
{
}

Position Game::getMousePosition()
{
    return Positions::Origin;
}

Position Game::getScreenPosition()
{
    return Positions::Origin;
}

int Game::getAPM(bool)
{
    return 0;
}

int Game::getGroundHeight(TilePosition position)
{
    return position.isValid() ? 0 : -1;
}

void Game::setLocalSpeed(int)
{
}

void Game::setFrameSkip(int)
{
}

void Game::setGUI(bool)
{
}

void Game::setCommandOptimizationLevel(int)
{
}

void Game::enableFlag(int)
{
}

void Game::refresh()
{
    if (!bridge_) {
        bridge_ = LAVBWAPIRM::getBridge();
    }
    snapshot_ = bridge_->snapshot();
    self_.update(snapshot_.self);
    enemy_.update(snapshot_.enemy);

    LAVBWAPIRM::PlayerSnapshot neutralSnapshot;
    neutralSnapshot.id = 0;
    neutralSnapshot.name = "Neutral";
    neutralSnapshot.race = "None";
    neutral_.update(neutralSnapshot);

    Unitset selfUnits;
    Unitset enemyUnits;
    Unitset neutralUnits;
    std::set<int> activeUnitIds;

    auto findUnit = [this](int id) -> Unit {
        for (auto& unit : units_) {
            if (unit && unit->getID() == id) {
                return unit.get();
            }
        }
        return nullptr;
    };

    auto appendUnits = [this, &selfUnits, &enemyUnits, &neutralUnits, &activeUnitIds, &findUnit](
                           const std::vector<LAVBWAPIRM::UnitSnapshot>& snapshots,
                           Player defaultOwner
                       ) {
        for (const auto& snapshot : snapshots) {
            Player owner = resolveOwner(snapshot, &self_, &enemy_, &neutral_);
            if (!owner) {
                owner = defaultOwner;
            }
            Unit unit = findUnit(snapshot.id);
            if (unit) {
                unit->update(snapshot);
                unit->setPlayer(owner);
            } else {
                units_.push_back(std::make_unique<UnitInterface>(snapshot, owner));
                unit = units_.back().get();
            }
            activeUnitIds.insert(snapshot.id);
            if (owner == &enemy_) {
                enemyUnits.insert(unit);
            } else if (owner == &neutral_) {
                neutralUnits.insert(unit);
            } else {
                selfUnits.insert(unit);
            }
        }
    };

    appendUnits(snapshot_.myUnits, &self_);
    appendUnits(snapshot_.enemyUnits, &enemy_);
    appendUnits(snapshot_.neutralUnits, &neutral_);

    units_.erase(
        std::remove_if(
            units_.begin(),
            units_.end(),
            [&activeUnitIds](const std::unique_ptr<UnitInterface>& unit) {
                return unit && activeUnitIds.find(unit->getID()) == activeUnitIds.end();
            }
        ),
        units_.end()
    );

    self_.setUnits(selfUnits);
    enemy_.setUnits(enemyUnits);
    neutral_.setUnits(neutralUnits);
}

void Game::setBridge(std::shared_ptr<LAVBWAPIRM::Bridge> bridge)
{
    bridge_ = std::move(bridge);
}

Game* GameHandle::get() const
{
    return BroodwarPtr;
}

void GameHandle::reset(Game* game)
{
    BroodwarPtr = game ? game : &defaultGame;
}

Game* GameHandle::operator->() const
{
    return get();
}

GameHandle::operator Game*() const
{
    return get();
}

GameHandle& GameHandle::operator<<(OStreamManipulator)
{
    if (get()) {
        get()->sendText("");
    }
    return *this;
}

std::ostream& operator<<(std::ostream& stream, const Race& race)
{
    stream << race.getName();
    return stream;
}

std::ostream& operator<<(std::ostream& stream, const UnitType& type)
{
    stream << type.getName();
    return stream;
}

std::ostream& operator<<(std::ostream& stream, const WeaponType& type)
{
    stream << type.getName();
    return stream;
}

std::ostream& operator<<(std::ostream& stream, const TechType& type)
{
    stream << type.getName();
    return stream;
}

std::ostream& operator<<(std::ostream& stream, const UpgradeType& type)
{
    stream << type.getName();
    return stream;
}

} // namespace BWAPI
