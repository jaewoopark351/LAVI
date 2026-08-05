package lavi.minecraft.diagnostics.inventory;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Own one InventorySubTracker updateState diagnostic scan.
public final class InventoryScanContext {
    private static final int MAX_REGISTER_BREADCRUMBS = 64;

    private final long scanId;
    private final String trackerIdentity;
    private final long startClientTickId;
    private final long startNanos;
    private final String threadName;
    private final long threadId;
    private final boolean clientThread;
    private final String clientThreadName;
    private final String clientThreadId;
    private final int activeScanCountBefore;
    private final int scanDepthOnCurrentThread;
    private final InventoryCallerSnapshot caller;
    private final InventoryScanSnapshot beginSnapshot;
    private final Map<String, Integer> expectedRegistrations = new HashMap<>();

    private boolean armed;
    private boolean beginEmitted;
    private boolean contextDriftLogged;
    private int slotsVisited;
    private int cursorSlotsSkipped;
    private int slotsIgnored;
    private int slotsRegistered;
    private int detailedBreadcrumbsEmitted;
    private int detailedBreadcrumbsSuppressed;
    private long lastSharedResetGeneration = -1;
    private volatile InventoryRegistrationState activeRegistration;

    public InventoryScanContext(long scanId,
                                String trackerIdentity,
                                long startClientTickId,
                                long startNanos,
                                String threadName,
                                long threadId,
                                boolean clientThread,
                                String clientThreadName,
                                String clientThreadId,
                                int activeScanCountBefore,
                                int scanDepthOnCurrentThread,
                                InventoryCallerSnapshot caller,
                                InventoryScanSnapshot beginSnapshot,
                                boolean armed) {
        this.scanId = scanId;
        this.trackerIdentity = trackerIdentity;
        this.startClientTickId = startClientTickId;
        this.startNanos = startNanos;
        this.threadName = threadName;
        this.threadId = threadId;
        this.clientThread = clientThread;
        this.clientThreadName = clientThreadName;
        this.clientThreadId = clientThreadId;
        this.activeScanCountBefore = activeScanCountBefore;
        this.scanDepthOnCurrentThread = scanDepthOnCurrentThread;
        this.caller = caller;
        this.beginSnapshot = beginSnapshot;
        this.armed = armed;
    }

    public long scanId() {
        return scanId;
    }

    public String trackerIdentity() {
        return trackerIdentity;
    }

    public long startClientTickId() {
        return startClientTickId;
    }

    public long startNanos() {
        return startNanos;
    }

    public String threadName() {
        return threadName;
    }

    public long threadId() {
        return threadId;
    }

    public boolean clientThread() {
        return clientThread;
    }

    public String clientThreadName() {
        return clientThreadName;
    }

    public String clientThreadId() {
        return clientThreadId;
    }

    public int activeScanCountBefore() {
        return activeScanCountBefore;
    }

    public int scanDepthOnCurrentThread() {
        return scanDepthOnCurrentThread;
    }

    public InventoryCallerSnapshot caller() {
        return caller;
    }

    public InventoryScanSnapshot beginSnapshot() {
        return beginSnapshot;
    }

    public boolean armed() {
        return armed;
    }

    public void arm() {
        armed = true;
    }

    public boolean beginEmitted() {
        return beginEmitted;
    }

    public void markBeginEmitted() {
        beginEmitted = true;
    }

    public void recordSlot(boolean cursorSlot, boolean ignored) {
        slotsVisited++;
        if (cursorSlot) {
            cursorSlotsSkipped++;
        }
        if (ignored) {
            slotsIgnored++;
        }
    }

    public int scanOrdinal() {
        return slotsVisited;
    }

    public boolean markContextDriftLogged() {
        if (contextDriftLogged) {
            return false;
        }
        contextDriftLogged = true;
        arm();
        return true;
    }

    public int expectedRegistrationsForItem(String targetMap, String itemId) {
        return expectedRegistrations.getOrDefault(key(targetMap, itemId), 0);
    }

    public void startRegistration(InventoryRegistrationState registration) {
        activeRegistration = registration;
        expectedRegistrations.put(
                key(registration.targetMap(), registration.itemId()),
                expectedRegistrationsForItem(registration.targetMap(), registration.itemId()) + 1
        );
    }

    public void completeRegistration() {
        slotsRegistered++;
        activeRegistration = null;
    }

    public InventoryRegistrationState activeRegistration() {
        return activeRegistration;
    }

    public void markSharedReset(long resetGeneration) {
        lastSharedResetGeneration = resetGeneration;
    }

    public long lastSharedResetGeneration() {
        return lastSharedResetGeneration;
    }

    public boolean canEmitRegisterBreadcrumb() {
        if (!armed) {
            return false;
        }
        if (detailedBreadcrumbsEmitted >= MAX_REGISTER_BREADCRUMBS) {
            detailedBreadcrumbsSuppressed++;
            return false;
        }
        detailedBreadcrumbsEmitted++;
        return true;
    }

    public int slotsVisited() {
        return slotsVisited;
    }

    public int cursorSlotsSkipped() {
        return cursorSlotsSkipped;
    }

    public int slotsIgnored() {
        return slotsIgnored;
    }

    public int slotsRegistered() {
        return slotsRegistered;
    }

    public int detailedBreadcrumbsEmitted() {
        return detailedBreadcrumbsEmitted;
    }

    public int detailedBreadcrumbsSuppressed() {
        return detailedBreadcrumbsSuppressed;
    }

    private static String key(String targetMap, String itemId) {
        return targetMap + "|" + itemId;
    }
}
