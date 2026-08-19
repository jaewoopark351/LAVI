<!-- 20260819_kpopmodder: Split the Python-only inventory cleanup preflight contract from the broader command orchestration plan. -->

# ChatClef Python Inventory Cleanup Preflight Contract

Date: 2026-08-19

Reviewed archive baseline:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

This document owns the Python-only inventory-full cleanup contract for Fabric
ChatClef natural-language commands. It is documentation only. It does not
approve code changes, tests, settings changes, Java changes, DTO changes, wire
payload changes, ChatClef / AltoClef behavior changes, Minecraft launch,
runtime reproduction, commit, push, or automatic replay.

## Authority

This contract is narrower than the general orchestration plan:

```text
chatclef-python-command-orchestration-plan.md
  -> user replies, lifecycle wording, operation-level flow, and references to
     this cleanup contract

chatclef-python-inventory-cleanup-preflight-contract.md
  -> inventory evidence, FULL / AVAILABLE / UNKNOWN policy, protected-item
     policy, cleanup planning, cleanup postconditions, and fail-closed gates
```

Korean item aliases, craft wording, and canonical display names are owned by:

```text
chatclef-korean-item-action-alias-v2-plan.md
chatclef-korean-test-strategy.md
chatclef-korean-post-review-merge-blockers.md
```

## Evidence Model

The cleanup preflight may use only reliable Python-owned inventory evidence.
It must not infer inventory-full from logs, task duration, path stalls,
timeouts, terminal UNKNOWN, or a long-running ChatClef task.

Every inventory snapshot must carry at least:

```text
snapshot_id
player/world/session identity
observed_at_ms
source
state: AVAILABLE | FULL | UNKNOWN
free_storage_slots
slot-level or exact per-target counts
equipped/offhand/armor distinction
confidence or validation result
```

The default provider is fail-safe:

```text
NoReliableInventorySnapshotProvider -> UNKNOWN
```

## State Semantics

Inventory evidence has three states:

```text
AVAILABLE:
  fresh evidence proves enough free storage slots for the primary command

FULL:
  fresh evidence proves no usable storage slot is available

UNKNOWN:
  evidence is missing, stale, ambiguous, identity-mismatched, or below
  confidence threshold
```

Preflight UNKNOWN and cleanup UNKNOWN are different:

```text
preflight inventory state == UNKNOWN:
  automatic cleanup is not allowed.
  existing behavior may submit the primary command once if the broader
  orchestrator policy still allows that path.

cleanup outcome == UNKNOWN:
  the primary command must not be submitted.

post-cleanup inventory state == UNKNOWN:
  the primary command must not be submitted.
```

## Cleanup Candidate Policy

`잡템` is a Python policy mode, not a concrete item alias. It must not be added
to `korean_item_aliases.json`.

Cleanup v1 is deny-by-default. A cleanup candidate must satisfy all of these:

```text
explicitly allowed disposable target
exact current inventory evidence
not protected
observed quantity is enough to free at least one slot
```

Protected items include:

```text
current primary target
primary dependency/material
tools
equipped armor
spare critical armor
food
torches
fuel
buckets and movement essentials
building/navigation essentials
rare items
user-protected targets
```

Automatic cleanup must not use argument-less `deposit`. Bare Java `deposit` can
store useful items such as food, torches, fuel, ingots, and ordinary blocks. It
is not a safe synonym for "junk cleanup."

## Cleanup Command Shape

The first automatic cleanup implementation, when separately approved, is
limited to exactly one targeted cleanup command:

```text
deposit <observed_disposable_target> <observed_quantity>
```

Example:

```text
deposit dirt 64
```

The quantity must come from the fresh inventory snapshot. These are prohibited:

```text
deposit
deposit dirt 9999
deposit <target> <placeholder-count>
```

Per operation:

```text
max_cleanup_attempts = 1
required_free_slots = 1
```

## State Machine

The cleanup state machine must fail closed:

```text
IDLE
PREFLIGHT_EVALUATION
CLEANUP_SUBMITTING
WAITING_CLEANUP_TERMINAL
VERIFYING_CLEANUP_EFFECT
PRIMARY_SUBMITTING
WAITING_PRIMARY_TERMINAL
COMPLETED
FAILED_BEFORE_PRIMARY
FAILED
MANUAL_RECOVERY_REQUIRED
ABORTED
```

Required cleanup-to-primary path:

```text
reliable FULL
  -> safe cleanup plan
  -> one targeted deposit
  -> matching cleanup terminal completed
  -> fresh post-cleanup inventory verification
  -> primary command exactly once
```

