//20260701_kpopmodder: Replaces SAIDA's tournament MyBotModule only in the local mock runtime.
#include "MyBotModule.h"
#include "BuildManager.h"
#include "ConstructionManager.h"
#include "EnemyStrategyManager.h"
#include "InformationManager.h"
#include "ScoutManager.h"
#include "StrategyManager.h"
#include "TerranConstructionPlaceFinder.h"
#include "TrainManager.h"
#include "UnitManager/ComsatStationManager.h"
#include "UnitManager/DropshipManager.h"
#include "UnitManager/EngineeringBayManager.h"
#include "UnitManager/GoliathManager.h"
#include "UnitManager/MarineManager.h"
#include "UnitManager/ScvManager.h"
#include "UnitManager/TankManager.h"
#include "UnitManager/VessleManager.h"
#include "UnitManager/VultureManager.h"
#include "UnitManager/WraithManager.h"

#include <algorithm>
#include <cctype>
#include <cstdlib>
#include <iostream>
#include <map>
#include <sstream>
#include <string>

using namespace BWAPI;
using namespace MyBot;

namespace {
enum class MockMode {
    Manual,
    InfoProbe,
    InfoUpdate,
    PlaceFinder,
    BuildManager,
    ConstructionManager,
    StrategyManager,
    EnemyStrategyManager,
    ScvManager,
    ScoutManager,
    TrainManager,
    ComsatStationManager,
    EngineeringBayManager,
    MarineManager,
    TankManager,
    VultureManager,
    GoliathManager,
    WraithManager,
    VessleManager,
    DropshipManager,
    CommanderStart,
    CommanderFrame,
    CommanderStartUnsafe,
    CommanderFrameUnsafe
};

std::string normalizedMode()
{
    const char* value = std::getenv("LAV_SAIDA_MOCK_MODE");
    std::string mode = value ? value : "manual";
    std::replace(mode.begin(), mode.end(), '-', '_');
    std::transform(mode.begin(), mode.end(), mode.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return mode;
}

MockMode currentMode()
{
    const std::string mode = normalizedMode();
    if (mode == "commander_frame_unsafe" || mode == "gamecommander_frame_unsafe") {
        return MockMode::CommanderFrameUnsafe;
    }
    if (mode == "commander_start_unsafe" || mode == "gamecommander_start_unsafe") {
        return MockMode::CommanderStartUnsafe;
    }
    if (mode == "info" || mode == "info_probe" || mode == "information" || mode == "information_manager") {
        return MockMode::InfoProbe;
    }
    if (mode == "info_update" || mode == "information_update" || mode == "information_manager_update") {
        return MockMode::InfoUpdate;
    }
    if (mode == "place_finder" || mode == "construction_place" || mode == "terran_construction_place"
        || mode == "terran_construction_place_finder" || mode == "tcpf") {
        return MockMode::PlaceFinder;
    }
    if (mode == "build_manager" || mode == "build" || mode == "bm") {
        return MockMode::BuildManager;
    }
    if (mode == "construction_manager" || mode == "construction" || mode == "cm") {
        return MockMode::ConstructionManager;
    }
    if (mode == "strategy_manager" || mode == "strategy" || mode == "sm") {
        return MockMode::StrategyManager;
    }
    if (mode == "enemy_strategy_manager" || mode == "enemy_strategy" || mode == "esm") {
        return MockMode::EnemyStrategyManager;
    }
    if (mode == "scv_manager" || mode == "scv" || mode == "worker_manager" || mode == "workers") {
        return MockMode::ScvManager;
    }
    if (mode == "scout_manager" || mode == "scout" || mode == "scouting") {
        return MockMode::ScoutManager;
    }
    if (mode == "train_manager" || mode == "train" || mode == "production_manager" || mode == "production") {
        return MockMode::TrainManager;
    }
    if (mode == "comsat_station_manager" || mode == "comsat_manager" || mode == "comsat" || mode == "scan_manager") {
        return MockMode::ComsatStationManager;
    }
    if (mode == "engineering_bay_manager" || mode == "engineering_bay" || mode == "engineering" || mode == "ebay") {
        return MockMode::EngineeringBayManager;
    }
    if (mode == "marine_manager" || mode == "marine" || mode == "marines") {
        return MockMode::MarineManager;
    }
    if (mode == "tank_manager" || mode == "tank" || mode == "tanks" || mode == "siege_tank_manager") {
        return MockMode::TankManager;
    }
    if (mode == "vulture_manager" || mode == "vulture" || mode == "vultures") {
        return MockMode::VultureManager;
    }
    if (mode == "goliath_manager" || mode == "goliath" || mode == "goliaths") {
        return MockMode::GoliathManager;
    }
    if (mode == "wraith_manager" || mode == "wraith" || mode == "wraiths") {
        return MockMode::WraithManager;
    }
    if (mode == "vessle_manager" || mode == "vessle" || mode == "vessles"
        || mode == "science_vessel_manager" || mode == "science_vessel"
        || mode == "vessel_manager" || mode == "vessel" || mode == "vessels") {
        return MockMode::VessleManager;
    }
    if (mode == "dropship_manager" || mode == "dropship" || mode == "dropships" || mode == "drop_manager") {
        return MockMode::DropshipManager;
    }
    if (mode == "commander" || mode == "commander_frame" || mode == "gamecommander") {
        return MockMode::CommanderFrame;
    }
    if (mode == "commander_start" || mode == "gamecommander_start") {
        return MockMode::CommanderStart;
    }
    return MockMode::Manual;
}

const char* modeName(MockMode mode)
{
    switch (mode) {
    case MockMode::CommanderStartUnsafe:
        return "commander-start-unsafe";
    case MockMode::CommanderFrameUnsafe:
        return "commander-frame-unsafe";
    case MockMode::InfoProbe:
        return "info-probe";
    case MockMode::InfoUpdate:
        return "info-update";
    case MockMode::PlaceFinder:
        return "place-finder";
    case MockMode::BuildManager:
        return "build-manager";
    case MockMode::ConstructionManager:
        return "construction-manager";
    case MockMode::StrategyManager:
        return "strategy-manager";
    case MockMode::EnemyStrategyManager:
        return "enemy-strategy-manager";
    case MockMode::ScvManager:
        return "scv-manager";
    case MockMode::ScoutManager:
        return "scout-manager";
    case MockMode::TrainManager:
        return "train-manager";
    case MockMode::ComsatStationManager:
        return "comsat-station-manager";
    case MockMode::EngineeringBayManager:
        return "engineering-bay-manager";
    case MockMode::MarineManager:
        return "marine-manager";
    case MockMode::TankManager:
        return "tank-manager";
    case MockMode::VultureManager:
        return "vulture-manager";
    case MockMode::GoliathManager:
        return "goliath-manager";
    case MockMode::WraithManager:
        return "wraith-manager";
    case MockMode::VessleManager:
        return "vessle-manager";
    case MockMode::DropshipManager:
        return "dropship-manager";
    case MockMode::CommanderStart:
        return "commander-start";
    case MockMode::CommanderFrame:
        return "commander-frame";
    case MockMode::Manual:
    default:
        return "manual";
    }
}

const char* boolText(bool value)
{
    return value ? "true" : "false";
}

template <typename Callback>
bool runCommanderStep(const char* label, Callback callback)
{
    std::cout << "[SAIDA mock] " << label << " begin" << std::endl;
    try {
        callback();
        std::cout << "[SAIDA mock] " << label << " end" << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] " << label << " exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] " << label << " unknown exception" << std::endl;
    }
    return false;
}

bool bwemInitializationAttempted = false;
bool bwemReady = false;
bool informationUnitsSeeded = false;
bool informationProbeDisabled = false;
bool informationUpdateDisabled = false;
bool placeFinderDisabled = false;
bool buildManagerDisabled = false;
bool buildManagerQueueSeeded = false;
bool constructionManagerDisabled = false;
bool constructionManagerTaskSeeded = false;
bool strategyManagerDisabled = false;
bool strategyManagerStarted = false;
bool enemyStrategyManagerDisabled = false;
bool scvManagerDisabled = false;
bool scoutManagerDisabled = false;
bool trainManagerDisabled = false;
bool comsatStationManagerDisabled = false;
bool engineeringBayManagerDisabled = false;
bool marineManagerDisabled = false;
bool tankManagerDisabled = false;
bool vultureManagerDisabled = false;
bool goliathManagerDisabled = false;
bool wraithManagerDisabled = false;
bool vessleManagerDisabled = false;
bool dropshipManagerDisabled = false;

std::string tileText(TilePosition position)
{
    if (!position.isValid()) {
        return "invalid";
    }

    std::ostringstream stream;
    stream << position.x << "," << position.y;
    return stream.str();
}

std::string positionText(Position position)
{
    if (position == Positions::None) {
        return "none";
    }
    if (position == Positions::Unknown) {
        return "unknown";
    }
    if (!position.isValid()) {
        std::ostringstream stream;
        stream << "invalid(" << position.x << "," << position.y << ")";
        return stream.str();
    }

    std::ostringstream stream;
    stream << position.x << "," << position.y;
    return stream.str();
}

