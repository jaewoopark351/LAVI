package lavi.minecraft.diagnostics.observation;

import java.lang.ref.WeakReference;

/** Captured on the owner thread; workers cannot silently rebind to a later world or mode. */
public final class ObservationActivation {
    private final long epoch, generation;
    private final WeakReference<Object> instance, world;
    private volatile boolean valid = true;

    public ObservationActivation(long epoch, long generation, Object instance, Object world) {
        this.epoch = epoch;
        this.generation = generation;
        this.instance = new WeakReference<>(instance);
        this.world = new WeakReference<>(world);
    }

    public boolean matches(long epoch, Object instance, Object world) {
        return valid && this.epoch == epoch && this.instance.get() == instance && this.world.get() == world;
    }
    public boolean live(long epoch) {
        return valid && this.epoch == epoch && instance.get() != null && world.get() != null;
    }
    public boolean isCurrent() { return ObservationDiagnostics.isCurrent(this); }
    public long generation() { return generation; }
    public long epoch() { return epoch; }
    public void invalidate() { valid = false; instance.clear(); world.clear(); }
}
