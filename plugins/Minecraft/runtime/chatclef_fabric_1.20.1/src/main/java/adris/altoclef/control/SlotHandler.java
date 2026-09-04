package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.container.gui.slot.ContainerSlotActionProbe;
import lavi.minecraft.diagnostics.toolselect.ToolEquipDiagnostics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;


public class SlotHandler {

    private final AltoClef mod;

    private final TimerGame slotActionTimer = new TimerGame(0);
    private boolean overrideTimerOnce = false;

    public SlotHandler(AltoClef mod) {
        this.mod = mod;
    }

    private void forceAllowNextSlotAction() {
        overrideTimerOnce = true;
    }

    public boolean canDoSlotAction() {
        if (overrideTimerOnce) {
            overrideTimerOnce = false;
            ChatClefDiagnostics.logEvent("SLOT", "CAN_DO_ACTION", "override_timer_once_allowed", null,
                    "overrideTimerOnce", true);
            return true;
        }
        slotActionTimer.setInterval(mod.getModSettings().getContainerItemMoveDelay());
        boolean allowed = slotActionTimer.elapsed();
        ChatClefDiagnostics.logEvent("SLOT", "CAN_DO_ACTION", "slot_action_timer_check", null,
                "allowed", allowed,
                "containerItemMoveDelay", mod.getModSettings().getContainerItemMoveDelay());
        return allowed;
    }

    public void registerSlotAction() {
        ChatClefDiagnostics.logEvent("SLOT", "REGISTER_ACTION", "slot_action_registered", null);
        mod.getItemStorage().registerSlotAction();
        slotActionTimer.reset();
    }


    public void clickSlot(Slot slot, int mouseButton, SlotActionType type) {
        ChatClefDiagnostics.logSlotClick("REQUEST", "clickSlot_requested", slot, mouseButton, type,
                "slotStackBefore", ChatClefDiagnostics.slotStackSummary(slot));
        if (!canDoSlotAction()) {
            ChatClefDiagnostics.logSlotClick("SUPPRESSED", "clickSlot_timer_blocked", slot, mouseButton, type);
            return;
        }

        if (slot.getWindowSlot() == -1) {
            ChatClefDiagnostics.logSlotClick("REDIRECT", "clickSlot_cursor_redirect_to_undefined", slot, mouseButton, type);
            clickSlot(PlayerSlot.UNDEFINED, 0, SlotActionType.PICKUP);
            return;
        }
        // NOT THE CASE! We may have something in the cursor slot to place.
        //if (getItemStackInSlot(slot).isEmpty()) return getItemStackInSlot(slot);

        ChatClefDiagnostics.logSlotClick("ACCEPTED", "clickSlot_before_window_click", slot, mouseButton, type,
                "windowSlot", slot.getWindowSlot(),
                "slotStackBefore", ChatClefDiagnostics.slotStackSummary(slot));
        clickWindowSlot(slot.getWindowSlot(), mouseButton, type);
        ChatClefDiagnostics.logSlotClick("RETURN", "clickSlot_after_window_click", slot, mouseButton, type,
                "windowSlot", slot.getWindowSlot(),
                "slotStackAfter", ChatClefDiagnostics.slotStackSummary(slot));
    }

    private void clickSlotForce(Slot slot, int mouseButton, SlotActionType type) {
        forceAllowNextSlotAction();
        clickSlot(slot, mouseButton, type);
    }

