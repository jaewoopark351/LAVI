package lavi.minecraft.integration.carryon.snapshot;

import adris.altoclef.AltoClef;
import baritone.api.utils.input.Input;

//20260731_kpopmodder: Keep input-state reads separate from Baritone path ownership diagnostics.
public final class CarryOnInputSnapshotCollector {
    private CarryOnInputSnapshotCollector() {
    }

    public static CarryOnInputSnapshot collect(AltoClef mod) {
        return new CarryOnInputSnapshot(
                inputState(mod, Input.CLICK_RIGHT),
                inputState(mod, Input.SNEAK),
                inputState(mod, Input.CLICK_LEFT),
                movementInputState(mod)
        );
    }

    private static String inputState(AltoClef mod, Input input) {
        try {
            if (mod == null || mod.getInputControls() == null) {
                return "unavailable";
            }
            return Boolean.toString(mod.getInputControls().isHeldDown(input));
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String movementInputState(AltoClef mod) {
        return "forward=" + inputState(mod, Input.MOVE_FORWARD)
                + ",back=" + inputState(mod, Input.MOVE_BACK)
                + ",left=" + inputState(mod, Input.MOVE_LEFT)
                + ",right=" + inputState(mod, Input.MOVE_RIGHT)
                + ",jump=" + inputState(mod, Input.JUMP)
                + ",sprint=" + inputState(mod, Input.SPRINT);
    }
}
