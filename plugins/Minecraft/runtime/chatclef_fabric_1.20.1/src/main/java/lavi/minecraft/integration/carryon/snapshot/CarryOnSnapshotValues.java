package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Share defensive snapshot string normalization across narrow snapshot collectors.
final class CarryOnSnapshotValues {
    private CarryOnSnapshotValues() {
    }

    static String value(String value) {
        return value == null || value.isBlank() ? "unavailable" : value;
    }

    static String value(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }

    static String unavailable(Throwable throwable) {
        return throwable == null ? "unavailable" : "unavailable:" + throwable.getClass().getSimpleName();
    }

    static String taskName(Object task) {
        return task == null ? "unavailable" : task.getClass().getSimpleName();
    }
}
