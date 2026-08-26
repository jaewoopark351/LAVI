package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static lavi.minecraft.testsupport.TestItems.item;
import static lavi.minecraft.testsupport.TestObjects.allocate;
import static lavi.minecraft.testsupport.TestObjects.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WorkingSetRequirementCollectorTest {
    @Test
    void reservesCurrentRecipeInputsAndProtectedItems() {
        Item output = item();
        Item ingredient = item();
        Item protectedItem = item();
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(new ItemTarget[]{
                new ItemTarget(ingredient),
                ItemTarget.EMPTY,
                ItemTarget.EMPTY,
                ItemTarget.EMPTY
        }, 4);
        RecipeTarget recipeTarget = new RecipeTarget(output, 8, recipe);
        CraftInInventoryTask root = allocate(CraftInInventoryTask.class);
        setField(root, ResourceTask.class, "itemTargets", new ItemTarget[]{new ItemTarget(output, 8)});
        setField(root, CraftInInventoryTask.class, "_target", recipeTarget);
        Object world = new Object();

        WorkingSetResolution resolution = new WorkingSetRequirementCollector().collect(
                root,
                List.of(root),
                world,
                Dimension.OVERWORLD,
                11L,
                Map.of(ingredient, 10, protectedItem, 3),
                Map.of(ingredient, 10, protectedItem, 3),
                item -> item == protectedItem
        );

        WorkingSetSnapshot snapshot = resolution.snapshot();
        assertEquals(WorkingSetResolution.Status.SUPPORTED, resolution.status());
        assertSame(root, snapshot.userTaskRoot());
        assertSame(world, snapshot.worldIdentity());
        assertEquals(2, snapshot.reservedCount(ingredient));
        assertEquals(3, snapshot.reservedCount(protectedItem));
    }

    @Test
    void conservativelyReservesEveryHeldAlternativeMatch() {
        Item first = item();
        Item second = item();
        WorkingSetRequirementAccumulator accumulator = new WorkingSetRequirementAccumulator(
                Map.of(first, 7, second, 9)
        );

        accumulator.reserveTarget(new ItemTarget(new Item[]{first, second}, 4));

        assertEquals(Map.of(first, 4, second, 4), accumulator.requirements());
    }

    @Test
    void unsupportedRootFailsClosedInsteadOfProducingAnEmptyReservation() {
        Task root = new UnsupportedTask();

        WorkingSetResolution resolution = new WorkingSetRequirementCollector().collect(
                root,
                List.of(root),
                new Object(),
                Dimension.OVERWORLD,
                12L,
                Map.of(),
                Map.of(),
                item -> false
        );

        assertEquals(WorkingSetResolution.Status.UNSUPPORTED, resolution.status());
        assertEquals("unsupported_user_task_root", resolution.reason());
    }

    private static final class UnsupportedTask extends Task {
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
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "unsupported working-set test task";
        }
    }
}
