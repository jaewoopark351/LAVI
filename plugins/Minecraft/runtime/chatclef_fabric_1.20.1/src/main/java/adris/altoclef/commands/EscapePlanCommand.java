package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.escape.EscapePlan;
import adris.altoclef.tasks.movement.escape.LocalTerrainEscapeTask;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Optional;

//20260729_kpopmodder: Added a dry-run command to inspect local terrain escape planning without executing block actions.
public class EscapePlanCommand extends Command {

    public EscapePlanCommand() {
        super("escapeplan", "Dry-runs local terrain escape planning without moving, breaking, or placing blocks.");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            Debug.logWarning("[EscapePlanCommand] dry-run skipped: player or world unavailable; action=none");
            Debug.logMessage("Escape plan dry-run skipped: player or world unavailable.");
            finish();
            return;
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        Debug.logWarning("[EscapePlanCommand] dry-run requested: origin=" + origin.toShortString()
                + ", facing=" + mod.getPlayer().getHorizontalFacing().getName()
                + ", action=none");

        Optional<EscapePlan> plan = LocalTerrainEscapeTask.findPlan(mod, Collections.emptySet());
        if (plan.isPresent()) {
            String detail = plan.get().describe();
            Debug.logWarning("[EscapePlanCommand] dry-run plan selected: " + detail + ", action=none");
            Debug.logMessage("Escape plan dry-run selected: " + detail + ". No action executed.");
        } else {
            String reason = LocalTerrainEscapeTask.describePlanSearchFailure(mod, Collections.emptySet());
            Debug.logWarning("[EscapePlanCommand] dry-run plan unavailable: " + reason + ", action=none");
            Debug.logMessage("Escape plan dry-run unavailable: " + reason + ". No action executed.");
        }
        finish();
    }
}
