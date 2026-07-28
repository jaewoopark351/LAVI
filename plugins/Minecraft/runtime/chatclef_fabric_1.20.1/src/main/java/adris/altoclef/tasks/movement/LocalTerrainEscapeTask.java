package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.MagmaBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

//20260728_kpopmodder: Local terrain escape stays bounded so blocked goal recovery cannot become broad mining.
class LocalTerrainEscapeTask extends Task implements ITaskRequiresGrounded {
    private static final double TIMEOUT_SECONDS = 8.0;
    private static final double MAX_DISTANCE_FROM_ORIGIN = 4.5;
    private static final int STAIR_STEPS = 2;
    private static final int MAX_BLOCKS_TO_CLEAR = 8;

    private final Plan plan;
    private final String parentTaskDebug;
    private final TimerGame timeout = new TimerGame(TIMEOUT_SECONDS);
    private final StateChangeLogger debugLogger = new StateChangeLogger("LocalTerrainEscapeTask");
    private int clearIndex;
    private boolean finished;
    private boolean timedOut;

    LocalTerrainEscapeTask(Plan plan, String parentTaskDebug) {
        this.plan = plan;
        this.parentTaskDebug = parentTaskDebug;
    }

    static Optional<Plan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        if (mod.getPlayer() == null) {
            return Optional.empty();
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (cooldownOrigins.contains(origin)) {
            return Optional.empty();
        }

        List<Direction> directions = orderedDirections(mod);
        for (Direction direction : directions) {
            Optional<Plan> stairPlan = buildStairPlan(mod, origin, direction);
            if (stairPlan.isPresent()) {
                return stairPlan;
            }
        }
        for (Direction direction : directions) {
            Optional<Plan> sidePlan = buildSidePocketPlan(mod, origin, direction);
            if (sidePlan.isPresent()) {
                return sidePlan;
            }
        }
        return buildVerticalHeadroomPlan(mod, origin);
    }

    @Override
    protected void onStart() {
        timeout.reset();
        clearIndex = 0;
        finished = false;
        timedOut = false;
        debugLogger.event("start: " + describePlan()
                + ", parentTask=" + parentTaskDebug
                + ", timeoutSeconds=" + TIMEOUT_SECONDS);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (mod.getPlayer() == null) {
            finished = true;
            debugLogger.event("finished: player unavailable, " + describePlan());
            return null;
        }
        if (timeout.elapsed()) {
            timedOut = true;
            finished = true;
            debugLogger.event("timed out: " + describePlan());
            return null;
        }
        if (!mod.getPlayer().getPos().isInRange(WorldHelper.toVec3d(plan.origin), MAX_DISTANCE_FROM_ORIGIN)) {
            finished = true;
            debugLogger.event("finished: left local recovery area, " + describePlan()
                    + ", player=" + formatVec(mod.getPlayer().getPos()));
            return null;
        }

        while (clearIndex < plan.blocksToClear.size()
                && isEscapeSpaceClear(mod, plan.blocksToClear.get(clearIndex))) {
            clearIndex++;
        }
        if (clearIndex >= plan.blocksToClear.size()) {
            finished = true;
            debugLogger.event("finished: route cleared, " + describePlan());
            return null;
        }

        BlockPos target = plan.blocksToClear.get(clearIndex);
        if (!isSafeBreakTarget(mod, target)) {
            finished = true;
            debugLogger.event("finished: target no longer safe to clear: target=" + target.toShortString()
                    + ", " + describePlan());
            return null;
        }

        setDebugState("Clearing terrain " + (clearIndex + 1) + "/" + plan.blocksToClear.size());
        return new DestroyBlockTask(target);
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    boolean didTimeOut() {
        return timedOut;
    }

    BlockPos getOrigin() {
        return plan.origin;
    }

    String describePlan() {
        return plan.describe();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof LocalTerrainEscapeTask task) {
            return plan.equals(task.plan);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Local terrain escape " + plan.kind + " " + plan.direction.getName();
    }

    private static Optional<Plan> buildStairPlan(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        if (!appendSidePocketBlocks(mod, origin, direction, blocksToClear)) {
            return Optional.empty();
        }
        for (int step = 1; step <= STAIR_STEPS; step++) {
            BlockPos foot = offset(origin, direction, step + 1).up(step);
            if (!hasSafeFloor(mod, foot.down())
                    || !appendClearBlock(mod, foot, blocksToClear)
                    || !appendClearBlock(mod, foot.up(), blocksToClear)) {
                return Optional.empty();
            }
        }
        if (blocksToClear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Plan(origin, direction, "stair", blocksToClear));
    }

    private static Optional<Plan> buildSidePocketPlan(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        if (!appendSidePocketBlocks(mod, origin, direction, blocksToClear) || blocksToClear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Plan(origin, direction, "side", blocksToClear));
    }

    private static Optional<Plan> buildVerticalHeadroomPlan(AltoClef mod, BlockPos origin) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        if (!appendClearBlock(mod, origin.up(2), blocksToClear)
                || !appendClearBlock(mod, origin.up(3), blocksToClear)
                || blocksToClear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Plan(origin, Direction.UP, "headroom", blocksToClear));
    }

