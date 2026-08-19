package lavi.minecraft.diagnostics.container.store.deposit.budget;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class StoreDepositDetailBudget {
    private final Map<String, Set<String>> emittedKeysByEvent = new LinkedHashMap<>();
    private final Map<String, Integer> suppressedByEvent = new LinkedHashMap<>();
    private int emittedCount;

    synchronized boolean shouldEmit(String eventName, String semanticKey) {
        String normalizedEvent = normalize(eventName);
        String normalizedKey = normalize(semanticKey);
        Set<String> emitted = emittedKeysByEvent.get(normalizedEvent);
        if (emitted != null && emitted.contains(normalizedKey)) {
            return false;
        }
        if (emittedCount >= StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP) {
            incrementSuppressed(normalizedEvent);
            return false;
        }
        if (emitted == null) {
            if (emittedKeysByEvent.size() >= StoreDepositBudgetConstants.MAX_DETAIL_KEYS_PER_EVENT) {
                incrementSuppressed(normalizedEvent);
                return false;
            }
            emitted = new HashSet<>();
            emittedKeysByEvent.put(normalizedEvent, emitted);
        }
        if (emitted.size() >= StoreDepositBudgetConstants.MAX_DETAIL_KEYS_PER_EVENT) {
            incrementSuppressed(normalizedEvent);
            return false;
        }
        emitted.add(normalizedKey);
        emittedCount++;
        return true;
    }

    synchronized int emittedCount() {
        return emittedCount;
    }

    synchronized String suppressedCounts() {
        return suppressedByEvent.toString();
    }

    synchronized String bucketSizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        emittedKeysByEvent.forEach((event, keys) -> sizes.put(event, keys.size()));
        return sizes.toString();
    }

    private void incrementSuppressed(String eventName) {
        suppressedByEvent.put(eventName, suppressedByEvent.getOrDefault(eventName, 0) + 1);
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() <= StoreDepositBudgetConstants.MAX_KEY_LENGTH
                ? value
                : value.substring(0, StoreDepositBudgetConstants.MAX_KEY_LENGTH) + "...";
    }
}
