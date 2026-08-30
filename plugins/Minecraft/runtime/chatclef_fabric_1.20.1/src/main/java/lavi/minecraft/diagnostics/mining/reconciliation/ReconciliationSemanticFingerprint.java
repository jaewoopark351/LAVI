package lavi.minecraft.diagnostics.mining.reconciliation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.mining.formatting.MiningDiagnosticTaskFields;
import lavi.minecraft.diagnostics.mining.formatting.MiningSemanticFingerprint;

//20260830_kpopmodder: Fingerprint reconciliation semantics without candidate allocation identity.
public final class ReconciliationSemanticFingerprint {
    private ReconciliationSemanticFingerprint() {
    }

    public static String create(Task parent,
                                ReconciliationTaskIdentity activeChildBefore,
                                ReconciliationTaskIdentity candidateChild,
                                ReconciliationTaskIdentity activeChildAfter,
                                ReconciliationOutcome outcome,
                                boolean isEqualResult,
                                boolean replacementApplied,
                                boolean candidateDiscardedBecauseEqual) {
        return MiningSemanticFingerprint.join(
                "TASK_CHILD_RECONCILIATION",
                MiningDiagnosticTaskFields.taskClass(parent),
                activeChildBefore.taskClass(),
                activeChildBefore.targetPositionText(),
                candidateChild.taskClass(),
                candidateChild.targetPositionText(),
                activeChildAfter.taskClass(),
                activeChildAfter.targetPositionText(),
                outcome.candidateOutcome(),
                outcome.reconciliationClassification(),
                Boolean.toString(isEqualResult),
                Boolean.toString(replacementApplied),
                Boolean.toString(candidateDiscardedBecauseEqual)
        );
    }
}
