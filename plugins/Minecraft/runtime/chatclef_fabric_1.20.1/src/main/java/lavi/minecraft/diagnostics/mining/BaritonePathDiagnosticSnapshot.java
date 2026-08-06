package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePlannerDiagnosticSnapshot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260806_kpopmodder: Keep Baritone path state reads isolated from mining diagnostic event emission.
public final class BaritonePathDiagnosticSnapshot {
    final String baritonePathing;
    final String customGoalActive;
    final String pathPresent;
    final String currentMovementPresent;
    final String ticksRemainingInSegment;
    final String pathMovementCount;
    final String safeToCancel;
    final String goalType;
    final String goalSummary;
    final String goalMatchesTarget;
    final String pathingBehaviorClass;
    final String customGoalProcessClass;
    final String pathSummary;
    final String currentMovementSummary;
    final String activeProcessOwner;
    final String calculationState;
    final String existingFailureReason;
    final BaritonePlannerDiagnosticSnapshot plannerSnapshot;

    private BaritonePathDiagnosticSnapshot(String baritonePathing,
                                           String customGoalActive,
                                           String pathPresent,
                                           String currentMovementPresent,
                                           String ticksRemainingInSegment,
                                           String pathMovementCount,
                                           String safeToCancel,
                                           String goalType,
                                           String goalSummary,
                                           String goalMatchesTarget,
                                           String pathingBehaviorClass,
                                           String customGoalProcessClass,
                                           String pathSummary,
                                           String currentMovementSummary,
                                           String activeProcessOwner,
                                           String calculationState,
                                           String existingFailureReason,
                                           BaritonePlannerDiagnosticSnapshot plannerSnapshot) {
        this.baritonePathing = baritonePathing;
        this.customGoalActive = customGoalActive;
        this.pathPresent = pathPresent;
        this.currentMovementPresent = currentMovementPresent;
        this.ticksRemainingInSegment = ticksRemainingInSegment;
        this.pathMovementCount = pathMovementCount;
        this.safeToCancel = safeToCancel;
        this.goalType = goalType;
        this.goalSummary = goalSummary;
        this.goalMatchesTarget = goalMatchesTarget;
        this.pathingBehaviorClass = pathingBehaviorClass;
        this.customGoalProcessClass = customGoalProcessClass;
        this.pathSummary = pathSummary;
        this.currentMovementSummary = currentMovementSummary;
        this.activeProcessOwner = activeProcessOwner;
        this.calculationState = calculationState;
        this.existingFailureReason = existingFailureReason;
        this.plannerSnapshot = plannerSnapshot;
    }

