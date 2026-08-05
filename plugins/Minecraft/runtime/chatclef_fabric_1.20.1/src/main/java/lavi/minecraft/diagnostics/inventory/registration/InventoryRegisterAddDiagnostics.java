package lavi.minecraft.diagnostics.inventory.registration;

import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.inventory.InventoryDiagnosticFields;
import lavi.minecraft.diagnostics.inventory.InventoryRegisterBreadcrumb;
import lavi.minecraft.diagnostics.inventory.InventoryRegistrationOverlap;
import lavi.minecraft.diagnostics.inventory.InventoryScanContext;
import lavi.minecraft.diagnostics.inventory.scan.InventoryScanEventEmitter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

//20260805_kpopmodder: Emit bounded pre-add breadcrumbs for InventorySubTracker registration.
public final class InventoryRegisterAddDiagnostics {
    private final InventoryRegistrationOverlapDetector overlapDetector = new InventoryRegistrationOverlapDetector();

    public void beforeRegisterAdd(InventoryScanContext context,
                                  Iterable<InventoryScanContext> activeScans,
                                  int activeScanCount,
                                  InventoryScanEventEmitter emitter,
                                  ItemStack stack,
                                  Item item,
                                  Slot slot,
                                  boolean isSlotPlayerInventory,
                                  boolean itemKeyPresentBefore,
                                  Map<Item, List<Slot>> targetItemSlots,
                                  int itemCountMapValueBefore,
                                  int itemCountMapValueAfterCountUpdate,
                                  int playerItemMapSize,
                                  int containerItemMapSize) {
        InventoryRegisterBreadcrumb breadcrumb = InventoryRegisterBreadcrumbCapture.capture(
                context,
                stack,
                item,
                slot,
                isSlotPlayerInventory,
                itemKeyPresentBefore,
                targetItemSlots,
                itemCountMapValueBefore,
                itemCountMapValueAfterCountUpdate,
                playerItemMapSize,
                containerItemMapSize,
                activeScanCount,
                InventoryRegistrationOverlap.none()
        );
        context.startRegistration(breadcrumb.registrationState());
        InventoryRegistrationOverlap overlap = overlapDetector.detect(context, breadcrumb.registrationState(), activeScans);
        InventoryRegisterBreadcrumb finalBreadcrumb = breadcrumb.withOverlap(overlap);
        if (overlap.any()) {
            context.arm();
        }
        if (!context.canEmitRegisterBreadcrumb()) {
            return;
        }
        emitter.emitBeginIfNeeded(context, "inventory_register_pre_add");
        emitter.emit("INVENTORY_SUBTRACKER_REGISTER_PRE_ADD",
                "inventory_register_pre_add",
                context,
                InventoryDiagnosticFields.registerPreAdd(context, finalBreadcrumb),
                overlap.any(),
                finalBreadcrumb.fingerprint());
    }

    public void afterRegisterAdd(InventoryScanContext context) {
        context.completeRegistration();
    }
}
