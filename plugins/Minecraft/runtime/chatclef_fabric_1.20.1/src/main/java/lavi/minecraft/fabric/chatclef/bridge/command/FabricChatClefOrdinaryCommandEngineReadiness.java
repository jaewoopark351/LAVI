package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Own only the AltoClef readiness check for ordinary command dispatch.

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.CommandExecutor;

public final class FabricChatClefOrdinaryCommandEngineReadiness {
    public boolean isReady() {
        AltoClef mod = AltoClef.getInstance();
        return mod != null
                && AltoClef.inGame()
                && commandExecutor() != null
                && mod.getModSettings() != null;
    }

    public CommandExecutor commandExecutor() {
        return AltoClef.getCommandExecutor();
    }
}
