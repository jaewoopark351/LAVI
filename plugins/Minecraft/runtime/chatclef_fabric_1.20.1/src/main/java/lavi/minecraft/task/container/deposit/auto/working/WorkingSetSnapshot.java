package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//20260826_kpopmodder: Added an immutable active-task reservation snapshot for automatic deposit maintenance.
public final class WorkingSetSnapshot {
    private final Task userTaskRoot;
    private final List<Task> taskPath;
    private final Object worldIdentity;
    private final Dimension dimension;
    private final long epoch;
    private final Map<Item, Integer> mainInventoryCounts;
    private final Map<Item, Integer> preDepositCounts;
    private final Map<Item, Integer> requiredCounts;
    private final Map<Item, Integer> reservedCounts;

    public WorkingSetSnapshot(Task userTaskRoot,
                              List<Task> taskPath,
                              Object worldIdentity,
                              Dimension dimension,
                              long epoch,
                              Map<Item, Integer> mainInventoryCounts,
                              Map<Item, Integer> preDepositCounts,
                              Map<Item, Integer> requiredCounts) {
        this.userTaskRoot = Objects.requireNonNull(userTaskRoot, "userTaskRoot");
        this.taskPath = List.copyOf(Objects.requireNonNull(taskPath, "taskPath"));
        this.worldIdentity = Objects.requireNonNull(worldIdentity, "worldIdentity");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.epoch = epoch;
        this.mainInventoryCounts = immutableCounts(mainInventoryCounts);
        this.preDepositCounts = immutableCounts(preDepositCounts);
        this.requiredCounts = immutableCounts(requiredCounts);

        Map<Item, Integer> reservations = new LinkedHashMap<>();
        this.requiredCounts.forEach((item, required) -> {
            int available = this.preDepositCounts.getOrDefault(item, 0);
            int reserved = Math.min(available, required);
            if (reserved > 0) {
                reservations.put(item, reserved);
            }
        });
        reservedCounts = Collections.unmodifiableMap(reservations);
    }

    public Task userTaskRoot() {
        return userTaskRoot;
    }

    public List<Task> taskPath() {
        return taskPath;
    }

    public Object worldIdentity() {
        return worldIdentity;
    }

    public Dimension dimension() {
        return dimension;
    }

    public long epoch() {
        return epoch;
    }

    public Map<Item, Integer> mainInventoryCounts() {
        return mainInventoryCounts;
    }

    public Map<Item, Integer> preDepositCounts() {
        return preDepositCounts;
    }

    public Map<Item, Integer> requiredCounts() {
        return requiredCounts;
    }

    public Map<Item, Integer> reservedCounts() {
        return reservedCounts;
    }

    public int reservedCount(Item item) {
        return reservedCounts.getOrDefault(item, 0);
    }

    public Map<Item, Integer> deficits(Map<Item, Integer> currentCounts) {
        Objects.requireNonNull(currentCounts, "currentCounts");
        Map<Item, Integer> deficits = new LinkedHashMap<>();
        reservedCounts.forEach((item, reserved) -> {
            int deficit = Math.max(0, reserved - currentCounts.getOrDefault(item, 0));
            if (deficit > 0) {
                deficits.put(item, deficit);
            }
        });
        return Collections.unmodifiableMap(deficits);
    }

    public boolean matchesContext(Task root, Object world, Dimension currentDimension) {
        return userTaskRoot == root && worldIdentity == world && dimension == currentDimension;
    }

    private static Map<Item, Integer> immutableCounts(Map<Item, Integer> source) {
        Objects.requireNonNull(source, "source");
        Map<Item, Integer> copy = new LinkedHashMap<>();
        source.forEach((item, count) -> {
            if (item != null && count != null && count > 0) {
                copy.put(item, count);
            }
        });
        return Collections.unmodifiableMap(copy);
    }
}
