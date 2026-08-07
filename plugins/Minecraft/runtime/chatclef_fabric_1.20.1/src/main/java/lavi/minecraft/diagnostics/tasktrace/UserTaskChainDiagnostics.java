package lavi.minecraft.diagnostics.tasktrace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.StringJoiner;

//20260807_kpopmodder: Keep UserTaskChain origin field assembly outside the upstream lifecycle owner.
public final class UserTaskChainDiagnostics {
    private static final int MAX_CALLER_FRAMES = 8;

    private UserTaskChainDiagnostics() {
    }

    public static void logTaskOriginDecision(AltoClef mod,
                                             Task previousTask,
                                             Task incomingTask,
                                             boolean runningIdleTask,
                                             boolean nextTaskIdleFlag,
                                             boolean incomingOnFinishPresent,
                                             Object... additionalFields) {
        String idleCommand = idleCommand(mod);
        boolean incomingMarkedIdle = nextTaskIdleFlag;
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_TASK_ORIGIN_DECISION",
                "user_task_chain_task_origin_decision",
                incomingTask,
                ChatClefDiagnostics.withCommandContextFields(merge(
                        new Object[]{
                        "diagnosticScope", "user_task_chain_origin",
                        "owner", "user_task_chain",
                        "mode", "BOUNDARY",
                        "trigger", "runTask_before_assignment",
                        "dedupe_key", originDecisionKey(previousTask, incomingTask, runningIdleTask, incomingMarkedIdle),
                        "max_emission", "one_per_runTask_call",
                        "correlation", "incomingTaskIdentity=" + identity(incomingTask),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "previousTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(previousTask),
                        "incomingTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(incomingTask),
                        "previousTaskWasRunningIdle", runningIdleTask,
                        "incomingMarkedIdleByNextFlag", incomingMarkedIdle,
                        "incomingWillConsumeNextIdleFlag", incomingMarkedIdle,
                        "incomingTaskOriginHint", incomingMarkedIdle ? "IDLE_COMMAND" : "EXTERNAL_OR_COMMAND_EXECUTOR_OR_INTERNAL_CALLBACK",
                        "incomingCommandHint", incomingMarkedIdle ? idleCommand : "command_context_or_command_executor_or_internal_callback",
                        "originIdleCommand", idleCommand,
                        "incomingOnFinishPresent", incomingOnFinishPresent,
                        "callerSummary", callerSummary()
                        },
                        additionalFields
                )));
    }

    public static Object[] withOriginAndCommandContext(AltoClef mod,
                                                       Task previousTask,
                                                       Task incomingTask,
                                                       boolean runningIdleTask,
                                                       boolean nextTaskIdleFlag,
                                                       Object... eventFields) {
        return ChatClefDiagnostics.withCommandContextFields(merge(eventFields, originFields(
                mod,
                previousTask,
                incomingTask,
                runningIdleTask,
                nextTaskIdleFlag
        )));
    }

    private static Object[] originFields(AltoClef mod,
                                         Task previousTask,
                                         Task incomingTask,
                                         boolean runningIdleTask,
                                         boolean nextTaskIdleFlag) {
        boolean idleOrigin = runningIdleTask || nextTaskIdleFlag;
        String idleCommand = idleCommand(mod);
        return new Object[]{
                "owner", "user_task_chain",
                "taskOriginHint", idleOrigin ? "IDLE_COMMAND" : "NON_IDLE_COMMAND",
                "originCommandHint", idleOrigin ? idleCommand : "command_context_or_command_executor",
                "originIdleCommand", idleCommand,
                "originPreviousTaskIdentity", identity(previousTask),
                "originIncomingTaskIdentity", identity(incomingTask),
                "behavior_effect", "none"
        };
    }

    private static String originDecisionKey(Task previousTask,
                                            Task incomingTask,
                                            boolean runningIdleTask,
                                            boolean incomingMarkedIdle) {
        return "user_task_chain_origin"
                + "|previous=" + ChatClefDiagnostics.className(previousTask)
                + "|incoming=" + ChatClefDiagnostics.className(incomingTask)
                + "|previousIdle=" + runningIdleTask
                + "|incomingMarkedIdle=" + incomingMarkedIdle;
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
                || className.equals(UserTaskChainDiagnostics.class.getName())
                || className.equals("adris.altoclef.chains.UserTaskChain");
    }

    private static Object[] merge(Object[] first, Object[] second) {
        first = flatten(first);
        second = flatten(second);
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second;
        }
        if (second == null || second.length == 0) {
            return first;
        }
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    private static Object[] flatten(Object[] fields) {
        if (fields == null || fields.length == 0) {
            return fields;
        }
        int flattenedLength = 0;
        for (Object field : fields) {
            flattenedLength += field instanceof Object[] nested ? nested.length : 1;
        }
        if (flattenedLength == fields.length) {
            return fields;
        }
        Object[] flattened = new Object[flattenedLength];
        int index = 0;
        for (Object field : fields) {
            if (field instanceof Object[] nested) {
                System.arraycopy(nested, 0, flattened, index, nested.length);
                index += nested.length;
            } else {
                flattened[index] = field;
                index++;
            }
        }
        return flattened;
    }

    private static String idleCommand(AltoClef mod) {
        return ChatClefDiagnostics.safeValueForDiagnosticLog(() ->
                mod == null || mod.getModSettings() == null ? "unavailable" : mod.getModSettings().getIdleCommand()
        );
    }

    private static String identity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
