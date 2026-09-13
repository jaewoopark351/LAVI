package lavi.minecraft.diagnostics.baritone.builder;

import baritone.api.pathing.calc.IPath;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BuilderPathSnapshotTest {
    @Test void rawCountsNeverCallPublicMovementsOrConflateVerifiedWithAssemblyReturn() {
        IPath raw = (IPath) Proxy.newProxyInstance(IPath.class.getClassLoader(),
                new Class<?>[]{IPath.class, BuilderPathView.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lavi$positionsCount" -> 1;
                    case "lavi$movementsCount" -> 0;
                    case "lavi$verified" -> true;
                    default -> throw new AssertionError("unexpected getter " + method.getName());
                });
        BuilderPathSnapshot snapshot = BuilderPathSnapshot.capture(raw);
        assertEquals(1, snapshot.positions()); assertEquals(0, snapshot.movements());
        assertEquals("RAW_FIELDS", snapshot.lookup()); assertEquals("true", snapshot.verified());
    }
    @Test void unavailableGetterIsNotReportedAsAnEmptyList() {
        IPath unknown = (IPath) Proxy.newProxyInstance(IPath.class.getClassLoader(), new Class<?>[]{IPath.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("positions")) return List.of();
                    throw new IllegalStateException("not verified");
                });
        BuilderPathSnapshot snapshot = BuilderPathSnapshot.capture(unknown);
        assertEquals(-1, snapshot.movements()); assertEquals("MOVEMENTS_UNAVAILABLE", snapshot.lookup());
    }
    @Test void rangeClassificationSeparatesUnavailableNegativeEmptyAndValid() {
        assertEquals("MOVEMENT_COUNT_UNAVAILABLE", BuilderPathSnapshot.accessReason(0, -1));
        assertEquals("MOVEMENT_INDEX_OUT_OF_RANGE", BuilderPathSnapshot.accessReason(0, 0));
        assertEquals("MOVEMENT_INDEX_OUT_OF_RANGE", BuilderPathSnapshot.accessReason(-1, 3));
        assertEquals("MOVEMENT_INDEX_OUT_OF_RANGE", BuilderPathSnapshot.accessReason(3, 3));
        assertEquals("MOVEMENT_INDEX_IN_RANGE", BuilderPathSnapshot.accessReason(2, 3));
    }
}
