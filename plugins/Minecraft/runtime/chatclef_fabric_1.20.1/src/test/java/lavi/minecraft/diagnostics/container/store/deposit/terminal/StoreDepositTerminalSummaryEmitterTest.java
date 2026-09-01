package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedgerSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalClassification;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize terminal summary emission before further StoreDepositDiagnostics no-growth extraction.
class StoreDepositTerminalSummaryEmitterTest {
    @BeforeEach
    void enableFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void emitsOneTerminalSummaryGroupThenPurgesTheOperation() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositTerminalSummaryEmitter emitter = new StoreDepositTerminalSummaryEmitter(bindings, emissionGate);
        Task rootTask = new TestTask();
        StoreDepositOperationState state = bindings.activateRoot(rootTask, "BARE_DEPOSIT_COMMAND");

        String firstOutput = captureOutput(() -> emitter.emitAndPurge(
                rootTask,
                state,
                "NATURAL_FINISH",
                "NATURAL_FINISH_OBSERVED"
        ));
        String secondOutput = captureOutput(() -> emitter.emitAndPurge(
                rootTask,
                state,
                "NATURAL_FINISH",
                "NATURAL_FINISH_OBSERVED"
        ));

        assertNull(bindings.stateFor(rootTask));
        assertTrue(state.terminalFinalized());
        assertEquals(1, occurrences(firstOutput, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
        assertEquals(1, occurrences(firstOutput, "event=STORE_DEPOSIT_EFFECT_SUMMARY"));
        assertEquals(1, occurrences(firstOutput, "event=STORE_BARITONE_OPERATION_SUMMARY"));
        assertEquals(1, occurrences(firstOutput, "event=STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY"));
        assertEquals(0, occurrences(firstOutput, "event=STORE_DEPOSIT_TERMINAL_GROUP_RESERVE_EXHAUSTED"));
        assertInOrder(
                firstOutput,
                "event=STORE_DEPOSIT_TERMINAL_SUMMARY",
                "event=STORE_DEPOSIT_EFFECT_SUMMARY",
                "event=STORE_BARITONE_OPERATION_SUMMARY",
                "event=STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY"
        );
        assertEquals("", secondOutput.trim());
        assertEquals(1, emitter.ledgerSnapshot().duplicateFinalizationAttempts());
    }

    @Test
    void mergesDepositAllRouteEvidenceIntoTheTerminalSummary() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositTerminalSummaryEmitter emitter = new StoreDepositTerminalSummaryEmitter(bindings, emissionGate);
        Task rootTask = new TestTask();
        StoreDepositOperationState state = bindings.activateRoot(rootTask, "BARE_DEPOSIT_ALL_COMMAND");

        String output = captureOutput(() -> emitter.emitAndPurge(
                rootTask,
                state,
                "NATURAL_FINISH",
                "NATURAL_FINISH_OBSERVED"
        ));

        assertEquals(1, occurrences(output, "routeSummaryAvailable=true"));
        assertEquals(1, occurrences(output, "finalBranch=NONE"));
        assertNull(bindings.stateFor(rootTask));
    }

    @Test
    void routineExhaustionDoesNotBorrowTheIndependentAbnormalTerminalQuota() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositTerminalSummaryEmitter emitter = new StoreDepositTerminalSummaryEmitter(bindings, emissionGate);
        String routineOutput = captureOutput(() -> {
            for (int index = 0; index < 8; index++) {
                Task routineRoot = new TestTask();
                StoreDepositOperationState routineState =
                        bindings.activateRoot(routineRoot, "AUTO_DEPOSIT_ALL_CHAIN");
                emitter.emitAndPurge(
                        routineRoot,
                        routineState,
                        "NATURAL_FINISH",
                        "NATURAL_FINISH_OBSERVED"
                );
                assertNull(bindings.stateFor(routineRoot));
            }
        });

