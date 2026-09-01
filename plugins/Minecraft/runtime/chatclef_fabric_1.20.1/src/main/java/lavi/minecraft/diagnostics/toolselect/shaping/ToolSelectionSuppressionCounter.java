package lavi.minecraft.diagnostics.toolselect.shaping;

//20260831_kpopmodder: Saturate tool-selection suppression accounting without wraparound.
final class ToolSelectionSuppressionCounter {
    private ToolSelectionSuppressionCounter() {
    }

    static long increment(long value) {
        return add(value, 1L);
    }

    static long add(long value, long increment) {
        if (increment <= 0L) {
            return Math.max(0L, value);
        }
        long normalized = Math.max(0L, value);
        if (normalized >= Long.MAX_VALUE - increment) {
            return Long.MAX_VALUE;
        }
        return normalized + increment;
    }
}
