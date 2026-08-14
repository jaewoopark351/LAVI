package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.FabricChatClefTerminationCompletionPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.event.FabricChatClefTerminationEventPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.FabricChatClefTerminationOwnershipChangePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.ownership.FabricChatClefTerminationOwnershipSnapshotPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state.FabricChatClefTerminationStopStatePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.task.FabricChatClefTerminationTaskSnapshotPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.FabricChatClefTerminationDequeueTimingPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.FabricChatClefTerminationObservationTimingPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate termination observation Map keys without changing emitted diagnostic fields.
public final class FabricChatClefCommandTerminationObservationPayloadMap {
    private FabricChatClefCommandTerminationObservationPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String terminationKind,
            boolean taskPresent,
            boolean taskStopped,
            boolean stopStateAvailable,
            String stopStateError,
            double durationSeconds,
            boolean eventPresent,
            String eventIdentity,
            boolean eventTaskPresent,
            String eventTaskClass,
            String eventTaskIdentity,
            String taskAbsenceReason,
            long observationSequence,
            long observedAtMs,
            long observedClientTick,
            String observationThread,
            int queueDepthBefore,
            int queueDepthAfter,
            long dequeuedAtMs,
            long dequeuedClientTick,
            long observationAgeMs,
            int queueDepthAfterDequeue,
            FabricChatClefTaskSnapshot taskSnapshot,
            FabricChatClefTaskOwnershipSnapshot ownershipAtObserve,
            FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue,
            boolean rootChangedBetweenObserveAndDequeue,
            boolean rootAssignmentChangedBetweenObserveAndDequeue
    ) {
        Map<String, Object> payload = new HashMap<>();
        FabricChatClefTerminationCompletionPayloadMap.writeTo(payload, terminationKind);
        FabricChatClefTerminationStopStatePayloadMap.writeTo(
                payload,
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                durationSeconds
        );
        FabricChatClefTerminationEventPayloadMap.writeTo(
                payload,
                eventPresent,
                eventIdentity,
                eventTaskPresent,
                eventTaskClass,
                eventTaskIdentity,
                taskAbsenceReason
        );
        FabricChatClefTerminationObservationTimingPayloadMap.writeTo(
                payload,
                observationSequence,
                observedAtMs,
                observedClientTick,
                observationThread,
                queueDepthBefore,
                queueDepthAfter
        );
        FabricChatClefTerminationDequeueTimingPayloadMap.writeTo(
                payload,
                dequeuedAtMs,
                dequeuedClientTick,
                observationAgeMs,
                queueDepthAfterDequeue
        );
        FabricChatClefTerminationTaskSnapshotPayloadMap.writeTo(payload, taskSnapshot);
        FabricChatClefTerminationOwnershipSnapshotPayloadMap.writeTo(
                payload,
                ownershipAtObserve,
                ownershipAtDequeue
        );
        FabricChatClefTerminationOwnershipChangePayloadMap.writeTo(
                payload,
                rootChangedBetweenObserveAndDequeue,
                rootAssignmentChangedBetweenObserveAndDequeue
        );
        return payload;
    }
}
