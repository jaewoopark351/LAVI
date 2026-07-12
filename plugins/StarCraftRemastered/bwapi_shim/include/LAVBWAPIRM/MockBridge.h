//20260701_kpopmodder: Logs BWAPI commands while feeding mock game state to strategy code.
#pragma once

#include <memory>
#include <ostream>
#include <vector>

#include "LAVBWAPIRM/Bridge.h"
#include "LAVBWAPIRM/GameStateProvider.h"

namespace LAVBWAPIRM {

class MockBridge final : public Bridge {
public:
    explicit MockBridge(
        std::shared_ptr<GameStateProvider> provider = nullptr,
        std::ostream* commandLog = nullptr
    );

    bool connect() override;
    void disconnect() override;
    GameSnapshot snapshot() override;
    bool sendCommand(const Command& command) override;
    void stopAllControl() override;
    void advanceFrame() override;

    const std::vector<Command>& commands() const;

private:
    std::shared_ptr<GameStateProvider> provider_;
    std::ostream* commandLog_ = nullptr;
    std::vector<Command> commands_;
    bool connected_ = false;
};

} // namespace LAVBWAPIRM