bool runBWEMInitialization()
{
    if (bwemReady) {
        return true;
    }
    if (bwemInitializationAttempted && !bwemReady) {
        std::cout << "[SAIDA mock] BWEM initialization previously failed; skipping retry" << std::endl;
        return false;
    }

    bwemInitializationAttempted = true;
    std::cout << "[SAIDA mock] BWEM initialization begin" << std::endl;
    try {
        if (!theMap.Initialized()) {
            theMap.Initialize();
        }
        theMap.EnableAutomaticPathAnalysis();
        const bool startingLocationsOK = theMap.FindBasesForStartingLocations();
        bwemReady = theMap.Initialized() && startingLocationsOK;
        std::cout << "[SAIDA mock] BWEM initialization end initialized=" << boolText(theMap.Initialized())
                  << " startingLocationsOK=" << boolText(startingLocationsOK)
                  << " map=" << theMap.Size().x << "x" << theMap.Size().y
                  << " areas=" << theMap.Areas().size()
                  << " chokes=" << theMap.ChokePointCount()
                  << " bases=" << theMap.BaseCount() << std::endl;
        for (const Area& area : theMap.Areas()) {
            std::cout << "[SAIDA mock] BWEM area id=" << area.Id()
                      << " bases=" << area.Bases().size()
                      << " chokes=" << area.ChokePoints().size()
                      << std::endl;
            for (const ChokePoint* choke : area.ChokePoints()) {
                if (!choke) {
                    continue;
                }
                const auto& areas = choke->GetAreas();
                std::cout << "[SAIDA mock] BWEM choke center=" << choke->Center().x << "," << choke->Center().y
                          << " areas=" << (areas.first ? areas.first->Id() : 0)
                          << "/" << (areas.second ? areas.second->Id() : 0)
                          << " width=" << choke->Pos(ChokePoint::end1).getApproxDistance(choke->Pos(ChokePoint::end2))
                          << std::endl;
            }
            for (const Base& base : area.Bases()) {
                std::cout << "[SAIDA mock] BWEM base location=" << base.Location().x << "," << base.Location().y
                          << " center=" << base.Center().x << "," << base.Center().y
                          << " starting=" << boolText(base.Starting()) << std::endl;
            }
        }
        return bwemReady;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] BWEM initialization exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] BWEM initialization unknown exception" << std::endl;
    }

    bwemReady = false;
    return false;
}

void seedInformationManagerUnits(InformationManager& info)
{
    if (informationUnitsSeeded) {
        return;
    }

    UnitData& selfData = info.getUnitData(S);
    for (Unit unit : S->getUnits()) {
        if (!unit || unit->getType().isNeutral()) {
            continue;
        }
        if (selfData.addUnitNBuilding(unit)) {
            selfData.increaseCreateUnits(unit->getType());
            if (unit->isCompleted()) {
                selfData.increaseCompleteUnits(unit->getType());
            }
        }
    }

    UnitData& enemyData = info.getUnitData(E);
    for (Unit unit : E->getUnits()) {
        if (!unit || unit->getType().isNeutral()) {
            continue;
        }
        enemyData.addUnitNBuilding(unit);
    }

    informationUnitsSeeded = true;
    std::cout << "[SAIDA mock] InformationManager seed units selfUnits="
              << selfData.getAllUnits().size()
              << " selfBuildings=" << selfData.getAllBuildings().size()
              << " enemyUnits=" << enemyData.getAllUnits().size()
              << " enemyBuildings=" << enemyData.getAllBuildings().size() << std::endl;
}

bool runInformationManagerUnitDataSlice()
{
    if (informationProbeDisabled) {
        std::cout << "[SAIDA mock] InformationManager unit-data slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] InformationManager unit-data slice begin" << std::endl;
    if (!runBWEMInitialization()) {
        std::cout << "[SAIDA mock] InformationManager unit-data slice skipped: BWEM not ready" << std::endl;
        return false;
    }

    try {
        InformationManager& info = InformationManager::Instance();
        seedInformationManagerUnits(info);

        UnitData& selfData = info.getUnitData(info.selfPlayer);
        UnitData& enemyData = info.getUnitData(info.enemyPlayer);
        selfData.initializeAllInfo();
        enemyData.updateNcheckTypeAllInfo();
        selfData.updateAllInfo();

        const Base* selfMain = info.getMainBaseLocation(info.selfPlayer);
        const Base* enemyMain = info.getMainBaseLocation(info.enemyPlayer);
        std::cout << "[SAIDA mock] InformationManager unit-data slice end selfUnits="
                  << selfData.getAllUnits().size()
                  << " selfBuildings=" << selfData.getAllBuildings().size()
                  << " enemyUnits=" << enemyData.getAllUnits().size()
                  << " enemyBuildings=" << enemyData.getAllBuildings().size();
        if (selfMain) {
            std::cout << " selfMain=" << selfMain->Location().x << "," << selfMain->Location().y;
        }
        if (enemyMain) {
            std::cout << " enemyMain=" << enemyMain->Location().x << "," << enemyMain->Location().y;
        }
        std::cout << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] InformationManager unit-data slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] InformationManager unit-data slice unknown exception" << std::endl;
    }

    informationProbeDisabled = true;
    return false;
}

bool runInformationManagerUpdateSlice()
{
    if (informationUpdateDisabled) {
        std::cout << "[SAIDA mock] InformationManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] InformationManager.update slice begin" << std::endl;
    if (!runBWEMInitialization()) {
        std::cout << "[SAIDA mock] InformationManager.update slice skipped: BWEM not ready" << std::endl;
        return false;
    }

    try {
        InformationManager& info = InformationManager::Instance();
        seedInformationManagerUnits(info);
        info.update();

        const Base* selfMain = info.getMainBaseLocation(info.selfPlayer);
        const Base* enemyMain = info.getMainBaseLocation(info.enemyPlayer);
        Base* selfFirstExpansion = info.getFirstExpansionLocation(info.selfPlayer);
        Base* enemyFirstExpansion = info.getFirstExpansionLocation(info.enemyPlayer);
        Base* selfSecondExpansion = info.getSecondExpansionLocation(info.selfPlayer);
        Base* enemySecondExpansion = info.getSecondExpansionLocation(info.enemyPlayer);
        const ChokePoint* selfFirstChoke = info.getFirstChokePoint(info.selfPlayer);
        const ChokePoint* enemyFirstChoke = info.getFirstChokePoint(info.enemyPlayer);
        int expansionPathLength = -1;
        const int expansionChokeCount = (selfFirstExpansion && enemyFirstExpansion)
            ? static_cast<int>(theMap.GetPath(selfFirstExpansion->getPosition(), enemyFirstExpansion->getPosition(), &expansionPathLength).size())
            : 0;
        std::cout << "[SAIDA mock] InformationManager.update slice end"
                  << " activation=" << info.getActivationMineralBaseCount()
                  << "/" << info.getActivationGasBaseCount()
                  << " occupied=" << info.getOccupiedBaseLocations(info.selfPlayer).size()
                  << "/" << info.getOccupiedBaseLocations(info.enemyPlayer).size();
        if (selfMain) {
            std::cout << " selfMain=" << selfMain->Location().x << "," << selfMain->Location().y;
        }
        if (enemyMain) {
            std::cout << " enemyMain=" << enemyMain->Location().x << "," << enemyMain->Location().y;
        }
        std::cout << " firstExpansion=" << boolText(selfFirstExpansion != nullptr)
                  << "/" << boolText(enemyFirstExpansion != nullptr)
                  << " secondExpansion=" << boolText(selfSecondExpansion != nullptr)
                  << "/" << boolText(enemySecondExpansion != nullptr)
                  << " firstChoke=" << boolText(selfFirstChoke != nullptr)
                  << "/" << boolText(enemyFirstChoke != nullptr)
                  << " secondChoke=" << boolText(info.getSecondChokePoint(info.selfPlayer) != nullptr)
                  << " expansionPath=" << expansionChokeCount << "/" << expansionPathLength
                  << std::endl;
        if (selfSecondExpansion) {
            std::cout << "[SAIDA mock] selfSecondExpansion="
                      << selfSecondExpansion->Location().x << "," << selfSecondExpansion->Location().y
                      << " center=" << selfSecondExpansion->Center().x << "," << selfSecondExpansion->Center().y
                      << std::endl;
        }
        if (enemySecondExpansion) {
            std::cout << "[SAIDA mock] enemySecondExpansion="
                      << enemySecondExpansion->Location().x << "," << enemySecondExpansion->Location().y
                      << " center=" << enemySecondExpansion->Center().x << "," << enemySecondExpansion->Center().y
                      << std::endl;
        }
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] InformationManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] InformationManager.update slice unknown exception" << std::endl;
    }

    informationUpdateDisabled = true;
    return false;
}

