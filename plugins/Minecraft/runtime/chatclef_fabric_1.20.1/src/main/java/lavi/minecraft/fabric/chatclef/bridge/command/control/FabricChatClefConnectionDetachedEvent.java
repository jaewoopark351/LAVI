package lavi.minecraft.fabric.chatclef.bridge.command.control;

//20260814_kpopmodder: Added this control event so WebSocket callbacks never clear live command ownership directly.
public final class FabricChatClefConnectionDetachedEvent {
    private final long connectionGeneration;
    private final String reason;
    private final long observedAtMs;
    private final String observedThread;

    private FabricChatClefConnectionDetachedEvent(long connectionGeneration, String reason) {
        this.connectionGeneration = connectionGeneration;
        this.reason = nullToEmpty(reason);
        this.observedAtMs = System.currentTimeMillis();
        this.observedThread = Thread.currentThread().getName();
    }

    public static FabricChatClefConnectionDetachedEvent of(long connectionGeneration, String reason) {
        return new FabricChatClefConnectionDetachedEvent(connectionGeneration, reason);
    }

    public long connectionGeneration() {
        return connectionGeneration;
    }

    public String reason() {
        return reason;
    }

    public long observedAtMs() {
        return observedAtMs;
    }

    public String observedThread() {
        return observedThread;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
