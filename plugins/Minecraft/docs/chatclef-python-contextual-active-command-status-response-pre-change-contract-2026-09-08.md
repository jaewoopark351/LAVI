<!-- 20260908_kpopmodder: Defined the Python-only contextual active-command status-response follow-up before source changes. -->
<!-- 20260908_kpopmodder: Preserved STOP precedence, command barriers, lifecycle truth, and existing command/item admission. -->
<!-- 20260908_kpopmodder: Required responsibility-based classifier folderization while retaining compatibility facades. -->
<!-- 20260908_kpopmodder: Reconciled the implemented Python-only STATUS route, fail-closed publication custody, and offline verification state. -->

# ChatClef Python Contextual Active-Command Status Response Pre-Change Contract

Date: 2026-09-08

## 1. Document status

This document records the requested follow-up as the historical pre-change
contract and is reconciled below with the implemented production source.
The user-visible goal is that a natural progress question asked while LAVI is
performing a Minecraft command receives a description of that exact active
work instead of falling through to the generic new-command busy rejection.

```text
DOCUMENT_TYPE: PRE_CHANGE_IMPLEMENTATION_CONTRACT
STATUS: IMPLEMENTED_VERIFIED_OFFLINE_LIVE_NOT_RUN
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: 92ea9e6140395cbe71a734ecc3320b08a9c377d9
REVIEWED_WORKTREE_AT_START: CLEAN

BACKEND_SCOPE: FABRIC_CHATCLEF_PYTHON_ONLY
PRIMARY_CHANGE: CONTEXTUAL_NATURAL_STATUS_QUESTION_COVERAGE
CURRENT_RESPONSE_PROFILE_REGISTRY_SCOPE: ALL_CURRENT_26_PROFILES
QUERYABLE_ACTIVE_SCOPE: ORDINARY_COMMAND_LIFECYCLE_ONLY
CURRENT_QUERYABLE_ORDINARY_PROFILE_COUNT: 25
SPECIALIZED_TRUSTED_STOP_STATUS_QUERY: OUT_OF_SCOPE_TERMINAL_ONLY
ITEM_SCOPE: EVERY_TARGET_ALREADY_ACCEPTED_BY_ITS_EXISTING_INGRESS
CURRENT_TARGET_POLICY_ARTIFACT_COUNT: 591
CURRENT_ALIAS_POLICY_NON_UNSUPPORTED_COUNT: 585
CURRENT_ALIAS_POLICY_UNSUPPORTED_COUNT: 6
TARGET_POLICY_ARTIFACT_IS_RUNTIME_ALLOWLIST: NO
NEW_COMMAND_OR_ITEM_AUTHORITY: NONE
COMMAND_SUBMISSION_FROM_VALIDATED_STATUS_CANDIDATE: ZERO
STATUS_RESPONSE_LLM_RECALL_OR_PARAPHRASE: NONE
OUTER_CONVERSATIONAL_FALLTHROUGH_LLM: EXISTING_BEHAVIOR_ALLOWED
AUTOMATIC_RETRY_OR_RESUBMIT: NONE

CURRENT_GENERAL_STATUS_RENDERER: IMPLEMENTED_ALL_REGISTERED_PROFILES
CURRENT_ACTIVE_EVIDENCE_GATE: IMPLEMENTED_FAIL_CLOSED
CURRENT_STATUS_QUERY_CLASSIFIER: IMPLEMENTED_EXPANDED_CLOSED_PHRASE_SET
CURRENT_PREFIXLESS_GENERIC_STATUS_QUERY: CONTEXTUAL_EXACT_OWNER_CLAIM_OR_EXPLICIT_OUTER_BYPASS
CURRENT_STATUS_MARK_AI_ADDRESSEE_SUPPORT: IMPLEMENTED_LONGEST_MATCH
CURRENT_STATUS_FIFO_PERMIT_ACKNOWLEDGEMENT_MODEL: IMPLEMENTED
REQUESTED_STATUS_POST_PERMIT_CUSTODY_CLOSURE: IMPLEMENTED_FAIL_CLOSED
CURRENT_GENERIC_BUSY_TEXT: [Minecraft] 다른 마인크래프트 명령을 실행 중이라 지금은 새 명령을 보낼 수 없어요.
REQUESTED_STATUS_TEXT_EXAMPLE: 다이아 곡괭이 만드는 중이야
LOCAL_RUNTIME_EVIDENCE: LOCAL_IGNORED_RUNTIME_INPUT

JAVA_SOURCE_CHANGE: NONE
WIRE_SCHEMA_CHANGE: NONE
MINECRAFT_TASK_BEHAVIOR_CHANGE: NONE
GRADLE_BUILD_FOR_THIS_PYTHON_ONLY_FOLLOW_UP: NOT_REQUIRED
PRODUCTION_SOURCE_CHANGE_AFTER_THIS_PRE_CHANGE_BASELINE: IMPLEMENTED_PYTHON_ONLY
TEST_SOURCE_CHANGE_AFTER_THIS_PRE_CHANGE_BASELINE: IMPLEMENTED
READ_ONLY_CLASSIFIER_PROBE: ONE_CURRENT_FORM_MATCHED_FIVE_NATURAL_VARIANTS_MISSED
OFFLINE_TEST_EXECUTION: PASSED_WITH_EXACT_COUNTS_RECORDED_IN_SECTION_19
DEPLOYMENT: NOT_RUN
MINECRAFT_RUNTIME: NOT_RUN
COMMIT_OR_PUSH: NONE
```

The screenshot supplied with the request proves the user-visible fallback text
but does not preserve the exact triggering utterance. The bounded local log at
`logs/20260908_133240_log.txt` preserves the remaining route evidence on the
reviewed machine:

- lines 196-207 bind `get iron_axe 1` and accept
  `status=running`, `result_reason=dispatch_started`, and
  `evidence_sequence=1` for the same request;
- lines 217-220 show the next trusted LAVI Chat event bypassing STATUS and
  reaching `minecraft_command_busy` with no follow-up submission;
- lines 221-232 show the generic busy sentence delivered to UI/output and TTS
  once.

This file is ignored local runtime input and will not accompany a normal Git
checkout. The sanitized line summary above is the durable documentation claim;
it is not a promise that another checkout contains the source log. Privacy-
bounded logging intentionally omits that second event's raw utterance, so the
log does not prove which exact phrase was missed or whether the input was a
genuine second command. Source inspection nevertheless establishes the bounded
mechanism that can produce the reported symptom: an intended progress question
outside the current closed classifier can fall through to ordinary command
translation and then reach the busy precheck while another command is active.
The implementation locks the requested closed conversational forms with
characterization tests rather than inferring STATUS through an LLM.

## 2. Authority and relationship to existing documents

This follow-up owns conversational recognition and routing of an active-command
status question and the bounded STATUS post-permit publication-custody failure
closures required by that route. The existing FIFO permit and acknowledgement
model is already implemented. The raw-permit diagnostic-custody/wrapper/
snapshot interval and the later acknowledgement-bearing decision custody across
optional-result validation, trusted response processing/proof closure, and
dispatcher decision resolution were specified here and are now implemented and
verified offline. Existing
documents retain their other authorities:

- [General Natural Korean Command Lifecycle Feedback Implementation Record](chatclef-general-natural-korean-command-lifecycle-feedback-implementation-record-2026-09-07.md)
  owns the implemented 26-profile descriptor, lifecycle, rendering,
  presentation, and delivery system.
- [General Natural Korean Command Lifecycle Feedback Pre-Change Contract](chatclef-general-natural-korean-command-lifecycle-feedback-pre-change-contract-2026-09-07.md)
  remains the historical generalized design baseline.
- [Python Command Orchestration Plan](chatclef-python-command-orchestration-plan.md)
  owns command submission, active ownership, busy rejection, reconciliation,
  and no-replay behavior.
- [Python Korean Command Registry Plan](chatclef-python-korean-command-registry-plan.md)
  owns command names, sources, safety, confirmation, slots, and public exposure.
- [Python STOP Single-Response and STORE_HOME Verified-Terminal Contract](chatclef-python-stop-single-response-store-home-verified-terminal-pre-change-contract-2026-09-08.md)
  owns trusted STOP START suppression, STOP terminal publication, and the
  STORE_HOME success evidence profile.
- [Korean Test Strategy](chatclef-korean-test-strategy.md) owns the broader
  parser, routing, lifecycle, and runtime test policy.

This document narrowly supersedes the historical generalized contract's rule
that every unqualified prefixless generic or family-only status question must
fall through. The exception is limited to a trusted LAVI Chat or final
microphone input for which the current synchronized lifecycle state proves one
matching exact nonterminal ordinary command owner. With no matching owner, the
validated prefixless candidate takes the explicit outer-conversation
fallthrough and skips all remaining Minecraft owners. Addressed candidates
retain their bounded idle/cautious result. This is a routing-ownership
refinement, not broader Minecraft intent capture.

## 3. Source-reviewed current behavior

The relevant current ownership chain is:

```text
MinecraftInputRouteSequence
  -> STOP route
  -> CommandStatusRouteOwner
     -> CommandStatusQueryClassifier
     -> inspect_command_feedback_status(query)
     -> CommandFeedbackStatusCoordinator
     -> CommandLifecycleResponseRenderer.render_status(...)
  -> generic crafting/default routes
  -> ordinary command route
     -> submission precheck
     -> minecraft_command_busy
     -> TrustedKoreanCommandFeedbackRenderer.render_busy()
```

The ordering is already correct: STOP is checked first, STATUS is checked
before ordinary busy handling, and a successful STATUS route returns
`command_submitted=False`. No route-order redesign is required.

The current `CommandStatusQueryClassifier` recognizes only a narrow set:

- addressed generic forms such as `마크 지금 뭐 하고 있어?`;
- addressed family-only forms for item GET, GOTO, and STORE_HOME;
- target-qualified forms for GET, DEPOSIT, EQUIP, GIVE, and FOLLOW;
- an optional leading `마크` or `마인크래프트` only in the forms explicitly
  represented by its closed patterns.

It does not recognize the project's established `마크 AI` addressee. Because
the current addressee expression matches only `마크` followed immediately by
whitespace, `마크 AI 지금 뭐 하고 있어?` leaves `AI` in the body and fails
classification. The future closed addressee set must recognize `마크 AI` as a
single longest-match token before `마크`. New vocatives such as `마크야` or
`마인크래프트야` remain outside this contract until explicitly added to the
bounded alias matrix.

It deliberately returns no query for unaddressed generic or family-only forms.
It also lacks common conversational variants such as:

```text
지금 뭐 해?
뭐해?
뭐 하는 중이야?
지금 무슨 작업 중이야?
진행 상황 알려줘
지금 어떤 작업 하고 있어?
```

A read-only classifier probe at the reviewed HEAD produced this exact boolean
shape:

```text
마크 지금 뭐 하고 있어?       -> matched
마크 AI 지금 뭐 하고 있어?    -> not matched
마크 뭐해?                     -> not matched
마크 뭐하고 있어?             -> not matched
마크 지금 뭐 하는 중이야?     -> not matched
마크 진행 상황 알려줘          -> not matched
```

The renderer is not limited in the same way. The current phrase-profile
registry covers every registered command name, and
`KoreanCommandStatusRenderer` already renders typed item, quantity, player,
location, hunger, setting, structure, storage, persistent, query, registry,
generic, and control families. Existing tests also require a nonempty natural
START and STATUS phrase for every registered name.

The current positive `… 중이야` truth gate is intentionally strict. The
`CommandFeedbackStatusCoordinator` requires the matching active owner, a live
connection, no quarantine, a matching query/descriptor, accepted `running`
evidence, and a command profile that accepts the exact progress reason. Merely
being busy or holding an accepted request is not proof that Minecraft work has
started. This contract preserves that rule.

## 4. Required user-visible behavior

### 4.1 Generic progress question

When a trusted LAVI Chat or final microphone input asks what LAVI is doing and
one active command is authoritatively matched, the result is one natural STATUS
answer describing that command:

```text
active command: get diamond_pickaxe 1
trusted request semantics: craft
accepted running evidence: dispatch_started

user: 지금 뭐 해?
LAVI: 다이아 곡괭이 만드는 중이야
```

