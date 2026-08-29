package lavi.minecraft.task.container.home.execution;

import lavi.minecraft.task.container.home.execution.slot.HomeStorageScreenSlotView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260827_kpopmodder: Added focused tests for handler-independent logical slot resolution.
class HomeStorageScreenSlotResolverTest {
    private final HomeStorageScreenSlotResolver resolver =
            new HomeStorageScreenSlotResolver();

    @Test
    void resolvesLogicalPlayerSlotWithoutAssumingContainerOffset() {
        List<HomeStorageScreenSlotView> slots = List.of(
                new HomeStorageScreenSlotView(0, false, 0),
                new HomeStorageScreenSlotView(26, false, 26),
                new HomeStorageScreenSlotView(54, true, 9),
                new HomeStorageScreenSlotView(81, true, 0)
        );

        assertEquals(81, resolver.findUnique(slots, 0).orElseThrow());
        assertEquals(54, resolver.findUnique(slots, 9).orElseThrow());
    }

    @Test
    void refusesMissingOrAmbiguousLogicalMapping() {
        List<HomeStorageScreenSlotView> duplicate = List.of(
                new HomeStorageScreenSlotView(27, true, 0),
                new HomeStorageScreenSlotView(63, true, 0)
        );

        assertTrue(resolver.findUnique(duplicate, 0).isEmpty());
        assertTrue(resolver.findUnique(List.of(), 0).isEmpty());
    }
}
