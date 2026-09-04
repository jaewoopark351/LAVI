package adris.altoclef.eventbus;

import adris.altoclef.eventbus.events.ScreenOpenEvent;
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
                        if (observeScreenListener
                                && screenListeners.wasEligible(subRaw)) {
                            screenDispatch.listenerSkippedInactive(listenerClass, listenerIdentity);
                        }
                    } else {
                        if (observeScreenListener) {
                            screenDispatch.listenerStarted(listenerClass, listenerIdentity);
                        }
                        sub.accept(event);
                        if (observeScreenListener) {
                            screenDispatch.listenerCompleted(listenerClass, listenerIdentity);
                        }
                    }
                } catch (ClassCastException e) {
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
    }

    private static <T> void subscribeInternal(Class<T> type, Subscription<T> sub) {
        if (!topics.containsKey(type)) {
            topics.put(type, new ArrayList<>());
        }
        topics.get(type).add(sub);
    }

    public static <T> Subscription<T> subscribe(Class<T> type, Consumer<T> consumeEvent) {
        Subscription<T> sub = new Subscription<>(consumeEvent);
        if (lock) {
            toAdd.add(new Pair<>(type, sub));
        } else {
            subscribeInternal(type, sub);
        }
        return sub;
    }

    public static <T> void unsubscribe(Subscription<T> subscription) {
        if (subscription != null)
            subscription.delete();
    }
}
