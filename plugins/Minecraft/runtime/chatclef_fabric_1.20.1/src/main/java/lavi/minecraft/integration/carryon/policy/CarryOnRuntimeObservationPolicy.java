package lavi.minecraft.integration.carryon.policy;

//20260804_kpopmodder: Keep Carry On runtime observation timing and session rules separate from tick-side observation.
public final class CarryOnRuntimeObservationPolicy {
    private static final int DEFAULT_OBSERVE_INTERVAL_TICKS = 5;
    private static final int DEFAULT_MAX_SESSION_TICKS = 20 * 10 * 60;

    private final int observeIntervalTicks;
    private final int maxSessionTicks;

    private CarryOnRuntimeObservationPolicy(int observeIntervalTicks, int maxSessionTicks) {
        this.observeIntervalTicks = observeIntervalTicks;
        this.maxSessionTicks = maxSessionTicks;
    }

    public static CarryOnRuntimeObservationPolicy defaults() {
        return new CarryOnRuntimeObservationPolicy(DEFAULT_OBSERVE_INTERVAL_TICKS, DEFAULT_MAX_SESSION_TICKS);
    }

    public boolean shouldObserve(int ticksSinceObservation) {
        return ticksSinceObservation >= observeIntervalTicks;
    }

    public boolean shouldStartSession(boolean taskRunnerActive,
                                      boolean carrying,
                                      boolean observedCarryRelease,
                                      boolean capabilityFailure,
                                      boolean capabilityFailureLogged) {
        if (taskRunnerActive) {
            return true;
        }
        if (carrying || observedCarryRelease) {
            return true;
        }
        return capabilityFailure && !capabilityFailureLogged;
    }

    public boolean observationWindowExpired(int elapsedTicks) {
        return elapsedTicks >= maxSessionTicks;
    }

    public boolean sessionEnded(boolean taskRunnerActive, boolean carrying, boolean capabilityFailure) {
        return !taskRunnerActive && !carrying && !capabilityFailure;
    }
}
