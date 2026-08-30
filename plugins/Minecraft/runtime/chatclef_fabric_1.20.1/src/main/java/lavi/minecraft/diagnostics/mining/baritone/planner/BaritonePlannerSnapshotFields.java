package lavi.minecraft.diagnostics.mining.baritone.planner;

//20260830_kpopmodder: Project planner snapshot data into the existing event-field contract.
public final class BaritonePlannerSnapshotFields {
    private BaritonePlannerSnapshotFields() {
    }

    public static Object[] project(BaritonePlannerSnapshotData data, String suffix) {
        String normalizedSuffix = suffix == null ? "" : suffix;
        Object[] fields = new Object[BaritonePlannerSnapshotSchema.EVENT_KEYS.length * 2];
        for (int index = 0; index < BaritonePlannerSnapshotSchema.EVENT_KEYS.length; index++) {
            fields[index * 2] = BaritonePlannerSnapshotSchema.EVENT_KEYS[index] + normalizedSuffix;
            fields[index * 2 + 1] = data == null ? "unavailable" : data.value(index);
        }
        return fields;
    }
}
