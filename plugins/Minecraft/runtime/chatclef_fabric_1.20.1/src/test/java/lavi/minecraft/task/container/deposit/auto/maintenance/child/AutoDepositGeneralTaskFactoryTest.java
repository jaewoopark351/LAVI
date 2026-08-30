package lavi.minecraft.task.container.deposit.auto.maintenance.child;

import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPostPlaceHandoff;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositGeneralTaskFactoryTest {
    @Test
    void createsOneAutomaticTaskPerTargetInSourceOrderWithIndependentHandoffState() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            ItemTarget firstTarget = target(1);
            ItemTarget secondTarget = target(2);

            List<DepositAllTask> tasks = new AutoDepositGeneralTaskFactory().create(
                    new ItemTarget[]{firstTarget, secondTarget}
            );

            assertEquals(2, tasks.size());
            assertSame(firstTarget, targets(tasks.get(0))[0]);
            assertSame(secondTarget, targets(tasks.get(1))[0]);

            DepositAllPlacementTaskOwner firstOwner = placementOwner(tasks.get(0));
            DepositAllPlacementTaskOwner secondOwner = placementOwner(tasks.get(1));
            DepositAllPostPlaceHandoff firstHandoff = postPlaceHandoff(tasks.get(0));
            DepositAllPostPlaceHandoff secondHandoff = postPlaceHandoff(tasks.get(1));

            assertTrue(firstOwner.retainingIdentity());
            assertTrue(secondOwner.retainingIdentity());
            assertTrue(firstHandoff.enabled());
            assertTrue(secondHandoff.enabled());
            assertNotSame(firstOwner, secondOwner);
            assertNotSame(firstHandoff, secondHandoff);
        }
    }

    @Test
    void manualConstructorKeepsEphemeralPlacementAndDisabledHandoffDefaults() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            ItemTarget target = target(1);
            DepositAllTask manual = new DepositAllTask(false, target);

            assertSame(target, targets(manual)[0]);
            assertFalse(placementOwner(manual).retainingIdentity());
            assertFalse(postPlaceHandoff(manual).enabled());
        }
    }

    @Test
    void rejectsMismatchedPlacementOwnershipAndHandoffPolicies() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            ItemTarget target = target(1);

            assertThrows(IllegalArgumentException.class, () -> new DepositAllTask(
                    false,
                    DepositAllPlacementTaskOwner.ephemeral(),
                    DepositAllPostPlaceHandoff.singleTick(),
                    target
            ));
            assertThrows(IllegalArgumentException.class, () -> new DepositAllTask(
                    false,
                    DepositAllPlacementTaskOwner.retaining(),
                    DepositAllPostPlaceHandoff.disabled(),
                    target
            ));
        }
    }

    private static ItemTarget target(int count) {
        return new ItemTarget(new Item[0], count);
    }

    private static ItemTarget[] targets(DepositAllTask task) {
        return field(task, "_toStore", ItemTarget[].class);
    }

    private static DepositAllPlacementTaskOwner placementOwner(DepositAllTask task) {
        return field(task, "_placementTaskOwner", DepositAllPlacementTaskOwner.class);
    }

    private static DepositAllPostPlaceHandoff postPlaceHandoff(DepositAllTask task) {
        return field(task, "_postPlaceHandoff", DepositAllPostPlaceHandoff.class);
    }

    private static <T> T field(DepositAllTask task, String fieldName, Class<T> fieldType) {
        try {
            Field field = DepositAllTask.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(task));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect DepositAllTask." + fieldName, exception);
        }
    }
}
