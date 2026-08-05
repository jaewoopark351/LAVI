package lavi.minecraft.diagnostics.inventory;

//20260805_kpopmodder: Capture a bounded write-ahead breadcrumb before InventorySubTracker list.add.
public final class InventoryRegisterBreadcrumb {
    private final long scanId;
    private final int scanOrdinal;
    private final String slotClass;
    private final String windowSlot;
    private final String inventorySlot;
    private final String isCursorSlot;
    private final String isPlayerInventoryClassification;
    private final String shouldIgnoreForContainer;
    private final String windowSlotInBeginHandlerBounds;
    private final String windowSlotInCurrentHandlerBounds;
    private final String itemId;
    private final String itemCount;
    private final String itemEmpty;
    private final String itemDamage;
    private final String itemMaxDamage;
    private final String customNamePresent;
    private final String targetMap;
    private final boolean itemKeyPresentBefore;
    private final boolean listPresentBefore;
    private final String listIdentity;
    private final String listSizeBefore;
    private final int diagnosticExpectedRegistrationsForItem;
    private final int diagnosticExpectedListSizeBefore;
    private final int itemCountMapValueBefore;
    private final int itemCountMapValueAfterCountUpdate;
    private final int playerItemMapSize;
    private final int containerItemMapSize;
    private final String handlerIdentityAtScanBegin;
    private final String handlerIdentityAtRegister;
    private final String handlerSameAsScanBegin;
    private final String syncIdAtScanBegin;
    private final String syncIdAtRegister;
    private final String handlerSlotCountAtScanBegin;
    private final String handlerSlotCountAtRegister;
    private final int activeScanCount;
    private final InventoryRegistrationOverlap overlap;

    public InventoryRegisterBreadcrumb(long scanId,
                                       int scanOrdinal,
                                       String slotClass,
                                       String windowSlot,
                                       String inventorySlot,
                                       String isCursorSlot,
                                       String isPlayerInventoryClassification,
                                       String shouldIgnoreForContainer,
                                       String windowSlotInBeginHandlerBounds,
                                       String windowSlotInCurrentHandlerBounds,
                                       String itemId,
                                       String itemCount,
                                       String itemEmpty,
                                       String itemDamage,
                                       String itemMaxDamage,
                                       String customNamePresent,
                                       String targetMap,
                                       boolean itemKeyPresentBefore,
                                       boolean listPresentBefore,
                                       String listIdentity,
                                       String listSizeBefore,
                                       int diagnosticExpectedRegistrationsForItem,
                                       int diagnosticExpectedListSizeBefore,
                                       int itemCountMapValueBefore,
                                       int itemCountMapValueAfterCountUpdate,
                                       int playerItemMapSize,
                                       int containerItemMapSize,
                                       String handlerIdentityAtScanBegin,
                                       String handlerIdentityAtRegister,
                                       String handlerSameAsScanBegin,
                                       String syncIdAtScanBegin,
                                       String syncIdAtRegister,
                                       String handlerSlotCountAtScanBegin,
                                       String handlerSlotCountAtRegister,
                                       int activeScanCount,
                                       InventoryRegistrationOverlap overlap) {
        this.scanId = scanId;
        this.scanOrdinal = scanOrdinal;
        this.slotClass = slotClass;
        this.windowSlot = windowSlot;
        this.inventorySlot = inventorySlot;
        this.isCursorSlot = isCursorSlot;
        this.isPlayerInventoryClassification = isPlayerInventoryClassification;
        this.shouldIgnoreForContainer = shouldIgnoreForContainer;
        this.windowSlotInBeginHandlerBounds = windowSlotInBeginHandlerBounds;
        this.windowSlotInCurrentHandlerBounds = windowSlotInCurrentHandlerBounds;
        this.itemId = itemId;
        this.itemCount = itemCount;
        this.itemEmpty = itemEmpty;
        this.itemDamage = itemDamage;
        this.itemMaxDamage = itemMaxDamage;
        this.customNamePresent = customNamePresent;
        this.targetMap = targetMap;
        this.itemKeyPresentBefore = itemKeyPresentBefore;
        this.listPresentBefore = listPresentBefore;
        this.listIdentity = listIdentity;
        this.listSizeBefore = listSizeBefore;
        this.diagnosticExpectedRegistrationsForItem = diagnosticExpectedRegistrationsForItem;
        this.diagnosticExpectedListSizeBefore = diagnosticExpectedListSizeBefore;
        this.itemCountMapValueBefore = itemCountMapValueBefore;
        this.itemCountMapValueAfterCountUpdate = itemCountMapValueAfterCountUpdate;
        this.playerItemMapSize = playerItemMapSize;
        this.containerItemMapSize = containerItemMapSize;
        this.handlerIdentityAtScanBegin = handlerIdentityAtScanBegin;
        this.handlerIdentityAtRegister = handlerIdentityAtRegister;
        this.handlerSameAsScanBegin = handlerSameAsScanBegin;
        this.syncIdAtScanBegin = syncIdAtScanBegin;
        this.syncIdAtRegister = syncIdAtRegister;
        this.handlerSlotCountAtScanBegin = handlerSlotCountAtScanBegin;
        this.handlerSlotCountAtRegister = handlerSlotCountAtRegister;
        this.activeScanCount = activeScanCount;
        this.overlap = overlap == null ? InventoryRegistrationOverlap.none() : overlap;
    }

