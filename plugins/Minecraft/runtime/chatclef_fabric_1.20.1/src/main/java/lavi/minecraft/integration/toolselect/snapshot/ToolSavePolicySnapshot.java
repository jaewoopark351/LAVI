package lavi.minecraft.integration.toolselect.snapshot;

//20260805_kpopmodder: Carry immutable tool-save facts from the client thread to Baritone worker code.
public record ToolSavePolicySnapshot(
        long generation,
        long publishedClientTick,
        boolean ready,
        boolean hasDiamondPickaxe,
        String status,
        String worldKey,
        String playerKey
) {
    public static ToolSavePolicySnapshot ready(long generation,
                                               long publishedClientTick,
                                               boolean hasDiamondPickaxe,
                                               String worldKey,
                                               String playerKey) {
        return new ToolSavePolicySnapshot(
                generation,
                publishedClientTick,
                true,
                hasDiamondPickaxe,
                "READY",
                worldKey,
                playerKey
        );
    }

    public static ToolSavePolicySnapshot unavailable(long generation,
                                                     long publishedClientTick,
                                                     String status,
                                                     String worldKey,
                                                     String playerKey) {
        return new ToolSavePolicySnapshot(
                generation,
                publishedClientTick,
                false,
                false,
                status,
                worldKey,
                playerKey
        );
    }
}
