//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.movement;
//$$ 
//$$ import java.util.List;
//$$ import java.util.Map;
//$$ import java.util.function.BooleanSupplier;
//$$ import java.util.function.LongSupplier;
//$$ import adris.altoclef.AltoClef;
//$$ import baritone.api.pathing.movement.MovementStatus;
//$$ import baritone.api.utils.Rotation;
//$$ import baritone.api.utils.input.Input;
//$$ import baritone.pathing.movement.Movement;
//$$ import baritone.pathing.movement.MovementState;
//$$ import lavi.minecraft.find.approach.input.FindOwnedMovementInputs;
//$$ import lavi.minecraft.find.approach.path.FindFlatPathPlanner;
//$$ import lavi.minecraft.find.approach.policy.FindApproachEnvelope;
//$$ import lavi.minecraft.find.approach.policy.MinecraftFindApproachSafety;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import lavi.minecraft.find.observation.MinecraftFindObservationPort;
//$$ import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Vec3d;
//$$ 
//$$ //20260914_kpopmodder: Reuse finite native calculations/step decisions without registering any global process or worker.
//$$ final class MinecraftFindExplorationNativeSession implements FindExplorationNativeSession {
//$$     private static final Input[] MOVEMENT_KEYS = {Input.MOVE_FORWARD, Input.MOVE_BACK, Input.MOVE_LEFT,
//$$             Input.MOVE_RIGHT, Input.SPRINT};
//$$     private final MinecraftClient client;
//$$     private final MinecraftFindObservationPort observations;
//$$     private final FindLog log;
//$$     private final LongSupplier clock;
//$$     private FindObservationPort.Binding admission;
//$$     private FindFlatPathPlanner planner;
//$$     private MovementState state;
//$$     private int stepIndex, completedSteps, plannedSteps, captureColumns;
//$$     private long stepStarted;
//$$     private boolean pathValidated;
//$$ 
//$$     MinecraftFindExplorationNativeSession(MinecraftClient client, FindLog log, LongSupplier clock) {
//$$         this.client = client; this.log = log; this.clock = clock;
//$$         observations = new MinecraftFindObservationPort(client);
//$$     }
//$$     @Override public boolean matches(FindObservationPort.Binding binding) {
//$$         if (!client.isOnThread()) throw new IllegalStateException("client_thread_required");
//$$         var mod = AltoClef.getInstance();
//$$         boolean matches = client == MinecraftClient.getInstance() && observations.matches(binding)
//$$                 && mod != null && mod.getPlayer() == client.player && mod.getWorld() == client.world;
//$$         if (matches) admission = binding;
//$$         return matches;
//$$     }
//$$     @Override public boolean supportedPlayer() {
//$$         var player = client.player;
//$$         return player != null && player.isAlive() && player.isOnGround() && !player.isTouchingWater()
//$$                 && !player.isInLava() && !player.isInsideWall() && !player.isSpectator() && !player.getAbilities().flying;
//$$     }
//$$     @Override public boolean contended() {
//$$         return MinecraftFindApproachSafety.contended(AltoClef.getInstance());
//$$     }
//$$     @Override public boolean safeWaypoint(BlockPos waypoint) {
//$$         return MinecraftFindApproachSafety.footprint(client, waypoint, waypoint);
//$$     }
//$$     @Override public boolean movementKeysFree(FindOwnedMovementInputs owned) {
//$$         var mod = AltoClef.getInstance();
//$$         if (mod == null || mod.getInputControls() == null || mod.getClientBaritone() == null) return false;
//$$         for (Input input : MOVEMENT_KEYS) {
//$$             if (mod.getInputControls().isHeldDown(input)) return false;
//$$             if (mod.getClientBaritone().getInputOverrideHandler().isInputForcedDown(input)
//$$                     && (owned == null || !owned.owns(input))) return false;
//$$         }
//$$         return true;
//$$     }
//$$     @Override public BlockPos playerBlock() { return client.player.getBlockPos().toImmutable(); }
//$$     @Override public Vec3d playerPosition() { return client.player.getPos(); }
//$$     @Override public double horizontalVelocitySquared() { return client.player.getVelocity().horizontalLengthSquared(); }
//$$     @Override public ForcedInputLeaseChannel inputChannel() {
//$$         var mod = AltoClef.getInstance();
//$$         if (mod == null || mod.getClientBaritone() == null) return null;
//$$         Object handler = mod.getClientBaritone().getInputOverrideHandler();
//$$         return handler instanceof ForcedInputLeaseChannel channel ? channel : null;
//$$     }
//$$     @Override public void start(FindObservationPort.Binding local, BlockPos start, BlockPos waypoint) {
//$$         if (planner != null) throw new IllegalStateException("exploration_native_route_already_active");
//$$         int previousSteps = completedSteps, previousColumns = captureColumns;
//$$         planner = new FindFlatPathPlanner(client, local, start,
//$$                 new Vec3d(waypoint.getX() + .5, waypoint.getY(), waypoint.getZ() + .5),
//$$                 new FindApproachEnvelope(0, .75), 64, log);
//$$         state = null; stepIndex = 0; completedSteps = 0; plannedSteps = 0; captureColumns = 0; pathValidated = false;
//$$         log.event("EXPLORATION_PLAN_STARTED", "radius", 64, "maxPathSteps", FindFlatPathPlanner.MAX_PATH_STEPS,
//$$                 "planningOriginX", local.x(), "planningOriginY", local.y(), "planningOriginZ", local.z(),
//$$                 "stepsBefore", previousSteps, "stepsDelta", -previousSteps, "stepsAfter", 0,
//$$                 "captureColumnsBefore", previousColumns, "captureColumnsAfter", 0,
//$$                 "globalGoalInstalled", false, "nativeWorkerSubmitted", false);
//$$     }
//$$     @Override public Decision tick(BooleanSupplier withinBudget) {
//$$         if (planner == null) return Decision.waiting();
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         try { planner.tick(); }
//$$         catch (IllegalStateException exception) {
//$$             if ("find_native_planner_read_limit".equals(exception.getMessage()))
//$$                 return Decision.rejected("native_planner_read_limit");
//$$             throw exception;
//$$         }
//$$         // Capture/calculation is synchronous and soft-bounded by the existing native owner.
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         captureColumns = planner.captureColumns();
//$$         if (planner.failure() != null) return Decision.rejected(planner.failure());
//$$         List<Movement> path = planner.path();
//$$         if (path == null) return Decision.waiting();
//$$         plannedSteps = path.size();
//$$         if (!pathValidated) {
//$$             for (Movement movement : path) {
//$$                 if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$                 if (!insideAdmissionFootprint(movement.getSrc(), movement.getDest()))
//$$                     return Decision.rejected("exploration_native_route_outside_admission_scope");
//$$             }
//$$             pathValidated = true;
//$$         }
//$$         if (stepIndex >= path.size()) return Decision.rejected("exploration_waypoint_not_satisfied_at_native_path_end");
//$$         Movement movement = path.get(stepIndex);
//$$         if (!MinecraftFindApproachSafety.footprint(client, movement.getSrc(), movement.getDest())
//$$                 || !movement.getValidPositions().contains(client.player.getBlockPos())
//$$                 || !insideAdmissionFootprint(movement.getSrc(), movement.getDest()))
//$$             return Decision.rejected("native_step_no_longer_safe_or_bound");
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         if (!MinecraftFindApproachSafety.nativeFootprintMatches(client, AltoClef.getInstance(),
//$$                 movement.getSrc(), movement.getDest()))
//$$             return Decision.rejected("native_cached_or_world_footprint_not_live_proven");
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         if (state == null) {
//$$             state = new MovementState(); stepStarted = clock.getAsLong();
//$$             log.event("EXPLORATION_STEP_STARTED", "stepIndex", stepIndex,
//$$                     "nativeKind", movement.getClass().getSimpleName());
//$$         }
//$$         if (clock.getAsLong() - stepStarted >= 10_000_000_000L)
//$$             return Decision.rejected("native_step_no_progress_timeout");
//$$         var mod = AltoClef.getInstance();
//$$         if (!movement.toBreak(mod.getClientBaritone().bsi).isEmpty()
//$$                 || !movement.toPlace(mod.getClientBaritone().bsi).isEmpty())
//$$             return Decision.rejected("native_step_requires_world_mutation");
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         if (!matches(admission)) return Decision.rejected("world_or_player_binding_changed");
//$$         if (!supportedPlayer()) return Decision.rejected("unsupported_player_movement_state");
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         state.getInputStates().clear();
//$$         MovementState next = movement.updateState(state);
//$$         // No Movement.update() call, native input setter, native goal or path executor is involved.
//$$         if (!withinBudget.getAsBoolean()) return Decision.rejected("elapsed_budget_exhausted");
//$$         state = next;
//$$         Map<Input, Boolean> staged = Map.copyOf(state.getInputStates());
//$$         if (FindOwnedMovementInputs.hasForbiddenInput(staged))
//$$             return Decision.rejected("native_step_requested_forbidden_input");
//$$         MovementStatus status = state.getStatus();
//$$         if (status.isComplete() && status != MovementStatus.SUCCESS)
//$$             return Decision.rejected("native_step_" + status.name().toLowerCase(java.util.Locale.ROOT));
//$$         if (status == MovementStatus.SUCCESS) {
//$$             int before = completedSteps; completedSteps++; stepIndex++; state = null;
//$$             log.event("EXPLORATION_STEP_COMPLETED", "stepsBefore", before, "stepsDelta", 1,
//$$                     "stepsAfter", completedSteps, "routeStepsCompleted", stepIndex);
//$$             return new Decision(null, Map.of(), null, true);
//$$         }
//$$         Rotation rotation = state.getTarget().getRotation().orElse(null);
//$$         if (rotation != null && (!Float.isFinite(rotation.getYaw()) || !Float.isFinite(rotation.getPitch())))
//$$             return Decision.rejected("invalid_native_rotation");
//$$         return new Decision(null, staged, rotation, false);
//$$     }
//$$     private boolean insideAdmissionFootprint(BlockPos source, BlockPos destination) {
//$$         if (admission == null) return false;
//$$         if (source.getY() != destination.getY() || Math.abs(source.getX() - destination.getX()) > 1
//$$                 || Math.abs(source.getZ() - destination.getZ()) > 1) return false;
//$$         for (int x = Math.min(source.getX(), destination.getX()) - 1; x <= Math.max(source.getX(), destination.getX()) + 1; x++) {
//$$             for (int z = Math.min(source.getZ(), destination.getZ()) - 1; z <= Math.max(source.getZ(), destination.getZ()) + 1; z++) {
//$$                 double dx = x + .5 - admission.x(), dz = z + .5 - admission.z();
//$$                 if (dx * dx + dz * dz > 512 * 512) return false;
//$$             }
//$$         }
//$$         return true;
//$$     }
//$$     @Override public void look(Rotation rotation) {
//$$         AltoClef.getInstance().getInputControls().forceLook(rotation.getYaw(), rotation.getPitch());
//$$     }
//$$     @Override public void suspend() {
//$$         try {
//$$             FindFlatPathPlanner stopping = planner;
//$$             if (stopping != null) {
//$$                 captureColumns = stopping.captureColumns();
//$$                 List<Movement> finalPath = stopping.path();
//$$                 if (finalPath != null) plannedSteps = finalPath.size();
//$$             }
//$$         } catch (RuntimeException unavailable) {
//$$             captureColumns = -1; plannedSteps = -1;
//$$         } finally {
//$$             planner = null; state = null; pathValidated = false;
//$$         }
//$$     }
//$$     @Override public boolean quiet() { return planner == null && state == null; }
//$$     @Override public FindExplorationMovementPort.Progress progress() {
//$$         return new FindExplorationMovementPort.Progress(completedSteps, plannedSteps, captureColumns, false);
//$$     }
//$$ }
//#endif
