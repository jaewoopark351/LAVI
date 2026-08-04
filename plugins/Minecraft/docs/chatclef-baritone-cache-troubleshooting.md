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
