package adris.altoclef.tasks.construction.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.util.logging.StateChangeLogger;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

//20260728_kpopmodder: Added this controller to isolate Carry On's sneak-right-click placement input sequence.
public final class CarryOnPlacementInputController {

    private static final int SNEAK_WARMUP_TICKS = 2;

    private final StateChangeLogger debugLogger = new StateChangeLogger("CarryOnPlacementInputController");

    private int sneakTicks;
    private int releaseCount;
    private int shiftRightClickCallCount;

    public void reset() {
        sneakTicks = 0;
        releaseCount = 0;
        shiftRightClickCallCount = 0;
        debugLogger.reset();
    }

    public void release(AltoClef mod) {
        if (mod == null) {
            return;
        }
        releaseCount++;
        debugLogger.state("release input before:" + releaseCount,
                "release input before: count=" + releaseCount
                        + ", sneakTicks=" + sneakTicks
                        + ", " + describeInputState(mod)
                        + ", screen=" + describeCurrentScreen());
        mod.getInputControls().release(Input.CLICK_RIGHT);
        mod.getInputControls().release(Input.SNEAK);
        if (mod.getPlayer() != null) {
            mod.getPlayer().input.sneaking = false;
        }
        debugLogger.state("release input after:" + releaseCount,
                "release input after: count=" + releaseCount
                        + ", sneakTicks=" + sneakTicks
                        + ", " + describeInputState(mod)
                        + ", screen=" + describeCurrentScreen());
    }

    public boolean tryShiftRightClickSupport(AltoClef mod, CarriedBlockPlacementPlanner.PlacementTarget target, int attempt) {
        shiftRightClickCallCount++;
        debugLogger.state("shift-right-click entry:" + shiftRightClickCallCount,
                "shift-right-click entry: call=" + shiftRightClickCallCount
                        + ", attempt=" + attempt
                        + ", target=" + describeTarget(target)
                        + ", screen=" + describeCurrentScreen()
                        + ", crosshair=" + describeCrosshair()
                        + ", modOrPlayerMissing=" + (mod == null || mod.getPlayer() == null)
                        + ", " + describeInputState(mod));
        if (mod == null || mod.getPlayer() == null || target == null) {
            return false;
        }

        holdSneak(mod);
        if (sneakTicks < SNEAK_WARMUP_TICKS) {
            debugLogger.state("warming-up-sneak:" + shiftRightClickCallCount,
                    "waiting for sneak before Carry On placement: call=" + shiftRightClickCallCount
                            + ", attempt=" + attempt
                            + ", ticks=" + sneakTicks
                            + ", " + describeInputState(mod)
                            + ", target=" + describeTarget(target));
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) {
            debugLogger.state("waiting-interaction-manager:" + shiftRightClickCallCount,
                    "waiting: no interaction manager for Carry On placement: call=" + shiftRightClickCallCount
                            + ", attempt=" + attempt
                            + ", target=" + describeTarget(target)
                            + ", " + describeInputState(mod));
            return false;
        }

        HitResult hitResult = client.crosshairTarget;
        boolean usingCrosshairTarget = hitResult instanceof BlockHitResult crosshairBlockHit
                && crosshairBlockHit.getBlockPos().equals(target.supportPos())
                && crosshairBlockHit.getSide() == target.supportFace();
        BlockHitResult blockHitResult = usingCrosshairTarget
                ? (BlockHitResult) hitResult
                : createDirectSupportHit(target);

        if (!usingCrosshairTarget) {
            debugLogger.state("direct-support-hit:" + shiftRightClickCallCount,
                    "using direct Carry On support hit because crosshair is not exact: call=" + shiftRightClickCallCount
                            + ", attempt=" + attempt
                            + ", support=" + target.supportPos().toShortString()
                            + ", face=" + target.supportFace()
                            + ", crosshair=" + describeCrosshair()
                            + ", " + describeInputState(mod));
        }

        ActionResult result = client.interactionManager.interactBlock(mod.getPlayer(), Hand.MAIN_HAND, blockHitResult);
        mod.getPlayer().swingHand(Hand.MAIN_HAND);
        debugLogger.event("shift-right-click carried block attempt=" + attempt
                + ", call=" + shiftRightClickCallCount
                + ", result=" + result
                + ", hitSource=" + (usingCrosshairTarget ? "crosshair" : "direct")
                + ", target=" + describeTarget(target)
                + ", crosshair=" + describeCrosshair()
                + ", " + describeInputState(mod));
        return true;
    }

    private BlockHitResult createDirectSupportHit(CarriedBlockPlacementPlanner.PlacementTarget target) {
        Vec3d hitPos = Vec3d.ofCenter(target.supportPos()).add(
                target.supportFace().getOffsetX() * 0.5,
                target.supportFace().getOffsetY() * 0.5,
                target.supportFace().getOffsetZ() * 0.5);
        return new BlockHitResult(hitPos, target.supportFace(), target.supportPos(), false);
    }

    private void holdSneak(AltoClef mod) {
        mod.getInputControls().hold(Input.SNEAK);
        mod.getPlayer().input.sneaking = true;
        sneakTicks++;
        debugLogger.state("hold sneak:" + sneakTicks + ":" + shiftRightClickCallCount,
                "hold sneak for Carry On placement: call=" + shiftRightClickCallCount
                        + ", sneakTicks=" + sneakTicks
                        + ", " + describeInputState(mod)
                        + ", screen=" + describeCurrentScreen());
    }

    private String describeInputState(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "input=context-missing";
        }
        return "playerSneaking=" + mod.getPlayer().isSneaking()
                + ", inputSneaking=" + mod.getPlayer().input.sneaking
                + ", sneakKeyHeld=" + mod.getInputControls().isHeldDown(Input.SNEAK)
                + ", useKeyHeld=" + mod.getInputControls().isHeldDown(Input.CLICK_RIGHT);
    }

    private String describeCurrentScreen() {
        Object screen = MinecraftClient.getInstance().currentScreen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private String describeCrosshair() {
        HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
        if (hitResult == null) {
            return "none";
        }
        if (hitResult instanceof BlockHitResult blockHit) {
            return "block:" + blockHit.getBlockPos().toShortString()
                    + ", side=" + blockHit.getSide()
                    + ", type=" + blockHit.getType();
        }
        return hitResult.getType().toString();
    }

    private String describeTarget(CarriedBlockPlacementPlanner.PlacementTarget target) {
        if (target == null) {
            return "none";
        }
        return "place=" + target.placePos().toShortString()
                + ", support=" + target.supportPos().toShortString()
                + ", face=" + target.supportFace();
    }
}
