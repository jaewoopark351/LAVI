package lavi.minecraft.diagnostics.session.emission;

@FunctionalInterface
public interface DiagnosticPhysicalSink {
    void emit(String encodedRecord);
}
