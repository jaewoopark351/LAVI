package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.ClearResult;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger.TerminalRecord;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;

//20260907_kpopmodder: Own automatic-run identity bindings, access ordering, eviction queue, and removal.
final class StoreDepositAutomaticRunRegistry {
    private static final int MAX_ACTIVE_RUNS = 16;
    private static final int MAX_PENDING_EVICTIONS = 16;

    private final LinkedHashMap<String, StoreDepositAutomaticRunState> runs =
            new LinkedHashMap<>(16, 0.75f, true);
    private final Deque<TerminalRecord> pendingEvictions = new ArrayDeque<>();
    private final IdentityHashMap<Task, StoreDepositAutomaticRunState> maintenanceBindings =
            new IdentityHashMap<>();
    private final IdentityHashMap<Task, ChildBinding> childBindings = new IdentityHashMap<>();
    private final IdentityHashMap<Task, Deque<StoreDepositAutomaticRunState>> userRootBindings =
            new IdentityHashMap<>();

    private long nextAutoOperationEpoch;
    private long pendingEvictionOverflowCount;
    private long activeRunEvictionEmissionSuppressedCount;

    StoreDepositAutomaticRunState createRun(Task maintenanceTask,
                                             Task userTaskRoot,
                                             long policyContextEpoch) {
        long epoch = ++nextAutoOperationEpoch;
        String autoOperationId = "auto-deposit-" + epoch;
        StoreDepositAutomaticRunState state = new StoreDepositAutomaticRunState(
                epoch,
                policyContextEpoch,
                autoOperationId,
                autoOperationId + "-maintenance-1",
                autoOperationId + "-pressure-run-1",
                maintenanceTask,
                userTaskRoot
        );
        runs.put(autoOperationId, state);
        if (maintenanceTask != null) {
            maintenanceBindings.put(maintenanceTask, state);
        }
        if (userTaskRoot != null) {
            userRootBindings.computeIfAbsent(userTaskRoot, ignored -> new ArrayDeque<>()).addLast(state);
        }
        return state;
    }

    boolean activeRunCapacityReached() {
        return runs.size() >= MAX_ACTIVE_RUNS;
    }

    StoreDepositAutomaticRunState oldestRun() {
        return runs.isEmpty() ? null : runs.values().iterator().next();
    }

    StoreDepositAutomaticRunState runForId(String autoOperationId) {
        return runs.get(autoOperationId);
    }

    StoreDepositAutomaticRunState stateForMaintenance(Task maintenanceTask) {
        StoreDepositAutomaticRunState state = maintenanceBindings.get(maintenanceTask);
        touch(state);
        return state;
    }

    ChildBinding childBinding(Task childTask) {
        ChildBinding binding = childBindings.get(childTask);
        if (binding != null) {
            touch(binding.state());
        }
        return binding;
    }

    ChildBinding existingChildBinding(Task childTask) {
        return childBindings.get(childTask);
    }

    void bindChild(Task childTask,
                   StoreDepositAutomaticRunState state,
                   StoreDepositAutomaticContext context) {
        childBindings.put(childTask, new ChildBinding(state, context));
    }

    StoreDepositAutomaticRunState firstUserRun(
            Task task,
            Predicate<StoreDepositAutomaticRunState> predicate) {
        for (StoreDepositAutomaticRunState state : userRuns(task)) {
            if (predicate.test(state)) {
                touch(state);
                return state;
            }
        }
        return null;
    }

    List<StoreDepositAutomaticRunState> userRuns(Task task) {
        Deque<StoreDepositAutomaticRunState> states = userRootBindings.get(task);
        return states == null ? List.of() : List.copyOf(states);
    }

    long makePendingEvictionRoom() {
        if (pendingEvictions.size() >= MAX_PENDING_EVICTIONS) {
            pendingEvictions.removeFirst();
            pendingEvictionOverflowCount++;
        }
        return pendingEvictionOverflowCount;
    }

    void addPendingEviction(TerminalRecord record) {
        pendingEvictions.addLast(record);
    }

    TerminalRecord takePendingEviction() {
        TerminalRecord record = pendingEvictions.pollFirst();
        return record == null ? TerminalRecord.unavailable() : record;
    }

    long incrementActiveRunEvictionEmissionSuppressedCount() {
        return ++activeRunEvictionEmissionSuppressedCount;
    }

    long activeRunEvictionEmissionSuppressedCount() {
        return activeRunEvictionEmissionSuppressedCount;
    }

    void remove(StoreDepositAutomaticRunState state) {
        runs.remove(state.autoOperationId());
        maintenanceBindings.entrySet().removeIf(entry -> entry.getValue() == state);
        childBindings.entrySet().removeIf(entry -> entry.getValue().state() == state);
        userRootBindings.values().forEach(states -> states.removeIf(candidate -> candidate == state));
        userRootBindings.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    void touch(StoreDepositAutomaticRunState state) {
        if (state != null) {
            runs.get(state.autoOperationId());
        }
    }

    ClearResult clearForModeTransition() {
        ClearResult result = new ClearResult(
                runs.size(),
                pendingEvictions.size(),
                maintenanceBindings.size(),
                childBindings.size(),
                userRootBindings.size()
        );
        runs.clear();
        pendingEvictions.clear();
        maintenanceBindings.clear();
        childBindings.clear();
        userRootBindings.clear();
        return result;
    }

    int activeRunCount() {
        return runs.size();
    }

    record ChildBinding(StoreDepositAutomaticRunState state,
                        StoreDepositAutomaticContext context) {
    }
}
