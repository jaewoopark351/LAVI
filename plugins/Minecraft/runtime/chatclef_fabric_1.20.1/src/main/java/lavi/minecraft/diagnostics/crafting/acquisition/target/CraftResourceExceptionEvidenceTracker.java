package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

//20260901_kpopmodder: Bound already-observed engine exceptions without adding a catch or retry.
public final class CraftResourceExceptionEvidenceTracker {
    private static final String EXCEPTION_EVENT_NAME =
            "BLOCK_OPTIONAL_META_MANAGER_EXCEPTION";
    private static final String COVERAGE_GAP_EVENT_NAME =
            "BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP";
    private static final int PER_CORRELATION_SIGNATURE_LIMIT = 4;
    private static final int SESSION_SIGNATURE_LIMIT = 16;
    private static final int STACK_FRAME_LIMIT = 8;
    private static final int STACK_FRAME_UTF8_LIMIT = 256;
    private static final int STACK_TOTAL_UTF8_LIMIT = 2_048;

    private final Map<String, Set<String>> signaturesByCorrelation = new LinkedHashMap<>();
    private final Map<String, MutableOccurrenceWindow> occurrenceWindows = new LinkedHashMap<>();

    private long ownedExceptionOccurrenceCount;
    private long ownedCoverageGapCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private boolean counterSaturated;

    public synchronized CraftResourceExceptionDecision evaluate(
            CraftResourceExceptionObservation observation) {
        Objects.requireNonNull(observation, "observation");
        String eventName = eventName(observation.evidenceKind());
        if (observation.associationStatus()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT
                || !observation.correlationIdentityAvailable()) {
            accountUnowned(observation.associationStatus());
            return decision(
                    CraftResourceExceptionDisposition.ASSOCIATION_NOT_OWNED,
                    eventName,
                    false,
                    observation,
                    "",
                    List.of()
            );
        }

        accountOwned(observation.evidenceKind());
        String fingerprint = fingerprint(observation);
        MutableOccurrenceWindow existing = occurrenceWindows.get(fingerprint);
        if (existing != null) {
            existing.occurrenceCount = increment(existing.occurrenceCount);
            existing.lastObservedTick = observation.observationTick();
            return decision(
                    CraftResourceExceptionDisposition.DUPLICATE_SUPPRESSED,
                    eventName,
                    false,
                    observation,
                    fingerprint,
                    List.of()
            );
        }

        Set<String> correlationSignatures = signaturesByCorrelation.get(
                observation.commandCorrelationId()
        );
        if (correlationSignatures != null
                && correlationSignatures.size() >= PER_CORRELATION_SIGNATURE_LIMIT) {
            return decision(
                    CraftResourceExceptionDisposition.PER_CORRELATION_LIMIT,
                    eventName,
                    false,
                    observation,
                    fingerprint,
                    List.of()
            );
        }
        if (occurrenceWindows.size() >= SESSION_SIGNATURE_LIMIT) {
            return decision(
                    CraftResourceExceptionDisposition.SESSION_LIMIT,
                    eventName,
                    false,
                    observation,
                    fingerprint,
                    List.of()
            );
        }

        if (correlationSignatures == null) {
            correlationSignatures = new LinkedHashSet<>(PER_CORRELATION_SIGNATURE_LIMIT);
            signaturesByCorrelation.put(
                    observation.commandCorrelationId(),
                    correlationSignatures
            );
        }
        correlationSignatures.add(fingerprint);
        occurrenceWindows.put(
                fingerprint,
                new MutableOccurrenceWindow(
                        observation.observationTick(),
                        observation.observationTick()
                )
        );

        List<String> selectedStackFrames = observation.evidenceKind()
                == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION
                ? selectBoundedStack(observation.stackFrames())
                : List.of();
        CraftResourceExceptionDisposition disposition = observation.evidenceKind()
                == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION
                ? CraftResourceExceptionDisposition.EMISSION_REQUESTED
                : CraftResourceExceptionDisposition.COVERAGE_GAP_REQUESTED;
        return decision(
                disposition,
                eventName,
                true,
                observation,
                fingerprint,
                selectedStackFrames
        );
    }