bool runTerranConstructionPlaceFinderSlice()
{
    if (placeFinderDisabled) {
        std::cout << "[SAIDA mock] TerranConstructionPlaceFinder.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] TerranConstructionPlaceFinder.update slice begin" << std::endl;
    if (!runInformationManagerUpdateSlice()) {
        std::cout << "[SAIDA mock] TerranConstructionPlaceFinder.update slice skipped: InformationManager.update failed"
                  << std::endl;
        return false;
    }

    try {
        TerranConstructionPlaceFinder& finder = TerranConstructionPlaceFinder::Instance();
        finder.update();

        const TilePosition barracks = finder.getBarracksPositionInSCP();
        const TilePosition engineeringBay = finder.getEngineeringBayPositionInSCP();
        std::cout << "[SAIDA mock] TerranConstructionPlaceFinder.update slice end"
                  << " barracksSCP=" << tileText(barracks)
                  << " engineeringBaySCP=" << tileText(engineeringBay)
                  << " secondChoke=" << boolText(INFO.getSecondChokePoint(S) != nullptr)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] TerranConstructionPlaceFinder.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] TerranConstructionPlaceFinder.update slice unknown exception" << std::endl;
    }

    placeFinderDisabled = true;
    return false;
}

void seedBuildManagerQueue(BuildManager& manager)
{
    if (buildManagerQueueSeeded) {
        return;
    }

    manager.buildQueue.clearAll();
    manager.buildQueue.queueAsHighestPriority(UnitTypes::Terran_SCV, false);
    buildManagerQueueSeeded = true;

    std::cout << "[SAIDA mock] BuildManager seed queue item=Terran SCV queueSize="
              << manager.buildQueue.size() << std::endl;
}

bool runBuildManagerSlice(bool seedProbe = true)
{
    if (buildManagerDisabled) {
        std::cout << "[SAIDA mock] BuildManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] BuildManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runTerranConstructionPlaceFinderSlice()) {
        std::cout << "[SAIDA mock] BuildManager.update slice skipped: TerranConstructionPlaceFinder.update failed"
                  << std::endl;
        return false;
    }

    try {
        BuildManager& manager = BuildManager::Instance();
        if (seedProbe) {
            seedBuildManagerQueue(manager);
        }

        const size_t queueBefore = manager.buildQueue.size();
        manager.update();
        const size_t queueAfter = manager.buildQueue.size();

        std::cout << "[SAIDA mock] BuildManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " queue=" << queueBefore << "/" << queueAfter
                  << " mineralsAvailable=" << manager.getAvailableMinerals()
                  << " gasAvailable=" << manager.getAvailableGas()
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] BuildManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] BuildManager.update slice unknown exception" << std::endl;
    }

    buildManagerDisabled = true;
    return false;
}

Unit firstAvailableConstructionWorker()
{
    for (Unit unit : S->getUnits()) {
        if (unit && unit->getType().isWorker() && unit->exists() && unit->isCompleted()) {
            return unit;
        }
    }
    return nullptr;
}

std::string constructionQueueSummary(ConstructionManager& manager)
{
    const vector<ConstructionTask>* queue = manager.getConstructionQueue();
    size_t unassigned = 0;
    size_t assigned = 0;
    size_t underConstruction = 0;
    size_t waiting = 0;

    for (const ConstructionTask& task : *queue) {
        if (task.status == ConstructionStatus::Unassigned) {
            ++unassigned;
        } else if (task.status == ConstructionStatus::Assigned) {
            ++assigned;
        } else if (task.status == ConstructionStatus::UnderConstruction) {
            ++underConstruction;
        } else if (task.status == ConstructionStatus::WaitToAssign) {
            ++waiting;
        }
    }

    std::ostringstream stream;
    stream << "size=" << queue->size()
           << " unassigned=" << unassigned
           << " assigned=" << assigned
           << " underConstruction=" << underConstruction
           << " waiting=" << waiting;
    return stream.str();
}

void seedConstructionManagerTask(ConstructionManager& manager)
{
    if (constructionManagerTaskSeeded) {
        return;
    }

    Unit builder = firstAvailableConstructionWorker();
    if (!builder) {
        std::cout << "[SAIDA mock] ConstructionManager seed skipped: no worker" << std::endl;
        constructionManagerTaskSeeded = true;
        return;
    }

    const TilePosition desiredPosition(10, 20);
    manager.addConstructionTask(UnitTypes::Terran_Supply_Depot, desiredPosition, builder);
    constructionManagerTaskSeeded = true;

    std::cout << "[SAIDA mock] ConstructionManager seed task building=Terran Supply Depot"
              << " desired=" << tileText(desiredPosition)
              << " builder=" << builder->getID()
              << " queue=" << constructionQueueSummary(manager)
              << " reserved=" << manager.getReservedMinerals()
              << "/" << manager.getReservedGas()
              << std::endl;
}

bool runConstructionManagerSlice(bool seedProbe = true)
{
    if (constructionManagerDisabled) {
        std::cout << "[SAIDA mock] ConstructionManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] ConstructionManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runBuildManagerSlice(seedProbe)) {
        std::cout << "[SAIDA mock] ConstructionManager.update slice skipped: BuildManager.update failed" << std::endl;
        return false;
    }

    try {
        ConstructionManager& manager = ConstructionManager::Instance();
        if (seedProbe) {
            seedConstructionManagerTask(manager);
        }

        const std::string queueBefore = constructionQueueSummary(manager);
        manager.update();
        const std::string queueAfter = constructionQueueSummary(manager);

        std::cout << "[SAIDA mock] ConstructionManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " queueBefore={" << queueBefore << "}"
                  << " queueAfter={" << queueAfter << "}"
                  << " reserved=" << manager.getReservedMinerals()
                  << "/" << manager.getReservedGas()
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] ConstructionManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] ConstructionManager.update slice unknown exception" << std::endl;
    }

    constructionManagerDisabled = true;
    return false;
}

std::string buildQueueSummary(BuildManager& manager)
{
    const deque<BuildOrderItem>* queue = manager.buildQueue.getQueue();
    std::ostringstream stream;
    stream << "size=" << queue->size();
    if (!queue->empty()) {
        const BuildOrderItem& item = queue->back();
        stream << " top=" << item.metaType.getName()
               << " blocking=" << boolText(item.blocking)
               << " seed=" << tileText(item.seedLocation)
               << " strategy=" << item.seedLocationStrategy;
    }
    return stream.str();
}

bool runStrategyManagerSlice()
{
    if (strategyManagerDisabled) {
        std::cout << "[SAIDA mock] StrategyManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] StrategyManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runConstructionManagerSlice(false)) {
        std::cout << "[SAIDA mock] StrategyManager.update slice skipped: ConstructionManager.update failed" << std::endl;
        return false;
    }

    try {
        StrategyManager& strategy = StrategyManager::Instance();
        if (!strategyManagerStarted) {
            strategy.onStart();
            strategyManagerStarted = true;
            std::cout << "[SAIDA mock] StrategyManager.onStart slice end" << std::endl;
        }

        BuildManager& buildManager = BuildManager::Instance();
        const std::string queueBefore = buildQueueSummary(buildManager);
        strategy.update();
        const std::string queueAfter = buildQueueSummary(buildManager);
        const Position attackPosition = strategy.getMainAttackPosition();

        std::cout << "[SAIDA mock] StrategyManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " buildQueueBefore={" << queueBefore << "}"
                  << " buildQueueAfter={" << queueAfter << "}"
                  << " needUpgrade=" << boolText(strategy.getNeedUpgrade())
                  << " needTank=" << boolText(strategy.getNeedTank())
                  << " attackPos=" << attackPosition.x << "," << attackPosition.y
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] StrategyManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] StrategyManager.update slice unknown exception" << std::endl;
    }

    strategyManagerDisabled = true;
    return false;
}

bool runEnemyStrategyManagerSlice()
{
    if (enemyStrategyManagerDisabled) {
        std::cout << "[SAIDA mock] EnemyStrategyManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] EnemyStrategyManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runStrategyManagerSlice()) {
        std::cout << "[SAIDA mock] EnemyStrategyManager.update slice skipped: StrategyManager.update failed" << std::endl;
        return false;
    }

    try {
        EnemyStrategyManager& enemyStrategy = EnemyStrategyManager::Instance();
        const std::string initialBefore = enemyStrategy.getEnemyInitialBuild().getName();
        const std::string mainBefore = enemyStrategy.getEnemyMainBuild().getName();
        const size_t initialHistoryBefore = enemyStrategy.getEIBHistory().size();
        const size_t mainHistoryBefore = enemyStrategy.getEMBHistory().size();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 24 * 30 && Broodwar->getFrameCount() % 12 == 0;

        enemyStrategy.update();

        const std::string initialAfter = enemyStrategy.getEnemyInitialBuild().getName();
        const std::string mainAfter = enemyStrategy.getEnemyMainBuild().getName();
        const size_t initialHistoryAfter = enemyStrategy.getEIBHistory().size();
        const size_t mainHistoryAfter = enemyStrategy.getEMBHistory().size();
        Unit gasRushRefinery = enemyStrategy.getEnemyGasRushRefinery();

        std::cout << "[SAIDA mock] EnemyStrategyManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " initialBuild=" << initialBefore << "->" << initialAfter
                  << " mainBuild=" << mainBefore << "->" << mainAfter
                  << " eibHistory=" << initialHistoryBefore << "->" << initialHistoryAfter
                  << " embHistory=" << mainHistoryBefore << "->" << mainHistoryAfter
                  << " waitDrop=" << enemyStrategy.getWaitTimeForDrop()
                  << " gasRushRefinery=" << (gasRushRefinery ? gasRushRefinery->getID() : 0)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] EnemyStrategyManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] EnemyStrategyManager.update slice unknown exception" << std::endl;
    }

    enemyStrategyManagerDisabled = true;
    return false;
}

