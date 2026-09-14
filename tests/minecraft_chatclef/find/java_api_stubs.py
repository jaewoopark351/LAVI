#20260914_kpopmodder: TEST ONLY; Minecraft/Baritone/Gson/Task APIs are simulated, not a production build.
STUBS = {
    'net/minecraft/util/math/Vec3d.java': r'''package net.minecraft.util.math;
public record Vec3d(double x,double y,double z) {
 public double getX(){return x;} public double getY(){return y;} public double getZ(){return z;}
 public double squaredDistanceTo(Vec3d v){return (x-v.x)*(x-v.x)+(y-v.y)*(y-v.y)+(z-v.z)*(z-v.z);}
 public static Vec3d ofCenter(BlockPos p){return new Vec3d(p.getX()+.5,p.getY()+.5,p.getZ()+.5);}
 public static Vec3d ofBottomCenter(BlockPos p){return new Vec3d(p.getX()+.5,p.getY(),p.getZ()+.5);}
}''',
    'net/minecraft/util/math/BlockPos.java': r'''package net.minecraft.util.math;
public record BlockPos(int x,int y,int z) {
 public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
 public BlockPos toImmutable(){return this;} public String toShortString(){return x+", "+y+", "+z;}
 public BlockPos add(int a,int b,int c){return new BlockPos(x+a,y+b,z+c);}
 public BlockPos down(){return add(0,-1,0);} public BlockPos up(){return add(0,1,0);}
 public double getSquaredDistance(BlockPos p){return (double)(x-p.x)*(x-p.x)+(double)(y-p.y)*(y-p.y)+(double)(z-p.z)*(z-p.z);}
 public boolean isWithinDistance(Vec3d p,double d){return Vec3d.ofCenter(this).squaredDistanceTo(p)<d*d;}
}''',
    'net/minecraft/util/math/Direction.java': r'''package net.minecraft.util.math; public enum Direction {UP}''',
    'net/minecraft/util/Identifier.java': r'''package net.minecraft.util; public record Identifier(String value) {
 public Identifier(String ns,String path){this(ns+":"+path);} public String toString(){return value;}
}''',
    'net/minecraft/util/Language.java': r'''package net.minecraft.util; import java.util.*;
public class Language { public static final Language INSTANCE=new Language(); public static Language getInstance(){return INSTANCE;}
 public final Map<String,String> names=new HashMap<>(); public String get(String key){return names.getOrDefault(key,key);}
}''',
    'net/minecraft/registry/RegistryKey.java': r'''package net.minecraft.registry; import net.minecraft.util.Identifier; public record RegistryKey(Identifier value){ public Identifier getValue(){return value;} }''',
    'net/minecraft/registry/Registries.java': r'''package net.minecraft.registry;
import java.util.*; import net.minecraft.util.Identifier; import net.minecraft.entity.EntityType; import net.minecraft.block.Block; import net.minecraft.item.Item;
public class Registries {
 public static final Registry<EntityType> ENTITY_TYPE=new Registry<>(); public static final Registry<Block> BLOCK=new Registry<>(); public static final Registry<Item> ITEM=new Registry<>();
 public static class Registry<T> implements Iterable<T> {
  private final Map<T,Identifier> ids=new LinkedHashMap<>();
  public T add(String id,T value){ids.put(value,new Identifier(id));return value;}
  public T get(Identifier id){return ids.entrySet().stream().filter(e->e.getValue().equals(id)).map(e->e.getKey()).findFirst().orElse(null);}
  public Identifier getId(T value){return ids.get(value);} public Iterator<T> iterator(){return ids.keySet().iterator();}
  public void clear(){ids.clear();}
 }
}''',
    'net/minecraft/entity/EntityType.java': r'''package net.minecraft.entity; public record EntityType(String key){public String getTranslationKey(){return key;}}''',
    'net/minecraft/entity/Entity.java': r'''package net.minecraft.entity;
import java.util.UUID; import net.minecraft.util.math.*;
public class Entity {
 public Vec3d pos; public boolean alive=true,removed=false; public int id; public UUID uuid=UUID.randomUUID(); public EntityType type;
 public Entity(EntityType t,int i,double x,double y,double z){type=t;id=i;pos=new Vec3d(x,y,z);}
 public Vec3d getPos(){return pos;} public BlockPos getBlockPos(){return new BlockPos((int)Math.floor(pos.x()),(int)Math.floor(pos.y()),(int)Math.floor(pos.z()));}
 public UUID getUuid(){return uuid;} public int getId(){return id;} public EntityType getType(){return type;}
 public boolean isAlive(){return alive;} public boolean isRemoved(){return removed;}
}''',
    'net/minecraft/entity/player/PlayerEntity.java': r'''package net.minecraft.entity.player;
import net.minecraft.entity.*;
public class PlayerEntity extends Entity { public String name;
 public PlayerEntity(EntityType t,int i,String n,double x,double y,double z){super(t,i,x,y,z);name=n;}
 public Profile getGameProfile(){return new Profile(name);} public record Profile(String name){public String getName(){return name;}}
}''',
    'net/minecraft/entity/ItemEntity.java': r'''package net.minecraft.entity; import net.minecraft.item.ItemStack;
public class ItemEntity extends Entity { public ItemStack stack;
 public ItemEntity(EntityType t,int i,ItemStack s,double x,double y,double z){super(t,i,x,y,z);stack=s;}
 public ItemStack getStack(){return stack;}
}''',
    'net/minecraft/item/Item.java': r'''package net.minecraft.item; public class Item { private final String key; public Item(String k){key=k;} public String getTranslationKey(){return key;} }''',
    'net/minecraft/item/BlockItem.java': r'''package net.minecraft.item; import net.minecraft.block.Block; public class BlockItem extends Item {private final Block block; public BlockItem(String k,Block b){super(k);block=b;} public Block getBlock(){return block;} }''',
    'net/minecraft/item/ItemStack.java': r'''package net.minecraft.item; public class ItemStack {public Item item;public int count=1;public ItemStack(Item i){item=i;}public boolean isEmpty(){return count<=0;}public Item getItem(){return item;}}''',
    'net/minecraft/block/Block.java': r'''package net.minecraft.block; public record Block(String key){public String getTranslationKey(){return key;} public BlockState getDefaultState(){return new BlockState(this,false);}}''',
    'net/minecraft/block/BlockState.java': r'''package net.minecraft.block; import net.minecraft.client.world.ClientWorld;import net.minecraft.util.math.*;
public record BlockState(Block block,boolean solid) {public Block getBlock(){return block;} public boolean isAir(){return block.key().endsWith("air");}
 public boolean isSideSolidFullSquare(ClientWorld w,BlockPos p,Direction d){return solid;}
 public FluidState getFluidState(){return new FluidState();} public static class FluidState {public boolean isEmpty(){return true;}}
}''',
    'net/minecraft/world/chunk/ChunkStatus.java': r'''package net.minecraft.world.chunk; public enum ChunkStatus {FULL}''',
    'net/minecraft/client/world/ClientChunkManager.java': r'''package net.minecraft.client.world; import net.minecraft.world.chunk.ChunkStatus;
public class ClientChunkManager {public boolean loaded=true; public int calls;
 public Object getChunk(int x,int z,ChunkStatus s,boolean create){if(create)throw new AssertionError("scan must not load chunks");calls++;return loaded?this:null;}
}''',
    'net/minecraft/client/world/ClientWorld.java': r'''package net.minecraft.client.world;
import java.util.*;import net.minecraft.entity.*;import net.minecraft.block.*;import net.minecraft.registry.*;import net.minecraft.util.*;import net.minecraft.util.math.*;
public class ClientWorld {
 public final List<Entity> entities=new ArrayList<>(); public final Map<BlockPos,BlockState> blocks=new HashMap<>();
 public final ClientChunkManager chunks=new ClientChunkManager(); public Block air,stone; public boolean floor=true;
 public Iterable<Entity> getEntities(){return entities;} public Entity getEntityById(int id){return entities.stream().filter(e->e.id==id).findFirst().orElse(null);}
 public ClientChunkManager getChunkManager(){return chunks;} public int getBottomY(){return -64;}public int getTopY(){return 320;}
 public BlockState getBlockState(BlockPos p){return blocks.getOrDefault(p,new BlockState(floor&&p.y()<64?stone:air,floor&&p.y()<64));}
 public boolean isAir(BlockPos p){return getBlockState(p).getBlock()==air;}
 public RegistryKey getRegistryKey(){return new RegistryKey(new Identifier("minecraft:overworld"));}
}''',
    'net/minecraft/resource/Resource.java': r'''package net.minecraft.resource; import java.io.*; public class Resource {
 public String text; public boolean closed; public Resource(String t){text=t;}
 public Reader getReader() throws IOException { return new StringReader(text){public void close(){closed=true;super.close();}}; }
}''',
    'net/minecraft/resource/ResourceManager.java': r'''package net.minecraft.resource;import java.util.*;import net.minecraft.util.Identifier;
public class ResourceManager {public Map<Identifier,List<Resource>> resources=new HashMap<>();
 public Set<String> getAllNamespaces(){var s=new HashSet<String>();for(var id:resources.keySet())s.add(id.toString().split(":")[0]);return s;}
 public List<Resource> getAllResources(Identifier id){return resources.getOrDefault(id,List.of());}
}''',
    'net/minecraft/client/MinecraftClient.java': r'''package net.minecraft.client;import net.minecraft.resource.ResourceManager;
public class MinecraftClient {public static final MinecraftClient INSTANCE=new MinecraftClient();public final ResourceManager resources=new ResourceManager();
 public static MinecraftClient getInstance(){return INSTANCE;}public ResourceManager getResourceManager(){return resources;}}
''',
    'com/google/gson/JsonObject.java': r'''package com.google.gson;import java.util.*;
public class JsonObject {public Map<String,JsonPrimitive> values=new LinkedHashMap<>();public Set<Map.Entry<String,JsonPrimitive>> entrySet(){return values.entrySet();}
 public JsonObject getAsJsonObject(){return this;}}
''',
    'com/google/gson/JsonPrimitive.java': r'''package com.google.gson;public record JsonPrimitive(String value){
 public boolean isJsonPrimitive(){return true;}public JsonPrimitive getAsJsonPrimitive(){return this;}public boolean isString(){return true;}public String getAsString(){return value;}}
''',
    'com/google/gson/JsonParser.java': r'''package com.google.gson;import java.util.*;
// This stand-in injects already-parsed objects. JSON parser correctness is NOT tested.
public class JsonParser {public static final Map<String,JsonObject> parsed=new HashMap<>();
 public static JsonObject parseString(String text){if(!parsed.containsKey(text))throw new IllegalArgumentException("unavailable test JSON");return parsed.get(text);}}
''',
    'baritone/api/pathing/goals/Goal.java': r'''package baritone.api.pathing.goals;import net.minecraft.util.math.BlockPos;
public interface Goal {boolean isInGoal(int x,int y,int z);double heuristic(int x,int y,int z);default boolean isInGoal(BlockPos p){return isInGoal(p.x(),p.y(),p.z());}}
''',
    'baritone/api/pathing/goals/GoalBlock.java': r'''package baritone.api.pathing.goals;import net.minecraft.util.math.BlockPos;
public class GoalBlock implements Goal {private final BlockPos p;public GoalBlock(BlockPos p){this.p=p;}
 public boolean isInGoal(int x,int y,int z){return p.equals(new BlockPos(x,y,z));}public double heuristic(int x,int y,int z){return calculate(x-p.x(),y-p.y(),z-p.z());}
 public static double calculate(double x,int y,double z){return Math.sqrt(x*x+y*y+z*z);}}
''',
    'adris/altoclef/tasksystem/Task.java': r'''package adris.altoclef.tasksystem;
public abstract class Task {protected abstract void onStart();protected abstract Task onTick();protected abstract void onStop(Task t);
 public abstract boolean isFinished(); protected abstract boolean isEqual(Task t);protected abstract String toDebugString();}
''',
    'adris/altoclef/AltoClef.java': r'''package adris.altoclef;
import java.util.*;import java.util.function.Predicate;import net.minecraft.client.world.ClientWorld;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.util.math.BlockPos;import adris.altoclef.tasksystem.Task;import baritone.api.pathing.goals.Goal;
public class AltoClef {public static AltoClef INSTANCE=new AltoClef(); public static AltoClef getInstance(){return INSTANCE;}
 public ClientWorld world;public PlayerEntity player;public final Behaviour behaviour=new Behaviour();public final Baritone baritone=new Baritone();
 public final List<Task> submitted=new ArrayList<>();public final List<Runnable> callbacks=new ArrayList<>();
 public ClientWorld getWorld(){return world;}public PlayerEntity getPlayer(){return player;}public Behaviour getBehaviour(){return behaviour;}public Baritone getClientBaritone(){return baritone;}
 public void runUserTask(Task task,Runnable callback){submitted.add(task);callbacks.add(callback);}
 public static class Behaviour {public int depth;public Predicate<BlockPos> breaking,placing,walking;
  public void push(){depth++;}public void pop(){if(--depth<0)throw new AssertionError("unbalanced policy");breaking=placing=walking=null;}
  public void avoidBlockBreaking(Predicate<BlockPos> p){breaking=p;}public void avoidBlockPlacing(Predicate<BlockPos> p){placing=p;}public void avoidWalkingThrough(Predicate<BlockPos> p){walking=p;}}
 public static class Baritone {public final Pathing path=new Pathing();public final Process process=new Process();public Pathing getPathingBehavior(){return path;}public Process getCustomGoalProcess(){return process;}}
 public static class Pathing {public int cancels;public void forceCancel(){cancels++;AltoClef.INSTANCE.baritone.process.active=false;}public boolean isSafeToCancel(){return true;}}
 public static class Process {public boolean active;public Goal goal;public int starts;public boolean isActive(){return active;}public void setGoalAndPath(Goal g){goal=g;active=true;starts++;}}
}''',
    'adris/altoclef/Debug.java': r'''package adris.altoclef; public class Debug {public static boolean fail;public static void logMessage(String s){if(fail)throw new RuntimeException("test logger");}public static void logError(String s){}}''',
    'adris/altoclef/commandsystem/ArgBase.java': r'''package adris.altoclef.commandsystem; public class ArgBase {public String getHelpRepresentation(){return "target";}}''',
    'adris/altoclef/commandsystem/Arg.java': r'''package adris.altoclef.commandsystem;public class Arg<T> extends ArgBase {public Arg(Class<T> c,String name){}public Arg<T> asArray(){return this;}}''',
    'adris/altoclef/commandsystem/ArgParser.java': r'''package adris.altoclef.commandsystem;public class ArgParser {public ArgParser(ArgBase...args){}public void loadArgs(String line,boolean b){}public ArgBase[] getArgs(){return new ArgBase[0];}public String[] getArgUnits(){return new String[0];}}''',
    'adris/altoclef/commandsystem/CommandException.java': r'''package adris.altoclef.commandsystem;public class CommandException extends Exception {public CommandException(String m){super(m);}}''',
    'lavi/minecraft/diagnostics/ChatClefDiagnostics.java': r'''package lavi.minecraft.diagnostics;public class ChatClefDiagnostics {public static boolean fail;public static int calls;
 public static void logBoundary(String a,String b,Object owner,Object...fields){calls++;if(fail)throw new RuntimeException("test diagnostic");}}
''',
    'lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandTerminationObservation.java': r'''package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;import adris.altoclef.tasksystem.Task;
public record FabricChatClefCommandTerminationObservation(Task task,boolean taskStopped){}
''',
}
