package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;

//20260728_kpopmodder: Added a user command to hide/show ChatClef HUD and Baritone path visuals.
public class OverlayCommand extends Command {

    public OverlayCommand() throws CommandException {
        super("overlay", "Shows or hides the ChatClef HUD and Baritone path visuals.",
                new Arg<>(ToggleState.class, "onOrOff"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        ToggleState toggle = parser.get(ToggleState.class);
        boolean visible = toggle == ToggleState.ON;
        Debug.logWarning("[OverlayCommand] command accepted: showInGameOverlay=" + (visible ? "ON" : "OFF"));
        mod.setInGameOverlayVisible(visible);
        Debug.logMessage("In-game overlay is now " + (visible ? "ON" : "OFF") + ".");
        finish();
    }

    public enum ToggleState {
        ON,
        OFF
    }
}