std::string scvStateSummary()
{
    const uList scvs = INFO.getUnits(UnitTypes::Terran_SCV, S);
    std::map<std::string, int> counts;
    for (UnitInfo* scv : scvs) {
        counts[scv ? scv->getState() : "null"]++;
    }

    std::ostringstream stream;
    stream << "total=" << scvs.size();
    for (const auto& entry : counts) {
        stream << " " << entry.first << "=" << entry.second;
    }
    return stream.str();
}

std::string unitInfoStateSummary(UnitType type, Player player)
{
    const uList units = INFO.getBuildings(type, player);
    std::map<std::string, int> counts;
    for (UnitInfo* unit : units) {
        counts[unit ? unit->getState() : "null"]++;
    }

    std::ostringstream stream;
    stream << type.getName() << "=" << units.size();
    for (const auto& entry : counts) {
        stream << " " << entry.first << "=" << entry.second;
    }
    return stream.str();
}

std::string unitStateSummary(UnitType type, Player player)
{
    const uList units = INFO.getUnits(type, player);
    std::map<std::string, int> counts;
    for (UnitInfo* unit : units) {
        counts[unit ? unit->getState() : "null"]++;
    }

    std::ostringstream stream;
    stream << type.getName() << "=" << units.size();
    for (const auto& entry : counts) {
        stream << " " << entry.first << "=" << entry.second;
    }
    return stream.str();
}

std::string tankStateSummary()
{
    std::ostringstream stream;
    stream << unitStateSummary(UnitTypes::Terran_Siege_Tank_Tank_Mode, S)
           << " " << unitStateSummary(UnitTypes::Terran_Siege_Tank_Siege_Mode, S);
    return stream.str();
}

std::string vultureStateSummary()
{
    std::ostringstream stream;
    stream << unitStateSummary(UnitTypes::Terran_Vulture, S)
           << " " << unitStateSummary(UnitTypes::Terran_Vulture_Spider_Mine, S);
    return stream.str();
}

std::string goliathStateSummary()
{
    return unitStateSummary(UnitTypes::Terran_Goliath, S);
}

std::string wraithStateSummary(Player player)
{
    return unitStateSummary(UnitTypes::Terran_Wraith, player);
}

std::string vessleStateSummary(Player player)
{
    return unitStateSummary(UnitTypes::Terran_Science_Vessel, player);
}

std::string dropshipStateSummary()
{
    return unitStateSummary(UnitTypes::Terran_Dropship, S);
}

std::string dropshipSpaceSummary()
{
    const uList dropships = INFO.getUnits(UnitTypes::Terran_Dropship, S);
    int emptyCount = 0;
    int fullCount = 0;
    int totalRemaining = 0;

    for (UnitInfo* dropship : dropships) {
        Unit unit = dropship ? dropship->unit() : nullptr;
        if (!unit) {
            continue;
        }

        const int remaining = unit->getSpaceRemaining();
        totalRemaining += remaining;
        if (remaining == UnitTypes::Terran_Dropship.spaceProvided()) {
            emptyCount++;
        }
        if (remaining == 0) {
            fullCount++;
        }
    }

    std::ostringstream stream;
    stream << "empty=" << emptyCount
           << " full=" << fullCount
           << " totalRemaining=" << totalRemaining;
    return stream.str();
}

bool runScvManagerSlice()
{
    if (scvManagerDisabled) {
        std::cout << "[SAIDA mock] ScvManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] ScvManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runEnemyStrategyManagerSlice()) {
        std::cout << "[SAIDA mock] ScvManager.update slice skipped: EnemyStrategyManager.update failed" << std::endl;
        return false;
    }

    try {
        ScvManager& scvManager = ScvManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() % FR == 0;
        const std::string statesBefore = scvStateSummary();
        const int mineralBefore = scvManager.getAllMineralScvCount();
        const int gasBefore = scvManager.getAllRefineryScvCount();
        const int repairBefore = scvManager.getRepairScvCount();

        scvManager.update();

        Unit scanTarget = scvManager.getScanMyBaseUnit();
        std::cout << "[SAIDA mock] ScvManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " statesBefore={" << statesBefore << "}"
                  << " statesAfter={" << scvStateSummary() << "}"
                  << " mineralScvs=" << mineralBefore << "->" << scvManager.getAllMineralScvCount()
                  << " gasScvs=" << gasBefore << "->" << scvManager.getAllRefineryScvCount()
                  << " repairScvs=" << repairBefore << "->" << scvManager.getRepairScvCount()
                  << " needRefinery=" << scvManager.getNeedCountForRefinery()
                  << " scanMyBaseUnit=" << (scanTarget ? scanTarget->getID() : 0)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] ScvManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] ScvManager.update slice unknown exception" << std::endl;
    }

    scvManagerDisabled = true;
    return false;
}

const char* scoutStatusName(int status)
{
    switch (status) {
    case ScoutStatus::NoScout:
        return "NoScout";
    case ScoutStatus::AssignScout:
        return "AssignScout";
    case ScoutStatus::LookAroundMyMainBase:
        return "LookAroundMyMainBase";
    case ScoutStatus::WaitAtMySecondChokePoint:
        return "WaitAtMySecondChokePoint";
    case ScoutStatus::MovingToAnotherBaseLocation:
        return "MovingToAnotherBaseLocation";
    case ScoutStatus::MoveAroundEnemyBaseLocation:
        return "MoveAroundEnemyBaseLocation";
    case ScoutStatus::CheckEnemyFirstExpansion:
        return "CheckEnemyFirstExpansion";
    case ScoutStatus::WaitAtEnemyFirstExpansion:
        return "WaitAtEnemyFirstExpansion";
    case ScoutStatus::FinishFirstScout:
        return "FinishFirstScout";
    default:
        return "UnknownScoutStatus";
    }
}

bool runScoutManagerSlice()
{
    if (scoutManagerDisabled) {
        std::cout << "[SAIDA mock] ScoutManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] ScoutManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runScvManagerSlice()) {
        std::cout << "[SAIDA mock] ScoutManager.update slice skipped: ScvManager.update failed" << std::endl;
        return false;
    }

    try {
        ScoutManager& scoutManager = ScoutManager::Instance();
        Unit scoutBefore = scoutManager.getScoutUnit();
        const int statusBefore = scoutManager.getScoutStatus();
        const std::string statesBefore = scvStateSummary();

        scoutManager.update();

        Unit scoutAfter = scoutManager.getScoutUnit();
        const int statusAfter = scoutManager.getScoutStatus();
        std::cout << "[SAIDA mock] ScoutManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " status=" << scoutStatusName(statusBefore) << "->" << scoutStatusName(statusAfter)
                  << " scoutUnit=" << (scoutBefore ? scoutBefore->getID() : 0)
                  << "->" << (scoutAfter ? scoutAfter->getID() : 0)
                  << " statesBefore={" << statesBefore << "}"
                  << " statesAfter={" << scvStateSummary() << "}"
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] ScoutManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] ScoutManager.update slice unknown exception" << std::endl;
    }

    scoutManagerDisabled = true;
    return false;
}

bool runTrainManagerSlice()
{
    if (trainManagerDisabled) {
        std::cout << "[SAIDA mock] TrainManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] TrainManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runScoutManagerSlice()) {
        std::cout << "[SAIDA mock] TrainManager.update slice skipped: ScoutManager.update failed" << std::endl;
        return false;
    }

    try {
        TrainManager& trainManager = TrainManager::Instance();
        bool originalCalled = false;
        const bool updateBodyActive = Broodwar->getFrameCount() % 4 == 0;
        const int mineralsBefore = trainManager.getAvailableMinerals();
        const int gasBefore = trainManager.getAvailableGas();
        const int scvBefore = INFO.getAllCount(UnitTypes::Terran_SCV, S);
        const std::string commandCentersBefore = unitInfoStateSummary(UnitTypes::Terran_Command_Center, S);

        trainManager.update();
        originalCalled = true;

        std::cout << "[SAIDA mock] TrainManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " availableMinerals=" << mineralsBefore << "->" << trainManager.getAvailableMinerals()
                  << " availableGas=" << gasBefore << "->" << trainManager.getAvailableGas()
                  << " scvs=" << scvBefore << "->" << INFO.getAllCount(UnitTypes::Terran_SCV, S)
                  << " commandCentersBefore={" << commandCentersBefore << "}"
                  << " commandCentersAfter={" << unitInfoStateSummary(UnitTypes::Terran_Command_Center, S) << "}"
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] TrainManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] TrainManager.update slice unknown exception" << std::endl;
    }

    trainManagerDisabled = true;
    return false;
}

