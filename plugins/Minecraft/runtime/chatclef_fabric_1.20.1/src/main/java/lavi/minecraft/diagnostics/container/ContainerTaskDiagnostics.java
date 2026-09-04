package lavi.minecraft.diagnostics.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.crafting.CraftingTableRouteRetryDiagnostics;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.CraftResourceContainerSourceEventObserver;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.lifecycle.ContainerTaskOwnerStopDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.reconciliation.ContainerTaskChildReconciliationDiagnostics;
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
        logBoundaryInternal(
                eventName,
                reason,
                stateKey,
                mod,
                task,
                containerTarget,
                containerBlocks,
                branchFields
        );
    }

    public static void logBoundaryWithSelectedChild(
            String eventName,
            String reason,
            String stateKey,
            AltoClef mod,
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            Task selectedChild,
            Object... branchFields) {
        boolean sourceEmissionCompleted = logBoundaryInternal(
                eventName,
                reason,
                stateKey,
                mod,
                task,
                containerTarget,
                containerBlocks,
                branchFields
        );
        CraftResourceContainerSourceEventObserver.observeChildSelection(
                task,
                selectedChild,
                containerTarget,
                containerBlocks,
                reason,
                stateKey,
                branchFields,
                sourceEmissionCompleted
        );
    }

    public static boolean logChildReconciliation(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean childCleared) {
        ContainerGuiDiagnostics.onTaskReconciliation(
                parent,
                activeChildBefore,
                candidateChild,
                isEqualResult,
                canInterruptEvaluated,
                canInterruptPreviousChild,
                replacementApplied,
                previousChildStopCalled,
                activeChildAfter,
                candidateDiscardedBecauseEqual,
                childCleared
        );
        return ContainerTaskChildReconciliationDiagnostics.log(
                parent,
                activeChildBefore,
                candidateChild,
                isEqualResult,
                canInterruptEvaluated,
                canInterruptPreviousChild,
                replacementApplied,
                previousChildStopCalled,
                activeChildAfter,
                candidateDiscardedBecauseEqual,
                childCleared
        );
    }

    public static boolean logOwnerStop(Task owner, Task interruptTask) {
        return ContainerTaskOwnerStopDiagnostics.log(owner, interruptTask);
    }

    private static boolean logBoundaryInternal(
            String eventName,
            String reason,
            String stateKey,
            AltoClef mod,
            Task task,
            ItemTarget containerTarget,
            Block[] containerBlocks,
            Object[] branchFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return false;
        }

        CarryOnObservation carryOn = observeCarryOn();
        CraftingTableRouteRetryDiagnostics.observe(eventName, reason, stateKey, mod, task, containerTarget,
                containerBlocks, branchFields);
        StoreDepositDiagnostics.observeContainerRouteEvent(eventName, reason, task, branchFields);
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
            return false;
        }
        if (decision.emitSummary()) {
            ChatClefDiagnostics.logBoundary("CONTAINER_TASK_DIAGNOSTIC_REPEAT_SUMMARY",
                    "container_task_diagnostic_repeat_summary",
                    task,
                    ChatClefDiagnostics.withCommandContextFields(
                            ContainerTaskDiagnosticFields.repeatSummary(repeatKey, decision.suppressedRepeatCount())
                    ));
            return false;
        }
        if (!decision.emitEvent()) {
            return false;
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
        boolean sourceEmissionCompleted = ChatClefDiagnostics.logBoundaryWithPhysicalOutcome(eventName,
                reason,
                task,
                ChatClefDiagnostics.withCommandContextFields(fields));
        if (sourceEmissionCompleted && "CONTAINER_TASK_TARGET_DECISION".equals(eventName)) {
            CraftResourceContainerSourceEventObserver.observeTargetDecision(
                    task,
                    containerTarget,
                    containerBlocks,
                    reason,
                    stateKey,
                    branchFields
            );
        }
        return sourceEmissionCompleted;
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
