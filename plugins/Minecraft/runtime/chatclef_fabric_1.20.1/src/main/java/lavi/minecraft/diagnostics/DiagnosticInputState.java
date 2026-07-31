package lavi.minecraft.diagnostics;

import adris.altoclef.AltoClef;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

//20260731_kpopmodder: Keep input and raw key diagnostic reads outside the event logging facade.
final class DiagnosticInputState {
    private DiagnosticInputState() {
    }

    static Object[] currentStateFields() {
        return new Object[]{
                "rightClickHeld", inputHeld(Input.CLICK_RIGHT),
                "leftClickHeld", inputHeld(Input.CLICK_LEFT),
                "sneakHeld", inputHeld(Input.SNEAK),
                "rawUseKeyPressed", rawKeyHeld(Input.CLICK_RIGHT),
                "rawAttackKeyPressed", rawKeyHeld(Input.CLICK_LEFT),
                "rawSneakKeyPressed", rawKeyHeld(Input.SNEAK),
                "playerSneaking", safeValue(() -> MinecraftClient.getInstance().player.isSneaking()),
                "sneakAndUsePressedTogether", sneakAndUsePressedTogether()
        };
    }

    static Object[] snapshotFields() {
        return new Object[]{
                "snapshotRightClickHeld", inputHeld(Input.CLICK_RIGHT),
                "snapshotLeftClickHeld", inputHeld(Input.CLICK_LEFT),
                "snapshotSneakHeld", inputHeld(Input.SNEAK),
                "snapshotRawUseKeyPressed", rawKeyHeld(Input.CLICK_RIGHT),
                "snapshotRawAttackKeyPressed", rawKeyHeld(Input.CLICK_LEFT),
                "snapshotRawSneakKeyPressed", rawKeyHeld(Input.SNEAK),
                "snapshotPlayerSneaking", safeValue(() -> MinecraftClient.getInstance().player.isSneaking()),
                "snapshotSneakAndUsePressedTogether", sneakAndUsePressedTogether(),
                "snapshotMainHandItem", safeValue(() -> MinecraftClient.getInstance().player.getMainHandStack()),
                "snapshotOffHandItem", safeValue(() -> MinecraftClient.getInstance().player.getOffHandStack()),
                "snapshotPlayerPosition", safeValue(() -> MinecraftClient.getInstance().player.getPos()),
                "snapshotCrosshairTarget", safeValue(() -> MinecraftClient.getInstance().crosshairTarget)
        };
    }

    static String inputHeld(Input input) {
        try {
            AltoClef instance = AltoClef.getInstance();
            if (instance == null || instance.getInputControls() == null) {
                return "unavailable";
            }
            return Boolean.toString(instance.getInputControls().isHeldDown(input));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String rawKeyHeld(Input input) {
        try {
            KeyBinding key = inputToKeyBinding(input);
            return key == null ? "unavailable" : Boolean.toString(key.isPressed());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String sneakAndUsePressedTogether() {
        try {
            return Boolean.toString(Boolean.parseBoolean(rawKeyHeld(Input.SNEAK))
                    && Boolean.parseBoolean(rawKeyHeld(Input.CLICK_RIGHT)));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String inputName(Input input) {
        if (input == null) {
            return "unavailable";
        }
        try {
            return input.name();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static KeyBinding inputToKeyBinding(Input input) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        GameOptions options = client.options;
        if (options == null || input == null) {
            return null;
        }
        return switch (input) {
            case MOVE_FORWARD -> options.forwardKey;
            case MOVE_BACK -> options.backKey;
            case MOVE_LEFT -> options.leftKey;
            case MOVE_RIGHT -> options.rightKey;
            case CLICK_LEFT -> options.attackKey;
            case CLICK_RIGHT -> options.useKey;
            case JUMP -> options.jumpKey;
            case SNEAK -> options.sneakKey;
            case SPRINT -> options.sprintKey;
            default -> null;
        };
    }

    private static String safeValue(java.util.function.Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(supplier, true);
    }
}
