package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Keep one STOP control identity immutable across validation, execution, and delivery.

import java.util.Objects;

public final class FabricChatClefStopControlIdentity {
    private final String sessionId;
    private final long serverConnectionGeneration;
    private final String requestId;
    private final String messageId;

    public FabricChatClefStopControlIdentity(
            String sessionId,
            long serverConnectionGeneration,
            String requestId,
            String messageId
    ) {
        this.sessionId = sessionId;
        this.serverConnectionGeneration = serverConnectionGeneration;
        this.requestId = requestId;
        this.messageId = messageId;
    }

    public String sessionId() {
        return sessionId;
    }

    public long serverConnectionGeneration() {
        return serverConnectionGeneration;
    }

    public String requestId() {
        return requestId;
    }

    public String messageId() {
        return messageId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FabricChatClefStopControlIdentity that)) {
            return false;
        }
        return serverConnectionGeneration == that.serverConnectionGeneration
                && sessionId.equals(that.sessionId)
                && requestId.equals(that.requestId)
                && messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, serverConnectionGeneration, requestId, messageId);
    }

    @Override
    public String toString() {
        return "session=" + sessionId
                + " server_generation=" + serverConnectionGeneration
                + " request=" + requestId
                + " message=" + messageId;
    }
}
