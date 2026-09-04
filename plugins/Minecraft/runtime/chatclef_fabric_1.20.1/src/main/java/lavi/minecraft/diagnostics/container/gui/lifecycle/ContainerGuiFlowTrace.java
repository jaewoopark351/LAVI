package lavi.minecraft.diagnostics.container.gui.lifecycle;

import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;
import lavi.minecraft.diagnostics.container.gui.slot.ContainerItemCountSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

//20260904_kpopmodder: Own bounded counters for one observed GUI-open flow, never gameplay state.
public final class ContainerGuiFlowTrace {
    private static final int MAX_TRANSFER_BASELINES = 32;

    private final ContainerScreenEventSnapshot source;
    private final long firstObservedGameTick;
    private final Map<String, ContainerItemCountSnapshot> transferBaselines = new LinkedHashMap<>();
    private long lastObservedGameTick;
    private long lastHeadSerial = -1L;
    private int postScreenDistinctClientTickOrdinal;
    private int screenDispatchDroppedCount;
    private int screenNoActiveListenerCount;
    private int screenDispatchStartedCount;
    private int screenDispatchCompletedCount;
    private int screenListenerStartedCount;
    private int screenListenerCompletedCount;
    private int screenListenerSkippedCount;
    private int taskEvaluationEntryCount;
    private int taskReconciliationCount;
    private int slotActionRequestCount;
    private int localDeltaCount;
    private int postTickSlotStateCheckCount;
    private int postTickLiveDeltaCount;
    private int serverReconciliationCount;
    private int serverReconciliationDeltaCount;
    private int slotActionFailureCount;
    private String routeOwnerClass;
    private String routeOwnerIdentity;
    private String lastSuccessfulBoundary = "SCREEN_TAIL_SOURCE";
    private boolean terminalEmitted;

    public ContainerGuiFlowTrace(ContainerScreenEventSnapshot source, long gameTick) {
        this.source = source;
        this.firstObservedGameTick = gameTick;
        this.lastObservedGameTick = gameTick;
        this.routeOwnerClass = source.interaction() == null
                ? "unavailable"
                : source.interaction().routeOwnerClass();
        this.routeOwnerIdentity = source.interaction() == null
                ? "unavailable"
                : source.interaction().routeOwnerIdentity();
    }

    public ContainerScreenEventSnapshot source() {
        return source;
    }

    public void onClientTickHead(long serial) {
        lastObservedGameTick = serial;
        if (lastHeadSerial != serial) {
            lastHeadSerial = serial;
            postScreenDistinctClientTickOrdinal++;
        }
    }

    public void screenDispatchStarted() {
        screenDispatchStartedCount++;
        lastSuccessfulBoundary = "SCREEN_EVENT_DISPATCH_STARTED";
    }

    public void screenDispatchDroppedNoActiveListener() {
        screenDispatchDroppedCount++;
        screenNoActiveListenerCount++;
    }

    public void screenListenerStarted() {
        screenListenerStartedCount++;
        lastSuccessfulBoundary = "SCREEN_EVENT_INTAKE";
    }

    public void screenListenerCompleted() {
        screenListenerCompletedCount++;
        lastSuccessfulBoundary = "SCREEN_EVENT_LISTENER_DECISION";
    }

    public void screenListenerSkipped() {
        screenListenerSkippedCount++;
    }

    public void screenDispatchCompleted() {
        screenDispatchCompletedCount++;
        lastSuccessfulBoundary = "SCREEN_EVENT_BUS_DISPATCH_COMPLETED";
    }

    public void taskEvaluationStarted(String ownerClass, String ownerIdentity, long gameTick) {
        taskEvaluationEntryCount++;
        lastObservedGameTick = gameTick;
        if (ownerClass != null && !"none".equals(ownerClass)) {
            routeOwnerClass = ownerClass;
            routeOwnerIdentity = ownerIdentity;
        }
        lastSuccessfulBoundary = "POST_SCREEN_TASK_EVALUATION_ENTRY";
    }

    public void taskReconciled(long gameTick) {
        taskReconciliationCount++;
        lastObservedGameTick = gameTick;
        lastSuccessfulBoundary = "POST_SCREEN_TASK_RECONCILIATION";
    }

    public void slotActionRequested(ContainerItemCountSnapshot before, long gameTick) {
        slotActionRequestCount++;
        lastObservedGameTick = gameTick;
        rememberTransferBaseline(before);
        lastSuccessfulBoundary = "SLOT_ACTION_REQUEST";
    }

    public void localDeltaObserved(long gameTick) {
        localDeltaCount++;
        lastObservedGameTick = gameTick;
        lastSuccessfulBoundary = "CLIENT_PREDICTED_CONTAINER_DELTA_OBSERVED";
    }

    public void postTickSlotStateChecked(boolean newLiveDeltaEvidence, long gameTick) {
        postTickSlotStateCheckCount++;
        lastObservedGameTick = gameTick;
        if (newLiveDeltaEvidence) {
            postTickLiveDeltaCount++;
            lastSuccessfulBoundary = "POST_REQUEST_LIVE_HANDLER_DELTA_OBSERVED";
        } else if (postTickLiveDeltaCount == 0) {
            lastSuccessfulBoundary = "POST_REQUEST_LIVE_HANDLER_CHECK_NO_DELTA";
        }
    }

