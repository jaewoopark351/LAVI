package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefConnectionDetachEventQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.ownership.FabricChatClefCommandOwnershipState;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.ownership.FabricChatClefStopAwareCommandAdmission;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletionQueue;

import java.util.Optional;

//20260801_kpopmodder: Preserve the legacy queue API as a thin facade over focused state and event owners.
public final class FabricChatClefCommandQueue {
    private final FabricChatClefCommandOwnershipState ownershipState;
    private final FabricChatClefStopAwareCommandAdmission stopAwareAdmission;
    private final FabricChatClefConnectionDetachEventQueue detachEvents;
    private final FabricChatClefCommandResultSendCompletionQueue resultCompletions;

    public FabricChatClefCommandQueue() {
        this(new FabricChatClefStopControlAdmissionBarrier());
    }

    public FabricChatClefCommandQueue(FabricChatClefStopControlAdmissionBarrier stopAdmissionBarrier) {
        this.ownershipState = new FabricChatClefCommandOwnershipState();
        this.stopAwareAdmission = new FabricChatClefStopAwareCommandAdmission(
                ownershipState,
                stopAdmissionBarrier
        );
        this.detachEvents = new FabricChatClefConnectionDetachEventQueue();
        this.resultCompletions = new FabricChatClefCommandResultSendCompletionQueue();
    }

    //20260905_kpopmodder: Commit ordinary admission under the same owner lock as the STOP barrier.
    public boolean offer(FabricChatClefCommandContext context) {
        return stopAwareAdmission.offer(context);
    }

    public Optional<FabricChatClefCommandContext> peekPending() {
        return ownershipState.peekPending();
    }

    public Optional<FabricChatClefCommandContext> pollForDispatch() {
        return stopAwareAdmission.pollForDispatch();
    }

    public boolean hasActive() {
        return ownershipState.hasActive();
    }

    public Optional<FabricChatClefCommandContext> activeContext() {
        return ownershipState.activeContext();
    }

    public Optional<String> activeRequestId() {
        return ownershipState.activeRequestId();
    }

    public boolean isActive(FabricChatClefCommandContext context) {
        return ownershipState.isActive(context);
    }

    public boolean isPending(FabricChatClefCommandContext context) {
        return ownershipState.isPending(context);
    }

    public FabricChatClefOrdinaryCommandStopCapture captureForUserStop() {
        return stopAwareAdmission.captureForUserStop();
    }

    public FabricChatClefStopControlAdmissionBarrier stopAdmissionBarrier() {
        return stopAwareAdmission.barrier();
    }

    public FabricChatClefCommandQueueCompletion complete(
            FabricChatClefCommandContext context,
            String reason
    ) {
        return ownershipState.complete(context, reason);
    }

    public boolean removePending(FabricChatClefCommandContext context) {
        return ownershipState.removePending(context);
    }

    public void enqueueCommandResultSendCompletion(
            FabricChatClefCommandResultSendCompletion completion
    ) {
        resultCompletions.enqueue(completion);
    }

    public FabricChatClefCommandResultSendCompletion pollCommandResultSendCompletion() {
        return resultCompletions.poll();
    }

    public void enqueueConnectionDetached(long connectionGeneration, String reason) {
        detachEvents.enqueue(connectionGeneration, reason);
    }

    public Optional<FabricChatClefConnectionDetachedEvent> pollConnectionDetached() {
        return detachEvents.poll();
    }

    public FabricChatClefConnectionDetachResult markConnectionDetached(
            FabricChatClefConnectionDetachedEvent event
    ) {
        return ownershipState.markConnectionDetached(event);
    }

    public FabricChatClefCommandQueueCompletion clearDetachedActive(
            FabricChatClefCommandContext context,
            String reason
    ) {
        return ownershipState.clearDetachedActive(context, reason);
    }

    public void clear(String reason) {
        ownershipState.clear(reason);
        detachEvents.clear();
        resultCompletions.clear();
    }
}
