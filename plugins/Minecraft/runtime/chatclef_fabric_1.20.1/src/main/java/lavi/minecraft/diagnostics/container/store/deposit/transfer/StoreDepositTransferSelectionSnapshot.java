package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Capture only values already selected by the production transfer path.
public record StoreDepositTransferSelectionSnapshot(BlockPos destinationPosition,
                                                    ItemTarget aggregateTarget,
                                                    Slot selectedSourceCandidate,
                                                    String selectedSourceCandidateItemId,
                                                    int selectedSourceCandidateCount,
                                                    Slot destinationSlot,
                                                    String destinationSelectionKind,
                                                    int destinationCountBefore,
                                                    int roomLeft,
                                                    boolean acceptPartial,
                                                    int potentialSourceSlotCount) {
    public StoreDepositTransferSelectionSnapshot {
        destinationPosition = destinationPosition == null
                ? null
                : destinationPosition.toImmutable();
        aggregateTarget = copyTarget(aggregateTarget);
        selectedSourceCandidate = freezeSlot(selectedSourceCandidate);
        selectedSourceCandidateItemId = normalize(selectedSourceCandidateItemId);
        destinationSlot = freezeSlot(destinationSlot);
        destinationSelectionKind = normalize(destinationSelectionKind);
    }

    public static StoreDepositTransferSelectionSnapshot capture(
            BlockPos destinationPosition,
            ItemTarget aggregateTarget,
            Slot selectedSourceCandidate,
            ItemStack selectedSourceStack,
            Slot destinationSlot,
            ItemStack destinationStack,
            int potentialSourceSlotCount) {
        ItemStack source = selectedSourceStack == null ? ItemStack.EMPTY : selectedSourceStack;
        boolean destinationStackRetained = destinationStack != null;
        ItemStack destination = destinationStackRetained ? destinationStack : ItemStack.EMPTY;
        int roomLeft = destinationSlot == null || !destinationStackRetained
                ? -1
                : Math.max(0, source.getMaxCount() - destination.getCount());
        return new StoreDepositTransferSelectionSnapshot(
                destinationPosition,
                aggregateTarget,
                selectedSourceCandidate,
                itemId(source),
                source.getCount(),
                destinationSlot,
                destinationStackRetained
                        ? destinationKind(source, destination)
                        : "UNAVAILABLE_NOT_RETAINED",
                destinationStackRetained ? destination.getCount() : -1,
                roomLeft,
                false,
                potentialSourceSlotCount
        );
    }

    public static StoreDepositTransferSelectionSnapshot captureWithoutDestinationStack(
            BlockPos destinationPosition,
            ItemTarget aggregateTarget,
            Slot selectedSourceCandidate,
            ItemStack selectedSourceStack,
            Slot destinationSlot,
            int potentialSourceSlotCount) {
        return capture(
                destinationPosition,
                aggregateTarget,
                selectedSourceCandidate,
                selectedSourceStack,
                destinationSlot,
                null,
                potentialSourceSlotCount
        );
    }

    public boolean cursorMatchesAggregate(ItemStack cursorStack) {
        return cursorStack != null
                && !cursorStack.isEmpty()
                && aggregateTarget != null
                && aggregateTarget.matches(cursorStack.getItem());
    }

    public boolean stackMatchesAggregate(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && aggregateTarget != null
                && aggregateTarget.matches(stack.getItem());
    }

    @Override
    public ItemTarget aggregateTarget() {
        return copyTarget(aggregateTarget);
    }

    public boolean isDestinationSlot(Slot observedSlot) {
        return sameSlot(destinationSlot, observedSlot);
    }

    public String selectedSourceMatchVerdict(Slot observedSlot, boolean cursorSource) {
        if (selectedSourceCandidate == null) {
            return "UNAVAILABLE_SELECTED_SOURCE_CANDIDATE";
        }
        if (cursorSource) {
            return selectedSourceCandidate instanceof FrozenSlot frozen && frozen.cursor
                    ? "MATCH"
                    : "MISMATCH";
        }
        if (observedSlot == null) {
            return "UNAVAILABLE_PHYSICAL_SOURCE_SLOT";
        }
        return sameSlot(selectedSourceCandidate, observedSlot) ? "MATCH" : "MISMATCH";
    }

    public String observedSlotSummary(Slot observedSlot) {
        return safeSlotSummary(observedSlot);
    }

    public Object[] fields() {
        return new Object[]{
                "selectedSourceCandidate", ChatClefDiagnostics.slotSummary(selectedSourceCandidate),
                "selectedSourceCandidateItemId", selectedSourceCandidateItemId,
                "selectedSourceCandidateCount", selectedSourceCandidateCount,
                "sourceCountBefore", selectedSourceCandidateCount,
                "aggregateTargetItemId", ChatClefDiagnostics.itemTargets(
                        aggregateTarget == null ? null : new ItemTarget[]{aggregateTarget}
                ),
                "aggregateTargetCount", aggregateTarget == null ? "UNAVAILABLE" : aggregateTarget.getTargetCount(),
                "destinationPosition", ChatClefDiagnostics.blockPos(destinationPosition),
                "destinationSlot", ChatClefDiagnostics.slotSummary(destinationSlot),
                "destinationSelectionKind", destinationSelectionKind,
                "destinationCountBefore", destinationCountBefore < 0
                        ? "UNAVAILABLE_NOT_RETAINED"
                        : destinationCountBefore,
                "roomLeft", roomLeft < 0 ? "UNAVAILABLE_NOT_RETAINED" : roomLeft,
                "destinationPrestateObservationComplete", destinationCountBefore >= 0 && roomLeft >= 0,
                "destinationPrestateMissingBoundaries", destinationCountBefore >= 0 && roomLeft >= 0
                        ? "NONE"
                        : "DESTINATION_STACK_BEFORE,DESTINATION_SELECTION_KIND,ROOM_LEFT",
                "acceptPartial", acceptPartial,
                "exactFitComparatorProvenance", "UNAVAILABLE_INTERNAL_SELECTOR_BRANCH",
                "potentialSourceSlotCount", potentialSourceSlotCount
        };
    }

    private static ItemTarget copyTarget(ItemTarget target) {
        return target == null ? null : new ItemTarget(target, target.getTargetCount());
    }

    private static Slot freezeSlot(Slot slot) {
        if (slot == null) {
            return null;
        }
        boolean cursor = Slot.isCursor(slot);
        try {
            return new FrozenSlot(
                    slot.getInventorySlot(),
                    slot.getWindowSlot(),
                    slot.isSlotInPlayerInventory(),
                    cursor,
                    safeSlotSummary(slot)
            );
        } catch (RuntimeException | LinkageError ignored) {
            return new FrozenSlot(-999, -999, false, cursor, safeSlotSummary(slot));
        }
    }

    private static boolean sameSlot(Slot expected, Slot observed) {
        if (!(expected instanceof FrozenSlot frozen) || observed == null) {
            return false;
        }
        if (frozen.cursor) {
            return Slot.isCursor(observed);
        }
        try {
            return frozen.inventorySlot == observed.getInventorySlot()
                    && frozen.windowSlot == observed.getWindowSlot();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static String safeSlotSummary(Slot slot) {
        try {
            return ChatClefDiagnostics.slotSummary(slot);
        } catch (RuntimeException | LinkageError ignored) {
            return slot == null ? "UNAVAILABLE" : slot.getClass().getName() + "#UNAVAILABLE_SLOT_KEY";
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }

    private static String destinationKind(ItemStack source, ItemStack destination) {
        if (destination == null || destination.isEmpty()) {
            return "EMPTY_SLOT";
        }
        if (source != null && !source.isEmpty() && source.getItem() == destination.getItem()) {
            return "STACKABLE_EXISTING";
        }
        return "OTHER";
    }

    private static String itemId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "EMPTY" : String.valueOf(stack.getItem());
    }

    private static final class FrozenSlot extends Slot {
        private final int inventorySlot;
        private final int windowSlot;
        private final boolean playerInventory;
        private final boolean cursor;
        private final String summary;

        private FrozenSlot(int inventorySlot,
                           int windowSlot,
                           boolean playerInventory,
                           boolean cursor,
                           String summary) {
            super(windowSlot, false);
            this.inventorySlot = inventorySlot;
            this.windowSlot = windowSlot;
            this.playerInventory = playerInventory;
            this.cursor = cursor;
            this.summary = normalize(summary);
        }

        @Override
        public int getInventorySlot() {
            return inventorySlot;
        }

        @Override
        public int getWindowSlot() {
            return windowSlot;
        }

        @Override
        public boolean isSlotInPlayerInventory() {
            return playerInventory;
        }

        @Override
        protected int inventorySlotToWindowSlot(int inventorySlot) {
            return windowSlot;
        }

        @Override
        protected int windowSlotToInventorySlot(int windowSlot) {
            return inventorySlot;
        }

        @Override
        protected String getName() {
            return "DiagnosticFrozen";
        }

        @Override
        public String toString() {
            return summary;
        }
    }
}
