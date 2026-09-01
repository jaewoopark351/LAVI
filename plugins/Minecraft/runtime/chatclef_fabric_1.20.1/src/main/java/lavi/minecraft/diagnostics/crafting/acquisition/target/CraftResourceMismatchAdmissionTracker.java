package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//20260901_kpopmodder: Bound same-boundary mismatch requests per command and diagnostic session.
public final class CraftResourceMismatchAdmissionTracker {
    private static final int ACTIVE_CORRELATION_LIMIT = 8;
    private static final int PER_CORRELATION_SIGNATURE_LIMIT = 2;
    private static final int SESSION_SIGNATURE_LIMIT = 8;

    private final Map<String, Set<String>> signaturesByCorrelation = new LinkedHashMap<>();
    private final Set<String> sessionSignatures = new LinkedHashSet<>();
    private final Set<String> admissionDeniedSignatures = new LinkedHashSet<>();
    private final Set<String> emissionFailedSignatures = new LinkedHashSet<>();

    private long ownedMismatchOccurrenceCount;
    private long coverageGapCount;
    private long duplicateSuppressedCount;
    private long perCorrelationLimitSuppressedCount;
    private long sessionLimitSuppressedCount;
    private long mismatchAdmissionDeniedCount;
    private long mismatchEmissionFailureCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private boolean counterSaturated;

    public synchronized boolean activateCorrelation(String commandCorrelationId) {
        Objects.requireNonNull(commandCorrelationId, "commandCorrelationId");
        if (signaturesByCorrelation.containsKey(commandCorrelationId)) {
            return true;
        }
        if (signaturesByCorrelation.size() >= ACTIVE_CORRELATION_LIMIT) {
            return false;
        }
        signaturesByCorrelation.put(
                commandCorrelationId,
                new LinkedHashSet<>(PER_CORRELATION_SIGNATURE_LIMIT)
        );
        return true;
    }

    public synchronized boolean retireCorrelation(String commandCorrelationId) {
        Objects.requireNonNull(commandCorrelationId, "commandCorrelationId");
        return signaturesByCorrelation.remove(commandCorrelationId) != null;
    }

    public synchronized CraftResourceMismatchDecision evaluate(
            CraftResourceMismatchObservation observation) {
        Objects.requireNonNull(observation, "observation");
        if (observation.associationStatus()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT
                || !observation.correlationIdentityAvailable()) {
            accountUnowned(observation.associationStatus());
            return decision(CraftResourceMismatchDisposition.ASSOCIATION_NOT_OWNED, "");
        }

        if (observation.expectedBlockIds().isEmpty()
                || observation.expectedBlockIds().get().isEmpty()
                || observation.expectedBlockIdsOmittedCount() > 0
                || observation.expectedBlockIdsTruncated()
                || observation.observedBlockId().isEmpty()
                || !observation.observedBlockIdComplete()) {
            coverageGapCount = increment(coverageGapCount);
            return decision(CraftResourceMismatchDisposition.COVERAGE_GAP, "");
        }

        List<String> expectedBlockIds = observation.expectedBlockIds().get();
        if (expectedBlockIds.contains(observation.observedBlockId().get())) {
            return decision(CraftResourceMismatchDisposition.MATCH, "");
        }

        ownedMismatchOccurrenceCount = increment(ownedMismatchOccurrenceCount);
        String fingerprint = fingerprint(observation, expectedBlockIds);
        Set<String> correlationSignatures = signaturesByCorrelation.get(
                observation.commandCorrelationId()
        );
        if (correlationSignatures == null) {
            if (!activateCorrelation(observation.commandCorrelationId())) {
                sessionLimitSuppressedCount = increment(sessionLimitSuppressedCount);
                return decision(CraftResourceMismatchDisposition.SESSION_LIMIT, fingerprint);
            }
            correlationSignatures = signaturesByCorrelation.get(
                    observation.commandCorrelationId()
            );
        }
        if (correlationSignatures.contains(fingerprint)) {
            duplicateSuppressedCount = increment(duplicateSuppressedCount);
            return decision(
                    CraftResourceMismatchDisposition.DUPLICATE_SUPPRESSED,
                    fingerprint
            );
        }
        if (correlationSignatures.size() >= PER_CORRELATION_SIGNATURE_LIMIT) {
            perCorrelationLimitSuppressedCount = increment(
                    perCorrelationLimitSuppressedCount
            );
            return decision(
                    CraftResourceMismatchDisposition.PER_CORRELATION_LIMIT,
                    fingerprint
            );
        }
        if (sessionSignatures.contains(fingerprint)) {
            correlationSignatures.add(fingerprint);
            duplicateSuppressedCount = increment(duplicateSuppressedCount);
            return decision(
                    CraftResourceMismatchDisposition.DUPLICATE_SUPPRESSED,
                    fingerprint
            );
        }
        if (sessionSignatures.size() >= SESSION_SIGNATURE_LIMIT) {
            sessionLimitSuppressedCount = increment(sessionLimitSuppressedCount);
            return decision(CraftResourceMismatchDisposition.SESSION_LIMIT, fingerprint);
        }

        correlationSignatures.add(fingerprint);
        sessionSignatures.add(fingerprint);
        return new CraftResourceMismatchDecision(
                CraftResourceMismatchDisposition.EMISSION_REQUESTED,
                true,
                fingerprint
        );
    }

