package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.time.Stopwatch;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.tasktrace.UserTaskChainDiagnostics;
import lavi.minecraft.diagnostics.tasktrace.userchain.UserTaskChainDiagnosticLedger;
import lavi.minecraft.diagnostics.tasktrace.userchain.UserTaskChainDiagnosticLedger.CancelInvocation;
import lavi.minecraft.diagnostics.tasktrace.userchain.UserTaskChainDiagnosticLedger.FinishTrigger;
import lavi.minecraft.diagnostics.tasktrace.userchain.UserTaskChainDiagnosticLedger.RootAssignment;

// A task chain that runs a user defined task at the same priority.
// This basically replaces our old Task Runner.
public class UserTaskChain extends SingleTaskChain {

    private final Stopwatch taskStopwatch = new Stopwatch();
    private final UserTaskChainDiagnosticLedger diagnosticLedger = new UserTaskChainDiagnosticLedger();
    private Runnable currentOnFinish = null;

    private boolean runningIdleTask;
    private boolean nextTaskIdleFlag;

    public UserTaskChain(TaskRunner runner) {
        super(runner);
    }

    private static String prettyPrintTimeDuration(double seconds) {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        int days = hours / 24;

        String result = "";
        if (days != 0) {
            result += days + " days ";
        }
        if (hours != 0) {
            result += (hours % 24) + " hours ";
        }
        if (minutes != 0) {
            result += (minutes % 60) + " minutes ";
        }
        if (!result.isEmpty()) {
            result += "and ";
        }
        result += String.format("%.3f", (seconds % 60));
        return result;
    }

    @Override
    protected void onTick() {

        // Pause if we're not loaded into a world.
        if (!AltoClef.inGame()) return;

        super.onTick();
    }

