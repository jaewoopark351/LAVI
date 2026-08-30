package lavi.minecraft.task.container.deposit.auto.composition;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.lifecycle.AutoDepositRuntimeTickSequence;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedCommandRegistrar;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;
import lavi.minecraft.task.container.home.command.StoreHomeCommandRegistrar;
import lavi.minecraft.task.container.home.command.StoreHomeTaskFactory;

import java.util.Objects;

//20260829_kpopmodder: Validate and assemble one shared automatic-storage runtime graph before activation.
public record AutoDepositRuntimePreparation(
        AltoClef mod,
        AutoDepositTrustedDestinationRepository trustedRepository,
        AutoDepositPolicyEngine policyEngine,
        AutoDepositOpenContainerBindingTracker openContainerBindingTracker,
        AutoDepositTrustedCommandRegistrar trustedCommandRegistrar,
        StoreHomeTaskFactory storeHomeTaskFactory,
        StoreHomeCommandRegistrar storeHomeCommandRegistrar,
        AutoDepositRuntimeTickSequence tickSequence,
        DepositAllInventoryPressureChainPreparation pressureChainPreparation) {

    public static AutoDepositRuntimePreparation prepare(
            AltoClef mod,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositPolicyEngine policyEngine) {
        AltoClef checkedMod = Objects.requireNonNull(mod, "mod");
        TaskRunner runner = Objects.requireNonNull(checkedMod.getTaskRunner(), "taskRunner");
        if (runner.getMod() != checkedMod) {
            throw new IllegalArgumentException(
                    "automatic runtime and TaskRunner must share one AltoClef instance"
            );
        }
        AutoDepositTrustedDestinationRepository checkedRepository = Objects.requireNonNull(
                trustedRepository,
                "trustedRepository"
        );
        AutoDepositPolicyEngine checkedEngine = Objects.requireNonNull(
                policyEngine,
                "policyEngine"
        );
        if (checkedEngine.trustedRepository() != checkedRepository) {
            throw new IllegalArgumentException(
                    "automatic runtime and policy must share one trusted repository"
            );
        }

        AutoDepositOpenContainerBindingTracker bindingTracker =
                new AutoDepositOpenContainerBindingTracker(checkedMod);
        AutoDepositTrustedCommandRegistrar trustedRegistrar =
                new AutoDepositTrustedCommandRegistrar(checkedRepository, bindingTracker);
        StoreHomeTaskFactory storeHomeFactory = new StoreHomeTaskFactory(
                checkedRepository,
                bindingTracker
        );

        return new AutoDepositRuntimePreparation(
                checkedMod,
                checkedRepository,
                checkedEngine,
                bindingTracker,
                trustedRegistrar,
                storeHomeFactory,
                new StoreHomeCommandRegistrar(storeHomeFactory),
                new AutoDepositRuntimeTickSequence(bindingTracker),
                DepositAllInventoryPressureChainPreparation.prepare(
                        runner,
                        checkedEngine,
                        checkedRepository,
                        bindingTracker
                )
        );
    }

    public AutoDepositRuntimePreparation {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(trustedRepository, "trustedRepository");
        Objects.requireNonNull(policyEngine, "policyEngine");
        Objects.requireNonNull(openContainerBindingTracker, "openContainerBindingTracker");
        Objects.requireNonNull(trustedCommandRegistrar, "trustedCommandRegistrar");
        Objects.requireNonNull(storeHomeTaskFactory, "storeHomeTaskFactory");
        Objects.requireNonNull(storeHomeCommandRegistrar, "storeHomeCommandRegistrar");
        Objects.requireNonNull(tickSequence, "tickSequence");
        Objects.requireNonNull(pressureChainPreparation, "pressureChainPreparation");
    }
}
