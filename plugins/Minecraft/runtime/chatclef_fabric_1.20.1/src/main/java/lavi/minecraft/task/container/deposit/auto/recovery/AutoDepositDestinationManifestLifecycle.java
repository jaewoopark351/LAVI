package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;

import java.util.Objects;

//20260831_kpopmodder: Own automatic-deposit manifest subscription start and stop boundaries.
/** Owns subscription lifecycle for one automatic-deposit destination manifest. */
public final class AutoDepositDestinationManifestLifecycle {
    private final AutoDepositDestinationManifest manifest;
    private AutoDepositDestinationManifestTracker tracker;

    public AutoDepositDestinationManifestLifecycle(AutoDepositDestinationManifest manifest) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
    }

    public void start(AltoClef mod) {
        if (tracker == null) {
            tracker = new AutoDepositDestinationManifestTracker(
                    Objects.requireNonNull(mod, "mod"),
                    manifest
            );
        }
        tracker.start();
    }

    public void stop() {
        if (tracker != null) {
            tracker.stop();
        }
    }

    public AutoDepositDestinationManifest manifest() {
        return manifest;
    }
}
