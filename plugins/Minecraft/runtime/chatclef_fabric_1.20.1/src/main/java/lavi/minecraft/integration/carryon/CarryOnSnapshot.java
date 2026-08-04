package lavi.minecraft.integration.carryon;

import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnInputSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnOperationSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnPlayerSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnScreenSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnSnapshotStateKey;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTargetSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshot;

//20260730_kpopmodder: Store Carry On diagnostic state observed by a LAVI-owned caller.
public final class CarryOnSnapshot {
    private final CarryOnOperationSnapshot operation;
    private final CarryOnTaskSnapshot task;
    private final CarryOnTargetSnapshot target;
    private final CarryOnPlayerSnapshot player;
    private final CarryOnInputSnapshot input;
    private final CarryOnBaritoneSnapshot baritone;
    private final CarryOnScreenSnapshot screen;

    public CarryOnSnapshot(CarryOnOperationSnapshot operation,
                           CarryOnTaskSnapshot task,
                           CarryOnTargetSnapshot target,
                           CarryOnPlayerSnapshot player,
                           CarryOnInputSnapshot input,
                           CarryOnBaritoneSnapshot baritone,
                           CarryOnScreenSnapshot screen) {
        this.operation = operation;
        this.task = task;
        this.target = target;
        this.player = player;
        this.input = input;
        this.baritone = baritone;
        this.screen = screen;
    }

    public long taskInstanceId() {
        return operation.taskInstanceId();
    }

    public long operationId() {
        return operation.operationId();
    }

    public String eventName() {
        return operation.eventName();
    }

    public CarryOnOperationType operationType() {
        return operation.operationType();
    }

    public CarryOnTransition expectedTransition() {
        return operation.expectedTransition();
    }

    public long gameTick() {
        return operation.gameTick();
    }

    public String threadName() {
        return operation.threadName();
    }

    public String topLevelTask() {
        return task.topLevelTask();
    }

    public String childTask() {
        return task.childTask();
    }

    public String targetType() {
        return target.targetType();
    }

    public String targetId() {
        return target.targetId();
    }

    public String targetPosition() {
        return target.targetPosition();
    }

    public String dimension() {
        return target.dimension();
    }

    public String currentChain() {
        return task.currentChain();
    }

    public String taskChain() {
        return task.taskChain();
    }

    public String taskRunnerActive() {
        return task.taskRunnerActive();
    }

    public String userTaskChainActive() {
        return task.userTaskChainActive();
    }

    public String paused() {
        return task.paused();
    }

    public String chatClefEnabled() {
        return task.chatClefEnabled();
    }

    public String playerMode() {
        return task.playerMode();
    }

    public String playerPosition() {
        return player.playerPosition();
    }

    public String playerVelocity() {
        return player.playerVelocity();
    }

    public String lookRotation() {
        return player.lookRotation();
    }

    public String playerPoseState() {
        return player.playerPoseState();
    }

    public String rightClickState() {
        return input.rightClickState();
    }

    public String sneakState() {
        return input.sneakState();
    }

    public String leftClickState() {
        return input.leftClickState();
    }

    public String movementInputState() {
        return input.movementInputState();
    }

    public String baritonePathing() {
        return baritone.baritonePathing();
    }

    public String customGoalOwner() {
        return baritone.customGoalOwner();
    }

    public String breakingBlockState() {
        return baritone.breakingBlockState();
    }

    public String crosshairType() {
        return target.crosshairType();
    }

    public String crosshairTarget() {
        return target.crosshairTarget();
    }

    public String crosshairBlockId() {
        return target.crosshairBlockId();
    }

    public CarryOnObservation stateBefore() {
        return operation.stateBefore();
    }

    public CarryOnObservation stateAfter() {
        return operation.stateAfter();
    }

    public String clickResult() {
        return operation.clickResult();
    }

    public int attemptCount() {
        return operation.attemptCount();
    }

    public int elapsedTicks() {
        return operation.elapsedTicks();
    }

    public String screenName() {
        return screen.screenName();
    }

    public String screenHandlerName() {
        return screen.screenHandlerName();
    }

    public String screenHandlerSyncId() {
        return screen.screenHandlerSyncId();
    }

    public String cursorStack() {
        return screen.cursorStack();
    }

    public String selectedHotbarSlot() {
        return player.selectedHotbarSlot();
    }

    public String mainHandItem() {
        return player.mainHandItem();
    }

    public String offHandItem() {
        return player.offHandItem();
    }

    public CarryOnTerminalReason terminalReason() {
        return operation.terminalReason();
    }

    public boolean terminal() {
        return terminalReason() != CarryOnTerminalReason.UNAVAILABLE;
    }

    public String stateKey() {
        return CarryOnSnapshotStateKey.from(this);
    }
}
