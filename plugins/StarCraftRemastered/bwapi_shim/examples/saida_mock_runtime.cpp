//20260701_kpopmodder: Runs local SAIDA source through MockBridge without using BWAPIClient or injection.
#include "BWAPI.h"
#include "LAVBWAPIRM/CompatRunner.h"
#include "LAVBWAPIRM/MockBridge.h"

#include "MyBotModule.h"

#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <algorithm>
#include <array>
#include <cctype>
#include <exception>
#include <iostream>
#include <memory>
#include <map>
#include <streambuf>
#include <string>

#ifdef _WIN32
#include <windows.h>
#endif

namespace {
constexpr std::array<const char*, 6> kCombatManagers = {
    "TankManager",
    "VultureManager",
    "GoliathManager",
    "WraithManager",
    "VessleManager",
    "DropshipManager",
};

constexpr std::array<const char*, 7> kTrackedManagers = {
    "TrainManager",
    "TankManager",
    "VultureManager",
    "GoliathManager",
    "WraithManager",
    "VessleManager",
    "DropshipManager",
};

void setEnvironmentValue(const char* name, const std::string& value)
{
#ifdef _WIN32
    _putenv_s(name, value.c_str());
#else
    setenv(name, value.c_str(), 1);
#endif
}

bool hasFlagArgument(int argc, char** argv, const char* flag)
{
    for (int index = 1; index < argc; ++index) {
        if (std::strcmp(argv[index], flag) == 0) {
            return true;
        }
    }
    return false;
}

std::string lowercase(std::string value)
{
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return value;
}

std::string removePrefix(std::string value, const std::string& prefix)
{
    if (value.rfind(prefix, 0) == 0) {
        return value.substr(prefix.size());
    }
    return value;
}

const char* boolWord(bool value)
{
    return value ? "true" : "false";
}

struct RuntimeSummaryCollector {
    explicit RuntimeSummaryCollector(int frameLimitValue)
        : frameLimit(frameLimitValue)
    {
        for (const char* manager : kTrackedManagers) {
            managerOriginalCalled[manager] = false;
        }
    }

    void processLine(const std::string& line)
    {
        if (line.empty()) {
            return;
        }

        if (line.find("BWEM initialization end") != std::string::npos) {
            bwemLine = line;
        }
        if (line.find("secondChoke=true") != std::string::npos) {
            secondChoke = true;
        }
        if (line.find("secondExpansion=true/true") != std::string::npos) {
            secondExpansion = true;
        }
        if (line.find("Reserve failed") != std::string::npos) {
            reserveFailed = true;
        }
        if (line.find("TODO") != std::string::npos) {
            todo = true;
        }

        const std::string lower = lowercase(line);
        if (lower.find("exception") != std::string::npos) {
            exceptionLine = true;
        }
        if (line.find("[Command]") != std::string::npos) {
            if (line.find("type=TRAIN") != std::string::npos) {
                ++trainCommands;
                if (line.find("unit=Terran SCV") != std::string::npos) {
                    ++trainScvCommands;
                }
                if (line.find("unit=Terran Vulture") != std::string::npos) {
                    ++trainVultureCommands;
                }
                if (line.find("unit=Terran Siege Tank") != std::string::npos) {
                    ++trainTankCommands;
                }
                if (line.find("unit=Terran Goliath") != std::string::npos) {
                    ++trainGoliathCommands;
                }
                if (line.find("unit=Terran Science Vessel") != std::string::npos) {
                    ++trainScienceVesselCommands;
                }
            }
            if (line.find("type=BUILD") != std::string::npos) {
                ++buildCommands;
                if (line.find("payload=\"addon\"") != std::string::npos) {
                    ++addonBuildCommands;
                    if (line.find("building=Terran Comsat Station") != std::string::npos) {
                        ++addonComsatCommands;
                    }
                    if (line.find("building=Terran Machine Shop") != std::string::npos) {
                        ++addonMachineShopCommands;
                    }
                    if (line.find("building=Terran Control Tower") != std::string::npos) {
                        ++addonControlTowerCommands;
                    }
                }
            }
            if (line.find("type=RESEARCH") != std::string::npos) {
                ++researchCommands;
                if (line.find("ability=Tank Siege Mode") != std::string::npos) {
                    ++researchSiegeModeCommands;
                }
                if (line.find("ability=Spider Mines") != std::string::npos) {
                    ++researchSpiderMinesCommands;
                }
                if (line.find("ability=Irradiate") != std::string::npos) {
                    ++researchIrradiateCommands;
                }
                if (line.find("ability=EMP Shockwave") != std::string::npos) {
                    ++researchEmpCommands;
                }
            }
            if (line.find("type=UPGRADE") != std::string::npos) {
                ++upgradeCommands;
                if (line.find("ability=Terran Vehicle Weapons") != std::string::npos) {
                    ++upgradeVehicleWeaponsCommands;
                }
                if (line.find("ability=Terran Vehicle Plating") != std::string::npos) {
                    ++upgradeVehiclePlatingCommands;
                }
                if (line.find("ability=Ion Thrusters") != std::string::npos) {
                    ++upgradeIonThrustersCommands;
                }
                if (line.find("ability=Charon Boosters") != std::string::npos) {
                    ++upgradeCharonBoostersCommands;
                }
            }
        }

        const std::string frameNeedle = "frame=" + std::to_string(frameLimit);
        for (const char* manager : kTrackedManagers) {
            const std::string managerNeedle = std::string(manager) + ".update slice end";
            if (line.find(managerNeedle) != std::string::npos
                && line.find(frameNeedle) != std::string::npos
                && line.find("originalCalled=true") != std::string::npos) {
                managerOriginalCalled[manager] = true;
            }
        }
    }

