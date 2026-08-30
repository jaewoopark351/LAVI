package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry.ActiveTransfer;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.LinkedHashSet;
import java.util.Set;

//20260830_kpopmodder: Correlate one local click action with its ordered slot mutations and both tracker roles.
public final class StoreDepositSlotActionDiagnostics {
    private final StoreDepositTransferAttemptRegistry attempts;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals;
    private final ThreadLocal<ActionContext> currentAction = new ThreadLocal<>();
    private final ThreadLocal<MutationContext> currentMutation = new ThreadLocal<>();

    public StoreDepositSlotActionDiagnostics(
            StoreDepositTransferAttemptRegistry attempts,
            StoreDepositEmissionGate emissionGate,
            StoreDepositAutomaticTerminalDiagnostics automaticTerminals) {
        this.attempts = attempts;
        this.emissionGate = emissionGate;
        this.automaticTerminals = automaticTerminals;
    }

    public void beginAction(Task exactLeafTask,
                            Object handler,
                            int syncId,
                            int clickedSlotIndex,
                            int clickButton,
                            SlotActionType clickActionType,
                            ItemStack cursorBefore) {
        currentMutation.remove();
        currentAction.remove();
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        ActiveTransfer transfer = attempts.activeFor(exactLeafTask);
        if (transfer == null) {
            return;
        }
        long sequence = transfer.nextSlotActionSequence();
        boolean cursorBeforeObserved = cursorBefore != null;
        ItemStack before = copy(cursorBefore);
        if (cursorBeforeObserved) {
            transfer.confirmCursorSource(before);
        }
        ActionContext action = new ActionContext(
                transfer,
                transfer.transferAttemptId() + "-action-" + sequence,
                sequence,
                StoreDepositOperationContext.identity(handler),
                syncId,
                clickedSlotIndex,
                clickButton,
                clickActionType == null ? "UNAVAILABLE" : clickActionType.name(),
                before,
                cursorBeforeObserved
        );
        currentAction.set(action);
        automaticTerminals.expectScopeIdentity(
                transfer.state().automaticContext(),
                "SLOT_ACTION",
                action.slotActionId
        );
    }

    public void observeLocalClickReturn(ItemStack cursorAfter) {
        ActionContext action = currentAction.get();
        if (action != null) {
            action.localClickReturned = true;
            if (cursorAfter != null) {
                action.cursorAfter = copy(cursorAfter);
                action.cursorAfterObserved = true;
                action.transfer.recordTerminalCursor(action.cursorAfter);
            }
        }
    }

    public void beginMutation(int ordinal,
                              Slot slot,
                              ItemStack before,
                              ItemStack after) {
        currentMutation.remove();
        ActionContext action = currentAction.get();
        if (action == null) {
            return;
        }
        MutationContext mutation = new MutationContext(
                action,
                action.slotActionId + "-mutation-" + ordinal,
                ordinal,
                slot,
                copy(before),
                copy(after)
        );
        action.mutationCount++;
        currentMutation.set(mutation);
        automaticTerminals.expectScopeIdentity(
                action.transfer.state().automaticContext(),
                "SLOT_MUTATION",
                mutation.slotMutationId
        );
    }

    public void observeTrackerMutation(TrackerBinding binding,
                                       Slot slot,
                                       ItemStack before,
                                       boolean playerInventorySlot) {
        MutationContext mutation = currentMutation.get();
        if (mutation == null) {
            return;
        }
        if (binding != null) {
            mutation.trackerRoles.add(binding.trackerRole());
        }
        if (playerInventorySlot) {
            mutation.action.transfer.confirmPlayerSlotSource(slot, before);
        }
    }

    public Object[] currentMutationFields() {
        MutationContext mutation = currentMutation.get();
        return mutation == null ? unavailableFields() : mutation.correlationFields();
    }

    public String currentSlotActionId() {
        ActionContext action = currentAction.get();
        return action == null ? "UNAVAILABLE" : action.slotActionId;
    }

    public String currentSlotMutationId() {
        MutationContext mutation = currentMutation.get();
        return mutation == null ? "UNAVAILABLE" : mutation.slotMutationId;
    }

    public void endMutation() {
        MutationContext mutation = currentMutation.get();
        currentMutation.remove();
        if (mutation == null) {
            return;
        }
        ActiveTransfer transfer = mutation.action.transfer;
        String operationId = transfer.state().context().operationId();
        if (emissionGate.shouldEmitDetail(
                operationId,
                "STORE_DEPOSIT_SLOT_MUTATION",
                mutation.slotMutationId
        )) {
            StoreDepositBoundedEventLogger.log(
                    "STORE_DEPOSIT_SLOT_MUTATION",
                    "store_deposit_slot_mutation",
                    transfer.transferTask(),
                    ChatClefDiagnostics.withCommandContextFields(mutation.fields())
            );
        }
        automaticTerminals.recordScopeIdentity(
                transfer.state().automaticContext(),
                "SLOT_MUTATION",
                mutation.slotMutationId,
                transfer.transferTask(),
                StoreDepositOperationContext.identity(transfer.parent()),
                StoreDepositOperationContext.identity(transfer.transferTask()),
                "LOCAL_INTERNAL_CLICK_MUTATION",
                StoreDepositEventFields.merge(
                        StoreDepositEventFields.operationFields(transfer.state()),
                        mutation.correlationFields()
                )
        );
    }

