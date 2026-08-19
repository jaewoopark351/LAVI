package lavi.minecraft.diagnostics.command.deposit;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;

//20260807_kpopmodder: Attribute deposit-created StoreInAnyContainerTask without changing command execution.
public final class DepositCommandDiagnostics {
    private DepositCommandDiagnostics() {
    }

    public static void logInvocation(AltoClef mod,
                                     boolean explicitItemListProvided,
                                     ItemTarget[] selectedItems,
                                     Task taskToRun) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        Object[] storeDepositFields = StoreDepositDiagnostics.registerBareDepositInvocation(
                mod,
                explicitItemListProvided,
                selectedItems,
                taskToRun
        );
        ChatClefDiagnostics.logBoundary("DEPOSIT_COMMAND_INVOCATION_DECISION",
                "deposit_command_invocation_decision",
                taskToRun,
                ChatClefDiagnostics.withCommandContextFields(
                        DepositCommandDiagnosticFields.invocationFields(
                                mod,
                                explicitItemListProvided,
                                selectedItems,
                                taskToRun,
                                storeDepositFields
                        )));
    }
}
