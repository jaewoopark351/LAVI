//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.GotoTarget;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

import static lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan.*;

/**
 * V3.1: native XYZ navigation first; local aerial preparation without an exact waypoint.
 * The original target is never replaced by foundation.up(). A loaded air column is
 * only evidence for a possible local preparation budget, not proof that every route
 * needs that many blocks or that a native path failure was caused by inventory.
 * Preparation is admitted once, from a nearby dry, grounded, sky-visible location.
 * Native navigation is not given a new timeout, retry policy or path-cost setting.
 */
public final class PreparedGotoTask extends Task {

    private enum Phase {
        NATIVE,
        DRAIN_TO_PREPARE,
        PREPARE,
        DRAIN_TO_FINAL,
        FINAL_NAVIGATION,
        ARRIVAL_CLEANUP,
        TERMINAL
    }

    private static final int AERIAL_RECHECK_TICKS = 20;

    private final ClientWorld world;
    private final ClientPlayerEntity player;
    private final BlockPos target;
    private final Dimension dimension;

    private GetToBlockTask navigation;
    private Phase phase = Phase.NATIVE;

    private GotoFallbackProbe.AirColumn aerial;
    private GotoMaterialPlan plan;
    private GotoMaterialInventory inventory;
    private CollectGotoMaterialsTask acquisition;
    private BlockPos acquisitionAnchor;

    private int placementDemand;
    private int aerialRecheck;
    private int cleanupTicks;
    private int quietTicks;

    private boolean nativeIssued;
    private String lastDecision;
    private int decisionLogs;
    private static final int MAX_DECISION_LOGS = 32;
    private final String operation = Integer.toHexString(System.identityHashCode(this));
    private boolean announced;
    private boolean collected;
    private boolean arrived;
    private Failure failure;

    /** Called only by GotoCommand.call; shared task factories remain unchanged. */
    public static Task forCommand(AltoClef mod, GotoTarget request, Task legacy) {
        if (request.getType() != GotoTarget.GotoTargetCoordType.XYZ
                || mod.getWorld() == null
                || mod.getPlayer() == null
                || (request.getDimension() != null
                    && request.getDimension() != WorldHelper.getCurrentDimension())
                || request.getY() <= mod.getPlayer().getBlockY()) {
            return legacy;
        }
        if (!(legacy instanceof GetToBlockTask nativeTask)) {
            return legacy;
        }
        return new PreparedGotoTask(mod, request, nativeTask);
    }

    private PreparedGotoTask(AltoClef mod, GotoTarget request, GetToBlockTask nativeTask) {
        world = mod.getWorld();
        player = mod.getPlayer();
        target = new BlockPos(request.getX(), request.getY(), request.getZ());
        dimension = request.getDimension();
        navigation = nativeTask;
    }

    @Override
    protected void onStart() {
        // Do not reset phase/target after survival-chain interruption.
    }

    @Override
    protected Task onTick() {
        if (phase == Phase.TERMINAL) return null;

        AltoClef mod = AltoClef.getInstance();
        try {
            requireBinding(mod);

            if (!announced) {
                announced = true;
                log("NATIVE_FIRST start=" + player.getBlockPos() + " target=" + target);
            }

            return switch (phase) {
                case NATIVE -> nativeTick(mod);
                case DRAIN_TO_PREPARE -> beginPreparation(mod);
                case PREPARE -> prepare(mod);
                case DRAIN_TO_FINAL -> handoff(mod);
                case FINAL_NAVIGATION -> finalNavigationTick(mod);
                case ARRIVAL_CLEANUP -> finishArrival(mod);
                case TERMINAL -> null;
            };
        } catch (Failure ex) {
            terminate(ex);
        } catch (RuntimeException | LinkageError ex) {
            terminate(new Failure(FailureReason.INTERNAL_ERROR, ex.toString()));
        }
        return null;
    }

