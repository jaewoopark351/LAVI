package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

import java.util.IdentityHashMap;

//20260830_kpopmodder: Promote staged transfer candidates only after the existing Task reconciliation installs them.
public final class StoreDepositTransferAttemptRegistry {
    private static final int MAX_STAGED_TRANSFERS = 256;
    private static final int MAX_ACTIVE_TRANSFERS = 256;

    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals;
    private final IdentityHashMap<Task, PendingTransfer> pending = new IdentityHashMap<>();
    private final IdentityHashMap<Task, ActiveTransfer> active = new IdentityHashMap<>();

    public StoreDepositTransferAttemptRegistry(
            StoreDepositBindingRegistry bindings,
            StoreDepositAutomaticTerminalDiagnostics automaticTerminals) {
        this.bindings = bindings;
        this.automaticTerminals = automaticTerminals;
    }

    public synchronized void stage(Task parent,
                                   Task candidate,
                                   StoreDepositTransferSelectionSnapshot selection) {
        StoreDepositOperationState state = bindings.stateFor(parent);
        if (!isAutomatic(state) || candidate == null || selection == null) {
            return;
        }
        evictPendingIfNeeded();
        pending.put(candidate, new PendingTransfer(parent, state, selection));
    }

    public synchronized Reconciliation reconcile(Task parent,
                                                 Task activeChildBefore,
                                                 Task candidateChild,
                                                 boolean subTasksEqual,
                                                 boolean replacementApplied,
                                                 Task activeChildAfter) {
        StoreDepositOperationState state = bindings.stateFor(parent);
        if (!isAutomatic(state)) {
            return Reconciliation.unavailable();
        }
        PendingTransfer staged = candidateChild == null ? null : pending.remove(candidateChild);
        ActiveTransfer previous = active.get(activeChildBefore);
        boolean previousClosed = previous != null
                && replacementApplied
                && activeChildBefore != activeChildAfter;
        if (previousClosed) {
            active.remove(activeChildBefore);
            automaticTerminals.recordScopeIdentity(
                    previous.state().automaticContext(),
                    "TRANSFER",
                    previous.transferAttemptId(),
                    activeChildBefore,
                    StoreDepositOperationContext.identity(parent),
                    StoreDepositOperationContext.identity(activeChildBefore),
                    "GENERIC_TASK_RECONCILIATION",
                    terminalCorrelation(previous)
            );
        }
        if (staged == null) {
            return previousClosed
                    ? new Reconciliation(true, "PREVIOUS_TRANSFER_CLOSED", null)
                    : Reconciliation.unavailable();
        }
        if (replacementApplied && activeChildAfter == candidateChild) {
            evictActiveIfNeeded();
            long sequence = state.nextTransferAttemptSequence();
            String transferAttemptId = state.context().operationId() + "-transfer-" + sequence;
            ActiveTransfer promoted = new ActiveTransfer(
                    state,
                    parent,
                    candidateChild,
                    transferAttemptId,
                    sequence,
                    staged.selection(),
                    candidateId(
                            state.context().operationId(),
                            state.routeState().activeRouteCandidateGeneration()
                    ),
                    attemptId(
                            state.context().operationId(),
                            state.routeState().activeStoreAttemptSequence()
                    ),
                    routeChildId(
                            state.context().operationId(),
                            state.routeState().activeStoreAttemptRouteChildLifecycleSequence()
                    )
            );
            active.put(candidateChild, promoted);
            automaticTerminals.expectScopeIdentity(
                    state.automaticContext(),
                    "TRANSFER",
                    transferAttemptId
            );
            return new Reconciliation(true, "CANDIDATE_INSTALLED", promoted);
        }
        return new Reconciliation(
                true,
                subTasksEqual
                        ? "EQUAL_CANDIDATE_DISCARDED_ACTIVE_RETAINED"
                        : "CANDIDATE_NOT_INSTALLED",
                null
        );
    }

    public synchronized ActiveTransfer activeFor(Task exactTask) {
        if (exactTask == null) {
            return null;
        }
        ActiveTransfer result = active.get(exactTask);
        if (result != null && bindings.stateFor(exactTask) != result.state()) {
            active.remove(exactTask);
            return null;
        }
        return result;
    }

