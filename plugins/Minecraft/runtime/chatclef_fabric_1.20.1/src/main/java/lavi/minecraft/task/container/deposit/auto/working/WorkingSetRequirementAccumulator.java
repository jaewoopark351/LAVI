package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class WorkingSetRequirementAccumulator {
    private final Map<Item, Integer> available;
    private final Map<Item, Integer> required = new LinkedHashMap<>();

    public WorkingSetRequirementAccumulator(Map<Item, Integer> available) {
        this.available = Map.copyOf(Objects.requireNonNull(available, "available"));
    }

    public void reserveTarget(ItemTarget target) {
        if (target != null) {
            reserveTarget(target, target.getTargetCount());
        }
    }

    public void reserveTarget(ItemTarget target, int count) {
        if (target == null || count <= 0) {
            return;
        }
        for (Item item : target.getMatches()) {
            int held = available.getOrDefault(item, 0);
            if (held > 0) {
                required.merge(item, Math.min(held, count), WorkingSetRequirementAccumulator::saturatingAdd);
            }
        }
    }

    public void reserveAll(Item item) {
        int held = available.getOrDefault(item, 0);
        if (held > 0) {
            required.merge(item, held, WorkingSetRequirementAccumulator::saturatingAdd);
        }
    }

    public int available(Item item) {
        return available.getOrDefault(item, 0);
    }

    public Map<Item, Integer> requirements() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(required));
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }
}