    /**
     * Existing navigation owns ordinary terrain. Aerial classification is geometry-driven,
     * never timeout-driven. If the target column is not yet loaded, simply keep navigating
     * and re-check later when the client actually knows that terrain.
     */
    private Task nativeTick(AltoClef mod) {
        if (atOriginalTarget() && player.isOnGround()) {
            enterDrain(Phase.ARRIVAL_CLEANUP);
            return null;
        }

        // Give the original XYZ task an actual tick before considering assistance.
        if (!nativeIssued) {
            nativeIssued = true;
            return navigation;
        }
        if (--aerialRecheck > 0) return navigation;
        aerialRecheck = AERIAL_RECHECK_TICKS;

        // Re-read live geometry: native navigation can place/break blocks en route.
        Optional<GotoFallbackProbe.AirColumn> observed = GotoFallbackProbe.airColumn(mod, target);
        if (observed.isEmpty()) {
            aerial = null;
            decision("AIR_COLUMN_UNAVAILABLE", "player=" + player.getBlockPos());
            return navigation;
        }
        if (!observed.get().equals(aerial)) {
            aerial = observed.get();
            decision("AERIAL_CLASSIFIED", "foundation=" + aerial.foundation()
                    + " columnFeet=" + aerial.approach()
                    + " nativeGoal=" + target + " waypointCreated=false");
        }

        String blocked = GotoFallbackProbe.preparationBlockReason(mod, aerial, player.getBlockPos());
        if (blocked != null) {
            decision(blocked, "player=" + player.getBlockPos() + " foundation=" + aerial.foundation());
            return navigation;
        }
        if (!safeToPause(mod)) {
            decision("NATIVE_NOT_SAFE_TO_PAUSE", "player=" + player.getBlockPos());
            return navigation;
        }

        // Inventory is not an admission requirement for ordinary/underground movement.
        GotoMaterialInventory view = new GotoMaterialInventory(mod);
        int held = view.count(mod);
        int estimate = GotoFallbackProbe.materialBudgetFrom(player.getBlockPos(), aerial);
        if (held >= estimate) {
            decision("MATERIALS_SUFFICIENT_NATIVE_CONTINUES", "held=" + held
                    + " demandEstimate=" + estimate + " reserveNotRequired=true");
            return navigation;
        }

        // Freeze only the selected local preparation site, never the initial underground Y.
        acquisitionAnchor = player.getBlockPos().toImmutable();
        inventory = view;
        placementDemand = estimate;
        plan = GotoMaterialPlan.forPlacements(acquisitionAnchor.getY(), target.getY(), estimate);
        log("PREPARATION_SELECTED anchor=" + acquisitionAnchor
                + " foundation=" + aerial.foundation() + " held=" + held
                + " demandEstimate=" + placementDemand + " targetHeld=" + plan.requiredHeld()
                + " budgetPolicy=LOCAL_VERTICAL_PLUS_HORIZONTAL nativeFailureCause=UNKNOWN"
                + " waypointCreated=false");
        enterDrain(Phase.DRAIN_TO_PREPARE);
        return null;
    }

    private Task beginPreparation(AltoClef mod) {
        if (!quiescent(mod)) return null;
        requireAerial(mod);

        // No exact foundation waypoint. Revalidate the actual selected standing location.
        String blocked = GotoFallbackProbe.preparationBlockReason(mod, aerial, player.getBlockPos());
        if (blocked != null || !player.getBlockPos().equals(acquisitionAnchor)) {
            throw new Failure(FailureReason.AIR_COLUMN_CHANGED,
                    "Preparation site changed during cleanup: reason=" + blocked
                            + " player=" + player.getBlockPos() + " anchor=" + acquisitionAnchor);
        }

        phase = Phase.PREPARE;
        log("PREPARATION_CHECK anchor=" + acquisitionAnchor
                + " held=" + inventory.count(mod)
                + " demandEstimate=" + placementDemand
                + " targetHeld=" + plan.requiredHeld() + " originalTarget=" + target);
        return null;
    }

    /** Bounded observer output; its counters never influence navigation or admission. */
    private void decision(String reason, String detail) {
        if (reason.equals(lastDecision)) return;
        lastDecision = reason;
        if (decisionLogs < MAX_DECISION_LOGS) {
            decisionLogs++;
            log("NATIVE_DECISION reason=" + reason + " " + detail);
        } else if (decisionLogs == MAX_DECISION_LOGS) {
            decisionLogs++;
            log("NATIVE_DECISION_LIMIT reached=true transitionsAndTerminalStillEnabled=true");
        }
    }

