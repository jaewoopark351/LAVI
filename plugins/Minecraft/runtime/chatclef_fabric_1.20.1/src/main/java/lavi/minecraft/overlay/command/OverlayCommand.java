package lavi.minecraft.overlay.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.overlay.visibility.OverlayVisibilityController;

//20260731_kpopmodder: Add a user command that toggles only existing HUD and Baritone render visibility.
public final class OverlayCommand extends Command {
    private final OverlayVisibilityController visibilityController = new OverlayVisibilityController();

    public OverlayCommand() throws CommandException {
        super("overlay", "Turns the built-in ChatClef and Baritone overlays on or off.",
                new Arg<>(OverlayToggleState.class, "onOrOff"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        OverlayToggleState toggle = parser.get(OverlayToggleState.class);
        switch (toggle) {
            case ON -> visibilityController.setVisible(mod, true);
            case OFF -> visibilityController.setVisible(mod, false);
        }
        finish();
    }
}
