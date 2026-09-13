package lavi.minecraft.diagnostics.mining.gold;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.correlation.MiningDiagnosticCorrelation;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.integration.mining.operation.MiningOperationToolState;
import lavi.minecraft.integration.mining.operation.PrepareMiningOperationToolsTask;

//20260913_kpopmodder: Project one parent's gold-tool lifecycle into the bounded observation scope.
public final class GoldToolLoopObserver {
    private ObservationScope scope;
    private GoldToolLoopState state = new GoldToolLoopState();
    private String requestKey;
    private String targetFilters = "NOT_OBSERVED";
    private String accessFilters = "NOT_OBSERVED";
    private long filterCapturedTick = -1;
    private long filterEvaluationSequence;
    private long selectedEquipAttempt = -1;
    private String selectedAccessBlock = "UNAVAILABLE";
    private Object[] latestEquipFields = new Object[]{"equipEvidence", "UNAVAILABLE"};

    public boolean bind(AltoClef mod, Task parent) {
        ObservationActivation activation = ObservationDiagnostics.captureActivation(mod, mod.getWorld());
        // A stop/interrupt can run under the next command context. Keep the existing live owner binding.
        if (scope != null && scope.isCurrent() && scope.activation() == activation) {
            bindUserRoot(mod);
            return true;
        }
        MiningDiagnosticCorrelation correlation = MiningDiagnosticCorrelation.capture(parent);
        String key = correlation.key() + "|" + GoldToolSnapshot.identity(parent);
        if (scope != null && scope.isCurrent() && key.equals(requestKey)) return true;
        if (scope != null && scope.isCurrent()) scope.close("REQUEST_BINDING_CHANGED", state.fields(ChatClefDiagnostics.currentClientTickId()));
        ObservationDiagnostics.retireOtherOperations(activation, "mining", key);
        scope = ObservationDiagnostics.open(activation, "mining", key, correlation.commandContextFields());
        //20260913_kpopmodder: Retain exact user-root assignment before its gold child can leave the task tree.
        bindUserRoot(mod);
        requestKey = key;
        state = new GoldToolLoopState();
        targetFilters = "NOT_OBSERVED";
        accessFilters = "NOT_OBSERVED";
        filterCapturedTick = -1;
        filterEvaluationSequence = 0;
        selectedEquipAttempt = -1;
        selectedAccessBlock = "UNAVAILABLE";
        latestEquipFields = new Object[]{"equipEvidence", "UNAVAILABLE"};
        return scope.isCurrent();
    }

    private void bindUserRoot(AltoClef mod) {
        if (mod.getUserTaskChain() != null) {
            mod.getUserTaskChain().diagnosticGoldRootObservation().bind(
                    mod.getUserTaskChain().getCurrentTask(), mod.getUserTaskChain().diagnosticRootAssignmentId(),
                    scope, () -> state.fields(ChatClefDiagnostics.currentClientTickId()));
        }
    }

    public void filters(String target, String access) {
        targetFilters = target;
        accessFilters = access;
        filterCapturedTick = ChatClefDiagnostics.currentClientTickId();
        filterEvaluationSequence++;
    }

    public void preparation(AltoClef mod, Task parent, MiningOperationToolState actualState, Task selectedChild) {
        long tick = ChatClefDiagnostics.currentClientTickId();
        String step = actualState.nextStep().name();
        String transition = state.preparation(actualState.ready(), step, actualState.targetInventoryCount(), tick);
        if (transition.equals("READY_TO_PREPARATION")) {
            scope.pin("first_equip", latestEquipFields);
            scope.pin("first_preparation_reentry", "tick", tick, "equipAttemptId", state.latestEquipAttempt(),
                    "actualStep", step, "targetFilters", targetFilters, "accessFilters", accessFilters);
            scope.record("GOLD_TOOL_PREPARATION_REENTRY", step, step, false,
                    merge(new Object[]{"actualStep", step, "targetFilters", targetFilters, "accessFilters", accessFilters,
                                    "filterCapturedTick", filterCapturedTick, "filterEvaluationSequence", filterEvaluationSequence},
                            GoldToolSnapshot.preparation(mod, actualState), state.fields(tick)));
        } else if (transition.equals("PREPARATION_TO_READY")) {
            scope.record("GOLD_TOOL_READY_RETURN", "PREPARATION_TO_READY", "PREPARATION_TO_READY", false, state.fields(tick));
        }
        scope.record("GOLD_TOOL_PREPARATION", step, actualState.signature(), false,
                merge(GoldToolSnapshot.preparation(mod, actualState), state.fields(tick),
                        new Object[]{"transition", transition, "parent", GoldToolSnapshot.identity(parent),
                                "selectedChild", GoldToolSnapshot.identity(selectedChild),
                                "targetFilterResults", targetFilters, "accessFilterResults", accessFilters,
                                "filterCapturedTick", filterCapturedTick, "filterEvaluationSequence", filterEvaluationSequence,
                                "filterEvidenceSource", "ACTUAL_POLICY_EVALUATION"}));
        if (state.preparationReentries() > 1) {
            scope.record("GOLD_TOOL_LOOP_SUMMARY", "REPEATED_PREPARATION", "REPEATED_PREPARATION", false, state.fields(tick));
        }
    }

