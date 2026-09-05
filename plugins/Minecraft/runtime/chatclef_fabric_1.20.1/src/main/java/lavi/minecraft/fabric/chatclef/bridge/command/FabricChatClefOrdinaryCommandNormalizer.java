package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Normalize one ordinary ChatClef command against the active executor.

import adris.altoclef.commandsystem.CommandExecutor;

public final class FabricChatClefOrdinaryCommandNormalizer {
    public String normalize(CommandExecutor executor, String command) {
        String normalized = command == null ? "" : command.trim();
        if (executor.isClientCommand(normalized)) {
            return normalized;
        }
        return executor.getCommandPrefix() + normalized;
    }
}
