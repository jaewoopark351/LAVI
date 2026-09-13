//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.tracking;

import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;

//20260913_kpopmodder: A terminal decision belongs to one task and cannot be overwritten by a later callback.
public final class GotoTerminalState {
    private GotoTerminalSnapshot terminal;

    public void commit(GotoTerminalSnapshot decision) {
        if (terminal == null && decision != null) terminal = decision;
    }

    public GotoTerminalSnapshot snapshot() {
        return terminal;
    }
}
//#endif
