package lavi.minecraft.integration.carryon;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.carryon.policy.CarryOnRuntimeObservationPolicy;
import net.minecraft.client.MinecraftClient;

//20260730_kpopmodder: Observe current Carry On stuck-state evidence from a LAVI-owned tick boundary only.
public final class CarryOnRuntimeStateObserver {
    private final CarryOnRuntimeObservationPolicy policy = CarryOnRuntimeObservationPolicy.defaults();
    private CarryOnDiagnosticSession session;
    private CarryOnObservation previousObservation;
    private int ticksSinceObservation;
    private boolean capabilityFailureLogged;

    public void onEndClientTick(MinecraftClient client) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ticksSinceObservation++;
        if (!canObserve(client)) {
            resetSession();
            return;
        }
        if (!policy.shouldObserve(ticksSinceObservation)) {
            return;
        }

        int elapsedTicks = ticksSinceObservation;
        ticksSinceObservation = 0;

        CarryOnObservation currentObservation = CarryOnDiagnostics.observe();
        boolean stateChanged = !CarryOnObservationClassifier.sameObservation(previousObservation, currentObservation);
        boolean carrying = CarryOnObservationClassifier.isCarrying(currentObservation);
        boolean observedCarryRelease = CarryOnObservationClassifier.observedCarryRelease(previousObservation, currentObservation);
        boolean capabilityFailure = CarryOnObservationClassifier.capabilityFailure(currentObservation);
        boolean taskRunnerActive = isTaskRunnerActive();

        if (session == null) {
            if (!policy.shouldStartSession(taskRunnerActive, carrying, observedCarryRelease, capabilityFailure, capabilityFailureLogged)) {
                previousObservation = currentObservation;
                return;
            }
            session = CarryOnDiagnostics.startSession(
                    CarryOnDiagnostics.nextTaskInstanceId(),
                    CarryOnOperationType.OBSERVATION_ONLY,
                    CarryOnTransition.NONE,
                    "runtime_state_observer",
                    "client_state",
                    "unavailable",
                    "unavailable"
            );
        } else {
            session.advanceTicks(elapsedTicks);
        }

        CarryOnObservation before = previousObservation == null ? currentObservation : previousObservation;
        session.logHeartbeat(before, currentObservation, "runtime_tick");
        if (stateChanged || carrying || observedCarryRelease || capabilityFailure) {
            session.logState(before, currentObservation, "runtime_observe");
        }

        if (capabilityFailure) {
            capabilityFailureLogged = true;
        }
        boolean observationWindowExpired = policy.observationWindowExpired(session.elapsedTicks());
        boolean sessionEnded = policy.sessionEnded(taskRunnerActive, carrying, capabilityFailure);
        if (observedCarryRelease || capabilityFailure || observationWindowExpired || sessionEnded) {
            session.logTerminal(
                    before,
                    currentObservation,
                    CarryOnObservationClassifier.terminalReason(before, currentObservation, CarryOnTransition.NONE, observedCarryRelease, observationWindowExpired, sessionEnded),
                    "runtime_observe"
            );
            session = null;
        }

        previousObservation = currentObservation;
    }

    private static boolean canObserve(MinecraftClient client) {
        return client != null && client.player != null && client.world != null;
    }

    private static boolean isTaskRunnerActive() {
        try {
            AltoClef mod = AltoClef.getInstance();
            return mod != null && mod.getTaskRunner() != null && mod.getTaskRunner().isActive();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private void resetSession() {
        session = null;
        previousObservation = null;
        ticksSinceObservation = 0;
    }
}