The orchestrator may submit the primary command only when all of these are
true:

```text
cleanup request accepted
cleanup terminal status completed
matching request/session/correlation/generation
active command cleared
fresh post-cleanup snapshot available
post-cleanup free slot count >= required_free_slots
protected_item_preservation_verified == true
cleanup_attempt_count == 1
primary_was_accepted == false
```

`cleanup terminal status == completed` is not enough. Java `DepositCommand` may
finish without proving that the requested cleanup actually freed an inventory
slot.

## Fail-Closed Outcomes

All of these outcomes submit zero primary commands:

```text
cleanup rejected
cleanup failed
cleanup cancelled
cleanup deadline_exceeded
cleanup UNKNOWN
disconnect
stale result
wrong request/session/correlation/generation
post-cleanup FULL
post-cleanup UNKNOWN
post-cleanup stale
protected-item preservation unknown or incomplete
protected-item policy violation
cleanup attempt count already consumed
primary_was_accepted == true
```

No automatic retry, replay, rerun, or resubmit is allowed for cleanup or the
primary command.

## Shadow Mode Before Execution

Before automatic cleanup execution is enabled, implement a shadow phase:

```text
inventory snapshot provider
FULL / AVAILABLE / UNKNOWN classification
snapshot freshness and identity checks
slot-level evidence capture
protected target computation
cleanup plan creation
diagnostic "would submit" command
actual cleanup submit count == 0
```

If no reliable Python inventory provider exists, this phase must end as
UNKNOWN. Do not add Java inventory payload fields or common DTO fields to fill
this gap.

## Test Contract

Required regression coverage:

```text
preflight AVAILABLE -> cleanup 0, primary 1
preflight UNKNOWN -> cleanup 0
preflight FULL + safe plan -> targeted cleanup 1
preflight FULL + no safe target -> cleanup 0, primary 0
automatic cleanup creates bare deposit 0
automatic cleanup creates placeholder count 0
primary target is never a cleanup candidate
required material is never a cleanup candidate
tools/armor/food/torch/fuel/protected items are excluded
cleanup accepted -> primary 0 before terminal
cleanup terminal completed alone -> primary 0
post-cleanup AVAILABLE + protected preservation verified -> primary 1
protected preservation false or unknown -> primary 0
post-cleanup UNKNOWN -> primary 0
post-cleanup FULL -> primary 0
cleanup failed/cancelled/deadline/unknown/disconnect -> primary 0
stale or wrong-identity result -> workflow advance 0
duplicate cleanup terminal -> primary duplicate 0
cleanup attempt max 1
primary accepted -> replay 0
restart -> prior operation replay 0
submission UNKNOWN reconciliation pending -> new command translation/submit 0
```

Suggested future test folder:

```text
tests/minecraft_chatclef/inventory_preflight/
```

## Hard Prohibitions

This work MUST NOT:

```text
modify any file under
  plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/**
modify plugins/Minecraft/common/dto/**
modify plugins/Minecraft/common/protocol/**
modify plugins/Minecraft/common/schema/**
add, remove, rename, or reinterpret any v1 wire message or payload field
add fields to CommandRequestDTO, CommandResultDTO, StatusSnapshotDTO,
  or BridgeEnvelopeDTO
add a new Java craft command or change GetCommand semantics
change DepositCommand, EquipCommand, GiveCommand, or ChatClef command grammar
change ChatClef, AltoClef, Baritone task selection, retry, timeout,
  completion, container transfer, or goal behavior
implement Korean parsing, aliases, responses, junk policy, or orchestration
  in Java
infer inventory-full from logs, task duration, path stalls, timeout,
  deadline, or UNKNOWN results
infer gameplay effect from ACCEPTED, RUNNING, terminal COMPLETED,
  active-request clearing, or connection state alone
use argument-less deposit as automatic junk cleanup
generate placeholder cleanup quantities
automatically retry, replay, rerun, or resubmit cleanup or primary commands
submit a follow-up command inline from a WebSocket callback
let an LLM generate raw ChatClef DSL or decide workflow state transitions
emit an @ prefix from the Python natural-language compiler
put contextual pseudo-targets such as 잡템, 갑옷, player names, quantities,
  or command modes into korean_item_aliases.json
introduce Forge MineMind fallback or shared Fabric/Forge orchestration
```

If an implementation appears to require any prohibited change, stop the phase
and open a separate evidence-backed design review. Do not expand this scope
implicitly.
