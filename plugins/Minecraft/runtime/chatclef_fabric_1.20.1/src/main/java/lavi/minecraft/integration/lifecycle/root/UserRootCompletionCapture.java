//#if MC == 12001
package lavi.minecraft.integration.lifecycle.root;

import adris.altoclef.tasksystem.Task;

//20260913_kpopmodder: Read stopped only at the authoritative onTaskFinish entry, never call isFinished again.
public final class UserRootCompletionCapture {
    private UserRootCompletionCapture() { }
    public static UserRootCompletion capture(Task task) {
        if (task == null) return UserRootCompletion.unavailable("root_absent");
        try {
            boolean stopped = task.stopped();
            return UserRootCompletion.known(stopped, stopped
                    ? lavi.minecraft.integration.lifecycle.root.failure.GoldMiningFailureCapture.read(task) : "");
        } catch (RuntimeException error) {
            return UserRootCompletion.unavailable(error.getClass().getSimpleName());
        }
    }
}
//#endif
