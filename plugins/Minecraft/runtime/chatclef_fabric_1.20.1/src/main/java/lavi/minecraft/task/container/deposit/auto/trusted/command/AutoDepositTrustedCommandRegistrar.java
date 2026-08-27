package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStatusInspector;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;

import java.util.Objects;

//20260827_kpopmodder: Register LAVI-owned trusted commands without modifying the upstream command list.
public final class AutoDepositTrustedCommandRegistrar {
    public static final String TRUST_COMMAND = "auto_deposit_trust";
    public static final String UNTRUST_COMMAND = "auto_deposit_untrust";
    public static final String LIST_COMMAND = "auto_deposit_trusted_list";

    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedTargetResolver targetResolver;
    private final AutoDepositTrustedDestinationStatusInspector statusInspector;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private boolean registrationAttempted;
    private boolean registered;

    public AutoDepositTrustedCommandRegistrar(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositOpenContainerBindingTracker bindingTracker) {
        this.repository = Objects.requireNonNull(repository, "repository");
        targetResolver = new AutoDepositTrustedTargetResolver(
                Objects.requireNonNull(bindingTracker, "bindingTracker")
        );
        statusInspector = new AutoDepositTrustedDestinationStatusInspector();
        worldKeyReader = new AutoDepositWorldKeyReader();
    }

    public void register(AltoClef mod) {
        if (registered || registrationAttempted) {
            return;
        }
        if (mod == null || mod.getCommandExecutor() == null) {
            return;
        }
        registrationAttempted = true;
        if (mod.getCommandExecutor().get(TRUST_COMMAND) != null
                || mod.getCommandExecutor().get(UNTRUST_COMMAND) != null
                || mod.getCommandExecutor().get(LIST_COMMAND) != null) {
            Debug.logError("Trusted automatic-deposit command name already registered; registration refused.");
            return;
        }
        try {
            mod.getCommandExecutor().registerNewCommand(
                    new AutoDepositTrustCommand(repository, targetResolver, worldKeyReader),
                    new AutoDepositUntrustCommand(repository, targetResolver, worldKeyReader),
                    new AutoDepositTrustedListCommand(repository, statusInspector, worldKeyReader)
            );
            registered = true;
        } catch (CommandException exception) {
            Debug.logError("Failed to register trusted automatic-deposit commands: "
                    + exception.getMessage());
        }
    }

    public boolean registered() {
        return registered;
    }
}
