//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.tracking;

import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;

//20260913_kpopmodder: Native goal observation is provisional until the existing natural-completion event.
public final class GotoNativeCompletionTracker {
    private final GotoTerminalState result = new GotoTerminalState();
    private boolean matchingGoalObserved;

    public void observeGoal(boolean matchingGoal) { matchingGoalObserved = matchingGoal; }

    public void complete(boolean taskStopped, boolean stopStateAvailable, boolean worldMatches,
                         boolean rootReleased, String terminalDimension) {
        if (!stopStateAvailable || taskStopped || !matchingGoalObserved || !worldMatches || !rootReleased) return;
        result.commit(new GotoTerminalSnapshot("ARRIVED", "NONE", true, true, true,
                "legacy_get_to_block_terminal", terminalDimension));
    }

    public GotoTerminalSnapshot snapshot() { return result.snapshot(); }
}
//#endif
