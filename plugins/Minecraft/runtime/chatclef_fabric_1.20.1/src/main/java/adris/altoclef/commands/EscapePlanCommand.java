package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.escape.EscapePlan;
import adris.altoclef.tasks.movement.escape.EscapePlanSearchResult;
import adris.altoclef.tasks.movement.escape.LocalTerrainEscapeTask;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;

//20260729_kpopmodder: Added a dry-run command to inspect local terrain escape planning without executing block actions.
public class EscapePlanCommand extends Command {
    private static final int MAX_TEST_CLEAR_BLOCKS = 8;
    private static final String RUN_MODE = "run";
    private static final String CONFIRM_TOKEN = "confirm";

    public EscapePlanCommand() {
        super("escapeplan", "Dry-runs local terrain escape planning. Use 'escapeplan run confirm' for a guarded test run.");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        String[] args = parser.getArgUnits();
        if (isDryRun(args)) {
            runDryRun(mod);
            finish();
            return;
        }
        if (!isRunConfirmed(args)) {
            Debug.logWarning("[EscapePlanCommand] rejected: args=" + describeArgs(args)
                    + ", usage=@escapeplan OR @escapeplan run confirm, action=none");
            Debug.logMessage("Usage: @escapeplan OR @escapeplan run confirm. No action executed.");
            finish();
            return;
        }
        runConfirmedTest(mod);
    }

    private void runDryRun(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] dry-run skipped: player or world unavailable; action=none");
            Debug.logMessage("Escape plan dry-run skipped: player or world unavailable.");
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        Debug.logWarning("[EscapePlanCommand] dry-run requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", action=none");

        EscapePlanSearchResult planSearch = LocalTerrainEscapeTask.searchPlan(mod, Collections.emptySet());
        if (planSearch.getPlan().isPresent()) {
            String detail = planSearch.getPlan().get().describe();
            Debug.logWarning("[EscapePlanCommand] dry-run plan selected: " + detail + ", action=none");
            Debug.logMessage("Escape plan dry-run selected: " + detail + ". No action executed.");
        } else {
            String reason = planSearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] dry-run plan unavailable: " + reason + ", action=none");
            Debug.logMessage("Escape plan dry-run unavailable: " + reason + ". No action executed.");
        }
    }

    private void runConfirmedTest(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] TEST run skipped: player or world unavailable; action=none");
            Debug.logMessage("Escape plan TEST run skipped: player or world unavailable.");
            finish();
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        Debug.logWarning("[EscapePlanCommand] TEST run requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", maxClearBlocks=" + MAX_TEST_CLEAR_BLOCKS
                + ", action=run-confirmed");

        EscapePlanSearchResult planSearch = LocalTerrainEscapeTask.searchPlan(mod, Collections.emptySet());
        if (planSearch.getPlan().isEmpty()) {
            String reason = planSearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] TEST run rejected: no plan available, reason=" + reason + ", action=none");
            Debug.logMessage("Escape plan TEST run unavailable: " + reason + ". No action executed.");
            finish();
            return;
        }

        EscapePlan selectedPlan = planSearch.getPlan().get();
        String rejectionReason = getRunRejectionReason(mod, origin, selectedPlan);
        if (rejectionReason != null) {
            Debug.logWarning("[EscapePlanCommand] TEST run rejected: " + rejectionReason
                    + ", " + selectedPlan.describe()
                    + ", action=none");
            Debug.logMessage("Escape plan TEST run rejected: " + rejectionReason + ". No action executed.");
            finish();
            return;
        }

        Debug.logWarning("[EscapePlanCommand] TEST run starting LocalTerrainEscapeTask: "
                + selectedPlan.describe()
                + ", clearBlockCount=" + selectedPlan.getClearBlockCount()
                + ", action=run-confirmed");
        Debug.logMessage("Escape plan TEST run started: " + selectedPlan.describe() + ". It may break listed blocks.");
        mod.runUserTask(new LocalTerrainEscapeTask(selectedPlan, "EscapePlanCommand TEST run confirm"), () -> {
            Debug.logWarning("[EscapePlanCommand] TEST run finished: " + selectedPlan.describe()
                    + ", action=run-confirmed");
            Debug.logMessage("Escape plan TEST run finished: " + selectedPlan.describe());
            finish();
        });
    }

    private boolean isDryRun(String[] args) {
        return args == null || args.length == 0;
    }

    private boolean isRunConfirmed(String[] args) {
        return args != null
                && args.length == 2
                && RUN_MODE.equalsIgnoreCase(args[0])
                && CONFIRM_TOKEN.equalsIgnoreCase(args[1]);
    }

    private String getRunRejectionReason(AltoClef mod, BlockPos origin, EscapePlan plan) {
        if (!plan.getOrigin().equals(origin)) {
            return "player origin changed before run, expected=" + origin.toShortString()
                    + ", planOrigin=" + plan.getOrigin().toShortString();
        }
        if (!plan.hasBlocksToClear()) {
            return "plan has no blocks to clear";
        }
        if (plan.getClearBlockCount() > MAX_TEST_CLEAR_BLOCKS) {
            return "plan has too many blocks to clear, count=" + plan.getClearBlockCount()
                    + ", max=" + MAX_TEST_CLEAR_BLOCKS;
        }
        if (mod.getFoodChain().needsToEat()) {
            return "food chain needs to eat";
        }
        if (mod.getControllerExtras().isBreakingBlock()) {
            return "already breaking block";
        }
        return null;
    }

    private String describeArgs(String[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }
        return String.join(" ", args);
    }
}
