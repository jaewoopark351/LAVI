//#if MC == 12001
package lavi.minecraft.integration.lifecycle.root.failure;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.integration.mining.operation.PrepareThenMineRawGoldTask;

//20260913_kpopmodder: Snapshot the stopped root's operation-owned reason before callbacks can replace its child tree.
public final class GoldMiningFailureCapture {
    private GoldMiningFailureCapture() { }
    public static String read(Task root) {
        if (root == null) return "";
        String[] reason = { "" };
        try {
            root.thisOrChildSatisfies(task -> {
                if (task instanceof PrepareThenMineRawGoldTask preparation) {
                    reason[0] = preparation.miningToolFailureReason().orElse("");
                    return !reason[0].isEmpty();
                }
                return false;
            });
        } catch (RuntimeException unavailable) {
            return "";
        }
        return reason[0];
    }
}
//#endif
