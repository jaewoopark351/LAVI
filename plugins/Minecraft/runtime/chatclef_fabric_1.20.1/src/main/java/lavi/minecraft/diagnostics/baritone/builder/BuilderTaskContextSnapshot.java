package lavi.minecraft.diagnostics.baritone.builder;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceRegistry;
import java.util.List;

//20260913_kpopmodder: Read cached chain identities only; never reevaluate priority, activity, or completion.
public final class BuilderTaskContextSnapshot {
    private BuilderTaskContextSnapshot() { }
    public static String capture() {
        try {
            AltoClef mod = AltoClef.getInstance();
            if (mod == null || mod.getTaskRunner() == null) return "UNAVAILABLE";
            TaskChain chain = mod.getTaskRunner().getCurrentTaskChain();
            if (chain == null) return "NO_CACHED_CHAIN";
            List<Task> tasks = chain.getTasks();
            if (tasks.isEmpty()) return chain.getClass().getSimpleName() + ":NO_CACHED_TASK";
            String text = "leaf=" + label(tasks.get(tasks.size() - 1))
                    + ";parent=" + (tasks.size() < 2 ? "none" : label(tasks.get(tasks.size() - 2)))
                    + ";root=" + label(tasks.get(0)) + ";chain=" + chain.getClass().getSimpleName();
            return BuilderTraceRegistry.bound(text, 256);
        } catch (RuntimeException | LinkageError error) { return "CAPTURE_FAILED"; }
    }
    private static String label(Task task) { return task.getClass().getSimpleName() + '@' + BuilderPathSnapshot.id(task); }
}
