package lavi.minecraft.diagnostics.mining.baritone.executor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Keep executor unchanged-state summaries at 200 ticks or ten seconds.
class BaritoneExecutorSamplingPolicyTest {
    @Test
    void heartbeatUsesTickOrMonotonicFallbackAndRestartsAfterProgress() {
        assertFalse(BaritoneExecutorSamplingPolicy.heartbeatDue(
                199, 199, 0, TimeUnit.MILLISECONDS.toNanos(9_999), 0, 0));
        assertTrue(BaritoneExecutorSamplingPolicy.heartbeatDue(
                200, 200, 0, TimeUnit.MILLISECONDS.toNanos(9_999), 0, 0));
        assertTrue(BaritoneExecutorSamplingPolicy.heartbeatDue(
                100, 100, 0, TimeUnit.SECONDS.toNanos(10), 0, 0));

        long progressAt = TimeUnit.SECONDS.toNanos(9);
        assertFalse(BaritoneExecutorSamplingPolicy.heartbeatDue(
                50, 250, 200, TimeUnit.SECONDS.toNanos(10), TimeUnit.SECONDS.toNanos(10), progressAt));
        assertTrue(BaritoneExecutorSamplingPolicy.heartbeatDue(
                200, 400, 200, TimeUnit.SECONDS.toNanos(19), TimeUnit.SECONDS.toNanos(10), progressAt));
    }
}
