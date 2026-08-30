package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public class ContainerStoredTracker {
    private final HashMap<Item, Integer> _totalDeposited = new HashMap<>();
    private final Predicate<Slot> _acceptDeposit;

    private Subscription<SlotClickChangedEvent> _slotClickChangedSubscription;

    public ContainerStoredTracker(Predicate<Slot> acceptDeposit) {
        _acceptDeposit = acceptDeposit;
    }

    private void trackChange(Item item, int delta) {
        _totalDeposited.put(item, _totalDeposited.getOrDefault(item, 0) + delta);
    }

    public void startTracking() {
        _slotClickChangedSubscription = EventBus.subscribe(SlotClickChangedEvent.class, evt -> {
            Slot slot = evt.slot;
            ItemStack before = evt.before;
            ItemStack after = evt.after;
            boolean diagnosticsEnabled = ChatClefDiagnostics.isBoundaryEnabled();
            if (diagnosticsEnabled) {
                StoreDepositDiagnostics.clearPredicateSnapshot();
            }
            boolean playerInventorySlot = slot.isSlotInPlayerInventory();
            boolean acceptPredicateEvaluated = false;
            boolean acceptPredicateResult = false;
            if (!playerInventorySlot) {
                acceptPredicateEvaluated = true;
                acceptPredicateResult = _acceptDeposit.test(slot);
            }
            boolean automaticDiagnostics = diagnosticsEnabled
                    && StoreDepositDiagnostics.hasAutomaticTrackerContext(this);
            String trackerTotalBefore = automaticDiagnostics
                    ? diagnosticStoredTotals()
                    : null;
            if (diagnosticsEnabled && !automaticDiagnostics) {
                StoreDepositDiagnostics.logEffectObservation(
                        this,
                        slot,
                        before,
                        after,
                        playerInventorySlot,
                        acceptPredicateEvaluated,
                        acceptPredicateResult);
            }
            if (!playerInventorySlot && acceptPredicateResult) {
                if (before.getItem() != after.getItem()) {
                    // Before has been replaced! We lost before and added all of after.
                    if (!before.isEmpty())
                        trackChange(before.getItem(), -1 * before.getCount());
                    if (!after.isEmpty())
                        trackChange(after.getItem(), after.getCount());
                } else {
                    // Before and after are the same, track the difference.
                    trackChange(after.getItem(), after.getCount() - before.getCount());
                }
            }
            if (automaticDiagnostics) {
                StoreDepositDiagnostics.logEffectObservation(
                        this,
                        slot,
                        before,
                        after,
                        playerInventorySlot,
                        acceptPredicateEvaluated,
                        acceptPredicateResult,
                        trackerTotalBefore,
                        diagnosticStoredTotals());
            }
        });
        if (ChatClefDiagnostics.isBoundaryEnabled()) {
            StoreDepositDiagnostics.trackerSubscriptionStarted(this);
        }
    }

    public void stopTracking() {
        EventBus.unsubscribe(_slotClickChangedSubscription);
        if (ChatClefDiagnostics.isBoundaryEnabled()) {
            StoreDepositDiagnostics.trackerSubscriptionStopped(this);
        }
    }

    /**
     * How many items have been ADDED to containers satisfying our conditions?
     */
    public int getStoredCount(Item... items) {
        int result = 0;
        for (Item item : items) {
            result += _totalDeposited.getOrDefault(item, 0);
        }
        return result;
    }

    public boolean matches(ItemTarget target) {
        return getStoredCount(target.getMatches()) >= target.getTargetCount();
    }

    public ItemTarget[] getUnstoredItemTargetsYouCanStore(AltoClef mod, ItemTarget[] toStore) {
        List<ItemTarget> result = new ArrayList<>();
        boolean automaticDiagnostics = ChatClefDiagnostics.isBoundaryEnabled()
                && StoreDepositDiagnostics.hasAutomaticTrackerContext(this);
        for (ItemTarget target : toStore) {
            boolean storedSatisfied = matches(target);
            int storedCount = automaticDiagnostics
                    ? diagnosticStoredCount(target.getMatches())
                    : -1;
            boolean hasItemEvaluated = false;
            boolean hasItem = false;
            int firstAvailableCount = -1;
            boolean secondAvailableCountEvaluated = false;
            int secondAvailableCount = -1;
            ItemTarget output = null;
            if (!storedSatisfied) {
                hasItemEvaluated = true;
                hasItem = mod.getItemStorage().hasItem(target.getMatches());
                if (hasItem) {
                    firstAvailableCount = mod.getItemStorage().getItemCount(target);
                    if (firstAvailableCount < target.getTargetCount()) {
                        secondAvailableCountEvaluated = true;
                        secondAvailableCount = mod.getItemStorage().getItemCount(target);
                        output = new ItemTarget(target, secondAvailableCount);
                    } else {
                        output = target;
                    }
                    result.add(output);
                }
            }
            if (automaticDiagnostics) {
                StoreDepositDiagnostics.observeAutomaticNotStoredDecision(
                        this,
                        target,
                        storedCount,
                        storedSatisfied,
                        hasItemEvaluated,
                        hasItem,
                        firstAvailableCount,
                        secondAvailableCountEvaluated,
                        secondAvailableCount,
                        output
                );
            }
        }
        return result.toArray(ItemTarget[]::new);
    }

    private int diagnosticStoredCount(Item... items) {
        int result = 0;
        for (Item item : items) {
            result += _totalDeposited.getOrDefault(item, 0);
        }
        return result;
    }

    private String diagnosticStoredTotals() {
        try {
            return _totalDeposited.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "UNAVAILABLE#DIAGNOSTIC_FORMATTING_ERROR";
        }
    }
}