    private Task prepare(AltoClef mod) {
        int held = inventory.count(mod);

        if (acquisition != null && acquisition.failure() != null) {
            throw acquisition.failure();
        }

        // Do not collect the reserve when the local preparation estimate is already held.
        if (acquisition == null && held >= placementDemand) {
            log("MATERIALS_ALREADY_SUFFICIENT held=" + held
                    + " placements=" + placementDemand + " reserveNotRequired=true");
            enterDrain(Phase.DRAIN_TO_FINAL);
            return null;
        }

        if (held >= plan.requiredHeld()) {
            log("PREPARED held=" + held
                    + " placements=" + placementDemand
                    + " targetHeld=" + plan.requiredHeld()
                    + " collected=" + collected);
            enterDrain(Phase.DRAIN_TO_FINAL);
            return null;
        }

        if (acquisition == null) {
            collected = true;
            acquisition = new CollectGotoMaterialsTask(plan, inventory, acquisitionAnchor, aerial.foundation());
            log("ACQUIRE_START held=" + held
                    + " placements=" + placementDemand
                    + " targetHeld=" + plan.requiredHeld());
        }

        if (acquisition.isFinished()) {
            if (acquisition.failure() != null) throw acquisition.failure();
            throw new Failure(FailureReason.HANDOFF_SHORTAGE,
                    "Acquisition ended before target inventory was reached");
        }

        return acquisition;
    }

    private Task handoff(AltoClef mod) {
        if (!quiescent(mod)) return null;
        requireBinding(mod);
        requireAerial(mod);
        if (!player.isOnGround() || player.isTouchingWater()) {
            throw new Failure(FailureReason.HANDOFF_SHORTAGE,
                    "Final navigation requires a stable dry collection exit");
        }

        int held = inventory.count(mod);
        int remainingEstimate = GotoFallbackProbe.materialBudgetFrom(player.getBlockPos(), aerial);
        int minimum = Math.max(collected ? plan.requiredHeld() : placementDemand, remainingEstimate);
        if (held < minimum) {
            throw new Failure(FailureReason.HANDOFF_SHORTAGE,
                    "held=" + held + " minimum=" + minimum
                            + " anchor=" + acquisitionAnchor + " player=" + player.getBlockPos()
                            + " remainingEstimate=" + remainingEstimate);
        }

        navigation = new GetToBlockTask(target, dimension);
        phase = Phase.FINAL_NAVIGATION;
        log("RESUME_ORIGINAL target=" + target
                + " held=" + held
                + " placements=" + placementDemand
                + " collected=" + collected);
        return navigation;
    }

    /** After handoff, material acquisition is permanently disabled for this command. */
    private Task finalNavigationTick(AltoClef mod) {
        if (atOriginalTarget() && player.isOnGround()) {
            enterDrain(Phase.ARRIVAL_CLEANUP);
            return null;
        }
        return navigation;
    }

    private Task finishArrival(AltoClef mod) {
        if (!quiescent(mod)) return null;
        requireBinding(mod);

        if (!atOriginalTarget()) {
            throw new Failure(FailureReason.ARRIVAL_LOST,
                    "player=" + player.getBlockPos() + " target=" + target);
        }
        if (aerial != null && !player.isOnGround()) {
            throw new Failure(FailureReason.ARRIVAL_LOST,
                    "Aerial target was crossed without a stable standing position");
        }

        arrived = true;
        phase = Phase.TERMINAL;
        log("ARRIVED target=" + target
                + " aerial=" + (aerial != null)
                + " collected=" + collected);
        return null;
    }

    private boolean atOriginalTarget() {
        return navigation.isFinished() && player.getBlockPos().equals(target);
    }

    private void requireAerial(AltoClef mod) {
        if (aerial == null || !GotoFallbackProbe.unchanged(mod, aerial)) {
            throw new Failure(FailureReason.AIR_COLUMN_CHANGED,
                    "Aerial column changed before handoff");
        }
    }

