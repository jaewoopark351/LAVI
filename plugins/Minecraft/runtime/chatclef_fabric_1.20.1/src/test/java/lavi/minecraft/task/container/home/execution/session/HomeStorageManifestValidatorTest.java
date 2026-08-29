package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;
import lavi.minecraft.task.container.home.execution.HomeStorageManifestProgress;
import lavi.minecraft.task.container.home.planning.HomeStorageDisposition;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.planning.HomeStoragePlanEntry;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;
import lavi.minecraft.task.container.home.planning.HomeStorageStackSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify full-baseline validation and pending-source exclusion.
class HomeStorageManifestValidatorTest {
    private static final HomeStorageStackFingerprint SURPLUS =
            HomeStorageStackFingerprint.of("minecraft:cobblestone", 0, null);
    private static final HomeStorageStackFingerprint KEPT =
            HomeStorageStackFingerprint.of("minecraft:diamond_pickaxe", 0, null);
    private static final HomeStorageStackFingerprint INTRUDER =
            HomeStorageStackFingerprint.of("minecraft:dirt", 0, null);
    private static final HomeStorageStackFingerprint ARMOR =
            HomeStorageStackFingerprint.of("minecraft:diamond_helmet", 0, null);
    private static final HomeStorageStackFingerprint OFFHAND =
            HomeStorageStackFingerprint.of("minecraft:shield", 0, null);

    @Test
    void acceptsConfirmedOverlayFromTheSameActivationBaseline() {
        Fixture fixture = fixture();
        HomeStorageManifestProgress progress = fixture.progress().confirmed(
                fixture.step(), 20, 44
        );

        HomeStorageManifestValidation validation = new HomeStorageManifestValidator()
                .validate(
                        fixture.baseline(),
                        progress,
                        snapshot(44, KEPT, false, 2),
                        OptionalInt.empty()
                );

        assertTrue(validation.valid());
    }

    @Test
    void excludesOnlyThePendingSourceSlot() {
        Fixture fixture = fixture();
        HomeStorageManifestValidator validator = new HomeStorageManifestValidator();

        assertTrue(validator.validate(
                fixture.baseline(),
                fixture.progress(),
                snapshot(3, KEPT, false, 2),
                OptionalInt.of(0)
        ).valid());

        HomeStorageManifestValidation keptChanged = validator.validate(
                fixture.baseline(),
                fixture.progress(),
                snapshot(3, INTRUDER, false, 2),
                OptionalInt.of(0)
        );
        assertFalse(keptChanged.valid());
        assertEquals("kept_fingerprint_changed", keptChanged.reason());
        assertEquals(2, keptChanged.logicalSlot());
    }

    @Test
    void rejectsSelectionAndPreviouslyEmptySlotChanges() {
        Fixture fixture = fixture();
        HomeStorageManifestValidator validator = new HomeStorageManifestValidator();

        HomeStorageManifestValidation selected = validator.validate(
                fixture.baseline(),
                fixture.progress(),
                snapshot(64, KEPT, false, 0),
                OptionalInt.empty()
        );
        assertEquals("selected_main_slot_changed", selected.reason());
        assertEquals(2, selected.expectedSelectedMainSlot().orElseThrow());
        assertEquals(0, selected.actualSelectedMainSlot().orElseThrow());

        HomeStorageManifestValidation emptyChanged = validator.validate(
                fixture.baseline(),
                fixture.progress(),
                snapshot(64, KEPT, true, 2),
                OptionalInt.empty()
        );
        assertEquals("baseline_empty_slot_changed", emptyChanged.reason());
        assertEquals(1, emptyChanged.logicalSlot());
    }

    @Test
    void rejectsArmorAndOffhandChanges() {
        Fixture fixture = fixture();
        HomeStorageManifestValidator validator = new HomeStorageManifestValidator();

        HomeStorageManifestValidation armorChanged = validator.validate(
                fixture.baseline(),
                fixture.progress(),
                snapshot(64, KEPT, false, 2, INTRUDER, OFFHAND),
                OptionalInt.empty()
        );
        assertEquals("kept_fingerprint_changed", armorChanged.reason());
        assertEquals(HomeStorageStackLocation.ARMOR,
                armorChanged.location().orElseThrow());

        HomeStorageManifestValidation offhandChanged = validator.validate(
                fixture.baseline(),
                fixture.progress(),
                snapshot(64, KEPT, false, 2, ARMOR, INTRUDER),
                OptionalInt.empty()
        );
        assertEquals("kept_fingerprint_changed", offhandChanged.reason());
        assertEquals(HomeStorageStackLocation.OFFHAND,
                offhandChanged.location().orElseThrow());
    }

