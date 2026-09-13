package lavi.minecraft.diagnostics.session.lifecycle.registration.emission;

import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationResult;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: A failing emergency sink is bounded and cannot become another initializer failure.
class DiagnosticRegistrationFailureEmitterTest {
    private static final DiagnosticObserverRegistrationResult FAILURE = new DiagnosticObserverRegistrationResult(
            DiagnosticObserverRegistrationStatus.CAPACITY_EXHAUSTED, 32, 32, 0);

    @Test
    void capsAllAttemptsEvenWhenEveryEmissionFails() {
        DiagnosticRegistrationFailureEmitter emitter = new DiagnosticRegistrationFailureEmitter(line -> {
            throw new NoClassDefFoundError("fixture");
        });
        assertDoesNotThrow(() -> {
            for (int index = 0; index < 100; index++) emitter.report("owner", FAILURE, "INIT", "OVER_CAPACITY");
        });
        assertEquals(32, emitter.attemptedCount());
        assertEquals(32, emitter.emissionFailureCount());
        assertEquals(0, emitter.callsReturnedCount());
    }

    @Test
    void reportsBoundedSanitizedEvidenceAndDoesNotClaimFilePersistence() {
        List<String> records = new ArrayList<>();
        DiagnosticRegistrationFailureEmitter emitter = new DiagnosticRegistrationFailureEmitter(records::add);
        emitter.report("owner\n".repeat(50), FAILURE, "REGISTER_OWNER", "CAPACITY_EXHAUSTED");
        assertEquals(1, records.size());
        assertTrue(records.get(0).length() < 600);
        assertFalse(records.get(0).contains("\n"));
        assertTrue(records.get(0).contains("registeredCount=32 capacity=32"));
        assertTrue(records.get(0).contains("available=false filePersistence=NOT_VERIFIED"));
        assertEquals(1, emitter.callsReturnedCount());
    }
}
