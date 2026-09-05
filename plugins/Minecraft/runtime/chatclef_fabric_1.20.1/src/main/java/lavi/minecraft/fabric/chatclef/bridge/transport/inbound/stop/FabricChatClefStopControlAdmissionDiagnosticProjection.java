package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.stop;

//20260905_kpopmodder: Project only STOP admission state and dedupe decisions into diagnostic values.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;

public final class FabricChatClefStopControlAdmissionDiagnosticProjection {
    private final FabricChatClefStopControlQueue stopQueue;

    public FabricChatClefStopControlAdmissionDiagnosticProjection(
            FabricChatClefStopControlQueue stopQueue
    ) {
        this.stopQueue = stopQueue;
    }

    public String barrierState() {
        return stopQueue.active().isPresent() ? "closed" : "none";
    }

    public String ordinaryGateState() {
        return stopQueue.active().isPresent() ? "blocked" : "unchanged";
    }

    public boolean quarantineActive() {
        return stopQueue.active()
                .map(context -> "quarantined".equals(context.phase()))
                .orElse(false);
    }

    public String disposition(FabricChatClefStopControlDedupeDecision decision) {
        return switch (decision) {
            case DUPLICATE_LIVE -> "duplicate_control_live";
            case DUPLICATE_TOMBSTONED -> "duplicate_control_tombstoned";
            case DUPLICATE_PAYLOAD_MISMATCH -> "duplicate_control_payload_mismatch";
            case CAPACITY_EXHAUSTED -> "stop_control_dedupe_capacity_exhausted";
            case RESERVED -> "reserved";
        };
    }
}
