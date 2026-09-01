package lavi.minecraft.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260831_kpopmodder: Verify trace, operation, tick, and event identities never wrap or reuse MAX.
class DiagnosticTraceStateSaturationTest {
    @Test
    void countersSaturateWithoutNegativeWraparoundOrMaxIdentityReuse() {
        DiagnosticTraceState state = new DiagnosticTraceState(
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                Long.MAX_VALUE - 1L
        );

        state.advanceClientTick();
        assertEquals(Long.MAX_VALUE, state.currentClientTickId());
        assertEquals(Long.MAX_VALUE, state.nextOperationId());
        assertEquals(-1L, state.nextOperationId());
        assertEquals(Long.MAX_VALUE, state.nextEventSequence());
        assertEquals(-1L, state.nextEventSequence());

        DiagnosticEventIdentity first = state.nextEventIdentity(true);
        DiagnosticEventIdentity second = state.nextEventIdentity(true);
        assertEquals("trace-" + Long.MAX_VALUE, first.traceId());
        assertEquals("trace-unavailable-saturated", second.traceId());
        assertEquals(-1L, first.eventSequence());
        assertEquals(-1L, second.eventSequence());
    }
}
