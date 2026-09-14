//#if MC == 12001
package lavi.minecraft.find.catalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Real bounded streams close on valid, invalid and overflow resources; pack priority stays explicit.
class FindTranslationResourcesTest {
    private static Resource resource(String json, AtomicInteger closed) {
        return new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)) {
            @Override public void close() throws IOException { closed.incrementAndGet(); super.close(); }
        });
    }
    private record Manager(List<Resource> resources) implements ResourceManager {
        @Override public Set<String> getAllNamespaces() { return Set.of("minecraft"); }
        @Override public List<Resource> getAllResources(Identifier id) { return resources; }
        @Override public Optional<Resource> getResource(Identifier id) { return resources.stream().findFirst(); }
        @Override public Map<Identifier, Resource> findResources(String path, Predicate<Identifier> filter) { return Map.of(); }
        @Override public Map<Identifier, List<Resource>> findAllResources(String path, Predicate<Identifier> filter) { return Map.of(); }
        @Override public Stream<ResourcePack> streamResourcePacks() { return Stream.empty(); }
    }
    @Test void explicitKoResourcesMergeLowToHighPackPriorityAndIgnoreUnrelatedKeys() throws Exception {
        AtomicInteger closed = new AtomicInteger(); Manager manager = new Manager(List.of(
                resource("{\"entity.test\":\"낮은 이름\",\"menu.unrelated\":12}", closed),
                resource("{\"entity.test\":\"높은 이름\"}", closed)));
        var labels = FindTranslationResources.read(manager, "ko_kr", Set.of("entity.test"), new int[]{4096});
        assertEquals(Map.of("entity.test", "높은 이름"), labels); assertEquals(2, closed.get());
    }
    @Test void invalidUsedTranslationRejectsSnapshotAndStillClosesStream() {
        AtomicInteger closed = new AtomicInteger(); Manager manager = new Manager(List.of(resource("{\"entity.test\":12}", closed)));
        assertThrows(IOException.class, () -> FindTranslationResources.read(manager, "ko_kr", Set.of("entity.test"), new int[]{4096}));
        assertEquals(1, closed.get());
    }
    @Test void sourceByteOverflowCannotBeMisrepresentedAsComplete() {
        AtomicInteger closed = new AtomicInteger(); Manager manager = new Manager(List.of(resource("{\"entity.test\":\"이름\"}", closed)));
        assertThrows(IOException.class, () -> FindTranslationResources.read(manager, "ko_kr", Set.of("entity.test"), new int[]{4}));
        assertEquals(1, closed.get());
    }
}
//#endif
