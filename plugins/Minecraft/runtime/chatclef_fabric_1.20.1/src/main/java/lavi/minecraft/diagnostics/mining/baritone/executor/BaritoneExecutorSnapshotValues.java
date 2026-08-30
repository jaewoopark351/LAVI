package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.pathing.calc.AbstractNodeCostSearch;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

import java.util.function.Supplier;

//20260830_kpopmodder: Own only executor snapshot scalar and object-summary formatting.
final class BaritoneExecutorSnapshotValues {
    static final int UNAVAILABLE_INT = -1;
    static final double UNAVAILABLE_DOUBLE = Double.NaN;

    private BaritoneExecutorSnapshotValues() {
    }

    static String identity(Object value) {
        return BaritonePathObjectFormatters.identity(value);
    }

    static String className(Object value) {
        return BaritonePathObjectFormatters.className(value);
    }

    static String safeValue(Supplier<?> supplier) {
        return ChatClefDiagnostics.safeValueForDiagnosticLog(supplier);
    }

    static String formatDouble(double value) {
        if (!Double.isFinite(value)) {
            return "unavailable";
        }
        return String.format("%.5f", value);
    }

    static String summarizeObject(Object value) {
        return BaritonePathObjectFormatters.summarizeObject(value);
    }

    static String summarizeFinder(AbstractNodeCostSearch finder) {
        return BaritonePathObjectFormatters.summarizeFinder(finder);
    }
}
