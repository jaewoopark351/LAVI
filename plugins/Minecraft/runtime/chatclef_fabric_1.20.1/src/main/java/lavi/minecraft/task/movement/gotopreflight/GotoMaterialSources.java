//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan.*;

/** Finite local source selection. Breaking and movement remain existing ChatClef tasks. */
final class GotoMaterialSources {
    record Source(BlockPos pos, BlockState state, Item drop) { }

    private final BlockPos anchor;
    private final BlockPos protectedFoundation;

    GotoMaterialSources(BlockPos anchor) { this(anchor, null); }

    GotoMaterialSources(BlockPos anchor, BlockPos protectedFoundation) {
        this.anchor = anchor.toImmutable();
        this.protectedFoundation = protectedFoundation == null ? null : protectedFoundation.toImmutable();
    }

    boolean inBounds(BlockPos p) {
        return Math.abs((long) p.getX() - anchor.getX()) <= SEARCH_RADIUS
                && Math.abs((long) p.getZ() - anchor.getZ()) <= SEARCH_RADIUS
                && Math.abs((long) p.getY() - anchor.getY()) <= SEARCH_VERTICAL_RADIUS;
    }

    String boundsDescription() {
        return "anchor=" + anchor + " horizontalRadius=" + SEARCH_RADIUS
                + " verticalRadius=" + SEARCH_VERTICAL_RADIUS;
    }

