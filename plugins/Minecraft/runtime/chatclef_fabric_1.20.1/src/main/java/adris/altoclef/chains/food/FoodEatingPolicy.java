package adris.altoclef.chains.food;

import adris.altoclef.multiversion.item.ItemVer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;

import java.util.Optional;

//20260727_kpopmodder: Isolates the hunger/health rules that decide whether eating should start.
public class FoodEatingPolicy {
    public boolean needsToEat(boolean hasFood, boolean shouldStop, Optional<Item> cachedBestFood, FoodChainConfig config) {
        if (!hasFood || shouldStop) {
            return false;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        int foodLevel = player.getHungerManager().getFoodLevel();
        float health = player.getHealth();

        if (foodLevel >= 20) {
            return false;
        }

        if (health <= 10) {
            return true;
        }

        if (player.isOnFire() || player.hasStatusEffect(StatusEffects.WITHER) || health < config.alwaysEatWhenWitherOrFireAndHealthBelow) {
            return true;
        } else if (foodLevel > config.alwaysEatWhenBelowHunger) {
            if (health < config.alwaysEatWhenBelowHealth) {
                return true;
            }
        } else {
            return true;
        }

        if (foodLevel < config.alwaysEatWhenBelowHungerAndPerfectFit && cachedBestFood.isPresent()) {
            int need = 20 - foodLevel;
            Item best = cachedBestFood.get();

            int fills = (ItemVer.getFoodComponent(best) != null) ? ItemVer.getFoodComponent(best).getHunger() : -1;
            return fills == need;
        }

        return false;
    }
}
