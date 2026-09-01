package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Prove exact container Task references are one-shot and fail closed.
class FabricChatClefCraftResourceContainerActivationRegistryTest {
    @Test
    void firstSameParentReconciliationConsumesAMismatchedPendingCandidate() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        IronPickaxeAcquisitionScopeKey key = key();
        Task parent = new StubTask("parent");
        Task pending = new StubTask("pending");
        Task different = new StubTask("different");
        assertTrue(registry.observeCandidate(candidate(key, parent, pending)));

        FabricChatClefCraftResourceContainerActivationDecision mismatch =
                registry.reconcile(
                        key, parent, null, different, true, different, false, true
                );
        FabricChatClefCraftResourceContainerActivationDecision later =
                registry.reconcile(
                        key, parent, different, pending, true, pending, false, true
                );

        assertTrue(mismatch.identityMismatchObserved());
        assertTrue(mismatch.installedCandidate().isEmpty());
        assertTrue(later.installedCandidate().isEmpty());
    }

    @Test
    void activeIdentityDivergenceInvalidatesWithoutFabricatingAClosure() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        IronPickaxeAcquisitionScopeKey key = key();
        Task parent = new StubTask("parent");
        Task active = new StubTask("active");
        Task differentBefore = new StubTask("different-before");
        Task replacement = new StubTask("replacement");
        FabricChatClefCraftResourceContainerActivationCandidate candidate =
                candidate(key, parent, active);
        assertTrue(registry.observeCandidate(candidate));
        FabricChatClefCraftResourceContainerActivationDecision installed =
                registry.reconcile(
                        key, parent, null, active, true, active, false, true
                );
        assertTrue(installed.installedCandidate().isPresent());
        assertTrue(registry.markActive(key, candidate, 1L));

        FabricChatClefCraftResourceContainerActivationDecision mismatch =
                registry.reconcile(
                        key,
                        parent,
                        differentBefore,
                        replacement,
                        true,
                        replacement,
                        false,
                        true
                );

        assertTrue(mismatch.identityMismatchObserved());
        assertTrue(mismatch.closedActiveTarget().isEmpty());
    }

    @Test
    void suppressedInstallationConsumesPendingWithoutLaterActivation() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        IronPickaxeAcquisitionScopeKey key = key();
        Task parent = new StubTask("parent");
        Task child = new StubTask("child");
        assertTrue(registry.observeCandidate(candidate(key, parent, child)));

        FabricChatClefCraftResourceContainerActivationDecision suppressed =
                registry.reconcile(
                        key, parent, null, child, true, child, false, false
                );
        FabricChatClefCraftResourceContainerActivationDecision later =
                registry.reconcile(
                        key, parent, null, child, true, child, false, true
                );

        assertTrue(suppressed.sourceTransitionSuppressed());
        assertTrue(suppressed.installedCandidate().isEmpty());
        assertTrue(later.installedCandidate().isEmpty());
    }

    @Test
    void ownerExitClosesAnExactActiveReferenceOnlyWithPhysicalSource() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        IronPickaxeAcquisitionScopeKey key = key();
        Task parent = new StubTask("parent");
        Task child = new StubTask("child");
        FabricChatClefCraftResourceContainerActivationCandidate candidate =
                candidate(key, parent, child);
        assertTrue(registry.observeCandidate(candidate));
        assertTrue(registry.reconcile(
                key, parent, null, child, true, child, false, true
        ).installedCandidate().isPresent());
        assertTrue(registry.markActive(key, candidate, 1L));

        FabricChatClefCraftResourceContainerOwnerExitDecision closed =
                registry.observeOwnerExit(parent, true);

        assertTrue(closed.closedActiveTarget().isPresent());
        assertTrue(closed.scopeKey().filter(key::equals).isPresent());
        assertTrue(closed.sourceEmissionCompleted());
        assertFalse(closed.sourceTransitionSuppressed());
        assertTrue(registry.observeOwnerExit(parent, true)
                .closedActiveTarget().isEmpty());
    }

    @Test
    void suppressedOwnerExitDropsTheExactReferenceWithoutDerivedClosure() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        IronPickaxeAcquisitionScopeKey key = key();
        Task parent = new StubTask("parent");
        Task child = new StubTask("child");
        FabricChatClefCraftResourceContainerActivationCandidate candidate =
                candidate(key, parent, child);
        assertTrue(registry.observeCandidate(candidate));
        assertTrue(registry.reconcile(
                key, parent, null, child, true, child, false, true
        ).installedCandidate().isPresent());
        assertTrue(registry.markActive(key, candidate, 1L));

        FabricChatClefCraftResourceContainerOwnerExitDecision suppressed =
                registry.observeOwnerExit(parent, false);

        assertTrue(suppressed.closedActiveTarget().isEmpty());
        assertTrue(suppressed.sourceTransitionSuppressed());
        assertTrue(registry.observeOwnerExit(parent, true)
                .closedActiveTarget().isEmpty());
    }

    @Test
    void oneOwnerInTwoScopesIsAmbiguousAndReturnsBothBoundedScopeKeys() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        Task owner = new StubTask("shared-owner");
        assertTrue(registry.observeCandidate(candidate(key(), owner, new StubTask("a"))));
        IronPickaxeAcquisitionScopeKey secondKey = new IronPickaxeAcquisitionScopeKey(
                "session-2", 2, "request-2", "correlation-2", "root-2", 3,
                "instance-2"
        );
        assertTrue(registry.observeCandidate(
                candidate(secondKey, owner, new StubTask("b"))
        ));

        FabricChatClefCraftResourceContainerOwnerExitDecision ambiguous =
                registry.observeOwnerExit(owner, true);

        assertTrue(ambiguous.ownerIdentityAmbiguous());
        assertTrue(ambiguous.closedActiveTarget().isEmpty());
        assertTrue(ambiguous.ambiguousScopeKeys().contains(key()));
        assertTrue(ambiguous.ambiguousScopeKeys().contains(secondKey));
    }

    @Test
    void aDifferentParentCannotOverwriteAnExistingActiveReference() {
        FabricChatClefCraftResourceContainerActivationRegistry registry =
                new FabricChatClefCraftResourceContainerActivationRegistry();
        IronPickaxeAcquisitionScopeKey key = key();
        Task firstParent = new StubTask("first-parent");
        Task firstChild = new StubTask("first-child");
        FabricChatClefCraftResourceContainerActivationCandidate first =
                candidate(key, firstParent, firstChild);
        assertTrue(registry.observeCandidate(first));
        assertTrue(registry.reconcile(
                key, firstParent, null, firstChild, true, firstChild, false, true
        ).installedCandidate().isPresent());
        assertTrue(registry.markActive(key, first, 1L));

        Task secondParent = new StubTask("second-parent");
        Task secondChild = new StubTask("second-child");
        FabricChatClefCraftResourceContainerActivationCandidate second =
                candidate(key, secondParent, secondChild);
        assertTrue(registry.observeCandidate(second));
        assertTrue(registry.reconcile(
                key, secondParent, null, secondChild, true, secondChild, false, true
        ).installedCandidate().isPresent());

        assertFalse(registry.canActivateCandidate(key, second));
        assertFalse(registry.markActive(key, second, 2L));
        assertTrue(registry.observeOwnerExit(firstParent, true)
                .closedActiveTarget().isPresent());
    }

    private static FabricChatClefCraftResourceContainerActivationCandidate candidate(
            IronPickaxeAcquisitionScopeKey key,
            Task parent,
            Task child) {
        return new FabricChatClefCraftResourceContainerActivationCandidate(
                key,
                parent,
                child,
                new CraftResourceTargetTuple(
                        CraftResourceStage.RAW_IRON_SMELTING,
                        CraftResourceTargetRole.FURNACE_INTERACTION,
                        "1,2,3",
                        List.of("minecraft:furnace")
                ),
                CraftResourceSourceEventName.FURNACE_CONTAINER_ROUTE_TRANSITION
        );
    }

    private static IronPickaxeAcquisitionScopeKey key() {
        return new IronPickaxeAcquisitionScopeKey(
                "session", 1, "request", "correlation", "root", 2, "instance"
        );
    }

    private static final class StubTask extends Task {
        private final String name;

        private StubTask(String name) {
            this.name = name;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return name;
        }
    }
}
