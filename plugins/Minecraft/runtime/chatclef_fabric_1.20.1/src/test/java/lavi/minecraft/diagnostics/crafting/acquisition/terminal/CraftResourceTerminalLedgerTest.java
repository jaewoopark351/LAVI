package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Define two-phase immutable terminal aggregation before implementation.
class CraftResourceTerminalLedgerTest {
    @Test
    void detachFreezesCauseButWaitsForMatchingCleanupAndContextUnbind() {
        CraftResourceTerminalLedger ledger = ledger();

        assertFalse(ledger.recordDetach("websocket_error", 10L, 1_000L).summaryRequested());
        assertFalse(ledger.recordOwnedRootCancellation(
                1L, "cancelled_without_task", 11L, 2_000L).summaryRequested());
        assertFalse(ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.NONMATCHING_ROOT,
                "cancelled_without_task",
                false,
                12L,
                3_000L
        ).summaryRequested());
        assertFalse(ledger.recordClassification(
                "task_finished_event_identity_mismatch",
                "unknown",
                "task_identity_mismatch",
                "callback_plus_nonmatching_user_task_event",
                "INCONCLUSIVE_IDENTITY_MISMATCH"
        ).summaryRequested());
        assertFalse(ledger.recordSendOutcome(
                CraftResourceResultSendStatus.NO_SOCKET,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        ).summaryRequested());
        assertFalse(ledger.recordLifecycleCleared(
                CraftResourceLifecycleClearKind.CONNECTION_DETACHED
        ).summaryRequested());

        CraftResourceTerminalDecision finalized = ledger.recordQueueContextCleared(
                true,
                "connection_detached_context_unbound"
        );

