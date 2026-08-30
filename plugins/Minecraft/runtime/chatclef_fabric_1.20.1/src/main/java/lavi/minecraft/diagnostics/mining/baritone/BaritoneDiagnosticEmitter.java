package lavi.minecraft.diagnostics.mining.baritone;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;

import java.util.function.Supplier;

//20260806_kpopmodder: Emit Baritone investigation events through the bounded mining diagnostic contract.
public final class BaritoneDiagnosticEmitter {
    private BaritoneDiagnosticEmitter() {
    }

    public static void emit(String eventName,
                            String reason,
                            String owner,
                            String trigger,
                            String bucket,
                            String fingerprint,
                            Object[] fields) {
        emitLazy(eventName, reason, owner, trigger, bucket, fingerprint, () -> fields);
    }

    public static void emitLazy(String eventName,
                                String reason,
                                String owner,
                                String trigger,
                                String bucket,
                                String fingerprint,
                                Supplier<Object[]> fieldsSupplier) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        MiningDiagnosticEmitter.emitLazyWithFallback(
                eventName, reason, null, bucket, fingerprint,
                bucket, "BARITONE_BUCKET_OR_GENERATION_FALLBACK",
                () -> MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", owner,
                        "trigger", trigger,
                        "correlation", bucket
                }, fieldsSupplier == null ? null : fieldsSupplier.get()));
    }
}
