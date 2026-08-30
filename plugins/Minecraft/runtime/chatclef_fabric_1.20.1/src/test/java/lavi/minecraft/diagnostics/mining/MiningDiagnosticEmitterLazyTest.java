package lavi.minecraft.diagnostics.mining;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260830_kpopmodder: Prove a suppressed mining event never evaluates its passive detail supplier.
class MiningDiagnosticEmitterLazyTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        MiningDiagnosticEmitter.resetSession();
    }

    @Test
    void suppressedEventDoesNotInvokeDetailSupplier() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        MiningDiagnosticEmitter.resetSession();
        AtomicInteger supplierCalls = new AtomicInteger();
        String bucket = "lazy-supplier-test-" + System.nanoTime();
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            MiningDiagnosticEmitter.emitLazy(
                    "MINING_LAZY_SUPPLIER_TEST",
                    "mining_lazy_supplier_test",
                    null,
                    bucket,
                    "stable-semantic-fingerprint",
                    () -> {
                        supplierCalls.incrementAndGet();
                        return new Object[]{"detail", "captured"};
                    }
            );
            MiningDiagnosticEmitter.emitLazy(
                    "MINING_LAZY_SUPPLIER_TEST",
                    "mining_lazy_supplier_test",
                    null,
                    bucket,
                    "stable-semantic-fingerprint",
                    () -> {
                        supplierCalls.incrementAndGet();
                        return new Object[]{"detail", "should_not_be_captured"};
                    }
            );
        } finally {
            System.setOut(original);
        }

        assertEquals(1, supplierCalls.get());
    }
}
