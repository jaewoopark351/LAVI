package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import java.util.concurrent.atomic.AtomicReference;

//20260915_kpopmodder: Transfer immutable main-thread captures to the handshake transport.
public final class FabricChatClefCatalogueSnapshotStore {
    private static final AtomicReference<FabricChatClefCatalogueSnapshot> CURRENT =
            new AtomicReference<>(FabricChatClefCatalogueSnapshot.unavailable("capture_pending"));

    private FabricChatClefCatalogueSnapshotStore() { }
    public static FabricChatClefCatalogueSnapshot current() { return CURRENT.get(); }
    public static void publish(FabricChatClefCatalogueSnapshot snapshot) { CURRENT.set(snapshot); }
}