    Source find(AltoClef mod, GotoMaterialInventory inventory, Set<BlockPos> attempted) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(anchor.add(-SEARCH_RADIUS, -SEARCH_VERTICAL_RADIUS, -SEARCH_RADIUS),
                anchor.add(SEARCH_RADIUS, SEARCH_VERTICAL_RADIUS, SEARCH_RADIUS))) {
            if (!attempted.contains(p) && safe(mod, p)) positions.add(p.toImmutable());
        }
        positions.sort(Comparator.<BlockPos>comparingInt(p -> p.getY() < anchor.getY() ? 1 : 0)
                .thenComparingDouble(p -> p.getSquaredDistance(mod.getPlayer().getBlockPos())));
        boolean capacityBlocked = false;
        for (BlockPos p : positions) {
            BlockState state = mod.getWorld().getBlockState(p);
            ItemStack tool = prospectiveTool(mod, state);
            Item drop = expectedDrop(state, tool);
            if (drop != null && inventory.accepts(drop) && harvestable(mod, state, tool)) {
                if (inventory.hasCapacity(mod, drop)) return new Source(p, state, drop);
                capacityBlocked = true;
            }
        }
        if (capacityBlocked) throw new Failure(FailureReason.INVENTORY_FULL);
        throw new Failure(FailureReason.NO_SAFE_SOURCE,
                "No loaded, unprotected, supported source with tool and inventory capacity within the aerial preparation area.");
    }

    boolean safe(AltoClef mod, BlockPos p) {
        var world = mod.getWorld();
        if (!inBounds(p) || !world.isChunkLoaded(p) || !world.getWorldBorder().contains(p)) return false;
        BlockState state = world.getBlockState(p);
        Block b = state.getBlock();
        if (b != Blocks.DIRT && b != Blocks.GRASS_BLOCK && b != Blocks.STONE
                && b != Blocks.COBBLESTONE && b != Blocks.NETHERRACK
                && b != Blocks.DEEPSLATE && b != Blocks.COBBLED_DEEPSLATE) return false;
        // The target column's footing must not become its own source of scaffolding.
        if (protectedFoundation != null
                && p.getX() == protectedFoundation.getX()
                && p.getZ() == protectedFoundation.getZ()
                && p.getY() <= protectedFoundation.getY()) return false;
        // Never excavate a descending staircase merely to gather scaffolding.
        if (p.getY() < anchor.getY() - 1) return false;
        // Keep the preparation foundation, even after the player moves to another column.
        if (p.getX() == anchor.getX() && p.getZ() == anchor.getZ() && p.getY() < anchor.getY()) return false;
        BlockPos feet = mod.getPlayer().getBlockPos();
        if (p.getY() < feet.getY()) {
            BlockPos landing = p.down();
            if (!world.isChunkLoaded(landing)
                    || !world.getBlockState(landing).getFluidState().isEmpty()
                    || !world.getBlockState(landing).isSideSolidFullSquare(world, landing, Direction.UP)) return false;
        }
        // Never remove the column supporting the player's present position.
        if (p.getX() == feet.getX() && p.getZ() == feet.getZ() && p.getY() < feet.getY()) return false;
        if (world.getBlockEntity(p) != null || state.getHardness(world, p) < 0
                || !state.getFluidState().isEmpty()
                || mod.getExtraBaritoneSettings().shouldAvoidBreaking(p)
                || mod.getBlockScanner().isUnreachable(p)) return false;
        boolean exposed = false;
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = p.offset(direction);
            if (!world.isChunkLoaded(adjacent)) return false;
            BlockState neighbor = world.getBlockState(adjacent);
            if (!neighbor.getFluidState().isEmpty()) return false;
            if (neighbor.isAir()) exposed = true;
        }
        return exposed && !MovementHelper.avoidBreaking(mod.getClientBaritone().bsi,
                p.getX(), p.getY(), p.getZ(), state);
    }

    void revalidate(AltoClef mod, GotoMaterialInventory inventory, Source source) {
        if (!safe(mod, source.pos()) || !mod.getWorld().getBlockState(source.pos()).equals(source.state())) {
            throw new Failure(FailureReason.SOURCE_INVALIDATED, source.pos().toString());
        }
        if (!inventory.accepts(source.drop()) || !inventory.hasCapacity(mod, source.drop())) {
            throw new Failure(FailureReason.INVENTORY_FULL);
        }
    }

    boolean equipAndCheck(AltoClef mod, Source source) {
        ItemStack current = mod.getPlayer().getMainHandStack();
        if (harvestable(mod, source.state(), current) && expectedDrop(source.state(), current) == source.drop()) return true;
        Optional<Slot> slot = StorageHelper.getBestToolSlot(mod, source.state());
        if (slot.isEmpty()) throw new Failure(FailureReason.TOOL_NOT_READY);
        mod.getSlotHandler().forceEquipSlot(slot.get());
        // The parent checks again on a later client tick before permitting any break.
        return false;
    }

    private ItemStack prospectiveTool(AltoClef mod, BlockState state) {
        if (!state.isToolRequired()) return mod.getPlayer().getMainHandStack();
        return StorageHelper.getBestToolSlot(mod, state)
                .map(StorageHelper::getItemStackInSlot).orElse(ItemStack.EMPTY);
    }

    private boolean harvestable(AltoClef mod, BlockState state, ItemStack tool) {
        if (state.isToolRequired() && !tool.isSuitableFor(state)) return false;
        return !tool.isDamageable() || !StorageHelper.shouldSaveStack(mod, state.getBlock(), tool);
    }

    static Item expectedDrop(BlockState state, ItemStack tool) {
        boolean silk = EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, tool) > 0;
        if (state.isOf(Blocks.DIRT)) return Items.DIRT;
        if (state.isOf(Blocks.GRASS_BLOCK)) return silk ? Items.GRASS_BLOCK : Items.DIRT;
        if (state.isOf(Blocks.STONE)) return silk ? Items.STONE : Items.COBBLESTONE;
        if (state.isOf(Blocks.COBBLESTONE)) return Items.COBBLESTONE;
        if (state.isOf(Blocks.NETHERRACK)) return Items.NETHERRACK;
        if (state.isOf(Blocks.DEEPSLATE)) return silk ? Items.DEEPSLATE : Items.COBBLED_DEEPSLATE;
        if (state.isOf(Blocks.COBBLED_DEEPSLATE)) return Items.COBBLED_DEEPSLATE;
        return null;
    }
}
//#endif
