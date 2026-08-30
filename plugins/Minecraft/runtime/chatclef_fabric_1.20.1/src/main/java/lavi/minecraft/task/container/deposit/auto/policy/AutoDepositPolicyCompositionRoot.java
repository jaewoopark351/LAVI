package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.AutoDepositRuntime;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStore;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class AutoDepositPolicyCompositionRoot {
    private static final String TRUSTED_DESTINATIONS_FILE =
            "lavi/automatic-deposit-trusted-destinations.json";

    public AutoDepositPolicyEngine createEngine() {
        AutoDepositTrustedDestinationRepository repository = createRepository();
        return createEngine(repository);
    }

    //20260829_kpopmodder: Compose one repository across trusted commands, StoreHome, policy, and automatic execution.
    public AutoDepositRuntime createRuntime(AltoClef mod) {
        AutoDepositTrustedDestinationRepository repository = createRepository();
        AutoDepositPolicyEngine engine = createEngine(repository);
        return AutoDepositRuntime.create(mod, repository, engine);
    }

    private AutoDepositPolicyEngine createEngine(
            AutoDepositTrustedDestinationRepository repository) {
        AutoDepositPolicyDefinition definition = new AutoDepositPolicyLoader().loadOrFailClosed();
        return new AutoDepositPolicyEngine(definition, repository);
    }

    private AutoDepositTrustedDestinationRepository createRepository() {
        Path destinationPath = FabricLoader.getInstance().getConfigDir().resolve(TRUSTED_DESTINATIONS_FILE);
        return new AutoDepositTrustedDestinationRepository(
                new AutoDepositTrustedDestinationStore(destinationPath)
        );
    }
}
