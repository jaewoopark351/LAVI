<!-- 20260815_kpopmodder: Recorded the post-implementation Korean ChatClef merge-blocker review. -->
<!-- 20260815_chatgpt: Reconciled the documentation-set resolution and retained only implementation/CI blockers as current merge gates. -->
<!-- 20260818_kpopmodder: Added the post-restore live-test addendum and corrected request-correlation versus environment-preflight status. -->
<!-- 20260818_kpopmodder: Bound live status to audited baseline 4239c23 and added approval, no-replay, and end-to-end observation blockers. -->
<!-- 20260818_kpopmodder: Added gameplay observation completeness and verified expected/partial/prohibited effect merge criteria. -->
<!-- 20260819_kpopmodder: Recorded strict listener identity, canonical submission reconciliation, and split batch advancement gates. -->
<!-- 20260819_kpopmodder: Added the Python Korean command orchestration v2 documentation-status addendum for reviewed archive e08af639. -->
<!-- 20260820_kpopmodder: Added Korean command registry and lifecycle-axis documentation status. -->

# ChatClef Korean Post-Review Merge Blockers

Original review date: 2026-08-15

Current live-status reconciliation date: 2026-08-18

Historical live-status audited implementation baseline: `4239c23`

Current reviewed source baseline for the 2026-08-19 Korean orchestration and
cleanup documentation pass:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

This document records the original documentation-only merge review for Korean
Fabric ChatClef routing and test work around:

```text
9f90246 feat: expand Korean ChatClef get command routing
cfc170a test: cover Korean ChatClef item command routing
```

The original conclusion was:

```text
The revised documentation set is acceptable when committed together.
The implementation merge remains on hold.
```

This document does not approve Python behavior changes, Java changes, CI
changes, DTO changes, wire payload changes, ChatClef / AltoClef engine changes,
Minecraft launch, runtime reproduction, commit, push, or automatic command
replay.

## Documentation Set And Authority

Original 2026-08-15 Korean alias documentation unit:

```text
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
```

The 2026-08-19 Python Korean command orchestration follow-up expands the
directly affected documentation set to seven files:

```text
plugins/Minecraft/README.md
plugins/Minecraft/docs/chatclef-python-command-orchestration-plan.md
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
plugins/Minecraft/docs/chatclef-korean-item-action-alias-v2-plan.md
plugins/Minecraft/docs/chatclef-korean-test-strategy.md
plugins/Minecraft/docs/chatclef-korean-post-review-merge-blockers.md
plugins/Minecraft/docs/chatclef-korean-item-command-resolution-analysis.md
```

The 2026-08-20 Korean command-registry split adds one normative planning
document without changing code:

```text
plugins/Minecraft/docs/chatclef-python-korean-command-registry-plan.md
```

Authority is split as follows:

```text
alias v2 plan:
  item/action UX design, aliases, canonical display, capability gates, and
  phase order

Python Korean command registry plan:
  20 registered command snapshot, command resolver domains, lifecycle kinds,
  safety tiers, confirmation modes, allowed sources, and public enablement axes

Python command orchestration plan:
  response evidence, lifecycle wording, single-pass submission, and primary
  command sequencing

Python inventory cleanup contract:
  inventory evidence, protected-item policy, targeted cleanup, fresh
  post-cleanup verification, and fail-closed primary admission

test strategy:
  test evidence, artifact schemas, source/hash and activation authority,
  coverage algorithm/parser provenance, CI scope, and live-test safety

this document:
  current implementation/CI status and the merge decision

Korean item command resolution analysis:
  historical diagnosis only

README:
  navigation and ownership summary only
```

The revised test strategy supersedes older conflicting test, coverage,
activation, hash, and CI wording in the v2 plan. This document does not repeat
large source-hash or command tables; the strategy and future JSON artifacts are
the authority for those details.

## Original 2026-08-15 Review Scope And Reported Result

The review covered the local source snapshot around `cfc170a` and the parent
implementation commit `9f90246`.

Reported focused ChatClef result:

```text
98 passed
9 failed
2 skipped
258 subtests passed
```

The nine failures were reported as Java source-hash subtest failures in:

```text
tests/test_minecraft_chatclef_java_item_command_contract.py
```

Additional reported notes:

```text
Python compileall passed.
JSON parsing passed.
Full repository pytest has unrelated collection errors from optional
  dependencies, so the review used the ChatClef-focused suite.
Ruff was unavailable in the review environment.
```

