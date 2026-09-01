package lavi.minecraft.diagnostics.session.runtime;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosticBoundedGroupEmissionGuardTest {
    @Test
    void acceptsExactlyFourReturnedPhysicalRecords() {
        DiagnosticBoundedGroupEmissionGuard guard =
                new DiagnosticBoundedGroupEmissionGuard(4);
        AtomicInteger physicalCalls = new AtomicInteger();
        DiagnosticBoundedGroupEmitter emitter = guard.guard(emitter(physicalCalls));

        emit(emitter);
        emit(emitter);
        emit(emitter);
        emit(emitter);
        guard.verifyComplete();

        assertEquals(4, physicalCalls.get());
        assertEquals(4, guard.returnedRecordCount());
    }

    @Test
    void rejectsZeroOrThreeReturnedRecordsAsIncomplete() {
        DiagnosticBoundedGroupEmissionGuard zero =
                new DiagnosticBoundedGroupEmissionGuard(4);
        DiagnosticBoundedGroupEmissionGuard three =
                new DiagnosticBoundedGroupEmissionGuard(4);
        DiagnosticBoundedGroupEmitter emitter = three.guard(emitter(new AtomicInteger()));
        emit(emitter);
        emit(emitter);
        emit(emitter);

        assertThrows(IllegalStateException.class, zero::verifyComplete);
        assertThrows(IllegalStateException.class, three::verifyComplete);
    }

    @Test
    void rejectsTheFifthCallBeforeItTouchesThePhysicalEmitter() {
        DiagnosticBoundedGroupEmissionGuard guard =
                new DiagnosticBoundedGroupEmissionGuard(4);
        AtomicInteger physicalCalls = new AtomicInteger();
        DiagnosticBoundedGroupEmitter emitter = guard.guard(emitter(physicalCalls));
        for (int index = 0; index < 4; index++) {
            emit(emitter);
        }

        assertThrows(IllegalStateException.class, () -> emit(emitter));
        assertEquals(4, physicalCalls.get());
        guard.verifyComplete();
    }

    @Test
    void physicalFailureDoesNotCountAsAReturnedRecord() {
        DiagnosticBoundedGroupEmissionGuard guard =
                new DiagnosticBoundedGroupEmissionGuard(4);
        AtomicInteger calls = new AtomicInteger();
        DiagnosticBoundedGroupEmitter emitter = guard.guard(
                (eventName, reason, task, maxUtf8Bytes, requiredFields, optionalFields) -> {
                    if (calls.incrementAndGet() == 2) {
                        throw new IllegalStateException("sink failed");
                    }
                }
        );

        emit(emitter);
        assertThrows(IllegalStateException.class, () -> emit(emitter));
        assertEquals(1, guard.returnedRecordCount());
        assertThrows(IllegalStateException.class, guard::verifyComplete);
    }

    private static DiagnosticBoundedGroupEmitter emitter(AtomicInteger calls) {
        return (eventName, reason, task, maxUtf8Bytes, requiredFields, optionalFields) ->
                calls.incrementAndGet();
    }

    private static void emit(DiagnosticBoundedGroupEmitter emitter) {
        emitter.emit("EVENT", "reason", null, 1024, new Object[0], new Object[0]);
    }
}
