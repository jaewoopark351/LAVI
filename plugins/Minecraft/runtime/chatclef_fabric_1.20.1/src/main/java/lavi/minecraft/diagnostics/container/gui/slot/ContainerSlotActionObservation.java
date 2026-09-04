package lavi.minecraft.diagnostics.container.gui.slot;

//20260904_kpopmodder: Freeze one physical controller slot-request boundary for diagnostics.
public record ContainerSlotActionObservation(
        String handlerIdentity,
        String handlerClass,
        int syncId,
        int windowSlot,
        int slotButton,
        String slotActionType,
        ContainerItemCountSnapshot before,
        long requestGameTick) {
}
