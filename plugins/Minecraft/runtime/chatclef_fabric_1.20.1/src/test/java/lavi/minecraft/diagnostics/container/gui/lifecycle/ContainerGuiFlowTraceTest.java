package lavi.minecraft.diagnostics.container.gui.lifecycle;

import lavi.minecraft.diagnostics.container.gui.support.ContainerGuiTestSnapshots;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Prove one observed flow reports progress and admits one terminal summary only.
class ContainerGuiFlowTraceTest {
    @Test
    void activeFlowRetainsTheProvidedBoundaryActivationId() {
        String activationId = "screen-boundary-activation-41";
        ContainerGuiActiveFlow flow = new ContainerGuiActiveFlow(
                ContainerGuiTestSnapshots.heuristicChestTail(),
                100L,
                activationId
        );

        assertEquals(activationId, flow.emitter().activeActivationIdOrUnavailable());
        assertEquals(activationId, flow.emitter().boundaryActivationId());
        assertEquals(activationId, flow.aggregateSnapshot().diagnosticBoundaryActivationId());
    }

    @Test
    void rootAssignmentMustMatchTheExactFrozenAvailableToken() {
        ContainerGuiFlowTrace unavailable = new ContainerGuiFlowTrace(
                ContainerGuiTestSnapshots.heuristicChestTail(),
                100L
        );
        ContainerGuiFlowTrace captured = new ContainerGuiFlowTrace(
                ContainerGuiTestSnapshots.heuristicChestTailWithRootAssignment("root-assignment-7"),
                100L
        );

        assertFalse(unavailable.rootAssignmentMatches("root-assignment-7"));
        assertFalse(unavailable.rootAssignmentMatches("unavailable"));
        assertFalse(captured.rootAssignmentMatches(null));
        assertFalse(captured.rootAssignmentMatches(""));
        assertFalse(captured.rootAssignmentMatches("none"));
        assertFalse(captured.rootAssignmentMatches("unavailable"));
        assertFalse(captured.rootAssignmentMatches("root-assignment-8"));
        assertTrue(captured.rootAssignmentMatches("root-assignment-7"));
    }

    @Test
    void advancesObservedBoundariesAndMarksTerminalExactlyOnce() {
        ContainerGuiFlowTrace trace = new ContainerGuiFlowTrace(
                ContainerGuiTestSnapshots.heuristicChestTail(),
                100L
        );

        assertEquals("SCREEN_EVENT_BUS_DISPATCH_STARTED", trace.firstUnconfirmedBoundary());
        trace.screenDispatchStarted();
        assertEquals("SCREEN_EVENT_INTAKE", trace.firstUnconfirmedBoundary());
        trace.screenListenerStarted();
        trace.screenListenerStarted();
        trace.screenListenerCompleted();
        trace.screenListenerSkipped();
        trace.screenDispatchCompleted();
        assertEquals("POST_SCREEN_TASK_EVALUATION_ENTRY", trace.firstUnconfirmedBoundary());

        trace.onClientTickHead(101L);
        trace.onClientTickHead(101L);
        trace.onClientTickHead(102L);
        assertEquals(2, trace.postScreenDistinctClientTickOrdinal());

        trace.taskEvaluationStarted(null, null, 102L);
        assertEquals("POST_SCREEN_TASK_RECONCILIATION", trace.firstUnconfirmedBoundary());
        trace.taskReconciled(102L);
        assertEquals("SLOT_ACTION_REQUEST", trace.firstUnconfirmedBoundary());
        trace.slotActionRequested(null, 103L);
        assertEquals("POST_REQUEST_LIVE_HANDLER_CHECK", trace.firstUnconfirmedBoundary());
        trace.localDeltaObserved(104L);
        assertEquals("POST_REQUEST_LIVE_HANDLER_CHECK", trace.firstUnconfirmedBoundary());
        trace.postTickSlotStateChecked(false, 104L);
        assertEquals("POST_REQUEST_LIVE_HANDLER_DELTA_OBSERVED", trace.firstUnconfirmedBoundary());
        trace.postTickSlotStateChecked(true, 105L);
        assertEquals(
                "SERVER_RECONCILIATION_PACKET_NOT_OBSERVED_PROTOCOL_OPTIONAL",
                trace.firstUnconfirmedBoundary()
        );

        assertEquals(1, trace.screenDispatchStartedCount());
        assertEquals(1, trace.screenDispatchCompletedCount());
        assertEquals(2, trace.screenListenerStartedCount());
        assertEquals(1, trace.screenListenerCompletedCount());
        assertEquals(1, trace.screenListenerSkippedCount());
        assertEquals(1, trace.taskEvaluationEntryCount());
        assertEquals(1, trace.taskReconciliationCount());
        assertEquals(1, trace.slotActionRequestCount());
        assertEquals(1, trace.localDeltaCount());
        assertEquals(2, trace.postTickSlotStateCheckCount());
        assertEquals(1, trace.postTickLiveDeltaCount());
        assertEquals(100L, trace.firstObservedGameTick());
        assertEquals(105L, trace.lastObservedGameTick());

        assertTrue(trace.markTerminalEmitted());
        assertFalse(trace.markTerminalEmitted());
    }

    @Test
    void serverReconciliationSeparatesAppliedPacketsFromRelevantDeltaEvidence() {
        ContainerGuiFlowTrace trace = new ContainerGuiFlowTrace(
                ContainerGuiTestSnapshots.heuristicChestTail(),
                100L
        );
        trace.screenDispatchStarted();
        trace.screenListenerStarted();
        trace.screenListenerCompleted();
        trace.screenDispatchCompleted();
        trace.taskEvaluationStarted(null, null, 101L);
        trace.taskReconciled(101L);
        trace.slotActionRequested(null, 102L);
        trace.postTickSlotStateChecked(true, 103L);

        assertEquals(
                "SERVER_RECONCILIATION_PACKET_NOT_OBSERVED_PROTOCOL_OPTIONAL",
                trace.firstUnconfirmedBoundary()
        );
        assertEquals(0, trace.serverReconciliationCount());
        assertEquals(0, trace.serverReconciliationDeltaCount());

        trace.serverReconciliationObserved(false, 104L);
        assertEquals(1, trace.serverReconciliationCount());
        assertEquals(0, trace.serverReconciliationDeltaCount());
        assertEquals(
                "SERVER_RECONCILIATION_APPLIED_NO_RELEVANT_DELTA",
                trace.lastSuccessfulBoundary()
        );
        assertEquals(
                "SERVER_RECONCILIATION_APPLIED_WITHOUT_RELEVANT_DELTA",
                trace.firstUnconfirmedBoundary()
        );

        trace.serverReconciliationObserved(true, 105L);
        assertEquals(2, trace.serverReconciliationCount());
        assertEquals(1, trace.serverReconciliationDeltaCount());
        assertEquals("SERVER_RECONCILIATION_DELTA_OBSERVED", trace.lastSuccessfulBoundary());
        assertEquals("NONE", trace.firstUnconfirmedBoundary());
        assertEquals(105L, trace.lastObservedGameTick());
    }
}
