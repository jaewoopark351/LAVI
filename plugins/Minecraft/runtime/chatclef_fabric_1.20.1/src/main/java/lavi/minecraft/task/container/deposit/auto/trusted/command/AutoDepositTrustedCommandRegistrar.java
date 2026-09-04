package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStatusInspector;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationService;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;

import java.util.Objects;

//20260827_kpopmodder: Register LAVI-owned trusted commands without modifying the upstream command list.
public final class AutoDepositTrustedCommandRegistrar {
    public static final String TRUST_COMMAND = "auto_deposit_trust";
    public static final String UNTRUST_COMMAND = "auto_deposit_untrust";
    public static final String LIST_COMMAND = "auto_deposit_trusted_list";
    public static final String KOREAN_BULK_TRUST_COMMAND =
            AutoDepositKoreanBulkTrustCommand.COMMAND_NAME;

    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedTargetResolver targetResolver;
    private final AutoDepositTrustedDestinationStatusInspector statusInspector;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation;
    private boolean registrationAttempted;
    private boolean registered;
    private boolean koreanBulkAliasRegistered;

    public AutoDepositTrustedCommandRegistrar(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositOpenContainerBindingTracker bindingTracker) {
        this(
                repository,
                bindingTracker,
                new AutoDepositTrustedBulkRegistrationCommandOperation(
                        new AutoDepositTrustedBulkRegistrationService(repository)
                )
        );
    }

    public AutoDepositTrustedCommandRegistrar(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositOpenContainerBindingTracker bindingTracker,
            AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation) {
        this.repository = Objects.requireNonNull(repository, "repository");
        targetResolver = new AutoDepositTrustedTargetResolver(
                Objects.requireNonNull(bindingTracker, "bindingTracker")
        );
        statusInspector = new AutoDepositTrustedDestinationStatusInspector();
        worldKeyReader = new AutoDepositWorldKeyReader();
        this.bulkRegistrationOperation = Objects.requireNonNull(
                bulkRegistrationOperation,
                "bulkRegistrationOperation"
        );
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
            Command trustCommand = new AutoDepositTrustCommand(
                    repository,
                    targetResolver,
                    worldKeyReader,
                    bulkRegistrationOperation
            );
            Command untrustCommand = new AutoDepositUntrustCommand(
                    repository,
                    targetResolver,
                    worldKeyReader
            );
            Command listCommand = new AutoDepositTrustedListCommand(
                    repository,
                    statusInspector,
                    worldKeyReader
            );
            mod.getCommandExecutor().registerNewCommand(
                    trustCommand,
                    untrustCommand,
                    listCommand
            );
            registered = mod.getCommandExecutor().get(TRUST_COMMAND) == trustCommand
                    && mod.getCommandExecutor().get(UNTRUST_COMMAND) == untrustCommand
                    && mod.getCommandExecutor().get(LIST_COMMAND) == listCommand;
            if (!registered) {
                Debug.logError("Trusted automatic-deposit English command registration could not be proven.");
                return;
            }
            if (mod.getCommandExecutor().get(KOREAN_BULK_TRUST_COMMAND) != null) {
                Debug.logError("Korean trusted batch command name already registered; alias registration refused.");
                return;
            }
            Command koreanBulkCommand = new AutoDepositKoreanBulkTrustCommand(
                    bulkRegistrationOperation
            );
            mod.getCommandExecutor().registerNewCommand(koreanBulkCommand);
            koreanBulkAliasRegistered = mod.getCommandExecutor().get(
                    KOREAN_BULK_TRUST_COMMAND
            ) == koreanBulkCommand;
            if (!koreanBulkAliasRegistered) {
                Debug.logError("Korean trusted batch command registration could not be proven.");
            }
        } catch (CommandException exception) {
            Debug.logError("Failed to register trusted automatic-deposit commands: "
                    + exception.getMessage());
        }
    }

    public boolean registered() {
        return registered;
    }

    public boolean koreanBulkAliasRegistered() {
        return koreanBulkAliasRegistered;
    }
}
