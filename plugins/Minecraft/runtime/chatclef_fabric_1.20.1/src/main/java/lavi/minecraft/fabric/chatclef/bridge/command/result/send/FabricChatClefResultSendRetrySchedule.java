package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Define only the bounded result-send retry schedule.

public final class FabricChatClefResultSendRetrySchedule {
    public static final int MAX_SEND_ATTEMPTS = 5;

    public long delayMs(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 250L;
            case 2 -> 500L;
            case 3 -> 1000L;
            case 4 -> 2000L;
            default -> 5000L;
        };
    }
}
