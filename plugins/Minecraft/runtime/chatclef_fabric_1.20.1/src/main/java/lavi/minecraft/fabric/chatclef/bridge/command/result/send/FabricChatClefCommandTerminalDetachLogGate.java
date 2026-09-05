package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Gate only the one-time terminal-send detach diagnostic.

public final class FabricChatClefCommandTerminalDetachLogGate {
    private boolean detachDeferLogged;

    public boolean markLogged() {
        if (detachDeferLogged) {
            return false;
        }
        detachDeferLogged = true;
        return true;
    }
}
