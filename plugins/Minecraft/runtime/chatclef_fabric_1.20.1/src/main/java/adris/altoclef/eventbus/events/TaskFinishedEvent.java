package adris.altoclef.eventbus.events;

import adris.altoclef.tasksystem.Task;

public class TaskFinishedEvent {
    public double durationSeconds;
    public Task lastTaskRan;
    //#if MC == 12001
    //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
    //20260913_kpopmodder: Optional opaque ownership identity; legacy constructors remain unbound.
    private final Object rootLifetime;
    public TaskFinishedEvent(double durationSeconds, Task lastTaskRan, Object rootLifetime) {
        this.durationSeconds = durationSeconds;
        this.lastTaskRan = lastTaskRan;
        this.rootLifetime = rootLifetime;
    }
    public Object rootLifetime() { return rootLifetime; }
    //#endif

    public TaskFinishedEvent(double durationSeconds, Task lastTaskRan) {
        this.durationSeconds = durationSeconds;
        this.lastTaskRan = lastTaskRan;
        //#if MC == 12001
        rootLifetime = null;
        //#endif
    }
}
