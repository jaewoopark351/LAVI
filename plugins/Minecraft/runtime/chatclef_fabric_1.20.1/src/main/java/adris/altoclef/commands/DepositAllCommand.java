package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import lavi.minecraft.diagnostics.command.deposit.DepositCommandDiagnostics;
import lavi.minecraft.diagnostics.command.deposit.DepositCommandVariant;
import lavi.minecraft.task.container.deposit.DepositAllInventoryTargetSelector;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

//20260826_kpopmodder: Added an independent deposit_all command as a behavior-preserving copy of DepositCommand.
public class DepositAllCommand extends Command {

    // TODO: Configuration
    private static final int NEARBY_RANGE = 20;

    private static final Block[] VALID_CONTAINERS = Stream.concat(Arrays.stream(new Block[]{Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL}), Arrays.stream(ItemHelper.itemsToBlocks(ItemHelper.SHULKER_BOXES))).toArray(Block[]::new);
    private static final DepositAllInventoryTargetSelector INVENTORY_TARGET_SELECTOR = new DepositAllInventoryTargetSelector();

    public DepositAllCommand() throws CommandException {
        super("deposit_all", "Deposit our items to a nearby chest, making a chest if one doesn't exist. Pass no arguments to depisot ALL items. Examples: `deposit_all` deposits ALL items, `deposit_all diamond 2` deposits 2 diamonds.", new Arg(ItemList.class, "items (empty for ALL non gear items)", null, 0, false));
    }

    public static ItemTarget[] getAllNonEquippedOrToolItemsAsTarget(AltoClef mod) {
        return INVENTORY_TARGET_SELECTOR.select(mod);
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        ItemList itemList = parser.get(ItemList.class);
        ItemTarget[] items;

        if (itemList != null) {
            // Validation that we have the items:
            Map<String, Integer> countsLeftover = new HashMap<>();

            // populate starting requirements
            for (ItemTarget itemTarget : itemList.items) {
                String name = itemTarget.getCatalogueName();
                countsLeftover.put(name, countsLeftover.getOrDefault(name, 0) + itemTarget.getTargetCount());
            }

            // subtract counts
            for (int i = 0; i < mod.getPlayer().getInventory().size(); ++i) {
                ItemStack stack = mod.getPlayer().getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    String name = ItemHelper.stripItemName(stack.getItem());
                    int count = stack.getCount();
                    if (countsLeftover.containsKey(name)) {
                        countsLeftover.put(name, countsLeftover.get(name) - count);
                        if (countsLeftover.get(name) <= 0) {
                            countsLeftover.remove(name);
                        }
                    }
                }
            }

            // invalid!
            if (countsLeftover.size() != 0) {
                String leftover = String.join(",", countsLeftover.entrySet().stream().map(e -> e.getKey() + " x " + e.getValue().toString()).toList());
                mod.log("Insuffucient items in inventory to deposit. We still need: " + leftover + ".");
                finish();
                return;
            }
        }

        if (itemList == null) {
            items = getAllNonEquippedOrToolItemsAsTarget(mod);
        } else {
            items = itemList.items;
        }

        // BlockScanner blockScanner = mod.getBlockScanner();
        // Optional<BlockPos> container = blockScanner.getNearestBlock(VALID_CONTAINERS);

        // if (!container.isPresent() || !container.get().isWithinDistance(mod.getPlayer().getPos(), NEARBY_RANGE)) {
        //     // Just use a random container
        //     mod.runUserTask(new DepositAllTask(false, items), this::finish);
        //     return;
        //     // mod.log("No container (chest, barrel, shulker) found nearby. Move close to a container and try again.");
        //     // finish();
        //     // return;
        // }

        // // Store in the nearby container
        // mod.runUserTask(new StoreInContainerTask(container.get(), false, items), this::finish);

        DepositAllTask storeTask = new DepositAllTask(false, items);
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        // trace-20260807-deposit-origin: diagnostics-only attribution for post-completion store loops.
        DepositCommandDiagnostics.logInvocation(
                mod,
                itemList != null,
                items,
                storeTask,
                DepositCommandVariant.DEPOSIT_ALL
        );
        mod.runUserTask(storeTask, this::finish);
    }
}