    private static Fixture fixture() {
        HomeStorageInventorySnapshot activation = snapshot(64, KEPT, false, 2);
        HomeStorageStackSnapshot surplus = activation.occupiedStacks().stream()
                .filter(stack -> stack.location() == HomeStorageStackLocation.MAIN)
                .filter(stack -> stack.logicalSlot() == 0)
                .findFirst().orElseThrow();
        HomeStorageStackSnapshot kept = activation.occupiedStacks().stream()
                .filter(stack -> stack.location() == HomeStorageStackLocation.MAIN)
                .filter(stack -> stack.logicalSlot() == 2)
                .findFirst().orElseThrow();
        HomeStorageStackSnapshot armor = activation.occupiedStacks().stream()
                .filter(stack -> stack.location() == HomeStorageStackLocation.ARMOR)
                .findFirst().orElseThrow();
        HomeStorageStackSnapshot offhand = activation.occupiedStacks().stream()
                .filter(stack -> stack.location() == HomeStorageStackLocation.OFFHAND)
                .findFirst().orElseThrow();
        HomeStorageManifestStep step = new HomeStorageManifestStep(
                0,
                SURPLUS,
                64,
                HomeStorageManifestStep.TransferMode.WHOLE_STACK_QUICK_MOVE,
                HomeStorageDisposition.STORE_HOME,
                "manual_home_surplus",
                7L
        );
        HomeStoragePlan plan = new HomeStoragePlan(
                7L,
                List.of(
                        new HomeStoragePlanEntry(
                                surplus,
                                HomeStorageDisposition.STORE_HOME,
                                "manual_home_surplus"
                        ),
                        new HomeStoragePlanEntry(
                                kept,
                                HomeStorageDisposition.KEEP_LOADOUT,
                                "primary_pickaxe"
                        ),
                        new HomeStoragePlanEntry(
                                armor,
                                HomeStorageDisposition.KEEP_LOADOUT,
                                "currently_equipped_armor"
                        ),
                        new HomeStoragePlanEntry(
                                offhand,
                                HomeStorageDisposition.KEEP_LOADOUT,
                                "current_offhand"
                        )
                ),
                new HomeStorageManifest(7L, List.of(step))
        );
        HomeStorageActivationBaseline baseline =
                new HomeStorageActivationBaseline(activation, plan);
        return new Fixture(
                baseline,
                step,
                new HomeStorageManifestProgress(plan.manifest())
        );
    }

    private static HomeStorageInventorySnapshot snapshot(
            int surplusCount,
            HomeStorageStackFingerprint keptFingerprint,
            boolean fillPreviouslyEmptySlot,
            int selectedSlot) {
        return snapshot(
                surplusCount,
                keptFingerprint,
                fillPreviouslyEmptySlot,
                selectedSlot,
                ARMOR,
                OFFHAND
        );
    }

    private static HomeStorageInventorySnapshot snapshot(
            int surplusCount,
            HomeStorageStackFingerprint keptFingerprint,
            boolean fillPreviouslyEmptySlot,
            int selectedSlot,
            HomeStorageStackFingerprint armorFingerprint,
            HomeStorageStackFingerprint offhandFingerprint) {
        List<HomeStorageInventorySlotSnapshot> slots = new ArrayList<>();
        List<HomeStorageStackSnapshot> occupied = new ArrayList<>();
        slots.add(HomeStorageInventorySlotSnapshot.occupied(
                HomeStorageStackLocation.MAIN, 0, SURPLUS, surplusCount
        ));
        occupied.add(stack(0, SURPLUS, surplusCount, selectedSlot == 0));
        if (fillPreviouslyEmptySlot) {
            slots.add(HomeStorageInventorySlotSnapshot.occupied(
                    HomeStorageStackLocation.MAIN, 1, INTRUDER, 1
            ));
            occupied.add(stack(1, INTRUDER, 1, false));
        } else {
            slots.add(HomeStorageInventorySlotSnapshot.empty(
                    HomeStorageStackLocation.MAIN, 1
            ));
        }
        slots.add(HomeStorageInventorySlotSnapshot.occupied(
                HomeStorageStackLocation.MAIN, 2, keptFingerprint, 1
        ));
        occupied.add(stack(2, keptFingerprint, 1, selectedSlot == 2));
        slots.add(HomeStorageInventorySlotSnapshot.occupied(
                HomeStorageStackLocation.ARMOR, 0, armorFingerprint, 1
        ));
        occupied.add(stack(
                HomeStorageStackLocation.ARMOR, 0, armorFingerprint, 1, false
        ));
        slots.add(HomeStorageInventorySlotSnapshot.occupied(
                HomeStorageStackLocation.OFFHAND, 0, offhandFingerprint, 1
        ));
        occupied.add(stack(
                HomeStorageStackLocation.OFFHAND, 0, offhandFingerprint, 1, false
        ));
        return new HomeStorageInventorySnapshot(slots, occupied, selectedSlot);
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

    private record Fixture(
            HomeStorageActivationBaseline baseline,
            HomeStorageManifestStep step,
            HomeStorageManifestProgress progress) {
    }
}
