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
        boolean stateChanged = !sameObservation(previousObservation, currentObservation);
        boolean carrying = isCarrying(currentObservation);
        boolean observedCarryRelease = observedCarryRelease(previousObservation, currentObservation);
        boolean capabilityFailure = isCapabilityFailure(currentObservation);
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
                    terminalReason(before, currentObservation, CarryOnTransition.NONE, observedCarryRelease, observationWindowExpired, sessionEnded),
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

    private static boolean observedCarryRelease(CarryOnObservation before, CarryOnObservation after) {
        return before != null
                && after != null
                && before.state() == CarryOnCarryState.AVAILABLE_CARRYING
                && after.state() == CarryOnCarryState.AVAILABLE_NOT_CARRYING;
    }

    private static boolean isCapabilityFailure(CarryOnObservation observation) {
        if (observation == null) {
            return false;
        }
        return observation.state() == CarryOnCarryState.INCOMPATIBLE
                || observation.state() == CarryOnCarryState.STATE_UNREADABLE
                || observation.state() == CarryOnCarryState.OBSERVATION_FAILED;
    }

    private static CarryOnTerminalReason terminalReason(CarryOnObservation before,
                                                        CarryOnObservation after,
                                                        CarryOnTransition expectedTransition,
                                                        boolean observedCarryRelease,
                                                        boolean observationWindowExpired,
                                                        boolean sessionEnded) {
        if (before == null || after == null) {
            return CarryOnTerminalReason.OBSERVATION_FAILED;
        }
        CarryOnTerminalReason capabilityTerminal = capabilityTerminal(before);
        if (capabilityTerminal != CarryOnTerminalReason.UNAVAILABLE) {
            return capabilityTerminal;
        }
        capabilityTerminal = capabilityTerminal(after);
        if (capabilityTerminal != CarryOnTerminalReason.UNAVAILABLE) {
            return capabilityTerminal;
        }
        if (observationWindowExpired) {
            return CarryOnTerminalReason.OBSERVATION_WINDOW_EXPIRED;
        }
        if (expectedTransition != CarryOnTransition.NONE && expectedTransition.matches(before, after)) {
            return CarryOnTerminalReason.SUCCESS;
        }
        if (observedCarryRelease) {
            return CarryOnTerminalReason.STATE_TRANSITION_OBSERVED;
        }
        if (sessionEnded) {
            return CarryOnTerminalReason.SESSION_ENDED;
        }
        return CarryOnTerminalReason.UNAVAILABLE;
    }

    private static CarryOnTerminalReason capabilityTerminal(CarryOnObservation observation) {
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
