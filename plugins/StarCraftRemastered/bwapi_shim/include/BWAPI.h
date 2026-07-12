//20260701_kpopmodder: Provides a source-level BWAPI facade for SAIDA compatibility experiments.
#pragma once

#include <cmath>
#include <functional>
#include <map>
#include <memory>
#include <ostream>
#include <set>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

#include "LAVBWAPIRM/Bridge.h"

#ifndef TILE_SIZE
#define TILE_SIZE 32
#endif

#ifndef TILEPOSITION_SCALE
#define TILEPOSITION_SCALE 32
#endif

namespace BWAPI {

template <typename Derived, int UnknownId>
class Type {
public:
    static const std::string typeNames[];

    explicit Type(int id = UnknownId) : id_(id) {}

    int getID() const { return id_; }
    std::string getName() const { return id_ >= 0 ? typeNames[id_] : "Unknown"; }
    std::string toString() const { return getName(); }
    const char* c_str() const { return getName().c_str(); }
    operator int() const { return id_; }
    bool operator==(const Type& other) const { return id_ == other.id_; }
    bool operator!=(const Type& other) const { return !(*this == other); }
    bool operator<(const Type& other) const { return id_ < other.id_; }

private:
    int id_ = UnknownId;
};

template <typename T, int Scale = 1>
struct Point {
    T x = 0;
    T y = 0;

    Point() = default;
    explicit Point(T value) : x(value), y(value) {}
    Point(T xValue, T yValue) : x(xValue), y(yValue) {}

    template <typename OtherT, int OtherScale>
    Point(const Point<OtherT, OtherScale>& other)
        : x(static_cast<T>((static_cast<long long>(other.x) * OtherScale) / Scale))
        , y(static_cast<T>((static_cast<long long>(other.y) * OtherScale) / Scale))
    {
    }

    bool isValid() const { return x >= 0 && y >= 0; }
    Point makeValid() const
    {
        return {static_cast<T>(x < 0 ? 0 : x), static_cast<T>(y < 0 ? 0 : y)};
    }

    Point operator+(Point other) const { return {static_cast<T>(x + other.x), static_cast<T>(y + other.y)}; }
    Point operator-(Point other) const { return {static_cast<T>(x - other.x), static_cast<T>(y - other.y)}; }
    Point operator+(int value) const { return {static_cast<T>(x + value), static_cast<T>(y + value)}; }
    Point operator-(int value) const { return {static_cast<T>(x - value), static_cast<T>(y - value)}; }
    Point operator*(int value) const { return {static_cast<T>(x * value), static_cast<T>(y * value)}; }
    Point operator/(int value) const { return value == 0 ? *this : Point(static_cast<T>(x / value), static_cast<T>(y / value)); }
    Point& operator+=(Point other)
    {
        x = static_cast<T>(x + other.x);
        y = static_cast<T>(y + other.y);
        return *this;
    }
    Point& operator-=(Point other)
    {
        x = static_cast<T>(x - other.x);
        y = static_cast<T>(y - other.y);
        return *this;
    }
    Point& operator*=(int value)
    {
        x = static_cast<T>(x * value);
        y = static_cast<T>(y * value);
        return *this;
    }
    Point& operator/=(int value)
    {
        if (value != 0) {
            x = static_cast<T>(x / value);
            y = static_cast<T>(y / value);
        }
        return *this;
    }
    bool operator==(Point other) const { return x == other.x && y == other.y; }
    bool operator!=(Point other) const { return !(*this == other); }
    explicit operator bool() const { return isValid(); }
    bool operator<(Point other) const
    {
        if (y != other.y) {
            return y < other.y;
        }
        return x < other.x;
    }

    template <typename OtherT, int OtherScale>
    int getApproxDistance(Point<OtherT, OtherScale> other) const
    {
        return static_cast<int>(getDistance(other));
    }

