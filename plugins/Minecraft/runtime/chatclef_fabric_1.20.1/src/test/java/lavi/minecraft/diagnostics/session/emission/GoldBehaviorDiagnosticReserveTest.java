package lavi.minecraft.diagnostics.session.emission;

import lavi.minecraft.diagnostics.session.admission.*;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticEventFamilyClassifier;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Prove new behavior boundaries physically emit after ordinary output is exhausted.
class GoldBehaviorDiagnosticReserveTest {
    @Test void equipFirstAndPlacementTerminalHaveIndependentFileEmissionCapacity() throws Exception {
        DiagnosticSessionAdmissionAuthority authority = new DiagnosticSessionAdmissionAuthority("gold-behavior-reserves");
        for (int i = 0; i < DiagnosticSessionLimits.ORDINARY_CEILING; i++)
            assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        assertFalse(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        Path output = Files.createTempFile("gold-behavior-reserve-", ".log");
        emit(authority, "MINING_OPERATION_TOOL_EXACT_RESULT", output);
        for (int i = 1; i < DiagnosticSessionLimits.TOOL_EQUIP_FIRST_SLOTS; i++)
            assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.TOOL_EQUIP_FIRST)).admitted());
        assertFalse(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.TOOL_EQUIP_FIRST)).admitted());
        emit(authority, "MINING_OPERATION_TOOL_PLACEMENT_FAILURE", output);
        emit(authority, "BLOCK_PROTECTION_PUBLICATION_BOUNDARY", output);
        String written = Files.readString(output);
        assertTrue(written.contains("MINING_OPERATION_TOOL_EXACT_RESULT"));
        assertTrue(written.contains("MINING_OPERATION_TOOL_PLACEMENT_FAILURE"));
        assertTrue(written.contains("BLOCK_PROTECTION_PUBLICATION_BOUNDARY"));
    }
    @Test void producerReasonCannotBorrowExactBehaviorReserves() {
        assertEquals(DiagnosticEventFamily.TOOL_EQUIP_FIRST,
                DiagnosticEventFamilyClassifier.classify("MINING_OPERATION_TOOL_EXACT_RESULT"));
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL,
                DiagnosticEventFamilyClassifier.classify("prefix_MINING_OPERATION_TOOL_EXACT_RESULT"));
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL,
                DiagnosticEventFamilyClassifier.classify("BLOCK_PROTECTION_PUBLICATION_BOUNDARY_detail"));
    }
    private static void emit(DiagnosticSessionAdmissionAuthority authority, String event, Path output) {
        var admission = authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamilyClassifier.classify(event)));
        assertTrue(admission.admitted());
        var outcome = new DiagnosticEmissionCoordinator(authority).emit(admission.token(), event,
                value -> value, value -> {
                    try { Files.writeString(output, value + System.lineSeparator(), java.nio.file.StandardOpenOption.APPEND); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                });
        assertTrue(outcome.completed());
    }
}
