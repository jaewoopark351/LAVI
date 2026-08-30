package lavi.minecraft.task.container.deposit.auto.composition;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoConflictGuard;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureReader;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureStateMachine;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.ActiveTaskWorkingSetResolver;

import java.util.Objects;

//20260829_kpopmodder: Prepare every automatic pressure-chain dependency before TaskChain self-registration.
public record DepositAllInventoryPressureChainPreparation(
        TaskRunner runner,
        AltoClef mod,
        AutoDepositInventoryPressureSource pressureSource,
        DepositAllInventoryPressureStateMachine stateMachine,
        DepositAllAutoConflictGuard conflictGuard,
        ActiveTaskWorkingSetResolver workingSetResolver,
        AutoDepositPolicyEngine policyEngine,
        AutoDepositTrustedDestinationRepository trustedRepository,
        AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
        long initialTrustedRevision) {

    public static DepositAllInventoryPressureChainPreparation prepare(
            TaskRunner runner,
            AutoDepositPolicyEngine policyEngine) {
        AutoDepositPolicyEngine checkedEngine = Objects.requireNonNull(
                policyEngine,
                "policyEngine"
        );
        return prepare(
                runner,
                checkedEngine,
                checkedEngine.trustedRepository(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        );
    }

    public static DepositAllInventoryPressureChainPreparation prepare(
            TaskRunner runner,
            AutoDepositPolicyEngine policyEngine,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        TaskRunner checkedRunner = Objects.requireNonNull(runner, "runner");
        AltoClef checkedMod = Objects.requireNonNull(checkedRunner.getMod(), "mod");
        AutoDepositPolicyEngine checkedEngine = Objects.requireNonNull(
                policyEngine,
                "policyEngine"
        );
        AutoDepositTrustedDestinationRepository checkedRepository = Objects.requireNonNull(
                trustedRepository,
                "trustedRepository"
        );
        if (checkedEngine.trustedRepository() != checkedRepository) {
            throw new IllegalArgumentException(
                    "automatic policy and execution must share one trusted repository"
            );
        }
        AutoDepositExactOpenContainerBinding checkedBinding = Objects.requireNonNull(
                exactOpenContainerBinding,
                "exactOpenContainerBinding"
        );

        return new DepositAllInventoryPressureChainPreparation(
                checkedRunner,
                checkedMod,
                new DepositAllInventoryPressureReader(),
                new DepositAllInventoryPressureStateMachine(),
                new DepositAllAutoConflictGuard(),
                new ActiveTaskWorkingSetResolver(),
                checkedEngine,
                checkedRepository,
                checkedBinding,
                checkedRepository.revision()
        );
    }

    public DepositAllInventoryPressureChainPreparation {
        Objects.requireNonNull(runner, "runner");
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(pressureSource, "pressureSource");
        Objects.requireNonNull(stateMachine, "stateMachine");
        Objects.requireNonNull(conflictGuard, "conflictGuard");
        Objects.requireNonNull(workingSetResolver, "workingSetResolver");
        Objects.requireNonNull(policyEngine, "policyEngine");
        Objects.requireNonNull(trustedRepository, "trustedRepository");
        Objects.requireNonNull(exactOpenContainerBinding, "exactOpenContainerBinding");
        if (runner.getMod() != mod) {
            throw new IllegalArgumentException(
                    "automatic pressure preparation and TaskRunner must share one AltoClef instance"
            );
        }
        if (policyEngine.trustedRepository() != trustedRepository) {
            throw new IllegalArgumentException(
                    "automatic policy and execution must share one trusted repository"
            );
        }
    }
}
