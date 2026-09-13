package lavi.minecraft.diagnostics.baritone.correlation;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;

//20260913_kpopmodder: Bound object links/history while keeping current and next origins pinned after eviction.
public final class BuilderTraceLedger {
    public static final int LINK_CAP = 64;
    public static final int HISTORY_CAP = 32;
    private final Map<Object, BuilderPathProvenance> links = new IdentityHashMap<>();
    private final ArrayDeque<Object> order = new ArrayDeque<>();
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private final Map<String, String> transitionFingerprints = new java.util.HashMap<>();
    private Object currentPath;
    private Object nextPath;
    private BuilderPathProvenance currentOrigin;
    private BuilderPathProvenance nextOrigin;
    private long evictions;
    private long sequence;
    private boolean invalidated;

    public synchronized void bind(Object key, BuilderPathProvenance value) {
        if (invalidated || key == null || value == null) return;
        if (!links.containsKey(key)) {
            if (links.size() == LINK_CAP) { links.remove(order.removeFirst()); evictions++; }
            order.addLast(key);
        }
        links.put(key, value);
        if (currentPath == key) currentOrigin = value;
        if (nextPath == key) nextOrigin = value;
    }

    public synchronized BuilderPathProvenance find(Object key) {
        if (key == null) return null;
        if (key == currentPath && currentOrigin != null) return currentOrigin;
        if (key == nextPath && nextOrigin != null) return nextOrigin;
        return links.get(key);
    }

    public synchronized void adopt(Object current, Object next) {
        if (invalidated) return;
        BuilderPathProvenance currentMetadata = find(current);
        BuilderPathProvenance nextMetadata = find(next);
        currentPath = current;
        nextPath = next;
        currentOrigin = currentMetadata;
        nextOrigin = nextMetadata;
    }

    public synchronized void remove(Object key) {
        links.remove(key);
        order.removeIf(existing -> existing == key);
    }

    public synchronized long record(String transition) {
        if (invalidated) return sequence;
        if (history.size() == HISTORY_CAP) history.removeFirst();
        history.addLast(++sequence + ":" + bound(transition, 512));
        return sequence;
    }
    public synchronized long recordIfChanged(String semantic, String transition) {
        if (invalidated) return sequence;
        if (transition.equals(transitionFingerprints.get(semantic))) return sequence;
        if (transitionFingerprints.containsKey(semantic) || transitionFingerprints.size() < 64)
            transitionFingerprints.put(semantic, transition);
        return record(transition);
    }

    public synchronized String recent() { return String.join(";", history); }
    public synchronized Object[] recentFields() {
        String[] items = history.toArray(String[]::new);
        int emitted = Math.min(items.length, 8);
        Object[] result = new Object[emitted * 2 + 4];
        result[0] = "historyRetainedCount"; result[1] = items.length;
        result[2] = "historyOmittedFromPayload"; result[3] = items.length - emitted;
        for (int i = 0; i < emitted; i++) {
            result[4 + i * 2] = "recentPathTransition" + i;
            result[5 + i * 2] = bound(items[items.length - emitted + i], 256);
        }
        return result;
    }
    public synchronized long evictions() { return evictions; }
    public synchronized int links() { return links.size(); }
    public synchronized int historySize() { return history.size(); }
    public synchronized void clear() {
        links.clear(); order.clear(); history.clear(); transitionFingerprints.clear();
        currentPath = null; nextPath = null; currentOrigin = null; nextOrigin = null;
    }

    //20260913_kpopmodder: Serialize teardown with worker writes; a cleared old generation stays unusable.
    public synchronized void invalidate() {
        invalidated = true;
        clear();
    }

    private static String bound(String value, int cap) {
        return value.length() > cap ? value.substring(0, cap) : value;
    }
}
