package lavi.minecraft.integration.carryon;

//20260730_kpopmodder: Bound Carry On diagnostic output without dropping click attempts or state changes.
public final class CarryOnDiagnosticSampler {
    public static final int DEFAULT_MAX_TICKS = 20 * 10 * 60;
    public static final int DEFAULT_MAX_ATTEMPTS = 8;

    private final int maxTicks;
    private final int maxAttempts;
    private String previousKey = "";
    private boolean compatibilityFailureLogged;

    public CarryOnDiagnosticSampler() {
        this(DEFAULT_MAX_TICKS, DEFAULT_MAX_ATTEMPTS);
    }

    public CarryOnDiagnosticSampler(int maxTicks, int maxAttempts) {
        this.maxTicks = maxTicks;
        this.maxAttempts = maxAttempts;
    }

    public boolean shouldLog(CarryOnSnapshot snapshot, boolean clickAttempt) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.terminal()) {
            remember(snapshot);
            return true;
        }
        if (snapshot.elapsedTicks() > maxTicks || snapshot.attemptCount() > maxAttempts) {
            return false;
        }
        if ("heartbeat".equals(snapshot.eventName())) {
            remember(snapshot);
            return true;
        }
        if (clickAttempt) {
            remember(snapshot);
            return true;
        }
        if (compatibilityFailure(snapshot)) {
            if (compatibilityFailureLogged) {
                return false;
            }
            compatibilityFailureLogged = true;
            remember(snapshot);
            return true;
        }
        String key = snapshot.stateKey();
        if (!key.equals(previousKey)) {
            previousKey = key;
            return true;
        }
        return false;
    }

    private void remember(CarryOnSnapshot snapshot) {
        previousKey = snapshot.stateKey();
    }

    private boolean compatibilityFailure(CarryOnSnapshot snapshot) {
        return isCompatibilityFailure(snapshot.stateBefore()) || isCompatibilityFailure(snapshot.stateAfter());
    }

    private boolean isCompatibilityFailure(CarryOnObservation observation) {
        if (observation == null) {
            return false;
        }
        return observation.state() == CarryOnCarryState.INCOMPATIBLE
                || observation.state() == CarryOnCarryState.STATE_UNREADABLE
                || observation.state() == CarryOnCarryState.OBSERVATION_FAILED;
    }
}
