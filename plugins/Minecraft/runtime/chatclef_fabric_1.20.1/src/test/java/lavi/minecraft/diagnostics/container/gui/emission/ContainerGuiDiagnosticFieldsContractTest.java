package lavi.minecraft.diagnostics.container.gui.emission;

import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiTerminalFields;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;
import lavi.minecraft.diagnostics.container.gui.support.ContainerGuiTestSnapshots;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Lock single-owner canonical keys and reject fabricated gate or permission state.
class ContainerGuiDiagnosticFieldsContractTest {
    private static final String COMPLETE = "COMPLETE_FOR_BOUNDARY_ACTIVATION";
    private static final List<String> CANONICAL_GAP_PRECEDENCE = List.of(
            "PARTIAL_LIFECYCLE_OBSERVER_CAPACITY",
            "PARTIAL_MODE_TRANSITION",
            "PARTIAL_UNCORRELATED_HUB_EVENT",
            "PARTIAL_LOCAL_DETAIL_CAP",
            "PARTIAL_SHARED_ADMISSION"
    );
    private static final Set<String> CANONICAL_COMPLETENESS = Set.of(
            COMPLETE,
            "PARTIAL_LIFECYCLE_OBSERVER_CAPACITY",
            "PARTIAL_MODE_TRANSITION",
            "PARTIAL_UNCORRELATED_HUB_EVENT",
            "PARTIAL_LOCAL_DETAIL_CAP",
            "PARTIAL_SHARED_ADMISSION"
    );

    @Test
    void detailPayloadHasUniqueKeysAndDeclaresTheAbsentGateHonestly() {
        ContainerScreenEventSnapshot source = ContainerGuiTestSnapshots.heuristicChestTail();
        Object[] envelope = new Object[]{
                "diagnosticSessionId", "session-1",
                "diagnosticBoundaryActivationId", "activation-1",
                "gameTick", 18L,
                "behavior_effect", "none"
        };
        Object[] screen = ContainerGuiDiagnosticFields.screen(
                source,
                "TAIL_SOURCE_PRE_PUBLISH",
                "OBSERVED",
                "NONE"
        );
        Map<String, Object> fields = uniqueFields(ContainerGuiDiagnosticFields.merge(envelope, screen));

        assertAbsentGateState(fields);
        assertEquals("none", fields.get("behavior_effect"));
        assertEquals("BETWEEN_TICKS", String.valueOf(fields.get("clientTickWindowState")));
        assertEquals(false, fields.get("activeClientTickSerialPresent"));
        assertEquals(-1L, fields.get("activeClientTickSerial"));
        assertEquals(false, fields.get("matchingBlockInteractEventObserved"));
        assertEquals(true, fields.get("diagnosticInteractionCandidateObserved"));
        assertEquals(true, fields.get("diagnosticCorrelationCandidateAccepted"));
        assertEquals(false, fields.get("targetScreenAssociationProven"));
        assertEquals(
                "HEURISTIC_RECENT_EXACT_CONTAINER_INTERACTION",
                fields.get("diagnosticCorrelationReason")
        );
        assertFalse(fields.containsKey("diagnosticCorrelationMatched"));
    }

    @Test
    void downstreamPayloadUsesFlowBoundaryAndNotScreenEventStage() {
        ContainerScreenEventSnapshot source = ContainerGuiTestSnapshots.heuristicChestTail();
        Map<String, Object> fields = uniqueFields(ContainerGuiDiagnosticFields.downstream(
                source,
                source.tickWindow(),
                "POST_SCREEN_TASK_EVALUATION_ENTRY",
                "OBSERVED"
        ));

        assertEquals("POST_SCREEN_TASK_EVALUATION_ENTRY", fields.get("containerFlowBoundary"));
        assertFalse(fields.containsKey("screenEventStage"));
    }

