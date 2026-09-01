package lavi.minecraft.diagnostics.session.runtime;

//20260831_kpopmodder: Represent one already-admitted physical diagnostic emission action.
@FunctionalInterface
public interface DiagnosticPhysicalEmission {
    void emit();
}
