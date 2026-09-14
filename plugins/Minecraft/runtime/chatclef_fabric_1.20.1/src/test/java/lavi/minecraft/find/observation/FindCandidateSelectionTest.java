//#if MC == 12001
package lavi.minecraft.find.observation;

import lavi.minecraft.find.model.FindCandidate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Ranking must not depend on tracker, chunk or collection iteration order.
class FindCandidateSelectionTest {
    @Test void entityDistanceTieUsesStableUuidRatherThanEntityIdOrVisitOrder() {
        var first = new FindCandidate(1, "a", "ffffffff", 4, 1, 1, 16);
        var second = new FindCandidate(200, "b", "00000000", 4, 1, 1, 16);
        assertSame(second, FindCandidateSelection.nearer(first, second));
        assertSame(second, FindCandidateSelection.nearer(second, first));
    }
    @Test void blockDistanceTieUsesSignedLexicographicCoordinates() {
        var negative = new FindCandidate(-1, "a", "", -10, 0, 0, 100);
        var positive = new FindCandidate(-1, "b", "", 10, 0, 0, 100);
        assertSame(negative, FindCandidateSelection.nearer(positive, negative));
    }
    @Test void blockCursorUsesFloorAtNegativeOriginAndClipsWorldHeight() {
        var cursor = new FindBlockObservationCursor(-0.25, -63.5, -0.25, -64, 320);
        var first = cursor.next(); assertEquals(-33, first.x()); assertEquals(-64, first.y()); assertEquals(-33, first.z());
        int visited = 1; while (!cursor.complete()) { assertNotNull(cursor.next()); visited++; }
        assertEquals(65 * 65 * 33, visited); assertNull(cursor.next());
    }
}
//#endif
