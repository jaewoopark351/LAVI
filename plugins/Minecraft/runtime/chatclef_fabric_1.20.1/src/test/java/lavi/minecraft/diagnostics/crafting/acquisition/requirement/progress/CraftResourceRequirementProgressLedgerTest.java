package lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260901_kpopmodder: Count semantic material requirements, not polling repeats.
class CraftResourceRequirementProgressLedgerTest {
    @Test
    void identicalProgressIsDeduplicatedAndSuppressedSourceDetailIsCounted() {
        CraftResourceRequirementProgressLedger ledger =
                new CraftResourceRequirementProgressLedger();
        ledger.observe(observation(
                CraftResourceStage.RECIPE_PLANNING,
                "minecraft:iron_pickaxe",
                1,
                true,
                1
        ));
        ledger.observe(observation(
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                "raw_iron",
                3,
                false,
                2
        ));
        ledger.observe(observation(
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                "raw_iron",
                3,
                false,
                3
        ));

        CraftResourceRequirementProgressSnapshot snapshot = ledger.snapshot();
        assertEquals(2, snapshot.requirementTransitionCount());
        assertEquals(CraftResourceStage.IRON_INPUT_ACQUISITION, snapshot.resourceStage());
        assertEquals("raw_iron", snapshot.activeRequirementItem());
        assertEquals(3, snapshot.activeRequirementCount());
        assertEquals(2, snapshot.suppressedDetailCount());
        assertEquals(1, snapshot.firstObservedTick());
        assertEquals(3, snapshot.lastObservedTick());
    }

    @Test
    void unownedRequirementCannotChangeCommandProgress() {
        CraftResourceRequirementProgressLedger ledger =
                new CraftResourceRequirementProgressLedger();
        ledger.observe(new CraftResourceRequirementProgressObservation(
                CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                CraftResourceStage.RAW_IRON_SMELTING,
                "raw_iron",
                3,
                "SMELT_MATERIAL_PROGRESS_SNAPSHOT",
                1,
                true
        ));

        assertEquals(0, ledger.snapshot().requirementTransitionCount());
        assertEquals(1, ledger.snapshot().unownedObservationCount());
    }

    private static CraftResourceRequirementProgressObservation observation(
            CraftResourceStage stage,
            String item,
            long count,
            boolean sourceEmissionCompleted,
            long tick) {
        return new CraftResourceRequirementProgressObservation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                stage,
                item,
                count,
                "SOURCE",
                tick,
                sourceEmissionCompleted
        );
    }
}
