package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.UUID;

//20260728_kpopmodder: Keep local obstacle removal bounded so recovery does not turn into animal hunting.
public class ClearBlockingEntityTask extends Task {
    private final Entity target;
    private final Vec3d origin;
    private final double maxChaseRange;
    private final TimerGame timeout;
    private final StateChangeLogger debugLogger = new StateChangeLogger("ClearBlockingEntityTask");
    private boolean finished;
    private boolean timedOut;

    public ClearBlockingEntityTask(Entity target, Vec3d origin, double maxChaseRange, double timeoutSeconds) {
        this.target = target;
        this.origin = origin;
        this.maxChaseRange = maxChaseRange;
        this.timeout = new TimerGame(timeoutSeconds);
    }

    @Override
    protected void onStart() {
        timeout.reset();
        finished = false;
        timedOut = false;
        debugLogger.event("start: target=" + describeTarget(AltoClef.getInstance())
                + ", origin=" + formatVec(origin));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (target == null || !target.isAlive()) {
            finished = true;
            debugLogger.event("finished: target gone");
            return null;
        }
        if (!target.getPos().isInRange(mod.getPlayer().getPos(), maxChaseRange)) {
            finished = true;
            debugLogger.event("finished: target no longer blocking: " + describeTarget(mod));
            return null;
        }
        if (!target.getPos().isInRange(origin, maxChaseRange + 1.0)) {
            finished = true;
            debugLogger.event("finished: target left local recovery area: " + describeTarget(mod));
            return null;
        }
        if (timeout.elapsed()) {
            timedOut = true;
            finished = true;
            debugLogger.event("timed out: " + describeTarget(mod));
            return null;
        }

        setDebugState("Clearing " + target.getType().getTranslationKey());
        return new KillEntityTask(target, 0, maxChaseRange, 0);
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    public boolean didTimeOut() {
        return timedOut;
    }

    public UUID getTargetUuid() {
        return target == null ? null : target.getUuid();
    }

    public String describeTarget(AltoClef mod) {
        if (target == null) {
            return "none";
        }
        String playerPos = mod == null || mod.getPlayer() == null
                ? "unknown"
                : mod.getPlayer().getBlockPos().toShortString();
        return target.getType().getTranslationKey()
                + " uuid=" + target.getUuid()
                + ", entityPos=" + target.getBlockPos().toShortString()
                + ", playerPos=" + playerPos
                + ", alive=" + target.isAlive();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ClearBlockingEntityTask task) {
            return target != null && target.equals(task.target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Clear blocking entity " + (target == null ? "none" : target.getType().getTranslationKey());
    }

    private String formatVec(Vec3d vec) {
        if (vec == null) {
            return "none";
        }
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }
}
