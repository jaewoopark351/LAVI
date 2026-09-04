package lavi.minecraft.diagnostics.container.gui.tick;

//20260904_kpopmodder: Observe HEAD, published-boundary, and RETURN without extending a tick window.
public final class ContainerClientTickWindow {
    private long activeSerial = -1L;
    private long lastIssuedSerial = -1L;
    private long lastPublishedBoundarySerial = -1L;
    private boolean started;

    public synchronized void onHead(long serial) {
        started = true;
        activeSerial = serial;
        lastIssuedSerial = serial;
    }

    public synchronized void onBoundaryPublished(long serial) {
        if (activeSerial == serial) {
            lastPublishedBoundarySerial = serial;
        }
    }

    public synchronized void onReturn(long serial) {
        if (activeSerial == serial) {
            activeSerial = -1L;
        }
    }

    public synchronized ContainerClientTickWindowSnapshot snapshot() {
        if (!started) {
            return new ContainerClientTickWindowSnapshot(
                    ContainerClientTickWindowState.UNAVAILABLE,
                    false,
                    -1L,
                    -1L,
                    -1L
            );
        }
        if (activeSerial >= 0L) {
            return new ContainerClientTickWindowSnapshot(
                    lastPublishedBoundarySerial == activeSerial
                            ? ContainerClientTickWindowState.ACTIVE_BOUNDARY_CLAIMED
                            : ContainerClientTickWindowState.ACTIVE_BEFORE_RETURN,
                    true,
                    activeSerial,
                    lastIssuedSerial,
                    lastPublishedBoundarySerial
            );
        }
        return new ContainerClientTickWindowSnapshot(
                ContainerClientTickWindowState.BETWEEN_TICKS,
                false,
                -1L,
                lastIssuedSerial,
                lastPublishedBoundarySerial
        );
    }

    public synchronized void clear() {
        activeSerial = -1L;
        lastIssuedSerial = -1L;
        lastPublishedBoundarySerial = -1L;
        started = false;
    }
}
