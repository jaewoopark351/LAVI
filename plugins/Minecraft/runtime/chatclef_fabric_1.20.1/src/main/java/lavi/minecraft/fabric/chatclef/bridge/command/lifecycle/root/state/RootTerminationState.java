//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.state;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootReadStatus;
import lavi.minecraft.integration.lifecycle.root.UserRootCompletion;

//20260913_kpopmodder: One execution owns one frozen retirement boundary and a finite admitted-event prefix.
public final class RootTerminationState {
    private UserRootSnapshot bound;
    private UserRootSnapshot retirement;
    private String reason = "";
    private long pendingThrough = -1;
    private long processedThrough;
    private boolean environmentValid;

    public void bind(UserRootSnapshot snapshot, Object root) {
        if (bound == null && snapshot != null && snapshot.status() == UserRootReadStatus.PRESENT
                && snapshot.root() == root && snapshot.lifetime() != null && snapshot.lifetime().owns(root)) {
            bound = snapshot;
            environmentValid = true;
        }
    }
    public boolean bound() { return bound != null; }
    public boolean environmentValid() { return environmentValid; }
    public boolean acceptsEnvironment(UserRootSnapshot snapshot) { return bound != null && bound.sameEnvironment(snapshot); }
    public void validateEnvironment(UserRootSnapshot snapshot) { environmentValid = acceptsEnvironment(snapshot); }
    public boolean accepts(Object task, Object lifetime) {
        return bound != null && task == bound.root() && lifetime == bound.lifetime();
    }
    public void observe(UserRootSnapshot snapshot, long accepted, long processed) {
        if (bound == null) return;
        processedThrough = Math.max(processedThrough, processed);
        validateEnvironment(snapshot);
        if (!environmentValid || retirement != null) return;
        String replacement = snapshot.root() != bound.root()
                ? (snapshot.root() == null ? "command_root_removed" : "command_root_replaced")
                : (snapshot.lifetime() != bound.lifetime() ? "command_callback_ownership_replaced" : "");
        if (!replacement.isEmpty() || bound.lifetime().completion() != null) {
            retirement = snapshot;
            reason = replacement;
            pendingThrough = accepted;
        }
    }
    public boolean pending() { return retirement != null && processedThrough < pendingThrough; }
    public boolean ready() { return retirement != null && environmentValid && !pending(); }
    public String reason() { return reason; }
    public long pendingThrough() { return pendingThrough; }
    public UserRootSnapshot boundSnapshot() { return bound; }
    public UserRootSnapshot retirementSnapshot() { return retirement; }
    public UserRootCompletion completion() { return bound == null ? null : bound.lifetime().completion(); }
}
//#endif