    template <typename OtherT, int OtherScale>
    double getDistance(Point<OtherT, OtherScale> other) const
    {
        Point<T, Scale> converted(other);
        const long long dx = static_cast<long long>(x) - converted.x;
        const long long dy = static_cast<long long>(y) - converted.y;
        return std::sqrt(static_cast<double>(dx * dx + dy * dy));
    }
};

using Position = Point<int, 1>;
using WalkPosition = Point<int, 8>;
using TilePosition = Point<int, TILE_SIZE>;

class Color {
public:
    Color() = default;
    explicit Color(int id) : id_(id) {}
    Color(int red, int green, int blue)
        : id_(((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff))
    {
    }
    int getID() const { return id_; }
    operator int() const { return id_; }
    bool operator==(Color other) const { return id_ == other.id_; }
    bool operator!=(Color other) const { return !(*this == other); }

private:
    int id_ = 0;
};

class Race {
public:
    Race() = default;
    explicit Race(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    const char* c_str() const { return name_.c_str(); }
    bool operator==(const Race& other) const { return name_ == other.name_; }
    bool operator!=(const Race& other) const { return !(*this == other); }

private:
    std::string name_ = "Unknown";
};

class UnitType;
class TechType;
class UpgradeType;

class UnitSizeType {
public:
    UnitSizeType() = default;
    explicit UnitSizeType(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    bool operator==(const UnitSizeType& other) const { return name_ == other.name_; }
    bool operator!=(const UnitSizeType& other) const { return !(*this == other); }
    bool operator<(const UnitSizeType& other) const { return name_ < other.name_; }

private:
    std::string name_ = "Unknown";
};

class WeaponType {
public:
    WeaponType() = default;
    explicit WeaponType(std::string name, int maxRange = 0, int minRange = 0)
        : name_(std::move(name))
        , maxRange_(maxRange)
        , minRange_(minRange)
    {
    }
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    int maxRange() const { return maxRange_; }
    int minRange() const { return minRange_; }
    int damageAmount() const { return damageAmount_; }
    int damageBonus() const { return 0; }
    int damageCooldown() const { return cooldown_; }
    int damageFactor() const { return 1; }
    int cooldown() const { return cooldown_; }
    bool targetsAir() const { return targetsAir_; }
    bool targetsGround() const { return targetsGround_; }
    bool operator==(const WeaponType& other) const { return name_ == other.name_; }
    bool operator!=(const WeaponType& other) const { return !(*this == other); }
    bool operator<(const WeaponType& other) const { return name_ < other.name_; }
    WeaponType& setDamage(int amount, int cooldown)
    {
        damageAmount_ = amount;
        cooldown_ = cooldown;
        return *this;
    }
    WeaponType& setTargets(bool ground, bool air)
    {
        targetsGround_ = ground;
        targetsAir_ = air;
        return *this;
    }

private:
    std::string name_ = "None";
    int maxRange_ = 0;
    int minRange_ = 0;
    int damageAmount_ = 0;
    int cooldown_ = 1;
    bool targetsGround_ = false;
    bool targetsAir_ = false;
};

class UnitType {
public:
    UnitType() = default;
    explicit UnitType(std::string name, int id = 0) : getType(id), name_(std::move(name)), id_(id) {}
    int getType = 0;
    int getID() const { return id_; }
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    const char* c_str() const { return name_.c_str(); }
    Race getRace() const;
    bool isWorker() const;
    bool isBuilding() const;
    bool isAddon() const;
    bool isResourceDepot() const;
    bool isMineralField() const;
    bool isRefinery() const;
    bool isFlyer() const;
    bool isFlyingBuilding() const;
    bool isSpell() const;
    bool isSpellcaster() const;
    bool isDetector() const;
    bool isSpecialBuilding() const;
    bool isCritter() const;
    bool isBurrowable() const;
    bool isMechanical() const;
    bool isNeutral() const;
    bool isTwoUnitsInOneEgg() const;
    bool requiresPsi() const;
    bool isInvincible() const;
    bool canProduce() const;
    bool canAttack() const;
    int mineralPrice() const;
    int gasPrice() const;
    int supplyRequired() const;
    int supplyProvided() const;
    int width() const;
    int height() const;
    int tileWidth() const;
    int tileHeight() const;
    TilePosition tileSize() const;
    int sightRange() const;
    int seekRange() const;
    int maxEnergy() const;
    double topSpeed() const;
    UnitSizeType size() const;
    int dimensionLeft() const;
    int dimensionRight() const;
    int dimensionUp() const;
    int dimensionDown() const;
    WeaponType airWeapon() const;
    WeaponType groundWeapon() const;
    std::pair<UnitType, int> whatBuilds() const;
    std::vector<UnitType> buildsWhat() const;
    const std::map<UnitType, int>& requiredUnits() const;
    TechType requiredTech() const;
    bool operator<(const UnitType& other) const { return name_ < other.name_; }
    bool operator==(const UnitType& other) const { return name_ == other.name_; }
    bool operator!=(const UnitType& other) const { return !(*this == other); }
    operator bool() const { return name_ != "None" && name_ != "Unknown"; }
    int maxHitPoints() const;
    int maxShields() const;
    int buildTime() const;
    bool canBuildAddon() const;
    int spaceProvided() const;
    int spaceRequired() const;
    bool isCloakable() const;

private:
    std::string name_ = "Unknown";
    int id_ = 0;
};

class TechType {
public:
    TechType() = default;
    explicit TechType(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    const char* c_str() const { return name_.c_str(); }
    Race getRace() const;
    int mineralPrice() const;
    int gasPrice() const;
    int energyCost() const;
    UnitType whatResearches() const;
    UnitType requiredUnit() const;
    bool operator==(const TechType& other) const { return name_ == other.name_; }
    bool operator!=(const TechType& other) const { return !(*this == other); }
    bool operator<(const TechType& other) const { return name_ < other.name_; }

private:
    std::string name_;
};

class UpgradeType {
public:
    UpgradeType() = default;
    explicit UpgradeType(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    const char* c_str() const { return name_.c_str(); }
    Race getRace() const;
    int mineralPrice() const;
    int gasPrice() const;
    int upgradeTime() const;
    UnitType whatUpgrades() const;
    UnitType whatsRequired(int level = 1) const;
    bool operator==(const UpgradeType& other) const { return name_ == other.name_; }
    bool operator!=(const UpgradeType& other) const { return !(*this == other); }
    bool operator<(const UpgradeType& other) const { return name_ < other.name_; }

private:
    std::string name_;
};

class GameType {
public:
    GameType() = default;
    explicit GameType(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    bool operator==(const GameType& other) const { return name_ == other.name_; }
    bool operator!=(const GameType& other) const { return !(*this == other); }

private:
    std::string name_ = "Unknown";
};

namespace GameTypes {
extern const GameType Melee;
extern const GameType Use_Map_Settings;
extern const GameType Unknown;
} // namespace GameTypes

class PlayerInterface;
class UnitInterface;
class BulletInterface;
class ForceInterface;

using Player = PlayerInterface*;
using Unit = UnitInterface*;
using Bullet = BulletInterface*;
using Force = ForceInterface*;
using Playerset = std::set<Player>;
using Unitset = std::set<Unit>;
using Bulletset = std::set<Bullet>;
using Forceset = std::set<Force>;

class ForceInterface {
public:
    int getID() const { return 0; }
    std::string getName() const { return "Force"; }
    Playerset getPlayers() const { return {}; }
};

class UnitFilter {
public:
    UnitFilter();
    explicit UnitFilter(std::function<bool(Unit)> predicate);
    bool operator()(Unit unit) const;
    UnitFilter operator!() const;
    friend UnitFilter operator&&(UnitFilter left, UnitFilter right);
    friend UnitFilter operator||(UnitFilter left, UnitFilter right);

private:
    std::function<bool(Unit)> predicate_;
};

class UnitCommandType {
public:
    UnitCommandType() = default;
    explicit UnitCommandType(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    const char* c_str() const { return name_.c_str(); }
    bool operator==(const UnitCommandType& other) const { return name_ == other.name_; }
    bool operator!=(const UnitCommandType& other) const { return !(*this == other); }
    bool operator<(const UnitCommandType& other) const { return name_ < other.name_; }

private:
    std::string name_ = "None";
};

namespace UnitCommandTypes {
extern const UnitCommandType None;
extern const UnitCommandType Move;
extern const UnitCommandType Patrol;
extern const UnitCommandType Attack_Unit;
extern const UnitCommandType Attack_Move;
extern const UnitCommandType Right_Click_Unit;
extern const UnitCommandType Build;
extern const UnitCommandType Build_Addon;
extern const UnitCommandType Train;
extern const UnitCommandType Research;
extern const UnitCommandType Upgrade;
extern const UnitCommandType Repair;
extern const UnitCommandType Stop;
extern const UnitCommandType Hold_Position;
extern const UnitCommandType Gather;
extern const UnitCommandType Return_Cargo;
extern const UnitCommandType Use_Tech;
extern const UnitCommandType Use_Tech_Unit;
extern const UnitCommandType Use_Tech_Position;
extern const UnitCommandType Land;
} // namespace UnitCommandTypes

class UnitCommand {
public:
    UnitCommand() = default;
    explicit UnitCommand(UnitCommandType type) : type_(std::move(type)) {}
    UnitCommandType getType() const { return type_; }
    Unit getUnit() const { return unit_; }
    Unit getTarget() const { return target_; }
    UnitType getUnitType() const { return unitType_; }
    TilePosition getTargetTilePosition() const { return targetTilePosition_; }
    Position getTargetPosition() const { return targetPosition_; }
    void setType(UnitCommandType type) { type_ = std::move(type); }
    void setUnit(Unit unit) { unit_ = unit; }
    void setTarget(Unit target) { target_ = target; }
    void setUnitType(UnitType type) { unitType_ = std::move(type); }
    void setTargetTilePosition(TilePosition position) { targetTilePosition_ = position; }
    void setTargetPosition(Position position) { targetPosition_ = position; }

private:
    UnitCommandType type_;
    Unit unit_ = nullptr;
    Unit target_ = nullptr;
    UnitType unitType_;
    TilePosition targetTilePosition_;
    Position targetPosition_;
};

class PlayerInterface {
public:
    explicit PlayerInterface(LAVBWAPIRM::PlayerSnapshot snapshot = {});

