package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260814_kpopmodder: Added this status enum to distinguish terminal send failures from successful sends.
public enum FabricChatClefCommandResultSendStatus {
    SENT,
    NO_SOCKET,
    GENERATION_MISMATCH,
    ENCODE_FAILED,
    SEND_FAILED,
    ASYNC_SEND_FAILED
}