    private static boolean appendSidePocketBlocks(AltoClef mod, BlockPos origin, Direction direction, List<BlockPos> blocksToClear) {
        BlockPos sideFoot = offset(origin, direction, 1);
        return hasSafeFloor(mod, sideFoot.down())
                && appendClearBlock(mod, sideFoot, blocksToClear)
                && appendClearBlock(mod, sideFoot.up(), blocksToClear);
    }

    private static boolean appendClearBlock(AltoClef mod, BlockPos pos, List<BlockPos> blocksToClear) {
        if (isEscapeSpaceClear(mod, pos)) {
            return true;
        }
        if (blocksToClear.size() >= MAX_BLOCKS_TO_CLEAR || !isSafeBreakTarget(mod, pos)) {
            return false;
        }
        if (!blocksToClear.contains(pos)) {
            blocksToClear.add(pos);
        }
        return true;
    }

    private static List<Direction> orderedDirections(AltoClef mod) {
        Direction facing = mod.getPlayer().getHorizontalFacing();
        List<Direction> result = new ArrayList<>();
        addDirection(result, facing);
        addDirection(result, facing.rotateYClockwise());
        addDirection(result, facing.rotateYCounterclockwise());
        addDirection(result, facing.getOpposite());
        addDirection(result, Direction.NORTH);
        addDirection(result, Direction.SOUTH);
        addDirection(result, Direction.EAST);
        addDirection(result, Direction.WEST);
        return result;
    }

    private static void addDirection(List<Direction> directions, Direction direction) {
        if (direction != null && direction.getAxis().isHorizontal() && !directions.contains(direction)) {
            directions.add(direction);
        }
    }

    private static BlockPos offset(BlockPos origin, Direction direction, int distance) {
        return origin.add(
                direction.getOffsetX() * distance,
                direction.getOffsetY() * distance,
                direction.getOffsetZ() * distance);
    }

    private static boolean isEscapeSpaceClear(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.getBlock() instanceof FluidBlock) {
            return false;
        }
        return state.getCollisionShape(mod.getWorld(), pos).isEmpty();
    }

    private static boolean hasSafeFloor(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        return !state.isAir()
                && !(block instanceof FluidBlock)
                && !(block instanceof CactusBlock)
                && !(block instanceof CampfireBlock)
                && !(block instanceof MagmaBlock)
                && !(block instanceof AbstractFireBlock)
                && state.isSolidBlock(mod.getWorld(), pos);
    }

    private static boolean isSafeBreakTarget(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (isProtectedBlock(pos, block) || !WorldHelper.canBreak(pos)) {
            return false;
        }
        return !(block instanceof FallingBlock) || WorldHelper.fallingBlockSafeToBreak(pos);
    }

    private static boolean isProtectedBlock(BlockPos pos, Block block) {
        return WorldHelper.isInteractableBlock(pos)
                || block instanceof BedBlock
                || block instanceof SpawnerBlock
                || block instanceof NetherPortalBlock
                || block instanceof EndPortalBlock
                || block instanceof EndPortalFrameBlock
                || block instanceof FluidBlock;
    }

    private static String formatVec(Vec3d vec) {
        if (vec == null) {
            return "none";
        }
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }

    static class Plan {
        private final BlockPos origin;
        private final Direction direction;
        private final String kind;
        private final List<BlockPos> blocksToClear;

        private Plan(BlockPos origin, Direction direction, String kind, List<BlockPos> blocksToClear) {
            this.origin = origin;
            this.direction = direction;
            this.kind = kind;
            this.blocksToClear = List.copyOf(blocksToClear);
        }

        String describe() {
            return "kind=" + kind
                    + ", origin=" + origin.toShortString()
                    + ", direction=" + direction.getName()
                    + ", blocks=" + describeBlocks();
        }

        private String describeBlocks() {
            List<String> blockStrings = new ArrayList<>();
            for (BlockPos block : blocksToClear) {
                blockStrings.add(block.toShortString());
            }
            return blockStrings.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Plan plan)) {
                return false;
            }
            return origin.equals(plan.origin)
                    && direction == plan.direction
                    && kind.equals(plan.kind)
                    && blocksToClear.equals(plan.blocksToClear);
        }

        @Override
        public int hashCode() {
            return Objects.hash(origin, direction, kind, blocksToClear);
        }
    }
}
