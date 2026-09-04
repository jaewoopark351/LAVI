package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;

import java.util.List;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedRegistrySnapshot {
    private final List<AutoDepositTrustedDestination> destinations;
    private final AutoDepositTrustedRegistryProvenance provenance;

    public AutoDepositTrustedRegistrySnapshot(
            List<AutoDepositTrustedDestination> destinations,
            AutoDepositTrustedRegistryProvenance provenance) {
        this.destinations = List.copyOf(Objects.requireNonNull(destinations, "destinations"));
        this.provenance = Objects.requireNonNull(provenance, "provenance");
    }

    public List<AutoDepositTrustedDestination> destinations() {
        return destinations;
    }

    public AutoDepositTrustedRegistryProvenance provenance() {
        return provenance;
    }
}
