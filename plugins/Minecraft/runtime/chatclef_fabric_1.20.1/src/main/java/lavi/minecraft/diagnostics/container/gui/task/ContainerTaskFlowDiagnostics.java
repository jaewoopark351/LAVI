package lavi.minecraft.diagnostics.container.gui.task;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiDiagnosticFields;
import lavi.minecraft.diagnostics.container.gui.emission.ContainerGuiSemanticFingerprint;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiActiveFlow;
import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;

//20260904_kpopmodder: Observe post-screen Task entry and reconciliation without owning Task selection.
public final class ContainerTaskFlowDiagnostics {
    private final ContainerTaskRoleClassifier roles = new ContainerTaskRoleClassifier();

    public void onEvaluationStarted(
            ContainerGuiActiveFlow active,
            ContainerClientTickWindowSnapshot tick,
            Task task) {
        if (active == null || !roles.relevant(active.trace().source().targetFamily(), task)) {
            return;
        }
        ContainerGuiFlowTrace flow = active.trace();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        String taskRole = roles.role(flow.source().targetFamily(), task);
        boolean routeOwner = "CHEST_ROUTE_OWNER".equals(taskRole)
                || "FURNACE_ROUTE_OWNER".equals(taskRole);
        flow.taskEvaluationStarted(
                routeOwner ? roles.className(task) : null,
                routeOwner ? roles.identity(task) : null,
                gameTick
        );
        active.emitter().detail(
                "POST_SCREEN_TASK_EVALUATION",
                "container_task_tick_entered_after_screen_tail",
                ContainerGuiSemanticFingerprint.of(
                        "TASK_ENTRY",
                        flow.source().targetFamily(),
                        taskRole
                ),
                gameTick,
                task,
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tick,
                        "POST_SCREEN_TASK_EVALUATION_ENTRY",
                        "NONE"
                ),
                new Object[]{
                        "postScreenDistinctClientTickOrdinal", flow.postScreenDistinctClientTickOrdinal(),
                        "evaluatedTaskClass", roles.className(task),
                        "evaluatedTaskIdentity", roles.identity(task),
                        "evaluatedTaskRole", taskRole,
                        "taskEvaluationPhase", "TICK_ENTRY"
                }
        );
    }

    public void onReconciliation(
            ContainerGuiActiveFlow active,
            ContainerClientTickWindowSnapshot tick,
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
        if (active == null || !roles.relevant(
                active.trace().source().targetFamily(),
                parent,
                activeChildBefore,
                candidateChild,
                activeChildAfter)) {
            return;
        }
        ContainerGuiFlowTrace flow = active.trace();
        long gameTick = ChatClefDiagnostics.currentClientTickId();
        flow.taskReconciled(gameTick);
        String openChildState = roles.isOpenChild(activeChildAfter)
                ? "OPEN_CHILD_STILL_ACTIVE"
                : roles.isOpenChild(activeChildBefore)
                ? "OPEN_CHILD_REPLACED_OR_CLEARED"
                : "NO_OPEN_CHILD_OBSERVED";
        boolean transferSelected = roles.isTransferChild(candidateChild)
                || roles.isTransferChild(activeChildAfter);
        active.emitter().detail(
                "POST_SCREEN_TASK_EVALUATION",
                "container_task_reconciliation_after_screen_tail",
                ContainerGuiSemanticFingerprint.of(
                        "TASK_RECONCILIATION",
                        flow.source().targetFamily(),
                        roles.role(flow.source().targetFamily(), parent),
                        roles.role(flow.source().targetFamily(), activeChildBefore),
                        roles.role(flow.source().targetFamily(), candidateChild),
                        roles.role(flow.source().targetFamily(), activeChildAfter),
                        isEqualResult,
                        replacementApplied,
                        childCleared,
                        openChildState,
                        transferSelected
                ),
                gameTick,
                parent,
                ContainerGuiDiagnosticFields.downstream(
                        flow.source(),
                        tick,
                        "POST_SCREEN_TASK_RECONCILIATION",
                        "NONE"
                ),
                new Object[]{
                        "postScreenDistinctClientTickOrdinal", flow.postScreenDistinctClientTickOrdinal(),
                        "parentTaskClass", roles.className(parent),
                        "parentTaskIdentity", roles.identity(parent),
                        "parentTaskRole", roles.role(flow.source().targetFamily(), parent),
                        "activeChildBeforeClass", roles.className(activeChildBefore),
                        "activeChildBeforeIdentity", roles.identity(activeChildBefore),
                        "candidateChildClass", roles.className(candidateChild),
                        "candidateChildIdentity", roles.identity(candidateChild),
                        "activeChildAfterClass", roles.className(activeChildAfter),
                        "activeChildAfterIdentity", roles.identity(activeChildAfter),
                        "openChildState", openChildState,
                        "transferChildSelected", transferSelected,
                        "isEqualResult", isEqualResult,
                        "canInterruptEvaluated", canInterruptEvaluated,
                        "canInterruptPreviousChild", canInterruptPreviousChild,
                        "replacementApplied", replacementApplied,
                        "previousChildStopCalled", previousChildStopCalled,
                        "candidateDiscardedBecauseEqual", candidateDiscardedBecauseEqual,
                        "childCleared", childCleared
                }
        );
    }
}
