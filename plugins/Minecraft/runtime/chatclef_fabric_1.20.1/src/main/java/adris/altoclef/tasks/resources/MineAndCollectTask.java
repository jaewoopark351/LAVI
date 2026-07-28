package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.ToolMaterialVer;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class MineAndCollectTask extends ResourceTask {

    private final Block[] _blocksToMine;

    private final MiningRequirement _requirement;

    private final TimerGame _cursorStackTimer = new TimerGame(3);

    private final MineOrCollectTask _subtask;
    private final StateChangeLogger debugLogger = new StateChangeLogger("MineAndCollectTask");

    public MineAndCollectTask(ItemTarget[] itemTargets, Block[] blocksToMine, MiningRequirement requirement) {
        super(itemTargets);
        _requirement = requirement;
        _blocksToMine = blocksToMine;
        _subtask = new MineOrCollectTask(_blocksToMine, this.itemTargets);
    }

    public MineAndCollectTask(ItemTarget[] blocksToMine, MiningRequirement requirement) {
        this(blocksToMine, itemTargetToBlockList(blocksToMine), requirement);
    }

    public MineAndCollectTask(ItemTarget target, Block[] blocksToMine, MiningRequirement requirement) {
        this(new ItemTarget[]{target}, blocksToMine, requirement);
    }

    public MineAndCollectTask(Item item, int count, Block[] blocksToMine, MiningRequirement requirement) {
        this(new ItemTarget(item, count), blocksToMine, requirement);
    }

    public static Block[] itemTargetToBlockList(ItemTarget[] targets) {
        List<Block> result = new ArrayList<>(targets.length);
        for (ItemTarget target : targets) {
            for (Item item : target.getMatches()) {
                Block block = Block.getBlockFromItem(item);
                if (block != null && !WorldHelper.isAir(block)) {
                    result.add(block);
                }
            }
        }
        return result.toArray(Block[]::new);
    }

    @Override
    public boolean isFinished() {
        if (!super.isFinished()) {
            return false;
        }
        if (!AltoClef.inGame()) {
            return true;
        }
        AltoClef mod = AltoClef.getInstance();
        return mod == null || !_subtask.shouldDelayResourceFinish(mod);
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();

        // We're mining, so don't throw away pickaxes.
        mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);

        _subtask.resetSearch();
        debugLogger.event("start: targets=" + Arrays.toString(itemTargets)
                + ", blocks=" + Arrays.toString(_blocksToMine)
                + ", requirement=" + _requirement);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // Picking up is controlled by a separate task here.
        return true;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!StorageHelper.miningRequirementMet(_requirement)) {
            debugLogger.state("satisfy mining requirement first: requirement=" + _requirement
                    + ", targets=" + Arrays.toString(itemTargets));
            return new SatisfyMiningRequirementTask(_requirement);
        }

        if (_subtask.isMining()) {
            debugLogger.state("mining active: pos=" + describePos(_subtask.miningPos())
                    + ", targets=" + Arrays.toString(itemTargets));
            makeSureToolIsEquipped(mod);
        }

        // Wrong dimension check.
        if (_subtask.wasWandering() && isInWrongDimension(mod) && !mod.getBlockScanner().anyFound(_blocksToMine)) {
            debugLogger.state("wrong dimension while mining; traveling: current=" + WorldHelper.getCurrentDimension()
                    + ", targets=" + Arrays.toString(itemTargets));
            return getToCorrectDimensionTask(mod);
        }

        debugLogger.state("delegate to mine/collect subtask: targets=" + Arrays.toString(itemTargets));
        return _subtask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        debugLogger.event("stop: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                + ", targets=" + Arrays.toString(itemTargets));
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof MineAndCollectTask task) {
            return Arrays.equals(task._blocksToMine, _blocksToMine);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Mine And Collect";
    }

    private void makeSureToolIsEquipped(AltoClef mod) {
        if (_cursorStackTimer.elapsed() && !mod.getFoodChain().needsToEat()) {
            assert MinecraftClient.getInstance().player != null;
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (cursorStack != null && !cursorStack.isEmpty()) {
                // We have something in our cursor stack
                Item item = cursorStack.getItem();
                if (item.getDefaultStack().isSuitableFor(mod.getWorld().getBlockState(_subtask.miningPos()))) {
                    // Our cursor stack would help us mine our current block
                    Item currentlyEquipped = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
                    if (item instanceof MiningToolItem) {
                        if (currentlyEquipped instanceof MiningToolItem currentPick) {
                            MiningToolItem swapPick = (MiningToolItem) item;
                            if (ToolMaterialVer.getMiningLevel(swapPick) > ToolMaterialVer.getMiningLevel(currentPick)) {
                                // We can equip a better pickaxe.
                                mod.getSlotHandler().forceEquipSlot(CursorSlot.SLOT);
                            }
                        } else {
                            // We're not equipped with a pickaxe...
                            mod.getSlotHandler().forceEquipSlot(CursorSlot.SLOT);
                        }
                    }
                }
            }
            _cursorStackTimer.reset();
        }
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

}
