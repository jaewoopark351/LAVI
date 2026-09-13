//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.model;
import java.util.Objects;
//20260913_kpopmodder: Freeze the selected source and displaced destination once before any action.
public record ToolEquipRequest(ToolEquipPurpose purpose, int sourceInventory, int destinationHotbar,
        ToolEquipFrame expected) {
    public ToolEquipRequest { Objects.requireNonNull(purpose); Objects.requireNonNull(expected); }
}
//#endif
