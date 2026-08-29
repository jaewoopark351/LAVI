package lavi.minecraft.task.container.home.execution.state.session;

import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only the active exact-container session reference.
public final class HomeStorageSessionState {
    private HomeStorageContainerSession current;

    public HomeStorageContainerSession current() {
        return current;
    }

    public void activate(HomeStorageContainerSession session) {
        current = Objects.requireNonNull(session, "session");
    }

    public void replace(HomeStorageContainerSession session) {
        current = Objects.requireNonNull(session, "session");
    }

    public void clear() {
        current = null;
    }
}
