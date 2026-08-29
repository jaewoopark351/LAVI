package lavi.minecraft.task.container.home.execution.slot;

import java.util.OptionalInt;

//20260829_kpopmodder: Added this type file to expose one diagnostic slot-match result.
public record HomeStorageScreenSlotInspection(
        int matchCount,
        OptionalInt resolvedWindowSlot) {
}
