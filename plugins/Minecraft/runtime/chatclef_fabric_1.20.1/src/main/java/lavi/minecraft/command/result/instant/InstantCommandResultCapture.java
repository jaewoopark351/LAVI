package lavi.minecraft.command.result.instant;

import java.util.Map;
import lavi.minecraft.command.result.instant.profile.InstantCommandProfile;

//20260915_kpopmodder: Bind observation to one existing synchronous invocation; no global result replay.
public final class InstantCommandResultCapture implements AutoCloseable {
    private static final ThreadLocal<InstantCommandResultCapture> CURRENT = new ThreadLocal<>();
    private final InstantCommandResultCapture previous;
    private final String commandName;
    private final String command;
    private InstantCommandResult result;

    private InstantCommandResultCapture(String commandText) {
        command = commandText.trim();
        commandName = InstantCommandProfile.name(commandText);
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static InstantCommandResultCapture begin(String prefixlessCommand) {
        return new InstantCommandResultCapture(prefixlessCommand);
    }

    public static void record(String command, boolean success, String reason, Map<String, Object> values) {
        recordOutcome(command, success ? "completed" : "failed", reason, values);
    }

    public static void recordOutcome(String command, String outcome, String reason, Map<String, Object> values) {
        try {
            InstantCommandResultCapture capture = CURRENT.get();
            if (capture == null || !capture.commandName.equals(command) || !InstantCommandProfile.observed(command)
                    || capture.result != null) return;
            capture.result = new InstantCommandResult(capture.command, command, outcome, reason, values);
        } catch (RuntimeException ignored) {
            // Observation failure cannot alter the command branch, finish callback or cleanup.
        }
    }

    public InstantCommandResult result() {
        if (!InstantCommandProfile.immediate(commandName)) return result;
        return result != null ? result : new InstantCommandResult(command, commandName, "unknown", "RESULT_UNAVAILABLE", Map.of());
    }

    @Override public void close() {
        if (CURRENT.get() != this) return;
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
