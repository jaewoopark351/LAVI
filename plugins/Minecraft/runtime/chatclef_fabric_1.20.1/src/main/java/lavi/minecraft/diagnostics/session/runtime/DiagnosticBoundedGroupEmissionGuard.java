package lavi.minecraft.diagnostics.session.runtime;

import java.util.Objects;

//20260831_kpopmodder: Enforce that one admitted terminal group projects exactly its slot count.
public final class DiagnosticBoundedGroupEmissionGuard {
    private final int expectedRecordCount;
    private int returnedRecordCount;

    public DiagnosticBoundedGroupEmissionGuard(int expectedRecordCount) {
        if (expectedRecordCount < 1) {
            throw new IllegalArgumentException("expectedRecordCount must be positive");
        }
        this.expectedRecordCount = expectedRecordCount;
    }

    public DiagnosticBoundedGroupEmitter guard(DiagnosticBoundedGroupEmitter delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return (eventName, reason, task, maxUtf8Bytes, requiredFields, optionalFields) -> {
            if (returnedRecordCount >= expectedRecordCount) {
                throw new IllegalStateException(
                        "A bounded group attempted more physical records than its admission unit."
                );
            }
            delegate.emit(
                    eventName,
                    reason,
                    task,
                    maxUtf8Bytes,
                    requiredFields,
                    optionalFields
            );
            returnedRecordCount++;
        };
    }

    public void verifyComplete() {
        if (returnedRecordCount != expectedRecordCount) {
            throw new IllegalStateException(
                    "A bounded group returned "
                            + returnedRecordCount
                            + " physical records for an admission unit of "
                            + expectedRecordCount
                            + "."
            );
        }
    }

    public int returnedRecordCount() {
        return returnedRecordCount;
    }
}
