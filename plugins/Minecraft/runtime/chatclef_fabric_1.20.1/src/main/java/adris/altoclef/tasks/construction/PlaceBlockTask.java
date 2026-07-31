package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.multiversion.versionedfields.Items;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
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
        ChatClefDiagnostics.logEvent("PLACE_BLOCK", "ON_START_BEGIN", "place_block_start", this,
                "targetPosition", target,
                "toPlace", Arrays.toString(toPlace),
                "useThrowaways", useThrowaways,
                "autoCollectStructureBlocks", autoCollectStructureBlocks,
                "materialCount", ChatClefDiagnostics.safeValue(() -> getMaterialCount(AltoClef.getInstance())));
        progressChecker.reset();
        // If we get interrupted by another task, this might cause problems...
        //_wanderTask.resetWander();
        ChatClefDiagnostics.logEvent("PLACE_BLOCK", "ON_START_END", "place_block_start", this,
                "targetPosition", target,
                "toPlace", Arrays.toString(toPlace));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        ChatClefDiagnostics.logEvent("PLACE_BLOCK", "ON_TICK_BEGIN", "place_block_tick_begin", this,
                "targetPosition", target,
                "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target)),
                "toPlace", Arrays.toString(toPlace),
                "materialCount", ChatClefDiagnostics.safeValue(() -> getMaterialCount(mod)),
                "failCount", failCount,
                "tryingAlternativeWay", tryingAlternativeWay(),
                "pathing", mod.getClientBaritone().getPathingBehavior().isPathing(),
                "builderActive", mod.getClientBaritone().getBuilderProcess().isActive(),
                "mainHandItem", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getMainHandStack()));

        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                ChatClefDiagnostics.logInput("REQUEST", "place_block_nether_portal_hold_sneak", Input.SNEAK,
                        "inputRequested", true,
                        "targetPosition", target);
                mod.getInputControls().hold(Input.SNEAK);
                ChatClefDiagnostics.logInput("REQUEST", "place_block_nether_portal_hold_move_forward", Input.MOVE_FORWARD,
                        "inputRequested", true,
                        "targetPosition", target);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "place_block_nether_portal_pathing_release_sneak", Input.SNEAK,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "place_block_pathing_release_sneak", Input.SNEAK,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        // Perform timeout wander
        if (wanderTask.isActive() && !wanderTask.isFinished()) {
            setDebugState("Wandering.");
            progressChecker.reset();
            ChatClefDiagnostics.logTaskTransition(this, null, wanderTask, "return_active_wander",
                    "targetPosition", target,
                    "toPlace", Arrays.toString(toPlace),
                    "failCount", failCount);
            return wanderTask;
        }

        if (autoCollectStructureBlocks) {
            if (materialTask != null && materialTask.isActive() && !materialTask.isFinished()) {
                setDebugState("No structure items, collecting cobblestone + dirt as default.");
                if (getMaterialCount(mod) < PREFERRED_MATERIALS) {
                    ChatClefDiagnostics.logTaskTransition(this, null, materialTask, "return_active_material_task",
                            "targetPosition", target,
                            "toPlace", Arrays.toString(toPlace),
                            "materialCount", getMaterialCount(mod));
                    return materialTask;
                } else {
                    materialTask = null;
                }
            }

            //Item[] items = Util.toArray(Item.class, mod.getClientBaritoneSettings().acceptableThrowawayItems.value);
            if (getMaterialCount(mod) < MIN_MATERIALS) {
                // TODO: Mine items, extract their resource key somehow.
                materialTask = getMaterialTask(PREFERRED_MATERIALS);
                progressChecker.reset();
                ChatClefDiagnostics.logTaskTransition(this, null, materialTask, "return_new_material_task",
                        "targetPosition", target,
                        "toPlace", Arrays.toString(toPlace),
                        "materialCount", getMaterialCount(mod));
                return materialTask;
            }
        }


        // Check if we're approaching our point. If we fail, wander for a bit.
        if (!progressChecker.check(mod)) {
            failCount++;
            if (!tryingAlternativeWay()) {
                Debug.logMessage("Failed to place, wandering timeout.");
                ChatClefDiagnostics.logTaskTransition(this, null, wanderTask, "return_wander_after_progress_failed",
                        "targetPosition", target,
                        "toPlace", Arrays.toString(toPlace),
                        "failCount", failCount);
                return wanderTask;
            } else {
                Debug.logMessage("Trying alternative way of placing block...");
                ChatClefDiagnostics.logEvent("PLACE_BLOCK", "ALTERNATIVE_WAY", "progress_failed_try_alternative_way", this,
                        "targetPosition", target,
                        "toPlace", Arrays.toString(toPlace),
                        "failCount", failCount);
            }
        }


        // Place block
        if (tryingAlternativeWay()) {
            setDebugState("Alternative way: Trying to go above block to place block.");
            Task getToBlockTask = new GetToBlockTask(target.up(), false);
            ChatClefDiagnostics.logTaskTransition(this, null, getToBlockTask, "return_get_to_block_for_alternative_place",
                    "targetPosition", target,
                    "goToPosition", target.up(),
                    "toPlace", Arrays.toString(toPlace),
                    "failCount", failCount);
            return getToBlockTask;
        } else {
            setDebugState("Letting baritone place a block.");
            // Perform baritone placement
            if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
                Debug.logInternal("Run Structure Build");
                ISchematic schematic = new PlaceStructureSchematic(mod);
                ChatClefDiagnostics.logEvent("PLACE_BLOCK", "BUILDER_BUILD_BEGIN", "builder_process_build_structure_begin", this,
                        "targetPosition", target,
                        "targetBlockStateBeforeBuild", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target)),
                        "toPlace", Arrays.toString(toPlace),
                        "builderActiveBefore", mod.getClientBaritone().getBuilderProcess().isActive(),
                        "mainHandItem", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getMainHandStack()));
                mod.getClientBaritone().getBuilderProcess().build("structure", schematic, target);
                ChatClefDiagnostics.logEvent("PLACE_BLOCK", "BUILDER_BUILD_END", "builder_process_build_structure_end", this,
                        "targetPosition", target,
                        "targetBlockStateAfterBuildCall", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target)),
                        "toPlace", Arrays.toString(toPlace),
                        "builderActiveAfter", mod.getClientBaritone().getBuilderProcess().isActive());
            }
        }
        ChatClefDiagnostics.logEvent("PLACE_BLOCK", "RETURN", "return_null_after_place_block_tick", this,
                "targetPosition", target,
                "toPlace", Arrays.toString(toPlace),
                "builderActive", mod.getClientBaritone().getBuilderProcess().isActive());
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "place_block_onStop_begin",
                "targetPosition", target,
                "toPlace", Arrays.toString(toPlace));
        AltoClef.getInstance().getClientBaritone().getBuilderProcess().onLostControl();
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "place_block_onStop_end",
                "targetPosition", target,
                "toPlace", Arrays.toString(toPlace));
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
        boolean finished = ArrayUtils.contains(toPlace, state.getBlock());
        ChatClefDiagnostics.logEvent("PLACE_BLOCK", "IS_FINISHED", "place_block_isFinished_check", this,
                "targetPosition", target,
                "targetBlockState", state,
                "toPlace", Arrays.toString(toPlace),
                "finished", finished);
        return finished;
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
                if (!available.isEmpty()) {
                    for (BlockState possible : available) {
                        if (possible == null) continue;
                        if (useThrowaways && _mod.getClientBaritoneSettings().acceptableThrowawayItems.value.contains(possible.getBlock().asItem())) {
                            return possible;
                        }
                        if (Arrays.asList(toPlace).contains(possible.getBlock())) {
                            return possible;
                        }
                    }
                }
                Debug.logInternal("Failed to find throwaway block");
                // No throwaways available!!
                return new BlockOptionalMeta(Blocks.COBBLESTONE).getAnyBlockState();
            }
            // Don't care.
            return blockState;
        }
    }
}