    public synchronized boolean recordAdmissionDenied(String fingerprint) {
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (!sessionSignatures.contains(fingerprint)
                || admissionDeniedSignatures.contains(fingerprint)) {
            return false;
        }
        admissionDeniedSignatures.add(fingerprint);
        mismatchAdmissionDeniedCount = increment(mismatchAdmissionDeniedCount);
        return true;
    }

    public synchronized boolean recordEmissionFailed(String fingerprint) {
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (!sessionSignatures.contains(fingerprint)
                || emissionFailedSignatures.contains(fingerprint)) {
            return false;
        }
        emissionFailedSignatures.add(fingerprint);
        mismatchEmissionFailureCount = increment(mismatchEmissionFailureCount);
        return true;
    }

    public synchronized CraftResourceMismatchSnapshot snapshot() {
        Map<String, Integer> retainedCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : signaturesByCorrelation.entrySet()) {
            retainedCounts.put(entry.getKey(), entry.getValue().size());
        }
        return new CraftResourceMismatchSnapshot(
                ownedMismatchOccurrenceCount,
                coverageGapCount,
                duplicateSuppressedCount,
                perCorrelationLimitSuppressedCount,
                sessionLimitSuppressedCount,
                mismatchAdmissionDeniedCount,
                mismatchEmissionFailureCount,
                unownedObservationCount,
                unknownAssociationCount,
                sessionSignatures.size(),
                signaturesByCorrelation.size(),
                retainedCounts,
                counterSaturated
        );
    }

    private void accountUnowned(CraftResourceAssociationStatus associationStatus) {
        if (associationStatus == CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED) {
            unownedObservationCount = increment(unownedObservationCount);
        } else {
            unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    private CraftResourceMismatchDecision decision(
            CraftResourceMismatchDisposition disposition,
            String fingerprint) {
        return new CraftResourceMismatchDecision(disposition, false, fingerprint);
    }

    private static String fingerprint(
            CraftResourceMismatchObservation observation,
            List<String> expectedBlockIds) {
        StringBuilder value = new StringBuilder();
        appendPart(value, "CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH");
        appendPart(value, observation.resourceStage().name());
        appendPart(value, observation.targetRole().name());
        appendPart(value, observation.targetPosition());
        for (String expectedBlockId : expectedBlockIds) {
            appendPart(value, expectedBlockId);
        }
        appendPart(value, observation.observedBlockId().orElse(""));
        appendPart(value, observation.observationBoundary());
        return value.toString();
    }

    private static void appendPart(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value).append('|');
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return Long.MAX_VALUE;
        }
        return value + 1L;
    }
}