    static BaritonePathDiagnosticSnapshot capture(AltoClef mod, BlockPos target, Object goal, String goalMatchesTarget) {
        String baritonePathing = ChatClefDiagnostics.safeValue(() ->
                mod != null && mod.getClientBaritone().getPathingBehavior().isPathing());
        String customGoalActive = ChatClefDiagnostics.safeValue(() ->
                mod != null && mod.getClientBaritone().getCustomGoalProcess().isActive());
        String pathPresent = ChatClefDiagnostics.safeValue(() ->
                mod != null && mod.getClientBaritone().getPathingBehavior().getPath().isPresent());
        String currentMovementPresent = ChatClefDiagnostics.safeValue(() ->
                mod != null && mod.getClientBaritone().getPathingBehavior().getCurrent() != null);
        String ticksRemainingInSegment = ChatClefDiagnostics.safeValue(() ->
                mod == null ? "unavailable" : mod.getClientBaritone().getPathingBehavior().ticksRemainingInSegment().map(Object::toString).orElse("empty"));
        String pathMovementCount = ChatClefDiagnostics.safeValue(() ->
                mod == null ? "unavailable" : mod.getClientBaritone().getPathingBehavior().getPath().map(path -> path.movements().size()).orElse(0));
        String safeToCancel = ChatClefDiagnostics.safeValue(() ->
                mod != null && mod.getClientBaritone().getPathingBehavior().isSafeToCancel());
        String pathingBehaviorClass = ChatClefDiagnostics.safeValue(() ->
                mod == null ? "unavailable" : ChatClefDiagnostics.className(mod.getClientBaritone().getPathingBehavior()));
        String customGoalProcessClass = ChatClefDiagnostics.safeValue(() ->
                mod == null ? "unavailable" : ChatClefDiagnostics.className(mod.getClientBaritone().getCustomGoalProcess()));
        String pathSummary = ChatClefDiagnostics.safeValue(() -> {
            if (mod == null) {
                return "unavailable";
            }
            return mod.getClientBaritone().getPathingBehavior().getPath()
                    .map(path -> ChatClefDiagnostics.className(path)
                            + "#"
                            + Integer.toHexString(System.identityHashCode(path))
                            + ",movements="
                            + path.movements().size())
                    .orElse("empty");
        });
        String currentMovementSummary = ChatClefDiagnostics.safeValue(() -> {
            if (mod == null) {
                return "unavailable";
            }
            Object currentMovement = mod.getClientBaritone().getPathingBehavior().getCurrent();
            if (currentMovement == null) {
                return "none";
            }
            return ChatClefDiagnostics.className(currentMovement)
                    + "#"
                    + Integer.toHexString(System.identityHashCode(currentMovement))
                    + ":"
                    + currentMovement;
        });
        String calculationState = derivePublicCalculationState(
                customGoalActive,
                baritonePathing,
                pathPresent,
                currentMovementPresent,
                ticksRemainingInSegment
        );
        BaritonePlannerDiagnosticSnapshot plannerSnapshot = BaritonePlannerDiagnosticSnapshot.capture(mod);
        return new BaritonePathDiagnosticSnapshot(
                baritonePathing,
                customGoalActive,
                pathPresent,
                currentMovementPresent,
                ticksRemainingInSegment,
                pathMovementCount,
                safeToCancel,
                goal == null ? "unavailable" : ChatClefDiagnostics.className(goal),
                goal == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> goal),
                goalMatchesTarget == null ? "unavailable" : goalMatchesTarget,
                pathingBehaviorClass,
                customGoalProcessClass,
                pathSummary,
                currentMovementSummary,
                deriveActiveProcessOwner(customGoalActive, baritonePathing, pathPresent),
                calculationState,
                deriveExistingFailureReason(calculationState),
                plannerSnapshot
        );
    }

    Object[] fields() {
        return MiningDiagnosticEmitter.merge(new Object[]{
                "baritonePathing", baritonePathing,
                "customGoalActive", customGoalActive,
                "pathPresent", pathPresent,
                "currentMovementPresent", currentMovementPresent,
                "ticksRemainingInSegment", ticksRemainingInSegment,
                "pathMovementCount", pathMovementCount,
                "safeToCancel", safeToCancel,
                "goalType", goalType,
                "goalSummary", goalSummary,
                "goalMatchesTarget", goalMatchesTarget,
                "pathingBehaviorClass", pathingBehaviorClass,
                "customGoalProcessClass", customGoalProcessClass,
                "pathSummary", pathSummary,
                "currentMovementSummary", currentMovementSummary,
                "activeProcessOwner", activeProcessOwner,
                "calculationState", calculationState,
                "existingFailureReason", existingFailureReason
        }, plannerSnapshot.fields());
    }

    Object[] fields(String suffix) {
        String normalizedSuffix = suffix == null ? "" : suffix;
        return MiningDiagnosticEmitter.merge(new Object[]{
                "baritonePathing" + normalizedSuffix, baritonePathing,
                "customGoalActive" + normalizedSuffix, customGoalActive,
                "pathPresent" + normalizedSuffix, pathPresent,
                "currentMovementPresent" + normalizedSuffix, currentMovementPresent,
                "ticksRemainingInSegment" + normalizedSuffix, ticksRemainingInSegment,
                "pathMovementCount" + normalizedSuffix, pathMovementCount,
                "safeToCancel" + normalizedSuffix, safeToCancel,
                "goalType" + normalizedSuffix, goalType,
                "goalSummary" + normalizedSuffix, goalSummary,
                "goalMatchesTarget" + normalizedSuffix, goalMatchesTarget,
                "pathingBehaviorClass" + normalizedSuffix, pathingBehaviorClass,
                "customGoalProcessClass" + normalizedSuffix, customGoalProcessClass,
                "pathSummary" + normalizedSuffix, pathSummary,
                "currentMovementSummary" + normalizedSuffix, currentMovementSummary,
                "activeProcessOwner" + normalizedSuffix, activeProcessOwner,
                "calculationState" + normalizedSuffix, calculationState,
                "existingFailureReason" + normalizedSuffix, existingFailureReason
        }, plannerSnapshot.fields(normalizedSuffix));
    }

    private static String derivePublicCalculationState(String customGoalActive,
                                                       String baritonePathing,
                                                       String pathPresent,
                                                       String currentMovementPresent,
                                                       String ticksRemainingInSegment) {
        if (isObservationError(customGoalActive, baritonePathing, pathPresent, currentMovementPresent, ticksRemainingInSegment)) {
            return "OBSERVATION_FAILED_PUBLIC_SNAPSHOT";
        }
        if (isTrue(baritonePathing)) {
            return "PATHING_ACTIVE_PUBLIC_SNAPSHOT";
        }
        if (isTrue(pathPresent) && isTrue(currentMovementPresent)) {
            return "PATH_PRESENT_WITH_MOVEMENT_PUBLIC_SNAPSHOT";
        }
        if (isTrue(pathPresent)) {
            return "PATH_PRESENT_IDLE_PUBLIC_SNAPSHOT";
        }
        if (isTrue(customGoalActive) && isFalse(pathPresent)) {
            return "GOAL_ACTIVE_PATH_ABSENT_PUBLIC_SNAPSHOT";
        }
        if (isFalse(customGoalActive) && isFalse(pathPresent)) {
            return "NO_GOAL_PATH_PUBLIC_SNAPSHOT";
        }
        return "UNKNOWN_PUBLIC_SNAPSHOT";
    }

    private static String deriveExistingFailureReason(String calculationState) {
        if ("GOAL_ACTIVE_PATH_ABSENT_PUBLIC_SNAPSHOT".equals(calculationState)) {
            return "not_exposed_by_public_api_goal_active_without_path";
        }
        if ("OBSERVATION_FAILED_PUBLIC_SNAPSHOT".equals(calculationState)) {
            return "public_snapshot_observation_failed";
        }
        if ("NO_GOAL_PATH_PUBLIC_SNAPSHOT".equals(calculationState)) {
            return "no_active_goal_or_path_observed";
        }
        return "none_observed";
    }

    private static String deriveActiveProcessOwner(String customGoalActive,
                                                   String baritonePathing,
                                                   String pathPresent) {
        if (isTrue(customGoalActive)) {
            return "custom_goal_process_active_owner_not_exposed";
        }
        if (isTrue(baritonePathing) || isTrue(pathPresent)) {
            return "pathing_behavior_active_owner_not_exposed";
        }
        if (isFalse(customGoalActive) && isFalse(pathPresent)) {
            return "no_active_public_process_observed";
        }
        return "owner_not_exposed";
    }

    private static boolean isTrue(String value) {
        return "true".equals(value);
    }

    private static boolean isFalse(String value) {
        return "false".equals(value);
    }

    private static boolean isObservationError(String... values) {
        for (String value : values) {
            if (value != null && (value.startsWith("error=") || value.startsWith("exception="))) {
                return true;
            }
        }
        return false;
    }
}