    int getID() const;
    std::string getName() const;
    Race getRace() const;
    int minerals() const;
    int gas() const;
    int supplyUsed() const;
    int supplyTotal() const;
    int gatheredMinerals() const;
    int gatheredGas() const;
    bool hasResearched(TechType tech) const;
    bool isResearching(TechType tech) const;
    bool isUpgrading(UpgradeType upgrade) const;
    int getUpgradeLevel(UpgradeType upgrade) const;
    int getMaxUpgradeLevel(UpgradeType upgrade) const;
    int weaponMaxRange(WeaponType weapon) const;
    int weaponDamageCooldown(WeaponType weapon) const;
    int weaponDamageCooldown(UnitType type) const;
    Color getTextColor() const;
    Force getForce() const;
    bool isDefeated() const;
    bool leftGame() const;
    TilePosition getStartLocation() const;
    Unitset getUnits() const;
    int allUnitCount(UnitType type = UnitType()) const;
    int completedUnitCount(UnitType type = UnitType()) const;
    int incompleteUnitCount(UnitType type = UnitType()) const;
    int visibleUnitCount(UnitType type = UnitType()) const;
    int maxEnergy(UnitType type) const;
    double topSpeed(UnitType type) const;
    bool isEnemy(Player player) const;
    bool isAlly(Player player) const;
    bool isNeutral() const;
    void update(LAVBWAPIRM::PlayerSnapshot snapshot);
    void setUnits(Unitset units);

private:
    LAVBWAPIRM::PlayerSnapshot snapshot_;
    Unitset units_;
};

class UnitInterface {
public:
    explicit UnitInterface(LAVBWAPIRM::UnitSnapshot snapshot = {}, Player player = nullptr);

