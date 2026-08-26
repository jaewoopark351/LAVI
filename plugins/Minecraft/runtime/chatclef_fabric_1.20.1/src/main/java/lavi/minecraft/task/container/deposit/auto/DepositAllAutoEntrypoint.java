package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyCompositionRoot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Objects;
import java.util.function.Supplier;

//20260826_kpopmodder: Register and drive the LAVI-owned automatic deposit_all chain after AltoClef initialization.
public final class DepositAllAutoEntrypoint implements ModInitializer {
    private final Supplier<AutoDepositPolicyEngine> policyEngineFactory;
    private DepositAllInventoryPressureChain chain;

    public DepositAllAutoEntrypoint() {
        this(() -> new AutoDepositPolicyCompositionRoot().createEngine());
    }

    DepositAllAutoEntrypoint(Supplier<AutoDepositPolicyEngine> policyEngineFactory) {
        this.policyEngineFactory = Objects.requireNonNull(policyEngineFactory, "policyEngineFactory");
    }

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
        if (chain != null) {
            return;
        }
        AltoClef mod = AltoClef.getInstance();
        TaskRunner runner = mod == null ? null : mod.getTaskRunner();
        if (runner == null) {
            return;
        }

        chain = new DepositAllInventoryPressureChain(runner, policyEngineFactory.get());
        DepositAllAutoDiagnostics.logRegistered(DepositAllInventoryPressureChain.PRIORITY);
    }
}
