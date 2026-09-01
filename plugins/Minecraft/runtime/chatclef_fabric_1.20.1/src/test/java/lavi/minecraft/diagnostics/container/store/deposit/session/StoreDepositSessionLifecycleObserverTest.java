package lavi.minecraft.diagnostics.container.store.deposit.session;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.effect.StoreDepositEffectDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.session.mode.StoreDepositModeLifecycleObserver;
import lavi.minecraft.diagnostics.container.store.deposit.session.snapshot.StoreDepositSessionSnapshotContributor;
import lavi.minecraft.diagnostics.container.store.deposit.session.snapshot.StoreDepositSessionStateSnapshotReader;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedgerSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Prove OFF invalidates only diagnostic Store state and records one gap.
class StoreDepositSessionLifecycleObserverTest {
    @Test
    void modeOffClearsActiveContextsAndRecordsExactlyOnePartialGap() {
        Fixture fixture = new Fixture();
        Task root = new TestTask();
        fixture.bindings.activateRoot(root, "AUTO_DEPOSIT_ALL_CHAIN");
        fixture.automaticLedger.beginRun(root, root, 17L);
        fixture.emissionGate.shouldEmitDetail("operation", "EVENT", "fingerprint");

        fixture.modeObserver.beforeModeOff();
        StoreDepositTerminalLedgerSnapshot first = fixture.terminalLedger.snapshot();

        assertNull(fixture.bindings.stateFor(root));
        assertEquals(0, fixture.automaticLedger.activeRunCount());
        assertEquals(1, first.partialModeDisabledCoverageGapCount());
        assertTrue(first.modeDisabledInvalidatedContextCount() >= 2);
        assertEquals(1, first.ledgerCoverageGapCount());

        fixture.modeObserver.beforeModeOff();
        StoreDepositTerminalLedgerSnapshot second = fixture.terminalLedger.snapshot();
        assertEquals(1, second.partialModeDisabledCoverageGapCount());
        assertEquals(first.modeDisabledInvalidatedContextCount(),
                second.modeDisabledInvalidatedContextCount());
    }

    @Test
    void cleanTeardownSnapshotIsCapturedBeforeTransientStateIsCleared() {
        Fixture fixture = new Fixture();
        Task root = new TestTask();
        fixture.bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        Object[] fields = fixture.snapshotContributor.finalSnapshotFields();
        fixture.modeObserver.afterCleanTeardownSnapshotAttempt(true);
        fixture.snapshotContributor.afterCleanTeardownSnapshotAttempt(true);

        assertEquals("1", field(fields, "activeStoreDiagnosticOperationCount"));
        assertNull(fixture.bindings.stateFor(root));
        assertEquals(0, fixture.terminalLedger.snapshot()
                .partialModeDisabledCoverageGapCount());
        assertEquals(1L, fixture.terminalLedger.snapshot()
                .previousEmissionCallsReturnedSnapshotSequence());
    }

    @Test
    void failedFinalSnapshotEmissionDoesNotClaimCallsReturnedProvenance() {
        Fixture fixture = new Fixture();

        fixture.snapshotContributor.finalSnapshotFields();
        fixture.snapshotContributor.afterCleanTeardownSnapshotAttempt(false);

        assertEquals(0L, fixture.terminalLedger.snapshot()
                .previousEmissionCallsReturnedSnapshotSequence());
    }

    private static String field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return String.valueOf(fields[index + 1]);
            }
        }
        return "MISSING";
    }

    private static final class Fixture {
        private final StoreDepositBindingRegistry bindings =
                new StoreDepositBindingRegistry();
        private final StoreDepositEmissionGate emissionGate =
                new StoreDepositEmissionGate();
        private final StoreDepositAutomaticLifecycleLedger automaticLedger =
                new StoreDepositAutomaticLifecycleLedger();
        private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals =
                new StoreDepositAutomaticTerminalDiagnostics(automaticLedger, emissionGate);
        private final StoreDepositInteractionDiagnostics interactions =
                new StoreDepositInteractionDiagnostics(
                        bindings,
                        emissionGate,
                        new StoreDepositInteractionBindingRegistry()
                );
        private final StoreDepositTransferAttemptRegistry transferAttempts =
                new StoreDepositTransferAttemptRegistry(bindings, automaticTerminals);
        private final StoreDepositSlotActionDiagnostics slotActions =
                new StoreDepositSlotActionDiagnostics(
                        transferAttempts,
                        emissionGate,
                        automaticTerminals
                );
        private final StoreDepositEffectDiagnostics effects =
                new StoreDepositEffectDiagnostics(bindings, emissionGate, slotActions);
        private final StoreDepositTerminalLedger terminalLedger =
                new StoreDepositTerminalLedger();
        private final StoreDepositModeLifecycleObserver modeObserver =
                new StoreDepositModeLifecycleObserver(
                        new StoreDepositModeStateInvalidator(
                                bindings,
                                interactions,
                                transferAttempts,
                                slotActions,
                                effects,
                                emissionGate,
                                automaticLedger
                        ),
                        terminalLedger
                );
        private final StoreDepositSessionSnapshotContributor snapshotContributor =
                new StoreDepositSessionSnapshotContributor(
                        new StoreDepositSessionStateSnapshotReader(
                                bindings,
                                automaticLedger
                        ),
                        terminalLedger
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
            return "store-deposit-session-lifecycle-observer-test";
        }
    }
}
