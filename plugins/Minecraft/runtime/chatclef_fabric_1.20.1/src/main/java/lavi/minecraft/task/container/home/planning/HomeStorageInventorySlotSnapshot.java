package lavi.minecraft.task.container.home.planning;

import java.util.Objects;
import java.util.Optional;

//20260828_kpopmodder: Capture one occupied or empty player-held slot without retaining a live ItemStack.
public record HomeStorageInventorySlotSnapshot(
        HomeStorageStackLocation location,
        int logicalSlot,
        Optional<HomeStorageStackFingerprint> fingerprint,
        int count) {

    public HomeStorageInventorySlotSnapshot {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (logicalSlot < 0) {
            throw new IllegalArgumentException("logicalSlot must be non-negative");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        if ((count == 0) != fingerprint.isEmpty()) {
            throw new IllegalArgumentException(
                    "empty slots must have no fingerprint and occupied slots must have one"
            );
        }
    }

    public static HomeStorageInventorySlotSnapshot empty(
            HomeStorageStackLocation location,
            int logicalSlot) {
        return new HomeStorageInventorySlotSnapshot(
                location, logicalSlot, Optional.empty(), 0
        );
    }

    public static HomeStorageInventorySlotSnapshot occupied(
            HomeStorageStackLocation location,
            int logicalSlot,
            HomeStorageStackFingerprint fingerprint,
            int count) {
        return new HomeStorageInventorySlotSnapshot(
                location,
                logicalSlot,
                Optional.of(Objects.requireNonNull(fingerprint, "fingerprint")),
                count
        );
    }

    public boolean occupied() {
        return fingerprint.isPresent();
    }

    public HomeStorageInventorySlotSnapshot withCount(int expectedCount) {
        if (expectedCount < 0) {
            throw new IllegalArgumentException("expectedCount must be non-negative");
        }
        if (expectedCount == 0) {
            return empty(location, logicalSlot);
        }
        return occupied(
                location,
                logicalSlot,
                fingerprint.orElseThrow(() -> new IllegalStateException(
                        "an empty activation slot cannot gain an expected count"
                )),
                expectedCount
        );
    }
}
