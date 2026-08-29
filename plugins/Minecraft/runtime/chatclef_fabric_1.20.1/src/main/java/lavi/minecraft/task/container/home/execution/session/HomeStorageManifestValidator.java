package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.home.execution.HomeStorageManifestProgress;
import lavi.minecraft.task.container.home.planning.HomeStorageDisposition;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlanEntry;
import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;

import java.util.Optional;
import java.util.OptionalInt;

//20260828_kpopmodder: Compare a fresh capture with one activation baseline plus confirmed overlay.
public final class HomeStorageManifestValidator {
    public HomeStorageManifestValidation validate(
            HomeStorageActivationBaseline baseline,
            HomeStorageManifestProgress progress,
            HomeStorageInventorySnapshot current,
            OptionalInt pendingLogicalSlot) {
        if (baseline.inventory().selectedMainSlot() != current.selectedMainSlot()) {
            int expectedSelected = baseline.inventory().selectedMainSlot();
            Optional<HomeStorageInventorySlotSnapshot> expected = baseline.inventory().slot(
                    HomeStorageStackLocation.MAIN, expectedSelected
            );
            Optional<HomeStorageInventorySlotSnapshot> actual = current.slot(
                    HomeStorageStackLocation.MAIN, expectedSelected
            );
            return mismatch(
                    baseline,
                    HomeStorageStackLocation.MAIN,
                    expectedSelected,
                    "selected_main_slot_changed",
                    expected,
                    actual,
                    OptionalInt.of(expectedSelected),
                    OptionalInt.of(current.selectedMainSlot())
            );
        }

        for (HomeStorageInventorySlotSnapshot activationSlot
                : baseline.inventory().slots()) {
            if (activationSlot.location() == HomeStorageStackLocation.MAIN
                    && pendingLogicalSlot.isPresent()
                    && pendingLogicalSlot.getAsInt() == activationSlot.logicalSlot()) {
                continue;
            }
            Optional<HomeStorageManifestStep> step = activationSlot.location()
                    == HomeStorageStackLocation.MAIN
                    ? baseline.manifestStep(activationSlot.logicalSlot())
                    : Optional.empty();
            HomeStorageInventorySlotSnapshot expected = step
                    .map(value -> activationSlot.withCount(progress.expectedCount(value)))
                    .orElse(activationSlot);
            Optional<HomeStorageInventorySlotSnapshot> actual = current.slot(
                    activationSlot.location(), activationSlot.logicalSlot()
            );
            if (actual.isEmpty()) {
                return mismatch(
                        baseline,
                        activationSlot.location(),
                        activationSlot.logicalSlot(),
                        "captured_slot_unavailable",
                        Optional.of(expected),
                        Optional.empty(),
                        OptionalInt.empty(),
                        OptionalInt.empty()
                );
            }
            if (same(expected, actual.orElseThrow())) {
                continue;
            }
            return mismatch(
                    baseline,
                    activationSlot.location(),
                    activationSlot.logicalSlot(),
                    reason(step, expected, actual.orElseThrow()),
                    Optional.of(expected),
                    actual,
                    OptionalInt.empty(),
                    OptionalInt.empty()
            );
        }
        return HomeStorageManifestValidation.validResult();
    }

    private static String reason(
            Optional<HomeStorageManifestStep> step,
            HomeStorageInventorySlotSnapshot expected,
            HomeStorageInventorySlotSnapshot actual) {
        if (step.isPresent()) {
            if (!expected.occupied() && actual.occupied()) {
                return "completed_slot_repopulated";
            }
            if (expected.occupied() && !actual.occupied()) {
                return "planned_source_missing";
            }
            if (!expected.fingerprint().equals(actual.fingerprint())) {
                return "planned_fingerprint_changed";
            }
            return "planned_count_changed";
        }
        if (!expected.occupied() && actual.occupied()) {
            return "baseline_empty_slot_changed";
        }
        if (expected.occupied() && !actual.occupied()) {
            return "kept_stack_missing";
        }
        if (!expected.fingerprint().equals(actual.fingerprint())) {
            return "kept_fingerprint_changed";
        }
        return "kept_count_changed";
    }

    private static boolean same(
            HomeStorageInventorySlotSnapshot expected,
            HomeStorageInventorySlotSnapshot actual) {
        return expected.location() == actual.location()
                && expected.logicalSlot() == actual.logicalSlot()
                && expected.count() == actual.count()
                && expected.fingerprint().equals(actual.fingerprint());
    }

    private static HomeStorageManifestValidation mismatch(
            HomeStorageActivationBaseline baseline,
            HomeStorageStackLocation location,
            int logicalSlot,
            String reason,
            Optional<HomeStorageInventorySlotSnapshot> expected,
            Optional<HomeStorageInventorySlotSnapshot> actual,
            OptionalInt expectedSelected,
            OptionalInt actualSelected) {
        Optional<HomeStoragePlanEntry> entry = baseline.entry(location, logicalSlot);
        Optional<HomeStorageManifestStep> step = location == HomeStorageStackLocation.MAIN
                ? baseline.manifestStep(logicalSlot)
                : Optional.empty();
        String disposition = entry.map(HomeStoragePlanEntry::disposition)
                .map(HomeStorageDisposition::name)
                .orElse("EMPTY_BASELINE");
        String dispositionReason = entry.map(HomeStoragePlanEntry::reason)
                .orElse("activation_slot_empty");
        int stepIndex = step.map(value -> baseline.plan().manifest().steps().indexOf(value))
                .orElse(-1);
        return new HomeStorageManifestValidation(
                false,
                reason,
                Optional.of(location),
                logicalSlot,
                stepIndex,
                disposition,
                dispositionReason,
                "full_player_inventory_snapshot",
                expected,
                actual,
                expectedSelected,
                actualSelected
        );
    }
}
