package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetInfo;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove the facade shares one operation registry with interaction correlation.
class StoreDepositDiagnosticsInteractionContractTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
    }

    @Test
    void bindsFieldsScopeAndDedupeToTheSameDepositAllAttempt() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask("root");
        Task routeChild = new TestTask("route-child");
        BlockPos target = new BlockPos(-533, 50, 126);
        Object[] registration = StoreDepositDiagnostics.registerBareDepositInvocation(
                null,
                false,
                new ItemTarget[0],
                root,
                "BARE_DEPOSIT_ALL_COMMAND"
        );
        String operationId = String.valueOf(field(registration, "storeOperationId"));
        StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
        StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                root,
                "OPEN_EXISTING",
                true,
                target,
                true,
                true,
                false,
                false,
                null,
                new ItemTarget[0]
        );
        StoreDepositDiagnostics.logChildReconciliation(
                root, null, routeChild, false, true, true, true, false, routeChild
        );
        StoreDepositDiagnostics.logPursuitDecision(
                routeChild, null, target, "PURSUIT_VALIDATION_ACCEPTED"
        );
        BlockInteractionContext interaction = interaction(91, target);

        StoreDepositDiagnostics.bindInteraction(routeChild, interaction);

        Object[] fields = StoreDepositDiagnostics.interactionFields(interaction);
        String expectedAttemptId = operationId + "-attempt-1";
        assertEquals(true, field(fields, "storeContextAvailable"));
        assertEquals(operationId, field(fields, "storeOperationId"));
        assertEquals(expectedAttemptId, field(fields, "storeAttemptId"));
        assertEquals(expectedAttemptId, StoreDepositDiagnostics.interactionScopeKey(interaction));
        assertTrue(StoreDepositDiagnostics.shouldEmitInteractionDetail(
                interaction, "INTERACTION_CONTRACT", "state-a"
        ));
        assertFalse(StoreDepositDiagnostics.shouldEmitInteractionDetail(
                interaction, "INTERACTION_CONTRACT", "state-a"
        ));

        StoreDepositDiagnostics.logNaturalFinish(root);
    }

    private static BlockInteractionContext interaction(long id, BlockPos target) {
        return new BlockInteractionContext(
                id,
                ChatClefDiagnostics.currentClientTickId(),
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

    private static Object field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private static final class TestTask extends Task {
        private final String name;

        private TestTask(String name) {
            this.name = name;
        }

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
            return name;
        }
    }
}
