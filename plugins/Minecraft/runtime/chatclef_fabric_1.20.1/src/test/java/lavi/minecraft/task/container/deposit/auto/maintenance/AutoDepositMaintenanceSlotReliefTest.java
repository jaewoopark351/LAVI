package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import lavi.minecraft.testsupport.auto.AutoDepositPlanFixtureFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260829_kpopmodder: Prove maintenance success from occupied-slot delta instead of requested item count.
class AutoDepositMaintenanceSlotReliefTest {
    @Test
    void classifiesFullPartialAndZeroReliefFromObservedOccupiedSlots() {
        Object previousAltoClef = readStatic(AltoClef.class, "instance");
        try (HeadlessMinecraftClientSession ignored =
                     HeadlessMinecraftClientSession.outOfGame()) {
            AltoClef mod = new AltoClef();
            setStatic(AltoClef.class, "instance", mod);

            assertRelief(28, AutoDepositMaintenanceOutcome.FULL_RELIEF);
            assertRelief(31, AutoDepositMaintenanceOutcome.PARTIAL_RELIEF);
            assertRelief(33, AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF);
        } finally {
            setStatic(AltoClef.class, "instance", previousAltoClef);
        }
    }

    private static void assertRelief(
            int endingOccupiedSlots,
            AutoDepositMaintenanceOutcome expected) {
        Object operationWorld = new Object();
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(
                operationWorld,
                Dimension.OVERWORLD,
                "slot-relief-world",
                endingOccupiedSlots,
                null,
                null,
                List.of()
        );
        AutoDepositPlan plan = AutoDepositPlanFixtureFactory.generalPlan(
                context,
                null,
                33,
                5,
                1
        );
        AutoDepositMaintenanceTask task = new AutoDepositMaintenanceTask(
                plan,
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                ignored -> Optional.of(
                        new DepositAllInventoryPressureSnapshot(endingOccupiedSlots, 36)
                )
        );
        TestObjects.setField(context, AutoDepositContextSnapshot.class, "worldIdentity", null);
        TestObjects.setField(
                task,
                AutoDepositMaintenanceTask.class,
                "phase",
                AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS
        );

        task.tick(new UserTaskChain(new TaskRunner(AltoClef.getInstance())));

        assertEquals(expected, task.outcome());
        assertEquals(AutoDepositMaintenancePhase.DONE, task.phase());
    }

    private static Object readStatic(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to read " + owner.getName() + "." + name, exception);
        }
    }

    private static void setStatic(Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to write " + owner.getName() + "." + name, exception);
        }
    }
}
