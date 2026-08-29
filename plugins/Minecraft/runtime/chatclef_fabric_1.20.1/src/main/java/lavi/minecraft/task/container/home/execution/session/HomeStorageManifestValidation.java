package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//20260828_kpopmodder: Carry immutable diagnostics-neutral evidence for one baseline decision.
public record HomeStorageManifestValidation(
        boolean valid,
        String reason,
        Optional<HomeStorageStackLocation> location,
        int logicalSlot,
        int manifestStepIndex,
        String expectedDisposition,
        String expectedDispositionReason,
        String observationSource,
        Optional<HomeStorageInventorySlotSnapshot> expected,
        Optional<HomeStorageInventorySlotSnapshot> actual,
        OptionalInt expectedSelectedMainSlot,
        OptionalInt actualSelectedMainSlot) {

    public HomeStorageManifestValidation {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(expectedDisposition, "expectedDisposition");
        Objects.requireNonNull(expectedDispositionReason, "expectedDispositionReason");
        Objects.requireNonNull(observationSource, "observationSource");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(expectedSelectedMainSlot, "expectedSelectedMainSlot");
        Objects.requireNonNull(actualSelectedMainSlot, "actualSelectedMainSlot");
        if (valid && !"valid".equals(reason)) {
            throw new IllegalArgumentException("valid result must use the valid reason");
        }
    }

    public static HomeStorageManifestValidation validResult() {
        return new HomeStorageManifestValidation(
                true,
                "valid",
                Optional.empty(),
                -1,
                -1,
                "not_available",
                "not_available",
                "full_player_inventory_snapshot",
                Optional.empty(),
                Optional.empty(),
                OptionalInt.empty(),
                OptionalInt.empty()
        );
    }

    public static HomeStorageManifestValidation unavailable(String reason) {
        return new HomeStorageManifestValidation(
                false,
                reason,
                Optional.empty(),
                -1,
                -1,
                "not_available",
                "not_available",
                "snapshot_unavailable",
                Optional.empty(),
                Optional.empty(),
                OptionalInt.empty(),
                OptionalInt.empty()
        );
    }
}
