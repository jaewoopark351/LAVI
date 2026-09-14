package lavi.minecraft.diagnostics.container.store.deposit.counter;

import adris.altoclef.eventbus.Subscription;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.ArrayList;
import java.util.List;

//20260914_kpopmodder: Observe synchronous slot-event delivery without modifying native listeners or exception handling.
public final class StoreCounterDispatchProbe implements AutoCloseable {
    private static final ThreadLocal<StoreCounterDispatchProbe> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> OVERFLOW = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_DEPTH = 8;
    private static final int MAX_LISTENERS = 16;
    public static final StoreCounterDispatchProbe NOOP = new StoreCounterDispatchProbe();
    private static final StoreCounterDispatchProbe OVERFLOW_PROBE = new StoreCounterDispatchProbe();
    private final StoreCounterTrace trace;
    private final StoreCounterDispatchProbe previous;
    private final long eventId;
    private final int depth;
    private long currentSubscription = -1;
    private StoreCounterTrace currentListenerTrace = StoreCounterTrace.NOOP;
    private int entered, returned, skipped, classCastFailures;
    private boolean completed, closed, listenerPending;

    private StoreCounterDispatchProbe() {
        trace = StoreCounterTrace.NOOP; previous = null; eventId = -1; depth = 0;
    }

    private StoreCounterDispatchProbe(StoreCounterTrace trace, StoreCounterDispatchProbe previous) {
        this.trace = trace; this.previous = previous;
        eventId = StoreCounterIdentity.next(); depth = previous == null ? 1 : previous.depth + 1;
    }

    public static StoreCounterDispatchProbe begin(StoreCounterTrace trace, List<?> subscribers,
            boolean playerInventorySlot, Object[] eventFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) return NOOP;
        StoreCounterDispatchProbe previous = CURRENT.get();
        if (OVERFLOW.get() > 0 || previous != null && previous.depth >= MAX_DEPTH) {
            OVERFLOW.set(OVERFLOW.get() + 1);
            return OVERFLOW_PROBE;
        }
        StoreCounterDispatchProbe result = new StoreCounterDispatchProbe(trace, previous);
        CURRENT.set(result);
        result.selected(subscribers, playerInventorySlot, eventFields);
        return result;
    }

    private void selected(List<?> subscribers, boolean playerInventorySlot, Object[] eventFields) {
        try {
            List<Object> listeners = new ArrayList<>();
            int total = subscribers == null ? 0 : subscribers.size();
            int eligible = 0;
            for (int i = 0; i < Math.min(total, MAX_LISTENERS); i++) {
                if (!(subscribers.get(i) instanceof Subscription<?> sub)) continue;
                boolean deleted = sub.shouldDelete();
                if (!deleted) eligible++;
                listeners.add("listener" + i + "SubscriptionId"); listeners.add(sub.diagnosticId());
                listeners.add("listener" + i + "DeleteRequested"); listeners.add(deleted);
            }
            trace.record("PUBLISH", playerInventorySlot ? "SELECTED_PLAYER_SLOT_LISTENERS" : "SELECTED_CONTAINER_SLOT_LISTENERS",
                    StoreCounterFields.concat(new Object[]{"slotEventId", eventId,
                            "registeredCount", total,
                            "eligibleCount", total <= MAX_LISTENERS ? eligible : "UNAVAILABLE_SCAN_LIMIT",
                            "eligibleInInspectedPrefix", eligible,
                            "omittedSubscriptions", Math.max(0, total - MAX_LISTENERS),
                            "publicationBoundary", "EVENTBUS_AFTER_DEFERRED_REGISTRATION_FLUSH"}, StoreCounterFields.concat(eventFields, listeners.toArray())));
        } catch (RuntimeException | LinkageError ignored) {
            trace.record("PUBLISH", "LISTENER_SNAPSHOT_UNAVAILABLE", "slotEventId", eventId);
        }
    }

    public void entering(Subscription<?> subscription) {
        if (this == NOOP || this == OVERFLOW_PROBE) return;
        entered++; listenerPending = true;
        currentSubscription = subscription.diagnosticId();
        currentListenerTrace = subscription.diagnosticCounterTrace();
        currentListenerTrace.record("DELIVERY", "CALLBACK_ENTER",
                "slotEventId", eventId, "subscriptionId", currentSubscription);
    }
    public void returned() {
        if (this == NOOP || this == OVERFLOW_PROBE) return;
        returned++; listenerPending = false;
        currentListenerTrace.record("DELIVERY", "CALLBACK_RETURN",
                "slotEventId", eventId, "subscriptionId", currentSubscription);
    }
    public void skipped(Subscription<?> subscription) {
        if (this == NOOP || this == OVERFLOW_PROBE) return;
        skipped++;
        subscription.diagnosticCounterTrace().record("DELIVERY", "DELETE_REQUESTED_SKIP",
                "slotEventId", eventId, "subscriptionId", subscription.diagnosticId());
    }
    public void classCastFailed() {
        if (this == NOOP || this == OVERFLOW_PROBE) return;
        classCastFailures++; listenerPending = false;
        currentListenerTrace.record("DELIVERY", "NATIVE_CLASS_CAST_CATCH",
                "slotEventId", eventId, "subscriptionId", currentSubscription,
                "exceptionType", "ClassCastException", "exceptionHandling", "EXISTING_EVENTBUS_CATCH");
    }
    public void completed() { if (this != NOOP && this != OVERFLOW_PROBE) completed = true; }

    public static Object[] currentFields() {
        StoreCounterDispatchProbe current = CURRENT.get();
        boolean available = current != null && OVERFLOW.get() == 0 && ChatClefDiagnostics.isBoundaryEnabled();
        return new Object[]{"slotEventId", available ? current.eventId : "UNAVAILABLE",
                "deliveringSubscriptionId", available ? current.currentSubscription : "UNAVAILABLE",
                "dispatchContextStatus", OVERFLOW.get() > 0 ? "NESTING_LIMIT" : available ? "OBSERVED" : "UNAVAILABLE"};
    }

    @Override public void close() {
        if (this == NOOP) return;
        if (this == OVERFLOW_PROBE) {
            int remaining = OVERFLOW.get() - 1;
            if (remaining <= 0) OVERFLOW.remove(); else OVERFLOW.set(remaining);
            return;
        }
        if (closed) return;
        closed = true;
        try {
            if (listenerPending) currentListenerTrace.record("DELIVERY", "CALLBACK_DID_NOT_RETURN",
                    "slotEventId", eventId, "subscriptionId", currentSubscription,
                    "exceptionType", "UNAVAILABLE_NO_NEW_CATCH", "exceptionPropagation", "UNCHANGED");
            trace.record("PUBLISH", completed ? "PUBLISH_RETURNED" : "PUBLISH_DID_NOT_RETURN",
                    "slotEventId", eventId, "callbacksEntered", entered, "callbacksReturned", returned,
                    "callbacksSkipped", skipped, "nativeClassCastCatches", classCastFailures,
                    "lastSubscriptionId", currentSubscription, "publicationReturned", completed,
                    "serverConfirmation", "NOT_OBSERVED");
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
