package lavi.minecraft.diagnostics;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;

//20260803_kpopmodder: Keep diagnostic field construction out of the public ChatClef diagnostics facade.
final class DiagnosticContextBuilder {
    private final DiagnosticModeController mode;
    private final DiagnosticTaskRegistry tasks;

    DiagnosticContextBuilder(DiagnosticModeController mode, DiagnosticTaskRegistry tasks) {
        this.mode = mode;
        this.tasks = tasks;
    }

    Object[] taskTransitionFields(Object[] fields, Task previousTask, Task nextTask) {
        return mergeFields(fields,
                "previousTask", taskName(previousTask),
                "previousTaskInstanceId", tasks.taskInstanceIdLabel(previousTask),
                "previousTaskRunId", tasks.taskRunIdLabel(previousTask),
                "nextTask", taskName(nextTask),
                "nextTaskInstanceId", tasks.taskInstanceIdLabel(nextTask),
                "nextTaskRunId", tasks.taskRunIdLabel(nextTask));
    }

    Object[] inputFields(Object[] fields, Input input) {
        return mergeFields(fields,
                "input", DiagnosticInputState.inputName(input),
                "inputCallerStack", DiagnosticCallerStack.captureExcluding(ChatClefDiagnostics.class));
    }

    Object[] inputSnapshotFields(Object[] fields) {
        return mergeFields(DiagnosticInputState.snapshotFields(), fields);
    }

    Object[] slotClickFields(Object[] fields, Slot slot, int mouseButton, Object type) {
        return mergeFields(fields,
                "slot", slotSummary(slot),
                "mouseButton", Integer.toString(mouseButton),
                "slotActionType", value(type),
                "cursorStack", safeValue(() -> MinecraftClient.getInstance().player.currentScreenHandler.getCursorStack()));
    }

    private Object[] mergeFields(Object[] fields, Object... extra) {
        return DiagnosticEventEmitter.mergeFields(fields, extra);
    }

    private String slotSummary(Slot slot) {
        if (mode.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotSummary(slot);
    }

    private String taskName(Task task) {
        return DiagnosticEventEmitter.taskName(task);
    }

    private String safeValue(java.util.function.Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(
                () -> supplier == null ? null : supplier.get(),
                !mode.isOff()
        );
    }

    private String value(Object rawValue) {
        return DiagnosticValueFormatter.value(rawValue);
    }
}