    public void cancel(AltoClef mod) {
        boolean willCallOnTaskFinish = mainTask != null && mainTask.isActive();
        CancelInvocation cancelInvocation = diagnosticLedger.beginCancel(
                mainTask,
                ChatClefDiagnostics.safeValueForDiagnosticLog(() -> getTasks())
        );
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_CANCEL_REQUESTED", "user_task_chain_cancel_requested", mainTask,
                UserTaskChainDiagnostics.withOriginAndCommandContext(mod, mainTask, null, runningIdleTask, nextTaskIdleFlag,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "mainTaskPresent", mainTask != null,
                        "mainTaskActive", ChatClefDiagnostics.safeValueForDiagnosticLog(() -> mainTask != null && mainTask.isActive()),
                        "willCallOnTaskFinish", willCallOnTaskFinish,
                        "runningIdleTask", runningIdleTask,
                        "nextTaskIdleFlag", nextTaskIdleFlag,
                        cancelInvocation.beforeStopFields()));
        if (mainTask != null && mainTask.isActive()) {
            diagnosticLedger.markCancelWillCallOnTaskFinish(cancelInvocation);
            StoreDepositDiagnostics.markExplicitCancelCandidate(mainTask);
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_CANCEL_STOP_BEGIN", "user_task_chain_cancel_stop_begin", mainTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, mainTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            cancelInvocation.beforeStopFields()));
            stop();
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_CANCEL_STOP_END", "user_task_chain_cancel_stop_end", cancelInvocation.rootBeforeStop(),
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, cancelInvocation.rootBeforeStop(), mainTask, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            cancelInvocation.afterStopFields(mainTask)));
            onTaskFinish(mod);
        }
    }

    @Override
    public float getPriority() {
        return 50;
    }

    @Override
    public String getName() {
        return "User Tasks";
    }

    public void runTask(AltoClef mod, Task task, Runnable onFinish) {
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        // Diagnostics-only: observe task assignment and idle classification without changing lifecycle behavior.
        RootAssignment rootAssignment = diagnosticLedger.beginRootAssignment(mainTask, task);
        UserTaskChainDiagnostics.logTaskOriginDecision(mod, mainTask, task, runningIdleTask, nextTaskIdleFlag, onFinish != null,
                rootAssignment.beforeFields());
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_RUN_TASK_ENTER", "user_task_chain_run_task_enter", task,
                UserTaskChainDiagnostics.withOriginAndCommandContext(mod, mainTask, task, runningIdleTask, nextTaskIdleFlag,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "previousMainTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask),
                        "incomingTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task),
                        "previousRunningIdleTask", runningIdleTask,
                        "previousNextTaskIdleFlag", nextTaskIdleFlag,
                        "incomingOnFinishPresent", onFinish != null,
                        rootAssignment.beforeFields()));
        runningIdleTask = nextTaskIdleFlag;
        nextTaskIdleFlag = false;

        currentOnFinish = onFinish;

        if (!runningIdleTask) {
            Debug.logMessage("User Task Set: " + task.toString());
        }
        mod.getTaskRunner().enable();
        taskStopwatch.begin();
        setTask(task);
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_RUN_TASK_ASSIGNED", "user_task_chain_run_task_assigned", task,
                UserTaskChainDiagnostics.withOriginAndCommandContext(mod, null, task, runningIdleTask, nextTaskIdleFlag,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "mainTaskAfterSetTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask),
                        "runningIdleTask", runningIdleTask,
                        "nextTaskIdleFlag", nextTaskIdleFlag,
                        "currentOnFinishPresent", currentOnFinish != null,
                        diagnosticLedger.commitRootAssignment(rootAssignment, mainTask)));

        if (mod.getModSettings().failedToLoad()) {
            Debug.logWarning("Settings file failed to load at some point. Check logs for more info, or delete the" +
                    " file to re-load working settings.");
        }
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        boolean shouldIdle = mod.getModSettings().shouldRunIdleCommandWhenNotActive();
        double seconds = taskStopwatch.time();
        Task oldTask = mainTask;
        FinishTrigger finishTrigger = diagnosticLedger.consumeFinishTrigger(oldTask);
        //20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
        // Diagnostics-only: observe callback and TaskFinishedEvent publish decisions without changing completion logic.
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_ON_TASK_FINISH_ENTER", "user_task_chain_on_task_finish_enter", oldTask,
                UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                        "shouldIdle", shouldIdle,
                        "runningIdleTask", runningIdleTask,
                        "nextTaskIdleFlag", nextTaskIdleFlag,
                        "currentOnFinishPresent", currentOnFinish != null,
                        "elapsedSeconds", seconds,
                        finishTrigger.fields(oldTask)));
        mainTask = null;
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_MAIN_TASK_CLEARED", "user_task_chain_main_task_cleared", oldTask,
                UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                        "mainTaskAfterClear", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask),
                        "shouldIdle", shouldIdle));
        if (!shouldIdle) {
            // Stop.
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_STOP_MOD_BEGIN", "user_task_chain_stop_mod_begin", oldTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask)));
            mod.stop();
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_STOP_MOD_END", "user_task_chain_stop_mod_end", oldTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask)));
        } else {
            // disable baritone at least
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_IDLE_CLEANUP_BEGIN", "user_task_chain_idle_cleanup_begin", oldTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask)));
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getClientBaritone().getInputOverrideHandler().clearAllKeys();    
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_IDLE_CLEANUP_END", "user_task_chain_idle_cleanup_end", oldTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask)));
        }
        if (currentOnFinish != null) {
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_ON_FINISH_CALLBACK_BEGIN", "user_task_chain_on_finish_callback_begin", oldTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                            "mainTaskBeforeCallback", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask)));
            currentOnFinish.run();
            ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_ON_FINISH_CALLBACK_END", "user_task_chain_on_finish_callback_end", oldTask,
                    UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                            "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                            "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                            "mainTaskAfterCallback", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask)));
        }
        // our `onFinish` might have triggered more tasks.
        boolean actuallyDone = mainTask == null;
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_ACTUALLY_DONE_DECISION", "user_task_chain_actually_done_decision", oldTask,
                UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, mainTask, runningIdleTask, nextTaskIdleFlag,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                        "mainTaskAfterCallback", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask),
                        "actuallyDone", actuallyDone,
                        "runningIdleTask", runningIdleTask,
                        "willPublishTaskFinishedEvent", actuallyDone && !runningIdleTask,
                        "willStartIdleCommand", actuallyDone && shouldIdle));
        if (actuallyDone) {
            if (!runningIdleTask) {
                Debug.logMessage("User task FINISHED. Took %s seconds.", prettyPrintTimeDuration(seconds));
                ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_TASK_FINISHED_EVENT_PUBLISH_BEGIN", "user_task_chain_task_finished_event_publish_begin", oldTask,
                        UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                                "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                                "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                                "elapsedSeconds", seconds,
                                "runningIdleTask", runningIdleTask,
                                finishTrigger.fields(oldTask)));
                TaskFinishedEvent finishedEvent = new TaskFinishedEvent(seconds, oldTask);
                ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_TASK_FINISHED_EVENT_READY", "user_task_chain_task_finished_event_ready", oldTask,
                        UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                                "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                                "event_present", true,
                                "event_identity", Integer.toHexString(System.identityHashCode(finishedEvent)),
                                "event_task_present", oldTask != null,
                                "event_task_identity", UserTaskChainDiagnosticLedger.taskIdentity(oldTask),
                                "task_absence_reason", oldTask == null ? "event_last_task_null" : "task_present",
                                finishTrigger.fields(oldTask)));
                EventBus.publish(finishedEvent);
                ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_TASK_FINISHED_EVENT_PUBLISH_END", "user_task_chain_task_finished_event_publish_end", oldTask,
                        UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, runningIdleTask, nextTaskIdleFlag,
                                "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                                "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                                "elapsedSeconds", seconds,
                                "runningIdleTask", runningIdleTask,
                                "event_present", true,
                                "event_identity", Integer.toHexString(System.identityHashCode(finishedEvent)),
                                "event_task_present", oldTask != null,
                                "event_task_identity", UserTaskChainDiagnosticLedger.taskIdentity(oldTask),
                                "task_absence_reason", oldTask == null ? "event_last_task_null" : "task_present",
                                finishTrigger.fields(oldTask)));
            }
            if (shouldIdle) {
                ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_IDLE_COMMAND_BEGIN", "user_task_chain_idle_command_begin", oldTask,
                        UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, null, true, nextTaskIdleFlag,
                                "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                                "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                                "idleCommand", mod.getModSettings().getIdleCommand()));
                //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
                // Mark the synchronous idle command before it creates its IdleTask so the flag is consumed by the idle task itself.
                signalNextTaskToBeIdleTask();
                AltoClef.getCommandExecutor().executeWithPrefix(mod.getModSettings().getIdleCommand());
                runningIdleTask = true;
                ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_IDLE_COMMAND_END", "user_task_chain_idle_command_end", oldTask,
                        UserTaskChainDiagnostics.withOriginAndCommandContext(mod, oldTask, mainTask, runningIdleTask, nextTaskIdleFlag,
                                "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                                "oldTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(oldTask),
                                "mainTaskAfterIdleCommand", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask),
                                "runningIdleTask", runningIdleTask,
                                "nextTaskIdleFlag", nextTaskIdleFlag));
            }
        }
    }

    public boolean isRunningIdleTask() {
        return isActive() && runningIdleTask;
    }

    public String diagnosticRootAssignmentId() {
        return diagnosticLedger.currentRootAssignmentId();
    }

    public long diagnosticRootGeneration() {
        return diagnosticLedger.currentRootGeneration();
    }

    public boolean diagnosticRunningIdleTaskFlag() {
        return runningIdleTask;
    }

    public boolean diagnosticNextTaskIdleFlag() {
        return nextTaskIdleFlag;
    }

    // The next task will be an idle task.
    public void signalNextTaskToBeIdleTask() {
        ChatClefDiagnostics.logLifecycleBoundary("USER_TASK_CHAIN_SIGNAL_NEXT_IDLE", "user_task_chain_signal_next_idle", mainTask,
                UserTaskChainDiagnostics.withOriginAndCommandContext(AltoClef.getInstance(), mainTask, null, runningIdleTask, true,
                        "chain", ChatClefDiagnostics.chainNameForDiagnosticLog(this),
                        "mainTask", ChatClefDiagnostics.taskSummaryForDiagnosticLog(mainTask),
                        "previousNextTaskIdleFlag", nextTaskIdleFlag,
                        "runningIdleTask", runningIdleTask));
        nextTaskIdleFlag = true;
    }
}
