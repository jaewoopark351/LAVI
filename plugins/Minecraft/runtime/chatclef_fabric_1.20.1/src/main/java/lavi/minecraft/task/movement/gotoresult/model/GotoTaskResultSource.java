//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.model;

//20260913_kpopmodder: Read-only bridge contract for command-owned movement outcomes.
public interface GotoTaskResultSource {
    GotoTargetSnapshot gotoTarget();
    GotoTerminalSnapshot gotoTerminal();
}
//#endif
