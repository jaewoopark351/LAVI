package lavi.minecraft.task.container.deposit.auto.progress;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.budget.*;
import lavi.minecraft.task.container.deposit.auto.rearm.*;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.AutoDepositConditions;
import java.util.*;

//20260915_kpopmodder: Real production policy/math checks runnable with javac or the JUnit wrapper.
public final class AutoDepositProgressContractChecks {
    private static int passed;
    private static final AutoDepositProgressSample.Target A = new AutoDepositProgressSample.Target(-4051, 63, -947);
    private static final AutoDepositProgressSample.Target B = new AutoDepositProgressSample.Target(-4094, 62, -924);
    private static AutoDepositProgressSample nav(AutoDepositProgressSample.Target target, double distance) {
        return new AutoDepositProgressSample(AutoDepositProgressSample.Phase.APPROACH_CONTAINER,
                Optional.of(new AutoDepositProgressSample.Navigation(target, distance)), List.of());
    }
    private static AutoDepositProgressSample resource(String key, int held, int needed) {
        return new AutoDepositProgressSample(AutoDepositProgressSample.Phase.PREPARE_CONTAINER, Optional.empty(),
                List.of(new AutoDepositProgressSample.Resource(key, held, needed)));
    }
    private static void require(boolean ok) { if (!ok) throw new AssertionError("contract failed"); }
    private static void invalid(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid fact accepted");
    }
    private static void check(String name, Runnable run) {
        try { run.run(); passed++; System.out.println("PASS core " + name); }
        catch (Throwable failure) { throw new AssertionError(name, failure); }
    }
    public static void main(String[] args) {
        passed = 0;
        check("new target is baseline, not progress", () -> {
            require(!new AutoDepositProgressTracker().observe(nav(A, 60)).progressed());
        });
        check("one block toward the same target counts", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A, 60));
            require(t.observe(nav(A, 59)).navigationAdvanced());
        });
        check("sub-block jitter cannot reset watchdog", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A, 60));
            for (int i=0;i<500;i++) require(!t.observe(nav(A, i%2==0 ? 59.7 : 60.3)).progressed());
        });
        check("small forward deltas accumulate to one block", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A,60));
            require(!t.observe(nav(A,59.6)).progressed()); require(!t.observe(nav(A,59.1)).progressed());
            require(t.observe(nav(A,59)).progressed());
        });
        check("backtracking to an old minimum is not new progress", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A,60)); require(t.observe(nav(A,55)).progressed());
            require(!t.observe(nav(A,60)).progressed()); require(!t.observe(nav(A,55)).progressed());
        });
        check("new coordinate object does not reset semantic target", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A,60)); t.observe(nav(A,55));
            var equivalent = new AutoDepositProgressSample.Target(-4051,63,-947);
            require(!t.observe(nav(equivalent,59)).progressed()); require(!t.observe(nav(equivalent,55)).progressed());
        });
        check("target alternation alone never grants progress", () -> {
            var t = new AutoDepositProgressTracker();
            for(int i=0;i<300;i++) require(!t.observe(nav(i%2==0 ? A : B, 20)).progressed());
        });
        check("returning to an old goal preserves its watermark", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A,50)); t.observe(nav(A,40));
            t.observe(nav(B,30)); require(!t.observe(nav(A,45)).progressed());
            require(!t.observe(nav(A,40)).progressed()); require(t.observe(nav(A,39)).progressed());
        });
        check("defense movement is rebaselined without credit", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A,50)); t.breakContinuity();
            require(!t.observe(nav(A,20)).progressed()); require(t.observe(nav(A,19)).progressed());
        });
        check("missing goal observation breaks attribution", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(nav(A,50));
            t.observe(AutoDepositProgressSample.UNAVAILABLE);
            require(!t.observe(nav(A,20)).progressed()); require(t.observe(nav(A,19)).progressed());
        });
        check("first inventory sample is baseline", () -> {
            require(!new AutoDepositProgressTracker().observe(resource("minecraft:oak_planks",7,8)).progressed());
        });
        check("actual needed material increase counts separately", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("minecraft:oak_planks",1,8));
            var r = t.observe(resource("minecraft:oak_planks",4,8));
            require(r.resourceAdvanced() && !r.navigationAdvanced());
            require(r.resourceBestBefore()==1 && r.resourceHeldAfter()==4 && r.resourceLimit()==8);
        });
        check("consumption and refill cannot mint new material progress", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("planks",0,8));
            require(t.observe(resource("planks",8,8)).progressed());
            for(int i=0;i<100;i++) require(!t.observe(resource("planks",i%2==0 ? 0:8,8)).progressed());
        });
        check("self-expanded requirement cannot expand the first resource limit", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("planks",0,8)); t.observe(resource("planks",8,8));
            require(!t.observe(resource("planks",64,128)).progressed());
        });
        check("excess above the current requirement is not progress", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("planks",1,8));
            require(!t.observe(resource("planks",64,1)).progressed());
        });
        check("new unrelated resource key is only a baseline", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("planks",0,8));
            require(!t.observe(resource("cobblestone",64,64)).progressed());
        });
        check("duplicate resource groups count at most once per sample", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("planks",0,8));
            var fact = new AutoDepositProgressSample.Resource("planks",4,8);
            var r = t.observe(new AutoDepositProgressSample(AutoDepositProgressSample.Phase.PREPARE_CONTAINER,
                    Optional.empty(),List.of(fact,fact)));
            require(r.advancedResourceCount()==1);
        });
        check("resources acquired in a safety gap are not credited", () -> {
            var t = new AutoDepositProgressTracker(); t.observe(resource("planks",0,8)); t.breakContinuity();
            require(!t.observe(resource("planks",4,8)).progressed()); require(t.observe(resource("planks",5,8)).progressed());
        });
        check("navigation table is bounded without eviction", () -> {
            var t = new AutoDepositProgressTracker();
            for(int i=0;i<64;i++) t.observe(nav(new AutoDepositProgressSample.Target(i,0,0),50));
            var r = t.observe(nav(new AutoDepositProgressSample.Target(65,0,0),50));
            require(r.capacityReached() && r.trackedTargets()==64 && !r.progressed());
            require(!t.observe(nav(new AutoDepositProgressSample.Target(0,0,0),50)).progressed());
        });
        check("resource table is bounded without eviction", () -> {
            var t = new AutoDepositProgressTracker(); for(int i=0;i<64;i++) t.observe(resource("item"+i,0,8));
            var r=t.observe(resource("overflow",8,8)); require(r.capacityReached() && r.trackedResources()==64 && !r.progressed());
        });
        check("non-finite and negative distances are rejected", () -> {
            invalid(()->nav(A,Double.NaN)); invalid(()->nav(A,Double.POSITIVE_INFINITY)); invalid(()->nav(A,-1));
        });
        check("invalid resources and unavailable evidence are rejected", () -> {
            invalid(()->resource("",0,1)); invalid(()->resource("a",-1,1)); invalid(()->resource("a",0,0));
            invalid(()->resource("x".repeat(4097),0,1));
            invalid(()->new AutoDepositProgressSample(AutoDepositProgressSample.Phase.UNAVAILABLE,
                    Optional.of(new AutoDepositProgressSample.Navigation(A,1)),List.of()));
        });
        check("sample defensively copies material list", () -> {
            var list=new ArrayList<AutoDepositProgressSample.Resource>(); list.add(new AutoDepositProgressSample.Resource("a",0,1));
            var s=new AutoDepositProgressSample(AutoDepositProgressSample.Phase.PREPARE_CONTAINER,Optional.empty(),list);
            list.clear(); require(s.resources().size()==1);
        });
        check("reproduced 2400-tick ascent continues without fabricated transfer", () -> {
            var t=new AutoDepositProgressTracker(); var b=new AutoDepositExecutionBudget();
            for(int i=0;i<2400;i++) {
                var sample=i<2034 ? nav(B,90-(i/40)) : nav(A,50-((i-2034)/20));
                require(b.onExecutionTick(t.observe(sample).progressed())==AutoDepositBudgetStatus.AVAILABLE);
            }
            require(b.consumedExecutionTicks()==2400 && b.consecutiveNoProgressTicks()<2400);
            require(b.completedUnits()==0 && b.recoveryGrants()==0);
        });
        check("real stationary work still stops at 2400", () -> {
            var t=new AutoDepositProgressTracker(); var b=new AutoDepositExecutionBudget();
            for(int i=1;i<2400;i++) require(b.onExecutionTick(t.observe(nav(A,50)).progressed())==AutoDepositBudgetStatus.AVAILABLE);
            require(b.onExecutionTick(t.observe(nav(A,50)).progressed())==AutoDepositBudgetStatus.NO_PROGRESS_LIMIT);
        });
        check("preparation cannot bypass the unchanged 12000-tick hard limit", () -> {
            var t=new AutoDepositProgressTracker(); var b=new AutoDepositExecutionBudget();
            for(int i=0;i<11999;i++) require(b.onExecutionTick(t.observe(nav(A,20000-i)).progressed())==AutoDepositBudgetStatus.AVAILABLE);
            require(b.onExecutionTick(t.observe(nav(A,8001)).progressed())==AutoDepositBudgetStatus.EXECUTION_TICK_LIMIT);
            require(b.consumedExecutionTicks()==12000);
        });
        check("progress clears only consecutive, not consumed or cumulative work", () -> {
            var b=new AutoDepositExecutionBudget(); for(int i=0;i<100;i++) b.onExecutionTick(false);
            b.onExecutionTick(true); require(b.consecutiveNoProgressTicks()==0);
            require(b.consumedExecutionTicks()==101 && b.cumulativeNoProgressTicks()==100);
        });
        check("progress cannot revive an already exhausted budget", () -> {
            var b=new AutoDepositExecutionBudget(50,2,3,0,10); b.onExecutionTick(false); b.onExecutionTick(false);
            require(b.onExecutionTick(true)==AutoDepositBudgetStatus.NO_PROGRESS_LIMIT && b.consumedExecutionTicks()==2);
        });
        check("inventory 34 to 36 alone retains SAME_FAILURE", () -> {
            var p=new AutoDepositRearmPolicy();
            var before=new AutoDepositConditions("automatic",Map.of("dirt",64),Map.of(),Map.of(),List.of("general:A"));
            var after=new AutoDepositConditions("automatic",Map.of("dirt",128,"stone",64),Map.of(),Map.of(),List.of("general:A"));
            String reason="BUDGET_EXHAUSTED:NO_PROGRESS_LIMIT";
            p.beginUnit(new DepositAllInventoryPressureSnapshot(34,36),before.conditionKey(reason),before.scope());
            p.finishUnit(false,new DepositAllInventoryPressureSnapshot(36,36),reason,before.scope(),before.conditionKey(reason));
            require(p.observe(new DepositAllInventoryPressureSnapshot(36,36),after.conditionKey(reason),after.scope())==AutoDepositRearmDecision.SAME_FAILURE);
        });
        check("cause-related destination change uses existing bounded recovery", () -> {
            var p=new AutoDepositRearmPolicy(); var full=new DepositAllInventoryPressureSnapshot(36,36);
            p.beginUnit(full,"destination:A","automatic");
            p.finishUnit(false,full,"BUDGET_EXHAUSTED:NO_PROGRESS_LIMIT","automatic","destination:A");
            require(p.observe(full,"destination:B","automatic")==AutoDepositRearmDecision.RELATED_CHANGE);
            p.beginUnit(full,"destination:B","automatic"); require(p.activeBudget().recoveryGrants()==1);
        });
        System.out.println("CORE_CASES_PASSED="+passed);
    }
}
