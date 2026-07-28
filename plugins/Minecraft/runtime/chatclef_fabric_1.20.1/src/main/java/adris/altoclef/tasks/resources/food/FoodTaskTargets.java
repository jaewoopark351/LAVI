package adris.altoclef.tasks.resources.food;

import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

//20260728_kpopmodder: Keep food target definitions separate from CollectFoodTask's runtime loop.
public final class FoodTaskTargets {
    private FoodTaskTargets() {
    }

    // Represents order of preferred mobs to least preferred
    public static final CollectFoodTask.CookableFoodTarget[] COOKABLE_FOODS = new CollectFoodTask.CookableFoodTarget[]{
            new CollectFoodTask.CookableFoodTarget("beef", CowEntity.class),
            new CollectFoodTask.CookableFoodTarget("porkchop", PigEntity.class),
            new CollectFoodTask.CookableFoodTarget("chicken", ChickenEntity.class),
            new CollectFoodTask.CookableFoodTarget("mutton", SheepEntity.class),
            new CollectFoodTask.CookableFoodTarget("rabbit", RabbitEntity.class)
    };

    public static final Item[] ITEMS_TO_PICK_UP = new Item[]{
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.GOLDEN_CARROT,
            Items.BREAD,
            Items.BAKED_POTATO
    };

    public static final CollectFoodTask.CropTarget[] CROPS = new CollectFoodTask.CropTarget[]{
            new CollectFoodTask.CropTarget(Items.WHEAT, Blocks.WHEAT),
            new CollectFoodTask.CropTarget(Items.CARROT, Blocks.CARROTS)
    };
}
