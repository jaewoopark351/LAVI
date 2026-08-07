package lavi.minecraft.diagnostics.command.deposit;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.StringJoiner;

//20260807_kpopmodder: Keep deposit command diagnostic field assembly out of the upstream command.
final class DepositCommandDiagnosticFields {
    private static final int MAX_CALLER_FRAMES = 8;

    private DepositCommandDiagnosticFields() {
    }

    static Object[] invocationFields(AltoClef mod,
                                     boolean explicitItemListProvided,
                                     ItemTarget[] selectedItems,
                                     Task taskToRun) {
        return new Object[]{
                "diagnosticScope", "deposit_command",
                "owner", "deposit_command_observer",
                "mode", "BOUNDARY",
                "trigger", "before_runUserTask",
                "dedupe_key", "deposit_command_invocation|" + identity(taskToRun),
                "max_emission", "one_per_deposit_command_invocation",
                "correlation", "taskToRunIdentity=" + identity(taskToRun),
                "payload", "flat_fields",
                "terminal", false,
                "behavior_effect", "none",
                "commandName", "deposit",
                "explicitItemListProvided", explicitItemListProvided,
                "selectedItems", ChatClefDiagnostics.itemTargets(selectedItems),
                "selectedItemTargetCount", selectedItems == null ? "unavailable" : selectedItems.length,
                "taskToRun", ChatClefDiagnostics.taskSummaryForDiagnosticLog(taskToRun),
                "taskToRunIdentity", identity(taskToRun),
                "getIfNotPresent", false,
                "dimension", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getWorld() == null ? null : mod.getWorld().getRegistryKey().getValue()),
                "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                "threadName", Thread.currentThread().getName(),
                "callerSummary", callerSummary()
        };
    }

    private static String callerSummary() {
        try {
            StackTraceElement[] frames = Thread.currentThread().getStackTrace();
            StringJoiner joiner = new StringJoiner(" <- ");
            int count = 0;
            for (StackTraceElement frame : frames) {
                String className = frame.getClassName();
                if (shouldSkipFrame(className)) {
                    continue;
                }
                joiner.add(className + "#" + frame.getMethodName() + ":" + frame.getLineNumber());
                count++;
                if (count >= MAX_CALLER_FRAMES) {
                    break;
                }
            }
            return count == 0 ? "unavailable" : joiner.toString();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable#error=" + error.getClass().getSimpleName();
        }
    }

    private static boolean shouldSkipFrame(String className) {
        return className == null
                || className.equals(Thread.class.getName())
                || className.equals(DepositCommandDiagnosticFields.class.getName())
                || className.equals(DepositCommandDiagnostics.class.getName());
    }

    private static String identity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
