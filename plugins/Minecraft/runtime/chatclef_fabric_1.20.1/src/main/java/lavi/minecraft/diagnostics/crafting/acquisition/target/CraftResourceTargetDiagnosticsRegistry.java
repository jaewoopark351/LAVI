package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureAggregateLedger;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureAggregateSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command.CraftResourceCommandMismatchLedger;
import lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command.CraftResourceCommandMismatchSnapshot;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

//20260901_kpopmodder: Own bounded target ledgers without creating state for unbound observations.
public final class CraftResourceTargetDiagnosticsRegistry {
    private static final int ACTIVE_SCOPE_LIMIT = 8;
    private static final int OPAQUE_ID_UTF8_LIMIT = 360;
    private static final String EXCEPTION_EVENT_NAME =
            "BLOCK_OPTIONAL_META_MANAGER_EXCEPTION";
    private static final String COVERAGE_GAP_EVENT_NAME =
            "BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP";

    private final Map<IronPickaxeAcquisitionScopeKey, CraftResourceTargetAttemptLedger>
            targetLedgers = new LinkedHashMap<>();
    private final Map<IronPickaxeAcquisitionScopeKey, CraftResourceFailureAggregateLedger>
            failureLedgers = new LinkedHashMap<>();
    private final Map<IronPickaxeAcquisitionScopeKey, CraftResourceCommandMismatchLedger>
            commandMismatchLedgers = new LinkedHashMap<>();
    private CraftResourceMismatchAdmissionTracker mismatchTracker =
            new CraftResourceMismatchAdmissionTracker();
    private CraftResourceExceptionEvidenceTracker exceptionTracker =
            new CraftResourceExceptionEvidenceTracker();

    public synchronized boolean activateScope(IronPickaxeAcquisitionScopeKey scopeKey) {
        if (!isValidScopeKey(scopeKey)) {
            return false;
        }
        if (targetLedgers.containsKey(scopeKey)) {
            return true;
        }
        if (targetLedgers.size() >= ACTIVE_SCOPE_LIMIT) {
            return false;
        }
        if (!mismatchTracker.activateCorrelation(scopeKey.commandCorrelationId())) {
            return false;
        }
        targetLedgers.put(scopeKey, new CraftResourceTargetAttemptLedger());
        failureLedgers.put(scopeKey, new CraftResourceFailureAggregateLedger());
        commandMismatchLedgers.put(scopeKey, new CraftResourceCommandMismatchLedger());
        return true;
    }

    public synchronized CraftResourceTargetAttemptDecision observeTarget(
            IronPickaxeAcquisitionScopeKey scopeKey,
            CraftResourceTargetObservation observation) {
        CraftResourceTargetAttemptLedger ledger = targetLedgers.get(scopeKey);
        if (ledger == null || observation == null) {
            return unavailableTargetDecision();
        }
        return ledger.observe(observation);
    }

    public synchronized Optional<CraftResourceTargetAttemptSnapshot> currentSnapshot(
            IronPickaxeAcquisitionScopeKey scopeKey) {
        CraftResourceTargetAttemptLedger ledger = targetLedgers.get(scopeKey);
        return ledger == null ? Optional.empty() : Optional.of(ledger.snapshot());
    }

    public synchronized Optional<CraftResourceTargetTuple> currentTuple(
            IronPickaxeAcquisitionScopeKey scopeKey) {
        CraftResourceTargetAttemptLedger ledger = targetLedgers.get(scopeKey);
        return ledger == null ? Optional.empty() : ledger.currentTuple();
    }

    public synchronized boolean observeFailure(
            IronPickaxeAcquisitionScopeKey scopeKey,
            CraftResourceFailureObservation observation) {
        CraftResourceTargetAttemptLedger targetLedger = targetLedgers.get(scopeKey);
        CraftResourceFailureAggregateLedger failureLedger = failureLedgers.get(scopeKey);
        if (targetLedger == null || failureLedger == null || observation == null) {
            return false;
        }
        CraftResourceTargetAttemptSnapshot targetSnapshot = targetLedger.snapshot();
        failureLedger.observe(
                observation,
                targetSnapshot.targetAttemptSequence(),
                targetSnapshot.currentTuple()
        );
        return true;
    }

    public synchronized Optional<CraftResourceFailureAggregateSnapshot> failureSnapshot(
            IronPickaxeAcquisitionScopeKey scopeKey) {
        CraftResourceFailureAggregateLedger ledger = failureLedgers.get(scopeKey);
        return ledger == null ? Optional.empty() : Optional.of(ledger.snapshot());
    }

    public synchronized CraftResourceMismatchDecision evaluateMismatch(
            IronPickaxeAcquisitionScopeKey scopeKey,
            CraftResourceMismatchObservation observation) {
        if (!matchesActiveScope(scopeKey, observation)) {
            return unavailableMismatchDecision();
        }
        CraftResourceMismatchDecision decision = mismatchTracker.evaluate(observation);
        CraftResourceCommandMismatchLedger commandLedger = commandMismatchLedgers.get(scopeKey);
        if (commandLedger != null) {
            commandLedger.observe(observation, decision);
        }
        return decision;
    }

    public synchronized boolean recordMismatchAdmissionDenied(
            IronPickaxeAcquisitionScopeKey scopeKey,
            String fingerprint) {
        if (scopeKey == null || !targetLedgers.containsKey(scopeKey)) {
            return false;
        }
        boolean sessionRecorded = mismatchTracker.recordAdmissionDenied(fingerprint);
        CraftResourceCommandMismatchLedger commandLedger =
                commandMismatchLedgers.get(scopeKey);
        boolean commandRecorded = commandLedger != null
                && commandLedger.recordAdmissionDenied(fingerprint);
        return sessionRecorded || commandRecorded;
    }

