//#if MC == 12001
package lavi.minecraft.integration.lifecycle.root;

//20260913_kpopmodder: Identify one actual root/callback ownership lifetime independently of diagnostics.
public final class UserRootLifetime {
    private final Object root;
    private final Object callback;
    private final Object world;
    private final Object player;
    private volatile UserRootCompletion completion;

    UserRootLifetime(Object root, Object callback, Object world, Object player) {
        this.root = root;
        this.callback = callback;
        this.world = world;
        this.player = player;
    }

    public boolean owns(Object candidate) { return root == candidate && candidate != null; }
    boolean matches(Object candidate, Object candidateCallback, Object candidateWorld, Object candidatePlayer) {
        return owns(candidate) && callback == candidateCallback && world == candidateWorld
                && player == candidatePlayer && completion == null;
    }
    public UserRootCompletion completion() { return completion; }
    void complete(UserRootCompletion value) {
        if (completion == null) completion = value;
    }
}
//#endif
