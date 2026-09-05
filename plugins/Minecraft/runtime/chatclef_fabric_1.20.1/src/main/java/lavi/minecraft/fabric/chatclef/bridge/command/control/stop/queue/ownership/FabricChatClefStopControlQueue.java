package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Enforce one independent STOP entry while retaining its barrier through delivery.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;

import java.util.Optional;

public final class FabricChatClefStopControlQueue {
    private final FabricChatClefStopControlAdmissionBarrier admissionBarrier;
    private FabricChatClefStopControlContext active;

    public FabricChatClefStopControlQueue(FabricChatClefStopControlAdmissionBarrier admissionBarrier) {
        this.admissionBarrier = admissionBarrier;
    }

    public Optional<FabricChatClefStopControlContext> offer(FabricChatClefStopControlRequest request) {
        return admissionBarrier.admitStop(
                request.identity(),
                request.javaSocketGeneration(),
                candidate -> commitOffer(request, candidate)
        );
    }

    public synchronized Optional<FabricChatClefStopControlContext> active() {
        return Optional.ofNullable(active);
    }

    public boolean completeAndRelease(FabricChatClefStopControlContext context) {
        return admissionBarrier.release(context == null ? null : context.barrierToken(), () -> clearExact(context));
    }

    public void resetForShutdown() {
        admissionBarrier.resetForShutdown(this::clearAll);
    }

    private synchronized FabricChatClefStopControlContext commitOffer(
            FabricChatClefStopControlRequest request,
            FabricChatClefStopControlAdmissionBarrierToken token
    ) {
        if (active != null) {
            return null;
        }
        active = new FabricChatClefStopControlContext(request, token);
        return active;
    }

    private synchronized boolean clearExact(FabricChatClefStopControlContext context) {
        if (active == context) {
            active = null;
            return true;
        }
        return false;
    }

    private synchronized void clearAll() {
        active = null;
    }
}
