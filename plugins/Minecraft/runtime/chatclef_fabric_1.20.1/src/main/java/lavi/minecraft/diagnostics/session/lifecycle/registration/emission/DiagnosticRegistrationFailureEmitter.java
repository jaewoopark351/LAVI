package lavi.minecraft.diagnostics.session.lifecycle.registration.emission;

import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationResult;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

//20260913_kpopmodder: Bound emergency attempts independently of the failed diagnostic runtime.
public final class DiagnosticRegistrationFailureEmitter {
    public static final int MAX_FAILURE_ATTEMPTS = 32;
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger callsReturned = new AtomicInteger();
    private final AtomicInteger emissionFailures = new AtomicInteger();
    private final Consumer<String> sink;

    public DiagnosticRegistrationFailureEmitter(Consumer<String> sink) {
        this.sink = sink;
    }

    public void report(String owner, DiagnosticObserverRegistrationResult result, String boundary, String reason) {
        int current;
        do {
            current = attempts.get();
            if (current >= MAX_FAILURE_ATTEMPTS) {
                return;
            }
        } while (!attempts.compareAndSet(current, current + 1));
        try {
            sink.accept(DiagnosticRegistrationFailureFormatter.format(owner, result, boundary, reason));
            callsReturned.incrementAndGet();
        } catch (RuntimeException | LinkageError ignored) {
            emissionFailures.incrementAndGet();
        }
    }

    public int attemptedCount() { return attempts.get(); }
    public int callsReturnedCount() { return callsReturned.get(); }
    public int emissionFailureCount() { return emissionFailures.get(); }
}