    public synchronized CraftResourceExceptionSnapshot snapshot() {
        Map<String, Integer> retainedCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : signaturesByCorrelation.entrySet()) {
            retainedCounts.put(entry.getKey(), entry.getValue().size());
        }
        Map<String, CraftResourceExceptionOccurrenceWindow> immutableWindows =
                new LinkedHashMap<>();
        for (Map.Entry<String, MutableOccurrenceWindow> entry : occurrenceWindows.entrySet()) {
            MutableOccurrenceWindow window = entry.getValue();
            immutableWindows.put(
                    entry.getKey(),
                    new CraftResourceExceptionOccurrenceWindow(
                            window.occurrenceCount,
                            window.firstObservedTick,
                            window.lastObservedTick
                    )
            );
        }
        return new CraftResourceExceptionSnapshot(
                ownedExceptionOccurrenceCount,
                ownedCoverageGapCount,
                unownedObservationCount,
                unknownAssociationCount,
                occurrenceWindows.size(),
                retainedCounts,
                immutableWindows,
                counterSaturated
        );
    }

    private void accountOwned(CraftResourceExceptionEvidenceKind evidenceKind) {
        if (evidenceKind == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION) {
            ownedExceptionOccurrenceCount = increment(ownedExceptionOccurrenceCount);
        } else {
            ownedCoverageGapCount = increment(ownedCoverageGapCount);
        }
    }

    private void accountUnowned(CraftResourceAssociationStatus associationStatus) {
        if (associationStatus == CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED) {
            unownedObservationCount = increment(unownedObservationCount);
        } else {
            unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    private static CraftResourceExceptionDecision decision(
            CraftResourceExceptionDisposition disposition,
            String eventName,
            boolean emissionRequested,
            CraftResourceExceptionObservation observation,
            String fingerprint,
            List<String> selectedStackFrames) {
        boolean stackIncluded = !selectedStackFrames.isEmpty();
        int framesSelected = selectedStackFrames.size();
        int framesOmitted = Math.max(0, observation.framesAvailable() - framesSelected);
        Optional<String> exceptionType = observation.evidenceKind()
                == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION
                ? observation.exceptionType()
                : Optional.empty();
        Optional<String> exceptionMessage = observation.evidenceKind()
                == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION
                ? observation.exceptionMessage()
                : Optional.empty();
        return new CraftResourceExceptionDecision(
                disposition,
                eventName,
                emissionRequested,
                stackIncluded,
                observation.framesAvailable(),
                framesSelected,
                framesOmitted,
                selectedStackFrames,
                fingerprint,
                exceptionType,
                exceptionMessage
        );
    }

    private static List<String> selectBoundedStack(List<String> stackFrames) {
        ArrayList<String> selected = new ArrayList<>(Math.min(
                stackFrames.size(),
                STACK_FRAME_LIMIT
        ));
        int remainingBytes = STACK_TOTAL_UTF8_LIMIT;
        for (String frame : stackFrames) {
            if (selected.size() == STACK_FRAME_LIMIT || remainingBytes <= 0) {
                break;
            }
            String bounded = CraftResourceUtf8Truncator.truncate(
                    frame,
                    Math.min(STACK_FRAME_UTF8_LIMIT, remainingBytes)
            );
            selected.add(bounded);
            remainingBytes -= CraftResourceUtf8Truncator.utf8Length(bounded);
        }
        return List.copyOf(selected);
    }

    private static String eventName(CraftResourceExceptionEvidenceKind evidenceKind) {
        return evidenceKind == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION
                ? EXCEPTION_EVENT_NAME
                : COVERAGE_GAP_EVENT_NAME;
    }

    private static String fingerprint(CraftResourceExceptionObservation observation) {
        StringBuilder value = new StringBuilder();
        appendPart(value, eventName(observation.evidenceKind()));
        appendPart(value, observation.commandCorrelationId());
        appendPart(value, observation.observationBoundary());
        if (observation.evidenceKind() == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION) {
            appendPart(value, observation.exceptionType().orElse("UNAVAILABLE"));
        } else {
            appendPart(value, observation.coverageReason().orElse("UNAVAILABLE"));
        }
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

    private final class MutableOccurrenceWindow {
        private long occurrenceCount = 1L;
        private final long firstObservedTick;
        private long lastObservedTick;

        private MutableOccurrenceWindow(long firstObservedTick, long lastObservedTick) {
            this.firstObservedTick = firstObservedTick;
            this.lastObservedTick = lastObservedTick;
        }
    }
}
