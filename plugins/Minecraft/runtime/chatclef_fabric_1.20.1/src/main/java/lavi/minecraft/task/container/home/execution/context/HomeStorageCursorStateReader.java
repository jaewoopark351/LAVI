package lavi.minecraft.task.container.home.execution.context;

import adris.altoclef.AltoClef;
import net.minecraft.item.ItemStack;

//20260829_kpopmodder: Added this type file to read only the live cursor safety state.
public final class HomeStorageCursorStateReader {
    public boolean isEmpty(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null
                || mod.getPlayer().currentScreenHandler == null) {
            return false;
        }
        ItemStack cursor = mod.getPlayer().currentScreenHandler.getCursorStack();
        return cursor == null || cursor.isEmpty();
    }
}
