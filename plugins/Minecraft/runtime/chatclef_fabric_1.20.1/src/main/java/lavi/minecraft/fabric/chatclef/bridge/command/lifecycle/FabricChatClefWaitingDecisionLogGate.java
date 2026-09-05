package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260905_kpopmodder: Throttle only repeated ordinary-command waiting-decision diagnostics.

public final class FabricChatClefWaitingDecisionLogGate {
    private static final long REPEAT_INTERVAL_MS = 5000L;

    private String lastDecisionKey = "";
    private long lastLoggedAtMs;

    public boolean shouldEmit(String requestId, String reason, long nowMs) {
        String key = requestId + ":" + reason;
        if (key.equals(lastDecisionKey) && nowMs - lastLoggedAtMs < REPEAT_INTERVAL_MS) {
            return false;
        }
        lastDecisionKey = key;
        lastLoggedAtMs = nowMs;
        return true;
    }

    public void reset() {
        lastDecisionKey = "";
    }
}
