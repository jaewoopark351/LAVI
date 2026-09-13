//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.model;
//20260913_kpopmodder: Keep rejected, pending and confirmed actions distinct from native void returns.
public enum ToolEquipStatus {
    NEW, PREEMPTED, WAITING_CONFIRMATION, PLACED, SELECTED,
    INVALID_BINDING, INVALID_MAPPING, CURSOR_OCCUPIED, SOURCE_CHANGED,
    DESTINATION_CHANGED, SOURCE_UNUSABLE, DESTINATION_UNAVAILABLE, SWAP_NOT_ALLOWED,
    CONFIRMATION_TIMEOUT;
    public boolean success() { return this == PLACED || this == SELECTED; }
    public boolean terminal() { return success() || ordinal() >= INVALID_BINDING.ordinal(); }
}
//#endif
