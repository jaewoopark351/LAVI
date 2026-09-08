package lavi.minecraft.diagnostics.container.store.deposit.budget;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventText;
import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.field;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep general mode, budget, payload, and bounded-emitter assertions with the budget owner.
class StoreDepositDiagnosticsBoundednessContractTest {

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
    @DisplayName("scenario 15 [assertion 22]: modes, dedupe, rate, payload, session cap, and suppression summary")
    void diagnosticsModesAndAllBoundednessControlsRemainExplicit() throws IOException {
        DiagnosticModeController controller = new DiagnosticModeController(DiagnosticOutputMode.OFF);
        assertTrue(controller.isOff());
        assertFalse(controller.isBoundaryEnabled());
        controller.setBoundaryEnabled(true);
        assertEquals(DiagnosticOutputMode.BOUNDARY, controller.current());
        assertTrue(controller.isBoundaryEnabled());
        assertFalse(controller.isVerboseEnabled());
        DiagnosticModeController verbose = new DiagnosticModeController(DiagnosticOutputMode.VERBOSE);
        assertTrue(verbose.isBoundaryEnabled());
        assertTrue(verbose.isVerboseEnabled());
    }

    @Test
    @DisplayName("scenario 15 [assertion 22]: detail family, policy, and session caps remain bounded")
    void detailFamilyPolicyAndSessionCapsRemainBounded() {
        StoreDepositEmissionGate dedupeAndRate = new StoreDepositEmissionGate();
        assertTrue(dedupeAndRate.shouldEmitDetail("operation", "STORE_DEPOSIT_SLOT_ACTION", "same"));
        assertFalse(dedupeAndRate.shouldEmitDetail("operation", "STORE_DEPOSIT_SLOT_ACTION", "same"));
        for (int index = 1; index < 16; index++) {
            assertTrue(dedupeAndRate.shouldEmitDetail(
                    "operation", "STORE_DEPOSIT_SLOT_ACTION", "distinct-" + index
            ));
        }
        assertFalse(dedupeAndRate.shouldEmitDetail(
                "operation", "STORE_DEPOSIT_SLOT_ACTION", "over-family-cap"
        ));
        assertTrue(dedupeAndRate.shouldEmitDetail(
                "operation", "STORE_DEPOSIT_MOVEMENT_RESULT", "movement-family-remains-available"
        ));
        assertTrue(dedupeAndRate.shouldEmitDetail(
                "operation", "STORE_DEPOSIT_SLOT_MUTATION", "mutation-family-remains-available"
        ));
        assertTrue(String.valueOf(field(
                dedupeAndRate.budgetSummaryFields("operation"),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_DEPOSIT_SLOT_ACTION=2"));

        StoreDepositEmissionGate policyWorstCase = new StoreDepositEmissionGate();
        assertTrue(policyWorstCase.shouldEmitDetail(
                "policy-operation", "AUTO_DEPOSIT_POLICY_SNAPSHOT", "snapshot"
        ));
        for (int index = 0; index < 41; index++) {
            assertTrue(policyWorstCase.shouldEmitDetail(
                    "policy-operation", "AUTO_DEPOSIT_POLICY_ITEM_DECISION", "item-" + index
            ));
        }
        for (int index = 0; index < 41; index++) {
            assertTrue(policyWorstCase.shouldEmitDetail(
                    "policy-operation", "AUTO_DEPOSIT_POLICY_STACK_FACT", "stack-" + index
            ));
        }
        assertTrue(policyWorstCase.shouldEmitDetail(
                "policy-operation", "AUTO_DEPOSIT_POLICY_SNAPSHOT", "bounded-headroom"
        ));
        assertFalse(policyWorstCase.shouldEmitDetail(
                "policy-operation", "AUTO_DEPOSIT_POLICY_SNAPSHOT", "over-family-cap"
        ));

        StoreDepositEmissionGate session = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP; index++) {
            assertTrue(session.shouldEmitDetail("operation-" + index, "SLICE_A_UNCATEGORIZED", "key"));
        }
        assertFalse(session.shouldEmitDetail("operation-over-cap", "SLICE_A_UNCATEGORIZED", "key"));
        assertEquals(StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP, field(
                session.budgetSummaryFields("operation-over-cap"),
                "storeBudgetSessionDetailEmittedCount"
        ));
    }

    @Test
    @DisplayName("scenario 15 [assertion 22]: critical reserve and generic suppression caps remain bounded")
    void criticalReserveAndGenericSuppressionCapsRemainBounded() {
        StoreDepositEmissionGate lateSummaryGate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_LATE_SUMMARIES; index++) {
            assertTrue(lateSummaryGate.shouldEmitLateSummary("automatic-operation-" + index));
        }
        assertFalse(lateSummaryGate.shouldEmitLateSummary("automatic-operation-over-cap"));
        assertTrue(lateSummaryGate.shouldEmitCoverageSuppressionSummary());
        assertFalse(lateSummaryGate.shouldEmitCoverageSuppressionSummary());
        assertEquals(
                StoreDepositBudgetConstants.CRITICAL_RESERVE_CAP,
                StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS
                        * StoreDepositBudgetConstants.TERMINAL_GROUP_EVENT_COUNT
                        + StoreDepositBudgetConstants.MAX_EXCEPTION_SIGNATURES
                        + StoreDepositBudgetConstants.MAX_LATE_SUMMARIES
                        + StoreDepositBudgetConstants.MAX_CONTROL_EVENTS
                        + StoreDepositBudgetConstants.MAX_COVERAGE_SUPPRESSION_SUMMARIES
        );

        StoreDepositEmissionGate terminalExhaustionGate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS; index++) {
            assertTrue(terminalExhaustionGate.reserveTerminalGroup(
                    "reserved-terminal-operation-" + index
            ).reserved());
        }
        for (int index = 0; index < 1_000; index++) {
            assertTrue(terminalExhaustionGate.reserveTerminalGroup(
                    "exhausted-terminal-operation-" + index
            ).exhausted());
        }
        assertEquals(1_000L, field(
                terminalExhaustionGate.budgetSummaryFields("terminal-budget-check"),
                "storeBudgetTerminalReserveExhaustedOperations"
        ));

        StoreDepositEmissionGate saturatedCriticalGate = new StoreDepositEmissionGate();
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_CONTROL_EVENTS; index++) {
            assertTrue(saturatedCriticalGate.shouldEmitControl(
                    "control-operation-" + index,
                    "control-event-" + index
            ));
        }
        assertFalse(saturatedCriticalGate.shouldEmitControl(
                "control-operation-over-cap",
                "control-event-over-cap"
        ));
        for (int index = 0; index < StoreDepositBudgetConstants.MAX_LATE_SUMMARIES; index++) {
            assertTrue(saturatedCriticalGate.shouldEmitLateSummary("late-operation-" + index));
        }
        assertFalse(saturatedCriticalGate.shouldEmitLateSummary("late-operation-over-cap"));
    }

    @Test
    @DisplayName("scenario 15 [assertion 22]: payload formatter and emitter boundaries remain bounded")
    void payloadFormatterAndEmitterBoundariesRemainBounded() throws IOException {
        DiagnosticBoundedEventText encoded = DiagnosticBoundedEventFormatter.format(
                "[SliceA] ",
                new Object[]{"event", "terminal"},
                new Object[]{"terminalReason", "x".repeat(500)},
                new Object[]{"optional", "y".repeat(500)},
                192
        );
        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(encoded.text()) <= 192);
        assertTrue(encoded.partial());

        String boundedLogger = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/budget/StoreDepositBoundedEventLogger.java"
        );
        String transferEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/transfer/StoreDepositTransferDiagnostics.java"
        );
        String movementEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/route/StoreDepositMovementDiagnostics.java"
        );
        String storeFacade = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java"
        );
        String policyEmitter = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/policy/diagnostics/AutoDepositPolicyDiagnostics.java"
        );
        String criticalBudget = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/budget/StoreDepositCriticalBudget.java"
        );
        assertTrue(boundedLogger.contains("ChatClefDiagnostics.logBoundedBoundary"));
        assertTrue(boundedLogger.contains("MAX_EVENT_UTF8_BYTES = 8192"));
        assertFalse(transferEmitter.contains("ChatClefDiagnostics.logBoundary("));
        assertFalse(movementEmitter.contains("ChatClefDiagnostics.logBoundary("));
        assertTrue(transferEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(movementEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(storeFacade.contains("StoreDepositSharedBudget.emissionGate()"));
        assertTrue(policyEmitter.contains("StoreDepositSharedBudget.emissionGate()"));
        assertTrue(policyEmitter.contains("StoreDepositBoundedEventLogger.log("));
        assertTrue(policyEmitter.contains("AUTO_DEPOSIT_POLICY_STACK_FACT"));
        assertTrue(policyEmitter.contains("snapshot.automaticContext().autoOperationId()"));
        assertFalse(criticalBudget.contains("exhaustedTerminalOperations"));
        assertTrue(criticalBudget.contains("long exhaustedTerminalOperationCount"));
    }
}
