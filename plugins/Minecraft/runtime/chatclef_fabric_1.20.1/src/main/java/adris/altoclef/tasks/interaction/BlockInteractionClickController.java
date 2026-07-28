package adris.altoclef.tasks.interaction;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

//20260728_kpopmodder: Isolate block-click execution from navigation and retry policy in InteractWithBlockTask.
class BlockInteractionClickController {
    private final ItemTarget toUse;
    private final Direction direction;
    private final BlockPos target;
    private final Input interactInput;
    private final boolean shiftClick;
    private final StateChangeLogger debugLogger;

    BlockInteractionClickController(ItemTarget toUse,
                                    Direction direction,
                                    BlockPos target,
                                    Input interactInput,
                                    boolean shiftClick,
                                    StateChangeLogger debugLogger) {
        this.toUse = toUse;
        this.direction = direction;
        this.target = target;
        this.interactInput = interactInput;
        this.shiftClick = shiftClick;
        this.debugLogger = debugLogger;
    }

    InteractWithBlockTask.ClickResponse click(AltoClef mod) {

        // Don't interact if baritone can't interact.
        if (mod.getExtraBaritoneSettings().isInteractionPaused() || mod.getFoodChain().needsToEat() ||
                mod.getPlayer().isBlocking()) {
            debugLogger.state("right click paused: interactionPaused=" + mod.getExtraBaritoneSettings().isInteractionPaused()
                    + ", needsToEat=" + mod.getFoodChain().needsToEat()
                    + ", blocking=" + mod.getPlayer().isBlocking()
                    + ", " + describeInteractionContext(mod));
            return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
        }

        // We can't interact while a screen is open.
        if (!StorageHelper.isPlayerInventoryOpen()) {
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                debugLogger.state("right click delayed: clearing cursor=" + describeStack(cursorStack),
                        "right click delayed: clearing cursor=" + describeStack(cursorStack)
                                + ", " + describeInteractionContext(mod));
                Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
                }
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
                }
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
            } else {
                debugLogger.state("right click delayed: closing open screen before interacting",
                        "right click delayed: closing open screen before interacting: " + describeInteractionContext(mod));
                StorageHelper.closeScreen();
            }
        }

        Optional<Rotation> reachable = getCurrentReach();
        if (reachable.isPresent()) {
            if (LookHelper.isLookingAt(mod, target)) {
                if (toUse != null) {
                    mod.getSlotHandler().forceEquipItem(toUse, false);
                } else {
                    mod.getSlotHandler().forceDeequipRightClickableItem();
                }
                debugLogger.state("right click pressing input: target=" + target.toShortString()
                        + ", input=" + interactInput
                        + ", shiftClick=" + shiftClick);
                mod.getInputControls().tryPress(interactInput);
                if (mod.getInputControls().isHeldDown(interactInput)) {
                    if (shiftClick) {
                        mod.getInputControls().hold(Input.SNEAK);
                    }
                    return InteractWithBlockTask.ClickResponse.CLICK_ATTEMPTED;
                }
                //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(_interactInput, true);
            } else {
                debugLogger.state("right click delayed: looking at reachable rotation for target=" + target.toShortString());
                LookHelper.lookAt(reachable.get());
            }
            return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
        }
        if (shiftClick) {
            mod.getInputControls().release(Input.SNEAK);
        }
        debugLogger.state("right click cannot reach target=" + target.toShortString() + ", direction=" + direction);
        return InteractWithBlockTask.ClickResponse.CANT_REACH;
    }

    Optional<Rotation> getCurrentReach() {
        return LookHelper.getReach(target, direction);
    }

    String describeInteractionContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "context=missing-client";
        }
        return "player=" + mod.getPlayer().getBlockPos().toShortString()
                + ", screen=" + describeCurrentScreen()
                + ", screenHandler=" + describeScreenHandler(mod)
                + ", cursor=" + describeStack(StorageHelper.getItemStackInCursorSlot())
                + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                + ", shiftClick=" + shiftClick;
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + " x " + stack.getCount();
    }

    private String describeCurrentScreen() {
        Object screen = MinecraftClient.getInstance().currentScreen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private String describeScreenHandler(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null || mod.getPlayer().currentScreenHandler == null) {
            return "none";
        }
        return mod.getPlayer().currentScreenHandler.getClass().getSimpleName();
    }
}
