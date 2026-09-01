package lavi.minecraft.diagnostics.toolselect.support;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

//20260804_kpopmodder: Centralize tool-selection diagnostic fingerprint gating without changing log fields.
public final class DiagnosticDeduplicator {
    static final int MAX_TRACKED_KEYS = 16;

    private static final int MAX_KEY_LENGTH = 80;
    private static final int MAX_FINGERPRINT_LENGTH = 360;

    private final Map<String, String> fingerprints =
            new LinkedHashMap<>(MAX_TRACKED_KEYS, 0.75f, true);

    public synchronized boolean shouldEmit(String key, String fingerprint) {
        String normalizedKey = normalize(key, MAX_KEY_LENGTH);
        String normalizedFingerprint = normalize(fingerprint, MAX_FINGERPRINT_LENGTH);
        if (normalizedFingerprint.equals(fingerprints.get(normalizedKey))) {
            return false;
        }
        if (!fingerprints.containsKey(normalizedKey)
                && fingerprints.size() >= MAX_TRACKED_KEYS) {
            Iterator<String> iterator = fingerprints.keySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        fingerprints.put(normalizedKey, normalizedFingerprint);
        return true;
    }

    public synchronized void clearForModeOff() {
        fingerprints.clear();
    }

    synchronized int trackedKeyCount() {
        return fingerprints.size();
    }

    private static String normalize(String value, int maximumLength) {
        String normalized = value == null || value.isEmpty() ? "none" : value;
        if (normalized.length() <= maximumLength) {
            return normalized;
        }
        String suffix = "#h=" + Integer.toUnsignedString(normalized.hashCode(), 16);
        int prefixLength = Math.max(0, maximumLength - suffix.length());
        return normalized.substring(0, prefixLength) + suffix;
    }
}
