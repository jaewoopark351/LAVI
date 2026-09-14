package lavi.minecraft.test.bootstrap;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

//20260914_kpopmodder: Fail the isolated verification before JUnit if real registry bootstrap cannot run.
public final class MinecraftBootstrapProbe {
    private MinecraftBootstrapProbe() { }

    public static void main(String[] arguments) {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        if (Registries.ITEM.getRawId(Items.AIR) != 0 || Registries.ITEM.getRawId(Items.DIAMOND) <= 0) {
            throw new IllegalStateException("Minecraft item registry did not bootstrap");
        }
        System.out.println("AUTO_DEPOSIT_MINECRAFT_BOOTSTRAP: PASS items=" + Registries.ITEM.size());
    }
}