        Task abnormalRoot = new TestTask();
        StoreDepositOperationState abnormalState =
                bindings.activateRoot(abnormalRoot, "AUTO_DEPOSIT_ALL_CHAIN");
        String abnormalOutput = captureOutput(() -> emitter.emitAndPurge(
                abnormalRoot,
                abnormalState,
                "ROOT_STOP_END",
                "UNKNOWN_STOP"
        ));

        StoreDepositTerminalLedgerSnapshot snapshot = emitter.ledgerSnapshot();
        assertEquals(2, occurrences(routineOutput, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
        assertEquals(1, occurrences(abnormalOutput, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
        assertEquals(9, snapshot.totalTerminalObserved());
        assertEquals(3, snapshot.fullTerminalGroupAdmissionGranted());
        assertEquals(6, snapshot.fullTerminalGroupSuppressedBeforeAdmission());
        assertEquals(3, snapshot.fullTerminalGroupEmissionCompleted());
        assertEquals(0, snapshot.fullTerminalGroupAdmissionPending());
        assertEquals(0, snapshot.fullTerminalGroupEmissionPending());
        assertEquals(8, snapshot.classificationCounts().get(
                lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalClassification.NATURAL_FINISH));
        assertEquals(1, snapshot.classificationCounts().get(
                lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalClassification.UNKNOWN_STOP));
        assertEquals(9, snapshot.scopeCounts().get(
                lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalScope.AUTOMATIC_GENERAL));
        assertTrue(snapshot.settled());
        assertTrue(snapshot.accountingEquationsHold());
        assertNull(bindings.stateFor(abnormalRoot));
    }

    @Test
    void recordsOneCoverageGapForAStateLessTerminalBoundaryWithoutPurgingAnotherOperation() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositTerminalSummaryEmitter emitter = new StoreDepositTerminalSummaryEmitter(bindings, emissionGate);
        Task rootTask = new TestTask();
        bindings.activateRoot(rootTask, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> emitter.emitAndPurge(
                rootTask,
                null,
                "NATURAL_FINISH",
                "NATURAL_FINISH_OBSERVED"
        ));

        assertEquals("", output.trim());
        assertTrue(bindings.stateFor(rootTask) != null);
        assertEquals(0, emitter.ledgerSnapshot().totalTerminalObserved());
        assertEquals(1, emitter.ledgerSnapshot().terminalBoundaryWithoutContextCount());
        assertEquals(1, emitter.ledgerSnapshot().ledgerCoverageGapCount());
    }

    @Test
    void accountsAStableStateWithMissingAncillaryContextAsUnavailableAndSettles() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositTerminalSummaryEmitter emitter = new StoreDepositTerminalSummaryEmitter(
                bindings,
                emissionGate
        );
        StoreDepositOperationState state = new StoreDepositOperationState(null);

        String output = captureOutput(() -> emitter.emitAndPurge(
                null,
                state,
                "ROOT_STOP_END",
                "UNKNOWN_STOP"
        ));

        StoreDepositTerminalLedgerSnapshot snapshot = emitter.ledgerSnapshot();
        assertEquals(1, occurrences(output, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
        assertEquals(1, snapshot.totalTerminalObserved());
        assertEquals(1, snapshot.classificationCounts().get(
                StoreDepositTerminalClassification.UNKNOWN_STOP
        ));
        assertEquals(1, snapshot.scopeCounts().get(StoreDepositTerminalScope.UNAVAILABLE));
        assertEquals(0, snapshot.terminalContextAvailableCount());
        assertEquals(1, snapshot.terminalContextUnavailableCount());
        assertEquals(0, snapshot.fullTerminalGroupAdmissionPending());
        assertEquals(0, snapshot.fullTerminalGroupEmissionPending());
        assertTrue(snapshot.settled());
        assertTrue(snapshot.accountingEquationsHold());
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void assertInOrder(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token);
            assertTrue(current > previous, "Expected event order token after index " + previous + ": " + token);
            previous = current;
        }
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
            return "store-deposit-terminal-summary-emitter-test";
        }
    }
}
