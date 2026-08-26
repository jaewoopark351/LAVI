package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StoreDepositInteractionBindingRegistry {
    private static final int MAX_BINDINGS = 256;
    private static final long MAX_AGE_TICKS = 40;

    private final LinkedHashMap<Long, StoreDepositInteractionContext> bindings = new LinkedHashMap<>();

    public synchronized void bind(StoreDepositInteractionContext context, long currentTick) {
        if (context == null) {
            return;
        }
        expire(currentTick);
        if (!bindings.containsKey(context.interactionId()) && bindings.size() >= MAX_BINDINGS) {
            Iterator<Long> oldest = bindings.keySet().iterator();
            if (oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
        bindings.put(context.interactionId(), context);
    }

    public synchronized StoreDepositInteractionContext find(long interactionId, long currentTick) {
        expire(currentTick);
        return bindings.get(interactionId);
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
            }
        }
    }
}
