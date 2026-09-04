package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.player2api.AICommandBridge;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.composition.DepositAllInventoryPressureChainPreparation;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyLoader;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkContainerKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkLogicalDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.ActiveTaskWorkingSetResolver;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class DepositAllInventoryPressureBulkTrustReevaluationTest {
    @Test
    void bulkRevisionChangeIsObservedOnlyWhenThePressureChainTicksAgain() {
        try (HeadlessMinecraftClientSession ignored =
                     HeadlessMinecraftClientSession.inGame()) {
            HeadlessAltoClef mod = new HeadlessAltoClef();
            TaskRunner runner = new TaskRunner(mod);
            AutoDepositTrustedDestinationRepository repository =
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty();
            AutoDepositPolicyEngine policyEngine = new AutoDepositPolicyEngine(
                    new AutoDepositPolicyLoader().loadOrFailClosed(),
                    repository
            );
            DepositAllInventoryPressureStateMachine stateMachine =
                    new DepositAllInventoryPressureStateMachine();
            stateMachine.observe(new DepositAllInventoryPressureSnapshot(33, 36));
            stateMachine.markThresholdSuppressed();
            long initialRevision = repository.revision();
            DepositAllInventoryPressureChain chain = DepositAllInventoryPressureChain.commit(
                    new DepositAllInventoryPressureChainPreparation(
                            runner,
                            mod,
                            ignoredMod -> Optional.of(
                                    new DepositAllInventoryPressureSnapshot(30, 36)
                            ),
                            stateMachine,
                            new DepositAllAutoConflictGuard(),
                            new ActiveTaskWorkingSetResolver(),
                            policyEngine,
                            repository,
                            AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                            initialRevision
                    )
            );

            AutoDepositTrustedBulkMutationResult mutation = repository.registerBulk(
                    "singleplayer:h5-lifecycle-test",
                    Dimension.OVERWORLD,
                    List.of(AutoDepositBulkLogicalDestination.single(
                            AutoDepositBulkContainerKind.BARREL,
                            new BlockPos(0, 64, 0)
                    ))
            );

            assertEquals(AutoDepositTrustedBulkMutationStatus.UPDATED, mutation.status());
            assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, stateMachine.state());

            chain.onEndClientTick();

            assertEquals(DepositAllInventoryPressureState.ARMED, stateMachine.state());
        }
    }

    private static final class HeadlessAltoClef extends AltoClef {
        private final AICommandBridge bridge = new AICommandBridge(null, this);

        @Override
        public AICommandBridge getAiBridge() {
            return bridge;
        }
    }
}
