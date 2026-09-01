package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionBindingRegistry.LookupResult;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionBindingRegistry.LookupStatus;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;

//20260829_kpopmodder: Split interaction correlation from the StoreDepositDiagnostics compatibility facade.
public final class StoreDepositInteractionDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositInteractionBindingRegistry interactionBindings;

    public StoreDepositInteractionDiagnostics(StoreDepositBindingRegistry bindings,
                                              StoreDepositEmissionGate emissionGate,
                                              StoreDepositInteractionBindingRegistry interactionBindings) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.interactionBindings = interactionBindings;
    }

    public void bindInteraction(Task activeTask, BlockInteractionContext interaction) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositInteractionContext context = StoreDepositInteractionContext.capture(
                    bindings.stateFor(activeTask),
                    activeTask,
                    interaction
            );
            interactionBindings.bind(context, ChatClefDiagnostics.currentClientTickId());
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public Object[] interactionFields(BlockInteractionContext interaction) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || interaction == null) {
            return StoreDepositInteractionDiagnosticFields.fields(null, null);
        }
        try {
            LookupResult lookup = interactionLookup(interaction);
            StoreDepositInteractionContext context = lookup.context();
            if (lookup.status() != LookupStatus.FOUND) {
                return StoreDepositInteractionDiagnosticFields.unavailableObservationFields(
                        context,
                        lookup.status()
                );
            }
            StoreDepositOperationState currentState = context == null
                    ? null
                    : bindings.stateForOperation(context.storeOperationId());
            return StoreDepositInteractionDiagnosticFields.fields(context, currentState);
        } catch (RuntimeException | LinkageError ignored) {
            return new Object[]{
                    "storeContextAvailable", "unavailable#error",
                    "storeContextCoverageReason", "DIAGNOSTIC_BINDING_LOOKUP_FAILED"
            };
        }
    }

    public String interactionScopeKey(BlockInteractionContext interaction) {
        StoreDepositInteractionContext context = interactionLookup(interaction).context();
        return context == null ? "store-unbound" : context.storeAttemptId();
    }

    public boolean shouldEmitInteractionDetail(BlockInteractionContext interaction,
                                               String eventName,
                                               String semanticKey) {
        StoreDepositInteractionContext context = interactionContext(interaction);
        return context == null || emissionGate.shouldEmitDetail(
                context.storeOperationId(),
                eventName,
                context.storeAttemptId() + "|" + semanticKey
        );
    }

    public int clearForModeTransition() {
        return interactionBindings.clearForModeTransition();
    }

    private StoreDepositInteractionContext interactionContext(BlockInteractionContext interaction) {
        return interactionLookup(interaction).context();
    }

    private LookupResult interactionLookup(BlockInteractionContext interaction) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || interaction == null) {
            return new LookupResult(LookupStatus.NOT_FOUND, null);
        }
        try {
            return interactionBindings.lookup(
                    interaction.interactionId(),
                    ChatClefDiagnostics.currentClientTickId()
            );
        } catch (RuntimeException | LinkageError ignored) {
            return new LookupResult(LookupStatus.NOT_FOUND, null);
        }
    }
}
