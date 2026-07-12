//20260701_kpopmodder: Implements command logging over a mock provider for local SAIDA loop tests.
#include "LAVBWAPIRM/MockBridge.h"

#include <iostream>
#include <memory>
#include <utility>

#include "LAVBWAPIRM/MockGameStateProvider.h"

namespace LAVBWAPIRM {

MockBridge::MockBridge(
    std::shared_ptr<GameStateProvider> provider,
    std::ostream* commandLog
)
    : provider_(provider ? std::move(provider) : std::make_shared<MockGameStateProvider>())
    , commandLog_(commandLog ? commandLog : &std::cout)
{
}

bool MockBridge::connect()
{
    connected_ = true;
    provider_->reset();
    return true;
}

void MockBridge::disconnect()
{
    connected_ = false;
}

GameSnapshot MockBridge::snapshot()
{
    GameSnapshot current = provider_->snapshot();
    current.connected = connected_;
    current.inGame = connected_ && current.inGame;
    current.singlePlayer = true;
    return current;
}

bool MockBridge::sendCommand(const Command& command)
{
    commands_.push_back(command);
    if (commandLog_) {
        *commandLog_ << formatCommandLog(command, snapshot().frameCount) << std::endl;
    }
    return connected_;
}

void MockBridge::stopAllControl()
{
    Command command;
    command.type = CommandType::Stop;
    command.payload = "stop_all_control";
    sendCommand(command);
}

void MockBridge::advanceFrame()
{
    provider_->advanceFrame();
}

const std::vector<Command>& MockBridge::commands() const
{
    return commands_;
}

} // namespace LAVBWAPIRM
