package lavi.minecraft.fabric.chatclef.bridge.command.queue.ownership;

//20260905_kpopmodder: Serialize ordinary admission, dispatch, and STOP capture through one barrier.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;

import java.util.Optional;

public final class FabricChatClefStopAwareCommandAdmission {
    private final FabricChatClefCommandOwnershipState ownershipState;
    private final FabricChatClefStopControlAdmissionBarrier admissionBarrier;

    public FabricChatClefStopAwareCommandAdmission(
            FabricChatClefCommandOwnershipState ownershipState,
            FabricChatClefStopControlAdmissionBarrier admissionBarrier
    ) {
        this.ownershipState = ownershipState;
        this.admissionBarrier = admissionBarrier;
    }

    public boolean offer(FabricChatClefCommandContext context) {
        return admissionBarrier.admitOrdinary(() -> ownershipState.offer(context));
    }

    public Optional<FabricChatClefCommandContext> pollForDispatch() {
        return admissionBarrier.dispatchOrdinary(ownershipState::pollForDispatch);
    }

    public FabricChatClefOrdinaryCommandStopCapture captureForUserStop() {
        FabricChatClefCommandOwnershipSnapshot snapshot = ownershipState.snapshot();
        if (snapshot.active() != null) {
            return FabricChatClefOrdinaryCommandStopCapture.active(snapshot.active());
        }
        if (snapshot.pending() != null) {
            return FabricChatClefOrdinaryCommandStopCapture.pending(snapshot.pending());
        }
        return FabricChatClefOrdinaryCommandStopCapture.none();
    }

    public FabricChatClefStopControlAdmissionBarrier barrier() {
        return admissionBarrier;
    }
}
