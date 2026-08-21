package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefFinishCallbackObservation;

import java.util.HashMap;
import java.util.Map;

//20260822_kpopmodder: Serialize idle-root decision diagnostics under the existing log details boundary.
public final class FabricChatClefPreexistingIdleRootDetailsPayloadMap {
    private FabricChatClefPreexistingIdleRootDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefRootOwnershipClassification classification,
            FabricChatClefFinishCallbackObservation finishCallbackObservation,
            int finishCallbackDuplicateCount,
            FabricChatClefStableRequestQuiescenceObservation stableObservation,
            String taskFinishedEventAssociation
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("root_ownership_classification", classification == null ? "OWNERSHIP_UNKNOWN" : classification.name());
        payload.put(
                "finish_callback_first_observation",
                finishCallbackObservation == null ? Map.of() : finishCallbackObservation.toMap(finishCallbackDuplicateCount)
        );
        payload.put("finish_callback_duplicate_count", finishCallbackDuplicateCount);
        payload.put(
                "stable_idle_root_observation",
                stableObservation == null ? Map.of() : stableObservation.toMap()
        );
        payload.put("task_finished_event_association", taskFinishedEventAssociation == null ? "" : taskFinishedEventAssociation);
        return payload;
    }
}
