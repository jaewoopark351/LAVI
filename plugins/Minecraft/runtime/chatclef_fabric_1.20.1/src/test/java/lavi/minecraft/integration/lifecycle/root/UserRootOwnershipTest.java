package lavi.minecraft.integration.lifecycle.root;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise actual ownership transitions independently of game scheduling.
class UserRootOwnershipTest {
    @Test void retainedRootWithIdenticalCallbackKeepsLifetime() {
        var owner = new UserRootOwnership();
        Object root = new Object(), callback = new Object(), world = new Object(), player = new Object();
        owner.assigned(root, callback, world, player);
        var before = owner.currentFor(root);
        var invocation = owner.invocationFor(root);
        owner.assigned(root, callback, world, player);
        assertSame(before, owner.currentFor(root));
        assertNotSame(invocation, owner.invocationFor(root), "new invocation resets local operation budgets without retiring its root");
    }
    @Test void callbackReplacementAndWorldReuseHaveDifferentLifetimes() {
        var owner = new UserRootOwnership();
        Object root = new Object(), world = new Object(), player = new Object(), nextCallback = new Object();
        owner.assigned(root, new Object(), world, player);
        var before = owner.currentFor(root);
        owner.assigned(root, nextCallback, world, player);
        var afterCallback = owner.currentFor(root);
        assertNotSame(before, afterCallback);
        owner.assigned(root, nextCallback, new Object(), player);
        assertNotSame(afterCallback, owner.currentFor(root));
    }
    @Test void completingOldLifetimeNeverClearsNewAssignment() {
        var owner = new UserRootOwnership();
        Object oldRoot = new Object(), newRoot = new Object();
        owner.assigned(oldRoot, null, null, null);
        var finishing = owner.currentFor(oldRoot);
        owner.assigned(newRoot, null, null, null);
        var replacement = owner.currentFor(newRoot);
        owner.finished(finishing, UserRootCompletion.known(false));
        assertSame(replacement, owner.currentFor(newRoot));
        assertFalse(finishing.completion().stopped());
        owner.finished(finishing, UserRootCompletion.known(true));
        assertFalse(finishing.completion().stopped(), "late completion must not change the frozen result");
    }
    @Test void completingAndReusingSameTaskStartsANewLifetime() {
        var owner = new UserRootOwnership();
        Object root = new Object();
        owner.assigned(root, null, null, null);
        var previous = owner.currentFor(root);
        owner.finished(previous, UserRootCompletion.known(false));
        assertNull(owner.currentFor(root));
        owner.assigned(root, null, null, null);
        assertNotSame(previous, owner.currentFor(root));
    }
}
