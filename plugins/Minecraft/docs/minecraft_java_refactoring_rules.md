<!-- 20260726_kpopmodder: Restored Java refactoring rules for Minecraft mod maintenance. -->

# Minecraft Java Refactoring Rules

This document defines mandatory maintainability rules for editing the LAVI Minecraft Java mod.

## Core Rule

When modifying Java code, if a class, module, chain, task, service, manager, adapter, or facade has two or more responsibilities, refactor it and organize the responsibility into an appropriate package before adding more behavior.

This is not optional. Split responsibilities first, then implement the feature.

## Responsibility Split Criteria

Split the code when one unit handles two or more of the following responsibilities:

- Priority decisions
- State decisions
- Inventory calculation
- Food or item selection
- Pathfinding task creation
- Combat or danger checks
- Container interaction
- Configuration interpretation
- Logging or status output
- Bridge API response generation
- User command parsing

For example, `FoodChain` must not directly own hunger checks, food scoring, raw-food cooking decisions, combat danger checks, and task construction at the same time.

## Package Organization

Keep new code close to the existing package structure and place focused logic under a domain folder.

- Chain decision policies: `adris.altoclef.chains.<domain>`
- Resource-task helper logic: `adris.altoclef.tasks.resources.<domain>`
- Bridge state readers: `adris.altoclef.lavibridge.state`
- Bridge actions and command handling: `adris.altoclef.lavibridge.actions`
- Shared calculations or helpers: use existing `util.helpers` only when the logic is truly generic

Do not create folders just for appearance. Each folder should have a responsibility that can be explained in one sentence.

## Required Workflow

1. Check how many responsibilities the target class currently owns.
2. If it owns two or more independent responsibilities, split them before adding behavior.
3. Preserve existing public APIs, config keys, command names, runtime behavior, and compatibility paths whenever possible.
4. Keep each new class focused on one primary responsibility.
5. Do not remove existing fallback behavior without a clear reason.
6. Run the smallest relevant build or compile verification after the change.

## Forbidden Patterns

- Do not add behavior with the intention of cleaning it up later.
- Do not keep accumulating calculation, decision, and execution logic in one class.
- Do not keep adding survival policy directly into large classes such as `FoodChain`, `MobDefenseChain`, or `CollectFoodTask`.
- Do not hide pathfinding failures with temporary conditionals only.
- Do not interpret the same config value differently in multiple locations.

## Recommended Shape

Good structure:

- `FoodChain`: chain priority and task handoff
- `HungryFoodCollectionPolicy`: decides whether hungry recovery should collect food
- `FoodInventoryCalculator`: calculates inventory food score
- `FoodActionSafetyPolicy`: decides whether combat or danger should defer food actions
- `RawFoodCookingTaskFactory`: creates raw-food cooking tasks

Bad structure:

- `FoodChain` directly implements food scoring, hostile detection, smoker interaction, hunting target selection, and config fallback logic.

## When Unsure

If it is unclear whether a responsibility should be split, split it.

If a large refactor may break working behavior, start with a small adapter, policy, or helper class that preserves the existing flow while separating the responsibility.

The purpose is maintainability. Refactoring should be smaller and safer than the feature change itself.
