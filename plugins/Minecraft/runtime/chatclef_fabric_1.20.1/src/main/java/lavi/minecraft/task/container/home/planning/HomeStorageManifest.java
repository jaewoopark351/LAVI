package lavi.minecraft.task.container.home.planning;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//20260827_kpopmodder: Own one immutable exact-slot transfer manifest for a trusted-container session.
public record HomeStorageManifest(long revision, List<HomeStorageManifestStep> steps) {
    public HomeStorageManifest {
        steps = List.copyOf(steps);
        Set<Integer> logicalSlots = new HashSet<>();
        for (HomeStorageManifestStep step : steps) {
            if (step.loadoutPlanRevision() != revision) {
                throw new IllegalArgumentException("manifest revision mismatch");
            }
            if (!logicalSlots.add(step.logicalPlayerInventorySlot())) {
                throw new IllegalArgumentException("duplicate logical player slot");
            }
        }
    }
}
