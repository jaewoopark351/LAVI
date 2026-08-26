package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenancePhase;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.TestItems;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositPlanBuilderTest {
    @Test
    void excludesAnEntireItemTypeWhenProtectedAndPlainStacksAreMixed() {
        Item chestplate = TestItems.item();
        AutoDepositStackSnapshot selected = AutoDepositPolicyTestFixtures.stack(
                chestplate, "minecraft:iron_chestplate", 1, 0, true, false, false,
                AutoDepositItemRole.CHESTPLATE, 500
        );
        AutoDepositStackSnapshot plain = AutoDepositPolicyTestFixtures.stack(
                chestplate, "minecraft:iron_chestplate", 1, 1, false, false, false,
                AutoDepositItemRole.CHESTPLATE, 400
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositPlanBuilder builder = builder(definition);
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(List.of(selected, plain));

        AutoDepositPlanDraft draft = builder.prepare(
                idleContext(), List.of(selected, plain), hard, Map.of(), 2, 1
        );
        AutoDepositPlan plan = builder.finish(draft, Optional.empty(), 1L, "none");

        assertFalse(plan.hasTargets());
        assertEquals(AutoDepositDisposition.HARD_PROTECTED, plan.dispositions().get(chestplate));
    }

    @Test
    void usesMaxOfWorkingSetAndCategoryReserveInsteadOfAddingThem() {
        Item logs = TestItems.item();
        AutoDepositStackSnapshot surplusStack = AutoDepositPolicyTestFixtures.stack(
                logs, "minecraft:oak_log", 64, 0, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot reservedStack = AutoDepositPolicyTestFixtures.stack(
                logs, "minecraft:oak_log", 16, 1, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        List<AutoDepositStackSnapshot> stacks = List.of(surplusStack, reservedStack);
        Object world = new Object();
        TestTask root = new TestTask();
        WorkingSetSnapshot workingSet = new WorkingSetSnapshot(
                root,
                List.of(root),
                world,
                Dimension.OVERWORLD,
                7L,
                Map.of(logs, 80),
                Map.of(logs, 80),
                Map.of(logs, 16)
        );
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(
                world,
                Dimension.OVERWORLD,
                "test-world",
                7L,
                root,
                workingSet,
                List.of("root")
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        Map<Item, Integer> reserves = new AutoDepositCategoryReservePolicy(
                definition, new AutoDepositItemClassificationPolicy(definition)
        ).allocate(stacks, hard, false);

        AutoDepositPlan plan = builder(definition).finish(
                builder(definition).prepare(context, stacks, hard, reserves, 2, 1),
                Optional.empty(),
                1L,
                "none"
        );

        assertEquals(Integer.valueOf(16), plan.protectedCounts().get(logs));
        assertEquals(64, plan.generalTargets()[0].getTargetCount());
    }

    @Test
    void routesValuablesOnlyToTrustedDestinationAndFailsClosedForUnknownItems() {
        Item diamond = TestItems.item();
        Item unknown = TestItems.item();
        AutoDepositStackSnapshot diamondStack = AutoDepositPolicyTestFixtures.stack(
                diamond, "minecraft:diamond", 64, 0, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot unknownStack = AutoDepositPolicyTestFixtures.stack(
                unknown, "examplemod:unknown_part", 64, 1, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositPlanBuilder builder = builder(definition);
        List<AutoDepositStackSnapshot> stacks = List.of(diamondStack, unknownStack);
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder.prepare(idleContext(), stacks, hard, Map.of(), 2, 1);

        AutoDepositPlan withoutTrust = builder.finish(draft, Optional.empty(), 1L, "none");
        AutoDepositPlan withTrust = builder.finish(
                draft, Optional.of(new BlockPos(10, 64, 10)), 2L, "eligible"
        );

        assertFalse(withoutTrust.hasTargets());
        assertEquals(1, withTrust.trustedTargets().length);
        assertEquals(64, withTrust.trustedTargets()[0].getTargetCount());
        assertEquals(AutoDepositDisposition.UNCLASSIFIED_CONSERVATIVE,
                withTrust.dispositions().get(unknown));
        assertTrue(withTrust.trustedDestination().isPresent());
    }

    @Test
    void splitsRepeatedItemsIntoSingleStackStepsAndStopsAtTheReliefGoal() {
        Item ingot = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 32, 2, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder(definition).prepare(
                idleContext(), stacks, hard, Map.of(), 3, 2
        );

        AutoDepositPlan plan = builder(definition).finish(
                draft, Optional.empty(), 1L, "none"
        );

        assertEquals(2, plan.generalTargets().length);
        assertEquals(64, plan.generalTargets()[0].getTargetCount());
        assertEquals(64, plan.generalTargets()[1].getTargetCount());
        assertEquals(2, plan.expectedFreedSlots());
        assertEquals(2, plan.targetReliefSlots());
    }

    @Test
    void usesGeneralSurplusBeforeMovingAValuableToTrustedStorage() {
        Item ingot = TestItems.item();
        Item diamond = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        diamond, "minecraft:diamond", 64, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder(definition).prepare(
                idleContext(), stacks, hard, Map.of(), 2, 1
        );

        AutoDepositPlan plan = builder(definition).finish(
                draft, Optional.of(new BlockPos(10, 64, 10)), 2L, "eligible"
        );

        assertEquals(1, plan.generalTargets().length);
        assertEquals(0, plan.trustedTargets().length);
        assertTrue(plan.trustedDestination().isEmpty());
    }

    @Test
    void reservesTrustedCapacityBeforeExecutingGeneralStepsWhenBothAreNeeded() {
        Item ingot = TestItems.item();
        Item diamond = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        diamond, "minecraft:diamond", 64, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder(definition).prepare(
                idleContext(), stacks, hard, Map.of(), 2, 2
        );
        AutoDepositPlan plan = builder(definition).finish(
                draft, Optional.of(new BlockPos(10, 64, 10)), 2L, "eligible"
        );

        AutoDepositMaintenanceTask maintenance = new AutoDepositMaintenanceTask(plan);

        assertEquals(1, plan.generalTargets().length);
        assertEquals(1, plan.trustedTargets().length);
        assertEquals(AutoDepositMaintenancePhase.DEPOSIT_TRUSTED, maintenance.phase());
    }

    private static AutoDepositPlanBuilder builder(AutoDepositPolicyDefinition definition) {
        return new AutoDepositPlanBuilder(
                definition, new AutoDepositItemClassificationPolicy(definition)
        );
    }

    private static AutoDepositContextSnapshot idleContext() {
        return new AutoDepositContextSnapshot(
                new Object(),
                Dimension.OVERWORLD,
                "test-world",
                1L,
                null,
                null,
                List.of("idle")
        );
    }

    private static final class TestTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "auto-deposit policy test task";
        }
    }
}
