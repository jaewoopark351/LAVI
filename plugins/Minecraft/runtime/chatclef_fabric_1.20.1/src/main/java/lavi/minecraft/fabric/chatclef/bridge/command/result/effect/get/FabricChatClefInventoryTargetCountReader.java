package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//20260907_kpopmodder: Read one immutable catalogue match set without changing gameplay state.
final class FabricChatClefInventoryTargetCountReader
        implements FabricChatClefGetItemTargetCountReader {
    private final Set<String> targetMatchIds;

    FabricChatClefInventoryTargetCountReader(List<String> targetMatchIds) {
        this.targetMatchIds = new HashSet<>(
                targetMatchIds == null ? List.of() : targetMatchIds
        );
        this.targetMatchIds.remove(null);
    }

    @Override
    public FabricChatClefGetItemCountObservation read() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return unavailable("minecraft_client_unavailable");
            }
            if (!client.isOnThread()) {
                return unavailable("minecraft_client_thread_required");
            }
            if (client.world == null || client.player == null) {
                return unavailable("client_world_or_player_unavailable");
            }
            if (targetMatchIds.isEmpty()) {
                return unavailable("target_match_set_empty");
            }
            AltoClef mod = AltoClef.getInstance();
            if (mod == null) {
                return unavailable("altoclef_unavailable");
            }
            if (mod.getWorld() != client.world || mod.getPlayer() != client.player) {
                return unavailable("altoclef_client_binding_mismatch");
            }
            int count = countInLivePlayerInventory(client);
            return FabricChatClefGetItemCountObservation.authoritative(
                    count,
                    client.world,
                    client.player
            );
        } catch (ArithmeticException error) {
            return unavailable("target_count_overflow");
        } catch (RuntimeException | LinkageError error) {
            return unavailable("count_read_failed:" + error.getClass().getSimpleName());
        }
    }

    private int countInLivePlayerInventory(MinecraftClient client) {
        PlayerInventory inventory = client.player.getInventory();
        return FabricChatClefInventoryAndCursorTargetCounter.count(
                inventory.size(),
                index -> matchingStackCount(inventory.getStack(index)),
                () -> client.player.currentScreenHandler == null
                        ? 0
                        : matchingStackCount(
                                client.player.currentScreenHandler.getCursorStack()
                        )
        );
    }

    private int matchingStackCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (itemId == null || !targetMatchIds.contains(itemId.toString())) {
            return 0;
        }
        return stack.getCount();
    }

    private static FabricChatClefGetItemCountObservation unavailable(String reason) {
        return FabricChatClefGetItemCountObservation.unavailable(reason);
    }
}
