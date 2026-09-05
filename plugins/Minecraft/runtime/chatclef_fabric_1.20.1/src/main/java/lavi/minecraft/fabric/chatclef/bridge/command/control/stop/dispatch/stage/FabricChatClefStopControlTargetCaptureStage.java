package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Capture the ordinary target atomically under the admitted STOP barrier.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlTargetResolver;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlTargetCaptureStage {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefStopControlAdmissionBarrier admissionBarrier;
    private final FabricChatClefStopControlTargetResolver targetResolver;

    public FabricChatClefStopControlTargetCaptureStage(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefStopControlAdmissionBarrier admissionBarrier,
            FabricChatClefStopControlTargetResolver targetResolver
    ) {
        this.commandQueue = commandQueue;
        this.admissionBarrier = admissionBarrier;
        this.targetResolver = targetResolver;
    }

    public FabricChatClefOrdinaryCommandStopCapture capture(
            FabricChatClefStopControlContext context
    ) {
        FabricChatClefOrdinaryCommandStopCapture capture;
        try {
            capture = admissionBarrier.inspectOwned(
                    context.barrierToken(),
                    commandQueue::captureForUserStop
            ).orElse(null);
        } catch (Throwable error) {
            return null;
        }
        if (capture == null) {
            return null;
        }
        context.capture(capture, targetResolver.resolve(context.request(), capture));
        return capture;
    }
}
