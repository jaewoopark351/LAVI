package lavi.minecraft.diagnostics.container.store.deposit.counter;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

//20260914_kpopmodder: Project exact counter-owner facts into the existing backend-local observation system.
public final class StoreCounterDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    public StoreCounterDiagnostics(StoreDepositBindingRegistry bindings) { this.bindings = bindings; }

    public StoreCounterTrace trackerStart(ContainerStoredTracker tracker, long trackerId, long generation,
                                         Object[] totals, Subscription<?> previousSubscription) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) return StoreCounterTrace.NOOP;
        try {
            StoreDepositOperationState state = bindings.stateFor(tracker);
            StoreDepositBindingRegistry.TrackerBinding binding = bindings.trackerBinding(tracker);
            String role = binding == null ? "UNBOUND_TRACKER" : binding.trackerRole();
            StoreCounterTrace trace = trace(state, "tracker-" + trackerId + "-generation-" + generation, role,
                    "TRACKER_START", new Object[]{"trackerId", trackerId, "observedSubscriptionGeneration", generation});
            trace.record("SUBSCRIPTION", "START_REQUESTED", StoreCounterFields.concat(new Object[]{
                    "previousSubscriptionId", previousSubscription == null ? "NONE" : previousSubscription.diagnosticId(),
                    "counterResetPerformed", false,
                    "counterInitialization", "CONSTRUCTOR_EMPTY_MAP_OR_RETAINED_INSTANCE"}, totals));
            return trace;
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("deposit", "COUNTER_TRACKER_START_UNAVAILABLE");
            return StoreCounterTrace.NOOP;
        }
    }

    public StoreCounterDispatchProbe beginDispatch(Object event, List<?> subscribers) {
        if (!(event instanceof SlotClickChangedEvent) || !ChatClefDiagnostics.isBoundaryEnabled())
            return StoreCounterDispatchProbe.NOOP;
        try {
            Task exactTask = ChatClefDiagnostics.currentTaskForDiagnostics();
            StoreDepositOperationState state = bindings.stateFor(exactTask);
            StoreCounterTrace trace = trace(state, "unbound-slot-transport", "EVENTBUS", "PUBLISH_ENTRY", new Object[0]);
            SlotClickChangedEvent slotEvent = (SlotClickChangedEvent) event;
            return StoreCounterDispatchProbe.begin(trace, subscribers, slotEvent.slot.isSlotInPlayerInventory(),
                    new Object[]{"publicationEventClass", slotEvent.getClass().getName(),
                            "eventSlot", ChatClefDiagnostics.slotSummary(slotEvent.slot),
                            "eventBefore", StoreCounterFields.stack(slotEvent.before),
                            "eventAfter", StoreCounterFields.stack(slotEvent.after),
                            "observationSource", "CLIENT_LOCAL_SLOT_DIFF_NOT_SERVER_ACK"});
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("deposit", "COUNTER_DISPATCH_CAPTURE_UNAVAILABLE");
            return StoreCounterDispatchProbe.NOOP;
        }
    }

    public void producer(String phase, ScreenHandler handler, int slotIndex, int button, SlotActionType action) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) return;
        try {
            Task exactTask = ChatClefDiagnostics.currentTaskForDiagnostics();
            StoreDepositOperationState state = bindings.stateFor(exactTask);
            trace(state, "unbound-slot-transport", "PRODUCER", phase, new Object[0]).record("PRODUCER", phase,
                    "handlerClass", handler == null ? "UNAVAILABLE" : handler.getClass().getSimpleName(),
                    "syncId", handler == null ? -1 : handler.syncId,
                    "slotIndex", slotIndex, "button", button, "slotActionType", action,
                    "observationSource", "CLIENT_LOCAL_SCREEN_HANDLER", "serverConfirmation", "NOT_OBSERVED");
        } catch (RuntimeException | LinkageError failure) {
            ObservationDiagnostics.captureFailed("deposit", "COUNTER_PRODUCER_CAPTURE_UNAVAILABLE");
        }
    }

    public static void callback(StoreCounterTrace trace, String boundary, Slot slot, ItemStack before, ItemStack after,
                                boolean classificationEvaluated, boolean playerSlot,
                                boolean predicateEvaluated, boolean predicateResult) {
        if (!trace.current()) return;
        try {
            String reason = !classificationEvaluated ? "ENTER" : playerSlot ? "RETURN_REJECT_PLAYER"
                    : predicateResult ? "RETURN_ACCEPT_CONTAINER" : "RETURN_REJECT_PREDICATE";
            trace.record("TRACKER_CALLBACK", reason, StoreCounterFields.concat(StoreCounterDispatchProbe.currentFields(),
                    "slot", ChatClefDiagnostics.slotSummary(slot), "before", StoreCounterFields.stack(before),
                    "after", StoreCounterFields.stack(after),
                    "playerInventorySlot", classificationEvaluated ? playerSlot : "NOT_EVALUATED",
                    "acceptPredicateEvaluated", predicateEvaluated,
                    "acceptPredicateResult", predicateEvaluated ? predicateResult : "NOT_EVALUATED",
                    "appliedBranch", !classificationEvaluated ? "NOT_EVALUATED" : playerSlot ? "REJECT_PLAYER_SLOT"
                            : predicateResult ? "ACCEPT_CONTAINER_SLOT" : "REJECT_PREDICATE"));
        } catch (RuntimeException | LinkageError failure) {
            trace.record("TRACKER_CALLBACK", "SNAPSHOT_UNAVAILABLE", StoreCounterDispatchProbe.currentFields());
        }
    }

    public static void updated(StoreCounterTrace trace, Item item, int before, int delta, int after) {
        if (!trace.current()) return;
        try {
            trace.record("COUNTER_UPDATE", delta > 0 ? "POSITIVE_DELTA" : delta < 0 ? "NEGATIVE_DELTA" : "ZERO_DELTA",
                    StoreCounterFields.concat(StoreCounterDispatchProbe.currentFields(),
                            "item", StoreCounterFields.item(item), "counterBefore", before,
                            "deltaApplied", delta, "counterAfter", after,
                            "counterUnit", "ITEMS", "counterOwner", "ContainerStoredTracker.trackChange"));
        } catch (RuntimeException | LinkageError failure) {
            trace.record("COUNTER_UPDATE", "SNAPSHOT_UNAVAILABLE", StoreCounterDispatchProbe.currentFields());
        }
    }

    public void completion(Task maintenance, Task child, ContainerStoredTracker tracker, ItemTarget[] targets,
                           int childIndex, boolean finished, boolean stopped,
                           boolean storedPredicateEvaluated, boolean storedSatisfied, String failure) {
        // No repeated snapshots while the native child is still running; no predicate is invoked here.
        if (!finished && !stopped) return;
        try {
            Object[] core = new Object[]{
                    "finished", finished, "stopped", stopped,
                    "storedTargetsSatisfiedEvaluated", storedPredicateEvaluated,
                    "storedTargetsSatisfied", storedPredicateEvaluated ? storedSatisfied : "NOT_EVALUATED",
                    "classifierStoredArgument", storedSatisfied, "failureReason", failure,
                    "generalTaskIndex", childIndex, "trackerId", tracker.diagnosticId(),
                    "subscriptionId", tracker.diagnosticSubscriptionId(),
                    "decisionOwner", "AutoDepositMaintenanceTask.GENERAL_DEPOSIT",
                    "decisionExpression", "stopped?CHILD_STOPPED:finished&&!storedTargetsSatisfied?GENERAL_CHILD_UNCONFIRMED:NONE",
                    "decisionBoundary", "BEFORE_CLEANUP_NOT_FINAL_RESULT",
                    "maintenanceTask", StoreDepositEventFields.identity(maintenance),
                    "childTask", StoreDepositEventFields.identity(child),
                    "diagnosticsMode", ChatClefDiagnostics.isBoundaryEnabled() ? "BOUNDARY_OR_VERBOSE" : "OFF",
                    "detailedTraceStatus", !ChatClefDiagnostics.isBoundaryEnabled() ? "DISABLED"
                            : tracker.diagnosticCounterTrace() == StoreCounterTrace.NOOP ? "NO_RETAINED_TRACE"
                            : tracker.diagnosticCounterTrace().current() ? "ACTIVE_OUTPUT_UNVERIFIED" : "INACTIVE_OR_CLOSED",
                    "capturedTrackerRole", tracker.diagnosticCounterTrace().role(),
                    "trackerTraceScopeAdmitted", tracker.diagnosticCounterTrace().scopeAdmitted(),
                    "detailedTraceComplete", false, "serverConfirmation", "NOT_OBSERVED"};
            Object[] snapshot;
            try { snapshot = StoreCounterFields.targets(tracker, targets); }
            catch (RuntimeException | LinkageError unavailable) {
                snapshot = new Object[]{"counterSnapshot", "UNAVAILABLE", "counterSnapshotReason", unavailable.getClass().getSimpleName()};
            }
            Object[] values = StoreCounterFields.concat(core, snapshot);
            // Binding, reservations and optional capture cannot prevent creation of the core normal failure record.
            if (!"NONE".equals(failure)) ChatClefDiagnostics.logLifecycleBoundary(
                    "AUTO_DEPOSIT_GENERAL_CHILD_FAILURE", failure, maintenance, values);
            if (!ChatClefDiagnostics.isBoundaryEnabled()) return;
            // Reuse the exact subscription-start handle, never a later global or rebound operation.
            tracker.diagnosticCounterTrace().record("COMPLETION",
                    !"NONE".equals(failure) ? failure : "SATISFIED", values);
        } catch (RuntimeException | LinkageError failureDuringObservation) {
            ObservationDiagnostics.captureFailed("deposit", "COUNTER_COMPLETION_CAPTURE_UNAVAILABLE");
        }
    }

    private StoreCounterTrace trace(StoreDepositOperationState state, String unboundKey, String role,
                                    String origin, Object[] subject) {
        AltoClef mod = AltoClef.getInstance();
        ObservationActivation activation = ObservationDiagnostics.captureActivation(mod, mod == null ? null : mod.getWorld());
        String key = state == null ? "store-counter-" + unboundKey
                : "store-counter-" + state.context().operationId() + "-activation-" + state.activationCount();
        // Target-tracker first events must not consume the root/producer reservation.
        // The existing process-wide four-deposit-scope admission limit is not increased.
        if ("TARGET_CONTAINER".equals(role)) key += "-" + unboundKey;
        Object[] common = StoreCounterFields.concat(StoreDepositEventFields.operationFields(state),
                "counterTraceKey", key, "bindingStatus", state == null ? "UNBOUND" : "EXACT_NATIVE_OWNER_BINDING",
                "traceOriginBoundary", origin, "sourceToJarCorrespondence", "NOT_VERIFIED_BY_THIS_OBSERVER",
                "observationComplete", false, "serverConfirmationEvidence", "NOT_OBSERVED");
        ObservationScope scope = ObservationDiagnostics.open(activation, "deposit", key, common);
        if (!scope.isCurrent()) return StoreCounterTrace.NOOP;
        return new StoreCounterTrace(scope, role,
                StoreCounterFields.concat(new Object[]{"trackerRole", role}, subject));
    }
}
