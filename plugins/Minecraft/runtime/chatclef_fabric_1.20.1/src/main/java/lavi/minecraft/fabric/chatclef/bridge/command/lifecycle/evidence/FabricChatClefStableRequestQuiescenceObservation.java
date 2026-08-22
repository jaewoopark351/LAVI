package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import java.util.HashMap;
import java.util.Map;

//20260820_kpopmodder: Represent request-quiescence evidence without changing command lifecycle behavior.
public final class FabricChatClefStableRequestQuiescenceObservation {
    private final boolean qualified;
    private final String blockedReason;
    private final int observationCount;
    private final long firstObservedAtMs;
    private final long lastObservedAtMs;
    private final long firstClientTickId;
    private final long lastClientTickId;
    private final long stableDurationMs;
    private final long elapsedSinceFinishCallbackMs;
    private final long snapshotAgeMs;
    private final boolean sameSessionGeneration;
    private final boolean requestRootReappeared;
    private final String requestRootObservationState;
    private final String signatureVersion;
    private final String policyVersion;
    private final long configuredMinNeutralSnapshots;
    private final long configuredMinNeutralDurationMs;
    private final long configuredMaxSnapshotAgeMs;

    private FabricChatClefStableRequestQuiescenceObservation(
            boolean qualified,
            String blockedReason,
            int observationCount,
            long firstObservedAtMs,
            long lastObservedAtMs,
            long firstClientTickId,
            long lastClientTickId,
            long stableDurationMs,
            long elapsedSinceFinishCallbackMs,
            long snapshotAgeMs,
            boolean sameSessionGeneration,
            boolean requestRootReappeared,
            String requestRootObservationState,
            String signatureVersion,
            String policyVersion,
            long configuredMinNeutralSnapshots,
            long configuredMinNeutralDurationMs,
            long configuredMaxSnapshotAgeMs
    ) {
        this.qualified = qualified;
        this.blockedReason = nullToEmpty(blockedReason);
        this.observationCount = observationCount;
        this.firstObservedAtMs = firstObservedAtMs;
        this.lastObservedAtMs = lastObservedAtMs;
        this.firstClientTickId = firstClientTickId;
        this.lastClientTickId = lastClientTickId;
        this.stableDurationMs = stableDurationMs;
        this.elapsedSinceFinishCallbackMs = elapsedSinceFinishCallbackMs;
        this.snapshotAgeMs = snapshotAgeMs;
        this.sameSessionGeneration = sameSessionGeneration;
        this.requestRootReappeared = requestRootReappeared;
        this.requestRootObservationState = nullToEmpty(requestRootObservationState);
        this.signatureVersion = nullToEmpty(signatureVersion);
        this.policyVersion = nullToEmpty(policyVersion);
        this.configuredMinNeutralSnapshots = configuredMinNeutralSnapshots;
        this.configuredMinNeutralDurationMs = configuredMinNeutralDurationMs;
        this.configuredMaxSnapshotAgeMs = configuredMaxSnapshotAgeMs;
    }

    public static FabricChatClefStableRequestQuiescenceObservation of(
            boolean qualified,
            String blockedReason,
            int observationCount,
            long firstObservedAtMs,
            long lastObservedAtMs,
            long firstClientTickId,
            long lastClientTickId,
            long stableDurationMs,
            long elapsedSinceFinishCallbackMs,
            long snapshotAgeMs,
            boolean sameSessionGeneration,
            boolean requestRootReappeared,
            String requestRootObservationState,
            String signatureVersion,
            String policyVersion,
            long configuredMinNeutralSnapshots,
            long configuredMinNeutralDurationMs,
            long configuredMaxSnapshotAgeMs
    ) {
        return new FabricChatClefStableRequestQuiescenceObservation(
                qualified,
                blockedReason,
                observationCount,
                firstObservedAtMs,
                lastObservedAtMs,
                firstClientTickId,
                lastClientTickId,
                stableDurationMs,
                elapsedSinceFinishCallbackMs,
                snapshotAgeMs,
                sameSessionGeneration,
                requestRootReappeared,
                requestRootObservationState,
                signatureVersion,
                policyVersion,
                configuredMinNeutralSnapshots,
                configuredMinNeutralDurationMs,
                configuredMaxSnapshotAgeMs
        );
    }

    public boolean qualified() {
        return qualified;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("qualified", qualified);
        payload.put("satisfied", qualified);
        payload.put("blocked_reason", blockedReason);
        payload.put("reset_reason", blockedReason);
        payload.put("observation_count", observationCount);
        payload.put("distinct_tick_count", observationCount);
        payload.put("consecutive_neutral_snapshots", observationCount);
        payload.put("first_observed_at_ms", firstObservedAtMs);
        payload.put("last_observed_at_ms", lastObservedAtMs);
        payload.put("first_client_tick_id", firstClientTickId);
        payload.put("last_client_tick_id", lastClientTickId);
        payload.put("stable_duration_ms", stableDurationMs);
        payload.put("neutral_duration_ms", stableDurationMs);
        payload.put("elapsed_since_finish_callback_ms", elapsedSinceFinishCallbackMs);
        payload.put("snapshot_age_ms", snapshotAgeMs);
        payload.put("same_session_generation", sameSessionGeneration);
        payload.put("request_root_reappeared", requestRootReappeared);
        payload.put("request_root_observation_state", requestRootObservationState);
        payload.put("signature_version", signatureVersion);
        payload.put("policy_version", policyVersion);
        payload.put("configured_min_neutral_snapshots", configuredMinNeutralSnapshots);
        payload.put("configured_min_neutral_duration_ms", configuredMinNeutralDurationMs);
        payload.put("configured_max_snapshot_age_ms", configuredMaxSnapshotAgeMs);
        return payload;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
