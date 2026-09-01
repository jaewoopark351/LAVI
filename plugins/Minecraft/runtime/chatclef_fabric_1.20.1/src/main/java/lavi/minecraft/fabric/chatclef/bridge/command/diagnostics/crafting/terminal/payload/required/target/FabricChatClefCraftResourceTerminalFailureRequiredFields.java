package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required.target;

import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureAggregateSnapshot;

import java.util.Map;
import java.util.Optional;

import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.unavailableLong;

//20260901_kpopmodder: Own terminal failure aggregate and its exact target attribution.
public final class FabricChatClefCraftResourceTerminalFailureRequiredFields {
    private FabricChatClefCraftResourceTerminalFailureRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            Optional<CraftResourceFailureAggregateSnapshot> failureSnapshot) {
        if (failureSnapshot.isEmpty()) {
            fields.put("ownerTaskClass", "UNAVAILABLE_FAILURE_SCOPE_NOT_ACTIVE");
            fields.put("failureAssociationStatus", "UNAVAILABLE_FAILURE_SCOPE_NOT_ACTIVE");
            fields.put("failureTargetAttemptSequence", "UNAVAILABLE");
            fields.put("failureResourceStage", "UNKNOWN");
            fields.put("failureTargetRole", "UNKNOWN");
            fields.put("failureTargetPosition", "UNAVAILABLE");
            fields.put("firstFailureCount", "UNAVAILABLE");
            fields.put("lastFailureCount", "UNAVAILABLE");
            fields.put("allowedFailures", "UNAVAILABLE");
            fields.put("unreachableBefore", "UNAVAILABLE");
            fields.put("unreachableAfter", "UNAVAILABLE");
            fields.put("firstObservedTick", "UNAVAILABLE");
            fields.put("lastObservedTick", "UNAVAILABLE");
            fields.put("suppressedDetailCount", "UNAVAILABLE_FAILURE_SCOPE_NOT_ACTIVE");
            return;
        }

        CraftResourceFailureAggregateSnapshot failure = failureSnapshot.get();
        fields.put("ownerTaskClass", failure.ownerTaskClass());
        fields.put("failureAssociationStatus", failure.associationStatus().name());
        fields.put("failureTargetAttemptSequence", unavailableLong(failure.targetAttemptSequence()));
        fields.put("failureResourceStage", failure.resourceStage().name());
        fields.put("failureTargetRole", failure.targetRole().name());
        fields.put("failureTargetPosition", failure.targetPosition());
        fields.put("firstFailureCount", unavailableLong(failure.firstFailureCount()));
        fields.put("lastFailureCount", unavailableLong(failure.lastFailureCount()));
        fields.put("allowedFailures", unavailableLong(failure.allowedFailures()));
        fields.put(
                "unreachableBefore",
                failure.unreachableBefore().map(value -> (Object) value).orElse("UNAVAILABLE")
        );
        fields.put(
                "unreachableAfter",
                failure.unreachableAfter().map(value -> (Object) value).orElse("UNAVAILABLE")
        );
        fields.put("firstObservedTick", unavailableLong(failure.firstObservedTick()));
        fields.put("lastObservedTick", unavailableLong(failure.lastObservedTick()));
        fields.put("suppressedDetailCount", failure.suppressedDetailCount());
    }
}
