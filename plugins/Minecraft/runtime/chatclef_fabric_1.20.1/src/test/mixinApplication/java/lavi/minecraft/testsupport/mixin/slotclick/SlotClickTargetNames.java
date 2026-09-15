package lavi.minecraft.testsupport.mixin.slotclick;

//20260916_kpopmodder: Keep the selected 1.20.1 namespace explicit for real-target transformation checks.
record SlotClickTargetNames(String owner, String outer, String inner, String action, String player) {
    static SlotClickTargetNames forArtifact(boolean intermediary) {
        return intermediary
                ? new SlotClickTargetNames("net/minecraft/class_1703", "method_7593", "method_30010",
                        "net/minecraft/class_1713", "net/minecraft/class_1657")
                : new SlotClickTargetNames("net/minecraft/screen/ScreenHandler", "onSlotClick", "internalOnSlotClick",
                        "net/minecraft/screen/slot/SlotActionType", "net/minecraft/entity/player/PlayerEntity");
    }

    String targetName() { return owner.replace('/', '.'); }
    String descriptor() { return "(IIL" + action + ";L" + player + ";)V"; }
    String redirectDescriptor() { return "(L" + owner + ";IIL" + action + ";L" + player + ";)V"; }
    String reference(String method) { return "L" + owner + ";" + method + descriptor(); }
}
