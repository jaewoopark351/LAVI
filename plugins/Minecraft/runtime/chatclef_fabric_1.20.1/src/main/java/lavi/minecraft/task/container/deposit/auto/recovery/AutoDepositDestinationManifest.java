package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.util.Dimension;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//20260826_kpopmodder: Added operation-local evidence of confirmed automatic deposit destinations.
public final class AutoDepositDestinationManifest {
    private final Object worldIdentity;
    private final Dimension dimension;
    private final long operationEpoch;
    private final Map<BlockPos, Map<Item, Integer>> confirmedDeposits = new LinkedHashMap<>();

    public AutoDepositDestinationManifest(Object worldIdentity, Dimension dimension, long operationEpoch) {
        this.worldIdentity = Objects.requireNonNull(worldIdentity, "worldIdentity");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.operationEpoch = operationEpoch;
    }

    public void record(BlockPos position, Item item, int positiveDelta) {
        if (position == null || item == null || positiveDelta <= 0) {
            return;
        }
        confirmedDeposits
                .computeIfAbsent(position.toImmutable(), ignored -> new LinkedHashMap<>())
                .merge(item, positiveDelta, AutoDepositDestinationManifest::saturatingAdd);
    }

    public int confirmedCount(BlockPos position, Item item) {
        return confirmedDeposits.getOrDefault(position, Map.of()).getOrDefault(item, 0);
    }

    public Map<Item, Integer> confirmedAt(BlockPos position) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(
                confirmedDeposits.getOrDefault(position, Map.of())
        ));
    }

    public Set<BlockPos> positions() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(confirmedDeposits.keySet()));
    }

    public Object worldIdentity() {
        return worldIdentity;
    }

    public Dimension dimension() {
        return dimension;
    }

    public long operationEpoch() {
        return operationEpoch;
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }
}
