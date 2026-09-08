package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//20260907_kpopmodder: Supply deterministic catalogue resolutions without bootstrapping a game JVM.
public final class FabricChatClefGetItemTestProfiles {
    private FabricChatClefGetItemTestProfiles() {
    }

    static FabricChatClefGetItemEffectProfile profile(String command) {
        return FabricChatClefGetItemEffectProfileDecoder.decode(
                command,
                FabricChatClefGetItemTestProfiles::resolve
        );
    }

    static FabricChatClefGetItemEffectTracker tracker(
            String command,
            FabricChatClefGetItemTargetCountReader reader
    ) {
        return FabricChatClefGetItemEffectTracker.withReader(
                profile(command),
                reader
        );
    }

    public static FabricChatClefCommandEffectTracker authoritativeTracker(
            String command,
            int beforeCount,
            int afterCount
    ) {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        return tracker(
                command,
                () -> FabricChatClefGetItemCountObservation.authoritative(
                        reads.getAndIncrement() == 0 ? beforeCount : afterCount,
                        world,
                        player
                )
        );
    }

    private static FabricChatClefGetItemCatalogueResolution resolve(String target) {
        return switch (target) {
            case "diamond_pickaxe" -> resolution("minecraft:diamond_pickaxe");
            case "iron_pickaxe" -> resolution("minecraft:iron_pickaxe");
            case "oak_log" -> resolution("minecraft:oak_log");
            case "log" -> FabricChatClefGetItemCatalogueResolution.resolved(
                    List.of(
                            "minecraft:spruce_log",
                            "minecraft:oak_log",
                            "minecraft:oak_log"
                    )
            );
            default -> FabricChatClefGetItemCatalogueResolution.unavailable();
        };
    }

    private static FabricChatClefGetItemCatalogueResolution resolution(String id) {
        return FabricChatClefGetItemCatalogueResolution.resolved(
                List.of(id)
        );
    }
}
