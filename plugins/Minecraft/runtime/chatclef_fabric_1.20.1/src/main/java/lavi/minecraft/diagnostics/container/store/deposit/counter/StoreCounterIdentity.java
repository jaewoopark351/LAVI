package lavi.minecraft.diagnostics.container.store.deposit.counter;

import java.util.concurrent.atomic.AtomicLong;

//20260914_kpopmodder: Give local observation subjects stable IDs without an object registry or wire changes.
public final class StoreCounterIdentity {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private StoreCounterIdentity() { }

    public static long next() {
        long previous = SEQUENCE.getAndUpdate(value -> value == Long.MAX_VALUE ? value : value + 1);
        return previous == Long.MAX_VALUE ? -1 : previous + 1;
    }
}