    public void slotActionFailed(long gameTick) {
        slotActionFailureCount++;
        lastObservedGameTick = gameTick;
    }

    public void serverReconciliationObserved(boolean stateChangedFromRequest, long gameTick) {
        serverReconciliationCount++;
        if (stateChangedFromRequest) {
            serverReconciliationDeltaCount++;
        }
        lastObservedGameTick = gameTick;
        lastSuccessfulBoundary = stateChangedFromRequest
                ? "SERVER_RECONCILIATION_DELTA_OBSERVED"
                : "SERVER_RECONCILIATION_APPLIED_NO_RELEVANT_DELTA";
    }

    public ContainerItemCountSnapshot transferBaseline(String itemId) {
        return transferBaselines.get(itemId);
    }

    public boolean markTerminalEmitted() {
        if (terminalEmitted) {
            return false;
        }
        terminalEmitted = true;
        return true;
    }

    public int postScreenDistinctClientTickOrdinal() {
        return postScreenDistinctClientTickOrdinal;
    }

    public int screenDispatchStartedCount() {
        return screenDispatchStartedCount;
    }

    public int screenDispatchDroppedCount() {
        return screenDispatchDroppedCount;
    }

    public int screenNoActiveListenerCount() {
        return screenNoActiveListenerCount;
    }

    public int screenDispatchCompletedCount() {
        return screenDispatchCompletedCount;
    }

    public int screenListenerStartedCount() {
        return screenListenerStartedCount;
    }

    public int screenListenerCompletedCount() {
        return screenListenerCompletedCount;
    }

    public int screenListenerSkippedCount() {
        return screenListenerSkippedCount;
    }

    public int taskEvaluationEntryCount() {
        return taskEvaluationEntryCount;
    }

    public int taskReconciliationCount() {
        return taskReconciliationCount;
    }

    public int slotActionRequestCount() {
        return slotActionRequestCount;
    }

    public int localDeltaCount() {
        return localDeltaCount;
    }

    public int postTickSlotStateCheckCount() {
        return postTickSlotStateCheckCount;
    }

    public int postTickLiveDeltaCount() {
        return postTickLiveDeltaCount;
    }

    public int slotActionFailureCount() {
        return slotActionFailureCount;
    }

    public int serverReconciliationCount() {
        return serverReconciliationCount;
    }

    public int serverReconciliationDeltaCount() {
        return serverReconciliationDeltaCount;
    }

    public long firstObservedGameTick() {
        return firstObservedGameTick;
    }

    public long lastObservedGameTick() {
        return lastObservedGameTick;
    }

    public String routeOwnerClass() {
        return routeOwnerClass;
    }

    public String routeOwnerIdentity() {
        return routeOwnerIdentity;
    }

    public String rootAssignmentId() {
        return source.interaction() == null
                ? "unavailable"
                : source.interaction().rootAssignmentId();
    }

    public boolean rootAssignmentMatches(String currentRootAssignmentId) {
        String captured = rootAssignmentId();
        return currentRootAssignmentId != null
                && !currentRootAssignmentId.isBlank()
                && !"none".equals(currentRootAssignmentId)
                && !"unavailable".equals(currentRootAssignmentId)
                && captured.equals(currentRootAssignmentId);
    }

    public String lastSuccessfulBoundary() {
        return lastSuccessfulBoundary;
    }

    public String firstUnconfirmedBoundary() {
        if (screenDispatchStartedCount == 0) return "SCREEN_EVENT_BUS_DISPATCH_STARTED";
        if (screenListenerStartedCount == 0) return "SCREEN_EVENT_INTAKE";
        if (screenListenerCompletedCount == 0) return "SCREEN_EVENT_LISTENER_DECISION";
        if (screenDispatchCompletedCount == 0) return "SCREEN_EVENT_BUS_DISPATCH_COMPLETED";
        if (taskEvaluationEntryCount == 0) return "POST_SCREEN_TASK_EVALUATION_ENTRY";
        if (taskReconciliationCount == 0) return "POST_SCREEN_TASK_RECONCILIATION";
        if (slotActionRequestCount == 0) return "SLOT_ACTION_REQUEST";
        if (serverReconciliationCount > 0) {
            return serverReconciliationDeltaCount > 0
                    ? "NONE"
                    : "SERVER_RECONCILIATION_APPLIED_WITHOUT_RELEVANT_DELTA";
        }
        if (postTickSlotStateCheckCount == 0) return "POST_REQUEST_LIVE_HANDLER_CHECK";
        if (postTickLiveDeltaCount == 0) return "POST_REQUEST_LIVE_HANDLER_DELTA_OBSERVED";
        return "SERVER_RECONCILIATION_PACKET_NOT_OBSERVED_PROTOCOL_OPTIONAL";
    }

    private void rememberTransferBaseline(ContainerItemCountSnapshot before) {
        if (before == null || before.focusItem() == null
                || "unavailable".equals(before.focusItemId())
                || transferBaselines.containsKey(before.focusItemId())) {
            return;
        }
        if (transferBaselines.size() >= MAX_TRANSFER_BASELINES) {
            return;
        }
        transferBaselines.put(before.focusItemId(), before);
    }
}