    public void endAction() {
        MutationContext dangling = currentMutation.get();
        if (dangling != null) {
            endMutation();
        }
        ActionContext action = currentAction.get();
        currentAction.remove();
        if (action == null) {
            return;
        }
        ActiveTransfer transfer = action.transfer;
        if (action.cursorAfterObserved) {
            transfer.recordTerminalCursor(action.cursorAfter);
        }
        String operationId = transfer.state().context().operationId();
        Object[] fields = StoreDepositEventFields.merge(
                StoreDepositEventFields.operationFields(transfer.state()),
                StoreDepositEventFields.merge(
                        transfer.identityFields(),
                        StoreDepositEventFields.merge(
                                transfer.selection().fields(),
                                StoreDepositEventFields.merge(
                                        transfer.sourceFields(),
                                        action.actionEventFields()
                                )
                        )
                )
        );
        if (emissionGate.shouldEmitDetail(
                operationId,
                "STORE_DEPOSIT_SLOT_ACTION",
                action.slotActionId
        )) {
            StoreDepositBoundedEventLogger.log(
                    "STORE_DEPOSIT_SLOT_ACTION",
                    "store_deposit_slot_action",
                    transfer.transferTask(),
                    ChatClefDiagnostics.withCommandContextFields(fields)
            );
        }
        automaticTerminals.recordScopeIdentity(
                transfer.state().automaticContext(),
                "SLOT_ACTION",
                action.slotActionId,
                transfer.transferTask(),
                StoreDepositOperationContext.identity(transfer.parent()),
                StoreDepositOperationContext.identity(transfer.transferTask()),
                action.localClickReturned
                        ? "LOCAL_CLICK_RETURN"
                        : "LOCAL_CLICK_DID_NOT_RETURN",
                StoreDepositEventFields.merge(
                        StoreDepositEventFields.operationFields(transfer.state()),
                        StoreDepositEventFields.merge(
                                transfer.identityFields(),
                                new Object[]{
                                        "slotActionId", action.slotActionId,
                                        "slotMutationId", "UNAVAILABLE_ACTION_SCOPE"
                                }
                        )
                )
        );
    }

