package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetInfo;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoreDepositInteractionContextTest {
    @Test
    void bindsOnlyTheExactActiveOpenRouteAndTarget() {
        BlockPos target = new BlockPos(-533, 50, 126);
        TestTask routeChild = new TestTask();
        StoreDepositOperationState state = new StoreDepositOperationState(new StoreDepositOperationContext(
                "operation-a",
                "BARE_DEPOSIT_ALL_COMMAND",
                null,
                10,
                0
        ));
        state.routeState().recordParentDecision(
                "OPEN_EXISTING",
                true,
                target,
                true,
                true,
                false,
                false,
                null,
                "RAW_CLOSEST_WITHIN_50",
                "targets-a"
        );
        state.routeState().recordChildReconciliation("ROOT_ROUTE", routeChild, true);
        state.routeState().recordPursuit(target, "PURSUIT_VALIDATION_ACCEPTED");

        StoreDepositInteractionContext bound = StoreDepositInteractionContext.capture(
                state,
                routeChild,
                interaction(41, target)
        );

        assertNotNull(bound);
        assertEquals("operation-a-attempt-1", bound.storeAttemptId());
        assertEquals("EXACT", bound.contextBindingConfidence());
        assertEquals("CURRENT_PURSUIT", bound.targetRole());
        assertNull(StoreDepositInteractionContext.capture(
                state,
                routeChild,
                interaction(42, target.add(10, 0, 0))
        ));
    }

    private static BlockInteractionContext interaction(long id, BlockPos target) {
        return new BlockInteractionContext(
                id,
                20,
                new BlockInteractionTargetInfo(
                        true,
                        "container",
                        "minecraft:chest",
                        "Chest",
                        "state",
                        target
                ),
                null,
                null,
                null,
                true
        );
    }

    private static final class TestTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "store-interaction-context-test";
        }
    }
}
