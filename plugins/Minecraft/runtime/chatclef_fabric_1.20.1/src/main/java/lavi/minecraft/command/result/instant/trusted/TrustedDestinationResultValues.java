package lavi.minecraft.command.result.instant.trusted;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStatus;
import java.util.Map;
import java.util.Locale;

//20260915_kpopmodder: Project bounded list fields without disclosing the repository's world/save identity.
public final class TrustedDestinationResultValues {
    private TrustedDestinationResultValues() { }
    public static Map<String, Object> entry(AutoDepositTrustedDestination destination,
                                             AutoDepositTrustedDestinationStatus status) {
        return Map.of("destination_id", destination.destinationId(),
                "dimension", destination.dimension().name().toLowerCase(Locale.ROOT),
                "x", destination.position().getX(), "y", destination.position().getY(), "z", destination.position().getZ(),
                "status", status.name());
    }
}
