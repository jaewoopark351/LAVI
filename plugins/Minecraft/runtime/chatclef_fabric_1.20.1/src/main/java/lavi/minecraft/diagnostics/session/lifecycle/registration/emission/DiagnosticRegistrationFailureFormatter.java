package lavi.minecraft.diagnostics.session.lifecycle.registration.emission;

import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationResult;

//20260913_kpopmodder: Format bounded initializer-safe evidence without invoking diagnostic owners.
final class DiagnosticRegistrationFailureFormatter {
    private DiagnosticRegistrationFailureFormatter() { }

    static String format(String owner, DiagnosticObserverRegistrationResult result, String boundary, String reason) {
        return "[LAVI_DIAGNOSTIC_REGISTRATION] owner=" + token(owner)
                + " status=" + (result == null ? "UNKNOWN" : result.status())
                + " registeredCount=" + (result == null ? -1 : result.registeredCount())
                + " capacity=" + (result == null ? -1 : result.capacity())
                + " boundary=" + token(boundary) + " reason=" + token(reason)
                + " available=false filePersistence=NOT_VERIFIED";
    }

    private static String token(String value) {
        if (value == null) {
            return "UNKNOWN";
        }
        String bounded = value.substring(0, Math.min(value.length(), 96));
        return bounded.replaceAll("[^a-zA-Z0-9_.:$-]", "_");
    }
}
