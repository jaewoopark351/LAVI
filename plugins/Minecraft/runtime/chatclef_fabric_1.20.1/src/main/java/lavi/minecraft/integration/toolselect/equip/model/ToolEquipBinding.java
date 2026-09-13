//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.model;
//20260913_kpopmodder: Bind an attempt to exact live objects instead of command text or item type.
public record ToolEquipBinding(Object world, Object player, Object root, Object rootInvocation, Object handler, int syncId) {
    public boolean matches(ToolEquipBinding other) {
        return other != null && world != null && player != null && root != null && rootInvocation != null && handler != null
                && world == other.world && player == other.player && root == other.root
                && rootInvocation == other.rootInvocation && handler == other.handler && syncId == other.syncId;
    }
}
//#endif
