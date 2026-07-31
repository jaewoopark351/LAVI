package lavi.minecraft.integration.carryon;

import net.minecraft.client.MinecraftClient;

public final class CarryOnDiagnostics {
    private static final CarryOnStateReader STATE_READER = new ReflectiveCarryOnStateReader();

    private CarryOnDiagnostics() {
    }

    public static long nextTaskInstanceId() {
        return CarryOnOperationIds.nextTaskInstanceId();
    }

    public static CarryOnDiagnosticSession startSession(long taskInstanceId,
                                                        CarryOnOperationType operationType,
                                                        CarryOnTransition expectedTransition,
                                                        String childTask,
                                                        String targetType,
                                                        String targetId,
                                                        String targetPosition) {
        return CarryOnDiagnosticSession.start(
                taskInstanceId,
                CarryOnOperationIds.nextOperationId(),
                operationType,
                expectedTransition,
                childTask,
                targetType,
                targetId,
                targetPosition,
                STATE_READER
        );
    }

    //20260730_kpopmodder: Keep Carry On observation available only through the LAVI-owned optional bridge.
    public static CarryOnObservation observe() {
        return observe(MinecraftClient.getInstance());
    }

    private static CarryOnObservation observe(MinecraftClient client) {
        return STATE_READER.observe(client == null ? null : client.player);
    }
}
