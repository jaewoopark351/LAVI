package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260814_kpopmodder: Added this outcome so command ownership is cleared only after an explicit send result.
public final class FabricChatClefCommandResultSendOutcome {
    private final FabricChatClefCommandResultSendStatus status;
    private final String message;

    private FabricChatClefCommandResultSendOutcome(
            FabricChatClefCommandResultSendStatus status,
            String message
    ) {
        this.status = status;
        this.message = nullToEmpty(message);
    }

    public static FabricChatClefCommandResultSendOutcome sent() {
        return new FabricChatClefCommandResultSendOutcome(
                FabricChatClefCommandResultSendStatus.SENT,
                ""
        );
    }

    public static FabricChatClefCommandResultSendOutcome inFlight() {
        return new FabricChatClefCommandResultSendOutcome(
                FabricChatClefCommandResultSendStatus.IN_FLIGHT,
                ""
        );
    }

    public static FabricChatClefCommandResultSendOutcome failed(
            FabricChatClefCommandResultSendStatus status,
            String message
    ) {
        return new FabricChatClefCommandResultSendOutcome(status, message);
    }

    public boolean succeeded() {
        return status == FabricChatClefCommandResultSendStatus.SENT;
    }

    public boolean retryable() {
        return status.retryable();
    }

    public FabricChatClefCommandResultSendStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public String diagnosticMessage() {
        if (message.isEmpty()) {
            return status.name();
        }
        return status.name() + ": " + message;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
