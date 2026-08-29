package lavi.minecraft.task.container.home.execution.slot;

//20260829_kpopmodder: Added this type file to expose one handler-slot ownership view.
public record HomeStorageScreenSlotView(
        int windowSlot,
        boolean playerInventory,
        int logicalSlot) {
}
