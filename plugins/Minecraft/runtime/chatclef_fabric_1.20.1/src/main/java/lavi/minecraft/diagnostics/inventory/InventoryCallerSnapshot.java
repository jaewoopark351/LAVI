package lavi.minecraft.diagnostics.inventory;

import java.util.StringJoiner;

//20260805_kpopmodder: Keep inventory scan caller attribution separate from scan state tracking.
public final class InventoryCallerSnapshot {
    private static final int TOP_FRAME_LIMIT = 8;

    private final String callerBoundary;
    private final String callerTopFrames;

    private InventoryCallerSnapshot(String callerBoundary, String callerTopFrames) {
        this.callerBoundary = callerBoundary;
        this.callerTopFrames = callerTopFrames;
    }

    public static InventoryCallerSnapshot capture(boolean includeTopFrames) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String boundary = "unavailable";
        StringJoiner topFrames = new StringJoiner(" | ");
        int included = 0;
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            if (className.equals(Thread.class.getName())
                    || className.startsWith("lavi.minecraft.diagnostics.inventory.")) {
                continue;
            }
            String value = frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber();
            if ("unavailable".equals(boundary)
                    && !className.equals("adris.altoclef.trackers.storage.InventorySubTracker")) {
                boundary = value;
            }
            if (includeTopFrames && included < TOP_FRAME_LIMIT) {
                topFrames.add(value);
                included++;
            }
        }
        return new InventoryCallerSnapshot(
                boundary,
                includeTopFrames && included > 0 ? topFrames.toString() : "not_captured"
        );
    }

    public String callerBoundary() {
        return callerBoundary;
    }

    public String callerTopFrames() {
        return callerTopFrames;
    }
}