## Original 2026-08-15 Documentation Findings

The original revised three-file set documented:

```text
historical c912ff baseline, reviewed cfc170a baseline, and a fixed audited implementation baseline
  as separate states
Git blob bytes as the source-hash authority
reviewed commit provenance and HEAD drift as separate assertions
full ChatClef built-in activation path:
  fabric.mod.json
  -> altoclef.mixins.json
  -> EntryMixin
  -> TitleScreenEntryEvent publication
  -> AltoClef event subscription
  -> onInitializeLoad
  -> initializeCommands
  -> AltoClefCommands
  -> CommandExecutor
full overlay activation path:
  fabric.mod.json
  -> OverlayEntrypoint
  -> END_CLIENT_TICK callback
  -> OverlayCommandRegistrar
  -> OverlayCommand
coverage algorithm version 1 plus catalog parser commit/path/hash
production catalog baseline-target requirements
support-matrix schema version 1 independent from evidence maturity
PRE_SHARED_CASE and SHARED_CASE mutually exclusive evidence fields
Tier 1 Python source/AST extraction without a Java process
optional Java harness only in a separate build-contract parity job
result_reason observation as last_result.data.result_reason
planned golden IDs separated from current test paths
current Java source-backed contracts separated from planned Python phases
```

These documentation resolutions do not mean the implementation or CI gates
below are complete.

## 2026-08-18 Post-Restore Live-Test Addendum

This addendum supersedes only the live request-correlation and preflight status
wording in the older review snapshot below. It does not claim that unrelated
merge blockers have been re-audited or cleared.

Read-only review of clean audited implementation baseline `4239c23` found:

```text
implemented:
  explicit live/mutating opt-in
  one Gradio command submission
  submitted request_id capture
  matching terminal request_id polling
  stale-result rejection regression test
  matching terminal and same-refresh active_request_id clear
  conditional nonblank result_reason check when data is a dict and the key exists
  no retry/replay in the current polling flow

absent or incomplete:
  explicit mutating loopback LAVI_GRADIO_URL without default fallback
  fresh exact command/URL/backend/instance/world/one-shot approval tuple
  fail-closed backend/instance/world preflight
  Gradio/Fabric listener same-process ownership gate
  hidden second LAVI detection
  exact latest.log strict-decode fallback
  command-immediate status/ownership TOCTOU recheck
  structured preflight runner and offline fixtures
  deterministic fail rather than skip after mutating opt-in
  lower adapter command_request exactly-once evidence
  submission_outcome_unknown reconciliation and next-run block
  IDE/CI/flaky/Codex automatic-rerun guard
  runtime-reported completion and gameplay/end-to-end effect verification
  gameplay_observation_complete and explicit expected/partial/prohibited effect fields
```

The request-ID correlation gap was closed in `6d75c6e` and is present in
audited baseline `4239c23`. The enhanced preflight was documented as implemented
only in a later dirty working tree; it was not included in the docs-only commits
and is absent from the audited baseline after restoration. A later source/test
commit requires re-audit rather than inheriting this status from the word
`HEAD`.

The current live test accepts any matching terminal status as lifecycle
completion, including failure statuses. It does not require `completed` and
does not inspect inventory, movement, or block changes. Consequently it is a
one-shot lifecycle-correlation live test, not a complete gameplay-effect E2E
test, and must not run unattended before fail-closed preflight is restored and
verified.

## Merge-Blocking Implementation Findings

### 1. Java Contract Hash Authority Is Not Implemented Consistently

The documented authority is raw Git blob bytes:

```text
git show --no-textconv <commit>:<repository-relative-path>
```

The current test still hashes working-tree bytes with `Path.read_bytes()` and
uses old expected constants. That is sensitive to CRLF conversion and does not
implement reviewed-commit provenance plus HEAD drift.

Required implementation:

```text
sha256(git_blob(reviewed_source_commit, path)) == expected_sha256
sha256(git_blob(HEAD, path)) == expected_sha256
working-tree line-ending conversion is not the authority
baseline or HEAD source drift stops fixture validation
```

If the test needs a historical commit in CI, checkout must provide it, for
example with `fetch-depth: 0`.

### 2. Windows CI Does Not Run The Focused ChatClef Scope

Required default offline scope:

