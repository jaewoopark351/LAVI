package lavi.minecraft.diagnostics.container.store.deposit.counter;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

//20260914_kpopmodder: Format bounded scalar item/count facts, without NBT or gameplay predicates.
public final class StoreCounterFields {
    public static final int MAX_TARGETS = 4;
    public static final int MAX_MATCHES = 4;
    public static final int MAX_TOTALS = 4;
    private static final int MAX_TOTAL_SCAN = 64;
    private StoreCounterFields() { }

    public static String item(Item item) {
        return item == null ? "UNAVAILABLE" : bounded(String.valueOf(Registries.ITEM.getId(item)), 160);
    }

    public static String stack(ItemStack stack) {
        return stack == null ? "UNAVAILABLE" : item(stack.getItem()) + ":" + stack.getCount();
    }

    public static Object[] totals(Map<Item, Integer> totals) {
        List<Map.Entry<String, Integer>> sample = new ArrayList<>();
        int scanned = 0;
        for (Map.Entry<Item, Integer> entry : totals.entrySet()) {
            if (scanned >= MAX_TOTAL_SCAN) break;
            scanned++;
            sample.add(Map.entry(item(entry.getKey()), entry.getValue()));
        }
        sample.sort(Comparator.comparing(Map.Entry::getKey));
        List<Object> fields = new ArrayList<>();
        int shown = Math.min(MAX_TOTALS, sample.size());
        add(fields, "nativeCounterEntryCount", totals.size());
        add(fields, "counterEntriesScanned", scanned);
        add(fields, "omittedCounterEntries", totals.size() - shown);
        add(fields, "counterEntryOrder", "SORTED_WITHIN_BOUNDED_SAMPLE");
        for (int i = 0; i < shown; i++) {
            add(fields, "counterEntry" + i + "Item", sample.get(i).getKey());
            add(fields, "counterEntry" + i + "Stored", sample.get(i).getValue());
        }
        return fields.toArray();
    }

    public static Object[] targets(ContainerStoredTracker tracker, ItemTarget[] targets) {
        List<Object> fields = new ArrayList<>();
        int length = targets == null ? 0 : targets.length;
        add(fields, "targetSnapshotAvailable", targets != null);
        add(fields, "targetCount", targets == null ? "UNAVAILABLE" : length);
        add(fields, "omittedTargets", Math.max(0, length - MAX_TARGETS));
        add(fields, "counterUnit", "ITEMS_NOT_OCCUPIED_SLOTS");
        add(fields, "counterSnapshotBoundary", "POST_DECISION_SAME_OWNER_THREAD_READ");
        add(fields, "counterSnapshotAuthority", "CLIENT_LOCAL_COUNTER_NOT_SERVER_ACK");
        for (int i = 0; i < Math.min(MAX_TARGETS, length); i++) {
            ItemTarget target = targets[i];
            String prefix = "target" + i;
            if (target == null) { add(fields, prefix + "Status", "UNAVAILABLE_NULL_TARGET"); continue; }
            Item[] matches = target.getMatches();
            add(fields, prefix + "Required", target.getTargetCount());
            add(fields, prefix + "Stored", matches == null ? "UNAVAILABLE_NULL_MATCHES"
                    : matches.length > MAX_MATCHES ? "UNAVAILABLE_MATCH_SCAN_LIMIT" : tracker.getStoredCount(matches));
            add(fields, prefix + "MatchCount", matches == null ? "UNAVAILABLE" : matches.length);
            for (int n = 0; matches != null && n < Math.min(MAX_MATCHES, matches.length); n++)
                add(fields, prefix + "Match" + n, item(matches[n]));
            add(fields, prefix + "OmittedMatches", matches == null ? 0 : Math.max(0, matches.length - MAX_MATCHES));
        }
        return fields.toArray();
    }

    private static void add(List<Object> fields, String key, Object value) { fields.add(key); fields.add(value); }

    public static String bounded(String value, int limit) {
        if (value == null) return "UNAVAILABLE";
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < Math.min(limit, value.length()); i++) {
            char character = value.charAt(i);
            safe.append(Character.isISOControl(character) ? '_' : character);
        }
        if (value.length() > limit) safe.append("[TRUNCATED]");
        return safe.toString();
    }

    // Preserve authoritative event fields and add only context keys that are not already present.
    public static Object[] mergeContext(Object[] fields, Object[] context) {
        java.util.LinkedHashMap<Object, Object> unique = new java.util.LinkedHashMap<>();
        for (Object[] source : new Object[][]{fields, context})
            for (int i = 0; i + 1 < source.length; i += 2)
                unique.putIfAbsent(source[i], source[i + 1]);
        Object[] result = new Object[unique.size() * 2];
        int offset = 0;
        for (Map.Entry<Object, Object> entry : unique.entrySet()) {
            result[offset++] = entry.getKey(); result[offset++] = entry.getValue();
        }
        return result;
    }

    public static Object[] concat(Object[] first, Object... second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
