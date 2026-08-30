package lavi.minecraft.diagnostics.mining.progress;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.formatting.MiningSemanticFingerprint;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Fingerprint already-computed movement progress and reset provenance.
public final class MovementProgressSemanticFingerprint {
    private MovementProgressSemanticFingerprint() {
    }

    public static String create(BlockPos target,
                                String checkerOwner,
                                int checkerCallIndex,
                                boolean checkEvaluated,
                                boolean checkResult,
                                boolean resetObservedBeforeCheck,
                                String resetReason,
                                String failureTransition) {
        return MiningSemanticFingerprint.join(
                "MOVEMENT_PROGRESS_CHECK_RESULT",
                checkerOwner,
                Integer.toString(checkerCallIndex),
                ChatClefDiagnostics.blockPos(target),
                Boolean.toString(checkEvaluated),
                Boolean.toString(checkResult),
                Boolean.toString(resetObservedBeforeCheck),
                normalize(resetReason),
                failureTransition
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
