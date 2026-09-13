package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationActivation;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureState;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlanningResult;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetResolution;

//20260913_kpopmodder: Observe reached pressure decisions while leaving all behavior evaluation with the chain.
public final class AutoDepositPressureObserver {
    private long sequence;
    private ObservationActivation activation;
    private ObservationScope scope;
    private AutoDepositEvaluationTrace trace;
    private AutoDepositWaitOrigin waitOrigin;

    public void begin(DepositAllInventoryPressureState state, boolean pending, Task task) {
        observe(() -> {
            if (activation != null && !activation.isCurrent()) {
                waitOrigin = null;
                sequence = 0;
            }
            trace = null;
            scope = null;
            scope = AutoDepositObservationScopeResolver.open(task);
            activation = scope.activation();
            if (sequence != Long.MAX_VALUE) sequence++;
            trace = new AutoDepositEvaluationTrace(sequence, tick(), state.name(), pending);
            if (waitOrigin != null) scope.pin("pressure-wait-origin", waitOrigin.fields(tick()));
            scope.record("AUTO_DEPOSIT_EVALUATION_ENTER", "pressure_callback_reached",
                    state.name(), false, trace.fields());
        });
    }

    public void value(String key, Object value) {
        observe(() -> { if (trace != null) trace.record(key, value); });
    }

    public void pressure(DepositAllInventoryPressureSnapshot snapshot) {
        observe(() -> {
            if (trace == null) return;
            trace.record("pressureRead", snapshot == null ? "UNAVAILABLE" : "OBSERVED");
            if (snapshot != null) {
                trace.record("occupiedSlots", snapshot.occupiedSlots());
                trace.record("totalSlots", snapshot.totalSlots());
                trace.record("freeSlots", snapshot.freeSlots());
            }
        });
    }

    public void planning(AutoDepositPlanningResult planning) {
        observe(() -> {
            if (trace == null) return;
            trace.record("planningStatus", planning.status().name());
            trace.record("planningReason", planning.reason());
            planning.diagnosticPlan().ifPresent(plan -> {
                trace.record("policyEpoch", plan.context().epoch());
                trace.record("protectedItemTypes", plan.protectedCounts().size());
                trace.record("protectedItemCount", plan.protectedCounts().values().stream().mapToLong(Integer::longValue).sum());
                trace.record("expectedFreedSlots", plan.expectedFreedSlots());
                trace.record("targetReliefSlots", plan.targetReliefSlots());
                trace.record("trustedCandidateCount", plan.trustedCandidates().size());
            });
        });
    }

    public void workingSet(WorkingSetResolution resolution) {
        observe(() -> {
            if (trace == null) return;
            trace.record("workingSetStatus", resolution.status().name());
            trace.record("workingSetReason", resolution.reason());
            resolution.supportedSnapshot().ifPresent(snapshot -> {
                trace.record("workingSetEpoch", snapshot.epoch());
                trace.record("workingSetTaskPathLength", snapshot.taskPath().size());
                trace.record("workingSetRoot", AutoDepositObservationFields.identity(snapshot.userTaskRoot()));
                trace.record("reservedItemTypes", snapshot.reservedCounts().size());
                trace.record("reservedItemCount", snapshot.reservedCounts().values().stream().mapToLong(Integer::longValue).sum());
            });
        });
    }

    public void decision(String reason, DepositAllInventoryPressureState state, boolean pending) {
        observe(() -> {
            if (trace == null || scope == null || !scope.isCurrent()) return;
            trace.record("stateAfter", state.name());
            trace.record("thresholdPendingAfter", pending);
            Object[] wait = waitOrigin == null
                    ? new Object[]{"waitOrigin", isWaiting(state) ? "UNAVAILABLE_BEFORE_OBSERVATION" : "NOT_WAITING"}
                    : waitOrigin.fields(tick());
            Object[] fields = AutoDepositObservationFields.concat(wait, trace.fields());
            scope.record("AUTO_DEPOSIT_DECISION", reason,
                    AutoDepositObservationFields.fingerprint("AUTO_DEPOSIT_DECISION", reason, fields), false, fields);
        });
    }

    public void transition(DepositAllInventoryPressureState previous, DepositAllInventoryPressureState next,
                           String reason, Task task, long trustedRevision) {
        observe(() -> {
            if (activation != null && !activation.isCurrent()) waitOrigin = null;
            scope = AutoDepositObservationScopeResolver.open(task);
            activation = scope.activation();
            if (isWaiting(next) && (previous != next || waitOrigin == null)) {
                waitOrigin = new AutoDepositWaitOrigin(next.name(), reason,
                        AutoDepositObservationFields.identity(task),
                        scope.operationKey(),
                        tick(), sequence, trustedRevision);
                scope.pin("pressure-wait-origin", waitOrigin.fields(tick()));
            }
            Object[] origin = waitOrigin == null ? new Object[]{"waitOrigin", "NOT_WAITING"} : waitOrigin.fields(tick());
            scope.record("AUTO_DEPOSIT_WAIT_TRANSITION", reason,
                    previous.name() + "|" + next.name() + "|" + reason, false,
                    AutoDepositObservationFields.concat(new Object[]{"stateBefore", previous.name(),
                            "stateAfter", next.name(), "evaluationSequence", sequence}, origin));
            if (!isWaiting(next)) waitOrigin = null;
        });
    }

    private void observe(Runnable observation) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            trace = null;
            scope = null;
            activation = null;
            waitOrigin = null;
            return;
        }
        try { observation.run(); } catch (RuntimeException | LinkageError ignored) {
            // The guarded code only captures/records diagnostic values, never engine work.
            ObservationDiagnostics.captureFailed("deposit", "PRESSURE_OBSERVATION_CAPTURE_FAILED");
        }
    }

    private static boolean isWaiting(DepositAllInventoryPressureState state) {
        return state == DepositAllInventoryPressureState.WAIT_FOR_REARM
                || state == DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT;
    }

    private static long tick() {
        return ChatClefDiagnostics.currentClientTickId();
    }
}
