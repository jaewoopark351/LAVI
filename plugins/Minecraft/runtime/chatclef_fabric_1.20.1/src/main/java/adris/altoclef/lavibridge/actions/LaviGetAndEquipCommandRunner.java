package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this runner to equip an item after AltoClef finishes collecting it.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.commands.LaviCommandSpec;

import java.util.concurrent.atomic.AtomicBoolean;

public class LaviGetAndEquipCommandRunner {

    private final AltoClef mod;
    private final LaviEquipItemRunner equipItemRunner;

    public LaviGetAndEquipCommandRunner(AltoClef mod) {
        this(mod, new LaviEquipItemRunner(mod));
    }

    public LaviGetAndEquipCommandRunner(
            AltoClef mod,
            LaviEquipItemRunner equipItemRunner
    ) {
        this.mod = mod;
        this.equipItemRunner = equipItemRunner;
    }

    public void run(LaviCommandSpec commandSpec, String actionId, LaviCommandActionLifecycle lifecycle) {
        AtomicBoolean failed = new AtomicBoolean(false);
        String commandLine = mod.getCommandExecutor().getCommandPrefix() + commandSpec.getCommand();

        mod.getCommandExecutor().execute(commandLine, () -> {
            if (failed.get()) {
                return;
            }
            try {
                lifecycle.succeeded(actionId, equipItemRunner.equip(commandSpec));
            } catch (RuntimeException exception) {
                lifecycle.failed(actionId, exception.getMessage());
            }
        }, exception -> {
            failed.set(true);
            lifecycle.failed(actionId, exception.getMessage());
        });
    }
}
