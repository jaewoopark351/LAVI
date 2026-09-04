package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Isolate the command facade from the bulk scan and repository implementation graph.
@FunctionalInterface
public interface AutoDepositTrustedBulkRegistrationOperation {
    void register(AltoClef mod, AutoDepositTrustCommandForm commandForm);

    static AutoDepositTrustedBulkRegistrationOperation unavailable() {
        return (mod, commandForm) -> mod.logWarning(
                "Trusted destination batch registration is unavailable: "
                        + commandForm.canonicalInvocation()
        );
    }
}
