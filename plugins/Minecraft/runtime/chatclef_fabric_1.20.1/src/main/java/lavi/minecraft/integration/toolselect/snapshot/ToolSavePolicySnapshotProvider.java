package lavi.minecraft.integration.toolselect.snapshot;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

//20260805_kpopmodder: Publish whole immutable tool-save snapshots with volatile/atomic visibility.
public final class ToolSavePolicySnapshotProvider {
    private static final AtomicReference<ToolSavePolicySnapshot> CURRENT = new AtomicReference<>(
            ToolSavePolicySnapshot.unavailable(0L, 0L, "STARTUP", "unavailable", "unavailable")
    );

    private ToolSavePolicySnapshotProvider() {
    }

    public static ToolSavePolicySnapshot current() {
        return CURRENT.get();
    }

    public static void publish(ToolSavePolicySnapshot snapshot) {
        CURRENT.set(Objects.requireNonNull(snapshot, "snapshot"));
    }
}
