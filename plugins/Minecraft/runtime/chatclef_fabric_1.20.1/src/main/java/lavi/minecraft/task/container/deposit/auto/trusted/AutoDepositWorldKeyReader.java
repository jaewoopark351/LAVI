package lavi.minecraft.task.container.deposit.auto.trusted;

import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.Optional;

public final class AutoDepositWorldKeyReader {
    public Optional<String> read() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return Optional.empty();
        }
        if (client.getCurrentServerEntry() != null
                && client.getCurrentServerEntry().address != null
                && !client.getCurrentServerEntry().address.isBlank()) {
            return Optional.of("multiplayer:"
                    + client.getCurrentServerEntry().address.trim().toLowerCase(Locale.ROOT));
        }
        if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
            String levelName = client.getServer().getSaveProperties().getLevelName();
            if (levelName != null && !levelName.isBlank()) {
                return Optional.of("singleplayer:" + levelName.trim());
            }
        }
        return Optional.empty();
    }
}