The newly admitted generic branch uses the following exact closed grammar after
normalization. `NOW` is either empty or the exact prefix `지금 `. `SUBJECT` is
exactly `무슨` or `어떤`. `ADDRESSEE` is empty or exactly one of `마크 ai `,
`마인크래프트 `, or `마크 ` and is removed by longest match in that order.
The full normalized candidate is `ADDRESSEE + NOW + BODY`; no other token,
suffix, or repeated prefix is implied.

| `BODY` family | Exact accepted normalized bodies |
| --- | --- |
| Short | `뭐 해`, `뭐해`, `뭐 해요`, `뭐해요` |
| Ongoing | `뭐 하고 있어`, `뭐하고 있어`, `뭐 하고 있어요`, `뭐하고 있어요` |
| In-progress | `뭐 하는 중이야`, `뭐 하는 중이에요` |
| Work + ongoing | `SUBJECT 작업 하고 있어`, `SUBJECT 작업을 하고 있어`, `SUBJECT 작업 하고 있어요`, `SUBJECT 작업을 하고 있어요` |
| Work + in-progress | `SUBJECT 작업 하는 중이야`, `SUBJECT 작업을 하는 중이야`, `SUBJECT 작업 하는 중이에요`, `SUBJECT 작업을 하는 중이에요` |
| Work + short in-progress | `SUBJECT 작업 중이야`, `SUBJECT 작업 중이에요` |
| Progress request | `진행 상황 알려줘`, `진행 상황 알려주세요` |

For example, `지금 뭐 해?`, `뭐 하는 중이에요`,
`어떤 작업을 하고 있어요?`, `지금 진행 상황 알려줘`, and
`마크 AI 지금 무슨 작업 중이야?` all map to the same generic query. English is lowercased by the
existing `KoreanTextNormalizer`, so raw `AI` and `ai` normalize to the same
longest-match addressee. Compact vocatives such as `마크AI`, `마크야`, and
`마인크래프트야` are not admitted by this contract.

Before the newly expanded generic matcher can claim a phrase, a dedicated raw-
shape validator must require `type(value) is str`, rejecting `str` subclasses,
and 1–128 characters measured after trimming surrounding U+0020 spaces but
before NFKC. It must reject every Unicode control, format, line-separator, or
paragraph-separator character. Only ordinary U+0020 spaces may separate words;
all other whitespace—including every other Unicode `Zs` space separator—is
rejected.
Repeated and surrounding ordinary spaces are allowed and then collapsed by the
existing normalizer. A run of one or more soft punctuation characters from
`[,.!?，。！？]` is allowed only at the end of the trimmed input. Internal soft
punctuation, quotes, semicolons, command separators, newlines, tabs, and mixed
command/status text are rejected before normalization can erase punctuation.
NFKC, English lowercasing, repeated-space collapse, optional terminal soft
punctuation, and final-microphone no-punctuation input therefore converge on
one of the exact forms above without widening the grammar.

This new raw-shape gate applies to newly admitted generic forms and addressee
expansion. Existing family-only and target-qualified forms retain their
characterized normalization and match behavior. Inputs such as
`뭐 해 그리고 철 10개 구해줘`, `"뭐 해"`, `뭐; 철 캐줘`, `뭐\n해`,
`철 10개 구해줘`, and `멈춰줘` must not become generic STATUS queries.

### 4.2 Family-specific progress question

A prefixless family question may be answered only when it agrees with the
active descriptor. An addressed family question is the explicit exception: on
mismatch it may answer with the actual proven active descriptor. Examples:

```text
active GET/craft + "뭐 만드는 중이야?"
  -> 다이아 곡괭이 만드는 중이야

active GOTO + "어디로 가는 중이야?"
  -> 좌표 120, 64, -30으로 가는 중이야

active STORE_HOME + "집에 정리하고 있어?"
  -> 아이템을 집에 정리하는 중이야
```

An addressed mismatch may answer with the actual proven work without submitting
anything. It reuses the current active renderer and does not add a contrastive
preface:

```text
active command: goto 120 64 -30
user: 마크 뭐 만드는 중이야?
LAVI: 좌표 120, 64, -30으로 가는 중이야
```

An unaddressed mismatch must not capture ordinary conversation. It returns an
explicit outer-conversation fallthrough decision: zero STATUS output, all later
Minecraft route owners skipped, and zero total Minecraft submissions for that
input. The existing outer LLM conversation may then answer normally.

### 4.3 Target-qualified progress question

A target-qualified question may state an item, quantity, or player only when
those values match the immutable active descriptor after the existing bounded
resolution rules:

```text
다이아 곡괭이 만들고 있어?
철 10개 구하는 중이야?
Steve 따라가는 중이야?
참나무 원목 2개 건네는 중이야?
```

An incorrect target, or a target phrase that the existing input authority did
not resolve and accept, must never be rewritten into the active target and
treated as if it matched. Addressed mismatch wording may state the actual
active work; an unaddressed mismatch takes the same explicit outer-conversation
fallthrough and skips every later Minecraft owner. New target-query resolution
for coordinates, destinations, structures, or settings is outside this follow-
up. A generic progress question still renders those exact active descriptor
values through the existing all-profile renderer.

### 4.4 Genuine new command while busy

An imperative request for a different Minecraft action is not a STATUS
question. It remains subject to the existing command barrier and must not
preempt, queue, retry, or resubmit:

```text
active command: get diamond_pickaxe 1
user: 철 10개 구해줘
  -> minecraft_command_busy
  -> new command submissions: 0
```

Changing genuine busy-rejection wording to include the active task is a
separate, independently reversible policy. It is not required by this
status-question contract. The screenshot's generic busy sentence is a defect
only when the triggering input was a progress question that should have been
owned by STATUS.

## 5. Meaning of “every command and every item”

“Every command” in the user-visible progress-query promise means every ordinary
command-lifecycle profile in the current source-backed registry: 25 at the
reviewed HEAD. Registry and direct-renderer drift coverage still spans all 26
profiles, including `stop`; the specialized trusted STOP lifecycle remains
terminal-only and is not a queryable active STATUS owner. The promise does not
mean every imaginable Minecraft action, an unregistered command, a native-only
command, or a future command lacking a profile.

“Every item” means every item target already accepted by the relevant existing
command and source policy. The reviewed
`intent/resources/chatclef_item_command_target_policy.json` artifact contains
591 canonical keys: 585 are classified as non-`UNSUPPORTED`, and 6 are
classified as `UNSUPPORTED`. Those classifications are alias/display metadata,
not a runtime allowlist or denylist. In particular, `UNSUPPORTED` means that the
artifact exposes no public Korean alias; it does not prohibit an existing
direct-canonical or raw ingress from accepting that target. Current direct
canonical Korean input may accept all 591 catalog targets, including those six,
while source-authorized raw forms such as dynamic GIVE, SCAN, or bare DEPOSIT
may preserve targets outside the 591-key artifact. Conversely, a missing or
invalid public Korean alias is still rejected by the existing natural-language
ingress. This follow-up must not reinterpret any artifact classification as an
execution decision.

Korean alias resolution remains owned by `ChatClefKoreanAliasRepository`
(currently 564 entries from `korean_item_aliases.json`, plus the separate
`korean_material_aliases.json` and `korean_equipment_aliases.json`
vocabularies) and `KoreanItemPhraseResolver`. The target-policy artifact does
not contain or own those aliases. At command admission, the existing descriptor
builder freezes the accepted canonical `target_item` and a bounded
`spoken_target_label`, using that resolver and the existing display-name
repository. STATUS rendering and target matching reuse those frozen values;
they do not rerun admission, add an alias, canonicalize a different synonym,
loosen target validation, or grant execution authority.

The current response-profile renderer coverage is shown below. The registered
STOP row is retained for registry completeness, but its specialized lifecycle
is not queryable through this follow-up:

| Command/family | Natural running response shape |
| --- | --- |
| `get` craft | `<item/count> 만드는 중이야` |
| `get` acquire or unknown route | `<item/count> 구하는 중이야` |
| `deposit`, `deposit_all` | `<item/count> 보관하는 중이야` or `아이템을 보관함에 넣는 중이야` |
| `equip` | `<item> 장착하는 중이야` |
| `give` | `<player>에게 <item/count> 건네는 중이야` when a validated player is frozen; otherwise `<item/count> 건네는 중이야` |
| `goto` | `<validated destination>으로/로 가는 중이야` |
| `follow` | `<player>을/를 따라가는 중이야` |
| `food` | `허기를 <amount>만큼 채울 음식을 모으는 중이야` |
| `meat` | `허기를 <amount>만큼 채울 고기를 모으는 중이야` |
| `store_home` | `아이템을 집에 정리하는 중이야` |
| `attack` | `대상을 공격하는 중이야` |
| `locate_structure` | `<structure>을/를 찾는 중이야` |
| `scan` | `요청한 대상을 찾는 중이야` |
| `auto_deposit_trust` | `자동 보관 대상을 등록하는 중이야` |
| `자동보관등록` | `주변 보관함을 등록하는 중이야` |
| `auto_deposit_trusted_list` | `자동 보관 위치를 확인하는 중이야` |
| `auto_deposit_untrust` | `자동 보관 등록을 해제하는 중이야` |
| `gamma` | `밝기를 <value>으로/로 바꾸는 중이야` |
| `overlay` | `오버레이 상태를 바꾸는 중이야` |
| `chatclef` | `ChatClef 상태를 바꾸는 중이야` |
| `reload_settings` | `설정을 다시 불러오는 중이야` |
| `resetmemory` | `ChatClef의 기억을 초기화하는 중이야` |
| `idle` | `가만히 기다리는 중이야` |
| `hero` | `주변 적을 정리하는 중이야` |
| `gamer` | `게임을 공략하는 중이야` |
| `stop` | `중지 요청을 처리 중이야` for direct-renderer compatibility only; trusted or raw/legacy STOP is never exposed through the ordinary STATUS query path in this follow-up. |

This table is response coverage, not proof that every command remains active
long enough to be queried. An asynchronous-immediate command may finish before
the question arrives. In that case the status path must not fabricate a running
window.

A command-name-only descriptor, including an admitted raw form without typed
slots, may use only its bounded family/profile fallback and must not invent a
slot:

```text
요청한 아이템 구하는 중이야
요청한 위치로 가는 중이야
```

It must not invent an item, count, player, coordinate, structure, or setting.

## 6. Contextual prefixless ownership

The implementation must separate route trust, lexical candidacy, semantic
ownership, and wording evidence. Recognizing Korean words is not permission to
capture the input. The route owner validates the current input proof; the
transport claim evaluator never receives or interprets a raw event or proof.

```text
phase 1: closed lexical candidate at the route boundary
  -> generic, family, or target-qualified query
  -> addressed flag preserved
  -> no command submission

route trust gate
  -> validate the candidate against the current event's trusted-source proof
  -> pass only the validated query into lifecycle inspection

phase 2: synchronized contextual claim under the existing command lock
  -> context is nonterminal and uses an ordinary lifecycle profile
  -> command_name != stop and response_lifecycle_kind != specialized_control
  -> current active command is the context's exact owner token
  -> prefixless query only: family/target/count constraints match
  -> claim STATUS once

phase 3: wording state from the same synchronized inspection
  -> proven running, accepted/pending, idle, or cautious unavailable
  -> freeze the existing immutable lifecycle snapshot and descriptor
```

For an addressed generic question, the status route may own the result even
when no tracked LAVI command exists and answer the existing bounded idle
sentence. For an unaddressed generic or family-only question, ownership is
allowed only when a nonterminal ordinary context with the exact active owner
disambiguates the question. Its stated family/target/count constraints must
also match. With no active context, a terminal-claimed context, a specialized
STOP context, a stale owner, or mismatch, the validated prefixless candidate
falls through.
For a validated closed STATUS candidate, `fallthrough` here always means a non-
`None` unhandled outer-conversation decision returned immediately from
`MinecraftInputRouteSequence`; it never means continuing to the later Minecraft
default, intent-gate, translation, busy, or submission owners. An invalid
source proof never enters contextual claim and remains governed by the existing
trusted-ingress/source policy; this follow-up grants it no new behavior.

