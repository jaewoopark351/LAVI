//20260701_kpopmodder: Implements a no-op bridge so the shim is safe until a runtime backend is attached.
#include "LAVBWAPIRM/Bridge.h"

#include <sstream>
#include <utility>

namespace LAVBWAPIRM {
namespace {
std::shared_ptr<Bridge>& bridgeSlot()
{
    static std::shared_ptr<Bridge> bridge = std::make_shared<NullBridge>();
    return bridge;
}
} // namespace

bool NullBridge::connect()
{
    snapshot_.connected = false;
    snapshot_.singlePlayer = true;
    return true;
}

void NullBridge::disconnect()
{
    snapshot_.connected = false;
}

GameSnapshot NullBridge::snapshot()
{
    return snapshot_;
}

bool NullBridge::sendCommand(const Command&)
{
    return false;
}

void NullBridge::stopAllControl()
{
}

std::shared_ptr<Bridge> getBridge()
{
    return bridgeSlot();
}

void setBridge(std::shared_ptr<Bridge> bridge)
{
    bridgeSlot() = bridge ? std::move(bridge) : std::make_shared<NullBridge>();
}

std::string commandTypeName(CommandType type)
{
    switch (type) {
    case CommandType::Train:
        return "TRAIN";
    case CommandType::Build:
        return "BUILD";
    case CommandType::Move:
        return "MOVE";
    case CommandType::Attack:
        return "ATTACK";
    case CommandType::Stop:
        return "STOP";
    case CommandType::HoldPosition:
        return "HOLD";
    case CommandType::RightClick:
        return "RIGHT_CLICK";
    case CommandType::Gather:
        return "GATHER";
    case CommandType::Repair:
        return "REPAIR";
    case CommandType::Research:
        return "RESEARCH";
    case CommandType::Upgrade:
        return "UPGRADE";
    case CommandType::UseTech:
        return "USE_TECH";
    case CommandType::LogOnly:
    default:
        return "CHAT_LOG_ONLY";
    }
}

std::string formatCommandLog(const Command& command, int frameCount)
{
    std::ostringstream line;
    line << "[Command] frame=" << frameCount
         << " type=" << commandTypeName(command.type)
         << " units=";
    for (size_t index = 0; index < command.unitIds.size(); ++index) {
        if (index > 0) {
            line << ",";
        }
        line << command.unitIds[index];
    }
    if (command.hasTargetUnit) {
        line << " target_unit=" << command.targetUnitId;
    }
    if (command.hasTargetPosition) {
        line << " target_pos=" << command.targetPosition.x << "," << command.targetPosition.y;
    }
    if (!command.unitName.empty()) {
        line << " unit=" << command.unitName;
    }
    if (!command.buildingName.empty()) {
        line << " building=" << command.buildingName;
    }
    if (!command.abilityName.empty()) {
        line << " ability=" << command.abilityName;
    }
    if (!command.payload.empty()) {
        line << " payload=\"" << command.payload << "\"";
    }
    return line.str();
}

} // namespace LAVBWAPIRM
