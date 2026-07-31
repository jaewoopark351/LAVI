package adris.altoclef.tasksystem;

import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.function.Predicate;

public abstract class Task {

    private String oldDebugState = "";
    private String debugState = "";

    private Task sub = null;

    private boolean first = true;

    private boolean stopped = false;

    private boolean active = false;

    public void tick(TaskChain parentChain) {
        ChatClefDiagnostics.enterTask(this);
        boolean diagnosticsVerbose = ChatClefDiagnostics.isVerboseEnabled();
        try {
            parentChain.addTaskToChain(this);
            if (first) {
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.beginTaskRun(this, parentChain);
                }
                Debug.logInternal("Task START: " + this);
                active = true;
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logEvent("TASK", "ON_START_BEGIN", "first_tick", this,
                            "parentChain", ChatClefDiagnostics.chainName(parentChain));
                }
                onStart();
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logEvent("TASK", "ON_START_END", "first_tick", this,
                            "parentChain", ChatClefDiagnostics.chainName(parentChain));
                }
                first = false;
                stopped = false;
            }
            if (stopped) {
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logEvent("TASK", "SKIP", "stopped_before_onTick", this);
                }
                return;
            }

            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logEvent("TASK", "ON_TICK_BEGIN", "tick_begin", this,
                        "parentChain", ChatClefDiagnostics.chainName(parentChain));
            }
            Task newSub = onTick();
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, sub, newSub, "onTick_result");
            }
            // Debug state print
            if (!oldDebugState.equals(debugState)) {
                Debug.logInternal(toString());
                oldDebugState = debugState;
            }
            // We have a sub task
            if (newSub != null) {
                boolean subTasksEqual = newSub.isEqual(sub);
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logTaskTransition(this, sub, newSub, "child_isEqual_result",
                            "isEqualResult", subTasksEqual);
                }
                if (!subTasksEqual) {
                    boolean canInterrupt = canBeInterrupted(sub, newSub);
                    if (diagnosticsVerbose) {
                        ChatClefDiagnostics.logTaskTransition(this, sub, newSub, "child_interruptibility_result",
                                "canInterruptPreviousChild", canInterrupt);
                    }
                    if (canInterrupt) {
                        // Our sub task is new
                        if (sub != null) {
                            // Our previous sub must be interrupted.
                            if (diagnosticsVerbose) {
                                ChatClefDiagnostics.logTaskTransition(this, sub, newSub, "previous_child_stop_begin");
                            }
                            sub.stop(newSub);
                            if (diagnosticsVerbose) {
                                ChatClefDiagnostics.logTaskTransition(this, sub, newSub, "previous_child_stop_end");
                            }
                        }

                        sub = newSub;
                        ChatClefDiagnostics.setParent(sub, this);
                        if (diagnosticsVerbose) {
                            ChatClefDiagnostics.logTaskTransition(this, null, sub, "child_replaced");
                        }
                    }
                }

                // Run our child
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logEvent("TASK_CHILD", "TICK_BEGIN", "child_tick_begin", sub,
                            "parentChain", ChatClefDiagnostics.chainName(parentChain));
                }
                sub.tick(parentChain);
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logEvent("TASK_CHILD", "TICK_END", "child_tick_end", sub,
                            "parentChain", ChatClefDiagnostics.chainName(parentChain));
                }
            } else {
                // We are null
                boolean canInterrupt = sub == null || canBeInterrupted(sub, null);
                if (diagnosticsVerbose) {
                    ChatClefDiagnostics.logTaskTransition(this, sub, null, "null_child_result",
                            "canInterruptPreviousChild", canInterrupt);
                }
                if (sub != null && canInterrupt) {
                    // Our previous sub must be interrupted.
                    if (diagnosticsVerbose) {
                        ChatClefDiagnostics.logTaskTransition(this, sub, null, "previous_child_stop_begin");
                    }
                    sub.stop();
                    if (diagnosticsVerbose) {
                        ChatClefDiagnostics.logTaskTransition(this, sub, null, "previous_child_stop_end");
                    }
                    sub = null;
                    if (diagnosticsVerbose) {
                        ChatClefDiagnostics.logTaskTransition(this, null, null, "child_cleared");
                    }
                }
            }
        } finally {
            ChatClefDiagnostics.exitTask(this);
        }
    }

    public void reset() {
        first = true;
        active = false;
        stopped = false;
    }

    public void stop() {
        stop(null);
    }

    /**
     * Stops the task. Next time it's run it will run `onStart`
     */
    public void stop(Task interruptTask) {
        boolean diagnosticsVerbose = ChatClefDiagnostics.isVerboseEnabled();
        if (!active) {
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "stop_skipped_inactive");
            }
            return;
        }
        if (diagnosticsVerbose) {
            ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "stop_begin");
        }
        Debug.logInternal("Task STOP: " + this + ", interrupted by " + interruptTask);
        if (!first) {
            onStop(interruptTask);
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "onStop_end");
            }
        }

        if (sub != null && !sub.stopped()) {
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, sub, interruptTask, "child_stop_from_parent_stop_begin");
            }
            sub.stop(interruptTask);
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, sub, interruptTask, "child_stop_from_parent_stop_end");
            }
        }

        first = true;
        active = false;
        stopped = true;
        if (diagnosticsVerbose) {
            ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "stop_end");
        }
    }

    public void fail(String reason) {
        stop();
        Debug.logMessage("Task FAILED: " + reason);
    }

    /**
     * Lets the task know it's execution has been "suspended"
     * <p>
     * STILL RUNS `onStop`
     * <p>
     * Doesn't stop it all-together (meaning `isActive` still returns true)
     */
    public void interrupt(Task interruptTask) {
        boolean diagnosticsVerbose = ChatClefDiagnostics.isVerboseEnabled();
        if (!active) {
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "interrupt_skipped_inactive");
            }
            return;
        }
        if (diagnosticsVerbose) {
            ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "interrupt_begin");
        }
        if (!first) {
            onStop(interruptTask);
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "interrupt_onStop_end");
            }
        }

        if (sub != null && !sub.stopped()) {
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, sub, interruptTask, "child_interrupt_begin");
            }
            sub.interrupt(interruptTask);
            if (diagnosticsVerbose) {
                ChatClefDiagnostics.logTaskTransition(this, sub, interruptTask, "child_interrupt_end");
            }
        }

        first = true;
        if (diagnosticsVerbose) {
            ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "interrupt_end");
        }
    }

    protected void setDebugState(String state) {
        if (state == null) {
            state = "";
        }
        debugState = state;
    }

    // Virtual
    public boolean isFinished() {
        return false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean stopped() {
        return stopped;
    }

    protected abstract void onStart();

    protected abstract Task onTick();

    // interruptTask = null if the task stopped cleanly
    protected abstract void onStop(Task interruptTask);

    protected abstract boolean isEqual(Task other);

    protected abstract String toDebugString();

    @Override
    public String toString() {
        return "<" + toDebugString() + "> " + debugState;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task task) {
            return isEqual(task);
        }
        return false;
    }

    public boolean thisOrChildSatisfies(Predicate<Task> pred) {
        Task t = this;
        while (t != null) {
            if (pred.test(t)) return true;
            t = t.sub;
        }
        return false;
    }

    public boolean thisOrChildAreTimedOut() {
        return thisOrChildSatisfies(task -> task instanceof TimeoutWanderTask);
    }

    /**
     * Sometimes a task just can NOT be bothered to be interrupted right now.
     * For instance, if we're in mid air and MUST complete the parkour movement.
     */
    private boolean canBeInterrupted(Task subTask, Task toInterruptWith) {
        if (subTask == null) return true;
        // Our task can declare that is FORCES itself to be active NOW.
        return (subTask.thisOrChildSatisfies(task -> {
            if (task instanceof ITaskCanForce canForce) {
                return !canForce.shouldForce(toInterruptWith);
            }
            return true;
        }));
    }
}
