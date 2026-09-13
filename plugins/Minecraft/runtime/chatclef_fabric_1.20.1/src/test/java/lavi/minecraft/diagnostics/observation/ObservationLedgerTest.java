package lavi.minecraft.diagnostics.observation;

import lavi.minecraft.diagnostics.observation.state.ObservationEmission;
import lavi.minecraft.diagnostics.observation.state.ObservationLedger;
import lavi.minecraft.diagnostics.observation.state.ObservationRegistry;
import lavi.minecraft.diagnostics.session.admission.*;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticEventFamilyClassifier;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ObservationLedgerTest {
    @Test void eightyRestartsKeepPinnedCauseWhileRepeatedFamiliesDoNotFillHistory() {
        ObservationLedger ledger = new ObservationLedger();
        ledger.pin("firstCause", "stone_left_hotbar");
        for (int run = 0; run < 80; run++) {
            ledger.pin("firstCause", "later_cause_" + run);
            ledger.capture("PREPARE", "HOTBAR_MISSING", "same", false, run, run, "child=" + run);
            ledger.capture("STOP", "PARENT_PREPARING", "same", false, run, run, "child=" + run);
        }
        Map<String, Object> result = values(ledger.summary());
        assertEquals(2, result.get("recentSize"));
        assertEquals(2, result.get("firstSize"));
        assertEquals(160L, result.get("captured"));
        assertTrue(result.get("firstPins").toString().contains("stone_left_hotbar"));
        assertFalse(result.get("firstPins").toString().contains("later_cause"));
    }

    @Test void firstReasonAndTerminalSurviveOrdinaryCeilingWithoutBorrowingOldCriticalPools() {
        DiagnosticSessionAdmissionAuthority authority = new DiagnosticSessionAdmissionAuthority("resource-reservation");
        for (int n = 0; n < DiagnosticSessionLimits.ORDINARY_CEILING; n++)
            assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        assertFalse(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        for (DiagnosticEventFamily family : new DiagnosticEventFamily[]{DiagnosticEventFamily.RESOURCE_MINING_FIRST,
                DiagnosticEventFamily.RESOURCE_DEPOSIT_FIRST, DiagnosticEventFamily.RESOURCE_BUILDER_FIRST}) {
            for (int n = 0; n < 128; n++) assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(family)).admitted());
            assertFalse(authority.admit(DiagnosticAdmissionRequest.eligible(family)).admitted());
        }
        assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.RESOURCE_OBSERVATION_TERMINAL)).admitted());
        assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL)).admitted());
        assertTrue(authority.snapshot().admittedSlots() < DiagnosticSessionLimits.HARD_CAP);
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL,
                DiagnosticEventFamilyClassifier.classify("RESOURCE_OBSERVATION_DETAIL"));
    }

    @Test void failureIsNotEmissionAndDoesNotRetryEveryTick() {
        ObservationLedger ledger = new ObservationLedger();
        ObservationEmission first = ledger.capture("TOOL", "MISSING", "missing", false, 1, 1, "slot=1");
        assertEquals("FIRST", first.tier());
        ledger.settle(true, false, "EMISSION_FAILED_AFTER_ADMISSION");
        for (int n = 2; n < 100; n++) assertNull(ledger.capture("TOOL", "MISSING", "missing", false, n, n, "slot=1"));
        ObservationEmission terminal = ledger.capture("CLOSE", "STOP", "stop", true, 100, 100, "");
        assertEquals("TERMINAL", terminal.tier());
        assertEquals(0L, values(terminal.required()).get("priorEmissionCallsReturned"));
        assertEquals(1L, values(terminal.required()).get("priorEmissionFailures"));
        assertNull(ledger.capture("CLOSE", "STOP", "stop", true, 101, 101, ""));
    }

    @Test void firstSlotsCannotBeExhaustedByRandomFingerprintsAndHistoryIsBounded() {
        ObservationLedger ledger = new ObservationLedger();
        for (int n = 0; n < 200; n++)
            ledger.capture("TOOL", "MISSING", "random-" + n, false, n, n, "");
        assertEquals(1, values(ledger.summary()).get("firstSize"));
        assertEquals(32, values(ledger.summary()).get("recentSize"));
        assertEquals("FIRST", ledger.capture("TOOL", "READY", "ready", false, 201, 201, "").tier());
    }

    @Test void worldAndModeInvalidateWorkerTokensAndCannotResetProcessScopeQuota() {
        ObservationRegistry registry = new ObservationRegistry();
        Object instance = new Object(), world = new Object(), nextWorld = new Object();
        ObservationActivation first = registry.capture(1, instance, world);
        assertTrue(first.live(1));
        assertSame(first, registry.capture(1, instance, world));
        registry.observeWorld(nextWorld);
        assertFalse(first.live(1));
        ObservationActivation second = registry.capture(1, instance, nextWorld);
        registry.invalidate();
        assertFalse(second.live(1));
        ObservationActivation third = registry.capture(2, instance, nextWorld);
        for (int n = 0; n < 4; n++) assertNotSame(ObservationScope.NOOP, registry.open(third, "mining", "request-" + n, ""));
        registry.invalidate();
        ObservationActivation fourth = registry.capture(3, instance, nextWorld);
        assertSame(ObservationScope.NOOP, registry.open(fourth, "mining", "later", ""));
        assertNotSame(ObservationScope.NOOP, registry.open(fourth, "deposit", "later", ""));
    }

    private static Map<String, Object> values(Object[] fields) {
        Map<String, Object> result = new HashMap<>();
        for (int n = 0; n < fields.length; n += 2) result.put(fields[n].toString(), fields[n + 1]);
        return result;
    }
}
