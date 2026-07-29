package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.destroy.DestroyBlockTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

//20260728_kpopmodder: Local terrain escape stays bounded so blocked goal recovery cannot become broad mining.
public class LocalTerrainEscapeTask extends Task implements ITaskRequiresGrounded {
    private static final double TIMEOUT_SECONDS = 8.0;
    private static final double MAX_DISTANCE_FROM_ORIGIN = 4.5;
    private static final EscapeCandidateSelector CANDIDATE_SELECTOR = new EscapeCandidateSelector();
    private static final EscapeBlockActionPolicy BLOCK_ACTION_POLICY = new EscapeBlockActionPolicy();

    private final EscapePlan plan;
    private final String parentTaskDebug;
    private final TimerGame timeout = new TimerGame(TIMEOUT_SECONDS);
    private final StateChangeLogger debugLogger = new StateChangeLogger("LocalTerrainEscapeTask");
    private int clearIndex;
    private boolean finished;
    private boolean timedOut;
    private boolean failed;
    private String failureReason;

    public LocalTerrainEscapeTask(EscapePlan plan, String parentTaskDebug) {
        this.plan = plan;
        this.parentTaskDebug = parentTaskDebug;
    }

    public static Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return CANDIDATE_SELECTOR.findPlan(mod, cooldownOrigins);
    }

    public static Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                Set<EscapeCandidateKey> cooldownCandidates) {
        return CANDIDATE_SELECTOR.findPlan(mod, cooldownOrigins, cooldownCandidates);
    }

    public static Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.findPlan(mod, cooldownOrigins, debugLogger);
    }

    public static Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                Set<EscapeCandidateKey> cooldownCandidates,
                                                StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.findPlan(mod, cooldownOrigins, cooldownCandidates, debugLogger);
    }

    public static EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return CANDIDATE_SELECTOR.searchPlan(mod, cooldownOrigins);
    }

    public static EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                    Set<EscapeCandidateKey> cooldownCandidates) {
        return CANDIDATE_SELECTOR.searchPlan(mod, cooldownOrigins, cooldownCandidates);
    }

    public static EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.searchPlan(mod, cooldownOrigins, debugLogger);
    }

    public static EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                    Set<EscapeCandidateKey> cooldownCandidates,
                                                    StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.searchPlan(mod, cooldownOrigins, cooldownCandidates, debugLogger);
    }

    public static EscapePlanSearchResult searchSpiralPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return CANDIDATE_SELECTOR.searchSpiralPlan(mod, cooldownOrigins);
    }

    public static EscapePlanSearchResult searchSpiralPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                          Set<EscapeCandidateKey> cooldownCandidates) {
        return CANDIDATE_SELECTOR.searchSpiralPlan(mod, cooldownOrigins, cooldownCandidates);
    }

    public static EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return CANDIDATE_SELECTOR.searchSidePlan(mod, cooldownOrigins);
    }

    public static EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                        Set<EscapeCandidateKey> cooldownCandidates) {
        return CANDIDATE_SELECTOR.searchSidePlan(mod, cooldownOrigins, cooldownCandidates);
    }

    public static EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                        StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.searchSidePlan(mod, cooldownOrigins, debugLogger);
    }

    public static EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                        Set<EscapeCandidateKey> cooldownCandidates,
                                                        StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.searchSidePlan(mod, cooldownOrigins, cooldownCandidates, debugLogger);
    }

    public static EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return CANDIDATE_SELECTOR.searchHeadroomPlan(mod, cooldownOrigins);
    }

    public static EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                            Set<EscapeCandidateKey> cooldownCandidates) {
        return CANDIDATE_SELECTOR.searchHeadroomPlan(mod, cooldownOrigins, cooldownCandidates);
    }

    public static EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                            StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.searchHeadroomPlan(mod, cooldownOrigins, debugLogger);
    }

    public static EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                            Set<EscapeCandidateKey> cooldownCandidates,
                                                            StateChangeLogger debugLogger) {
        return CANDIDATE_SELECTOR.searchHeadroomPlan(mod, cooldownOrigins, cooldownCandidates, debugLogger);
    }

    public static String describePlanSearchFailure(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return CANDIDATE_SELECTOR.describePlanSearchFailure(mod, cooldownOrigins);
    }

    public static String describePlanSearchFailure(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                   Set<EscapeCandidateKey> cooldownCandidates) {
        return CANDIDATE_SELECTOR.describePlanSearchFailure(mod, cooldownOrigins, cooldownCandidates);
    }

    @Override
    protected void onStart() {
        timeout.reset();
        clearIndex = 0;
        finished = false;
        timedOut = false;
        failed = false;
        failureReason = "none";
        debugLogger.event("start: " + describePlan()
                + ", parentTask=" + parentTaskDebug
                + ", timeoutSeconds=" + TIMEOUT_SECONDS);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (mod.getPlayer() == null) {
            finished = true;
            debugLogger.event("finished: player unavailable, " + describePlan());
            return null;
        }
        if (timeout.elapsed()) {
            timedOut = true;
            failed = true;
            failureReason = "timeout";
            finished = true;
            debugLogger.event("timed out: " + describePlan());
            return null;
        }
        if (!mod.getPlayer().getPos().isInRange(WorldHelper.toVec3d(plan.getOrigin()), MAX_DISTANCE_FROM_ORIGIN)) {
            finished = true;
            debugLogger.event("finished: left local recovery area, " + describePlan()
                    + ", player=" + formatVec(mod.getPlayer().getPos()));
            return null;
        }

        EscapeBlockActionPolicy.ClearanceDecision clearanceDecision =
                BLOCK_ACTION_POLICY.planNextClearance(mod, plan, clearIndex);
        logSkippedClearBlocks(mod, clearIndex, clearanceDecision.getClearIndex());
        clearIndex = clearanceDecision.getClearIndex();

        if (clearanceDecision.isRouteCleared()) {
            finished = true;
            debugLogger.event("finished: route cleared, " + describePlan());
            return null;
        }

        BlockPos target = clearanceDecision.getTarget();
        if (clearanceDecision.isTargetUnsafe()) {
            failed = true;
            failureReason = "target unsafe: target=" + target.toShortString()
                    + ", reason=" + BLOCK_ACTION_POLICY.describeBreakSafety(mod, target);
            finished = true;
            debugLogger.event("finished: " + failureReason
                    + ", " + describePlan());
            return null;
        }

        setDebugState("Clearing terrain " + plan.describeClearProgress(clearIndex));
        debugLogger.state("clear escape block " + target.toShortString(),
                "clearing escape block: index=" + plan.describeClearProgress(clearIndex)
                        + ", target=" + target.toShortString()
                        + ", space=" + BLOCK_ACTION_POLICY.describeEscapeSpace(mod, target)
                        + ", break=" + BLOCK_ACTION_POLICY.describeBreakSafety(mod, target)
                        + ", " + describePlan());
        return new DestroyBlockTask(target);
    }

    private void logSkippedClearBlocks(AltoClef mod, int fromIndex, int toIndex) {
        for (int skippedIndex = fromIndex; skippedIndex < toIndex; skippedIndex++) {
            BlockPos skipped = plan.getBlockToClear(skippedIndex);
            debugLogger.state("skip clear escape block " + skipped.toShortString(),
                    "skip already clear escape block: index=" + plan.describeClearProgress(skippedIndex)
                            + ", target=" + skipped.toShortString()
                            + ", reason=" + BLOCK_ACTION_POLICY.describeEscapeSpace(mod, skipped)
                            + ", " + describePlan());
        }
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    public boolean didTimeOut() {
        return timedOut;
    }

    public boolean didFail() {
        return failed;
    }

    public String describeFailureReason() {
        return failureReason;
    }

    public BlockPos getOrigin() {
        return plan.getOrigin();
    }

    public EscapePlan getPlan() {
        return plan;
    }

    public String describePlan() {
        return plan.describe();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof LocalTerrainEscapeTask task) {
            return plan.equals(task.plan);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Local terrain escape " + plan.getKind() + " " + plan.getDirection().getName();
    }

    private static String formatVec(Vec3d vec) {
        if (vec == null) {
            return "none";
        }
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }

}
