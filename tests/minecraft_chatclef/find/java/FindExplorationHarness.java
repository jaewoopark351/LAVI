package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasksystem.*;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import baritone.api.pathing.goals.GoalBlock;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;

import java.util.*;

import static lavi.minecraft.task.find.FindRuntimeHarness.*;

/** Executes real TaskRunner arbitration and real recursive Task interruption/resumption. */
public class FindExplorationHarness {
    private static class DefenseChain extends SingleTaskChain {
        boolean demand;
        int priority = 65;
        int priorityEvaluations;
        DefenseChain(TaskRunner runner, Task task) { super(runner); setTask(task); }
        @Override public float getPriority() {
            priorityEvaluations++;
            AltoClef.getInstance().defense.claimed = demand;
            return demand ? priority : 0;
        }
        @Override protected void onTaskFinish(AltoClef m) { mainTask.stop(); mainTask = null; }
        @Override public String getName() { return "Simulated automatic defense demand"; }
    }
    private static class DefendTask extends Task {
        final Entity victim;
        int hits;
        boolean cleanupObserved;
        DefendTask(Entity victim) { this.victim = victim; }
        @Override protected void onStart() {
            var m = AltoClef.getInstance();
            cleanupObserved = m.behaviour.depth == 1 && !m.baritone.process.active && !m.baritone.explore.active;
        }
        @Override protected Task onTick() { if (victim != null) victim.alive = false; hits++; return null; }
        @Override protected void onStop(Task t) {}
        @Override protected boolean isEqual(Task t) { return this == t; }
        @Override protected String toDebugString() { return "Simulated defense kill"; }
    }
    private static Object field(Object owner, String name) throws Exception {
        var f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    private static boolean logged(String reason) {
        return ChatClefDiagnostics.records.stream().anyMatch(r -> reason.equals(r.get("reason")));
    }
    private static int terminalCount() {
        return (int) ChatClefDiagnostics.records.stream().filter(r -> "terminal".equals(r.get("reason"))).count();
    }

    public static void main(String[] args) throws Exception {
        int initialChecks = checks;
        var m = reset(); var t = start("find 마을 주민");
        check(t.outcome() == null, "initial empty observation must NOT be terminal");
        check(m.baritone.explore.active && m.baritone.explore.starts == 1, "missing target actually starts native exploration");
        check(t.thisOrChildSatisfies(c -> c instanceof DoToClosestEntityTask), "reuses the actual ATTACK selection class");
        check(m.behaviour.depth == 0, "exploration keeps default movement and defense policies");
        for (int i = 0; i < 30; i++) tick(t);
        check(m.baritone.explore.starts == 1, "reobservation never restarts an active exploration process each tick");
        check(m.entities.observations >= 31, "tracker is reobserved while wandering");
        var beforeTime = field(t, "startedNanos");
        m.player.pos = new Vec3d(80, 64, .5);
        var e = entity(m, 100); tick(t);
        check(!m.baritone.explore.active && m.baritone.explore.releases == 1, "exploration relinquishes before approach starts");
        check(m.baritone.process.active && m.baritone.process.starts == 1, "late-loaded resident selects movement leaf");
        check(t.outcome() == null, "observation alone is not arrival");
        check(field(t, "startedNanos").equals(beforeTime), "moving scan center does not reset operation identity/start time");
        m.player.pos = new Vec3d(98,64,.5); tick(t); code(t,"ARRIVED");
        check(t.outcome().position().equals(List.of(100,64,0)), "can find beyond original fixed 64-block scope");
        check(!m.baritone.process.active && m.baritone.path.cancels == 0, "cleanup uses only owned process, no forceCancel");
        check(logged("explore_started") && logged("selected") && logged("terminal"), "causal transitions carry FIND records");
        String operation = t.outcome().operationId();
        check(ChatClefDiagnostics.records.stream().allMatch(r -> operation.equals(r.get("findOperationId"))), "same operation correlates all child boundaries");

        // Real scheduler chooses 65-priority defense instead of 50-priority FIND; defense can kill the target.
        m = reset(); e = new Entity(zombie,10,20,64,.5); m.world.entities.add(e);
        var runner = new TaskRunner(m); var user = new HarnessChain(runner);
        t = new FindTask(FindRequest.parse("find 좀비")); user.setTask(t);
        var defenseTask = new DefendTask(e); var defense = new DefenseChain(runner, defenseTask);
        runner.enable(); runner.tick();
        check(runner.getCurrentTaskChain() == user && m.baritone.process.active, "FIND runs when defense has no demand");
        beforeTime = field(t,"startedNanos"); defense.demand = true; runner.tick();
        check(runner.getCurrentTaskChain() == defense, "automatic defense preempts FIND by native priority");
        check(defenseTask.cleanupObserved, "FIND releases its movement before the defense task starts");
        check(!e.alive && defenseTask.hits == 1, "automatic defense may kill the currently sought zombie");
        check(t.outcome() == null, "defense kill does not mark FIND as completed or failed");
        defense.demand = false; runner.tick();
        check(runner.getCurrentTaskChain() == user && m.baritone.explore.active, "after defense kill, FIND returns to exploration");
        check(logged("target_lost_research"), "death invalidation and re-search are observable");
        check(field(t,"startedNanos").equals(beforeTime), "defense preemption preserves operation start time");
        m.world.entities.add(new Entity(zombie,20,2,64,.5)); runner.tick(); code(t,"ARRIVED");
        check(t.outcome().entityUuid().equals(m.world.entities.get(2).getUuid().toString()), "only newly live target may satisfy arrival");
        check(defense.priorityEvaluations == 4, "FIND does not reevaluate defense priority for polling");
        runner.tick(); runner.disable(); check(m.behaviour.depth == 0, "scheduler shutdown leaves no FIND stack state");

        // Repeated defense/flee preemption preserves ownership and the original request identity.
        m=reset(); runner=new TaskRunner(m); user=new HarnessChain(runner);
        t=new FindTask(FindRequest.parse("find 주민")); user.setTask(t);
        defenseTask=new DefendTask(null); defense=new DefenseChain(runner,defenseTask);
        runner.enable(); runner.tick(); beforeTime=field(t,"startedNanos");
        for(int i=0;i<6;i++) {
            defense.priority=i%2==0?65:80; defense.demand=true; runner.tick();
            check(runner.getCurrentTaskChain()==defense && !m.baritone.explore.active,"defense/flee keeps precedence over exploring");
            defense.demand=false; runner.tick();
            check(runner.getCurrentTaskChain()==user && m.baritone.explore.active,"same FIND resumes after repeated defense");
        }
        check(field(t,"startedNanos").equals(beforeTime),"repeated preemption preserves operation start time");
        check(t.outcome()==null && m.baritone.path.cancels==0,"yield is neither completion nor global cancellation");
        runner.disable(); check(m.behaviour.depth==0 && !m.baritone.explore.active,"disable cleans the active leaf");

        // An aura/shield input claim can happen even without a higher-priority chain.
        m = reset(); e = entity(m,20); t = start("find 주민");
        m.defense.claimed = true; tick(t);
        check(!m.baritone.process.active && t.outcome() == null, "force-field input claim yields movement without terminal outcome");
        var external = new GoalBlock(new BlockPos(30,64,0)); m.baritone.process.setGoalAndPath(external);
        tick(t); check(m.baritone.process.getGoal() == external, "defense-owned goal survives repeated yielding");
        m.defense.claimed = false; tick(t);
        check(m.baritone.process.getGoal() == external && m.behaviour.depth == 0, "FIND neither steals foreign goal nor imposes policy while busy");
        m.baritone.process.onLostControl(); tick(t);
        check(m.baritone.process.active && m.baritone.process.getGoal() != external, "FIND resumes after foreign process releases");
        t.stop(); check(logged("defense_yield") && logged("defense_resume"), "aura-level yield/resume are logged");

        m = reset(); e = entity(m,20); t = start("find 주민");
        e.pos = new Vec3d(26,64,.5); tick(t);
        check(m.baritone.process.starts == 2, "moving entity goal is replanned by the same owner");
        m.player.pos = new Vec3d(18,64,.5); tick(t);
        check(t.outcome() == null, "stale position is not accepted as arrival");
        m.player.pos = new Vec3d(24,64,.5); tick(t); code(t,"ARRIVED");

        // Blocks use the native scanner/closest-object loop, not a fixed one-shot cube.
        m = reset(); t = start("find block 상자");
        check(m.baritone.explore.active && t.thisOrChildSatisfies(c -> c instanceof DoToClosestBlockTask), "missing block uses native block selector and explore");
        m.player.pos = new Vec3d(80,64,.5); var chestPos = new BlockPos(100,64,0);
        m.world.blocks.put(chestPos,new BlockState(chest,true)); tick(t);
        check(!m.baritone.explore.active && m.baritone.process.active, "newly observed block switches to approach beyond original cube");
        check(m.behaviour.breaking.test(chestPos) && !m.behaviour.breaking.test(new BlockPos(99,64,0)), "only selected block is protected; native navigation remains usable");
        m.defense.claimed = true; tick(t);
        check(m.behaviour.depth == 0 && m.behaviour.breaking == null, "block target protection is gone before defense movement");
        m.world.blocks.clear(); m.defense.claimed = false; tick(t);
        check(m.baritone.explore.active && t.outcome() == null, "removed block returns to exploration");
        expire(t); tick(t); check(!t.isFinished() && m.baritone.explore.active,"block search survives old lifetime");
        m.world.blocks.put(new BlockPos(82,64,0),new BlockState(chest,true)); tick(t); code(t,"ARRIVED");

        // Runtime air-state types are absent from the native block index, but remain findable.
        m = reset(); t = start("find block minecraft:air"); code(t,"ARRIVED");
        check(!m.baritone.explore.active,"air at live player feet is found without artificial index entries");
        m = reset();
        var caveAir = net.minecraft.registry.Registries.BLOCK.add("minecraft:cave_air",
                new net.minecraft.block.Block("block.minecraft.cave_air"));
        m.world.blocks.put(new BlockPos(8,64,0),new BlockState(caveAir,false));
        t = start("find block minecraft:cave_air");
        for (int i=0;i<1000 && !m.baritone.process.active;i++) tick(t);
        check(m.baritone.process.active && t.outcome()==null,"unindexed air subtype selected by bounded live probe");
        m.player.pos = new Vec3d(6,64,.5); tick(t); code(t,"ARRIVED");

        // Dropped item identity is the contained ITEM id, and disappearance re-enters the same search.
        m = reset(); t = start("find item 다이아몬드");
        var drop = new ItemEntity(itemType,10,new ItemStack(diamond),20,64,.5); m.world.entities.add(drop); tick(t);
        check(m.baritone.process.active && m.behaviour.walking != null, "dropped item discovered after exploration");
        drop.stack.count=0; tick(t);
        check(m.baritone.explore.active && m.behaviour.depth == 0, "collected/empty drop does not freeze old movement policy");
        drop.stack.count=1; drop.pos=new Vec3d(2,64,.5); tick(t); code(t,"ARRIVED");

        // STOP and stale owners: no replay, no late cleanup of another operation's goal.
        m = reset(); t = start("find 주민"); chains.get(t).stop();
        check(!m.baritone.explore.active, "STOP releases ongoing explore"); int starts=m.baritone.explore.starts;
        chains.get(t).tick(); check(m.baritone.explore.starts==starts, "stopped user chain cannot replay FIND");
        entity(m,20); var next=start("find 주민"); var nextGoal=m.baritone.process.getGoal(); t.stop();
        check(m.baritone.process.getGoal()==nextGoal, "late old-root cleanup cannot clear next request goal"); next.stop();

        m = reset(); entity(m,20); t = start("find 주민");
        m.player=new PlayerEntity(playerType,2,"LAVI",.5,64,.5); tick(t); code(t,"SCOPE_CHANGED");
        check(!m.baritone.process.active && m.behaviour.depth==0, "player replacement aborts cleanly");
        m = reset(); t=start("find 주민"); t.interrupt(null); expire(t); tick(t);
        check(!t.isFinished() && m.baritone.explore.active, "same search resumes even after old lifetime");
        chains.get(t).stop(); check(!m.baritone.explore.active,"STOP still releases continuous search");

        // Continuous search: elapsed wall time alone is not evidence that a target cannot be found.
        m=reset(); t=start("find 마을 주민"); expire(t);
        for(int i=0;i<100;i++) tick(t);
        check(!t.isFinished() && m.baritone.explore.starts==1,"search continues after former 90-second cutoff without resubmission");
        entity(m,20); tick(t);
        check(!t.isFinished() && m.baritone.process.active,"resident loaded after deadline gets a fresh approach");
        check((long)field(t,"approachActiveNanos")==0L,"search time is excluded from approach budget");
        m.player.pos=new Vec3d(18,64,.5); tick(t); code(t,"ARRIVED");

        // Report remains a stationary bounded query, not an endless watcher.
        m=reset(); t=start("find block 상자 report"); expire(t); tick(t); code(t,"SEARCH_LIMIT");
        check(!m.baritone.explore.active && !m.baritone.process.active,"report deadline never starts movement");

        // Real TaskRunner interruption excludes defense wall time without resetting time already spent approaching.
        m=reset(); entity(m,20); runner=new TaskRunner(m); user=new HarnessChain(runner);
        t=new FindTask(FindRequest.parse("find 주민")); user.setTask(t);
        defenseTask=new DefendTask(null); defense=new DefenseChain(runner,defenseTask);
        runner.enable(); runner.tick(); runner.tick();
        var checkpoint=FindTask.class.getDeclaredField("approachTickNanos"); checkpoint.setAccessible(true);
        checkpoint.setLong(t,System.nanoTime()-45_000_000_000L); runner.tick();
        long spent=(long)field(t,"approachActiveNanos");
        check(spent>=45_000_000_000L && !t.isFinished(),"selected-target active time is counted");
        defense.demand=true; runner.tick();
        check(!(boolean)field(t,"approachClockRunning"),"native defense preemption pauses approach clock");
        checkpoint.setLong(t,System.nanoTime()-180_000_000_000L); expire(t);
        defense.demand=false; runner.tick();
        check(!t.isFinished() && m.baritone.process.active,"long defense does not abort resident approach");
        check((long)field(t,"approachActiveNanos")==spent,"resume retains spent budget but excludes defense interval");
        m.player.pos=new Vec3d(18,64,.5); runner.tick(); code(t,"ARRIVED"); runner.disable();

        // Force-field/shield claims are equally authoritative, even when the user chain stays selected.
        m=reset(); entity(m,20); t=start("find 주민"); tick(t);
        checkpoint.setLong(t,System.nanoTime()-30_000_000_000L); tick(t);
        spent=(long)field(t,"approachActiveNanos"); m.defense.claimed=true; tick(t);
        checkpoint.setLong(t,System.nanoTime()-180_000_000_000L); expire(t); tick(t);
        m.defense.claimed=false; tick(t);
        check(!t.isFinished() && (long)field(t,"approachActiveNanos")==spent,"aura/shield wait is excluded from approach budget");
        expireApproach(t); m.defense.claimed=true; tick(t);
        check(!t.isFinished(),"expired approach budget cannot interrupt a current defense input claim");
        m.defense.claimed=false; tick(t); code(t,"APPROACH_TIMEOUT");
        check(!m.baritone.process.active && m.behaviour.depth==0,"real approach timeout cleans only FIND state");
        check(logged("approach_time_limit"),"approach timeout reason and budget are logged");

        // Defense may kill the selected zombie long after search begins; a different live zombie can complete it.
        m=reset(); e=new Entity(zombie,10,20,64,.5); m.world.entities.add(e); t=start("find 좀비");
        expire(t); m.defense.claimed=true; tick(t); e.alive=false; m.defense.claimed=false; tick(t);
        check(!t.isFinished() && m.baritone.explore.active,"defense kill reenters continuous search beyond old lifetime");
        check((long)field(t,"approachActiveNanos")==0L,"lost target clears only its own approach budget");
        m.world.entities.add(new Entity(zombie,11,2,64,.5)); tick(t); code(t,"ARRIVED");

        // Resource caps still terminate honestly instead of converting incomplete scans into NOT_FOUND.
        m=reset(); for(int i=0;i<4097;i++) m.world.entities.add(new Entity(zombie,i+20,3,64,3));
        t=start("find 주민"); code(t,"SEARCH_LIMIT");
        check(!m.baritone.explore.active && !t.outcome().scanComplete(),"continuous mode retains per-observation entity cap");

        // A partial movement-start failure must not leak process or policy ownership.
        m = reset(); m.baritone.explore.failStart=true; t=start("find 주민"); code(t,"INTERNAL_ERROR");
        check(!m.baritone.explore.active && m.behaviour.depth==0, "partial exploration failure unwinds local state");
        m = reset(); m.world.blocks.put(new BlockPos(20,64,0),new BlockState(chest,true));
        m.baritone.process.failStart=true; t=start("find block 상자"); code(t,"INTERNAL_ERROR");
        check(!m.baritone.process.active && m.behaviour.depth==0, "partial goal failure releases policy and own goal");
        m = reset(); m.world.blocks.put(new BlockPos(20,64,0),new BlockState(chest,true));
        m.behaviour.failConfigure=true; t=start("find block 상자"); code(t,"INTERNAL_ERROR");
        check(m.behaviour.depth==0 && !m.baritone.process.active, "policy failure does not leak a behavior frame");

        // Diagnostics are bounded and changing diagnostics alone cannot change gameplay decisions.
        for (int mode=0;mode<3;mode++) {
            m=reset(); ChatClefDiagnostics.off=mode==1; ChatClefDiagnostics.fail=mode==2;
            t=start("find 주민"); expire(t); for(int i=0;i<100;i++)tick(t);
            check(!t.isFinished()&&m.baritone.explore.starts==1,"all diagnostic modes retain exploration");
            entity(m,2); tick(t); code(t,"ARRIVED");
            check(m.baritone.path.cancels==0&&m.behaviour.depth==0,"diagnostics do not alter cleanup");
            if(mode==0) {
                check(ChatClefDiagnostics.records.size()<50,"repeated observation detail has a finite local cap");
                check(terminalCount()==1,"terminal survives repeat-detail suppression");
            } else if(mode==1) check(ChatClefDiagnostics.records.isEmpty(),"OFF emits no local records");
            ChatClefDiagnostics.fail=false;
        }
        System.out.println("REAL_TASK_ENGINE_CHECKS="+(checks-initialChecks)+"; COMBAT/CHUNKS/PATH EXECUTION SIMULATED, NOT LIVE GAME");
    }
}
