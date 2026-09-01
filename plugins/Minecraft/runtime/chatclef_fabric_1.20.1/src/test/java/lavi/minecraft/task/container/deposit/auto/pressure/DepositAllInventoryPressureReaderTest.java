package lavi.minecraft.task.container.deposit.auto.pressure;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureReader;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.composition.DepositAllInventoryPressureChainPreparation;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import net.minecraft.client.network.ClientPlayerEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Verify automatic-deposit pressure from the real main-inventory read boundary.
class DepositAllInventoryPressureReaderTest {

    @Test
    void defaultCompositionWiresTheRealInventoryPressureReader() {
        TestAltoClef mod = new TestAltoClef(null);
        DepositAllInventoryPressureChainPreparation preparation =
                DepositAllInventoryPressureChainPreparation.prepare(
                        new TaskRunner(mod),
                        AutoDepositPolicyEngine.inMemoryDefault()
                );

        assertInstanceOf(
                DepositAllInventoryPressureReader.class,
                preparation.pressureSource()
        );
    }

    @Test
    void returnsNoSnapshotWhenThePlayerIsUnavailable() {
        Optional<DepositAllInventoryPressureSnapshot> snapshot =
                new DepositAllInventoryPressureReader().read(new TestAltoClef(null));

        assertTrue(snapshot.isEmpty());
    }

    private static final class TestAltoClef extends AltoClef {
        private final ClientPlayerEntity player;

        private TestAltoClef(ClientPlayerEntity player) {
            this.player = player;
        }

        @Override
        public ClientPlayerEntity getPlayer() {
            return player;
        }
    }
}
