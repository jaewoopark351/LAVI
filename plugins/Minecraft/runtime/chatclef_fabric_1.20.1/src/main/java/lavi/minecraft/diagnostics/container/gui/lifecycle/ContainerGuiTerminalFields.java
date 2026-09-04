package lavi.minecraft.diagnostics.container.gui.lifecycle;

import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiEvidenceFields;

//20260904_kpopmodder: Format one terminal summary independently of lifecycle decisions.
public final class ContainerGuiTerminalFields {
    private ContainerGuiTerminalFields() {
    }

    public static Object[] from(
            ContainerGuiFlowTrace trace,
            String terminalReason,
            ContainerGuiDiagnosticAggregateSnapshot aggregate) {
        return ContainerGuiDiagnosticFields.merge(
                ContainerGuiDiagnosticFields.merge(new Object[]{
                        "target", trace.source().target(),
                        "targetBlockId", trace.source().targetBlockId(),
                        "targetFamily", trace.source().targetFamily(),
                        "worldIdentity", trace.source().worldIdentity(),
                        "dimension", trace.source().dimension(),
                        "routeOwnerClass", trace.routeOwnerClass(),
                        "routeOwnerIdentity", trace.routeOwnerIdentity(),
                        "firstObservedGameTick", trace.firstObservedGameTick(),
                        "lastObservedGameTick", trace.lastObservedGameTick(),
                        "diagnosticInteractionCandidateCount",
                        trace.source().interaction() == null ? 0 : 1,
                        "screenTailSourceObservedCount", 1,
                        "screenTailHubDroppedCount", trace.screenDispatchDroppedCount(),
                        "screenTailHubDispatchStartedCount", trace.screenDispatchStartedCount(),
                        "screenTailHubDispatchCompletedCount", trace.screenDispatchCompletedCount(),
                        "screenTailHubNoActiveListenerCount", trace.screenNoActiveListenerCount(),
                        "screenTailHubInactiveAfterSnapshotSkipCount", trace.screenListenerSkippedCount(),
                        "screenEventListenerStartedCount", trace.screenListenerStartedCount(),
                        "completedGuiListenerCallbackCount", trace.screenListenerCompletedCount(),
                        "postScreenTaskEvaluationCount", trace.taskEvaluationEntryCount(),
                        "postScreenTaskReconciliationCount", trace.taskReconciliationCount(),
                        "slotActionRequestCount", trace.slotActionRequestCount(),
                        "clientPredictedContainerDeltaObservedCount", trace.localDeltaCount(),
                        "postRequestLiveHandlerCheckCount", trace.postTickSlotStateCheckCount(),
                        "containerDeltaObservedCount", trace.postTickLiveDeltaCount(),
                        "serverReconciliationObservedCount", trace.serverReconciliationCount(),
                        "serverReconciliationDeltaObservedCount",
                        trace.serverReconciliationDeltaCount(),
                        "slotActionFailureCount", trace.slotActionFailureCount(),
                        "deltaObservationSource", trace.serverReconciliationCount() > 0
                                ? "SERVER_S2C_RECONCILIATION_APPLIED"
                                : trace.postTickLiveDeltaCount() > 0
                                ? "POST_REQUEST_CLIENT_TICK_LIVE_HANDLER"
                                : "CLIENT_PREDICTED_SCREEN_HANDLER",
                        "serverAcknowledgementObserved", false
                }, ContainerGuiEvidenceFields.from(
                        aggregate,
                        true,
                        "DIAGNOSTICS_MODE_OFF".equals(terminalReason)
                )),
                ContainerGuiDiagnosticFields.aggregate(aggregate)
        );
    }
}
