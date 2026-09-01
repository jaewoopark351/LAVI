package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasks.container.DepositAllTask;
import lavi.minecraft.task.container.deposit.DepositAllContainerTargetState;
import lavi.minecraft.task.container.deposit.DepositAllStoreTaskGeneration;

import java.lang.reflect.Field;

//20260831_kpopmodder: Isolate inspection of production DepositAllTask operation-state identities.
final class DepositAllTaskOperationStateAccess {
    private DepositAllTaskOperationStateAccess() {
    }

    static DepositAllContainerTargetState targetState(DepositAllTask task) {
        return field(task, "_targetState", DepositAllContainerTargetState.class);
    }

    static DepositAllStoreTaskGeneration storeTaskGeneration(DepositAllTask task) {
        return field(task, "_storeTaskGeneration", DepositAllStoreTaskGeneration.class);
    }

    private static <T> T field(DepositAllTask task, String fieldName, Class<T> fieldType) {
        try {
            Field field = DepositAllTask.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(task));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect DepositAllTask." + fieldName, exception);
        }
    }
}
