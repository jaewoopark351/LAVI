package adris.altoclef.lavibridge.state;

//20260725_kpopmodder: Added this reader for Minecraft player state snapshots.

import adris.altoclef.AltoClef;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviPlayerStateReader {

    private final AltoClef mod;

    public LaviPlayerStateReader(AltoClef mod) {
        this.mod = mod;
    }

    public Map<String, Object> playerStatus() {
        Map<String, Object> player = new LinkedHashMap<>();
        if (!AltoClef.inGame() || mod.getPlayer() == null || mod.getWorld() == null) {
            player.put("available", false);
            return player;
        }

        ClientPlayerEntity entity = mod.getPlayer();
        BlockPos blockPos = entity.getBlockPos();

        player.put("available", true);
        player.put("name", entity.getName().getString());
        player.put("health", entity.getHealth());
        player.put("food", entity.getHungerManager().getFoodLevel());
        player.put("saturation", entity.getHungerManager().getSaturationLevel());
        player.put("air", entity.getAir());
        player.put("dimension", mod.getWorld().getRegistryKey().getValue().toString());
        player.put("position", Map.of(
                "x", entity.getX(),
                "y", entity.getY(),
                "z", entity.getZ(),
                "block_x", blockPos.getX(),
                "block_y", blockPos.getY(),
                "block_z", blockPos.getZ()
        ));
        player.put("creative", entity.isCreative());
        return player;
    }
}
