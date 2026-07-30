package lavi.minecraft.integration.carryon;

//20260730_kpopmodder: Store Carry On diagnostic state observed by a LAVI-owned caller.
public final class CarryOnSnapshot {
    private final long taskInstanceId;
    private final long operationId;
    private final String eventName;
    private final CarryOnOperationType operationType;
    private final CarryOnTransition expectedTransition;
    private final long gameTick;
    private final String threadName;
    private final String topLevelTask;
    private final String childTask;
    private final String targetType;
    private final String targetId;
    private final String targetPosition;
    private final String dimension;
    private final String currentChain;
    private final String taskChain;
    private final String taskRunnerActive;
    private final String userTaskChainActive;
    private final String paused;
    private final String chatClefEnabled;
    private final String playerMode;
    private final String playerPosition;
    private final String playerVelocity;
    private final String lookRotation;
    private final String playerPoseState;
    private final String rightClickState;
    private final String sneakState;
    private final String leftClickState;
    private final String movementInputState;
    private final String baritonePathing;
    private final String customGoalOwner;
    private final String breakingBlockState;
    private final String crosshairType;
    private final String crosshairTarget;
    private final String crosshairBlockId;
    private final CarryOnObservation stateBefore;
    private final CarryOnObservation stateAfter;
    private final String clickResult;
    private final int attemptCount;
    private final int elapsedTicks;
    private final String screenName;
    private final String screenHandlerName;
    private final String screenHandlerSyncId;
    private final String cursorStack;
    private final String selectedHotbarSlot;
    private final String mainHandItem;
    private final String offHandItem;
    private final CarryOnTerminalReason terminalReason;

    public CarryOnSnapshot(long taskInstanceId,
                           long operationId,
                           String eventName,
                           CarryOnOperationType operationType,
                           CarryOnTransition expectedTransition,
                           long gameTick,
                           String threadName,
                           String topLevelTask,
                           String childTask,
                           String targetType,
                           String targetId,
                           String targetPosition,
                           String dimension,
                           String currentChain,
                           String taskChain,
                           String taskRunnerActive,
                           String userTaskChainActive,
                           String paused,
                           String chatClefEnabled,
                           String playerMode,
                           String playerPosition,
                           String playerVelocity,
                           String lookRotation,
                           String playerPoseState,
                           String rightClickState,
                           String sneakState,
                           String leftClickState,
                           String movementInputState,
                           String baritonePathing,
                           String customGoalOwner,
                           String breakingBlockState,
                           String crosshairType,
                           String crosshairTarget,
                           String crosshairBlockId,
                           CarryOnObservation stateBefore,
                           CarryOnObservation stateAfter,
                           String clickResult,
                           int attemptCount,
                           int elapsedTicks,
                           String screenName,
                           String screenHandlerName,
                           String screenHandlerSyncId,
                           String cursorStack,
                           String selectedHotbarSlot,
                           String mainHandItem,
                           String offHandItem,
                           CarryOnTerminalReason terminalReason) {
        this.taskInstanceId = taskInstanceId;
        this.operationId = operationId;
        this.eventName = eventName;
        this.operationType = operationType;
        this.expectedTransition = expectedTransition;
        this.gameTick = gameTick;
        this.threadName = threadName;
        this.topLevelTask = topLevelTask;
        this.childTask = childTask;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetPosition = targetPosition;
        this.dimension = dimension;
        this.currentChain = currentChain;
        this.taskChain = taskChain;
        this.taskRunnerActive = taskRunnerActive;
        this.userTaskChainActive = userTaskChainActive;
        this.paused = paused;
        this.chatClefEnabled = chatClefEnabled;
        this.playerMode = playerMode;
        this.playerPosition = playerPosition;
        this.playerVelocity = playerVelocity;
        this.lookRotation = lookRotation;
        this.playerPoseState = playerPoseState;
        this.rightClickState = rightClickState;
        this.sneakState = sneakState;
        this.leftClickState = leftClickState;
        this.movementInputState = movementInputState;
        this.baritonePathing = baritonePathing;
        this.customGoalOwner = customGoalOwner;
        this.breakingBlockState = breakingBlockState;
        this.crosshairType = crosshairType;
        this.crosshairTarget = crosshairTarget;
        this.crosshairBlockId = crosshairBlockId;
        this.stateBefore = stateBefore;
        this.stateAfter = stateAfter;
        this.clickResult = clickResult;
        this.attemptCount = attemptCount;
        this.elapsedTicks = elapsedTicks;
        this.screenName = screenName;
        this.screenHandlerName = screenHandlerName;
        this.screenHandlerSyncId = screenHandlerSyncId;
        this.cursorStack = cursorStack;
        this.selectedHotbarSlot = selectedHotbarSlot;
        this.mainHandItem = mainHandItem;
        this.offHandItem = offHandItem;
        this.terminalReason = terminalReason;
    }

