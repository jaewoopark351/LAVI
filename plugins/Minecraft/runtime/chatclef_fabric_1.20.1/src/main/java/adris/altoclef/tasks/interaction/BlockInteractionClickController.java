package adris.altoclef.tasks.interaction;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
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
    private int clickTickCount;
    private int pressAttemptCount;

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
        clickTickCount++;
        debugLogger.state("right click tick:" + clickTickCount,
                "right click tick=" + clickTickCount
                        + ", target=" + target.toShortString()
                        + ", direction=" + direction
                        + ", toUse=" + toUse
                        + ", input=" + interactInput
                        + ", shiftClick=" + shiftClick
                        + ", " + describeInteractionContext(mod));

        // Don't interact if baritone can't interact.
        if (mod.getExtraBaritoneSettings().isInteractionPaused() || mod.getFoodChain().needsToEat() ||
                mod.getPlayer().isBlocking()) {
            debugLogger.state("right click paused:" + clickTickCount,
                    "right click paused: tick=" + clickTickCount
                    + ", interactionPaused=" + mod.getExtraBaritoneSettings().isInteractionPaused()
                    + ", needsToEat=" + mod.getFoodChain().needsToEat()
                    + ", blocking=" + mod.getPlayer().isBlocking()
                    + ", " + describeInteractionContext(mod));
            return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
        }

        // We can't interact while a screen is open.
        if (!StorageHelper.isPlayerInventoryOpen()) {
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                debugLogger.state("right click delayed cursor:" + clickTickCount,
                        "right click delayed: tick=" + clickTickCount
                                + ", clearing cursor=" + describeStack(cursorStack)
                                + ", " + describeInteractionContext(mod));
                Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    debugLogger.state("right click cursor move inventory:" + clickTickCount,
                            "right click cursor move inventory: tick=" + clickTickCount
                                    + ", cursor=" + describeStack(cursorStack)
                                    + ", destination=" + moveTo.get()
                                    + ", " + describeInteractionContext(mod));
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
                }
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    debugLogger.state("right click cursor throwaway direct:" + clickTickCount,
                            "right click cursor throwaway direct: tick=" + clickTickCount
                                    + ", cursor=" + describeStack(cursorStack)
                                    + ", " + describeInteractionContext(mod));
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    debugLogger.state("right click cursor throwaway garbage:" + clickTickCount,
                            "right click cursor throwaway garbage: tick=" + clickTickCount
                                    + ", cursor=" + describeStack(cursorStack)
                                    + ", garbageSlot=" + garbage.get()
                                    + ", " + describeInteractionContext(mod));
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
                }
                debugLogger.state("right click cursor drop undefined:" + clickTickCount,
                        "right click cursor drop undefined: tick=" + clickTickCount
                                + ", cursor=" + describeStack(cursorStack)
                                + ", " + describeInteractionContext(mod));
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
            } else {
                debugLogger.state("right click close screen:" + clickTickCount,
                        "right click delayed: closing open screen before interacting: tick=" + clickTickCount
                                + ", " + describeInteractionContext(mod));
                StorageHelper.closeScreen();
            }
        }

        Optional<Rotation> reachable = getCurrentReach();
        boolean lookingAtTarget = reachable.isPresent() && LookHelper.isLookingAt(mod, target);
        debugLogger.state("right click reach check:" + clickTickCount,
                "right click reach check: tick=" + clickTickCount
                        + ", reachable=" + reachable.isPresent()
                        + ", lookingAtTarget=" + lookingAtTarget
                        + ", rotation=" + reachable.map(Object::toString).orElse("none")
                        + ", target=" + target.toShortString()
                        + ", direction=" + direction
                        + ", " + describeInteractionContext(mod));
        if (reachable.isPresent()) {
            if (lookingAtTarget) {
                if (toUse != null) {
                    debugLogger.state("right click force equip:" + clickTickCount,
                            "right click force equip: tick=" + clickTickCount
                                    + ", item=" + toUse
                                    + ", " + describeInteractionContext(mod));
                    mod.getSlotHandler().forceEquipItem(toUse, false);
                } else {
                    debugLogger.state("right click force deequip:" + clickTickCount,
                            "right click force deequip: tick=" + clickTickCount
                                    + ", " + describeInteractionContext(mod));
                    mod.getSlotHandler().forceDeequipRightClickableItem();
                }
                pressAttemptCount++;
                debugLogger.state("right click pressing input:" + pressAttemptCount,
                        "right click pressing input: tick=" + clickTickCount
                        + ", pressAttempt=" + pressAttemptCount
                        + ", target=" + target.toShortString()
                        + ", input=" + interactInput
                        + ", shiftClick=" + shiftClick
                        + ", preClick=" + describeInputAndCarryContext(mod));
                mod.getInputControls().tryPress(interactInput);
                boolean interactHeld = mod.getInputControls().isHeldDown(interactInput);
                debugLogger.state("right click press result:" + pressAttemptCount,
                        "right click press result: tick=" + clickTickCount
                                + ", pressAttempt=" + pressAttemptCount
                                + ", target=" + target.toShortString()
                                + ", input=" + interactInput
                                + ", interactHeld=" + interactHeld
                                + ", shiftClick=" + shiftClick
                                + ", " + describeInputAndCarryContext(mod));
                if (interactHeld) {
                    if (shiftClick) {
                        mod.getInputControls().hold(Input.SNEAK);
                        debugLogger.state("right click shift held after press:" + pressAttemptCount,
                                "right click shift held after press: tick=" + clickTickCount
                                        + ", pressAttempt=" + pressAttemptCount
                                        + ", " + describeInputAndCarryContext(mod));
                    }
                    return InteractWithBlockTask.ClickResponse.CLICK_ATTEMPTED;
                }
                //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(_interactInput, true);
            } else {
                debugLogger.state("right click look toward reachable:" + clickTickCount,
                        "right click delayed: looking at reachable rotation: tick=" + clickTickCount
                                + ", target=" + target.toShortString()
                                + ", rotation=" + reachable.get()
                                + ", " + describeInteractionContext(mod));
                LookHelper.lookAt(reachable.get());
            }
            return InteractWithBlockTask.ClickResponse.WAIT_FOR_CLICK;
        }
        if (shiftClick) {
            mod.getInputControls().release(Input.SNEAK);
            debugLogger.state("right click released shift no reach:" + clickTickCount,
                    "right click released shift because target is unreachable: tick=" + clickTickCount
                            + ", " + describeInputAndCarryContext(mod));
        }
        debugLogger.state("right click cannot reach:" + clickTickCount,
                "right click cannot reach target=" + target.toShortString()
                        + ", tick=" + clickTickCount
                        + ", direction=" + direction
                        + ", " + describeInteractionContext(mod));
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
                + ", shiftClick=" + shiftClick
                + ", " + describeInputAndCarryContext(mod);
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

    private String describeInputAndCarryContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "input=context-missing, carriedBlock=context-missing";
        }
        return "playerSneaking=" + mod.getPlayer().isSneaking()
                + ", inputSneaking=" + mod.getPlayer().input.sneaking
                + ", sneakKeyHeld=" + mod.getInputControls().isHeldDown(Input.SNEAK)
                + ", useKeyHeld=" + mod.getInputControls().isHeldDown(Input.CLICK_RIGHT)
                + ", carriedBlock=" + describeCarriedBlock(mod);
    }

    private String describeCarriedBlock(AltoClef mod) {
        try {
            Optional<BlockState> carried = CarryOnCompat.getCarriedBlockState(mod.getPlayer());
            return carried.map(state -> state.getBlock().getTranslationKey()).orElse("none");
        } catch (RuntimeException | LinkageError e) {
            return "read-failed:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }
}
