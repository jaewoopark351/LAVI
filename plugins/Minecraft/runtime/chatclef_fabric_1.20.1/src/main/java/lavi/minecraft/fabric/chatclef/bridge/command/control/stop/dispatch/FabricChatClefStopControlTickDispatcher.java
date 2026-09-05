package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Give one admitted STOP exclusive client-tick ownership before ordinary dispatch.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.util.Optional;
import java.util.function.Supplier;

public final class FabricChatClefStopControlTickDispatcher {
    public static final int STOP_VERIFY_MAX_LATER_TICKS =
            FabricChatClefStopControlRetirementVerifier.MAX_LATER_TICKS;

    private final FabricChatClefStopControlQueue stopQueue;
    private final FabricChatClefStopControlAdmissionBarrier admissionBarrier;
    private final FabricChatClefStopControlResultOutbox resultOutbox;
    private final FabricChatClefStopControlQueuedExecutor queuedExecutor;
    private final FabricChatClefStopControlRetirementVerifier retirementVerifier;

    public FabricChatClefStopControlTickDispatcher(
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlCommandLifecycle commandLifecycle,
            FabricChatClefStopCommandExecutor stopCommandExecutor,
            FabricChatClefStopControlResultOutbox resultOutbox,
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            Supplier<FabricChatClefTaskOwnershipEvidence> taskOwnershipReader,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.stopQueue = stopQueue;
        this.admissionBarrier = commandQueue.stopAdmissionBarrier();
        this.resultOutbox = resultOutbox;
        FabricChatClefStopControlTransitionEmitter transitionEmitter =
                new FabricChatClefStopControlTransitionEmitter(diagnostics);
        FabricChatClefStopControlTaskOwnershipReader safeTaskOwnershipReader =
                new FabricChatClefStopControlTaskOwnershipReader(taskOwnershipReader);
        FabricChatClefStopControlResultCommitter resultCommitter =
                new FabricChatClefStopControlResultCommitter(
                        stopQueue,
                        new FabricChatClefStopControlResultFactory(),
                        resultOutbox,
                        dedupeRegistry,
                        transitionEmitter
                );
        this.queuedExecutor = new FabricChatClefStopControlQueuedExecutor(
                commandQueue,
                admissionBarrier,
                sessionGuard,
                commandLifecycle,
                stopCommandExecutor,
                safeTaskOwnershipReader,
                new FabricChatClefStopControlTargetResolver(),
                transitionEmitter,
                resultCommitter
        );
        this.retirementVerifier = new FabricChatClefStopControlRetirementVerifier(
                commandQueue,
                sessionGuard,
                commandLifecycle,
                safeTaskOwnershipReader,
                transitionEmitter,
                resultCommitter
        );
    }

    public boolean onEndClientTick(long nowMs, long clientTick) {
        resultOutbox.onEndClientTick(nowMs);
        Optional<FabricChatClefStopControlContext> active = stopQueue.active();
        if (active.isEmpty()) {
            return admissionBarrier.blocksOrdinary();
        }
        FabricChatClefStopControlContext context = active.get();
        if (!context.resultCommitted() && !"quarantined".equals(context.phase())) {
            if ("queued".equals(context.phase())) {
                queuedExecutor.execute(context, nowMs, clientTick);
            } else if ("verifying".equals(context.phase())) {
                retirementVerifier.verify(context, nowMs, clientTick);
            }
        }
        return admissionBarrier.blocksOrdinary();
    }
}
