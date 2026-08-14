package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionScreenSnapshot;

//20260815_kpopmodder: Keep container GUI postcondition classification out of the Carry On interaction callback.
final class CarryOnContainerExpectedGui {
    private CarryOnContainerExpectedGui() {
    }

    static String expectedScreenHandler(BlockInteractionContext context) {
        if (context == null || context.targetKind() == null) {
            return "unavailable";
        }
        return switch (context.targetKind()) {
            case "blast_furnace" -> "BlastFurnaceScreenHandler";
            case "brewing_stand" -> "BrewingStandScreenHandler";
            case "crafting_table" -> "CraftingScreenHandler";
            case "enchanting_table" -> "EnchantmentScreenHandler";
            case "furnace" -> "FurnaceScreenHandler";
            case "hopper" -> "HopperScreenHandler";
            case "smoker" -> "SmokerScreenHandler";
            case "smithing_table" -> "SmithingScreenHandler";
            case "anvil" -> "AnvilScreenHandler";
            case "dispenser", "dropper" -> "Generic3x3ContainerScreenHandler";
            case "barrel", "chest", "trapped_chest", "shulker_box" -> "GenericContainerScreenHandler";
            default -> "container_screen_handler";
        };
    }

    static boolean expectedGuiOpened(BlockInteractionContext context, BlockInteractionScreenSnapshot screenObserved) {
        if (context == null || screenObserved == null) {
            return false;
        }
        if (visibleScreenOpened(screenObserved)) {
            return true;
        }
        return screenHandlerChanged(context, screenObserved)
                && !isUnavailable(screenObserved.screenHandlerName());
    }

    static boolean screenHandlerChanged(BlockInteractionContext context, BlockInteractionScreenSnapshot screenObserved) {
        if (context == null || screenObserved == null) {
            return false;
        }
        BlockInteractionScreenSnapshot before = context.screenBefore();
        if (before == null) {
            return false;
        }
        return !before.screenHandlerName().equals(screenObserved.screenHandlerName())
                || !before.screenHandlerSyncId().equals(screenObserved.screenHandlerSyncId());
    }

    private static boolean visibleScreenOpened(BlockInteractionScreenSnapshot screenObserved) {
        return screenObserved != null
                && !isUnavailable(screenObserved.screenName())
                && !"none".equals(screenObserved.screenName());
    }

    private static boolean isUnavailable(String value) {
        return value == null || "unavailable".equals(value);
    }
}
