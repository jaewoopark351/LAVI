//#if MC == 12001
//$$ package lavi.minecraft.find.approach.path;
//$$
//$$ import java.util.ArrayList;
//$$ import java.util.HashSet;
//$$ import java.util.List;
//$$ import java.util.Set;
//$$ import java.util.Comparator;
//$$ import adris.altoclef.AltoClef;
//$$ import baritone.Baritone;
//$$ import baritone.api.pathing.goals.Goal;
//$$ import baritone.api.pathing.goals.GoalBlock;
//$$ import baritone.api.pathing.goals.GoalComposite;
//$$ import baritone.api.utils.BetterBlockPos;
//$$ import baritone.api.utils.PathCalculationResult;
//$$ import baritone.pathing.calc.AStarPathFinder;
//$$ import baritone.pathing.movement.Movement;
//$$ import baritone.pathing.movement.movements.MovementDiagonal;
//$$ import baritone.pathing.movement.movements.MovementTraverse;
//$$ import baritone.utils.pathing.Favoring;
//$$ import lavi.minecraft.find.approach.policy.FindApproachEnvelope;
//$$ import lavi.minecraft.find.approach.policy.MinecraftFindApproachSafety;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Vec3d;
//$$
//$$ //20260914_kpopmodder: Capture incrementally, then use one finite native calculation without installing a global goal.
//$$ public final class FindFlatPathPlanner {
//$$     public static final int MAX_PATH_STEPS = 128, CAPTURE_COLUMNS_PER_TICK = 128;
//$$     public static final long CAPTURE_SOFT_NANOS = 2_000_000;
//$$     private final MinecraftClient client;
//$$     private final FindObservationPort.Binding binding;
//$$     private final BlockPos start;
//$$     private final Vec3d target;
//$$     private final FindApproachEnvelope envelope;
//$$     private final FindLog log;
//$$     private final int radius, minX, maxX, minZ, maxZ;
//$$     private final Set<Long> cells = new HashSet<>();
//$$     private int x, z, visited;
//$$     private boolean captured, planned;
//$$     private List<Movement> path;
//$$     private String failure;
//$$
//$$     public FindFlatPathPlanner(MinecraftClient client, FindObservationPort.Binding binding, BlockPos start,
//$$                                Vec3d target, FindApproachEnvelope envelope, int radius, FindLog log) {
//$$         this.client = client; this.binding = binding; this.start = start.toImmutable();
//$$         this.target = target; this.envelope = envelope; this.radius = radius; this.log = log;
//$$         int originX = (int)Math.floor(binding.x()), originZ = (int)Math.floor(binding.z());
//$$         // Collision margins are observed, but path nodes remain inside the original FIND radius.
//$$         minX = originX - radius; maxX = originX + radius; minZ = originZ - radius; maxZ = originZ + radius;
//$$         x = minX; z = minZ;
//$$     }
//$$     public void tick() {
//$$         if (failure != null || path != null) return;
//$$         if (!captured) {
//$$             long began = System.nanoTime();
//$$             int count = 0;
//$$             while (!captured && count++ < CAPTURE_COLUMNS_PER_TICK) {
//$$                 BlockPos position = new BlockPos(x, start.getY(), z);
//$$                 double dx = x + .5 - binding.x(), dy = start.getY() - binding.y(), dz = z + .5 - binding.z();
//$$                 if (dx * dx + dy * dy + dz * dz <= radius * radius
//$$                         && MinecraftFindApproachSafety.footprint(client, position, position)) cells.add(position.asLong());
//$$                 visited++;
//$$                 if (++z > maxZ) { z = minZ; if (++x > maxX) captured = true; }
//$$                 if (System.nanoTime() - began >= CAPTURE_SOFT_NANOS) break;
//$$             }
//$$             if (!captured) return;
//$$             log.event("APPROACH_CAPTURE_COMPLETE", "columnsVisited", visited, "safeCells", cells.size(),
//$$                     "radius", radius, "floorY", start.getY());
//$$         }
//$$         if (planned) return;
//$$         planned = true;
//$$         if (Boolean.TRUE.equals(Baritone.settings().slowPath.value)) { fail("native_slow_path_setting_unsupported"); return; }
//$$         if (!cells.contains(start.asLong())) { fail("start_not_in_supported_loaded_flat_scope"); return; }
//$$         List<BlockPos> goalCells = new ArrayList<>();
//$$         for (long packed : cells) {
//$$             BlockPos cell = BlockPos.fromLong(packed);
//$$             Vec3d center = new Vec3d(cell.getX() + .5, cell.getY(), cell.getZ() + .5);
//$$             if (envelope.contains(center.squaredDistanceTo(target))) goalCells.add(cell);
//$$         }
//$$         if (goalCells.isEmpty()) { fail("no_supported_safe_standoff_cell"); return; }
//$$         goalCells.sort(Comparator.comparingDouble((BlockPos cell) -> cell.getSquaredDistance(start))
//$$                 .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ));
//$$         List<Goal> goals = goalCells.stream().limit(64).map(GoalBlock::new).map(goal -> (Goal)goal).toList();
//$$         var baritone = AltoClef.getInstance().getClientBaritone();
//$$         var context = new FindFlatPathContext(baritone, cells, start.getY());
//$$         Goal goal = new GoalComposite(goals.toArray(Goal[]::new));
//$$         BetterBlockPos nativeStart = new BetterBlockPos(start);
//$$         AStarPathFinder finder = new AStarPathFinder(nativeStart, start.getX(), start.getY(), start.getZ(),
//$$                 goal, new Favoring(null, context), context);
//$$         long before = System.nanoTime();
//$$         // Native checks occur between node batches: 5ms is a soft limit, finite context reads are separate.
//$$         PathCalculationResult result = finder.calculate(5, 5);
//$$         log.event("APPROACH_PLAN_RESULT", "nativeResult", result.getType().name(), "contextReads", context.reads(),
//$$                 "softBudgetMillis", 5, "actualElapsedNanos", System.nanoTime() - before);
//$$         if (result.getType() != PathCalculationResult.Type.SUCCESS_TO_GOAL || result.getPath().isEmpty()) {
//$$             fail(context.reads() > FindFlatPathContext.MAX_READS ? "native_planner_read_limit" : "native_finite_plan_unreachable"); return;
//$$         }
//$$         List<Movement> approved = new ArrayList<>();
//$$         for (var movement : result.getPath().get().movements()) {
//$$             if (movement.getClass() != MovementTraverse.class && movement.getClass() != MovementDiagonal.class) {
//$$                 fail("unsupported_native_movement_kind"); return;
//$$             }
//$$             Movement nativeMovement = (Movement) movement;
//$$             if (approved.size() >= MAX_PATH_STEPS || nativeMovement.getSrc().getY() != start.getY()
//$$                     || nativeMovement.getDest().getY() != start.getY()
//$$                     || !cells.contains(nativeMovement.getSrc().asLong()) || !cells.contains(nativeMovement.getDest().asLong())) {
//$$                 fail("native_path_bounds_exhausted"); return;
//$$             }
//$$             approved.add(nativeMovement);
//$$         }
//$$         path = List.copyOf(approved);
//$$         log.event("APPROACH_PLAN_ADMITTED", "steps", path.size(), "replanAllowed", false,
//$$                 "globalGoalInstalled", false, "nativeMovementUpdateCalled", false);
//$$     }
//$$     public List<Movement> path() { return path; }
//$$     public int captureColumns() { return visited; }
//$$     public String failure() { return failure; }
//$$     private void fail(String reason) { failure = reason; log.event("APPROACH_PLAN_REJECTED", "reason", reason); }
//$$ }
//#endif
