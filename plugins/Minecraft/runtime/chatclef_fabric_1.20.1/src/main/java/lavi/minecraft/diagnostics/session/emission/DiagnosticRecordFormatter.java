package lavi.minecraft.diagnostics.session.emission;

@FunctionalInterface
public interface DiagnosticRecordFormatter<T> {
    String format(T record);
}
