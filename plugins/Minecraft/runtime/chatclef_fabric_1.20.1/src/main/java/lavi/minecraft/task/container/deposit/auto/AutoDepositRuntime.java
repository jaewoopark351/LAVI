package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.home.command.StoreHomeCommandRegistrar;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedCommandRegistrar;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;

import java.util.Objects;

//20260827_kpopmodder: Own one injected trusted repository across policy, commands, and execution.
//20260827_kpopmodder: Keep the request-only runtime independent from the disabled automatic policy engine.
public final class AutoDepositRuntime {
    private final AltoClef mod;
    private final AutoDepositTrustedDestinationRepository trustedRepository;
    private final AutoDepositOpenContainerBindingTracker openContainerBindingTracker;
    private final AutoDepositTrustedCommandRegistrar trustedCommandRegistrar;
    private final StoreHomeTaskFactory storeHomeTaskFactory;
    private final StoreHomeCommandRegistrar storeHomeCommandRegistrar;

    private AutoDepositRuntime(
            AltoClef mod,
            AutoDepositTrustedDestinationRepository trustedRepository) {
        this.mod = Objects.requireNonNull(mod, "mod");
        this.trustedRepository = Objects.requireNonNull(trustedRepository, "trustedRepository");
        openContainerBindingTracker = new AutoDepositOpenContainerBindingTracker(this.mod);
        trustedCommandRegistrar = new AutoDepositTrustedCommandRegistrar(
                trustedRepository,
                openContainerBindingTracker
        );
        storeHomeTaskFactory = new StoreHomeTaskFactory(
                trustedRepository,
                openContainerBindingTracker
        );
        storeHomeCommandRegistrar = new StoreHomeCommandRegistrar(storeHomeTaskFactory);
    }

    public static AutoDepositRuntime create(
            AltoClef mod,
            AutoDepositTrustedDestinationRepository trustedRepository) {
        return new AutoDepositRuntime(mod, trustedRepository);
    }

    public void registerCommands(AltoClef mod) {
        trustedCommandRegistrar.register(mod);
        storeHomeCommandRegistrar.register(mod);
        openContainerBindingTracker.start();
    }

    public void onEndClientTick() {
        trustedCommandRegistrar.register(mod);
        storeHomeCommandRegistrar.register(mod);
        openContainerBindingTracker.start();
        openContainerBindingTracker.onEndClientTick();
    }

    public AutoDepositTrustedDestinationRepository trustedRepository() {
        return trustedRepository;
    }

    public AutoDepositTrustedCommandRegistrar trustedCommandRegistrar() {
        return trustedCommandRegistrar;
    }

    public AutoDepositOpenContainerBindingTracker openContainerBindingTracker() {
        return openContainerBindingTracker;
    }

    public StoreHomeTaskFactory storeHomeTaskFactory() {
        return storeHomeTaskFactory;
    }

    public StoreHomeCommandRegistrar storeHomeCommandRegistrar() {
        return storeHomeCommandRegistrar;
    }
}
