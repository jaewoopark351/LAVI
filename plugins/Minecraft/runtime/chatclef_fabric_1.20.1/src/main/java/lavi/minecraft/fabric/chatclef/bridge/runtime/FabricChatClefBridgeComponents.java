package lavi.minecraft.fabric.chatclef.bridge.runtime;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementSourceEventObserver;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.CraftResourceContainerSourceEventObserver;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.lifecycle.CraftResourceContainerLifecycleSourceEventObserver;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.reconciliation.CraftResourceContainerReconciliationSourceEventObserver;
import lavi.minecraft.diagnostics.crafting.acquisition.source.craftingtable.CraftResourceCraftingTableSourceEventObserver;
import lavi.minecraft.diagnostics.crafting.acquisition.source.furnace.CraftResourceFurnaceSourceEventObserver;
import lavi.minecraft.diagnostics.mining.projection.MiningProjectionObserverRegistry;
import lavi.minecraft.diagnostics.session.lifecycle.mode.DiagnosticStateCleanupLifecycleObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefDiagnosticCommandContextProvider;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation.FabricChatClefCraftResourceContainerActivationDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.craftingtable.FabricChatClefCraftResourceCraftingTableProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.furnace.FabricChatClefCraftResourceFurnaceProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.generic.FabricChatClefCraftResourceContainerProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.lifecycle.FabricChatClefCraftResourceContainerLifecycleDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.FabricChatClefCraftResourceInteractionObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.FabricChatClefIronPickaxeRequirementProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceMiningProjectionListener;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfigLoader;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefBridgeClient;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefReconnectScheduler;

//20260804_kpopmodder: Keep Fabric ChatClef bridge object graph assembly out of the Fabric entrypoint.
public final class FabricChatClefBridgeComponents {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeClient bridgeClient;
    private final FabricChatClefUserTaskFinishedObserver taskFinishedObserver;
    private final FabricChatClefCommandDispatcher commandDispatcher;

    private FabricChatClefBridgeComponents(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeClient bridgeClient,
            FabricChatClefUserTaskFinishedObserver taskFinishedObserver,
            FabricChatClefCommandDispatcher commandDispatcher
    ) {
        this.diagnostics = diagnostics;
        this.bridgeClient = bridgeClient;
        this.taskFinishedObserver = taskFinishedObserver;
        this.commandDispatcher = commandDispatcher;
    }

    public static FabricChatClefBridgeComponents create() {
        FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
        FabricChatClefCommandQueue commandQueue = new FabricChatClefCommandQueue();
        ChatClefDiagnostics.registerCommandContextProvider(new FabricChatClefDiagnosticCommandContextProvider(commandQueue));
        IronPickaxeAcquisitionScopeDiagnostics.installRequirementObserver();
        CraftResourceRequirementSourceEventObserver.install(
                FabricChatClefIronPickaxeRequirementProjectionDiagnostics::observeVisibleTaskReturn
        );
        CraftResourceFurnaceSourceEventObserver.install(
                FabricChatClefCraftResourceFurnaceProjectionDiagnostics.instance()
        );
        CraftResourceContainerSourceEventObserver.install(
                FabricChatClefCraftResourceContainerProjectionDiagnostics.instance()
        );
        CraftResourceContainerReconciliationSourceEventObserver.install(
                FabricChatClefCraftResourceContainerActivationDiagnostics.instance()
        );
        CraftResourceContainerLifecycleSourceEventObserver.install(
                FabricChatClefCraftResourceContainerLifecycleDiagnostics.instance()
        );
        CraftResourceCraftingTableSourceEventObserver.install(
                FabricChatClefCraftResourceCraftingTableProjectionDiagnostics.instance()
        );
        MiningProjectionObserverRegistry.install(
                new FabricChatClefCraftResourceMiningProjectionListener()
        );
        ChatClefDiagnostics.registerBlockInteractionObserver(
                new FabricChatClefCraftResourceInteractionObserver()
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new DiagnosticStateCleanupLifecycleObserver(
                        IronPickaxeAcquisitionScopeDiagnostics::clearForModeOff
                )
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new DiagnosticStateCleanupLifecycleObserver(
                        FabricChatClefCraftResourceAssociationScopeDiagnostics::clearForModeOff
                )
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new DiagnosticStateCleanupLifecycleObserver(
                        FabricChatClefCraftResourceTargetScopeDiagnostics::clearForModeOff
                )
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new DiagnosticStateCleanupLifecycleObserver(
                        FabricChatClefCraftResourceContainerProjectionSupport::clearForModeOff
                )
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new DiagnosticStateCleanupLifecycleObserver(
                        FabricChatClefIronPickaxeRequirementProjectionDiagnostics::clearForModeOff
                )
        );
        ChatClefDiagnostics.registerSessionLifecycleObserver(
                new DiagnosticStateCleanupLifecycleObserver(
                        FabricChatClefCraftResourceTerminalDiagnostics::clearForModeOff
                )
        );
        FabricChatClefTaskStateReader taskStateReader = new FabricChatClefTaskStateReader();
        FabricChatClefBridgeConfig config = new FabricChatClefBridgeConfigLoader().load();
        FabricChatClefBridgeClient bridgeClient = new FabricChatClefBridgeClient(
                config,
                commandQueue,
                new FabricChatClefBridgeState(),
                diagnostics,
                new FabricChatClefBridgeJson(),
                new FabricChatClefBridgeMessageFactory(),
                new FabricChatClefReconnectScheduler()
        );
        FabricChatClefUserTaskFinishedObserver taskFinishedObserver = new FabricChatClefUserTaskFinishedObserver(
                diagnostics,
                taskStateReader
        );
        FabricChatClefCommandResultOutbox commandResultOutbox = new FabricChatClefCommandResultOutbox(
                commandQueue,
                bridgeClient,
                diagnostics
        );
        FabricChatClefCommandLifecycleCoordinator commandLifecycleCoordinator = new FabricChatClefCommandLifecycleCoordinator(
                taskFinishedObserver,
                new FabricChatClefCommandOutcomeClassifier(),
                commandResultOutbox,
                bridgeClient,
                diagnostics,
                taskStateReader
        );
        FabricChatClefCommandDispatcher commandDispatcher = new FabricChatClefCommandDispatcher(
                commandQueue,
                bridgeClient,
                commandLifecycleCoordinator,
                diagnostics,
                taskStateReader
        );
        return new FabricChatClefBridgeComponents(
                diagnostics,
                bridgeClient,
                taskFinishedObserver,
                commandDispatcher
        );
    }

    public FabricChatClefBridgeDiagnostics diagnostics() {
        return diagnostics;
    }

    public FabricChatClefBridgeClient bridgeClient() {
        return bridgeClient;
    }

    public FabricChatClefUserTaskFinishedObserver taskFinishedObserver() {
        return taskFinishedObserver;
    }

    public FabricChatClefCommandDispatcher commandDispatcher() {
        return commandDispatcher;
    }
}
