package adris.altoclef.tasks.construction.destroy;

import adris.altoclef.AltoClef;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260729_kpopmodder: Added this plan object so DestroyBlockTask can log planned decisions before using them.
public final class DestroyBlockPlan {
    private final BlockPos target;
    private final DestroyTargetValidator.TargetDecision targetDecision;
    private final DestroyToolSelector.ToolDecision toolDecision;
    private final DestroyReachPolicy.ReachDecision reachDecision;
    private final DestroyProgressTracker.ProgressSnapshot progressSnapshot;

    private DestroyBlockPlan(BlockPos target,
                             DestroyTargetValidator.TargetDecision targetDecision,
                             DestroyToolSelector.ToolDecision toolDecision,
                             DestroyReachPolicy.ReachDecision reachDecision,
                             DestroyProgressTracker.ProgressSnapshot progressSnapshot) {
        this.target = target;
        this.targetDecision = targetDecision;
        this.toolDecision = toolDecision;
        this.reachDecision = reachDecision;
        this.progressSnapshot = progressSnapshot;
    }

    public static DestroyBlockPlan observe(AltoClef mod, BlockPos target, boolean taskMining,
                                           boolean unstuckTaskActive) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(target, "target");

        DestroyTargetValidator.TargetDecision targetDecision = DestroyTargetValidator.evaluate(mod, target);
        DestroyToolSelector.ToolDecision toolDecision = DestroyToolSelector.select(mod, targetDecision);
        DestroyReachPolicy.ReachDecision reachDecision = DestroyReachPolicy.evaluate(mod, target, targetDecision);
        DestroyProgressTracker.ProgressSnapshot progressSnapshot = DestroyProgressTracker.capture(mod, target,
                taskMining, unstuckTaskActive);

        return new DestroyBlockPlan(target, targetDecision, toolDecision, reachDecision, progressSnapshot);
    }

    public void log(StateChangeLogger logger) {
        Objects.requireNonNull(logger, "logger");
        logger.state(stateKey(), describe());
    }

    private String stateKey() {
        return "target=" + target.toShortString()
                + "|targetDecision=" + targetDecision.reasonKey()
                + "|reach=" + reachDecision.stateKey()
                + "|tool=" + toolDecision.stateKey()
                + "|progress=" + progressSnapshot.stateKey();
    }

    private String describe() {
        return "observed destroy plan: targetDecision={" + targetDecision.describe()
                + "}, reachDecision={" + reachDecision.describe()
                + "}, toolDecision={" + toolDecision.describe()
                + "}, progress={" + progressSnapshot.describe()
                + "}";
    }
}