    private boolean safeToPause(AltoClef mod) {
        return player.isOnGround()
                && !player.isTouchingWater()
                && !WorldHelper.isInNetherPortal()
                && !mod.getExtraBaritoneSettings().isInteractionPaused()
                && mod.getClientBaritone().getPathingBehavior().isSafeToCancel()
                && !mod.getControllerExtras().isBreakingBlock();
    }

    private void requireBinding(AltoClef mod) {
        if (mod.getUserTaskChain().getCurrentTask() != this) {
            throw new Failure(FailureReason.ROOT_REPLACED);
        }
        if (mod.getWorld() != world || mod.getPlayer() != player || !player.isAlive()) {
            throw new Failure(FailureReason.WORLD_CHANGED);
        }
        if (player.isSpectator() || player.getAbilities().creativeMode) {
            throw new Failure(FailureReason.NOT_SURVIVAL);
        }
    }

    private void enterDrain(Phase next) {
        Phase previous = phase;
        stopOwnedChildren();
        cleanupTicks = 0;
        quietTicks = 0;
        phase = next;
        log("PHASE from=" + previous + " to=" + next + " player=" + player.getBlockPos());
    }

    private boolean quiescent(AltoClef mod) {
        if (++cleanupTicks > MAX_CLEANUP_TICKS) {
            throw new Failure(FailureReason.CLEANUP_TIMEOUT,
                    "phase=" + phase + " target=" + target);
        }

        boolean quiet = (acquisition == null || !acquisition.isActive())
                && (navigation == null || !navigation.isActive())
                && !mod.getClientBaritone().getPathingBehavior().isPathing()
                && mod.getClientBaritone().getPathingBehavior().getCurrent() == null
                && mod.getClientBaritone().getPathingBehavior().getInProgress().isEmpty()
                && !mod.getClientBaritone().getCustomGoalProcess().isActive()
                && !mod.getClientBaritone().getMineProcess().isActive()
                && !mod.getClientBaritone().getBuilderProcess().isActive()
                && !mod.getControllerExtras().isBreakingBlock()
                && !mod.getInputControls().isHeldDown(Input.CLICK_LEFT)
                && !mod.getClientBaritone().getInputOverrideHandler().isInputForcedDown(Input.CLICK_LEFT);

        if (!quiet && (cleanupTicks == 1 || cleanupTicks == MAX_CLEANUP_TICKS)) {
            log("CLEANUP_WAIT phase=" + phase + " ticks=" + cleanupTicks
                    + " navigationActive=" + (navigation != null && navigation.isActive())
                    + " acquisitionActive=" + (acquisition != null && acquisition.isActive())
                    + " pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                    + " calculating=" + mod.getClientBaritone().getPathingBehavior().getInProgress().isPresent()
                    + " breaking=" + mod.getControllerExtras().isBreakingBlock());
        }
        quietTicks = quiet ? quietTicks + 1 : 0;
        return quietTicks >= 2;
    }

    private void terminate(Failure ex) {
        stopOwnedChildren();
        failure = ex;
        phase = Phase.TERMINAL;
        log("FAILED reason=" + ex.reason()
                + " detail=" + ex.getMessage()
                + " target=" + target);
    }

    private void stopOwnedChildren() {
        if (acquisition != null && acquisition.isActive()) acquisition.stop();
        if (navigation != null && navigation.isActive()) navigation.stop();
    }

    @Override
    protected void onStop(Task interruptTask) {
        stopOwnedChildren();
        quietTicks = 0;
    }

    @Override
    public boolean isFinished() {
        return phase == Phase.TERMINAL;
    }

    public boolean arrived() {
        return arrived;
    }

    public FailureReason failureReason() {
        return failure == null ? null : failure.reason();
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        return "Local aerial GOTO V3.1 " + target + " " + phase;
    }

    private void log(String message) {
        try {
            Debug.logMessage("[LAVI GOTO V3.1] " + message + " operation=" + operation);
        } catch (RuntimeException ignored) {
            // Logs are observers, never behavior gates.
        }
    }
}
//#endif