    int frameLimit = 0;
    std::string bwemLine;
    bool secondChoke = false;
    bool secondExpansion = false;
    bool reserveFailed = false;
    bool todo = false;
    bool exceptionLine = false;
    int trainCommands = 0;
    int trainScvCommands = 0;
    int trainVultureCommands = 0;
    int trainTankCommands = 0;
    int trainGoliathCommands = 0;
    int trainScienceVesselCommands = 0;
    int buildCommands = 0;
    int addonBuildCommands = 0;
    int addonComsatCommands = 0;
    int addonMachineShopCommands = 0;
    int addonControlTowerCommands = 0;
    int researchCommands = 0;
    int researchSiegeModeCommands = 0;
    int researchSpiderMinesCommands = 0;
    int researchIrradiateCommands = 0;
    int researchEmpCommands = 0;
    int upgradeCommands = 0;
    int upgradeVehicleWeaponsCommands = 0;
    int upgradeVehiclePlatingCommands = 0;
    int upgradeIonThrustersCommands = 0;
    int upgradeCharonBoostersCommands = 0;
    std::map<std::string, bool> managerOriginalCalled;
};

class SummaryStreamBuffer final : public std::streambuf {
public:
    explicit SummaryStreamBuffer(RuntimeSummaryCollector& collector)
        : collector_(collector)
    {
    }

    void flushLine()
    {
        if (!line_.empty()) {
            collector_.processLine(line_);
            line_.clear();
        }
    }

protected:
    int overflow(int ch) override
    {
        if (traits_type::eq_int_type(ch, traits_type::eof())) {
            return traits_type::not_eof(ch);
        }

        const char value = traits_type::to_char_type(ch);
        if (value == '\n') {
            flushLine();
        } else if (value != '\r') {
            line_.push_back(value);
        }
        return ch;
    }

