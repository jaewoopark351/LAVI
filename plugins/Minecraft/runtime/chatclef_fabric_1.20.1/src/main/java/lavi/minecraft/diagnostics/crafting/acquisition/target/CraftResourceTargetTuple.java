package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.target.position.CraftResourceTargetPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

//20260901_kpopmodder: Normalize one already-observed semantic target without rescanning the world.
public final class CraftResourceTargetTuple {
    private static final int EXPECTED_BLOCK_ID_LIMIT = 8;
    private static final int RETAINED_SCALAR_UTF8_LIMIT = 256;

    private final CraftResourceStage resourceStage;
    private final CraftResourceTargetRole targetRole;
    private final String targetPosition;
    private final List<String> expectedBlockIds;
    private final int expectedBlockIdsOmittedCount;
    private final long normalizedExpectedBlockIdsFingerprint;

    public CraftResourceTargetTuple(
            CraftResourceStage resourceStage,
            CraftResourceTargetRole targetRole,
            String targetPosition,
            List<String> expectedBlockIds) {
        this.resourceStage = Objects.requireNonNull(resourceStage, "resourceStage");
        this.targetRole = Objects.requireNonNull(targetRole, "targetRole");
        this.targetPosition = CraftResourceUtf8Truncator.truncate(
                CraftResourceTargetPosition.canonicalize(targetPosition),
                RETAINED_SCALAR_UTF8_LIMIT
        );

        TreeSet<String> normalized = new TreeSet<>();
        for (String blockId : Objects.requireNonNull(expectedBlockIds, "expectedBlockIds")) {
            String value = normalizeBlockId(blockId);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        this.expectedBlockIdsOmittedCount = Math.max(
                0,
                normalized.size() - EXPECTED_BLOCK_ID_LIMIT
        );
        this.normalizedExpectedBlockIdsFingerprint = fingerprint(normalized);
        ArrayList<String> retained = new ArrayList<>(EXPECTED_BLOCK_ID_LIMIT);
        for (String blockId : normalized) {
            if (retained.size() == EXPECTED_BLOCK_ID_LIMIT) {
                break;
            }
            retained.add(CraftResourceUtf8Truncator.truncate(
                    blockId,
                    RETAINED_SCALAR_UTF8_LIMIT
            ));
        }
        this.expectedBlockIds = Collections.unmodifiableList(retained);
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

    public List<String> expectedBlockIds() {
        return expectedBlockIds;
    }

    public int expectedBlockIdsOmittedCount() {
        return expectedBlockIdsOmittedCount;
    }

    public long normalizedExpectedBlockIdsFingerprint() {
        return normalizedExpectedBlockIdsFingerprint;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CraftResourceTargetTuple that)) {
            return false;
        }
        return resourceStage == that.resourceStage
                && targetRole == that.targetRole
                && targetPosition.equals(that.targetPosition)
                && expectedBlockIds.equals(that.expectedBlockIds)
                && expectedBlockIdsOmittedCount == that.expectedBlockIdsOmittedCount
                && normalizedExpectedBlockIdsFingerprint
                == that.normalizedExpectedBlockIdsFingerprint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                resourceStage,
                targetRole,
                targetPosition,
                expectedBlockIds,
                expectedBlockIdsOmittedCount,
                normalizedExpectedBlockIdsFingerprint
        );
    }

    @Override
    public String toString() {
        return "CraftResourceTargetTuple[resourceStage=" + resourceStage
                + ", targetRole=" + targetRole
                + ", targetPosition=" + targetPosition
                + ", expectedBlockIds=" + expectedBlockIds
                + ", expectedBlockIdsOmittedCount=" + expectedBlockIdsOmittedCount
                + ", normalizedExpectedBlockIdsFingerprint="
                + normalizedExpectedBlockIdsFingerprint
                + "]";
    }

    static String normalizeBlockId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static long fingerprint(Iterable<String> normalizedBlockIds) {
        long hash = 0xcbf29ce484222325L;
        for (String blockId : normalizedBlockIds) {
            for (int index = 0; index < blockId.length(); index++) {
                hash ^= blockId.charAt(index);
                hash *= 0x100000001b3L;
            }
            hash ^= 0xffL;
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
