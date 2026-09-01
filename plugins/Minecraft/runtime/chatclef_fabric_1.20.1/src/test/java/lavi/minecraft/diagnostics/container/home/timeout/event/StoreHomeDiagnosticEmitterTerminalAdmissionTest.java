package lavi.minecraft.diagnostics.container.home.timeout.event;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticFamilySnapshot;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Verify the StoreHome producer retains terminal evidence after aggregate exhaustion.
class StoreHomeDiagnosticEmitterTerminalAdmissionTest {
    @BeforeEach
    void enableFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void terminalProducerEmitsAfterAggregateQuotaIsFull() {
        StoreHomeDiagnosticEmitter emitter = new StoreHomeDiagnosticEmitter(
                91L,
                () -> "terminal-producer-test"
        );

        String output = captureOutput(() -> {
            for (int index = 0;
                 index < DiagnosticSessionLimits.AGGREGATE_CHECKPOINT_SLOTS;
                 index++) {
                ChatClefDiagnostics.logBoundedBoundary(
                        "STORE_HOME_PROGRESS_SUMMARY_" + index,
                        "aggregate_fixture",
                        null,
                        8192,
                        new Object[0],
                        new Object[0]
                );
            }
            assertTrue(emitter.emitTerminal(
                    "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
                    "paired_delta_confirmed",
                    null,
                    new Object[]{
                            "diagnosticCaptureStatus", "complete",
                            "terminal", true,
                            "resultReason", "paired_delta_confirmed"
                    }
            ));
        });

        assertEquals(1, occurrences(
                output,
                "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY"
        ));
        DiagnosticSessionSnapshot snapshot =
                ChatClefDiagnostics.diagnosticSessionSnapshot();
        DiagnosticFamilySnapshot aggregate = snapshot.family(
                DiagnosticEventFamily.AGGREGATE_CHECKPOINT
        );
        DiagnosticFamilySnapshot terminal = snapshot.family(
                DiagnosticEventFamily.NON_STORE_TERMINAL
        );
        assertEquals(DiagnosticSessionLimits.AGGREGATE_CHECKPOINT_SLOTS,
                aggregate.admittedSlots());
        assertEquals(1, terminal.admittedSlots());
        assertEquals(1, terminal.emissionCompleted());
        assertEquals(0, terminal.emissionPending());
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
