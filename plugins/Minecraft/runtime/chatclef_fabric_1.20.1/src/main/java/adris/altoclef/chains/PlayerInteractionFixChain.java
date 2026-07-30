package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

public class PlayerInteractionFixChain extends TaskChain {
    private final TimerGame stackHeldTimeout = new TimerGame(1);
    private final TimerGame generalDuctTapeSwapTimeout = new TimerGame(30);
    private final TimerGame shiftDepressTimeout = new TimerGame(10);
    private final TimerGame betterToolTimer = new TimerGame(0);
    private final TimerGame mouseMovingButScreenOpenTimeout = new TimerGame(1);
    private ItemStack lastHandStack = null;

    private Screen lastScreen;
    private Rotation lastLookRotation;

    public PlayerInteractionFixChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onStop() {

    }

    @Override
    public void onInterrupt(TaskChain other) {

    }

    @Override
    protected void onTick() {
    }

    @Override
    public float getPriority() {
        if (!AltoClef.inGame()) {
            ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "PRIORITY_RETURN", "not_in_game", null);
            return Float.NEGATIVE_INFINITY;
        }

        AltoClef mod = AltoClef.getInstance();
        ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "PRIORITY_BEGIN", "getPriority_begin", null,
                "userTaskChainActive", ChatClefDiagnostics.safeValue(() -> mod.getUserTaskChain().isActive()),
                "isBreakingBlock", ChatClefDiagnostics.safeValue(() -> mod.getControllerExtras().isBreakingBlock()));

        if (mod.getUserTaskChain().isActive() && betterToolTimer.elapsed()) {
            // Equip the right tool for the job if we're not using one.
            betterToolTimer.reset();
            if (mod.getControllerExtras().isBreakingBlock()) {
                BlockState state = mod.getWorld().getBlockState(mod.getControllerExtras().getBreakingBlockPos());
                Optional<Slot> bestToolSlot = StorageHelper.getBestToolSlot(mod, state);
                Slot currentEquipped = PlayerSlot.getEquipSlot();

                // if baritone is running, only accept tools OUTSIDE OF HOTBAR!
                // Baritone will take care of tools inside the hotbar.
                if (bestToolSlot.isPresent() && !bestToolSlot.get().equals(currentEquipped)) {
                    // ONLY equip if the item class is STRICTLY different (otherwise we swap around a lot)
                    if (StorageHelper.getItemStackInSlot(currentEquipped).getItem() != StorageHelper.getItemStackInSlot(bestToolSlot.get()).getItem()) {
                        boolean isAllowedToManage = (!mod.getClientBaritone().getPathingBehavior().isPathing() ||
                                bestToolSlot.get().getInventorySlot() >= 9) && !mod.getFoodChain().isTryingToEat();
                        if (isAllowedToManage) {
                            Debug.logMessage("Found better tool in inventory, equipping.");
                            ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "force_equip_better_tool", null,
                                    "bestToolSlot", bestToolSlot.get(),
                                    "currentEquipped", currentEquipped,
                                    "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));
                            ItemStack bestToolItemStack = StorageHelper.getItemStackInSlot(bestToolSlot.get());
                            Item bestToolItem = bestToolItemStack.getItem();
                            mod.getSlotHandler().forceEquipItem(bestToolItem);
                        }
                    }
                }
            }
        }

        // Unpress shift (it gets stuck for some reason???)
        if (mod.getInputControls().isHeldDown(Input.SNEAK)) {
            if (shiftDepressTimeout.elapsed()) {
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "player_interaction_fix_chain_shift_depress_release", Input.SNEAK,
                        "inputReleased", true,
                        "inputAutoReleased", false);
                mod.getInputControls().release(Input.SNEAK);
            }
        } else {
            shiftDepressTimeout.reset();
        }

        // Refresh inventory
        if (generalDuctTapeSwapTimeout.elapsed()) {
            if (!mod.getControllerExtras().isBreakingBlock()) {
                Debug.logMessage("Refreshed inventory...");
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "refresh_inventory", null);
                mod.getSlotHandler().refreshInventory();
                generalDuctTapeSwapTimeout.reset();
                return Float.NEGATIVE_INFINITY;
            }
        }

        ItemStack currentStack = StorageHelper.getItemStackInCursorSlot();

        if (currentStack != null && !currentStack.isEmpty()) {
            //noinspection PointlessNullCheck
            if (lastHandStack == null || !ItemStack.areEqual(currentStack, lastHandStack)) {
                // We're holding a new item in our stack!
                stackHeldTimeout.reset();
                lastHandStack = currentStack.copy();
            }
        } else {
            stackHeldTimeout.reset();
            lastHandStack = null;
        }

        // If we have something in our hand for a period of time...
        if (lastHandStack != null && stackHeldTimeout.elapsed()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(lastHandStack, false);
            if (moveTo.isPresent()) {
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "cursor_stack_move_to_inventory", null,
                        "cursorStack", lastHandStack,
                        "slot", moveTo.get());
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return Float.NEGATIVE_INFINITY;
            }
            if (ItemHelper.canThrowAwayStack(mod, StorageHelper.getItemStackInCursorSlot())) {
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "cursor_stack_throwaway", null,
                        "cursorStack", lastHandStack);
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return Float.NEGATIVE_INFINITY;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "cursor_stack_to_garbage_slot", null,
                        "cursorStack", lastHandStack,
                        "slot", garbage.get());
                mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return Float.NEGATIVE_INFINITY;
            }
            ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "cursor_stack_pickup_undefined", null,
                    "cursorStack", lastHandStack);
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return Float.NEGATIVE_INFINITY;
        }

        boolean closeOpenScreen = shouldCloseOpenScreen();
        ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SCREEN_CLOSE_DECISION", "shouldCloseOpenScreen_result", null,
                "shouldCloseOpenScreen", closeOpenScreen);
        if (closeOpenScreen) {
            //Debug.logMessage("Closed screen since we changed our look.");
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "screen_close_cursor_stack_move_to_inventory", null,
                            "cursorStack", cursorStack,
                            "slot", moveTo.get());
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return Float.NEGATIVE_INFINITY;
                }
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "screen_close_cursor_stack_throwaway", null,
                            "cursorStack", cursorStack);
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return Float.NEGATIVE_INFINITY;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "screen_close_cursor_stack_to_garbage_slot", null,
                            "cursorStack", cursorStack,
                            "slot", garbage.get());
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return Float.NEGATIVE_INFINITY;
                }
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "screen_close_cursor_stack_pickup_undefined", null,
                        "cursorStack", cursorStack);
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SIDE_EFFECT", "close_screen", null,
                        "cursorStackEmpty", true);
                StorageHelper.closeScreen();
            }
            return Float.NEGATIVE_INFINITY;
        }

        return Float.NEGATIVE_INFINITY;
    }

    private boolean shouldCloseOpenScreen() {
        ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SHOULD_CLOSE_SCREEN_BEGIN", "shouldCloseOpenScreen_begin", null);
        if (!AltoClef.getInstance().getModSettings().shouldCloseScreenWhenLookingOrMining()) {
            ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SHOULD_CLOSE_SCREEN_RETURN", "setting_disabled", null,
                    "shouldCloseOpenScreen", false);
            return false;
        }

        // Only check look if we've had the same screen open for a while
        Screen openScreen = MinecraftClient.getInstance().currentScreen;
        if (openScreen != lastScreen) {
            ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SHOULD_CLOSE_SCREEN_STATE", "screen_changed_reset_timer", null,
                    "openScreenClass", ChatClefDiagnostics.className(openScreen),
                    "lastScreenClass", ChatClefDiagnostics.className(lastScreen));
            mouseMovingButScreenOpenTimeout.reset();
        }
        // We're in the player screen/a screen we DON'T want to cancel out of
        if (openScreen == null || openScreen instanceof ChatScreen || openScreen instanceof GameMenuScreen || openScreen instanceof DeathScreen) {
            mouseMovingButScreenOpenTimeout.reset();
            ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SHOULD_CLOSE_SCREEN_RETURN", "screen_exempt_or_none", null,
                    "openScreenClass", ChatClefDiagnostics.className(openScreen),
                    "shouldCloseOpenScreen", false);
            return false;
        }
        // Check for rotation change
        Rotation look = LookHelper.getLookRotation();
        if (lastLookRotation != null && mouseMovingButScreenOpenTimeout.elapsed()) {
            Rotation delta = look.subtract(lastLookRotation);
            if (Math.abs(delta.getYaw()) > 0.1f || Math.abs(delta.getPitch()) > 0.1f) {
                lastLookRotation = look;
                ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SHOULD_CLOSE_SCREEN_RETURN", "look_delta_exceeded", null,
                        "openScreenClass", ChatClefDiagnostics.className(openScreen),
                        "deltaYaw", delta.getYaw(),
                        "deltaPitch", delta.getPitch(),
                        "shouldCloseOpenScreen", true);
                return true;
            }
            // do NOT update our last look rotation, just because we want to measure long term rotation.
        } else {
            lastLookRotation = look;
        }
        lastScreen = openScreen;
        ChatClefDiagnostics.logEvent("PLAYER_INTERACTION_FIX_CHAIN", "SHOULD_CLOSE_SCREEN_RETURN", "look_delta_not_exceeded", null,
                "openScreenClass", ChatClefDiagnostics.className(openScreen),
                "shouldCloseOpenScreen", false);
        return false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getName() {
        return "Hand Stack Fix Chain";
    }
}
