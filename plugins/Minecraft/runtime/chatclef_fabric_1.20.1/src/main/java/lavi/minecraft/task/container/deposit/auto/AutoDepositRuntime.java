package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.composition.AutoDepositRuntimePreparation;
import lavi.minecraft.task.container.deposit.auto.lifecycle.AutoDepositRuntimeTickSequence;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.home.command.StoreHomeCommandRegistrar;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedCommandRegistrar;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;

import java.util.Objects;

//20260827_kpopmodder: Own one injected trusted repository across policy, commands, and execution.
//20260829_kpopmodder: Restore one validated automatic pressure chain behind the shared runtime lifecycle.
public final class AutoDepositRuntime {
    private final AltoClef mod;
    private final AutoDepositTrustedDestinationRepository trustedRepository;
    private final AutoDepositPolicyEngine policyEngine;
    private final AutoDepositOpenContainerBindingTracker openContainerBindingTracker;
    private final AutoDepositTrustedCommandRegistrar trustedCommandRegistrar;
    private final StoreHomeTaskFactory storeHomeTaskFactory;
    private final StoreHomeCommandRegistrar storeHomeCommandRegistrar;
    private final AutoDepositRuntimeTickSequence tickSequence;
    private final DepositAllInventoryPressureChain pressureChain;

    private AutoDepositRuntime(AutoDepositRuntimePreparation preparation) {
        AutoDepositRuntimePreparation checked = Objects.requireNonNull(
                preparation,
                "preparation"
        );
        mod = checked.mod();
        trustedRepository = checked.trustedRepository();
        policyEngine = checked.policyEngine();
        openContainerBindingTracker = checked.openContainerBindingTracker();
        trustedCommandRegistrar = checked.trustedCommandRegistrar();
        storeHomeTaskFactory = checked.storeHomeTaskFactory();
        storeHomeCommandRegistrar = checked.storeHomeCommandRegistrar();
        tickSequence = checked.tickSequence();
        pressureChain = DepositAllInventoryPressureChain.commit(
                checked.pressureChainPreparation()
        );
    }

    public static AutoDepositRuntime create(
            AltoClef mod,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositPolicyEngine policyEngine) {
        return new AutoDepositRuntime(
                AutoDepositRuntimePreparation.prepare(
                        mod,
                        trustedRepository,
                        policyEngine
                )
        );
    }

    public void onEndClientTick() {
        trustedCommandRegistrar.register(mod);
        storeHomeCommandRegistrar.register(mod);
        openContainerBindingTracker.start();
        tickSequence.onEndClientTick(pressureChain);
    }

    public AutoDepositTrustedDestinationRepository trustedRepository() {
        return trustedRepository;
    }

    public AutoDepositTrustedCommandRegistrar trustedCommandRegistrar() {
        return trustedCommandRegistrar;
    }

    public AutoDepositPolicyEngine policyEngine() {
        return policyEngine;
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

    public DepositAllInventoryPressureChain pressureChain() {
        return pressureChain;
    }
}
