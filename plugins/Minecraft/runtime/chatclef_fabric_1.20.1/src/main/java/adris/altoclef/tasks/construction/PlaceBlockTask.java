package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.multiversion.versionedfields.Items;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.escape.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Place a block type at a position
 */
public class PlaceBlockTask extends Task implements ITaskRequiresGrounded {

    private static final int MIN_MATERIALS = 1;
    private static final int PREFERRED_MATERIALS = 32;
    private final BlockPos target;
    private final Block[] toPlace;
    private final boolean useThrowaways;
    private final boolean autoCollectStructureBlocks;
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5); // This can get stuck forever, so we increase the range.
    private Task materialTask;
    private int failCount = 0;
    private final StateChangeLogger debugLogger = new StateChangeLogger("PlaceBlockTask");

    public PlaceBlockTask(BlockPos target, Block[] toPlace, boolean useThrowaways, boolean autoCollectStructureBlocks) {
        this.target = target;
        this.toPlace = toPlace;
        this.useThrowaways = useThrowaways;
        this.autoCollectStructureBlocks = autoCollectStructureBlocks;
    }

    public PlaceBlockTask(BlockPos target, Block... toPlace) {
        this(target, toPlace, false, false);
    }

    public int getMaterialCount(AltoClef mod) {
        int count = mod.getItemStorage().getItemCount(ItemHelper.blocksToItems(toPlace));

        if (useThrowaways) {
            count += mod.getItemStorage().getItemCount(mod.getClientBaritoneSettings().acceptableThrowawayItems.value.toArray(new Item[0]));
        }
        return count;
    }

    public static Task getMaterialTask(int count) {
        return TaskCatalogue.getSquashedItemTask(new ItemTarget(Items.DIRT, count), new ItemTarget(Items.COBBLESTONE,
                count), new ItemTarget(Items.NETHERRACK, count), new ItemTarget(Items.COBBLED_DEEPSLATE, count));
    }

    @Override
    protected void onStart() {
        progressChecker.reset();
        debugLogger.event("start: target=" + target.toShortString()
                + ", blocks=" + describeBlocks()
                + ", useThrowaways=" + useThrowaways
                + ", autoCollectStructureBlocks=" + autoCollectStructureBlocks
                + ", " + describePlacementContext(AltoClef.getInstance()));
        // If we get interrupted by another task, this might cause problems...
        //_wanderTask.resetWander();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        // Perform timeout wander
        if (wanderTask.isActive() && !wanderTask.isFinished()) {
            setDebugState("Wandering.");
            debugLogger.state("wandering before place retry: target=" + target.toShortString()
                    + ", failCount=" + failCount);
            progressChecker.reset();
            return wanderTask;
        }

        if (autoCollectStructureBlocks) {
            if (materialTask != null && materialTask.isActive() && !materialTask.isFinished()) {
                setDebugState("No structure items, collecting cobblestone + dirt as default.");
                if (getMaterialCount(mod) < PREFERRED_MATERIALS) {
                    debugLogger.state("continue collecting place materials: count=" + getMaterialCount(mod)
                            + ", preferred=" + PREFERRED_MATERIALS
                            + ", target=" + target.toShortString());
                    return materialTask;
                } else {
                    debugLogger.state("place material target satisfied: count=" + getMaterialCount(mod)
                            + ", preferred=" + PREFERRED_MATERIALS);
                    materialTask = null;
                }
            }

            //Item[] items = Util.toArray(Item.class, mod.getClientBaritoneSettings().acceptableThrowawayItems.value);
            if (getMaterialCount(mod) < MIN_MATERIALS) {
                // TODO: Mine items, extract their resource key somehow.
                materialTask = getMaterialTask(PREFERRED_MATERIALS);
                debugLogger.state("collect place materials: count=" + getMaterialCount(mod)
                        + ", minimum=" + MIN_MATERIALS
                        + ", preferred=" + PREFERRED_MATERIALS
                        + ", target=" + target.toShortString());
                progressChecker.reset();
                return materialTask;
            }
        }


        // Check if we're approaching our point. If we fail, wander for a bit.
        if (!progressChecker.check(mod)) {
            failCount++;
            if (!tryingAlternativeWay()) {
                Debug.logMessage("Failed to place, wandering timeout.");
                debugLogger.state("place progress failed; wandering: target=" + target.toShortString()
                        + ", failCount=" + failCount
                        + ", " + describePlacementContext(mod));
                return wanderTask;
            } else {
                Debug.logMessage("Trying alternative way of placing block...");
                debugLogger.state("place progress failed; trying alternative: target=" + target.toShortString()
                        + ", failCount=" + failCount
                        + ", " + describePlacementContext(mod));
            }
        }


        // Place block
        if (tryingAlternativeWay()) {
            setDebugState("Alternative way: Trying to go above block to place block.");
            debugLogger.state("alternative place route: target=" + target.toShortString()
                    + ", " + describePlacementContext(mod));
            return new GetToBlockTask(target.up(), false);
        } else {
            setDebugState("Letting baritone place a block.");
            // Perform baritone placement
            if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
                Debug.logInternal("Run Structure Build");
                debugLogger.state("start baritone build: target=" + target.toShortString()
                        + ", " + describePlacementContext(mod));
                ISchematic schematic = new PlaceStructureSchematic(mod);
                try {
                    mod.getClientBaritone().getBuilderProcess().build("structure", schematic, target);
                } catch (RuntimeException exception) {
                    debugLogger.event("baritone build exception: " + describeException(exception)
                            + ", " + describePlacementContext(mod));
                    exception.printStackTrace();
                    mod.getClientBaritone().getBuilderProcess().onLostControl();
                    progressChecker.reset();
                    failCount++;
                    return wanderTask;
                }
            } else {
                debugLogger.state("baritone build already active: target=" + target.toShortString()
                        + ", " + describePlacementContext(mod));
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        debugLogger.event("stop: target=" + target.toShortString()
                + ", interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName()));
        AltoClef.getInstance().getClientBaritone().getBuilderProcess().onLostControl();
    }

    //TODO: Place structure where a leaf block was???? Might need to delete the block first if it's not empty/air/water.

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceBlockTask task) {
            return task.target.equals(target) && task.useThrowaways == useThrowaways && Arrays.equals(task.toPlace, toPlace);
        }
        return false;
    }

    @Override
    public boolean isFinished() {
        assert MinecraftClient.getInstance().world != null;
        if (useThrowaways) {
            return WorldHelper.isSolidBlock(target);
        }
        BlockState state = AltoClef.getInstance().getWorld().getBlockState(target);
        return ArrayUtils.contains(toPlace, state.getBlock());
    }

    @Override
    protected String toDebugString() {
        return "Place structure" + ArrayUtils.toString(toPlace) + " at " + target.toShortString();
    }

    private boolean tryingAlternativeWay() {
        return failCount % 4 == 3;
    }

    private class PlaceStructureSchematic extends AbstractSchematic {

        private final AltoClef _mod;

        public PlaceStructureSchematic(AltoClef mod) {
            super(1, 1, 1);
            _mod = mod;
        }

        @Override
        public BlockState desiredState(int x, int y, int z, BlockState blockState, List<BlockState> available) {
            if (x == 0 && y == 0 && z == 0) {
                // Place!!
                if (available != null && !available.isEmpty()) {
                    for (BlockState possible : available) {
                        if (possible == null) continue;
                        if (useThrowaways && _mod.getClientBaritoneSettings().acceptableThrowawayItems.value.contains(possible.getBlock().asItem())) {
                            debugLogger.state("schematic selected throwaway block: target=" + target.toShortString(),
                                    "schematic selected throwaway block: selected=" + describeBlockState(possible)
                                            + ", target=" + target.toShortString()
                                            + ", available=" + describeAvailableBlocks(available));
                            return possible;
                        }
                        if (Arrays.asList(toPlace).contains(possible.getBlock())) {
                            debugLogger.state("schematic selected requested block: target=" + target.toShortString(),
                                    "schematic selected requested block: selected=" + describeBlockState(possible)
                                            + ", target=" + target.toShortString()
                                            + ", available=" + describeAvailableBlocks(available));
                            return possible;
                        }
                    }
                }
                Debug.logInternal("Failed to find throwaway block");
                debugLogger.state("schematic could not find matching available block: target=" + target.toShortString()
                        + ", blocks=" + describeBlocks()
                        + ", available=" + describeAvailableBlocks(available));
                // No throwaways available!!
                BlockState fallback = getFallbackDesiredState();
                debugLogger.state("schematic fallback desired state: fallback=" + describeBlockState(fallback)
                                + ", target=" + target.toShortString(),
                        "schematic fallback desired state: fallback=" + describeBlockState(fallback)
                                + ", target=" + target.toShortString()
                                + ", blocks=" + describeBlocks()
                                + ", available=" + describeAvailableBlocks(available));
                return fallback;
            }
            // Don't care.
            return blockState;
        }
    }

    private BlockState getFallbackDesiredState() {
        //20260727_kpopmodder: Avoid BlockOptionalMeta here; it can throw through Baritone during resource reloads.
        for (Block block : toPlace) {
            if (block != null) {
                return block.getDefaultState();
            }
        }
        return Blocks.COBBLESTONE.getDefaultState();
    }

    private String describePlacementContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null || mod.getWorld() == null) {
            return "context=missing-client";
        }

        return "player=" + mod.getPlayer().getBlockPos().toShortString()
                + ", targetState=" + describeBlockState(mod.getWorld().getBlockState(target))
                + ", materialCount=" + getMaterialCount(mod)
                + ", cursor=" + describeStack(StorageHelper.getItemStackInCursorSlot())
                + ", equipped=" + StorageHelper.isEquipped(ItemHelper.blocksToItems(toPlace))
                + ", screen=" + describeCurrentScreen()
                + ", screenHandler=" + describeScreenHandler(mod)
                + ", builderActive=" + mod.getClientBaritone().getBuilderProcess().isActive()
                + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                + ", failCount=" + failCount
                + ", blocks=" + describeBlocks();
    }

    private String describeCurrentScreen() {
        Object screen = MinecraftClient.getInstance().currentScreen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private String describeScreenHandler(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null || mod.getPlayer().currentScreenHandler == null) {
            return "none";
        }
        return mod.getPlayer().currentScreenHandler.getClass().getSimpleName();
    }

    private String describeAvailableBlocks(List<BlockState> available) {
        if (available == null) {
            return "null";
        }

        StringBuilder result = new StringBuilder();
        result.append(available.size()).append(" [");
        int limit = Math.min(available.size(), 8);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(describeBlockState(available.get(i)));
        }
        if (available.size() > limit) {
            result.append(", ...");
        }
        result.append("]");
        return result.toString();
    }

    private String describeBlockState(BlockState state) {
        if (state == null) {
            return "null";
        }
        return state.getBlock().getTranslationKey();
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + " x " + stack.getCount();
    }

    private String describeException(RuntimeException exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }

    private String describeBlocks() {
        return ArrayUtils.toString(toPlace);
    }
}
