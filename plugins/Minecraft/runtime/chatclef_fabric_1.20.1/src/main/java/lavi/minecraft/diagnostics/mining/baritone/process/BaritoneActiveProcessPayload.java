package lavi.minecraft.diagnostics.mining.baritone.process;

import baritone.api.process.IBaritoneProcess;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;

import java.util.List;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
final class BaritoneActiveProcessPayload {
    private static final int SUMMARY_LIMIT = 6;

    private BaritoneActiveProcessPayload() {
    }

    static int count(List<IBaritoneProcess> activeProcesses) {
        if (activeProcesses == null) {
            return -1;
        }
        try {
            return activeProcesses.size();
        } catch (RuntimeException | LinkageError error) {
            return -1;
        }
    }

    static String semanticFingerprint(List<IBaritoneProcess> activeProcesses) {
        if (activeProcesses == null) {
            return "none";
        }
        try {
            StringBuilder builder = new StringBuilder();
            int limit = Math.min(activeProcesses.size(), SUMMARY_LIMIT);
            builder.append("count=").append(activeProcesses.size()).append(",classes=");
            for (int index = 0; index < limit; index++) {
                if (index > 0) {
                    builder.append(",");
                }
                builder.append(BaritonePathObjectFormatters.className(activeProcesses.get(index)));
            }
            if (activeProcesses.size() > limit) {
                builder.append(",...");
            }
            return builder.toString();
        } catch (RuntimeException | LinkageError error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }

    static String detailedSummary(List<IBaritoneProcess> activeProcesses) {
        if (activeProcesses == null) {
            return "none";
        }
        try {
            StringBuilder builder = new StringBuilder();
            int limit = Math.min(activeProcesses.size(), SUMMARY_LIMIT);
            for (int index = 0; index < limit; index++) {
                if (index > 0) {
                    builder.append(",");
                }
                builder.append(BaritonePathObjectFormatters.summarizeProcess(activeProcesses.get(index)));
            }
            if (activeProcesses.size() > limit) {
                builder.append(",...");
            }
            return builder.toString();
        } catch (RuntimeException | LinkageError error) {
            return "error=" + error.getClass().getSimpleName();
        }
    }
}
