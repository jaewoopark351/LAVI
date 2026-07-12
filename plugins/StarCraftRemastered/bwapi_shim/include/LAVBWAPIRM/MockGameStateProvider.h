//20260701_kpopmodder: Provides deterministic single-player state for SAIDA source-compatibility tests.
#pragma once

#include "LAVBWAPIRM/GameStateProvider.h"

namespace LAVBWAPIRM {

class MockGameStateProvider final : public GameStateProvider {
public:
    MockGameStateProvider();

    GameSnapshot snapshot() const override;
    void advanceFrame() override;
    void reset() override;

private:
    GameSnapshot snapshot_;

    void seedInitialState();
    void seedTechSurfaceScenario();
    void seedResearchUpgradeScenario();
    void seedCombatScenario();
};

} // namespace LAVBWAPIRM
