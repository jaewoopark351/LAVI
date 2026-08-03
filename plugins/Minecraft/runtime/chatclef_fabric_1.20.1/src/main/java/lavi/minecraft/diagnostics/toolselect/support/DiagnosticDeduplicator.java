package lavi.minecraft.diagnostics.toolselect.support;

import java.util.HashMap;
import java.util.Map;

//20260804_kpopmodder: Centralize tool-selection diagnostic fingerprint gating without changing log fields.
public final class DiagnosticDeduplicator {
    private final Map<String, String> fingerprints = new HashMap<>();

    public boolean shouldEmit(String key, String fingerprint) {
        String normalizedFingerprint = fingerprint == null ? "null" : fingerprint;
        if (normalizedFingerprint.equals(fingerprints.get(key))) {
            return false;
        }
        fingerprints.put(key, normalizedFingerprint);
        return true;
    }
}