    int sync() override
    {
        flushLine();
        return 0;
    }

private:
    RuntimeSummaryCollector& collector_;
    std::string line_;
};

void applyModeArgument(int argc, char** argv)
{
    constexpr const char* modePrefix = "--mode=";
    for (int index = 1; index < argc; ++index) {
        if (std::strncmp(argv[index], modePrefix, std::strlen(modePrefix)) == 0) {
            const std::string mode = argv[index] + std::strlen(modePrefix);
            if (mode == "combat-commander" || mode == "combat_commander"
                || mode == "active-combat" || mode == "active_combat") {
                setEnvironmentValue("LAV_SAIDA_MOCK_MODE", "commander-frame");
                setEnvironmentValue("LAV_SAIDA_MOCK_SCENARIO", "combat");
            } else if (mode == "tech-surface" || mode == "tech_surface"
                || mode == "tech-buildings" || mode == "tech_buildings" || mode == "tech") {
                setEnvironmentValue("LAV_SAIDA_MOCK_MODE", "info-update");
                setEnvironmentValue("LAV_SAIDA_MOCK_SCENARIO", "tech");
            } else if (mode == "research-surface" || mode == "research_surface"
                || mode == "research-upgrade" || mode == "research_upgrade"
                || mode == "upgrade-surface" || mode == "upgrade_surface") {
                setEnvironmentValue("LAV_SAIDA_MOCK_MODE", "info-update");
                setEnvironmentValue("LAV_SAIDA_MOCK_SCENARIO", "research-upgrade");
            } else {
                setEnvironmentValue("LAV_SAIDA_MOCK_MODE", mode);
            }
        }
    }
}

void applyScenarioArgument(int argc, char** argv)
{
    constexpr const char* scenarioPrefix = "--scenario=";
    for (int index = 1; index < argc; ++index) {
        if (std::strncmp(argv[index], scenarioPrefix, std::strlen(scenarioPrefix)) == 0) {
            setEnvironmentValue("LAV_SAIDA_MOCK_SCENARIO", argv[index] + std::strlen(scenarioPrefix));
        }
    }
}

int frameLimitFromArgs(int argc, char** argv)
{
    constexpr const char* framesPrefix = "--frames=";
    for (int index = 1; index < argc; ++index) {
        if (std::strncmp(argv[index], framesPrefix, std::strlen(framesPrefix)) == 0) {
            const int frameLimit = std::atoi(argv[index] + std::strlen(framesPrefix));
            if (frameLimit > 0) {
                return frameLimit;
            }
        }
    }

    const char* modeValue = std::getenv("LAV_SAIDA_MOCK_MODE");
    const std::string mode = modeValue ? modeValue : "";
    if (mode == "enemy-strategy-manager" || mode == "enemy_strategy_manager"
        || mode == "enemy-strategy" || mode == "enemy_strategy" || mode == "esm") {
        return 24 * 30;
    }

    if (mode == "comsat-station-manager" || mode == "comsat_station_manager"
        || mode == "comsat-manager" || mode == "comsat_manager" || mode == "comsat" || mode == "scan-manager"
        || mode == "scan_manager") {
        return 300;
    }

    if (mode == "strategy-manager" || mode == "strategy_manager" || mode == "strategy" || mode == "sm"
        || mode == "scv-manager" || mode == "scv_manager" || mode == "scv" || mode == "worker-manager"
        || mode == "worker_manager" || mode == "workers"
        || mode == "scout-manager" || mode == "scout_manager" || mode == "scout" || mode == "scouting"
        || mode == "train-manager" || mode == "train_manager" || mode == "train" || mode == "production-manager"
        || mode == "production_manager" || mode == "production"
        || mode == "engineering-bay-manager" || mode == "engineering_bay_manager"
        || mode == "engineering-bay" || mode == "engineering_bay" || mode == "engineering" || mode == "ebay"
        || mode == "marine-manager" || mode == "marine_manager" || mode == "marine" || mode == "marines"
        || mode == "tank-manager" || mode == "tank_manager" || mode == "tank" || mode == "tanks"
        || mode == "siege-tank-manager" || mode == "siege_tank_manager"
        || mode == "vulture-manager" || mode == "vulture_manager" || mode == "vulture" || mode == "vultures"
        || mode == "goliath-manager" || mode == "goliath_manager" || mode == "goliath" || mode == "goliaths"
        || mode == "wraith-manager" || mode == "wraith_manager" || mode == "wraith" || mode == "wraiths"
        || mode == "vessle-manager" || mode == "vessle_manager" || mode == "vessle" || mode == "vessles"
        || mode == "science-vessel-manager" || mode == "science_vessel_manager"
        || mode == "science-vessel" || mode == "science_vessel"
        || mode == "vessel-manager" || mode == "vessel_manager" || mode == "vessel" || mode == "vessels"
        || mode == "dropship-manager" || mode == "dropship_manager" || mode == "dropship" || mode == "dropships"
        || mode == "drop-manager" || mode == "drop_manager"
        || mode == "commander-frame" || mode == "commander_frame" || mode == "commander") {
        if (mode == "marine-manager" || mode == "marine_manager" || mode == "marine" || mode == "marines"
            || mode == "tank-manager" || mode == "tank_manager" || mode == "tank" || mode == "tanks"
            || mode == "siege-tank-manager" || mode == "siege_tank_manager"
            || mode == "vulture-manager" || mode == "vulture_manager" || mode == "vulture" || mode == "vultures"
            || mode == "goliath-manager" || mode == "goliath_manager" || mode == "goliath" || mode == "goliaths"
            || mode == "wraith-manager" || mode == "wraith_manager" || mode == "wraith" || mode == "wraiths"
            || mode == "vessle-manager" || mode == "vessle_manager" || mode == "vessle" || mode == "vessles"
            || mode == "science-vessel-manager" || mode == "science_vessel_manager"
            || mode == "science-vessel" || mode == "science_vessel"
            || mode == "vessel-manager" || mode == "vessel_manager" || mode == "vessel" || mode == "vessels"
            || mode == "dropship-manager" || mode == "dropship_manager" || mode == "dropship" || mode == "dropships"
            || mode == "drop-manager" || mode == "drop_manager") {
            return 304;
        }
        return 24;
    }

    if (mode == "build-manager" || mode == "build_manager" || mode == "build" || mode == "bm"
        || mode == "construction-manager" || mode == "construction_manager" || mode == "construction" || mode == "cm") {
        return 8;
    }

    return 5;
}

int countSelfUnitsByType(const std::string& unitTypeName)
{
    int count = 0;
    BWAPI::Player self = BWAPI::Broodwar->self();
    if (!self) {
        return count;
    }

    for (BWAPI::Unit unit : self->getUnits()) {
        if (unit && unit->getType().getName() == unitTypeName) {
            ++count;
        }
    }
    return count;
}

int countSelfAddonLinks(const std::string& parentTypeName, const std::string& addonTypeName)
{
    int count = 0;
    BWAPI::Player self = BWAPI::Broodwar->self();
    if (!self) {
        return count;
    }

    for (BWAPI::Unit unit : self->getUnits()) {
        if (!unit || unit->getType().getName() != parentTypeName) {
            continue;
        }
        BWAPI::Unit addon = unit->getAddon();
        if (addon && addon->getType().getName() == addonTypeName) {
            ++count;
        }
    }
    return count;
}

void printResearchUpgradeSummary()
{
    BWAPI::Player self = BWAPI::Broodwar->self();
    if (!self) {
        std::cout << "[SAIDA summary] research missingSelf=true" << std::endl;
        return;
    }

    std::cout << "[SAIDA summary] research"
              << " siegeMode=" << boolWord(self->hasResearched(BWAPI::TechTypes::Tank_Siege_Mode))
              << " spiderMines=" << boolWord(self->hasResearched(BWAPI::TechTypes::Spider_Mines))
              << " irradiate=" << boolWord(self->hasResearched(BWAPI::TechTypes::Irradiate))
              << " emp=" << boolWord(self->hasResearched(BWAPI::TechTypes::EMP_Shockwave))
              << " researchingCloaking=" << boolWord(self->isResearching(BWAPI::TechTypes::Cloaking_Field))
              << " canResearchCloaking=" << boolWord(BWAPI::Broodwar->canResearch(BWAPI::TechTypes::Cloaking_Field))
              << " canResearchDefensiveMatrix=" << boolWord(BWAPI::Broodwar->canResearch(BWAPI::TechTypes::Defensive_Matrix))
              << std::endl;

    std::cout << "[SAIDA summary] upgrades"
              << " vehicleWeapons=" << self->getUpgradeLevel(BWAPI::UpgradeTypes::Terran_Vehicle_Weapons)
              << " vehicleWeaponsMax=" << self->getMaxUpgradeLevel(BWAPI::UpgradeTypes::Terran_Vehicle_Weapons)
              << " vehiclePlating=" << self->getUpgradeLevel(BWAPI::UpgradeTypes::Terran_Vehicle_Plating)
              << " ionThrusters=" << self->getUpgradeLevel(BWAPI::UpgradeTypes::Ion_Thrusters)
              << " charonBoosters=" << self->getUpgradeLevel(BWAPI::UpgradeTypes::Charon_Boosters)
              << " upgradingVehiclePlating=" << boolWord(self->isUpgrading(BWAPI::UpgradeTypes::Terran_Vehicle_Plating))
              << " canUpgradeVehicleWeapons=" << boolWord(BWAPI::Broodwar->canUpgrade(BWAPI::UpgradeTypes::Terran_Vehicle_Weapons))
              << " canUpgradeVehiclePlating=" << boolWord(BWAPI::Broodwar->canUpgrade(BWAPI::UpgradeTypes::Terran_Vehicle_Plating))
              << std::endl;
}

void printRuntimeSummary(const RuntimeSummaryCollector& summary, bool ok)
{
    std::cout << "[SAIDA summary] frames=" << summary.frameLimit
              << " ok=" << (ok ? "true" : "false") << std::endl;

    const std::string bwemDetails = removePrefix(
        summary.bwemLine,
        "[SAIDA mock] BWEM initialization end "
    );
    std::cout << "[SAIDA summary] bwem "
              << (bwemDetails.empty() ? "missing=true" : bwemDetails)
              << std::endl;

    std::cout << "[SAIDA summary] surfaces"
              << " secondChoke=" << (summary.secondChoke ? "true" : "false")
              << " secondExpansion=" << (summary.secondExpansion ? "true/true" : "false")
              << " reserveFailed=" << (summary.reserveFailed ? "true" : "false")
              << " todo=" << (summary.todo ? "true" : "false")
              << " exception=" << (summary.exceptionLine ? "true" : "false")
              << std::endl;

    for (const char* manager : kTrackedManagers) {
        const auto found = summary.managerOriginalCalled.find(manager);
        const bool originalCalled = found != summary.managerOriginalCalled.end() && found->second;
        std::cout << "[SAIDA summary] manager " << manager
                  << " originalCalled=" << (originalCalled ? "true" : "false")
                  << std::endl;
    }

    std::cout << "[SAIDA summary] tech"
              << " refinery=" << countSelfUnitsByType("Terran Refinery")
              << " barracks=" << countSelfUnitsByType("Terran Barracks")
              << " engineeringBay=" << countSelfUnitsByType("Terran Engineering Bay")
              << " academy=" << countSelfUnitsByType("Terran Academy")
              << " factory=" << countSelfUnitsByType("Terran Factory")
              << " machineShop=" << countSelfUnitsByType("Terran Machine Shop")
              << " armory=" << countSelfUnitsByType("Terran Armory")
              << " starport=" << countSelfUnitsByType("Terran Starport")
              << " controlTower=" << countSelfUnitsByType("Terran Control Tower")
              << " scienceFacility=" << countSelfUnitsByType("Terran Science Facility")
              << " comsat=" << countSelfUnitsByType("Terran Comsat Station")
              << std::endl;

    std::cout << "[SAIDA summary] addonLinks"
              << " commandCenterComsat=" << countSelfAddonLinks("Terran Command Center", "Terran Comsat Station")
              << " factoryMachineShop=" << countSelfAddonLinks("Terran Factory", "Terran Machine Shop")
              << " starportControlTower=" << countSelfAddonLinks("Terran Starport", "Terran Control Tower")
              << std::endl;

    printResearchUpgradeSummary();

    std::cout << "[SAIDA summary] commands"
              << " train=" << summary.trainCommands
              << " trainSCV=" << summary.trainScvCommands
              << " trainVulture=" << summary.trainVultureCommands
              << " trainTank=" << summary.trainTankCommands
              << " trainGoliath=" << summary.trainGoliathCommands
              << " trainScienceVessel=" << summary.trainScienceVesselCommands
              << " build=" << summary.buildCommands
              << " addonBuild=" << summary.addonBuildCommands
              << " addonComsat=" << summary.addonComsatCommands
              << " addonMachineShop=" << summary.addonMachineShopCommands
              << " addonControlTower=" << summary.addonControlTowerCommands
              << " research=" << summary.researchCommands
              << " researchSiegeMode=" << summary.researchSiegeModeCommands
              << " researchSpiderMines=" << summary.researchSpiderMinesCommands
              << " researchIrradiate=" << summary.researchIrradiateCommands
              << " researchEMP=" << summary.researchEmpCommands
              << " upgrade=" << summary.upgradeCommands
              << " upgradeVehicleWeapons=" << summary.upgradeVehicleWeaponsCommands
              << " upgradeVehiclePlating=" << summary.upgradeVehiclePlatingCommands
              << " upgradeIonThrusters=" << summary.upgradeIonThrustersCommands
              << " upgradeCharonBoosters=" << summary.upgradeCharonBoostersCommands
              << std::endl;
}
} // namespace

