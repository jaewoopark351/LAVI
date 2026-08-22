package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefPreexistingIdleRootDetailsPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefFinishCallbackObservation;

import java.util.Map;

//20260822_kpopmodder: Log incident-only idle-root classification details without expanding result data.
public final class FabricChatClefPreexistingIdleRootDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private final FabricChatClefRootOwnershipClassification classification;
    private final FabricChatClefFinishCallbackObservation finishCallbackObservation;
    private final int finishCallbackDuplicateCount;
    private final FabricChatClefStableRequestQuiescenceObservation stableObservation;
    private final String taskFinishedEventAssociation;
    private final FabricChatClefCommandTerminationObservation unboundTaskFinishedObservation;

    public FabricChatClefPreexistingIdleRootDetailsPayload(
            FabricChatClefRootOwnershipClassification classification,
            FabricChatClefFinishCallbackObservation finishCallbackObservation,
            int finishCallbackDuplicateCount,
            FabricChatClefStableRequestQuiescenceObservation stableObservation,
            String taskFinishedEventAssociation,
            FabricChatClefCommandTerminationObservation unboundTaskFinishedObservation
    ) {
        this.classification = classification;
        this.finishCallbackObservation = finishCallbackObservation;
        this.finishCallbackDuplicateCount = finishCallbackDuplicateCount;
        this.stableObservation = stableObservation;
        this.taskFinishedEventAssociation = taskFinishedEventAssociation == null ? "" : taskFinishedEventAssociation;
        this.unboundTaskFinishedObservation = unboundTaskFinishedObservation;
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefPreexistingIdleRootDetailsPayloadMap.toMap(
                classification,
                finishCallbackObservation,
                finishCallbackDuplicateCount,
                stableObservation,
                taskFinishedEventAssociation,
                unboundTaskFinishedObservation
        );
    }
}
