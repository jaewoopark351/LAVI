<!-- 20260805_kpopmodder: Documented Baritone cache handling for copied or restored Minecraft test worlds. -->

# ChatClef / Baritone Cache Troubleshooting

This note records an operational failure mode seen while testing Fabric
ChatClef / AltoClef with Baritone.

It is documentation only. It does not approve a Java behavior change, Baritone
patch, build, runtime reproduction, deletion, commit, or push.

## Scope

This applies when LAVI is using the Fabric ChatClef runtime:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/**
```

and the Minecraft instance uses Baritone world cache under a save directory,
for example:

```text
C:\Users\jaewo\curseforge\minecraft\Instances\<instance>\saves\<world>\baritone\...\cache
C:\Users\jaewo\curseforge\minecraft\Instances\<instance>\saves\<world>\.baritone\...\cache
```

The exact directory shape can vary by Baritone version and launcher layout.
Always inspect the actual save directory before deciding what to remove.

## Symptom Pattern

Before treating the following as a LAVI command, TaskRunner, mining, container,
or Carry On behavior bug, check for stale Baritone cache:

```text
long pathfinding after a world copy or restore
repeated path planning with very large movement/node counts
movement toward an apparently wrong or impossible target
mining loop where Baritone appears to mine a block that does not match reality
pathing around terrain that was changed, copied, replaced, or restored
normal behavior returning immediately after replacing the world save
normal behavior returning immediately after removing the Baritone cache
```

This does not prove Baritone cache is always the root cause. It means cache
state is a first-class suspect and should be ruled out before changing LAVI
behavior.

## Why This Can Happen

Baritone stores simplified chunk data for pathfinding and search. The upstream
documentation describes `chunkCaching` as saving chunks in a simplified format
for better very-long-distance pathing.

Baritone also exposes cache management commands:

```text
#repack
#reloadall
#saveall
```

The presence of these commands means cache regeneration and reload are expected
operational tools, not unusual maintenance.

Baritone has a `repackOnAnyBlockChange` setting whose documented purpose is to
repack a chunk when a block changes. In practice, LAVI test workflows can still
produce stale or mismatched cache because the world directory may be copied,
restored, or replaced outside the normal in-game block-change event flow.

The important local conclusion is:

```text
When a test world is copied, restored, replaced, or renamed, its Baritone cache
may no longer match the actual world state.
```

<!-- 20260805_kpopmodder: Documented BlockOptionalMeta JVM cache poisoning as a separate Baritone failure mode. -->

## Separate Failure Mode: BlockOptionalMeta NPE

Do not confuse Baritone disk world cache staleness with the `BlockOptionalMeta`
failure mode below.

Observed local evidence from `LAVI_TEST_Fabric01\logs\latest.log`:

```text
[02:12:04] RuntimeException: ExecutionException: NullPointerException
BlockOptionalMeta.getManager()
BlockOptionalMeta.drops()
BlockOptionalMeta.getStackHashes()
BlockOptionalMeta.<init>()
BuilderProcess$2.partOfMask()
BuilderProcess.fullRecalc()
BuilderProcess.onTick()
```

In this pattern, ChatClef / LAVI command lifecycle may still report success:

```text
command result status=completed ok=True
terminal command decision reason=matching_task_finished
```

That only proves the command lifecycle reached a terminal result. It does not
prove Baritone's block or item matching state remained correct.

### Current Local Interpretation

Treat this as a Baritone `BlockOptionalMeta` failure-containment issue, not as
a LAVI WebSocket bridge, ChatClef command dispatcher, TaskRunner, or general
pathfinding lifecycle bug.

The likely risk is JVM-internal cache poisoning:

```text
LootDataManager candidate is created
server-data reload starts
reload fails inside the async reload path
drops() catches or prints the exception
empty or incomplete drop result is returned
failed empty result may be cached as if it were authoritative
later BlockOptionalMeta.matches(ItemStack) checks can silently change meaning
```

This is separate from Baritone disk cache:

```text
Baritone disk world cache:
    persisted chunk/pathing/search data under the save directory

BlockOptionalMeta JVM cache:
    static LootDataManager and block drop cache inside the running Minecraft JVM
```

Closing Minecraft resets the JVM static state. Deleting the Baritone disk cache
does not directly fix `BlockOptionalMeta` static state in a running process.

### Stage Decision

If the stack above is present, the next source change is no longer
diagnostics-only. It should be a narrow crash guard / failure-containment patch
with structured, rate-limited diagnostics.

This does not authorize a broad root-cause rewrite of Minecraft resource
initialization. First preserve the actual inner cause and prevent poisoned
state from being published or cached.

The intended contract is:

```text
publish LootDataManager only after reload completed successfully
do not cache failed drop-resolution output as authoritative block drops
replace unbounded printStackTrace output with rate-limited structured logging
preserve the deepest root cause from ExecutionException or related wrappers
```

### Required Diagnostic Event

Use a bounded event like:

```text
BLOCK_OPTIONAL_META_MANAGER_FAILURE
stage=WAIT_FOR_RELOAD
block=minecraft:...
lootTable=minecraft:blocks/...
thread=Render thread
vanillaPackPresent=true|false
candidateManagerCreated=true|false
reloadCompleted=true|false
managerPublished=true|false
outerException=...
rootException=...
rootMessage=...
rootTopFrame=...
caller=baritone.process.BuilderProcess$2.partOfMask
fallback=BLOCK_ITEM|EMPTY|NONE
dropCacheWrite=true|false
```

Suggested stages:

```text
LOOKUP_VANILLA_PACK_METHOD
INVOKE_VANILLA_PACK_METHOD
CREATE_SERVER_DATA_RESOURCES
CREATE_LOOT_MANAGER
START_RELOAD
WAIT_FOR_RELOAD
LOOKUP_LOOT_TABLE
EVALUATE_LOOT_DROPS
```

Logging rules:

```text
no per-tick output
no per-block stack trace spam
first full stack trace per unique stage + root exception + root top frame
BOUNDARY mode must show the first failure boundary
normal successful paths should stay quiet
```

### Guard Rules

Do not fix this with only:

```java
if (manager == null) {
    return Collections.emptyList();
}
```

That hides the crash while preserving the worst semantic failure: a failed drop
resolution can become indistinguishable from a legitimate empty drop result.

A safe minimal direction is:

```text
use a local LootDataManager candidate
perform server-data reload
publish the candidate only after successful reload
separate normal empty drops from failed drop resolution
cache only successful authoritative drop results
on failure, return a clearly degraded fallback without writing the authoritative cache
```

Block-item fallback may be used only as degraded containment:

```text
use only when manager initialization or drop resolution failed
do not use Items.AIR as a fallback
do not write fallback output to authoritative drops cache
log fallbackApplied=BLOCK_ITEM
document that ore, stone, leaves, gravel, silk-touch, and fortune-sensitive
blocks may not preserve full drop semantics
```

### Files To Inspect First

Primary local file:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/api/java/baritone/api/utils/BlockOptionalMeta.java
```

Primary methods and state:

```text
getVanillaServerPack()
getManager()
drops(Block)
getStackHashes(...)
static lootTables
static drops cache
small root-cause unwrap and diagnostic helper, if needed
```

Inspect, but do not change in the first containment patch unless local diff
evidence proves it is required:

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/api/java/baritone/process/BuilderProcess.java
BuilderProcess$2.partOfMask()
```

Only consider `BlockOptionalMeta$ServerLevelStub` when the deepest `Caused by`
frame directly points at that stub, `LootContext`, registry access, or enabled
features.

### Areas Not To Change For This Bug

Do not use this failure as a reason to modify:

```text
LAVI WebSocket bridge
FabricChatClefCommandDispatcher
ChatClef command lifecycle
TaskRunner
CommandExecutor
UserTaskChain
SingleTaskChain
AltoClef Task common lifecycle
Baritone pathfinding algorithm
BuilderProcess fullRecalc algorithm
Baritone world or disk cache management
Fabric API global mixins
Minecraft global resource reload behavior
```

Do not add broad `catch (Throwable)`, reset all Baritone caches per command,
or delete Baritone disk cache as a root-cause fix for this JVM-local failure.

### Minimum Verification Before Committing A Fix

Before committing a source fix for this failure, verify:

```text
new BlockOptionalMeta(Blocks.COBBLESTONE) does not throw
new BlockOptionalMeta(Blocks.DIRT) does not throw
new BlockOptionalMeta(Blocks.FURNACE) does not throw
self-drop ItemStack matching still works
unrelated ItemStack matching stays false
reload failure does not publish a partial manager
failed drop resolution does not write the authoritative drops cache
identical failure logging is rate-limited
same JVM can run multiple commands without static-state poisoning
```

Runtime smoke flow:

```text
get cooked_beef 10
get gold_ingot 10
get iron_ingot 10
```

Expected result:

```text
no BlockOptionalMeta manager failure stack trace
ChatClef bridge connection remains normal
Baritone pathing and BuilderProcess continue normally
command lifecycle can still complete with matching_task_finished
no new log explosion
```

## Commit Split For A Future Source Fix

Keep any future source work small and reviewable:

```text
test/diag(baritone): capture BlockOptionalMeta loot reload failures
fix(baritone): publish LootDataManager only after successful reload
fix(baritone): avoid caching failed block drop resolution
```

Only add a separate BuilderProcess commit if a local-vs-upstream diff proves an
unintended local `partOfMask()` regression:

```text
fix(baritone-builder): remove unintended BlockOptionalMeta construction from mask scan
```

## Recovery Procedure

Use this when the goal is a clean operational reset of Baritone's world cache.

1. Stop the current AltoClef / Baritone task.
2. Exit the Minecraft world.
3. Fully close Minecraft.
4. Open the affected world save directory.
5. Delete or rename only the affected Baritone `cache` directory.
6. Preserve Baritone waypoints and settings unless the user explicitly approves
   deleting them.
7. Restart Minecraft.
8. Re-enter the world and reproduce the same command.

Do not delete Baritone cache while Minecraft is running. Baritone may still
have cache data in memory and may write it back to disk during shutdown.

Prefer renaming over deletion when evidence preservation matters, for example:

```text
cache -> cache_disabled_20260805
```

## What To Delete

Allowed target, after Minecraft is closed and the exact affected world is
verified:

```text
<world>\baritone\...\cache
<world>\.baritone\...\cache
```

Do not delete these unless the user explicitly asks for them and the impact is
explained:

```text
waypoints
settings
the whole world save
the whole Baritone directory
the whole Minecraft instance
logs
mods
resource packs
shader packs
```

If the cache path is unclear, stop and ask. Do not guess a broad delete target.

## Lightweight In-Game Commands

These commands may help with a lighter cache refresh:

```text
#repack
#reloadall
```

Use them when the suspected mismatch is local or minor.

They are not a guaranteed replacement for closing Minecraft and removing the
affected cache directory after a world copy, restore, replacement, or rename.

## LAVI Test Workflow Recommendation

For repeated LAVI testing:

```text
world copy/restore/replace
    -> close Minecraft first
    -> replace the world
    -> remove or rename that world's Baritone cache
    -> launch Minecraft
    -> run the test command
```

Treat "world replacement plus cache reset" as one operational unit.

If this becomes frequent, a future launcher-side feature may safely automate
the pre-launch backup/removal of the affected Baritone cache. That future
automation must:

```text
run only while Minecraft is not running
verify the exact instance and world path
rename or back up before deletion when practical
delete only cache directories
preserve waypoints and settings
log the exact path and action
ask before broad or ambiguous deletion
```

## Diagnostics Before Code Changes

When this symptom appears, check logs for:

```text
current command and correlation id
root task and child task
pathfinding start and elapsed time
movement/node/open-set/path-node counts already emitted by Baritone
world name and save path
recent world copy, restore, replacement, or rename
Baritone cache save/reload lines
whether the same command works after cache reset
```

Do not make a LAVI behavior fix merely because pathfinding looks wrong after a
world replacement. First determine whether a stale Baritone cache explains the
behavior.

If the cache has already been reset and the same command still shows repeated
task selection, repeated `DestroyBlockTask`, `task_identity_mismatch`,
`cancelled_without_task`, or a command lifecycle that remains
`waiting_for_terminal_condition`, use the lifecycle diagnostic runbook before
proposing a behavior fix:

```text
plugins/Minecraft/docs/chatclef-task-lifecycle-diagnostics.md
```

That runbook separates Baritone cache suspicion from command root ownership,
TaskFinishedEvent correlation, parent-child target handoff, child replacement,
Interact lifecycle, and Baritone path ownership.

If cache reset has already been performed and the same world still reproduces
the symptom after waiting, do not keep treating cache as the only owner. The
next diagnostic boundary is:

```text
DestroyBlockTask custom goal active
  -> Baritone path present/adopted
  -> Baritone pathing started or no-path/failure state observed
```

In that case, collect bounded lifecycle diagnostics for:

```text
TASK_CHILD_RECONCILIATION
BARITONE_EXISTING_CANCEL_BOUNDARY
DESTROY_NAVIGATION_STATE_TRANSITION
BARITONE_GOAL_PATH_TRANSITION
MOVEMENT_PROGRESS_CHECK_RESULT
BLOCK_UNREACHABLE_REQUEST
BLOCK_BLACKLIST_STATE_CHANGED
```

Do not add a timeout, retry, blacklist threshold change, global path
cancellation, or Baritone source change until this boundary is proven.

## References

Primary upstream references:

```text
Baritone USAGE.md:
https://github.com/cabaletta/baritone/blob/1.19.4/USAGE.md

Baritone Settings Javadoc:
https://baritone.leijurv.com/baritone/api/Settings.html
```

Relevant upstream behavior notes:

```text
USAGE.md documents repack, reloadall, and saveall cache commands.
Settings Javadoc documents chunkCaching as simplified saved chunks for
very-long-distance pathing.
Settings Javadoc documents repackOnAnyBlockChange as repacking the whole chunk
when a block changes.
```

Related upstream issue example:

```text
Baritone issue #3790:
https://github.com/cabaletta/baritone/issues/3790

This issue reports a mining loop/stall pattern. It is not proof that every LAVI
case has the same cause, but it is evidence that Baritone can have long-running
or looping mining/pathing symptoms independent of LAVI.
```

## Current Local Conclusion

For the LAVI test environment, stale Baritone cache should be considered a
high-priority operational suspect when all of these are true:

```text
the world was copied, restored, replaced, or renamed
the same code and command behave differently after changing the world files
the bot shows impossible routes, long pathfinding, or repeated mining/pathing
the behavior improves after removing or renaming the affected Baritone cache
```

This conclusion does not remove the need for diagnostics when the cache has
already been reset and the same failure still reproduces.
