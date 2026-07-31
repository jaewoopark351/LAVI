package lavi.minecraft.integration.carryon.snapshot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;

import java.util.List;
import java.util.StringJoiner;

//20260731_kpopmodder: Keep ChatClef task-chain state collection separate from player and input snapshots.
public final class CarryOnTaskSnapshotCollector {
    private CarryOnTaskSnapshotCollector() {
    }

    public static CarryOnTaskSnapshot collect(AltoClef mod, String childTask) {
        return new CarryOnTaskSnapshot(
                topLevelTask(mod),
                CarryOnSnapshotValues.value(childTask),
                currentChain(mod),
                taskChain(mod),
                taskRunnerActive(mod),
                userTaskChainActive(mod),
                paused(mod),
                chatClefEnabled(mod),
                playerMode(mod)
        );
    }

    private static String topLevelTask(AltoClef mod) {
        if (mod == null || mod.getUserTaskChain() == null || mod.getUserTaskChain().getCurrentTask() == null) {
            return "unavailable";
        }
        return CarryOnSnapshotValues.taskName(mod.getUserTaskChain().getCurrentTask());
    }

    private static String currentChain(AltoClef mod) {
        try {
            if (mod == null || mod.getTaskRunner() == null || mod.getTaskRunner().getCurrentTaskChain() == null) {
                return "unavailable";
            }
            return CarryOnSnapshotValues.value(mod.getTaskRunner().getCurrentTaskChain().getName());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String taskChain(AltoClef mod) {
        try {
            if (mod == null || mod.getTaskRunner() == null || mod.getTaskRunner().getCurrentTaskChain() == null) {
                return "unavailable";
            }
            TaskChain chain = mod.getTaskRunner().getCurrentTaskChain();
            List<Task> tasks = chain.getTasks();
            if (tasks == null || tasks.isEmpty()) {
                return "empty";
            }
            StringJoiner joiner = new StringJoiner(" > ");
            for (Task task : tasks) {
                joiner.add(CarryOnSnapshotValues.value(task).replace('\n', ' '));
            }
            return joiner.toString();
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String taskRunnerActive(AltoClef mod) {
        try {
            return mod == null || mod.getTaskRunner() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getTaskRunner().isActive());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String userTaskChainActive(AltoClef mod) {
        try {
            return mod == null || mod.getUserTaskChain() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getUserTaskChain().isActive());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String paused(AltoClef mod) {
        try {
            return mod == null ? "unavailable" : Boolean.toString(mod.isPaused());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String chatClefEnabled(AltoClef mod) {
        try {
            return mod == null || mod.getAiBridge() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getAiBridge().getEnabled());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String playerMode(AltoClef mod) {
        try {
            return mod == null || mod.getAiBridge() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getAiBridge().getPlayerMode());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }
}
