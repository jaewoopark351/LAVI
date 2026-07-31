package lavi.minecraft.overlay.command;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.CommandExecutor;

//20260731_kpopmodder: Register the overlay command from a LAVI-owned tick boundary after AltoClef is ready.
public final class OverlayCommandRegistrar {
    private static final String COMMAND_NAME = "overlay";

    private boolean commandRegistered;
    private boolean failureLogged;

    public void onEndClientTick() {
        if (commandRegistered) {
            return;
        }
        try {
            CommandExecutor executor = AltoClef.getCommandExecutor();
            if (executor == null) {
                return;
            }
            if (executor.get(COMMAND_NAME) == null) {
                executor.registerNewCommand(new OverlayCommand());
            }
            commandRegistered = true;
        } catch (CommandException | RuntimeException | LinkageError e) {
            if (!failureLogged) {
                failureLogged = true;
                Debug.logWarning("LAVI overlay command registration failed: " + e.getClass().getSimpleName());
            }
        }
    }
}
