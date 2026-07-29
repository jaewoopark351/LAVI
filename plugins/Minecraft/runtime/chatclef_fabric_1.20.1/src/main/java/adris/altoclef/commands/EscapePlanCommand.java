package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.escape.EscapePlan;
import adris.altoclef.tasks.movement.escape.EscapePlanSearchResult;
import adris.altoclef.tasks.movement.escape.EscapeRetryCooldowns;
import adris.altoclef.tasks.movement.escape.LocalTerrainEscapeTask;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Set;

//20260729_kpopmodder: Added a dry-run command to inspect local terrain escape planning without executing block actions.
public class EscapePlanCommand extends Command {
    private static final int MAX_TEST_CLEAR_BLOCKS = 8;
    private static final int TEST_COOLDOWN_TICKS = 20 * 8;
    private static final String AUTO_MODE = "auto";
    private static final String RUN_MODE = "run";
    private static final String SIDE_MODE = "side";
    private static final String SPIRAL_MODE = "spiral";
    private static final String HEADROOM_MODE = "headroom";
    private static final String COOLDOWN_MODE = "cooldown";
    private static final String CONFIRM_TOKEN = "confirm";

    public EscapePlanCommand() {
        super("escapeplan", "Dry-runs local terrain escape planning. Use 'escapeplan side', 'escapeplan headroom', 'escapeplan spiral', 'escapeplan cooldown confirm', or 'escapeplan run confirm'.");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        String[] args = parser.getArgUnits();
        if (isDryRun(args)) {
            runDryRun(mod);
            finish();
            return;
        }
        if (isSpiralDryRun(args)) {
            runSpiralDryRun(mod);
            finish();
            return;
        }
        if (isCandidateDryRun(args)) {
            runCandidateDryRun(mod, normalizeCandidateMode(args[0]));
            finish();
            return;
        }
        if (isCooldownConfirmed(args)) {
            runCooldownConfirmedTest(mod);
            finish();
            return;
        }
        if (isCandidateRunConfirmed(args)) {
            runConfirmedTest(mod, normalizeCandidateMode(args[0]));
            return;
        }
        if (!isRunConfirmed(args)) {
            Debug.logWarning("[EscapePlanCommand] rejected: args=" + describeArgs(args)
                    + ", usage=@escapeplan OR @escapeplan side OR @escapeplan headroom OR @escapeplan spiral OR @escapeplan cooldown confirm OR @escapeplan run confirm OR @escapeplan side run confirm OR @escapeplan headroom run confirm, action=none");
            Debug.logMessage("Usage: @escapeplan OR @escapeplan side OR @escapeplan headroom OR @escapeplan spiral OR @escapeplan cooldown confirm OR @escapeplan run confirm OR @escapeplan side run confirm OR @escapeplan headroom run confirm. No action executed.");
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

    //20260729_kpopmodder: Spiral-only dry-run lets us validate the fallback candidate without changing block actions.
    private void runSpiralDryRun(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] spiral dry-run skipped: player or world unavailable; action=none");
            Debug.logMessage("Escape plan spiral dry-run skipped: player or world unavailable.");
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        Debug.logWarning("[EscapePlanCommand] spiral dry-run requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", action=none");

        EscapePlanSearchResult planSearch = LocalTerrainEscapeTask.searchSpiralPlan(mod, Collections.emptySet());
        if (planSearch.getPlan().isPresent()) {
            String detail = planSearch.getPlan().get().describe();
            Debug.logWarning("[EscapePlanCommand] spiral dry-run plan selected: " + detail + ", action=none");
            Debug.logMessage("Escape plan spiral dry-run selected: " + detail + ". No action executed.");
        } else {
            String reason = planSearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] spiral dry-run plan unavailable: " + reason + ", action=none");
            Debug.logMessage("Escape plan spiral dry-run unavailable: " + reason + ". No action executed.");
        }
    }

    //20260729_kpopmodder: Candidate-specific dry-runs let side/headroom success cases be verified without changing escape ordering.
    private void runCandidateDryRun(AltoClef mod, String planMode) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] " + planMode + " dry-run skipped: player or world unavailable; action=none");
            Debug.logMessage("Escape plan " + planMode + " dry-run skipped: player or world unavailable.");
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        Debug.logWarning("[EscapePlanCommand] " + planMode + " dry-run requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", action=none");

        EscapePlanSearchResult planSearch = searchPlanForMode(mod, planMode);
        if (planSearch.getPlan().isPresent()) {
            String detail = planSearch.getPlan().get().describe();
            Debug.logWarning("[EscapePlanCommand] " + planMode + " dry-run plan selected: " + detail + ", action=none");
            Debug.logMessage("Escape plan " + planMode + " dry-run selected: " + detail + ". No action executed.");
        } else {
            String reason = planSearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] " + planMode + " dry-run plan unavailable: " + reason + ", action=none");
            Debug.logMessage("Escape plan " + planMode + " dry-run unavailable: " + reason + ". No action executed.");
        }
    }

    //20260729_kpopmodder: Dry-run candidate cooldown without executing LocalTerrainEscapeTask block actions.
    private void runCooldownConfirmedTest(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] cooldown test skipped: player or world unavailable; action=none");
            Debug.logMessage("Escape plan cooldown test skipped: player or world unavailable.");
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        EscapeRetryCooldowns cooldowns = new EscapeRetryCooldowns(TEST_COOLDOWN_TICKS);
        StateChangeLogger cooldownDebugLogger = new StateChangeLogger("EscapePlanCommandCooldownTest");
        Debug.logWarning("[EscapePlanCommand] cooldown test requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", cooldownTicks=" + TEST_COOLDOWN_TICKS
                + ", action=none");

        EscapePlanSearchResult firstSearch = LocalTerrainEscapeTask.searchPlan(mod,
                Collections.emptySet(),
                Set.of(),
                cooldownDebugLogger);
        if (firstSearch.getPlan().isEmpty()) {
            String reason = firstSearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] cooldown test rejected: first plan unavailable, reason="
                    + reason
                    + ", action=none");
            Debug.logMessage("Escape plan cooldown test unavailable: " + reason + ". No action executed.");
            return;
        }

        EscapePlan firstPlan = firstSearch.getPlan().get();
        cooldowns.rememberFailure(firstPlan);
        Debug.logWarning("[EscapePlanCommand] cooldown test first plan selected: "
                + firstPlan.describe()
                + ", action=none");
        Debug.logWarning("[EscapePlanCommand] cooldown test remembered candidate: "
                + firstPlan.getCandidateKey().describe()
                + ", cooldowns=" + cooldowns.describe()
                + ", action=none");

        EscapePlanSearchResult retrySearch = LocalTerrainEscapeTask.searchPlan(mod,
                cooldowns.activeOrigins(),
                cooldowns.activeCandidates(),
                cooldownDebugLogger);
        if (retrySearch.getPlan().isPresent()) {
            EscapePlan retryPlan = retrySearch.getPlan().get();
            boolean sameCandidate = firstPlan.getCandidateKey().equals(retryPlan.getCandidateKey());
            String detail = retryPlan.describe();
            Debug.logWarning("[EscapePlanCommand] cooldown test retry selected: "
                    + detail
                    + ", sameCandidateAsFirst=" + sameCandidate
                    + ", cooldowns=" + cooldowns.describe()
                    + ", action=none");
            Debug.logMessage("Escape plan cooldown test selected retry plan: " + detail
                    + ". sameCandidateAsFirst=" + sameCandidate + ". No action executed.");
        } else {
            String reason = retrySearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] cooldown test retry unavailable: reason="
                    + reason
                    + ", cooldowns=" + cooldowns.describe()
                    + ", action=none");
            Debug.logMessage("Escape plan cooldown test retry unavailable: " + reason + ". No action executed.");
        }
    }

    private void runConfirmedTest(AltoClef mod) {
        runConfirmedTest(mod, AUTO_MODE);
    }

    private void runConfirmedTest(AltoClef mod, String planMode) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] TEST run skipped: player or world unavailable, planMode=" + planMode + ", action=none");
            Debug.logMessage("Escape plan TEST run skipped: player or world unavailable.");
            finish();
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        Debug.logWarning("[EscapePlanCommand] TEST run requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", planMode=" + planMode
                + ", maxClearBlocks=" + MAX_TEST_CLEAR_BLOCKS
                + ", action=run-confirmed");

        EscapePlanSearchResult planSearch = searchPlanForMode(mod, planMode);
        if (planSearch.getPlan().isEmpty()) {
            String reason = planSearch.describeFailure();
            Debug.logWarning("[EscapePlanCommand] TEST run rejected: no plan available, planMode=" + planMode
                    + ", reason=" + reason + ", action=none");
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
                + ", planMode=" + planMode
                + ", clearBlockCount=" + selectedPlan.getClearBlockCount()
                + ", action=run-confirmed");
        Debug.logMessage("Escape plan TEST run started: " + selectedPlan.describe() + ". It may break listed blocks.");
        mod.runUserTask(new LocalTerrainEscapeTask(selectedPlan, "EscapePlanCommand TEST " + planMode + " run confirm"), () -> {
            Debug.logWarning("[EscapePlanCommand] TEST run finished: " + selectedPlan.describe()
                    + ", planMode=" + planMode
                    + ", action=run-confirmed");
            Debug.logMessage("Escape plan TEST run finished: " + selectedPlan.describe());
            finish();
        });
    }

    private boolean isDryRun(String[] args) {
        return args == null || args.length == 0;
    }

    private boolean isSpiralDryRun(String[] args) {
        return args != null
                && args.length == 1
                && SPIRAL_MODE.equalsIgnoreCase(args[0]);
    }

    private boolean isCandidateDryRun(String[] args) {
        return args != null
                && args.length == 1
                && isCandidateMode(args[0]);
    }

    private boolean isCooldownConfirmed(String[] args) {
        return args != null
                && args.length == 2
                && COOLDOWN_MODE.equalsIgnoreCase(args[0])
                && CONFIRM_TOKEN.equalsIgnoreCase(args[1]);
    }

    private boolean isCandidateRunConfirmed(String[] args) {
        return args != null
                && args.length == 3
                && isCandidateMode(args[0])
                && RUN_MODE.equalsIgnoreCase(args[1])
                && CONFIRM_TOKEN.equalsIgnoreCase(args[2]);
    }

    private boolean isRunConfirmed(String[] args) {
        return args != null
                && args.length == 2
                && RUN_MODE.equalsIgnoreCase(args[0])
                && CONFIRM_TOKEN.equalsIgnoreCase(args[1]);
    }

    private boolean isCandidateMode(String mode) {
        return SIDE_MODE.equalsIgnoreCase(mode)
                || HEADROOM_MODE.equalsIgnoreCase(mode);
    }

    private String normalizeCandidateMode(String mode) {
        if (SIDE_MODE.equalsIgnoreCase(mode)) {
            return SIDE_MODE;
        }
        if (HEADROOM_MODE.equalsIgnoreCase(mode)) {
            return HEADROOM_MODE;
        }
        return AUTO_MODE;
    }

    private EscapePlanSearchResult searchPlanForMode(AltoClef mod, String planMode) {
        if (SIDE_MODE.equals(planMode)) {
            return LocalTerrainEscapeTask.searchSidePlan(mod, Collections.emptySet());
        }
        if (HEADROOM_MODE.equals(planMode)) {
            return LocalTerrainEscapeTask.searchHeadroomPlan(mod, Collections.emptySet());
        }
        return LocalTerrainEscapeTask.searchPlan(mod, Collections.emptySet());
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
