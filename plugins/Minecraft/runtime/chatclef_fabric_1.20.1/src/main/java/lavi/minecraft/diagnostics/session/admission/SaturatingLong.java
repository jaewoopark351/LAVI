package lavi.minecraft.diagnostics.session.admission;

public final class SaturatingLong {
    private SaturatingLong() {
    }

    public static Result increment(long current) {
        return add(current, 1L);
    }

    public static Result add(long current, long delta) {
        if (current < 0L || delta < 0L) {
            throw new IllegalArgumentException("Saturating diagnostic counters must be non-negative.");
        }
        if (delta > 0L && current > Long.MAX_VALUE - delta) {
            return new Result(Long.MAX_VALUE, true);
        }
        return new Result(current + delta, false);
    }

    public record Result(long value, boolean saturated) {
    }
}
