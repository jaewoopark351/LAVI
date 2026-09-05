package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Keep bounded STOP replay admission outcomes explicit and non-wire.
public enum FabricChatClefStopControlDedupeDecision {
    RESERVED,
    DUPLICATE_LIVE,
    DUPLICATE_TOMBSTONED,
    DUPLICATE_PAYLOAD_MISMATCH,
    CAPACITY_EXHAUSTED
}
