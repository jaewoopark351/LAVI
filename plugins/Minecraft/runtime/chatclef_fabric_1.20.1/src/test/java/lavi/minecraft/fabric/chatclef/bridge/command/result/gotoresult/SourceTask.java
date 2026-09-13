//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;
import lavi.minecraft.task.movement.gotoresult.model.GotoTaskResultSource;
import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;

//20260913_kpopmodder: A terminal-source double rejects any accidental engine or goal invocation.
final class SourceTask extends Task implements GotoTaskResultSource {
    final GotoTargetSnapshot target;
    GotoTerminalSnapshot terminal;
    boolean wasStopped;
    boolean stopReadFails;
    SourceTask(GotoTargetSnapshot target, GotoTerminalSnapshot terminal) { this.target = target; this.terminal = terminal; }
    @Override public GotoTargetSnapshot gotoTarget() { return target; }
    @Override public GotoTerminalSnapshot gotoTerminal() { return terminal; }
    @Override public boolean isFinished() { throw new AssertionError("Bridge must never evaluate goals"); }
    @Override public boolean stopped() { if (stopReadFails) throw new IllegalStateException("unavailable"); return wasStopped; }
    @Override protected void onStart() { }
    @Override protected Task onTick() { throw new AssertionError("Bridge must never tick navigation"); }
    @Override protected void onStop(Task task) { throw new AssertionError("Bridge must never stop navigation"); }
    @Override protected boolean isEqual(Task task) { return this == task; }
    @Override protected String toDebugString() { return "GOTO source double"; }
}
//#endif