Connection, quarantine, status, reason, and reconciliation decide the wording
state after contextual ownership is established; they do not turn an eligible
prefixless query back into ordinary conversation. Thus an exact ordinary owner
that is disconnected or quarantined receives cautious wording, never a false
running sentence. Addressed questions retain their existing ability to produce
idle or cautious wording without a prefixless contextual claim.

The closed query taxonomy is:

| Query kind | Example | Claim rule |
| --- | --- | --- |
| Addressed generic | `마크 AI 지금 뭐 해?` | May claim; render matched active, bounded idle, or cautious unavailable state. |
| Prefixless generic | `지금 뭐 해?` | Claim only with one exact nonterminal ordinary LAVI Minecraft owner. |
| Addressed family | `마크 뭐 만드는 중이야?` | May claim; a mismatch reports actual proven work without submission. |
| Prefixless family | `뭐 만드는 중이야?` | Claim only when active family matches. |
| Addressed target | `마크 철 10개 구하는 중이야?` | May claim; mismatch remains an internal read-only match fact, while the response states only the actual proven work. |
| Prefixless target | `철 10개 구하는 중이야?` | Claim only when the currently supported family and complete target match. |
| Imperative command | `철 10개 구해줘` | Never STATUS; ordinary admission/busy path owns it. |
| STOP command | `멈춰줘` | STOP owner runs first; never reclaimed as STATUS. |

The classifier remains deterministic and must not ask an LLM whether a phrase
is a status question. The newly admitted generic/addressee branch must reject
chaining, embedded commands, control characters, quotations that change
meaning, and ambiguous mixed imperative forms according to Section 4.1's raw-
shape gate. This follow-up does not retroactively tighten the current
normalization contract for existing family-only and target-qualified forms;
their positive and negative behavior is characterization-locked. A global raw-
shape hardening of those existing forms would be a separate behavior change.
The required lexical expansion is the generic progress-question family that
unlocks every ordinary active profile. Existing GET, GOTO, STORE_HOME, DEPOSIT,
EQUIP, GIVE, and FOLLOW family/target forms remain supported. Adding a new
family-specific question grammar for every rendered profile is not required by
this follow-up and would need its own closed positive/negative matrix.

## 7. Authoritative progress evidence

A natural running sentence reuses the existing immutable
`CommandFeedbackLifecycleSnapshot` and frozen command descriptor built from the
same synchronized state read. The pure claim evaluation may add only typed
eligibility/match flags; it is not a second mutable lifecycle owner:

```text
command_name
phrase_profile_id
requested_family
detail_level
validated renderable slots only
lifecycle state
latest status
result reason
query match result
```

The snapshot is read-only and immutable. Session, generation, request, and
message correlation has already been validated when the existing context and
result state are bound or reconciled. The query-time synchronized inspection
then validates the exact active owner token and consumes only that reconciled
state. Only the typed claim result, existing frozen descriptor, and render-safe
snapshot leave the lock. Rendering must not retain a live owner or lifecycle
object that can change afterward. No new projection file is required solely to
duplicate fields the existing frozen descriptor and snapshot already own.

Contextual ownership and natural-running wording are separate gates. A
prefixless query may claim one STATUS publication permit only when:

1. the route owner already validated the current input's trusted-source proof;
2. the lifecycle context has not been terminal-claimed and is not the `stop`
   / `specialized_control` lifecycle;
3. the active command object is the context's exact owner token;
4. for a prefixless constrained query, family, target, and any stated quantity
   agree; an addressed mismatch may instead render the exact proven active
   descriptor without claiming that the requested family matched;
5. the context/session/generation/request/message identity and existing
   reconciliation invariants remain valid; and
6. the query wins one STATUS publication permit.

After that contextual claim, the same synchronized snapshot chooses wording in
closed precedence order. Disconnected, quarantined, contradictory, or
evidence-unavailable state is cautious. Otherwise an accepted command without
proven start is pending. Natural `… 중이야` wording is allowed only when the
latest accepted result is `status=running`, the command evidence profile accepts
the exact progress reason, and every applicable evidence invariant is valid.
Running, pending, and cautious responses all use the same existing FIFO STATUS
permit and acknowledgement path; wording classification never issues a second
permit. An addressed idle response with no tracked context has no lifecycle
permit because there is no command publication to order.

Busy ownership by itself is insufficient for natural-running wording. The
implementation must not turn `accepted`, a socket send, an allocated request
ID, a stale active pointer, generic Java `running`, callback-only completion,
or quiescence observation into `… 중이야` unless the existing profile accepts
that evidence.

The state-to-language contract remains:

| State | Response |
| --- | --- |
| Proven running | Exact natural active work, such as `다이아 곡괭이 만드는 중이야` |
| Accepted/pending but start not proven | `<command/subject> 작업이 시작됐는지 확인 중이야` |
| Addressed query and no tracked LAVI owner | `지금 내가 처리 중인 마인크래프트 명령은 없어` |
| Exact ordinary context claimed while disconnected, quarantined, contradictory, or evidence-unavailable | `지금 마인크래프트 작업 상태를 확인하지 못했어` or the narrower existing family form |
| Prefixless query with no matching owner | Explicit outer-conversation fallthrough; skip remaining Minecraft owners and submit zero commands. |

The linearization point is the existing locked lifecycle inspection and STATUS
publication-permit issuance. Connection, quarantine, owner, lifecycle status,
and evidence are sampled there and frozen into the returned result. A disconnect
or terminal event after permit issuance does not retroactively rewrite that
snapshot; the existing FIFO publication/terminal ordering decides which already-
committed fact is delivered first.

## 8. STOP, terminal, and race ordering

STOP remains a specialized control lane and must stay before STATUS
classification. `멈춰줘`, its guarded stop-like variants, and malformed STOP
candidates must not be reinterpreted by the broader conversational matcher.

The ordinary command lifecycle and specialized STOP lifecycle remain separate.
This follow-up does not expose an in-flight trusted STOP through STATUS. A STOP-
like input remains owned by the STOP route, its accepted START stays suppressed,
and its terminal response remains `멈췄어` once. Adding a read-only STOP
projection would be a separate implementation contract. This follow-up must not
change:

- STOP tracker ownership;
- command barrier state;
- target correlation;
- STOP terminal validator;
- terminal CAS;
- accepted START suppression;
- the one visible/spoken `멈췄어` terminal result.

A STATUS/TERMINAL race has one of two valid outcomes:

```text
STATUS permit committed first
  -> one status response
  -> later one terminal response

terminal claim committed first
  -> no new running-status claim
  -> one terminal response
```

It must never produce a status response after the terminal state was consumed,
duplicate the terminal, reopen a retired command, or hold the Gradio submit
open.

There is also one fail-closed post-permit outcome: if bounded diagnostic-custody
creation, acknowledgement-wrapper creation, or immutable snapshot attachment
fails after the STATUS permit was issued, the server API consumes that raw
permit as `published=False` exactly
once. No STATUS response follows; any terminal staged behind the permit is
released once through the existing terminal-delivery path. A permit must never
remain pending merely because its wrapper could not be handed to the route.

## 9. Responsibility and folder contract

The implementation has multiple responsibilities and must remain split by
owner. Existing stable facades remain compatible; new phrase coverage must not
turn one classifier into a combined parser, state reader, router, and renderer.

```text
plugins/Minecraft/fabric/chatclef/input/status/command_lifecycle/
  __init__.py
  classification/
    __init__.py
    command_status_query.py
    command_status_query_classifier.py          # compatibility facade only
    validation/
      __init__.py
      command_status_question_input_validator.py # new generic raw-shape gate
    addressing/
      __init__.py
      command_status_addressee_parser.py         # normalized longest match
    generic/
      __init__.py
      generic_command_status_question_matcher.py
    family/
      __init__.py
      family_command_status_question_matcher.py
    target/
      __init__.py
      target_command_status_question_matcher.py
  routing/
    __init__.py
    command_status_route_owner.py                # route orchestration only
    decision/
      __init__.py
      command_status_route_decision_factory.py   # decision construction only
      command_status_emergency_decision.py       # dedicated slotted sentinel type/value
  diagnostics/
    __init__.py
    command_status_route_failure_diagnostics.py  # bounded fields only
    command_status_publication_failure_adapter.py # generic-dispatch observer adapter
  publication/
    __init__.py
    command_status_publication_custody_policy.py # one STATUS-only predicate

plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/status/
  command_feedback_status_coordinator.py         # synchronized state inspection
  command_status_target_resolver.py              # compatibility facade
  claim/
    __init__.py
    contextual_command_status_claim_evaluator.py
    contextual_command_status_claim_evaluation.py # frozen eligibility result
  publication/
    __init__.py
    command_status_publication_handoff.py         # attach or clean raw permit
    command_status_publication_handoff_failure.py # typed post-permit signal
  diagnostics/
    __init__.py
    command_status_failure_diagnostic_custody.py # post-permit first-failure/once gate
    command_status_failure_diagnostic_custody_factory.py # frozen snapshot projection

plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/
  command_feedback_server_api.py                  # opaque factory invocation/handoff

plugins/Minecraft/fabric/chatclef/input/routing/trusted_korean/publication/status/
  __init__.py
  command_status_publication_custody_guard.py     # hold ack through trusted pipeline
  command_status_publication_pipeline_failure.py # typed closed-stage outcome

plugins/Minecraft/fabric/chatclef/input/routing/composition/
  minecraft_chatclef_router_component_graph.py   # assemble STATUS custody ports

plugins/Minecraft/fabric/chatclef/input/
  minecraft_chatclef_input_router.py              # expose generic custody ports

plugins/Minecraft/fabric/chatclef/input/routing/orchestration/invocation/publication/
  __init__.py
  acknowledged_optional_route_result_guard.py    # protect raw decision validation

llm_core/input_routing/publication/
  routed_input_publication_custody_guard.py       # injected-policy dispatcher guard
  routed_input_publication_custody_failure.py     # backend-neutral stage/class

plugins/Minecraft/fabric/chatclef/transport/command_feedback/lifecycle/state/
  command_feedback_lifecycle_snapshot.py          # existing immutable renderer snapshot

plugins/Minecraft/fabric/chatclef/transport/server/runtime/
  fabric_chatclef_server_component_graph.py       # inject transport-local custody factory

plugins/Minecraft/fabric/chatclef/response/command_lifecycle/
  grammar/boundary/
    korean_command_status_renderer.py             # stable facade
```

The exact filenames may be adjusted to repository naming conventions during
implementation, but the ownership split is mandatory:

| Responsibility | Owner | Must not own |
| --- | --- | --- |
| Validate newly admitted raw generic shapes | Input validator | Semantic matching, state, rendering |
| Normalize accepted text | Existing `KoreanTextNormalizer` | Claim, routing, rendering |
| Remove one normalized longest-match addressee | Addressee parser | Source trust, state, rendering |
| Identify prevalidated normalized generic shapes | Generic matcher | Active state, rendering, submission |
| Identify family-only question shapes | Family matcher | Descriptor matching, lifecycle truth |
| Identify target-qualified question shapes | Target matcher | Item admission, command execution |
| Decide contextual claim from validated values | Claim evaluator | Raw event/proof, mutable lifecycle state, output/TTS |
| Read synchronized lifecycle truth | Existing status coordinator | Phrase parsing, UI behavior |
| Freeze render-safe state | Existing lifecycle snapshot plus frozen descriptor | Live owner/correlation objects, later state reads |
| Render a proven snapshot | Existing status renderer facade | State reads, command submission |
| Assemble handled, cautious, or conversational-bypass decisions | Status decision factory | Parsing, lifecycle reads, LLM invocation |
| Own the prebuilt deeply immutable handled/suppressed emergency sentinel | Dedicated slotted status-emergency decision module | Runtime callbacks, lifecycle state, logging |
| Attach acknowledgement to a snapshot or clean a raw issued permit | Status publication handoff | Phrase parsing, rendering, terminal policy |
| Decide whether a decision is protected by this follow-up | Shared Minecraft STATUS custody policy | Failure conversion, acknowledgement calls, generic dispatch |
| Preserve the first bounded post-permit failure and cap its diagnostic to one attempt for the current input | Transport-local STATUS diagnostic custody | Global registries, raw text, exception objects, route behavior |
| Retain a protected STATUS decision through trusted feedback, authorization, and proof close | STATUS publication custody guard | Rendering policy, proof issuance, dispatcher delivery |
| Protect a protected optional-route result while its type is validated | Optional-route result guard | Route behavior, response rendering, terminal policy |
| Resolve a protected decision fail-closed if dispatcher decision resolution raises | Routed-input publication custody guard | Minecraft semantics, lifecycle truth, sink behavior |
| Convert backend-neutral dispatcher failure observation into one bounded STATUS diagnostic | Minecraft STATUS publication-failure adapter | Retaining decisions, raw input, generic dispatch behavior |
| Publish the current-input response | Existing route/publication boundary | Java execution, retry |
| Record one redacted STATUS failure fact under the shared closed schema | Stage-local pre-claim diagnostics or post-permit diagnostic custody | Raw text, exception messages, behavior decisions |

