package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.multiversion.OptionsVer;
import net.minecraft.client.MinecraftClient;

public class SetGammaCommand extends Command {

    public SetGammaCommand() throws CommandException {
        super("gamma", "Sets the brightness to a value", new Arg<>(Double.class, "gamma", 1.0, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        double gammaValue = parser.get(Double.class);
        changeGamma(gammaValue);
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        //20260915_kpopmodder: The synchronous setter returned; complete this command's existing callback contract.
        // Exact inverse and source evidence: docs/chatclef-all-commands-korean-implementation-2026-09-15.md.
        finish();
    }

    public static void changeGamma(double value) {
        Debug.logMessage("Gamma set to " + value);

        OptionsVer.setGamma(value);
    }

}
