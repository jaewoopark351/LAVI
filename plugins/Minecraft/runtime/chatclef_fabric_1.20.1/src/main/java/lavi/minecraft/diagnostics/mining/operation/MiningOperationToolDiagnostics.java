package lavi.minecraft.diagnostics.mining.operation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.mining.operation.MiningOperationToolState;
import lavi.minecraft.integration.mining.operation.MiningToolCandidate;
import lavi.minecraft.integration.mining.operation.MiningToolRole;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class MiningOperationToolDiagnostics {
    private static final int MAX_STATE_KEY_LENGTH = 240;
    private static final Map<Task, Map<String, String>> LAST_STATE_BY_TASK =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MiningOperationToolDiagnostics() {
    }

    public static void logDecision(Task task, String reason, MiningOperationToolState state, Object... fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        if (!markStateChanged(task, reason, state.signature())) {
            return;
        }
        ChatClefDiagnostics.logBoundary(
                "MINING_OPERATION_TOOL_PREPARATION_DECISION",
                reason,
                task,
                mergeFields(stateFields(state), fields)
        );
    }

    public static void logHotbarMove(Task task,
                                     String reason,
                                     MiningToolRole role,
                                     MiningOperationToolState state,
                                     MiningToolCandidate source,
                                     int destinationHotbarSlot) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ChatClefDiagnostics.logBoundary(
                "MINING_OPERATION_TOOL_HOTBAR_MOVE",
                reason,
                task,
                mergeFields(
                        stateFields(state),
                        new Object[]{
                                "toolRole", role,
                                "sourceSlot", source == null ? "none" : ChatClefDiagnostics.slotSummary(source.slot()),
                                "sourceStack", source == null ? "none" : ChatClefDiagnostics.itemStackSummary(source.stack()),
                                "sourceRemainingDurability", source == null ? "none" : source.remainingDurability(),
                                "destinationHotbarSlot", destinationHotbarSlot
                        }
                )
        );
    }

    private static Object[] stateFields(MiningOperationToolState state) {
        return new Object[]{
                "operationType", state.operationType(),
                "targetCount", state.targetCount(),
                "targetInventoryCount", state.targetInventoryCount(),
                "targetDurabilityReserve", state.targetDurabilityReserve(),
                "targetToolReady", state.targetToolReady(),
                "targetToolHotbarVisible", state.targetToolHotbarVisible(),
                "targetToolSlot", slotSummary(state.targetToolCandidate()),
                "targetToolStack", stackSummary(state.targetToolCandidate()),
                "targetToolRemainingDurability", remainingDurability(state.targetToolCandidate()),
                "accessToolReady", state.accessToolReady(),
                "accessToolHotbarVisible", state.accessToolHotbarVisible(),
                "accessToolSlot", slotSummary(state.accessToolCandidate()),
                "accessToolStack", stackSummary(state.accessToolCandidate()),
                "accessToolRemainingDurability", remainingDurability(state.accessToolCandidate()),
                "nextPreparationStep", state.nextStep(),
                "requiredIronPickaxeCount", state.requiredIronPickaxeCount(),
                "requiredStonePickaxeCount", state.requiredStonePickaxeCount()
        };
    }

    private static String slotSummary(Optional<MiningToolCandidate> candidate) {
        return candidate.map(value -> ChatClefDiagnostics.slotSummary(value.slot())).orElse("none");
    }

    private static String stackSummary(Optional<MiningToolCandidate> candidate) {
        return candidate.map(value -> ChatClefDiagnostics.itemStackSummary(value.stack())).orElse("none");
    }

    private static Object remainingDurability(Optional<MiningToolCandidate> candidate) {
        return candidate.<Object>map(MiningToolCandidate::remainingDurability).orElse("none");
    }

    private static boolean markStateChanged(Task task, String bucket, String stateKey) {
        if (task == null) {
            return true;
        }
        String normalizedStateKey = normalizeStateKey(stateKey);
        synchronized (LAST_STATE_BY_TASK) {
            Map<String, String> taskStates = LAST_STATE_BY_TASK.computeIfAbsent(task, ignored -> new HashMap<>());
            String previousStateKey = taskStates.get(bucket);
            if (normalizedStateKey.equals(previousStateKey)) {
                return false;
            }
            taskStates.put(bucket, normalizedStateKey);
            return true;
        }
    }

    private static String normalizeStateKey(String stateKey) {
        String normalized = stateKey == null ? "none" : stateKey;
        if (normalized.length() <= MAX_STATE_KEY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_STATE_KEY_LENGTH) + "...";
    }

    private static Object[] mergeFields(Object[] first, Object[] second) {
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second;
        }
        if (second == null || second.length == 0) {
            return first;
        }
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }
}
