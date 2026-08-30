package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.composition.DepositAllInventoryPressureChainPreparation;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyLoader;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260829_kpopmodder: Prove invalid pressure-chain composition fails before TaskChain self-registration.
class DepositAllInventoryPressureChainConstructionTest {
    @Test
    void repositoryMismatchAndNullBindingLeaveNoOrphanBeforeSuccessfulRetry() {
        AltoClef mod = new AltoClef();
        TaskRunner runner = new TaskRunner(mod);
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        AutoDepositTrustedDestinationRepository otherRepository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        AutoDepositPolicyEngine engine = new AutoDepositPolicyEngine(
                new AutoDepositPolicyLoader().loadOrFailClosed(),
                repository
        );

        assertThrows(IllegalArgumentException.class, () ->
                new DepositAllInventoryPressureChain(
                        runner,
                        engine,
                        otherRepository,
                        AutoDepositExactOpenContainerBinding.UNAVAILABLE
                ));
        assertEquals(0, pressureChains(runner).size());

        assertThrows(NullPointerException.class, () ->
                new DepositAllInventoryPressureChain(runner, engine, repository, null));
        assertEquals(0, pressureChains(runner).size());

        DepositAllInventoryPressureChainPreparation preparation =
                DepositAllInventoryPressureChainPreparation.prepare(
                        runner,
                        engine,
                        repository,
                        AutoDepositExactOpenContainerBinding.UNAVAILABLE
                );
        assertEquals(0, pressureChains(runner).size());

        DepositAllInventoryPressureChain chain =
                DepositAllInventoryPressureChain.commit(preparation);

        assertEquals(1, pressureChains(runner).size());
        assertSame(chain, pressureChains(runner).get(0));
    }

    private static List<DepositAllInventoryPressureChain> pressureChains(TaskRunner runner) {
        try {
            Field field = TaskRunner.class.getDeclaredField("chains");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<TaskChain> chains = (List<TaskChain>) field.get(runner);
            return chains.stream()
                    .filter(DepositAllInventoryPressureChain.class::isInstance)
                    .map(DepositAllInventoryPressureChain.class::cast)
                    .toList();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect registered pressure chains", exception);
        }
    }
}
