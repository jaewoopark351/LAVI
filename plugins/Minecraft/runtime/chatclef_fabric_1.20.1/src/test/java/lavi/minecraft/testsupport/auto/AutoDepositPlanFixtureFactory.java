package lavi.minecraft.testsupport.auto;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositDecisionFingerprint;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositDisposition;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import net.minecraft.item.Item;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

//20260829_kpopmodder: Build immutable automatic-plan fixtures without exposing production constructors.
public final class AutoDepositPlanFixtureFactory {
    private AutoDepositPlanFixtureFactory() {
    }

    public static AutoDepositPlan generalPlan(
            AutoDepositContextSnapshot context,
            Task userTaskRoot,
            int startingOccupiedSlots,
            int targetReliefSlots,
            int expectedFreedSlots) {
        try {
            Constructor<AutoDepositPlan> constructor = AutoDepositPlan.class.getDeclaredConstructor(
                    AutoDepositContextSnapshot.class,
                    ItemTarget[].class,
                    ItemTarget[].class,
                    List.class,
                    Map.class,
                    Map.class,
                    List.class,
                    String.class,
                    long.class,
                    String.class,
                    int.class,
                    int.class,
                    int.class,
                    AutoDepositDecisionFingerprint.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    context,
                    new ItemTarget[]{new ItemTarget(new Item[0], 32)},
                    new ItemTarget[0],
                    List.<AutoDepositTrustedDestinationCandidate>of(),
                    Map.<Item, Integer>of(),
                    Map.<Item, AutoDepositDisposition>of(),
                    List.of(),
                    "EMPTY",
                    0L,
                    "not_required",
                    startingOccupiedSlots,
                    targetReliefSlots,
                    expectedFreedSlots,
                    new AutoDepositDecisionFingerprint(
                            context.worldIdentity(),
                            userTaskRoot,
                            context.dimension(),
                            context.persistentWorldKey(),
                            1,
                            0L,
                            "not_required",
                            List.of("test-fixture")
                    )
            );
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create immutable automatic plan fixture", exception);
        }
    }
}
