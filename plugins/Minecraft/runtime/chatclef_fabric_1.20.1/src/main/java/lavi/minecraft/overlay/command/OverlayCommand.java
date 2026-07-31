package lavi.minecraft.overlay.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import baritone.api.Settings;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260731_kpopmodder: Add a user command that toggles only existing HUD and Baritone render visibility.
public final class OverlayCommand extends Command {
    public OverlayCommand() throws CommandException {
        super("overlay", "Turns the built-in ChatClef and Baritone overlays on or off.",
                new Arg<>(OverlayToggleState.class, "onOrOff"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        OverlayToggleState toggle = parser.get(OverlayToggleState.class);
        switch (toggle) {
            case ON -> setVisible(mod, true);
            case OFF -> setVisible(mod, false);
        }
        finish();
    }

    private void setVisible(AltoClef mod, boolean visible) {
        mod.setBuiltInHudVisible(visible);
        Settings baritoneSettings = mod.getClientBaritoneSettings();
        baritoneSettings.renderPath.value = visible;
        baritoneSettings.renderGoal.value = visible;
        ChatClefDiagnostics.setBoundaryEnabled(visible);
        log("LAVI overlay " + (visible ? "ON" : "OFF") + "; diagnostics=" + (visible ? "BOUNDARY" : "OFF"));
    }
}
