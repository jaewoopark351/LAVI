package lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchDisposition;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchObservation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

//20260901_kpopmodder: Retain exact-command mismatch accounting apart from session admission.
public final class CraftResourceCommandMismatchLedger {
    private static final int RETAINED_SIGNATURE_LIMIT = 2;

    private final Set<String> retainedSignatures = new LinkedHashSet<>();
    private final Set<String> admissionDeniedSignatures = new LinkedHashSet<>();
    private final Set<String> emissionFailedSignatures = new LinkedHashSet<>();
    private long ownedMismatchOccurrenceCount;
    private long coverageGapCount;
    private long duplicateSuppressedCount;
    private long perCorrelationLimitSuppressedCount;
    private long sessionLimitSuppressedCount;
    private long admissionDeniedCount;
    private long emissionFailureCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private boolean counterSaturated;

    public synchronized void observe(
            CraftResourceMismatchObservation observation,
            CraftResourceMismatchDecision decision) {
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(decision, "decision");
        if (observation.associationStatus()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            accountUnowned(observation.associationStatus());
            return;
        }

        switch (decision.disposition()) {
            case MATCH -> {
                return;
            }
            case COVERAGE_GAP -> {
                coverageGapCount = increment(coverageGapCount);
                return;
            }
            case ASSOCIATION_NOT_OWNED -> {
                accountUnowned(observation.associationStatus());
                return;
            }
            case EMISSION_REQUESTED -> retainSignature(decision.fingerprint());
            case DUPLICATE_SUPPRESSED -> duplicateSuppressedCount = increment(
                    duplicateSuppressedCount
            );
            case PER_CORRELATION_LIMIT -> perCorrelationLimitSuppressedCount = increment(
                    perCorrelationLimitSuppressedCount
            );
            case SESSION_LIMIT -> sessionLimitSuppressedCount = increment(
                    sessionLimitSuppressedCount
            );
        }
        ownedMismatchOccurrenceCount = increment(ownedMismatchOccurrenceCount);
    }

    public synchronized boolean recordAdmissionDenied(String fingerprint) {
        if (!isRetained(fingerprint) || !admissionDeniedSignatures.add(fingerprint)) {
            return false;
        }
        admissionDeniedCount = increment(admissionDeniedCount);
        return true;
    }

    public synchronized boolean recordEmissionFailed(String fingerprint) {
        if (!isRetained(fingerprint) || !emissionFailedSignatures.add(fingerprint)) {
            return false;
        }
        emissionFailureCount = increment(emissionFailureCount);
        return true;
    }

    public synchronized CraftResourceCommandMismatchSnapshot snapshot() {
        return new CraftResourceCommandMismatchSnapshot(
                ownedMismatchOccurrenceCount,
                coverageGapCount,
                duplicateSuppressedCount,
                perCorrelationLimitSuppressedCount,
                sessionLimitSuppressedCount,
                admissionDeniedCount,
                emissionFailureCount,
                unownedObservationCount,
                unknownAssociationCount,
                retainedSignatures.size(),
                counterSaturated
        );
    }

    private void retainSignature(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()
                || retainedSignatures.size() >= RETAINED_SIGNATURE_LIMIT) {
            return;
        }
        retainedSignatures.add(fingerprint);
    }

    private boolean isRetained(String fingerprint) {
        return fingerprint != null && retainedSignatures.contains(fingerprint);
    }

    private void accountUnowned(CraftResourceAssociationStatus status) {
        if (status == CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED) {
            unownedObservationCount = increment(unownedObservationCount);
        } else {
            unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return Long.MAX_VALUE;
        }
        return value + 1L;
    }
}
