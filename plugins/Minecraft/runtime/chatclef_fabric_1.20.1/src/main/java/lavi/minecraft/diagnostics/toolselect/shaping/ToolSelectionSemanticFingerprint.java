package lavi.minecraft.diagnostics.toolselect.shaping;

//20260831_kpopmodder: Build bounded tool-selection fingerprints from semantic decision fields only.
public final class ToolSelectionSemanticFingerprint {
    private static final int MAX_FIELD_LENGTH = 120;
    private static final int MAX_FINGERPRINT_LENGTH = 360;
    private static final int MAX_CHANNEL_LENGTH = 80;

    private ToolSelectionSemanticFingerprint() {
    }

    public static String selectionDecision(
            String decisionReason,
            String targetIdentity,
            String currentToolIdentity,
            String selectedToolIdentity,
            String decisionDetails) {
        StringBuilder result = new StringBuilder();
        append(result, decisionReason);
        append(result, targetIdentity);
        append(result, currentToolIdentity);
        append(result, selectedToolIdentity);
        append(result, decisionDetails);
        return normalizeFingerprint(result.toString());
    }

    static String normalizeChannel(String value) {
        return normalize(value, MAX_CHANNEL_LENGTH);
    }

    static String normalizeFingerprint(String value) {
        return normalize(value, MAX_FINGERPRINT_LENGTH);
    }

    private static void append(StringBuilder result, String value) {
        String normalized = normalize(value, MAX_FIELD_LENGTH);
        result.append(normalized.length()).append(':').append(normalized).append('|');
    }

    private static String normalize(String value, int maxLength) {
        String normalized = value == null || value.isEmpty() ? "none" : value;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        String suffix = "#h=" + Long.toUnsignedString(stableHash(normalized), 16);
        int prefixLength = Math.max(0, maxLength - suffix.length());
        return normalized.substring(0, prefixLength) + suffix;
    }

    private static long stableHash(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
