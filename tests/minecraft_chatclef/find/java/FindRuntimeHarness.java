package lavi.minecraft.task.find;

import adris.altoclef.*;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasksystem.*;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindResultProjector;

import java.util.*;

/** Real FIND and native Task lifecycle; Minecraft/Baritone/Gson and combat APIs are test doubles. */
public class FindRuntimeHarness {
    static int checks;
    static EntityType villager, zombie, itemType, playerType;
    static Item diamond;
    static Block chest;
    static final Map<FindTask, HarnessChain> chains = new IdentityHashMap<>();

    static class HarnessChain extends SingleTaskChain {
        HarnessChain(TaskRunner runner) { super(runner); }
        @Override protected void onTaskFinish(AltoClef mod) { mainTask.stop(); mainTask = null; }
        @Override public float getPriority() { return 50; }
        @Override public String getName() { return "FIND harness user chain"; }
    }

    static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }

    static AltoClef reset() {
        AltoClef.INSTANCE = new AltoClef();
        var m = AltoClef.INSTANCE;
        chains.clear();
        Registries.ENTITY_TYPE.clear(); Registries.BLOCK.clear(); Registries.ITEM.clear();
        villager = Registries.ENTITY_TYPE.add("minecraft:villager", new EntityType("entity.minecraft.villager"));
        zombie = Registries.ENTITY_TYPE.add("minecraft:zombie", new EntityType("entity.minecraft.zombie"));
        itemType = Registries.ENTITY_TYPE.add("minecraft:item", new EntityType("entity.minecraft.item"));
        playerType = Registries.ENTITY_TYPE.add("minecraft:player", new EntityType("entity.minecraft.player"));
        diamond = Registries.ITEM.add("minecraft:diamond", new Item("item.minecraft.diamond"));
        m.world = new ClientWorld();
        m.world.air = Registries.BLOCK.add("minecraft:air", new Block("block.minecraft.air"));
        m.world.stone = Registries.BLOCK.add("minecraft:stone", new Block("block.minecraft.stone"));
        chest = Registries.BLOCK.add("minecraft:chest", new Block("block.minecraft.chest"));
        Registries.ITEM.add("minecraft:chest", new BlockItem("block.minecraft.chest", chest));
        m.player = new PlayerEntity(playerType, 1, "LAVI", .5, 64, .5);
        m.world.entities.add(m.player);
        Language.INSTANCE.names.clear();
        Language.INSTANCE.names.put("entity.minecraft.villager", "주민");
        Language.INSTANCE.names.put("entity.minecraft.zombie", "좀비");
        Language.INSTANCE.names.put("block.minecraft.chest", "상자");
        Language.INSTANCE.names.put("item.minecraft.diamond", "다이아몬드");
        MinecraftClient.INSTANCE.resources.resources.clear();
        com.google.gson.JsonParser.parsed.clear();
        ChatClefDiagnostics.fail = false; ChatClefDiagnostics.off = false;
        ChatClefDiagnostics.calls = 0; ChatClefDiagnostics.records.clear(); Debug.fail = false;
        return m;
    }

    static Entity entity(AltoClef m, double x) {
        var e = new Entity(villager, 10, x, 64, .5);
        m.world.entities.add(e);
        return e;
    }

    static FindTask start(String command) {
        var t = new FindTask(FindRequest.parse(command));
        var chain = new HarnessChain(new TaskRunner(AltoClef.getInstance()));
        chain.setTask(t); chains.put(t, chain); tick(t);
        return t;
    }

    static void tick(FindTask t) { chains.get(t).tick(); }
    static void completeScan(FindTask t) {
        for (int i = 0; i < 3000 && !t.isFinished(); i++) tick(t);
        check(t.isFinished(), "bounded report scan terminates");
    }
    static void code(FindTask t, String expected) {
        check(t.outcome() != null && t.outcome().code().equals(expected), "expected " + expected + ", actual " + t.outcome());
        System.out.println("WIRE_FIND=" + json(t.outcome().toMap()));
    }
    static void expire(FindTask t) throws Exception {
        var f = FindTask.class.getDeclaredField("startedNanos"); f.setAccessible(true);
        f.setLong(t, System.nanoTime() - FindTask.MAX_NANOS - 1);
    }
    static void expireApproach(FindTask t) throws Exception {
        var f = FindTask.class.getDeclaredField("approachActiveNanos"); f.setAccessible(true);
        f.setLong(t, FindTask.MAX_NANOS + 1);
    }
    static String projected(FindTask t, String command, boolean stopped, boolean stopBound) {
        var r = FabricChatClefFindResultProjector.fromMatchingTask("request", command,
                FabricChatClefCommandResultDataPayload.empty(), new FabricChatClefCommandTerminationObservation(t, stopped), stopBound);
        return (String) r.toMap().get("status");
    }
    static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Map<?, ?> map) return "{" + String.join(",", map.entrySet().stream().map(e -> json(e.getKey()) + ":" + json(e.getValue())).toList()) + "}";
        if (value instanceof List<?> list) return "[" + String.join(",", list.stream().map(FindRuntimeHarness::json).toList()) + "]";
        return value.toString();
    }

    public static void main(String[] args) throws Exception {
        var m = reset(); entity(m, 20); var t = start("find entity minecraft:villager report"); code(t, "FOUND");
        check(m.baritone.process.starts == 0 && m.baritone.explore.starts == 0 && m.behaviour.depth == 0, "report never moves");
        check(projected(t, "@find entity minecraft:villager report", false, false).equals("completed"), "matching success projected");
        check(projected(t, "@find entity minecraft:zombie report", false, false).equals("unknown"), "wrong command not projected");
        check(projected(t, "@find entity minecraft:villager report", true, false).equals("unknown"), "stopped root not projected");
        check(projected(t, "@find entity minecraft:villager report", false, true).equals("unknown"), "stop bound not projected");

        m = reset(); t = start("find missing:mob"); code(t, "UNKNOWN_TARGET");
        check(projected(t, "@find missing:mob", false, false).equals("failed"), "unknown not generic success");
        m = reset(); t = start("find entity minecraft:villager report"); code(t, "NOT_FOUND");
        check(t.outcome().scanComplete(), "report not-found is a completed local observation");

        m = reset(); var e = entity(m, 20); t = start("find entity minecraft:villager");
        check(t.thisOrChildSatisfies(c -> c instanceof FindEntitySearchTask), "native entity selector is in the actual task graph");
        check(t.thisOrChildSatisfies(c -> c instanceof FindApproachTask), "movement-only leaf is in actual task graph");
        check(m.baritone.process.starts == 1, "existing goal submitted once"); tick(t);
        check(m.baritone.process.starts == 1, "stationary active goal not resubmitted");
        t.interrupt(null);
        check(m.behaviour.depth == 0 && t.outcome() == null && !m.baritone.process.active, "defense preemption releases movement, not terminal success");
        tick(t); check(m.baritone.process.starts == 2, "same operation reacquires its own goal on resume");
        m.player.pos = new Vec3d(17, 64, .5); tick(t); code(t, "ARRIVED"); t.stop();
        check(m.behaviour.depth == 0 && !m.baritone.process.active, "arrival restores policy and releases own goal");

        // A removed/dead/out-of-range candidate now re-enters exploration, never false success.
        m = reset(); e = entity(m, 20); t = start("find entity minecraft:villager"); e.removed = true; tick(t);
        check(!t.isFinished() && m.baritone.explore.active, "removed target restarts search"); expire(t); tick(t);
        check(!t.isFinished() && m.baritone.explore.active, "removed-target search survives old 90-second lifetime");
        m.world.entities.remove(e); entity(m, 2); tick(t); code(t, "ARRIVED");
        m = reset(); e = entity(m, 20); t = start("find entity minecraft:villager"); m.world.entities.remove(e);
        var replacement = entity(m, 20); replacement.uuid = e.uuid; tick(t);
        check(!t.isFinished(), "same UUID replacement is reselected, not falsely marked arrived");
        m.player.pos = new Vec3d(18, 64, .5); tick(t); code(t, "ARRIVED");
        m = reset(); e = entity(m, 20); t = start("find entity minecraft:villager"); e.pos = new Vec3d(100, 64, 0); tick(t);
        check(!t.isFinished() && m.baritone.explore.active, "out-of-view target triggers exploration"); expire(t); tick(t);
        check(!t.isFinished(), "out-of-view search has no elapsed-time cutoff"); e.pos = new Vec3d(2,64,.5); tick(t); code(t, "ARRIVED");
        m = reset(); entity(m, 20); t = start("find entity minecraft:villager"); t.interrupt(null); expire(t); tick(t);
        check(!t.isFinished(), "operation age does not expire an interrupted approach");
        expireApproach(t); tick(t); code(t, "APPROACH_TIMEOUT");
        m = reset(); entity(m, 20); t = start("find entity minecraft:villager"); m.world = new ClientWorld(); tick(t); code(t, "SCOPE_CHANGED");
        m = reset(); m.player.alive = false; t = start("find entity minecraft:villager"); code(t, "PLAYER_UNAVAILABLE");
        m = reset(); for (int i = 0; i < 4096; i++) m.world.entities.add(new Entity(zombie, i + 20, 3, 64, 3));
        t = start("find entity minecraft:villager report"); code(t, "SEARCH_LIMIT");
        check(t.outcome().scanned() == 4096 && !t.outcome().scanComplete(), "entity cap does not claim absence");

        m = reset(); m.world.blocks.put(new BlockPos(10,64,0), new BlockState(chest,true));
        t = start("find block minecraft:chest report"); completeScan(t); code(t, "FOUND");
        check(t.outcome().position().equals(List.of(10,64,0)), "exact block coordinates"); check(m.world.chunks.calls > 0, "loaded chunks inspected");
        m = reset(); m.world.chunks.loaded = false; m.world.blocks.put(new BlockPos(1,64,0), new BlockState(chest,true));
        t = start("find block minecraft:chest report"); completeScan(t); code(t, "NOT_FOUND"); check(t.outcome().scanned() == 0, "unloaded report blocks ignored");
        m = reset(); m.world.floor = false; m.world.blocks.put(new BlockPos(10,64,0), new BlockState(chest,false));
        t = start("find block minecraft:chest"); code(t, "NO_APPROACH");
        m = reset(); var drop = new ItemEntity(itemType,10,new ItemStack(diamond),20,64,.5); m.world.entities.add(drop);
        t = start("find item minecraft:diamond"); check(m.behaviour.walking.test(new BlockPos(20,64,0)), "stand off dropped item");
        drop.stack.count = 0; tick(t); check(!t.isFinished() && m.baritone.explore.active, "empty drop causes re-search, not success");
        expire(t); tick(t); check(!t.isFinished(), "dropped-item search survives 90 seconds");
        m.world.entities.add(new ItemEntity(itemType,11,new ItemStack(diamond),2,64,.5));
        tick(t); code(t, "ARRIVED");
        m = reset(); m.world.entities.add(new PlayerEntity(playerType,5,"Steve",20,64,0)); t = start("find player Steve report");
        code(t, "FOUND"); check(t.outcome().registryId().equals("Steve"), "player identity");
        m = reset(); var custom = Registries.ENTITY_TYPE.add("my_mod:new_mob",new EntityType("entity.my_mod.new_mob"));
        Language.INSTANCE.names.put("entity.my_mod.new_mob","새로운 몹"); m.world.entities.add(new Entity(custom,10,20,64,0));
        t = start("find entity my_mod:new_mob report"); code(t, "FOUND"); check(t.outcome().registryId().equals("my_mod:new_mob"), "mod registry without whitelist");
        m = reset(); Registries.ENTITY_TYPE.add("minecraft:chest",new EntityType("entity.other.chest"));
        t = start("find minecraft:chest report"); code(t, "AMBIGUOUS_TARGET");
        check(t.outcome().suggestions().size() == 2, "collision candidates");
        m = reset(); var low = new Resource("low"); var high = new Resource("high");
        var j1 = new com.google.gson.JsonObject(); j1.values.put("entity.minecraft.villager",new com.google.gson.JsonPrimitive("옛 주민"));
        var j2 = new com.google.gson.JsonObject(); j2.values.put("entity.minecraft.villager",new com.google.gson.JsonPrimitive("새 주민"));
        com.google.gson.JsonParser.parsed.put("low",j1); com.google.gson.JsonParser.parsed.put("high",j2);
        MinecraftClient.INSTANCE.resources.resources.put(new Identifier("minecraft","lang/ko_kr.json"),List.of(low,high));
        entity(m,10); t = start("find entity minecraft:villager report"); code(t, "FOUND"); check(!low.closed && !high.closed, "Java never opens language resources");
        m = reset(); MinecraftClient.INSTANCE.resources.resources.put(new Identifier("minecraft","lang/ko_kr.json"),List.of(new Resource("broken")));
        entity(m,10); t = start("find entity minecraft:villager report"); code(t, "FOUND"); check(t.outcome().languageWarnings() == 0, "broken translation cannot affect ID resolution");
        m = reset(); entity(m,2); ChatClefDiagnostics.fail = true; t = start("find entity minecraft:villager"); code(t, "ARRIVED"); ChatClefDiagnostics.fail = false;
        m = reset(); var command = new FindCommand(); int[] callbacks = {0,0};
        command.run(m,"find entity minecraft:villager",()->callbacks[0]++); command.run(m,"find entity minecraft:zombie",()->callbacks[1]++);
        m.callbacks.get(0).run(); m.callbacks.get(1).run(); check(callbacks[0] == 1 && callbacks[1] == 1, "native callback isolation");
        boolean rejected = false; try {command.run(m,"find entity minecraft:villager # injection",()->{});} catch(adris.altoclef.commandsystem.CommandException bad) {rejected=true;}
        check(rejected && m.submitted.size() == 2, "raw separator rejected before task submission");
        // Force a completed outcome for the callback rendering failure check.
        var submitted=(FindTask)m.submitted.get(0); entity(m,2); submitted.onStart(); submitted.onTick();
        submitted.selectEntity(m.world.entities.get(1)); Debug.fail = true;
        try {m.callbacks.get(0).run();} catch(RuntimeException loggingFailure) {}
        check(callbacks[0] == 2, "native completion callback survives rendering failure"); Debug.fail = false;
        //20260915_kpopmodder: No monster whitelist: passive mobs, golems and mod types use the same predicate.
        for (String id : List.of("minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:iron_golem", "mod:unlisted_pet")) {
            m = reset(); var type = Registries.ENTITY_TYPE.add(id, new EntityType("unused.translation.key"));
            m.world.entities.add(new Entity(type, 77, 2, 64, .5));
            t = start("find entity " + id); code(t, "ARRIVED");
            check(t.outcome().registryId().equals(id) && t.outcome().label().equals(id), "exact runtime ID " + id);
        }
        m = reset(); Registries.ITEM.add("mod:unlisted_relic", new Item("unused.translation.key"));
        t = start("find item mod:unlisted_relic report"); code(t, "NOT_FOUND");
        check(t.outcome().registryId().equals("mod:unlisted_relic"), "registered item resolves without a GET task or language file");
        m = reset(); t = start("find entity mod:uninstalled report"); code(t, "UNKNOWN_TARGET");
        check(t.outcome().scanned() == 0, "uninstalled ID rejected before world scan");
        System.out.println("SIMULATED_RUNTIME_CHECKS=" + checks + "; REAL TASK LIFECYCLE, NOT A MINECRAFT BUILD OR GAME TEST");
    }
}
