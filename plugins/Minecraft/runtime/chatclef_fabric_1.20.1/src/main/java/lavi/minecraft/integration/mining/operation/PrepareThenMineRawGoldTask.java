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
import net.minecraft.block.Block;
import net.minecraft.item.Items;

import java.util.Arrays;

public final class PrepareThenMineRawGoldTask extends ResourceTask {
    private final int count;
    private final Block[] blocksToMine;
    private final MiningOperationToolPolicy toolPolicy;
    private final PrepareMiningOperationToolsTask prepareToolsTask;
    private final MineAndCollectTask mineTask;

    public PrepareThenMineRawGoldTask(int count, Block[] blocksToMine) {
        super(Items.RAW_GOLD, count);
        this.count = count;
        this.blocksToMine = blocksToMine;
        this.toolPolicy = MiningOperationToolPolicy.rawGold(count);
        this.prepareToolsTask = new PrepareMiningOperationToolsTask(toolPolicy);
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
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
        MiningOperationToolDiagnostics.logDecision(this, "prepare_then_mine_raw_gold_start", toolPolicy.evaluate(mod),
                "blocksToMine", Arrays.toString(blocksToMine));
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        MiningOperationToolState state = toolPolicy.evaluate(mod);
        MiningOperationToolDiagnostics.logDecision(this, "prepare_then_mine_raw_gold_tick", state,
                "blocksToMine", Arrays.toString(blocksToMine));
        if (!state.ready()) {
            return prepareToolsTask;
        }
        return mineTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
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
