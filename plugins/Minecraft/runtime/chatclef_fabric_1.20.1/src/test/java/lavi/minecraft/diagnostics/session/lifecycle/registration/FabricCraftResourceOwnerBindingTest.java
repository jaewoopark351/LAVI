package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.lifecycle.FabricChatClefCraftResourceDiagnosticLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise the bridge's actual registration path without its config or transport graph.
class FabricCraftResourceOwnerBindingTest {
    @BeforeEach
    void prepareDiagnosticSession() {
        FabricChatClefCraftResourceDiagnosticLifecycle.register();
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
    }

    @AfterEach
    void clearDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void actualCraftingOwnerRemainsRegisteredAndCleansAllActivatedScopeEvidenceOnModeOff() {
        DiagnosticObserverRegistrationResult registered = FabricChatClefCraftResourceDiagnosticLifecycle.register();
        assertEquals(DiagnosticObserverRegistrationStatus.ALREADY_REGISTERED, registered.status());
        assertTrue(FabricChatClefCraftResourceDiagnosticLifecycle.isAvailable());
        Object rootTask = new Object();
        var activation = IronPickaxeAcquisitionScopeDiagnostics.activate(true, "@get iron_pickaxe 1",
                "session", 1L, "request", "correlation", "root-assignment", 1L,
                "root-task-instance", rootTask, "fixture.RootTask", 1L, 1_000_000L);
        assertTrue(activation.binding().isPresent());
        FabricChatClefCraftResourceAssociationScopeDiagnostics.observeActivation(activation);
        FabricChatClefCraftResourceTargetScopeDiagnostics.observeActivation(activation);
        assertEquals(1, IronPickaxeAcquisitionScopeDiagnostics.snapshot().activeCount());
        assertEquals(1, FabricChatClefCraftResourceTargetScopeDiagnostics.registry().snapshot().activeScopeCount());

        ChatClefDiagnostics.setBoundaryEnabled(false);

        assertEquals(0, IronPickaxeAcquisitionScopeDiagnostics.snapshot().activeCount());
        assertEquals(0, FabricChatClefCraftResourceTargetScopeDiagnostics.registry().snapshot().activeScopeCount());
        assertTrue(FabricChatClefCraftResourceDiagnosticLifecycle.isAvailable());
        assertTrue(FabricChatClefCraftResourceAssociationScopeDiagnostics.snapshot(activation.binding().get().key()).isEmpty());
    }

    @Test
    void anEscapedTargetRegistryCannotReactivateAfterItsOwnerIsDisabled() {
        AtomicBoolean available = new AtomicBoolean(true);
        CraftResourceTargetDiagnosticsRegistry registry = new CraftResourceTargetDiagnosticsRegistry(available::get);
        IronPickaxeAcquisitionScopeKey key = new IronPickaxeAcquisitionScopeKey(
                "session", 1L, "request", "correlation", "root", 1L, "task");
        assertTrue(registry.activateScope(key));
        available.set(false);
        registry.clearForModeOff();
        assertFalse(registry.activateScope(key));
        assertEquals(0, registry.snapshot().activeScopeCount());
    }
}
