package lavi.minecraft.testsupport;

import net.minecraft.item.Item;
import org.opentest4j.TestAbortedException;

public final class TestItems {
    private TestItems() {
    }

    public static Item item() {
        // Newer Minecraft versions validate item registry bootstrap in the constructor.
        // These unit tests only need distinct Item identities as map keys and target matches.
        try {
            return TestObjects.allocateBootstrapped(Item.class);
        } catch (Throwable throwable) {
            throw new TestAbortedException(
                    "This Minecraft test runtime cannot allocate registry-free Item identities.",
                    throwable
            );
        }
    }
}
