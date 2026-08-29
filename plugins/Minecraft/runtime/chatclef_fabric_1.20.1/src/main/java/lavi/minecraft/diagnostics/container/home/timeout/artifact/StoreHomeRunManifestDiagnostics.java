package lavi.minecraft.diagnostics.container.home.timeout.artifact;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;

//20260828_kpopmodder: Emit one STORE_HOME run manifest and reuse its ID for every operation event.
public final class StoreHomeRunManifestDiagnostics {
    private static final String EVENT_NAME = "STORE_HOME_RUN_MANIFEST";

    private static StoreHomeRunManifestSnapshot snapshot;
    private static boolean emissionSucceeded;

    private StoreHomeRunManifestDiagnostics() {
    }

    public static synchronized String currentRunManifestId() {
        return snapshot().runManifestId();
    }

    public static synchronized void emitOnce(
            Task task,
            StoreHomeDiagnosticEmitter emitter) {
        if (emissionSucceeded || !ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreHomeRunManifestSnapshot current = snapshot();
        emissionSucceeded = emitter.emitBoundary(
                EVENT_NAME,
                "store_home_controlled_run_manifest",
                task,
                current.fields()
        );
    }

    private static StoreHomeRunManifestSnapshot snapshot() {
        if (snapshot == null) {
            String mode = ChatClefDiagnostics.isVerboseEnabled()
                    ? "VERBOSE"
                    : ChatClefDiagnostics.isBoundaryEnabled() ? "BOUNDARY" : "OFF";
            snapshot = StoreHomeRunManifestSnapshot.capture(
                    mode,
                    StoreHomeRunManifestDiagnostics.class
            );
        }
        return snapshot;
    }
}
