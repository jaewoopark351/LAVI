package lavi.minecraft.diagnostics.mining.baritone;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.mining.baritone.planner.BaritonePlannerSnapshotCapture;
import lavi.minecraft.diagnostics.mining.baritone.planner.BaritonePlannerSnapshotData;
import lavi.minecraft.diagnostics.mining.baritone.planner.BaritonePlannerSnapshotFields;

//20260830_kpopmodder: Preserve the planner snapshot API as a thin compatibility facade.
public final class BaritonePlannerDiagnosticSnapshot {
    private final BaritonePlannerSnapshotData data;

    private BaritonePlannerDiagnosticSnapshot(BaritonePlannerSnapshotData data) {
        this.data = data;
    }

    public static BaritonePlannerDiagnosticSnapshot capture(AltoClef mod) {
        return new BaritonePlannerDiagnosticSnapshot(BaritonePlannerSnapshotCapture.capture(mod));
    }

    public Object[] fields() {
        return fields("");
    }

    public Object[] fields(String suffix) {
        return BaritonePlannerSnapshotFields.project(data, suffix);
    }
}
