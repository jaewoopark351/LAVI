//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight.diagnostics;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

//20260913_kpopmodder: Observe existing preparation log boundaries without reading or choosing movement.
public final class GotoPreparationDiagnostics {
    private final String operation;
    private final GotoPreparationLogBudget budget = new GotoPreparationLogBudget();
    private final Consumer<String> legacySink;
    private final BiConsumer<String, Object[]> physicalSink;
    private final Supplier<Object[]> contextSupplier;
    private Object[] originalContext;

    public GotoPreparationDiagnostics(Task owner) {
        this(GotoPreparationLogFormatter.operation(owner), Debug::logMessage,
                (event, fields) -> ChatClefDiagnostics.logLifecycleBoundary(
                        event, "goto_preparation_owner_observation", owner, fields),
                ChatClefDiagnostics::currentCommandContextFields);
    }

    GotoPreparationDiagnostics(String operation, Consumer<String> legacySink,
                               BiConsumer<String, Object[]> physicalSink, Supplier<Object[]> contextSupplier) {
        this.operation = operation;
        this.legacySink = legacySink;
        this.physicalSink = physicalSink;
        this.contextSupplier = contextSupplier;
    }

    public void decision(String reason, String detail) {
        try {
            int legacy = budget.legacyDecision(reason);
            if (legacy == GotoPreparationLogBudget.LEGACY_DETAIL) {
                legacy("NATIVE_DECISION reason=" + reason + " " + detail);
            } else if (legacy == GotoPreparationLogBudget.LEGACY_LIMIT) {
                legacy("NATIVE_DECISION_LIMIT reached=true transitionsAndTerminalStillEnabled=true");
            }
            observe("NATIVE_DECISION reason=" + reason + " " + detail);
        } catch (RuntimeException | LinkageError ignored) {
            // Diagnostic counters, formatting, and sinks cannot terminate the movement task.
        }
    }

    public void log(String message) {
        legacy(message);
        observe(message);
    }

    private void legacy(String message) {
        try {
            legacySink.accept("[LAVI GOTO V3.1] " + message + " operation=" + operation);
        } catch (RuntimeException | LinkageError ignored) {
            // Preserve the old sink without coupling its failure to the physical observer.
        }
    }

    private void observe(String message) {
        try {
            String admission = budget.admission(message);
            if (admission == null) return;
            String signature = GotoPreparationLogFormatter.requiredSignature(message);
            if (admission.equals("native_detail_suppression_summary")) {
                message = "NATIVE_DECISION_LIMIT reached=true requiredBoundariesReserved=true";
                signature = null;
            }
            physicalSink.accept("GOTO_PREPARATION_" + GotoPreparationLogFormatter.event(message),
                    GotoPreparationLogFormatter.fields(operation, message, signature, admission, originalContext()));
        } catch (RuntimeException | LinkageError ignored) {
            // Physical failure is an evidence gap, never a retry, cleanup, or success decision.
        }
    }

    private Object[] originalContext() {
        if (originalContext == null) {
            try {
                originalContext = GotoPreparationLogFormatter.contextSnapshot(contextSupplier.get());
            } catch (RuntimeException | LinkageError error) {
                originalContext = new Object[]{"commandContextAvailable", false,
                        "commandContextError", error.getClass().getSimpleName()};
            }
        }
        return originalContext;
    }
}
//#endif
