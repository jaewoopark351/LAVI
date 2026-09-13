//#if MC == 12001
package lavi.minecraft.integration.mining.operation.hotbar.layout;
import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.integration.mining.operation.*;
//20260913_kpopmodder: Resolve live task-local role placement without a global slot reservation registry.
public final class MiningHotbarContext {
    private MiningHotbarContext() { }
    public static PrepareThenMineRawGoldTask parent(AltoClef mod) {
        Task root = mod.getUserTaskChain().getCurrentTask();
        PrepareThenMineRawGoldTask[] found = {null};
        if (root != null) root.thisOrChildSatisfies(task -> {
            if (task instanceof PrepareThenMineRawGoldTask gold && gold.isActive() && !gold.stopped()) {
                found[0] = gold; return true;
            }
            return false;
        });
        return found[0];
    }
    public static int destination(AltoClef mod, int source, MiningOperationToolState state) {
        boolean[] empty = new boolean[9];
        for (int i = 0; i < 9; i++) empty[i] = mod.getPlayer().getInventory().getStack(i).isEmpty();
        return MiningHotbarLayout.choose(source, roleSlot(state, true), roleSlot(state, false),
                mod.getPlayer().getInventory().selectedSlot, empty);
    }
    public static boolean available(AltoClef mod, int source, int destination, MiningOperationToolState state) {
        return MiningHotbarLayout.available(destination, source, roleSlot(state, true), roleSlot(state, false),
                mod.getPlayer().getInventory().selectedSlot);
    }
    private static int roleSlot(MiningOperationToolState state, boolean target) {
        if (state == null) return -1;
        return (target ? state.targetToolCandidate() : state.accessToolCandidate())
                .filter(MiningToolCandidate::hotbarVisible).map(candidate -> candidate.slot().getInventorySlot()).orElse(-1);
    }
}
//#endif
