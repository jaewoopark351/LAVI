package lavi.minecraft.task.container.home.execution.session;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerType;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

import java.util.Objects;

//20260828_kpopmodder: Admit a session only for the exact enabled trusted GUI and empty cursor.
public final class HomeStorageContainerActivationGate {
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositExactOpenContainerBinding binding;
    private final AutoDepositWorldKeyReader worldKeyReader;

    public HomeStorageContainerActivationGate(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositExactOpenContainerBinding binding,
            AutoDepositWorldKeyReader worldKeyReader) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.binding = Objects.requireNonNull(binding, "binding");
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
    }

    public HomeStorageContainerActivation evaluate(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageOperationContext context,
            boolean transferPending) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(context, "context");
        if (transferPending) {
            return refused(
                    HomeStorageContainerActivationStatus.PENDING_TRANSFER,
                    "activation_with_pending_transfer"
            );
        }
        if (!context.matches(mod, worldKeyReader)) {
            return refused(
                    HomeStorageContainerActivationStatus.CONTEXT_CHANGED,
                    "world_or_dimension_changed"
            );
        }
        if (!repository.containsEnabled(candidate.destination())) {
            return refused(
                    HomeStorageContainerActivationStatus.TRUST_LOST,
                    "registration_removed_or_disabled"
            );
        }
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return refused(
                    HomeStorageContainerActivationStatus.HANDLER_UNAVAILABLE,
                    "runtime_unavailable"
            );
        }
        if (!binding.matches(candidate.position())) {
            return refused(
                    HomeStorageContainerActivationStatus.EXACT_BINDING_MISSING,
                    "exact_trusted_gui_not_open"
            );
        }
        Block block = mod.getWorld().getBlockState(candidate.position()).getBlock();
        if (!AutoDepositTrustedContainerSupport.isSupported(block)) {
            return refused(
                    HomeStorageContainerActivationStatus.CONTAINER_UNSUPPORTED,
                    "bound_container_unsupported"
            );
        }
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        if (handler == null
                || !ContainerType.screenHandlerMatches(
                ContainerType.getFromBlock(block), handler)) {
            return refused(
                    HomeStorageContainerActivationStatus.HANDLER_UNAVAILABLE,
                    "bound_container_handler_invalid"
            );
        }
        ItemStack cursor = handler.getCursorStack();
        if (cursor != null && !cursor.isEmpty()) {
            return refused(
                    HomeStorageContainerActivationStatus.CURSOR_NOT_EMPTY,
                    "cursor_not_empty"
            );
        }
        return HomeStorageContainerActivation.ready(handler, handler.syncId);
    }

    private static HomeStorageContainerActivation refused(
            HomeStorageContainerActivationStatus status,
            String reason) {
        return HomeStorageContainerActivation.refused(status, reason);
    }
}