    @Test
    void terminalPayloadCountsServerReconciliationWithoutInventingAcknowledgement() {
        ContainerGuiFlowTrace trace = new ContainerGuiFlowTrace(
                ContainerGuiTestSnapshots.heuristicChestTail(),
                17L
        );
        trace.screenDispatchStarted();
        trace.screenListenerStarted();
        trace.screenListenerCompleted();
        trace.screenDispatchCompleted();
        trace.taskEvaluationStarted(null, null, 18L);
        trace.taskReconciled(18L);
        trace.slotActionRequested(null, 18L);
        trace.localDeltaObserved(18L);
        trace.serverReconciliationObserved(false, 19L);
        ContainerGuiDiagnosticAggregateSnapshot aggregate =
                new ContainerGuiDiagnosticAggregateSnapshot(
                        "activation-1",
                        5L,
                        1L,
                        4L,
                        1L,
                        1L,
                        4L,
                        1L
                );
        Object[] envelope = new Object[]{
                "diagnosticSessionId", "session-1",
                "diagnosticBoundaryActivationId", "activation-1",
                "gameTick", 18L,
                "behavior_effect", "none"
        };
        Object[] terminalRequired = ContainerGuiDiagnosticFields.merge(new Object[]{
                "operationId", "unavailable",
                "openAttemptId", "unavailable",
                "correlationId", "unavailable",
                "interactionId", trace.source().interaction().interactionId(),
                "screenOpenEventIdentity", trace.source().screenOpenEventIdentity(),
                "terminalReason", "DIAGNOSTICS_MODE_OFF",
                "lastSuccessfulBoundary", trace.lastSuccessfulBoundary(),
                "firstFailingBoundary", trace.firstUnconfirmedBoundary()
        }, trace.source().interaction().commandContextFields());
        Object[] terminalOptional = ContainerGuiTerminalFields.from(
                trace,
                "DIAGNOSTICS_MODE_OFF",
                aggregate
        );
        Map<String, Object> fields = uniqueFields(ContainerGuiDiagnosticFields.merge(
                ContainerGuiDiagnosticFields.merge(envelope, terminalRequired),
                terminalOptional
        ));

        assertEquals("unavailable", fields.get("operationId"));
        assertEquals("unavailable", fields.get("openAttemptId"));
        assertEquals("unavailable", fields.get("correlationId"));
        assertEquals(1, fields.get("serverReconciliationObservedCount"));
        assertEquals(0, fields.get("serverReconciliationDeltaObservedCount"));
        assertEquals("SERVER_S2C_RECONCILIATION_APPLIED", fields.get("deltaObservationSource"));
        assertEquals(false, fields.get("serverAcknowledgementObserved"));
        assertEquals("none", fields.get("behavior_effect"));
        assertEquals(0, fields.get("screenTailHubDroppedCount"));
        assertEquals(1, fields.get("screenTailHubDispatchStartedCount"));
        assertEquals(1, fields.get("screenTailHubDispatchCompletedCount"));
        assertEquals(0, fields.get("screenTailHubNoActiveListenerCount"));
        assertEquals(0, fields.get("screenTailHubInactiveAfterSnapshotSkipCount"));
        assertEquals(1, fields.get("completedGuiListenerCallbackCount"));
        assertFalse(fields.containsKey("screenTailEventBusDroppedCount"));
        assertFalse(fields.containsKey("screenTailEventBusDispatchStartedCount"));
        assertFalse(fields.containsKey("screenTailEventBusDispatchCompletedCount"));
        assertFalse(fields.containsKey("screenTailEventBusNoActiveListenerCount"));
        assertFalse(fields.containsKey("screenTailEventBusInactiveListenerSkipCount"));
        assertFalse(fields.containsKey("completedScreenEventListenerCallbackCount"));
        assertFalse(fields.containsKey("skippedInactiveScreenEventListenerCount"));
        assertFalse(fields.containsKey("containerFlowEvidenceCompleteness"));
        assertFalse(fields.containsKey("containerFlowEvidenceGapReason"));
        assertEquals("PARTIAL_MODE_TRANSITION", fields.get("diagnosticEvidenceCompleteness"));
        assertEquals(
                "PARTIAL_LOCAL_DETAIL_CAP,PARTIAL_MODE_TRANSITION,"
                        + "PARTIAL_SHARED_ADMISSION,PARTIAL_UNCORRELATED_HUB_EVENT",
                fields.get("diagnosticEvidenceGapReasons")
        );
        assertCanonicalEvidenceCompleteness(fields);
        assertNoFabricatedPermissionOrGateValue(fields);
    }

    private static void assertAbsentGateState(Map<String, Object> fields) {
        assertEquals("unavailable", fields.get("operationId"));
        assertEquals("unavailable", fields.get("openAttemptId"));
        assertEquals("unavailable", fields.get("correlationId"));
        assertEquals("NOT_APPLICABLE", fields.get("gateOutcome"));
        assertEquals("NOT_CALLED", fields.get("coordinatorOutcome"));
        assertEquals(false, fields.get("operationContextAvailable"));
        assertEquals(false, fields.get("activeAttemptPresent"));
        assertEquals(false, fields.get("matchingBlockInteractEventConsumed"));
        assertEquals(false, fields.get("screenTailCandidate"));
        assertNoFabricatedPermissionOrGateValue(fields);
    }

    private static void assertNoFabricatedPermissionOrGateValue(Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey().toUpperCase(Locale.ROOT);
            String value = String.valueOf(entry.getValue()).toUpperCase(Locale.ROOT);
            assertFalse(key.contains("PERMISSION"), entry::toString);
            assertFalse(value.contains("GUI_BOUND"), entry::toString);
            assertFalse(value.contains("GUI_STABILIZING"), entry::toString);
            assertFalse(value.contains("GUI_INPUT_ALLOWED"), entry::toString);
        }
    }

    private static void assertCanonicalEvidenceCompleteness(Map<String, Object> fields) {
        String completeness = String.valueOf(fields.get("diagnosticEvidenceCompleteness"));
        assertTrue(CANONICAL_COMPLETENESS.contains(completeness), completeness);

        String serializedGaps = String.valueOf(fields.get("diagnosticEvidenceGapReasons"));
        List<String> gaps = serializedGaps.isBlank()
                ? List.of()
                : Arrays.asList(serializedGaps.split(","));
        List<String> sortedGaps = new ArrayList<>(gaps);
        sortedGaps.sort(String::compareTo);

        assertEquals(sortedGaps, gaps, "gap reasons must use canonical sorted order");
        assertEquals(gaps.size(), new HashSet<>(gaps).size(), "gap reasons must be an exact set");
        assertTrue(CANONICAL_GAP_PRECEDENCE.containsAll(gaps), gaps::toString);

        String expectedPrimary = gaps.isEmpty()
                ? COMPLETE
                : CANONICAL_GAP_PRECEDENCE.stream()
                        .filter(gaps::contains)
                        .findFirst()
                        .orElseThrow();
        assertEquals(expectedPrimary, completeness);
    }

    private static Map<String, Object> uniqueFields(Object[] pairs) {
        assertEquals(0, pairs.length % 2, "field arrays must contain key/value pairs");
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            String key = String.valueOf(pairs[index]);
            assertTrue(!result.containsKey(key), () -> "duplicate diagnostic key: " + key);
            result.put(key, pairs[index + 1]);
        }
        return result;
    }
}
