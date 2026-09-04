package lavi.minecraft.diagnostics.container.gui.budget;

import java.util.LinkedHashMap;
import java.util.Map;

//20260904_kpopmodder: Deduplicate and locally cap exact-container observations without affecting shared admission.
public final class ContainerGuiDiagnosticLimiter {
    private static final String OVERFLOW_FINGERPRINT = "__SEMANTIC_BUCKET_OVERFLOW__";

    private final Map<String, RepeatBucket> buckets = new LinkedHashMap<>();
    private long nextActivationOrdinal = 1L;
    private String activationId;
    private long observationCount;
    private long dedupeSuppressedCount;
    private long localAdmissionCount;
    private long localCapSuppressedCount;
    private long sharedAdmissionRejectedCount;
    private long physicalEmissionCount;
    private long omittedCount;

    public ContainerGuiDiagnosticLimiter() {
    }

    public ContainerGuiDiagnosticLimiter(String diagnosticBoundaryActivationId) {
        if (diagnosticBoundaryActivationId != null && !diagnosticBoundaryActivationId.isBlank()) {
            activationId = diagnosticBoundaryActivationId;
        }
    }

    public synchronized ContainerGuiEmissionDecision evaluate(String fingerprint, long gameTick) {
        ensureActivation();
        observationCount++;
        String stableFingerprint = fingerprint == null ? "UNAVAILABLE" : fingerprint;
        RepeatBucket bucket = buckets.get(stableFingerprint);
        if (bucket == null
                && buckets.size() >= ContainerGuiDiagnosticLimits.MAX_SEMANTIC_BUCKETS - 1) {
            omittedCount++;
            bucket = buckets.get(OVERFLOW_FINGERPRINT);
            if (bucket == null) {
                buckets.put(OVERFLOW_FINGERPRINT, new RepeatBucket(gameTick));
            } else {
                return suppressRepeat(bucket, gameTick);
            }
        }
        if (bucket != null) {
            return suppressRepeat(bucket, gameTick);
        }
        if (buckets.size() < ContainerGuiDiagnosticLimits.MAX_SEMANTIC_BUCKETS - 1) {
            buckets.put(stableFingerprint, new RepeatBucket(gameTick));
        }
        if (localAdmissionCount >= ContainerGuiDiagnosticLimits.DETAIL_LIMIT_PER_ACTIVATION) {
            localCapSuppressedCount++;
            return new ContainerGuiEmissionDecision(
                    ContainerGuiEmissionDecision.Outcome.SUPPRESSED_LOCAL_CAP,
                    0,
                    activationId
            );
        }
        localAdmissionCount++;
        return new ContainerGuiEmissionDecision(
                ContainerGuiEmissionDecision.Outcome.DETAIL,
                0,
                activationId
        );
    }

    public synchronized String activationId() {
        ensureActivation();
        return activationId;
    }

    public synchronized boolean hasActivation() {
        return activationId != null;
    }

    public synchronized String activeActivationIdOrUnavailable() {
        return activationId == null ? "unavailable" : activationId;
    }

    public synchronized void recordSharedOutcome(boolean admitted, boolean physicallyEmitted) {
        if (!admitted) {
            sharedAdmissionRejectedCount++;
        }
        if (physicallyEmitted) {
            physicalEmissionCount++;
        }
    }

    public synchronized ContainerGuiDiagnosticAggregateSnapshot snapshot() {
        ensureActivation();
        return new ContainerGuiDiagnosticAggregateSnapshot(
                activationId,
                observationCount,
                dedupeSuppressedCount,
                localAdmissionCount,
                localCapSuppressedCount,
                sharedAdmissionRejectedCount,
                physicalEmissionCount,
                omittedCount
        );
    }

    public synchronized ContainerGuiDiagnosticAggregateSnapshot snapshotIfActive() {
        if (activationId == null) {
            return null;
        }
        return new ContainerGuiDiagnosticAggregateSnapshot(
                activationId,
                observationCount,
                dedupeSuppressedCount,
                localAdmissionCount,
                localCapSuppressedCount,
                sharedAdmissionRejectedCount,
                physicalEmissionCount,
                omittedCount
        );
    }

    public synchronized void clearForModeTransition() {
        activationId = null;
        buckets.clear();
        observationCount = 0L;
        dedupeSuppressedCount = 0L;
        localAdmissionCount = 0L;
        localCapSuppressedCount = 0L;
        sharedAdmissionRejectedCount = 0L;
        physicalEmissionCount = 0L;
        omittedCount = 0L;
    }

    private void ensureActivation() {
        if (activationId == null) {
            activationId = "container-gui-boundary-" + nextActivationOrdinal++;
        }
    }

    private ContainerGuiEmissionDecision suppressRepeat(RepeatBucket bucket, long gameTick) {
        bucket.suppressed++;
        dedupeSuppressedCount++;
        if (gameTick >= bucket.lastSummaryTick
                && gameTick - bucket.lastSummaryTick
                >= ContainerGuiDiagnosticLimits.REPEAT_SUMMARY_INTERVAL_TICKS) {
            int suppressed = bucket.suppressed;
            bucket.suppressed = 0;
            bucket.lastSummaryTick = gameTick;
            return new ContainerGuiEmissionDecision(
                    ContainerGuiEmissionDecision.Outcome.REPEAT_SUMMARY,
                    suppressed,
                    activationId
            );
        }
        return new ContainerGuiEmissionDecision(
                ContainerGuiEmissionDecision.Outcome.SUPPRESSED_DUPLICATE,
                bucket.suppressed,
                activationId
        );
    }

    private static final class RepeatBucket {
        private long lastSummaryTick;
        private int suppressed;

        private RepeatBucket(long firstTick) {
            lastSummaryTick = firstTick;
        }
    }
}
