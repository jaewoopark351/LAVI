//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.integration.mining.operation.PrepareThenMineRawGoldTask;
import lavi.minecraft.integration.mining.operation.hotbar.layout.MiningHotbarContext;
import lavi.minecraft.integration.toolselect.equip.diagnostics.ToolEquipAttemptDiagnostics;
import lavi.minecraft.integration.toolselect.equip.model.*;
import lavi.minecraft.integration.toolselect.equip.validation.ToolEquipRequestFactory;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

//20260913_kpopmodder: Own one pending exact request on the existing selection chain without retrying an uncertain swap.
public final class ExactToolEquipController {
    private ToolEquipAttemptDiagnostics diagnostics = new ToolEquipAttemptDiagnostics();
    private Object root, rootInvocation, world, player;
    private ToolEquipAttempt pending;
    private MinecraftToolEquipPort port;
    private PrepareThenMineRawGoldTask miningOwner;
    private boolean uncertainSwap;
    public void onPriorityEvaluation(AltoClef mod) {
        if (root != mod.getUserTaskChain().getCurrentTask() || rootInvocation != mod.getUserTaskChain().currentRootInvocation()
                || world != mod.getWorld() || player != mod.getPlayer()) {
            root = mod.getUserTaskChain().getCurrentTask(); world = mod.getWorld(); player = mod.getPlayer();
            rootInvocation = mod.getUserTaskChain().currentRootInvocation();
            pending = null; port = null; miningOwner = null; uncertainSwap = false;
            diagnostics = new ToolEquipAttemptDiagnostics();
        }
        if (pending != null) advance(mod);
    }
    public void equip(AltoClef mod, Slot source, ItemStack selected, BlockPos target, BlockState state) {
        if (pending != null || uncertainSwap) return;
        miningOwner = MiningHotbarContext.parent(mod);
        var miningState = miningOwner == null ? null : miningOwner.currentMiningToolState(mod);
        int sourceIndex = source.getInventorySlot();
        int destination = MiningHotbarContext.destination(mod, sourceIndex, miningState);
        port = new MinecraftToolEquipPort(mod,
                stack -> mod.getWorld().getBlockState(target).equals(state) && !StorageHelper.shouldSaveStack(mod, state.getBlock(), stack),
                slot -> MiningHotbarContext.available(mod, sourceIndex, slot,
                        miningOwner == null ? null : miningOwner.currentMiningToolState(mod)));
        pending = new ToolEquipAttempt(ToolEquipRequestFactory.capture(port, ToolEquipPurpose.SELECT_HAND,
                sourceIndex, source.getWindowSlot(), MinecraftToolEquipPort.value(selected), destination));
        advance(mod);
    }
    private void advance(AltoClef mod) {
        ToolEquipStatus status = pending.advance(port);
        diagnostics.result(miningOwner, pending, "PLAYER_INTERACTION_FIX_CHAIN");
        if (!status.terminal()) return;
        if (!status.success() && status != ToolEquipStatus.INVALID_BINDING) {
            uncertainSwap = pending.swapCount() != 0;
            if (miningOwner != null && miningOwner.isActive() && !miningOwner.stopped()) miningOwner.exactToolEquipFailed(pending);
        }
        pending = null; port = null;
    }
}
//#endif
