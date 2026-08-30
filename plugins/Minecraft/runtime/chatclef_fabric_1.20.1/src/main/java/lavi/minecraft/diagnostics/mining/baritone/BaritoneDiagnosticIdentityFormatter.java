package lavi.minecraft.diagnostics.mining.baritone;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Own only stable identity, type, and scalar diagnostic formatting.
final class BaritoneDiagnosticIdentityFormatter {
    private BaritoneDiagnosticIdentityFormatter() {
    }

    static String identity(Object value) {
        if (value == null) {
            return "none";
        }
        return Integer.toHexString(System.identityHashCode(value));
    }

    static String className(Object value) {
        return value == null ? "none" : ChatClefDiagnostics.className(value);
    }

    static String safeValue(Object value) {
        return ChatClefDiagnostics.safeValueForDiagnosticLog(() -> value);
    }
}
