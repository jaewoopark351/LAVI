package lavi.minecraft.diagnostics.tasktrace.userchain;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.StringJoiner;

//20260808_kpopmodder: Track UserTaskChain ownership ids for diagnostics without changing task behavior.
public final class UserTaskChainDiagnosticLedger {
    private static final int MAX_CALLER_FRAMES = 8;
    private long currentRootGeneration;
    private long nextCancelInvocationId;
    private long pendingCancelInvocationId;
    private String pendingCancelCallerSummary = "";
    private String pendingFinishTriggerHint = "natural_task_state_transition";
    private String rootAssignmentId = "none";

    public synchronized RootAssignment beginRootAssignment(Task previousRoot, Task incomingRoot) {
        long nextGeneration = currentRootGeneration + 1L;
        return new RootAssignment(nextGeneration, rootAssignmentId(nextGeneration), previousRoot, incomingRoot);
    }

    public synchronized Object[] commitRootAssignment(RootAssignment assignment, Task assignedRoot) {
        currentRootGeneration = assignment.rootGeneration();
        rootAssignmentId = assignment.rootAssignmentId();
        return new Object[]{
                "rootAssignmentId", rootAssignmentId,
                "rootGeneration", currentRootGeneration,
                "assignedRootClass", ChatClefDiagnostics.className(assignedRoot),
                "assignedRootIdentity", taskIdentity(assignedRoot)
        };
    }

    public synchronized CancelInvocation beginCancel(Task rootBeforeStop, String selectedChainTaskPathBeforeStop) {
        long cancelInvocationId = ++nextCancelInvocationId;
        String callerSummary = callerSummary();
        return new CancelInvocation(
                cancelInvocationId,
                callerSummary,
                rootBeforeStop,
                selectedChainTaskPathBeforeStop,
                rootAssignmentId,
                currentRootGeneration
        );
    }

    public synchronized void markCancelWillCallOnTaskFinish(CancelInvocation cancelInvocation) {
        pendingCancelInvocationId = cancelInvocation.cancelInvocationId();
        pendingCancelCallerSummary = cancelInvocation.cancelCallerSummary();
        pendingFinishTriggerHint = "cancel_after_chain_stop";
    }

    public synchronized FinishTrigger consumeFinishTrigger(Task rootAtEntry) {
        FinishTrigger trigger = new FinishTrigger(
                pendingCancelInvocationId,
                pendingCancelCallerSummary,
                pendingFinishTriggerHint,
                rootAtEntry,
                rootAssignmentId,
                currentRootGeneration
        );
        pendingCancelInvocationId = 0L;
        pendingCancelCallerSummary = "";
        pendingFinishTriggerHint = "natural_task_state_transition";
        return trigger;
    }

    public synchronized String currentRootAssignmentId() {
        return rootAssignmentId;
    }

    public synchronized long currentRootGeneration() {
        return currentRootGeneration;
    }

    public static String taskIdentity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }

    private static String rootAssignmentId(long generation) {
        return "user-root-" + generation;
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
                || className.equals(UserTaskChainDiagnosticLedger.class.getName())
                || className.equals("adris.altoclef.chains.UserTaskChain");
    }

    public record RootAssignment(
            long rootGeneration,
            String rootAssignmentId,
            Task previousRoot,
            Task incomingRoot
    ) {
        public Object[] beforeFields() {
            return new Object[]{
                    "rootAssignmentId", rootAssignmentId,
                    "rootGeneration", rootGeneration,
                    "previousRootClass", ChatClefDiagnostics.className(previousRoot),
                    "previousRootIdentity", taskIdentity(previousRoot),
                    "incomingRootClass", ChatClefDiagnostics.className(incomingRoot),
                    "incomingRootIdentity", taskIdentity(incomingRoot)
            };
        }
    }

    public record CancelInvocation(
            long cancelInvocationId,
            String cancelCallerSummary,
            Task rootBeforeStop,
            String selectedChainTaskPathBeforeStop,
            String rootAssignmentId,
            long rootGeneration
    ) {
        public Object[] beforeStopFields() {
            return new Object[]{
                    "cancelInvocationId", cancelInvocationId,
                    "cancelCallerSummary", cancelCallerSummary,
                    "rootAssignmentId", rootAssignmentId,
                    "rootGeneration", rootGeneration,
                    "rootBeforeStopClass", ChatClefDiagnostics.className(rootBeforeStop),
                    "rootBeforeStopIdentity", taskIdentity(rootBeforeStop),
                    "rootBeforeStopActive", ChatClefDiagnostics.safeValueForDiagnosticLog(() ->
                            rootBeforeStop != null && rootBeforeStop.isActive()
                    ),
                    "rootBeforeStopStopped", ChatClefDiagnostics.safeValueForDiagnosticLog(() ->
                            rootBeforeStop != null && rootBeforeStop.stopped()
                    ),
                    "selectedChainTaskPathBeforeStop", selectedChainTaskPathBeforeStop
            };
        }

        public Object[] afterStopFields(Task rootAfterStop) {
            return new Object[]{
                    "cancelInvocationId", cancelInvocationId,
                    "cancelCallerSummary", cancelCallerSummary,
                    "rootAssignmentId", rootAssignmentId,
                    "rootGeneration", rootGeneration,
                    "rootBeforeStopClass", ChatClefDiagnostics.className(rootBeforeStop),
                    "rootBeforeStopIdentity", taskIdentity(rootBeforeStop),
                    "rootAfterStopClass", ChatClefDiagnostics.className(rootAfterStop),
                    "rootAfterStopIdentity", taskIdentity(rootAfterStop),
                    "selectedChainTaskPathBeforeStop", selectedChainTaskPathBeforeStop
            };
        }
    }

    public record FinishTrigger(
            long cancelInvocationId,
            String cancelCallerSummary,
            String finishTriggerHint,
            Task rootAtOnTaskFinishEntry,
            String rootAssignmentId,
            long rootGeneration
    ) {
        public Object[] fields(Task oldTask) {
            return new Object[]{
                    "cancelInvocationId", cancelInvocationId,
                    "cancelCallerSummary", cancelCallerSummary,
                    "finishTriggerHint", finishTriggerHint,
                    "rootAssignmentId", rootAssignmentId,
                    "rootGeneration", rootGeneration,
                    "rootAtOnTaskFinishEntryClass", ChatClefDiagnostics.className(rootAtOnTaskFinishEntry),
                    "rootAtOnTaskFinishEntryIdentity", taskIdentity(rootAtOnTaskFinishEntry),
                    "oldTaskPresent", oldTask != null,
                    "oldTaskIdentity", taskIdentity(oldTask)
            };
        }
    }
}
