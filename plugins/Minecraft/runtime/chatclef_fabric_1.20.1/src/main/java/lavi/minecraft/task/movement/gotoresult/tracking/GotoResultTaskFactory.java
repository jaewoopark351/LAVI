//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.tracking;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.GotoTarget;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;

//20260913_kpopmodder: Observe only direct same-dimension XYZ command roots; shared task factories stay native.
public final class GotoResultTaskFactory {
    private GotoResultTaskFactory() { }

    public static Task observeLegacy(AltoClef mod, GotoTarget request, Task task) {
        if (task.getClass() != GetToBlockTask.class || request.getType() != GotoTarget.GotoTargetCoordType.XYZ
                || mod.getWorld() == null || mod.getPlayer() == null
                || (request.getDimension() != null && request.getDimension() != WorldHelper.getCurrentDimension())) {
            return task;
        }
        return new ReportedGotoBlockTask(mod, request);
    }
}
//#endif