```text
tests/minecraft_chatclef/**
tests/test_minecraft_chatclef_*.py
tests/test_llm_minecraft_input_router.py
```

Required result:

```text
0 failed
live Minecraft tests skipped unless explicit opt-in is present
```

PowerShell-safe discovery is required. GitHub Actions must not remain green
while the focused local suite is red.

### 3. Numeric Intent Validation Is Too Loose

Strict validation applies before DTO coercion to every numeric slot:

```text
quantity, food_units:
  exact int 1..2147483647

x, y, z:
  exact int -2147483648..2147483647
```

Reject before adapter submission:

```text
bool
float
None when a required slot is present
overflow
numeric strings unless an explicit normalization policy accepts them
```

Minimum regression vectors include:

```text
2147483648
False
1.0
1.5
True
"1.5"
None
food_units=True
food_units=1.5
x=True
y=1.5
x=-2147483649
z=2147483648
```

Every invalid path must have adapter submit count `0`.

### 4. TranslationResult And Submission Boundary Need Hardening

`executable` must be an actual `bool`; `bool("false")` must never create an
executable command.

An executable result requires:

```text
status == VALIDATED
nonblank prefixless command
safe command characters
validated structured intent
recompiled command == supplied command
```

Reject before submission when command is missing, blank, prefixed, contains
CR/LF, `#`, semicolon, quote, or a control character, or when status and
`executable` disagree.

Required result for every malformed case:

```text
adapter submit count == 0
retry count == 0
no LLM fall-through after a malformed validated result
```

### 5. Source-Backed Artifacts Are Not Committed

Still required:

```text
tests/minecraft_chatclef/command_catalog/chatclef_registered_commands.snapshot.json
tests/minecraft_chatclef/command_catalog/chatclef_command_support_matrix.json
shared repository-owned golden case artifact
runtime baseline/reviewed/current coverage artifacts
coverage floor artifact
```

The registered-command snapshot must verify the full Fabric/Mixin activation
chains, registration sources, command classes, command-name sources, and global
effective-name uniqueness. Commented `// new StashCommand()` must not become a
registered command.

Coverage artifacts must carry:

```text
coverage_algorithm_version
catalog_parser_source_commit
catalog_parser_source_path
catalog_parser_source_sha256
```

They must fail when a production baseline target is missing, even if the total
catalog count remains 591.

### 6. Live Mutating Environment Preflight Is Missing

Before sending a mutating command, the live test must verify:

```text
fresh approval tuple matches exact command, Gradio URL, backend, instance,
  world/save directory, and one-shot invocation
explicit Gradio URL is an approved loopback endpoint; no default fallback
backend == fabric_chatclef
instance == dedicated expected instance
world == dedicated expected world
no conflicting active request
```

No live/mutating opt-in means `skip` and zero submissions. Once both opt-ins
select a mutating run, a missing dependency/value or unknown, inaccessible,
stale, unstable, ambiguous, or mismatched evidence is `fail`, not `skip`, and
must leave both Gradio submit and adapter command counts at zero.

Audited baseline terminal lifecycle observation requires:

```text
last_result.request_id == submitted request_id
last_result.status is terminal
active_request_id == null in the same refresh snapshot
if last_result.data is a dict and result_reason exists, it is nonblank
```

Missing `result_reason` alone is not a baseline failure; status-specific
requiredness remains `[unverified]`. Submitted request-ID matching is already
implemented in audited baseline `4239c23`. The
remaining blocker is to run that correlation only after the intended backend,
process, instance, world, connection, lifecycle, and idle ownership have been
verified fail closed immediately before submission.

Keep lifecycle, runtime report, effect observation, and end-to-end success
separate:

```text
terminal lifecycle observed:
  same request_id + terminal status + active request clear

runtime-reported completion:
  terminal lifecycle observed + status == completed

gameplay effect observed:
  expected, partial, or unexpected Minecraft state/effect recorded independently
  of terminal status

end-to-end success:
  runtime-reported completion
  + gameplay observation complete
  + expected gameplay effect verified
  + prohibited effect absence verified
```

A failed, rejected, cancelled, deadline-exceeded, or runtime terminal status
`"unknown"` can still leave partial movement, block, or inventory changes.
Runtime value `"unknown"` is distinct from tri-state `unknown`, meaning an
observation value could not be established.

