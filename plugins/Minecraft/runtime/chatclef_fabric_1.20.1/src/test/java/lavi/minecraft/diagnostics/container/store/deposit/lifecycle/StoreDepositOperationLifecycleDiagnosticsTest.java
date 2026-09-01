package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize operation registration and tracker binding after lifecycle extraction.
class StoreDepositOperationLifecycleDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void keepsTheOffModeSentinelAndDoesNotCreateAnOperation() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositOperationRegistrationDiagnostics diagnostics =
                new StoreDepositOperationRegistrationDiagnostics(bindings);
        Task root = new TestTask("root");

        Object[] fields = diagnostics.registerBareDepositInvocation(
                null,
                false,
                new ItemTarget[0],
                root,
                "BARE_DEPOSIT_COMMAND"
        );

        assertArrayEquals(new Object[]{"storeContextAvailable", false}, fields);
        assertEquals(null, bindings.stateFor(root));

        bindings.activateRoot(
                root,
                "AUTO_DEPOSIT_ALL_CHAIN"
        );
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        StoreDepositTrackerBindingDiagnostics trackerBindings =
                new StoreDepositTrackerBindingDiagnostics(bindings);
        trackerBindings.bindRootTracker(root, tracker);
        trackerBindings.trackerSubscriptionStarted(tracker);
        assertEquals(null, bindings.trackerBinding(tracker));
    }

    @Test
    void preservesOneOperationAcrossInvocationActivationTrackersAndStopCorrelation() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositOperationRegistrationDiagnostics registration =
                new StoreDepositOperationRegistrationDiagnostics(bindings);
        StoreDepositRootLifecycleDiagnostics rootLifecycle =
                new StoreDepositRootLifecycleDiagnostics(bindings);
        StoreDepositTrackerBindingDiagnostics trackerBindings =
                new StoreDepositTrackerBindingDiagnostics(bindings);
        Task root = new TestTask("root");
        Task interrupt = new TestTask("interrupt");
        ItemTarget[] targets = new ItemTarget[0];

        Object[] invocationFields = registration.registerBareDepositInvocation(
                null,
                false,
                targets,
                root,
                "BARE_DEPOSIT_ALL_COMMAND"
        );
        StoreDepositOperationState registered = bindings.stateFor(root);
        Object[] activationFields = rootLifecycle.onStoreRootStart(root, false, targets);

        assertSame(registered, bindings.stateFor(root));
        assertEquals(field(invocationFields, "storeOperationId"), field(activationFields, "storeOperationId"));
        assertEquals("BARE_DEPOSIT_ALL_COMMAND", field(invocationFields, "storeRequestSource"));
        assertEquals(1, registered.activationCount());

        ContainerStoredTracker rootTracker = new ContainerStoredTracker(slot -> true);
        ContainerStoredTracker targetTracker = new ContainerStoredTracker(slot -> true);
        BlockPos target = new BlockPos(5, 64, -9);
        trackerBindings.bindRootTracker(root, rootTracker);
        trackerBindings.bindTargetTracker(root, targetTracker, target);

        TrackerBinding rootBinding = bindings.trackerBinding(rootTracker);
        TrackerBinding targetBinding = bindings.trackerBinding(targetTracker);
        assertEquals(registered.context().operationId(), rootBinding.operationId());
        assertEquals("ROOT_ANY_CONTAINER", rootBinding.trackerRole());
        assertEquals(registered.context().operationId(), targetBinding.operationId());
        assertEquals("TARGET_CONTAINER", targetBinding.trackerRole());
        assertEquals(target, targetBinding.targetContainer());

        assertFalse(registered.explicitStopCorrelated());
        rootLifecycle.markExplicitCancelCandidate(root);
        assertTrue(registered.explicitStopCorrelated());

        Object[] stopFields = rootLifecycle.onStoreRootStopCallback(root, interrupt);
        assertEquals(true, field(stopFields, "storeStopCallbackOnly"));
        assertEquals(false, field(stopFields, "storeStopCallbackIsTerminalAuthority"));
        assertEquals(interrupt.getClass().getName(), field(stopFields, "storeInterruptTaskClass"));
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
