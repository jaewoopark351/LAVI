//#if MC == 12001
package lavi.minecraft.integration.mining.operation.hotbar.layout;
//20260913_kpopmodder: Place tools without evicting role tools, active hand, or the engine's first/last work slots.
public final class MiningHotbarLayout {
    private MiningHotbarLayout() { }
    public static boolean available(int slot, int source, int targetTool, int accessTool, int selected) {
        if (source >= 0 && source < 9) return slot == source;
        return slot > 0 && slot < 8 && slot != targetTool && slot != accessTool && slot != selected;
    }
    public static int choose(int source, int targetTool, int accessTool, int selected, boolean[] empty) {
        if (source >= 0 && source < 9) return source;
        if (empty == null || empty.length != 9) return -1;
        for (int pass = 0; pass < 2; pass++) for (int slot = 1; slot < 8; slot++) {
            if (available(slot, source, targetTool, accessTool, selected) && (pass != 0 || empty[slot])) return slot;
        }
        return -1;
    }
}
//#endif
