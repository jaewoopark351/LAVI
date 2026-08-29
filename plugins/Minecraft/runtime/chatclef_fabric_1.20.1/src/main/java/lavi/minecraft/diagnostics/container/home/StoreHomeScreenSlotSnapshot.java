package lavi.minecraft.diagnostics.container.home;

import net.minecraft.item.ItemStack;

import java.util.Objects;

//20260828_kpopmodder: Preserve one read-only logical-to-window slot inspection.
public final class StoreHomeScreenSlotSnapshot {
    private final boolean inspectionAttempted;
    private final boolean observed;
    private final int matchCount;
    private final int resolvedWindowSlot;
    private final boolean resolvedSlotPlayerInventory;
    private final int resolvedSlotLogicalIndex;
    private final StoreHomeStackIdentitySnapshot resolvedStack;
    private final String playerMainAndResolvedSlotEqual;
    private final String captureStatus;
    private final String errorClass;

    private StoreHomeScreenSlotSnapshot(
            boolean inspectionAttempted,
            boolean observed,
            int matchCount,
            int resolvedWindowSlot,
            boolean resolvedSlotPlayerInventory,
            int resolvedSlotLogicalIndex,
            StoreHomeStackIdentitySnapshot resolvedStack,
            String playerMainAndResolvedSlotEqual,
            String captureStatus,
            String errorClass) {
        this.inspectionAttempted = inspectionAttempted;
        this.observed = observed;
        this.matchCount = Math.max(0, matchCount);
        this.resolvedWindowSlot = resolvedWindowSlot;
        this.resolvedSlotPlayerInventory = resolvedSlotPlayerInventory;
        this.resolvedSlotLogicalIndex = resolvedSlotLogicalIndex;
        this.resolvedStack = Objects.requireNonNull(resolvedStack, "resolvedStack");
        this.playerMainAndResolvedSlotEqual = Objects.requireNonNull(
                playerMainAndResolvedSlotEqual, "playerMainAndResolvedSlotEqual"
        );
        this.captureStatus = Objects.requireNonNull(captureStatus, "captureStatus");
        this.errorClass = Objects.requireNonNull(errorClass, "errorClass");
    }

    public static StoreHomeScreenSlotSnapshot observed(
            int matchCount,
            int resolvedWindowSlot,
            boolean resolvedSlotPlayerInventory,
            int resolvedSlotLogicalIndex,
            ItemStack playerMainStack,
            ItemStack resolvedStack) {
        StoreHomeStackIdentitySnapshot playerMain =
                StoreHomeStackIdentitySnapshot.captureActual(playerMainStack);
        StoreHomeStackIdentitySnapshot resolved =
                StoreHomeStackIdentitySnapshot.captureActual(resolvedStack);
        String equal = compare(playerMain, resolved);
        String status = "complete".equals(playerMain.captureStatus())
                && "complete".equals(resolved.captureStatus())
                ? "complete"
                : "partial";
        String errors = combineErrors(playerMain.errorClass(), resolved.errorClass());
        return new StoreHomeScreenSlotSnapshot(
                true,
                true,
                matchCount,
                resolvedWindowSlot,
                resolvedSlotPlayerInventory,
                resolvedSlotLogicalIndex,
                resolved,
                equal,
                status,
                errors
        );
    }

    public static StoreHomeScreenSlotSnapshot observedWithoutUniqueMapping(int matchCount) {
        return new StoreHomeScreenSlotSnapshot(
                true,
                true,
                matchCount,
                -1,
                false,
                -1,
                StoreHomeStackIdentitySnapshot.unavailable("mapping_not_unique"),
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                "complete",
                "none"
        );
    }

    public static StoreHomeScreenSlotSnapshot notObserved(String errorClass) {
        return new StoreHomeScreenSlotSnapshot(
                true,
                false,
                0,
                -1,
                false,
                -1,
                StoreHomeStackIdentitySnapshot.unavailable(errorClass),
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                "partial",
                errorClass == null ? "unknown" : errorClass
        );
    }

    public static StoreHomeScreenSlotSnapshot notAttempted(String errorClass) {
        return new StoreHomeScreenSlotSnapshot(
                false,
                false,
                0,
                -1,
                false,
                -1,
                StoreHomeStackIdentitySnapshot.unavailable(errorClass),
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                "partial",
                errorClass == null ? "unknown" : errorClass
        );
    }

    private static String compare(
            StoreHomeStackIdentitySnapshot left,
            StoreHomeStackIdentitySnapshot right) {
        if (!left.identityObserved() || !right.identityObserved()) {
            return StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
        }
        if (!left.present() && !right.present()) {
            return "true";
        }
        if (!left.present() || !right.present()) {
            return "false";
        }
        if (StoreHomeStackIdentitySnapshot.NOT_AVAILABLE.equals(left.metadataDigest())
                || StoreHomeStackIdentitySnapshot.NOT_AVAILABLE.equals(right.metadataDigest())) {
            return StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
        }
        return Boolean.toString(
                left.itemId().equals(right.itemId())
                        && left.count() == right.count()
                        && left.damage() == right.damage()
                        && left.metadataDigest().equals(right.metadataDigest())
        );
    }

    private static String combineErrors(String left, String right) {
        if ("none".equals(left)) {
            return right;
        }
        if ("none".equals(right) || left.equals(right)) {
            return left;
        }
        return left + "," + right;
    }

    public boolean observed() {
        return observed;
    }

    public boolean inspectionAttempted() {
        return inspectionAttempted;
    }

    public int matchCount() {
        return matchCount;
    }

    public Object matchCountValue() {
        return observed ? matchCount : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object resolvedWindowSlotValue() {
        return observed && matchCount == 1
                ? resolvedWindowSlot
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object resolvedSlotPlayerInventoryValue() {
        return observed && matchCount == 1
                ? resolvedSlotPlayerInventory
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object resolvedSlotLogicalIndexValue() {
        return observed && matchCount == 1
                ? resolvedSlotLogicalIndex
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public StoreHomeStackIdentitySnapshot resolvedStack() {
        return resolvedStack;
    }

    public String playerMainAndResolvedSlotEqual() {
        return playerMainAndResolvedSlotEqual;
    }

    public String captureStatus() {
        return captureStatus;
    }

    public String errorClass() {
        return errorClass;
    }
}
