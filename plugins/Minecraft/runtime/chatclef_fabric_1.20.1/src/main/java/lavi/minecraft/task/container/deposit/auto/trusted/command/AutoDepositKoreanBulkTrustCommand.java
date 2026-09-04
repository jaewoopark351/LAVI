package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositKoreanBulkTrustCommandFormParser;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Keep the Korean direct alias batch-only and isolated from mutable English command state.
public final class AutoDepositKoreanBulkTrustCommand extends Command {
    public static final String COMMAND_NAME = "\uC790\uB3D9\uBCF4\uAD00\uB4F1\uB85D";

    private static final String INVALID_FORM_MESSAGE =
            "Unsupported Korean trusted registration form. Use @"
                    + COMMAND_NAME
                    + " \uC601\uC5ED 16x16 or @"
                    + COMMAND_NAME
                    + " \uBC18\uACBD 16x16.";

    private final AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation;
    private final AutoDepositKoreanBulkTrustCommandFormParser formParser;

    public AutoDepositKoreanBulkTrustCommand(
            AutoDepositTrustedBulkRegistrationOperation bulkRegistrationOperation)
            throws CommandException {
        super(COMMAND_NAME, "Register loaded nearby containers as trusted automatic storage.");
        this.bulkRegistrationOperation = Objects.requireNonNull(
                bulkRegistrationOperation,
                "bulkRegistrationOperation"
        );
        formParser = new AutoDepositKoreanBulkTrustCommandFormParser();
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        try {
            Optional<AutoDepositTrustCommandForm> parsed = formParser.parse(parser);
            if (parsed.isEmpty()) {
                throw new CommandException(INVALID_FORM_MESSAGE);
            }
            bulkRegistrationOperation.register(mod, parsed.get());
        } finally {
            finish();
        }
    }
}
