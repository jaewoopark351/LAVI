//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.model;
//20260913_kpopmodder: Carry validated slot mapping and observed values to the exact-action owner.
public record ToolEquipFrame(ToolEquipBinding binding, ToolStackValue source, ToolStackValue destination,
        int sourceWindow, int destinationWindow, int selectedHotbar,
        boolean mappingValid, boolean cursorEmpty, boolean exchangeAllowed,
        boolean inputAvailable, boolean sourceUsable, boolean destinationAvailable) {
    public ToolEquipFrame {
        java.util.Objects.requireNonNull(binding);
        java.util.Objects.requireNonNull(source);
        java.util.Objects.requireNonNull(destination);
    }
}
//#endif
