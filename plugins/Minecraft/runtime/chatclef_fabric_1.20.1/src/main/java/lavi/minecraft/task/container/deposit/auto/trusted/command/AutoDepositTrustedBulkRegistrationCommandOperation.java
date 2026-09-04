package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationFormatter;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationService;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;

import java.util.Objects;
import java.util.function.BiFunction;

//20260904_kpopmodder: Added a command adapter that exclusively owns bulk-result formatting and emission.
public final class AutoDepositTrustedBulkRegistrationCommandOperation
        implements AutoDepositTrustedBulkRegistrationOperation {
    private final BiFunction<AltoClef, AutoDepositTrustCommandForm,
            AutoDepositTrustedBulkRegistrationResult> executor;

    public AutoDepositTrustedBulkRegistrationCommandOperation(
            AutoDepositTrustedBulkRegistrationService service) {
        this(Objects.requireNonNull(service, "service")::execute);
    }

    AutoDepositTrustedBulkRegistrationCommandOperation(
            BiFunction<AltoClef, AutoDepositTrustCommandForm,
                    AutoDepositTrustedBulkRegistrationResult> executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void register(AltoClef mod, AutoDepositTrustCommandForm commandForm) {
        AltoClef checkedMod = Objects.requireNonNull(mod, "mod");
        AutoDepositTrustedBulkRegistrationResult result = Objects.requireNonNull(
                executor.apply(
                        checkedMod,
                        Objects.requireNonNull(commandForm, "commandForm")
                ),
                "bulk registration result"
        );
        String message = AutoDepositTrustedBulkRegistrationFormatter.format(result);
        if (result.success()) {
            checkedMod.log(message);
        } else {
            checkedMod.logWarning(message);
        }
    }
}
