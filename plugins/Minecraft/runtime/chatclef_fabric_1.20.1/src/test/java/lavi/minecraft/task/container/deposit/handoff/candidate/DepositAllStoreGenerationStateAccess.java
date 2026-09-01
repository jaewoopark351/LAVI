package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.DepositAllStoreTaskGeneration;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;

//20260831_kpopmodder: Prime test-only Store generation state without Minecraft registry bootstrap.
final class DepositAllStoreGenerationStateAccess {
    private DepositAllStoreGenerationStateAccess() {
    }

    static void prime(DepositAllStoreTaskGeneration generation,
                      BlockPos target,
                      ItemTarget[] snapshot,
                      Task task,
                      int generationId) {
        set(generation, "activeStoreTarget", target);
        set(generation, "activeStoreSnapshot", snapshot);
        set(generation, "activeStoreTask", task);
        set(generation, "generationId", generationId);
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to prime " + fieldName, exception);
        }
    }
}
