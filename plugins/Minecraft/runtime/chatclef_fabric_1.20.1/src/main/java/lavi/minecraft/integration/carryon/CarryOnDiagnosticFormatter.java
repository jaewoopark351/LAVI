package lavi.minecraft.integration.carryon;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260730_kpopmodder: Format Carry On diagnostics separately from observation and sampling.
public final class CarryOnDiagnosticFormatter {
    private CarryOnDiagnosticFormatter() {
    }

    public static String format(CarryOnSnapshot snapshot) {
        CarryOnObservation before = snapshot.stateBefore();
        CarryOnObservation after = snapshot.stateAfter();
        return String.format(
                "[LAVI CarryOnDiag] traceId=%s clientTickId=%d eventSequence=%d event=%s taskInstanceId=%d operationId=%d operationType=%s expectedTransition=%s transitionObserved=%s gameTick=%s threadName=%s currentChain=%s topLevelTask=%s childTask=%s taskChain=%s taskRunnerActive=%s userTaskChainActive=%s paused=%s chatClefEnabled=%s playerMode=%s targetType=%s targetId=%s targetPosition=%s playerPosition=%s playerVelocity=%s lookRotation=%s playerPoseState=%s dimension=%s carryOnLoadedBefore=%s carryOnLoadedAfter=%s carryOnVersionBefore=%s carryOnVersionAfter=%s carryStateBefore=%s carryStateAfter=%s carriedBlockIdBefore=%s carriedBlockIdAfter=%s carriedBlockDescriptionBefore=%s carriedBlockDescriptionAfter=%s carriedBlockStateBefore=%s carriedBlockStateAfter=%s carriedBlockExceptionTypeBefore=%s carriedBlockExceptionTypeAfter=%s rightClickState=%s sneakState=%s leftClickState=%s movementInputState=%s baritonePathing=%s customGoalOwner=%s breakingBlockState=%s crosshairType=%s crosshairTarget=%s crosshairBlockId=%s clickResult=%s attemptCount=%d elapsedTicks=%d screenName=%s screenHandlerName=%s screenHandlerSyncId=%s cursorStack=%s selectedHotbarSlot=%s mainHandItem=%s offHandItem=%s terminalReason=%s exceptionTypeBefore=%s exceptionTypeAfter=%s",
                ChatClefDiagnostics.currentTraceId(),
                ChatClefDiagnostics.currentClientTickId(),
                ChatClefDiagnostics.nextEventSequence(),
                snapshot.eventName(),
                snapshot.taskInstanceId(),
                snapshot.operationId(),
                snapshot.operationType(),
                snapshot.expectedTransition(),
                snapshot.expectedTransition().matches(before, after),
                tick(snapshot.gameTick()),
                snapshot.threadName(),
                snapshot.currentChain(),
                snapshot.topLevelTask(),
                snapshot.childTask(),
                snapshot.taskChain(),
                snapshot.taskRunnerActive(),
                snapshot.userTaskChainActive(),
                snapshot.paused(),
                snapshot.chatClefEnabled(),
                snapshot.playerMode(),
                snapshot.targetType(),
                snapshot.targetId(),
                snapshot.targetPosition(),
                snapshot.playerPosition(),
                snapshot.playerVelocity(),
                snapshot.lookRotation(),
                snapshot.playerPoseState(),
                snapshot.dimension(),
                loaded(before),
                loaded(after),
                version(before),
                version(after),
                state(before),
                state(after),
                carriedBlockId(before),
                carriedBlockId(after),
                carriedBlockDescription(before),
                carriedBlockDescription(after),
                carriedBlockState(before),
                carriedBlockState(after),
                carriedBlockExceptionType(before),
                carriedBlockExceptionType(after),
                snapshot.rightClickState(),
                snapshot.sneakState(),
                snapshot.leftClickState(),
                snapshot.movementInputState(),
                snapshot.baritonePathing(),
                snapshot.customGoalOwner(),
                snapshot.breakingBlockState(),
                snapshot.crosshairType(),
                snapshot.crosshairTarget(),
                snapshot.crosshairBlockId(),
                snapshot.clickResult(),
                snapshot.attemptCount(),
                snapshot.elapsedTicks(),
                snapshot.screenName(),
                snapshot.screenHandlerName(),
                snapshot.screenHandlerSyncId(),
                snapshot.cursorStack(),
                snapshot.selectedHotbarSlot(),
                snapshot.mainHandItem(),
                snapshot.offHandItem(),
                snapshot.terminalReason(),
                exceptionType(before),
                exceptionType(after)
        );
    }

    private static String tick(long gameTick) {
        return gameTick < 0 ? "unavailable" : Long.toString(gameTick);
    }

    private static String loaded(CarryOnObservation observation) {
        return observation == null ? "unavailable" : Boolean.toString(observation.loaded());
    }

    private static String version(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.version();
    }

    private static String state(CarryOnObservation observation) {
        return observation == null ? "unavailable" : String.valueOf(observation.state());
    }

    private static String exceptionType(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.exceptionType();
    }

    private static String carriedBlockId(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockId();
    }

    private static String carriedBlockDescription(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockDescription();
    }

    private static String carriedBlockState(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockState();
    }

    private static String carriedBlockExceptionType(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockExceptionType();
    }
}
