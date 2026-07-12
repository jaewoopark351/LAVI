//20260701_kpopmodder: Minimal SAIDA-style compile probe for the BWAPI-compatible shim.
#include "BWAPI.h"

class MinimalSaidaStyleBot : public BWAPI::AIModule {
public:
    void onStart() override
    {
        BWAPI::Broodwar->sendText("LAV-BWAPI-RM minimal bot started");
    }

    void onFrame() override
    {
        if (!BWAPI::Broodwar->isInGame()) {
            return;
        }

        for (BWAPI::Unit unit : BWAPI::Broodwar->getAllUnits()) {
            if (unit && unit->isIdle() && unit->isCompleted()) {
                unit->move(BWAPI::Position(64, 64));
                break;
            }
        }
    }
};

#ifdef _WIN32
#define LAV_BWAPI_RM_EXPORT __declspec(dllexport)
#else
#define LAV_BWAPI_RM_EXPORT
#endif

extern "C" LAV_BWAPI_RM_EXPORT BWAPI::AIModule* newAIModule()
{
    return new MinimalSaidaStyleBot();
}
