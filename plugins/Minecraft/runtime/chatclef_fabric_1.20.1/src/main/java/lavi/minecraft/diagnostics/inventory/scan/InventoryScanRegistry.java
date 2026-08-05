package lavi.minecraft.diagnostics.inventory.scan;

import lavi.minecraft.diagnostics.inventory.InventoryScanContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//20260805_kpopmodder: Own diagnostic-only active InventorySubTracker scan bookkeeping.
public final class InventoryScanRegistry {
    private final ConcurrentMap<Long, InventoryScanContext> activeScans = new ConcurrentHashMap<>();
    private final ThreadLocal<ArrayDeque<InventoryScanContext>> threadScans =
            ThreadLocal.withInitial(ArrayDeque::new);

    public int scanDepthOnCurrentThread() {
        return threadScans.get().size();
    }

    public void activate(InventoryScanContext context) {
        threadScans.get().push(context);
        activeScans.put(context.scanId(), context);
    }

    public InventoryScanContext currentContext(String trackerIdentity) {
        for (InventoryScanContext context : threadScans.get()) {
            if (context.trackerIdentity().equals(trackerIdentity)) {
                return context;
            }
        }
        return null;
    }

    public void deactivate(InventoryScanContext context) {
        activeScans.remove(context.scanId());
        ArrayDeque<InventoryScanContext> scans = threadScans.get();
        if (!scans.isEmpty() && scans.peek() == context) {
            scans.pop();
            return;
        }
        scans.remove(context);
    }

    public int activeScanCount() {
        return activeScans.size();
    }

    public int activeScanCount(String trackerIdentity) {
        int count = 0;
        for (InventoryScanContext context : activeScans.values()) {
            if (context.trackerIdentity().equals(trackerIdentity)) {
                count++;
            }
        }
        return count;
    }

    public List<InventoryScanContext> activeScansForTracker(String trackerIdentity) {
        List<InventoryScanContext> result = new ArrayList<>();
        for (InventoryScanContext context : activeScans.values()) {
            if (context.trackerIdentity().equals(trackerIdentity)) {
                result.add(context);
            }
        }
        return result;
    }

    public Collection<InventoryScanContext> activeScans() {
        return activeScans.values();
    }
}
