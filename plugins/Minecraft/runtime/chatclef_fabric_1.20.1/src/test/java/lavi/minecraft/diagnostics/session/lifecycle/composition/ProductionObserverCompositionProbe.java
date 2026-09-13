package lavi.minecraft.diagnostics.session.lifecycle.composition;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.BlockCollectionDiagnostics;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleRegistry;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticObserverRegistrationStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.lifecycle.FabricChatClefCraftResourceDiagnosticLifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//20260913_kpopmodder: Initialize the real production bindings in a fresh JVM, including the lazy last owner.
public final class ProductionObserverCompositionProbe {
    private static final String ROOT = "lavi.minecraft.diagnostics.";
    private static final String TOOL = ROOT + "toolselect.ToolEquipDiagnostics";
    private static final String BLOCK = ROOT + "blocks.collection.BlockCollectionDiagnostics";
    private static final List<String> OWNERS = List.of(
            ROOT + "ChatClefDiagnostics", ROOT + "observation.ObservationDiagnostics",
            ROOT + "container.store.deposit.StoreDepositDiagnostics",
            ROOT + "toolselect.BestToolSlotDiagnostics", ROOT + "toolselect.ToolSavePolicySnapshotDiagnostics",
            ROOT + "baritone.correlation.BuilderTraceRegistry", BLOCK);

    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        List<String> order = new ArrayList<>(OWNERS);
        if (scenario.equals("reverse")) Collections.reverse(order);
        if (scenario.equals("bridge-first")) FabricChatClefCraftResourceDiagnosticLifecycle.register();
        for (String name : order) {
            Class.forName(name);
            if (name.equals(BLOCK)) BlockCollectionDiagnostics.install();
        }
        FabricChatClefCraftResourceDiagnosticLifecycle.register();
        DiagnosticSessionLifecycleRegistry registry = registry();
        require(registry.registeredObserverCount() == 17, "Actual pre-ToolEquip graph must contain 17 observers");
        for (String name : OWNERS) {
            String ownerName = name.equals(BLOCK) ? ROOT + "blocks.collection.BlockCollectionRuntime" : name;
            require(owner(Class.forName(ownerName)).isAvailable(), name + " unavailable");
        }
        require(FabricChatClefCraftResourceDiagnosticLifecycle.isAvailable(), "bridge unavailable");

        boolean overflow = scenario.equals("tool-overflow");
        if (overflow) {
            // The first 17 are still the real production bindings. Fill only the remaining reserved capacity.
            for (int i = 0; i < 15; i++) require(registry.register(new DiagnosticSessionLifecycleObserver() { }).accepted(), "fill rejected");
        }
        Class<?> tool = Class.forName(TOOL);
        DiagnosticOwnerRegistration toolOwner = owner(tool);
        require(toolOwner.isAvailable() != overflow, "ToolEquip availability");
        require(registry.registeredObserverCount() == (overflow ? 32 : 18), "Final observer count");
        if (overflow) {
            require(toolOwner.registrationResult().status() == DiagnosticObserverRegistrationStatus.CAPACITY_EXHAUSTED,
                    "typed capacity failure");
            ChatClefDiagnostics.setBoundaryEnabled(true);
            AtomicInteger captures = new AtomicInteger();
            Object result = tool.getMethod("withAvailableOwner", java.util.function.Supplier.class, Object.class)
                    .invoke(null, (java.util.function.Supplier<Object>) () -> { captures.incrementAndGet(); return "bad"; }, "disabled");
            require(result.equals("disabled") && captures.get() == 0, "Failed real owner must not collect");
        }
        // Re-entry uses precisely the same production callbacks and must never add six more observers.
        FabricChatClefCraftResourceDiagnosticLifecycle.register();
        BlockCollectionDiagnostics.install();
        require(registry.registeredObserverCount() == (overflow ? 32 : 18), "Repeated production registration grew registry");
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.setBoundaryEnabled(true);
        registry.finalSnapshotFields();
        registry.notifyAfterCleanTeardownSnapshotAttempt(false);
        require(toolOwner.isAvailable() != overflow, "OFF/ON or teardown revived failed owner");
        System.out.println("PRODUCTION_OBSERVERS_PASS scenario=" + scenario + " count=" + registry.registeredObserverCount());
    }

    private static DiagnosticSessionLifecycleRegistry registry() throws Exception {
        Field field = ChatClefDiagnostics.class.getDeclaredField("SESSION_LIFECYCLE");
        field.setAccessible(true);
        return (DiagnosticSessionLifecycleRegistry) field.get(null);
    }
    private static DiagnosticOwnerRegistration owner(Class<?> type) throws Exception {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == DiagnosticOwnerRegistration.class) {
                field.setAccessible(true);
                return (DiagnosticOwnerRegistration) field.get(null);
            }
        }
        throw new AssertionError("No lifecycle owner on " + type.getName());
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