    int getID() const;
    UnitType getType() const;
    Player getPlayer() const;
    Position getPosition() const;
    TilePosition getTilePosition() const;
    int getHitPoints() const;
    int getInitialHitPoints() const;
    Position getInitialPosition() const;
    TilePosition getInitialTilePosition() const;
    UnitType getInitialType() const;
    int getShields() const;
    int getEnergy() const;
    int getResources() const;
    int getInitialResources() const;
    int getSpaceRemaining() const;
    Unitset getLoadedUnits() const;
    int getDistance(Unit target) const;
    int getDistance(Position position) const;
    bool exists() const;
    bool isCompleted() const;
    bool isVisible(Player player = nullptr) const;
    bool isSelected() const;
    bool isIdle() const;
    bool isFlying() const;
    bool isGatheringMinerals() const;
    bool isGatheringGas() const;
    bool isCarryingMinerals() const;
    bool isCarryingGas() const;
    bool isBeingGathered() const;
    bool isConstructing() const;
    bool isBeingConstructed() const;
    bool isTraining() const;
    bool isResearching() const;
    bool isUpgrading() const;
    bool isPowered() const;
    bool isLifted() const;
    bool isCloaked() const;
    bool isUnderAttack() const;
    bool isMorphing() const;
    bool isDetected() const;
    bool isAttacking() const;
    bool isDefenseMatrixed() const;
    bool isBurrowed() const;
    bool isHoldingPosition() const;
    bool isMoving() const;
    bool isRepairing() const;
    bool isPatrolling() const;
    bool isUnderDarkSwarm() const;
    bool isStasised() const;
    bool isIrradiated() const;
    bool isStuck() const;
    bool isUnderDisruptionWeb() const;
    bool isUnderStorm() const;
    bool isInWeaponRange(Unit target) const;
    bool canMove() const;
    bool canAttack() const;
    bool canGather() const;
    bool canBuildAddon() const;
    bool canResearch(TechType tech) const;
    bool canUpgrade(UpgradeType upgrade = UpgradeType()) const;
    bool canTrain(UnitType type = UnitType()) const;
    bool canPatrol() const;
    bool canHoldPosition() const;
    bool canCancelConstruction() const;
    bool canCancelAddon() const;
    bool canCancelTrain() const;
    bool canReturnCargo() const;
    bool canCommand() const;
    bool isInterruptible() const;
    bool canLift() const;
    bool canSiege() const;
    bool canUnsiege() const;
    bool isSieged() const;
    bool canCloak() const;
    bool isMineralField() const;
    bool isFlyingBuilding() const;
    bool isRefinery() const;
    bool isBuilding() const;
    Unit getTarget() const;
    Unit getOrderTarget() const;
    Unit getBuildUnit() const;
    Unit getAddon() const;
    Unit getClosestUnit(UnitFilter filter = UnitFilter(), int radius = 0) const;
    Position getTargetPosition() const;
    double getAngle() const;
    int getLeft() const;
    int getRight() const;
    int getTop() const;
    int getBottom() const;
    double getVelocityX() const;
    double getVelocityY() const;
    int getAirWeaponCooldown() const;
    int getGroundWeaponCooldown() const;
    int getSpellCooldown() const;
    int getSpiderMineCount() const;
    int getRemainingBuildTime() const;
    int getRemainingTrainTime() const;
    int getRemainingResearchTime() const;
    int getRemainingUpgradeTime() const;
    std::vector<UnitType> getTrainingQueue() const;
    UnitType getBuildType() const;
    UnitCommand getLastCommand() const;
    int getLastCommandFrame() const;

    bool train(UnitType type);
    bool build(UnitType type, TilePosition position = {});
    bool buildAddon(UnitType type);
    bool research(TechType tech);
    bool upgrade(UpgradeType upgrade);
    bool move(Position position);
    bool attack(Unit target);
    bool attack(Position position);
    bool rightClick(Unit target);
    bool rightClick(Position position);
    bool gather(Unit target);
    bool repair(Unit target);
    bool holdPosition(bool queuedCommand = false);
    bool returnCargo();
    bool stop();
    bool patrol(Position position);
    bool lift();
    bool siege();
    bool unsiege();
    bool cloak();
    bool decloak();
    bool land(TilePosition position);
    bool canLand() const;
    bool canLand(bool checkCanIssueCommandType) const;
    bool canLand(TilePosition position) const;
    bool unload(Unit unit);
    bool unloadAll();
    bool cancelConstruction();
    bool cancelAddon();
    bool cancelTrain(int slot = -1);
    bool cancelUpgrade();
    bool canUnload(Unit unit = nullptr) const;
    bool isLoaded() const;
    Unit getTransport() const;
    bool canBurrow(bool checkCanIssueCommandType = true) const;
    bool canUnburrow(bool checkCanIssueCommandType = true) const;
    bool isAttackFrame() const;
    bool useTech(TechType tech);
    bool useTech(TechType tech, Unit target);
    bool useTech(TechType tech, Position position);

    void update(LAVBWAPIRM::UnitSnapshot snapshot);
    void setPlayer(Player player);

private:
    LAVBWAPIRM::UnitSnapshot snapshot_;
    Player player_ = nullptr;
};

class Game {
public:
    Game();
    explicit Game(std::shared_ptr<LAVBWAPIRM::Bridge> bridge);

