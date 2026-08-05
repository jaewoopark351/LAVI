package lavi.minecraft.diagnostics.command;

//20260805_kpopmodder: Let diagnostics read active command ownership without depending on the Fabric bridge.
@FunctionalInterface
public interface DiagnosticCommandContextProvider {
    DiagnosticCommandContextSnapshot snapshot();
}
