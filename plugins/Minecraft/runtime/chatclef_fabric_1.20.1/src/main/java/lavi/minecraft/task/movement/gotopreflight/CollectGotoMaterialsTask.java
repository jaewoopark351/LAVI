//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan.*;

/** Serial, bounded local acquisition. No resource-command recursion, item discarding or tool acquisition. */
final class CollectGotoMaterialsTask extends Task {
    private enum Phase { SELECT, BREAK, OBSERVE_DROP, PICKUP }
    private final GotoMaterialPlan plan;
    private final GotoMaterialInventory inventory;
    private final GotoMaterialSources sources;
    private final Set<BlockPos> attempted = new HashSet<>();
    private final Set<UUID> existingDrops = new HashSet<>();
    private Phase phase = Phase.SELECT;
    private GotoMaterialSources.Source source;
    private Task child;
    private ItemEntity drop;
    private int sourceItemCount;
    private int ticks;
    private int lastProgressTick;
    private int sourceStartedTick;
    private int dropWaitTicks;
    private int highestUsableHeld;
    private boolean breakStarted;
    private boolean complete;
    private Failure failure;

    CollectGotoMaterialsTask(GotoMaterialPlan plan, GotoMaterialInventory inventory, BlockPos anchor) {
        this(plan, inventory, anchor, null);
    }

    CollectGotoMaterialsTask(GotoMaterialPlan plan, GotoMaterialInventory inventory,
                             BlockPos anchor, BlockPos protectedFoundation) {
        this.plan = plan;
        this.inventory = inventory;
        this.sources = new GotoMaterialSources(anchor, protectedFoundation);
    }

    @Override protected void onStart() { /* Preserve the original budget on survival-chain resume. */ }

    @Override protected Task onTick() {
        if (isFinished()) return null;
        AltoClef mod = AltoClef.getInstance();
        try {
            if (++ticks > MAX_ACQUISITION_TICKS) throw new Failure(FailureReason.ACQUISITION_TIMEOUT);
            if (ticks - lastProgressTick > MAX_NO_PROGRESS_TICKS) throw new Failure(FailureReason.NO_PROGRESS);
            inventory.validateSettings(mod);
            if (!mod.getClientBaritoneSettings().allowInventory.value) {
                throw new Failure(FailureReason.INVENTORY_UNAVAILABLE,
                        "Automatic acquisition requires allowInventory; this feature does not change that setting.");
            }
            GotoMaterialInventory.requirePlayerInventory(mod);
            if (!sources.inBounds(mod.getPlayer().getBlockPos())) {
                throw new Failure(FailureReason.OUT_OF_BOUNDS,
                        "player=" + mod.getPlayer().getBlockPos() + " " + sources.boundsDescription() + " phase=" + phase);
            }
            if (!mod.getClientBaritoneSettings().allowBreak.value) throw new Failure(FailureReason.BREAKING_DISABLED);
            if (mod.getExtraBaritoneSettings().isInteractionPaused() || WorldHelper.isInNetherPortal()
                    || mod.getPlayer().isTouchingWater()) throw new Failure(FailureReason.INTERACTION_PAUSED);

            // This watchdog measures the objective inventory, not asserted mining provenance.
            int usableHeld = inventory.count(mod);
            if (usableHeld > highestUsableHeld) {
                highestUsableHeld = usableHeld;
                lastProgressTick = ticks;
            }
            // Sufficient actual inventory is authoritative; never count broken blocks as inventory.
            if (plan.shortage(usableHeld) == 0) {
                stopChild();
                complete = true;
                log("MATERIALS_HELD required=" + plan.requiredHeld());
                return null;
            }
            if (phase != Phase.SELECT && ticks - sourceStartedTick > MAX_SOURCE_TICKS) {
                abandon(FailureReason.SOURCE_TIMEOUT);
                return null;
            }
            if (child != null && child.thisOrChildAreTimedOut()) {
                abandon(FailureReason.SOURCE_TIMEOUT);
                return null;
            }
            return switch (phase) {
                case SELECT -> select(mod);
                case BREAK -> breakSource(mod);
                case OBSERVE_DROP -> observeDrop(mod);
                case PICKUP -> pickup(mod);
            };
        } catch (Failure ex) {
            stopChild();
            failure = ex;
            return null;
        } catch (RuntimeException ex) {
            stopChild();
            failure = new Failure(FailureReason.INTERNAL_ERROR, ex.toString());
            return null;
        }
    }

