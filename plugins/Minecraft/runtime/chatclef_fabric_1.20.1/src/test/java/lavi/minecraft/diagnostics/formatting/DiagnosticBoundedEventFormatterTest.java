package lavi.minecraft.diagnostics.formatting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class DiagnosticBoundedEventFormatterTest {
    @Test
    void preservesRequiredFieldsAndDropsOptionalFieldsUnderTheUtf8Cap() {
        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "ALTO CLEF: [test] ",
                new Object[]{"event", "STORE_HOME_MANIFEST_STALE"},
                new Object[]{
                        "operationId", "store-home-42",
                        "diagnosticCaptureStatus", "complete"
                },
                new Object[]{"optional", "x".repeat(4000)},
                256
        );

        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 256);
        assertTrue(result.text().contains("operationId=store-home-42"));
        assertTrue(result.text().contains("diagnosticCaptureStatus=partial"));
        assertTrue(result.omittedOptionalFieldCount() > 0);
        assertTrue(result.partial());
    }

    @Test
    void leavesSmallCompleteEventsUnchanged() {
        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "prefix ",
                new Object[]{"event", "event-name"},
                new Object[]{"diagnosticCaptureStatus", "complete"},
                new Object[]{"slot", 8},
                8192
        );

        assertFalse(result.partial());
        assertEquals(0, result.omittedOptionalFieldCount());
        assertTrue(result.text().contains("diagnosticCaptureStatus=complete"));
        assertTrue(result.text().contains("slot=8"));
    }

    @Test
    void percentEncodesWhitespaceEqualsPercentAndUnicodeInsideValues() {
        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "prefix ",
                new Object[]{"threadName", "Render thread"},
                new Object[]{"detail", "a=b 100% 한글"},
                new Object[0],
                8192
        );

        assertTrue(result.text().contains("threadName=Render%20thread"));
        assertTrue(result.text().contains("detail=a%3Db%20100%25%20%ED%95%9C%EA%B8%80"));
        assertFalse(result.text().contains("threadName=Render thread"));
    }

    @Test
    void usesAnUnambiguousTokenForEmptyValuesAndEscapesLiteralTildes() {
        assertEquals("~EMPTY~", DiagnosticFieldValueEncoder.encode(""));
        assertEquals("%7EEMPTY%7E", DiagnosticFieldValueEncoder.encode("~EMPTY~"));
    }

    @Test
    void keepsAllRequiredKeysWhenLargeValuesNeedFurtherBounding() {
        Object[] required = new Object[122];
        for (int index = 0; index < 60; index++) {
            required[index * 2] = "requiredField" + index;
            required[index * 2 + 1] = "v".repeat(4000);
        }
        required[120] = "diagnosticCaptureStatus";
        required[121] = "complete";

        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "prefix ",
                new Object[]{"event", "STORE_HOME_MANIFEST_STALE"},
                required,
                new Object[0],
                8192
        );

        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 8192);
        assertTrue(result.text().contains("requiredField0="));
        assertTrue(result.text().contains("requiredField59="));
        assertTrue(result.text().contains("diagnosticCaptureStatus=partial"));
        assertFalse(result.text().contains("boundedPayloadUnavailable=true"));
    }

    @Test
    void keepsTheCraftResourceTerminalReconstructionKeySetUnderThePhysicalCap() {
        List<String> keys = List.of(
                "commandRequestId", "commandCorrelationId", "commandSessionId",
                "commandConnectionGeneration", "rootAssignmentId", "requestedItem",
                "requestedCount", "currentItemCountAtStart", "currentItemCountAtTerminal",
                "targetItemCount", "naturalTaskFinished",
                "thisOrChildTimedOutAtFinalization", "thisOrChildTimedOutEverObserved",
                "firstTimedOutObservationTick", "lastTimedOutObservationTick",
                "cancelInvocationId", "connectionDetached", "detachReason",
                "primaryTerminationCause", "taskTerminationKind", "terminalDecisionReason",
                "classifiedResultStatus", "classifiedResultReason",
                "classifiedResultFidelity", "evidenceConclusion", "terminalSent",
                "resultSendStatus", "resultDeliveryStatus", "lifecycleCleared",
                "queueContextCleared", "contextUnbindReason", "finalizationBoundary",
                "finalizationMode", "terminalEmissionAttempted", "terminalEmissionAdmitted",
                "elapsedTicks", "elapsedMs", "requirementTransitionCount",
                "targetAttemptCount", "boundedTargetHistory", "unreachableRequestCount",
                "blacklistTransitionCount", "chainOwnerTransitionCount",
                "commandDescendantObservationCount", "unownedObservationCount",
                "unknownAssociationCount", "blockOptionalMetaExceptionCount",
                "blockOptionalMetaCoverageGapCount", "lastSuccessfulBoundary",
                "firstExplicitFailureBoundary", "firstUnobservedBoundaryAfter",
                "suppressedDetailCount", "coverageStatus"
        );
        Set<String> exactIdentityKeys = Set.of(
                "commandRequestId",
                "commandCorrelationId",
                "commandSessionId",
                "rootAssignmentId",
                "boundRootTaskInstanceId"
        );
        ArrayList<Object> required = new ArrayList<>();
        for (String key : keys) {
            required.add(key);
            required.add(exactIdentityKeys.contains(key)
                    ? "validated-opaque-identity-" + key
                    : "v".repeat(4000));
        }
        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "ALTO CLEF: [LAVI ChatClefBoundary] ",
                new Object[]{"event", "CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY"},
                required.toArray(),
                new Object[]{"optional", "o".repeat(16000)},
                8192
        );

        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 8192);
        for (String key : keys) {
            assertTrue(result.text().contains(key + "="), key);
        }
        assertFalse(result.text().contains("optional="));
        assertFalse(result.text().contains("boundedPayloadUnavailable=true"));
    }

    @Test
    void preservesValidatedOpaqueIdentityValuesExactlyWhileBoundingOtherFields() {
        List<String> exactKeys = List.of(
                "commandRequestId",
                "commandCorrelationId",
                "commandSessionId",
                "rootAssignmentId",
                "boundRootTaskInstanceId"
        );
        String opaqueId = "=".repeat(360);
        ArrayList<Object> required = new ArrayList<>();
        for (String key : terminalRequiredKeys()) {
            required.add(key);
            required.add(exactKeys.contains(key) ? opaqueId : "v".repeat(4000));
        }

        DiagnosticBoundedEventText result = DiagnosticBoundedEventFormatter.format(
                "ALTO CLEF: [LAVI ChatClefBoundary] ",
                new Object[]{"event", "CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY"},
                required.toArray(),
                new Object[]{"optional", "o".repeat(16000)},
                8192
        );

        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 8192);
        String encodedOpaqueId = DiagnosticFieldValueEncoder.encode(opaqueId);
        for (String key : exactKeys) {
            assertTrue(result.text().contains(key + "=" + encodedOpaqueId), key);
        }
        for (String key : terminalRequiredKeys()) {
            assertTrue(result.text().contains(key + "="), key);
        }
        assertTrue(result.text().contains("diagnosticCaptureStatus=partial"));
        assertFalse(result.text().contains("boundedPayloadUnavailable=true"));
    }

    private static List<String> terminalRequiredKeys() {
        return List.of(
                "allowedFailures", "behavior_effect", "blacklistTransitionCount",
                "blockOptionalMetaCoverageGapCount", "blockOptionalMetaExceptionCount",
                "boundedTargetHistory", "boundRootTaskInstanceId", "cancelInvocationId",
                "chainOwnerTransitionCount", "classificationObserved",
                "classifiedResultFidelity", "classifiedResultReason",
                "classifiedResultStatus", "commandConnectionGeneration",
                "commandCorrelationId", "commandDescendantObservationCount",
                "commandMismatchAdmissionDeniedCount", "commandMismatchCounterSaturated",
                "commandMismatchCoverageGapCount", "commandMismatchDuplicateSuppressedCount",
                "commandMismatchEmissionFailureCount",
                "commandMismatchPerCorrelationLimitSuppressedCount",
                "commandMismatchRetainedSignatureCount",
                "commandMismatchSessionLimitSuppressedCount",
                "commandMismatchUnknownAssociationCount",
                "commandMismatchUnownedObservationCount", "commandOwnedMismatchOccurrenceCount",
                "commandRequestId", "commandSessionId", "connectionDetached",
                "contextUnbindReason", "coverageStatus", "currentItemCountAtStart",
                "currentItemCountAtTerminal", "detachReason", "diagnosticCaptureStatus",
                "elapsedMs", "elapsedTicks", "evidenceConclusion", "expectedBlockIds",
                "failureResourceStage", "failureTargetAttemptSequence",
                "failureTargetPosition", "failureTargetRole", "finalizationBoundary",
                "finalizationMode", "firstExplicitFailureBoundary", "firstFailureCount",
                "firstObservedTick", "firstTimedOutObservationTick",
                "firstUnobservedBoundaryAfter", "lastFailureCount", "lastObservedTick",
                "lastSuccessfulBoundary", "lastTimedOutObservationTick", "lifecycleCleared",
                "mismatchAggregateScope", "naturalTaskFinished", "ownerTaskClass",
                "primaryTerminationCause", "queueContextCleared", "requestedCount",
                "requestedItem", "requirementTransitionCount", "resourceStage",
                "resultDeliveryStatus", "resultSendStatus", "rootAssignmentId",
                "rootGeneration", "rootTaskClass", "sendOutcomeObserved", "sourceEventName",
                "sourceEventSequence", "suppressedDetailCount", "targetAttemptCount",
                "targetAttemptSequence", "targetAttributionSource", "targetClosureKind",
                "targetItemCount", "targetPosition", "targetRole", "taskFinishObserved",
                "taskTerminationKind", "terminalDecisionReason",
                "terminalEmissionAdmissionStatus", "terminalEmissionAdmitted",
                "terminalEmissionAttempted", "terminalEmissionCompleted", "terminalSent",
                "terminalSummaryRequestReason", "thisOrChildTimedOutAtFinalization",
                "thisOrChildTimedOutEverObserved", "unknownAssociationCount",
                "unownedObservationCount", "unreachableAfter", "unreachableBefore",
                "unreachableRequestCount"
        );
    }
}
