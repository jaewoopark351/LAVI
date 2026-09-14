# Test-only API seams. Task/TaskRunner/TaskChain/SingleTaskChain/closest-object selection are REAL sources.
# Minecraft entity delivery, combat decisions, and Baritone path execution are explicitly simulated.
ENGINE_STUBS = {
    'lavi/minecraft/diagnostics/DiagnosticScreenState.java': 'package lavi.minecraft.diagnostics; class DiagnosticScreenState {static Object[] currentFields(){return new Object[0];}}',
    'lavi/minecraft/diagnostics/DiagnosticInputState.java': 'package lavi.minecraft.diagnostics; class DiagnosticInputState {static Object[] currentStateFields(){return new Object[0];}}',
    'baritone/api/process/ICustomGoalProcess.java': '''package baritone.api.process;
import baritone.api.pathing.goals.Goal;
public interface ICustomGoalProcess {boolean isActive();Goal getGoal();void setGoalAndPath(Goal g);void onLostControl();}''',
    'baritone/api/process/IExploreProcess.java': '''package baritone.api.process;
public interface IExploreProcess {boolean isActive();void explore(int x,int z);void onLostControl();}''',
    'adris/altoclef/BotBehaviour.java': '''package adris.altoclef;
import java.util.*;import java.util.function.Predicate;import net.minecraft.util.math.BlockPos;
public class BotBehaviour {
 public int depth;public Predicate<BlockPos> breaking,placing,walking;public boolean failConfigure;
 private final Deque<Snapshot> stack=new ArrayDeque<>();
 private record Snapshot(Predicate<BlockPos> breaking,Predicate<BlockPos> placing,Predicate<BlockPos> walking){}
 public void push(){stack.push(new Snapshot(breaking,placing,walking));depth++;}
 public void pop(){if(stack.isEmpty())throw new AssertionError("policy stack underflow");var s=stack.pop();depth--;breaking=s.breaking;placing=s.placing;walking=s.walking;}
 public void avoidBlockBreaking(Predicate<BlockPos> p){if(failConfigure)throw new IllegalStateException("test policy failure");breaking=p;}
 public void avoidBlockPlacing(Predicate<BlockPos> p){placing=p;}
 public void avoidWalkingThrough(Predicate<BlockPos> p){walking=p;}
 public void setPauseOnLostFocus(boolean value){}
}''',
    'adris/altoclef/AltoClef.java': '''package adris.altoclef;
import java.util.*;import java.util.function.Predicate;import net.minecraft.client.world.ClientWorld;import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;import net.minecraft.util.math.*;import net.minecraft.block.Block;
import adris.altoclef.tasksystem.Task;import baritone.api.pathing.goals.Goal;import baritone.api.process.*;
public class AltoClef {
 public static AltoClef INSTANCE=new AltoClef();public static AltoClef getInstance(){return INSTANCE;}
 public static boolean inGame(){return INSTANCE.world!=null&&INSTANCE.player!=null;}
 public ClientWorld world;public PlayerEntity player;public final BotBehaviour behaviour=new BotBehaviour();public final Baritone baritone=new Baritone();
 public final MobDefense defense=new MobDefense();public final EntityTracker entities=new EntityTracker();public final BlockScanner blocks=new BlockScanner();
 public final List<Task> submitted=new ArrayList<>();public final List<Runnable> callbacks=new ArrayList<>();
 public ClientWorld getWorld(){return world;}public PlayerEntity getPlayer(){return player;}public BotBehaviour getBehaviour(){return behaviour;}
 public Baritone getClientBaritone(){return baritone;}public MobDefense getMobDefenseChain(){return defense;}
 public EntityTracker getEntityTracker(){return entities;}public BlockScanner getBlockScanner(){return blocks;}
 public ChunkTracker getChunkTracker(){return new ChunkTracker();}
 public void runUserTask(Task task,Runnable callback){submitted.add(task);callbacks.add(callback);}
 public static class MobDefense {public boolean claimed;public boolean isToolInputClaimed(){return claimed;}}
 public static class EntityTracker {
  public final Set<Entity> unreachable=new HashSet<>();public int observations;
  public boolean entityFound(Class<?>...types){observations++;return !INSTANCE.world.entities.isEmpty();}
  public Optional<Entity> getClosestEntity(Vec3d p,Predicate<Entity> predicate,Class<?>...types){
   return INSTANCE.world.entities.stream().filter(e->e!=INSTANCE.player&&e.isAlive()&&!unreachable.contains(e)).filter(predicate)
     .min(Comparator.comparingDouble(e->e.getPos().squaredDistanceTo(p)));}
  public boolean isEntityReachable(Entity e){return !unreachable.contains(e);}
 }
 public static class ChunkTracker {public boolean isChunkLoaded(BlockPos p){return INSTANCE.world.chunks.loaded;}}
 public static class BlockScanner {
  public final Set<BlockPos> unreachable=new HashSet<>();public int observations;
  public Optional<BlockPos> getNearestBlock(Vec3d p,Predicate<BlockPos> predicate,Block...types){observations++;
   return INSTANCE.world.blocks.keySet().stream().filter(b->isBlockAtPosition(b,types)).filter(predicate).filter(b->!unreachable.contains(b))
    .min(Comparator.comparingDouble(b->Vec3d.ofCenter(b).squaredDistanceTo(p)));}
  public boolean isBlockAtPosition(BlockPos p,Block...types){return Arrays.asList(types).contains(INSTANCE.world.getBlockState(p).getBlock());}
 }
 public static class Baritone {
  public final Pathing path=new Pathing();public final Process process=new Process();public final Explore explore=new Explore();
  public Pathing getPathingBehavior(){return path;}public Process getCustomGoalProcess(){return process;}public Explore getExploreProcess(){return explore;}
 }
 public static class Pathing {
  public int cancels;public boolean safe=true;
  public void forceCancel(){cancels++;INSTANCE.baritone.process.active=false;INSTANCE.baritone.explore.active=false;}
  public boolean isSafeToCancel(){return safe;}
  public boolean isPathing(){return INSTANCE.baritone.process.active||INSTANCE.baritone.explore.active;}
  public Optional<Double> ticksRemainingInSegment(){return Optional.of(10.0);}
 }
 public static class Process implements ICustomGoalProcess {
  public boolean active;public Goal goal;public int starts,releases;public boolean failStart;
  public boolean isActive(){return active;}public Goal getGoal(){return goal;}
  public void setGoalAndPath(Goal g){goal=g;active=true;starts++;if(failStart)throw new IllegalStateException("test partial goal start");}
  public void onLostControl(){active=false;goal=null;releases++;}
 }
 public static class Explore implements IExploreProcess {
  public boolean active;public int starts,releases,x,z;public boolean failStart;
  public boolean isActive(){return active;}public void explore(int x,int z){this.x=x;this.z=z;active=true;starts++;if(failStart)throw new IllegalStateException("test partial explore start");}
  public void onLostControl(){active=false;releases++;}
 }
}''',
    'adris/altoclef/Debug.java': '''package adris.altoclef;
public class Debug {public static boolean fail;public static void logInternal(String s){}public static void logMessage(String s){if(fail)throw new RuntimeException("test logger");}
 public static void logError(String s){}public static void logWarning(String s){System.out.println(s);}}''',
    'adris/altoclef/tasks/movement/TimeoutWanderTask.java': '''package adris.altoclef.tasks.movement;
import adris.altoclef.tasksystem.Task;
public class TimeoutWanderTask extends Task {
 public TimeoutWanderTask(boolean force){throw new AssertionError("FIND must not invoke the killing/shimmy fallback");}
 protected void onStart(){}protected Task onTick(){return null;}protected void onStop(Task t){}
 protected boolean isEqual(Task t){return this==t;}protected String toDebugString(){return "Forbidden upstream fallback";}
}''',
    'adris/altoclef/util/helpers/WorldHelper.java': '''package adris.altoclef.util.helpers;
import net.minecraft.util.math.*;
public class WorldHelper {public static int ticks;public static int getTicks(){return ticks;}public static Vec3d toVec3d(BlockPos p){return Vec3d.ofCenter(p);}}''',
    'lavi/minecraft/diagnostics/ChatClefDiagnostics.java': '''package lavi.minecraft.diagnostics;
import java.util.*;import java.util.function.Supplier;import adris.altoclef.*;import net.minecraft.entity.Entity;
public class ChatClefDiagnostics {
 public static boolean fail,off;public static int calls;public static final List<Map<String,Object>> records=new ArrayList<>();
 public static void logBoundary(String name,String reason,Object task,Object... fields){
  if(off)return;calls++;if(fail)throw new IllegalStateException("test diagnostic failure");
  FindDiagnosticEmitterBridge.emit(name,reason,task,fields);
  var r=new LinkedHashMap<String,Object>();r.put("event",name);r.put("reason",reason);
  for(int i=0;i+1<fields.length;i+=2)r.put(String.valueOf(fields[i]),fields[i+1]);records.add(r);
 }
 public static void enterTask(Object t){}public static void exitTask(Object t){}
 public static boolean isVerboseEnabled(){return false;}public static void beginTaskRun(Object a,Object b){}
 public static void setParent(Object a,Object b){}public static void logTaskTransition(Object...args){}
 public static void logEvent(String a,String b,String c,Object d,Object...fields){}
 public static String taskSummary(Object o){return String.valueOf(o);}public static String entitySummary(Object o){return String.valueOf(o);}
 public static Object safeValue(Supplier<?> s){try{return s.get();}catch(RuntimeException e){return "unavailable";}}
 public static Object entityDistanceSqrToPlayer(AltoClef m,Entity e){return e.getPos().squaredDistanceTo(m.getPlayer().getPos());}
 public static Object classList(Class<?>[] cs){return Arrays.toString(cs);}public static String className(Object o){return o==null?"none":o.getClass().getName();}
 public static Object playerPosition(AltoClef m){return m.getPlayer().getPos();}public static Object vec3d(Object p){return p;}
 public static String chainName(Object c){return String.valueOf(c);}
}''',
    'lavi/minecraft/diagnostics/container/ContainerTaskDiagnostics.java': '''package lavi.minecraft.diagnostics.container;
public class ContainerTaskDiagnostics {public static void logChildReconciliation(Object...args){}}''',
    'lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java': '''package lavi.minecraft.diagnostics.container.store.deposit;
public class StoreDepositDiagnostics {
 public static void logPursuitDecision(Object...args){}public static void logChildReconciliation(Object...args){}
 public static void logTaskLifecycleBoundary(Object...args){}public static void logNaturalFinish(Object...args){}
 public static void beginFilteredSearchObservation(Object...args){}public static void endFilteredSearchObservation(Object...args){}
 public static void logFilteredSearchResult(Object...args){}
}''',
    'lavi/minecraft/diagnostics/mining/MiningPathDiagnostics.java': '''package lavi.minecraft.diagnostics.mining;
public class MiningPathDiagnostics {public static void logTaskChildReconciliation(Object...args){}}''',
    'lavi/minecraft/diagnostics/container/store/deposit/pressure/AutoDepositSchedulerDiagnostics.java': '''package lavi.minecraft.diagnostics.container.store.deposit.pressure;
public class AutoDepositSchedulerDiagnostics {public static void evaluated(Object...args){}public static void selected(Object...args){}}''',
}
