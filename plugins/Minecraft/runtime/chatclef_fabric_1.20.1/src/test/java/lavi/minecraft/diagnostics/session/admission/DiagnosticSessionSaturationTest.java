package lavi.minecraft.diagnostics.session.admission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSessionSaturationTest {
    @Test
    void nonNegativeArithmeticSaturatesWithoutWraparound() {
        SaturatingLong.Result exactMaximum = SaturatingLong.add(Long.MAX_VALUE - 1L, 1L);
        assertEquals(Long.MAX_VALUE, exactMaximum.value());
        assertFalse(exactMaximum.saturated());

        SaturatingLong.Result overflow = SaturatingLong.increment(exactMaximum.value());
        assertEquals(Long.MAX_VALUE, overflow.value());
        assertTrue(overflow.saturated());

        SaturatingLong.Result wideOverflow = SaturatingLong.add(Long.MAX_VALUE - 4L, 8L);
        assertEquals(Long.MAX_VALUE, wideOverflow.value());
        assertTrue(wideOverflow.saturated());
    }

    @Test
    void sequenceSaturationDoesNotReuseLongMaximumAsTokenIdentity() {
        DiagnosticSessionAdmissionAuthority authority = new DiagnosticSessionAdmissionAuthority(
                "sequence-saturation-session",
                0L,
                Long.MAX_VALUE - 1L
        );

        DiagnosticAdmissionToken maximum = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.EXCEPTION_COVERAGE)).token();
        DiagnosticAdmissionToken unavailable = authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.EXCEPTION_COVERAGE)).token();

        assertTrue(maximum.sequenceAvailable());
        assertEquals(Long.MAX_VALUE, maximum.tokenSequence());
        assertFalse(unavailable.sequenceAvailable());
        assertEquals(0L, unavailable.tokenSequence());
        assertFalse(maximum == unavailable);
        assertFalse(authority.snapshot().tokenSequenceAvailable());
        assertTrue(authority.snapshot().counterSaturated());
    }

    @Test
    void suppressionCountersRemainAtMaximumAfterSaturation() {
        DiagnosticSessionAdmissionAuthority authority = new DiagnosticSessionAdmissionAuthority(
                "counter-saturation-session",
                Long.MAX_VALUE - 1L,
                0L
        );
        for (int index = 0; index < 2; index++) {
            assertTrue(authority.admit(
                    DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL)).admitted());
        }

        assertFalse(authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL)).admitted());
        assertEquals(Long.MAX_VALUE, authority.snapshot().suppressedRequests());
        assertFalse(authority.snapshot().counterSaturated());

        assertFalse(authority.admit(
                DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL)).admitted());
        assertEquals(Long.MAX_VALUE, authority.snapshot().suppressedRequests());
        assertTrue(authority.snapshot().counterSaturated());
    }
}
