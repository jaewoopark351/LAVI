package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerType;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Objects;
import java.util.OptionalInt;

//20260827_kpopmodder: Execute and verify one exact-slot QUICK_MOVE against the bound trusted GUI.
public final class HomeStorageTransferExecutor {
    private static final int MAX_UNCONFIRMED_TICKS = 40;

    private final HomeStorageScreenSlotResolver slotResolver;
    private final HomeStorageTransferDeltaVerifier deltaVerifier =
            new HomeStorageTransferDeltaVerifier();
    private PendingTransfer pending;

    public HomeStorageTransferExecutor(HomeStorageScreenSlotResolver slotResolver) {
        this.slotResolver = Objects.requireNonNull(slotResolver, "slotResolver");
    }

    public Result tick(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding,
            HomeStorageManifestStep step,
            int expectedSourceCount) {
        LiveContainer live = inspectLiveContainer(mod, destination, binding);
        if (!live.open()) {
            return Result.of(Status.CONTAINER_NOT_OPEN, live.reason());
        }
        if (!cursorEmpty(live.handler())) {
            return Result.of(Status.CURSOR_NOT_EMPTY, "cursor_not_empty");
        }

        OptionalInt resolved = slotResolver.resolve(mod, step.logicalPlayerInventorySlot());
        if (resolved.isEmpty()) {
            return Result.of(Status.MANIFEST_STALE, "logical_slot_mapping_unavailable");
        }
        int sourceWindowSlot = resolved.getAsInt();
        ItemStack source = live.handler().slots.get(sourceWindowSlot).getStack();

        if (pending != null) {
            return verifyPending(live, destination, step, sourceWindowSlot, source);
        }
        if (source == null || source.isEmpty()
                || !step.fingerprint().matches(source)
                || source.getCount() != expectedSourceCount) {
            return Result.of(Status.MANIFEST_STALE, "exact_source_changed_before_click");
        }

        int capacity = availableCapacity(live, step.fingerprint(), source);
        if (capacity <= 0) {
            return Result.of(Status.NO_CAPACITY, "trusted_gui_has_no_capacity");
        }
        if (mod.getSlotHandler() == null || mod.getController() == null
                || !mod.getSlotHandler().canDoSlotAction()) {
            return Result.of(Status.WAITING, "slot_action_delay");
        }

        pending = new PendingTransfer(
                destination.key(),
                step.logicalPlayerInventorySlot(),
                sourceWindowSlot,
                source.getCount(),
                countInContainer(live, step.fingerprint()),
                0
        );
        try {
            mod.getSlotHandler().registerSlotAction();
            mod.getController().clickSlot(
                    live.handler().syncId,
                    sourceWindowSlot,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mod.getPlayer()
            );
        } catch (RuntimeException exception) {
            pending = null;
            mod.logWarning("Store-home exact slot click failed: "
                    + exception.getClass().getSimpleName());
            return Result.of(Status.NO_PROGRESS, "slot_click_exception");
        }
        return Result.of(Status.CLICK_REQUESTED, "quick_move_requested");
    }

    public OptionalInt pendingLogicalSlot() {
        return pending == null
                ? OptionalInt.empty()
                : OptionalInt.of(pending.logicalSlot());
    }

    public boolean hasPending() {
        return pending != null;
    }

    public void clearPending() {
        pending = null;
    }

