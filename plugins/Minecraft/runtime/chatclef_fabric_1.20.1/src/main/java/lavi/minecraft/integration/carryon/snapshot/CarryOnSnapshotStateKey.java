package lavi.minecraft.integration.carryon.snapshot;

import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.CarryOnSnapshot;

//20260805_kpopmodder: Split Carry On snapshot key formatting from the diagnostic state holder.
public final class CarryOnSnapshotStateKey {
    private CarryOnSnapshotStateKey() {
    }

    public static String from(CarryOnSnapshot snapshot) {
        return value(snapshot.eventName())
                + "|" + value(snapshot.targetId())
                + "|" + value(snapshot.targetPosition())
                + "|" + value(snapshot.currentChain())
                + "|" + value(snapshot.taskChain())
                + "|" + value(snapshot.taskRunnerActive())
                + "|" + value(snapshot.userTaskChainActive())
                + "|" + value(snapshot.paused())
                + "|" + value(snapshot.chatClefEnabled())
                + "|" + value(snapshot.playerMode())
                + "|" + value(snapshot.playerPosition())
                + "|" + value(snapshot.playerVelocity())
                + "|" + value(snapshot.lookRotation())
                + "|" + value(snapshot.playerPoseState())
                + "|" + value(snapshot.rightClickState())
                + "|" + value(snapshot.sneakState())
                + "|" + value(snapshot.leftClickState())
                + "|" + value(snapshot.movementInputState())
                + "|" + value(snapshot.baritonePathing())
                + "|" + value(snapshot.customGoalOwner())
                + "|" + value(snapshot.breakingBlockState())
                + "|" + value(snapshot.crosshairType())
                + "|" + value(snapshot.crosshairTarget())
                + "|" + value(snapshot.crosshairBlockId())
                + "|" + stateName(snapshot.stateBefore())
                + "|" + stateName(snapshot.stateAfter())
                + "|" + carriedBlockId(snapshot.stateBefore())
                + "|" + carriedBlockId(snapshot.stateAfter())
                + "|" + carriedBlockDescription(snapshot.stateBefore())
                + "|" + carriedBlockDescription(snapshot.stateAfter())
                + "|" + carriedBlockState(snapshot.stateBefore())
                + "|" + carriedBlockState(snapshot.stateAfter())
                + "|" + exceptionName(snapshot.stateBefore())
                + "|" + exceptionName(snapshot.stateAfter())
                + "|" + value(snapshot.clickResult())
                + "|" + value(snapshot.screenName())
                + "|" + value(snapshot.screenHandlerName())
                + "|" + value(snapshot.screenHandlerSyncId())
                + "|" + value(snapshot.cursorStack())
                + "|" + value(snapshot.selectedHotbarSlot())
                + "|" + snapshot.terminalReason();
    }

    private static String stateName(CarryOnObservation observation) {
        return observation == null ? "unavailable" : String.valueOf(observation.state());
    }

    private static String exceptionName(CarryOnObservation observation) {
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

    private static String value(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }
}
