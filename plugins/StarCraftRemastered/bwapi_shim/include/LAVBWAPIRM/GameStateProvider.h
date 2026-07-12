//20260701_kpopmodder: Separates BWAPI-compatible state reads from the future SCR/Samase backend.
#pragma once

#include "LAVBWAPIRM/Bridge.h"

namespace LAVBWAPIRM {

class GameStateProvider {
public:
    virtual ~GameStateProvider() = default;
    virtual GameSnapshot snapshot() const = 0;
    virtual void advanceFrame() = 0;
    virtual void reset() = 0;
};

} // namespace LAVBWAPIRM
