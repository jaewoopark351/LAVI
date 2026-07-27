package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.SmokerSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmokerScreenHandler;

import java.util.Objects;

//20260727_kpopmodder: Keeps food potential scoring separate from food task control flow.
public final class FoodPotentialCalculator {
    private FoodPotentialCalculator() {
    }

    public static double getFoodPotential(ItemStack food, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        if (food == null) return 0;
        int count = food.getCount();
        if (count <= 0) return 0;
        for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
            if (food.getItem() == cookable.getRaw()) {
                assert ItemVer.getFoodComponent(cookable.getCooked()) != null;
                return count * ItemVer.getFoodComponent(cookable.getCooked()).getHunger();
            }
        }

        assert ItemVer.getFoodComponent(Items.BREAD) != null;

        if (food.getItem().equals(Items.HAY_BLOCK)) {
            return 3 * ItemVer.getFoodComponent(Items.BREAD).getHunger() * count;
        }
        if (food.getItem().equals(Items.WHEAT)) {
            return (double) (ItemVer.getFoodComponent(Items.BREAD).getHunger() * count) / 3;
        }

        if (ItemVer.isFood(food.getItem())) {
            assert ItemVer.getFoodComponent(food.getItem()) != null;
            return count * ItemVer.getFoodComponent(food.getItem()).getHunger();
        }
        return 0;
    }

    @SuppressWarnings("RedundantCast")
    public static double calculateFoodPotential(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        double potentialFood = 0;
        for (ItemStack food : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
            potentialFood += getFoodPotential(food, cookableFoods);
        }
        int potentialBread = (int) (mod.getItemStorage().getItemCount(Items.WHEAT) / 3) + mod.getItemStorage().getItemCount(Items.HAY_BLOCK) * 3;
        potentialFood += Objects.requireNonNull(ItemVer.getFoodComponent(Items.BREAD)).getHunger() * potentialBread;

        ScreenHandler screen = mod.getPlayer().currentScreenHandler;
        if (screen instanceof SmokerScreenHandler) {
            potentialFood += getFoodPotential(StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS), cookableFoods);
            potentialFood += getFoodPotential(StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT), cookableFoods);
        }
        return potentialFood;
    }
}
