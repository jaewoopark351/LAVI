package adris.altoclef.tasks.construction.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.util.logging.StateChangeLogger;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

//20260728_kpopmodder: Added this controller to isolate Carry On's sneak-right-click placement input sequence.
public final class CarryOnPlacementInputController {

    private static final int SNEAK_WARMUP_TICKS = 2;

    private final StateChangeLogger debugLogger = new StateChangeLogger("CarryOnPlacementInputController");

    private int sneakTicks;

    public void reset() {
        sneakTicks = 0;
        debugLogger.reset();
    }

    public void release(AltoClef mod) {
        if (mod == null) {
            return;
        }
        mod.getInputControls().release(Input.CLICK_RIGHT);
        mod.getInputControls().release(Input.SNEAK);
        if (mod.getPlayer() != null) {
            mod.getPlayer().input.sneaking = false;
        }
    }

    public boolean tryShiftRightClickSupport(AltoClef mod, CarriedBlockPlacementPlanner.PlacementTarget target, int attempt) {
        if (mod == null || mod.getPlayer() == null || target == null) {
            return false;
        }

        holdSneak(mod);
        if (sneakTicks < SNEAK_WARMUP_TICKS) {
            debugLogger.state("warming-up-sneak", "waiting for sneak before Carry On placement: ticks=" + sneakTicks);
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) {
            debugLogger.state("waiting-interaction-manager", "waiting: no interaction manager for Carry On placement");
            return false;
        }

        HitResult hitResult = client.crosshairTarget;
        if (!(hitResult instanceof BlockHitResult blockHitResult)
                || !blockHitResult.getBlockPos().equals(target.supportPos())
                || blockHitResult.getSide() != target.supportFace()) {
            debugLogger.state("waiting-crosshair",
                    "waiting: crosshair has not reached Carry On support target=" + target.supportPos().toShortString()
                            + " face=" + target.supportFace());
            return false;
        }

        ActionResult result = client.interactionManager.interactBlock(mod.getPlayer(), Hand.MAIN_HAND, blockHitResult);
        mod.getPlayer().swingHand(Hand.MAIN_HAND);
        if (shouldLogAttempt(attempt)) {
            debugLogger.event("shift-right-click carried block attempt=" + attempt
                    + ", result=" + result
                    + ", playerSneaking=" + mod.getPlayer().isSneaking()
                    + ", inputSneaking=" + mod.getPlayer().input.sneaking
                    + ", support=" + target.supportPos().toShortString()
                    + ", face=" + target.supportFace()
                    + ", place=" + target.placePos().toShortString());
        }
        return true;
    }

    private void holdSneak(AltoClef mod) {
        mod.getInputControls().hold(Input.SNEAK);
        mod.getPlayer().input.sneaking = true;
        sneakTicks++;
    }

    private boolean shouldLogAttempt(int attempt) {
        return attempt <= 3 || attempt % 5 == 0;
    }
}
