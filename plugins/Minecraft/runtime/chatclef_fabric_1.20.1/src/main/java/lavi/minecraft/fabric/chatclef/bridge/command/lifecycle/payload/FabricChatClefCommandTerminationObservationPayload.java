package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.Map;

//20260805_kpopmodder: Keep termination observation values typed before final diagnostic map serialization.
public final class FabricChatClefCommandTerminationObservationPayload {
    private final String terminationKind;
    private final boolean taskPresent;
    private final boolean taskStopped;
    private final boolean stopStateAvailable;
    private final String stopStateError;
    private final double durationSeconds;
    private final boolean eventPresent;
    private final String eventIdentity;
    private final boolean eventTaskPresent;
    private final String eventTaskClass;
    private final String eventTaskIdentity;
    private final String taskAbsenceReason;
    private final long observationSequence;
    private final long observedAtMs;
    private final long observedClientTick;
    private final String observationThread;
    private final int queueDepthBefore;
    private final int queueDepthAfter;
    private final long dequeuedAtMs;
    private final long dequeuedClientTick;
    private final long observationAgeMs;
    private final int queueDepthAfterDequeue;
    private final FabricChatClefTaskSnapshot taskSnapshot;
    private final FabricChatClefTaskOwnershipSnapshot ownershipAtObserve;
    private final FabricChatClefTaskOwnershipSnapshot ownershipAtDequeue;
    private final boolean rootChangedBetweenObserveAndDequeue;
    private final boolean rootAssignmentChangedBetweenObserveAndDequeue;

    public FabricChatClefCommandTerminationObservationPayload(
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
        this.terminationKind = terminationKind;
        this.taskPresent = taskPresent;
        this.taskStopped = taskStopped;
        this.stopStateAvailable = stopStateAvailable;
        this.stopStateError = stopStateError;
        this.durationSeconds = durationSeconds;
        this.eventPresent = eventPresent;
        this.eventIdentity = eventIdentity;
        this.eventTaskPresent = eventTaskPresent;
        this.eventTaskClass = eventTaskClass;
        this.eventTaskIdentity = eventTaskIdentity;
        this.taskAbsenceReason = taskAbsenceReason;
        this.observationSequence = observationSequence;
        this.observedAtMs = observedAtMs;
        this.observedClientTick = observedClientTick;
        this.observationThread = observationThread;
        this.queueDepthBefore = queueDepthBefore;
        this.queueDepthAfter = queueDepthAfter;
        this.dequeuedAtMs = dequeuedAtMs;
        this.dequeuedClientTick = dequeuedClientTick;
        this.observationAgeMs = observationAgeMs;
        this.queueDepthAfterDequeue = queueDepthAfterDequeue;
        this.taskSnapshot = taskSnapshot;
        this.ownershipAtObserve = ownershipAtObserve;
        this.ownershipAtDequeue = ownershipAtDequeue;
        this.rootChangedBetweenObserveAndDequeue = rootChangedBetweenObserveAndDequeue;
        this.rootAssignmentChangedBetweenObserveAndDequeue = rootAssignmentChangedBetweenObserveAndDequeue;
    }

    public Map<String, Object> toMap() {
        return FabricChatClefCommandTerminationObservationPayloadMap.toMap(
                terminationKind,
                taskPresent,
                taskStopped,
                stopStateAvailable,
                stopStateError,
                durationSeconds,
                eventPresent,
                eventIdentity,
                eventTaskPresent,
                eventTaskClass,
                eventTaskIdentity,
                taskAbsenceReason,
                observationSequence,
                observedAtMs,
                observedClientTick,
                observationThread,
                queueDepthBefore,
                queueDepthAfter,
                dequeuedAtMs,
                dequeuedClientTick,
                observationAgeMs,
                queueDepthAfterDequeue,
                taskSnapshot,
                ownershipAtObserve,
                ownershipAtDequeue,
                rootChangedBetweenObserveAndDequeue,
                rootAssignmentChangedBetweenObserveAndDequeue
        );
    }
}