    public InventoryRegistrationState registrationState() {
        return new InventoryRegistrationState(targetMap, itemId, listIdentity, scanId, Thread.currentThread().getId());
    }

    public InventoryRegisterBreadcrumb withOverlap(InventoryRegistrationOverlap overlap) {
        return new InventoryRegisterBreadcrumb(
                scanId,
                scanOrdinal,
                slotClass,
                windowSlot,
                inventorySlot,
                isCursorSlot,
                isPlayerInventoryClassification,
                shouldIgnoreForContainer,
                windowSlotInBeginHandlerBounds,
                windowSlotInCurrentHandlerBounds,
                itemId,
                itemCount,
                itemEmpty,
                itemDamage,
                itemMaxDamage,
                customNamePresent,
                targetMap,
                itemKeyPresentBefore,
                listPresentBefore,
                listIdentity,
                listSizeBefore,
                diagnosticExpectedRegistrationsForItem,
                diagnosticExpectedListSizeBefore,
                itemCountMapValueBefore,
                itemCountMapValueAfterCountUpdate,
                playerItemMapSize,
                containerItemMapSize,
                handlerIdentityAtScanBegin,
                handlerIdentityAtRegister,
                handlerSameAsScanBegin,
                syncIdAtScanBegin,
                syncIdAtRegister,
                handlerSlotCountAtScanBegin,
                handlerSlotCountAtRegister,
                activeScanCount,
                overlap
        );
    }

    public Object[] fields() {
        return new Object[]{
                "scanId", scanId,
                "scanOrdinal", scanOrdinal,
                "slotClass", slotClass,
                "windowSlot", windowSlot,
                "inventorySlot", inventorySlot,
                "isCursorSlot", isCursorSlot,
                "isPlayerInventoryClassification", isPlayerInventoryClassification,
                "shouldIgnoreForContainer", shouldIgnoreForContainer,
                "windowSlotInBeginHandlerBounds", windowSlotInBeginHandlerBounds,
                "windowSlotInCurrentHandlerBounds", windowSlotInCurrentHandlerBounds,
                "itemId", itemId,
                "itemCount", itemCount,
                "itemEmpty", itemEmpty,
                "itemDamage", itemDamage,
                "itemMaxDamage", itemMaxDamage,
                "customNamePresent", customNamePresent,
                "targetMap", targetMap,
                "itemKeyPresentBefore", itemKeyPresentBefore,
                "listPresentBefore", listPresentBefore,
                "listIdentity", listIdentity,
                "listSizeBefore", listSizeBefore,
                "diagnosticExpectedRegistrationsForItem", diagnosticExpectedRegistrationsForItem,
                "diagnosticExpectedListSizeBefore", diagnosticExpectedListSizeBefore,
                "itemCountMapValueBefore", itemCountMapValueBefore,
                "itemCountMapValueAfterCountUpdate", itemCountMapValueAfterCountUpdate,
                "playerItemMapSize", playerItemMapSize,
                "containerItemMapSize", containerItemMapSize,
                "handlerIdentityAtScanBegin", handlerIdentityAtScanBegin,
                "handlerIdentityAtRegister", handlerIdentityAtRegister,
                "handlerSameAsScanBegin", handlerSameAsScanBegin,
                "syncIdAtScanBegin", syncIdAtScanBegin,
                "syncIdAtRegister", syncIdAtRegister,
                "handlerSlotCountAtScanBegin", handlerSlotCountAtScanBegin,
                "handlerSlotCountAtRegister", handlerSlotCountAtRegister,
                "activeScanCount", activeScanCount,
                "otherActiveScanId", overlap.otherActiveScanId(),
                "otherActiveThreadId", overlap.otherActiveThreadId(),
                "sameItemActiveInOtherScan", overlap.sameItemActiveInOtherScan(),
                "sameTargetMapActiveInOtherScan", overlap.sameTargetMapActiveInOtherScan(),
                "sameListIdentityActiveInOtherScan", overlap.sameListIdentityActiveInOtherScan()
        };
    }

    public String fingerprint() {
        return "REGISTER_PRE_ADD|" + targetMap + "|" + itemId + "|" + scanOrdinal + "|" + windowSlot;
    }

}
