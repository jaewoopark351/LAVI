package lavi.minecraft.integration.carryon;

import java.util.concurrent.atomic.AtomicLong;

//20260730_kpopmodder: Split Carry On diagnostic task identity from per-run operation identity.
public final class CarryOnOperationIds {
    private static final AtomicLong NEXT_TASK_INSTANCE_ID = new AtomicLong(1);
    private static final AtomicLong NEXT_OPERATION_ID = new AtomicLong(1);

    private CarryOnOperationIds() {
    }

    public static long nextTaskInstanceId() {
        return NEXT_TASK_INSTANCE_ID.getAndIncrement();
    }

    public static long nextOperationId() {
        return NEXT_OPERATION_ID.getAndIncrement();
    }
}
