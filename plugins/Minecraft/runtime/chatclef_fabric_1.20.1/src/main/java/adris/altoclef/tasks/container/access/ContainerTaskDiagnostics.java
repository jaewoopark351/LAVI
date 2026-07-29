package adris.altoclef.tasks.container.access;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.helpers.StorageHelper;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Optional;

//20260729_kpopmodder: Added this helper to keep container task diagnostic formatting out of behavior code.
public final class ContainerTaskDiagnostics {

    private ContainerTaskDiagnostics() {
    }

    public static String describeOptionalPos(Optional<BlockPos> pos) {
        return pos.map(BlockPos::toShortString).orElse("none");
    }

    public static String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

    public static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + " x " + stack.getCount();
    }

    public static String describeInteractionContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "context=missing-client";
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return "player=" + mod.getPlayer().getBlockPos().toShortString()
                + ", screen=" + (screen == null ? "none" : screen.getClass().getSimpleName())
                + ", handler=" + (mod.getPlayer().currentScreenHandler == null
                ? "none"
                : mod.getPlayer().currentScreenHandler.getClass().getSimpleName())
                + ", cursor=" + describeStack(StorageHelper.getItemStackInCursorSlot())
                + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                + ", breaking=" + mod.getControllerExtras().isBreakingBlock()
                + ", " + describeInputState(mod)
                + ", carriedBlock=" + describeCarriedBlock(mod);
    }

    public static String describeInputState(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "input=context-missing";
        }
        return "playerSneaking=" + mod.getPlayer().isSneaking()
                + ", inputSneaking=" + mod.getPlayer().input.sneaking
                + ", sneakKeyHeld=" + mod.getInputControls().isHeldDown(Input.SNEAK)
                + ", useKeyHeld=" + mod.getInputControls().isHeldDown(Input.CLICK_RIGHT);
    }

    public static String describeCarriedBlock(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "context-missing";
        }
        try {
            Optional<BlockState> carried = CarryOnCompat.getCarriedBlockState(mod.getPlayer());
            return carried.map(state -> state.getBlock().getTranslationKey()).orElse("none");
        } catch (RuntimeException | LinkageError e) {
            return "read-failed:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    public static String describeTask(Task task) {
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getSimpleName() + "{" + task + "}";
        } catch (RuntimeException ex) {
            return task.getClass().getSimpleName() + "{debugString failed: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "}";
        }
    }

    public static String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
