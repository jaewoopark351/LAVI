package lavi.minecraft.diagnostics.container.store.deposit.budget;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class StoreDepositDetailBudget {
    private final Map<StoreDepositDetailFamily, Set<String>> emittedKeysByFamily = new LinkedHashMap<>();
    private final Map<String, Integer> emittedByEvent = new LinkedHashMap<>();
    private final Map<String, Integer> suppressedByEvent = new LinkedHashMap<>();
    private final Map<StoreDepositDetailFamily, Integer> suppressedByFamily = new LinkedHashMap<>();
    private int emittedCount;

    synchronized boolean shouldEmit(String eventName, String semanticKey) {
        String normalizedEvent = normalize(eventName);
        String normalizedKey = normalize(semanticKey);
        String familyKey = normalizedEvent + "|" + normalizedKey;
        StoreDepositDetailFamily family = StoreDepositDetailFamily.forEvent(normalizedEvent);
        Set<String> emitted = emittedKeysByFamily.computeIfAbsent(family, ignored -> new HashSet<>());
        if (emitted.contains(familyKey)) {
            incrementSuppressed(normalizedEvent, family);
            return false;
        }
        if (emittedCount >= StoreDepositBudgetConstants.OPERATION_DETAIL_CAP || emitted.size() >= family.cap()) {
            incrementSuppressed(normalizedEvent, family);
            return false;
        }
        emitted.add(familyKey);
        emittedCount++;
        emittedByEvent.put(normalizedEvent, emittedByEvent.getOrDefault(normalizedEvent, 0) + 1);
        return true;
    }

    synchronized void recordSuppressed(String eventName) {
        String normalizedEvent = normalize(eventName);
        incrementSuppressed(normalizedEvent, StoreDepositDetailFamily.forEvent(normalizedEvent));
    }

    synchronized int emittedCount() {
        return emittedCount;
    }

    synchronized String suppressedCounts() {
        return suppressedByEvent.toString();
    }

    synchronized String suppressedFamilyCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        suppressedByFamily.forEach((family, count) -> counts.put(family.name(), count));
        return counts.toString();
    }

    synchronized String familySizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        emittedKeysByFamily.forEach((family, keys) -> sizes.put(family.name(), keys.size()));
        return sizes.toString();
    }

    synchronized String eventSizes() {
        return emittedByEvent.toString();
    }

    private void incrementSuppressed(String eventName, StoreDepositDetailFamily family) {
        suppressedByEvent.put(eventName, suppressedByEvent.getOrDefault(eventName, 0) + 1);
        suppressedByFamily.put(family, suppressedByFamily.getOrDefault(family, 0) + 1);
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
