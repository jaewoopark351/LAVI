package lavi.minecraft.integration.carryon;

import adris.altoclef.AltoClef;
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
        boolean stateChanged = !sameObservation(previousObservation, currentObservation);
        boolean carrying = isCarrying(currentObservation);
        boolean carryingEnded = isCarrying(previousObservation) && !carrying;
        boolean capabilityFailure = isCapabilityFailure(currentObservation);
        boolean taskRunnerActive = isTaskRunnerActive();

        if (session == null) {
            if (!shouldStartSession(taskRunnerActive, carrying, carryingEnded, capabilityFailure)) {
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
        if (stateChanged || carrying || carryingEnded || capabilityFailure) {
            session.logState(before, currentObservation, "runtime_observe");
        }

        if (capabilityFailure) {
            capabilityFailureLogged = true;
        }
        boolean observationWindowExpired = session.elapsedTicks() >= MAX_SESSION_TICKS;
        boolean sessionEnded = !taskRunnerActive && !carrying && !capabilityFailure;
        if (carryingEnded || capabilityFailure || observationWindowExpired || sessionEnded) {
            session.logTerminal(
                    before,
                    currentObservation,
                    terminalReason(currentObservation, carryingEnded, observationWindowExpired, sessionEnded),
                    "runtime_observe"
            );
            session = null;
        }

        previousObservation = currentObservation;
    }

    private static boolean canObserve(MinecraftClient client) {
        return client != null && client.player != null && client.world != null;
    }

    private boolean shouldStartSession(boolean taskRunnerActive, boolean carrying, boolean carryingEnded, boolean capabilityFailure) {
        if (taskRunnerActive) {
            return true;
        }
        if (carrying || carryingEnded) {
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

    private static boolean sameObservation(CarryOnObservation previous, CarryOnObservation current) {
        if (previous == current) {
            return true;
        }
        if (previous == null || current == null) {
            return false;
        }
        return previous.loaded() == current.loaded()
                && previous.state() == current.state()
                && previous.version().equals(current.version())
                && previous.exceptionType().equals(current.exceptionType());
    }

    private static boolean isCarrying(CarryOnObservation observation) {
        return observation != null && observation.state() == CarryOnCarryState.AVAILABLE_CARRYING;
    }

    private static boolean isCapabilityFailure(CarryOnObservation observation) {
        if (observation == null) {
            return false;
        }
        return observation.state() == CarryOnCarryState.INCOMPATIBLE
                || observation.state() == CarryOnCarryState.STATE_UNREADABLE
                || observation.state() == CarryOnCarryState.OBSERVATION_FAILED;
    }

    private static CarryOnTerminalReason terminalReason(CarryOnObservation observation,
                                                        boolean carryingEnded,
                                                        boolean observationWindowExpired,
                                                        boolean sessionEnded) {
        if (carryingEnded) {
            return CarryOnTerminalReason.SUCCESS;
        }
        if (observationWindowExpired) {
            return CarryOnTerminalReason.OBSERVATION_WINDOW_EXPIRED;
        }
        if (sessionEnded) {
            return CarryOnTerminalReason.SESSION_ENDED;
        }
        if (observation == null) {
            return CarryOnTerminalReason.OBSERVATION_FAILED;
        }
        return switch (observation.state()) {
            case ABSENT -> CarryOnTerminalReason.CAPABILITY_ABSENT;
            case INCOMPATIBLE -> CarryOnTerminalReason.CAPABILITY_INCOMPATIBLE;
            case STATE_UNREADABLE -> CarryOnTerminalReason.STATE_UNREADABLE;
            case OBSERVATION_FAILED -> CarryOnTerminalReason.OBSERVATION_FAILED;
            default -> CarryOnTerminalReason.UNAVAILABLE;
        };
    }

    private void resetSession() {
        session = null;
        previousObservation = null;
        ticksSinceObservation = 0;
    }
}