    public synchronized boolean recordMismatchEmissionFailed(
            IronPickaxeAcquisitionScopeKey scopeKey,
            String fingerprint) {
        if (scopeKey == null || !targetLedgers.containsKey(scopeKey)) {
            return false;
        }
        boolean sessionRecorded = mismatchTracker.recordEmissionFailed(fingerprint);
        CraftResourceCommandMismatchLedger commandLedger =
                commandMismatchLedgers.get(scopeKey);
        boolean commandRecorded = commandLedger != null
                && commandLedger.recordEmissionFailed(fingerprint);
        return sessionRecorded || commandRecorded;
    }

    public synchronized Optional<CraftResourceCommandMismatchSnapshot>
            commandMismatchSnapshot(IronPickaxeAcquisitionScopeKey scopeKey) {
        CraftResourceCommandMismatchLedger ledger = commandMismatchLedgers.get(scopeKey);
        return ledger == null ? Optional.empty() : Optional.of(ledger.snapshot());
    }

    public synchronized CraftResourceExceptionDecision evaluateException(
            IronPickaxeAcquisitionScopeKey scopeKey,
            CraftResourceExceptionObservation observation) {
        if (!matchesActiveScope(scopeKey, observation)) {
            return unavailableExceptionDecision(observation);
        }
        return exceptionTracker.evaluate(observation);
    }

    public synchronized boolean retireScope(IronPickaxeAcquisitionScopeKey scopeKey) {
        failureLedgers.remove(scopeKey);
        commandMismatchLedgers.remove(scopeKey);
        boolean removed = targetLedgers.remove(scopeKey) != null;
        if (removed && targetLedgers.keySet().stream().noneMatch(activeKey ->
                activeKey.commandCorrelationId().equals(
                        scopeKey.commandCorrelationId()
                ))) {
            mismatchTracker.retireCorrelation(scopeKey.commandCorrelationId());
        }
        return removed;
    }

    public synchronized void clearForModeOff() {
        targetLedgers.clear();
        failureLedgers.clear();
        commandMismatchLedgers.clear();
        mismatchTracker = new CraftResourceMismatchAdmissionTracker();
        exceptionTracker = new CraftResourceExceptionEvidenceTracker();
    }

    public synchronized CraftResourceTargetDiagnosticsRegistrySnapshot snapshot() {
        return new CraftResourceTargetDiagnosticsRegistrySnapshot(
                targetLedgers.size(),
                targetLedgers.keySet(),
                mismatchTracker.snapshot(),
                exceptionTracker.snapshot()
        );
    }

    private boolean matchesActiveScope(
            IronPickaxeAcquisitionScopeKey scopeKey,
            CraftResourceMismatchObservation observation) {
        return scopeKey != null
                && observation != null
                && targetLedgers.containsKey(scopeKey)
                && scopeKey.commandCorrelationId().equals(
                        observation.commandCorrelationId()
                );
    }

    private boolean matchesActiveScope(
            IronPickaxeAcquisitionScopeKey scopeKey,
            CraftResourceExceptionObservation observation) {
        return scopeKey != null
                && observation != null
                && targetLedgers.containsKey(scopeKey)
                && scopeKey.commandCorrelationId().equals(
                        observation.commandCorrelationId()
                );
    }

    private static boolean isValidScopeKey(IronPickaxeAcquisitionScopeKey scopeKey) {
        return scopeKey != null
                && scopeKey.commandConnectionGeneration() >= 0L
                && scopeKey.rootGeneration() >= 0L
                && isValidOpaqueId(scopeKey.commandSessionId())
                && isValidOpaqueId(scopeKey.commandRequestId())
                && isValidOpaqueId(scopeKey.commandCorrelationId())
                && isValidOpaqueId(scopeKey.rootAssignmentId())
                && isValidOpaqueId(scopeKey.boundRootTaskInstanceId());
    }

    private static boolean isValidOpaqueId(String value) {
        return value != null
                && !value.isBlank()
                && value.getBytes(StandardCharsets.UTF_8).length <= OPAQUE_ID_UTF8_LIMIT;
    }

    private static CraftResourceTargetAttemptDecision unavailableTargetDecision() {
        return new CraftResourceTargetAttemptDecision(
                false,
                OptionalLong.empty(),
                false
        );
    }

    private static CraftResourceMismatchDecision unavailableMismatchDecision() {
        return new CraftResourceMismatchDecision(
                CraftResourceMismatchDisposition.ASSOCIATION_NOT_OWNED,
                false,
                ""
        );
    }

    private static CraftResourceExceptionDecision unavailableExceptionDecision(
            CraftResourceExceptionObservation observation) {
        boolean engineException = observation != null
                && observation.evidenceKind()
                == CraftResourceExceptionEvidenceKind.ENGINE_EXCEPTION;
        int framesAvailable = observation == null ? 0 : observation.framesAvailable();
        return new CraftResourceExceptionDecision(
                CraftResourceExceptionDisposition.ASSOCIATION_NOT_OWNED,
                engineException ? EXCEPTION_EVENT_NAME : COVERAGE_GAP_EVENT_NAME,
                false,
                false,
                framesAvailable,
                0,
                framesAvailable,
                List.of(),
                "",
                engineException ? observation.exceptionType() : Optional.empty(),
                engineException ? observation.exceptionMessage() : Optional.empty()
        );
    }
}
