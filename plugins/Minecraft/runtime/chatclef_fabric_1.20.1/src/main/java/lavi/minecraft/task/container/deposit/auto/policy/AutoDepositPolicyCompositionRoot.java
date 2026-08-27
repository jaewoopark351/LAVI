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

    //20260827_kpopmodder: Compose one repository instance across trusted commands, policy, and execution.
    //20260827_kpopmodder: Do not load the disabled automatic policy for the request-only runtime.
    public AutoDepositRuntime createRuntime(AltoClef mod) {
        AutoDepositTrustedDestinationRepository repository = createRepository();
        return AutoDepositRuntime.create(mod, repository);
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
