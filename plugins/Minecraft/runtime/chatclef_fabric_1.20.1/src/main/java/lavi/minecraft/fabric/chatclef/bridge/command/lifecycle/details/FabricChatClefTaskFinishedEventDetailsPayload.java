package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefTaskFinishedEventDetailsPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskFinishedObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.Map;

//20260805_kpopmodder: Keep TaskFinishedEvent lifecycle detail fields typed until the Map edge.
public final class FabricChatClefTaskFinishedEventDetailsPayload implements FabricChatClefCommandDiagnosticDetailsPayload {
    private final FabricChatClefTaskFinishedObservationPayload taskFinishedEvent;
    private final boolean matchedBoundRootTask;
    private final FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship;
    private final String eventTaskBoundRootMatchReason;
    private final boolean finishCallbackReceived;
    private final FabricChatClefTaskRuntimeObservationPayload runtime;

    private FabricChatClefTaskFinishedEventDetailsPayload(
            FabricChatClefCommandTerminationObservation observation,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        this.taskFinishedEvent = FabricChatClefTaskFinishedObservationPayload.requiredFrom(observation);
        this.matchedBoundRootTask = matchedBoundRootTask;
        this.eventTaskRelationship = eventTaskRelationship;
        this.eventTaskBoundRootMatchReason = eventTaskBoundRootMatchReason;
        this.finishCallbackReceived = finishCallbackReceived;
        this.runtime = runtime;
    }

    public static FabricChatClefTaskFinishedEventDetailsPayload of(
            FabricChatClefCommandTerminationObservation observation,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        return new FabricChatClefTaskFinishedEventDetailsPayload(
                observation,
                matchedBoundRootTask,
                eventTaskRelationship,
                eventTaskBoundRootMatchReason,
                finishCallbackReceived,
                runtime
        );
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefTaskFinishedEventDetailsPayloadMap.toMap(
                taskFinishedEvent,
                matchedBoundRootTask,
                eventTaskRelationship,
                eventTaskBoundRootMatchReason,
                finishCallbackReceived,
                runtime
        );
    }
}
