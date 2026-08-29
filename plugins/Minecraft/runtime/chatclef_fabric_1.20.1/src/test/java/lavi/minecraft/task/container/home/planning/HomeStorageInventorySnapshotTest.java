package lavi.minecraft.task.container.home.planning;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify full-slot capture invariants independently of live Minecraft state.
class HomeStorageInventorySnapshotTest {
    @Test
    void preservesAnEmptySelectedMainSlot() {
        HomeStorageStackFingerprint fingerprint =
                HomeStorageStackFingerprint.of("minecraft:stone", 0, null);
        HomeStorageInventorySnapshot snapshot = new HomeStorageInventorySnapshot(
                List.of(
                        HomeStorageInventorySlotSnapshot.empty(
                                HomeStorageStackLocation.MAIN, 0
                        ),
                        HomeStorageInventorySlotSnapshot.occupied(
                                HomeStorageStackLocation.MAIN, 1, fingerprint, 4
                        )
                ),
                List.of(stack(1, fingerprint, 4, false)),
                0
        );

        assertEquals(0, snapshot.selectedMainSlot());
        assertFalse(snapshot.slot(HomeStorageStackLocation.MAIN, 0)
                .orElseThrow().occupied());
    }

    @Test
    void rejectsDuplicateLogicalSlotKeys() {
        assertThrows(IllegalArgumentException.class, () ->
                new HomeStorageInventorySnapshot(
                        List.of(
                                HomeStorageInventorySlotSnapshot.empty(
                                        HomeStorageStackLocation.MAIN, 0
                                ),
                                HomeStorageInventorySlotSnapshot.empty(
                                        HomeStorageStackLocation.MAIN, 0
                                )
                        ),
                        List.of(),
                        0
                ));
    }

    @Test
    void distinguishesUnavailableCaptureFromAnEmptySnapshot() {
        assertTrue(new HomeStorageInventorySnapshotReader().capture(null).isEmpty());

        HomeStorageInventorySnapshot empty = new HomeStorageInventorySnapshot(
                List.of(HomeStorageInventorySlotSnapshot.empty(
                        HomeStorageStackLocation.MAIN, 0
                )),
                List.of(),
                0
        );
        assertTrue(empty.occupiedStacks().isEmpty());
    }

    @Test
    void preservesArmorAndOffhandOccupiedAndEmptyStates() {
        HomeStorageStackFingerprint helmet =
                HomeStorageStackFingerprint.of("minecraft:diamond_helmet", 0, null);
        HomeStorageStackFingerprint shield =
                HomeStorageStackFingerprint.of("minecraft:shield", 0, null);
        HomeStorageInventorySnapshot snapshot = new HomeStorageInventorySnapshot(
                List.of(
                        HomeStorageInventorySlotSnapshot.empty(
                                HomeStorageStackLocation.MAIN, 0
                        ),
                        HomeStorageInventorySlotSnapshot.occupied(
                                HomeStorageStackLocation.ARMOR, 0, helmet, 1
                        ),
                        HomeStorageInventorySlotSnapshot.empty(
                                HomeStorageStackLocation.ARMOR, 1
                        ),
                        HomeStorageInventorySlotSnapshot.occupied(
                                HomeStorageStackLocation.OFFHAND, 0, shield, 1
                        )
                ),
                List.of(
                        stack(HomeStorageStackLocation.ARMOR, 0, helmet, 1, false),
                        stack(HomeStorageStackLocation.OFFHAND, 0, shield, 1, false)
                ),
                0
        );

        assertTrue(snapshot.slot(HomeStorageStackLocation.ARMOR, 0)
                .orElseThrow().occupied());
        assertFalse(snapshot.slot(HomeStorageStackLocation.ARMOR, 1)
                .orElseThrow().occupied());
        assertTrue(snapshot.slot(HomeStorageStackLocation.OFFHAND, 0)
                .orElseThrow().occupied());
    }

    private static HomeStorageStackSnapshot stack(
            int slot,
            HomeStorageStackFingerprint fingerprint,
            int count,
            boolean selected) {
        return stack(
                HomeStorageStackLocation.MAIN,
                slot,
                fingerprint,
                count,
                selected
        );
    }

    private static HomeStorageStackSnapshot stack(
            HomeStorageStackLocation location,
            int slot,
            HomeStorageStackFingerprint fingerprint,
            int count,
            boolean selected) {
        return new HomeStorageStackSnapshot(
                slot,
                location,
                fingerprint,
                count,
                AutoDepositItemRole.NONE,
                0,
                0,
                0,
                0,
                selected,
                false,
                false,
                0
        );
    }
}
