//20260701_kpopmodder: Runs BWAPI::AIModule callbacks without attaching to StarCraft Remastered.
#pragma once

#include <memory>

#include "BWAPI.h"
#include "LAVBWAPIRM/Bridge.h"

namespace LAVBWAPIRM {

class CompatRunner {
public:
    explicit CompatRunner(std::shared_ptr<Bridge> bridge = nullptr);

    bool run(BWAPI::AIModule& module, int frameLimit);
    void stop();

private:
    std::shared_ptr<Bridge> bridge_;
    bool running_ = false;
};

} // namespace LAVBWAPIRM
