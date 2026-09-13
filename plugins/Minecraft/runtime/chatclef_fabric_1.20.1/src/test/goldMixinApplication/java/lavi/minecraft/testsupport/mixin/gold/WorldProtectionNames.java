package lavi.minecraft.testsupport.mixin.gold;

//20260914_kpopmodder: Exact 1.20.1 Yarn/intermediary names from the existing cached mappings.
public record WorldProtectionNames(String world, String blockPos, String blockState, String changedMethod) {
    public static WorldProtectionNames named() {
        return new WorldProtectionNames("net/minecraft/world/World", "net/minecraft/util/math/BlockPos",
                "net/minecraft/block/BlockState", "onBlockChanged");
    }
    public static WorldProtectionNames intermediary() {
        return new WorldProtectionNames("net/minecraft/class_1937", "net/minecraft/class_2338",
                "net/minecraft/class_2680", "method_19282");
    }
    public String targetName() { return world.replace('/', '.'); }
    public String changedDescriptor() { return "(L" + blockPos + ";L" + blockState + ";L" + blockState + ";)V"; }
    public String bridgeDescriptor() { return "(L" + world + ";L" + blockPos + ";L" + blockState + ";)V"; }
}