    private Result verifyPending(
            LiveContainer live,
            AutoDepositTrustedDestination destination,
            HomeStorageManifestStep step,
            int sourceWindowSlot,
            ItemStack source) {
        if (!pending.destinationKey().equals(destination.key())
                || pending.logicalSlot() != step.logicalPlayerInventorySlot()
                || pending.sourceWindowSlot() != sourceWindowSlot) {
            return terminal(Status.TRANSFER_UNCONFIRMED, "pending_transfer_context_changed");
        }
        if (source != null && !source.isEmpty() && !step.fingerprint().matches(source)) {
            return terminal(Status.MANIFEST_STALE, "source_fingerprint_changed_after_click");
        }

        int sourceAfter = source == null || source.isEmpty() ? 0 : source.getCount();
        int destinationAfter = countInContainer(live, step.fingerprint());
        HomeStorageTransferDeltaVerifier.Verification verification = deltaVerifier.verify(
                pending.sourceCountBefore(),
                sourceAfter,
                pending.destinationCountBefore(),
                destinationAfter
        );
        if (verification.status() == HomeStorageTransferDeltaVerifier.Status.CONFIRMED) {
            Result result = new Result(
                    Status.TRANSFERRED,
                    verification.sourceDelta(),
                    sourceAfter,
                    "paired_delta_confirmed"
            );
            pending = null;
            return result;
        }
        if (verification.status() == HomeStorageTransferDeltaVerifier.Status.MISMATCH) {
            return terminal(Status.TRANSFER_UNCONFIRMED, "paired_delta_mismatch");
        }
        if (verification.status() == HomeStorageTransferDeltaVerifier.Status.REVERSED) {
            return terminal(Status.TRANSFER_UNCONFIRMED, "paired_delta_reversed");
        }

        pending = pending.nextTick();
        if (pending.elapsedTicks() >= MAX_UNCONFIRMED_TICKS) {
            Status status = verification.sourceDelta() == 0
                    && verification.destinationDelta() == 0
                    ? Status.NO_PROGRESS
                    : Status.TRANSFER_UNCONFIRMED;
            return terminal(status, "paired_delta_timeout");
        }
        return Result.of(Status.WAITING, "awaiting_paired_delta");
    }

    private Result terminal(Status status, String reason) {
        pending = null;
        return Result.of(status, reason);
    }

    private static LiveContainer inspectLiveContainer(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null
                || !binding.matches(destination.position())) {
            return new LiveContainer(null, null, false, "exact_trusted_gui_not_open");
        }
        Block block = mod.getWorld().getBlockState(destination.position()).getBlock();
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        if (!AutoDepositTrustedContainerSupport.isSupported(block)
                || handler == null
                || !ContainerType.screenHandlerMatches(ContainerType.getFromBlock(block), handler)) {
            return new LiveContainer(null, null, false, "bound_container_invalid");
        }
        return new LiveContainer(handler, mod.getPlayer().getInventory(), true, "open");
    }

    private static boolean cursorEmpty(ScreenHandler handler) {
        ItemStack cursor = handler == null ? ItemStack.EMPTY : handler.getCursorStack();
        return cursor == null || cursor.isEmpty();
    }

    private static int countInContainer(
            LiveContainer live,
            HomeStorageStackFingerprint fingerprint) {
        int count = 0;
        for (net.minecraft.screen.slot.Slot slot : live.handler().slots) {
            if (slot.inventory == live.playerInventory()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty() && fingerprint.matches(stack)) {
                count = saturatingAdd(count, stack.getCount());
            }
        }
        return count;
    }

    private static int availableCapacity(
            LiveContainer live,
            HomeStorageStackFingerprint fingerprint,
            ItemStack source) {
        int capacity = 0;
        for (net.minecraft.screen.slot.Slot slot : live.handler().slots) {
            if (slot.inventory == live.playerInventory() || !slot.canInsert(source)) {
                continue;
            }
            ItemStack current = slot.getStack();
            int maximum = Math.min(source.getMaxCount(), slot.getMaxItemCount(source));
            if (current == null || current.isEmpty()) {
                capacity = saturatingAdd(capacity, maximum);
            } else if (fingerprint.matches(current)) {
                capacity = saturatingAdd(capacity, Math.max(0, maximum - current.getCount()));
            }
        }
        return capacity;
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    public enum Status {
        WAITING,
        CLICK_REQUESTED,
        TRANSFERRED,
        CONTAINER_NOT_OPEN,
        NO_CAPACITY,
        NO_PROGRESS,
        MANIFEST_STALE,
        CURSOR_NOT_EMPTY,
        TRANSFER_UNCONFIRMED
    }

    public record Result(Status status, int transferredCount, int sourceCountAfter, String reason) {
        public static Result of(Status status, String reason) {
            return new Result(status, 0, -1, reason);
        }
    }

    private record LiveContainer(
            ScreenHandler handler,
            PlayerInventory playerInventory,
            boolean open,
            String reason) {
    }

    private record PendingTransfer(
            String destinationKey,
            int logicalSlot,
            int sourceWindowSlot,
            int sourceCountBefore,
            int destinationCountBefore,
            int elapsedTicks) {

        private PendingTransfer nextTick() {
            return new PendingTransfer(
                    destinationKey,
                    logicalSlot,
                    sourceWindowSlot,
                    sourceCountBefore,
                    destinationCountBefore,
                    elapsedTicks + 1
            );
        }
    }
}
