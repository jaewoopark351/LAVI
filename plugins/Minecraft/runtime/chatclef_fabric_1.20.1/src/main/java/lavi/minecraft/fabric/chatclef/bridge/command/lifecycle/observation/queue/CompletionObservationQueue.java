//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.queue;

import java.util.ArrayDeque;

//20260913_kpopmodder: Admission order is behavior-owned and independent of diagnostic mode/sequence counters.
public final class CompletionObservationQueue<T> {
    private final ArrayDeque<Entry<T>> queue = new ArrayDeque<>();
    private long accepted;
    private long dequeued;

    public synchronized void offer(T value) {
        if (value == null) throw new IllegalArgumentException("completion observation required");
        queue.addLast(new Entry<>(++accepted, value));
    }
    public synchronized T poll() {
        Entry<T> entry = queue.pollFirst();
        if (entry == null) return null;
        dequeued = entry.sequence;
        return entry.value;
    }
    public synchronized int size() { return queue.size(); }
    public synchronized long acceptedThrough() { return accepted; }
    public synchronized long dequeuedThrough() { return dequeued; }
    private record Entry<T>(long sequence, T value) { }
}
//#endif
