package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Keep HEAD and RETURN on one immutable proven attempt reference.
class FabricChatClefCraftResourceInteractionReferenceRegistryTest {
    @Test
    void completionReturnsTheSameAttemptWithoutCreatingAnotherTarget() {
        FabricChatClefCraftResourceInteractionReferenceRegistry registry =
                new FabricChatClefCraftResourceInteractionReferenceRegistry();
        FabricChatClefCraftResourceInteractionAttemptReference reference = reference(42);

        assertTrue(registry.begin(reference));
        assertFalse(registry.begin(reference));
        assertEquals(reference, registry.complete(42).orElseThrow());
        assertTrue(registry.complete(42).isEmpty());
    }

    @Test
    void ninthInFlightHeadIsRefusedWithoutEvictingAnExistingReference() {
        FabricChatClefCraftResourceInteractionReferenceRegistry registry =
                new FabricChatClefCraftResourceInteractionReferenceRegistry();
        for (long interactionId = 1; interactionId <= 8; interactionId++) {
            assertTrue(registry.begin(reference(interactionId)));
        }

        assertFalse(registry.begin(reference(9)));
        assertEquals(8, registry.size());
        assertTrue(registry.complete(1).isPresent());
    }

    private static FabricChatClefCraftResourceInteractionAttemptReference reference(
            long interactionId) {
        return new FabricChatClefCraftResourceInteractionAttemptReference(
                interactionId,
                new IronPickaxeAcquisitionScopeKey(
                        "session", 1, "request", "correlation", "root", 2, "instance"
                ),
                3,
                new CraftResourceTargetTuple(
                        CraftResourceStage.RAW_IRON_SMELTING,
                        CraftResourceTargetRole.FURNACE_INTERACTION,
                        "1,2,3",
                        List.of("minecraft:furnace")
                ),
                "minecraft.furnace",
                "furnace",
                80
        );
    }
}
