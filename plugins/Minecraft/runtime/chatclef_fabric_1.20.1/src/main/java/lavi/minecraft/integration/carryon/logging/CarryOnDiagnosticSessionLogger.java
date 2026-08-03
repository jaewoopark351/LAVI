package lavi.minecraft.integration.carryon.logging;

import lavi.minecraft.integration.carryon.CarryOnDiagnosticLogger;
import lavi.minecraft.integration.carryon.CarryOnDiagnosticSampler;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.CarryOnSnapshot;
import lavi.minecraft.integration.carryon.CarryOnSnapshotCollector;
import lavi.minecraft.integration.carryon.CarryOnTerminalReason;

//20260804_kpopmodder: Separate Carry On diagnostic session logging from session state and observation timing.
public final class CarryOnDiagnosticSessionLogger {
    private final CarryOnDiagnosticSessionContext context;
    private final CarryOnDiagnosticSampler sampler = new CarryOnDiagnosticSampler();

    public CarryOnDiagnosticSessionLogger(CarryOnDiagnosticSessionContext context) {
        this.context = context;
    }

    public void log(String eventName,
                    CarryOnObservation stateBefore,
                    CarryOnObservation stateAfter,
                    String clickResult,
                    boolean clickAttempt,
                    int attemptCount,
                    int elapsedTicks,
                    CarryOnTerminalReason terminalReason) {
        CarryOnSnapshot snapshot = CarryOnSnapshotCollector.collect(
                context.taskInstanceId(),
                context.operationId(),
                eventName,
                context.operationType(),
                context.expectedTransition(),
                context.childTask(),
                context.targetType(),
                context.targetId(),
                context.targetPosition(),
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
