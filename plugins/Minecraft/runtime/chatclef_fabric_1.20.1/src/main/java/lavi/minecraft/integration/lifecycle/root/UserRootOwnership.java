//#if MC == 12001
package lavi.minecraft.integration.lifecycle.root;

//20260913_kpopmodder: Keep ownership in its user chain; equality-retained roots can still replace callbacks.
public final class UserRootOwnership {
    private volatile UserRootLifetime current;
    private volatile Object invocation;

    public void assigned(Object root, Object callback, Object world, Object player) {
        // A new invocation can retain the same actual root/callback; it must not imply root retirement.
        invocation = root == null ? null : new Object();
        if (root == null) {
            current = null;
        } else if (current == null || !current.matches(root, callback, world, player)) {
            current = new UserRootLifetime(root, callback, world, player);
        }
    }
    public UserRootLifetime currentFor(Object root) {
        UserRootLifetime value = current;
        return value != null && value.owns(root) ? value : null;
    }
    public Object invocationFor(Object root) { return currentFor(root) == null ? null : invocation; }
    public void finished(UserRootLifetime lifetime, UserRootCompletion completion) {
        if (lifetime == null || completion == null) return;
        lifetime.complete(completion);
        if (current == lifetime) { current = null; invocation = null; }
    }
}
//#endif
