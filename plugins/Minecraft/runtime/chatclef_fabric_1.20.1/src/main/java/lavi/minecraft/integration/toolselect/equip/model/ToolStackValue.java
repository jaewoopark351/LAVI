//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.model;
//20260913_kpopmodder: Compare frozen stack values without claiming persistent entity identity.
public record ToolStackValue(Object item, int count, int damage, Object tag) {
    public boolean empty() { return count <= 0; }
}
//#endif
