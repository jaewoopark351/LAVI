package lavi.minecraft.task.container.deposit.auto.trusted.command;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStatus;

//20260827_kpopmodder: Keep trusted command output stable and explicit about world and position.
public final class AutoDepositTrustedCommandFormatter {
    private AutoDepositTrustedCommandFormatter() {
    }

    public static String mutation(AutoDepositTrustedDestinationMutationResult result) {
        String target = result.destination()
                .map(AutoDepositTrustedCommandFormatter::identity)
                .orElse("destination=unknown");
        return "Trusted destination " + result.status().name()
                + ": " + target + ", detail=" + result.detail();
    }

    public static String listing(
            AutoDepositTrustedDestination destination,
            AutoDepositTrustedDestinationStatus status) {
        return identity(destination)
                + ", enabled=" + destination.enabled()
                + ", status=" + status;
    }

    public static String identity(AutoDepositTrustedDestination destination) {
        return "id=" + destination.destinationId()
                + ", world=" + destination.worldKey()
                + ", dimension=" + destination.dimension()
                + ", pos=" + destination.position().toShortString();
    }
}
