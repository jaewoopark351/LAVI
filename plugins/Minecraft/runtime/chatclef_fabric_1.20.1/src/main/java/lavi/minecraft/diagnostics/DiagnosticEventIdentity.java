package lavi.minecraft.diagnostics;

//20260731_kpopmodder: Carry immutable trace/tick/event ids between the trace state owner and log emitters.
record DiagnosticEventIdentity(String traceId, long clientTickId, long eventSequence) {
}
