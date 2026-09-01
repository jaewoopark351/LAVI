package lavi.minecraft.diagnostics.session.lifecycle.snapshot;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260831_kpopmodder: Prove optional lifecycle evidence is bounded and self-describing.
class DiagnosticLifecycleSnapshotAggregatorTest {
    @Test
    void retainsOnlyEvenPairsInsideTheFixedBoundAndReportsEveryOmission() {
        Object[] oversized = new Object[301];
        for (int index = 0; index < oversized.length; index++) {
            oversized[index] = "value-" + index;
        }
        DiagnosticLifecycleSnapshotAggregator aggregator =
                new DiagnosticLifecycleSnapshotAggregator(
                        () -> List.of(observerWith(oversized))
                );

        Object[] fields = aggregator.fields();

        assertEquals(256, fields.length);
        assertEquals("0", field(fields,
                "diagnosticLifecycleObserverSnapshotFailureCount"));
        assertEquals("49", field(fields,
                "diagnosticLifecycleObserverOmittedFieldObjectCount"));
        assertEquals("value-0", fields[4]);
        assertEquals("value-251", fields[255]);
    }

    @Test
    void oneFailingObserverDoesNotHideFieldsFromTheNextObserver() {
        DiagnosticSessionLifecycleObserver failing =
                new DiagnosticSessionLifecycleObserver() {
                    @Override
                    public Object[] finalSnapshotFields() {
                        throw new IllegalStateException("fixture");
                    }
                };
        DiagnosticLifecycleSnapshotAggregator aggregator =
                new DiagnosticLifecycleSnapshotAggregator(
                        () -> List.of(failing, observerWith(new Object[]{
                                "survivingField", "present"
                        }))
                );

        Object[] fields = aggregator.fields();

        assertEquals("1", field(fields,
                "diagnosticLifecycleObserverSnapshotFailureCount"));
        assertEquals("present", field(fields, "survivingField"));
    }

    private static DiagnosticSessionLifecycleObserver observerWith(Object[] fields) {
        return new DiagnosticSessionLifecycleObserver() {
            @Override
            public Object[] finalSnapshotFields() {
                return fields;
            }
        };
    }

    private static String field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return String.valueOf(fields[index + 1]);
            }
        }
        return "MISSING";
    }
}
