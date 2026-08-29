package lavi.minecraft.diagnostics.container.home.timeout.progress;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.StoreHomeHandlerSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBindingDiagnosticView;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//20260828_kpopmodder: Read only the live state needed to diagnose STORE_HOME candidate progress.
public final class StoreHomeProgressSnapshotReader {
    private static final int MAX_CAPTURE_ERROR_FIELDS = 12;

    private final AutoDepositExactOpenContainerBinding exactBinding;
    private final HomeStorageTransferExecutor transferExecutor;

    public StoreHomeProgressSnapshotReader(
            AutoDepositExactOpenContainerBinding exactBinding,
            HomeStorageTransferExecutor transferExecutor) {
        this.exactBinding = Objects.requireNonNull(exactBinding, "exactBinding");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
    }

    public StoreHomeProgressSnapshot capture(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session) {
        Objects.requireNonNull(candidate, "candidate");
        BlockPos target = candidate.position();
        StoreHomePlayerPositionSnapshot player = capturePlayer(mod, candidate);
        StoreHomeHandlerSnapshot handler = StoreHomeHandlerSnapshot.capture(mod);

        String pathing = Boolean.toString(mod != null
                && mod.getClientBaritone().getPathingBehavior().isPathing());
        String pathPresent = Boolean.toString(mod != null
                && mod.getClientBaritone().getPathingBehavior().getPath().isPresent());
        String calculationPresent = Boolean.toString(mod != null
                && mod.getClientBaritone().getPathingBehavior()
                .getInProgress().isPresent());
        String calculationFailed = Boolean.toString(mod != null
                && mod.getClientBaritone().getPathingBehavior().calcFailedLastTick());
        String customGoalActive = Boolean.toString(mod != null
                && mod.getClientBaritone().getCustomGoalProcess().isActive());
        String pathingGoalType = mod == null
                ? "none"
                : ChatClefDiagnostics.className(
                mod.getClientBaritone().getPathingBehavior().getGoal()
        );
        String customGoalType = mod == null
                ? "none"
                : ChatClefDiagnostics.className(
                mod.getClientBaritone().getCustomGoalProcess().getGoal()
        );
        String goalType = "true".equals(customGoalActive)
                ? customGoalType
                : pathingGoalType;
        String goalMatchesCandidate = goalMatchesCandidate(mod, target);
        String exactBindingMatched = exactBindingMatched(target);
        String executorPending = Boolean.toString(transferExecutor.hasPending());

        Object sessionOrdinal = session == null ? "unavailable" : session.ordinal();
        Object planRevision = session == null
                ? "unavailable"
                : session.plan().revision();
        String sessionPending = session == null
                ? "false"
                : Boolean.toString(session.pendingTransfer().isPresent());

        List<String> errors = new ArrayList<>();
        addUnavailable(errors, "playerPosition", player.captureStatus());
        addUnavailable(errors, "baritonePathingActive", pathing);
        addUnavailable(errors, "baritonePathPresent", pathPresent);
        addUnavailable(errors, "baritoneCalculationPresent", calculationPresent);
        addUnavailable(errors, "baritoneCalculationFailed", calculationFailed);
        addUnavailable(errors, "customGoalActive", customGoalActive);
        addUnavailable(errors, "normalizedGoalType", goalType);
        addUnavailable(errors, "goalMatchesCandidatePosition", goalMatchesCandidate);
        addUnavailable(errors, "exactBindingMatched", exactBindingMatched);
        addUnavailable(errors, "executorPendingTransfer", executorPending);
        if (!"complete".equals(handler.captureStatus())) {
            addError(errors, "handler:" + handler.errorClass());
        }

        return new StoreHomeProgressSnapshot(
                player.position(),
                player.positionText(),
                player.distanceSquared3d(),
                pathing,
                calculationState(
                        calculationPresent,
                        calculationFailed,
                        pathPresent,
                        pathing
                ),
                pathPresent,
                customGoalActive,
                goalType,
                "unavailable_non_opaque_goal_target",
                goalMatchesCandidate,
                handler.screenClass(),
                handler.handlerClass(),
                handler.syncIdValue(),
                exactBindingMatched,
                session == null ? "NO_ACTIVE_SESSION" : "ACTIVE_EXACT_SESSION",
                sessionOrdinal,
                planRevision,
                sessionPending,
                executorPending,
                countUnavailable(
                        pathing,
                        pathPresent,
                        calculationPresent,
                        calculationFailed,
                        customGoalActive,
                        goalType,
                        goalMatchesCandidate
                ),
                errors.isEmpty() ? "complete" : "partial",
                errors.isEmpty() ? "none" : String.join(",", errors)
        );
    }

    public StoreHomePlayerPositionSnapshot capturePlayer(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate) {
        if (mod == null || mod.getPlayer() == null) {
            return StoreHomePlayerPositionSnapshot.unavailable();
        }
        BlockPos target = candidate.position();
        BlockPos position = mod.getPlayer().getBlockPos().toImmutable();
        long dx = (long) position.getX() - target.getX();
        long dy = (long) position.getY() - target.getY();
        long dz = (long) position.getZ() - target.getZ();
        long distanceSquared = dx * dx + dy * dy + dz * dz;
        return new StoreHomePlayerPositionSnapshot(
                position,
                position.toShortString(),
                distanceSquared,
                "complete"
        );
    }

    private String exactBindingMatched(BlockPos target) {
        if (!(exactBinding
                instanceof AutoDepositExactOpenContainerBindingDiagnosticView view)) {
            return "unavailable_non_mutating_view";
        }
        return Boolean.toString(view.peekCurrentExactPositionForDiagnostics()
                .filter(target::equals)
                .isPresent());
    }

    private static String goalMatchesCandidate(AltoClef mod, BlockPos target) {
        if (mod == null) {
            return "unavailable";
        }
        if (mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            var goal = mod.getClientBaritone().getCustomGoalProcess().getGoal();
            return goal == null ? "none" : Boolean.toString(goal.isInGoal(target));
        }
        var goal = mod.getClientBaritone().getPathingBehavior().getGoal();
        return goal == null ? "none" : Boolean.toString(goal.isInGoal(target));
    }

    private static String calculationState(
            String calculationPresent,
            String calculationFailed,
            String pathPresent,
            String pathing) {
        if ("true".equals(calculationFailed)) {
            return "FAILED_LAST_TICK";
        }
        if ("true".equals(calculationPresent)) {
            return "CALCULATION_IN_PROGRESS";
        }
        if ("true".equals(pathPresent) || "true".equals(pathing)) {
            return "PATH_EXECUTING_OR_PRESENT";
        }
        if (isUnavailable(calculationPresent)
                || isUnavailable(calculationFailed)
                || isUnavailable(pathPresent)
                || isUnavailable(pathing)) {
            return "UNAVAILABLE";
        }
        return "NO_VISIBLE_CALCULATION_OR_PATH";
    }

    private static void addUnavailable(
            List<String> errors,
            String field,
            String value) {
        if (isUnavailable(value)) {
            addError(errors, field);
        }
    }

    private static void addError(List<String> errors, String value) {
        if (errors.size() < MAX_CAPTURE_ERROR_FIELDS) {
            errors.add(value);
        }
    }

    private static boolean isUnavailable(String value) {
        return value == null
                || "unavailable".equals(value)
                || value.startsWith("unavailable_");
    }

    private static int countUnavailable(String... values) {
        int count = 0;
        for (String value : values) {
            if (isUnavailable(value) && count < Integer.MAX_VALUE) {
                count++;
            }
        }
        return count;
    }

}
