package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;

@FunctionalInterface
public interface DiagnosticFinalSnapshotPhysicalEmission {
    void emit(DiagnosticSessionSnapshot snapshotAfterAdmission);
}