    private Task select(AltoClef mod) {
        if (attempted.size() >= MAX_CANDIDATES) throw new Failure(FailureReason.CANDIDATE_LIMIT);
        source = sources.find(mod, inventory, attempted);
        attempted.add(source.pos());
        sourceStartedTick = ticks;
        breakStarted = false;
        sourceItemCount = inventory.countItem(mod, source.drop());
        existingDrops.clear();
        for (Entity entity : mod.getWorld().getEntities()) {
            if (entity instanceof ItemEntity) existingDrops.add(entity.getUuid());
        }
        drop = null;
        phase = Phase.BREAK;
        log("SOURCE pos=" + source.pos() + " expected=" + source.drop());
        return null;
    }

    private Task breakSource(AltoClef mod) {
        if (!mod.getWorld().isChunkLoaded(source.pos())) {
            abandon(FailureReason.SOURCE_INVALIDATED);
            return null;
        }
        if (mod.getWorld().getBlockState(source.pos()).isAir()) {
            if (!breakStarted) {
                abandon(FailureReason.SOURCE_INVALIDATED);
                return null;
            }
            stopChild();
            phase = Phase.OBSERVE_DROP;
            dropWaitTicks = 0;
            return null;
        }
        try {
            sources.revalidate(mod, inventory, source);
        } catch (Failure ex) {
            if (ex.reason() != FailureReason.SOURCE_INVALIDATED) throw ex;
            abandon(ex.reason());
            return null;
        }
        if (!sources.equipAndCheck(mod, source)) {
            stopChild();
            return null;
        }
        if (child == null) child = new DestroyBlockTask(source.pos());
        breakStarted = true;
        return child;
    }

    private Task observeDrop(AltoClef mod) {
        for (Entity entity : mod.getWorld().getEntities()) {
            if (entity instanceof ItemEntity item && item.isAlive()
                    && !existingDrops.contains(item.getUuid()) && item.getStack().isOf(source.drop())
                    && item.squaredDistanceTo(source.pos().getX() + 0.5, source.pos().getY() + 0.5,
                            source.pos().getZ() + 0.5) <= 4.0) {
                drop = item;
                phase = Phase.PICKUP;
                // This is the same exact-entity movement primitive used by PickupDroppedItemTask.
                child = new GetToEntityTask(item);
                log("DROP source=" + source.pos() + " entity=" + item.getUuid());
                return null;
            }
        }
        if (inventory.countItem(mod, source.drop()) > sourceItemCount) {
            // An immediate pickup can occur between client ticks. Do not invent entity provenance.
            log("INVENTORY_INCREASE_UNATTRIBUTED source=" + source.pos());
            abandon(FailureReason.DROP_NOT_OBSERVED);
            return null;
        }
        if (++dropWaitTicks > 40) abandon(FailureReason.DROP_NOT_OBSERVED);
        return null;
    }

    private Task pickup(AltoClef mod) {
        int now = inventory.countItem(mod, source.drop());
        if (!drop.isAlive()) {
            stopChild();
            if (now > sourceItemCount) {
                lastProgressTick = ticks;
                log("PICKUP_OBSERVED source=" + source.pos() + " entity=" + drop.getUuid()
                        + " itemCountBefore=" + sourceItemCount + " itemCountAfter=" + now);
                phase = Phase.SELECT;
            } else {
                abandon(FailureReason.PICKUP_UNCONFIRMED);
            }
            return null;
        }
        if (!drop.getStack().isOf(source.drop()) || !sources.inBounds(drop.getBlockPos())) {
            abandon(FailureReason.DROP_LOST);
            return null;
        }
        if (!inventory.hasCapacity(mod, source.drop())) throw new Failure(FailureReason.INVENTORY_FULL);
        if (child == null) child = new GetToEntityTask(drop);
        return child;
    }

    private void abandon(FailureReason reason) {
        stopChild();
        log("SOURCE_ENDED reason=" + reason + " pos=" + (source == null ? "none" : source.pos()));
        phase = Phase.SELECT;
        drop = null;
    }

    private void stopChild() {
        if (child != null && child.isActive()) child.stop();
        child = null;
    }

    @Override protected void onStop(Task interruptTask) { stopChild(); }
    @Override public boolean isFinished() { return complete || failure != null; }
    boolean succeeded() { return complete && failure == null; }
    Failure failure() { return failure; }
    @Override protected boolean isEqual(Task other) { return this == other; }
    @Override protected String toDebugString() { return "GOTO materials " + phase + " required=" + plan.requiredHeld(); }

    private static void log(String message) {
        try { Debug.logMessage("[LAVI GOTO V3.1] " + message); }
        catch (RuntimeException ignored) { /* Logging is never a behavior gate. */ }
    }
}
//#endif
