package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

//20260901_kpopmodder: Preserve same-boundary expected and observed values without a world rescan.
public final class CraftResourceMismatchObservation {
    private static final int EXPECTED_BLOCK_ID_LIMIT = 8;
    private static final int OPAQUE_ID_UTF8_LIMIT = 360;
    private static final int RETAINED_SCALAR_UTF8_LIMIT = 256;

    private final String commandCorrelationId;
    private final boolean correlationIdentityAvailable;
    private final CraftResourceAssociationStatus associationStatus;
    private final CraftResourceStage resourceStage;
    private final CraftResourceTargetRole targetRole;
    private final String targetPosition;
    private final Optional<List<String>> expectedBlockIds;
    private final int expectedBlockIdsOmittedCount;
    private final boolean expectedBlockIdsTruncated;
    private final Optional<String> observedBlockId;
    private final boolean observedBlockIdComplete;
    private final String observationBoundary;
    private final long observationTick;
    private final String sourceTaskInstanceId;

    public CraftResourceMismatchObservation(
            String commandCorrelationId,
            CraftResourceAssociationStatus associationStatus,
            CraftResourceStage resourceStage,
            CraftResourceTargetRole targetRole,
            String targetPosition,
            Optional<List<String>> expectedBlockIds,
            Optional<String> observedBlockId,
            String observationBoundary,
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
        this.resourceStage = Objects.requireNonNull(resourceStage, "resourceStage");
        this.targetRole = Objects.requireNonNull(targetRole, "targetRole");
        this.targetPosition = boundedScalar(targetPosition);

        Objects.requireNonNull(expectedBlockIds, "expectedBlockIds");
        if (expectedBlockIds.isPresent()) {
            TreeSet<String> normalized = new TreeSet<>();
            boolean truncated = false;
            for (String blockId : expectedBlockIds.get()) {
                String value = CraftResourceTargetTuple.normalizeBlockId(blockId);
                if (!value.isEmpty()) {
                    if (CraftResourceUtf8Truncator.utf8Length(value)
                            > RETAINED_SCALAR_UTF8_LIMIT) {
                        truncated = true;
                    }
                    normalized.add(CraftResourceUtf8Truncator.truncate(
                            value,
                            RETAINED_SCALAR_UTF8_LIMIT
                    ));
                }
            }
            this.expectedBlockIdsTruncated = truncated;
            this.expectedBlockIdsOmittedCount = Math.max(
                    0,
                    normalized.size() - EXPECTED_BLOCK_ID_LIMIT
            );
            ArrayList<String> retained = new ArrayList<>(EXPECTED_BLOCK_ID_LIMIT);
            for (String blockId : normalized) {
                if (retained.size() == EXPECTED_BLOCK_ID_LIMIT) {
                    break;
                }
                retained.add(blockId);
            }
            this.expectedBlockIds = Optional.of(Collections.unmodifiableList(retained));
        } else {
            this.expectedBlockIdsOmittedCount = 0;
            this.expectedBlockIdsTruncated = false;
            this.expectedBlockIds = Optional.empty();
        }

        Objects.requireNonNull(observedBlockId, "observedBlockId");
        Optional<String> normalizedObserved = observedBlockId
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty());
        this.observedBlockIdComplete = normalizedObserved.isPresent()
                && CraftResourceUtf8Truncator.utf8Length(normalizedObserved.get())
                <= RETAINED_SCALAR_UTF8_LIMIT;
        this.observedBlockId = normalizedObserved.map(value ->
                CraftResourceUtf8Truncator.truncate(value, RETAINED_SCALAR_UTF8_LIMIT));
        this.observationBoundary = boundedScalar(observationBoundary);
        this.observationTick = observationTick;
        this.sourceTaskInstanceId = boundedScalar(sourceTaskInstanceId);
    }

    public String commandCorrelationId() {
        return commandCorrelationId;
    }

    public CraftResourceAssociationStatus associationStatus() {
        return associationStatus;
    }

    public CraftResourceStage resourceStage() {
        return resourceStage;
    }

    public CraftResourceTargetRole targetRole() {
        return targetRole;
    }

    public String targetPosition() {
        return targetPosition;
    }

    public Optional<List<String>> expectedBlockIds() {
        return expectedBlockIds;
    }

    public int expectedBlockIdsOmittedCount() {
        return expectedBlockIdsOmittedCount;
    }

    public boolean expectedBlockIdsTruncated() {
        return expectedBlockIdsTruncated;
    }

    public Optional<String> observedBlockId() {
        return observedBlockId;
    }

    public boolean observedBlockIdComplete() {
        return observedBlockIdComplete;
    }

    public String observationBoundary() {
        return observationBoundary;
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
