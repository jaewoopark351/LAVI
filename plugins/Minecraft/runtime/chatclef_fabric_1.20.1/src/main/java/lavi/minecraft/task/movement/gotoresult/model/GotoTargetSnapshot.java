//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.model;

//20260913_kpopmodder: Keep admission coordinates and dimension semantics immutable.
public record GotoTargetSnapshot(int x, int y, int z, String requestedDimension, String worldDimension) {
}
//#endif
