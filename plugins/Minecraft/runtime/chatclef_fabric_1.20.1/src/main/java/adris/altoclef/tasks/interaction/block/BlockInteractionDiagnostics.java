package adris.altoclef.tasks.interaction.block;

import adris.altoclef.tasksystem.Task;

//20260730_kpopmodder: Added this diagnostics helper so task-flow logging can stay small and consistent.
final class BlockInteractionDiagnostics {

    private BlockInteractionDiagnostics() {
    }

    static String describeTask(Task task) {
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getSimpleName() + "{" + task + "}";
        } catch (RuntimeException ex) {
            return task.getClass().getSimpleName() + "{debugString failed: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "}";
        }
    }
}
