package lavi.minecraft.diagnostics.inventory;

import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.inventory.registration.InventoryRegisterAddDiagnostics;
import lavi.minecraft.diagnostics.inventory.scan.InventoryScanEventEmitter;
import lavi.minecraft.diagnostics.inventory.scan.InventoryScanLifecycleDiagnostics;
import lavi.minecraft.diagnostics.inventory.scan.InventoryScanRegistry;
import lavi.minecraft.diagnostics.inventory.scan.InventorySharedResetDiagnostics;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

//20260805_kpopmodder: Observe InventorySubTracker scan boundaries without changing inventory behavior.
public final class InventorySubTrackerScanProbe {
    private static final InventoryScanRegistry SCAN_REGISTRY = new InventoryScanRegistry();
    private static final InventoryScanEventEmitter EMITTER = new InventoryScanEventEmitter();
    private static final InventoryScanLifecycleDiagnostics LIFECYCLE = new InventoryScanLifecycleDiagnostics();
    private static final InventorySharedResetDiagnostics SHARED_RESETS = new InventorySharedResetDiagnostics();
    private static final InventoryRegisterAddDiagnostics REGISTER_ADDS = new InventoryRegisterAddDiagnostics();

    private InventorySubTrackerScanProbe() {
    }

    public static void beginScan(Object tracker) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            LIFECYCLE.beginScan(identity(tracker), SCAN_REGISTRY, EMITTER);
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    public static void observeSlot(Object tracker, Slot slot, boolean cursorSlot, boolean ignored) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            InventoryScanContext context = currentContext(tracker);
            if (context == null) {
                return;
            }
            context.recordSlot(cursorSlot, ignored);
            InventoryScreenSnapshot current = InventoryScreenSnapshot.capture();
            String driftReason = context.beginSnapshot().screen().driftReason(current);
            if (!"none".equals(driftReason) && context.markContextDriftLogged()) {
                EMITTER.emitBeginIfNeeded(context, "inventory_scan_context_drift");
                EMITTER.emit("INVENTORY_SUBTRACKER_CONTEXT_DRIFT",
                        "inventory_scan_context_drift",
                        context,
                        InventoryDiagnosticFields.contextDrift(context, current, driftReason),
                        true,
                        "CONTEXT_DRIFT|" + driftReason + "|" + context.beginSnapshot().screen().stableKey() + "|" + current.stableKey());
            }
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    public static void beforeRegisterAdd(Object tracker,
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
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            InventoryScanContext context = currentContext(tracker);
            if (context == null) {
                return;
            }
            REGISTER_ADDS.beforeRegisterAdd(
                    context,
                    SCAN_REGISTRY.activeScans(),
                    SCAN_REGISTRY.activeScanCount(context.trackerIdentity()),
                    EMITTER,
                    stack,
                    item,
                    slot,
                    isSlotPlayerInventory,
                    itemKeyPresentBefore,
                    targetItemSlots,
                    itemCountMapValueBefore,
                    itemCountMapValueAfterCountUpdate,
                    playerItemMapSize,
                    containerItemMapSize
            );
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    public static void afterRegisterAdd(Object tracker) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            InventoryScanContext context = currentContext(tracker);
            if (context != null) {
                REGISTER_ADDS.afterRegisterAdd(context);
            }
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    public static void beforeSharedReset(Object tracker,
                                         int uniquePlayerItems,
                                         int uniqueContainerItems,
                                         int playerItemCountEntries,
                                         int containerItemCountEntries) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            InventoryScanContext context = currentContext(tracker);
            if (context == null) {
                return;
            }
            context.markSharedReset(ChatClefDiagnostics.nextOperationId());
            SHARED_RESETS.emit(
                    context,
                    SCAN_REGISTRY,
                    EMITTER,
                    "BEGIN",
                    uniquePlayerItems,
                    uniqueContainerItems,
                    playerItemCountEntries,
                    containerItemCountEntries
            );
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    public static void afterSharedReset(Object tracker,
                                        int uniquePlayerItems,
                                        int uniqueContainerItems,
                                        int playerItemCountEntries,
                                        int containerItemCountEntries) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            InventoryScanContext context = currentContext(tracker);
            if (context == null) {
                return;
            }
            SHARED_RESETS.emit(
                    context,
                    SCAN_REGISTRY,
                    EMITTER,
                    "END",
                    uniquePlayerItems,
                    uniqueContainerItems,
                    playerItemCountEntries,
                    containerItemCountEntries
            );
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    public static void endScan(Object tracker,
                               int uniquePlayerItems,
                               int uniqueContainerItems,
                               int playerItemCountEntries,
                               int containerItemCountEntries) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            LIFECYCLE.endScan(
                    identity(tracker),
                    SCAN_REGISTRY,
                    EMITTER,
                    uniquePlayerItems,
                    uniqueContainerItems,
                    playerItemCountEntries,
                    containerItemCountEntries
            );
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    private static InventoryScanContext currentContext(Object tracker) {
        return SCAN_REGISTRY.currentContext(identity(tracker));
    }

    public static int activeScanCount() {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return 0;
        }
        try {
            return SCAN_REGISTRY.activeScanCount();
        } catch (RuntimeException | LinkageError ignoredError) {
            return -1;
        }
    }

    private static String identity(Object value) {
        return value == null ? "unavailable" : Integer.toHexString(System.identityHashCode(value));
    }
}
