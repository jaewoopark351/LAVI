package lavi.minecraft.diagnostics.container.store.deposit.context;

import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StoreDepositOperationState {
    private static final int MAX_BOUND_TASKS_PER_OPERATION = 512;
    private static final int MAX_TRACKER_BINDINGS_PER_OPERATION = 128;

    private final StoreDepositOperationContext context;
    private final long startNanos;
    private ItemTarget[] requestedTargets = new ItemTarget[0];
    private int activationCount;
    private int interruptCount;
    private int resumeCount;
    private int lifecycleEventCount;
    private int childReconciliationCount;
    private int parentCandidateDecisionCount;
    private int filteredSearchResultCount;
    private int pursuitDecisionCount;
    private int targetCallbackDecisionCount;
    private int craftRouteEventCount;
    private int transferDecisionCount;
    private int effectObservationCount;
    private int expectedPositiveEffectCount;
    private int exceptionObservationCount;
    private int boundTaskCount = 1;
    private int droppedTaskBindingCount;
    private int trackerBindingCount;
    private int droppedTrackerBindingCount;
    private int evictedOperationCount;
    private boolean explicitStopCorrelated;
    private boolean trueStopObserved;
    private boolean naturalFinishObserved;
    private boolean terminalFinalized;
    private String lastLifecycleAction = "none";
    private String lastLifecyclePhase = "none";
    private String lastActiveChildIdentity = "none";
    private String lastActiveChildClass = "none";
    private final Map<String, Integer> lifecycleCounts = new LinkedHashMap<>();
    private final Map<String, Integer> reconciliationCounts = new LinkedHashMap<>();
    private final Map<String, Integer> parentCandidateCounts = new LinkedHashMap<>();
    private final Map<String, Integer> filteredSearchCounts = new LinkedHashMap<>();
    private final Map<String, Integer> pursuitCounts = new LinkedHashMap<>();
    private final Map<String, Integer> targetCallbackCounts = new LinkedHashMap<>();
    private final Map<String, Integer> craftRouteCounts = new LinkedHashMap<>();
    private final Map<String, Integer> transferCounts = new LinkedHashMap<>();
    private final Map<String, Integer> effectCounts = new LinkedHashMap<>();
    private final Map<String, Integer> exceptionCounts = new LinkedHashMap<>();

    public StoreDepositOperationState(StoreDepositOperationContext context) {
        this.context = context;
        this.startNanos = System.nanoTime();
    }

    public StoreDepositOperationContext context() {
        return context;
    }

    public long elapsedMillis() {
        long elapsed = System.nanoTime() - startNanos;
        return elapsed < 0 ? 0 : elapsed / 1_000_000L;
    }

    public void recordRequestedTargets(ItemTarget[] targets) {
        requestedTargets = targets == null ? new ItemTarget[0] : Arrays.copyOf(targets, targets.length);
    }

    public boolean matchesRequestedItem(Item item) {
        if (item == null) {
            return false;
        }
        for (ItemTarget target : requestedTargets) {
            if (target != null && target.matches(item)) {
                return true;
            }
        }
        return false;
    }

    public void recordActivation(boolean resume) {
        activationCount++;
        if (resume) {
            resumeCount++;
        }
    }

    public void recordLifecycle(String action, String phase, boolean rootStop) {
        lifecycleEventCount++;
        lastLifecycleAction = action;
        lastLifecyclePhase = phase;
        increment(lifecycleCounts, action + ":" + phase);
        if ("INTERRUPT".equals(action) && "BEGIN".equals(phase)) {
            interruptCount++;
        }
        if (rootStop && "STOP".equals(action) && "BEGIN".equals(phase)) {
            trueStopObserved = true;
        }
    }

    public void recordNaturalFinish() {
        naturalFinishObserved = true;
        increment(lifecycleCounts, "NATURAL_FINISH:OBSERVED");
    }

    public void recordExplicitStopCorrelation() {
        explicitStopCorrelated = true;
    }

    public void recordChildReconciliation(String outcome, Object activeChild) {
        childReconciliationCount++;
        lastActiveChildIdentity = StoreDepositOperationContext.identity(activeChild);
        lastActiveChildClass = activeChild == null ? "none" : activeChild.getClass().getName();
        increment(reconciliationCounts, outcome);
    }

    public void recordParentCandidateDecision(String branch) {
        parentCandidateDecisionCount++;
        increment(parentCandidateCounts, branch);
    }

    public void recordFilteredSearchResult(String relation) {
        filteredSearchResultCount++;
        increment(filteredSearchCounts, relation);
    }

    public void recordPursuitDecision(String decision) {
        pursuitDecisionCount++;
        increment(pursuitCounts, decision);
    }

    public void recordTargetCallbackDecision(String outcome) {
        targetCallbackDecisionCount++;
        increment(targetCallbackCounts, outcome);
    }

    public void recordCraftRouteEvent(String eventName, String reason) {
        craftRouteEventCount++;
        increment(craftRouteCounts, eventName + ":" + reason);
    }

    public void recordTransferDecision(String action) {
        transferDecisionCount++;
        increment(transferCounts, action);
    }

    public void recordEffectObservation(String outcome, boolean expectedPositiveEffect) {
        effectObservationCount++;
        if (expectedPositiveEffect) {
            expectedPositiveEffectCount++;
        }
        increment(effectCounts, outcome);
    }

    public void recordExceptionObservation(String signature) {
        exceptionObservationCount++;
        increment(exceptionCounts, signature);
    }

    public boolean tryRecordTaskBinding() {
        if (boundTaskCount >= MAX_BOUND_TASKS_PER_OPERATION) {
            droppedTaskBindingCount++;
            return false;
        }
        boundTaskCount++;
        return true;
    }

    public boolean tryRecordTrackerBinding() {
        if (trackerBindingCount >= MAX_TRACKER_BINDINGS_PER_OPERATION) {
            droppedTrackerBindingCount++;
            return false;
        }
        trackerBindingCount++;
        return true;
    }

    public void recordOperationEvicted() {
        evictedOperationCount++;
    }

    public boolean markTerminalFinalized() {
        if (terminalFinalized) {
            return false;
        }
        terminalFinalized = true;
        return true;
    }

    public int activationCount() {
        return activationCount;
    }

    public int interruptCount() {
        return interruptCount;
    }

    public int resumeCount() {
        return resumeCount;
    }

    public int lifecycleEventCount() {
        return lifecycleEventCount;
    }

    public int childReconciliationCount() {
        return childReconciliationCount;
    }

    public int parentCandidateDecisionCount() {
        return parentCandidateDecisionCount;
    }

    public int filteredSearchResultCount() {
        return filteredSearchResultCount;
    }

    public int pursuitDecisionCount() {
        return pursuitDecisionCount;
    }

    public int targetCallbackDecisionCount() {
        return targetCallbackDecisionCount;
    }

    public int craftRouteEventCount() {
        return craftRouteEventCount;
    }

    public int transferDecisionCount() {
        return transferDecisionCount;
    }

    public int effectObservationCount() {
        return effectObservationCount;
    }

    public int expectedPositiveEffectCount() {
        return expectedPositiveEffectCount;
    }

    public int exceptionObservationCount() {
        return exceptionObservationCount;
    }

    public int boundTaskCount() {
        return boundTaskCount;
    }

    public int droppedTaskBindingCount() {
        return droppedTaskBindingCount;
    }

    public int trackerBindingCount() {
        return trackerBindingCount;
    }

    public int droppedTrackerBindingCount() {
        return droppedTrackerBindingCount;
    }

    public int evictedOperationCount() {
        return evictedOperationCount;
    }

    public boolean explicitStopCorrelated() {
        return explicitStopCorrelated;
    }

    public boolean trueStopObserved() {
        return trueStopObserved;
    }

    public boolean naturalFinishObserved() {
        return naturalFinishObserved;
    }

    public boolean terminalFinalized() {
        return terminalFinalized;
    }

    public String lastLifecycleAction() {
        return lastLifecycleAction;
    }

    public String lastLifecyclePhase() {
        return lastLifecyclePhase;
    }

    public String lastActiveChildIdentity() {
        return lastActiveChildIdentity;
    }

    public String lastActiveChildClass() {
        return lastActiveChildClass;
    }

    public String lifecycleCounts() {
        return lifecycleCounts.toString();
    }

    public String reconciliationCounts() {
        return reconciliationCounts.toString();
    }

    public String parentCandidateCounts() {
        return parentCandidateCounts.toString();
    }

    public String filteredSearchCounts() {
        return filteredSearchCounts.toString();
    }

    public String pursuitCounts() {
        return pursuitCounts.toString();
    }

    public String targetCallbackCounts() {
        return targetCallbackCounts.toString();
    }

    public String craftRouteCounts() {
        return craftRouteCounts.toString();
    }

    public String transferCounts() {
        return transferCounts.toString();
    }

    public String effectCounts() {
        return effectCounts.toString();
    }

    public String exceptionCounts() {
        return exceptionCounts.toString();
    }

    private static void increment(Map<String, Integer> counts, String key) {
        String normalized = key == null ? "unknown" : key;
        counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
    }
}
