package lavi.minecraft.diagnostics.inventory;

import java.util.List;

//20260805_kpopmodder: Keep InventorySubTracker diagnostic field assembly out of the upstream tracker.
public final class InventoryDiagnosticFields {
    private InventoryDiagnosticFields() {
    }

    public static Object[] scanBegin(InventoryScanContext context) {
        return merge(
                common(context),
                context.beginSnapshot().fields(),
                new Object[]{
                        "diagnosticScanArmed", context.armed()
                }
        );
    }

    public static Object[] overlap(InventoryScanContext context,
                                   InventoryScanContext other,
                                   List<InventoryScanContext> activeOwners,
                                   String overlapType) {
        return merge(
                common(context),
                new Object[]{
                        "overlapType", overlapType,
                        "otherActiveScanId", other == null ? "unavailable" : other.scanId(),
                        "otherActiveTrackerIdentity", other == null ? "unavailable" : other.trackerIdentity(),
                        "otherActiveThreadName", other == null ? "unavailable" : other.threadName(),
                        "otherActiveThreadId", other == null ? "unavailable" : other.threadId(),
                        "otherActiveStartClientTickId", other == null ? "unavailable" : other.startClientTickId()
                },
                InventoryActiveScanOwners.fields(context, activeOwners),
                context.beginSnapshot().fields()
        );
    }

    public static Object[] offThreadCallerSummary(InventoryScanContext context,
                                                  InventoryOffThreadCallerCensus.InventoryOffThreadCallerSummary summary) {
        return merge(
                common(context),
                new Object[]{
                        "offThreadCallerFingerprint", summary.callerFingerprint(),
                        "offThreadCallerFirstObservedClientTickId", summary.firstObservedClientTickId(),
                        "offThreadCallerLastObservedClientTickId", summary.lastObservedClientTickId(),
                        "offThreadCallerObservationCount", summary.observationCount(),
                        "offThreadCallerOverlapObservationCount", summary.overlapObservationCount(),
                        "offThreadCallerMaxActiveScanCount", summary.maxActiveScanCount()
                },
                context.beginSnapshot().fields()
        );
    }

    public static Object[] contextDrift(InventoryScanContext context,
                                        InventoryScreenSnapshot current,
                                        String driftReason) {
        return merge(
                common(context),
                new Object[]{
                        "driftReason", driftReason
                },
                context.beginSnapshot().screen().beginFields("AtBegin"),
                current == null ? new Object[0] : current.beginFields("AtDrift")
        );
    }

    public static Object[] registerPreAdd(InventoryScanContext context,
                                          InventoryRegisterBreadcrumb breadcrumb) {
        return merge(
                common(context),
                new Object[]{
                        "resetGenerationObserved", context.lastSharedResetGeneration()
                },
                breadcrumb.fields()
        );
    }

    public static Object[] sharedReset(InventoryScanContext context,
                                       String phase,
                                       long resetGeneration,
                                       int uniquePlayerItems,
                                       int uniqueContainerItems,
                                       int playerItemCountEntries,
                                       int containerItemCountEntries,
                                       List<InventoryScanContext> activeOwners) {
        return merge(
                common(context),
                new Object[]{
                        "sharedResetPhase", phase,
                        "resetGeneration", resetGeneration,
                        "uniquePlayerItems", uniquePlayerItems,
                        "uniqueContainerItems", uniqueContainerItems,
                        "playerItemCountEntries", playerItemCountEntries,
                        "containerItemCountEntries", containerItemCountEntries
                },
                InventoryActiveScanOwners.fields(context, activeOwners),
                context.beginSnapshot().screen().beginFields("AtReset")
        );
    }

    public static Object[] scanEnd(InventoryScanContext context,
                                   InventoryScreenSnapshot endScreen,
                                   int uniquePlayerItems,
                                   int uniqueContainerItems,
                                   int playerItemCountEntries,
                                   int containerItemCountEntries,
                                   long elapsedNanos) {
        return merge(
                common(context),
                new Object[]{
                        "elapsedNanos", elapsedNanos,
                        "slotsVisited", context.slotsVisited(),
                        "cursorSlotsSkipped", context.cursorSlotsSkipped(),
                        "slotsIgnored", context.slotsIgnored(),
                        "slotsRegistered", context.slotsRegistered(),
                        "detailedBreadcrumbsEmitted", context.detailedBreadcrumbsEmitted(),
                        "detailedBreadcrumbsSuppressed", context.detailedBreadcrumbsSuppressed(),
                        "uniquePlayerItems", uniquePlayerItems,
                        "uniqueContainerItems", uniqueContainerItems,
                        "playerItemCountEntries", playerItemCountEntries,
                        "containerItemCountEntries", containerItemCountEntries
                },
                context.beginSnapshot().screen().beginFields("AtBegin"),
                endScreen == null ? new Object[0] : endScreen.beginFields("AtEnd")
        );
    }

    public static Object[] repeatSummary(String repeatKey, int suppressedRepeatCount) {
        return new Object[]{
                "capScope", "inventory_subtracker",
                "repeatKey", repeatKey,
                "suppressedRepeatCount", suppressedRepeatCount
        };
    }

    public static Object[] cap(int cap) {
        return new Object[]{
                "capScope", "inventory_subtracker",
                "cap", cap
        };
    }

    public static Object[] merge(Object[]... parts) {
        int length = 0;
        for (Object[] part : parts) {
            if (part != null) {
                length += part.length;
            }
        }
        Object[] merged = new Object[length];
        int offset = 0;
        for (Object[] part : parts) {
            if (part == null || part.length == 0) {
                continue;
            }
            System.arraycopy(part, 0, merged, offset, part.length);
            offset += part.length;
        }
        return merged;
    }

    private static Object[] common(InventoryScanContext context) {
        return new Object[]{
                "scanId", context.scanId(),
                "trackerIdentity", context.trackerIdentity(),
                "inventoryScanStartClientTickId", context.startClientTickId(),
                "inventoryScanStartNanos", context.startNanos(),
                "scanThreadName", context.threadName(),
                "scanThreadId", context.threadId(),
                "isClientThread", context.clientThread(),
                "clientThreadName", context.clientThreadName(),
                "clientThreadId", context.clientThreadId(),
                "activeScanCountBefore", context.activeScanCountBefore(),
                "scanDepthOnCurrentThread", context.scanDepthOnCurrentThread(),
                "callerBoundary", context.caller().callerBoundary(),
                "callerTopFrames", context.caller().callerTopFrames()
        };
    }
}
