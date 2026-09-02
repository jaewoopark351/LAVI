package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Characterize null user-block range diagnostics before extracting the event family.
class StoreDepositUserBlockRangeNullDiagnosticsTest {
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
    void facadePreservesOrderedUnavailablePayloadAndFirstSignatureDeduplication() {
        Object owner = new TestOwner();

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.logUserBlockRangeNullInput(owner, null);
            StoreDepositDiagnostics.logUserBlockRangeNullInput(owner, null);
        });

        assertEquals(1, occurrences(output, "event=USER_BLOCK_RANGE_NULL_INPUT_OBSERVED"));
        String event = eventLine(output, "USER_BLOCK_RANGE_NULL_INPUT_OBSERVED");
        assertTrue(event.contains("reason=user_block_range_null_input_observed"));
        assertInOrder(
                event,
                "diagnosticsMode=BOUNDARY",
                "storeOperationId=UNAVAILABLE",
                "storeContextAvailable=false",
                "requestSource=UNAVAILABLE",
                "diagnosticScope=store_deposit_exception",
                "owner=store_deposit_exception_observer",
                "mode=BOUNDARY",
                "trigger=null_user_block_position",
                "dedupe_key=user_block_range_null_input|UserBlockRangeTracker.updateState|null_block_pos",
                "max_emission=first_signature_only,session%3D16",
                "correlation=storeOperationId%3DUNAVAILABLE",
                "payload=flat_fields",
                "terminal=false",
                "behavior_effect=none",
                "exceptionBoundary=" + TestOwner.class.getName(),
                "observedPosition=unavailable",
                "exceptionSignature=UserBlockRangeTracker.updateState|null_block_pos",
                "threadName="
        );
    }

    @Test
    void offModeLeavesTheNullInputFamilySilent() {
        ChatClefDiagnostics.setBoundaryEnabled(false);

        String output = captureOutput(() ->
                StoreDepositDiagnostics.logUserBlockRangeNullInput(new TestOwner(), null));

        assertEquals("", output.trim());
    }

    private static String eventLine(String output, String eventName) {
        for (String line : output.split("\\R")) {
            if (line.contains("event=" + eventName)) {
                return line;
            }
        }
        throw new AssertionError("Missing event: " + eventName);
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
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void assertInOrder(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token, previous + 1);
            assertTrue(current > previous, "Expected ordered token after index " + previous + ": " + token);
            previous = current;
        }
    }

    private static final class TestOwner {
    }
}
