package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

public abstract class SingleTaskChain extends TaskChain {

    protected Task mainTask = null;
    private boolean interrupted = false;

    private final AltoClef mod;

    public SingleTaskChain(TaskRunner runner) {
        super(runner);
        mod = runner.getMod();
    }

    @Override
    protected void onTick() {
        if (!isActive()) {
            ChatClefDiagnostics.logEvent("SINGLE_TASK_CHAIN", "SKIP", "inactive_chain", null,
                    "chain", ChatClefDiagnostics.chainName(this));
            return;
        }

        if (interrupted) {
            ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_interrupted_reset_begin",
                    "chain", ChatClefDiagnostics.chainName(this));
            interrupted = false;
            if (mainTask != null) {
                mainTask.reset();
                ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_main_task_reset",
                        "chain", ChatClefDiagnostics.chainName(this));
            }
        }

        if (mainTask != null) {
            boolean mainTaskFinished = mainTask.isFinished();
            boolean mainTaskStopped = false;
            if (!mainTaskFinished) {
                mainTaskStopped = mainTask.stopped();
            }
            ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_main_task_state",
                    "chain", ChatClefDiagnostics.chainName(this),
                    "mainTaskFinished", mainTaskFinished,
                    "mainTaskStoppedEvaluated", !mainTaskFinished,
                    "mainTaskStopped", mainTaskStopped);
            if ((mainTaskFinished) || mainTaskStopped) {
                ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_onTaskFinish_begin",
                        "chain", ChatClefDiagnostics.chainName(this));
                onTaskFinish(mod);
                ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_onTaskFinish_end",
                        "chain", ChatClefDiagnostics.chainName(this));
            } else {
                ChatClefDiagnostics.logTaskTransition(mainTask, null, mainTask, "single_task_chain_main_task_tick_begin",
                        "chain", ChatClefDiagnostics.chainName(this));
                mainTask.tick(this);
                ChatClefDiagnostics.logTaskTransition(mainTask, null, mainTask, "single_task_chain_main_task_tick_end",
                        "chain", ChatClefDiagnostics.chainName(this));
            }
        }
    }

    protected void onStop() {
        if (isActive() && mainTask != null) {
            ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_onStop_main_task_stop_begin",
                    "chain", ChatClefDiagnostics.chainName(this));
            mainTask.stop();
            ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_onStop_main_task_stop_end",
                    "chain", ChatClefDiagnostics.chainName(this));
            mainTask = null;
            ChatClefDiagnostics.logEvent("SINGLE_TASK_CHAIN", "TASK_CLEARED", "single_task_chain_onStop_main_task_cleared", null,
                    "chain", ChatClefDiagnostics.chainName(this));
        }
    }

    public void setTask(Task task) {
        boolean taskChanged = mainTask == null || !mainTask.equals(task);
        ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, task, "single_task_chain_setTask_decision",
                "chain", ChatClefDiagnostics.chainName(this),
                "taskChanged", taskChanged);
        if (taskChanged) {
            if (mainTask != null) {
                ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, task, "single_task_chain_setTask_previous_stop_begin",
                        "chain", ChatClefDiagnostics.chainName(this));
                mainTask.stop(task);
                ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, task, "single_task_chain_setTask_previous_stop_end",
                        "chain", ChatClefDiagnostics.chainName(this));
            }
            mainTask = task;
            ChatClefDiagnostics.logTaskTransition(mainTask, null, mainTask, "single_task_chain_setTask_assigned",
                    "chain", ChatClefDiagnostics.chainName(this));
            if (task != null) {
                task.reset();
                ChatClefDiagnostics.logTaskTransition(task, null, task, "single_task_chain_setTask_reset_new_task",
                        "chain", ChatClefDiagnostics.chainName(this));
            }
        }
    }


    @Override
    public boolean isActive() {
        return mainTask != null;
    }

    protected abstract void onTaskFinish(AltoClef mod);

    @Override
    public void onInterrupt(TaskChain other) {
        if (other != null) {
            Debug.logInternal("Chain Interrupted: " + this + " by " + other);
        }
        ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_onInterrupt_begin",
                "chain", ChatClefDiagnostics.chainName(this),
                "interruptingChain", ChatClefDiagnostics.chainName(other));
        // Stop our task. When we're started up again, let our task know we need to run.
        interrupted = true;
        if (mainTask != null && mainTask.isActive()) {
            ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_main_task_interrupt_begin",
                    "chain", ChatClefDiagnostics.chainName(this));
            mainTask.interrupt(null);
            ChatClefDiagnostics.logTaskTransition(mainTask, mainTask, null, "single_task_chain_main_task_interrupt_end",
                    "chain", ChatClefDiagnostics.chainName(this));
        }
    }

    protected boolean isCurrentlyRunning(AltoClef mod) {
        return !interrupted && mainTask.isActive() && !mainTask.isFinished();
    }

    public Task getCurrentTask() {
        return mainTask;
    }
}
