package lavi.minecraft.task.container.home.execution;

import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Own session-local confirmed progress separately from the immutable exact-slot manifest.
public final class HomeStorageManifestProgress {
    private final HomeStorageManifest manifest;
    private final Map<Integer, Integer> remainingByLogicalSlot;
    private final int confirmedItemCount;
    private final int touchedStackCount;

    public HomeStorageManifestProgress(HomeStorageManifest manifest) {
        this(manifest, initialRemaining(manifest), 0, 0);
    }

    private HomeStorageManifestProgress(
            HomeStorageManifest manifest,
            Map<Integer, Integer> remainingByLogicalSlot,
            int confirmedItemCount,
            int touchedStackCount) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.remainingByLogicalSlot = Collections.unmodifiableMap(
                new LinkedHashMap<>(remainingByLogicalSlot)
        );
        this.confirmedItemCount = confirmedItemCount;
        this.touchedStackCount = touchedStackCount;
    }

    public Optional<HomeStorageManifestStep> currentStep() {
        return manifest.steps().stream()
                .filter(step -> expectedCount(step) > 0)
                .findFirst();
    }

    public int expectedCount(HomeStorageManifestStep step) {
        return remainingByLogicalSlot.getOrDefault(
                step.logicalPlayerInventorySlot(), 0
        );
    }

    //20260828_kpopmodder: Return a new confirmed overlay so commit preparation has no partial mutation.
    public HomeStorageManifestProgress confirmed(
            HomeStorageManifestStep step,
            int transferredCount,
            int sourceCountAfter) {
        if (!manifest.steps().contains(step)) {
            throw new IllegalArgumentException("step does not belong to this manifest");
        }
        int expectedBefore = expectedCount(step);
        if (transferredCount <= 0
                || sourceCountAfter < 0
                || expectedBefore - transferredCount != sourceCountAfter) {
            throw new IllegalArgumentException("confirmed transfer does not match manifest progress");
        }
        Map<Integer, Integer> remaining = new LinkedHashMap<>(remainingByLogicalSlot);
        remaining.put(step.logicalPlayerInventorySlot(), sourceCountAfter);
        return new HomeStorageManifestProgress(
                manifest,
                remaining,
                saturatingAdd(confirmedItemCount, transferredCount),
                expectedBefore == step.expectedCount()
                        ? saturatingAdd(touchedStackCount, 1)
                        : touchedStackCount
        );
    }

    public int confirmedItemCount() {
        return confirmedItemCount;
    }

    public int touchedStackCount() {
        return touchedStackCount;
    }

    public int remainingStackCount() {
        return (int) remainingByLogicalSlot.values().stream()
                .filter(count -> count > 0)
                .count();
    }

    public boolean complete() {
        return remainingStackCount() == 0;
    }

    public long manifestRevision() {
        return manifest.revision();
    }

    public int manifestStepCount() {
        return manifest.steps().size();
    }

    public int manifestStepIndex(HomeStorageManifestStep step) {
        return manifest.steps().indexOf(step);
    }

    public int currentStepLogicalSlot() {
        return currentStep()
                .map(HomeStorageManifestStep::logicalPlayerInventorySlot)
                .orElse(-1);
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private static Map<Integer, Integer> initialRemaining(HomeStorageManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        Map<Integer, Integer> remaining = new LinkedHashMap<>();
        for (HomeStorageManifestStep step : manifest.steps()) {
            remaining.put(step.logicalPlayerInventorySlot(), step.expectedCount());
        }
        return remaining;
    }
}
