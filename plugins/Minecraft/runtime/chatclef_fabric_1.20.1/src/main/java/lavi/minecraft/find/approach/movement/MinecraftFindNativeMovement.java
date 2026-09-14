//#if MC == 12001
//$$ package lavi.minecraft.find.approach.movement;
//$$
//$$ import java.util.List;
//$$ import java.util.Map;
//$$ import java.util.function.BooleanSupplier;
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
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import lavi.minecraft.find.observation.MinecraftFindObservationPort;
//$$ import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.util.math.Vec3d;
//$$
//$$ //20260914_kpopmodder: Delegate native flat step decisions; never execute Movement.update or its global cleanup.
//$$ public final class MinecraftFindNativeMovement implements FindApproachMovementPort {
//$$     private final MinecraftClient client;
//$$     private final MinecraftFindObservationPort observations;
//$$     private final FindLog log;
//$$     private final BooleanSupplier withinBudget;
//$$     private FindObservationPort.Binding binding;
//$$     private FindRequest request;
//$$     private FindCandidate originalCandidate;
//$$     private FindApproachEnvelope envelope;
//$$     private FindFlatPathPlanner planner;
//$$     private FindOwnedMovementInputs inputs;
//$$     private MovementState state;
//$$     private int stepIndex, stableTicks;
//$$     private boolean began, moved;
//$$     private long stepStarted;
//$$     private String failure;
//$$
//$$     public MinecraftFindNativeMovement(MinecraftClient client, FindLog log) {
//$$         this(client, log, () -> true);
//$$     }
//$$     public MinecraftFindNativeMovement(MinecraftClient client, FindLog log, BooleanSupplier withinBudget) {
//$$         this.client = client; this.log = log; observations = new MinecraftFindObservationPort(client);
//$$         this.withinBudget = withinBudget;
//$$     }
//$$     @Override public void begin(FindObservationPort.Binding binding, FindRequest request, FindCandidate candidate) {
//$$         if (began) throw new IllegalStateException("find_approach_movement_already_bound");
//$$         began = true; this.binding = binding; this.request = request; originalCandidate = candidate;
//$$         envelope = MinecraftFindApproachSafety.envelope(client, request, candidate);
//$$         if (envelope == null) failure = "unsupported_target_safety_profile";
//$$         Object handler = AltoClef.getInstance().getClientBaritone().getInputOverrideHandler();
//$$         if (!(handler instanceof ForcedInputLeaseChannel channel)) failure = "forced_input_lease_capability_unavailable";
//$$         else inputs = new FindOwnedMovementInputs(channel, log);
//$$         log.event("APPROACH_STARTED", "targetKind", request.kind(), "dimension", binding.dimension(),
//$$                 "safetyMinimum", envelope == null ? "UNSUPPORTED" : envelope.minimum(),
//$$                 "safetyMaximum", envelope == null ? "UNSUPPORTED" : envelope.maximum(),
//$$                 "movementOwner", "FIND_LOCAL_NATIVE_FLAT_STEPS", "pathOwner", "FIND_LOCAL_CALCULATED_PATH",
//$$                 "globalGoalOwner", "NONE", "candidateIdentityDigest", candidate.identityDigest());
//$$     }
//$$     @Override public Step tick() {
//$$         if (!client.isOnThread()) throw new IllegalStateException("client_thread_required");
//$$         if (!withinBudget.getAsBoolean()) return failed("elapsed_budget_exhausted");
//$$         if (failure != null) return failed(failure);
//$$         if (!observations.matches(binding) || !client.player.isAlive()) return failed("world_or_player_binding_changed");
//$$         FindCandidate current = observations.revalidate(request, binding, originalCandidate);
//$$         if (current == null) return failed("selected_target_lost");
//$$         Vec3d target = MinecraftFindApproachSafety.target(client, request, current);
//$$         if (target == null) return failed("selected_target_lost");
//$$         var player = client.player;
//$$         if (MinecraftFindApproachSafety.contended(AltoClef.getInstance())) {
//$$             suspend(); return Step.waiting();
//$$         }
//$$         if (!player.isOnGround() || player.isTouchingWater() || player.isInLava() || player.isInsideWall()
//$$                 || player.isSpectator() || player.getAbilities().flying) return failed("unsupported_player_movement_state");
//$$         double distance = player.getPos().squaredDistanceTo(target);
//$$         if (distance < envelope.minimum() * envelope.minimum()) return failed("target_inside_minimum_standoff");
//$$         if (envelope.contains(distance) && visible(target, current)) {
//$$             inputs.release();
//$$             if (player.getVelocity().horizontalLengthSquared() <= .0025 && ++stableTicks >= 2) {
//$$                 return new Step(moved ? "FOUND_AND_IN_SAFE_RANGE" : "ALREADY_IN_SAFE_RANGE", true, current);
//$$             }
//$$             return Step.waiting();
//$$         }
//$$         stableTicks = 0;
//$$         if (planner == null) {
//$$             if (!movementKeysFree()) return failed("preexisting_movement_input_contended");
//$$             planner = new FindFlatPathPlanner(client, binding, player.getBlockPos(), target, envelope,
//$$                     request.kind().equals("block") ? 32 : 64, log);
//$$             log.event("APPROACH_PLAN_STARTED", "originalDeadlinePreserved", true, "replanCount", 0,
//$$                     "startX", player.getBlockX(), "startY", player.getBlockY(), "startZ", player.getBlockZ());
//$$         }
//$$         planner.tick();
//$$         if (planner.failure() != null) return failed(planner.failure());
//$$         List<Movement> path = planner.path();
//$$         if (path == null) return Step.waiting();
//$$         if (stepIndex >= path.size()) return failed("safe_range_not_satisfied_at_native_path_end");
//$$         Movement movement = path.get(stepIndex);
//$$         if (!MinecraftFindApproachSafety.footprint(client, movement.getSrc(), movement.getDest())
//$$                 || !movement.getValidPositions().contains(player.getBlockPos())) return failed("native_step_no_longer_safe_or_bound");
//$$         if (!MinecraftFindApproachSafety.nativeFootprintMatches(client, AltoClef.getInstance(), movement.getSrc(), movement.getDest()))
//$$             return failed("native_cached_or_world_footprint_not_live_proven");
//$$         if (state == null) {
//$$             state = new MovementState(); stepStarted = System.nanoTime();
//$$             log.event("APPROACH_STEP_STARTED", "stepIndex", stepIndex, "nativeKind", movement.getClass().getSimpleName());
//$$         }
//$$         if (System.nanoTime() - stepStarted >= 10_000_000_000L) return failed("native_step_no_progress_timeout");
//$$         // AIR-only body and supported solid floor prevent prepared/tool/bridge/door branches before delegation.
//$$         if (!movement.toBreak(AltoClef.getInstance().getClientBaritone().bsi).isEmpty()
//$$                 || !movement.toPlace(AltoClef.getInstance().getClientBaritone().bsi).isEmpty()) return failed("native_step_requires_world_mutation");
//$$         state.getInputStates().clear();
//$$         if (!withinBudget.getAsBoolean()) return failed("elapsed_budget_exhausted");
//$$         state = movement.updateState(state);
//$$         if (!withinBudget.getAsBoolean()) return failed("elapsed_budget_exhausted");
//$$         Map<Input, Boolean> staged = Map.copyOf(state.getInputStates());
//$$         if (FindOwnedMovementInputs.hasForbiddenInput(staged)) return failed("native_step_requested_forbidden_input");
//$$         MovementStatus status = state.getStatus();
//$$         if (status.isComplete() && status != MovementStatus.SUCCESS)
//$$             return failed("native_step_" + status.name().toLowerCase(java.util.Locale.ROOT));
//$$         if (status == MovementStatus.SUCCESS) {
//$$             inputs.release(); stepIndex++; state = null;
//$$             log.event("APPROACH_STEP_COMPLETED", "stepsBefore", stepIndex - 1, "stepsDelta", 1,
//$$                     "stepsAfter", stepIndex, "stepsCompleted", stepIndex);
//$$             return Step.waiting();
//$$         }
//$$         var rotation = state.getTarget().getRotation();
//$$         if (!movementKeysUnclaimedOutsideLease()) return failed("external_movement_input_contended");
//$$         if (rotation.isPresent()) {
//$$             Rotation requested = rotation.get();
//$$             if (!Float.isFinite(requested.getYaw()) || !Float.isFinite(requested.getPitch())) return failed("invalid_native_rotation");
//$$             if (!withinBudget.getAsBoolean()) return failed("elapsed_budget_exhausted");
//$$             AltoClef.getInstance().getInputControls().forceLook(requested.getYaw(), requested.getPitch());
//$$         }
//$$         if (!withinBudget.getAsBoolean()) return failed("elapsed_budget_exhausted");
//$$         if (!inputs.apply(staged)) return failed("forced_input_lease_rejected_or_superseded");
//$$         moved |= staged.entrySet().stream().anyMatch(e -> Boolean.TRUE.equals(e.getValue()) && e.getKey() != Input.SPRINT);
//$$         return Step.waiting();
//$$     }
//$$     private boolean visible(Vec3d target, FindCandidate candidate) {
//$$         Vec3d aim = request.kind().equals("block") ? target : target.add(0, .9, 0);
//$$         var hit = client.world.raycast(new net.minecraft.world.RaycastContext(client.player.getEyePos(), aim,
//$$                 net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE,
//$$                 client.player));
//$$         return hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS || request.kind().equals("block")
//$$                 && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
//$$                 && hit.getBlockPos().equals(new net.minecraft.util.math.BlockPos(candidate.x(), candidate.y(), candidate.z()));
//$$     }
//$$     private boolean movementKeysFree() {
//$$         var mod = AltoClef.getInstance();
//$$         for (Input input : new Input[]{Input.MOVE_FORWARD, Input.MOVE_BACK, Input.MOVE_LEFT, Input.MOVE_RIGHT, Input.SPRINT}) {
//$$             if (mod.getInputControls().isHeldDown(input) || mod.getClientBaritone().getInputOverrideHandler().isInputForcedDown(input)) return false;
//$$         }
//$$         return true;
//$$     }
//$$     private boolean movementKeysUnclaimedOutsideLease() {
//$$         // Physical movement belongs to the user; the lease API handles superseding forced writers independently.
//$$         var mod = AltoClef.getInstance();
//$$         for (Input input : new Input[]{Input.MOVE_FORWARD, Input.MOVE_BACK, Input.MOVE_LEFT, Input.MOVE_RIGHT, Input.SPRINT}) {
//$$             if (mod.getInputControls().isHeldDown(input)) return false;
//$$             if (mod.getClientBaritone().getInputOverrideHandler().isInputForcedDown(input) && !inputs.owns(input)) return false;
//$$         }
//$$         return true;
//$$     }
//$$     private Step failed(String reason) {
//$$         failure = reason; suspend(); return new Step(reason, false, null);
//$$     }
//$$     @Override public void suspend() { if (inputs != null) inputs.release(); stableTicks = 0; }
//$$     @Override public boolean quiet() { return inputs == null || inputs.quiet(); }
//$$     @Override public Progress progress() {
//$$         return new Progress(stepIndex, planner == null || planner.path() == null ? 0 : planner.path().size(),
//$$                 planner == null ? 0 : planner.captureColumns(), moved);
//$$     }
//$$ }
//#endif
