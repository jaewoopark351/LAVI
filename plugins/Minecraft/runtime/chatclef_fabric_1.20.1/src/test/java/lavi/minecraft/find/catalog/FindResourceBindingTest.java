//#if MC == 12001
package lavi.minecraft.find.catalog;

import java.util.List;
import lavi.minecraft.find.model.FindRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: A translated request cannot keep publishing against a replaced resource cohort.
class FindResourceBindingTest {
    private static FindCatalogSnapshot catalog(long generation, String label) {
        return FindCatalogSnapshot.create(generation, List.of(new FindCatalogRecord(
                "block", "minecraft:chest", "block.minecraft.chest", label, "Chest", "")));
    }
    @Test void reloadInvalidatesEvenAnIdenticalCatalogDigest() {
        var admitted = catalog(1, "상자"); var reloaded = catalog(2, "상자");
        var request = new FindRequest("block", "minecraft:chest", "report", admitted.digest(), 1);
        assertEquals(admitted.digest(), reloaded.digest());
        assertTrue(FindResourceBinding.matches(request, admitted));
        assertFalse(FindResourceBinding.matches(request, reloaded));
        assertFalse(FindResourceBinding.matches(request, null));
    }
    @Test void incompleteOrChangedVocabularyCannotProvideFinalBindingEvidence() {
        var admitted = catalog(1, "상자");
        var request = new FindRequest("block", "minecraft:chest", "report", admitted.digest(), 1);
        assertFalse(FindResourceBinding.matches(request, catalog(1, "보관 상자")));
        assertFalse(FindResourceBinding.matches(request, FindCatalogSnapshot.incomplete(1, "capture_failed")));
    }
    @Test void nativeCanonicalAndExactPlayerRequestsDoNotNeedCatalogAvailability() {
        assertTrue(FindResourceBinding.currentMatches(new FindRequest("block", "minecraft:chest", "report", "", 0)));
        assertTrue(FindResourceBinding.currentMatches(new FindRequest("player", "Alex", "report", "", 0)));
        assertFalse(FindResourceBinding.currentMatches(null));
    }
}
//#endif
