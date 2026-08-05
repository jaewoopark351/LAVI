package lavi.minecraft.diagnostics.inventory;

import java.util.List;
import java.util.StringJoiner;

//20260805_kpopmodder: Format active InventorySubTracker scan owners without mutating tracker state.
public final class InventoryActiveScanOwners {
    private static final int MAX_OWNER_DETAILS = 8;

    private InventoryActiveScanOwners() {
    }

    public static Object[] fields(InventoryScanContext current, List<InventoryScanContext> owners) {
        int ownerCount = owners == null ? 0 : owners.size();
        return new Object[]{
                "activeScanOwnerCount", ownerCount,
                "activeScanOwnerSelfPresent", selfPresent(current, owners),
                "sameThreadOtherScanOwnerPresent", sameThreadOtherPresent(current, owners),
                "differentThreadScanOwnerPresent", differentThreadPresent(current, owners),
                "activeScanOwnersTruncated", ownerCount > MAX_OWNER_DETAILS,
                "activeScanOwners", format(current, owners)
        };
    }

    public static InventoryScanContext firstOther(InventoryScanContext current, List<InventoryScanContext> owners) {
        if (owners == null) {
            return null;
        }
        for (InventoryScanContext owner : owners) {
            if (owner == null) {
                continue;
            }
            if (current == null || owner.scanId() != current.scanId()) {
                return owner;
            }
        }
        return null;
    }

    public static String overlapType(InventoryScanContext current, List<InventoryScanContext> owners) {
        boolean sameThreadReentrant = current != null
                && (current.scanDepthOnCurrentThread() > 0 || sameThreadOtherPresent(current, owners));
        boolean differentThread = differentThreadPresent(current, owners);
        if (sameThreadReentrant && differentThread) {
            return "SAME_THREAD_REENTRANT_AND_OTHER_ACTIVE_SCAN";
        }
        if (sameThreadReentrant) {
            return "SAME_THREAD_REENTRANT";
        }
        if (differentThread || (owners != null && !owners.isEmpty())) {
            return "OTHER_ACTIVE_SCAN";
        }
        return "NONE";
    }

    public static String fingerprint(InventoryScanContext current, List<InventoryScanContext> owners) {
        if (owners == null || owners.isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner("|");
        int included = 0;
        for (InventoryScanContext owner : owners) {
            if (owner == null || included >= MAX_OWNER_DETAILS) {
                continue;
            }
            joiner.add(owner.scanId()
                    + ":"
                    + owner.threadId()
                    + ":"
                    + owner.beginSnapshot().stableKey()
                    + ":"
                    + relation(current, owner));
            included++;
        }
        return joiner.toString();
    }

    private static boolean selfPresent(InventoryScanContext current, List<InventoryScanContext> owners) {
        if (current == null || owners == null) {
            return false;
        }
        for (InventoryScanContext owner : owners) {
            if (owner != null && owner.scanId() == current.scanId()) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameThreadOtherPresent(InventoryScanContext current, List<InventoryScanContext> owners) {
        if (current == null || owners == null) {
            return false;
        }
        for (InventoryScanContext owner : owners) {
            if (owner != null && owner.scanId() != current.scanId() && owner.threadId() == current.threadId()) {
                return true;
            }
        }
        return false;
    }

    private static boolean differentThreadPresent(InventoryScanContext current, List<InventoryScanContext> owners) {
        if (current == null || owners == null) {
            return false;
        }
        for (InventoryScanContext owner : owners) {
            if (owner != null && owner.threadId() != current.threadId()) {
                return true;
            }
        }
        return false;
    }

    private static String format(InventoryScanContext current, List<InventoryScanContext> owners) {
        if (owners == null || owners.isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(" ; ");
        int included = 0;
        for (InventoryScanContext owner : owners) {
            if (owner == null || included >= MAX_OWNER_DETAILS) {
                continue;
            }
            joiner.add("scanId:" + owner.scanId()
                    + ",relation:" + relation(current, owner)
                    + ",tracker:" + owner.trackerIdentity()
                    + ",thread:" + owner.threadName()
                    + ",threadId:" + owner.threadId()
                    + ",clientThread:" + owner.clientThread()
                    + ",startTick:" + owner.startClientTickId()
                    + ",elapsedNanos:" + (System.nanoTime() - owner.startNanos())
                    + ",caller:" + owner.caller().callerBoundary());
            included++;
        }
        return joiner.toString();
    }

    private static String relation(InventoryScanContext current, InventoryScanContext owner) {
        if (current == null || owner == null) {
            return "unknown";
        }
        if (owner.scanId() == current.scanId()) {
            return "self";
        }
        if (owner.threadId() == current.threadId()) {
            return "same_thread_other_scan";
        }
        return "different_thread_other_scan";
    }
}