The strategy and `LiveRunObservation` must preserve these separate fields:

```text
gameplay_observation_complete
expected_gameplay_effect_verified
partial_gameplay_effect_observed
prohibited_effect_absence_verified
```

The future command-specific oracle must define pre-state, expected effect,
observation source, tolerance, forbidden effect, timeout, and reconciliation
procedure. Not seeing a prohibited effect through an incomplete observation
source is not verified absence and must leave prohibited-effect absence and E2E
as `unknown`.

Ownership and loopback tests must cover session, correlation, connection
generation, stale-session rejection, stale-generation rejection, and wrong
request-ID rejection. A stale terminal result from an earlier command must not
make a new live test pass.

No mutating live test may auto-retry or replay an accepted command. If the
server may have received a request but the accepted response was lost, record
`submission_outcome_unknown`, do not replay, and reconcile active request, last
result, request/correlation evidence, and Minecraft state before another
mutating run. Observer timeout is not terminal and must not trigger automatic
`@stop`, cancel, replay, or IDE/CI/flaky/Codex rerun. `active_request_id == null`
proves only Python lifecycle clear, not Minecraft Task cessation or absence of
partial/unexpected effects.

## Accepted Direction

The following direction remains approved:

```text
다이아몬드 가져와줘 -> get diamond 1
돌 10개 가져와줘 -> get stone 10
석탄 5개 캐와줘 -> get coal 5
철 10개 캐줘 -> get iron_ingot 10
```

Python Korean natural-language compilation remains prefixless. The router
translates once, validates once, compiles once, and submits at most once.
Unknown, invalid, disconnected, busy, and malformed paths submit zero commands;
an adapter rejection is not retried; an accepted command is never replayed.

EQUIP, DEPOSIT, GIVE, and multi-item GET remain phased work and are not approved
for broad implementation before Phase 0 is green.

## Required Acceptance Before Merge

Do not merge until all of the following are true:

```text
focused ChatClef suite has 0 failures
Windows CI runs the same focused offline scope
checkout provides required Git history
hash tests use raw Git blob bytes and reviewed/HEAD assertions
strict numeric validation rejects bool, float, overflow, malformed strings, and
  invalid optional-slot values before DTO coercion
malformed TranslationResult values cannot become executable submissions
invalid, unknown, disconnected, busy, and malformed paths submit 0 commands
registered-command and support-matrix artifacts are committed and source-backed
coverage artifacts and a separate non-regression floor are committed
catalog parser provenance and ten baseline-target invariants are tested
shared golden IDs are committed or remain explicitly planned
live preflight requires an explicit loopback Gradio URL, exact one-shot approval
  tuple, backend/instance/world/process identity, and command-immediate recheck
the actual gateway endpoint matches the approved endpoint before initial
  preflight and immediately before submit
the approved command candidate is snapshotted before preflight, bound with the
  validated evidence in an immutable ticket, and not reread at submission
the fixed batch retains the first verified process fingerprint across steps and
  requires exact submitted/terminal request-ID equality before advancement
selected mutating runs fail deterministically on missing/unknown/stale/ambiguous evidence
existing live request-ID matching remains covered by regression tests
terminal lifecycle, runtime-reported completion, gameplay effect, and end-to-end
  success are recorded separately
gameplay_observation_complete, expected_gameplay_effect_verified,
  partial_gameplay_effect_observed, and prohibited_effect_absence_verified are
  represented with the same tri-state meanings as the test strategy
end-to-end success requires runtime completion plus complete observation,
  expected effect verified, and prohibited-effect absence verified
incomplete observation never turns "prohibited effect not seen" into verified absence
failure terminals are checked for partial and unexpected gameplay effects
submission_outcome_unknown blocks replay and requires reconciliation
observer timeout does not cause automatic stop, cancel, retry, or replay
mutating tests are not automatically rerun by IDE/CI/flaky/Codex tooling
accepted commands are never automatically retried or replayed
the supported GET live objective verifies additive acquisition delta; the
  movement/mining objective fails before submission until complete observers exist
```

## Historical Next Work Classification Before 2026-08-19 Hardening

At the original review point, the next work was implementation/test/CI work,
not another broad design rewrite:

```text
repair Java source-hash tests
add focused Windows CI coverage
harden DTO/raw mapping numeric validation
harden TranslationResult and submission boundaries
commit source-backed command/matrix/coverage/golden artifacts
restore live backend/instance/world/process preflight while preserving existing request correlation
run the focused suite to 0 failures
```

