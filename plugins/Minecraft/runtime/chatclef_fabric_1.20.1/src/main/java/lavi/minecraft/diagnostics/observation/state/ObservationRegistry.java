package lavi.minecraft.diagnostics.observation.state;

import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Fixed process-lifetime scope admission keeps each reserved first-event claim bounded. */
public final class ObservationRegistry {
    private final List<ObservationActivation> activations = new ArrayList<>();
    private final List<ObservationScope> scopes = new ArrayList<>();
    private final Map<String, Integer> accepted = new HashMap<>();
    private WeakReference<Object> world = new WeakReference<>(null);
    private long epoch = -1, generation, refused, captureFailures, staleOrUnavailableScope;
    private String lastFailure = "NONE";

    public synchronized void observeWorld(Object currentWorld) {
        if (world.get() != currentWorld) {
            invalidate();
            world = new WeakReference<>(currentWorld);
        }
    }
    public synchronized ObservationActivation capture(long modeEpoch, Object instance, Object currentWorld) {
        if (instance == null || currentWorld == null) return null;
        if (epoch != modeEpoch) { invalidate(); epoch = modeEpoch; }
        observeWorld(currentWorld);
        for (ObservationActivation activation : activations)
            if (activation.matches(modeEpoch, instance, currentWorld)) return activation;
        activations.removeIf(activation -> !activation.live(modeEpoch));
        if (activations.size() >= 8) { refused++; return null; }
        ObservationActivation result = new ObservationActivation(modeEpoch, ++generation, instance, currentWorld);
        activations.add(result);
        return result;
    }
    public synchronized ObservationScope open(ObservationActivation activation, String domain, String key, String context) {
        return open(activation, domain, key, new Object[]{"context", context});
    }
    public synchronized ObservationScope open(ObservationActivation activation, String domain, String key, Object[] context) {
        for (ObservationScope scope : scopes)
            if (scope.activation() == activation && scope.domain().equals(domain) && scope.operationKey().equals(key))
                return scope;
        if (!List.of("mining", "deposit", "builder").contains(domain) || accepted.getOrDefault(domain, 0) >= 4) {
            refused++;
            return ObservationScope.NOOP;
        }
        accepted.merge(domain, 1, Integer::sum);
        ObservationScope scope = new ObservationScope(activation, domain, key, context);
        scopes.add(scope);
        return scope;
    }
    public synchronized List<ObservationScope> otherOperations(ObservationActivation activation, String domain, String keepKey) {
        return scopes.stream().filter(scope -> scope.activation() == activation && scope.domain().equals(domain)
                && !scope.operationKey().equals(keepKey)).toList();
    }
    public synchronized void invalidate() {
        activations.forEach(ObservationActivation::invalidate);
        activations.clear();
        // Frozen ledgers (at most twelve) remain inspectable; live objects are weakly held only.
    }
    public synchronized void captureFailed(String domain, String exceptionType) {
        captureFailures++;
        lastFailure = domain + ":" + exceptionType;
    }
    public synchronized void recordRejected() { staleOrUnavailableScope++; }
    public synchronized Object[] snapshot() {
        long attempted = 0, failures = 0;
        long summaryAttempted = 0, summaryAdmitted = 0, summaryReturned = 0, summaryExhaustedScopes = 0;
        for (ObservationScope scope : scopes) {
            Object[] summary = scope.ledger().summary();
            attempted += ((Number) summary[3]).longValue();
            failures += ((Number) summary[9]).longValue();
            for (int i = 0; i + 1 < summary.length; i += 2) {
                switch (String.valueOf(summary[i])) {
                    case "summaryAttemptedCount" -> summaryAttempted += ((Number) summary[i + 1]).longValue();
                    case "summaryAdmittedCount" -> summaryAdmitted += ((Number) summary[i + 1]).longValue();
                    case "summaryEmissionCallsReturned" -> summaryReturned += ((Number) summary[i + 1]).longValue();
                    case "summaryAllowanceExhausted" -> { if (Boolean.TRUE.equals(summary[i + 1])) summaryExhaustedScopes++; }
                    default -> { }
                }
            }
        }
        return new Object[]{"resourceObservationScopeCount", scopes.size(), "resourceObservationScopeRefused", refused,
                "resourceObservationAttempted", attempted, "resourceObservationDispatchFailures", failures,
                "resourceObservationCaptureFailures", captureFailures, "resourceObservationLastCaptureFailure", lastFailure,
                "resourceObservationStaleOrUnavailableScope", staleOrUnavailableScope,
                "resourceSummaryAttempted", summaryAttempted, "resourceSummaryAdmitted", summaryAdmitted,
                "resourceSummaryEmissionCallsReturned", summaryReturned, "resourceSummaryExhaustedScopes", summaryExhaustedScopes,
                "resourceSummaryFilePersistence", "NOT_VERIFIED"};
    }
}