The existing `CommandFeedbackServerApi` remains the facade and command-lock
owner. Its new transport publication-handoff collaborator owns only the first
interval after `inspect_command_feedback_status_for_publication()` has returned
a raw permit and before the acknowledgement-bearing immutable snapshot has been
returned. On success it creates bounded diagnostic custody and attaches the
acknowledgement exactly as today. On custody-factory, wrapper, or snapshot-
attachment failure it resolves the raw permit as
`published=False` through the existing locked publication lifecycle, forwards
any released terminal through the existing terminal-delivery owner, and emits
only the typed handoff-failure signal. The route owner selects—but does not
construct—the decision module's already-created emergency sentinel for that
signal.

The next custody boundary begins immediately after an optional STATUS owner
returns a raw decision, before `MinecraftOptionalRouteOwnerResultValidator`
runs. One Minecraft-owned shared custody policy is used at this boundary, at
every trusted-pipeline boundary below, and at dispatcher composition. Its
non-throwing `matches(decision: object) -> bool` returns true only when all of
these facts hold: the decision is an `isinstance` of
`MinecraftChatClefInputRouteDecision` (matching
the current optional-result validator), `decision.handled is True`,
`decision.route_kind` is exactly `command_status_query` or
`crafting_status_query`, the acknowledgement has the exact
`CommandFeedbackPublicationAcknowledgement` type, its `acknowledge` attribute
is callable, and its `kind` equals
`CommandFeedbackPublicationPermit.STATUS` (`status`). The existing
`CommandFeedbackReadyDecisionAcknowledgement` is produced only by the START
decorator in the reviewed source and is always policy-negative in this
follow-up.
`response_kind` is deliberately not a predicate because the compatible legacy
crafting route currently uses its default `immediate` value. An admitted route-
decision subclass with an exact STATUS acknowledgement remains protected, as
the existing validator already accepts it. A STATUS-like mapping, duck-typed/
lookalike acknowledgement, wrong-kind acknowledgement, START acknowledgement,
unacknowledged decision, malformed object, and every other route return false
without raising.

For a policy-positive raw decision, the optional-route result guard captures the
original decision and acknowledgement by identity before validation. If
validation raises, or if its nominally successful return `is not` that same raw
decision carrying that same acknowledgement object, the guard invokes the
captured original acknowledgement once with `published=False` and returns the
prebuilt emergency sentinel. The latter case is the closed
`optional_route_result_acknowledgement_identity` stage. Custody transfers to the
trusted layer only with the same acknowledgement identity. Both generalized
and legacy STATUS routes receive this same failure-only protection through all
later custody boundaries, not only optional-result validation. Including the
legacy crafting route changes no accepted phrase or success response; it only
prevents the same failure-only permit leak. A policy-negative result follows
the existing validator, failure converter, and generic logger behavior without
a STATUS sentinel, STATUS diagnostic, or new acknowledgement call.

The one exception to sending a policy-negative object into the existing result
validator is identity with the module-owned emergency sentinel. The optional-
result guard returns that sentinel unchanged before
`MinecraftOptionalRouteOwnerResultValidator` runs. This narrow pass-through is
required because the sentinel's dedicated slotted type is intentionally not a
subclass of the current non-slotted route-decision type; no other new result
type is admitted.

After successful optional-result validation, the policy-gated trusted-pipeline
custody guard retains the original acknowledgement object while the existing
trusted feedback renderer, response-capability authorizer, feature-admission
finalization, and proof lifecycle closer run. The coordinator—not the guard—
must invoke the existing proof closer exactly once in one nested `finally`; the
guard must never retry it. Custody is relinquished only when all stages complete
and the final decision still passes the shared policy, carries the same
acknowledgement object by identity, keeps `handled is True`, and preserves the
original exact `route_kind` and `response_kind`. A missing or replaced
acknowledgement is `publication_acknowledgement_identity`; a changed decision
type, handled flag, route identity, or response identity is
`publication_route_identity`. Both are fail-closed non-exception failures.

On a trusted-pipeline or identity failure, the guard invokes the original
acknowledgement once with `published=False`, records one typed pipeline-failure
outcome, and selects the same prebuilt emergency sentinel without allowing the
exception to reach the generic router-failure logger. Any terminal release is
performed only by the existing acknowledgement/lifecycle callback, never by the
guard. A capability created before a later authorization-stage exception must
not be attached to or exposed by the returned sentinel, and no publisher may
receive it on that failed path. Revoking a capability independently retained by
an injected proof implementation would require a new input-core API and is not
claimed here. An acknowledgement call that returns false or raises is not
retried; Section 12 records the narrower guarantee for that synthetic boundary.

After the trusted route returns successfully, the dispatcher becomes the
existing acknowledgement owner. A small generic routed-input custody guard must
cover both calls to `RoutedInputDecisionResolver.resolve()`, the two currently
uncontained dispatcher stages. The guard is inactive unless its injected
`claim_predicate(decision)` callable returns true. Production Minecraft
composition must inject the one shared policy above. If either resolver raises
for a policy-positive decision, the dispatcher invokes its acknowledgement once
with `published=False` and returns its existing suppressed outcome. Existing
wait, ready-resolution, external-publication, and acknowledgement helpers retain
their current containment and are not duplicated. The constructor defaults are
a false predicate and no-op observer, so an acknowledgement-free decision,
START acknowledgement, non-STATUS acknowledgement, or other backend keeps its
existing resolver-exception behavior.

The `llm_core` guard remains backend-neutral: it must not import Minecraft,
STATUS classifiers, lifecycle state, terminal delivery, route literals, permit
kinds, or the STATUS sentinel. It owns only injected callables named
`claim_predicate(decision) -> bool` and
`failure_observer(decision, *, stage, exception_class) -> None`, with false/no-
op defaults. On a protected failure it passes only the
captured original decision plus the already-bounded stage and exception-class
name to that observer, then returns the existing generic suppressed dispatch
outcome. The guard does not inspect or retain Minecraft fields.

`MinecraftChatClefRouterComponentGraph` must assemble the shared predicate and
publication-failure adapter, and `MinecraftChatClefInputRouter` must expose them
through the backend-neutral ports
`claims_routed_input_publication_custody(decision)` and
`observe_routed_input_publication_failure(decision, *, stage, exception_class)`.
Both the `RoutedInputDispatchCoordinator(router=...)`
constructor path and every later `set_router()` call must bind these ports. A
router replacement or `None` clears both old callables to their defaults so a
stale Minecraft policy cannot affect another router. This is reached through
the existing `MinecraftInputRouterWiring -> llm.set_input_router(router)` seam;
no app-core Minecraft import or second wiring API is added.

The Minecraft adapter immediately reapplies the shared policy, obtains the
call-local bounded diagnostic custody from the acknowledgement, records at most
the first failure, and returns without retaining the decision. A missing or
throwing observer cannot disable policy-positive acknowledgement cleanup; the
dispatcher attempts the captured original acknowledgement false once first,
calls the observer from `finally`, and then returns suppressed. An adapter or
diagnostic-sink exception is swallowed and cannot alter acknowledgement or
routing. This mandatory binding preserves the dependency direction while
making both dispatcher stages observable as STATUS-owned failures.

Pre-claim validation/parsing failures end their route and are recorded once from
the bounded typed values known at that exact local boundary. They do not use a
premature frozen projection that would incorrectly turn later-known query or
lifecycle fields into `none`. After the one synchronized inspection has returned
its frozen snapshot and raw STATUS permit, the existing server API's new
publication-handoff collaborator invokes an injected, side-effect-free
`status_publication_failure_diagnostic_custody_factory(query, snapshot)` before
it creates an acknowledgement or attaches it to the snapshot. The factory
receives no raw event or live lifecycle owner and returns one call-local
`CommandStatusFailureDiagnosticCustody` from the complete bounded query/snapshot
projection. There is no second state read and no callback executes inside the
command lock.

`FabricChatClefServerComponentGraph`, which already owns the Fabric server API
construction and its bounded diagnostics dependency, constructs the transport-
local `CommandStatusFailureDiagnosticCustodyFactory` and passes
`status_publication_failure_diagnostic_custody_factory=factory.create` to
`CommandFeedbackServerApi`. The server API stores and invokes only that opaque
callable and does not import its concrete type. Generalized and legacy crafting
STATUS inspection already converge on this same API, so no router-to-extension-
to-adapter-to-server setter or per-call signature propagation is added.

The custody exposes one synchronized `record_once(stage, exception_class)`
operation. Its exterior is slotted/frozen; its private one-shot cell is claimed
before calling the sink, so a sink exception cannot enable a second record. If
the injected factory raises, the handoff treats it as
`publication_handoff_diagnostic_custody`, resolves the already-issued raw permit
false exactly once through the existing locked lifecycle, and returns the same
typed handoff-failure signal used for wrapper/snapshot failures. Production
server composition must inject the factory; the server API constructor default
remains `None` as a compatibility no-op for existing direct construction and
does not change acknowledgement behavior.

The raw-handoff owner keeps the new custody reference until an acknowledgement
exists. `CommandFeedbackPublicationAcknowledgement` gains one optional opaque
constructor argument and read-only property named
`publication_failure_diagnostic_custody`; transport stores but never imports or
interprets its type. STATUS wrapper creation receives the same custody object;
START and other acknowledgement creation leaves the property `None`.
The optional, trusted, and dispatcher adapters recover only this exact property
from the captured original acknowledgement. No ready-wrapper change, global
registry, acknowledgement-ID map, decision-ID map, or presentation-detail-log
parsing is allowed. This is how a renderer failure followed by an authorizer
failure, or finalization followed by proof-close failure, remains one diagnostic
whose stage/class are those of the first failure.

Neither typed failure nor the sentinel retains a raw permit, acknowledgement,
snapshot, terminal, capability, proof, event, or exception object. A typed
failure may carry only its closed stage and bounded exception class name for the
one redacted diagnostic.

The emergency sentinel is one module-owned value of a dedicated
`@dataclass(frozen=True, slots=True)` status-emergency decision type, rather
than a shared instance of the current non-slotted
`MinecraftChatClefInputRouteDecision`. Its empty mappings are read-only mapping
proxies, it has no writable `__dict__`, and `dataclasses.replace()` used by the
existing trusted renderer/authorizer preserves the dedicated slotted type. It
has this exact shape:

```text
handled=true
reason=command_status_publication_fail_closed
response_text=""
result=<immutable empty mapping>
translation=<immutable empty mapping>
publish_external_response=false
response_source=minecraft_chatclef
response_emission_capability=None
suppress_response=true
route_kind=command_status_query
response_kind=command_status
response_publication_acknowledgement=None
presentation_detail_log=""
```

Its nested empty mappings must reject mutation; dataclass-level `frozen=True`
alone is insufficient. `TrustedKoreanResponseAuthorizer` may create a replaced
suppressed decision from it, but that operation must preserve every value above
and must not mutate the shared sentinel.

If implementation requires changing status rendering responsibilities rather
than only reusing the current renderer, split active, pending, and unavailable
state rendering below a compatibility `KoreanCommandStatusRenderer` facade.
Do not move or broadly refactor an untouched renderer merely to satisfy a file
count. The mandatory split applies to responsibilities actually expanded by
this follow-up.

