package lavi.minecraft.task.container.deposit.auto.admission.conditions.gain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

//20260914_kpopmodder: Count newly gained safe surplus without treating survival actions as recovery.
public final class AutoDepositGainEvidence {
    static final int MAX_TRACKED_ITEMS = 64;

    private final Map<String, Integer> highWater = new TreeMap<>();
    private final Map<String, Gain> pending = new TreeMap<>();
    private Object world;
    private long episode;
    private boolean saturated;
    private String published = "";

    /** Counts include main inventory, armor and offhand; safe targets come from the current plan. */
    public String observe(Object currentWorld, long currentEpisode,
                          Map<String, Integer> totalCounts, Set<String> safeTargetItems) {
        Objects.requireNonNull(currentWorld, "currentWorld");
        Objects.requireNonNull(safeTargetItems, "safeTargetItems");
        Map<String, Integer> counts = validatedCounts(totalCounts);
        boolean changedWorld = world != currentWorld;
        if (changedWorld || episode != currentEpisode) {
            world = currentWorld;
            episode = currentEpisode;
            highWater.clear();
            pending.clear();
            saturated = counts.size() > MAX_TRACKED_ITEMS;
            if (!saturated) highWater.putAll(counts);
            // A fresh pressure episode is not itself a physical recovery condition.
            if (changedWorld) published = "";
            return published;
        }
        if (saturated) return published;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String item = entry.getKey();
            int previous = highWater.getOrDefault(item, 0);
            if (entry.getValue() <= previous) continue;
            if (!highWater.containsKey(item) && highWater.size() == MAX_TRACKED_ITEMS) {
                saturated = true;
                pending.clear();
                return published;
            }
            highWater.put(item, entry.getValue());
            pending.put(item, new Gain(previous, entry.getValue()));
        }
        StringBuilder gained = new StringBuilder();
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Gain> entry = iterator.next();
            Gain gain = entry.getValue();
            if (counts.getOrDefault(entry.getKey(), 0) < gain.current()) {
                iterator.remove();
            } else if (safeTargetItems.contains(entry.getKey())) {
                String item = entry.getKey();
                gained.append(item.length()).append(':').append(item)
                        .append(gain.previous()).append('>').append(gain.current()).append(';');
                iterator.remove();
            }
        }
        if (!gained.isEmpty()) published = gained.toString();
        return published;
    }

    private static Map<String, Integer> validatedCounts(Map<String, Integer> values) {
        Objects.requireNonNull(values, "totalCounts");
        Map<String, Integer> counts = new TreeMap<>();
        values.forEach((item, count) -> {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(count, "count");
            if (count < 0) throw new IllegalArgumentException("Negative item count");
            if (count > 0) counts.put(item, count);
        });
        return counts;
    }

    private record Gain(int previous, int current) { }
}
