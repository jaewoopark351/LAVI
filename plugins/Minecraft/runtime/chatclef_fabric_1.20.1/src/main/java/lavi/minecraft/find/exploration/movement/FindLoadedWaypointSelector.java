//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.movement;
//$$ 
//$$ import java.util.Set;
//$$ import java.util.function.BooleanSupplier;
//$$ import java.util.function.Predicate;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import net.minecraft.util.math.BlockPos;
//$$ 
//$$ //20260914_kpopmodder: A finite loaded-cell frontier policy does not query native global exploration/cache state.
//$$ final class FindLoadedWaypointSelector {
//$$     static final int MAX_CANDIDATES = 24;
//$$     static final double MINIMUM_TRAVEL = 16;
//$$     private static final int[] DISTANCES = {24, 48};
//$$     private static final int[][] DIRECTIONS = {{1,0},{0,1},{-1,0},{0,-1},{1,1},{-1,1},{-1,-1},{1,-1}};
//$$     record Selection(BlockPos waypoint, int candidatesVisited, int safeCandidates, String reason) { }
//$$ 
//$$     static Selection select(FindObservationPort.Binding admission, BlockPos start, Set<Long> visited,
//$$                             Predicate<BlockPos> safe, BooleanSupplier withinBudget) {
//$$         BlockPos selected = null;
//$$         double bestNovelty = -1, bestAdmissionDistance = -1;
//$$         int visits = 0, safeCount = 0;
//$$         for (int distance : DISTANCES) {
//$$             for (int[] direction : DIRECTIONS) {
//$$                 if (!withinBudget.getAsBoolean()) return new Selection(null, visits, safeCount, "elapsed_budget_exhausted");
//$$                 if (visits >= MAX_CANDIDATES) return new Selection(null, visits, safeCount, "exploration_waypoint_work_limit");
//$$                 double scale = distance / Math.hypot(direction[0], direction[1]);
//$$                 BlockPos candidate = start.add(direction[0] * (int)Math.floor(scale), 0,
//$$                         direction[1] * (int)Math.floor(scale));
//$$                 visits++;
//$$                 double dx = candidate.getX() + .5 - (start.getX() + .5);
//$$                 double dz = candidate.getZ() + .5 - (start.getZ() + .5);
//$$                 // Rounding cannot expand the promised 48-block local horizontal admission radius.
//$$                 if (dx * dx + dz * dz > 48 * 48 || dx * dx + dz * dz < MINIMUM_TRAVEL * MINIMUM_TRAVEL
//$$                         || visited.contains(candidate.asLong())) continue;
//$$                 double adx = candidate.getX() + .5 - admission.x(), adz = candidate.getZ() + .5 - admission.z();
//$$                 double admissionDistance = adx * adx + adz * adz;
//$$                 if (admissionDistance > 512 * 512) continue;
//$$                 boolean supported = safe.test(candidate);
//$$                 if (!withinBudget.getAsBoolean()) return new Selection(null, visits, safeCount, "elapsed_budget_exhausted");
//$$                 if (!supported) continue;
//$$                 safeCount++;
//$$                 double novelty = admissionDistance;
//$$                 for (long packed : visited) {
//$$                     BlockPos previous = BlockPos.fromLong(packed);
//$$                     double px = candidate.getX() - previous.getX(), pz = candidate.getZ() - previous.getZ();
//$$                     novelty = Math.min(novelty, px * px + pz * pz);
//$$                 }
//$$                 if (selected == null || novelty > bestNovelty || novelty == bestNovelty
//$$                         && (admissionDistance > bestAdmissionDistance || admissionDistance == bestAdmissionDistance
//$$                         && compare(candidate, selected) < 0)) {
//$$                     selected = candidate; bestNovelty = novelty; bestAdmissionDistance = admissionDistance;
//$$                 }
//$$             }
//$$         }
//$$         return new Selection(selected, visits, safeCount,
//$$                 selected == null ? "no_supported_loaded_exploration_waypoint" : "loaded_waypoint_selected");
//$$     }
//$$     private static int compare(BlockPos first, BlockPos second) {
//$$         int x = Integer.compare(first.getX(), second.getX());
//$$         return x != 0 ? x : Integer.compare(first.getZ(), second.getZ());
//$$     }
//$$ }
//#endif
