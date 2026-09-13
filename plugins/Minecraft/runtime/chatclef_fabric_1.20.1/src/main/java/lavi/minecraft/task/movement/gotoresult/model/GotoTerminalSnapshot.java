//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.model;

//20260913_kpopmodder: Freeze the existing owner's decision, never re-evaluate it during result sending.
public record GotoTerminalSnapshot(
        String outcome, String failureReason, boolean goalSatisfied, boolean bindingValid,
        boolean childrenQuiescent, String evidenceKind, String terminalDimension
) {
}
//#endif
