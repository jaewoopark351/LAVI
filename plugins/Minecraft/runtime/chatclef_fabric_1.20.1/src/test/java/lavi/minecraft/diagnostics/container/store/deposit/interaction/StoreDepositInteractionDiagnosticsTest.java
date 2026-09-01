package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetInfo;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize interaction correlation behind the unchanged store-deposit facade.
class StoreDepositInteractionDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void keepsTheUnboundOffModeContract() {
        StoreDepositInteractionDiagnostics diagnostics = diagnostics(
                new StoreDepositBindingRegistry(),
                new StoreDepositEmissionGate()
        );

        assertArrayEquals(new Object[]{
                "storeContextAvailable", false,
                "storeContextCoverageReason", "NO_EXACT_ACTIVE_ROUTE_AND_TARGET_BINDING"
        }, diagnostics.interactionFields(null));
        assertEquals("store-unbound", diagnostics.interactionScopeKey(null));
        assertTrue(diagnostics.shouldEmitInteractionDetail(null, "EVENT_A", "state-a"));
    }

    @Test
    void bindsTheExactDepositAllAttemptAndDeduplicatesWithinItsScope() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositInteractionDiagnostics diagnostics = diagnostics(bindings, emissionGate);
        Task root = new TestTask("root");
        Task routeChild = new TestTask("route-child");
        BlockPos target = new BlockPos(-533, 50, 126);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");
        bindings.bindChild(root, routeChild);
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
        BlockInteractionContext interaction = interaction(41, target);

        diagnostics.bindInteraction(routeChild, interaction);

        Object[] fields = diagnostics.interactionFields(interaction);
        String expectedAttemptId = state.context().operationId() + "-attempt-1";
        assertEquals(true, field(fields, "storeContextAvailable"));
        assertEquals(state.context().operationId(), field(fields, "storeOperationId"));
        assertEquals(expectedAttemptId, field(fields, "storeAttemptId"));
        assertEquals(expectedAttemptId, diagnostics.interactionScopeKey(interaction));
        assertTrue(diagnostics.shouldEmitInteractionDetail(interaction, "EVENT_A", "state-a"));
        assertFalse(diagnostics.shouldEmitInteractionDetail(interaction, "EVENT_A", "state-a"));
        assertTrue(diagnostics.shouldEmitInteractionDetail(interaction, "EVENT_B", "state-a"));

        bindings.purgeOperation(root);
        Object[] fieldsAfterPurge = diagnostics.interactionFields(interaction);
        assertEquals(true, field(fieldsAfterPurge, "storeContextAvailable"));
        assertEquals(false, field(fieldsAfterPurge, "storeOperationActiveAtObservation"));
        assertEquals("UNAVAILABLE_OPERATION_NOT_ACTIVE", field(fieldsAfterPurge, "branchAtObservation"));
    }

    @Test
    void refusesAContextForAnUnrelatedTarget() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositInteractionDiagnostics diagnostics = diagnostics(
                bindings,
                new StoreDepositEmissionGate()
        );
        Task root = new TestTask("root");
        Task routeChild = new TestTask("route-child");
        BlockPos target = new BlockPos(10, 64, 10);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");
        bindings.bindChild(root, routeChild);
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
        BlockInteractionContext unrelated = interaction(42, target.add(1, 0, 0));

        diagnostics.bindInteraction(routeChild, unrelated);

        assertEquals(false, field(diagnostics.interactionFields(unrelated), "storeContextAvailable"));
        assertEquals("store-unbound", diagnostics.interactionScopeKey(unrelated));
    }

    private static StoreDepositInteractionDiagnostics diagnostics(StoreDepositBindingRegistry bindings,
                                                                  StoreDepositEmissionGate emissionGate) {
        return new StoreDepositInteractionDiagnostics(
                bindings,
                emissionGate,
                new StoreDepositInteractionBindingRegistry()
        );
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
