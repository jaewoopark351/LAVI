package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;

//20260829_kpopmodder: Keep store-deposit operation registration in one focused lifecycle collaborator.
public final class StoreDepositOperationRegistrationDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositAutomaticTerminalDiagnostics automaticTerminals;

    public StoreDepositOperationRegistrationDiagnostics(StoreDepositBindingRegistry bindings) {
        this(bindings, null);
    }

    public StoreDepositOperationRegistrationDiagnostics(
            StoreDepositBindingRegistry bindings,
            StoreDepositAutomaticTerminalDiagnostics automaticTerminals) {
        this.bindings = bindings;
        this.automaticTerminals = automaticTerminals;
    }

    public Object[] registerBareDepositInvocation(AltoClef mod,
                                                  boolean explicitItemListProvided,
                                                  ItemTarget[] selectedItems,
                                                  Task taskToRun,
                                                  String requestSource) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return new Object[]{"storeContextAvailable", false};
        }
        try {
            String resolvedRequestSource = requestSource == null || requestSource.isBlank()
                    ? "BARE_DEPOSIT_COMMAND"
                    : requestSource;
            StoreDepositAutomaticContext automaticContext = automaticTerminals == null
                    ? StoreDepositAutomaticContext.unavailable()
                    : automaticTerminals.contextForChild(taskToRun);
            StoreDepositOperationState state = bindings.registerRoot(
                    taskToRun,
                    resolvedRequestSource,
                    automaticContext
            );
            state.recordRequestedTargets(selectedItems);
            return StoreDepositEventFields.merge(
                    StoreDepositEventFields.operationFields(state),
                    new Object[]{
                            "storeRequestSource", resolvedRequestSource,
                            "storeExplicitItemListProvided", explicitItemListProvided,
                            "storeSelectedItems", ChatClefDiagnostics.itemTargets(selectedItems),
                            "storeInvocationDimension", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getWorld() == null ? null : mod.getWorld().getRegistryKey().getValue())
                    });
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{"storeContextAvailable", "unavailable#error"};
        }
    }

    public void registerAutomaticChild(StoreDepositAutomaticContext automaticContext,
                                       ItemTarget[] selectedItems,
                                       Task taskToRun) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || automaticContext == null
                || !automaticContext.available()
                || taskToRun == null) {
            return;
        }
        StoreDepositOperationState state = bindings.registerRoot(
                taskToRun,
                "AUTO_DEPOSIT_ALL_CHAIN",
                automaticContext
        );
        state.recordRequestedTargets(selectedItems);
    }
}
