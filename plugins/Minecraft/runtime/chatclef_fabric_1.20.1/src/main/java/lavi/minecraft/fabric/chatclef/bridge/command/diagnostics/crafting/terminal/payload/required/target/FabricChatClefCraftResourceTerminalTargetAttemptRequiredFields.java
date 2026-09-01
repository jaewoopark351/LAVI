package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.target;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.diagnostics.crafting.acquisition.target.closure.CraftResourceTargetClosureSnapshot;

import java.util.List;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Own terminal target-attempt fields without losing a just-closed tuple.
public final class FabricChatClefCraftResourceTerminalTargetAttemptRequiredFields {
    private FabricChatClefCraftResourceTerminalTargetAttemptRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            Optional<CraftResourceTargetAttemptSnapshot> targetSnapshot) {
        if (targetSnapshot.isEmpty()) {
            fields.put("targetAttemptCount", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            fields.put("boundedTargetHistory", List.of());
            fields.put("unreachableRequestCount", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            fields.put("blacklistTransitionCount", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            fields.put("targetAttemptSequence", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            fields.put("resourceStage", "UNKNOWN");
            fields.put("targetRole", "UNKNOWN");
            fields.put("targetPosition", "UNAVAILABLE");
            fields.put("expectedBlockIds", List.of());
            fields.put("targetAttributionSource", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            fields.put("targetClosureKind", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            return;
        }

        CraftResourceTargetAttemptSnapshot target = targetSnapshot.get();
        fields.put("targetAttemptCount", target.attemptTransitionCount());
        fields.put("boundedTargetHistory", target.history());
        fields.put("unreachableRequestCount", target.unreachableRequestCount());
        fields.put("blacklistTransitionCount", target.blacklistTransitionCount());
        fields.put("targetAttemptSequence", target.targetAttemptSequence());
        if (target.currentTuple().isPresent()) {
            appendTuple(fields, target.currentTuple().get());
            fields.put("targetAttributionSource", "CURRENT_ACTIVE_TARGET");
            fields.put("targetClosureKind", "NOT_CLOSED");
            return;
        }
        if (target.lastClosure().isPresent()) {
            CraftResourceTargetClosureSnapshot closure = target.lastClosure().get();
            appendTuple(fields, closure.targetTuple());
            fields.put("targetAttributionSource", "LAST_OWNED_TARGET_CLOSURE");
            fields.put("targetClosureKind", closure.closureKind().name());
            return;
        }
        fields.put("resourceStage", "UNKNOWN");
        fields.put("targetRole", "UNKNOWN");
        fields.put("targetPosition", "UNAVAILABLE_NO_TARGET_OBSERVED");
        fields.put("expectedBlockIds", List.of());
        fields.put("targetAttributionSource", "UNAVAILABLE_NO_TARGET_OBSERVED");
        fields.put("targetClosureKind", "UNAVAILABLE_NO_TARGET_OBSERVED");
    }

    private static void appendTuple(
            Map<String, Object> fields,
            CraftResourceTargetTuple tuple) {
        fields.put("resourceStage", tuple.resourceStage().name());
        fields.put("targetRole", tuple.targetRole().name());
        fields.put("targetPosition", tuple.targetPosition());
        fields.put("expectedBlockIds", tuple.expectedBlockIds());
    }
}