    public void equip(long attempt, String reason, Object... fields) {
        state.equip(attempt, "equipAttempt=" + attempt + ",result=" + reason);
        Object[] binding = new Object[]{"equipAttemptId", attempt,
                "accessBlockTarget", selectedEquipAttempt == attempt ? selectedAccessBlock : "UNAVAILABLE_ATTEMPT_NOT_OBSERVED"};
        latestEquipFields = merge(binding, fields);
        scope.record("GOLD_TOOL_EQUIP", reason, reason, false,
                merge(state.fields(ChatClefDiagnostics.currentClientTickId()), binding, fields));
    }

    public void selection(long attempt, String target, Object... fields) {
        selectedEquipAttempt = attempt;
        selectedAccessBlock = target;
        scope.record("GOLD_TOOL_SELECTION", "EQUIP_REQUEST", "EQUIP_REQUEST", false,
                merge(state.fields(ChatClefDiagnostics.currentClientTickId()),
                        new Object[]{"equipAttemptId", attempt, "accessBlockTarget", target}, fields));
    }

    public void childStarted(Task child, String target) {
        long run = state.childStarted(child, target);
        scope.record("GOLD_DESTROY_LIFECYCLE", "START", target, false,
                merge(state.fields(ChatClefDiagnostics.currentClientTickId()),
                        new Object[]{"child", GoldToolSnapshot.identity(child), "childRun", run, "target", target,
                                "scopeResetByChildStart", false}));
    }

    public void childStopped(Task child, Task interrupter, String target, Object... progress) {
        boolean preparation = interrupter instanceof PrepareMiningOperationToolsTask;
        String reason = preparation ? "PREPARATION_INTERRUPTED_DESTROY" : "OTHER_DESTROY_STOP";
        if (!state.childStopped(child, preparation, "run=" + state.childRuns() + ",target=" + target)) return;
        if (preparation) scope.pin("first_destroy_interruption",
                "childRun", state.childRuns(), "target", target, "interrupter", GoldToolSnapshot.identity(interrupter),
                "equipAttemptId", state.latestEquipAttempt(), "causalLink", state.firstCausalLink());
        scope.record("GOLD_DESTROY_LIFECYCLE", reason, reason + "|" + target, false,
                merge(state.fields(ChatClefDiagnostics.currentClientTickId()), progress,
                        new Object[]{"child", GoldToolSnapshot.identity(child), "interrupter", GoldToolSnapshot.identity(interrupter),
                                "target", target, "childStopIsScopeTerminal", false}));
    }

    public void parentStopped(Task interrupter) {
        if (scope == null || !scope.isCurrent()) return;
        scope.record("GOLD_PARENT_STOP_OBSERVED", "ON_RESOURCE_STOP", GoldToolSnapshot.identity(interrupter), false,
                merge(state.fields(ChatClefDiagnostics.currentClientTickId()),
                        new Object[]{"interrupter", GoldToolSnapshot.identity(interrupter),
                                "scopeTerminal", "NOT_INFERRED_FROM_INTERRUPT_CALLBACK"}));
    }

    public void hotbar(String reason, Object... fields) {
        scope.record("GOLD_TOOL_HOTBAR_MOVE", reason, reason, false,
                merge(state.fields(ChatClefDiagnostics.currentClientTickId()), fields));
    }

    private static Object[] merge(Object[]... arrays) {
        int size = 0;
        for (Object[] array : arrays) size += array.length;
        Object[] result = new Object[size];
        int offset = 0;
        for (Object[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