    private static Object[] unavailableFields() {
        return new Object[]{
                "slotActionId", "UNAVAILABLE",
                "slotMutationId", "UNAVAILABLE",
                "mutationObservationSource", "UNAVAILABLE",
                "stableOrServerSnapshotAvailable", false,
                "durableEffect", "UNAVAILABLE"
        };
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static String stack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "EMPTY";
        }
        return String.valueOf(stack.getItem()) + "x" + stack.getCount();
    }

    private static final class ActionContext {
        private final ActiveTransfer transfer;
        private final String slotActionId;
        private final long clickSequence;
        private final String handlerIdentity;
        private final int syncId;
        private final int clickedSlotIndex;
        private final int clickButton;
        private final String clickActionType;
        private final ItemStack cursorBefore;
        private final boolean cursorBeforeObserved;
        private ItemStack cursorAfter;
        private boolean cursorAfterObserved;
        private int mutationCount;
        private boolean localClickReturned;

        private ActionContext(ActiveTransfer transfer,
                              String slotActionId,
                              long clickSequence,
                              String handlerIdentity,
                              int syncId,
                              int clickedSlotIndex,
                              int clickButton,
                              String clickActionType,
                              ItemStack cursorBefore,
                              boolean cursorBeforeObserved) {
            this.transfer = transfer;
            this.slotActionId = slotActionId;
            this.clickSequence = clickSequence;
            this.handlerIdentity = handlerIdentity;
            this.syncId = syncId;
            this.clickedSlotIndex = clickedSlotIndex;
            this.clickButton = clickButton;
            this.clickActionType = clickActionType;
            this.cursorBefore = cursorBefore;
            this.cursorBeforeObserved = cursorBeforeObserved;
            this.cursorAfter = copy(cursorBefore);
        }

        private Object[] coreFields() {
            return new Object[]{
                    "slotActionId", slotActionId,
                    "handlerIdentity", handlerIdentity,
                    "syncId", syncId,
                    "handlerRevision", "UNAVAILABLE",
                    "clickedSlotIndex", clickedSlotIndex,
                    "clickActionType", clickActionType,
                    "clickButton", clickButton,
                    "clickSequence", clickSequence,
                    "cursorBefore", cursorBeforeObserved ? stack(cursorBefore) : "UNAVAILABLE",
                    "cursorAfter", cursorAfterObserved ? stack(cursorAfter) : "UNAVAILABLE",
                    "cursorCountBefore", cursorBeforeObserved ? cursorBefore.getCount() : "UNAVAILABLE",
                    "localClickReturned", localClickReturned,
                    "mutationCount", mutationCount
            };
        }

        private Object[] actionEventFields() {
            return StoreDepositEventFields.merge(coreFields(), new Object[]{
                    "slotMutationId", "UNAVAILABLE_ACTION_SCOPE",
                    "mutationObservationSource", localClickReturned
                            ? "LOCAL_INTERNAL_CLICK"
                            : "UNAVAILABLE",
                    "stableOrServerSnapshotAvailable", false,
                    "durableEffect", "UNAVAILABLE",
                    "observationComplete", false,
                    "missingBoundaries", missingBoundaries(),
                    "behavior_effect", "none"
            });
        }

        private String missingBoundaries() {
            String missing = "SERVER_SLOT_UPDATE,POST_ACTION_STABLE,HANDLER_REVISION";
            if (!cursorBeforeObserved) {
                missing = "CURSOR_BEFORE," + missing;
            }
            if (!localClickReturned) {
                return "LOCAL_CLICK_RETURN,CURSOR_AFTER," + missing;
            }
            return cursorAfterObserved ? missing : "CURSOR_AFTER," + missing;
        }
    }

    private static final class MutationContext {
        private final ActionContext action;
        private final String slotMutationId;
        private final int ordinal;
        private final Slot slot;
        private final ItemStack before;
        private final ItemStack after;
        private final Set<String> trackerRoles = new LinkedHashSet<>();

        private MutationContext(ActionContext action,
                                String slotMutationId,
                                int ordinal,
                                Slot slot,
                                ItemStack before,
                                ItemStack after) {
            this.action = action;
            this.slotMutationId = slotMutationId;
            this.ordinal = ordinal;
            this.slot = slot;
            this.before = before;
            this.after = after;
        }

        private Object[] fields() {
            ActiveTransfer transfer = action.transfer;
            boolean destination = transfer.selection().isDestinationSlot(slot);
            return StoreDepositEventFields.merge(
                    StoreDepositEventFields.operationFields(transfer.state()),
                    StoreDepositEventFields.merge(
                            transfer.identityFields(),
                            StoreDepositEventFields.merge(
                            transfer.selection().fields(),
                            StoreDepositEventFields.merge(
                                    transfer.sourceFields(),
                                    StoreDepositEventFields.merge(
                                            action.coreFields(),
                                            mutationFields(destination)
                                    )
                            )
                            )
                    )
            );
        }

        private Object[] correlationFields() {
            ActiveTransfer transfer = action.transfer;
            return StoreDepositEventFields.merge(
                    transfer.identityFields(),
                    StoreDepositEventFields.merge(
                            action.coreFields(),
                            new Object[]{
                                    "slotMutationId", slotMutationId,
                                    "slotMutationOrdinal", ordinal,
                                    "trackerRolesObserved", Set.copyOf(trackerRoles),
                                    "mutationObservationSource", "LOCAL_INTERNAL_CLICK",
                                    "stableOrServerSnapshotAvailable", false,
                                    "durableEffect", "UNAVAILABLE",
                                    "observationComplete", false,
                                    "missingBoundaries", "SERVER_SLOT_UPDATE,POST_ACTION_STABLE,HANDLER_REVISION"
                            }
                    )
            );
        }

        private Object[] mutationFields(boolean destination) {
            return new Object[]{
                    "slotMutationId", slotMutationId,
                    "slotMutationOrdinal", ordinal,
                    "mutatedSlot", ChatClefDiagnostics.slotSummary(slot),
                    "mutatedSlotBefore", stack(before),
                    "mutatedSlotAfter", stack(after),
                    "sourceBefore", destination ? "UNAVAILABLE" : stack(before),
                    "sourceAfter", destination ? "UNAVAILABLE" : stack(after),
                    "destinationBefore", destination ? stack(before) : "UNAVAILABLE",
                    "destinationAfter", destination ? stack(after) : "UNAVAILABLE",
                    "trackerRolesObserved", Set.copyOf(trackerRoles),
                    "mutationObservationSource", "LOCAL_INTERNAL_CLICK",
                    "stableOrServerSnapshotAvailable", false,
                    "durableEffect", "UNAVAILABLE",
                    "observationComplete", false,
                    "missingBoundaries", "SERVER_SLOT_UPDATE,POST_ACTION_STABLE,HANDLER_REVISION",
                    "behavior_effect", "none"
            };
        }
    }
}
