package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Invoke only the already-registered upstream StopCommand on the client tick.

import adris.altoclef.AltoClef;
import adris.altoclef.commands.StopCommand;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FabricChatClefRegisteredStopCommandExecutor implements FabricChatClefStopCommandExecutor {
    @Override
    public void executeRegisteredStop() throws Exception {
        AltoClef mod = AltoClef.getInstance();
        CommandExecutor executor = AltoClef.getCommandExecutor();
        if (mod == null || executor == null) {
            throw new IllegalStateException("ChatClef command runtime is unavailable");
        }
        Command registered = executor.get("stop");
        if (!(registered instanceof StopCommand) || !"stop".equals(registered.getName())) {
            throw new IllegalStateException("registered stop command is unavailable or was replaced");
        }
        AtomicBoolean finished = new AtomicBoolean(false);
        registered.run(mod, "stop", () -> finished.set(true));
        if (!finished.get()) {
            throw new IllegalStateException("registered stop command did not finish synchronously");
        }
    }
}