Do not add a generic `utils`, `manager`, `common`, or mutable global registry.
Use frozen values or immutable projections and preserve one synchronized
lifecycle owner.

## 10. Route and delivery contract

The route order and explicit STATUS-candidate branch are:

```text
normalized LAVI input event, with raw text retained
  -> STOP classification/control lane
  -> STATUS lexical validation/classification
     -> explicit non-candidate
        -> continue to existing specialized/default routes
     -> validator/classifier exception before candidate certainty
        -> validate the current-event proof only for failure ownership
           -> invalid or failed proof: no new STATUS ownership
           -> valid proof: one handled fixed cautious decision; no permit
     -> lexical STATUS candidate
        -> validate candidate against the current-event proof
           -> invalid or failed proof
              -> no new STATUS ownership; preserve existing source-policy routing
           -> valid proof + addressed candidate
              -> safe state/claim result
                 -> no permit: one handled idle or cautious decision
                 -> claimed raw permit: enter post-permit handoff below
              -> pre-claim state/evaluator exception
                 -> one handled fixed cautious decision
           -> valid proof + prefixless candidate
              -> contextual claim absent
                 -> one non-None unhandled conversation-fallthrough decision
              -> pre-claim state/evaluator exception
                 -> the same non-None conversation-fallthrough decision
              -> contextual claim plus raw publication permit
                 -> enter post-permit handoff below
     -> post-permit diagnostic-custody/acknowledgement/snapshot handoff
        -> success
            -> render one handled STATUS decision carrying the acknowledgement
            -> renderer/projection/primary-assembly exception:
               one fixed cautious fallback on that acknowledgement
            -> fallback construction exception: acknowledge false once,
               then return one preconstructed handled/suppressed decision
        -> custody-factory, wrapper, or snapshot-attachment failure
            -> resolve raw permit as published=false exactly once
            -> deliver any released terminal through its existing owner
            -> return the preconstructed handled/suppressed decision
     -> optional-route result validation
        -> policy-positive STATUS raw decision: retain original decision/ack
        -> validation exception or replacement identity: acknowledge false once
           and return sentinel
        -> sentinel identity: bypass validator unchanged
           -> traverse trusted renderer/authorizer without acquiring text or a
              capability; finalize admission, close proof once, then suppress
     -> acknowledgement-bearing STATUS decision enters trusted response pipeline
        -> trusted feedback rendering
        -> response-capability authorization
        -> feature-admission finalization and exactly one proof-lifecycle close
        -> require shared policy, original ack identity, handled=true, and the
           original route_kind/response_kind
        -> successful return transfers acknowledgement custody to dispatcher
        -> exception at any stage: proof close attempted once, acknowledge false
           once, deliver any released terminal, return the same preconstructed
           handled/suppressed decision
     -> dispatcher initial/ready decision resolution under generic policy-gated
        acknowledgement custody
        -> protected resolution exception: acknowledge false once and return
           suppressed
  -> ordinary Minecraft command gate
  -> busy/disconnected/admission response
  -> non-Minecraft fallthrough
```

The bypass decision uses the existing
`MinecraftChatClefInputRouteDecision.not_handled(...)` shape with a bounded
STATUS-specific reason such as `command_status_conversational_fallthrough`.
`MinecraftInputRouteSequence` returns that non-`None` decision immediately, so
the outer prediction dispatcher may invoke the normal conversational LLM once
without re-entering Minecraft classification, translation, busy, or submission.
An explicit lexical non-candidate, a candidate with invalid current-event proof,
or a classifier-failure input whose proof is invalid preserves the existing
source-policy/ordinary-routing behavior. A proof-validation exception grants no
STATUS ownership and remains fail-closed under the existing source-policy
boundary. After the current proof is validated, either a produced STATUS
candidate or an unexpected STATUS validator/classifier failure always returns
a non-`None` result, so no later Minecraft owner receives that input.

One successful progress question produces:

```text
Minecraft command submissions: 0
LLM calls: 0
automatic retries: 0
STATUS response facts: 1
UI presentation: 1
output listener delivery: 1
TTS queue acceptance: at most 1
TTS playback observation: at most 1
```

Repeated user questions are independent inputs and may each receive one fresh
status answer. Gradio retry, timer refresh, presentation acknowledgement, or UI
serialization must not replay output or TTS. Because a successful STATUS
response is nonempty, the suppressed-stream invisible completion adapter is not
the product response; it remains relevant only to genuinely empty streams.

The literal `[Minecraft]` prefix is UI decoration, not speech content. Natural
STATUS text uses the existing Minecraft source metadata and must not speak the
prefix through TTS.

## 11. Source matrix

| Current input source | New STATUS ownership |
| --- | --- |
| Exact `(lavi_chat_ui, lavi_chat_ui, chat_submit, final=True)` event with its matching unspent proof | In scope |
| Exact `(voice_input_final, VoiceInput, final_transcript, final=True)` event with its matching unspent proof | In scope |
| Partial/interim microphone transcript | No new STATUS ownership; existing source policy and fallthrough remain unchanged. |
| Raw/legacy GUI command input | Unchanged |
| Native in-game ChatClef command | Unchanged; no Python current-input response identity |
| Butler whisper or background automation | Unchanged |
| Untrusted/lookalike event object | No new STATUS ownership; existing source policy and fallthrough remain unchanged. |
| Valid-looking event with foreign, cross-event, cross-owner, or spent proof | No new STATUS ownership; existing source policy and fallthrough remain unchanged. |

An active command may have a typed or command-name-only descriptor from an
existing accepted route. The new status input still requires its own valid
LAVI Chat/final-microphone proof. Cross-source correlation must not be inferred
from display text alone.

## 12. Exception and fail-closed behavior

Malformed query objects, invalid field types, missing descriptors, unknown
profiles, renderer exceptions, state-reader exceptions, and unexpected
evaluator exceptions after valid current-event proof must not submit a command
or reach a later Minecraft owner. The STATUS owner and Minecraft routed-response
path must not authorize or fabricate an active-progress fact. The exact outcomes
are:

- A normal raw-shape or lexical rejection is an explicit non-candidate and
  preserves existing routing. It is not an exception path.
- An invalid current-event proof grants no new STATUS ownership. A proof-
  validation exception is treated as invalid by this STATUS owner and cannot
  authorize any STATUS result; downstream behavior remains subject to the
  existing source-policy gates.
- After valid proof, an input-validator, addressee-parser, or matcher exception
  before candidate certainty returns one handled fixed cautious response:
  `지금 마인크래프트 작업 상태를 확인하지 못했어`. It acquires no lifecycle
  permit and blocks every later Minecraft route owner for that input.
- A validated prefixless STATUS candidate without a proven contextual claim
  returns the explicit outer-conversation fallthrough decision. A pre-claim
  state-reader or claim-evaluator exception takes the same bypass. Both skip all
  later Minecraft owners and may invoke the existing outer conversational LLM
  at most once.
- An addressed candidate with safely observed unavailable state, or with a
  pre-claim state/evaluator exception, returns the same fixed cautious response
  as one handled decision. It does not invoke the LLM.
- If lifecycle inspection has already issued a raw STATUS permit but creating
  its bounded diagnostic custody, creating its acknowledgement wrapper, or
  attaching that wrapper to the frozen snapshot fails, this is neither a pre-
  claim bypass nor a cautious STATUS response. The
  server API resolves the raw permit as `published=False` exactly once under the
  existing publication lock, sends any released terminal to the existing
  terminal-delivery owner, and returns the typed post-permit failure signal.
  The route then returns the preconstructed handled/suppressed emergency
  decision with no acknowledgement, current-input STATUS/UI/output/TTS product,
  or LLM call. A separately identified terminal released by the failed permit
  may still use its existing UI/output/TTS path exactly once; it is not a STATUS
  product for this question.
- A renderer, presentation-detail projection, or primary handled-decision
  assembly exception after a claim and permit does not discard the permit. The
  fixed fallback avoids the failed renderer/projector, uses empty bounded
  presentation detail, and carries the original acknowledgement. Its
  `published=True` linearization point remains the existing routed external-
  response boundary: a `RoutedResponseEmission` exists and
  `output_delivered is True` after the output dispatcher commit. Chat UI and TTS
  happen on their existing independent receipt paths and must not delay or
  redefine that acknowledgement. Output failure acknowledges `published=False`
  exactly once.
- If even fixed-fallback decision construction fails after a permit exists, the
  owner acknowledges `published=False` once and returns a preconstructed route-
  owned, non-`None`, handled-and-suppressed emergency decision without that
  spent acknowledgement. It skips all later Minecraft routes. The existing
  Gradio invisible-completion adapter may close the empty stream, while
  acknowledgement failure resolution remains free to release an already staged
  terminal response.
- If optional-route result validation raises after an exact acknowledged STATUS
  decision has been returned, the result guard invokes the original
  acknowledgement with `published=False` once and returns the prebuilt sentinel
  instead of the ordinary failure converter's response.
- After optional validation, an exception in the trusted feedback renderer,
  response-capability authorizer, feature finalization, or one proof lifecycle
  close must not strand the original acknowledgement before dispatcher
  ownership. A final decision that drops or replaces that acknowledgement is the
  same fail-closed error. The trusted-pipeline custody guard follows Section 9,
  invokes the original acknowledgement false once, returns the prebuilt
  emergency sentinel, and prevents the exception from reaching the generic
  router-failure logger. There is no current-input STATUS/UI/output/TTS product;
  a separately identified terminal released by the real acknowledgement
  callback may still be delivered once. The guard never calls proof close a
  second time. A capability created before a later exception is not returned,
  attached to the sentinel, or passed to a publisher; this contract does not
  claim revocation of an object independently retained by injected code.
- `feature_admission_finalization` names only an exception escaping from the
  coordinator's `log_feature_admission(...)` call seam outside
  `TrustedKoreanFeatureAdmissionObserver.observe()`. The existing observer
  already catches its own projector/logger exceptions; an internal observer
  failure remains swallowed and must preserve the otherwise healthy decision,
  acknowledgement, and proof-close path without creating a STATUS failure.
  Tests may replace or inject the outer coordinator seam to exercise the closed
  finalization stage, but must not relabel an internally swallowed sink failure
  as that stage. Nested `finally` blocks still invoke proof close exactly once
  when the outer finalization seam raises; if both finalization and close fail,
  the first finalization stage/class remains authoritative.
- Once dispatcher custody begins, an exception from either the initial or
  ready-decision resolver invokes the attached acknowledgement false once and
  returns the existing suppressed dispatch outcome. It does not re-enter the
  router or invoke a sink.
- If the one acknowledgement callback returns false or raises, the routed-input
  boundary suppresses the current-input outcome and does not invoke it again,
  re-enter Minecraft routing, or replay delivery. Existing terminal-delivery
  side effects that completed before the callback result are not undone. A
  synthetic callback that raises before its lifecycle transition does not prove
  pending-permit cleanup or terminal release; this contract requires one call
  and no retry at that boundary, not a fabricated success claim.
- A phrase that the closed classifier explicitly identifies as a non-candidate
  remains on the existing ordinary routing path. A genuine new command that
  reaches busy still receives one busy rejection.
- No path invokes a STATUS-response LLM or retries/re-invokes the outer
  conversational LLM. An explicitly allowed prefixless bypass, including its
  specified pre-claim failure branch, may make one normal outer-conversation
  call. No path retries transport, resubmits a command, consumes or manufactures
  a terminal claim, or replays output/TTS on a UI retry.

The STATUS boundary must catch these exceptions before the generic optional-
route failure logger, because that current logger includes the exception
message. It makes exactly one bounded diagnostic attempt per failed current
input, capped across cascading fallback failures; a healthy diagnostic sink
must therefore observe exactly one record for each injected unexpected-
exception fixture. The record contains only `stage`, `query_kind`, `addressed`,
`requested_family`, `active_command_name`, `lifecycle_state`, `terminal_state`,
`availability_reason`, and `exception_class`. Values come only from their closed
query/profile/state vocabularies or existing bounded controlled reason codes.
Every unavailable field is the exact atom `none`; empty strings and an
interchangeable `unknown` spelling are forbidden. The exception class is from
the first failure in a cascade. A normal validation rejection emits zero
exception diagnostics. A diagnostic-sink exception must not change routing,
acknowledgement, or terminal behavior. Tests must prove every record omits raw
free-form input, exception message text, arbitrary payload dictionaries, full
item catalogs, player chat history, tokens, sockets, and secrets.

