package lavi.minecraft.task.container.deposit.auto.trusted.interaction;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockInteractEvent;
import adris.altoclef.eventbus.events.ScreenOpenEvent;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260827_kpopmodder: Bind an open container only to the exact preceding block interaction.
public final class AutoDepositOpenContainerBindingTracker
        implements AutoDepositExactOpenContainerBinding,
        AutoDepositExactOpenContainerBindingDiagnosticView {
    private static final long MAX_PENDING_TICKS = 20L;

    private final AltoClef mod;
    private Subscription<BlockInteractEvent> blockInteractionSubscription;
    private Subscription<ScreenOpenEvent> screenSubscription;
    private final AutoDepositOpenContainerCorrelationWindow correlationWindow =
            new AutoDepositOpenContainerCorrelationWindow();
    private long clientTick;
    private OpenBinding binding;

    public AutoDepositOpenContainerBindingTracker(AltoClef mod) {
        this.mod = mod;
    }

    public void start() {
        if (blockInteractionSubscription != null || screenSubscription != null) {
            return;
        }
        blockInteractionSubscription = EventBus.subscribe(
                BlockInteractEvent.class, this::onBlockInteraction
        );
        screenSubscription = EventBus.subscribe(ScreenOpenEvent.class, this::onScreenEvent);
    }

    public void stop() {
        EventBus.unsubscribe(blockInteractionSubscription);
        EventBus.unsubscribe(screenSubscription);
        blockInteractionSubscription = null;
        screenSubscription = null;
        correlationWindow.clear();
        binding = null;
    }

    public void onEndClientTick() {
        clientTick++;
        correlationWindow.expire(clientTick, MAX_PENDING_TICKS);
        if (binding != null && !binding.matchesCurrent(mod)) {
            binding = null;
        }
    }

    @Override
    public Optional<BlockPos> currentExactPosition() {
        if (binding == null || !binding.matchesCurrent(mod)) {
            binding = null;
            return Optional.empty();
        }
        return Optional.of(binding.position);
    }

    //20260828_kpopmodder: Observe exact binding validity without clearing behavior-owned state.
    @Override
    public Optional<BlockPos> peekCurrentExactPositionForDiagnostics() {
        if (binding == null || !binding.matchesCurrent(mod)) {
            return Optional.empty();
        }
        return Optional.of(binding.position);
    }

    private void onBlockInteraction(BlockInteractEvent event) {
        if (event == null || event.hitResult == null
                || mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            correlationWindow.clear();
            return;
        }
        BlockPos position = event.hitResult.getBlockPos();
        Block block = mod.getWorld().getBlockState(position).getBlock();
        if (!AutoDepositTrustedContainerSupport.isSupported(block)) {
            correlationWindow.clear();
            return;
        }
        correlationWindow.record(
                mod.getWorld(),
                WorldHelper.getCurrentDimension(),
                position,
                clientTick
        );
    }

    private void onScreenEvent(ScreenOpenEvent event) {
        if (event == null || event.preOpen) {
            return;
        }
        Screen screen = event.screen;
        if (screen == null || mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            correlationWindow.clear();
            binding = null;
            return;
        }
        Optional<BlockPos> candidatePosition = correlationWindow.consume(
                mod.getWorld(),
                WorldHelper.getCurrentDimension(),
                clientTick,
                MAX_PENDING_TICKS
        );
        if (candidatePosition.isEmpty()) {
            binding = null;
            return;
        }
        BlockPos position = candidatePosition.get();
        Block block = mod.getWorld().getBlockState(position).getBlock();
        ContainerType type = ContainerType.getFromBlock(block);
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        if (!AutoDepositTrustedContainerSupport.isSupported(block)
                || handler == null
                || !ContainerType.screenHandlerMatches(type, handler)
                || MinecraftClient.getInstance().currentScreen != screen) {
            binding = null;
            return;
        }
        binding = new OpenBinding(
                mod.getWorld(),
                WorldHelper.getCurrentDimension(),
                position,
                screen,
                handler
        );
    }

    private static final class OpenBinding {
        private final Object worldIdentity;
        private final Dimension dimension;
        private final BlockPos position;
        private final Screen screen;
        private final ScreenHandler handler;

        private OpenBinding(Object worldIdentity,
                            Dimension dimension,
                            BlockPos position,
                            Screen screen,
                            ScreenHandler handler) {
            this.worldIdentity = worldIdentity;
            this.dimension = dimension;
            this.position = position.toImmutable();
            this.screen = screen;
            this.handler = handler;
        }

        private boolean matchesCurrent(AltoClef mod) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (mod == null || mod.getWorld() == null || mod.getPlayer() == null
                    || client == null || client.currentScreen != screen
                    || mod.getPlayer().currentScreenHandler != handler
                    || mod.getWorld() != worldIdentity
                    || WorldHelper.getCurrentDimension() != dimension) {
                return false;
            }
            Block block = mod.getWorld().getBlockState(position).getBlock();
            return AutoDepositTrustedContainerSupport.isSupported(block)
                    && ContainerType.screenHandlerMatches(ContainerType.getFromBlock(block), handler);
        }
    }
}
