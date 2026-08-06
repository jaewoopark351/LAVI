package lavi.minecraft.diagnostics.mining.baritone;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

//20260806_kpopmodder: Emit Baritone investigation events through the bounded mining diagnostic contract.
final class BaritoneDiagnosticEmitter {
    private BaritoneDiagnosticEmitter() {
    }

    static void emit(String eventName,
                     String reason,
                     String owner,
                     String trigger,
                     String bucket,
                     String fingerprint,
                     Object[] fields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        MiningDiagnosticEmitter.emit(eventName, reason, null, bucket, fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", owner,
                        "trigger", trigger,
                        "correlation", bucket
                }, fields));
    }
}
