package lavi.minecraft.diagnostics.mining.baritone.executor;

//20260830_kpopmodder: Store only the cheap semantic values required to classify executor progress.
final class BaritoneExecutorProgressObservation {
    private final String phase;
    private final long tick;
    private final String threadName;
    private final String pathingBehaviorIdentity;
    private final String pathingBehaviorType;
    private final String pathing;
    private final String cancelRequested;
    private final String calcFailedLastTick;
    private final String goalIdentity;
    private final String inProgressIdentity;
    private final String currentExecutorIdentity;
    private final String nextExecutorIdentity;
    private final boolean currentExecutorPresent;
    private final int currentExecutorPosition;
    private final String currentExecutorPositionText;
    private final String currentExecutorFailed;
    private final String currentExecutorFinished;
    private final double playerX;
    private final double playerY;
    private final double playerZ;
    private final double targetDistanceSq;

    BaritoneExecutorProgressObservation(String phase,
                                        long tick,
                                        String threadName,
                                        String pathingBehaviorIdentity,
                                        String pathingBehaviorType,
                                        String pathing,
                                        String cancelRequested,
                                        String calcFailedLastTick,
                                        String goalIdentity,
                                        String inProgressIdentity,
                                        String currentExecutorIdentity,
                                        String nextExecutorIdentity,
                                        boolean currentExecutorPresent,
                                        int currentExecutorPosition,
                                        String currentExecutorPositionText,
                                        String currentExecutorFailed,
                                        String currentExecutorFinished,
                                        double playerX,
                                        double playerY,
                                        double playerZ,
                                        double targetDistanceSq) {
        this.phase = phase;
        this.tick = tick;
        this.threadName = threadName;
        this.pathingBehaviorIdentity = pathingBehaviorIdentity;
        this.pathingBehaviorType = pathingBehaviorType;
        this.pathing = pathing;
        this.cancelRequested = cancelRequested;
        this.calcFailedLastTick = calcFailedLastTick;
        this.goalIdentity = goalIdentity;
        this.inProgressIdentity = inProgressIdentity;
        this.currentExecutorIdentity = currentExecutorIdentity;
        this.nextExecutorIdentity = nextExecutorIdentity;
        this.currentExecutorPresent = currentExecutorPresent;
        this.currentExecutorPosition = currentExecutorPosition;
        this.currentExecutorPositionText = currentExecutorPositionText;
        this.currentExecutorFailed = currentExecutorFailed;
        this.currentExecutorFinished = currentExecutorFinished;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.targetDistanceSq = targetDistanceSq;
    }

    boolean hasRelevantState() {
        return currentExecutorPresent
                || !"none".equals(nextExecutorIdentity)
                || !"none".equals(inProgressIdentity)
                || !"none".equals(goalIdentity)
                || "true".equals(pathing);
    }

    String phase() { return phase; }
    long tick() { return tick; }
    String threadName() { return threadName; }
    String pathingBehaviorIdentity() { return pathingBehaviorIdentity; }
    String pathingBehaviorType() { return pathingBehaviorType; }
    String pathing() { return pathing; }
    String cancelRequested() { return cancelRequested; }
    String calcFailedLastTick() { return calcFailedLastTick; }
    String goalIdentity() { return goalIdentity; }
    String inProgressIdentity() { return inProgressIdentity; }
    String currentExecutorIdentity() { return currentExecutorIdentity; }
    String nextExecutorIdentity() { return nextExecutorIdentity; }
    boolean currentExecutorPresent() { return currentExecutorPresent; }
    int currentExecutorPosition() { return currentExecutorPosition; }
    String currentExecutorPositionText() { return currentExecutorPositionText; }
    String currentExecutorFailed() { return currentExecutorFailed; }
    String currentExecutorFinished() { return currentExecutorFinished; }
    double playerX() { return playerX; }
    double playerY() { return playerY; }
    double playerZ() { return playerZ; }
    boolean targetDistanceAvailable() { return Double.isFinite(targetDistanceSq); }
    double targetDistanceSq() { return targetDistanceSq; }
}
