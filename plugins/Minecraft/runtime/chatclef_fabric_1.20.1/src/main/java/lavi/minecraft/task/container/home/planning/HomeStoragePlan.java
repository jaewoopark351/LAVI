package lavi.minecraft.task.container.home.planning;

import java.util.List;

//20260827_kpopmodder: Added this type file to bind loadout decisions to one immutable manifest revision.
public record HomeStoragePlan(
        long revision,
        List<HomeStoragePlanEntry> entries,
        HomeStorageManifest manifest) {

    public HomeStoragePlan {
        entries = List.copyOf(entries);
        if (manifest.revision() != revision) {
            throw new IllegalArgumentException("plan and manifest revisions must match");
        }
    }
}
