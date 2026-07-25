package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this runner to isolate AltoClef command executor callback wiring.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;

import java.util.concurrent.atomic.AtomicBoolean;

public class LaviAltoClefCommandRunner {

    private final AltoClef mod;

    public LaviAltoClefCommandRunner(AltoClef mod) {
        this.mod = mod;
    }

    public void run(LaviCommandSpec commandSpec, String actionId, LaviCommandActionLifecycle lifecycle) {
        AtomicBoolean failed = new AtomicBoolean(false);
        String commandLine = mod.getCommandExecutor().getCommandPrefix() + commandSpec.getCommand();

        mod.getCommandExecutor().execute(commandLine, () -> {
            if (!failed.get()) {
                lifecycle.succeeded(actionId, "AltoClef command finished.");
            }
        }, exception -> {
            failed.set(true);
            lifecycle.failed(actionId, exception.getMessage());
        });
    }
}
