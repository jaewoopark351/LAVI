package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Read a cheap trusted-cache fingerprint without selecting a destination.
public final class AutoDepositTrustedDestinationStateReader {
    private static final int MAX_FINGERPRINT_STATES = 64;
    private final AutoDepositTrustedDestinationRepository repository;
    private final int maximumDistance;

    public AutoDepositTrustedDestinationStateReader(
            AutoDepositTrustedDestinationRepository repository,
            int maximumDistance) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.maximumDistance = Math.max(1, maximumDistance);
    }

    public AutoDepositTrustedDestinationState read(AltoClef mod,
                                                    String worldKey,
                                                    Dimension dimension) {
        repository.reloadIfChanged();
        long revision = repository.revision();
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null
                || worldKey == null || dimension == null) {
            return new AutoDepositTrustedDestinationState(revision, "runtime_unavailable");
        }

        List<String> states = new ArrayList<>();
        repository.destinations().stream()
                .filter(AutoDepositTrustedDestination::enabled)
                .filter(destination -> destination.worldKey().equals(worldKey))
                .filter(destination -> destination.dimension() == dimension)
                .sorted((left, right) -> left.key().compareTo(right.key()))
                .forEach(destination -> {
                    Optional<ContainerCache> cache = mod.getItemStorage()
                            .getContainerAtPosition(destination.position());
                    boolean withinDistance = destination.position().isWithinDistance(
                            mod.getPlayer().getPos(), maximumDistance
                    );
                    boolean chunkLoaded = mod.getChunkTracker().isChunkLoaded(destination.position());
                    boolean unreachable = mod.getBlockScanner().isUnreachable(destination.position());
                    states.add(destination.position().toShortString() + "="
                            + "within:" + withinDistance
                            + ",loaded:" + chunkLoaded
                            + ",unreachable:" + unreachable
                            + ","
                            + cache.map(value -> "empty:" + value.getEmptySlotCount())
                            .orElse("uncached"));
                });
        int included = Math.min(MAX_FINGERPRINT_STATES, states.size());
        String capacityState = states.isEmpty()
                ? "no_matching_registration"
                : String.join("|", states.subList(0, included));
        if (included < states.size()) {
            capacityState += "|truncated=" + (states.size() - included);
        }
        return new AutoDepositTrustedDestinationState(revision, capacityState);
    }
}
