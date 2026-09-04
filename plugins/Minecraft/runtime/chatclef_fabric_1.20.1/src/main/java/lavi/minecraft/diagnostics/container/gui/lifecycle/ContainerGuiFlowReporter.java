package lavi.minecraft.diagnostics.container.gui.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiSemanticFingerprint;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;

//20260904_kpopmodder: Format flow heartbeat, terminal, and clean-snapshot evidence separately.
public final class ContainerGuiFlowReporter {
    public void emitHeartbeat(
            ContainerGuiActiveFlow active,
            ContainerClientTickWindowSnapshot tick,
            long gameTick) {
        if (active == null) {
            return;
        }
        ContainerGuiFlowTrace flow = active.trace();
        active.emitter().detail(
                "CONTAINER_GUI_FLOW_HEARTBEAT",
                "active_container_gui_flow_progress_snapshot",
                ContainerGuiSemanticFingerprint.of(
                        "FLOW_HEARTBEAT",
                        flow.source().targetFamily(),
                        flow.lastSuccessfulBoundary(),
                        flow.firstUnconfirmedBoundary()
                ),
                gameTick,
                ChatClefDiagnostics.currentTaskForDiagnostics(),
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tick,
                        "POST_SCREEN_FLOW_HEARTBEAT",
                        flow.firstUnconfirmedBoundary()
                ),
                new Object[]{
                        "postScreenDistinctClientTickOrdinal", flow.postScreenDistinctClientTickOrdinal(),
                        "lastSuccessfulBoundary", flow.lastSuccessfulBoundary(),
                        "firstFailingBoundary", flow.firstUnconfirmedBoundary(),
                        "postScreenTaskEvaluationCount", flow.taskEvaluationEntryCount(),
                        "postScreenTaskReconciliationCount", flow.taskReconciliationCount(),
                        "slotActionRequestCount", flow.slotActionRequestCount(),
                        "clientPredictedContainerDeltaObservedCount", flow.localDeltaCount(),
                        "postRequestLiveHandlerCheckCount", flow.postTickSlotStateCheckCount(),
                        "containerDeltaObservedCount", flow.postTickLiveDeltaCount()
                }
        );
    }

    public void emitTerminal(
            ContainerGuiActiveFlow active,
            String terminalReason,
            Task task) {
        if (active == null || !active.trace().markTerminalEmitted()) {
            return;
        }
        ContainerGuiFlowTrace flow = active.trace();
        Object[] interactionFields = flow.source().interaction() == null
                ? new Object[]{
                        "interactionId", "unavailable",
                        "commandRequestId", "unavailable",
                        "commandCorrelationId", "unavailable",
                        "commandSessionId", "unavailable",
                        "commandContextAvailable", false
                }
                : ContainerGuiDiagnosticFields.merge(
                        new Object[]{
                                "interactionId", flow.source().interaction().interactionId(),
                                "rootAssignmentId", flow.rootAssignmentId()
                        },
                        flow.source().interaction().commandContextFields()
                );
        active.emitter().terminal(
                "CONTAINER_GUI_FLOW_TERMINAL_SUMMARY",
                "container_gui_flow_terminal_summary",
                ChatClefDiagnostics.currentClientTickId(),
                task,
                ContainerGuiDiagnosticFields.merge(new Object[]{
                        "operationId", "unavailable",
                        "openAttemptId", "unavailable",
                        "correlationId", "unavailable",
                        "screenOpenEventIdentity", flow.source().screenOpenEventIdentity(),
                        "containerFlowBoundary", "TERMINAL",
                        "decisionReason", terminalReason,
                        "terminalReason", terminalReason,
                        "lastSuccessfulBoundary", flow.lastSuccessfulBoundary(),
                        "firstFailingBoundary", flow.firstUnconfirmedBoundary(),
                        "containerFlowBudgetScope", "LOCAL_DETAIL_EXEMPT_TERMINAL"
                }, interactionFields),
                ContainerGuiTerminalFields.from(
                        flow,
                        terminalReason,
                        active.aggregateSnapshot()
                )
        );
    }

}
