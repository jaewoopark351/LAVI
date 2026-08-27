package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Snapshot every same-world enabled trusted destination with no distance cap.
public final class HomeStorageDestinationSelector {
    private final AutoDepositTrustedDestinationRepository repository;

    public HomeStorageDestinationSelector(AutoDepositTrustedDestinationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<AutoDepositTrustedDestinationCandidate> snapshot(
            AltoClef mod,
            HomeStorageOperationContext context) {
        if (mod == null || mod.getPlayer() == null) {
            return List.of();
        }
        repository.reloadIfChanged();
        return snapshot(
                repository.destinations(),
                context.worldKey(),
                context.dimension(),
                mod.getPlayer().getX(),
                mod.getPlayer().getY(),
                mod.getPlayer().getZ()
        );
    }

    public List<AutoDepositTrustedDestinationCandidate> snapshot(
            List<AutoDepositTrustedDestination> destinations,
            String worldKey,
            Dimension dimension,
            double playerX,
            double playerY,
            double playerZ) {
        return destinations.stream()
                .filter(AutoDepositTrustedDestination::enabled)
                .filter(destination -> destination.worldKey().equals(worldKey))
                .filter(destination -> destination.dimension() == dimension)
                .map(destination -> new AutoDepositTrustedDestinationCandidate(
                        destination,
                        0,
                        distanceSquared(destination, playerX, playerY, playerZ),
                        "manual_home_snapshot"
                ))
                .sorted(Comparator
                        .comparingDouble(AutoDepositTrustedDestinationCandidate::distanceSquared)
                .thenComparing(AutoDepositTrustedDestinationCandidate::destinationId))
                .toList();
    }

    private static double distanceSquared(
            AutoDepositTrustedDestination destination,
            double playerX,
            double playerY,
            double playerZ) {
        double x = destination.position().getX() + 0.5 - playerX;
        double y = destination.position().getY() + 0.5 - playerY;
        double z = destination.position().getZ() + 0.5 - playerZ;
        return x * x + y * y + z * z;
    }
}
