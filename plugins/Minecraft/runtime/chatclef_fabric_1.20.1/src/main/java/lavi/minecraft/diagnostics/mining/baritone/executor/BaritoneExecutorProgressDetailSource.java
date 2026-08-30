package lavi.minecraft.diagnostics.mining.baritone.executor;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

//20260830_kpopmodder: Retain only passive references needed for permit-gated executor detail capture.
final class BaritoneExecutorProgressDetailSource {
    final PathingBehavior behavior;
    final PathExecutor current;
    final PathExecutor next;
    final AbstractNodeCostSearch inProgress;
    final Goal activeGoal;
    final BetterBlockPos expectedSegmentStart;
    final MinecraftClient client;
    final ClientPlayerEntity player;

    BaritoneExecutorProgressDetailSource(PathingBehavior behavior,
                                         PathExecutor current,
                                         PathExecutor next,
                                         AbstractNodeCostSearch inProgress,
                                         Goal activeGoal,
                                         BetterBlockPos expectedSegmentStart,
                                         MinecraftClient client,
                                         ClientPlayerEntity player) {
        this.behavior = behavior;
        this.current = current;
        this.next = next;
        this.inProgress = inProgress;
        this.activeGoal = activeGoal;
        this.expectedSegmentStart = expectedSegmentStart;
        this.client = client;
        this.player = player;
    }
}
