package adris.altoclef.util.compat.carryon;

import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//20260727_kpopmodder: Logs Carry On state transitions once per player instead of once per tick.
public final class CarryOnStateChangeReporter {
    private final StateChangeLogger logger = new StateChangeLogger("CarryOnCompat");
    private final Map<UUID, String> lastStateByPlayer = new HashMap<>();

    public void report(PlayerEntity player, String state) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUuid();
        String previous = lastStateByPlayer.get(uuid);
        if (state.equals(previous)) {
            return;
        }
        lastStateByPlayer.put(uuid, state);
        logger.state("carry-on state " + uuid + " " + state,
                "Carry On state changed: "
                        + (previous == null ? "UNKNOWN" : previous)
                        + " -> " + state
                        + ", player=" + player.getName().getString()
                        + ", uuid=" + uuid);
    }

    public void reset() {
        lastStateByPlayer.clear();
        logger.reset();
    }
}
