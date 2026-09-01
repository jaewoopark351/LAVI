package lavi.minecraft.diagnostics.crafting.acquisition.target.failure;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Carry only already-observed mining failure facts into diagnostics.
public record CraftResourceFailureObservation(
        CraftResourceAssociationStatus associationStatus,
        CraftResourceFailureKind kind,
        String ownerTaskClass,
        String targetPosition,
        long failureCountBefore,
        long failureCountAfter,
        long allowedFailures,
        Optional<Boolean> unreachableBefore,
        Optional<Boolean> unreachableAfter,
        long observedTick,
        boolean sourceEmissionCompleted) {
    private static final int RETAINED_SCALAR_UTF8_LIMIT = 256;

    public CraftResourceFailureObservation {
        associationStatus = Objects.requireNonNull(
                associationStatus,
                "associationStatus"
        );
        kind = Objects.requireNonNull(kind, "kind");
        ownerTaskClass = bound(ownerTaskClass);
        targetPosition = bound(targetPosition);
        unreachableBefore = unreachableBefore == null
                ? Optional.empty()
                : unreachableBefore;
        unreachableAfter = unreachableAfter == null
                ? Optional.empty()
                : unreachableAfter;
    }

    private static String bound(String value) {
        String normalized = value == null || value.isBlank()
                ? "UNAVAILABLE"
                : value.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length
                <= RETAINED_SCALAR_UTF8_LIMIT) {
            return normalized;
        }
        StringBuilder result = new StringBuilder();
        int byteCount = 0;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterBytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (byteCount + characterBytes > RETAINED_SCALAR_UTF8_LIMIT) {
                break;
            }
            result.appendCodePoint(codePoint);
            byteCount += characterBytes;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }
}
