package lavi.minecraft.diagnostics.container.gui.screen;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Lock canonical hub aggregate names without legacy EventBus aliases.
class ContainerScreenTransportAggregateTest {
    @Test
    void aggregateUsesCanonicalHubAndGuiListenerCountNames() {
        ContainerScreenTransportAggregate aggregate = new ContainerScreenTransportAggregate();
        aggregate.sourceObserved(10L);
        aggregate.droppedNoActiveListener(11L);
        aggregate.dispatchStarted(12L);
        aggregate.listenerStarted(12L);
        aggregate.listenerCompleted(12L);
        aggregate.listenerSkippedInactive(12L);
        aggregate.listenerClassCastFailed(12L);
        aggregate.dispatchCompleted(12L);

        Map<String, Object> fields = uniqueFields(aggregate.fields(
                null,
                "CLEAN_TEARDOWN_FINAL_SNAPSHOT",
                "LOCAL_DETAIL_EXEMPT_FINAL_SNAPSHOT_PROJECTION",
                false
        ));

        assertEquals(1, fields.get("screenTailSourceObservedCount"));
        assertEquals(1, fields.get("screenTailHubDroppedCount"));
        assertEquals(1, fields.get("screenTailHubDispatchStartedCount"));
        assertEquals(1, fields.get("screenTailHubDispatchCompletedCount"));
        assertEquals(1, fields.get("screenTailHubNoActiveListenerCount"));
        assertEquals(1, fields.get("screenTailHubInactiveAfterSnapshotSkipCount"));
        assertEquals(1, fields.get("completedGuiListenerCallbackCount"));

        assertFalse(fields.containsKey("screenTailEventBusDroppedCount"));
        assertFalse(fields.containsKey("screenTailEventBusDispatchStartedCount"));
        assertFalse(fields.containsKey("screenTailEventBusDispatchCompletedCount"));
        assertFalse(fields.containsKey("screenTailEventBusNoActiveListenerCount"));
        assertFalse(fields.containsKey("screenTailEventBusInactiveListenerSkipCount"));
        assertFalse(fields.containsKey("completedScreenEventListenerCallbackCount"));
        assertFalse(fields.containsKey("skippedInactiveScreenEventListenerCount"));
    }

    private static Map<String, Object> uniqueFields(Object[] pairs) {
        assertEquals(0, pairs.length % 2, "field arrays must contain key/value pairs");
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            String key = String.valueOf(pairs[index]);
            assertTrue(!result.containsKey(key), () -> "duplicate diagnostic key: " + key);
            result.put(key, pairs[index + 1]);
        }
        return result;
    }
}
