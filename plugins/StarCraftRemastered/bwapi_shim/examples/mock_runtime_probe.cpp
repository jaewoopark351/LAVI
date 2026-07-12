//20260701_kpopmodder: Exercises onStart/onFrame and command logging without controlling the game.
#include "BWAPI.h"
#include "LAVBWAPIRM/CompatRunner.h"
#include "LAVBWAPIRM/MockBridge.h"

#include <iostream>
#include <memory>

class MockSaidaOpeningBot : public BWAPI::AIModule {
public:
    void onStart() override
    {
        BWAPI::Broodwar->sendText("Mock SAIDA opening started");
        std::cout << "[Bot] onStart map=" << BWAPI::Broodwar->mapName() << "\n";
    }

    void onFrame() override
    {
        const int frame = BWAPI::Broodwar->getFrameCount();
        BWAPI::Player self = BWAPI::Broodwar->self();
        std::cout << "[Bot] onFrame frame=" << frame
                  << " minerals=" << self->minerals()
                  << " gas=" << self->gas()
                  << " supply=" << self->supplyUsed() << "/" << self->supplyTotal()
                  << " units=" << self->getUnits().size() << "\n";

        BWAPI::Unit firstMineral = nullptr;
        for (BWAPI::Unit unit : BWAPI::Broodwar->getMinerals()) {
            firstMineral = unit;
            break;
        }

        for (BWAPI::Unit unit : self->getUnits()) {
            if (!unit || !unit->exists() || !unit->isCompleted()) {
                continue;
            }

            if (frame == 1 && unit->getType().isResourceDepot()) {
                unit->train(BWAPI::UnitTypes::Terran_SCV);
            } else if (frame == 2 && unit->getType().isWorker() && firstMineral) {
                unit->gather(firstMineral);
            } else if (frame == 3 && unit->getType().isWorker()) {
                unit->build(BWAPI::UnitTypes::Terran_Supply_Depot, BWAPI::TilePosition(10, 10));
            } else if (frame == 4 && unit->canMove()) {
                unit->move(BWAPI::Position(384, 384));
            } else if (frame == 5 && unit->canAttack()) {
                unit->attack(BWAPI::Position(3000, 300));
            }
        }
    }

    void onEnd(bool) override
    {
        std::cout << "[Bot] onEnd\n";
    }
};

int main()
{
    auto bridge = std::make_shared<LAVBWAPIRM::MockBridge>();
    LAVBWAPIRM::CompatRunner runner(bridge);
    MockSaidaOpeningBot bot;
    return runner.run(bot, 5) ? 0 : 1;
}
