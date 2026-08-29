package lavi.minecraft.task.container.home.planning;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

//20260828_kpopmodder: Bind planner input and the full activation baseline to one immutable capture.
public record HomeStorageInventorySnapshot(
        List<HomeStorageInventorySlotSnapshot> slots,
        List<HomeStorageStackSnapshot> occupiedStacks,
        int selectedMainSlot) {

    public HomeStorageInventorySnapshot {
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        occupiedStacks = List.copyOf(Objects.requireNonNull(
                occupiedStacks, "occupiedStacks"
        ));
        Set<String> slotKeys = new HashSet<>();
        for (HomeStorageInventorySlotSnapshot slot : slots) {
            if (!slotKeys.add(key(slot.location(), slot.logicalSlot()))) {
                throw new IllegalArgumentException("duplicate inventory slot snapshot");
            }
        }
        Set<String> occupiedKeys = new HashSet<>();
        for (HomeStorageStackSnapshot stack : occupiedStacks) {
            String key = key(stack.location(), stack.logicalSlot());
            if (!occupiedKeys.add(key)) {
                throw new IllegalArgumentException("duplicate occupied stack snapshot");
            }
            HomeStorageInventorySlotSnapshot slot = findSlot(
                    slots, stack.location(), stack.logicalSlot()
            ).orElseThrow(() -> new IllegalArgumentException(
                    "occupied stack has no matching full-slot snapshot"
            ));
            if (!slot.occupied()
                    || slot.count() != stack.count()
                    || !slot.fingerprint().orElseThrow().equals(stack.fingerprint())) {
                throw new IllegalArgumentException(
                        "occupied stack does not match its full-slot snapshot"
                );
            }
            boolean selected = stack.location() == HomeStorageStackLocation.MAIN
                    && stack.logicalSlot() == selectedMainSlot;
            if (stack.selectedMainHand() != selected) {
                throw new IllegalArgumentException(
                        "planner facts must preserve the selected main slot"
                );
            }
        }
        long occupiedSlotCount = slots.stream()
                .filter(HomeStorageInventorySlotSnapshot::occupied)
                .count();
        if (occupiedSlotCount != occupiedStacks.size()) {
            throw new IllegalArgumentException(
                    "every occupied full-slot snapshot must have planner facts"
            );
        }
        if (selectedMainSlot < 0
                || findSlot(slots, HomeStorageStackLocation.MAIN, selectedMainSlot)
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "selectedMainSlot must identify a captured main inventory slot"
            );
        }
    }

    public Optional<HomeStorageInventorySlotSnapshot> slot(
            HomeStorageStackLocation location,
            int logicalSlot) {
        return findSlot(slots, location, logicalSlot);
    }

    private static Optional<HomeStorageInventorySlotSnapshot> findSlot(
            List<HomeStorageInventorySlotSnapshot> slots,
            HomeStorageStackLocation location,
            int logicalSlot) {
        return slots.stream()
                .filter(slot -> slot.location() == location)
                .filter(slot -> slot.logicalSlot() == logicalSlot)
                .findFirst();
    }

    private static String key(HomeStorageStackLocation location, int logicalSlot) {
        return location.name() + ':' + logicalSlot;
    }
}