The closed diagnostic stages are:

```text
proof_validation
input_validation
addressee_parsing
generic_matching
family_matching
target_matching
state_inspection
claim_evaluation
publication_handoff_diagnostic_custody
publication_handoff_acknowledgement
publication_handoff_snapshot
status_rendering
presentation_detail_projection
primary_decision_assembly
fallback_decision_assembly
optional_route_result_validation
optional_route_result_acknowledgement_identity
trusted_feedback_rendering
response_capability_authorization
feature_admission_finalization
proof_lifecycle_close
publication_acknowledgement_identity
publication_route_identity
dispatcher_initial_decision_resolution
dispatcher_ready_decision_resolution
dispatcher_external_response_publication
dispatcher_publication_commit_inspection
```

An exception class name must match one bounded ASCII identifier of 1–96
characters; an invalid value becomes `invalid`, never arbitrary exception text.
The non-exception identity stages
`optional_route_result_acknowledgement_identity` and
`publication_acknowledgement_identity`, plus the non-exception metadata stage
`publication_route_identity`, use the exact
`exception_class=none` atom. They must not manufacture a synthetic exception or
class name.

## 13. Required characterization and implementation tests

### 13.1 Lock the current symptom first

Before changing behavior, add a characterization that demonstrates an intended
natural progress question falling through STATUS. If the exact user utterance
is recovered from runtime evidence, use it. Otherwise use the smallest
representative missing form and label the screenshot's exact input as unknown
rather than inventing it. Add the full `minecraft_command_busy` pipeline
characterization only when that exact or representative form is successfully
translated by the existing pipeline; otherwise keep classifier fallthrough and
the sanitized local runtime-line summary as separate evidence.

The test must prove:

- active command remains unchanged;
- new command submission count is zero;
- current pre-change route is not `command_status_query`;
- when the existing translator routes the phrase as a command, the current
  visible response is the generic busy sentence;
- no terminal or retry is created; record the actual translator and LLM call
  counts rather than assuming zero. A classifier-only fallthrough test makes no
  outer LLM-call claim. Zero LLM calls are required only after the STATUS route
  owns the question.

### 13.2 Query-shape matrix

For the newly admitted generic/addressee branch, cover LAVI Chat and final
microphone input for:

- every exact `BODY` production in Section 4.1, including both `SUBJECT`
  choices and every listed informal/polite pair;
- each production with empty or exact `지금 `, and with no addressee or each of
  normalized `마크 ai `, `마인크래프트 `, and `마크 `;
- no terminal punctuation, each allowed terminal soft-punctuation character,
  a mixed terminal run, repeated/surrounding U+0020 spaces, and representative
  NFKC/lowercase convergence such as raw `마크 ＡＩ 지금 뭐 해?`;
- exact raw-validator boundaries: an exact base `str` at 128 characters is
  structurally accepted, 129 is rejected, and a `str` subclass is rejected;
  length is measured after U+0020 trim and before NFKC;
- exact rejection for empty/non-`str`, internal soft punctuation, quote,
  semicolon, command separator, CR/LF, tab, Unicode control/format/line-
  separator/paragraph-separator, and representative non-U+0020 `Zs` values
  U+00A0 NO-BREAK SPACE, U+2003 EM SPACE, and U+3000 IDEOGRAPHIC SPACE;
- repeated prefix, compact `마크AI`, `마크야`, and unlisted ending cases.

Separately characterize and preserve:

- the existing family-only forms plus only the explicitly approved new closed
  variants;
- the existing target-qualified item/count and player forms;
- imperative/question separation;
- negated, chained, quoted, malformed, and mixed STOP-like forms.

### 13.3 Contextual ownership matrix

Verify:

- prefixless generic + one matching active owner -> one STATUS response;
- prefixless generic + no owner -> explicit outer-conversation fallthrough;
- prefixless family + matching family -> one STATUS response;
- prefixless family + family mismatch -> explicit outer-conversation fallthrough;
- prefixless target + exact match -> one STATUS response;
- prefixless target + wrong target/count/player -> explicit outer-conversation
  fallthrough;
- addressed mismatch -> one bounded actual-work response from the proven active
  descriptor;
- prefixless stale owner, terminal-claimed state, specialized STOP context, or
  descriptor mismatch -> explicit outer-conversation fallthrough;
- exact ordinary owner plus disconnected transport, quarantine, contradictory
  state, or unavailable evidence -> one cautious response and never `… 중이야`;
- addressed stale/no-owner inspection -> the existing bounded idle or cautious
  response, never a fabricated active task;
- accepted/pending evidence -> pending wording, not running wording.

### 13.4 Source-ownership matrix

Exercise every row in Section 11. The two positive fixtures must use the exact
trusted tuples `(lavi_chat_ui, lavi_chat_ui, chat_submit, final=True)` and
`(voice_input_final, VoiceInput, final_transcript, final=True)`, with a fresh
matching event ID and its exact unspent consumed-ingress evidence/proof. They
must be integration fixtures built through the real `LocalChatInputEventAdapter`
or `ProviderBoundInputEventAdapter`, the
`TrustedIngressProducerRegistrarFactory`/real claim registry, consumed evidence,
and `KoreanChatMicrophoneEligibilityAdmission`; a hand-constructed public
`LaviInputEvent`, fake proof, or always-true proof-validator stub does not satisfy
the positive source contract.

Negative fixtures must separately cover partial/interim microphone input,
Butler/background input, raw/legacy GUI input, native in-game ChatClef input,
an untrusted/lookalike event object, a proof issued for another event, a proof
from another owner, and an already-spent proof. For every out-of-scope row,
assert zero new STATUS ownership and preservation of that row's existing route
behavior. Do not incorrectly assert universal zero command submission: an
unchanged raw, native, or other already-authorized route may still perform its
pre-existing action. Include the exact trusted source/provider/event-kind values
with `final=False`, plus representative cross-pairs such as Chat source with
`VoiceInput` or `final_transcript`, so validating only one tuple field cannot
pass. Inject a throwing current-event proof validator separately and assert no
new STATUS ownership, STATUS cautious response, STATUS conversation-bypass
decision, or leak into the generic optional-route failure logger; existing
downstream source-policy and outer-conversation behavior remains authoritative.

### 13.5 All-profile and all-target rendering

Keep a source-derived registry and direct-renderer drift test over all 26 current
command names, including the non-queryable STOP profile. For each profile, the
direct renderer fixture must render the exact table-owned Korean STATUS
sentence, including representative typed slots and the exact name-only
fallback, without `[Minecraft]` in speech. A truthy-only assertion is
insufficient because it cannot detect regression to a generic fallback. The
test fails when a new registered command lacks an explicit phrase/profile
decision.

For every one of the 25 queryable ordinary lifecycle profiles, construct an
exact active owner and a completely reconciled source-valid evidence fixture
with accepted `status=running` / `result_reason=dispatch_started` plus every
existing profile requirement, then pass the exact newly admitted prefixless
query `지금 뭐 해?` through the coordinator and route. Assert the table-exact
response, zero Minecraft submissions, and zero LLM calls. Using only an already
supported addressed question does not satisfy this matrix because it would not
exercise the new contextual prefixless claim. The specialized trusted STOP
profile remains limited to registry/renderer drift coverage and is not included
in this route matrix.

Characterize the existing descriptorless `diamond_pickaxe` renderer fallback
without changing it, but keep it outside the new contextual/all-target oracle.
Every new contextual positive fixture must carry a non-`None` frozen active
descriptor. A scoped AST/source regression may reject target literals such as
`diamond_pickaxe` in the newly created classifier, claim, projection, route, and
custody modules; it must not incorrectly scan or outlaw the pre-existing
compatibility branch in `CommandLifecycleResponseRenderer`.

Derive item fixtures from the existing 591-key target-policy artifact, but do
not use its classifications as admission rules. Preserve and test the existing
contract that all 591 canonical targets—including the 6 marked
`UNSUPPORTED`/`no_public_alias`—may be accepted through direct-canonical Korean
input, while invalid or absent public Korean aliases remain rejected and
authorized raw targets outside the 591-key artifact retain bounded fallback
rendering. Separately cover `ChatClefKoreanAliasRepository`'s current 564 fixed
aliases, representative material/equipment composition through
`KoreanItemPhraseResolver`, and direct canonical input through that existing
resolver. Admission must happen before the descriptor is frozen; STATUS then
matches only the descriptor's existing canonical `target_item`, exact
`spoken_target_label`, player, allowed particles, and exact quantity.

Preserve the existing source-derived action-domain cross-product and strengthen
its STATUS assertion: run GET, DEPOSIT, DEPOSIT_ALL, and GIVE over every one of
the 591 catalog targets, and EQUIP over every target from
`ChatClefEquipmentTargetComposer.all_targets()`. Preserve the existing raw GIVE
fixture as `give <target> 2`; because it freezes no player, its exact oracle is
`<item/count> 건네는 중이야`, not a fabricated `<player>에게` prefix. Cover a
separate admitted typed GIVE fixture with `player_name=Steve` and require
`Steve에게 <item/count> 건네는 중이야`.

For every command/target pair, bind the resulting accepted descriptor as the
exact active owner and route the single prefixless generic query `지금 뭐 해?`.
Assert the Section 5 table-exact label, count, applicable Korean particle,
optional validated player, and action verb—not merely nonempty output—plus zero
submission and zero LLM calls for the question. This matrix prevents an
all-item GET-only loop from satisfying the “every item/every current item
action” promise.

Also verify ASCII fallback, command-name-only descriptors, and one authorized
outside-591 raw fallback; the unbounded dynamic raw domain needs representative
fallback coverage, not impossible enumeration. Add a smaller target-qualified
matrix for exact canonical target, one submitted fixed alias retained as the
spoken label, one composed material/equipment phrase, player, applicable
particles, and exact quantity, plus wrong-label and wrong-count mismatches. Do
not add cross-alias canonicalization or multiply every item by every question
phrase.

### 13.6 Ordering, correlation, and delivery

Required cases:

- `STOP > STATUS > ordinary busy` remains exact;
- one validated STATUS candidate submits zero commands and claims at most one
  permit;
- classifier non-candidate -> `None` from the STATUS owner and the existing
  later Minecraft routes remain reachable;
- validated candidate without contextual claim -> non-`None`, `handled=False`,
  bounded STATUS-specific reason, later Minecraft owner/intent/translation/
  busy/submission spies all remain at zero, and the outer conversation runs at
  most once;
- current-event proof-validator, input-validator, addressee-parser, matcher,
  state-reader, claim-evaluator, renderer, presentation-detail projector, and
  primary decision-assembly exceptions each follow their exact Section 12
  source-policy, cautious, bypass, or post-permit fallback branch and never
  escape to the generic optional-route failure logger;
- diagnostic-custody factory failure, acknowledgement-wrapper creation failure,
  and immutable snapshot-attachment failure are injected separately after a raw
  permit is issued; each resolves that permit false exactly once, leaves no
  pending publication, returns only the emergency suppressed decision, emits no
  current-input STATUS/UI/output/TTS/LLM result, and releases one concurrently
  staged terminal exactly once under its separate terminal identity and existing
  UI/output/TTS path. The factory receives the same frozen query/snapshot from
  the single synchronized inspection and neither rereads lifecycle state nor
  runs inside the command lock. A real `FabricChatClefServerComponentGraph`
  fixture proves constructor injection into `CommandFeedbackServerApi` and the
  resulting STATUS acknowledgement's exact
  `publication_failure_diagnostic_custody` object; a direct API fixture with the
  default `None` factory preserves its current acknowledgement behavior;
