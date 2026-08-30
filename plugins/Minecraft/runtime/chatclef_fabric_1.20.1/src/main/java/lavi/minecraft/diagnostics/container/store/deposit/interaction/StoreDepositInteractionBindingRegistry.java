package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StoreDepositInteractionBindingRegistry {
    private static final int MAX_BINDINGS = 256;
    private static final int MAX_TOMBSTONES = 256;
    private static final long MAX_AGE_TICKS = 40;

    private final LinkedHashMap<Long, StoreDepositInteractionContext> bindings = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Tombstone> tombstones = new LinkedHashMap<>();

    public synchronized void bind(StoreDepositInteractionContext context, long currentTick) {
        if (context == null) {
            return;
        }
        expire(currentTick);
        tombstones.remove(context.interactionId());
        if (!bindings.containsKey(context.interactionId()) && bindings.size() >= MAX_BINDINGS) {
            Iterator<Long> oldest = bindings.keySet().iterator();
            if (oldest.hasNext()) {
                Long interactionId = oldest.next();
                StoreDepositInteractionContext evicted = bindings.get(interactionId);
                oldest.remove();
                addTombstone(interactionId, evicted, LookupStatus.EVICTED);
            }
        }
        bindings.put(context.interactionId(), context);
    }

    public synchronized StoreDepositInteractionContext find(long interactionId, long currentTick) {
        LookupResult result = lookup(interactionId, currentTick);
        return result.status() == LookupStatus.FOUND ? result.context() : null;
    }

    public synchronized LookupResult lookup(long interactionId, long currentTick) {
        expire(currentTick);
        StoreDepositInteractionContext context = bindings.get(interactionId);
        if (context != null) {
            return new LookupResult(LookupStatus.FOUND, context);
        }
        Tombstone tombstone = tombstones.get(interactionId);
        return tombstone == null
                ? new LookupResult(LookupStatus.NOT_FOUND, null)
                : new LookupResult(tombstone.status(), tombstone.context());
    }

    synchronized int size() {
        return bindings.size();
    }

    private void expire(long currentTick) {
        Iterator<Map.Entry<Long, StoreDepositInteractionContext>> iterator = bindings.entrySet().iterator();
        while (iterator.hasNext()) {
            StoreDepositInteractionContext context = iterator.next().getValue();
            long age = currentTick - context.interactionStartClientTick();
            if (age < 0 || age > MAX_AGE_TICKS) {
                iterator.remove();
                addTombstone(
                        context.interactionId(),
                        context,
                        LookupStatus.EXPIRED
                );
            }
        }
    }

    private void addTombstone(long interactionId,
                              StoreDepositInteractionContext context,
                              LookupStatus status) {
        if (context == null) {
            return;
        }
        if (!tombstones.containsKey(interactionId) && tombstones.size() >= MAX_TOMBSTONES) {
            Iterator<Long> oldest = tombstones.keySet().iterator();
            if (oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
        tombstones.put(interactionId, new Tombstone(status, context));
    }

    public enum LookupStatus {
        FOUND,
        EXPIRED,
        EVICTED,
        NOT_FOUND
    }

    public record LookupResult(LookupStatus status,
                               StoreDepositInteractionContext context) {
    }

    private record Tombstone(LookupStatus status,
                             StoreDepositInteractionContext context) {
    }
}