    bool isConnected();
    bool isInGame();
    int getFrameCount();
    std::string mapName();
    std::string mapFileName();
    int mapWidth();
    int mapHeight();
    Player self();
    Player enemy();
    Player neutral();
    Playerset getPlayers();
    Forceset getForces();
    Unitset getAllUnits();
    Unitset getMinerals();
    Unitset getNeutralUnits();
    Unitset getStaticNeutralUnits();
    Bulletset getBullets();
    std::vector<Position> getNukeDots();
    Unitset getUnitsInRadius(Position center, int radius);
    Unitset getUnitsInRadius(Position center, int radius, UnitFilter filter);
    Unitset getUnitsInRectangle(Position leftTop, Position rightBottom);
    Unitset getUnitsInRectangle(Position leftTop, Position rightBottom, UnitFilter filter);
    Unitset getUnitsInRectangle(int left, int top, int right, int bottom);
    Unit getClosestUnit(Position center, UnitFilter filter = UnitFilter(), int radius = 0);
    Unitset getUnitsOnTile(TilePosition position);
    Unitset getUnitsOnTile(int tileX, int tileY);
    std::vector<TilePosition> getStartLocations();
    bool canBuildHere(TilePosition position, UnitType type, Unit builder = nullptr, bool checkExplored = false);
    bool isReplay();
    GameType getGameType();
    bool isPaused();
    void pauseGame();
    void resumeGame();
    bool isVisible(TilePosition position);
    bool isVisible(int tileX, int tileY);
    bool isExplored(TilePosition position);
    bool isExplored(int tileX, int tileY);
    bool hasCreep(TilePosition position);
    bool hasCreep(int tileX, int tileY);
    bool isBuildable(int tileX, int tileY, bool includeBuildings = false);
    bool isBuildable(TilePosition position, bool includeBuildings = false);
    bool isWalkable(TilePosition position);
    bool isWalkable(WalkPosition position);
    bool isWalkable(int walkX, int walkY);
    bool hasPath(Position source, Position destination);
    bool canMake(UnitType type, Unit builder = nullptr);
    bool canResearch(TechType tech, Unit unit = nullptr);
    bool canUpgrade(UpgradeType upgrade, Unit unit = nullptr);
    int getDamageFrom(UnitType fromType, UnitType toType, Player fromPlayer = nullptr, Player toPlayer = nullptr);
    bool sendText(const char* text);
    bool sendText(const char* format, const char* text);
    void printf(const char* text);
    void drawTextScreen(int x, int y, const char* text, ...);
    void drawTextMap(Position position, const char* text, ...);
    void drawTextMap(int x, int y, const char* text, ...);
    void drawBoxMap(Position leftTop, Position rightBottom, Color color, bool isSolid = false);
    void drawBoxMap(int left, int top, int right, int bottom, Color color, bool isSolid = false);
    void drawCircleMap(Position center, int radius, Color color, bool isSolid = false);
    void drawDotMap(Position position, Color color);
    void drawLineMap(Position point1, Position point2, Color color);
    void drawLineMap(Position point1, Position point2);
    void drawLineMap(int x1, int y1, int x2, int y2, Color color);
    void drawTriangleMap(Position point1, Position point2, Position point3, Color color, bool isSolid = false);
    void setTextSize(int size);
    void setTextSize();
    Position getMousePosition();
    Position getScreenPosition();
    int getAPM(bool includeSelects = false);
    int getGroundHeight(TilePosition position);
    void setLocalSpeed(int speed);
    void setFrameSkip(int frameSkip);
    void setGUI(bool enabled);
    void setCommandOptimizationLevel(int level);
    void enableFlag(int flag);
    void refresh();
    void setBridge(std::shared_ptr<LAVBWAPIRM::Bridge> bridge);
    template <typename Value>
    void writeStreamValue(const Value& value)
    {
        std::ostringstream stream;
        stream << value;
        sendText(stream.str().c_str());
    }

private:
    LAVBWAPIRM::GameSnapshot snapshot_;
    std::shared_ptr<LAVBWAPIRM::Bridge> bridge_;
    PlayerInterface self_;
    PlayerInterface enemy_;
    PlayerInterface neutral_;
    std::vector<std::unique_ptr<UnitInterface>> units_;
};

using OStreamManipulator = std::ostream& (*)(std::ostream&);

class GameHandle {
public:
    Game* get() const;
    void reset(Game* game);
    Game* operator->() const;
    operator Game*() const;

    template <typename Value>
    GameHandle& operator<<(const Value& value)
    {
        if (get()) {
            get()->writeStreamValue(value);
        }
        return *this;
    }

