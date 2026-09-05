package lavi.minecraft.fabric.chatclef.bridge.transport.session;

//20260905_kpopmodder: Keep accepted server generation distinct from the Java-local socket generation.
public final class FabricChatClefAcceptedSessionIdentity {
    private final String sessionId;
    private final long serverConnectionGeneration;
    private final long javaSocketGeneration;
    private final String handshakeCorrelationId;

    public FabricChatClefAcceptedSessionIdentity(
            String sessionId,
            long serverConnectionGeneration,
            long javaSocketGeneration,
            String handshakeCorrelationId
    ) {
        this.sessionId = sessionId;
        this.serverConnectionGeneration = serverConnectionGeneration;
        this.javaSocketGeneration = javaSocketGeneration;
        this.handshakeCorrelationId = handshakeCorrelationId;
    }

    public String sessionId() {
        return sessionId;
    }

    public long serverConnectionGeneration() {
        return serverConnectionGeneration;
    }

    public long javaSocketGeneration() {
        return javaSocketGeneration;
    }

    public String handshakeCorrelationId() {
        return handshakeCorrelationId;
    }
}
