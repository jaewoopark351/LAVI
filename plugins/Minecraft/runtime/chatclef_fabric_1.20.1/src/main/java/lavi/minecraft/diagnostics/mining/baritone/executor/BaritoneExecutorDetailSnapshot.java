package lavi.minecraft.diagnostics.mining.baritone.executor;

//20260830_kpopmodder: Store only permit-gated executor event details.
final class BaritoneExecutorDetailSnapshot {
    final String safeToCancel;
    final String estimatedTicksToGoal;
    final String expectedSegmentStart;
    final String goalType;
    final String goalSummary;
    final String inProgressType;
    final String inProgressSummary;
    final BaritoneExecutorStateSnapshot currentExecutor;
    final BaritoneExecutorStateSnapshot nextExecutor;
    final BaritonePlayerProgressSnapshot player;
    final BaritoneExecutorTargetSnapshot target;

    BaritoneExecutorDetailSnapshot(String safeToCancel,
                                   String estimatedTicksToGoal,
                                   String expectedSegmentStart,
                                   String goalType,
                                   String goalSummary,
                                   String inProgressType,
                                   String inProgressSummary,
                                   BaritoneExecutorStateSnapshot currentExecutor,
                                   BaritoneExecutorStateSnapshot nextExecutor,
                                   BaritonePlayerProgressSnapshot player,
                                   BaritoneExecutorTargetSnapshot target) {
        this.safeToCancel = safeToCancel;
        this.estimatedTicksToGoal = estimatedTicksToGoal;
        this.expectedSegmentStart = expectedSegmentStart;
        this.goalType = goalType;
        this.goalSummary = goalSummary;
        this.inProgressType = inProgressType;
        this.inProgressSummary = inProgressSummary;
        this.currentExecutor = currentExecutor;
        this.nextExecutor = nextExecutor;
        this.player = player;
        this.target = target;
    }
}
