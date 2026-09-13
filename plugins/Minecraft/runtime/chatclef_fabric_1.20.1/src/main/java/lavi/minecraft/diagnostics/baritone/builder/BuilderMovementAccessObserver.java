package lavi.minecraft.diagnostics.baritone.builder;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderPathProvenance;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceRegistry;
import lavi.minecraft.diagnostics.baritone.correlation.BuilderTraceState;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import java.util.List;

//20260913_kpopmodder: Observe the exact List.get operands before the unchanged access and exception propagation.
public final class BuilderMovementAccessObserver {
    private BuilderMovementAccessObserver() { }
    public static void beforeAccess(Object builder, PathExecutor executor, IPath actualPath, List<?> actualList, int index) {
        try {
            if (!(executor instanceof BuilderExecutorView view)) return;
            BuilderTraceState state = BuilderTraceRegistry.owner(view.lavi$pathingBehavior());
            if (state == null) return;
            int count = BuilderPathSnapshot.size(actualList);
            String reason = BuilderPathSnapshot.accessReason(index, count);
            BuilderPathSnapshot snapshot = BuilderPathSnapshot.capture(actualPath);
            BuilderPathProvenance origin = state.ledger.find(actualPath);
            boolean invalid = "MOVEMENT_INDEX_OUT_OF_RANGE".equals(reason);
            Object[] fields = MiningDiagnosticEmitter.merge(new Object[]{
                    "builderIdentity", BuilderPathSnapshot.id(builder), "executorIdentity", BuilderPathSnapshot.id(executor),
                    "movementListIdentity", BuilderPathSnapshot.id(actualList), "listIdentityMeaning", "ACTUAL_RECEIVER_VIEW_ONLY",
                    "pathPosition", index, "movementsCountAtAccess", count,
                    "finishedAtGuard", false, "failedAtGuard", false, "guardEvidence", "REACHED_ORIGINAL_LIST_GET",
                    "actualOperandsCaptured", true, "currentCommandAtConsumption", BuilderTraceRegistry.commandOrigin(),
                    "taskContextAtConsumption", BuilderTaskContextSnapshot.capture()
            }, snapshot.fields("consumed"), origin == null ? new Object[]{"originStatus", "UNBOUND"} : origin.fields(),
                    invalid ? state.ledger.recentFields() : new Object[]{"recentHistory", "NOT_DUMPED"});
            state.event(invalid ? "BARITONE_BUILDER_MOVEMENT_ACCESS_INVALID" : "BARITONE_BUILDER_MOVEMENT_ACCESS",
                    reason, snapshot.identity() + ':' + index + ':' + count, fields);
        } catch (RuntimeException | LinkageError failure) {
            lavi.minecraft.diagnostics.observation.ObservationDiagnostics.captureFailed("builder", failure.getClass().getSimpleName());
        }
    }
}
