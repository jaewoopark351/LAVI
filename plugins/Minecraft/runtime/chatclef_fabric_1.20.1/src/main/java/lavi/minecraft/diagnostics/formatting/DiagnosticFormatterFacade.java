package lavi.minecraft.diagnostics.formatting;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

//20260803_kpopmodder: Keep diagnostic formatter delegation separate from the public ChatClef diagnostics facade.
public final class DiagnosticFormatterFacade {
    private final BooleanSupplier diagnosticsOff;
    private final Function<Task, String> taskInstanceIdLabel;
    private final Function<Task, String> taskRunIdLabel;

    public DiagnosticFormatterFacade(BooleanSupplier diagnosticsOff,
                                     Function<Task, String> taskInstanceIdLabel,
                                     Function<Task, String> taskRunIdLabel) {
        this.diagnosticsOff = diagnosticsOff;
        this.taskInstanceIdLabel = taskInstanceIdLabel;
        this.taskRunIdLabel = taskRunIdLabel;
    }

    public String className(Object value) {
        return DiagnosticValueFormatter.className(value);
    }

    public String safeValue(Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(
                () -> supplier == null ? null : supplier.get(),
                !isOff()
        );
    }

    public String safeValueForDiagnosticLog(Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(
                () -> supplier == null ? null : supplier.get(),
                true
        );
    }

    public String taskSummary(Task task) {
        if (isOff()) {
            return "unavailable";
        }
        return taskSummaryForDiagnosticLog(task);
    }

    public String taskSummaryForDiagnosticLog(Task task) {
        return DiagnosticGameStateFormatter.taskSummary(task, taskInstanceIdLabel, taskRunIdLabel);
    }

    public String entitySummary(Entity entity) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.entitySummary(entity);
    }

    public String entityDistanceSqrToPlayer(AltoClef mod, Entity entity) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.entityDistanceSqrToPlayer(mod, entity);
    }

    public String playerPosition(AltoClef mod) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.playerPosition(mod);
    }

    public String vec3d(Vec3d pos) {
        return DiagnosticGameStateFormatter.vec3d(pos);
    }

    public String blockPos(BlockPos pos) {
        return DiagnosticGameStateFormatter.blockPos(pos);
    }

    public String itemStackSummary(ItemStack stack) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.itemStackSummary(stack);
    }

    public String slotSummary(Slot slot) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotSummary(slot);
    }

    public String slotStackSummary(Slot slot) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotStackSummary(slot);
    }

    public String itemTargets(ItemTarget[] targets) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.itemTargets(targets);
    }

    public String classList(Class<?>[] classes) {
        if (isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.classList(classes);
    }

    public String chainName(TaskChain chain) {
        if (isOff()) {
            return "unavailable";
        }
        return chainNameForDiagnosticLog(chain);
    }

    public String chainNameForDiagnosticLog(TaskChain chain) {
        if (chain == null) {
            return "unavailable";
        }
        try {
            return chain.getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public String value(Object rawValue) {
        return DiagnosticValueFormatter.value(rawValue);
    }

    private boolean isOff() {
        return diagnosticsOff.getAsBoolean();
    }
}
