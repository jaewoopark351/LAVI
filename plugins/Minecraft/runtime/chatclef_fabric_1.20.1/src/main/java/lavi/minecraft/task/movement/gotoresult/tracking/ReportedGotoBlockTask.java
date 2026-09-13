//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.tracking;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.GotoTarget;
import adris.altoclef.tasks.movement.GetToBlockTask;
import lavi.minecraft.task.movement.gotoresult.binding.GotoTaskBinding;
import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;
import lavi.minecraft.task.movement.gotoresult.model.GotoTaskResultSource;
import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;
import net.minecraft.util.math.BlockPos;

//20260913_kpopmodder: Participate in the native Task contract without changing its goal, return value or cleanup.
public final class ReportedGotoBlockTask extends GetToBlockTask implements GotoTaskResultSource {
    private final GotoTaskBinding binding;
    private final GotoNativeCompletionTracker result = new GotoNativeCompletionTracker();

    public ReportedGotoBlockTask(AltoClef mod, GotoTarget request) {
        super(new BlockPos(request.getX(), request.getY(), request.getZ()), request.getDimension());
        binding = new GotoTaskBinding(mod, request);
    }

    @Override
    public boolean isFinished() {
        boolean finished = super.isFinished();
        result.observeGoal(finished && !stopped() && binding.rootMatches(AltoClef.getInstance(), this));
        return finished;
    }

    /** The existing user-chain event is published after its root release and path/input cleanup. */
    public void observeNaturalCompletion(boolean taskStopped, boolean stopStateAvailable) {
        AltoClef mod = AltoClef.getInstance();
        result.complete(taskStopped, stopStateAvailable, binding.worldMatches(mod),
                mod != null && mod.getUserTaskChain().getCurrentTask() == null, GotoTaskBinding.dimension(mod));
    }

    @Override public GotoTargetSnapshot gotoTarget() { return binding.target(); }
    @Override public GotoTerminalSnapshot gotoTerminal() { return result.snapshot(); }
}
//#endif
