package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasks.container.DepositAllTask;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPostPlaceHandoff;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

//20260831_kpopmodder: Isolate inspection of production DepositAllTask handoff collaborators.
final class DepositAllTaskHandoffAccess {
    private DepositAllTaskHandoffAccess() {
    }

    static DepositAllPlacementTaskOwner placementOwner(DepositAllTask task) {
        return field(task, "_placementTaskOwner", DepositAllPlacementTaskOwner.class);
    }

    static DepositAllPostPlaceHandoff postPlaceHandoff(DepositAllTask task) {
        return field(task, "_postPlaceHandoff", DepositAllPostPlaceHandoff.class);
    }

    static boolean deferAfterCompletedPlacement(DepositAllTask task) {
        try {
            Method method = DepositAllTask.class.getDeclaredMethod("deferAfterCompletedPlacement");
            method.setAccessible(true);
            return (boolean) method.invoke(task);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Failed to invoke DepositAllTask.deferAfterCompletedPlacement",
                    exception
            );
        }
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
