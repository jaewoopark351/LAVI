package lavi.minecraft.diagnostics.formatting;

import java.util.function.Supplier;

//20260731_kpopmodder: Isolate defensive diagnostic value formatting from event/session ownership.
public final class DiagnosticValueFormatter {
    private DiagnosticValueFormatter() {
    }

    public static String className(Object value) {
        if (value == null) {
            return "none";
        }
        try {
            return value.getClass().getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String safeValue(Supplier<?> supplier, boolean enabled) {
        if (!enabled) {
            return "unavailable";
        }
        try {
            return value(supplier.get());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String value(Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        String stringValue;
        try {
            stringValue = String.valueOf(rawValue);
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
        return stringValue
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}
