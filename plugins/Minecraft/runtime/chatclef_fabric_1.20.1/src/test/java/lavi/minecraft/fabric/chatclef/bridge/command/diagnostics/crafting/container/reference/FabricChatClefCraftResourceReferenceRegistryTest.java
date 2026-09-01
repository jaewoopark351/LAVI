package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Prove candidate reference deduplication is separate from target attempts.
class FabricChatClefCraftResourceReferenceRegistryTest {
    @Test
    void unknownAssociationCannotInstallOrAdvanceAReference() {
        FabricChatClefCraftResourceReferenceRegistry registry =
                new FabricChatClefCraftResourceReferenceRegistry();
        IronPickaxeAcquisitionScopeKey key = key(1);
        CraftResourceTargetTuple furnace = furnaceInteraction();

        FabricChatClefCraftResourceReferenceDecision unknown = registry.observe(
                key,
                CraftResourceAssociationStatus.UNKNOWN,
                furnace
        );
        FabricChatClefCraftResourceReferenceDecision firstOwned = registry.observe(
                key,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                furnace
        );
        FabricChatClefCraftResourceReferenceDecision repeat = registry.observe(
                key,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                furnace
        );

        assertFalse(unknown.changed());
        assertFalse(unknown.retained());
        assertTrue(firstOwned.changed());
        assertTrue(firstOwned.retained());
        assertTrue(firstOwned.detailEligible());
        assertFalse(repeat.changed());
        assertTrue(repeat.retained());
        assertFalse(repeat.detailEligible());
    }

    @Test
    void referenceTransitionsKeepUpdatingButOnlyFirstSixtyFourRequestDetail() {
        FabricChatClefCraftResourceReferenceRegistry registry =
                new FabricChatClefCraftResourceReferenceRegistry();
        IronPickaxeAcquisitionScopeKey key = key(1);
        FabricChatClefCraftResourceReferenceDecision decision = null;

        for (int index = 0; index < 65; index++) {
            decision = registry.observe(
                    key,
                    CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                    furnaceInteraction("-525,120," + index)
            );
            assertEquals(index < 64, decision.detailEligible(), Integer.toString(index));
        }

        assertEquals(65L, decision.transitionCount());
        assertEquals(64L, decision.detailEligibleTransitionCount());
        assertEquals(1L, decision.suppressedDetailTransitionCount());
        assertFalse(decision.counterSaturated());
        assertEquals("-525,120,64", decision.current().targetPosition());
    }

    @Test
    void registryRefusesANinthConcurrentReferenceInsteadOfEvictingAnActiveOne() {
        FabricChatClefCraftResourceReferenceRegistry registry =
                new FabricChatClefCraftResourceReferenceRegistry();
        for (int index = 0; index < 8; index++) {
            FabricChatClefCraftResourceReferenceDecision decision = registry.observe(
                    key(index),
                    CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                    furnaceInteraction()
            );
            assertTrue(decision.changed(), Integer.toString(index));
            assertTrue(decision.retained(), Integer.toString(index));
        }

        FabricChatClefCraftResourceReferenceDecision ninth = registry.observe(
                key(8),
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                furnaceInteraction()
        );

        assertFalse(ninth.changed());
        assertFalse(ninth.retained());
        assertEquals(CraftResourceTargetRole.FURNACE_INTERACTION, ninth.current().targetRole());
    }

    private static CraftResourceTargetTuple furnaceInteraction() {
        return furnaceInteraction("-525,120,-1067");
    }

    private static CraftResourceTargetTuple furnaceInteraction(String targetPosition) {
        return new CraftResourceTargetTuple(
                CraftResourceStage.RAW_IRON_SMELTING,
                CraftResourceTargetRole.FURNACE_INTERACTION,
                targetPosition,
                List.of("minecraft:furnace")
        );
    }

    private static IronPickaxeAcquisitionScopeKey key(int index) {
        return new IronPickaxeAcquisitionScopeKey(
                "session-" + index,
                index,
                "request-" + index,
                "correlation-" + index,
                "assignment-" + index,
                index,
                "root-" + index
        );
    }
}
