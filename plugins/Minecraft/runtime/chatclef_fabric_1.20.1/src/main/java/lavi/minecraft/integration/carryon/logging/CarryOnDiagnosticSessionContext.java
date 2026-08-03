package lavi.minecraft.integration.carryon.logging;

import lavi.minecraft.integration.carryon.CarryOnOperationType;
import lavi.minecraft.integration.carryon.CarryOnTransition;

//20260804_kpopmodder: Keep Carry On diagnostic session identity separate from tick and observation state.
public final class CarryOnDiagnosticSessionContext {
    private final long taskInstanceId;
    private final long operationId;
    private final CarryOnOperationType operationType;
    private final CarryOnTransition expectedTransition;
    private final String childTask;
    private final String targetType;
    private final String targetId;
    private final String targetPosition;

    public CarryOnDiagnosticSessionContext(long taskInstanceId,
                                           long operationId,
                                           CarryOnOperationType operationType,
                                           CarryOnTransition expectedTransition,
                                           String childTask,
                                           String targetType,
                                           String targetId,
                                           String targetPosition) {
        this.taskInstanceId = taskInstanceId;
        this.operationId = operationId;
        this.operationType = operationType;
        this.expectedTransition = expectedTransition;
        this.childTask = childTask;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetPosition = targetPosition;
    }

    public long taskInstanceId() {
        return taskInstanceId;
    }

    public long operationId() {
        return operationId;
    }

    public CarryOnOperationType operationType() {
        return operationType;
    }

    public CarryOnTransition expectedTransition() {
        return expectedTransition;
    }

    public String childTask() {
        return childTask;
    }

    public String targetType() {
        return targetType;
    }

    public String targetId() {
        return targetId;
    }

    public String targetPosition() {
        return targetPosition;
    }
}
