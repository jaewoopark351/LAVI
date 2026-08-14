package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260814_kpopmodder: Added this status enum to distinguish terminal send failures from successful sends.
public enum FabricChatClefCommandResultSendStatus {
    IN_FLIGHT,
    SENT,
    NO_SOCKET,
    GENERATION_MISMATCH,
    ENCODE_FAILED,
    SEND_FAILED,
    ASYNC_SEND_FAILED;

    public boolean retryable() {
        return this == NO_SOCKET
                || this == SEND_FAILED
                || this == ASYNC_SEND_FAILED;
    }
}