int main(int argc, char** argv)
{
#ifdef _WIN32
    SetErrorMode(SEM_FAILCRITICALERRORS | SEM_NOGPFAULTERRORBOX);
#endif

    applyModeArgument(argc, argv);
    applyScenarioArgument(argc, argv);

    const int frameLimit = frameLimitFromArgs(argc, argv);
    const bool summaryMode = hasFlagArgument(argc, argv, "--summary");
    const bool quietMode = summaryMode || hasFlagArgument(argc, argv, "--quiet");
    RuntimeSummaryCollector summary(frameLimit);
    SummaryStreamBuffer summaryOut(summary);
    SummaryStreamBuffer summaryErr(summary);
    std::streambuf* originalOut = nullptr;
    std::streambuf* originalErr = nullptr;

    auto redirectOutput = [&]() {
        if (!quietMode) {
            return;
        }
        originalOut = std::cout.rdbuf(&summaryOut);
        originalErr = std::cerr.rdbuf(&summaryErr);
    };

    auto restoreOutput = [&]() {
        if (!quietMode || !originalOut || !originalErr) {
            return;
        }
        std::cout.flush();
        std::cerr.flush();
        summaryOut.flushLine();
        summaryErr.flushLine();
        std::cout.rdbuf(originalOut);
        std::cerr.rdbuf(originalErr);
        originalOut = nullptr;
        originalErr = nullptr;
    };

    int exitCode = 1;
    try {
        redirectOutput();

        auto bridge = std::make_shared<LAVBWAPIRM::MockBridge>();
        LAVBWAPIRM::setBridge(bridge);
        BWAPI::Broodwar->setBridge(bridge);
        bridge->connect();
        BWAPI::Broodwar->refresh();

        std::cout << "[SAIDA] constructing MyBotModule" << std::endl;
        MyBot::MyBotModule bot;

        std::cout << "[SAIDA] running mock onStart/onFrame loop" << std::endl;
        LAVBWAPIRM::CompatRunner runner(bridge);
        const bool ok = runner.run(bot, frameLimit);
        std::cout << "[SAIDA] mock runtime finished ok=" << (ok ? "true" : "false") << std::endl;

        restoreOutput();
        if (summaryMode) {
            printRuntimeSummary(summary, ok);
        } else if (quietMode) {
            std::cout << "[SAIDA] mock runtime finished ok=" << (ok ? "true" : "false") << std::endl;
        }
        exitCode = ok ? 0 : 1;
    } catch (const std::exception& error) {
        summary.exceptionLine = true;
        restoreOutput();
        if (summaryMode) {
            printRuntimeSummary(summary, false);
        }
        std::cerr << "[SAIDA] mock runtime exception: " << error.what() << std::endl;
        exitCode = 2;
    } catch (...) {
        summary.exceptionLine = true;
        restoreOutput();
        if (summaryMode) {
            printRuntimeSummary(summary, false);
        }
        std::cerr << "[SAIDA] mock runtime unknown exception" << std::endl;
        exitCode = 3;
    }

    std::cout.flush();
    std::cerr.flush();
    std::fflush(nullptr);
    std::_Exit(exitCode);
}
