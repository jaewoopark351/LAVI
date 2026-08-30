package lavi.minecraft.diagnostics.mining.baritone.planner;

//20260830_kpopmodder: Store only normalized planner observation values.
public final class BaritonePlannerSnapshotData {
    private final String[] values;

    public BaritonePlannerSnapshotData(String... values) {
        this.values = values == null ? new String[0] : values.clone();
    }

    public String value(int index) {
        return index >= 0 && index < values.length ? values[index] : "unavailable";
    }

    public int size() {
        return values.length;
    }
}
