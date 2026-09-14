package lavi.minecraft.task.container.deposit.auto.admission.conditions;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.trackers.storage.ContainerType;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import net.minecraft.block.Block;
//#if MC >= 11903
import net.minecraft.registry.Registries;
//#else
//$$ import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

//20260914_kpopmodder: Read bounded destination facts without path requests or priority evaluation.
final class AutoDepositDestinationConditionReader {
    private static final int MAX_CANDIDATES = 64;
    private final AutoDepositTrustedDestinationRepository repository;
    private final int trustedDistance;

    AutoDepositDestinationConditionReader(AutoDepositTrustedDestinationRepository repository, int trustedDistance) {
        this.repository = Objects.requireNonNull(repository, "repository");
        if (trustedDistance <= 0) throw new IllegalArgumentException("trustedDistance must be positive");
        this.trustedDistance = trustedDistance;
    }

    AutoDepositDestinationConditions read(AltoClef mod, AutoDepositPlan plan) {
        TreeMap<String, Candidate> candidates = new TreeMap<>();
        for (BlockPos position : mod.getBlockScanner()
                .getKnownLocationsIncludeUnreachable(StoreInContainerTask.CONTAINER_BLOCKS)) {
            if (position != null && position.isWithinDistance(mod.getPlayer().getPos(), 50)) {
                add(candidates, false, position);
            }
        }
        for (ContainerCache cache : mod.getItemStorage().getCachedContainers()) {
            if (cache != null && cache.getDimension() == plan.context().dimension()
                    && (cache.getContainerType() == ContainerType.CHEST
                    || cache.getContainerType() == ContainerType.SHULKER
                    || cache.getContainerType() == ContainerType.MISC)
                    && cache.getBlockPos().isWithinDistance(mod.getPlayer().getPos(), 50)) {
                add(candidates, false, cache.getBlockPos());
            }
        }
        for (AutoDepositTrustedDestination destination : repository.destinations()) {
            if (destination != null && destination.enabled()
                    && destination.worldKey().equals(plan.context().persistentWorldKey())
                    && destination.dimension() == plan.context().dimension()) {
                add(candidates, true, destination.position());
            }
        }
        List<String> states = new ArrayList<>();
        for (var entry : candidates.entrySet()) {
            if (states.size() >= MAX_CANDIDATES) break;
            Candidate candidate = entry.getValue();
            BlockPos position = candidate.position();
            boolean loaded = mod.getChunkTracker().isChunkLoaded(position);
            boolean within = position.isWithinDistance(mod.getPlayer().getPos(),
                    candidate.trusted() ? trustedDistance : 50);
            boolean unreachable = mod.getBlockScanner().isUnreachable(position);
            String block = "unloaded";
            String above = "unloaded";
            boolean supported = false;
            if (loaded) {
                Block actual = mod.getWorld().getBlockState(position).getBlock();
                supported = Arrays.stream(StoreInContainerTask.CONTAINER_BLOCKS).anyMatch(actual::equals);
                block = blockId(actual);
                above = blockId(mod.getWorld().getBlockState(position.up()).getBlock());
                if (!candidate.trusted() && !supported) continue;
            }
            // Presence and zero capacity are distinct; uncached never means an empty chest.
            Optional<ContainerCache> cache = mod.getItemStorage().getContainerAtPosition(position);
            int empty = cache.map(ContainerCache::getEmptySlotCount).orElse(-1);
            states.add(entry.getKey() + ":loaded=" + loaded + ":within=" + within
                    + ":unreachable=" + unreachable + ":supported=" + supported
                    + ":block=" + block + ":above=" + above + ":cachedEmpty=" + empty);
        }
        if (candidates.size() > MAX_CANDIDATES) states.add("truncated=" + (candidates.size() - MAX_CANDIDATES));
        return new AutoDepositDestinationConditions("automatic", states);
    }

    private static void add(TreeMap<String, Candidate> candidates, boolean trusted, BlockPos position) {
        if (position == null) return;
        String key = (trusted ? "trusted:" : "general:") + position.getX() + ','
                + position.getY() + ',' + position.getZ();
        candidates.putIfAbsent(key, new Candidate(trusted, position.toImmutable()));
    }

    private static String blockId(Block block) {
//#if MC >= 11903
        return String.valueOf(Registries.BLOCK.getId(block));
//#else
//$$         return String.valueOf(Registry.BLOCK.getId(block));
//#endif
    }

    private record Candidate(boolean trusted, BlockPos position) { }
}
