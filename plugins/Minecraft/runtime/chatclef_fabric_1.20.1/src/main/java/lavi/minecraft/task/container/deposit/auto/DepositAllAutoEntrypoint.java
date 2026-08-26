package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskRunner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260826_kpopmodder: Register and drive the LAVI-owned automatic deposit_all chain after AltoClef initialization.
public final class DepositAllAutoEntrypoint implements ModInitializer {
    private DepositAllInventoryPressureChain chain;

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onEndClientTick());
    }

    private void onEndClientTick() {
        if (chain == null) {
            registerIfReady();
        }
        if (chain == null) {
            return;
        }
        chain.onEndClientTick();
    }

    private void registerIfReady() {
        AltoClef mod = AltoClef.getInstance();
        TaskRunner runner = mod == null ? null : mod.getTaskRunner();
        if (runner == null) {
            return;
        }

        chain = new DepositAllInventoryPressureChain(runner);
        DepositAllAutoDiagnostics.logRegistered(DepositAllInventoryPressureChain.PRIORITY);
    }
}
