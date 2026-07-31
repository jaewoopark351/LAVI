package lavi.minecraft.integration.carryon;

//20260731_kpopmodder: Centralize Carry On observation and terminal classification without changing behavior.
public final class CarryOnObservationClassifier {
    private CarryOnObservationClassifier() {
    }

    public static boolean sameObservation(CarryOnObservation previous, CarryOnObservation current) {
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

    public static boolean isCarrying(CarryOnObservation observation) {
        return observation != null && observation.state() == CarryOnCarryState.AVAILABLE_CARRYING;
    }

    public static boolean observedCarryRelease(CarryOnObservation before, CarryOnObservation after) {
        return before != null
                && after != null
                && before.state() == CarryOnCarryState.AVAILABLE_CARRYING
                && after.state() == CarryOnCarryState.AVAILABLE_NOT_CARRYING;
    }

    public static boolean capabilityFailure(CarryOnObservation before, CarryOnObservation after) {
        return capabilityFailure(before) || capabilityFailure(after);
    }

    public static boolean capabilityFailure(CarryOnObservation observation) {
        if (observation == null) {
            return false;
        }
        return observation.state() == CarryOnCarryState.INCOMPATIBLE
                || observation.state() == CarryOnCarryState.STATE_UNREADABLE
                || observation.state() == CarryOnCarryState.OBSERVATION_FAILED;
    }

    public static boolean warningTerminal(CarryOnTerminalReason terminalReason) {
        return terminalReason == CarryOnTerminalReason.CAPABILITY_INCOMPATIBLE
                || terminalReason == CarryOnTerminalReason.STATE_UNREADABLE
                || terminalReason == CarryOnTerminalReason.OBSERVATION_FAILED;
    }

    public static CarryOnTerminalReason terminalReason(CarryOnObservation before,
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

    public static CarryOnTerminalReason capabilityTerminal(CarryOnObservation observation) {
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
}
