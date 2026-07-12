//20260701_kpopmodder: Drives BWAPI AIModule callbacks against a safe bridge backend.
#include "LAVBWAPIRM/CompatRunner.h"

#include <iostream>
#include <memory>
#include <utility>

#include "LAVBWAPIRM/MockBridge.h"

namespace LAVBWAPIRM {

CompatRunner::CompatRunner(std::shared_ptr<Bridge> bridge)
    : bridge_(bridge ? std::move(bridge) : std::make_shared<MockBridge>())
{
}

bool CompatRunner::run(BWAPI::AIModule& module, int frameLimit)
{
    setBridge(bridge_);
    BWAPI::Broodwar->setBridge(bridge_);

    if (!bridge_->connect()) {
        return false;
    }

    running_ = true;
    BWAPI::Broodwar->refresh();
    std::cout << "[CompatRunner] onStart begin" << std::endl;
    module.onStart();
    std::cout << "[CompatRunner] onStart end" << std::endl;

    for (int frame = 0; running_ && frame < frameLimit; ++frame) {
        bridge_->advanceFrame();
        BWAPI::Broodwar->refresh();
        std::cout << "[CompatRunner] onFrame begin frame=" << BWAPI::Broodwar->getFrameCount() << std::endl;
        module.onFrame();
        std::cout << "[CompatRunner] onFrame end frame=" << BWAPI::Broodwar->getFrameCount() << std::endl;
    }

    std::cout << "[CompatRunner] onEnd begin" << std::endl;
    module.onEnd(false);
    std::cout << "[CompatRunner] onEnd end" << std::endl;
    bridge_->disconnect();
    running_ = false;
    return true;
}

void CompatRunner::stop()
{
    running_ = false;
}

} // namespace LAVBWAPIRM
