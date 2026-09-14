package adris.altoclef.eventbus;

import adris.altoclef.eventbus.events.ScreenOpenEvent;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.counter.StoreCounterDispatchProbe;
import lavi.minecraft.diagnostics.container.store.deposit.counter.StoreCounterTrace;
import lavi.minecraft.diagnostics.container.store.deposit.counter.StoreCounterSubscriptionSnapshot;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.container.gui.dispatch.ContainerScreenDispatchProbe;
import lavi.minecraft.diagnostics.container.gui.dispatch.ContainerScreenListenerSnapshot;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * A static class to solve dependency issues. Lets us send and receive events globally, decoupling our codebase.
 * <p>
 * Technically `ConfigHelper` does something like this, but here is a more general case.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EventBus {

    private static final HashMap<Class, List<Subscription>> topics = new HashMap<>();
    private static final List<Pair<Class, Subscription>> toAdd = new ArrayList<>();
    private static boolean lock;

    public static <T> void publish(T event) {
        Class type = event.getClass();

        // Add all subscriptions we need to add
        for (Pair<Class, Subscription> toAdd : toAdd) {
            subscribeInternal(toAdd.getLeft(), toAdd.getRight());
        }
        toAdd.clear();

        List<Subscription> subscribers = topics.get(type);
        //20260914_kpopmodder: Observe this exact slot-event dispatch; all native mutations and catches remain unchanged.
        StoreCounterDispatchProbe counterDispatch = event instanceof SlotClickChangedEvent
                ? StoreDepositDiagnostics.beginCounterDispatch(event, subscribers)
                : StoreCounterDispatchProbe.NOOP;
        try {
        ScreenOpenEvent screenOpenEvent = event instanceof ScreenOpenEvent
                ? (ScreenOpenEvent) event
                : null;
        boolean observeScreenTransport = screenOpenEvent != null
                && ContainerGuiDiagnostics.screenTransportObservationEnabled();
        ContainerScreenListenerSnapshot screenListeners = observeScreenTransport
                ? ContainerScreenListenerSnapshot.capture(subscribers)
                : ContainerScreenListenerSnapshot.empty();

        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerScreenDispatchProbe screenDispatch = observeScreenTransport
                ? ContainerGuiDiagnostics.beginScreenDispatch(
                        screenOpenEvent,
                        screenListeners.registeredCount(),
                        screenListeners.eligibleCount()
                )
                : ContainerScreenDispatchProbe.noop();

        if (subscribers != null) {

            // Subscriptions can be deleted while they're called
            List<Subscription> toDelete = new ArrayList<>();

            // Go through our subscription list. We shouldn't modify the list while we're iterating it.
            lock = true;
            for (Subscription subRaw : subscribers) {
                boolean observeScreenListener = screenDispatch.isActive();
                String listenerClass = "unavailable";
                String listenerIdentity = "unavailable";
                if (observeScreenListener) {
                    listenerClass = subRaw.diagnosticCallbackClassName();
                    listenerIdentity = listenerClass + "@"
                            + Integer.toHexString(System.identityHashCode(subRaw));
                }
                Subscription<T> sub;
                try {
                    sub = (Subscription<T>) subRaw;
                    if (sub.shouldDelete()) {
                        toDelete.add(sub);
                        counterDispatch.skipped(sub);
                        if (observeScreenListener
                                && screenListeners.wasEligible(subRaw)) {
                            screenDispatch.listenerSkippedInactive(listenerClass, listenerIdentity);
                        }
                    } else {
                        if (observeScreenListener) {
                            screenDispatch.listenerStarted(listenerClass, listenerIdentity);
                        }
                        counterDispatch.entering(sub);
                        sub.accept(event);
                        counterDispatch.returned();
                        if (observeScreenListener) {
                            screenDispatch.listenerCompleted(listenerClass, listenerIdentity);
                        }
                    }
                } catch (ClassCastException e) {
                    counterDispatch.classCastFailed();
                    if (observeScreenListener) {
                        screenDispatch.listenerClassCastFailed(listenerClass, listenerIdentity);
                    }
                    System.err.println("TRIED PUBLISHING MISMAPPED EVENT: " + event);
                    e.printStackTrace();
                }
            }
            // Delete all subscriptions
            lock = false;
        }
        screenDispatch.dispatchCompleted();
        counterDispatch.completed();
        } finally {
            counterDispatch.close();
        }
    }

    private static <T> void subscribeInternal(Class<T> type, Subscription<T> sub) {
        if (!topics.containsKey(type)) {
            topics.put(type, new ArrayList<>());
        }
        topics.get(type).add(sub);
        sub.diagnosticCounterTrace().record("SUBSCRIPTION", "APPLIED_TO_NATIVE_TOPIC",
                "subscriptionId", sub.diagnosticId(), "nativeRegisteredIndex", topics.get(type).size() - 1,
                "nativeRegisteredCount", topics.get(type).size(), "nativeDeleteRequested", sub.shouldDelete());
    }

    public static <T> Subscription<T> subscribe(Class<T> type, Consumer<T> consumeEvent) {
        return subscribe(type, consumeEvent, StoreCounterTrace.NOOP);
    }

    //20260914_kpopmodder: Optional observation context only; existing callers retain the same native subscription path.
    public static <T> Subscription<T> subscribe(Class<T> type, Consumer<T> consumeEvent, StoreCounterTrace counterTrace) {
        Subscription<T> sub = new Subscription<>(consumeEvent);
        sub.diagnosticCounterTrace(counterTrace);
        if (lock) {
            toAdd.add(new Pair<>(type, sub));
            sub.diagnosticCounterTrace().record("SUBSCRIPTION", "DEFERRED_BY_NATIVE_LOCK",
                    "subscriptionId", sub.diagnosticId(), "nativePendingCount", toAdd.size());
        } else {
            subscribeInternal(type, sub);
        }
        return sub;
    }

    public static <T> void unsubscribe(Subscription<T> subscription) {
        if (subscription != null)
            subscription.delete();
    }

    //20260914_kpopmodder: Read actual collections at subscription boundaries, without flushing or removing anything.
    public static StoreCounterSubscriptionSnapshot diagnosticSlotSubscription(Subscription<?> subscription) {
        if (subscription == null) return StoreCounterSubscriptionSnapshot.unavailable("NO_SUBSCRIPTION");
        try {
            List<Subscription> registered = topics.get(SlotClickChangedEvent.class);
            // A bounded absence scan must never report a false proven absence.
            if ((registered != null && registered.size() > 256) || toAdd.size() > 256)
                return StoreCounterSubscriptionSnapshot.unavailable("SCAN_LIMIT_EXCEEDED");
            int index = -1;
            if (registered != null) {
                for (int i = 0; i < registered.size(); i++) {
                    if (registered.get(i) == subscription) { index = i; break; }
                }
            }
            boolean pending = false;
            for (Pair<Class, Subscription> entry : toAdd) {
                if (entry.getLeft() == SlotClickChangedEvent.class && entry.getRight() == subscription) {
                    pending = true; break;
                }
            }
            return new StoreCounterSubscriptionSnapshot(true, index >= 0, pending, subscription.shouldDelete(),
                    index, registered == null ? 0 : registered.size(), toAdd.size(), "NONE");
        } catch (RuntimeException | LinkageError observationFailure) {
            return StoreCounterSubscriptionSnapshot.unavailable("CAPTURE_EXCEPTION_" + observationFailure.getClass().getSimpleName());
        }
    }
}
