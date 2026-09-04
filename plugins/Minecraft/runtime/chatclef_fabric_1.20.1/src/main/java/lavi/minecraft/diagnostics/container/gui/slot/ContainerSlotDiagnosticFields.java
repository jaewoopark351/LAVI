package lavi.minecraft.diagnostics.container.gui.slot;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

//20260904_kpopmodder: Format slot evidence without observing or controlling slot behavior.
public final class ContainerSlotDiagnosticFields {
    private ContainerSlotDiagnosticFields() {
    }

    public static Object[] from(
            ContainerSlotActionObservation action,
            ContainerItemCountSnapshot counts) {
        return new Object[]{
                "slotActionOwner", ChatClefDiagnostics.taskSummaryForDiagnosticLog(
                        ChatClefDiagnostics.currentTaskForDiagnostics()
                ),
                "windowSlot", action.windowSlot(),
                "slotButton", action.slotButton(),
                "slotActionType", action.slotActionType(),
                "focusItemId", counts.focusItemId(),
                "playerItemCount", counts.playerItemCount(),
                "containerItemCount", counts.containerItemCount(),
                "cursorItemCount", counts.cursorItemCount(),
                "cursorStack", counts.cursorStack(),
                "clickedSlotStack", counts.clickedSlotStack(),
                "furnaceMaterialSlot", counts.furnaceMaterialSlot(),
                "furnaceFuelSlot", counts.furnaceFuelSlot(),
                "furnaceOutputSlot", counts.furnaceOutputSlot()
        };
    }

    public static String stackSummary(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? "empty"
                : itemId(stack) + "x" + stack.getCount();
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        try {
            return Registries.ITEM.getId(stack.getItem()).toString();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }
}
