//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.movement;
//$$ 
//$$ import java.util.HashSet;
//$$ import java.util.Objects;
//$$ import java.util.Set;
//$$ import java.util.function.BooleanSupplier;
//$$ import java.util.function.LongSupplier;
//$$ import baritone.api.utils.input.Input;
//$$ import lavi.minecraft.find.approach.input.FindOwnedMovementInputs;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Vec3d;
//$$ 
//$$ //20260914_kpopmodder: This owner fences synchronous native decisions and retires only its own flat-route resources.
//$$ public final class MinecraftFindExplorationMovement implements FindExplorationMovementPort {
//$$     private final FindExplorationNativeSession nativeSession;
//$$     private final FindLog log;
//$$     private final Set<Long> visitedWaypoints = new HashSet<>();
//$$     private FindObservationPort.Binding admission;
//$$     private BooleanSupplier withinBudget;
//$$     private FindOwnedMovementInputs inputs;
//$$     private BlockPos waypoint;
//$$     private Vec3d routeStart;
//$$     private long generation;
//$$     private int stableTicks;
//$$     private boolean active, movementInputIssued;
//$$     private String failure;
//$$ 
//$$     public MinecraftFindExplorationMovement(MinecraftClient client, FindLog log) {
//$$         this(client, log, System::nanoTime);
//$$     }
//$$     public MinecraftFindExplorationMovement(MinecraftClient client, FindLog log, LongSupplier clock) {
//$$         this(new MinecraftFindExplorationNativeSession(client, log, clock), log);
//$$     }
//$$     MinecraftFindExplorationMovement(FindExplorationNativeSession nativeSession, FindLog log) {
//$$         this.nativeSession = Objects.requireNonNull(nativeSession); this.log = Objects.requireNonNull(log);
//$$     }
//$$     @Override public void begin(FindObservationPort.Binding admission, BooleanSupplier withinBudget) {
//$$         if (!quiet()) throw new IllegalStateException("exploration_previous_route_not_quiet");
//$$         if (this.admission != null && !sameAdmission(this.admission, admission))
//$$             throw new IllegalArgumentException("exploration_admission_binding_changed");
//$$         this.admission = Objects.requireNonNull(admission);
//$$         this.withinBudget = Objects.requireNonNull(withinBudget);
//$$         boolean previouslyIssued = movementInputIssued;
//$$         movementInputIssued = false;
//$$         failure = null; stableTicks = 0; active = true; long entered = ++generation;
//$$         log.event("EXPLORATION_ROUTE_STARTED", "routeGeneration", generation, "retainedWaypoints", visitedWaypoints.size(),
//$$                 "movementInputIssuedBefore", previouslyIssued, "movementInputIssuedAfter", false,
//$$                 "inputIssueResetReason", "explicit_new_route",
//$$                 "movementOwner", "FIND_LOCAL_NATIVE_FLAT_STEPS", "globalGoalInstalled", false,
//$$                 "nativeWorkerSubmitted", false);
//$$         try {
//$$             if (!runnable(entered)) { expiredOrStale(entered); return; }
//$$             if (!nativeSession.matches(admission)) { reject("world_or_player_binding_changed"); return; }
//$$             if (!runnable(entered)) { expiredOrStale(entered); return; }
//$$             if (!nativeSession.supportedPlayer()) { reject("unsupported_player_movement_state"); return; }
//$$             if (!nativeSession.movementKeysFree(null)) { reject("preexisting_movement_input_contended"); return; }
//$$             var channel = nativeSession.inputChannel();
//$$             if (!runnable(entered)) { expiredOrStale(entered); return; }
//$$             if (channel == null) { reject("forced_input_lease_capability_unavailable"); return; }
//$$             inputs = new FindOwnedMovementInputs(channel, log);
//$$             BlockPos start = nativeSession.playerBlock();
//$$             routeStart = nativeSession.playerPosition();
//$$             if (!finite(routeStart)) { reject("exploration_position_unavailable"); return; }
//$$             if (!insideAdmission(routeStart.x, routeStart.z)) { reject("exploration_displacement_limit_exhausted"); return; }
//$$             if (visitedWaypoints.size() >= 8) { reject("exploration_route_start_limit_exhausted"); return; }
//$$             var selected = FindLoadedWaypointSelector.select(admission, start, visitedWaypoints,
//$$                     nativeSession::safeWaypoint, () -> runnable(entered));
//$$             if (!runnable(entered)) { expiredOrStale(entered); return; }
//$$             if (selected.waypoint() == null) {
//$$                 log.event("EXPLORATION_WAYPOINT_REJECTED", "routeGeneration", generation,
//$$                         "candidatesVisited", selected.candidatesVisited(), "safeCandidates", selected.safeCandidates(),
//$$                         "reason", selected.reason());
//$$                 reject(selected.reason()); return;
//$$             }
//$$             waypoint = selected.waypoint().toImmutable();
//$$             if (!insideAdmission(waypoint.getX() + .5, waypoint.getZ() + .5)) {
//$$                 reject("exploration_displacement_limit_exhausted"); return;
//$$             }
//$$             int waypointsBefore = visitedWaypoints.size();
//$$             visitedWaypoints.add(waypoint.asLong());
//$$             log.event("EXPLORATION_WAYPOINT_SELECTED", "routeGeneration", generation,
//$$                     "waypointsBefore", waypointsBefore, "waypointsDelta", 1, "waypointsAfter", visitedWaypoints.size(),
//$$                     "candidatesVisited", selected.candidatesVisited(), "safeCandidates", selected.safeCandidates(),
//$$                     "startX", routeStart.x, "startY", routeStart.y, "startZ", routeStart.z,
//$$                     "waypointX", waypoint.getX(), "waypointY", waypoint.getY(), "waypointZ", waypoint.getZ(),
//$$                     "minimumTravel", FindLoadedWaypointSelector.MINIMUM_TRAVEL, "maximumLocalRadius", 48,
//$$                     "reason", selected.reason());
//$$             if (!runnable(entered)) { expiredOrStale(entered); return; }
//$$             var local = new FindObservationPort.Binding(admission.world(), admission.player(), admission.dimension(),
//$$                     routeStart.x, routeStart.y, routeStart.z, admission.bottomY(), admission.topY());
//$$             nativeSession.start(local, start, waypoint);
//$$             if (!runnable(entered)) expiredOrStale(entered);
//$$         } catch (RuntimeException exception) {
//$$             rejectReadFailure(exception);
//$$         }
//$$     }
//$$     @Override public Step tick() {
//$$         if (!active) return step(false);
//$$         long entered = generation;
//$$         try {
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (!nativeSession.matches(admission)) return reject("world_or_player_binding_changed");
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (!nativeSession.supportedPlayer()) return reject("unsupported_player_movement_state");
//$$             Vec3d current = nativeSession.playerPosition();
//$$             if (!finite(current)) return reject("exploration_position_unavailable");
//$$             if (!insideAdmission(current.x, current.z)) return reject("exploration_displacement_limit_exhausted");
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (nativeSession.contended()) {
//$$                 if (inputs != null) inputs.release();
//$$                 stableTicks = 0;
//$$                 return step(false);
//$$             }
//$$             if (waypoint != null && nativeSession.playerBlock().equals(waypoint)) {
//$$                 if (!nativeSession.safeWaypoint(waypoint)) return reject("native_step_no_longer_safe_or_bound");
//$$                 double travelledX = current.x - routeStart.x, travelledZ = current.z - routeStart.z;
//$$                 if (travelledX * travelledX + travelledZ * travelledZ
//$$                         < FindLoadedWaypointSelector.MINIMUM_TRAVEL * FindLoadedWaypointSelector.MINIMUM_TRAVEL)
//$$                     return reject("exploration_waypoint_real_travel_not_verified");
//$$                 double velocity = nativeSession.horizontalVelocitySquared();
//$$                 if (!Double.isFinite(velocity)) return reject("exploration_position_unavailable");
//$$                 if (inputs != null) inputs.release();
//$$                 stableTicks = velocity <= .0025 ? stableTicks + 1 : 0;
//$$                 if (!runnable(entered)) return expiredOrStale(entered);
//$$                 if (stableTicks >= 2) {
//$$                     log.event("EXPLORATION_WAYPOINT_REACHED", "routeGeneration", generation,
//$$                             "waypointX", waypoint.getX(), "waypointY", waypoint.getY(), "waypointZ", waypoint.getZ(),
//$$                             "horizontalTravelSquared", travelledX * travelledX + travelledZ * travelledZ,
//$$                             "horizontalVelocitySquared", velocity, "stableTicks", stableTicks,
//$$                             "reason", "verified_loaded_waypoint_reached");
//$$                     if (!runnable(entered)) return expiredOrStale(entered);
//$$                     suspend();
//$$                     return step(true);
//$$                 }
//$$                 return step(false);
//$$             }
//$$             stableTicks = 0;
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             var decision = nativeSession.tick(() -> runnable(entered));
//$$             // Native calculations/getters may reenter retirement. Never commit their late staged decisions.
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (!nativeSession.matches(admission)) return reject("world_or_player_binding_changed");
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (decision.reason() != null) return reject(decision.reason());
//$$             if (FindOwnedMovementInputs.hasForbiddenInput(decision.inputs()))
//$$                 return reject("native_step_requested_forbidden_input");
//$$             if (decision.stepCompleted()) {
//$$                 if (inputs != null) inputs.release();
//$$                 return step(false);
//$$             }
//$$             if (!nativeSession.movementKeysFree(inputs)) return reject("external_movement_input_contended");
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (!nativeSession.matches(admission)) return reject("world_or_player_binding_changed");
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (decision.rotation() != null) {
//$$                 var rotation = decision.rotation();
//$$                 if (!Float.isFinite(rotation.getYaw()) || !Float.isFinite(rotation.getPitch()))
//$$                     return reject("invalid_native_rotation");
//$$                 nativeSession.look(rotation);
//$$             }
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (!nativeSession.matches(admission)) return reject("world_or_player_binding_changed");
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             if (inputs == null || !inputs.apply(decision.inputs()))
//$$                 return reject("forced_input_lease_rejected_or_superseded");
//$$             movementInputIssued |= decision.inputs().entrySet().stream().anyMatch(entry ->
//$$                     Boolean.TRUE.equals(entry.getValue()) && entry.getKey() != Input.SPRINT);
//$$             if (!runnable(entered)) return expiredOrStale(entered);
//$$             return step(false);
//$$         } catch (RuntimeException exception) {
//$$             return rejectReadFailure(exception);
//$$         }
//$$     }
//$$     private boolean runnable(long entered) {
//$$         if (!active || generation != entered || withinBudget == null) return false;
//$$         boolean allowed = withinBudget.getAsBoolean();
//$$         return allowed && active && generation == entered;
//$$     }
//$$     private Step expiredOrStale(long entered) {
//$$         if (active && generation == entered) return reject("elapsed_budget_exhausted");
//$$         return step(false);
//$$     }
//$$     private Step rejectReadFailure(RuntimeException exception) {
//$$         log.event("EXPLORATION_MOVEMENT_READ_FAILED", "routeGeneration", generation,
//$$                 "failureType", exception.getClass().getName(), "reason", "exploration_movement_read_failed");
//$$         return reject("exploration_movement_read_failed");
//$$     }
//$$     private Step reject(String reason) {
//$$         failure = reason;
//$$         log.event("EXPLORATION_MOVEMENT_REJECTED", "routeGeneration", generation, "reason", reason);
//$$         suspend();
//$$         return step(false);
//$$     }
//$$     @Override public void suspend() {
//$$         boolean wasActive = active;
//$$         active = false; generation++; stableTicks = 0; waypoint = null; routeStart = null;
//$$         try { nativeSession.suspend(); }
//$$         finally { if (inputs != null) inputs.release(); }
//$$         if (wasActive) log.event("EXPLORATION_MOVEMENT_SUSPENDED", "routeGeneration", generation - 1,
//$$                 "nativeSessionQuiet", nativeSession.quiet(), "ownedInputsQuiet", inputs == null || inputs.quiet(),
//$$                 "pendingNativeCallback", false, "nativeWorkerSubmitted", false);
//$$     }
//$$     @Override public boolean quiet() {
//$$         return !active && nativeSession.quiet() && (inputs == null || inputs.quiet());
//$$     }
//$$     private Step step(boolean reached) {
//$$         Progress nativeProgress;
//$$         try { nativeProgress = nativeSession.progress(); }
//$$         catch (RuntimeException ignored) { nativeProgress = new Progress(-1, -1, -1, false); }
//$$         Progress progress = new Progress(nativeProgress.completedSteps(), nativeProgress.plannedSteps(),
//$$                 nativeProgress.captureColumns(), movementInputIssued);
//$$         return new Step(failure, reached, quiet(), progress);
//$$     }
//$$     private boolean insideAdmission(double x, double z) {
//$$         double dx = x - admission.x(), dz = z - admission.z();
//$$         return Double.isFinite(dx) && Double.isFinite(dz) && dx * dx + dz * dz <= 512 * 512;
//$$     }
//$$     private static boolean finite(Vec3d position) {
//$$         return position != null && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
//$$     }
//$$     private static boolean sameAdmission(FindObservationPort.Binding first, FindObservationPort.Binding second) {
//$$         return second != null && first.world() == second.world() && first.player() == second.player()
//$$                 && Objects.equals(first.dimension(), second.dimension()) && first.x() == second.x()
//$$                 && first.y() == second.y() && first.z() == second.z();
//$$     }
//$$ }
//#endif
