package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoreDepositInteractionBindingRegistryTest {
    @Test
    void expiresBindingsAfterThePostconditionWindowHorizon() {
        StoreDepositInteractionBindingRegistry registry = new StoreDepositInteractionBindingRegistry();
        registry.bind(context(1, 10), 10);

        assertNotNull(registry.find(1, 50));
        assertNull(registry.find(1, 51));
        assertEquals(0, registry.size());
    }

    private static StoreDepositInteractionContext context(long interactionId, long startTick) {
        return new StoreDepositInteractionContext(
                interactionId,
                "operation-a",
                "operation-a-attempt-1",
                1,
                1,
                1,
                "OPEN_EXISTING",
                1,
                "route",
                "route-id",
                "active",
                "active-id",
                1,
                "CURRENT_PURSUIT",
                new BlockPos(1, 2, 3),
                "ACTIVE_TASK_IDENTITY_BINDING_AND_TARGET_VALUE",
                "EXACT",
                startTick
        );
    }
}
