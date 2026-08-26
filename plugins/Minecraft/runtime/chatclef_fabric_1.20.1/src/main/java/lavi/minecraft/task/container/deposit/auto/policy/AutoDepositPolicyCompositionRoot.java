package lavi.minecraft.task.container.deposit.auto.policy;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStore;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class AutoDepositPolicyCompositionRoot {
    private static final String TRUSTED_DESTINATIONS_FILE =
            "lavi/automatic-deposit-trusted-destinations.json";

    public AutoDepositPolicyEngine createEngine() {
        AutoDepositPolicyDefinition definition = new AutoDepositPolicyLoader().loadOrFailClosed();
        Path destinationPath = FabricLoader.getInstance().getConfigDir().resolve(TRUSTED_DESTINATIONS_FILE);
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(
                        new AutoDepositTrustedDestinationStore(destinationPath)
                );
        return new AutoDepositPolicyEngine(definition, repository);
    }
}
