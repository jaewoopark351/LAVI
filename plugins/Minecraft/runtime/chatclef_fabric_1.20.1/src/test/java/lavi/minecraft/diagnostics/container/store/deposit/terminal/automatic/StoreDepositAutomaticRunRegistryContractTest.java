package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Characterize identity bindings, clear scope, and cumulative registry counters.
class StoreDepositAutomaticRunRegistryContractTest {
    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void taskBindingsUseObjectIdentityEvenWhenTasksAreLogicallyEqual() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenanceOne = new LogicallyEqualTask("maintenance");
        Task maintenanceTwo = new LogicallyEqualTask("maintenance");
        Task userRootOne = new LogicallyEqualTask("user-root");
        Task userRootTwo = new LogicallyEqualTask("user-root");
        Task childOne = new LogicallyEqualTask("child");
        Task childTwo = new LogicallyEqualTask("child");

        assertEquals(maintenanceOne, maintenanceTwo);
        assertEquals(userRootOne, userRootTwo);
        assertEquals(childOne, childTwo);

        StoreDepositAutomaticContext runOne = ledger.beginRun(maintenanceOne, userRootOne, 1L);
        StoreDepositAutomaticContext runTwo = ledger.beginRun(maintenanceTwo, userRootTwo, 2L);
        StoreDepositAutomaticContext childContextOne = ledger.registerChild(maintenanceOne, childOne, 0);
        StoreDepositAutomaticContext childContextTwo = ledger.registerChild(maintenanceOne, childTwo, 1);

        assertEquals(2, ledger.activeRunCount());
        assertNotEquals(runOne.autoOperationId(), runTwo.autoOperationId());
        assertNotEquals(childContextOne.autoChildOperationId(), childContextTwo.autoChildOperationId());
        assertEquals(childContextOne, ledger.contextForChild(childOne));
        assertEquals(childContextTwo, ledger.contextForChild(childTwo));

        ledger.recordPressureOwnedRunClose(maintenanceOne, "FIRST_DONE");
        ledger.recordPressureOwnedRunClose(maintenanceTwo, "SECOND_DONE");
        assertEquals(
                runOne.autoOperationId(),
                ledger.observeUserTaskResume(userRootOne).context().autoOperationId()
        );
        assertEquals(
                runTwo.autoOperationId(),
                ledger.observeUserTaskResume(userRootTwo).context().autoOperationId()
        );
    }

    @Test
    void modeTransitionClearRemovesOnlyTheFiveCollectionsAndPreservesEpoch() {
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task userRoot = new TestTask("shared-user-root");
        Task maintenanceOne = new TestTask("maintenance-one");
        Task maintenanceTwo = new TestTask("maintenance-two");
        Task childOne = new TestTask("child-one");
        Task childTwo = new TestTask("child-two");
        StoreDepositAutomaticContext first = ledger.beginRun(maintenanceOne, userRoot, 11L);
        StoreDepositAutomaticContext second = ledger.beginRun(maintenanceTwo, userRoot, 12L);
        ledger.registerChild(maintenanceOne, childOne, 0);
        ledger.registerChild(maintenanceTwo, childTwo, 0);

        StoreDepositAutomaticLifecycleLedger.ClearResult cleared = ledger.clearForModeTransition();
        assertEquals(2, cleared.activeRunCount());
        assertEquals(0, cleared.pendingEvictionCount());
        assertEquals(2, cleared.maintenanceBindingCount());
        assertEquals(2, cleared.childBindingCount());
        assertEquals(1, cleared.userRootBindingCount());
        assertEquals(7, cleared.totalEntryCount());
        assertEquals(0, ledger.activeRunCount());
        assertFalse(ledger.contextForMaintenance(maintenanceOne).available());
        assertFalse(ledger.contextForChild(childOne).available());

        StoreDepositAutomaticContext afterClear = ledger.beginRun(
                new TestTask("maintenance-after-clear"),
                null,
                13L
        );
        assertEquals(first.autoOperationEpoch() + 2, afterClear.autoOperationEpoch());
        assertEquals(second.autoOperationEpoch() + 1, afterClear.autoOperationEpoch());
        assertEquals("auto-deposit-3", afterClear.autoOperationId());
    }

    @Test
    void modeTransitionClearPreservesEvictionOverflowAndSuppressionCounters() {
        StoreDepositAutomaticLifecycleLedger overflowLedger = new StoreDepositAutomaticLifecycleLedger();
        for (int index = 0; index < 33; index++) {
            overflowLedger.beginRun(new TestTask("overflow-before-clear-" + index), null, index);
        }
        overflowLedger.clearForModeTransition();
        for (int index = 0; index < 17; index++) {
            overflowLedger.beginRun(new TestTask("overflow-after-clear-" + index), null, index);
        }
        StoreDepositAutomaticLifecycleLedger.TerminalRecord overflowAfterClear =
                overflowLedger.takePendingEviction();
        assertTrue(overflowAfterClear.available());
        assertEquals(1L, overflowAfterClear.snapshot().activeRunEvictionQueueOverflowCount());

        StoreDepositAutomaticLifecycleLedger suppressionLedger =
                new StoreDepositAutomaticLifecycleLedger();
        StoreDepositAutomaticLifecycleLedger.TerminalRecord firstEviction = fillAndEvict(
                suppressionLedger,
                "suppression-before-clear"
        );
        assertEquals(
                1L,
                suppressionLedger.recordTerminalEmissionSuppressed(firstEviction)
                        .activeRunEvictionEmissionSuppressedCount()
        );
        suppressionLedger.clearForModeTransition();
        StoreDepositAutomaticLifecycleLedger.TerminalRecord secondEviction = fillAndEvict(
                suppressionLedger,
                "suppression-after-clear"
        );
        assertEquals(
                2L,
                suppressionLedger.recordTerminalEmissionSuppressed(secondEviction)
                        .activeRunEvictionEmissionSuppressedCount()
        );
    }

    private static StoreDepositAutomaticLifecycleLedger.TerminalRecord fillAndEvict(
            StoreDepositAutomaticLifecycleLedger ledger,
            String prefix) {
        for (int index = 0; index < 17; index++) {
            ledger.beginRun(new TestTask(prefix + "-" + index), null, index);
        }
        return ledger.takePendingEviction();
    }

    private static final class LogicallyEqualTask extends Task {
        private final String identity;

        private LogicallyEqualTask(String identity) {
            this.identity = identity;
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
            return other instanceof LogicallyEqualTask equal && identity.equals(equal.identity);
        }

        @Override
        protected String toDebugString() {
            return identity;
        }
    }
}