    public synchronized ActiveTransfer closeForTaskStop(Task task, String terminalReason) {
        ActiveTransfer transfer = task == null ? null : active.remove(task);
        if (transfer != null) {
            automaticTerminals.recordScopeIdentity(
                    transfer.state().automaticContext(),
                    "TRANSFER",
                    transfer.transferAttemptId(),
                    task,
                    StoreDepositOperationContext.identity(transfer.parent()),
                    StoreDepositOperationContext.identity(task),
                    terminalReason,
                    terminalCorrelation(transfer)
            );
        }
        return transfer;
    }

    public synchronized int clearForModeTransition() {
        int invalidated = pending.size() + active.size();
        pending.clear();
        active.clear();
        return invalidated;
    }

    private void evictPendingIfNeeded() {
        if (pending.size() < MAX_STAGED_TRANSFERS) {
            return;
        }
        Task oldest = pending.keySet().iterator().next();
        PendingTransfer removed = pending.remove(oldest);
        if (removed != null) {
            automaticTerminals.recordCoverageGap(
                    removed.state().automaticContext(),
                    "STAGED_TRANSFER_ATTEMPT_REGISTRY",
                    StoreDepositOperationContext.identity(oldest),
                    oldest,
                    "DIAGNOSTIC_REGISTRY_EVICTED",
                    StoreDepositEventFields.merge(
                            StoreDepositEventFields.operationFields(removed.state()),
                            removed.selection().fields()
                    )
            );
        }
    }

    private void evictActiveIfNeeded() {
        if (active.size() < MAX_ACTIVE_TRANSFERS) {
            return;
        }
        Task oldest = active.keySet().iterator().next();
        ActiveTransfer removed = active.remove(oldest);
        if (removed != null) {
            automaticTerminals.recordCoverageGap(
                    removed.state().automaticContext(),
                    "TRANSFER_ATTEMPT_REGISTRY",
                    removed.transferAttemptId(),
                    oldest,
                    "DIAGNOSTIC_REGISTRY_EVICTED",
                    terminalCorrelation(removed)
            );
        }
    }

    private static boolean isAutomatic(StoreDepositOperationState state) {
        return state != null
                && state.context() != null
                && state.context().isAutomaticDepositOperation()
                && state.automaticContext().available();
    }

    private static Object[] terminalCorrelation(ActiveTransfer transfer) {
        return transfer == null
                ? new Object[0]
                : StoreDepositEventFields.merge(
                        StoreDepositEventFields.operationFields(transfer.state()),
                        transfer.identityFields()
                );
    }

    private static String candidateId(String operationId, long generation) {
        return generation <= 0 ? "UNAVAILABLE" : operationId + "-candidate-" + generation;
    }

