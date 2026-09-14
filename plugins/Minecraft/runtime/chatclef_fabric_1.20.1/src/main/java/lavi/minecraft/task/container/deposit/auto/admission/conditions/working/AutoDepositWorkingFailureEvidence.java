package lavi.minecraft.task.container.deposit.auto.admission.conditions.working;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

//20260914_kpopmodder: A fresh plan cannot erase the original failed reservation.
public final class AutoDepositWorkingFailureEvidence {
    private static final int MAX_RESERVED_ITEM_TYPES = 64;
    private Object world;
    private Object dimension;
    private Object userRoot;
    private Map<String, Integer> reserved = Map.of();
    private String reservationKey;

    /** Capture the verifier's immutable reservation and the failure, not a later healthy observation. */
    public String capture(Object world, Object dimension, Object userRoot, Map<String, Integer> reservedCounts) {
        Map<String, Integer> copy = new TreeMap<>();
        Objects.requireNonNull(reservedCounts, "reservedCounts").forEach((item, count) -> {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(count, "count");
            if (count < 0) throw new IllegalArgumentException("Negative reservation");
            if (count > 0) copy.put(item, count);
        });
        this.world = world;
        this.dimension = dimension;
        this.userRoot = userRoot;
        if (world == null || dimension == null || userRoot == null || copy.isEmpty()
                || copy.size() > MAX_RESERVED_ITEM_TYPES) {
            reserved = Map.of();
            reservationKey = null;
            return null;
        }
        reserved = Map.copyOf(copy);
        StringBuilder key = new StringBuilder("working_reservation:");
        copy.forEach((item, count) -> key.append(item.length()).append(':').append(item).append(count).append(';'));
        reservationKey = key.toString();
        return reservationKey + "deficit";
    }

    /** Main-plus-cursor counts match the authoritative working-set verifier. */
    public String observe(Object currentWorld, Object currentDimension, Object currentRoot,
                          Map<String, Integer> mainAndCursorCounts) {
        if (!matchesContext(currentWorld, currentDimension, currentRoot) || mainAndCursorCounts == null) return null;
        for (Map.Entry<String, Integer> required : reserved.entrySet()) {
            Integer held = mainAndCursorCounts.getOrDefault(required.getKey(), 0);
            if (held == null || held < 0) return null;
            if (held < required.getValue()) return reservationKey + "deficit";
        }
        return reservationKey + "restored";
    }

    public boolean matchesContext(Object currentWorld, Object currentDimension, Object currentRoot) {
        return reservationKey != null && world == currentWorld && dimension == currentDimension && userRoot == currentRoot;
    }
}
