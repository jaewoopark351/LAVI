package lavi.minecraft.diagnostics.container.home;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;

//20260828_kpopmodder: Represent only the trusted destination state observed at failure.
public record StoreHomeDestinationSnapshot(
        boolean active,
        String destinationId,
        String position,
        String exactBindingMatched,
        String captureStatus,
        String errorClass) {

    public static StoreHomeDestinationSnapshot capture(
            AutoDepositTrustedDestinationCandidate candidate,
            AutoDepositExactOpenContainerBinding binding) {
        if (candidate == null) {
            return none();
        }
        return capture(candidate.destination(), binding);
    }

    public static StoreHomeDestinationSnapshot capture(
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding) {
        if (destination == null) {
            return none();
        }
        return new StoreHomeDestinationSnapshot(
                true,
                destination.destinationId(),
                destination.position().toShortString(),
                Boolean.toString(binding != null
                        && binding.matches(destination.position())),
                "complete",
                "none"
        );
    }

    public static StoreHomeDestinationSnapshot none() {
        return new StoreHomeDestinationSnapshot(
                false,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                "complete",
                "none"
        );
    }

}