bool runComsatStationManagerSlice()
{
    if (comsatStationManagerDisabled) {
        std::cout << "[SAIDA mock] ComsatStationManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] ComsatStationManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runTrainManagerSlice()) {
        std::cout << "[SAIDA mock] ComsatStationManager.update slice skipped: TrainManager.update failed" << std::endl;
        return false;
    }

    try {
        ComsatStationManager& comsatManager = ComsatStationManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 12 == 0;
        const int managerScansBefore = comsatManager.getAvailableScanCount();
        const int infoScansBefore = INFO.getAvailableScanCount();
        const size_t scanSweepsBefore = INFO.getUnits(UnitTypes::Spell_Scanner_Sweep, S).size();
        const std::string comsatsBefore = unitInfoStateSummary(UnitTypes::Terran_Comsat_Station, S);

        comsatManager.update();

        std::cout << "[SAIDA mock] ComsatStationManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " managerScans=" << managerScansBefore << "->" << comsatManager.getAvailableScanCount()
                  << " infoScans=" << infoScansBefore << "->" << INFO.getAvailableScanCount()
                  << " scanSweeps=" << scanSweepsBefore << "->" << INFO.getUnits(UnitTypes::Spell_Scanner_Sweep, S).size()
                  << " comsatsBefore={" << comsatsBefore << "}"
                  << " comsatsAfter={" << unitInfoStateSummary(UnitTypes::Terran_Comsat_Station, S) << "}"
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] ComsatStationManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] ComsatStationManager.update slice unknown exception" << std::endl;
    }

    comsatStationManagerDisabled = true;
    return false;
}

bool runEngineeringBayManagerSlice()
{
    if (engineeringBayManagerDisabled) {
        std::cout << "[SAIDA mock] EngineeringBayManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] EngineeringBayManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runComsatStationManagerSlice()) {
        std::cout << "[SAIDA mock] EngineeringBayManager.update slice skipped: ComsatStationManager.update failed"
                  << std::endl;
        return false;
    }

    try {
        EngineeringBayManager& engineeringBayManager = EngineeringBayManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() % 4 == 0;
        const std::string engineeringBaysBefore = unitInfoStateSummary(UnitTypes::Terran_Engineering_Bay, S);

        engineeringBayManager.update();

        std::cout << "[SAIDA mock] EngineeringBayManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " engineeringBaysBefore={" << engineeringBaysBefore << "}"
                  << " engineeringBaysAfter={" << unitInfoStateSummary(UnitTypes::Terran_Engineering_Bay, S) << "}"
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] EngineeringBayManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] EngineeringBayManager.update slice unknown exception" << std::endl;
    }

    engineeringBayManagerDisabled = true;
    return false;
}

bool runMarineManagerSlice()
{
    if (marineManagerDisabled) {
        std::cout << "[SAIDA mock] MarineManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] MarineManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runEngineeringBayManagerSlice()) {
        std::cout << "[SAIDA mock] MarineManager.update slice skipped: EngineeringBayManager.update failed"
                  << std::endl;
        return false;
    }

    try {
        MarineManager& marineManager = MarineManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 2 == 0;
        const std::string marinesBefore = unitStateSummary(UnitTypes::Terran_Marine, S);
        Unit bunkerBefore = marineManager.getBunker();
        Unit firstBarrackBefore = marineManager.getFirstBarrack();
        Unit nextBarrackSupplyBefore = marineManager.getNextBarrackSupply();
        const bool rangeCloseBefore = marineManager.isRangeUnitClose();

        marineManager.update();

        Unit bunkerAfter = marineManager.getBunker();
        Unit firstBarrackAfter = marineManager.getFirstBarrack();
        Unit nextBarrackSupplyAfter = marineManager.getNextBarrackSupply();

        std::cout << "[SAIDA mock] MarineManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " marinesBefore={" << marinesBefore << "}"
                  << " marinesAfter={" << unitStateSummary(UnitTypes::Terran_Marine, S) << "}"
                  << " bunker=" << (bunkerBefore ? bunkerBefore->getID() : 0)
                  << "->" << (bunkerAfter ? bunkerAfter->getID() : 0)
                  << " firstBarrack=" << (firstBarrackBefore ? firstBarrackBefore->getID() : 0)
                  << "->" << (firstBarrackAfter ? firstBarrackAfter->getID() : 0)
                  << " nextBarrackSupply=" << (nextBarrackSupplyBefore ? nextBarrackSupplyBefore->getID() : 0)
                  << "->" << (nextBarrackSupplyAfter ? nextBarrackSupplyAfter->getID() : 0)
                  << " rangeUnitClose=" << boolText(rangeCloseBefore)
                  << "->" << boolText(marineManager.isRangeUnitClose())
                  << " waitingNearCommand=" << marineManager.waitingNearCommand.x
                  << "," << marineManager.waitingNearCommand.y
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] MarineManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] MarineManager.update slice unknown exception" << std::endl;
    }

    marineManagerDisabled = true;
    return false;
}

bool runTankManagerSlice()
{
    if (tankManagerDisabled) {
        std::cout << "[SAIDA mock] TankManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] TankManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runMarineManagerSlice()) {
        std::cout << "[SAIDA mock] TankManager.update slice skipped: MarineManager.update failed" << std::endl;
        return false;
    }

    try {
        TankManager& tankManager = TankManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 2 == 0;
        const std::string tanksBefore = tankStateSummary();
        const size_t firstDefencePositionsBefore = tankManager.firstDefencePositions.size();
        const int usableBefore = tankManager.getUsableTankCnt();
        const size_t keepMultiBefore = tankManager.getKeepMultiTanks(1).size();
        const size_t keepMulti2Before = tankManager.getKeepMultiTanks(2).size();
        const size_t baseDefenceBefore = tankManager.getBaseDefenceTankSet().size();
        const int dropshipBefore = tankManager.getDropshipTankNum();
        UnitInfo* frontBefore = tankManager.frontTankOfNotDefenceTank;
        const bool hasSelfMain = INFO.getMainBaseLocation(S) != nullptr;
        const bool hasSelfFirstExpansion = INFO.getFirstExpansionLocation(S) != nullptr;
        const bool hasSelfFirstChoke = INFO.getFirstChokePoint(S) != nullptr;
        const bool hasSelfSecondChoke = INFO.getSecondChokePoint(S) != nullptr;
        const bool canCallOriginalBody = !updateBodyActive
            || (hasSelfMain && hasSelfFirstExpansion && hasSelfFirstChoke && hasSelfSecondChoke);
        bool originalCalled = false;

        if (canCallOriginalBody) {
            tankManager.update();
            originalCalled = true;
        } else {
            std::cout << "[SAIDA mock] TankManager.update slice TODO: skipped original body"
                      << " because TankManager::getClosestPosition() needs mock second choke data"
                      << std::endl;
        }

        UnitInfo* frontAfter = tankManager.frontTankOfNotDefenceTank;
        std::cout << "[SAIDA mock] TankManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " prereqMain=" << boolText(hasSelfMain)
                  << " prereqFirstExpansion=" << boolText(hasSelfFirstExpansion)
                  << " prereqFirstChoke=" << boolText(hasSelfFirstChoke)
                  << " prereqSecondChoke=" << boolText(hasSelfSecondChoke)
                  << " tanksBefore={" << tanksBefore << "}"
                  << " tanksAfter={" << tankStateSummary() << "}"
                  << " firstDefencePositions=" << firstDefencePositionsBefore
                  << "->" << tankManager.firstDefencePositions.size()
                  << " usable=" << usableBefore << "->" << tankManager.getUsableTankCnt()
                  << " keepMulti=" << keepMultiBefore << "->" << tankManager.getKeepMultiTanks(1).size()
                  << " keepMulti2=" << keepMulti2Before << "->" << tankManager.getKeepMultiTanks(2).size()
                  << " baseDefence=" << baseDefenceBefore << "->" << tankManager.getBaseDefenceTankSet().size()
                  << " dropship=" << dropshipBefore << "->" << tankManager.getDropshipTankNum()
                  << " siegeModeDefenceTank=" << tankManager.getSiegeModeDefenceTank()
                  << " zealotAllinRush=" << boolText(tankManager.getZealotAllinRush())
                  << " frontTank=" << (frontBefore ? frontBefore->id() : 0)
                  << "->" << (frontAfter ? frontAfter->id() : 0)
                  << " waitingPosition=" << positionText(tankManager.waitingPosition)
                  << " nextMovingPoint=" << positionText(tankManager.getNextMovingPoint())
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] TankManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] TankManager.update slice unknown exception" << std::endl;
    }

    tankManagerDisabled = true;
    return false;
}

