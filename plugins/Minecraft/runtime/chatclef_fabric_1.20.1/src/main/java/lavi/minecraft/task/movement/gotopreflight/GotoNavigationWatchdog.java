//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;

/** Finite timing only. A timeout never asserts that the bot needs building materials. */
final class GotoNavigationWatchdog {
    static final int MIN_NATIVE_TICKS = 20;
    static final int AIR_FALLBACK_IDLE_TICKS = 100;
    static final int MAX_STILL_TICKS = 1200;
    static final int MAX_NATIVE_TICKS = 18_000;
    private BlockPos lastPosition;
    private BlockPos breakingPosition;
    private boolean breakingWasSolid;
    private int ticks;
    private int legTicks;
    private int stillTicks;
    private int noPathTicks;

    void observe(AltoClef mod) {
        ticks++;
        legTicks++;
        BlockPos current = mod.getPlayer().getBlockPos();
        boolean progressed = lastPosition == null || !lastPosition.equals(current);
        if (breakingWasSolid && breakingPosition != null
                && mod.getWorld().isChunkLoaded(breakingPosition)
                && mod.getWorld().getBlockState(breakingPosition).isAir()) progressed = true;
        lastPosition = current.toImmutable();
        breakingPosition = mod.getControllerExtras().isBreakingBlock()
                ? mod.getControllerExtras().getBreakingBlockPos() : null;
        breakingWasSolid = breakingPosition != null && mod.getWorld().isChunkLoaded(breakingPosition)
                && !mod.getWorld().getBlockState(breakingPosition).isAir();
        stillTicks = progressed ? 0 : stillTicks + 1;
        var pathing = mod.getClientBaritone().getPathingBehavior();
        boolean noPath = pathing.getCurrent() == null && !pathing.isPathing()
                && pathing.getInProgress().isEmpty() && !mod.getControllerExtras().isBreakingBlock();
        noPathTicks = noPath && !progressed ? noPathTicks + 1 : 0;
    }

    boolean probeDue() { return nativeHadTurn() && legTicks % 10 == 0; }
    boolean nativeHadTurn() { return legTicks >= MIN_NATIVE_TICKS; }
    boolean mayCheckAirFallback() {
        return nativeHadTurn() && stillTicks >= AIR_FALLBACK_IDLE_TICKS && noPathTicks >= 40;
    }
    boolean timedOut() { return ticks > MAX_NATIVE_TICKS; }
    boolean stalled() { return stillTicks > MAX_STILL_TICKS; }
    void newLeg() {
        lastPosition = null;
        breakingPosition = null;
        breakingWasSolid = false;
        legTicks = stillTicks = noPathTicks = 0;
        // The command-wide native time budget is deliberately NOT reset.
    }
    void resume() { newLeg(); }
}
//#endif
