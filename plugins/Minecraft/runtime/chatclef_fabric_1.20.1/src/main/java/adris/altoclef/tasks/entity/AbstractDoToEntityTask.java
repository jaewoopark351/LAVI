package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.speedrun.beatgame.BeatMinecraftTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalRunAway;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Optional;

/**
 * Interacts with an entity while maintaining distance.
 * <p>
 * The interaction is abstract.
 */
public abstract class AbstractDoToEntityTask extends Task implements ITaskRequiresGrounded {
    protected final MovementProgressChecker progress = new MovementProgressChecker();
    private final double maintainDistance;
    private final double combatGuardLowerRange;
    private final double combatGuardLowerFieldRadius;
    private TimeoutWanderTask wanderTask;

    protected AbstractDoToEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        this.maintainDistance = maintainDistance;
        this.combatGuardLowerRange = combatGuardLowerRange;
        this.combatGuardLowerFieldRadius = combatGuardLowerFieldRadius;
    }

    protected AbstractDoToEntityTask(double maintainDistance) {
        this(maintainDistance, 0, Double.POSITIVE_INFINITY);
    }

    protected AbstractDoToEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        this(-1, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();

        progress.reset();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        //20260730_kpopmodder: Diagnostics-only LAVI log for entity interaction loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "ON_START", "do_to_entity_start", this,
                "maintainDistance", maintainDistance,
                "combatGuardLowerRange", combatGuardLowerRange,
                "combatGuardLowerFieldRadius", combatGuardLowerFieldRadius,
                "cursorStack", ChatClefDiagnostics.itemStackSummary(cursorStack));
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        } // Kinda duct tape but it should be future proof ish
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        boolean pathing = mod.getClientBaritone().getPathingBehavior().isPathing();
        ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "ON_TICK", "do_to_entity_tick_begin", this,
                "pathing", pathing,
                "playerPosition", ChatClefDiagnostics.playerPosition(mod));
        if (pathing) {
            ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "OBSERVE", "pathing_active_resets_progress", this);
            progress.reset();
        }

        Optional<Entity> checkEntity = getEntityTarget(mod);
        ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "OBSERVE", "entity_target_result", this,
                "targetPresent", checkEntity.isPresent(),
                "targetEntity", checkEntity.map(ChatClefDiagnostics::entitySummary).orElse("none"));


        // Oof
        if (checkEntity.isEmpty()) {
            ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "clear_mob_defense_no_target", this);
            mod.getMobDefenseChain().resetTargetEntity();
            mod.getMobDefenseChain().resetForceField();
        } else {
            mod.getMobDefenseChain().setTargetEntity(checkEntity.get());
        }
        if (checkEntity.isPresent()) {
            Entity entity = checkEntity.get();

            double playerReach = mod.getModSettings().getEntityReachRange();

            // TODO: This is basically useless.
            EntityHitResult result = LookHelper.raycast(mod.getPlayer(), entity, playerReach);

            double sqDist = entity.squaredDistanceTo(mod.getPlayer());

            if (sqDist < combatGuardLowerRange * combatGuardLowerRange) {
                ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "lower_combat_force_field", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "squaredDistance", sqDist,
                        "combatGuardLowerRange", combatGuardLowerRange,
                        "combatGuardLowerFieldRadius", combatGuardLowerFieldRadius);
                mod.getMobDefenseChain().setForceFieldRange(combatGuardLowerFieldRadius);
            } else {
                ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "reset_combat_force_field", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "squaredDistance", sqDist,
                        "combatGuardLowerRange", combatGuardLowerRange);
                mod.getMobDefenseChain().resetForceField();
            }

            // If we don't specify a maintain distance, default to within 1 block of our reach.
            double maintainDistance = this.maintainDistance >= 0 ? this.maintainDistance : playerReach - 1;

            boolean tooClose = sqDist < maintainDistance * maintainDistance;
            boolean customGoalActive = mod.getClientBaritone().getCustomGoalProcess().isActive();
            ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "OBSERVE", "entity_interaction_conditions", this,
                    "entity", ChatClefDiagnostics.entitySummary(entity),
                    "playerReach", playerReach,
                    "raycastResult", result == null ? "none" : ChatClefDiagnostics.className(result),
                    "raycastType", result == null ? "none" : result.getType(),
                    "squaredDistance", sqDist,
                    "maintainDistance", maintainDistance,
                    "tooClose", tooClose,
                    "customGoalActive", customGoalActive,
                    "controllerInRange", ChatClefDiagnostics.safeValue(() -> mod.getControllerExtras().inRange(entity)),
                    "needsToEat", ChatClefDiagnostics.safeValue(() -> mod.getFoodChain().needsToEat()),
                    "mlgFalling", ChatClefDiagnostics.safeValue(() -> mod.getMLGBucketChain().isFalling(mod)),
                    "mlgDone", ChatClefDiagnostics.safeValue(() -> mod.getMLGBucketChain().doneMLG()),
                    "chorusFruiting", ChatClefDiagnostics.safeValue(() -> mod.getMLGBucketChain().isChorusFruiting()),
                    "safeToCancel", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isSafeToCancel()),
                    "onGround", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isOnGround()));

            // Step away if we're too close
            if (tooClose && !customGoalActive) {
                ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "set_goal_run_away", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "maintainDistance", maintainDistance);
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAway(maintainDistance, entity.getBlockPos()));
            }

            if (mod.getControllerExtras().inRange(entity) && result != null &&
                    result.getType() == HitResult.Type.ENTITY && !mod.getFoodChain().needsToEat() &&
                    !mod.getMLGBucketChain().isFalling(mod) && mod.getMLGBucketChain().doneMLG() &&
                    !mod.getMLGBucketChain().isChorusFruiting() &&
                    mod.getClientBaritone().getPathingBehavior().isSafeToCancel() &&
                    mod.getPlayer().isOnGround()) {
                ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "interact_with_entity", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "squaredDistance", sqDist);
                progress.reset();
                return onEntityInteract(mod, entity);
            } else if (!tooClose) {
                setDebugState("Approaching target");
                boolean progressOk = progress.check(mod);
                ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "OBSERVE", "approach_progress_check", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "progressOk", progressOk,
                        "squaredDistance", sqDist,
                        "maintainDistance", maintainDistance);
                if (!progressOk) {
                    progress.reset();
                    Debug.logMessage("Failed to get to target, blacklisting.");
                    ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "blacklist_unreachable_entity", this,
                            "entity", ChatClefDiagnostics.entitySummary(entity));
                    mod.getEntityTracker().requestEntityUnreachable(entity);
                }
                // Move to target
                Task getToEntityTask = new GetToEntityTask(entity, maintainDistance);
                ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "return_get_to_entity_task", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "getToEntityTask", ChatClefDiagnostics.taskSummary(getToEntityTask),
                        "maintainDistance", maintainDistance);
                return getToEntityTask;
            }
        }
        if (BeatMinecraftTask.isTaskRunning(mod,wanderTask)) {
            ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "reuse_entity_wander_task", this,
                    "wanderTask", ChatClefDiagnostics.taskSummary(wanderTask));
            return wanderTask;
        }

        if (!mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "wait_for_safe_cancel_before_wander", this);
            return null;
        }
        wanderTask = new TimeoutWanderTask();
        ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "DECISION", "start_entity_wander_task", this,
                "wanderTask", ChatClefDiagnostics.taskSummary(wanderTask));
        return wanderTask;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AbstractDoToEntityTask task) {
            if (!doubleCheck(task.maintainDistance, maintainDistance)) return false;
            if (!doubleCheck(task.combatGuardLowerFieldRadius, combatGuardLowerFieldRadius)) return false;
            if (!doubleCheck(task.combatGuardLowerRange, combatGuardLowerRange)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean doubleCheck(double a, double b) {
        if (Double.isInfinite(a) == Double.isInfinite(b)) return true;
        return Math.abs(a - b) < 0.1;
    }

    protected abstract boolean isSubEqual(AbstractDoToEntityTask other);

    protected abstract Task onEntityInteract(AltoClef mod, Entity entity);

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();

        ChatClefDiagnostics.logEvent("ENTITY_INTERACT", "ON_STOP", "do_to_entity_stop", this,
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
        mod.getMobDefenseChain().setTargetEntity(null);
        mod.getMobDefenseChain().resetForceField();
    }

    protected abstract Optional<Entity> getEntityTarget(AltoClef mod);

}