- optional-route result validation, optional-result decision/acknowledgement-
  identity, trusted feedback rendering, response-capability authorization,
  feature-admission finalization, proof lifecycle close, final-
  acknowledgement-identity, and final route/response-identity failures are
  injected separately
  after an acknowledgement-bearing STATUS decision exists. Using the real
  production acknowledgement callback, each applicable fixture invokes that
  original object false exactly once, leaves no pending publication, returns
  the emergency sentinel without a capability, emits no current-input STATUS/UI/
  output/TTS/LLM result, releases one concurrently staged terminal exactly once
  under its terminal identity, and never reaches the generic router-failure
  logger. The proof closer itself is invoked exactly once per fixture, including
  when it raises; the custody guard never retries it;
- a policy-positive optional validator that returns an equal-looking replacement
  decision instead of the original object fails closed, even when it preserves
  the original acknowledgement. Separately, a trusted renderer/authorizer that
  preserves the acknowledgement but changes the decision type, `handled`,
  `route_kind`, or original `response_kind` fails closed at
  `publication_route_identity`; both non-exception identity/metadata diagnostics
  use `exception_class=none`;
- the generalized `command_status_query` and legacy `crafting_status_query`
  route identities both receive the same optional-result, trusted-pipeline, and
  dispatcher failure-only permit cleanup, while their healthy paths and accepted
  phrase sets remain unchanged;
- the shared policy matrix covers the exact base
  `CommandFeedbackPublicationAcknowledgement`, both approved route identities,
  legacy `response_kind=immediate`, and generalized
  `response_kind=command_status`. The START-only
  `CommandFeedbackReadyDecisionAcknowledgement`, exact START base
  acknowledgement, wrong-kind, unacknowledged, handled-false, route-decision
  with an unrelated route, duck-typed `kind=status` acknowledgement lookalike,
  and malformed-object rows all return false without raising. A
  `MinecraftChatClefInputRouteDecision` subclass carrying an exact approved
  STATUS acknowledgement remains policy-positive, matching the existing
  validator and preventing an injected subclass from stranding a permit;
- initial-decision and ready-decision resolver exceptions are injected
  separately in the real `RoutedInputDispatchCoordinator` for both approved
  route identities; each invokes the captured original production
  acknowledgement false once, leaves no pending permit, emits no sink product,
  and returns one suppressed dispatch outcome. Constructor-time router binding,
  later router replacement, and replacement with `None` prove that the two
  generic ports are installed and stale ports are cleared;
- a non-STATUS optional owner/result-validator exception keeps the existing
  optional failure converter and generic logger, with no STATUS sentinel,
  STATUS diagnostic, or new acknowledgement call. A trusted
  `route_kind=command_lifecycle` decision with `ack.kind=start` and a throwing
  renderer/authorizer is not caught by STATUS custody. A dispatcher resolver
  exception for an acknowledgement-free or otherwise policy-negative decision
  keeps its prior behavior and invokes neither the new custody acknowledgement
  path nor the STATUS observer;
- the shared emergency sentinel has every exact Section 9 field, uses deeply
  immutable empty `result` and `translation` mappings, rejects field assignment
  and nested mutation, exposes no writable `__dict__`, and remains unchanged for
  later uses. The full fixture begins with the STATUS owner's sentinel return,
  passes the real optional-result guard/validator boundary, trusted feedback
  renderer, response authorizer, and full routed-input dispatch, and proves no
  capability, acknowledgement, presentation detail, output, TTS, or default
  `minecraft_chatclef/immediate` identity is acquired;
- renderer fallback delivery acknowledges the original permit exactly once as
  true only at `RoutedResponseEmission.output_delivered is True`, or false on
  output failure; fallback-construction failure spends it false once and can
  still release one staged terminal;
- acknowledgement callback return-false and raise fixtures each prove one call,
  a suppressed current-input result, no later Minecraft route, and no delivery
  replay. The synthetic pre-transition raise fixture must not claim pending-zero
  or terminal-release evidence that its callback did not produce;
- STATUS before terminal yields one STATUS then one terminal;
- terminal before STATUS prevents a STATUS-owned/Minecraft-routed false late
  running answer;
- repeated questions do not duplicate START or TERMINAL;
- UI retry does not repeat output or TTS;
- before serialization, the route decision has
  `result.command_submitted=False`, and the resulting
  `RoutedResponseEmission` plus delivery receipts carry
  `route_kind=command_status_query` and `response_kind=command_status`. The real
  Gradio 6.18.0 postprocess/preprocess round trip separately proves the exact
  STATUS text, Minecraft title/source presentation, one user/assistant pair, no
  empty or duplicate assistant card, submit completion, and an immediate second
  submit without refresh/restart; it does not require those internal route/result
  fields to appear in `gr.ChatMessage.metadata`;
- with TTS enabled, one positive STATUS fixture records exactly one queue
  acceptance and exactly one playback observation; the spoken text equals the
  natural response and omits the `[Minecraft]` badge, command IDs, request IDs,
  and presentation-detail metadata;
- Chat UI rendering failure, TTS enqueue failure, and playback failure fixtures
  each prove that an already successful output-dispatch commit still
  acknowledges the original STATUS permit true exactly once; those independent
  sink failures do not redefine output-delivered acknowledgement and do not
  replay any sink;
- final microphone receives the same product wording without any Gradio no-op
  and records its current-input UI boundary as
  `ui_presentation/delivered=True/reason=enqueued`, while Chat direct yield uses
  `chat_ui/delivered=True/reason=yielded`;
- raw/legacy/native routes remain unchanged;
- a raw/legacy `stop` descriptor is never projected as an ordinary STATUS
  owner, while its execution behavior remains unchanged;
- genuine different and duplicate imperative commands remain busy and do not
  preempt the active command.

For every injected single-stage exception caught by the STATUS owner, transport
handoff, optional-result guard, trusted-pipeline custody guard, or dispatcher
custody guard, a healthy logger must receive exactly one STATUS-owned diagnostic
with only the Section 12 allowlisted fields. Each
fixture must assert `stage` equals its exact closed injection stage and
`exception_class` equals the actual first injected exception's bounded class
name; the three non-exception identity/metadata stages instead require the
exact `none` atom. Every known query-kind, addressed, family, active-command,
lifecycle, terminal, and availability field equals its expected bounded value;
unavailable fields must use the exact `none` atom. A cascading primary-plus-
fallback failure remains capped at one record and retains the first exception's
stage and class. Add one cross-boundary fixture in which `status_rendering`
raises, fixed fallback succeeds, and `response_capability_authorization` then
raises: it must produce one diagnostic for the renderer's first stage/class,
acknowledge false once, and return the sentinel. Add one nested-finally fixture
where the outer `feature_admission_finalization` seam and
`proof_lifecycle_close` both raise: proof close is attempted once, one
diagnostic retains the finalization stage/class, and the acknowledgement is
failed closed once.

Assert that raw input, exception message text, arbitrary dictionaries,
credentials/secrets, and unbounded values are absent, and that the current
generic optional-route failure logger receives zero STATUS exceptions. A normal
validation rejection emits exactly zero exception diagnostics. Inject an
internal projector and logger failure separately inside
`TrustedKoreanFeatureAdmissionObserver.observe()`; each remains swallowed and
preserves the healthy route, acknowledgement, and proof-close behavior. Then
inject a distinct coordinator `log_feature_admission(...)` call-seam failure to
exercise `feature_admission_finalization`. An injected STATUS-diagnostic sink
failure must not escape or alter the selected route/acknowledgement outcome or
open a second diagnostic attempt. The downstream acknowledgement-callback raise
fixture instead retains its existing one bounded
`publication_acknowledgement` diagnostic and must not be relogged by the STATUS
owner.

Exercise the exception-class sanitizer itself with dynamically constructed
class names at the exact ASCII identifier lengths 1 and 96, plus a 97-character
name, a non-ASCII name, and a name containing punctuation. The two valid
boundaries are preserved exactly; every invalid value becomes the atom
`invalid`, and no exception message is logged.

### 13.7 Existing tests to preserve

At minimum, retain and extend:

```text
tests/minecraft_chatclef/command_lifecycle/test_command_status_matching_and_route.py
tests/minecraft_chatclef/command_lifecycle/test_korean_command_lifecycle_renderer.py
tests/minecraft_chatclef/command_lifecycle/test_all_item_command_lifecycle_rendering.py
tests/minecraft_chatclef/command_lifecycle/test_initial_running_wire_fixture.py
tests/minecraft_chatclef/crafting_feedback/test_natural_crafting_feedback_integration.py
tests/minecraft_chatclef/crafting_feedback/test_crafting_feedback_publication_ordering.py
tests/minecraft_chatclef/lavi_input/test_korean_input_to_chatclef_submission_path.py
tests/minecraft_chatclef/lavi_input/test_minecraft_chatclef_input_router_delegation.py
tests/minecraft_chatclef/lavi_input/test_router_submission_precheck.py
tests/minecraft_chatclef/lavi_input/trusted_korean/test_trusted_korean_input_route_coordinator.py
tests/minecraft_chatclef/response/test_trusted_korean_command_feedback.py
tests/llm_core/chat_input/gradio/test_gradio_local_chat_stream_completion_adapter.py
tests/llm_core/chat_input/test_local_chat_interface_factory.py
tests/llm_core/input_routing/test_routed_input_dispatch_coordinator.py
tests/llm_core/routed_response/test_command_feedback_delivery_diagnostics.py
tests/tts_core/delivery/lifecycle_response/test_tts_lifecycle_response_component_integration.py
```

New tests should follow the same responsibility folders as production code.
Do not grow one umbrella test file across classifier, context claim, rendering,
route order, and delivery responsibilities.

## 14. Implementation order

1. Characterize at least one missing natural question at the classifier
   boundary; add the full busy-path characterization only when the existing
   translator actually routes that exact or representative form to busy.
2. Freeze the exact generic raw-shape/normalized grammar plus family, target,
   imperative, and STOP-like boundaries.
3. Extract the current generic/family/target matching behind the compatibility
   facade without changing its accepted phrase set or behavior.
4. Add and wire contextual claim evaluation, the explicit non-`None`
   conversation-bypass decision, and STATUS-local bounded exception handling/
   diagnostics. Have `FabricChatClefServerComponentGraph` constructor-inject the
   transport-local diagnostic-custody factory, close the raw-permit-to-custody/
   acknowledgement/snapshot handoff, and prebuild the deeply immutable emergency
   suppressed sentinel in the same rollback unit. Then close acknowledgement
   custody across optional-result validation, trusted feedback/authorization/
   finalization/proof close, identity-preserving transfer, and both dispatcher
   decision-resolution calls through the router's generic bind/clear ports.
   Verify claim,
   bypass, cautious fallback, permit cleanup/acknowledgement, terminal release,
   and generic-logger isolation with directly constructed queries before
   enabling any new phrase.
5. Activate the new generic forms and `마크 AI` longest-match addressee only
   after the claim/bypass integration is present, so no intermediate revision
   can send a newly recognized STATUS candidate into ordinary Minecraft routing.
6. Reuse the current immutable descriptor and evidence-gated lifecycle snapshot.
7. Reuse the existing all-profile renderer; split it only if new rendering
   responsibilities are actually required.
8. Add exception, race, source, all-profile, all-item, output, TTS, and Gradio
   regression coverage alongside the owning behavioral unit.
9. Run focused Minecraft Python tests, adjacent LLM/TTS tests, any app-wiring
   tests whose production boundary was touched, and the full selected Python
   regression.
10. Perform one live LAVI Chat round and one live final-microphone round only
    after separate runtime authorization if it is not already part of a later
    implementation request.

No Java source or Gradle build is required if implementation remains within
this exact Python-only boundary. Discovery of a missing Java lifecycle fact or
wire field is a new scope: stop, report the evidence, and obtain approval before
changing Java or the protocol.

## 15. Offline and live verification contract

The implementation verification records exact commands, versions, counts,
skips, failures, and Ruff status in the final implementation handoff. Minimum
focused selections are:

