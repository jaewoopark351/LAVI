//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.execution;
import lavi.minecraft.integration.toolselect.equip.model.ToolEquipFrame;
//20260913_kpopmodder: Separate live Minecraft reads/actions from the finite attempt state machine.
public interface ToolEquipPort {
    ToolEquipFrame read(int sourceInventory, int destinationHotbar);
    void swap(int sourceWindow, int destinationHotbar);
    void selectHotbar(int hotbar);
}
//#endif
