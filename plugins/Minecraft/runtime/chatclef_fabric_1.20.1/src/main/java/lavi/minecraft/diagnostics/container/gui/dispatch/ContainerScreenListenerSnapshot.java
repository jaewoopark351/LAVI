package lavi.minecraft.diagnostics.container.gui.dispatch;

import adris.altoclef.eventbus.Subscription;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

//20260904_kpopmodder: Freeze listener eligibility for diagnostics without changing EventBus membership.
public final class ContainerScreenListenerSnapshot {
    private static final ContainerScreenListenerSnapshot EMPTY =
            new ContainerScreenListenerSnapshot(0, 0, new IdentityHashMap<>());

    private final int registeredCount;
    private final int eligibleCount;
    private final Map<Subscription, Boolean> eligibility;

    private ContainerScreenListenerSnapshot(
            int registeredCount,
            int eligibleCount,
            Map<Subscription, Boolean> eligibility) {
        this.registeredCount = registeredCount;
        this.eligibleCount = eligibleCount;
        this.eligibility = eligibility;
    }

    public static ContainerScreenListenerSnapshot capture(List<Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return EMPTY;
        }
        Map<Subscription, Boolean> eligibility = new IdentityHashMap<>();
        int eligibleCount = 0;
        for (Subscription subscription : subscriptions) {
            boolean eligible = subscription != null && !subscription.shouldDelete();
            eligibility.put(subscription, eligible);
            if (eligible) {
                eligibleCount++;
            }
        }
        return new ContainerScreenListenerSnapshot(
                subscriptions.size(),
                eligibleCount,
                eligibility
        );
    }

    public static ContainerScreenListenerSnapshot empty() {
        return EMPTY;
    }

    public int registeredCount() {
        return registeredCount;
    }

    public int eligibleCount() {
        return eligibleCount;
    }

    public boolean wasEligible(Subscription subscription) {
        return Boolean.TRUE.equals(eligibility.get(subscription));
    }
}