The latest dated addenda below own the current status. Do not read the original
98/9 focused-suite result or the historical blocker list as the current
`e08af639` implementation state.

These historical items remain Python, test, artifact, and CI responsibilities
unless a separate source review explicitly approves the smallest necessary
Java-side change. Do not modify bridge DTOs, wire payloads, the ChatClef/AltoClef
engine, Forge MineMind, or automatic command replay as part of merely closing
these gates.

## Historical ChatGPT Handoff Summary Before 2026-08-19 Hardening

```text
The revised Korean ChatClef documentation set is internally reconciled and
should be committed as one unit:

- chatclef-korean-item-action-alias-v2-plan.md
- chatclef-korean-test-strategy.md
- chatclef-korean-post-review-merge-blockers.md

Documentation is acceptable; implementation merge remains on hold.

Reported focused result remains 98 passed, 9 failed, 2 skipped, 258 subtests.
The 9 failures are Java source-hash subtests. Windows CI still does not run the
focused scope. Numeric/TranslationResult hardening and source-backed artifacts
remain part of the historical review findings. Live request correlation is
implemented in `6d75c6e`, while fail-closed approval/endpoint/backend/instance/
world/process preflight, submission-unknown reconciliation, automatic-rerun
protection, and gameplay-effect oracles remain absent from audited baseline
`4239c23`.

The documentation contract now requires complete gameplay observation,
expected-effect verification, a separate partial-effect field, and actively
verified prohibited-effect absence before end-to-end success can be true.

Do not merge until the focused suite has 0 failures and every gate in Required
Acceptance Before Merge is satisfied.
```

## 2026-08-19 Current Hardening Addendum

This addendum records the current implementation gate without rewriting the
historical `4239c23` counts above. The current hardening change set implements
the reviewed work within LAVI-owned Python and offline tests:

```text
P1: parse the actual Windows Python invocation and require exact entrypoint
    provenance; bind PID/numeric UTC start time/executable/invocation/resolved
    entrypoint/repository root/approved ancestor in the pre-submit identity
    recheck; reject hidden pythonw and ambiguous fallback LAVI candidates
P2: canonicalize outer and nested submit-result mirrors and convert every
    malformed or contradictory result to UNKNOWN + reconciliation required;
    the canonical top-level and nested request IDs are always identical
P2: exercise a real delayed asyncio send so timeout ownership, no second wire
    request, late first delivery, and clean teardown are proven
P2: keep router-level UNKNOWN ownership latched until matching validated
    terminal evidence is explicitly reconciled
P2: separate terminal/checkpoint admission from batch advancement and reject
    non-finite gameplay polling time values before any polling loop
```

The only approved script operand is exact absolute
`python.exe <repository root>\main.py`. Relative `main.py` remains rejected even
when an approved launcher ancestor is visible; that ancestor is identity
evidence, not an exemption from the exact entrypoint rule. `pythonw.exe`, `-c`,
descendant or other-checkout `main.py`, and `-m lavi` without resolved module
provenance fail closed. A confirmed or ambiguous second LAVI candidate in the
fallback range also fails closed, while a structurally unrelated Python script
does not.

The default saved-target-inventory GET oracle remains intentionally incomplete.
It stops a real batch at the first gameplay checkpoint; fixture-driven
four-command completion is orchestration evidence only. A safely correlated
non-completed terminal may run the bounded read-only checkpoint to record
partial or unexpected effects, but it can never advance the batch. Submission
UNKNOWN, request mismatch, timeout, duplicate-submit evidence, or uncleared
active ownership stops before that checkpoint. This addendum does not authorize
Java, ChatClef/AltoClef, DTO, wire-payload, live Minecraft, commit, or push
changes. Passing the local offline suite closes these implementation gaps but
does not replace a final read-only merge review.

## 2026-08-19 Python Korean Command Orchestration V2 Addendum

This addendum records the docs-only status for the user-requested Korean
response, item-alias, craft-wording, and inventory-cleanup follow-up. It does
not rewrite the historical `4239c23` or `cfc170a` findings above.

Reviewed archive baseline:

```text
e08af63948a3fa4675c70279db59c2a70b00a332
```

Implemented at the reviewed archive:

```text
GET acquisition matcher
철 10개 캐줘 / 캐오기 / 캐와줘 family
prefixless GET compiler
single-pass submission and reconciliation
some equipment composition cases, for example 철바지, 철신발, 철헬멧
```

Not implemented at the reviewed archive:

```text
canonical Korean display resource
broad alias generator
emerald / torch aliases
갑바 / 레깅스 / 모자 component aliases
deterministic Korean response renderer
external response dispatch to Translate/TTS
transport result publisher
command orchestrator
reliable Python inventory snapshot provider
cleanup planner
automatic cleanup execution
```

Current working-tree follow-up after this addendum:

```text
emerald / torch aliases are source-present
갑바 / 레깅스 / 모자 equipment-component aliases are source-present
whole-phrase equipment composition enforcement is source-present
canonical Korean display-name resource is source-present for initial GET replies
deterministic Korean router response_text rendering is source-present
Python targeted cleanup policy helper is source-present
Python post-cleanup primary admission helper is source-present
reliable inventory snapshot provider, external response dispatch, and automatic
cleanup execution remain planned
```

Craft wording remains GET acquisition. The runtime has no separate registered
ChatClef craft command, so `만들어줘` and `제작해줘` must compile to prefixless
`get <target> <count>` when the target is supported.

Inventory cleanup is not approved as Java behavior. Automatic cleanup must be
Python-owned, targeted, one-attempt, and fail-closed. It must not use bare
`deposit`, placeholder quantities, retry, replay, or cleanup-completed alone as
permission to submit the primary command. The detailed contract is owned by:

```text
plugins/Minecraft/docs/chatclef-python-inventory-cleanup-preflight-contract.md
```

This addendum is a documentation-status record only. It does not authorize
code, tests, settings, Java, DTO, wire-payload, ChatClef/AltoClef behavior,
Minecraft launch, runtime reproduction, commit, or push changes.

## 2026-08-20 Korean Command Registry Addendum

This addendum records the docs-only status for the user request to make all
registered ChatClef commands available through Korean natural language and to
support all Minecraft item targets in Korean.

The current registered command surface is source-backed as 20 commands:

```text
attack
chatclef
deposit
equip
follow
food
gamer
gamma
get
give
goto
hero
idle
locate_structure
meat
overlay
reload_settings
resetmemory
scan
stop
```

The command registry must verify actual registration through `AltoClefCommands`
and `OverlayCommandRegistrar`. It must not count commented or unregistered
command classes such as `StashCommand`.

The status term `IMPLEMENTED` must not be used as a single end-to-end readiness
claim. Future status reviews must split at least these axes:

```text
SOURCE_REGISTERED
KOREAN_PARSE_COMPILE_READY
PYTHON_ADMISSION_READY
BRIDGE_LIFECYCLE_READY
GAMEPLAY_EFFECT_VERIFIABLE
PUBLIC_KOREAN_ENABLED
```

Current classification constraints:

```text
get, food, goto, meat:
  Korean parser/compiler present for the current supported scope; finite command
  path intended but live mutating gates still apply.

follow, idle:
  Korean parser/compiler present, but persistent lifecycle and cancellation
  contracts remain unresolved.

stop:
  Korean parser/compiler present for idle-state submission; active-command
  cancellation through the normal command-request lane is not proven.

attack, chatclef, deposit, equip, gamer, gamma, give, hero, locate_structure,
overlay, reload_settings, resetmemory, scan:
  no current public Korean parser/compiler contract.
```

Full item coverage also remains a classified-catalog goal, not an unconditional
alias-per-target claim. All 591 ChatClef catalog targets must be classified as
one of:

```text
DIRECT_ITEM
DIRECT_BLOCK
CANONICALIZED_LEGACY
GENERIC_GROUP
COMMAND_INTERNAL
AMBIGUOUS_POLICY
UNSUPPORTED
UNRESOLVED
```

The `잡템` wording remains Python junk-cleanup policy mode only. It is not a
flat item alias and must not compile directly to bare `deposit`. A future
cleanup execution path may submit only a safe targeted command such as
`deposit <observed_disposable_target> <observed_quantity>` after reliable
inventory evidence and post-cleanup verification are available.

This addendum is documentation-only. It does not authorize code, tests, build,
runtime execution, Java, DTO, wire-payload, ChatClef/AltoClef behavior, commit,
or push changes.