bool runVultureManagerSlice()
{
    if (vultureManagerDisabled) {
        std::cout << "[SAIDA mock] VultureManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] VultureManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runTankManagerSlice()) {
        std::cout << "[SAIDA mock] VultureManager.update slice skipped: TankManager.update failed" << std::endl;
        return false;
    }

    try {
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 2 == 0;
        const bool hasSelfMain = INFO.getMainBaseLocation(S) != nullptr;
        const bool hasEnemyMain = INFO.getMainBaseLocation(E) != nullptr;
        const bool hasSelfFirstExpansion = INFO.getFirstExpansionLocation(S) != nullptr;
        const bool hasSelfSecondChoke = INFO.getSecondChokePoint(S) != nullptr;
        const size_t vulturesBeforeCount = INFO.getUnits(UnitTypes::Terran_Vulture, S).size();
        const std::string vulturesBefore = vultureStateSummary();

        if (!hasEnemyMain) {
            std::cout << "[SAIDA mock] VultureManager.update slice TODO: skipped manager construction"
                      << " because VultureKiting::initParam() needs enemy main base data"
                      << std::endl;
            std::cout << "[SAIDA mock] VultureManager.update slice end"
                      << " frame=" << Broodwar->getFrameCount()
                      << " bodyActive=" << boolText(updateBodyActive)
                      << " originalCalled=false"
                      << " prereqMain=" << boolText(hasSelfMain)
                      << " prereqEnemyMain=" << boolText(hasEnemyMain)
                      << " prereqFirstExpansion=" << boolText(hasSelfFirstExpansion)
                      << " prereqSecondChoke=" << boolText(hasSelfSecondChoke)
                      << " vulturesBefore={" << vulturesBefore << "}"
                      << " vulturesAfter={" << vultureStateSummary() << "}"
                      << std::endl;
            return true;
        }

        VultureManager& vultureManager = VultureManager::Instance();
        UnitInfo* frontBefore = vultureManager.getFrontVultureFromPos(SM.getMainAttackPosition());
        const bool needsFullMapPrereqs = updateBodyActive && vulturesBeforeCount > 0;
        const bool canCallOriginalBody = !needsFullMapPrereqs
            || (hasSelfMain && hasEnemyMain && hasSelfFirstExpansion && hasSelfSecondChoke);
        bool originalCalled = false;

        if (canCallOriginalBody) {
            vultureManager.update();
            originalCalled = true;
        } else {
            std::cout << "[SAIDA mock] VultureManager.update slice TODO: skipped original body"
                      << " because active vultures need mock second choke data"
                      << std::endl;
        }

        UnitInfo* frontAfter = vultureManager.getFrontVultureFromPos(SM.getMainAttackPosition());
        std::cout << "[SAIDA mock] VultureManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " prereqMain=" << boolText(hasSelfMain)
                  << " prereqEnemyMain=" << boolText(hasEnemyMain)
                  << " prereqFirstExpansion=" << boolText(hasSelfFirstExpansion)
                  << " prereqSecondChoke=" << boolText(hasSelfSecondChoke)
                  << " vulturesBefore={" << vulturesBefore << "}"
                  << " vulturesAfter={" << vultureStateSummary() << "}"
                  << " needPcontrol=" << boolText(vultureManager.getNeedPcon())
                  << " diveDone=" << boolText(vultureManager.diveDone)
                  << " scoutDone=" << boolText(vultureManager.scoutDone)
                  << " needScoutCnt=" << vultureManager.needScoutCnt
                  << " lastScoutTime=" << vultureManager.lastScoutTime
                  << " checkedForwardPylon=" << boolText(vultureManager.checkedForwardPylon)
                  << " forwardBuildingPosition=" << positionText(vultureManager.forwardBuildingPosition)
                  << " frontVulture=" << (frontBefore ? frontBefore->id() : 0)
                  << "->" << (frontAfter ? frontAfter->id() : 0)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] VultureManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] VultureManager.update slice unknown exception" << std::endl;
    }

    vultureManagerDisabled = true;
    return false;
}

bool runGoliathManagerSlice()
{
    if (goliathManagerDisabled) {
        std::cout << "[SAIDA mock] GoliathManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] GoliathManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runVultureManagerSlice()) {
        std::cout << "[SAIDA mock] GoliathManager.update slice skipped: VultureManager.update failed" << std::endl;
        return false;
    }

    try {
        GoliathManager& goliathManager = GoliathManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 2 == 0;
        const bool hasSelfMain = INFO.getMainBaseLocation(S) != nullptr;
        const bool hasEnemyMain = INFO.getMainBaseLocation(E) != nullptr;
        const bool hasSelfFirstExpansion = INFO.getFirstExpansionLocation(S) != nullptr;
        const bool hasSelfSecondChoke = INFO.getSecondChokePoint(S) != nullptr;
        const size_t goliathsBeforeCount = INFO.getUnits(UnitTypes::Terran_Goliath, S).size();
        const std::string goliathsBefore = goliathStateSummary();
        const int usableBefore = goliathManager.getUsableGoliathCnt();
        const bool enoughForDropBefore = goliathManager.enoughGoliathForDrop();
        UnitInfo* frontBefore = goliathManager.getFrontGoliathFromPos(SM.getMainAttackPosition());
        const bool needsFullMapPrereqs = updateBodyActive && goliathsBeforeCount > 0;
        const bool canCallOriginalBody = !needsFullMapPrereqs
            || (hasSelfMain && hasEnemyMain && hasSelfFirstExpansion && hasSelfSecondChoke);
        bool originalCalled = false;

        if (canCallOriginalBody) {
            goliathManager.update();
            originalCalled = true;
        } else {
            std::cout << "[SAIDA mock] GoliathManager.update slice TODO: skipped original body"
                      << " because active goliaths need mock second choke data"
                      << std::endl;
        }

        UnitInfo* frontAfter = goliathManager.getFrontGoliathFromPos(SM.getMainAttackPosition());
        std::cout << "[SAIDA mock] GoliathManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " prereqMain=" << boolText(hasSelfMain)
                  << " prereqEnemyMain=" << boolText(hasEnemyMain)
                  << " prereqFirstExpansion=" << boolText(hasSelfFirstExpansion)
                  << " prereqSecondChoke=" << boolText(hasSelfSecondChoke)
                  << " goliathsBefore={" << goliathsBefore << "}"
                  << " goliathsAfter={" << goliathStateSummary() << "}"
                  << " usable=" << usableBefore << "->" << goliathManager.getUsableGoliathCnt()
                  << " enoughForDrop=" << boolText(enoughForDropBefore)
                  << "->" << boolText(goliathManager.enoughGoliathForDrop())
                  << " frontGoliath=" << (frontBefore ? frontBefore->id() : 0)
                  << "->" << (frontAfter ? frontAfter->id() : 0)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] GoliathManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] GoliathManager.update slice unknown exception" << std::endl;
    }

    goliathManagerDisabled = true;
    return false;
}

bool runWraithManagerSlice()
{
    if (wraithManagerDisabled) {
        std::cout << "[SAIDA mock] WraithManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] WraithManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runGoliathManagerSlice()) {
        std::cout << "[SAIDA mock] WraithManager.update slice skipped: GoliathManager.update failed" << std::endl;
        return false;
    }

    try {
        WraithManager& wraithManager = WraithManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 2 == 0;
        const bool hasSelfMain = INFO.getMainBaseLocation(S) != nullptr;
        const bool hasSelfFirstExpansion = INFO.getFirstExpansionLocation(S) != nullptr;
        const bool hasSelfSecondChoke = INFO.getSecondChokePoint(S) != nullptr;
        const bool hasEnemyFirstExpansion = INFO.getFirstExpansionLocation(E) != nullptr;
        const size_t selfWraithsBeforeCount = INFO.getUnits(UnitTypes::Terran_Wraith, S).size();
        const std::string selfWraithsBefore = wraithStateSummary(S);
        const std::string enemyWraithsBefore = wraithStateSummary(E);
        const std::string enemyValkyriesBefore = unitStateSummary(UnitTypes::Terran_Valkyrie, E);
        const size_t enemyOccupiedBasesBefore = INFO.getOccupiedBaseLocations(E).size();
        Position killScvTargetBefore = Positions::Unknown;
        if (hasSelfFirstExpansion) {
            killScvTargetBefore = wraithManager.getKillScvTargetBase();
        }

        const bool needsFullMapPrereqs = updateBodyActive && selfWraithsBeforeCount > 0;
        const bool canCallOriginalBody = !needsFullMapPrereqs
            || (hasSelfMain && hasSelfFirstExpansion && hasSelfSecondChoke && hasEnemyFirstExpansion);
        bool originalCalled = false;

        if (canCallOriginalBody) {
            wraithManager.update();
            originalCalled = true;
        } else {
            std::cout << "[SAIDA mock] WraithManager.update slice TODO: skipped original body"
                      << " because active wraiths need mock expansion and choke data"
                      << std::endl;
        }

        Position killScvTargetAfter = Positions::Unknown;
        if (hasSelfFirstExpansion) {
            killScvTargetAfter = wraithManager.getKillScvTargetBase();
        }

        std::cout << "[SAIDA mock] WraithManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " prereqMain=" << boolText(hasSelfMain)
                  << " prereqFirstExpansion=" << boolText(hasSelfFirstExpansion)
                  << " prereqSecondChoke=" << boolText(hasSelfSecondChoke)
                  << " prereqEnemyFirstExpansion=" << boolText(hasEnemyFirstExpansion)
                  << " selfWraithsBefore={" << selfWraithsBefore << "}"
                  << " selfWraithsAfter={" << wraithStateSummary(S) << "}"
                  << " enemyWraithsBefore={" << enemyWraithsBefore << "}"
                  << " enemyWraithsAfter={" << wraithStateSummary(E) << "}"
                  << " enemyValkyriesBefore={" << enemyValkyriesBefore << "}"
                  << " enemyValkyriesAfter={" << unitStateSummary(UnitTypes::Terran_Valkyrie, E) << "}"
                  << " enemyOccupiedBases=" << enemyOccupiedBasesBefore
                  << "->" << INFO.getOccupiedBaseLocations(E).size()
                  << " killScvTarget=" << positionText(killScvTargetBefore)
                  << "->" << positionText(killScvTargetAfter)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] WraithManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] WraithManager.update slice unknown exception" << std::endl;
    }

    wraithManagerDisabled = true;
    return false;
}

