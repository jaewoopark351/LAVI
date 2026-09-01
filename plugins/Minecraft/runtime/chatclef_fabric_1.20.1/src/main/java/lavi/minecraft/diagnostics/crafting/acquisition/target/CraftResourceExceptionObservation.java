package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Carry only an already-captured Throwable or an explicit coverage gap.
public final class CraftResourceExceptionObservation {
    private static final int OPAQUE_ID_UTF8_LIMIT = 360;
    private static final int RETAINED_INPUT_FRAME_LIMIT = 8;
    private static final int RETAINED_SCALAR_UTF8_LIMIT = 256;

    private final String commandCorrelationId;
    private final boolean correlationIdentityAvailable;
    private final CraftResourceAssociationStatus associationStatus;
    private final String observationBoundary;
    private final CraftResourceExceptionEvidenceKind evidenceKind;
    private final Optional<String> exceptionType;
    private final Optional<String> exceptionMessage;
    private final Optional<String> coverageReason;
    private final List<String> stackFrames;
    private final int framesAvailable;
    private final long observationTick;
    private final String sourceTaskInstanceId;

    private CraftResourceExceptionObservation(
            String commandCorrelationId,
            CraftResourceAssociationStatus associationStatus,
            String observationBoundary,
            CraftResourceExceptionEvidenceKind evidenceKind,
            Optional<String> exceptionType,
            Optional<String> exceptionMessage,
            Optional<String> coverageReason,
            List<String> stackFrames,
            long observationTick,
            String sourceTaskInstanceId) {
        String normalizedCorrelationId = normalizeScalar(commandCorrelationId);
        boolean correlationAvailable = !normalizedCorrelationId.isEmpty()
                && normalizedCorrelationId.getBytes(StandardCharsets.UTF_8).length
                <= OPAQUE_ID_UTF8_LIMIT;
        this.correlationIdentityAvailable = correlationAvailable;
        this.commandCorrelationId = correlationAvailable
                ? normalizedCorrelationId
                : normalizedCorrelationId.isEmpty()
                        ? "UNAVAILABLE"
                        : "UNAVAILABLE_OVERSIZE";
        this.associationStatus = Objects.requireNonNull(associationStatus, "associationStatus");
        this.observationBoundary = boundedScalar(observationBoundary);
        this.evidenceKind = Objects.requireNonNull(evidenceKind, "evidenceKind");
        this.exceptionType = normalizeOptional(exceptionType)
                .map(value -> CraftResourceUtf8Truncator.truncate(value, 256));
        this.exceptionMessage = normalizeOptional(exceptionMessage)
                .map(value -> CraftResourceUtf8Truncator.truncate(value, 256));
        this.coverageReason = normalizeOptional(coverageReason)
                .map(value -> CraftResourceUtf8Truncator.truncate(value, 256));

        List<String> suppliedFrames = Objects.requireNonNull(stackFrames, "stackFrames");
        this.framesAvailable = suppliedFrames.size();
        ArrayList<String> retainedFrames = new ArrayList<>(Math.min(
                suppliedFrames.size(),
                RETAINED_INPUT_FRAME_LIMIT
        ));
        for (String frame : suppliedFrames) {
            if (retainedFrames.size() == RETAINED_INPUT_FRAME_LIMIT) {
                break;
            }
            retainedFrames.add(frame == null ? "" : frame);
        }
        this.stackFrames = List.copyOf(retainedFrames);
        this.observationTick = observationTick;
        this.sourceTaskInstanceId = boundedScalar(sourceTaskInstanceId);
    }

    public static CraftResourceExceptionObservation engineException(
            String commandCorrelationId,
            CraftResourceAssociationStatus associationStatus,
            String observationBoundary,
            String exceptionType,
            String exceptionMessage,
            List<String> stackFrames,
            long observationTick,
            String sourceTaskInstanceId) {
        return new CraftResourceExceptionObservation(
                commandCorrelationId,
                associationStatus,
                observationBoundary,
                CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION,
                Optional.ofNullable(exceptionType),
                Optional.ofNullable(exceptionMessage),
                Optional.empty(),
                stackFrames,
                observationTick,
                sourceTaskInstanceId
        );
    }

    public static CraftResourceExceptionObservation coverageGap(
            String commandCorrelationId,
            CraftResourceAssociationStatus associationStatus,
            String observationBoundary,
            String coverageReason,
            long observationTick,
            String sourceTaskInstanceId) {
        return new CraftResourceExceptionObservation(
                commandCorrelationId,
                associationStatus,
                observationBoundary,
                CraftResourceExceptionEvidenceKind.COVERAGE_GAP,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(coverageReason),
                List.of(),
                observationTick,
                sourceTaskInstanceId
        );
    }

    public String commandCorrelationId() {
        return commandCorrelationId;
    }

    public CraftResourceAssociationStatus associationStatus() {
        return associationStatus;
    }

    public String observationBoundary() {
        return observationBoundary;
    }

    public CraftResourceExceptionEvidenceKind evidenceKind() {
        return evidenceKind;
    }

    public Optional<String> exceptionType() {
        return exceptionType;
    }

    public Optional<String> exceptionMessage() {
        return exceptionMessage;
    }

    public Optional<String> coverageReason() {
        return coverageReason;
    }

    public List<String> stackFrames() {
        return stackFrames;
    }

    public int framesAvailable() {
        return framesAvailable;
    }

    public long observationTick() {
        return observationTick;
    }

    public String sourceTaskInstanceId() {
        return sourceTaskInstanceId;
    }

    public boolean correlationIdentityAvailable() {
        return correlationIdentityAvailable;
    }

    private static Optional<String> normalizeOptional(Optional<String> value) {
        Objects.requireNonNull(value, "value");
        return value.map(String::trim).filter(item -> !item.isEmpty());
    }

    private static String normalizeScalar(String value) {
        return value == null ? "" : value.trim();
    }

    private static String boundedScalar(String value) {
        return CraftResourceUtf8Truncator.truncate(
                normalizeScalar(value),
                RETAINED_SCALAR_UTF8_LIMIT
        );
    }
}
