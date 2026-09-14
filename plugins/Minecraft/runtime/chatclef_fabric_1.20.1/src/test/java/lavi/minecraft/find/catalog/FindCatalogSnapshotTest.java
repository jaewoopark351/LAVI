//#if MC == 12001
package lavi.minecraft.find.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify actual encoded catalog bounds, immutable pages and canonical Unicode digest.
class FindCatalogSnapshotTest {
    private static FindCatalogRecord record(String kind, String id, String korean) {
        return new FindCatalogRecord(kind, id, kind + ".test.name", korean, "name", kind.equals("entity") ? "unknown" : "");
    }
    @Test void digestIsDeterministicAcrossRegistryIterationAndNormalizesKoreanNfc() {
        var a = record("item", "mod:test", "가"); var b = record("block", "mod:test", "가");
        var first = FindCatalogSnapshot.create(1, List.of(a, b)); var second = FindCatalogSnapshot.create(2, List.of(b, a));
        assertTrue(first.complete()); assertEquals(first.digest(), second.digest()); assertEquals("가", a.koreanName());
        assertEquals(2, first.pages().get(0).get("record_count"));
        assertThrows(UnsupportedOperationException.class, () -> first.records().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.pages().get(0).put("complete", false));
    }
    @Test void duplicateIdentityCannotBePublishedAsComplete() {
        var first = record("item", "mod:test", "첫 이름"); var second = record("item", "mod:test", "다른 이름");
        var catalog = FindCatalogSnapshot.create(1, List.of(first, second));
        assertFalse(catalog.complete()); assertTrue(catalog.pages().isEmpty()); assertEquals("", catalog.digest());
    }
    @Test void actualJsonPageSizeAndRecordCapsAreRespectedWithEnvelopeRoom() throws Exception {
        List<FindCatalogRecord> records = new ArrayList<>();
        for (int index = 0; index < 1000; index++) records.add(record("item", "mod:long_item_" + index, "가".repeat(256)));
        var snapshot = FindCatalogSnapshot.create(1, records); assertTrue(snapshot.complete());
        ObjectMapper mapper = new ObjectMapper(); long total = 0;
        for (var page : snapshot.pages()) {
            int bytes = mapper.writeValueAsBytes(page).length; total += bytes + 1024;
            assertTrue(bytes <= 65536 - 1024); assertTrue(((List<?>) page.get("records")).size() <= 256);
        }
        assertTrue(total <= 8 * 1024 * 1024); assertTrue(snapshot.pages().size() > 1);
    }
    @Test void unicodeLimitsAreCodePointsAndControlCharactersAreRejected() {
        assertDoesNotThrow(() -> record("item", "mod:test", "😀".repeat(256)));
        assertThrows(IllegalArgumentException.class, () -> record("item", "mod:test", "😀".repeat(257)));
        assertThrows(IllegalArgumentException.class, () -> record("item", "mod:test", "line\nforged"));
        assertThrows(IllegalArgumentException.class, () -> record("item", "mod:test", "label\u202Ehidden"));
    }
    @Test void excessiveSourceRecordsProduceNoPartialPagesOrDigest() {
        var catalog = FindCatalogSnapshot.create(1, java.util.Collections.nCopies(50001, record("item", "mod:test", "name")));
        assertFalse(catalog.complete()); assertTrue(catalog.pages().isEmpty()); assertEquals("catalog_record_bound", catalog.reason());
    }
    @Test void vocabularyRejectsInvalidSurrogatesAndEmptyTranslationKeys() {
        assertThrows(IllegalArgumentException.class, () -> record("item", "mod:test", "name\uD800"));
        assertThrows(IllegalArgumentException.class, () -> record("item", "mod:test", "name\uDC00"));
        assertThrows(IllegalArgumentException.class, () -> new FindCatalogRecord("item", "mod:test", "", "이름", "Name", ""));
    }
    @Test void genuinePrivateUseAndUnassignedVocabularyIsPreservedInDigest() {
        var value = record("item", "mod:test", "모드 \uE000 \u0378");
        var catalog = FindCatalogSnapshot.create(1, List.of(value));
        assertTrue(catalog.complete()); assertEquals("모드 \uE000 \u0378", value.koreanName());
        assertTrue(value.digestLine().contains("\uE000 \u0378"));
    }
}
//#endif
