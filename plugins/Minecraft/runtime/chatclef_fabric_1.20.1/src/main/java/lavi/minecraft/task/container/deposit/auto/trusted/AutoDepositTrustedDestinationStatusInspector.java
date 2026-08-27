package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.Dimension;

import java.util.Optional;

//20260827_kpopmodder: Expose conservative trusted-container status without treating cache as live proof.
public final class AutoDepositTrustedDestinationStatusInspector {
    public AutoDepositTrustedDestinationStatus inspect(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            String currentWorldKey,
            Dimension currentDimension) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null
                || currentWorldKey == null || currentDimension == null
                || !destination.worldKey().equals(currentWorldKey)
                || destination.dimension() != currentDimension) {
            return AutoDepositTrustedDestinationStatus.UNKNOWN_OR_STALE;
        }
        if (mod.getBlockScanner().isUnreachable(destination.position())) {
            return AutoDepositTrustedDestinationStatus.KNOWN_UNREACHABLE;
        }
        if (mod.getChunkTracker().isChunkLoaded(destination.position())
                && !AutoDepositTrustedContainerSupport.isSupported(
                mod.getWorld().getBlockState(destination.position()).getBlock())) {
            return AutoDepositTrustedDestinationStatus.MISSING;
        }
        Optional<ContainerCache> cache = mod.getItemStorage()
                .getContainerAtPosition(destination.position());
        if (cache.isEmpty()) {
            return AutoDepositTrustedDestinationStatus.UNKNOWN_OR_STALE;
        }
        return cache.get().isFull()
                ? AutoDepositTrustedDestinationStatus.KNOWN_FULL
                : AutoDepositTrustedDestinationStatus.KNOWN_AVAILABLE;
    }
}
