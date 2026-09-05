package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Preserve the STOP result-commit API as a thin facade over focused commit stages.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit.FabricChatClefStopControlBarrierRelease;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit.FabricChatClefStopControlCommitDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit.FabricChatClefStopControlDedupeFinalizer;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit.FabricChatClefStopControlQuarantine;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit.FabricChatClefStopControlResultProfileCommit;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

public final class FabricChatClefStopControlResultCommitter {
    private final FabricChatClefStopControlResultProfileCommit profileCommit;
    private final FabricChatClefStopControlResultOutbox resultOutbox;
    private final FabricChatClefStopControlDedupeFinalizer dedupeFinalizer;
    private final FabricChatClefStopControlBarrierRelease barrierRelease;
    private final FabricChatClefStopControlQuarantine quarantine;

    public FabricChatClefStopControlResultCommitter(
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefStopControlResultFactory resultFactory,
            FabricChatClefStopControlResultOutbox resultOutbox,
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            FabricChatClefStopControlTransitionEmitter transitionEmitter
    ) {
        this.resultOutbox = resultOutbox;
        this.profileCommit = new FabricChatClefStopControlResultProfileCommit(resultFactory);
        this.dedupeFinalizer = new FabricChatClefStopControlDedupeFinalizer(dedupeRegistry);
        this.barrierRelease = new FabricChatClefStopControlBarrierRelease(stopQueue);
        this.quarantine = new FabricChatClefStopControlQuarantine(
                dedupeFinalizer,
                new FabricChatClefStopControlCommitDiagnostics(transitionEmitter)
        );
    }

    public void commitNoMutation(
            FabricChatClefStopControlContext context,
            String reason,
            long clientTick
    ) {
        try {
            profileCommit.rejected(context, reason, clientTick)
                    .ifPresent(payload -> finalizeAndSend(context, payload, true));
        } catch (RuntimeException error) {
            quarantineWithoutWire(context, "result_profile_construction_failed");
        }
    }

    public void commitCompleted(FabricChatClefStopControlContext context, long clientTick) {
        try {
            profileCommit.completed(context, clientTick)
                    .ifPresent(payload -> finalizeAndSend(context, payload, true));
        } catch (RuntimeException error) {
            quarantineWithoutWire(context, "result_profile_construction_failed");
        }
    }

    public void commitUnknown(
            FabricChatClefStopControlContext context,
            String reason,
            long clientTick,
            boolean mutationPossible
    ) {
        try {
            profileCommit.unknown(context, reason, clientTick, mutationPossible)
                    .ifPresent(payload -> finalizeAndSend(context, payload, false));
            context.markQuarantined();
        } catch (RuntimeException error) {
            quarantineWithoutWire(context, "result_profile_construction_failed");
        }
    }

    public void quarantineWithoutWire(
            FabricChatClefStopControlContext context,
            String disposition
    ) {
        quarantine.apply(context, disposition);
    }

    private void finalizeAndSend(
            FabricChatClefStopControlContext context,
            FabricChatClefCommandResultPayload payload,
            boolean releaseOnSent
    ) {
        dedupeFinalizer.finalizeIdentity(context);
        resultOutbox.commitAndSend(
                context.request().base(),
                payload,
                true,
                "none",
                releaseOnSent ? barrierRelease.afterSent(context) : barrierRelease.retain()
        );
    }
}