bool runVessleManagerSlice()
{
    if (vessleManagerDisabled) {
        std::cout << "[SAIDA mock] VessleManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] VessleManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runWraithManagerSlice()) {
        std::cout << "[SAIDA mock] VessleManager.update slice skipped: WraithManager.update failed" << std::endl;
        return false;
    }

    try {
        VessleManager& vessleManager = VessleManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() % 2 == 0;
        const bool targetListRefreshActive = INFO.enemyRace == Races::Protoss
            ? Broodwar->getFrameCount() % 480 == 0
            : Broodwar->getFrameCount() % 24 == 0;
        const bool hasSelfMain = INFO.getMainBaseLocation(S) != nullptr;
        const bool hasEnemyMain = INFO.getMainBaseLocation(E) != nullptr;
        const bool hasSelfFirstExpansion = INFO.getFirstExpansionLocation(S) != nullptr;
        const bool hasSelfSecondChoke = INFO.getSecondChokePoint(S) != nullptr;
        const Position mainAttackPosition = SM.getMainAttackPosition();
        const bool hasMainAttackPosition = mainAttackPosition != Positions::None
            && mainAttackPosition != Positions::Unknown
            && mainAttackPosition.isValid();
        const size_t vesslesBeforeCount = INFO.getUnits(UnitTypes::Terran_Science_Vessel, S).size();
        const std::string vesslesBefore = vessleStateSummary(S);
        Unit enemyTargetBefore = vessleManager.enemyTargetUnit;
        Unit guideTargetBefore = nullptr;
        if (!updateBodyActive || vesslesBeforeCount == 0 || hasMainAttackPosition) {
            guideTargetBefore = vessleManager.choicePosition(1);
        }

        const bool needsFullCombatPrereqs = updateBodyActive && vesslesBeforeCount > 0;
        const bool canCallOriginalBody = !needsFullCombatPrereqs
            || (hasSelfMain && hasEnemyMain && hasSelfFirstExpansion && hasSelfSecondChoke && hasMainAttackPosition);
        bool originalCalled = false;

        if (canCallOriginalBody) {
            vessleManager.update();
            originalCalled = true;
        } else {
            std::cout << "[SAIDA mock] VessleManager.update slice TODO: skipped original body"
                      << " because active science vessels need mock combat target and choke data"
                      << std::endl;
        }

        Unit guideTargetAfter = nullptr;
        if (!updateBodyActive || INFO.getUnits(UnitTypes::Terran_Science_Vessel, S).empty() || hasMainAttackPosition) {
            guideTargetAfter = vessleManager.choicePosition(1);
        }
        Unit enemyTargetAfter = vessleManager.enemyTargetUnit;

        std::cout << "[SAIDA mock] VessleManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " targetListRefreshActive=" << boolText(targetListRefreshActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " prereqMain=" << boolText(hasSelfMain)
                  << " prereqEnemyMain=" << boolText(hasEnemyMain)
                  << " prereqFirstExpansion=" << boolText(hasSelfFirstExpansion)
                  << " prereqSecondChoke=" << boolText(hasSelfSecondChoke)
                  << " mainAttackPosition=" << positionText(mainAttackPosition)
                  << " scienceVesselsBefore={" << vesslesBefore << "}"
                  << " scienceVesselsAfter={" << vessleStateSummary(S) << "}"
                  << " guideTarget=" << (guideTargetBefore ? guideTargetBefore->getID() : 0)
                  << "->" << (guideTargetAfter ? guideTargetAfter->getID() : 0)
                  << " enemyTarget=" << (enemyTargetBefore ? enemyTargetBefore->getID() : 0)
                  << "->" << (enemyTargetAfter ? enemyTargetAfter->getID() : 0)
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] VessleManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] VessleManager.update slice unknown exception" << std::endl;
    }

    vessleManagerDisabled = true;
    return false;
}

bool runDropshipManagerSlice()
{
    if (dropshipManagerDisabled) {
        std::cout << "[SAIDA mock] DropshipManager.update slice disabled after first failure" << std::endl;
        return false;
    }

    std::cout << "[SAIDA mock] DropshipManager.update slice begin frame=" << Broodwar->getFrameCount() << std::endl;
    if (!runVessleManagerSlice()) {
        std::cout << "[SAIDA mock] DropshipManager.update slice skipped: VessleManager.update failed" << std::endl;
        return false;
    }

    try {
        DropshipManager& dropshipManager = DropshipManager::Instance();
        const bool updateBodyActive = Broodwar->getFrameCount() >= 300 && Broodwar->getFrameCount() % 2 == 0;
        const bool resetSpaceActive = Broodwar->getFrameCount() % (24 * 5) == 0;
        const bool hasSelfMain = INFO.getMainBaseLocation(S) != nullptr;
        const bool hasEnemyMain = INFO.getMainBaseLocation(E) != nullptr;
        const Position enemyMainCenter = hasEnemyMain ? INFO.getMainBaseLocation(E)->Center() : Positions::Unknown;
        const size_t dropshipsBeforeCount = INFO.getUnits(UnitTypes::Terran_Dropship, S).size();
        const std::string dropshipsBefore = dropshipStateSummary();
        const std::string spaceBefore = dropshipSpaceSummary();
        const bool dropshipModeBefore = SM.getDropshipMode();
        const bool boardingBefore = dropshipManager.getBoardingState();

        const bool needsFullRoutePrereqs = updateBodyActive && dropshipsBeforeCount > 0;
        const bool canCallOriginalBody = !needsFullRoutePrereqs || (hasSelfMain && hasEnemyMain);
        bool originalCalled = false;

        if (canCallOriginalBody) {
            dropshipManager.update();
            originalCalled = true;
        } else {
            std::cout << "[SAIDA mock] DropshipManager.update slice TODO: skipped original body"
                      << " because active dropships need mock self/enemy base route data"
                      << std::endl;
        }

        std::cout << "[SAIDA mock] DropshipManager.update slice end"
                  << " frame=" << Broodwar->getFrameCount()
                  << " bodyActive=" << boolText(updateBodyActive)
                  << " resetSpaceActive=" << boolText(resetSpaceActive)
                  << " originalCalled=" << boolText(originalCalled)
                  << " prereqMain=" << boolText(hasSelfMain)
                  << " prereqEnemyMain=" << boolText(hasEnemyMain)
                  << " enemyMainCenter=" << positionText(enemyMainCenter)
                  << " dropshipMode=" << boolText(dropshipModeBefore)
                  << "->" << boolText(SM.getDropshipMode())
                  << " boardingState=" << boolText(boardingBefore)
                  << "->" << boolText(dropshipManager.getBoardingState())
                  << " dropshipsBefore={" << dropshipsBefore << "}"
                  << " dropshipsAfter={" << dropshipStateSummary() << "}"
                  << " spaceBefore={" << spaceBefore << "}"
                  << " spaceAfter={" << dropshipSpaceSummary() << "}"
                  << std::endl;
        return true;
    } catch (const std::exception& error) {
        std::cerr << "[SAIDA mock] DropshipManager.update slice exception: " << error.what() << std::endl;
    } catch (...) {
        std::cerr << "[SAIDA mock] DropshipManager.update slice unknown exception" << std::endl;
    }

    dropshipManagerDisabled = true;
    return false;
}

bool runCommanderOnStartSlice()
{
    std::cout << "[SAIDA mock] GameCommander.onStart slice begin" << std::endl;

    Player self = Broodwar->self();
    if (!self) {
        std::cout << "[SAIDA mock] GameCommander.onStart slice: self player missing" << std::endl;
        return false;
    }

    const TilePosition startLocation = self->getStartLocation();
    std::cout << "[SAIDA mock] GameCommander.onStart slice: startLocation="
              << startLocation.x << "," << startLocation.y << std::endl;

    if (startLocation == TilePositions::None || startLocation == TilePositions::Unknown) {
        std::cout << "[SAIDA mock] GameCommander.onStart slice: start location unresolved; matching SAIDA early return"
                  << std::endl;
        return true;
    }

    if (!runInformationManagerUpdateSlice()) {
        std::cout << "[SAIDA mock] GameCommander.onStart slice: StrategyManager.onStart skipped; InformationManager.update failed"
                  << std::endl;
        return false;
    }

    StrategyManager::Instance().onStart();
    strategyManagerStarted = true;
    std::cout << "[SAIDA mock] GameCommander.onStart slice: StrategyManager::Instance().onStart() passed"
              << std::endl;
    std::cout << "[SAIDA mock] GameCommander.onStart slice: next SAIDA step is GameCommander.onFrame()"
              << std::endl;
    std::cout << "[SAIDA mock] GameCommander.onStart slice end" << std::endl;
    return true;
}

