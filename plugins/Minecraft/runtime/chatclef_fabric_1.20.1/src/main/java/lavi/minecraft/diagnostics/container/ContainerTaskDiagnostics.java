package lavi.minecraft.diagnostics.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.crafting.CraftingTableRouteRetryDiagnostics;
import lavi.minecraft.integration.carryon.CarryOnDiagnostics;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import net.minecraft.block.Block;

//20260805_kpopmodder: Observe container acquisition branches that can be confused by Carry On-held containers.
public final class ContainerTaskDiagnostics {
    private static final String CAP_SCOPE = "container_task";
    private static final ContainerTaskEmissionLimiter LIMITER = new ContainerTaskEmissionLimiter();
    private static long lastCarryOnObservationTick = -1;
    private static CarryOnObservation lastCarryOnObservation;

    private ContainerTaskDiagnostics() {
    }

    public static void logBoundary(String eventName,
                                   String reason,
                                   String stateKey,
                                   AltoClef mod,
                                   Task task,
                                   ItemTarget containerTarget,
                                   Block[] containerBlocks,
                                   Object... branchFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }

        CarryOnObservation carryOn = observeCarryOn();
        CraftingTableRouteRetryDiagnostics.observe(eventName, reason, stateKey, mod, task, containerTarget,
                containerBlocks, branchFields);
        String repeatKey = ContainerTaskDiagnosticFields.repeatKey(
                eventName,
                reason,
                stateKey,
                task,
                containerTarget,
                containerBlocks,
                carryOn
        );
        ContainerTaskEmissionDecision decision = LIMITER.evaluate(repeatKey, ChatClefDiagnostics.currentClientTickId());
        if (decision.emitCap()) {
            ChatClefDiagnostics.logBoundary("CONTAINER_TASK_DIAGNOSTIC_CAP_REACHED",
                    "container_task_diagnostic_cap_reached",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            ContainerTaskDiagnosticFields.cap(CAP_SCOPE, ContainerTaskEmissionLimiter.SESSION_HARD_CAP)
                    ));
            return;
        }
        if (decision.emitSummary()) {
            ChatClefDiagnostics.logBoundary("CONTAINER_TASK_DIAGNOSTIC_REPEAT_SUMMARY",
                    "container_task_diagnostic_repeat_summary",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            ContainerTaskDiagnosticFields.repeatSummary(repeatKey, decision.suppressedRepeatCount())
                    ));
            return;
        }
        if (!decision.emitEvent()) {
            return;
        }

        Object[] fields = ContainerTaskDiagnosticFields.fields(
                mod,
                task,
                containerTarget,
                containerBlocks,
                carryOn,
                decision.suppressedRepeatCount(),
                branchFields
        );
        ChatClefDiagnostics.logBoundary(eventName,
                reason,
                task,
                ChatClefDiagnostics.withCommandContextFields(fields));
    }

    private static CarryOnObservation observeCarryOn() {
        long currentTick = ChatClefDiagnostics.currentClientTickId();
        if (currentTick == lastCarryOnObservationTick && lastCarryOnObservation != null) {
            return lastCarryOnObservation;
        }
        lastCarryOnObservation = CarryOnDiagnostics.observe();
        lastCarryOnObservationTick = currentTick;
        return lastCarryOnObservation;
    }
}