    public long taskInstanceId() {
        return taskInstanceId;
    }

    public long operationId() {
        return operationId;
    }

    public String eventName() {
        return eventName;
    }

    public CarryOnOperationType operationType() {
        return operationType;
    }

    public CarryOnTransition expectedTransition() {
        return expectedTransition;
    }

    public long gameTick() {
        return gameTick;
    }

    public String threadName() {
        return threadName;
    }

    public String topLevelTask() {
        return topLevelTask;
    }

    public String childTask() {
        return childTask;
    }

    public String targetType() {
        return targetType;
    }

    public String targetId() {
        return targetId;
    }

    public String targetPosition() {
        return targetPosition;
    }

    public String dimension() {
        return dimension;
    }

    public String currentChain() {
        return currentChain;
    }

    public String taskChain() {
        return taskChain;
    }

    public String taskRunnerActive() {
        return taskRunnerActive;
    }

    public String userTaskChainActive() {
        return userTaskChainActive;
    }

    public String paused() {
        return paused;
    }

    public String chatClefEnabled() {
        return chatClefEnabled;
    }

    public String playerMode() {
        return playerMode;
    }

    public String playerPosition() {
        return playerPosition;
    }

    public String playerVelocity() {
        return playerVelocity;
    }

    public String lookRotation() {
        return lookRotation;
    }

    public String playerPoseState() {
        return playerPoseState;
    }

    public String rightClickState() {
        return rightClickState;
    }

    public String sneakState() {
        return sneakState;
    }

    public String leftClickState() {
        return leftClickState;
    }

    public String movementInputState() {
        return movementInputState;
    }

    public String baritonePathing() {
        return baritonePathing;
    }

    public String customGoalOwner() {
        return customGoalOwner;
    }

    public String breakingBlockState() {
        return breakingBlockState;
    }

    public String crosshairType() {
        return crosshairType;
    }

    public String crosshairTarget() {
        return crosshairTarget;
    }

    public String crosshairBlockId() {
        return crosshairBlockId;
    }

    public CarryOnObservation stateBefore() {
        return stateBefore;
    }

    public CarryOnObservation stateAfter() {
        return stateAfter;
    }

    public String clickResult() {
        return clickResult;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public String screenName() {
        return screenName;
    }

    public String screenHandlerName() {
        return screenHandlerName;
    }

    public String screenHandlerSyncId() {
        return screenHandlerSyncId;
    }

    public String cursorStack() {
        return cursorStack;
    }

    public String selectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public String mainHandItem() {
        return mainHandItem;
    }

    public String offHandItem() {
        return offHandItem;
    }

    public CarryOnTerminalReason terminalReason() {
        return terminalReason;
    }

    public boolean terminal() {
        return terminalReason != CarryOnTerminalReason.UNAVAILABLE;
    }

    public String stateKey() {
        return value(eventName)
                + "|" + value(targetId)
                + "|" + value(targetPosition)
                + "|" + value(currentChain)
                + "|" + value(taskChain)
                + "|" + value(taskRunnerActive)
                + "|" + value(userTaskChainActive)
                + "|" + value(paused)
                + "|" + value(chatClefEnabled)
                + "|" + value(playerMode)
                + "|" + value(playerPosition)
                + "|" + value(playerVelocity)
                + "|" + value(lookRotation)
                + "|" + value(playerPoseState)
                + "|" + value(rightClickState)
                + "|" + value(sneakState)
                + "|" + value(leftClickState)
                + "|" + value(movementInputState)
                + "|" + value(baritonePathing)
                + "|" + value(customGoalOwner)
                + "|" + value(breakingBlockState)
                + "|" + value(crosshairType)
                + "|" + value(crosshairTarget)
                + "|" + value(crosshairBlockId)
                + "|" + stateName(stateBefore)
                + "|" + stateName(stateAfter)
                + "|" + exceptionName(stateBefore)
                + "|" + exceptionName(stateAfter)
                + "|" + value(clickResult)
                + "|" + value(screenName)
                + "|" + value(screenHandlerName)
                + "|" + value(screenHandlerSyncId)
                + "|" + value(cursorStack)
                + "|" + value(selectedHotbarSlot)
                + "|" + terminalReason;
    }

    private static String stateName(CarryOnObservation observation) {
        return observation == null ? "unavailable" : String.valueOf(observation.state());
    }

    private static String exceptionName(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.exceptionType();
    }

    private static String value(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }
}
