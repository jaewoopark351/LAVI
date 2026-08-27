package lavi.minecraft.task.container.home.planning;

import java.util.Objects;

//20260827_kpopmodder: Added this type file to expose one planner decision for focused tests and diagnostics.
public record HomeStoragePlanEntry(
        HomeStorageStackSnapshot snapshot,
        HomeStorageDisposition disposition,
        String reason) {

    public HomeStoragePlanEntry {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(disposition, "disposition");
        Objects.requireNonNull(reason, "reason");
    }
}
