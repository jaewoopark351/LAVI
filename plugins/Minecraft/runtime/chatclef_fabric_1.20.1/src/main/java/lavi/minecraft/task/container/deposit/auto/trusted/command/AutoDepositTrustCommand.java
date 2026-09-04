package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationService;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositEnglishTrustCommandFormParser;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import lavi.minecraft.task.container.deposit.auto.trusted.command.single.AutoDepositExactTrustService;
import lavi.minecraft.task.container.deposit.auto.trusted.command.single.AutoDepositSingleTrustOperation;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Register only a user-selected exact container as trusted automatic storage.
public final class AutoDepositTrustCommand extends Command {
    private static final String INVALID_FORM_MESSAGE =
            "Unsupported trusted registration form. Use @auto_deposit_trust, "
                    + "@auto_deposit_trust area 16x16, or "
                    + "@auto_deposit_trust \uBC18\uACBD 16x16.";

    private final AutoDepositSingleTrustOperation singleTrustOperation;
    private final AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation;
    private final AutoDepositEnglishTrustCommandFormParser formParser;

    public AutoDepositTrustCommand(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositTrustedTargetResolver targetResolver,
            AutoDepositWorldKeyReader worldKeyReader) throws CommandException {
        this(
                repository,
                targetResolver,
                worldKeyReader,
                new AutoDepositTrustedBulkRegistrationCommandOperation(
                        new AutoDepositTrustedBulkRegistrationService(repository)
                )
        );
    }

    public AutoDepositTrustCommand(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositTrustedTargetResolver targetResolver,
            AutoDepositWorldKeyReader worldKeyReader,
            AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation)
            throws CommandException {
        this(
                new AutoDepositExactTrustService(repository, targetResolver, worldKeyReader),
                bulkRegistrationOperation
        );
    }

    AutoDepositTrustCommand(
            AutoDepositSingleTrustOperation singleTrustOperation,
            AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation)
            throws CommandException {
        super("auto_deposit_trust", "Register the open or targeted container as trusted automatic storage.");
        this.singleTrustOperation = Objects.requireNonNull(
                singleTrustOperation,
                "singleTrustOperation"
        );
        this.bulkRegistrationOperation = Objects.requireNonNull(
                bulkRegistrationOperation,
                "bulkRegistrationOperation"
        );
        formParser = new AutoDepositEnglishTrustCommandFormParser();
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        try {
            Optional<AutoDepositTrustCommandForm> parsed = formParser.parse(parser);
            if (parsed.isEmpty()) {
                throw new CommandException(INVALID_FORM_MESSAGE);
            }
            AutoDepositTrustCommandForm form = parsed.get();
            if (!form.bulk()) {
                singleTrustOperation.register(mod);
                return;
            }
            bulkRegistrationOperation.register(mod, form);
        } finally {
            finish();
        }
    }
}
