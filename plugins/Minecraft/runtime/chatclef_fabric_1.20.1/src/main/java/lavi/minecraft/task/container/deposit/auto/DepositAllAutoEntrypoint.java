package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyCompositionRoot;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Objects;
import java.util.function.Function;

//20260826_kpopmodder: Register and drive the LAVI-owned automatic deposit_all chain after AltoClef initialization.
//20260827_kpopmodder: Disable that pressure chain while keeping the manual trusted-storage runtime alive.
public final class DepositAllAutoEntrypoint implements ModInitializer {
    private final Function<AltoClef, AutoDepositRuntime> runtimeFactory;
    private AutoDepositRuntime runtime;

    public DepositAllAutoEntrypoint() {
        this(mod -> new AutoDepositPolicyCompositionRoot().createRuntime(mod));
    }

    DepositAllAutoEntrypoint(Function<AltoClef, AutoDepositRuntime> runtimeFactory) {
        this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
    }

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onEndClientTick());
    }

    private void onEndClientTick() {
        if (runtime == null) {
            registerIfReady();
        }
        if (runtime != null) {
            runtime.onEndClientTick();
        }
    }

    private void registerIfReady() {
        if (runtime != null) {
            return;
        }
        AltoClef mod = AltoClef.getInstance();
        if (mod == null || mod.getTaskRunner() == null) {
            return;
        }

        AutoDepositRuntime createdRuntime = Objects.requireNonNull(
                runtimeFactory.apply(mod),
                "runtimeFactory returned null"
        );
        createdRuntime.registerCommands(mod);
        runtime = createdRuntime;
    }
}
