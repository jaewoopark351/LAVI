package lavi.minecraft.diagnostics.session.lifecycle.registration;

import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise bounded admission, equality, and atomic owner groups without a game.
class DiagnosticSessionObserverRegistryTest {
    @Test
    void admitsThirtyTwoButRejectsTheThirtyThirdWithoutThrowing() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        for (int index = 0; index < 32; index++) {
            DiagnosticObserverRegistrationResult result = registry.register(observer());
            assertEquals(DiagnosticObserverRegistrationStatus.REGISTERED, result.status());
            assertEquals(index + 1, result.registeredCount());
            assertEquals(32, result.capacity());
            assertEquals(1, result.addedCount());
        }
        DiagnosticObserverRegistrationResult rejected = assertDoesNotThrow(() -> registry.register(observer()));
        assertEquals(DiagnosticObserverRegistrationStatus.CAPACITY_EXHAUSTED, rejected.status());
        assertFalse(rejected.accepted());
        assertEquals(32, registry.snapshot().size());
    }

    @Test
    void preservesCandidateEqualsDuplicateContractEvenWhenFull() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        registry.register(new EqualObserver(17));
        for (int index = 1; index < 32; index++) {
            registry.register(observer());
        }
        DiagnosticObserverRegistrationResult duplicate = registry.register(new EqualObserver(17));
        assertEquals(DiagnosticObserverRegistrationStatus.ALREADY_REGISTERED, duplicate.status());
        assertTrue(duplicate.accepted());
        assertEquals(0, duplicate.addedCount());
        assertEquals(32, registry.registeredCount());
    }

    @Test
    void invalidObserversAndInvalidGroupsNeverPartiallyPublish() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        assertEquals(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER, registry.register(null).status());
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("invalid-group", () -> { });
        assertEquals(DiagnosticObserverRegistrationStatus.UNREGISTERED, owner.registrationResult().status());
        assertFalse(owner.isAvailable());
        assertEquals(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER,
                registry.registerOwner(owner, observer(), null).status());
        assertEquals(0, registry.registeredCount());
        assertFalse(owner.isAvailable());
    }

    @Test
    void aTwoObserverOwnerCannotConsumeTheLastSingleSlot() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        AtomicInteger cleanup = new AtomicInteger();
        for (int index = 0; index < 31; index++) registry.register(observer());
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("two-required", cleanup::incrementAndGet);
        DiagnosticObserverRegistrationResult result = registry.registerOwner(owner, observer(), observer());
        assertEquals(DiagnosticObserverRegistrationStatus.CAPACITY_EXHAUSTED, result.status());
        assertEquals(31, registry.registeredCount());
        assertEquals(1, cleanup.get());
        assertFalse(owner.isAvailable());
        owner.runIfAvailable(() -> fail("Rejected owner must never collect diagnostics"));
        assertEquals(-1L, owner.callIfAvailable(() -> 100L, -1L));
        assertEquals(DiagnosticObserverRegistrationStatus.REGISTERED, registry.register(observer()).status());
    }

    @Test
    void duplicateMembersWithinOneGroupConsumeOneSlotAndOwnersRemainDistinct() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        DiagnosticOwnerRegistration first = new DiagnosticOwnerRegistration("same-name", () -> { });
        DiagnosticOwnerRegistration second = new DiagnosticOwnerRegistration("same-name", () -> { });
        DiagnosticSessionLifecycleObserver callback = observer();
        assertEquals(1, registry.registerOwner(first, callback, callback).addedCount());
        assertEquals(1, registry.registerOwner(second, observer()).addedCount());
        assertTrue(first.isAvailable());
        assertTrue(second.isAvailable());
        assertEquals(2, registry.registeredCount());
        DiagnosticOwnerRegistration alias = new DiagnosticOwnerRegistration("same-name", () -> { });
        assertEquals(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER,
                registry.registerOwner(alias, callback).status());
        assertFalse(alias.isAvailable());
        assertTrue(first.isAvailable());
    }

    @Test
    void concurrentRegistrationsCannotExceedTheFixedCapacity() throws Exception {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            Thread worker = new Thread(() -> {
                try { start.await(); } catch (InterruptedException failure) { throw new AssertionError(failure); }
                if (registry.register(observer()).accepted()) accepted.incrementAndGet();
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) worker.join(5_000);
        assertTrue(workers.stream().noneMatch(Thread::isAlive));
        assertEquals(32, accepted.get());
        assertEquals(32, registry.snapshot().size());
    }

    @Test
    void repeatingTheSameOwnerReturnsAlreadyRegisteredWithoutAddingCallbacks() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("repeat", () -> { });
        DiagnosticSessionLifecycleObserver first = observer();
        DiagnosticSessionLifecycleObserver second = observer();
        assertEquals(DiagnosticObserverRegistrationStatus.REGISTERED,
                registry.registerOwner(owner, first, second).status());
        assertEquals(DiagnosticObserverRegistrationStatus.ALREADY_REGISTERED,
                registry.registerOwner(owner, first, second).status());
        assertEquals(2, registry.registeredCount());
        assertTrue(owner.isAvailable());
    }

    @Test
    void invalidExpansionDisablesPreviouslyRegisteredCallbacksWithoutPublishingTheNewOne() {
        DiagnosticSessionObserverRegistry registry = new DiagnosticSessionObserverRegistry();
        AtomicInteger calls = new AtomicInteger();
        DiagnosticOwnerRegistration owner = new DiagnosticOwnerRegistration("partial", () -> calls.set(0));
        DiagnosticSessionLifecycleObserver first = new DiagnosticSessionLifecycleObserver() {
            @Override public void beforeModeOff() { calls.incrementAndGet(); }
        };
        registry.registerOwner(owner, first);
        List<DiagnosticSessionLifecycleObserver> captured = registry.snapshot();
        assertEquals(DiagnosticObserverRegistrationStatus.INVALID_OBSERVER,
                registry.registerOwner(owner, first, observer()).status());
        assertFalse(owner.isAvailable());
        assertEquals(1, registry.registeredCount());
        captured.get(0).beforeModeOff();
        assertEquals(0, calls.get());
        assertFalse(registry.registerOwner(owner, first).accepted());
    }

    private static DiagnosticSessionLifecycleObserver observer() { return new DiagnosticSessionLifecycleObserver() { }; }

    private record EqualObserver(int identity) implements DiagnosticSessionLifecycleObserver { }
}
