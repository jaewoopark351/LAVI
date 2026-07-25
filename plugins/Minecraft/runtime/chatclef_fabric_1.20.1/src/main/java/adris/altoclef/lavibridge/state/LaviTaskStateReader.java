package adris.altoclef.lavibridge.state;

//20260725_kpopmodder: Added this reader for AltoClef task runner snapshots.

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LaviTaskStateReader {

    private final AltoClef mod;

    public LaviTaskStateReader(AltoClef mod) {
        this.mod = mod;
    }

    public Map<String, Object> taskStatus() {
        Map<String, Object> task = new LinkedHashMap<>();
        if (mod == null || mod.getTaskRunner() == null || mod.getUserTaskChain() == null) {
            task.put("available", false);
            return task;
        }

        Task currentTask = mod.getUserTaskChain().getCurrentTask();
        TaskChain currentChain = mod.getTaskRunner().getCurrentTaskChain();

        task.put("available", true);
        task.put("task_runner_active", mod.getTaskRunner().isActive());
        task.put("user_task_active", mod.getUserTaskChain().isActive());
        task.put("running_idle_task", mod.getUserTaskChain().isRunningIdleTask());
        task.put("paused", mod.isPaused());
        task.put("status_report", mod.getTaskRunner().statusReport);
        task.put("current_chain", currentChain == null ? null : currentChain.getName());
        task.put("current_task", currentTask == null ? null : currentTask.toString());
        task.put("user_task_chain", userTaskChain());
        return task;
    }

    private List<String> userTaskChain() {
        List<String> taskChain = new ArrayList<>();
        for (Task chainTask : mod.getUserTaskChain().getTasks()) {
            taskChain.add(chainTask.toString());
        }
        return taskChain;
    }
}