    GameHandle& operator<<(OStreamManipulator manipulator);
};

class AIModule {
public:
    virtual ~AIModule() = default;
    virtual void onStart() {}
    virtual void onEnd(bool isWinner) {}
    virtual void onFrame() {}
    virtual void onUnitDiscover(Unit unit) {}
    virtual void onUnitEvade(Unit unit) {}
    virtual void onUnitShow(Unit unit) {}
    virtual void onUnitHide(Unit unit) {}
    virtual void onUnitCreate(Unit unit) {}
    virtual void onUnitDestroy(Unit unit) {}
    virtual void onUnitMorph(Unit unit) {}
    virtual void onUnitRenegade(Unit unit) {}
    virtual void onUnitComplete(Unit unit) {}
};

extern GameHandle Broodwar;
extern Game* BroodwarPtr;

namespace Races {
extern const Race Terran;
extern const Race Protoss;
extern const Race Zerg;
extern const Race None;
extern const Race Unknown;
extern const Race Random;
} // namespace Races

namespace UnitSizeTypes {
extern const UnitSizeType Independent;
extern const UnitSizeType Small;
extern const UnitSizeType Medium;
extern const UnitSizeType Large;
extern const UnitSizeType Unknown;
} // namespace UnitSizeTypes

namespace UnitTypes {
std::vector<UnitType> allUnitTypes();
extern const UnitType None;
extern const UnitType AllUnits;
extern const UnitType Men;
extern const UnitType Buildings;
extern const UnitType Terran_SCV;
extern const UnitType Terran_Command_Center;
extern const UnitType Terran_Comsat_Station;
extern const UnitType Terran_Nuclear_Silo;
extern const UnitType Terran_Supply_Depot;
extern const UnitType Terran_Barracks;
extern const UnitType Terran_Bunker;
extern const UnitType Terran_Missile_Turret;
extern const UnitType Terran_Refinery;
extern const UnitType Terran_Engineering_Bay;
 extern const UnitType Terran_Academy;
extern const UnitType Terran_Armory;
extern const UnitType Terran_Factory;
extern const UnitType Terran_Machine_Shop;
extern const UnitType Terran_Starport;
extern const UnitType Terran_Control_Tower;
extern const UnitType Terran_Science_Facility;
extern const UnitType Terran_Covert_Ops;
extern const UnitType Terran_Physics_Lab;
extern const UnitType Terran_Marine;
extern const UnitType Terran_Firebat;
extern const UnitType Terran_Medic;
extern const UnitType Terran_Ghost;
extern const UnitType Terran_Vulture;
extern const UnitType Terran_Vulture_Spider_Mine;
extern const UnitType Terran_Siege_Tank_Tank_Mode;
extern const UnitType Terran_Siege_Tank_Siege_Mode;
extern const UnitType Terran_Goliath;
extern const UnitType Terran_Wraith;
extern const UnitType Terran_Dropship;
extern const UnitType Terran_Science_Vessel;
extern const UnitType Terran_Battlecruiser;
extern const UnitType Terran_Valkyrie;
extern const UnitType Terran_Nuclear_Missile;
extern const UnitType Zerg_Larva;
extern const UnitType Zerg_Egg;
extern const UnitType Zerg_Drone;
extern const UnitType Zerg_Zergling;
extern const UnitType Zerg_Hydralisk;
extern const UnitType Zerg_Lurker;
extern const UnitType Zerg_Mutalisk;
extern const UnitType Zerg_Guardian;
extern const UnitType Zerg_Scourge;
extern const UnitType Zerg_Queen;
extern const UnitType Zerg_Defiler;
extern const UnitType Zerg_Ultralisk;
extern const UnitType Zerg_Overlord;
extern const UnitType Zerg_Broodling;
extern const UnitType Zerg_Infested_Terran;
extern const UnitType Zerg_Lurker_Egg;
extern const UnitType Zerg_Cocoon;
extern const UnitType Zerg_Devourer;
extern const UnitType Zerg_Hatchery;
extern const UnitType Zerg_Lair;
extern const UnitType Zerg_Hive;
extern const UnitType Zerg_Creep_Colony;
extern const UnitType Zerg_Sunken_Colony;
extern const UnitType Zerg_Spore_Colony;
extern const UnitType Zerg_Extractor;
extern const UnitType Zerg_Spawning_Pool;
extern const UnitType Zerg_Evolution_Chamber;
extern const UnitType Zerg_Hydralisk_Den;
extern const UnitType Zerg_Spire;
extern const UnitType Zerg_Greater_Spire;
extern const UnitType Zerg_Queens_Nest;
extern const UnitType Zerg_Nydus_Canal;
extern const UnitType Zerg_Defiler_Mound;
extern const UnitType Zerg_Ultralisk_Cavern;
extern const UnitType Zerg_Infested_Command_Center;
extern const UnitType Protoss_Probe;
extern const UnitType Protoss_Zealot;
extern const UnitType Protoss_Dragoon;
extern const UnitType Protoss_High_Templar;
extern const UnitType Protoss_Dark_Templar;
extern const UnitType Protoss_Archon;
extern const UnitType Protoss_Reaver;
extern const UnitType Protoss_Scarab;
extern const UnitType Protoss_Shuttle;
extern const UnitType Protoss_Scout;
extern const UnitType Protoss_Observer;
extern const UnitType Protoss_Carrier;
extern const UnitType Protoss_Interceptor;
extern const UnitType Protoss_Arbiter;
extern const UnitType Protoss_Corsair;
extern const UnitType Protoss_Dark_Archon;
extern const UnitType Protoss_Nexus;
extern const UnitType Protoss_Pylon;
extern const UnitType Protoss_Assimilator;
extern const UnitType Protoss_Gateway;
extern const UnitType Protoss_Forge;
extern const UnitType Protoss_Photon_Cannon;
extern const UnitType Protoss_Cybernetics_Core;
extern const UnitType Protoss_Robotics_Facility;
extern const UnitType Protoss_Stargate;
extern const UnitType Protoss_Citadel_of_Adun;
extern const UnitType Protoss_Robotics_Support_Bay;
extern const UnitType Protoss_Fleet_Beacon;
extern const UnitType Protoss_Templar_Archives;
extern const UnitType Protoss_Observatory;
extern const UnitType Protoss_Arbiter_Tribunal;
extern const UnitType Protoss_Shield_Battery;
extern const UnitType Spell_Scanner_Sweep;
extern const UnitType Special_Pit_Door;
extern const UnitType Special_Right_Pit_Door;
extern const UnitType Special_Power_Generator;
extern const UnitType Resource_Mineral_Field;
extern const UnitType Resource_Vespene_Geyser;
extern const UnitType Unknown;

namespace Enum {
using BWAPI::UnitTypes::None;
using BWAPI::UnitTypes::AllUnits;
using BWAPI::UnitTypes::Men;
using BWAPI::UnitTypes::Buildings;
using BWAPI::UnitTypes::Terran_SCV;
using BWAPI::UnitTypes::Terran_Command_Center;
using BWAPI::UnitTypes::Terran_Comsat_Station;
using BWAPI::UnitTypes::Terran_Nuclear_Silo;
using BWAPI::UnitTypes::Terran_Supply_Depot;
using BWAPI::UnitTypes::Terran_Barracks;
using BWAPI::UnitTypes::Terran_Bunker;
using BWAPI::UnitTypes::Terran_Missile_Turret;
using BWAPI::UnitTypes::Terran_Refinery;
using BWAPI::UnitTypes::Terran_Engineering_Bay;
using BWAPI::UnitTypes::Terran_Academy;
using BWAPI::UnitTypes::Terran_Armory;
using BWAPI::UnitTypes::Terran_Factory;
using BWAPI::UnitTypes::Terran_Machine_Shop;
using BWAPI::UnitTypes::Terran_Starport;
using BWAPI::UnitTypes::Terran_Control_Tower;
using BWAPI::UnitTypes::Terran_Science_Facility;
using BWAPI::UnitTypes::Terran_Covert_Ops;
using BWAPI::UnitTypes::Terran_Physics_Lab;
using BWAPI::UnitTypes::Terran_Marine;
using BWAPI::UnitTypes::Terran_Firebat;
using BWAPI::UnitTypes::Terran_Medic;
using BWAPI::UnitTypes::Terran_Ghost;
using BWAPI::UnitTypes::Terran_Vulture;
using BWAPI::UnitTypes::Terran_Vulture_Spider_Mine;
using BWAPI::UnitTypes::Terran_Siege_Tank_Tank_Mode;
using BWAPI::UnitTypes::Terran_Siege_Tank_Siege_Mode;
using BWAPI::UnitTypes::Terran_Goliath;
using BWAPI::UnitTypes::Terran_Wraith;
using BWAPI::UnitTypes::Terran_Dropship;
using BWAPI::UnitTypes::Terran_Science_Vessel;
using BWAPI::UnitTypes::Terran_Battlecruiser;
using BWAPI::UnitTypes::Terran_Valkyrie;
using BWAPI::UnitTypes::Terran_Nuclear_Missile;
using BWAPI::UnitTypes::Zerg_Larva;
using BWAPI::UnitTypes::Zerg_Egg;
using BWAPI::UnitTypes::Zerg_Drone;
using BWAPI::UnitTypes::Zerg_Zergling;
using BWAPI::UnitTypes::Zerg_Hydralisk;
using BWAPI::UnitTypes::Zerg_Lurker;
using BWAPI::UnitTypes::Zerg_Mutalisk;
using BWAPI::UnitTypes::Zerg_Guardian;
using BWAPI::UnitTypes::Zerg_Scourge;
using BWAPI::UnitTypes::Zerg_Queen;
using BWAPI::UnitTypes::Zerg_Defiler;
using BWAPI::UnitTypes::Zerg_Ultralisk;
using BWAPI::UnitTypes::Zerg_Overlord;
using BWAPI::UnitTypes::Zerg_Broodling;
using BWAPI::UnitTypes::Zerg_Infested_Terran;
using BWAPI::UnitTypes::Zerg_Lurker_Egg;
using BWAPI::UnitTypes::Zerg_Cocoon;
using BWAPI::UnitTypes::Zerg_Devourer;
using BWAPI::UnitTypes::Zerg_Hatchery;
using BWAPI::UnitTypes::Zerg_Lair;
using BWAPI::UnitTypes::Zerg_Hive;
using BWAPI::UnitTypes::Zerg_Creep_Colony;
using BWAPI::UnitTypes::Zerg_Sunken_Colony;
using BWAPI::UnitTypes::Zerg_Spore_Colony;
using BWAPI::UnitTypes::Zerg_Extractor;
using BWAPI::UnitTypes::Zerg_Spawning_Pool;
using BWAPI::UnitTypes::Zerg_Evolution_Chamber;
using BWAPI::UnitTypes::Zerg_Hydralisk_Den;
using BWAPI::UnitTypes::Zerg_Spire;
using BWAPI::UnitTypes::Zerg_Greater_Spire;
using BWAPI::UnitTypes::Zerg_Queens_Nest;
using BWAPI::UnitTypes::Zerg_Nydus_Canal;
using BWAPI::UnitTypes::Zerg_Defiler_Mound;
using BWAPI::UnitTypes::Zerg_Ultralisk_Cavern;
using BWAPI::UnitTypes::Zerg_Infested_Command_Center;
using BWAPI::UnitTypes::Protoss_Probe;
using BWAPI::UnitTypes::Protoss_Zealot;
using BWAPI::UnitTypes::Protoss_Dragoon;
using BWAPI::UnitTypes::Protoss_High_Templar;
using BWAPI::UnitTypes::Protoss_Dark_Templar;
using BWAPI::UnitTypes::Protoss_Archon;
using BWAPI::UnitTypes::Protoss_Reaver;
using BWAPI::UnitTypes::Protoss_Scarab;
using BWAPI::UnitTypes::Protoss_Shuttle;
using BWAPI::UnitTypes::Protoss_Scout;
using BWAPI::UnitTypes::Protoss_Observer;
using BWAPI::UnitTypes::Protoss_Carrier;
using BWAPI::UnitTypes::Protoss_Interceptor;
using BWAPI::UnitTypes::Protoss_Arbiter;
using BWAPI::UnitTypes::Protoss_Corsair;
using BWAPI::UnitTypes::Protoss_Dark_Archon;
using BWAPI::UnitTypes::Protoss_Nexus;
using BWAPI::UnitTypes::Protoss_Pylon;
using BWAPI::UnitTypes::Protoss_Assimilator;
using BWAPI::UnitTypes::Protoss_Gateway;
using BWAPI::UnitTypes::Protoss_Forge;
using BWAPI::UnitTypes::Protoss_Photon_Cannon;
using BWAPI::UnitTypes::Protoss_Cybernetics_Core;
using BWAPI::UnitTypes::Protoss_Robotics_Facility;
using BWAPI::UnitTypes::Protoss_Stargate;
using BWAPI::UnitTypes::Protoss_Citadel_of_Adun;
using BWAPI::UnitTypes::Protoss_Robotics_Support_Bay;
using BWAPI::UnitTypes::Protoss_Fleet_Beacon;
using BWAPI::UnitTypes::Protoss_Templar_Archives;
using BWAPI::UnitTypes::Protoss_Observatory;
using BWAPI::UnitTypes::Protoss_Arbiter_Tribunal;
using BWAPI::UnitTypes::Protoss_Shield_Battery;
using BWAPI::UnitTypes::Spell_Scanner_Sweep;
using BWAPI::UnitTypes::Special_Pit_Door;
using BWAPI::UnitTypes::Special_Right_Pit_Door;
using BWAPI::UnitTypes::Special_Power_Generator;
using BWAPI::UnitTypes::Resource_Mineral_Field;
using BWAPI::UnitTypes::Resource_Vespene_Geyser;
using BWAPI::UnitTypes::Unknown;
} // namespace Enum
} // namespace UnitTypes

namespace TechTypes {
std::vector<TechType> allTechTypes();
extern const TechType None;
extern const TechType Unknown;
extern const TechType Tank_Siege_Mode;
extern const TechType Spider_Mines;
extern const TechType Cloaking_Field;
extern const TechType EMP_Shockwave;
extern const TechType Irradiate;
extern const TechType Defensive_Matrix;
extern const TechType Lurker_Aspect;
extern const TechType Scanner_Sweep;
} // namespace TechTypes

namespace UpgradeTypes {
std::vector<UpgradeType> allUpgradeTypes();
extern const UpgradeType None;
extern const UpgradeType Unknown;
extern const UpgradeType Terran_Vehicle_Weapons;
extern const UpgradeType Terran_Vehicle_Plating;
extern const UpgradeType Ion_Thrusters;
extern const UpgradeType Charon_Boosters;
extern const UpgradeType Metabolic_Boost;
extern const UpgradeType Muscular_Augments;
extern const UpgradeType Grooved_Spines;
extern const UpgradeType Singularity_Charge;
extern const UpgradeType Leg_Enhancements;
extern const UpgradeType Ventral_Sacs;
extern const UpgradeType U_238_Shells;
} // namespace UpgradeTypes

namespace WeaponTypes {
extern const WeaponType None;
extern const WeaponType Gauss_Rifle;
extern const WeaponType C_10_Canister_Rifle;
extern const WeaponType Fragmentation_Grenade;
extern const WeaponType Spider_Mines;
extern const WeaponType Twin_Autocannons;
extern const WeaponType Hellfire_Missile_Pack;
extern const WeaponType Arclite_Cannon;
extern const WeaponType Arclite_Shock_Cannon;
extern const WeaponType Burst_Lasers;
extern const WeaponType Gemini_Missiles;
extern const WeaponType ATS_Laser_Battery;
extern const WeaponType ATA_Laser_Battery;
extern const WeaponType Needle_Spines;
extern const WeaponType Subterranean_Spines;
extern const WeaponType Glave_Wurm;
extern const WeaponType Seeker_Spores;
extern const WeaponType Phase_Disruptor;
extern const WeaponType Psi_Blades;
extern const WeaponType Photon_Cannon;
extern const WeaponType Unknown;
} // namespace WeaponTypes

class BulletType {
public:
    BulletType() = default;
    explicit BulletType(std::string name) : name_(std::move(name)) {}
    std::string getName() const { return name_; }
    std::string toString() const { return name_; }
    const char* c_str() const { return name_.c_str(); }
    operator bool() const { return name_ != "None"; }
    bool operator==(const BulletType& other) const { return name_ == other.name_; }
    bool operator==(bool value) const { return static_cast<bool>(*this) == value; }
    bool operator!=(const BulletType& other) const { return !(*this == other); }
    bool operator<(const BulletType& other) const { return name_ < other.name_; }

private:
    std::string name_ = "None";
};

namespace BulletTypes {
extern const BulletType None;
extern const BulletType Fusion_Cutter_Hit;
extern const BulletType EMP_Missile;
extern const BulletType Gauss_Rifle_Hit;
extern const BulletType Invisible;
extern const BulletType Psionic_Storm;
} // namespace BulletTypes

class BulletInterface {
public:
    int getID() const { return 0; }
    BulletType getType() const { return BulletTypes::None; }
    Unit getSource() const { return nullptr; }
    Unit getTarget() const { return nullptr; }
    Player getPlayer() const { return nullptr; }
    Position getPosition() const { return Position(-1, -1); }
    Position getTargetPosition() const { return Position(-1, -1); }
    double getVelocityX() const { return 0.0; }
    double getVelocityY() const { return 0.0; }
    bool exists() const { return false; }
};

namespace Colors {
extern const Color Green;
extern const Color Purple;
extern const Color Red;
extern const Color Blue;
extern const Color White;
extern const Color Yellow;
extern const Color Orange;
extern const Color Cyan;
extern const Color Brown;
extern const Color Grey;
} // namespace Colors

namespace Text {
extern const char White;
extern const char Yellow;
extern const char Green;
extern const char Red;
namespace Size {
extern const int Small;
extern const int Large;
} // namespace Size
} // namespace Text

namespace Filter {
extern const UnitFilter IsEnemy;
extern const UnitFilter IsAlly;
extern const UnitFilter IsNeutral;
extern const UnitFilter IsOwned;
extern const UnitFilter IsBuilding;
extern const UnitFilter IsWorker;
extern const UnitFilter IsMineralField;
extern const UnitFilter IsRefinery;
extern const UnitFilter IsFlying;
extern const UnitFilter IsFlyingBuilding;
extern const UnitFilter IsSpell;
extern const UnitFilter IsCompleted;
extern const UnitFilter IsVisible;
extern const UnitFilter CanAttack;
} // namespace Filter

namespace Positions {
extern const Position None;
extern const Position Unknown;
extern const Position Invalid;
extern const Position Origin;
} // namespace Positions

namespace TilePositions {
extern const TilePosition None;
extern const TilePosition Unknown;
extern const TilePosition Invalid;
extern const TilePosition Origin;
} // namespace TilePositions

namespace WalkPositions {
extern const WalkPosition None;
extern const WalkPosition Unknown;
extern const WalkPosition Invalid;
extern const WalkPosition Origin;
} // namespace WalkPositions

namespace Flag {
enum Enum {
    UserInput = 1,
    CompleteMapInformation = 2
};
} // namespace Flag

std::ostream& operator<<(std::ostream& stream, const Race& race);
std::ostream& operator<<(std::ostream& stream, const UnitType& type);
std::ostream& operator<<(std::ostream& stream, const WeaponType& type);
std::ostream& operator<<(std::ostream& stream, const TechType& type);
std::ostream& operator<<(std::ostream& stream, const UpgradeType& type);

} // namespace BWAPI
