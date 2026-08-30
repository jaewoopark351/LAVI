package lavi.minecraft.task.container.deposit.auto.maintenance.child;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreTask;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;

import java.util.Objects;
import java.util.Optional;

//20260831_kpopmodder: Isolate trusted automatic-deposit child construction from general routing.
/** Creates the optional trusted-destination child for one immutable plan. */
public final class AutoDepositTrustedTaskFactory {
    private final AutoDepositTrustedDestinationRepository trustedRepository;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;

    public AutoDepositTrustedTaskFactory(
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this.trustedRepository = Objects.requireNonNull(trustedRepository, "trustedRepository");
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding,
                "exactOpenContainerBinding"
        );
    }

    public Optional<AutoDepositTrustedStoreTask> create(AutoDepositPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan.trustedTargets().length == 0 || plan.trustedCandidates().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AutoDepositTrustedStoreTask(
                plan.context(),
                trustedRepository,
                plan.trustedCandidates(),
                exactOpenContainerBinding,
                plan.trustedTargets()
        ));
    }
}
