<!-- 20260821_kpopmodder: Added scope clarification separating the deposit idle-root fix from diamond-pickaxe crafting behavior. -->
<!-- 20260821_openai: Reviewed v2; clarified the Python admission boundary and separated scope clarification from implementation/provenance approval. -->

# ChatClef Deposit Sync-Finish Idle Root Scope Clarification - 2026-08-21

Date: 2026-08-21 KST

Status:

```text
document status: SCOPE_CLARIFICATION_ADDENDUM
source code change in this pass: none
test source change in this pass: none
Gradle/build/test execution in this pass: none
runtime/JAR/Minecraft execution in this pass: none
implementation approval granted by this document: no
Approval unit A evidence status changed by this document: no
Approval unit B edit approval changed by this document: no
```

This addendum clarifies the user-facing scope of the deposit sync-finish
idle-root documentation set without changing the reviewed implementation plan,
investigation, pre-change report, or raw evidence files.

## Relationship To The Reviewed Document Set

This is an interpretive scope addendum only. It does not replace or amend the
root-cause analysis, implementation hunks, result-fidelity matrix, test plan,
pre-edit evidence, or approval gates in the reviewed document set.

Referenced document identities reported for this package are:

```text
investigation: 49628CC0AF62602E8062E1AE75D6E49603F78CAADEC53BA99F8A4B4FC0BF66B2
implementation plan: 7AB95468FB3BEAFC1F522A782A8281D428DCE0DBCE1910F81939E5E24602D9D5
pre-change report: 2D9D4D3E5BCFC404A36CA92D9FE7D2440F0288635A7BCB6BF4850DB40819F288
pre-edit evidence summary: 6DB56F7E24AF339578FA0CE68B7D02ADA5BB50CD457B86926C04B24FB0516A1F
```

Those hashes identify the referenced package state; this addendum does not
claim that a missing evidence item is supplied merely by citing its hash.

## Incident Evidence Versus Production Predicate

The verified incident and approval evidence are deposit-specific. The production
bridge classifier must nevertheless remain command-agnostic, as required by the
reviewed implementation plan. It must not inspect the command name, Korean text,
`StoreInAnyContainerTask` absence, or the insufficient-item log string.

A generic classifier may safely recognize the same synchronous-finish / no-new-
root / pre-existing-idle ownership shape in another command. That does not prove
the other command's parsing, compilation, task construction, terminal semantics,
or gameplay behavior.

## Direct Target

The current documentation and proposed first patch target this verified failure
class:

```text
deposit diamond 2
DepositCommand finishes synchronously
no new StoreInAnyContainerTask or UserTaskChain root is created
the LAVI Java bridge mistakes the pre-existing automatic IdleTask for a command-owned root
Java waits for a TaskFinishedEvent that running idle does not publish
no terminal command_result is sent
Python remains fail-closed busy
later commands can be rejected before a new Java bridge command is submitted or executed
```

The precise patch name should remain:

```text
sync-finish no-new-root idle-root ownership fix
```

It should not be renamed or approved as a diamond-pickaxe crafting fix.

## Not Directly Proven

The current deposit idle-root evidence does not prove any of these for the
Korean command `다이아 곡괭이 만들어줘`:

```text
Korean intent parsing succeeds or fails
item aliasing maps correctly to diamond_pickaxe
the command becomes a craft/get command
the command reaches the Java bridge
a craft task root is created
the player has or lacks required materials
a crafting table/workbench is found or used
recipe lookup succeeds or fails
Craft/Get task execution succeeds or fails
```

Those belong to a separate diamond-pickaxe command investigation if the command
fails from a fresh baseline where Java and Python active ownership are both clear,
or if it still fails after the false-active bridge path is fixed or ruled out.

## Allowed Relationship To Diamond Pickaxe Reports

The deposit idle-root work can explain only this narrow relationship:

```text
previous command stuck active
-> Python active/admission gate remains closed
-> the next request is not admitted as a new Java bridge command
-> that request might be "다이아 곡괭이 만들어줘"
```

The precise Python stage is not part of this addendum's causal claim. Depending
on the route state, reconciliation may block before translation, or Korean
trigger/intent/alias/translation work may already occur before submission
precheck rejects the request as `minecraft_command_busy`. The supported boundary
is that no new Java command is admitted and no Java Craft/Get task for the
diamond-pickaxe request begins.

Fixing the false-active state may allow the diamond-pickaxe request to proceed
past admission, but it does not prove that parsing, compilation, recipe lookup,
materials, workbench selection, task execution, or terminal reporting is correct.

## Required Evidence For A Diamond-Pickaxe-Specific Claim

Before claiming that `다이아 곡괭이 만들어줘` itself is fixed or broken, collect a
separate evidence package showing:

```text
the exact user input
pre-submit Java queue and Python active-ownership state
Python intent and item/action resolution
admission or active-gate decision
bridge request id / command message id / correlation id
Java command string actually dispatched
whether the Java command returns a synchronous finish, command exception, or user task root
whether a Craft/Get task is created
inventory/material/workbench/recipe observations
terminal command_result status, result_reason, and result_fidelity
observed gameplay effect, recorded separately from lifecycle terminal status
```

Until that evidence exists, keep the implementation and approval language tied
to the deposit sync-finish idle-root ownership fix only.

## Approval And Evidence Boundary

This addendum does not:

```text
close Approval unit A or prove that its raw evidence package is complete
approve Approval unit B or authorize Java/test/document edits
replace an exact investigation file, registered-command audit, manifest, or provenance addendum
change the approved Java file list, minimum hunks, fidelity matrix, test strategy, or non-goals
make the diamond-pickaxe command a mandatory acceptance fixture for the deposit patch
authorize test/Gradle execution, build, JAR copy, Minecraft launch, commit, or push
```

Until separate evidence and approval gates are satisfied, use this document only
to prevent the deposit lifecycle fix from being mislabeled as a diamond-pickaxe
crafting fix.
