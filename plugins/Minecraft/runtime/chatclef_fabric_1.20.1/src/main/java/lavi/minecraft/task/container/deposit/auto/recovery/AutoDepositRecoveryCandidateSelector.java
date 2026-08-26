package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.trackers.storage.ContainerType;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AutoDepositRecoveryCandidateSelector {
    private static final int MAX_CANDIDATES = 64;
    private final AutoDepositRecoveryCandidatePolicy policy = new AutoDepositRecoveryCandidatePolicy();

    public List<AutoDepositRecoveryCandidate> select(AltoClef mod,
                                                     WorkingSetSnapshot snapshot,
                                                     Map<Item, Integer> deficits,
                                                     AutoDepositDestinationManifest manifest) {
        Map<BlockPos, AutoDepositRecoveryCandidate> ordered = new LinkedHashMap<>();
        addManifestCandidates(mod, deficits, manifest, ordered);
        addCacheCandidates(mod, snapshot, deficits, ordered);
        addScannerCandidates(mod, deficits, ordered);
        return List.copyOf(ordered.values());
    }

    private void addManifestCandidates(AltoClef mod,
                                       Map<Item, Integer> deficits,
                                       AutoDepositDestinationManifest manifest,
                                       Map<BlockPos, AutoDepositRecoveryCandidate> result) {
        List<BlockPos> positions = new ArrayList<>(manifest.positions());
        positions.sort(byDistance(mod));
        for (BlockPos position : positions) {
            if (result.size() >= MAX_CANDIDATES) {
                return;
            }
            Map<Item, Integer> limits = boundedManifestLimits(manifest, position, deficits);
            if (!limits.isEmpty() && policy.accepts(mod, position)) {
                result.putIfAbsent(position, new AutoDepositRecoveryCandidate(
                        position,
                        AutoDepositRecoveryCandidate.Tier.CONFIRMED_DESTINATION,
                        limits
                ));
            }
        }
    }

    private void addCacheCandidates(AltoClef mod,
                                    WorkingSetSnapshot snapshot,
                                    Map<Item, Integer> deficits,
                                    Map<BlockPos, AutoDepositRecoveryCandidate> result) {
        List<ContainerCache> caches = new ArrayList<>(mod.getItemStorage().getCachedContainers(cache ->
                cache.getDimension() == snapshot.dimension()
                        && isSupportedCache(mod, cache)
                        && deficits.keySet().stream().anyMatch(item -> cache.getItemCount(item) > 0)));
        caches.sort(Comparator.comparingDouble(cache -> distanceSquared(mod, cache.getBlockPos())));
        for (ContainerCache cache : caches) {
            if (result.size() >= MAX_CANDIDATES) {
                return;
            }
            BlockPos position = cache.getBlockPos();
            if (policy.accepts(mod, position)) {
                result.putIfAbsent(position, new AutoDepositRecoveryCandidate(
                        position,
                        AutoDepositRecoveryCandidate.Tier.CURRENT_DIMENSION_CACHE,
                        deficits
                ));
            }
        }
    }

    private void addScannerCandidates(AltoClef mod,
                                      Map<Item, Integer> deficits,
                                      Map<BlockPos, AutoDepositRecoveryCandidate> result) {
        List<BlockPos> positions = new ArrayList<>(
                mod.getBlockScanner().getKnownLocations(StoreInContainerTask.CONTAINER_BLOCKS)
        );
        positions.sort(byDistance(mod));
        for (BlockPos position : positions) {
            if (result.size() >= MAX_CANDIDATES) {
                return;
            }
            if (policy.accepts(mod, position)) {
                result.putIfAbsent(position, new AutoDepositRecoveryCandidate(
                        position,
                        AutoDepositRecoveryCandidate.Tier.NEARBY_SCANNER,
                        deficits
                ));
            }
        }
    }

    private static Map<Item, Integer> boundedManifestLimits(AutoDepositDestinationManifest manifest,
                                                            BlockPos position,
                                                            Map<Item, Integer> deficits) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        deficits.forEach((item, deficit) -> {
            int confirmed = manifest.confirmedCount(position, item);
            int allowed = Math.min(deficit, confirmed);
            if (allowed > 0) {
                result.put(item, allowed);
            }
        });
        return result;
    }

    private static boolean isSupportedCache(AltoClef mod, ContainerCache cache) {
        ContainerType type = cache.getContainerType();
        if (type == ContainerType.CHEST || type == ContainerType.SHULKER) {
            return true;
        }
        if (type != ContainerType.MISC || !mod.getChunkTracker().isChunkLoaded(cache.getBlockPos())) {
            return false;
        }
        return Arrays.stream(StoreInContainerTask.CONTAINER_BLOCKS)
                .anyMatch(block -> block == mod.getWorld().getBlockState(cache.getBlockPos()).getBlock());
    }

    private static Comparator<BlockPos> byDistance(AltoClef mod) {
        return Comparator.comparingDouble(position -> distanceSquared(mod, position));
    }

    private static double distanceSquared(AltoClef mod, BlockPos position) {
        return BlockPosVer.getSquaredDistance(position, mod.getPlayer().getPos());
    }
}
