package lavi.minecraft.task.resources.wood;

import adris.altoclef.tasks.resources.wood.CollectWoodenButtonTask;
import adris.altoclef.tasks.resources.wood.CollectWoodenPressurePlateTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260906_kpopmodder: Guard the one-plank wooden-button recipe against zero-match TaskCatalogue recursion.
class WoodenButtonRecipeMaskRegressionTest {
    private static final String BUTTON_SOURCE =
            "src/main/java/adris/altoclef/tasks/resources/wood/CollectWoodenButtonTask.java";
    private static final String PRESSURE_PLATE_SOURCE =
            "src/main/java/adris/altoclef/tasks/resources/wood/CollectWoodenPressurePlateTask.java";
    private static final String MATCHING_MATERIALS_SOURCE =
            "src/main/java/adris/altoclef/tasks/resources/CraftWithMatchingMaterialsTask.java";
    private static final String MATCHING_PLANKS_SOURCE =
            "src/main/java/adris/altoclef/tasks/resources/CraftWithMatchingPlanksTask.java";
    private static final String TASK_CATALOGUE_SOURCE =
            "src/main/java/adris/altoclef/TaskCatalogue.java";
    private static final Pattern FIRST_BOOLEAN_MASK = Pattern.compile(
            "new\\s+boolean\\s*\\[\\]\\s*\\{([^}]*)}"
    );

    @Test
    void buttonConstructorKeepsAnyWoodOutputAndMarksOnlyItsOneIngredientSlot() throws IOException {
        String raw = source(BUTTON_SOURCE);
        String compact = compact(raw);

        assertArrayEquals(new boolean[]{true, false, false, false}, firstBooleanMask(raw));
        assertTrue(compact.contains(
                "super(targets,woodItems->woodItems.button,createRecipe(planks)," +
                        "newboolean[]{true,false,false,false},count);"
        ));
        assertTrue(compact.contains(
                "returnCraftingRecipe.newShapedRecipe(" +
                        "newItemTarget[]{p,null,null,null},1);"
        ));
        assertTrue(raw.contains(
                "//20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary."
        ));
        assertFalse(compact.contains("newboolean[]{true,true,false,false}"));
    }

    @Test
    void buttonMaskSelectsTheRealPlankSlotInsteadOfAnEmptyRecipeSlot() throws IOException {
        ItemTarget planks = registryFreeTarget();
        CraftingRecipe buttonRecipe = recipe(CollectWoodenButtonTask.class, planks);
        boolean[] mask = firstBooleanMask(source(BUTTON_SOURCE));
        ItemTarget selected = lastMaskedSlot(buttonRecipe, mask);

        assertEquals(1, countTrue(mask));
        assertEquals(1, Arrays.stream(buttonRecipe.getSlots()).filter(slot -> !slot.isEmpty()).count());
        assertSame(planks, buttonRecipe.getSlot(0));
        assertSame(planks, selected);
        assertFalse(selected.isEmpty());
        assertSame(ItemTarget.EMPTY, buttonRecipe.getSlot(1));
        assertSame(ItemTarget.EMPTY, buttonRecipe.getSlot(2));
        assertSame(ItemTarget.EMPTY, buttonRecipe.getSlot(3));
    }

    @Test
    void broadButtonAcquisitionStillTargetsTheFinitePlanksTaskBoundary() throws IOException {
        String button = compact(source(BUTTON_SOURCE));
        String matchingMaterials = compact(source(MATCHING_MATERIALS_SOURCE));
        String catalogue = compact(source(TASK_CATALOGUE_SOURCE));

        assertTrue(button.contains(
                "this(ItemHelper.WOOD_BUTTON,TaskCatalogue.getItemTarget(\"planks\",1),count);"
        ));
        assertTrue(matchingMaterials.contains(
                "ItemTargetinfinityVersion=newItemTarget(sameResourceTarget,999999);"
        ));
        assertTrue(matchingMaterials.contains("returnTaskCatalogue.getItemTask(infinityVersion);"));
        assertTrue(catalogue.contains(
                "simple(\"planks\",ItemHelper.PLANKS,CollectPlanksTask::new).dontMineIfPresent();"
        ));
    }

    @Test
    void selectedWoodSpeciesStillDeterminesTheButtonOutput() throws IOException {
        String button = compact(source(BUTTON_SOURCE));
        String matchingPlanks = compact(source(MATCHING_PLANKS_SOURCE));

        assertTrue(button.contains("woodItems->woodItems.button"));
        assertTrue(matchingPlanks.contains("if(woodItems.planks==majority)"));
        assertTrue(matchingPlanks.contains("return_getTargetItem.apply(woodItems);"));
    }

    @Test
    void woodenPressurePlateRetainsItsTwoPlankMaskAndRecipe() throws IOException {
        String raw = source(PRESSURE_PLATE_SOURCE);
        String compact = compact(raw);
        ItemTarget planks = registryFreeTarget();
        CraftingRecipe pressurePlateRecipe = recipe(CollectWoodenPressurePlateTask.class, planks);

        assertArrayEquals(new boolean[]{true, true, false, false}, firstBooleanMask(raw));
        assertTrue(compact.contains(
                "super(targets,woodItems->woodItems.pressurePlate,createRecipe(planks)," +
                        "newboolean[]{true,true,false,false},count);"
        ));
        assertTrue(compact.contains(
                "returnCraftingRecipe.newShapedRecipe(" +
                        "newItemTarget[]{p,p,null,null},1);"
        ));
        assertEquals(2, Arrays.stream(pressurePlateRecipe.getSlots()).filter(slot -> !slot.isEmpty()).count());
        assertSame(planks, pressurePlateRecipe.getSlot(0));
        assertSame(planks, pressurePlateRecipe.getSlot(1));
        assertSame(ItemTarget.EMPTY, pressurePlateRecipe.getSlot(2));
        assertSame(ItemTarget.EMPTY, pressurePlateRecipe.getSlot(3));
    }

    private static ItemTarget registryFreeTarget() {
        return new ItemTarget(new Item[]{null}, 1);
    }

    private static CraftingRecipe recipe(Class<?> taskType, ItemTarget planks) {
        try {
            Method createRecipe = taskType.getDeclaredMethod("createRecipe", ItemTarget.class);
            createRecipe.setAccessible(true);
            return (CraftingRecipe) createRecipe.invoke(null, planks);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to invoke recipe builder for " + taskType.getName(), exception);
        }
    }

    private static boolean[] firstBooleanMask(String source) {
        Matcher matcher = FIRST_BOOLEAN_MASK.matcher(source);
        assertTrue(matcher.find(), "Expected a boolean recipe mask in source.");
        String[] values = matcher.group(1).split(",");
        boolean[] result = new boolean[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = Boolean.parseBoolean(values[index].trim());
        }
        return result;
    }

    private static ItemTarget lastMaskedSlot(CraftingRecipe recipe, boolean[] mask) {
        ItemTarget selected = null;
        for (int index = 0; index < mask.length; index++) {
            if (mask[index]) {
                selected = recipe.getSlot(index);
            }
        }
        return selected;
    }

    private static long countTrue(boolean[] values) {
        long count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(locate(relativePath))
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static Path locate(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate source path: " + relativePath);
    }
}
