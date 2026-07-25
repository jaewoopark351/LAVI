package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added guard checks shared by LAVI Minecraft action executors.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.LaviActionRegistry;

public class LaviMinecraftActionGuard {

    private final AltoClef mod;
    private final LaviActionRegistry actionRegistry;

    public LaviMinecraftActionGuard(AltoClef mod, LaviActionRegistry actionRegistry) {
        this.mod = mod;
        this.actionRegistry = actionRegistry;
    }

    public void ensureInGame() {
        if (!AltoClef.inGame()) {
            throw new IllegalStateException("Minecraft player is not in game.");
        }
    }

    public void ensureNoRunningAction() {
        if (actionRegistry.hasRunningAction()) {
            throw new IllegalStateException("A LAVI action is already running.");
        }
    }

    public void ensureNoManualTask() {
        if (mod.getUserTaskChain().isActive() && !mod.getUserTaskChain().isRunningIdleTask()) {
            throw new IllegalStateException("An AltoClef user task is already active.");
        }
    }
}
