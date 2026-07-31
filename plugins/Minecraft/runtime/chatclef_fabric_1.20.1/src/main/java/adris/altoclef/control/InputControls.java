package adris.altoclef.control;

import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Sometimes we want to trigger a "press" for one frame, or do other input forcing.
 * <p>
 * Dealing with keeping track of a press and timing each time you do this is annoying.
 * <p>
 * For some reason using baritone's "Forcestate" doesn't always work, perhaps that's my bad.
 * <p>
 * But this will alleviate all confusion.
 */
@SuppressWarnings("UnnecessaryDefault")
public class InputControls {

    private final Queue<Input> toUnpress = new ArrayDeque<>();
    private final Set<Input> _waitForRelease = new HashSet<>(); // a click requires a release.

    private static KeyBinding inputToKeyBinding(Input input) {
        GameOptions o = MinecraftClient.getInstance().options;
        return switch (input) {
            case MOVE_FORWARD -> o.forwardKey;
            case MOVE_BACK -> o.backKey;
            case MOVE_LEFT -> o.leftKey;
            case MOVE_RIGHT -> o.rightKey;
            case CLICK_LEFT -> o.attackKey;
            case CLICK_RIGHT -> o.useKey;
            case JUMP -> o.jumpKey;
            case SNEAK -> o.sneakKey;
            case SPRINT -> o.sprintKey;
            default -> throw new IllegalArgumentException("Invalid key input/not accounted for: " + input);
        };
    }

    public void tryPress(Input input) {
        // We just pressed, so let us release.
        if (_waitForRelease.contains(input)) {
            ChatClefDiagnostics.logInput("SUPPRESSED", "waitForRelease_suppression", input,
                    "inputRequested", true,
                    "inputAccepted", false,
                    "inputHeldBefore", "unavailable",
                    "waitForReleaseSuppression", true,
                    "waitForReleaseCount", _waitForRelease.size(),
                    "autoReleaseQueueCount", toUnpress.size());
            return;
        }
        boolean heldBefore = inputToKeyBinding(input).isPressed();
        ChatClefDiagnostics.logInput("REQUEST", "tryPress_begin", input,
                "inputRequested", true,
                "inputAccepted", false,
                "inputHeldBefore", heldBefore,
                "waitForReleaseSuppression", false,
                "waitForReleaseCountBefore", _waitForRelease.size(),
                "autoReleaseQueueCountBefore", toUnpress.size());
        inputToKeyBinding(input).setPressed(true);
        // Also necessary to ensure the game registers the input as "pressed"
        KeyBinding.onKeyPressed(inputToKeyBinding(input).getDefaultKey());
        toUnpress.add(input);
        _waitForRelease.add(input);
        ChatClefDiagnostics.logInput("ACCEPTED", "tryPress_end", input,
                "inputRequested", true,
                "inputAccepted", true,
                "inputHeldBefore", heldBefore,
                "inputHeldAfter", ChatClefDiagnostics.safeValue(() -> inputToKeyBinding(input).isPressed()),
                "queuedForAutoRelease", true,
                "waitForReleaseSuppression", false,
                "waitForReleaseCountAfter", _waitForRelease.size(),
                "autoReleaseQueueCountAfter", toUnpress.size());
    }

    public void hold(Input input) {
        boolean heldBefore = inputToKeyBinding(input).isPressed();
        ChatClefDiagnostics.logInput("REQUEST", "hold_begin", input,
                "inputRequested", true,
                "inputAccepted", false,
                "inputHeldBefore", heldBefore,
                "waitForReleaseCountBefore", _waitForRelease.size(),
                "autoReleaseQueueCountBefore", toUnpress.size());
        if (!heldBefore) {
            KeyBinding.onKeyPressed(inputToKeyBinding(input).getDefaultKey());
        }
        inputToKeyBinding(input).setPressed(true);
        ChatClefDiagnostics.logInput("HELD", "hold_end", input,
                "inputRequested", true,
                "inputAccepted", true,
                "inputHeldBefore", heldBefore,
                "inputHeldAfter", ChatClefDiagnostics.safeValue(() -> inputToKeyBinding(input).isPressed()),
                "waitForReleaseCountAfter", _waitForRelease.size(),
                "autoReleaseQueueCountAfter", toUnpress.size());
    }

    public void release(Input input) {
        boolean heldBefore = inputToKeyBinding(input).isPressed();
        inputToKeyBinding(input).setPressed(false);
        ChatClefDiagnostics.logInput("RELEASED", "release_end", input,
                "inputReleased", true,
                "inputHeldBefore", heldBefore,
                "inputHeldAfter", ChatClefDiagnostics.safeValue(() -> inputToKeyBinding(input).isPressed()),
                "waitForReleaseCountAfter", _waitForRelease.size(),
                "autoReleaseQueueCountAfter", toUnpress.size());
    }

    public boolean isHeldDown(Input input) {
        return inputToKeyBinding(input).isPressed();
    }

    public void forceLook(float yaw, float pitch) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.setYaw(yaw);
            MinecraftClient.getInstance().player.setPitch(pitch);
        }
    }

    // Before the user calls input commands for the frame
    public void onTickPre() {
        ChatClefDiagnostics.logInputSnapshot("TICK_PRE_BEGIN", "input_controls_tick_pre_begin",
                "waitForReleaseCount", _waitForRelease.size(),
                "autoReleaseQueueCount", toUnpress.size());
        while (!toUnpress.isEmpty()) {
            Input input = toUnpress.remove();
            boolean heldBefore = inputToKeyBinding(input).isPressed();
            inputToKeyBinding(input).setPressed(false);
            ChatClefDiagnostics.logInput("AUTO_RELEASED", "onTickPre_auto_release", input,
                    "inputAutoReleased", true,
                    "inputHeldBefore", heldBefore,
                    "inputHeldAfter", ChatClefDiagnostics.safeValue(() -> inputToKeyBinding(input).isPressed()),
                    "waitForReleaseCount", _waitForRelease.size(),
                    "autoReleaseQueueCount", toUnpress.size());
        }
        ChatClefDiagnostics.logInputSnapshot("TICK_PRE_END", "input_controls_tick_pre_end",
                "waitForReleaseCount", _waitForRelease.size(),
                "autoReleaseQueueCount", toUnpress.size());
    }

    // After the user calls input commands for the frame
    public void onTickPost() {
        ChatClefDiagnostics.logInputSnapshot("TICK_POST_BEGIN", "input_controls_tick_post_begin",
                "waitForReleaseCount", _waitForRelease.size(),
                "autoReleaseQueueCount", toUnpress.size());
        if (!_waitForRelease.isEmpty()) {
            ChatClefDiagnostics.logEvent("INPUT", "WAIT_FOR_RELEASE_CLEAR", "onTickPost_clear", null,
                    "waitForReleaseCount", _waitForRelease.size());
        }
        _waitForRelease.clear();
        ChatClefDiagnostics.logInputSnapshot("TICK_POST_END", "input_controls_tick_post_end",
                "waitForReleaseCount", _waitForRelease.size(),
                "autoReleaseQueueCount", toUnpress.size());
    }
}
