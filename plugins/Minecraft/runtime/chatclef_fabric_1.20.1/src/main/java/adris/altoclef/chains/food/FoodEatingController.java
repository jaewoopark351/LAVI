package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import baritone.api.utils.input.Input;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

//20260727_kpopmodder: Keeps right-click eating input separate from FoodChain decisions.
public class FoodEatingController {
    private boolean tryingToEat = false;
    private boolean fillupRequested = false;

    public void startEating(AltoClef mod, Item food) {
        if (mod.getPlayer().isBlocking()) {
            mod.log("want to eat, trying to stop shielding...");
            mod.getInputControls().release(Input.CLICK_RIGHT);
            return;
        }

        tryingToEat = true;
        fillupRequested = true;
        mod.getSlotHandler().forceEquipItem(new Item[]{food}, true);
        mod.getInputControls().hold(Input.CLICK_RIGHT);
        mod.getExtraBaritoneSettings().setInteractionPaused(true);
    }

    public void stopEating() {
        if (!tryingToEat) {
            return;
        }

        AltoClef altoClef = AltoClef.getInstance();
        if (altoClef == null) {
            tryingToEat = false;
            fillupRequested = false;
            return;
        }

        if (altoClef.getItemStorage().hasItem(Items.SHIELD) || altoClef.getItemStorage().hasItemInOffhand(Items.SHIELD)) {
            if (StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT).getItem() != Items.SHIELD) {
                altoClef.getSlotHandler().forceEquipItemToOffhand(Items.SHIELD);
            } else {
                tryingToEat = false;
                fillupRequested = false;
            }
        } else {
            tryingToEat = false;
            fillupRequested = false;
        }
        altoClef.getInputControls().release(Input.CLICK_RIGHT);
        altoClef.getExtraBaritoneSettings().setInteractionPaused(false);
    }

    public void updateFillupRequest(AltoClef mod, boolean hasFood) {
        if (fillupRequested && mod.getPlayer().getHungerManager().getFoodLevel() >= 20) {
            fillupRequested = false;
        }
        if (!hasFood) {
            fillupRequested = false;
        }
    }

    public boolean isTryingToEat() {
        return tryingToEat;
    }

    public boolean isFillupRequested() {
        return fillupRequested;
    }
}
