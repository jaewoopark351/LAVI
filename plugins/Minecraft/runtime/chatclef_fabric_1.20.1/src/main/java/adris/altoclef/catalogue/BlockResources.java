package adris.altoclef.catalogue;

import adris.altoclef.catalogue.TaskCatalogue.CataloguedResource;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.multiversion.versionedfields.Entities;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.resources.*;
import adris.altoclef.tasks.resources.wood.*;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Items;

//20260728_kpopmodder: Split catalogue registrations by responsibility to keep TaskCatalogue maintainable.
@SuppressWarnings({"rawtypes"})
final class BlockResources extends TaskRegistrar {
    private BlockResources() {
    }

    static void register() {
        String p = "planks";
        String s = "stick";
        String o = null;

        /// RAW RESOURCES
        mine("log", MiningRequirement.HAND, ItemHelper.LOG, ItemHelper.LOG).anyDimension();
        woodTasks("log", wood -> wood.log, (wood, count) -> new MineAndCollectTask(wood.log, count, new Block[]{Block.getBlockFromItem(wood.log)}, MiningRequirement.HAND), true);
        mine("dirt", MiningRequirement.HAND, new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH}, Items.DIRT);
        simple("cobblestone", Items.COBBLESTONE, CollectBlockByOneTask.CollectCobblestoneTask::new).dontMineIfPresent();
        simple("cobbled_deepslate", Items.COBBLED_DEEPSLATE, CollectBlockByOneTask.CollectCobbledDeepslateTask::new).dontMineIfPresent();
        mine("andesite", MiningRequirement.WOOD, Blocks.ANDESITE, Items.ANDESITE);
        mine("granite", MiningRequirement.WOOD, Blocks.GRANITE, Items.GRANITE);
        mine("diorite", MiningRequirement.WOOD, Blocks.DIORITE, Items.DIORITE);
        mine("calcite", MiningRequirement.WOOD, Blocks.CALCITE, Items.CALCITE);
        mine("tuff", MiningRequirement.WOOD, Blocks.TUFF, Items.TUFF);
        mine("netherrack", MiningRequirement.WOOD, Blocks.NETHERRACK, Items.NETHERRACK).forceDimension(Dimension.NETHER);
        mine("magma_block", MiningRequirement.WOOD, Blocks.MAGMA_BLOCK, Items.MAGMA_BLOCK).forceDimension(Dimension.NETHER);
        mine("blackstone", MiningRequirement.WOOD, Blocks.BLACKSTONE, Items.BLACKSTONE).forceDimension(Dimension.NETHER);
        mine("basalt", MiningRequirement.WOOD, Blocks.BASALT, Items.BASALT).forceDimension(Dimension.NETHER);
        mine("soul_sand", Items.SOUL_SAND).forceDimension(Dimension.NETHER);
        mine("soul_soil", Items.SOUL_SOIL).forceDimension(Dimension.NETHER);
        mine("glowstone_dust", Blocks.GLOWSTONE, Items.GLOWSTONE_DUST).forceDimension(Dimension.NETHER);
        mine("coal", MiningRequirement.WOOD, new Block[]{Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE}, Items.COAL);
        mine("raw_iron", MiningRequirement.STONE, new Block[]{Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE}, Items.RAW_IRON);
        mine("raw_gold", MiningRequirement.IRON, new Block[]{Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE}, Items.RAW_GOLD);
        mine("raw_copper", MiningRequirement.STONE, new Block[]{Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE}, Items.RAW_COPPER);
        mine("diamond", MiningRequirement.IRON, new Block[]{Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE}, Items.DIAMOND);
        mine("emerald", MiningRequirement.IRON, new Block[]{Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE}, Items.EMERALD);
        mine("redstone", MiningRequirement.IRON, new Block[]{Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE}, Items.REDSTONE);
        mine("lapis_lazuli", MiningRequirement.STONE, new Block[]{Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE}, Items.LAPIS_LAZULI);
        alias("lapis", "lapis_lazuli");
        mine("amethyst_shard", MiningRequirement.WOOD, Blocks.AMETHYST_CLUSTER, Items.AMETHYST_SHARD);
        mine("pointed_dripstone", MiningRequirement.WOOD, Blocks.POINTED_DRIPSTONE, Items.POINTED_DRIPSTONE);
        mine("sand", Blocks.SAND, Items.SAND);
        mine("red_sand", Blocks.RED_SAND, Items.RED_SAND);
        mine("gravel", Blocks.GRAVEL, Items.GRAVEL);
        mine("clay_ball", Blocks.CLAY, Items.CLAY_BALL);
        mine("ancient_debris", MiningRequirement.DIAMOND, Blocks.ANCIENT_DEBRIS, Items.ANCIENT_DEBRIS).forceDimension(Dimension.NETHER);
        mine("gilded_blackstone", MiningRequirement.STONE, Blocks.GILDED_BLACKSTONE, Items.GILDED_BLACKSTONE).forceDimension(Dimension.NETHER);
        mine("oak_sapling", Blocks.OAK_LEAVES, Items.OAK_SAPLING);
        mine("spruce_sapling", Blocks.SPRUCE_LEAVES, Items.SPRUCE_SAPLING);
        mine("birch_sapling", Blocks.BIRCH_LEAVES, Items.BIRCH_SAPLING);
        mine("jungle_sapling", Blocks.JUNGLE_LEAVES, Items.JUNGLE_SAPLING);
        mine("acacia_sapling", Blocks.ACACIA_LEAVES, Items.ACACIA_SAPLING);
        mine("dark_oak_sapling", Blocks.DARK_OAK_LEAVES, Items.DARK_OAK_SAPLING);
        mine("mangrove_propagule", Blocks.MANGROVE_PROPAGULE, Items.MANGROVE_PROPAGULE);
        mine("cherry_sapling", Blocks.CHERRY_LEAVES, Items.CHERRY_SAPLING);
        simple("sapling", ItemHelper.SAPLINGS, CollectSaplingsTask::new);
        simple("sandstone", Items.SANDSTONE, CollectSandstoneTask::new).dontMineIfPresent();
        simple("red_sandstone", Items.RED_SANDSTONE, CollectRedSandstoneTask::new).dontMineIfPresent();
        simple("coarse_dirt", Items.COARSE_DIRT, CollectCoarseDirtTask::new).dontMineIfPresent();
        simple("amethyst_block", Items.AMETHYST_BLOCK, CollectAmethystBlockTask::new).dontMineIfPresent();
        simple("dripstone_block", Items.DRIPSTONE_BLOCK, CollectDripstoneBlockTask::new).dontMineIfPresent();
        simple("flint", Items.FLINT, CollectFlintTask::new);
        simple("obsidian", Items.OBSIDIAN, CollectObsidianTask::new).dontMineIfPresent();
        simple("wool", ItemHelper.WOOL, CollectWoolTask::new);
        simple("egg", Items.EGG, CollectEggsTask::new);
        mob("bone", Items.BONE, SkeletonEntity.class);
        mob("gunpowder", Items.GUNPOWDER, CreeperEntity.class);
        simple("ender_pearl", Items.ENDER_PEARL, KillEndermanTask::new);
        mob("spider_eye", Items.SPIDER_EYE, SpiderEntity.class);
        mob("leather", Items.LEATHER, CowEntity.class);
        mob("feather", Items.FEATHER, ChickenEntity.class);
        mob("rotten_flesh", Items.ROTTEN_FLESH, ZombieEntity.class);
        mob("rabbit_foot", Items.RABBIT_FOOT, RabbitEntity.class);
        mob("rabbit_hide", Items.RABBIT_HIDE, RabbitEntity.class);
        mob("slime_ball", Items.SLIME_BALL, SlimeEntity.class);
        mob("wither_skeleton_skull", Items.WITHER_SKELETON_SKULL, WitherSkeletonEntity.class).forceDimension(Dimension.NETHER);
        mob("ink_sac", Items.INK_SAC, SquidEntity.class); // Warning, this probably won't work.
        mob("glow_ink_sac", Items.GLOW_INK_SAC, Entities.GLOW_SQUID); // Warning, this probably won't work.
        mob("string", Items.STRING, SpiderEntity.class); // Warning, this probably won't work.
        mine("sugar_cane", Items.SUGAR_CANE);
        mine("brown_mushroom", MiningRequirement.HAND, new Block[]{Blocks.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK}, Items.BROWN_MUSHROOM);
        mine("red_mushroom", MiningRequirement.HAND, new Block[]{Blocks.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK}, Items.RED_MUSHROOM);
        mine("mushroom", MiningRequirement.HAND, new Block[]{Blocks.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK}, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM);
        mine("melon_slice", MiningRequirement.HAND, Blocks.MELON, Items.MELON_SLICE);
        mine("pumpkin", MiningRequirement.HAND, Blocks.PUMPKIN, Items.PUMPKIN);
        mine("bell", MiningRequirement.WOOD, Blocks.BELL, Items.BELL);
        mine("nether_wart", MiningRequirement.HAND, Blocks.NETHER_WART, Items.NETHER_WART).forceDimension(Dimension.NETHER);
        mine("crimson_fungus", MiningRequirement.HAND, Blocks.CRIMSON_FUNGUS, Items.CRIMSON_FUNGUS).forceDimension(Dimension.NETHER);
        mine("warped_fungus", MiningRequirement.HAND, Blocks.WARPED_FUNGUS, Items.WARPED_FUNGUS).forceDimension(Dimension.NETHER);
        mine("crimson_roots", MiningRequirement.HAND, Blocks.CRIMSON_ROOTS, Items.CRIMSON_ROOTS).forceDimension(Dimension.NETHER);
        mine("warped_roots", MiningRequirement.HAND, Blocks.WARPED_ROOTS, Items.WARPED_ROOTS).forceDimension(Dimension.NETHER);
        mine("weeping_vines", MiningRequirement.HAND, Blocks.WEEPING_VINES, Items.WEEPING_VINES).forceDimension(Dimension.NETHER);
        mine("twisting_vines", MiningRequirement.HAND, Blocks.TWISTING_VINES, Items.TWISTING_VINES).forceDimension(Dimension.NETHER);
        mine("nether_wart_block", MiningRequirement.HAND, Blocks.NETHER_WART_BLOCK, Items.NETHER_WART_BLOCK).forceDimension(Dimension.NETHER);
        mine("warped_wart_block", MiningRequirement.HAND, Blocks.WARPED_WART_BLOCK, Items.WARPED_WART_BLOCK).forceDimension(Dimension.NETHER);
        mine("shroomlight", MiningRequirement.HAND, Blocks.SHROOMLIGHT, Items.SHROOMLIGHT).forceDimension(Dimension.NETHER);
        simple("blaze_rod", Items.BLAZE_ROD, CollectBlazeRodsTask::new).forceDimension(Dimension.NETHER); // Not super simple tbh lmao
        //simple("quartz", Items.QUARTZ, CollectQuartzTask::new);
        mine("quartz", MiningRequirement.WOOD, Blocks.NETHER_QUARTZ_ORE, Items.QUARTZ).forceDimension(Dimension.NETHER);
        simple("cocoa_beans", Items.COCOA_BEANS, CollectCocoaBeansTask::new);
        shear("cobweb", Blocks.COBWEB, Items.COBWEB).dontMineIfPresent();
        colorfulTasks("wool", color -> color.wool, (color, count) -> new CollectWoolTask(color.color, count));
        // Misc greenery
        shear("leaves", ItemHelper.itemsToBlocks(ItemHelper.LEAVES), ItemHelper.LEAVES).dontMineIfPresent();
        for (CataloguedResource resource : woodTasks(
                "leaves",
                woodItems -> woodItems.leaves,
                (woodItems, count) -> {
                    if (woodItems.isNetherWood()) {
                        // Nether "leaves" aren't sheared, they can simply be mined.
                        return new MineAndCollectTask(woodItems.leaves, count, new Block[]{Block.getBlockFromItem(woodItems.leaves)}, MiningRequirement.HAND).forceDimension(Dimension.NETHER);
                    } else {
                        return new ShearAndCollectBlockTask(woodItems.leaves, count, Block.getBlockFromItem(woodItems.leaves));
                    }
                })
        ) {
            resource.dontMineIfPresent();
        }
        mine("bamboo", Blocks.BAMBOO, Items.BAMBOO);
        shear("vine", Blocks.VINE, Items.VINE).dontMineIfPresent();
        shear("grass", Blocks.SHORT_GRASS, Items.SHORT_GRASS).dontMineIfPresent();
        shear("lily_pad", Blocks.LILY_PAD, Items.LILY_PAD).dontMineIfPresent();
        shear("tall_grass", Blocks.TALL_GRASS, Items.TALL_GRASS).dontMineIfPresent();
        shear("fern", Blocks.FERN, Items.FERN).dontMineIfPresent();
        shear("large_fern", Blocks.LARGE_FERN, Items.LARGE_FERN).dontMineIfPresent();
        shear("dead_bush", Blocks.DEAD_BUSH, Items.DEAD_BUSH).dontMineIfPresent();
        shear("glow_lichen", Blocks.GLOW_LICHEN, Items.GLOW_LICHEN).dontMineIfPresent();
        // Flowers
        simple("flower", ItemHelper.FLOWER, CollectFlowerTask::new);
        mine("allium", Items.ALLIUM);
        mine("azure_bluet", Items.AZURE_BLUET);
        mine("blue_orchid", Items.BLUE_ORCHID);
        mine("cactus", Items.CACTUS);
        mine("cornflower", Items.CORNFLOWER);
        mine("dandelion", Items.DANDELION);
        mine("lilac", Items.LILAC);
        mine("lily_of_the_valley", Items.LILY_OF_THE_VALLEY);
        mine("orange_tulip", Items.ORANGE_TULIP);
        mine("oxeye_daisy", Items.OXEYE_DAISY);
        mine("pink_tulip", Items.PINK_TULIP);
        mine("poppy", Items.POPPY);
        mine("peony", Items.PEONY);
        mine("red_tulip", Items.RED_TULIP);
        mine("rose_bush", Items.ROSE_BUSH);
        mine("sunflower", Items.SUNFLOWER);
        mine("white_tulip", Items.WHITE_TULIP);
        // Crops
        simple("wheat", Items.WHEAT, CollectWheatTask::new);
        crop("carrot", Items.CARROT, Blocks.CARROTS, Items.CARROT);
        crop("potato", Items.POTATO, Blocks.POTATOES, Items.POTATO);
        crop("poisonous_potato", Items.POISONOUS_POTATO, Blocks.POTATOES, Items.POTATO);
        crop("beetroot", Items.BEETROOT, Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
        simple("wheat_seeds", Items.WHEAT_SEEDS, CollectWheatSeedsTask::new);
        crop("beetroot_seeds", Items.BEETROOT_SEEDS, Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
    }
}
