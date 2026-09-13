package lavi.minecraft.diagnostics.observation.state;

/** Immutable plan captured before shared admission. */
public record ObservationEmission(String event, String reason, String tier, long tick, long nanos,
                                  Object[] required, Object[] optional) { }
