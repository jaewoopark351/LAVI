package lavi.minecraft.integration.carryon;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.carryon.logging.CarryOnDiagnosticSessionContext;
import lavi.minecraft.integration.carryon.logging.CarryOnDiagnosticSessionLogger;
import net.minecraft.client.MinecraftClient;

//20260730_kpopmodder: Model a bounded Carry On diagnostic session owned by a LAVI wrapper or parent Task.
public final class CarryOnDiagnosticSession {
    private final CarryOnStateReader stateReader;
    private final CarryOnDiagnosticSessionContext context;
    private final CarryOnDiagnosticSessionLogger logger;
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
                                     CarryOnStateReader stateReader) {
        this.stateReader = stateReader;
        this.context = new CarryOnDiagnosticSessionContext(
                taskInstanceId,
                operationId,
                operationType,
                expectedTransition,
                childTask,
                targetType,
                targetId,
                targetPosition
        );
        this.logger = new CarryOnDiagnosticSessionLogger(context);
    }

    public static CarryOnDiagnosticSession start(long taskInstanceId,
                                                 long operationId,
                                                 CarryOnOperationType operationType,
                                                 CarryOnTransition expectedTransition,
                                                 String childTask,
                                                 String targetType,
                                                 String targetId,
                                                 String targetPosition,
                                                 CarryOnStateReader stateReader) {
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
        return context.operationId();
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
        if (!ChatClefDiagnostics.isVerboseEnabled() && !CarryOnObservationClassifier.capabilityFailure(before, after)) {
            return;
        }
        log("state", before, after, clickResult, false, CarryOnTerminalReason.UNAVAILABLE);
    }

    public void logHeartbeat(CarryOnObservation before, CarryOnObservation after, String clickResult) {
        if (!ChatClefDiagnostics.isVerboseEnabled()) {
            return;
        }
        log("heartbeat", before, after, clickResult, false, CarryOnTerminalReason.UNAVAILABLE);
    }

    public void logTerminal(CarryOnTerminalReason terminalReason, String clickResult) {
        if (!ChatClefDiagnostics.isVerboseEnabled() && !CarryOnObservationClassifier.warningTerminal(terminalReason)) {
            return;
        }
        CarryOnObservation observation = observe();
        log("terminal", observation, observation, clickResult, false, terminalReason);
    }

    public void logTerminal(CarryOnObservation before, CarryOnObservation after, CarryOnTerminalReason terminalReason, String clickResult) {
        if (!ChatClefDiagnostics.isVerboseEnabled()
                && !CarryOnObservationClassifier.warningTerminal(terminalReason)
                && !CarryOnObservationClassifier.capabilityFailure(before, after)) {
            return;
        }
        log("terminal", before, after, clickResult, false, terminalReason);
    }

    private void logEntry() {
        if (!ChatClefDiagnostics.isVerboseEnabled()) {
            return;
        }
        CarryOnObservation observation = observe();
        log("entry", observation, observation, "unavailable", false, CarryOnTerminalReason.UNAVAILABLE);
    }

    private void log(String eventName,
                     CarryOnObservation stateBefore,
                     CarryOnObservation stateAfter,
                     String clickResult,
                     boolean clickAttempt,
                     CarryOnTerminalReason terminalReason) {
        logger.log(
                eventName,
                stateBefore,
                stateAfter,
                clickResult,
                clickAttempt,
                attemptCount,
                elapsedTicks,
                terminalReason
        );
    }

}
