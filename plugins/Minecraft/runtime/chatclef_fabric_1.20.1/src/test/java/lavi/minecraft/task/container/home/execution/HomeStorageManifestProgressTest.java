package lavi.minecraft.task.container.home.execution;

import lavi.minecraft.task.container.home.planning.HomeStorageDisposition;
import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260827_kpopmodder: Added focused tests for partial exact-slot transfer progress.
class HomeStorageManifestProgressTest {
    @Test
    void preservesPartialRemainderForTheSameExactSlot() {
        HomeStorageManifestStep step = new HomeStorageManifestStep(
                17,
                HomeStorageStackFingerprint.of("minecraft:redstone", 0, null),
                64,
                HomeStorageManifestStep.TransferMode.WHOLE_STACK_QUICK_MOVE,
                HomeStorageDisposition.STORE_HOME,
                "manual_home_surplus",
                7L
        );
        HomeStorageManifestProgress progress = new HomeStorageManifestProgress(
                new HomeStorageManifest(7L, List.of(step))
        );

        progress.confirm(step, 20, 44);

        assertEquals(44, progress.expectedCount(step));
        assertEquals(20, progress.confirmedItemCount());
        assertEquals(1, progress.touchedStackCount());
        assertFalse(progress.complete());

        progress.confirm(step, 44, 0);
        assertTrue(progress.complete());
        assertEquals(1, progress.touchedStackCount());
    }
}