    private static String attemptId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-attempt-" + sequence;
    }

    private static String routeChildId(String operationId, long sequence) {
        return sequence <= 0 ? "UNAVAILABLE" : operationId + "-route-child-" + sequence;
    }

    private record PendingTransfer(Task parent,
                                   StoreDepositOperationState state,
                                   StoreDepositTransferSelectionSnapshot selection) {
    }

    public static final class ActiveTransfer {
        private final StoreDepositOperationState state;
        private final Task parent;
        private final Task transferTask;
        private final String transferAttemptId;
        private final long transferAttemptSequence;
        private final StoreDepositTransferSelectionSnapshot selection;
        private final String selectedCandidateGenerationId;
        private final String storeAttemptId;
        private final String routeChildLifecycleId;
        private long slotActionSequence;
        private String physicalSourceSlot = "UNAVAILABLE";
        private String physicalSourceItemId = "UNAVAILABLE";
        private int physicalSourceCount = -1;
        private String physicalSourceKind = "UNAVAILABLE";
        private String selectedSourceCandidateMatch = "UNAVAILABLE_PHYSICAL_SOURCE";
        private String terminalCursor = "UNAVAILABLE";

        private ActiveTransfer(StoreDepositOperationState state,
                               Task parent,
                               Task transferTask,
                               String transferAttemptId,
                               long transferAttemptSequence,
                               StoreDepositTransferSelectionSnapshot selection,
                               String selectedCandidateGenerationId,
                               String storeAttemptId,
                               String routeChildLifecycleId) {
            this.state = state;
            this.parent = parent;
            this.transferTask = transferTask;
            this.transferAttemptId = transferAttemptId;
            this.transferAttemptSequence = transferAttemptSequence;
            this.selection = selection;
            this.selectedCandidateGenerationId = selectedCandidateGenerationId;
            this.storeAttemptId = storeAttemptId;
            this.routeChildLifecycleId = routeChildLifecycleId;
        }

        public synchronized long nextSlotActionSequence() {
            return ++slotActionSequence;
        }

        public synchronized void confirmCursorSource(ItemStack cursorBefore) {
            if (!"UNAVAILABLE".equals(physicalSourceKind)
                    || !selection.cursorMatchesAggregate(cursorBefore)) {
                return;
            }
            physicalSourceSlot = "CURSOR";
            physicalSourceItemId = itemId(cursorBefore);
            physicalSourceCount = cursorBefore.getCount();
            physicalSourceKind = "CURSOR";
            selectedSourceCandidateMatch = selection.selectedSourceMatchVerdict(null, true);
        }

        public synchronized void confirmPlayerSlotSource(Slot slot, ItemStack before) {
            if (!"UNAVAILABLE".equals(physicalSourceKind)
                    || !selection.stackMatchesAggregate(before)) {
                return;
            }
            physicalSourceSlot = selection.observedSlotSummary(slot);
            physicalSourceItemId = itemId(before);
            physicalSourceCount = before.getCount();
            physicalSourceKind = "PLAYER_SLOT_MUTATION";
            selectedSourceCandidateMatch = selection.selectedSourceMatchVerdict(
                    slot,
                    false
            );
        }

        public synchronized void recordTerminalCursor(ItemStack cursorAfter) {
            terminalCursor = stack(cursorAfter);
        }

        public StoreDepositOperationState state() {
            return state;
        }

        public Task parent() {
            return parent;
        }

        public Task transferTask() {
            return transferTask;
        }

        public String transferAttemptId() {
            return transferAttemptId;
        }

        public long transferAttemptSequence() {
            return transferAttemptSequence;
        }

        public StoreDepositTransferSelectionSnapshot selection() {
            return selection;
        }

        public Object[] identityFields() {
            return new Object[]{
                    "selectedCandidateGenerationId", selectedCandidateGenerationId,
                    "storeAttemptId", storeAttemptId,
                    "routeChildLifecycleId", routeChildLifecycleId,
                    "transferAttemptId", transferAttemptId
            };
        }

        public synchronized Object[] sourceFields() {
            return new Object[]{
                    "physicalSourceSlot", physicalSourceSlot,
                    "physicalSourceItemId", physicalSourceItemId,
                    "physicalSourceCount", physicalSourceCount < 0 ? "UNAVAILABLE" : physicalSourceCount,
                    "physicalSourceKind", physicalSourceKind,
                    "selectedSourceCandidateMatch", selectedSourceCandidateMatch
            };
        }

        public synchronized Object[] terminalFields(String terminalReason) {
            return new Object[]{
                    "terminalProjectionScope", "TRANSFER_STATE_AT_TASK_STOP",
                    "terminalProjectionReason", terminalReason,
                    "terminalCursor", terminalCursor,
                    "durableEffect", "UNAVAILABLE",
                    "stableOrServerSnapshotAvailable", false
            };
        }

        private static String itemId(ItemStack stack) {
            return stack == null || stack.isEmpty() ? "EMPTY" : String.valueOf(stack.getItem());
        }

        private static String stack(ItemStack stack) {
            return stack == null || stack.isEmpty()
                    ? "EMPTY"
                    : String.valueOf(stack.getItem()) + "x" + stack.getCount();
        }
    }

    public record Reconciliation(boolean available,
                                 String outcome,
                                 ActiveTransfer promoted) {
        public static Reconciliation unavailable() {
            return new Reconciliation(false, "UNAVAILABLE", null);
        }
    }
}