```text
tests/minecraft_chatclef/command_lifecycle/
tests/minecraft_chatclef/crafting_feedback/
tests/minecraft_chatclef/lavi_input/
tests/minecraft_chatclef/response/
tests/minecraft_chatclef/stop/
tests/minecraft_chatclef/alias_contract/
tests/input_core/input_event/provenance/trusted_user_ingress/
tests/input_core/input_event/delivery/
tests/llm_core/chat_input/gradio/
tests/llm_core/chat_input/test_local_chat_interface_factory.py
tests/llm_core/chat_input/test_trusted_local_chat_input_dispatch_coordinator.py
tests/llm_core/input_routing/test_routed_input_dispatch_coordinator.py
tests/llm_core/routed_response/
tests/llm_core/trusted_ingress/
tests/tts_core/delivery/lifecycle_response/
tests/test_minecraft_chatclef_input_router.py
```

The complete live smoke requires TTS to be enabled and healthy and performs two
consecutive rounds without refresh or restart: round 1 uses exact LAVI Chat
final submit, and round 2 uses an exact final-microphone transcript. Reversing
the order is acceptable, but two Chat-only rounds or two microphone-only rounds
do not close the source matrix. If TTS is disabled or unhealthy, record the
speech portion as `NOT_RUN` or failed; do not call the full live gate complete.

1. submit a command long enough to remain active;
2. ask a newly supported natural progress question;
3. verify the exact active work is shown and spoken once;
4. verify the generic busy sentence is absent for that question;
5. verify no second Minecraft command was submitted;
6. submit a genuine different command while still active and verify it remains
   blocked by busy;
7. stop or allow the original command to finish;
8. verify one terminal response and immediate next-input availability.

Filter every delivery assertion by this exact status-question identity:

```text
event_id=<that round's status-question event ID>
route_kind=command_status_query
response_kind=command_status
delivery_mode=current_input
```

The route itself has no standalone event-ID-bearing decision receipt at the
reviewed HEAD. Prove its admission by joining exactly one existing bounded
feature-admission record to the status delivery receipts by `event_id`. For each
source round, the admission record must contain these exact values in addition
to that round's source/provider/event-kind tuple:

```text
event=minecraft_korean_feature_admission
event_id=<that round's status-question event ID>
final=true
ingress_claim_status=consumed
korean_eligible=true
eligibility_proof_status=issued
feature_scope=none
feature_policy_id=none
phrase_rule_id=none
feature_activation_status=not_activated
decision=handled
reason=minecraft_command_status_query
```

`feature_scope=none` and `feature_activation_status=not_activated` are the exact
current diagnostic-classifier result for `route_kind=command_status_query`; they
do not mean the route fell through. The joined output/UI/TTS receipts—not that
admission record alone—supply `route_kind=command_status_query` and
`response_kind=command_status`. Adding a new feature-scope value solely for this
smoke test is unnecessary.

Counts for the original command START, genuine busy check, STOP, or terminal in
the same round do not count. For each round require one exact admission record
above, zero command admission for the question, exactly one
`output_listener` receipt with `delivered=True/reason=delivered`, exactly one
TTS enqueue receipt with `accepted=True/reason=enqueued`, and exactly one TTS
playback receipt with `observed=True/reason=played`. Require
`played_item_count == item_count > 0`.

The canonical production TTS delivery log projects accepted/observed state but
does not include `item_count` or `played_item_count`. Before Chat or microphone
input is exposed, a disposable live harness must therefore register temporary
in-memory collectors through
`add_lifecycle_response_enqueue_receipt_listener(...)` and
`add_lifecycle_response_playback_receipt_listener(...)`, then retain the exact
immutable enqueue/playback receipt objects for these count assertions. It must
not clear or replace production listeners. If those collectors were not attached
before the round, the count and playback-completeness portion is `NOT_RUN`; log
lines alone cannot satisfy it.

The source-specific UI receipt is exactly one `chat_ui` record with
`delivered=True/reason=yielded` for the LAVI Chat round, and exactly one
`ui_presentation` record with `delivered=True/reason=enqueued` for the final-
microphone round. The status event ID must agree across the admission record and
the source-appropriate UI, output, TTS queue, and TTS playback records; response
generation must also agree across every receipt that carries it.
The active command's existing `active_session_id`, `active_generation`,
`active_request_id`, and `active_command_message_id` values must be present and
exactly equal before and after the status question. The general duplicate-
prevention invariant remains at most one matching record at every delivery
stage; failed receipts do not satisfy the positive live gate.

## 16. Independent rollback units

The applied source changes remain independently reversible:

```text
RB1  behavior-preserving generic/family/target matcher extraction
RB2  contextual claim, non-None bypass, complete STATUS permit/ack custody cleanup, deeply immutable emergency sentinel, bounded diagnostics, and owning tests
RB3  generic phrase and longest-match addressee activation plus owning tests
RB4  any necessary status-renderer responsibility extraction
RB5  source/delivery regression reinforcement
RB6  optional context-rich genuine-busy wording, only if separately approved
```

`RB6` is not part of the required behavior in this document. If it is later
approved, disable that policy first so genuine new commands return to the
existing generic busy response, then remove its renderer/projection wiring.

For a STATUS-feature rollback, disable `RB3` phrase/addressee activation first.
Only then remove `RB2` contextual claim/bypass wiring; this prevents a newly
recognized candidate from ever reaching ordinary Minecraft routing during the
rollback. `RB1` may remain as a behavior-preserving refactor or be removed after
all dependents. Rolling back STATUS expansion must not roll back the all-command
lifecycle renderer, STOP single-response policy, STORE_HOME evidence evaluator,
Gradio completion adapter, command tracker, barrier, reconciliation, terminal
CAS, or Java GET evidence.

## 17. Non-goals

- changing Java command execution, Minecraft Tasks, ChatClef, AltoClef, or
  Baritone behavior;
- adding or changing v1 wire fields, DTOs, status enums, or evidence meanings;
- making busy ownership itself proof of active gameplay;
- accepting a new command, item, alias, quantity, source, or safety tier;
- canonicalizing one target-qualified STATUS synonym into another alias that is
  not already frozen in the active descriptor;
- replacing deterministic matching with an LLM;
- automatically retrying, queueing, replaying, or resubmitting a command;
- changing genuine new-command busy policy without a separate decision;
- changing raw/legacy STOP or native in-game command handling;
- changing STOP tracker, barrier, correlation, terminal validation, or CAS;
- changing STORE_HOME success evidence or terminal wording;
- removing or generalizing the existing descriptorless legacy
  `diamond_pickaxe` STATUS-renderer fallback; the new contextual path must carry
  the frozen active descriptor and never depend on that compatibility branch;
- replaying UI, output, or TTS on retries;
- adding an input-core revocation API for an emission capability independently
  retained by injected code; failed STATUS paths must not return, attach, or
  publish such a capability, but cannot revoke an external reference today;
- hardening the separate existing START-permit interval between
  `claim_start()` and start-decision decoration; it has no STATUS decision to
  place under this follow-up's custody guards;
- redesigning the acknowledgement wrapper's internal transaction when an
  injected callback raises before performing its lifecycle transition; this
  follow-up invokes it at most once and never fabricates cleanup evidence;
- Java build, deployment, Minecraft launch, live-world mutation, commit, push,
  or release as part of this documentation-only task.

## 18. Completion gate

The implementation completion gate requires all of the following:

1. A trusted user can ask a common natural progress question during any
   authoritatively active ordinary command lifecycle and receive one description
   of the actual work.
2. `get diamond_pickaxe 1` with craft semantics answers
   `다이아 곡괭이 만드는 중이야`.
3. The new contextual classifier, claim, projection, and routing path is
   profile-driven for every ordinary command lifecycle profile and descriptor-
   driven for every item accepted by its existing ingress; it adds no target-
   specific or `diamond_pickaxe` branch. The existing descriptorless
   `diamond_pickaxe` renderer fallback is compatibility-characterized and
   unchanged, but no new contextual fixture may reach it. The registered STOP
   phrase profile remains covered by registry drift tests, while trusted STOP
   stays terminal-only and is not a STATUS-query target in this follow-up.
4. Prefixless generic, family, and target-qualified questions are claimed only
   with a matching exact nonterminal ordinary context and any stated
   constraints. A validated prefixless candidate that is not claimed takes the
   explicit outer-conversation fallthrough and cannot reach another Minecraft
   route owner.
5. A successfully claimed status question submits zero commands, invokes no
   LLM, retries nothing, publishes UI and output at most once, and produces at
   most one receipt at each existing TTS queue and playback stage. A candidate
   routed to outer conversation may use the existing LLM once but still submits
   zero Minecraft commands.
6. A genuine second command remains blocked and does not preempt or queue behind
   the active command.
7. Missing, pending, stale, mismatched, disconnected, quarantined, or terminal
   evidence never authorizes a STATUS-owned or Minecraft routed-response
   `… 중이야` claim. The semantic accuracy of an independently generated outer
   conversational LLM reply after explicit fallthrough is outside this change.
8. STOP ordering and the terminal-only visible STOP policy remain unchanged.
9. Terminal correlation, CAS, session/generation identity, Gradio completion,
   and delivery receipts remain unchanged.
10. With the real production acknowledgement callback, every injected STATUS
    custody failure from raw permit handoff through both dispatcher decision
    resolutions leaves zero pending permit and releases any staged terminal once
    under its separate identity. A synthetic callback failure is reported only
    as one attempted call with no retry.
11. Focused and aggregate Python tests plus Ruff pass, with exact results
    recorded; deployment and runtime remain explicitly separate.

## 19. Post-implementation reconciliation

The Python-only implementation now satisfies this contract at repository and
offline-test level. The historical analysis, matrices, implementation order,
and rollback design above remain the review baseline; future-tense wording that
describes those design steps should be read as that pre-change record.

Implemented boundaries:

- deterministic validation, addressee, generic, family, and target classifiers
  remain split by responsibility behind the compatible status-classifier facade;
- contextual claim evaluation uses the exact immutable admitted descriptor and
  one synchronized nonterminal lifecycle snapshot;
- STOP remains first, claimed STATUS returns before ordinary busy, and an
  unclaimed validated prefixless candidate returns an explicit non-`None`
  unhandled bypass so no later Minecraft owner can submit it;
- all 25 ordinary command-lifecycle profiles and every target already admitted
  by existing ingress reuse the profile-driven renderer with no new
  target-specific or `diamond_pickaxe` production branch;
- STATUS raw-permit handoff, acknowledgement-bearing trusted/optional paths,
  dispatcher resolution, external publication, and publication-commit
  inspection retain the original permit custody and fail it false at most once;
- unexpected evaluator or custody failures produce one bounded, redacted
  first-failure diagnostic and one immutable suppressed emergency outcome;
- normal STATUS output continues through the existing UI, output, and TTS
  boundaries without retry, replay, LLM recall, or command resubmission.

Verification and scope status:

```text
PYTHON_PRODUCTION_SOURCE: IMPLEMENTED
PYTHON_TEST_SOURCE: IMPLEMENTED
STATUS_FOCUSED: 122_PASSED_4460_SUBTESTS
MINECRAFT_PYTHON: 1189_PASSED_2_SKIPPED_11474_SUBTESTS
LLM_CORE_AND_TTS_CORE: 156_PASSED_52_SUBTESTS
FULL_PYTHON_EXCLUDING_PREEXISTING_JAVA_HEAD_HASH_ASSERTION: 2487_PASSED_4_SKIPPED_1_DESELECTED_12549_SUBTESTS
FULL_PYTHON_DESELECTED_ASSERTION: PREEXISTING_JAVA_HEAD_CONTRACT_HASH_MISMATCH
JAVA_WORKTREE_CHANGE: NONE
RUFF: PASSED
JAVA_SOURCE_CHANGE: NONE
WIRE_SCHEMA_CHANGE: NONE
MINECRAFT_TASK_BEHAVIOR_CHANGE: NONE
GRADLE_BUILD: NOT_REQUIRED_NOT_RUN
DEPLOYMENT: NOT_RUN
MINECRAFT_RUNTIME: NOT_RUN
COMMIT_OR_PUSH: NONE
```

The one deselected full-suite assertion is a pre-existing Java HEAD contract-
hash mismatch. This Python-only change did not modify Java source or the Java
contract hash, and neither Java tests nor Gradle were required or run. The
deselection is recorded explicitly rather than being counted as a pass.

Live LAVI Chat/final-microphone and Minecraft runtime validation remains a
separate, explicitly authorized step. Its absence does not weaken the offline
contract proof and must not be reported as live verification.
