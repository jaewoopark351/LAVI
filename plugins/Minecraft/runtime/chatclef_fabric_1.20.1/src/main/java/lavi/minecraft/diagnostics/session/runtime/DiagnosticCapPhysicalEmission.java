package lavi.minecraft.diagnostics.session.runtime;

//20260831_kpopmodder: Keep canonical cap payload emission outside the shared accounting lock.
@FunctionalInterface
public interface DiagnosticCapPhysicalEmission {
    void emit(DiagnosticCapEventContext context);
}