bool runCommanderOnFrameSlice()
{
    std::cout << "[SAIDA mock] GameCommander.onFrame slice begin" << std::endl;

    Player self = Broodwar->self();
    Player enemy = Broodwar->enemy();
    const bool blocked = Broodwar->isPaused()
        || self == nullptr
        || (self && self->isDefeated())
        || (self && self->leftGame())
        || enemy == nullptr
        || (enemy && enemy->isDefeated())
        || (enemy && enemy->leftGame());

    std::cout << "[SAIDA mock] GameCommander.onFrame guard paused=" << boolText(Broodwar->isPaused())
              << " self=" << boolText(self != nullptr)
              << " enemy=" << boolText(enemy != nullptr);
    if (self) {
        std::cout << " selfDefeated=" << boolText(self->isDefeated())
                  << " selfLeft=" << boolText(self->leftGame());
    }
    if (enemy) {
        std::cout << " enemyDefeated=" << boolText(enemy->isDefeated())
                  << " enemyLeft=" << boolText(enemy->leftGame());
    }
    std::cout << std::endl;

    if (blocked) {
        std::cout << "[SAIDA mock] GameCommander.onFrame slice: guard blocked; matching SAIDA early return"
                  << std::endl;
        return true;
    }

    if (!runDropshipManagerSlice()) {
        return false;
    }

    //20260701_kpopmodder: DropshipManager is the final manager in SAIDA's onFrame chain.
    std::cout << "[SAIDA mock] GameCommander.onFrame slice: DropshipManager::Instance().update() passed"
              << std::endl;
    std::cout << "[SAIDA mock] GameCommander.onFrame slice: SAIDA manager chain complete"
              << std::endl;
    std::cout << "[SAIDA mock] GameCommander.onFrame slice end" << std::endl;
    return true;
}

Unit firstUnitOfType(const Unitset& units, UnitType type)
{
    for (Unit unit : units) {
        if (unit && unit->getType() == type) {
            return unit;
        }
    }
    return nullptr;
}

Unit firstWorker(const Unitset& units)
{
    for (Unit unit : units) {
        if (unit && unit->getType().isWorker()) {
            return unit;
        }
    }
    return nullptr;
}

void runManualFrame()
{
    Player self = Broodwar->self();
    if (!self) {
        return;
    }

    Unitset units = self->getUnits();
    Unit commandCenter = firstUnitOfType(units, UnitTypes::Terran_Command_Center);
    Unit worker = firstWorker(units);
    Unit mineral = firstUnitOfType(Broodwar->getMinerals(), UnitTypes::Resource_Mineral_Field);
    Unit enemy = firstUnitOfType(Broodwar->enemy()->getUnits(), UnitTypes::Zerg_Hatchery);

    switch (Broodwar->getFrameCount()) {
    case 1:
        if (commandCenter) {
            commandCenter->train(UnitTypes::Terran_SCV);
        }
        break;
    case 2:
        if (worker && mineral) {
            worker->gather(mineral);
        }
        break;
    case 3:
        if (worker) {
            worker->build(UnitTypes::Terran_Supply_Depot, TilePosition(10, 20));
        }
        break;
    case 4:
        if (worker) {
            worker->move(Position(384, 640));
        }
        break;
    case 5:
        if (worker && enemy) {
            worker->attack(enemy->getPosition());
        }
        break;
    default:
        break;
    }
}

bool commanderFrameDisabled = false;
} // namespace

MyBotModule::MyBotModule() = default;

MyBotModule::~MyBotModule() = default;

void MyBotModule::onStart()
{
    const MockMode mode = currentMode();
    std::cout << "[SAIDA mock] mode=" << modeName(mode) << std::endl;

    Broodwar->setCommandOptimizationLevel(1);

    if (Broodwar->enemy()) {
        Broodwar << "The matchup is " << Broodwar->self()->getRace()
                 << " vs " << Broodwar->enemy()->getRace() << std::endl;
    }

    Broodwar << "Map initialization..." << std::endl;

    Broodwar->setLocalSpeed(Config::BWAPIOptions::SetLocalSpeed);
    Broodwar->setFrameSkip(Config::BWAPIOptions::SetFrameSkip);

    if (mode == MockMode::InfoProbe || mode == MockMode::InfoUpdate || mode == MockMode::PlaceFinder
        || mode == MockMode::BuildManager
        || mode == MockMode::ConstructionManager
        || mode == MockMode::StrategyManager
        || mode == MockMode::EnemyStrategyManager
        || mode == MockMode::ScvManager
        || mode == MockMode::ScoutManager
        || mode == MockMode::TrainManager
        || mode == MockMode::ComsatStationManager
        || mode == MockMode::EngineeringBayManager
        || mode == MockMode::MarineManager
        || mode == MockMode::TankManager
        || mode == MockMode::VultureManager
        || mode == MockMode::GoliathManager
        || mode == MockMode::WraithManager
        || mode == MockMode::VessleManager
        || mode == MockMode::DropshipManager
        || mode == MockMode::CommanderStart || mode == MockMode::CommanderFrame) {
        runBWEMInitialization();
    }

    if (mode == MockMode::CommanderStart || mode == MockMode::CommanderFrame) {
        runCommanderOnStartSlice();
    } else if (mode == MockMode::CommanderStartUnsafe || mode == MockMode::CommanderFrameUnsafe) {
        runCommanderStep("GameCommander.onStart", [this]() {
            gameCommander.onStart();
        });
    }
}

void MyBotModule::onEnd(bool isWinner)
{
    std::cout << "[SAIDA mock] onEnd winner=" << (isWinner ? "true" : "false") << std::endl;
}

void MyBotModule::onFrame()
{
    Player self = Broodwar->self();
    if (!self) {
        return;
    }

    Unitset units = self->getUnits();
    std::cout << "[SAIDA mock] onFrame frame=" << Broodwar->getFrameCount()
              << " minerals=" << self->minerals()
              << " gas=" << self->gas()
              << " supply=" << self->supplyUsed() << "/" << self->supplyTotal()
              << " units=" << units.size() << std::endl;

    const MockMode mode = currentMode();
    if (mode == MockMode::InfoProbe) {
        runInformationManagerUnitDataSlice();
    } else if (mode == MockMode::InfoUpdate) {
        runInformationManagerUpdateSlice();
    } else if (mode == MockMode::PlaceFinder) {
        runTerranConstructionPlaceFinderSlice();
    } else if (mode == MockMode::BuildManager) {
        runBuildManagerSlice();
        return;
    } else if (mode == MockMode::ConstructionManager) {
        runConstructionManagerSlice();
        return;
    } else if (mode == MockMode::StrategyManager) {
        runStrategyManagerSlice();
        return;
    } else if (mode == MockMode::EnemyStrategyManager) {
        runEnemyStrategyManagerSlice();
        return;
    } else if (mode == MockMode::ScvManager) {
        runScvManagerSlice();
        return;
    } else if (mode == MockMode::ScoutManager) {
        runScoutManagerSlice();
        return;
    } else if (mode == MockMode::TrainManager) {
        runTrainManagerSlice();
        return;
    } else if (mode == MockMode::ComsatStationManager) {
        runComsatStationManagerSlice();
        return;
    } else if (mode == MockMode::EngineeringBayManager) {
        runEngineeringBayManagerSlice();
        return;
    } else if (mode == MockMode::MarineManager) {
        runMarineManagerSlice();
        return;
    } else if (mode == MockMode::TankManager) {
        runTankManagerSlice();
        return;
    } else if (mode == MockMode::VultureManager) {
        runVultureManagerSlice();
        return;
    } else if (mode == MockMode::GoliathManager) {
        runGoliathManagerSlice();
        return;
    } else if (mode == MockMode::WraithManager) {
        runWraithManagerSlice();
        return;
    } else if (mode == MockMode::VessleManager) {
        runVessleManagerSlice();
        return;
    } else if (mode == MockMode::DropshipManager) {
        runDropshipManagerSlice();
        return;
    } else if (mode == MockMode::CommanderFrame && !commanderFrameDisabled) {
        if (runCommanderOnFrameSlice()) {
            return;
        }
    } else if (mode == MockMode::CommanderFrameUnsafe && !commanderFrameDisabled) {
        const bool ok = runCommanderStep("GameCommander.onFrame", [this]() {
            gameCommander.onFrame();
        });
        if (ok) {
            return;
        }
        commanderFrameDisabled = true;
        std::cout << "[SAIDA mock] GameCommander.onFrame disabled after first failure; using manual command probe"
                  << std::endl;
    }

    runManualFrame();
}

void MyBotModule::onUnitCreate(Unit) {}
void MyBotModule::onUnitDestroy(Unit) {}
void MyBotModule::onUnitMorph(Unit) {}
void MyBotModule::onUnitRenegade(Unit) {}
void MyBotModule::onUnitComplete(Unit) {}
void MyBotModule::onUnitDiscover(Unit) {}
void MyBotModule::onUnitEvade(Unit) {}
void MyBotModule::onUnitShow(Unit) {}
void MyBotModule::onUnitHide(Unit) {}
void MyBotModule::onNukeDetect(Position) {}
void MyBotModule::onPlayerLeft(Player) {}
void MyBotModule::onSaveGame(string) {}
void MyBotModule::ParseTextCommand(const string&) {}
void MyBotModule::onSendText(string) {}
void MyBotModule::onReceiveText(Player, string) {}
