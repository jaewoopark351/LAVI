package lavi.minecraft.diagnostics.session.lifecycle.snapshot;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

//20260831_kpopmodder: Aggregate optional lifecycle snapshot fields under one fixed bound.
public final class DiagnosticLifecycleSnapshotAggregator {
    private static final int MAX_SNAPSHOT_FIELD_OBJECTS = 256;
    private static final int META_FIELD_OBJECTS = 4;

    private final Supplier<List<DiagnosticSessionLifecycleObserver>> observers;

    public DiagnosticLifecycleSnapshotAggregator(
            Supplier<List<DiagnosticSessionLifecycleObserver>> observers) {
        this.observers = Objects.requireNonNull(observers, "observers");
    }

    public Object[] fields() {
        List<Object> observerFields = new ArrayList<>();
        int failedObserverCount = 0;
        int omittedFieldObjectCount = 0;
        for (DiagnosticSessionLifecycleObserver observer : observers.get()) {
            try {
                omittedFieldObjectCount += appendBounded(
                        observerFields,
                        observer.finalSnapshotFields(),
                        MAX_SNAPSHOT_FIELD_OBJECTS - META_FIELD_OBJECTS
                );
            } catch (RuntimeException | LinkageError ignored) {
                failedObserverCount++;
            }
        }
        List<Object> fields = new ArrayList<>(observerFields.size() + META_FIELD_OBJECTS);
        fields.add("diagnosticLifecycleObserverSnapshotFailureCount");
        fields.add(failedObserverCount);
        fields.add("diagnosticLifecycleObserverOmittedFieldObjectCount");
        fields.add(omittedFieldObjectCount);
        fields.addAll(observerFields);
        return fields.toArray();
    }

    private static int appendBounded(List<Object> target,
                                     Object[] source,
                                     int fieldObjectLimit) {
        if (source == null) {
            return 0;
        }
        int evenSourceLength = source.length - source.length % 2;
        int remaining = fieldObjectLimit - target.size();
        int retained = Math.min(evenSourceLength, Math.max(0, remaining));
        for (int index = 0; index < retained; index++) {
            target.add(source[index]);
        }
        return evenSourceLength - retained + source.length % 2;
    }
}
