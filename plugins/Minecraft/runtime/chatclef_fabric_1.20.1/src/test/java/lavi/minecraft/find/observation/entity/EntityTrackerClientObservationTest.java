//#if MC == 12001
package lavi.minecraft.find.observation.entity;

import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.trackers.blacklisting.AbstractObjectBlacklist;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import lavi.minecraft.find.diagnostics.FindLog;
import lavi.minecraft.find.model.FindRequest;
import net.minecraft.entity.Entity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Exercise the actual additive engine getter and preserve cache and blacklist ownership.
class EntityTrackerClientObservationTest {
    private static final class RefreshGuardTracker extends EntityTracker {
        int refreshCalls;
        RefreshGuardTracker(TrackerManager manager) { super(manager); }
        @Override protected void updateState() { refreshCalls++; throw new AssertionError("Observation refreshed engine caches"); }
    }

    @Test void defaultGetterBorrowsLiveMembershipWithoutAnEagerCopyOrEngineRefresh() {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.subclassVillager(80, 0, 0);
            var tracker = new RefreshGuardTracker(new TrackerManager(fixture.mod));
            var source = tracker.getClientObservedEntities();
            assertEquals(1, fixture.world.sourceCalls);
            assertEquals(0, fixture.world.iteratorCalls); assertEquals(0, fixture.world.nextCalls);
            assertEquals(0, tracker.refreshCalls);
            var iterator = source.iterator(); assertTrue(iterator.hasNext()); assertSame(resident, iterator.next());
            assertFalse(iterator.hasNext()); assertEquals(1, fixture.world.nextCalls);
            fixture.world.members.clear();
            assertFalse(tracker.getClientObservedEntities().iterator().hasNext());
            assertEquals(0, tracker.refreshCalls);
        }
    }

    @Test void actualMovementBlacklistCannotHideAnExistingMobFromReportAndIsNotCleared() throws Exception {
        try (var fixture = new FindEntityObservationFixture()) {
            var resident = fixture.villager(80, 0, 0);
            var tracker = new RefreshGuardTracker(new TrackerManager(fixture.mod));
            blacklist(tracker, resident);
            assertFalse(tracker.isEntityReachable(resident));
            var observation = new MinecraftFindEntityObservation(fixture.client, () -> tracker);
            var request = new FindRequest("entity", "minecraft:villager", "report", "", 0);
            var result = observation.scan(request, fixture.binding(), 4096, () -> true,
                    new FindLog("blacklisted-observation", (event, fields) -> { }));
            assertTrue(result.complete()); assertEquals(1, result.matched());
            assertEquals(resident.getId(), result.nearest().entityId());
            assertFalse(tracker.isEntityReachable(resident));
            assertEquals(0, tracker.refreshCalls);
        }
    }

    @Test void defaultGetterRejectsOffThreadMissingWorldAndMissingOwnerWithoutTraversing() {
        try (var fixture = new FindEntityObservationFixture()) {
            var tracker = new RefreshGuardTracker(new TrackerManager(fixture.mod));
            fixture.client.onThread = false;
            assertThrows(IllegalStateException.class, tracker::getClientObservedEntities);
            fixture.client.onThread = true; fixture.client.world = null;
            assertThrows(IllegalStateException.class, tracker::getClientObservedEntities);
            fixture.client.world = fixture.world; fixture.client.player = null;
            assertThrows(IllegalStateException.class, tracker::getClientObservedEntities);
            fixture.client.player = fixture.self;
            var ownerless = new RefreshGuardTracker(new TrackerManager(null));
            assertThrows(IllegalStateException.class, ownerless::getClientObservedEntities);
            assertEquals(0, fixture.world.sourceCalls); assertEquals(0, fixture.world.nextCalls);
        }
    }

    @SuppressWarnings("unchecked")
    private static void blacklist(EntityTracker tracker, Entity entity) throws Exception {
        Field blacklistField = EntityTracker.class.getDeclaredField("entityBlacklist");
        blacklistField.setAccessible(true);
        Object blacklist = blacklistField.get(tracker);
        Field entriesField = AbstractObjectBlacklist.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Class<?> entryType = Class.forName("adris.altoclef.trackers.blacklisting.AbstractObjectBlacklist$BlacklistEntry");
        Constructor<?> constructor = entryType.getDeclaredConstructor(); constructor.setAccessible(true);
        Object entry = constructor.newInstance();
        Field allowed = entryType.getDeclaredField("numberOfFailuresAllowed"); allowed.setAccessible(true); allowed.setInt(entry, 0);
        Field failures = entryType.getDeclaredField("numberOfFailures"); failures.setAccessible(true); failures.setInt(entry, 1);
        ((Map<Entity, Object>) entriesField.get(blacklist)).put(entity, entry);
    }
}
//#endif
