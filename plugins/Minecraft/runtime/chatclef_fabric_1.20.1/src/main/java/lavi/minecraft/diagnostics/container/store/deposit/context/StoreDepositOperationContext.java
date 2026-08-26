package lavi.minecraft.diagnostics.container.store.deposit.context;

import adris.altoclef.tasksystem.Task;

//20260819_kpopmodder: Add deposit diagnostics correlation without changing Store task behavior.
public final class StoreDepositOperationContext {
    private final String operationId;
    private final String requestSource;
    private final Task rootTask;
    private final String rootTaskIdentity;
    private final String rootTaskClass;
    private final long startTick;
    private final long startEventSequence;

    public StoreDepositOperationContext(String operationId,
                                        String requestSource,
                                        Task rootTask,
                                        long startTick,
                                        long startEventSequence) {
        this.operationId = operationId;
        this.requestSource = requestSource;
        this.rootTask = rootTask;
        this.rootTaskIdentity = identity(rootTask);
        this.rootTaskClass = rootTask == null ? "none" : rootTask.getClass().getName();
        this.startTick = startTick;
        this.startEventSequence = startEventSequence;
    }

    public String operationId() {
        return operationId;
    }

    public String requestSource() {
        return requestSource;
    }

    public boolean isDepositAllOperation() {
        return "BARE_DEPOSIT_ALL_COMMAND".equals(requestSource)
                || "AUTO_DEPOSIT_ALL_CHAIN".equals(requestSource);
    }

    public Task rootTask() {
        return rootTask;
    }

    public String rootTaskIdentity() {
        return rootTaskIdentity;
    }

    public String rootTaskClass() {
        return rootTaskClass;
    }

    public long startTick() {
        return startTick;
    }

    public long startEventSequence() {
        return startEventSequence;
    }

    public boolean isRoot(Task task) {
        return rootTask == task;
    }

    public static String identity(Object value) {
        return value == null ? "none" : Integer.toHexString(System.identityHashCode(value));
    }
}
