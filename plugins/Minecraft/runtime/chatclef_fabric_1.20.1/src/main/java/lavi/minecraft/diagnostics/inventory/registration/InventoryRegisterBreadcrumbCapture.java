package lavi.minecraft.diagnostics.inventory.registration;

import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.inventory.InventoryRegisterBreadcrumb;
import lavi.minecraft.diagnostics.inventory.InventoryRegistrationOverlap;
import lavi.minecraft.diagnostics.inventory.InventoryScanContext;
import lavi.minecraft.diagnostics.inventory.InventoryScreenSnapshot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

//20260805_kpopmodder: Capture registerItem breadcrumb inputs separately from breadcrumb storage.
public final class InventoryRegisterBreadcrumbCapture {
    private InventoryRegisterBreadcrumbCapture() {
    }

    public static InventoryRegisterBreadcrumb capture(InventoryScanContext context,
                                                      ItemStack stack,
                                                      Item item,
                                                      Slot slot,
                                                      boolean isSlotPlayerInventory,
                                                      boolean itemKeyPresentBefore,
                                                      Map<Item, List<Slot>> targetItemSlots,
                                                      int itemCountMapValueBefore,
                                                      int itemCountMapValueAfterCountUpdate,
                                                      int playerItemMapSize,
                                                      int containerItemMapSize,
                                                      int activeScanCount,
                                                      InventoryRegistrationOverlap overlap) {
        String itemId = InventoryRegisterBreadcrumbValues.itemId(item);
        String targetMap = isSlotPlayerInventory ? "PLAYER" : "CONTAINER";
        List<Slot> list = targetItemSlots == null ? null : targetItemSlots.get(item);
        InventoryScreenSnapshot currentScreen = InventoryScreenSnapshot.capture();
        String windowSlot = InventoryRegisterBreadcrumbValues.safeSlotValue(() -> slot == null ? null : slot.getWindowSlot());
        return new InventoryRegisterBreadcrumb(
                context.scanId(),
                context.scanOrdinal(),
                InventoryRegisterBreadcrumbValues.slotClass(slot),
                windowSlot,
                InventoryRegisterBreadcrumbValues.safeSlotValue(() -> slot == null ? null : slot.getInventorySlot()),
                Boolean.toString(slot != null && Slot.isCursor(slot)),
                Boolean.toString(isSlotPlayerInventory),
                "false",
                InventoryRegisterBreadcrumbValues.inBounds(windowSlot, context.beginSnapshot().screen().playerHandlerSlotCount()),
                InventoryRegisterBreadcrumbValues.inBounds(windowSlot, currentScreen.playerHandlerSlotCount()),
                itemId,
                stack == null ? "unavailable" : Integer.toString(stack.getCount()),
                stack == null ? "unavailable" : Boolean.toString(stack.isEmpty()),
                stack == null ? "unavailable" : Integer.toString(stack.getDamage()),
                stack == null ? "unavailable" : Integer.toString(stack.getMaxDamage()),
                "unavailable",
                targetMap,
                itemKeyPresentBefore,
                list != null,
                InventoryRegisterBreadcrumbValues.identity(list),
                list == null ? "unavailable" : Integer.toString(list.size()),
                context.expectedRegistrationsForItem(targetMap, itemId),
                context.expectedRegistrationsForItem(targetMap, itemId),
                itemCountMapValueBefore,
                itemCountMapValueAfterCountUpdate,
                playerItemMapSize,
                containerItemMapSize,
                context.beginSnapshot().screen().playerHandlerIdentity(),
                currentScreen.playerHandlerIdentity(),
                Boolean.toString(context.beginSnapshot().screen().playerHandlerIdentity().equals(currentScreen.playerHandlerIdentity())),
                context.beginSnapshot().screen().playerHandlerSyncId(),
                currentScreen.playerHandlerSyncId(),
                context.beginSnapshot().screen().playerHandlerSlotCount(),
                currentScreen.playerHandlerSlotCount(),
                activeScanCount,
                overlap
        );
    }
}
