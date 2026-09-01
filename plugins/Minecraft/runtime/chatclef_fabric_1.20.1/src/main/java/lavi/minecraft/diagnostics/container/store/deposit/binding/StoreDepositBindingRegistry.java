package lavi.minecraft.diagnostics.container.store.deposit.binding;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import net.minecraft.util.math.BlockPos;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StoreDepositBindingRegistry {
    private static final int MAX_ACTIVE_OPERATIONS = 16;
    private static final int MAX_TRACKER_BINDINGS = 256;

    private final IdentityHashMap<Task, String> taskToOperation = new IdentityHashMap<>();
    private final IdentityHashMap<ContainerStoredTracker, TrackerBinding> trackerBindings = new IdentityHashMap<>();
    private final LinkedHashMap<String, StoreDepositOperationState> operations = new LinkedHashMap<>();
    private String lastActiveOperationId = "none";

    public synchronized StoreDepositOperationState registerRoot(Task rootTask, String requestSource) {
        return registerRoot(rootTask, requestSource, StoreDepositAutomaticContext.unavailable());
    }

    public synchronized StoreDepositOperationState registerRoot(Task rootTask,
                                                                String requestSource,
                                                                StoreDepositAutomaticContext automaticContext) {
        StoreDepositOperationState existing = stateFor(rootTask);
        if (existing != null) {
            existing.attachAutomaticContext(automaticContext);
            return existing;
        }
        evictOldestOperationIfNeeded();
        String operationId = "store-deposit-" + ChatClefDiagnostics.nextOperationId();
        StoreDepositOperationContext context = new StoreDepositOperationContext(
                operationId,
                requestSource,
                rootTask,
                ChatClefDiagnostics.currentClientTickId(),
                -1
        );
        StoreDepositOperationState state = new StoreDepositOperationState(context);
        state.attachAutomaticContext(automaticContext);
        operations.put(operationId, state);
        if (rootTask != null) {
            taskToOperation.put(rootTask, operationId);
        }
        lastActiveOperationId = operationId;
        return state;
    }

    public synchronized StoreDepositOperationState activateRoot(Task rootTask, String requestSource) {
        StoreDepositOperationState state = registerRoot(rootTask, requestSource);
        boolean resume = state.activationCount() > 0;
        state.recordActivation(resume);
        lastActiveOperationId = state.context().operationId();
        return state;
    }

    public synchronized StoreDepositOperationState stateFor(Task task) {
        if (task == null) {
            return null;
        }
        String operationId = taskToOperation.get(task);
        return operationId == null ? null : operations.get(operationId);
    }

    public synchronized StoreDepositOperationState stateFor(ContainerStoredTracker tracker) {
        TrackerBinding binding = trackerBindings.get(tracker);
        return binding == null ? null : operations.get(binding.operationId());
    }

    public synchronized StoreDepositOperationState stateForOperation(String operationId) {
        return operationId == null ? null : operations.get(operationId);
    }

    public synchronized StoreDepositOperationState lastActiveState() {
        return operations.get(lastActiveOperationId);
    }

    public synchronized String roleFor(Task task) {
        StoreDepositOperationState state = stateFor(task);
        if (state == null) {
            return "UNBOUND";
        }
        return state.context().isRoot(task) ? "ROOT_STORE" : "DESCENDANT";
    }

    public synchronized TrackerBinding trackerBinding(ContainerStoredTracker tracker) {
        return trackerBindings.get(tracker);
    }

    public synchronized void bindChild(Task parent, Task child) {
        if (parent == null || child == null) {
            return;
        }
        StoreDepositOperationState state = stateFor(parent);
        if (state == null) {
            return;
        }
        String existingOperation = taskToOperation.get(child);
        if (state.context().operationId().equals(existingOperation)) {
            return;
        }
        if (!state.tryRecordTaskBinding()) {
            return;
        }
        taskToOperation.put(child, state.context().operationId());
    }

    public synchronized void bindTracker(Task owner,
                                         ContainerStoredTracker tracker,
                                         String trackerRole,
                                         BlockPos targetContainer) {
        if (tracker == null) {
            return;
        }
        StoreDepositOperationState state = stateFor(owner);
        if (state == null) {
            return;
        }
        TrackerBinding existing = trackerBindings.get(tracker);
        if (existing != null && state.context().operationId().equals(existing.operationId())) {
            trackerBindings.put(tracker, new TrackerBinding(
                    state.context().operationId(),
                    trackerRole,
                    targetContainer,
                    existing.subscriptionGeneration(),
                    existing.subscriptionActive()
            ));
            return;
        }
        if (!state.tryRecordTrackerBinding()) {
            return;
        }
        evictTrackerBindingIfNeeded();
        trackerBindings.put(tracker, new TrackerBinding(
                state.context().operationId(),
                trackerRole,
                targetContainer,
                0L,
                false
        ));
    }

    public synchronized TrackerBinding markTrackerSubscriptionStarted(ContainerStoredTracker tracker) {
        TrackerBinding binding = trackerBindings.get(tracker);
        if (binding == null) {
            return null;
        }
        TrackerBinding updated = new TrackerBinding(
                binding.operationId(),
                binding.trackerRole(),
                binding.targetContainer(),
                binding.subscriptionGeneration() + 1L,
                true
        );
        trackerBindings.put(tracker, updated);
        return updated;
    }

    public synchronized TrackerBinding markTrackerSubscriptionStopped(ContainerStoredTracker tracker) {
        TrackerBinding binding = trackerBindings.get(tracker);
        if (binding == null) {
            return null;
        }
        TrackerBinding updated = new TrackerBinding(
                binding.operationId(),
                binding.trackerRole(),
                binding.targetContainer(),
                binding.subscriptionGeneration(),
                false
        );
        trackerBindings.put(tracker, updated);
        return updated;
    }

    public synchronized StoreDepositOperationState markFinal(Task task) {
        StoreDepositOperationState state = stateFor(task);
        if (state == null) {
            return null;
        }
        return state;
    }

    public synchronized void purgeOperation(Task rootTask) {
        StoreDepositOperationState state = stateFor(rootTask);
        if (state == null) {
            return;
        }
        String operationId = state.context().operationId();
        taskToOperation.entrySet().removeIf(entry -> operationId.equals(entry.getValue()));
        trackerBindings.entrySet().removeIf(entry -> operationId.equals(entry.getValue().operationId()));
        operations.remove(operationId);
        if (operationId.equals(lastActiveOperationId)) {
            lastActiveOperationId = operations.isEmpty() ? "none" : operations.keySet().iterator().next();
        }
    }

    public synchronized ClearResult clearForModeTransition() {
        ClearResult result = new ClearResult(
                operations.size(),
                taskToOperation.size(),
                trackerBindings.size()
        );
        operations.clear();
        taskToOperation.clear();
        trackerBindings.clear();
        lastActiveOperationId = "none";
        return result;
    }

    public synchronized int activeOperationCount() {
        return operations.size();
    }

    private void evictOldestOperationIfNeeded() {
        if (operations.size() < MAX_ACTIVE_OPERATIONS) {
            return;
        }
        String oldestOperationId = operations.keySet().iterator().next();
        StoreDepositOperationState state = operations.get(oldestOperationId);
        if (state != null) {
            state.recordOperationEvicted();
        }
        taskToOperation.entrySet().removeIf(entry -> oldestOperationId.equals(entry.getValue()));
        trackerBindings.entrySet().removeIf(entry -> oldestOperationId.equals(entry.getValue().operationId()));
        operations.remove(oldestOperationId);
    }

    private void evictTrackerBindingIfNeeded() {
        if (trackerBindings.size() < MAX_TRACKER_BINDINGS) {
            return;
        }
        ContainerStoredTracker first = trackerBindings.keySet().iterator().next();
        TrackerBinding binding = trackerBindings.get(first);
        StoreDepositOperationState state = binding == null ? null : operations.get(binding.operationId());
        if (state != null) {
            state.recordOperationEvicted();
        }
        trackerBindings.remove(first);
    }

    public record TrackerBinding(String operationId,
                                 String trackerRole,
                                 BlockPos targetContainer,
                                 long subscriptionGeneration,
                                 boolean subscriptionActive) {
    }

    public record ClearResult(int operationCount,
                              int taskBindingCount,
                              int trackerBindingCount) {
        public int totalEntryCount() {
            return operationCount + taskBindingCount + trackerBindingCount;
        }
    }
}