    private void clickWindowSlot(int windowSlot, int mouseButton, SlotActionType type) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            ChatClefDiagnostics.logEvent("SLOT", "RETURN", "clickWindowSlot_no_player", null,
                    "windowSlot", windowSlot,
                    "mouseButton", mouseButton,
                    "slotActionType", type);
            return;
        }
        registerSlotAction();
        ScreenHandler handler = player.currentScreenHandler;
        int syncId = handler.syncId;
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        ContainerSlotActionProbe containerProbe = ContainerGuiDiagnostics.beginSlotAction(
                handler,
                syncId,
                windowSlot,
                mouseButton,
                type,
                player
        );

        try {
            ChatClefDiagnostics.logEvent("SLOT", "WINDOW_CLICK_HEAD", "controller_clickSlot_begin", null,
                    "syncId", syncId,
                    "windowSlot", windowSlot,
                    "mouseButton", mouseButton,
                    "slotActionType", type,
                    "cursorStackBefore", ChatClefDiagnostics.safeValue(() -> player.currentScreenHandler.getCursorStack()));
            mod.getController().clickSlot(syncId, windowSlot, mouseButton, type, player);
            containerProbe.returned();
            ChatClefDiagnostics.logEvent("SLOT", "WINDOW_CLICK_RETURN", "controller_clickSlot_end", null,
                    "syncId", syncId,
                    "windowSlot", windowSlot,
                    "mouseButton", mouseButton,
                    "slotActionType", type,
                    "cursorStackAfter", ChatClefDiagnostics.safeValue(() -> player.currentScreenHandler.getCursorStack()));
        } catch (Exception e) {
            containerProbe.failed(e);
            ChatClefDiagnostics.logEvent("SLOT", "WINDOW_CLICK_EXCEPTION", "controller_clickSlot_exception", null,
                    "syncId", syncId,
                    "windowSlot", windowSlot,
                    "mouseButton", mouseButton,
                    "slotActionType", type,
                    "exceptionType", e.getClass().getName());
            Debug.logWarning("Slot Click Error (ignored)");
            e.printStackTrace();
        }
    }

    public void forceEquipItemToOffhand(Item toEquip) {
        if (StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT).getItem() == toEquip) {
            return;
        }
        List<Slot> currentItemSlot = mod.getItemStorage().getSlotsWithItemPlayerInventory(false,
                toEquip);
        for (Slot CurrentItemSlot : currentItemSlot) {
            if (!Slot.isCursor(CurrentItemSlot)) {
                mod.getSlotHandler().clickSlot(CurrentItemSlot, 0, SlotActionType.PICKUP);
            } else {
                mod.getSlotHandler().clickSlot(PlayerSlot.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            }
        }
    }

    public boolean forceEquipItem(Item toEquip) {
        return forceEquipItem(toEquip, -1L, null, null);
    }

    public boolean forceEquipItem(Item toEquip, long equipAttemptId, Slot expectedSourceSlot, ItemStack expectedSourceStack) {
        int selectedSlotBefore = selectedSlotIndex();
        Slot hotbarSlot1 = Slot.getFromCurrentScreenInventory(1);
        ItemStack hotbarSlot1Before = copyStackInSlot(hotbarSlot1);
        Slot equipSlotBefore = currentEquipSlot();
        ItemStack mainHandBefore = copyStackInSlot(equipSlotBefore);
        ItemStack expectedSourceStackSnapshot = expectedSourceStack == null ? copyStackInSlot(expectedSourceSlot) : expectedSourceStack.copy();

        ChatClefDiagnostics.logEvent("SLOT", "FORCE_EQUIP_BEGIN", "forceEquipItem_begin", null,
                "toEquip", toEquip,
                "selectedSlotBefore", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getInventory().selectedSlot),
                "equippedBefore", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot())),
                "equipAttemptId", equipAttemptId,
                "expectedSourceSlot", ChatClefDiagnostics.slotSummary(expectedSourceSlot));

        // Already equipped
        if (StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem() == toEquip) {
            ChatClefDiagnostics.logEvent("SLOT", "FORCE_EQUIP_RETURN", "forceEquipItem_already_equipped", null,
                    "toEquip", toEquip);
            ToolEquipDiagnostics.logEquipResult(mod, toEquip, equipAttemptId, expectedSourceSlot, expectedSourceStackSnapshot,
                    java.util.Collections.emptyList(), false, selectedSlotBefore, selectedSlotIndex(),
                    hotbarSlot1Before, copyStackInSlot(hotbarSlot1), mainHandBefore, copyStackInSlot(currentEquipSlot()),
                    true, "already_equipped");
            return true;
        }

        // Always equip to the second slot. First + last is occupied by baritone.
        mod.getPlayer().getInventory().selectedSlot = 1;

        // If our item is in our cursor, simply move it to the hotbar.
        boolean inCursor = StorageHelper.getItemStackInSlot(CursorSlot.SLOT).getItem() == toEquip;

        List<Slot> itemSlots = mod.getItemStorage().getSlotsWithItemScreen(toEquip);
        if (!itemSlots.isEmpty()) {
            for (Slot ItemSlots : itemSlots) {
                int hotbar = 1;
                //_mod.getPlayer().getInventory().swapSlotWithHotbar();
                ChatClefDiagnostics.logSlotClick("FORCE_EQUIP_CLICK", "forceEquipItem_click_source_slot", ItemSlots, inCursor ? 0 : hotbar, inCursor ? SlotActionType.PICKUP : SlotActionType.SWAP,
                        "toEquip", toEquip,
                        "inCursor", inCursor);
                clickSlotForce(Objects.requireNonNull(ItemSlots), inCursor ? 0 : hotbar, inCursor ? SlotActionType.PICKUP : SlotActionType.SWAP);
                //registerSlotAction();
            }
            ChatClefDiagnostics.logEvent("SLOT", "FORCE_EQUIP_RETURN", "forceEquipItem_success", null,
                    "toEquip", toEquip,
                    "selectedSlotAfter", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getInventory().selectedSlot),
                    "equippedAfter", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot())));
            ToolEquipDiagnostics.logEquipResult(mod, toEquip, equipAttemptId, expectedSourceSlot, expectedSourceStackSnapshot,
                    itemSlots, inCursor, selectedSlotBefore, selectedSlotIndex(),
                    hotbarSlot1Before, copyStackInSlot(hotbarSlot1), mainHandBefore, copyStackInSlot(currentEquipSlot()),
                    true, "matching_slots_swapped");
            return true;
        }
        ChatClefDiagnostics.logEvent("SLOT", "FORCE_EQUIP_RETURN", "forceEquipItem_missing_item", null,
                "toEquip", toEquip);
        ToolEquipDiagnostics.logEquipResult(mod, toEquip, equipAttemptId, expectedSourceSlot, expectedSourceStackSnapshot,
                itemSlots, inCursor, selectedSlotBefore, selectedSlotIndex(),
                hotbarSlot1Before, copyStackInSlot(hotbarSlot1), mainHandBefore, copyStackInSlot(currentEquipSlot()),
                false, "missing_item");
        return false;
    }

    private Slot currentEquipSlot() {
        try {
            return PlayerSlot.getEquipSlot();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private ItemStack copyStackInSlot(Slot slot) {
        try {
            return slot == null ? ItemStack.EMPTY : StorageHelper.getItemStackInSlot(slot).copy();
        } catch (RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private int selectedSlotIndex() {
        try {
            return mod.getPlayer().getInventory().selectedSlot;
        } catch (RuntimeException | LinkageError ignored) {
            return -1;
        }
    }

    public boolean forceDeequipHitTool() {
        return forceDeequip(stack -> stack.getItem() instanceof ToolItem);
    }

    public void forceDeequipRightClickableItem() {
        forceDeequip(stack -> {
                    Item item = stack.getItem();
                    return item instanceof BucketItem // water,lava,milk,fishes
                            || item instanceof EnderEyeItem
                            || item == Items.BOW
                            || item == Items.CROSSBOW
                            || item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE
                            || item == Items.ENDER_PEARL
                            || item instanceof FireworkRocketItem
                            || item instanceof SpawnEggItem
                            || item == Items.END_CRYSTAL
                            || item == Items.EXPERIENCE_BOTTLE
                            || item instanceof PotionItem // also includes splash/lingering
                            || item == Items.TRIDENT
                            || item == Items.WRITABLE_BOOK
                            || item == Items.WRITTEN_BOOK
                            || item instanceof FishingRodItem
                            || item instanceof OnAStickItem
                            || item == Items.COMPASS
                            || item instanceof EmptyMapItem
                            || item instanceof Equipment
                            || item == Items.LEAD
                            || item == Items.SHIELD;
                }
        );
    }

    /**
     * Tries to de-equip any item that we don't want equipped.
     *
     * @param isBad: Whether an item is bad/shouldn't be equipped
     * @return Whether we successfully de-equipped, or if we didn't have the item equipped at all.
     */
    public boolean forceDeequip(Predicate<ItemStack> isBad) {
        ItemStack equip = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot());
        ItemStack cursor = StorageHelper.getItemStackInSlot(CursorSlot.SLOT);
        if (isBad.test(cursor)) {
            // Throw away cursor slot OR move
            Optional<Slot> fittableSlots = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(equip, false);
            if (fittableSlots.isEmpty()) {
                // Try to swap items with the first non-bad slot.
                for (Slot slot : Slot.getCurrentScreenSlots()) {
                    if (!isBad.test(StorageHelper.getItemStackInSlot(slot))) {
                        clickSlotForce(slot, 0, SlotActionType.PICKUP);
                        return false;
                    }
                }
                if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                    clickSlotForce(PlayerSlot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return true;
                }
                // Can't throw :(
                return false;
            } else {
                // Put in the empty/available slot.
                clickSlotForce(fittableSlots.get(), 0, SlotActionType.PICKUP);
                return true;
            }
        } else if (isBad.test(equip)) {
            // Pick up the item
            clickSlotForce(PlayerSlot.getEquipSlot(), 0, SlotActionType.PICKUP);
            return false;
        } else if (equip.isEmpty() && !cursor.isEmpty()) {
            // cursor is good and equip is empty, so finish filling it in.
            clickSlotForce(PlayerSlot.getEquipSlot(), 0, SlotActionType.PICKUP);
            return true;
        }
        // We're already de-equipped
        return true;
    }

    public void forceEquipSlot(Slot slot) {
        Slot target = PlayerSlot.getEquipSlot();
        clickSlotForce(slot, target.getInventorySlot(), SlotActionType.SWAP);
    }

    public boolean forceEquipItem(Item[] matches, boolean unInterruptable) {
        return forceEquipItem(new ItemTarget(matches, 1), unInterruptable);
    }

    public boolean forceEquipItem(ItemTarget toEquip, boolean unInterruptable) {
        if (toEquip == null) return false;

        //If the bot try to eat
        if (mod.getFoodChain().needsToEat() && !unInterruptable) { //unless we really need to force equip the item
            return false; //don't equip the item for now
        }

        Slot target = PlayerSlot.getEquipSlot();
        // Already equipped
        if (toEquip.matches(StorageHelper.getItemStackInSlot(target).getItem())) return true;

        for (Item item : toEquip.getMatches()) {
            if (mod.getItemStorage().hasItem(item)) {
                if (forceEquipItem(item)) return true;
            }
        }
        return false;
    }

    // By default, don't force equip if the bot is eating.
    public boolean forceEquipItem(Item... toEquip) {
        return forceEquipItem(toEquip, false);
    }

    public void refreshInventory() {
        if (MinecraftClient.getInstance().player == null)
            return;
        for (int i = 0; i < MinecraftClient.getInstance().player.getInventory().main.size(); ++i) {
            Slot slot = Slot.getFromCurrentScreenInventory(i);
            clickSlotForce(slot, 0, SlotActionType.PICKUP);
            clickSlotForce(slot, 0, SlotActionType.PICKUP);
        }
    }
}