        assertTrue(finalized.summaryRequested());
        assertTrue(finalized.finalized());
        CraftResourceTerminalSnapshot snapshot = finalized.snapshot();
        assertEquals(CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL,
                snapshot.primaryTerminationCause());
        assertEquals("cancelled_without_task", snapshot.taskTerminationKind());
        assertEquals("task_finished_event_identity_mismatch",
                snapshot.terminalDecisionReason());
        assertEquals("task_identity_mismatch", snapshot.classifiedResultReason());
        assertEquals("INCONCLUSIVE_IDENTITY_MISMATCH", snapshot.evidenceConclusion());
        assertEquals(CraftResourceResultSendStatus.NO_SOCKET, snapshot.resultSendStatus());
        assertEquals(CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                snapshot.resultDeliveryStatus());
        assertTrue(snapshot.lifecycleCleared());
        assertTrue(snapshot.queueContextCleared());
        assertEquals(CraftResourceFinalizationMode.OBSERVED_BARRIER,
                snapshot.finalizationMode());
        assertEquals(CraftResourceCoverageStatus.COMPLETE, snapshot.coverageStatus());
        assertEquals(1L, snapshot.terminalAdmissionRequestCount());
    }

    @Test
    void laterTimeoutMismatchAndDuplicateSignalsCannotOverwriteOrReemitFrozenDetach() {
        CraftResourceTerminalLedger ledger = ledger();
        ledger.recordDetach("websocket_error", 10L, 1_000L);
        ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.NONMATCHING_ROOT,
                "cancelled_without_task",
                true,
                20L,
                2_000L
        );
        ledger.recordClassification(
                "unknown",
                "task_identity_mismatch",
                "callback_plus_nonmatching_user_task_event",
                "INCONCLUSIVE_IDENTITY_MISMATCH"
        );
        ledger.recordSendOutcome(
                CraftResourceResultSendStatus.NO_SOCKET,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        );
        ledger.recordLifecycleCleared(CraftResourceLifecycleClearKind.CONNECTION_DETACHED);
        CraftResourceTerminalDecision first = ledger.recordQueueContextCleared(
                true,
                "connection_detached_context_unbound"
        );
        CraftResourceTerminalDecision duplicateUnbind = ledger.recordQueueContextCleared(
                true,
                "duplicate_unbind"
        );
        CraftResourceTerminalDecision duplicateDetach = ledger.recordDetach(
                "duplicate_detach", 30L, 3_000L
        );

        assertTrue(first.summaryRequested());
        assertFalse(duplicateUnbind.summaryRequested());
        assertFalse(duplicateDetach.summaryRequested());
        CraftResourceTerminalSnapshot snapshot = ledger.snapshot();
        assertEquals(CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL,
                snapshot.primaryTerminationCause());
        assertTrue(snapshot.thisOrChildTimedOutEverObserved());
        assertEquals(1L, snapshot.terminalAdmissionRequestCount());
        assertEquals(2L, snapshot.lateOrDuplicateSignalCount());
    }

    @Test
    void firstDirectlyObservedPrimaryCauseWinsWithoutInferenceOrOverwrite() {
        CraftResourceTerminalLedger ledger = ledger();

        ledger.recordPrimaryCause(
                CraftResourcePrimaryTerminationCause.COMMAND_EXCEPTION,
                5L,
                500L
        );
        ledger.recordPrimaryCause(
                CraftResourcePrimaryTerminationCause.EXISTING_COMMAND_DEADLINE,
                6L,
                600L
        );
        ledger.recordDetach("later_detach", 7L, 700L);

        assertEquals(
                CraftResourcePrimaryTerminationCause.COMMAND_EXCEPTION,
                ledger.snapshot().primaryTerminationCause()
        );
        assertTrue(ledger.snapshot().connectionDetachedObserved());
    }

    @Test
    void matchingNaturalFinishWaitsForSendQueueAndNormalLifecycleBarrier() {
        CraftResourceTerminalLedger ledger = ledger();

        assertFalse(ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.MATCHING_ROOT,
                "finished",
                false,
                50L,
                5_000L
        ).summaryRequested());
        assertFalse(ledger.recordClassification(
                "completed",
                "matching_task_finished",
                "callback_plus_matching_user_task_event",
                "VERIFIED_MATCHING_ROOT_FINISH"
        ).summaryRequested());
        assertFalse(ledger.recordSendOutcome(
                CraftResourceResultSendStatus.SENT,
                CraftResourceResultDeliveryStatus.DELIVERED,
                true,
                false
        ).summaryRequested());
        assertFalse(ledger.recordQueueContextCleared(
                true,
                "matching_terminal_result_dequeued"
        ).summaryRequested());
        CraftResourceTerminalDecision finalized = ledger.recordLifecycleCleared(
                CraftResourceLifecycleClearKind.NORMAL_TERMINAL_RESULT_SENT
        );

        assertTrue(finalized.summaryRequested());
        assertEquals(CraftResourcePrimaryTerminationCause.NATURAL_TASK_FINISH,
                finalized.snapshot().primaryTerminationCause());
        assertEquals(CraftResourceResultSendStatus.SENT,
                finalized.snapshot().resultSendStatus());
        assertEquals(CraftResourceResultDeliveryStatus.DELIVERED,
                finalized.snapshot().resultDeliveryStatus());
        assertTrue(finalized.snapshot().terminalSent());
        assertEquals(CraftResourceCoverageStatus.COMPLETE,
                finalized.snapshot().coverageStatus());
    }

    @Test
    void failedNonDetachSendRemainsPendingUntilDiagnosticFallbackClosesIt() {
        CraftResourceTerminalLedger ledger = ledger();
        ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.MATCHING_ROOT,
                "finished",
                false,
                1L,
                1L
        );
        CraftResourceTerminalDecision sendFailure = ledger.recordSendOutcome(
                CraftResourceResultSendStatus.FAILED,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        );

        assertFalse(sendFailure.summaryRequested());
        assertFalse(ledger.snapshot().finalized());
        CraftResourceTerminalDecision fallback = ledger.observeRetention(
                201L,
                9_000_000_000L
        );

        assertTrue(fallback.summaryRequested());
        assertEquals(CraftResourceFinalizationMode.DIAGNOSTIC_FALLBACK_INCOMPLETE,
                fallback.snapshot().finalizationMode());
        assertEquals(
                CraftResourceCoverageStatus.INCOMPLETE_TERMINAL_CLEANUP_UNOBSERVED,
                fallback.snapshot().coverageStatus()
        );
        assertEquals(CraftResourcePrimaryTerminationCause.NATURAL_TASK_FINISH,
                fallback.snapshot().primaryTerminationCause());
    }

    @Test
    void retentionClosesAtFirstOfTwoHundredTicksOrTenMonotonicSeconds() {
        CraftResourceTerminalLedger tickLedger = ledger();
        tickLedger.recordDetach("websocket_error", 0L, 0L);
        assertFalse(tickLedger.observeRetention(199L, 9_999_999_999L).summaryRequested());
        CraftResourceTerminalDecision tickFallback = tickLedger.observeRetention(
                200L,
                9_999_999_999L
        );

        CraftResourceTerminalLedger timeLedger = ledger();
        timeLedger.recordDetach("websocket_error", 0L, 0L);
        CraftResourceTerminalDecision timeFallback = timeLedger.observeRetention(
                199L,
                10_000_000_000L
        );

        assertTrue(tickFallback.summaryRequested());
        assertTrue(timeFallback.summaryRequested());
        assertEquals(CraftResourceFinalizationMode.DIAGNOSTIC_FALLBACK_INCOMPLETE,
                tickFallback.snapshot().finalizationMode());
        assertEquals(CraftResourceFinalizationMode.DIAGNOSTIC_FALLBACK_INCOMPLETE,
                timeFallback.snapshot().finalizationMode());
        assertEquals(1L, tickFallback.snapshot().terminalAdmissionRequestCount());
        assertEquals(1L, timeFallback.snapshot().terminalAdmissionRequestCount());
    }

    @Test
    void admissionDenialIsRecordedOnceAndLateSignalsOnlyUpdateBoundedAccounting() {
        CraftResourceTerminalLedger ledger = ledger();
        ledger.recordDetach("websocket_error", 1L, 1L);
        ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.UNAVAILABLE,
                "UNAVAILABLE",
                false,
                1L,
                1L
        );
        ledger.recordClassification("unknown", "detach", "unavailable", "inconclusive");
        ledger.recordSendOutcome(
                CraftResourceResultSendStatus.NOT_ATTEMPTED,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        );
        ledger.recordLifecycleCleared(CraftResourceLifecycleClearKind.CONNECTION_DETACHED);
        CraftResourceTerminalDecision finalized = ledger.recordQueueContextCleared(
                true,
                "connection_detached_context_unbound"
        );
        assertTrue(finalized.summaryRequested());

        assertTrue(ledger.recordAdmissionOutcome(false));
        assertFalse(ledger.recordAdmissionOutcome(false));
        assertFalse(ledger.recordLateSignal("TASK_FINISHED_LATE").summaryRequested());
        assertFalse(ledger.recordLateSignal("SEND_COMPLETION_LATE").summaryRequested());

        CraftResourceTerminalSnapshot snapshot = ledger.snapshot();
        assertEquals(CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL,
                snapshot.primaryTerminationCause());
        assertTrue(snapshot.terminalEmissionAttempted());
        assertFalse(snapshot.terminalEmissionAdmitted());
        assertEquals(1L, snapshot.terminalAdmissionRequestCount());
        assertEquals(2L, snapshot.lateOrDuplicateSignalCount());
    }

    @Test
    void inFlightSendDefersDetachFinalizationUntilSendOutcomeIsKnown() {
        CraftResourceTerminalLedger ledger = ledger();
        ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.NONMATCHING_ROOT,
                "cancelled_without_task",
                false,
                9L,
                900L
        );
        ledger.recordClassification(
                "unknown",
                "task_identity_mismatch",
                "callback_plus_nonmatching_user_task_event",
                "INCONCLUSIVE_IDENTITY_MISMATCH"
        );
        ledger.recordSendOutcome(
                CraftResourceResultSendStatus.IN_FLIGHT,
                CraftResourceResultDeliveryStatus.UNKNOWN,
                false,
                true
        );
        ledger.recordDetach("websocket_error", 10L, 1_000L);
        ledger.recordLifecycleCleared(CraftResourceLifecycleClearKind.CONNECTION_DETACHED);
        assertFalse(ledger.recordQueueContextCleared(
                true,
                "connection_detached_context_unbound"
        ).summaryRequested());

        CraftResourceTerminalDecision sendCompleted = ledger.recordSendOutcome(
                CraftResourceResultSendStatus.NO_SOCKET,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        );

        assertTrue(sendCompleted.summaryRequested());
        assertEquals(CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL,
                sendCompleted.snapshot().primaryTerminationCause());
        assertEquals(CraftResourceResultSendStatus.NO_SOCKET,
                sendCompleted.snapshot().resultSendStatus());
    }

    @Test
    void observedBarrierWaitsForTaskClassificationAndSendAxesIndependently() {
        CraftResourceTerminalLedger missingTask = detachWithCleanup();
        missingTask.recordClassification("unknown", "detach", "unavailable", "inconclusive");
        CraftResourceTerminalDecision missingTaskDecision = missingTask.recordSendOutcome(
                CraftResourceResultSendStatus.NOT_ATTEMPTED,
                CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                false,
                false
        );
        assertFalse(missingTaskDecision.summaryRequested());

        CraftResourceTerminalLedger missingClassification = detachWithCleanup();
        missingClassification.recordTaskFinished(
                CraftResourceTaskFinishMatch.UNAVAILABLE,
                "UNAVAILABLE",
                false,
                2L,
                2L
        );
        CraftResourceTerminalDecision missingClassificationDecision =
                missingClassification.recordSendOutcome(
                        CraftResourceResultSendStatus.NOT_ATTEMPTED,
                        CraftResourceResultDeliveryStatus.NOT_DELIVERED,
                        false,
                        false
                );
        assertFalse(missingClassificationDecision.summaryRequested());

        CraftResourceTerminalLedger missingSend = detachWithCleanup();
        missingSend.recordTaskFinished(
                CraftResourceTaskFinishMatch.UNAVAILABLE,
                "UNAVAILABLE",
                false,
                2L,
                2L
        );
        CraftResourceTerminalDecision missingSendDecision = missingSend.recordClassification(
                "unknown",
                "detach",
                "unavailable",
                "inconclusive"
        );
        assertFalse(missingSendDecision.summaryRequested());
    }

    @Test
    void retainedTerminalTextIsUtf8BoundedWithoutChangingOpaqueIdentity() {
        CraftResourceTerminalLedger ledger = ledger();
        String oversized = "종료원인".repeat(200);

        ledger.recordDetach(oversized, 1L, 1L);
        ledger.recordOwnedRootCancellation(1L, oversized, 1L, 1L);
        ledger.recordTaskFinished(
                CraftResourceTaskFinishMatch.NONMATCHING_ROOT,
                oversized,
                false,
                1L,
                1L
        );
        ledger.recordClassification(
                oversized,
                oversized,
                oversized,
                oversized,
                oversized
        );
        ledger.recordQueueContextCleared(true, oversized);

        CraftResourceTerminalSnapshot snapshot = ledger.snapshot();
        assertUtf8Bounded(snapshot.detachReason());
        assertUtf8Bounded(snapshot.taskTerminationKind());
        assertUtf8Bounded(snapshot.terminalDecisionReason());
        assertUtf8Bounded(snapshot.classifiedResultStatus());
        assertUtf8Bounded(snapshot.classifiedResultReason());
        assertUtf8Bounded(snapshot.classifiedResultFidelity());
        assertUtf8Bounded(snapshot.evidenceConclusion());
        assertUtf8Bounded(snapshot.contextUnbindReason());
    }

    private static CraftResourceTerminalLedger detachWithCleanup() {
        CraftResourceTerminalLedger ledger = ledger();
        ledger.recordDetach("websocket_error", 1L, 1L);
        ledger.recordLifecycleCleared(CraftResourceLifecycleClearKind.CONNECTION_DETACHED);
        ledger.recordQueueContextCleared(true, "connection_detached_context_unbound");
        return ledger;
    }

    private static CraftResourceTerminalLedger ledger() {
        return new CraftResourceTerminalLedger(
                new CraftResourceTerminalKey(
                        "session-1",
                        1L,
                        "request-1",
                        "correlation-1",
                        "user-root-1"
                ),
                0L,
                0L
        );
    }

    private static void assertUtf8Bounded(String value) {
        assertTrue(value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 512);
    }
}
