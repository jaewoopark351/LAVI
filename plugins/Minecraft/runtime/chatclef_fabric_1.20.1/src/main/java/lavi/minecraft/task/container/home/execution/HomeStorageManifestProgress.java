package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

//20260827_kpopmodder: Own mutable confirmed progress separately from the immutable exact-slot manifest.
public final class HomeStorageManifestProgress {
    private final HomeStorageManifest manifest;
    private final Map<Integer, Integer> remainingByLogicalSlot = new LinkedHashMap<>();
    private int confirmedItemCount;
    private int touchedStackCount;

    public HomeStorageManifestProgress(HomeStorageManifest manifest) {
        this.manifest = manifest;
        for (HomeStorageManifestStep step : manifest.steps()) {
            remainingByLogicalSlot.put(
                    step.logicalPlayerInventorySlot(), step.expectedCount()
            );
        }
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

    public void confirm(
            HomeStorageManifestStep step,
            int transferredCount,
            int sourceCountAfter) {
        int expectedBefore = expectedCount(step);
        if (transferredCount <= 0
                || sourceCountAfter < 0
                || expectedBefore - transferredCount != sourceCountAfter) {
            throw new IllegalArgumentException("confirmed transfer does not match manifest progress");
        }
        if (expectedBefore == step.expectedCount()) {
            touchedStackCount++;
        }
        remainingByLogicalSlot.put(step.logicalPlayerInventorySlot(), sourceCountAfter);
        confirmedItemCount = saturatingAdd(confirmedItemCount, transferredCount);
    }

    public Validation validate(AltoClef mod, OptionalInt pendingLogicalSlot) {
        if (mod == null || mod.getPlayer() == null) {
            return new Validation(false, -1, "player_unavailable");
        }
        for (HomeStorageManifestStep step : manifest.steps()) {
            int logicalSlot = step.logicalPlayerInventorySlot();
            if (pendingLogicalSlot.isPresent() && pendingLogicalSlot.getAsInt() == logicalSlot) {
                continue;
            }
            int expected = expectedCount(step);
            ItemStack current = mod.getPlayer().getInventory().main.get(logicalSlot);
            if (expected == 0) {
                if (current != null && !current.isEmpty()
                        && step.fingerprint().matches(current)) {
                    return new Validation(false, logicalSlot, "completed_slot_repopulated");
                }
                continue;
            }
            if (current == null || current.isEmpty()) {
                return new Validation(false, logicalSlot, "planned_source_missing");
            }
            if (!step.fingerprint().matches(current)) {
                return new Validation(false, logicalSlot, "planned_fingerprint_changed");
            }
            if (current.getCount() != expected) {
                return new Validation(false, logicalSlot, "planned_count_changed");
            }
        }
        return new Validation(true, -1, "valid");
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

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    public record Validation(boolean valid, int logicalSlot, String reason) {
    }
}
