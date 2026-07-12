//20260701_kpopmodder: Minimal BWAPI client stub for SAIDA source-compatibility builds.
#pragma once

#include "BWAPI.h"

namespace BWAPI {

class Client {
public:
    bool connect() { return true; }
    void update() {}
    bool isConnected() const { return true; }
};

inline Client BWAPIClient;

} // namespace BWAPI
