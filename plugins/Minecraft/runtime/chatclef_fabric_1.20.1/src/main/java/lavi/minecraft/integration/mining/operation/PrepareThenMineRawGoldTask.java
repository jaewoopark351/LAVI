package lavi.minecraft.integration.mining.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.operation.MiningOperationToolDiagnostics;
import lavi.minecraft.diagnostics.mining.gold.GoldMiningToolObservers;
import lavi.minecraft.diagnostics.mining.gold.GoldToolLoopObserver;
//#if MC == 12001
import lavi.minecraft.integration.mining.operation.hotbar.lifecycle.MiningHotbarOperation;
import lavi.minecraft.integration.toolselect.equip.execution.ToolEquipAttempt;
//#endif
import net.minecraft.block.Block;
import net.minecraft.item.Items;

import java.util.Arrays;

public final class PrepareThenMineRawGoldTask extends ResourceTask {
    private final int count;
    private final Block[] blocksToMine;
    private final MiningOperationToolPolicy toolPolicy;
    private final PrepareMiningOperationToolsTask prepareToolsTask;
    private final MineAndCollectTask mineTask;
    //#if MC == 12001
    //20260913_kpopmodder: Share finite placement state across child restarts of this exact parent.
    private final MiningHotbarOperation hotbarOperation;
    public MiningOperationToolState currentMiningToolState(AltoClef mod) { return toolPolicy.evaluate(mod); }
    public java.util.Optional<String> miningToolFailureReason() { return hotbarOperation.failureReason(); }
    public void exactToolEquipFailed(ToolEquipAttempt attempt) { hotbarOperation.exactEquipFailed(this, attempt); }
    //#endif
    //20260913_kpopmodder: Observe this parent's loop without resetting it on descendant interruption.
    private final GoldToolLoopObserver diagnosticToolLoop = new GoldToolLoopObserver();

    public GoldToolLoopObserver diagnosticToolLoopObserver() {
        return diagnosticToolLoop;
    }

    public PrepareThenMineRawGoldTask(int count, Block[] blocksToMine) {
        super(Items.RAW_GOLD, count);
        this.count = count;
        this.blocksToMine = blocksToMine;
        this.toolPolicy = MiningOperationToolPolicy.rawGold(count);
        //#if MC == 12001
        this.hotbarOperation = new MiningHotbarOperation(toolPolicy);
        this.prepareToolsTask = new PrepareMiningOperationToolsTask(toolPolicy, hotbarOperation);
        //#else
        //$$ this.prepareToolsTask = new PrepareMiningOperationToolsTask(toolPolicy);
        //#endif
        this.mineTask = new MineAndCollectTask(new ItemTarget(Items.RAW_GOLD, count), blocksToMine, MiningRequirement.IRON);
    }

    @Override
    public ResourceTask forceDimension(Dimension dimension) {
        super.forceDimension(dimension);
        mineTask.forceDimension(dimension);
        return this;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        //#if MC == 12001
        hotbarOperation.bind(mod);
        //#endif
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
        MiningOperationToolDiagnostics.logDecision(this, "prepare_then_mine_raw_gold_start", toolPolicy.evaluate(mod),
                "blocksToMine", Arrays.toString(blocksToMine));
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        //#if MC == 12001
        hotbarOperation.bind(mod);
        if (hotbarOperation.failureReason().isPresent()) {
            hotbarOperation.failRootIfCurrent(mod, this);
            return null;
        }
        //#endif
        MiningOperationToolState state = toolPolicy.evaluate(mod);
        MiningOperationToolDiagnostics.logDecision(this, "prepare_then_mine_raw_gold_tick", state,
                "blocksToMine", Arrays.toString(blocksToMine));
        //#if MC == 12001
        hotbarOperation.pollPending(mod, state, this);
        if (hotbarOperation.failureReason().isPresent()) {
            hotbarOperation.failRootIfCurrent(mod, this);
            return null;
        }
        if (hotbarOperation.hasPending()) return prepareToolsTask;
        //#endif
        if (!state.ready()) {
            GoldMiningToolObservers.preparation(mod, this, state, prepareToolsTask);
            return prepareToolsTask;
        }
        GoldMiningToolObservers.preparation(mod, this, state, mineTask);
        return mineTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        GoldMiningToolObservers.parentStopped(mod, this, interruptTask);
        MiningOperationToolDiagnostics.logDecision(this, "prepare_then_mine_raw_gold_stop", toolPolicy.evaluate(mod),
                "blocksToMine", Arrays.toString(blocksToMine),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof PrepareThenMineRawGoldTask task
                && task.count == count
                && Arrays.equals(task.blocksToMine, blocksToMine);
    }

    @Override
    protected String toDebugStringName() {
        return "Prepare Then Mine Raw Gold";
    }
}
