package lavi.minecraft.integration.carryon;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.client.MinecraftClient;

//20260730_kpopmodder: Observe current Carry On stuck-state evidence from a LAVI-owned tick boundary only.
public final class CarryOnRuntimeStateObserver {
    private static final int OBSERVE_INTERVAL_TICKS = 5;
    private static final int MAX_SESSION_TICKS = 20 * 10 * 60;

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
        if (ticksSinceObservation < OBSERVE_INTERVAL_TICKS) {
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
            if (!shouldStartSession(taskRunnerActive, carrying, observedCarryRelease, capabilityFailure)) {
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
        boolean observationWindowExpired = session.elapsedTicks() >= MAX_SESSION_TICKS;
        boolean sessionEnded = !taskRunnerActive && !carrying && !capabilityFailure;
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

    private boolean shouldStartSession(boolean taskRunnerActive, boolean carrying, boolean observedCarryRelease, boolean capabilityFailure) {
        if (taskRunnerActive) {
            return true;
        }
        if (carrying || observedCarryRelease) {
            return true;
        }
        return capabilityFailure && !capabilityFailureLogged;
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
