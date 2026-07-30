package lavi.minecraft.integration.carryon;

import net.minecraft.client.MinecraftClient;

//20260730_kpopmodder: Model a bounded Carry On diagnostic session owned by a LAVI wrapper or parent Task.
public final class CarryOnDiagnosticSession {
    private final long taskInstanceId;
    private final long operationId;
    private final CarryOnOperationType operationType;
    private final CarryOnTransition expectedTransition;
    private final String childTask;
    private final String targetType;
    private final String targetId;
    private final String targetPosition;
    private final ReflectiveCarryOnStateReader stateReader;
    private final CarryOnDiagnosticSampler sampler = new CarryOnDiagnosticSampler();
    private int attemptCount;
    private int elapsedTicks;

    private CarryOnDiagnosticSession(long taskInstanceId,
                                     long operationId,
                                     CarryOnOperationType operationType,
                                     CarryOnTransition expectedTransition,
                                     String childTask,
                                     String targetType,
                                     String targetId,
                                     String targetPosition,
                                     ReflectiveCarryOnStateReader stateReader) {
        this.taskInstanceId = taskInstanceId;
        this.operationId = operationId;
        this.operationType = operationType;
        this.expectedTransition = expectedTransition;
        this.childTask = childTask;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetPosition = targetPosition;
        this.stateReader = stateReader;
    }

    public static CarryOnDiagnosticSession start(long taskInstanceId,
                                                 long operationId,
                                                 CarryOnOperationType operationType,
                                                 CarryOnTransition expectedTransition,
                                                 String childTask,
                                                 String targetType,
                                                 String targetId,
                                                 String targetPosition,
                                                 ReflectiveCarryOnStateReader stateReader) {
        CarryOnDiagnosticSession session = new CarryOnDiagnosticSession(
                taskInstanceId,
                operationId,
                operationType,
                expectedTransition,
                childTask,
                targetType,
                targetId,
                targetPosition,
                stateReader
        );
        session.logEntry();
        return session;
    }

    public long operationId() {
        return operationId;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public void tick() {
        advanceTicks(1);
    }

    public void advanceTicks(int ticks) {
        elapsedTicks += Math.max(1, ticks);
    }

    public CarryOnObservation observe() {
        MinecraftClient client = MinecraftClient.getInstance();
        return stateReader.observe(client == null ? null : client.player);
    }

    public void logClickAttempt(CarryOnObservation before, CarryOnObservation after, String clickResult) {
        attemptCount++;
        log("click_attempt", before, after, clickResult, true, CarryOnTerminalReason.UNAVAILABLE);
    }

    public void logState(CarryOnObservation before, CarryOnObservation after, String clickResult) {
        log("state", before, after, clickResult, false, CarryOnTerminalReason.UNAVAILABLE);
    }

    public void logHeartbeat(CarryOnObservation before, CarryOnObservation after, String clickResult) {
        log("heartbeat", before, after, clickResult, false, CarryOnTerminalReason.UNAVAILABLE);
    }

    public void logTerminal(CarryOnTerminalReason terminalReason, String clickResult) {
        CarryOnObservation observation = observe();
        log("terminal", observation, observation, clickResult, false, terminalReason);
    }

    public void logTerminal(CarryOnObservation before, CarryOnObservation after, CarryOnTerminalReason terminalReason, String clickResult) {
        log("terminal", before, after, clickResult, false, terminalReason);
    }

    private void logEntry() {
        CarryOnObservation observation = observe();
        log("entry", observation, observation, "unavailable", false, CarryOnTerminalReason.UNAVAILABLE);
    }

    private void log(String eventName,
                     CarryOnObservation stateBefore,
                     CarryOnObservation stateAfter,
                     String clickResult,
                     boolean clickAttempt,
                     CarryOnTerminalReason terminalReason) {
        CarryOnSnapshot snapshot = CarryOnSnapshotCollector.collect(
                taskInstanceId,
                operationId,
                eventName,
                operationType,
                expectedTransition,
                childTask,
                targetType,
                targetId,
                targetPosition,
                stateBefore,
                stateAfter,
                clickResult,
                attemptCount,
                elapsedTicks,
                terminalReason
        );
        if (sampler.shouldLog(snapshot, clickAttempt)) {
            CarryOnDiagnosticLogger.log(snapshot);
        }
    }
}
