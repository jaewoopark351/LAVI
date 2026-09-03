<!-- 20260827_openai: Documented the explicit-request-only manual trusted home storage direction. -->
<!-- 20260827_openai: One-time directional review approved the design with focused safety and V1-scope clarifications. -->
<!-- 20260827_openai: Recorded the authorized H1-H4 source implementation without claiming build or runtime verification. -->
<!-- 20260827_openai: Recorded verified core runtime evidence and the initially conditional H6 chat/microphone direction. -->
<!-- 20260827_openai: Final one-time H6 direction review passed after all mandatory routing and terminal conditions were incorporated. -->
<!-- 20260827_openai: Recorded the approved H6 source implementation while keeping public enable, build, and runtime gates open. -->
<!-- 20260827_openai: Recorded the H6 clean build, typed bridge tests, responsibility split, and still-closed gameplay/public gates. -->
<!-- 20260827_openai: Separated the recurring JVM G1 Remark native crash and its A/B investigation from STORE_HOME policy. -->
<!-- 20260827_openai: Clarified the JDK 21 build SDK versus Java 17 target/runtime boundary and the minimum JVM evidence/A-B controls. -->
<!-- 20260828_openai: Recorded user-approved Korean chat runtime evidence and the PASS diagnostics-only plan for the transient MANIFEST_STALE. -->
<!-- 20260828_openai: Final one-time diagnostics direction review passed after exact-snapshot, command-context, bounded-payload, and acceptance-gate clarifications. -->
<!-- 20260828_openai: Documented the CONDITIONAL PASS trusted-container-session-local manifest lifetime direction without authorizing source changes. -->
<!-- 20260828_openai: Clarified single-snapshot activation, transfer-authoritative planning scope, and exactly-once confirmed-delta commit ordering. -->
<!-- 20260828_openai: Recorded the long-distance STORE_HOME candidate-budget exhaustion and bounded timeout diagnostics direction without changing runtime behavior. -->
<!-- 20260828_openai: Final docs-only review clarified elapsed-clientTick evidence, source/artifact qualifiers, and the unobserved candidate decision boundary. -->
<!-- 20260828_openai: Recorded the direct candidate-timeout runtime proof and the PASS phase-scoped timeout behavior direction without authorizing behavior source changes. -->
<!-- 20260828_openai: Final docs-only review fixed timeout-clock ownership, reset semantics, one-way local handoff, and stable reason-code gates. -->
<!-- 20260828_openai: Recorded the phase-scoped timeout source, focused test source, and responsibility split without claiming execution. -->
<!-- 20260829_openai: Completed the focused state, slot, transfer, fingerprint, and diagnostic responsibility split without running the build. -->
<!-- 20260829_openai: Recorded the clean forced build, matching deployed JAR, long-distance STORE_HOME runtime success, and the separate Python command-catalog parity blocker. -->
<!-- 20260829_openai: Corrected the final direct-test topology, timeout decision-order scope, and observed-runtime evidence boundary. -->
<!-- 20260829_openai: Recorded registry parity, direct STORE_HOME semantic gates, and the new clean forced build without extending deployment or runtime claims. -->
<!-- 20260829_openai: Reconciled the final record with the latest clean build, deployed artifact, runtime completion, and source commit evidence. -->
<!-- 20260829_openai: Clarified that STORE_HOME remains explicit-request-only while any automatic pressure-deposit restoration is a separate, still-disabled subsystem. -->
<!-- 20260829_openai: Reconciled the independent automatic-pressure review with STORE_HOME invariants while preserving the current disabled source status and avoiding any false restoration claim. -->
<!-- 20260829_kpopmodder: Recorded the separately authorized automatic-pressure composition restoration while preserving STORE_HOME behavior and keeping build/runtime evidence open. -->
<!-- 20260902_openai: Reviewed and closed the H5 fixed-volume bulk-trust pre-change contract, including fail-closed grammar, exact scan coverage, double-chest identity, one repository transaction, downstream revision effects, and deferred bulk undo without authorizing source work. -->
<!-- 20260903_kpopmodder: Applied the canonical GUI-gate continuous implementation, bounded-log, and verification workflow without rewriting historical approval records. -->
<!-- 20260903_openai: Aligned trusted-home GUI activation with safe open-child quiescence, exact one-shot interaction binding, full-path suppression, explicit serials, and one-time permission consumption; recorded the independent dirty H5 worktree provenance. -->

# ChatClef Manual Trusted Home Storage Direction

문서 상태: `CORE_V1_RUNTIME_VERIFIED_H6_CHAT_RUNTIME_VERIFIED_MIC_PENDING_SESSION_LOCAL_MANIFEST_RUNTIME_VERIFIED_LONG_DISTANCE_TIMEOUT_CAUSE_PROVEN_PHASE_SCOPED_TIMEOUT_OBSERVED_HAPPY_PATH_VERIFIED_PARITY_UNPROVEN_PYTHON_COMMAND_CATALOG_PARITY_VERIFIED_STORE_HOME_DIRECT_TESTS_VERIFIED_HISTORICAL_2026_08_29_LATEST_CLEAN_BUILD_VERIFIED_SOURCE_COMMITS_CREATED_AUTO_PRESSURE_SOURCE_RESTORED_TEST_VERIFIED_BUILD_RUNTIME_PENDING_H5_DOCUMENT_SNAPSHOT_STALE_DIRTY_WORKTREE_SOURCE_PRESENT_UNVERIFIED`

작성 기준일: 2026-08-27, 상태 갱신일: 2026-09-03

위 `HISTORICAL_2026_08_29_LATEST_CLEAN_BUILD_VERIFIED`는 §26.14에 기록된 당시 artifact의
역사적 상태다. 현재 dirty H5 source나 이후 GUI-gate source를 포함하는 최신 worktree build를
뜻하지 않는다. 두 change unit이 함께 dirty인 상태에서 수행하는 향후 build는 별도 attribution이
없는 한 mixed provenance다.

이 문서는 Fabric ChatClef 1.20.1에서 사용자가 명시적으로 요청했을 때만
등록된 trusted destination을 집 보관소로 사용해 인벤토리를 정리하는 방향을
정의한다.

이 문서 자체를 읽는 것만으로 새 작업이 시작되지는 않는다. 사용자가 manual trusted-home
GUI exact-binding/three-later-tick 구현을 직접 요청하면 canonical Carry On 방향의
`구현 -> bounded 로그 보강 -> focused test/clean build 검증` workflow를 별도 단계 승인 없이
연속 수행한다. 외부 JAR 배포, Minecraft 실행, commit 또는 push는 현재 요청이 그 정확한
작업을 포함할 때만 수행한다.

아래 승인·미승인 표현은 각 날짜의 역사적 실행 범위와 증거를 보존하기 위한 기록이며,
현재 GUI gate의 단계별 정지 규칙이 아니다.

특히 아래 H1-H7, H5/H6, §26 approval row와 `별도 승인` 문장은 해당 역사 작업의
provenance만 기록한다. 현재 exact chest/trapped-chest/regular-furnace GUI gate에는 위
continuous override가 이미 적용되며, 그 역사 문구를 source/test/required clean build의 새
중단점으로 재해석하지 않는다. 반대로 이 override를 H5 bulk trust, 외부 배포, Minecraft
runtime, commit 또는 push 승인으로 확대하지 않는다.

2026-08-27 사용자 요청은 H1-H4 Java source와 focused test source를 먼저 승인했다.
이후 별도 명시적 요청으로 clean forced build, JAR 배포, Minecraft runtime log 검토,
commit과 push가 각각 수행됐다. 검증 결과는 23절에 기록한다. 이 이력은 이후 source
수정, build, 배포, commit 또는 push를 자동 승인하지 않는다.

2026-08-27 core V1 1회 방향성 검수 결과는 `APPROVE WITH DIRECTIONAL CHANGES`다.
이 판정은 아래에 반영한 cursor 안전 gate, safety-chain interruption 의미,
trusted registry 재검증, V1 기본값과 범위 고정을 조건으로 한다.

같은 날 H6 chat/microphone adapter의 초기 방향 검수 결과는 `CONDITIONAL PASS`였고,
24절에 필수 조건을 반영한 뒤 최종 1회 방향성 검수 결과는 `PASS`다. H6 입력 adapter
범위에서는 기존 저장 정책과 `StoreHomeTask` 실행 behavior를 재설계하지 않으며,
typed terminal 전달에 필요한
immutable outcome snapshot만 추가한다. 24절의 intent 우선순위, prefixless canonical
command, registry/readiness와 typed terminal projection은 구현 필수 gate로 유지한다.
이후 사용자가 H6 source 구현, clean build와 public Korean live validation을 각각
승인했다. H6 Java source는 clean forced build와 focused JUnit을 통과했고 같은 JAR이
active instance에 배포된 것이 확인됐다. Matching LAVI request의 typed terminal과
한국어 chat happy path도 runtime에서 확인됐다. 다만 microphone parity, 입력 안전성,
busy와 전체 failure/fallback runtime matrix는 아직 별도 gate다. 첫 chat 요청에서 한 번
관찰된 `MANIFEST_STALE`에는 26절이 다루는 operation-wide manifest lifetime 결합이
있었지만, `planned_fingerprint_changed`를 만든 exact 비교값과 reason-label 원인은
미확정이다. 당시에는 25절의 diagnostics-only 보강 뒤 통제 재현으로 좁히기로 했다.

2026-08-28 후속 문서 검수는 operation 시작 때 만들어진 exact manifest가
`NAVIGATE_AND_OPEN`의 정상 inventory mutation까지 감시하는 수명 결합을 분리하는
방향에 `CONDITIONAL PASS`를 부여했다. 목표 구조는 operation-scoped
candidate/result state와 trusted-container-session-scoped immutable manifest의 분리다.
이 판정은 26절에 문서화한 설계 방향만 승인한다. Java, Python, JSON, test source 수정,
test/build 실행, JAR 배포, Minecraft 재현, commit 또는 push는 승인하지 않는다.
25절의 당시 diagnostics 증거와 `planned_fingerprint_changed` reason-label
비정합도 삭제하거나 해결된 것으로 바꾸지 않는다.

현재 dirty worktree에는 26절의 trusted-container-session-local 구조와 phase-scoped
timeout 구조가 source로 존재한다. 이는 각 절을 처음 작성했을 때의 docs-only 승인
이력과 구분한다. 2026-08-29 별도 사용자 요청에 따른 최종 folderization 뒤 clean forced
build가 통과했고, build output과 active instance JAR의 file-level SHA-256이 일치했다.
같은 runtime에서 exact container activation, capacity fallback, paired-delta transfer와
terminal `COMPLETED`가 두 operation에서 확인됐다. 다만 runtime manifest의 complete
repository/source/build-input identity는 계속 `UNVERIFIED`이므로
`artifactParity=PARITY_UNPROVEN`을 유지한다. 23절 상태표는 file-level build/deploy
identity와 observed runtime behavior를 이 strict provenance qualifier와 분리한다.

같은 날 초기 장거리 실행에서는 `@store_home`의 세 trusted candidate ID가 순차
관찰됐고 elapsed `clientTickId` 7,202 뒤 `trusted_candidates_exhausted`로 종료됐다.
당시 `2,400 tick * 3 candidates`와 2 tick 차이로 정렬되는 산술은 candidate lifetime
budget exhaustion을 강하게 지지했지만 direct Task counter와 decision은 없었다.

후속 diagnostics-only build와 통제 재현은 그 미관찰 경계를 닫았다. 세 candidate가
각각 `candidateTicks=2400`에서 candidate timeout으로 실제 제거됐고, 세 decision 모두
operation timeout은 false였다. Timeout 순간 Baritone path/goal은 active였으며 마지막
player movement 또는 best-distance improvement는 각각 12, 2, 1 client tick 전이었다.
Screen, exact binding과 container session에는 도달하지 못했다. 따라서 observed run의
직접 원인은 진행 중인 장거리 navigation을 후보별 고정 lifetime이 자른 것이며, Baritone
stall, binding 실패와 operation timeout은 직접 원인으로 지지되지 않는다. 23.3절과 별도
조사 문서는 direct evidence, `@goto`와의 차이, PASS를 받은 phase-scoped timeout 방향과
아직 미승인인 behavior 경계를 기록한다.

2026-09-02 H5 nearby batch registration 재검토 당시에는 문서 계약만 보강했다. 당시
`@auto_deposit_trust` 구현은 무인자 command이며 handler가 parser를 소비하지 않고, upstream
command runner도 call 뒤 남은 argument를 검증하지 않는다. 따라서 당시 2026-09-02 baseline tree에서
`@auto_deposit_trust area 16x16`을 실행하면 batch로 거절되지 않고 기존 단일 target 등록으로
떨어질 수 있다는 것이 문서 snapshot의 결론이었다. 현재 dirty worktree에는 이 문서와 독립적인
modified/untracked H5 Java/test source가 존재하므로 `SOURCE_NOT_STARTED`는 더 이상 current-tree
사실이 아니다. 그 source는 이 문서 보정에서 검수·승인·검증된 것으로 간주하지 않고 그대로
보존한다. 13절의 fail-closed grammar가 deterministic evidence로 검증되기 전까지 네 batch form은
계속 실행 금지다. 이후 runtime-root clean build는 H5와 GUI 변경을 함께 포함하는 mixed-provenance
build이므로 GUI-only artifact 또는 H5 완료 증거로 보고하지 않는다.

2026-08-29 phase-scoped artifact 재현에서는 이전 고정 lifetime 실패가 재현되지 않았다.
Operation `225`의 첫 candidate는 `7,726` active ticks 동안 유지된 뒤 exact activation에
성공했고, 첫 두 candidate는 timeout이 아니라 capacity로 제외된 뒤 세 번째 candidate가
435개, 23 stack을 저장했다. Operation `30351`도 capacity fallback 뒤 155개, 7 stack을
저장했다. 두 operation 모두 `remainingStacks=0`, terminal
`COMPLETED/paired_delta_confirmed`였고 candidate/operation timeout decision은 0건이었다.

## 1. 최우선 불변조건

다음 조건은 다른 편의 기능이나 자동화 정책보다 우선한다.

```text
사용자의 명시적 요청 없음
    -> 인벤토리가 가득 차도 home storage를 시작하지 않음
    -> trusted destination으로 이동하지 않음
    -> container를 열거나 아이템을 옮기지 않음

사용자의 명시적 요청 있음
    -> 하나의 canonical STORE_HOME 요청 생성
    -> 등록된 exact trusted destination만 사용
    -> 보유 loadout과 reserve를 제외한 아이템을 가능한 한 모두 저장
```

인벤토리 점유율, high-water threshold, idle 상태, 시간 경과 또는 trusted
registry 변경은 `STORE_HOME` 시작 권한이 아니다.

AI는 사용자를 대신해 저장 시작 시점을 결정하지 않는다. 요청을 받은 뒤
어느 trusted destination을 먼저 시도할지, 어느 stack을 보유할지, 상자가
가득 찼을 때 다음 후보로 이동할지는 고정된 정책에 따라 실행할 수 있다.

## 2. 기존 기능과의 의미 분리

세 명령 의미를 혼합하지 않는다.

```text
manual @deposit_all
    -> 기존 ChatClef 수동 전체 저장 의미

inventory-pressure automatic deposit
    -> 인벤토리 압박을 자동 감지해 slot relief를 시도하던 별도 방향

manual STORE_HOME
    -> 사용자 요청으로 집의 trusted storage에 inventory를 정리하는 새 의미
```

`STORE_HOME`은 기존 `@deposit_all`을 재정의하지 않는다. 기존 automatic
planner의 free-slot 목표, working-set recovery, valuable/general fallback
분류도 재사용하지 않는다.

이 기능의 canonical 의미는 `STORE_HOME`이다. 계층별 문자열은 다음과 같이
구분한다.

```text
Python intent / wire canonical command: store_home
Minecraft ChatClef executor command:     @store_home
```

Python adapter가 `@`를 붙이지 않는다. 기존 Fabric bridge의 Java 실행 경계가
prefixless command를 ChatClef 실행 문자열로 정규화한다.

## 3. 입력 경계

모든 입력 방식은 admission 이후 동일한 command와 Task factory에 도달해야 한다.

```text
게임 내 직접 입력: @store_home
    -> StoreHomeCommand
    -> StoreHomeTaskFactory.create()

chat: "지금 아이템 다 집에 가져다 놔"
microphone: 같은 의미의 final transcript
    -> 기존 공통 input_component
    -> llm.receive_input
    -> MinecraftChatClefInputRouter
    -> deterministic STORE_HOME intent
    -> prefixless canonical command: store_home
    -> 기존 submission / reconciliation / Fabric bridge
    -> Java executor normalization: @store_home
    -> StoreHomeCommand
    -> StoreHomeTaskFactory.create()
```

채팅과 마이크가 별도의 parser, planner 또는 executor를 소유하면 안 된다. 이미
합쳐진 공통 입력 뒤에서 한 번만 판정한다. 자연어 adapter는
`StoreHomeTaskFactory`를 직접 호출하지 않고 기존 command bridge에 `store_home`을
한 번 제출한다. bridge 연결 확인, busy admission, request ID, root Task binding,
terminal reconciliation과 cursor gate를 우회하거나 중복 구현하지 않는다.

직접 `@store_home`은 구현되고 core happy path가 검증됐다. 자연어 chat과
microphone mapping도 H6 source로 구현됐고 public Korean live validation과 Korean
chat runtime은 23.2절처럼 확인됐다. Microphone parity와 나머지 runtime matrix는
남아 있다. 입력 표면마다
admission 전 선점 의미는 다를 수 있지만, admission 이후에는 같은
`StoreHomeCommand`, `StoreHomeTaskFactory`, planner, manifest, repository,
executor와 typed terminal 의미를 사용해야 한다.

## 4. UserTask 계약

`@store_home`은 사용자의 새 현재 의도다.

- 기존 `@get`, `@goto`, 제작 등의 현재 UserTask는 기존 명령 실행 경계에서
  정상적으로 교체한다.
- 기존 UserTask를 storage 완료 뒤 자동 재개하지 않는다.
- command 수락 시 cursor stack이 비어 있어야 한다. 비어 있지 않으면 item을
  임의 slot에 놓거나 drop하지 않고 inventory를 변경하지 않은 채
  `CURSOR_NOT_EMPTY`로 거절한다.
- food, defense, unstuck 등 더 높은 우선순위 safety chain은 기존 규칙대로
  `STORE_HOME`을 일시 선점할 수 있다. 이 일시 선점 자체는 terminal이 아니다.
- 같은 `StoreHomeTask`가 manifest activation 전에 재개되면 active manifest가
  없는 상태에서 cursor, world/dimension, runtime context와 현재 후보의 trust를
  재검증한다. navigation 중 발생한 inventory 변화 자체는 stale 사유가 아니며,
  exact trusted GUI에 결합한 뒤의 fresh plan에 반영한다.
- trusted-container session의 manifest activation 뒤 재개되면 기존 local manifest,
  exact GUI binding과 transfer context를 strict revalidate한다. 계약이 달라졌으면
  `MANIFEST_STALE`, `CONTEXT_CHANGED` 또는 대응 terminal로 종료하고
  같은 GUI에서 fresh replan하지 않는다.
- 사용자의 새 명시적 UserTask가 `STORE_HOME`을 교체하면 `INTERRUPTED`로 종료하며
  다시 자동 재개하지 않는다.
- `STORE_HOME`이 `TaskRunner`, `UserTaskChain`, Baritone, 전역 input 또는 다른
  chain을 직접 정리하거나 중단하지 않는다.
- Task가 직접 획득한 input, cursor transaction 또는 operation-local 상태만
  cleanup할 수 있다.
- background thread, `Thread.sleep`, 독립 timer 또는 tick 밖 Minecraft state
  mutation을 만들지 않는다.

root `StoreHomeTask`는 하나의 command-owned root로 operation ID와 context,
candidate queue snapshot, current candidate attempt와 candidate ticks,
attempted/rejected destination, operation timeout, 누적 confirmed 결과,
interruption과 하나의 terminal result를 소유한다. 각 trusted-container session은
active destination, fresh `HomeStoragePlan`, immutable manifest/progress, session
ordinal과 pending transfer를 소유한다. child task는 자신에게 위임된 navigation,
open 또는 exact transfer 단계만 소유하며 A에서 B로 이동해도 command, request,
root와 operation ID를 교체하지 않는다.

## 5. 저장 목표

이 기능은 필요한 몇 개의 slot만 확보하는 relief operation이 아니다.

```text
목표:
    각 trusted-container session 시작 시점의 fresh plan에 포함된
    STORE_HOME step을 exact transfer 계약에 따라 가능한 한 실행

정상 종료:
    pending transfer 없음
    AND (
        authoritative advisory preflight의 STORE_HOME step 0
        OR latest authoritative session progress의 STORE_HOME step 0
    )

부분 종료:
    모든 trusted destination 소진
    destination capacity 소진
    접근 가능한 destination 없음
    safety-chain 재개 시 context 또는 manifest 안전성 상실
    사용자의 새 UserTask에 의한 명시적 교체
```

목표 slot 수를 달성했다는 이유로 active local manifest를 조기 종료하지 않는다.
한 candidate가 capacity를 소진하면 pending이 없고 confirmed delta가 operation 누계에
반영된 것을 확인한 뒤에만 local session을 끝내고 다음 candidate로 이동한다.

기존 `nothing_to_store` no-op은 보존한다. command 수락과 context/cursor
검사 뒤 같은 `StoreHomeTask`의 첫 lifecycle 안에서 기존 planner를 advisory
preflight로 한 번 사용할 수 있다.

```text
STORE_HOME step 0
    -> candidate queue, navigation, GUI open과 click 없이
       COMPLETED / nothing_to_store

STORE_HOME step 1 이상
    -> step 수를 latest-known reporting 값으로 보존할 수 있음
    -> preflight plan과 그 manifest는 전송 계약으로 사용하지 않고 폐기
```

비어 있지 않은 preflight 결과는 root tick manifest revalidation, click source,
authoritative plan revision 또는 도착 뒤 동일성 가정에 사용하지 않는다.
`StoreHomeCommand` 안에서 Task root를 만들지 않고 동기 finish하는 방식으로
옮기지 않는다.

## 6. V1 아이템 disposition

`STORE_HOME`에서는 귀중품과 일반품을 목적지별로 나누지 않는다. 다음
disposition만 사용한다.

```text
KEEP_LOADOUT
KEEP_RESERVE
KEEP_EXPLICIT
STORE_HOME
DEFER_UNSAFE
```

| 상태 | V1 disposition | trusted 저장 |
| --- | --- | --- |
| 현재 착용 중인 armor | `KEEP_LOADOUT` | 금지 |
| 현재 offhand | `KEEP_LOADOUT` | 금지 |
| 주력 곡괭이 1개 | `KEEP_LOADOUT` | 금지 |
| 주력 도끼 1개 | `KEEP_LOADOUT` | 금지 |
| 주력 삽 1개 | `KEEP_LOADOUT` | 금지 |
| 주력 검 또는 근접 무기 1개 | `KEEP_LOADOUT` | 금지 |
| 사용자가 명시적으로 pin한 stack | `KEEP_EXPLICIT` | 금지 |
| survival reserve에 필요한 stack | `KEEP_RESERVE` | 금지 |
| 여분 도구, 무기와 미착용 armor | `STORE_HOME` | 허용 |
| diamond, iron, gold, redstone, gunpowder, bone meal | `STORE_HOME` | 허용 |
| 일반 block, 광물, 제작 재료 | `STORE_HOME` | 허용 |
| reserve 초과 음식, 횃불과 화살 | `STORE_HOME` | 허용 |
| loadout이 아닌 이름/인챈트/trim/custom metadata stack | `STORE_HOME` | exact 검증 시 허용 |
| stale fingerprint, 지원하지 않는 slot mapping | `DEFER_UNSAFE` | V1 금지 |

`STORE_HOME`은 explicit request이므로 `filled_map`, `written_book`, 내용물이 있는
shulker box 같은 고유 stack도 원칙적으로 집에 보관할 수 있다. 단, exact source
slot과 전체 metadata fingerprint를 검증할 수 있을 때만 이동한다.

unknown 또는 modded item을 자동으로 버리거나 일반 상자로 보내지 않는다.
fingerprint와 slot mapping을 안전하게 만들 수 있으면 `STORE_HOME`, 그렇지 않으면
`DEFER_UNSAFE`로 유지한다.

`KEEP_EXPLICIT`은 기존 source에 이미 사용 가능한 pin 또는 명시적 protection signal이
있을 때만 적용한다. `STORE_HOME` V1을 위해 새 pin command, pin JSON 또는 별도 pin UX를
추가하지 않는다. 그런 signal이 없으면 이 disposition은 사용하지 않아도 된다.

## 7. 주력 도구 선택

도구는 재질마다 하나씩이 아니라 역할마다 하나를 유지한다.

```text
pickaxe:      1
axe:          1
shovel:       1
melee weapon: 1
```

검이 없으면 한 개의 주력 axe가 axe와 melee 역할을 함께 만족할 수 있다.
괭이는 기본 loadout 역할이 아니므로 별도 사용자 정책이 없으면 저장 대상이다.

주력 도구 comparator는 deterministic해야 하며 단일 임의 가중치보다 다음
lexicographic 순서를 우선 검토한다.

1. 실제로 해당 역할을 수행하는가
2. 내구도가 critical 상태가 아닌가
3. 역할 capability 또는 mining tier
4. 역할 관련 enchantment utility
5. 남은 내구도 비율
6. 남은 내구도 절대량
7. 현재 선택 중인가
8. logical inventory slot 순서

권장 critical 기준은 남은 내구도가 최대 내구도의 10% 미만인 경우다.
비-critical 후보가 있으면 critical 후보를 주력으로 선택하지 않는다.

Fortune과 Silk Touch처럼 목적이 다른 후보에 객관적인 우열이 없으면 현재
선택된 후보를 우선하고, 그래도 결정되지 않으면 낮은 logical slot을 사용한다.
사용자별 specialty preference는 V1 이후 별도 opt-in 정책으로 둔다.

## 8. V1 armor 정책

V1은 현재 착용 중인 armor를 그대로 `KEEP_LOADOUT`으로 유지한다.
미착용 armor는 exact 검증 후 `STORE_HOME`으로 분류한다.

`@store_home`은 V1에서 다음을 하지 않는다.

- 더 좋은 armor 자동 탐색과 교체
- elytra와 chestplate 자동 비교
- Curse of Binding 장비 교체
- protection enchantment의 상황별 최적화
- 장비 교체 실패 rollback

armor 최적화는 storage와 별도의 사용자 의도이므로, 필요하면 이후
`optimize loadout` 단계 또는 별도 명령으로 설계한다.

## 9. Survival reserve

V1 기본값은 별도 profile UX 없이 `SAFE`로 확정한다.

| 범주 | SAFE reserve | 조건 |
| --- | ---: | --- |
| 안전한 일반 음식 | 합계 16 | 유해하거나 특별 보존 대상인 음식은 제외하고, 적합한 한 종류에 집중 |
| 횃불 | 32 | 한 stack 우선 |
| 화살 | 32 | retained bow 또는 crossbow가 있을 때 |
| 물 양동이 | 1 | 이미 보유한 경우 |

`MINIMAL` profile은 armor, offhand와 주력 도구만 유지하고 위 reserve를 0으로
두는 후속 opt-in 정책이다. V1 구현을 위해 profile 선택 UI나 새 설정 체계를 만들지
않는다.

V1 exact transfer가 partial-stack split을 안전하게 보장하지 못하면 reserve가
포함된 stack 전체를 유지할 수 있다. 예를 들어 음식 64개 한 stack에 reserve
16이 필요하면 64개 stack 전체를 보유하는 결과를 허용한다.

## 10. Exact-slot manifest와 수명

`STORE_HOME`은 command 입력 순간의 inventory를 operation 끝까지 동결해 저장하는
명령이 아니다. 같은 operation에서 exact trusted-container transfer session이
시작되는 시점의 현재 inventory를 loadout 정책에 따라 정리하는 명령이다.

navigation 중에는 authoritative plan, active manifest와 manifest progress가 없다.
§5의 advisory preflight 결과도 active manifest가 아니다. 다음 조건을 모두 통과한
직후에만 현재 inventory를 다시 snapshot하고 planner를 실행한다.

```text
same STORE_HOME operation
AND cursor empty
AND same worldKey and dimension
AND candidate still registered, trusted and enabled
AND the exact matching BlockInteractEvent for this attempt was consumed once
AND the route-owned open child and its normal operation cleanup are quiescent
AND current GUI exactly bound to that destination
AND exact in-scope screen and handler classes match
AND three distinct later client-tick boundaries completed
AND this attempt's one-time GUI permission is available
AND no pending transfer
```

For the GUI stabilization change, trusted-home activation uses the following
additional fixed contract:

```text
in-scope targets:
    minecraft:chest
    minecraft:trapped_chest

exact screen/handler:
    GenericContainerScreen
    GenericContainerScreenHandler

explicitly unchanged on this route:
    minecraft:barrel
    every shulker box
    every unlisted container and handler
```

The matching `BlockInteractEvent` must occur after `WORLD_OPEN_REQUESTED` and
before the accepted TAIL candidate, match the immutable target/world/dimension,
and be consumed exactly once. The TAIL callback captures an immutable candidate
snapshot only. Before `GUI_BOUND`, the parent lets the reusable open child stop
through its normal lifecycle and completes only operation-owned cleanup. If a
binding identity changes while that happens, discard the candidate rather than
binding the replacement. The consumed event remains permanently spent for that
attempt even when the candidate is discarded; a retry needs a new
attempt/correlation and matching event. A stop/cleanup path that releases global
or otherwise unowned input, screen, goal or path state is not operation-owned
cleanup and cannot satisfy the binding predicate.

The TAIL candidate tick `K` and route-owned promotion tick `B` are both excluded;
no `K`-to-`B` gap is backfilled. The implementation ledger must prove current-tick
serial publication before route promotion and observation of that same serial at
the chosen boundary. The bound attempt stores immutable `boundClientTickSerial` and initializes
`lastCountedBoundarySerial` to it. A serial at or below either boundary does not
count; only a strictly later serial may increment after full validation. Boundary
#3 publishes permission only. On the next normal `StoreHomeTask` evaluation,
the activation gate revalidates the entire live predicate and route-specific
transfer preconditions. Permission consumption, activation-snapshot creation and
entry into the existing transfer lifecycle form one logical commit. If that entry
cannot be established, do not consume permission; return a typed invalidation or
terminal result without a slot action.

During stabilization, the entire reachable path—including the parent, current
or former open child, observer, normal cleanup, fallback, helper and any
`super.onTick()` path—is gameplay-mutation-free. Operation-local validation,
serial, deduplication, counter, invalidation and permission bookkeeping may
change. It must short-circuit before QUICK_MOVE,
any other `clickSlot`, slot-action registration, cursor change, screen close or
reopen, `interactBlock`, input change, or Baritone goal/path change. This does
not authorize global cleanup or a generic Task/chain behavior change. A child
tick or stop that can perform one of those mutations also cannot run after
`GUI_BOUND` and before the one-time permission is consumed.

Activation gate를 모두 통과한 뒤 player-held state를 정확히 한 번 immutable
activation snapshot으로 capture한다. Planner input, full activation inventory baseline,
plan revision과 disposition binding, manifest step은 모두 그 동일 snapshot에서
파생해야 한다. Planner용 값과 baseline을 서로 다른 live inventory read에서 만들거나
live mutable `ItemStack` reference를 보관하지 않는다. 같은 activation 안에서 두 번째
read의 값을 섞어 plan과 baseline이 처음부터 달라지는 상태를 허용하지 않는다.

그 single-snapshot planner result의 plan revision과 immutable exact-slot manifest만 현재
trusted-container session의 authoritative transfer 계약이다. Plan identity는
`operationId + containerSessionOrdinal + planRevision`으로 해석하며
`planRevision` 단독으로 operation 전역 identity를 만들지 않는다.

같은 Item의 여러 stack 중 특정 stack만 남겨야 하므로 aggregate `ItemTarget`은
최종 source-selection 권한자가 될 수 없다.

```text
logical slot 5
    좋은 diamond_pickaxe
    -> KEEP_LOADOUT

logical slot 17
    낮은 내구도 diamond_pickaxe
    -> STORE_HOME
```

`ItemTarget(diamond_pickaxe x1)`은 어느 slot을 선택할지 보장하지 못한다.
보호 stack을 다른 slot으로 옮겨 놓는 것만으로도 충분하지 않다.

각 manifest step은 최소한 다음을 보유한다.

```text
logicalPlayerInventorySlot
stackFingerprint
expectedCount
transferMode
disposition
dispositionReason
loadoutPlanRevision
```

manifest에는 현재 `ScreenHandler`의 slot ID가 아니라 player inventory의 logical
slot을 저장한다. container가 열린 뒤 LAVI-owned slot resolver가 logical slot을
현재 handler slot ID로 변환한다.

fingerprint에는 최소한 다음 identity를 포함한다.

- canonical item ID
- damage
- 전체 component 또는 NBT의 canonical hash
- custom name과 lore
- enchantments
- armor trim
- custom attributes와 CustomModelData
- portable-container contents

count는 fingerprint와 분리한다. 전송 후 count가 감소해도 같은 stack identity의
진행으로 검증할 수 있어야 한다.

Transfer manifest step은 activation snapshot에서 `STORE_HOME`으로 판정된 exact
source slot만 소유하며 기존 manifest schema를 유지한다. 별도의 session-local
activation inventory baseline은 planner가 읽은 player-held state를 고정한다.

```text
36 main slots의 occupied/empty 상태와 stack identity/count
armor and offhand state
selected main-hand slot
fresh plan revision and each step's activation-time disposition binding
```

Activation inventory baseline은 session 동안 절대 바꾸지 않는다. Executor가 paired
delta로 확인한 source 변화는 별도의 session-local confirmed-delta overlay와 progress에
기록한다. Validator는 immutable baseline에 그 overlay를 적용해 계산한 expected current
state와 live player-held state를 비교한다. 이 경로로 확인되지 않은 차이는
`MANIFEST_STALE`이다. 따라서 기존 empty slot에 새 stack이 생기거나 KEEP stack,
armor/offhand 또는 selected slot이 바뀌어도 같은 session에서 조용히 무시하지 않는다.
Fresh plan revision과 disposition binding은 live policy를 다시 계산해 비교한다는 뜻이
아니라 activation 때 기록한 불변 참조다. Validator는 activation 뒤 planner를 다시
호출하지 않고, live player-held state가 baseline + confirmed-delta overlay의 expected
state와 같은지와 manifest step이 같은 revision의 `STORE_HOME` disposition에 계속
결합돼 있는지만 검증한다.
Manifest는 새 item을 자동으로 편입하는 live subscription이 아니며, baseline 변화 뒤
같은 session manifest를 refresh하지 않는다. 이전 session이 pending 없이 안전하게
종료된 뒤 다음 candidate GUI에 정확히 결합하면 그때의 fresh snapshot과 baseline이
새 item을 포함한다.

## 11. Exact-slot 전송 계약

operation 시작 전 cursor stack은 비어 있어야 한다. 실행 중 예기치 않게 cursor가
비어 있지 않게 되면 추가 click을 수행하지 않고 현재까지 검증된 결과만 보존한 채
`CURSOR_NOT_EMPTY` 또는 대응하는 context terminal로 종료한다.

아래 strict 검증은 §10의 activation gate를 통과한 현재 trusted-container session의
active local manifest에만 적용한다. Navigation phase에는 검증할 transfer manifest가
없으며 inventory mutation을 source별로 분류하지 않는다.

각 click 직전과 active-session root revalidation에서 다음을 검증한다.

- 같은 world와 dimension
- 같은 `STORE_HOME` operation
- 열린 GUI가 현재 trusted destination과 정확히 결합됨
- immutable activation inventory baseline + confirmed-delta overlay의 expected state와
  live player-held state가 동일
- logical slot이 현재 handler slot으로 정확히 변환됨
- 현재 stack fingerprint가 planned fingerprint와 일치
- 현재 count가 예상 범위와 일치
- manifest step의 activation-time disposition이 `STORE_HOME`
- cursor가 비어 있음

Activation baseline, source fingerprint/count 또는 logical-slot resolution 검증이
실패하면 같은 Item의 다른 slot을 대신 검색해 이동하지 않고 `MANIFEST_STALE`로
종료한다. Cursor, context, trust와 binding은 아래의 각 terminal/candidate-local
규칙을 적용한다.

상태별 우선 의미는 다음과 같다. 여러 terminal 조건이 같은 tick에 겹치면 기존
terminal precedence를 임의로 재정의하지 않되, pending 결과가 불확실한 상태에서
후보 이동이나 fresh plan을 허용하지 않는다.

| 상태 | 처리 |
| --- | --- |
| baseline + confirmed-delta overlay의 expected occupied/empty, item/count/damage/metadata, slot, armor/offhand/selected state와 다른 외부 변경 | `MANIFEST_STALE` |
| logical slot resolver 결과가 unavailable 또는 non-unique | `MANIFEST_STALE`, 임의 offset/substitute 금지 |
| cursor non-empty | 추가 click 없이 `CURSOR_NOT_EMPTY` |
| world/dimension 또는 runtime context 상실 | `CONTEXT_CHANGED` 또는 기존 context terminal |
| 새 UserTask가 root 교체 | `INTERRUPTED`, 자동 재개 없음 |
| destination trust/binding 상실, pending 없음 | local session 종료, candidate 제외, 다음 후보가 있으면 계속 |
| destination trust/binding 상실, pending 있음 | `TRANSFER_UNCONFIRMED`, 후보 이동과 replan 금지 |

Root validation은 executor가 소유한 pending logical slot을 대체 판정하지 않는다.
그 slot의 pre/post-click source, destination delta, cursor와 handler context는 기존
executor가 독점 검증한다.

V1 transfer mode는 exact source slot의 whole-stack `QUICK_MOVE`를 우선한다.
container가 일부만 수용하면 source count 감소량과 destination 증가를 paired delta로
확인하고 operation 누계에 반영한다. Pending이 해소된 뒤 현재 candidate의 capacity가
소진되면 local manifest/progress를 폐기하고, 다음 trusted destination의 exact GUI에서
현재 source remaining을 포함한 fresh plan을 만든다. 이전 manifest를 B에서 이어 쓰지
않는다.

전송 성공은 다음 paired delta가 실제로 확인된 경우에만 인정한다.

```text
source slot count 감소 또는 empty
AND trusted container의 대응 stack delta 증가
AND cursor empty
```

click 요청, client animation, child Task 종료 또는 source 감소만으로 성공 처리하지
않는다. cursor 기반 정밀 stack 분할은 V1 범위에서 제외한다.

Executor가 위 paired delta를 확인한 감소만 `progress.confirm(...)`으로 허용되는 정상
state transition이며 immutable baseline은 바꾸지 않고 confirmed-delta overlay만
갱신한다. 이 confirmation은 해당 pending transfer identity에 대해 정확히 한 번
commit한다. Confirmed-delta overlay, local progress와 operation accumulator는 같은
`StoreHomeTask`-owned transition에서 일관되게 갱신되고, 다음 root revalidation,
candidate transition 또는 tick return 전에는 commit된 expected state가 확정돼야 한다.
Executor 내부 pending marker의 세부 해제 순서를 강제하지 않지만, 다른 Task phase가
`pending=false`와 이전 overlay/progress를 함께 관찰하는 중간 상태는 허용하지 않는다.
이는 client-thread/task transition의 logical atomicity이며 새 lock, thread, timer 또는
upstream lifecycle 변경을 뜻하지 않는다. 같은 confirmation 결과가 중복 관찰돼도
stored item, touched slot과 local progress를 두 번 증가시키지 않는다.

외부 count 증가·감소는 stale다. `MANIFEST_STALE`, `TRANSFER_UNCONFIRMED` 또는
cursor/context terminal 뒤 같은 GUI에서 replan하거나, fingerprint가 같은 다른 slot을
찾거나, pending click을 버리고 새 manifest를 만드는 recovery는 금지한다.

## 12. Trusted destination 정책

후보는 다음 조건을 모두 만족해야 한다.

```text
exact destination으로 명시적 등록
AND enabled
AND 같은 worldKey
AND 같은 dimension
AND operation-local blacklist에 없음
```

실행 순서는 다음과 같다.

1. command 수락, context/cursor 검사와 §5 advisory no-op preflight 수행
2. 저장 대상이 있으면 후보 snapshot을 만들어 operation의 안정적인 시도 순서를 고정
3. 가까운 후보부터 선택
4. 이동 또는 전송 직전에 현재 repository에서 같은 destination ID가 여전히
   등록되고 enabled인지 재검증
5. operation 중 해제되거나 disabled된 후보는 사용하지 않고 제외
6. operation 중 새로 등록된 후보는 현재 snapshot에 합류시키지 않고 다음 명령에서 사용
7. active manifest 없이 실제로 이동
8. exact destination과 결합된 container GUI, supported handler/container,
   cursor, world/dimension과 pending 없음 검증
9. 현재 inventory fresh snapshot으로 planner를 다시 실행하고 local manifest 활성화
10. 실제 server GUI slot으로 capacity와 exact source mapping 검증
11. 전송 직전에도 registration/enabled 상태를 한 번 더 확인한 뒤 manifest step 실행
12. full, missing, unreachable 또는 candidate timeout이면 pending 여부를 먼저
    확인하고, pending이 없을 때만 현재 candidate를 제외
13. confirmed 결과와 failure를 operation에 보존한 채 다음 후보로 단방향 이동하고,
    다음 exact GUI에서 새 plan revision과 local manifest 생성

Operation timeout은 candidate-local 제외가 아니다.

```text
candidate timeout + no pending
    -> current candidate 제외 후 다음 후보

candidate timeout + pending result 불확실
    -> TRANSFER_UNCONFIRMED

operation timeout + no pending
    -> 누적 confirmed/failure에 따른 기존 no/partial terminal

operation timeout + pending result 불확실
    -> TRANSFER_UNCONFIRMED
```

후보 A를 제외한 뒤 B와 C를 시도하고 다시 A로 돌아가지 않는다. cache capacity와
접근성은 ordering hint일 뿐 성공 증거가 아니다.

A session의 local remaining과 B session의 local remaining을 합산하지 않는다.
A에서 paired-delta confirmed된 item 수와 operation-level failure만 누적하고,
B에서는 A 전송 뒤 실제 player inventory의 fresh snapshot을 authoritative source로
사용한다. A에서 B로 이동하는 동안 발생한 inventory 변화도 B activation 전이면
출처와 관계없이 B의 fresh plan에 반영한다.

모든 trusted destination이 소진되면 남은 아이템을 inventory에 유지한다.
다음 fallback은 금지한다.

- 가까운 일반 상자
- scanner 또는 cache가 발견한 미등록 상자
- 새 상자 제작과 배치
- 임시 container
- 다른 차원 portal 이동

manual home storage는 자동 압박 해소보다 먼 이동을 의도할 수 있으므로 기존
automatic trusted max distance를 재사용하지 않는다. V1은 같은 worldKey와 같은
dimension의 모든 활성 trusted destination을 후보로 두고 절대 거리 상한을 두지
않으며, 거리는 시도 순서에만 사용한다. Candidate의 장거리 navigation에는 후보 총
lifetime이 아니라 recent actual movement 또는 best-distance improvement가 모두 사라진
연속 구간을 기준으로 bounded no-progress 판정을 적용한다. Interaction neighborhood에
진입한 뒤에는 screen open과 exact binding을 위한 별도의 bounded local timeout을 적용한다.
Progress가 계속되는 operation을 절대 tick lifetime만으로 자르지 않으며 operation-wide
no-progress와 충분히 큰 emergency hard cap은 별도 의미로 둔다. Exact predicate와 각
숫자는 behavior source 승인 전에 focused tests와 함께 확정한다. 실제 실패 뒤 다음 후보로
이동하는 기존 단방향 정책은 유지한다.

Timeout clock과 reset 의미는 숫자와 별개로 다음처럼 고정한다.

```text
candidate navigation no-progress clock
    -> candidate-attempt-local active StoreHome root ticks만 소비
    -> safety-chain 선점 또는 root가 실행되지 않은 tick은 소비하지 않음
    -> bounded jitter threshold를 넘는 actual movement
       OR bounded epsilon을 넘는 best-distance improvement에서만 reset

progress로 보지 않는 것
    -> path/goal active flag 자체
    -> path calculation/re-adoption 또는 goal 재제출 자체
    -> candidate 선택/교체 자체
    -> sub-threshold position jitter 또는 timestamp 변화

candidate switch
    -> 새 candidate-attempt-local state와 navigation no-progress clock 시작
    -> local interaction clock은 UNSTARTED로 두고 handoff 때 정확히 한 번 시작
    -> operation 누계와 operation no-progress/emergency state를 성공처럼 reset하지 않음
    -> confirmed metrics와 operation failure history도 reset하지 않음

OPEN_AND_BIND_CANDIDATE handoff
    -> candidate attempt 안에서 local interaction clock을 정확히 한 번 시작
    -> interaction-neighborhood 경계 진동, screen/handler flicker와 반복 click 시도로 reset하지 않음
    -> phase를 되돌릴 필요가 있으면 같은 attempt의 누적 local elapsed를 보존하거나
       명시적인 새 attempt generation을 만들어 silent timer restart를 금지

operation emergency hard cap
    -> StoreHome-owned active root tick 기준의 별도 최종 안전 ceiling
    -> candidate가 unusable하다는 증거 또는 candidate-local rejection으로 사용하지 않음
```

Timeout boundary의 stable reason은 최소한 다음을 구분한다.

```text
candidate_navigation_no_progress
candidate_local_interaction_timeout
operation_no_progress
operation_emergency_hard_cap
```

새 `StoreHomeResult` enum은 필수가 아니지만 위 reason을 일반 `candidate_timeout` 하나로
합쳐 원인과 phase를 잃지 않는다. Pending uncertainty, cursor/context와 기존 terminal
precedence는 timeout보다 우선하며 diagnostics나 clock 계산이 behavior를 바꾸면 안 된다.
다른 dimension 이동과 portal 탐색은 계속 제외한다.

## 13. Trusted 등록 UX와 JSON

기존 exact destination JSON registry를 유지한다.

```text
schemaVersion
worldKey
dimension
x / y / z
enabled
```

capacity, path cost, retry, current operation, item policy, trusted area radius 또는 batch receipt를
destination JSON에 저장하지 않는다.

V1 loadout, SAFE reserve와 item disposition은 Java source의 고정 정책으로 구현하며
별도 item-policy JSON, profile JSON 또는 pin JSON을 추가하지 않는다. H5도 persistent trusted
area를 만들지 않고, command 실행 시 발견한 logical destination을 기존 exact coordinate entry로만
추가하거나 다시 enable한다.

### 13.1 H5 상태와 현재 실행 금지

```text
H5_DOCUMENT_STATUS: DIRECTION_AND_PRECHANGE_CONTRACT
H5_DOCUMENT_SNAPSHOT_IMPLEMENTATION_STATUS: NOT_STARTED_AT_2026-09-02_REVIEW
H5_CURRENT_WORKTREE_STATUS: INDEPENDENT_MODIFIED_AND_UNTRACKED_JAVA_TEST_SOURCE_PRESENT; UNVERIFIED_BY_THIS_DOCUMENT
H5_CURRENT_SCOPED_BUILD_RUNTIME_COMMIT_PUSH: NOT_ESTABLISHED_BY_THIS DOCUMENT
H5_FUTURE_RUNTIME_ROOT_CLEAN_BUILD: MIXED_H5_AND_GUI_PROVENANCE_WHILE_BOTH_CHANGE_SETS_REMAIN
H5_CURRENT_BATCH_COMMAND_EXECUTION: PROHIBITED
```

현재 `AutoDepositTrustCommand`는 argument가 선언되지 않은 기존 단일 등록 command이고
`call(...)`에서 `ArgParser`를 읽지 않는다. 현재 upstream `Command.run(...)`과 `ArgParser`는
handler가 소비하지 않은 trailing argument를 call 뒤 자동 거절하지 않는다. 따라서 현재 source에서
`@auto_deposit_trust area 16x16` 또는 유사 form을 실행하면 batch parser failure가 아니라 기존
exact-open/crosshair 단일 등록 경로가 실행될 수 있다.

H5가 구현되기 전에는 아래 네 batch form을 실행하지 않는다. 구현 뒤에도 malformed, unsupported 또는
extra argument가 기존 무인자 single-registration 경로로 fallback하는 순간 H5 acceptance는 실패다.
이 parser 결함을 고치기 위해 `adris/**` command system을 변경하지 않고 LAVI-owned command 경계에서
fail closed한다.

### 13.2 exact command grammar와 alias 경계

승인된 command form은 다음 다섯 개뿐이다.

```text
@auto_deposit_trust
    -> 기존 exact-open 또는 crosshair 단일 등록 의미 유지

@auto_deposit_trust area 16x16
@auto_deposit_trust 반경 16x16
@자동보관등록 영역 16x16
@자동보관등록 반경 16x16
    -> 동일한 H5 fixed-volume batch snapshot
```

`반경`은 사용자-facing alias일 뿐 원형 거리, Manhattan 거리 또는 anchor에서 각 방향 16블록을
뜻하지 않는다. 네 batch form은 모두 같은 16 x 16 X/Z footprint와 16-level Y window를 사용한다.

다음 입력은 H5에서 승인하지 않는다.

```text
@자동보관등록                         # 무인자 Korean single alias는 H5 범위 아님
@auto_deposit_trust 영역 16x16        # 승인되지 않은 mixed alias
@자동보관등록 area 16x16              # 승인되지 않은 mixed alias
@auto_deposit_trust area              # size 누락
@auto_deposit_trust area 8x8          # unsupported size
@auto_deposit_trust area 16x16 extra  # extra argument
```

Whitespace tokenization 외의 fuzzy matching, LLM normalization 또는 자연어 추론을 direct `@`
command parser에 넣지 않는다. H5의 direct Korean command name `@자동보관등록`은 batch-only
alias이며 반드시 `영역 16x16` 또는 `반경 16x16` argument를 요구한다. 무인자 invocation은
승인하지 않는다. 이 alias registration은 LAVI-owned Java command 범위다. Python chat/microphone
natural-language phrase, command catalog, schema, router와 Fabric wire protocol은 별도 범위이며
H5 구현으로 자동 추가하지 않는다.

정상적인 H5 command registration 뒤에는 기존 English trust/untrust/list 세 이름과 Korean batch-only
direct alias `자동보관등록` 한 이름, 총 네 trusted command name이 존재한다. English와 Korean command instance는
mutable parser/onFinish state를 공유하지 않고 pure H5 batch service만 공유한다. Korean alias name이 이미
사용 중이면 existing command를 overwrite하지 않고 alias readiness를 명시적 failure로 남긴다. 이 충돌을
이유로 기존 English trust/untrust/list registration을 제거하거나 막지 않으며, 반대로
`CommandExecutor.registerNewCommand(...)`의 skip-and-continue만 믿고 Korean alias도 등록됐다고 false
success를 보고하지 않는다.

### 13.3 anchor, fixed half-open volume와 loaded coverage

기존 §13의 H5 anchor 의미를 유지한다. H5 anchor는 player position이나 exact-open binding이 아니라
사용자가 현재 crosshair로 바라본 exact block target이다.

```text
1. existing crosshair distance/screen safety boundary 안의 current BlockHitResult가 anchor 후보
2. anchor 후보 자체를 H5 exact block allowlist로 검증
3. exact-open container binding, last interaction position 또는 nearest container로 fallback하지 않음
4. anchor가 없거나 world/dimension identity가 불완전하면 repository mutation 0
```

무인자 `@auto_deposit_trust`는 기존처럼 exact-open binding을 우선하고 없으면 crosshair target을 쓰는
single-registration 의미와 기존 support predicate를 그대로 유지한다. H5 argument branch만 looked-at
anchor와 더 좁은 exact allowlist를 사용한다. 따라서 batch 구현을 이유로 existing no-arg target
precedence나 supported-container 종류를 바꾸지 않고, 반대로 stale/open binding을 H5 area 중심으로
조용히 사용하는 일도 없다.

Anchor의 `(anchorX, anchorY, anchorZ)`를 한 번 snapshot한 뒤 다음 half-open 범위를 사용한다.

```text
X: [anchorX - 8, anchorX + 8)   -> anchorX-8 ... anchorX+7
Y: [anchorY - 8, anchorY + 8)   -> anchorY-8 ... anchorY+7
Z: [anchorZ - 8, anchorZ + 8)   -> anchorZ-8 ... anchorZ+7
```

수직 범위의 review decision은 권장안인 `[anchorY-8, anchorY+8)`로 닫는다. World build-height 밖의
Y는 world의 유효 `[bottomY, topYExclusive)`와 교집합만 취하고 반대편으로 window를 밀어 16칸을
보충하지 않는다. Requested range와 effective range를 결과에 구분한다. Base scan은 최대
`16 * 16 * 16 = 4096` BlockPos로 bounded하다.

H5는 chunk를 load, generate 또는 pathfind하지 않는다. Requested X/Z footprint와 double-chest
pair 검증에 필요한 adjacent position의 chunk가 모두 이미 loaded인지 먼저 확인한다. 하나라도
확인되지 않으면 loaded subset만 등록하지 않고 `SCAN_COVERAGE_INCOMPLETE`로 전체 command를
무변경 실패시킨다. Scan 도중 world/dimension/anchor provenance가 바뀌거나 complete coverage를
유지할 수 없으면 같은 fail-closed 결과를 사용한다.

World block-state scan은 Minecraft client-thread의 한 bounded command action에서 수행한다. Async
world access, entity enumeration, screen open, click, input acquisition, Task 생성 또는 Baritone
path request를 사용하지 않는다.

### 13.4 exact block allowlist와 double-chest logical identity

H5 batch target predicate는 다음 exact vanilla block allowlist다.

```text
Blocks.CHEST
Blocks.TRAPPED_CHEST
Blocks.BARREL
```

다음은 batch anchor와 scan target에서 제외한다.

```text
all shulker boxes
ender chest
minecart/entity inventory
furnace, smoker, blast furnace, hopper, dispenser, dropper 등 processing/utility container
modded container와 tag 기반 확장
```

현재 `AutoDepositTrustedContainerSupport.isSupported(...)`는 upstream
`StoreInContainerTask.CONTAINER_BLOCKS`를 재사용하고 그 목록에는 shulker box가 포함된다. H5가 이
predicate를 그대로 재사용하면 확정된 batch exclusion과 충돌한다. 따라서 H5는 LAVI-owned exact
allowlist를 별도로 소유하거나 그와 동등한 명시적 좁힘을 사용하며, upstream support array나 generic
container/block-entity predicate를 batch discovery 기준으로 사용하지 않는다. 이미 no-arg single
command로 등록된 shulker나 다른 H5-excluded exact entry는 batch discovery 대상이 아닐 뿐 H5가 삭제,
disable 또는 migrate하지 않는다.

Double chest는 두 physical half를 하나의 logical destination으로 취급한다. 단순 인접만으로 pair를
추정하지 않고, 같은 vanilla block type, 같은 facing, complementary chest type와 reciprocal partner
관계가 모두 확인된 pair만 인정한다.

```text
logicalPairKey:
    두 half BlockPos를 (x, y, z) lexicographic order로 정렬한 order-independent pair identity

repository representative:
    pair half 중 정확히 하나가 기존 registry에 있으면 그 exact position과 destination ID를 보존
    기존 half가 disabled면 같은 position을 enabled=true로 update
    기존 half가 하나도 없으면 lexicographically smaller BlockPos를 새 representative로 등록
    두 half가 모두 기존 registry에 있으면 H5가 조용히 삭제/병합하지 않고
        PREEXISTING_DOUBLE_CHEST_DUPLICATE로 전체 transaction 실패
```

이 규칙은 H5가 기존 exact ID를 몰래 바꾸거나 registration command가 cleanup/untrust까지 수행하는
것을 막으면서, 새 batch 결과와 기존 registry를 합친 최종 상태에서 한 pair가 한 entry만 갖게 한다.
Pair의 한 half만 requested volume 안에 있어도 logical destination은 포함할 수 있으며, validated
partner가 volume 경계를 한 칸 넘어가면 representative도 경계 밖에 있을 수 있다. 이 경우에도 pair
partner는 loaded이고 reciprocal validation을 통과해야 한다.

Chest state가 `LEFT/RIGHT`인데 partner chunk가 unloaded이거나 reciprocal pair를 증명할 수 없으면
single chest로 꾸미지 않고 `AMBIGUOUS_DOUBLE_CHEST`로 전체 transaction을 거절한다. 등록 뒤 chest가
분리, 재결합 또는 교체돼 topology가 달라져도 H5가 registry를 자동 migration하지 않는다. 기존 exact
runtime revalidation과 다음 explicit registration/untrust가 그 후속 상태를 다룬다.

### 13.5 한 repository atomic transaction

Scan과 double-chest normalization이 complete하게 끝난 immutable logical destination set 전체를
repository에 한 번 전달한다. Command 또는 service가 발견 순서대로 기존 `register(...)`를 반복
호출하지 않는다.

Repository-level transaction은 current registry snapshot이 정상적으로 읽혔다는 사실부터 소유해야
한다. 현재 repository의 `loadSafely()`는 persistence `IOException`을 log한 뒤 empty list로 바꿀 수
있으므로, H5 bulk path가 그 empty fallback을 정상적인 빈 registry로 해석해 save하면 기존 trusted
entries를 덮어쓸 수 있다. H5 transaction은 missing-and-valid-empty, successful parsed registry와
read/parse failure를 구분한다. `REGISTRY_READ_FAILED` 또는 schema/provenance 불명확 상태에서는
scan 결과가 완전해도 save, in-memory publish와 H5-owned revision mutation이 모두 0이다.

Repository-level transaction은 다음 의미를 가져야 한다.

```text
strictly load/reload current registry once
-> require registryReadStatus=OK
-> on read/parse failure: save 0, publish 0, H5-owned revision mutation 0
-> otherwise preserve existing entry order
-> resolve every new / already-enabled / disabled-to-enabled / conflict outcome
-> append genuinely new representatives in deterministic (x, y, z) order
-> construct one complete updated exact-destination list
-> persist at most once
-> publish in-memory list at most once
-> H5-owned effective mutation이면 repository revision exactly +1
```

모든 발견 destination이 이미 enabled여서 final set이 같으면 `NO_CHANGE`이며 persistence mutation과
revision increment는 0이다. 새 entry 또는 disabled-to-enabled update가 하나 이상 있으면 destination
수와 무관하게 H5-owned revision increment는 정확히 한 번이다. Persistence failure, validation conflict,
incomplete scan 또는 ambiguous double chest에서는 in-memory list, persisted logical set과 H5-owned
revision mutation이 모두 0이다. Partial success list를 success로 반환하지 않는다.

`atomic transaction`은 one-process repository semantic all-or-none, one save/publish와 one revision을
뜻한다. 현재 file store가 `ATOMIC_MOVE` 실패 시 ordinary replace로 fallback할 수 있으므로 power loss,
filesystem crash 또는 외부 process와의 완전한 serializable transaction까지 보장한다고 과장하지
않는다. Command 실행 중 registry JSON을 외부에서 동시에 수정하는 것은 지원하지 않는다. Reload 뒤
external modification conflict가 탐지되면 덮어쓰기나 임의 merge 대신 무변경 실패한다.

H5는 기존 exact destination JSON schema만 사용한다. Area bounds, anchor, scan receipt, logical pair key,
capacity, path cost와 current operation을 persistent destination entry에 추가하지 않는다.

### 13.6 64-candidate 정책과 side-effect 경계

H5 discovery/registration에는 임의 `64` 절단을 적용하지 않는다. Fixed volume 안에서 검증된 모든
logical destination을 한 repository transaction의 input과 결과에 포함한다. 현재
`AutoDepositTrustedDestinationSelector`의 `MAX_OPERATION_CANDIDATES=64`와 bounded fingerprint state는
한 automatic/storage operation의 candidate snapshot과 diagnostics 정책이며 registry capacity 또는
H5 registration limit가 아니다.

따라서 65개 이상의 destination이 성공적으로 등록될 수 있지만, 뒤의 한 automatic operation은 기존
정책에 따라 그중 최대 64개만 candidate로 볼 수 있다. H5 command success를 모든 destination이 다음
operation에서 즉시 시도된다는 의미로 사용하지 않는다.

현재 selector는 final candidate list를 64개로 자르기 전에 repository의 matching destination을 순회하고
evaluator를 호출한다. 따라서 operation output 64 제한만으로 repository lifetime size와 per-operation
evaluation cost가 bounded하다고 주장할 수 없다. H5는 per-batch 또는 global repository entry를 조용히
64개로 자르지 않는다. 별도 global repository capacity/indexing 정책이 필요하다고 판정되면 exact 숫자와
근거를 별도 승인하고, limit 초과 시 whole transaction을 무변경 거절해야지 앞의 N개만 저장하지 않는다.
현재 H5 문서는 global capacity 숫자를 확정하지 않으며 release 전 repeated-batch size/per-operation cost
stress evidence를 요구한다.

H5 command handler가 직접 허용되는 side effect는 다음뿐이다.

```text
loaded world block-state read
repository bulk mutation attempt
bounded command result/log output
```

Command handler는 `Task`, path, click, screen open, cursor transfer, input acquisition 또는 automatic
operation을 직접 만들지 않는다. 그러나 effective repository revision 변경은 기존
automatic-pressure chain의 `WAIT_FOR_REARM` 또는 policy fingerprint를 다음 client tick에서 다시
평가하게 할 수 있다. Inventory pressure와 기존 guard가 만족되면 command가 끝난 뒤 downstream
automatic execution이 별도로 시작될 수 있다. 이를 command handler의 직접 navigation/click으로
기록하거나, 반대로 registration 뒤 gameplay side effect가 절대 없다고 주장하지 않는다.

`NO_CHANGE`는 revision을 바꾸지 않으므로 H5 자체가 revision-change reevaluation을 만들지 않는다.
Registration result와 뒤의 automatic operation identity, terminal과 transfer evidence는 별도 lifecycle로
기록한다.

### 13.7 bounded result와 acceptance contract

Command output은 전체 destination ID 목록을 한 줄에 펼치지 않고 다음 bounded summary를 최소
포함한다.

```text
status / reason / boundedFirstConflict
commandForm
registryReadStatus
anchorSource / anchorPos
requestedRange / effectiveRange
coverageComplete
scannedPositionCount
physicalSupportedBlockCount
logicalDestinationCount
newlyRegisteredCount
reenabledCount
alreadyRegisteredCount
doubleChestCollapsedHalfCount
repositoryRevisionBefore / repositoryRevisionAfter
totalRegistryCountBefore / totalRegistryCountAfter
downstreamAutomaticReevaluationPossible
```

새 Fabric wire payload, bridge result schema 또는 Python command result를 H5 때문에 추가하지 않는다.
Exact before/after destination set이 필요한 runtime fixture는 별도 승인된 read-only list/repository evidence로
검증한다.

향후 source-edit와 test-execution이 각각 별도 승인된 뒤 최소 acceptance는 다음이다.

1. 무인자 English command의 기존 single target 의미와 기존 supported-container 범위가 변하지 않는다.
2. 네 batch form만 정확히 승인되고 malformed/extra/unsupported argument는 mutation 0이며 single fallback 0이다.
3. 정상 등록에서는 English trust/untrust/list + Korean batch alias의 정확히 네 command name이 존재한다.
   Korean alias collision은 overwrite/false-ready 0으로 실패하되 기존 English 세 command는 보존하고,
   English/Korean command instance의 mutable parser/onFinish 공유는 0이다.
4. Direct Korean batch-only alias는 Java command registry에만 존재하고 Python chat/microphone route 변화는 0이다.
5. H5는 looked-at crosshair anchor만 사용하고 exact-open/nearest/player-position fallback은 0이다.
6. negative coordinate, half-open high edge와 world build-height 교집합이 exact range와 일치한다.
7. incomplete chunk coverage, world/dimension change와 invalid anchor는 scan subset을 commit하지 않는다.
8. chest, trapped chest, barrel은 포함되고 shulker, ender chest, entity/processing/modded container는 제외된다.
9. double chest 두 half, volume-edge pair, existing-one-half, existing-both-halves와 ambiguous partner를 각각 검증한다.
10. missing registry와 valid empty registry는 성공적으로 구분되고, corrupt JSON/load IOException/unknown read
    provenance는 save 0, publish 0, H5-owned revision mutation 0이다.
11. 65개 이상 logical destination fixture에서 registration truncation 0, repository final set 전부 보존,
    downstream operation candidate limit 64는 기존 selector 계약으로 별도 유지된다.
12. persistence failure와 external conflict는 final set/revision mutation 0이며 effective multi-entry success는
    save 1회, publish 1회, revision +1회다.
13. repeated-batch registry growth stress는 total registry size와 selector evaluation cost를 기록하며, explicit
    global capacity 정책 없이 hidden truncation 또는 bounded-cost claim을 만들지 않는다.
14. command handler의 Task/path/click/input call 0을 deterministic seam으로 검증하고, revision 변경 뒤
    automatic chain reevaluation은 별도 next-tick lifecycle test로 구분한다.

2026-09-02 문서 snapshot에는 test source나 test/build/runtime 실행이 없었다. 현재 dirty
worktree의 독립 H5 Java/test source는 이 절의 acceptance를 통과했다고 검수되지 않았고, 관련
clean build/runtime 증거도 이 문서에 없다. 위 항목은 여전히 acceptance contract이며 현재 구현
완료 증거가 아니다.

### 13.8 bulk undo/untrust UX

H5 initial slice에는 bulk undo, area untrust와 batch receipt persistence를 넣지 않는다. 기존
`@auto_deposit_untrust <exact-destination-id>`와 `@auto_deposit_trusted_list` 의미만 유지한다.

단순히 같은 area를 다시 scan해 현재 들어 있는 destination을 모두 지우는 undo는 금지한다. 그러면 H5
이전부터 있던 trusted destination이나 그 사이 사용자가 수동 등록한 entry까지 삭제할 수 있다. 향후
bulk undo가 필요하면 특정 successful batch가 **새로 추가하거나 enable한 exact ID delta만** 소유하는
bounded receipt, repository revision/conflict 검증과 all-or-none removal을 별도 UX/JSON 결정으로
설계한다. 그 전에는 H5 registration command가 cleanup, migration 또는 bulk removal을 수행하지 않는다.

Persistent trusted area는 V1/H5 범위에 포함하지 않는다. 미래에 새로 배치된 container를 자동 trusted로
승격하지 않으며, 기존 단일 exact registration, exact unregistration과 list command의 의미를 변경하지
않는다.

## 14. 재사용 경계

현재 LAVI-owned trusted implementation에서 다음 책임은 재사용 후보다.

- instance-owned exact destination repository
- world/dimension/BlockPos identity와 stable destination ID
- persistence 실패 시 mutation 거절
- trusted registration, unregistration과 list command
- candidate filtering, ordering과 operation-local blacklist
- exact-open container binding
- 실제 GUI capacity acceptance
- source와 destination paired delta tracking

현재 `AutoDepositTrustedStoreTask`의 destination queue, navigation, open과 GUI
validation 방향은 참고 또는 composition할 수 있다. 그러나 generic `ItemTarget`
source selection은 `STORE_HOME`의 최종 전송기로 재사용하지 않는다.

공유 upstream `StoreInContainerTask`, `DepositAllTask`, `Task`, `TaskRunner`,
`UserTaskChain`, `PlayerInteractionFixChain` 또는 Baritone 의미를 변경하지 않는다.

## 15. LAVI-owned 구성 방향

정확한 class 수와 package는 source audit 후 최소화하되 책임 경계는 다음과 같다.

```text
StoreHomeCommand
    -> canonical request를 한 번 생성

StoreHomeTask
    -> 전체 operation phase, candidate/result accumulator,
       interruption, timeout과 terminal 소유

HomeLoadoutPlanner
    -> logical slot별 KEEP / STORE_HOME 판정

HomeStorageManifest
    -> 현재 trusted-container session의 immutable exact-slot plan과 fingerprint 소유

HomeStorageManifestProgress
    -> 현재 trusted-container session의 immutable activation inventory baseline,
       confirmed-delta expected-state overlay, confirmed progress와 pending 제외 검증 소유

StoreHome operation accumulator
    -> confirmedStoredItems, confirmedTouchedLogicalSlots,
       capacityFailures, unavailableFailures,
       latestKnownRemainingStacks와 operationTicks 소유

HomeStorageTransferExecutor
    -> 현재 trusted GUI에서 exact source slot 전송과 검증

HomeStorageScreenSlotResolver
    -> logical player slot을 current handler slot으로 변환
```

새 코드는 `adris/**`가 아닌 LAVI-owned namespace에 둔다. 새 Task가 기존
`Task` contract에 참여하기 위한 상속은 허용하지만 engine-wide lifecycle을
override하지 않는다. composition을 우선하며, 단순 DTO마다 불필요한 manager나
framework를 만들지 않는다.

Operation accumulator는 `StoreHomeTask`의 좁은 필드 집합 또는 작은 LAVI-owned
collaborator로 구현할 수 있다. 새 manager/service framework는 만들지 않으며 local
manifest를 교체해도 operation 누계를 초기화하지 않는다.

## 16. 기존 inventory-pressure chain과의 공존

`STORE_HOME`의 explicit-request-only activation과 별도의 automatic inventory-pressure
subsystem은 공존할 수 있다. 양립할 수 없는 것은 inventory pressure가 `StoreHomeTask`를
자동 생성하거나 StoreHome planner, manifest, timeout, exact-slot executor 또는 terminal
contract를 automatic execution에 재사용하는 구조다.

```text
STORE_HOME
    activation: explicit user request only
    owner: UserTaskChain / StoreHomeTask
    destination: exact trusted destination only
    policy: manual home loadout policy
    timeout/result: StoreHome-owned

automatic inventory-pressure deposit
    activation: occupied-slot high-water policy
    owner: DepositAllInventoryPressureChain / AutoDepositMaintenanceTask
    destination: automatic safe-surplus and trusted-only policy
    success: actual free-slot postcondition
    StoreHomeTask creation or behavior reuse: forbidden
```

2026-08-27의 automatic entrypoint disable은 manual V1을 고립해 구현하기 위한 당시의
composition gate로 보존한다. 이를 `STORE_HOME`이 존재하는 동안 모든 automatic pressure
기능이 영구 금지된다는 제품 계약으로 확대하지 않는다. 동시에 독립 subsystem이 공존
가능하다는 설계 판정을 현재 source에서 이미 복구됐다는 완료 주장으로 바꾸지 않는다.

현재 상태는 다음과 같다.

```text
automatic pressure chain production construction: exactly 1 per AutoDepositRuntime
automatic pressure chain tick: active after exact-binding tracker tick
forward-only restoration contract: documented
forward-only restoration implementation: source restored; focused/targeted tests passed
restoration clean build / JAR / Minecraft runtime: not run
STORE_HOME dependency on automatic chain: none
STORE_HOME explicit-request-only behavior: preserved
```

기존 automatic source를 삭제하거나 리팩터링할지는 복구와 분리된 후속 결정이다. 현재 dirty
worktree의 lifecycle, diagnostics와 test 변경을 이 composition 작업과 한 diff에서 삭제,
이동 또는 흡수하지 않는다.

### 16.1 2026-08-29 automatic pressure conflict 계약

현재 source root cause와 forward-only 복구 게이트의 canonical owner는
[ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md)의
§28이다. 이 문서는 StoreHome 쪽 불변조건만 고정한다.

```text
공유하는 runtime-scoped identity
    AutoDepositTrustedDestinationRepository
    AutoDepositOpenContainerBindingTracker
    TaskRunner

공유하지 않는 operation behavior
    StoreHome planner, manifest, timeout and terminal result
    automatic pressure state machine and NO_SAFE latch
    automatic immutable plan and maintenance phases
    manual @deposit_all selector
```

Automatic side는 current `UserTaskChain`의 non-idle root가 exact `StoreHomeTask`이면 selected
chain 검사나 working-set resolution보다 먼저 user-owned storage conflict로 처리한다.

```text
StoreHome root assigned
    -> automatic task submission 0
    -> NO_SAFE_SURPLUS 오분류 0
    -> existing_store_home_task suppression
    -> 기존 storage-conflict WAIT_FOR_REARM contract
```

권위는 selected chain이나 cached task path가 아니라 current UserTask root object identity다.
`StoreHomeTask.isActive()`, phase, timeout, current child, manifest, exact binding과 diagnostic
generation을 conflict 식별자로 사용하지 않는다. 첫 tick 전 root도 conflict이며, one-tick
observation을 suppression 뒤 보관해 stale root를 계속 막지 않는다.

Safety chain이 StoreHome을 선점해도 UserTaskChain의 root identity는 유지된다. 이 시간에는
StoreHome candidate, operation 또는 interaction clock을 소비하지 않고, safety 종료 뒤 같은
StoreHome root가 선택돼야 한다. Automatic chain은 StoreHome callback, result, timeout 또는
cleanup을 소유하지 않는다.

Automatic run이 먼저 시작된 뒤 StoreHome request가 제출되면 automatic maintenance의
immutable context mismatch 또는 automatic-chain interruption 경계가 automatic-owned root만
종료한다. Context mismatch가 발생한 tick과 `SingleTaskChain`이 terminal root를 clear하는
reconciliation tick은 다를 수 있다. Context-mismatch 경로에서는 phase-specific
child/container work보다 mismatch 검사가 먼저다. Safety interruption 경로에서는 mismatch
검사 없이 `onInterrupt()/stopOwnedRun()`이 automatic-owned run을 종료한다. 두 경로 모두
그 이후 automatic child/container work와 click은 `0`이어야 한다.

StoreHome conflict를 기존 `WAIT_FOR_REARM`으로 처리하는 최소 복구에서는 StoreHome이 slot
relief 없이 종료돼도 automatic deposit이 즉시 재평가되지 않는다. 즉시 재평가가 필요하면
별도 상태와 승인을 요구하며 StoreHome 문서에 숨겨 추가하지 않는다.

Automatic pressure composition 복구는 이 문서의 StoreHome behavior 변경 승인이 아니다.
다음은 그대로 유지한다.

```text
StoreHome explicit-request-only
StoreHomeTask exactly one root per accepted request
StoreHome timeout and terminal precedence
exact trusted-only destination
no general-container fallback
manual @deposit_all independence
TaskRunner, UserTaskChain, Task and Baritone shared behavior unchanged
```

### 16.2 2026-08-29 automatic composition 복구 뒤 StoreHome 보존 증거

§16.1의 별도 automatic subsystem 복구가 승인되어 production source에 적용됐다. 이 변경은
StoreHome 쪽 planner, execution state, timeout lifecycle, exact transfer, terminal outcome 또는
command submission source를 수정하지 않았다. Automatic side가 의존하는 StoreHome type 정보는
current non-idle `UserTaskChain` root의 exact `instanceof StoreHomeTask` 분류 하나뿐이다.

같은 runtime-scoped repository와 exact-binding tracker를 trusted commands, StoreHome factory와
automatic chain이 공유하지만, operation-scoped context/plan/Task/manifest는 서로 공유하지 않는다.
따라서 automatic pressure가 `StoreHomeTask`를 만들거나 StoreHome timeout과 result를 재사용하는
경로는 열리지 않았다.

현재 direct/integration evidence는 다음과 같다.

```text
StoreHome root assigned before its first tick
    -> production automatic callback submission 0
    -> NO_SAFE_SURPLUS latch 0
    -> WAIT_FOR_REARM
    -> actual TaskRunner first eligible selection preserves the exact StoreHome root

StoreHome assigned during an automatic run
    -> immutable context mismatch before automatic child/container work
    -> automatic root reconciled and cleared
    -> exact submitted StoreHome root selected

safety preempts an active automatic run after StoreHome assignment
    -> automatic-owned root/child stop exactly once
    -> StoreHome remains inactive while safety owns selection
    -> safety end hands off to the same StoreHome root
    -> TaskRunner.disable calls 0
```

2026-08-29 current test result:

```text
automatic focused: 26 classes, 62 tests, 48 executed, 14 existing registry-free fixture aborts,
                   failures 0, errors 0
automatic + manual DepositAll + StoreHome targeted:
                   38 classes, 93 tests, 79 executed, 14 existing registry-free fixture aborts,
                   failures 0, errors 0
final targeted test graph: 46/46 tasks executed in 2m 26s with --rerun-tasks;
                           no per-test retry
test weakening, failure deselection or retry: none
```

14건은 registry-free `Item` identity 생성이 불가능한 기존 fixture abort다. 이번 결과는
automatic composition, exact StoreHome decision order와 TaskRunner handoff의 current
실행 증거지만, item-dependent hard protection/reserve/classification/planner case가 현재
JUnit에서 실행됐다는 뜻은 아니다. 해당 policy source는 이번 복구에서 변경하지 않았다.

이 수치는 source/test evidence이며 이번 restoration의 clean forced build나 runtime artifact
증거가 아니다. 최신 build/JAR/runtime 기록인 §26.14는 그 당시 STORE_HOME artifact의 역사적
증거로 유지되지만, 현재 automatic source가 포함된 새 artifact provenance를 증명하지 않는다.
따라서 이번 restoration에는 `BUILD_PASSED_CURRENT=false`,
`RUNTIME_PATH_PROVEN_CURRENT=false`, `artifactParity=PARITY_UNPROVEN`을 적용한다. Planner
`[64,32]` 문제도 automatic composition과 분리된 후속 change unit으로 남는다.

## 17. Terminal result

최소 terminal 의미는 다음을 구분한다.

```text
COMPLETED
PARTIAL_TRUSTED_CAPACITY_EXHAUSTED
PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE
NO_USABLE_TRUSTED_DESTINATION
NO_TRUSTED_CAPACITY
CURSOR_NOT_EMPTY
MANIFEST_STALE
CONTEXT_CHANGED
TRANSFER_UNCONFIRMED
INTERRUPTED
```

일부 저장 후 모든 상자가 가득 찬 경우의 사용자 메시지 예:

```text
집 정리 부분 완료: 아이템 14개를 저장했고 저장 대상 stack 3개가 남았습니다.
등록된 trusted storage에 더 이상 빈 공간이 없습니다.
```

아무것도 저장하지 못한 경우 inventory를 변경하지 않았는지 함께 보고한다.
capacity 부족과 missing/unreachable은 같은 `FAILED`로 합치지 않는다.

Chat/microphone bridge에서 generic Task `completed`를 저장 성공으로 해석하면 안 된다.
`matching_task_finished`는 lifecycle 종료 증거일 뿐 `StoreHomeResult.COMPLETED` 증거가
아니다. H6 public enable 전에 LAVI-owned Java command/bridge 경계가 최소한 다음
operation-specific data를 기존 `command_result.data`에 typed payload로 투영해야 한다.

```text
operation: store_home
store_home_result: COMPLETED | PARTIAL_* | NO_* | CURSOR_NOT_EMPTY |
                   MANIFEST_STALE | CONTEXT_CHANGED |
                   TRANSFER_UNCONFIRMED | INTERRUPTED
stored_items: non-negative integer
remaining_stacks: non-negative integer
reason: stable reason code
goal_satisfied: boolean
```

wire key와 enum은 바꾸지 않고 operation-level 의미를 다음처럼 고정한다.

```text
stored_items
    = 모든 trusted-container session에서 paired delta로 confirmed된 item 수량의 합

remaining_stacks
    = 이전 local manifest remaining의 합이 아님
    = 안전한 경우 latest/final fresh read-only plan의 현재 STORE_HOME step 수
    = 안전한 final snapshot이 불가능하면 마지막 authoritative reporting 값을 보존

goal_satisfied
    = store_home_result == COMPLETED
    AND pending transfer 없음
    AND (
        session 시작 전 authoritative advisory preflight의 STORE_HOME step 0
        OR latest authoritative session progress의 remaining STORE_HOME step 0
    )
```

Final read-only plan은 terminal reporting에만 사용할 수 있으며 click, retry,
same-GUI replan 또는 manifest refresh 권한이 아니다. `CONTEXT_CHANGED`,
`INTERRUPTED` 또는 `TRANSFER_UNCONFIRMED`처럼 안전한 snapshot을 만들 수 없는
terminal에서 `remaining_stacks`를 임의로 0으로 만들지 않는다.
아직 advisory 또는 session reporting baseline을 한 번도 얻지 못한 조기
`CURSOR_NOT_EMPTY`나 context rejection은 기존 terminal-specific non-negative
fallback을 유지할 수 있다. 그 fallback 0은 surplus가 0이거나 goal이 충족됐다는
증거가 아니며 `goal_satisfied=false`와 함께 해석한다.

기존 로그의 `touchedStacks`는 operation 전체에서 paired-delta confirmed transfer가
한 번 이상 있었던 distinct logical player slot 수로 유지한다. 같은 logical slot이
여러 session 또는 서로 다른 stack에 재사용돼도 operation당 한 번만 세며, local
`touchedStackCount`를 단순 합산하지 않는다.

`goal_satisfied=true`는 `store_home_result=COMPLETED`와 일관되어야 한다. partial,
no-capacity, no-destination, stale, context change, unconfirmed와 interruption을
성공 문구로 렌더링하지 않는다. typed projection이 없다면 사용자 응답은
"Task 종료 응답은 받았지만 실제 집 보관 결과는 확정하지 못했다"는 중립 의미를
사용한다.

`StoreHomeTask.toDebugString()`, 일반 로그 또는 사람이 읽는 message를 파싱해 결과를
복원하지 않는다. upstream ChatClef/AltoClef lifecycle을 변경하지 않고,
LAVI-owned `StoreHomeCommand`와 Fabric bridge result data 경계에서 연결한다.

## 18. Diagnostics

normal runtime에서는 경계와 terminal 중심의 bounded diagnostics만 사용한다.
매 tick 또는 매 slot unchanged-state logging은 금지한다.

최소 operation correlation 후보:

```text
operationId
gameTick
phase
topLevelTask
childTask
worldKey
dimension
trustedDestinationId
trustedPosition
containerSessionOrdinal
manifestActive
manifestRevision
logicalSourceSlot
stackFingerprintSummary
expectedCount
sourceDelta
destinationDelta
cursorState
candidateAttempt
elapsedTicks
terminalReason
```

diagnostics는 source selection, click, retry, timeout, fallback, Task completion 또는
cleanup behavior를 변경하지 않는다.

`manifestRevision`은 session-local이므로 `operationId`와
`containerSessionOrdinal` 없이 전역 identity로 해석하지 않는다.

2026-08-28 한국어 chat runtime에서 한 번 관찰된
`planned_fingerprint_changed:slot=8`과 당시 diagnostics-only 계약은 25절에 역사적
증거로 보존한다. 26절의 후속 lifetime 방향은 stale 판정과 fingerprint equality를
완화하지 않으며 reason-label/source-artifact 비정합을 별도 gate로 유지한다.

같은 날 장거리 timeout용 bounded diagnostics는 candidate start, sampled progress,
Task-owned timeout decision, 실제 candidate rejection과 operation terminal을 같은
operation ID로 연결했다. 세 후보 모두 timeout 순간 path/goal active와 recent movement를
보였고 candidate 2400 / operation non-match를 직접 기록했다. Operation progress detail은
24/64건, 전체 accepted diagnostic은 35/256건이었으며 7,173건의 반복 progress는
summary count로 억제됐다. 상세 event/field, artifact qualifier와 runtime 결과는
[ChatClef STORE_HOME Long-Distance Timeout Investigation](chatclef-store-home-long-distance-timeout-investigation-2026-08-28.md)이
소유한다.

## 19. V1 acceptance tests

최소 acceptance matrix는 다음과 같다.

| Scenario | Expected |
| --- | --- |
| inventory 36/36, 사용자 요청 없음 | STORE_HOME Task와 StoreHome-owned trusted 이동 0. 별도 automatic pressure subsystem은 §16/automatic 문서의 독립 matrix로 검증 |
| `@store_home` 입력 | 정확히 한 manual UserTask 생성 |
| 기존 `@get` 중 `@store_home` | 기존 UserTask 정상 교체, 자동 재개 없음 |
| command 수락 시 cursor stack 존재 | inventory mutation 없이 `CURSOR_NOT_EMPTY` |
| advisory preflight STORE_HOME step 0 | 같은 StoreHome root 안에서 `nothing_to_store`, 이동/open/click 0 |
| advisory preflight STORE_HOME step 1 이상 | preflight manifest 폐기, navigation 중 active manifest 0 |
| navigation 중 safety chain 일시 선점 뒤 context 동일 | active manifest 없이 같은 StoreHome root가 재검증 후 계속 |
| transfer 중 safety chain 일시 선점 뒤 manifest 동일 | 같은 local manifest를 strict revalidate한 뒤 계속 |
| transfer 중 safety chain 일시 선점 뒤 manifest 변경 | 추가 전송과 fresh replan 없이 `MANIFEST_STALE` |
| navigation 중 count/pickup/tool damage/reserve/slot 변화 | stale 없이 도착 fresh plan에 반영 |
| 좋은/낮은 내구도 diamond pickaxe | 좋은 exact slot 유지, 낮은 exact slot만 저장 |
| 같은 Item의 enchanted/plain stack | manifest가 선택한 exact slot만 이동 |
| manifest activation 후 source item/count/damage/metadata/slot 변경 | 대체/replan 없이 `MANIFEST_STALE` |
| manifest activation 후 empty/KEEP/armor/offhand/selected state 외부 변경 | same-session 편입/replan 없이 `MANIFEST_STALE` |
| 현재 armor와 미착용 armor | 현재 armor 유지, 미착용 exact slots 저장 |
| current offhand | 유지 |
| food 64 한 stack, SAFE reserve 16 | V1에서 stack 전체 유지 허용 |
| trusted A 일부 공간, B 충분 | A confirmed 누적 후 B exact GUI에서 fresh plan |
| trusted A full 또는 missing, pending 없음 | A local session 폐기 후 다음 후보 |
| A pending 중 trust/binding 상실 | `TRANSFER_UNCONFIRMED`, B 이동/replan 0 |
| 후보 snapshot 뒤 A를 untrust/disable | A를 열거나 전송하지 않고 제외 |
| operation 중 새 trusted 등록 | 현재 operation에는 합류하지 않고 다음 명령부터 사용 |
| trusted 모두 full, 일부 저장 | typed partial capacity terminal |
| trusted 없음 | inventory 불변, no-destination terminal |
| 가까운 일반 상자 존재 | 절대 사용하지 않음 |
| 다른 dimension에만 trusted 존재 | V1에서 사용하지 않음 |
| click 후 paired delta 불일치 | 성공 처리 금지 |
| 장거리 navigation 중 candidate counter가 기존 2400을 넘지만 recent actual progress 존재 | candidate 유지; absolute lifetime rejection 0 |
| path/goal active지만 movement와 best-distance improvement 모두 bounded window 동안 없음 | navigation no-progress로 candidate 제외 |
| 정상 우회 중 direct distance가 잠시 증가하지만 player movement 존재 | 즉시 timeout하지 않고 navigation 계속 |
| interaction neighborhood 진입 뒤 screen/binding이 bounded local budget 동안 없음 | local interaction timeout으로 candidate 제외 |
| operation counter가 기존 12000을 넘지만 active progress 존재 | 기존 absolute lifetime만으로 terminal 0 |
| navigation 중 safety-chain 선점 시간이 어떤 timeout window보다 김 | 모든 StoreHome-owned clock 미산입; resume 뒤 같은 attempt와 누적 clock 계속 |
| path/goal recalculation/re-adoption/재제출, sub-threshold position jitter 또는 epsilon 미만 distance 변화만 반복 | semantic progress reset 0; bounded no-progress 뒤 candidate 제외 |
| candidate 교체만 발생하고 실제 movement/distance improvement 없음 | operation no-progress/emergency elapsed와 confirmed metrics/failure history reset 0; local clock은 handoff 전 UNSTARTED |
| interaction-neighborhood 경계 진동, screen/handler/binding flicker, 반복 click 또는 path/goal update | 같은 attempt의 local elapsed silent reset 0; navigation 복귀 시 누적 elapsed 보존 또는 명시적 새 attempt generation |
| candidate 또는 operation timeout 때 pending 결과 불확실 | `TRANSFER_UNCONFIRMED` 우선; 다음 candidate/replan/추가 click 0 |
| emergency hard cap 도달 | `operation_emergency_hard_cap` operation-level terminal; candidate rejection/unusable 판정으로 오용 0; pending precedence 유지 |
| phase별 네 timeout 경로 | `candidate_navigation_no_progress`, `candidate_local_interaction_timeout`, `operation_no_progress`, `operation_emergency_hard_cap`을 서로 다른 stable reason으로 보존 |
| 모든 candidate가 실제 phase-scoped failure | 기존 terminal precedence와 `NO_USABLE_TRUSTED_DESTINATION` 의미 유지; IdleTask 전환 1회 |
| matching BlockInteractEvent 없음/중복/다른 target 또는 TAIL 뒤 event | GUI_BOUND 0; event consume 최대 1회; open-wait refresh 0 |
| TAIL candidate 뒤 open child가 아직 tickable 또는 cleanup 미완료 | GUI_BOUND/slot mutation 0; parent의 정상 quiescence 뒤 재검증 |
| child quiescence 중 screen/handler/world/operation 변경 | candidate 폐기; consumed event는 attempt에서 영구 spent이며 재사용 0 |
| chest/trapped chest의 exact screen/handler mismatch | GUI_BOUND 0; broad ContainerType fallback 0 |
| Barrel/shulker 또는 unlisted container | GUI gate state 생성 0; 기존 route behavior 유지 |
| opening/stale/duplicate boundary serial | count 0; `lastCountedBoundarySerial` 불변 |
| 서로 다른 later boundary #1/#2 | full validation 뒤 count만 증가; 전체 reachable mutation 0 |
| later boundary #3 | one-time permission만 available; QUICK_MOVE 0 |
| 다음 정상 Task evaluation의 full revalidation 실패 | permission 폐기; activation snapshot/manifest/click 0 |
| 다음 정상 Task evaluation의 full revalidation 성공 | permission consume + activation/기존 QUICK_MOVE lifecycle 진입을 logical commit 1회로 수행 |
| permission은 valid하지만 activation/transfer lifecycle 진입 생성 실패 | permission consume 0; typed invalidation/terminal; slot action 0 |
| 같은 permission의 재진입/두 번째 consume | 거절; 추가 slot action 0 |
| parent/former child/cleanup/fallback/super path | stabilization 동안 slot/cursor/screen/interact/input/Baritone mutation 0 |
| H6 chat/microphone adapter | admission 이후 command와 같은 Task factory 및 policy |
| 기존 `@deposit_all`, `@deposit`, `@get` | 기존 behavior 유지 |

GUI gate의 bounded logs/tests는
[Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md#canonical-exact-gui-gate-log-fields)의
canonical 이름을 그대로 사용한다. 최소한 `operationId`, `openAttemptId`, `correlationId`,
`matchingBlockInteractEventObserved`, `matchingBlockInteractEventCount`,
`matchingBlockInteractEventConsumed`, `duplicateBlockInteractEventRejected`, `openChildIdentity`,
`openChildQuiescent`, `openChildCleanupComplete`, `screenTypeExpected`, `screenTypeActual`,
`screenTypeMatched`, `handlerTypeExpected`, `handlerTypeActual`, `handlerTypeMatched`,
`screenObjectIdentity`, `handledScreenHandlerIdentity`, `playerHandlerIdentity`, `capturedSyncId`,
`liveSyncId`, `candidateClientTickSerial`, `boundClientTickSerial`, `clientTickBoundarySerial`,
`lastCountedBoundarySerial`, `candidateTickExcluded`, `boundPromotionTickExcluded`,
`sameBoundaryDuplicateSuppressed`, `stableLaterBoundaries`, `fullGuiBoundPredicate`, `guiInputAllowed`,
`slotMutationSuppressed`, `reachableMutationPath`, `reachableMutationKind`,
`suppressedMutationOwner`, `permissionAvailable`, `permissionFullRevalidationPassed`,
`permissionConsumed`, `permissionReuseRejected`, `transferLifecycleEntryCommitted`, `slotActionOwner`, `slotActionType`와
`slotButton`을 같은 attempt로 연결한다. Unchanged tick/slot polling log는 추가하지 않는다.

Core focused tests, clean forced build, deployed JAR hash와 direct-command happy path
검증은 23절과 같이 완료됐다. 아직 runtime으로 확인하지 않은 core failure/fallback
scenario와 H6 microphone/input-safety/busy runtime gate는 각각 별도 승인 뒤 같은 build
runbook과 runtime evidence 기준을 다시 적용한다.

위 historical core 검증만으로 26절의 session-local lifetime source를 검증한 것은
아니었다. 이후 current source의 clean build, 동일 JAR 배포와 2026-08-29 runtime
재현에서 exact activation, capacity fallback과 paired-delta transfer가 확인되어 observed
happy-path acceptance는 닫혔다. 상세 regression contract는 26.10절에 유지한다.

Phase-scoped timeout의 window/predicate와 focused test source도 구현·실행됐고 clean
forced build 및 실제 장거리 성공까지 확인됐다. 다만 실제 no-progress/local interaction/
operation no-progress/emergency hard-cap 발동, 장시간 safety-chain 선점과 pending-timeout
click 0회 matrix는 아직 runtime으로 모두 재현한 것이 아니다.

## 20. H1-H4 core V1 범위 제외

- inventory-pressure automatic trigger
- high-water/low-water/hysteresis
- automatic working-set recovery
- 일반 상자 fallback
- 발견한 상자의 자동 trusted 승격
- persistent trusted area
- cross-dimension home travel와 portal 탐색
- 새 trusted 상자 제작과 배치
- best armor 자동 교체
- elytra와 chestplate 자동 선택
- 범용 enchantment 가치 점수화
- cursor 기반 정밀 partial-stack split
- 새 pin command, pin JSON 또는 별도 pin UX
- nearby batch registration UX(H5 fixed-volume 별도 범위)
- chat와 microphone intent adapter 구현(H6 별도 범위)
- home storage retrieval
- generic `StoreInContainerTask` source-selection 변경
- shared Task, TaskRunner, UserTaskChain 또는 Baritone 변경
- Forge/MineMind 구현, placeholder, config 또는 test

## 21. V1 확정 기본값

1회 방향성 검수에서 다음 기본값을 확정한다.

1. survival reserve: profile UX 없이 `SAFE`
2. manual home destination 거리: 같은 worldKey와 같은 dimension의 모든 enabled
   destination, 절대 거리 상한 없음, 거리순 시도, navigation no-progress와
   local interaction timeout의 phase-scoped failure
3. V1 armor: 현재 착용 armor를 그대로 유지하고 미착용 armor만 저장

이 세 항목 외에 behavior 구현 전에 추가 사용자 정책 결정을 요구하지 않는다.
정확한 no-progress window, local timeout, interaction-neighborhood predicate, emergency
hard cap, movement jitter threshold와 distance epsilon의 숫자는 source audit과 focused
test에서 최소화할 구현 매개변수이며 방향성 재검수 gate가 아니다. 다만
active-root-tick clock basis, semantic progress에만 reset, candidate switch 비진행 처리,
local-interaction timer의 silent reset 금지와 phase별 stable reason 구분은 숫자 선택이
아니라 구현 전 필수 계약이다. explicit request-only, exact trusted-only, no general
fallback과 exact-slot source selection은 확정된 핵심 방향으로 취급한다.

## 22. 구현 slice와 승인 경계

V1 core는 다음 순서로 제한한다.

```text
H0  documentation and source audit
H1  automatic entrypoint disable contract and no-request tests
H2  StoreHome request/command and loadout/manifest unit tests
H3  exact logical-slot resolver and transfer executor
H4  sequential trusted destination integration and terminal results
H7  clean build, JAR deployment and Minecraft runtime reproduction
```

다음은 V1 core가 runtime에서 검증된 뒤의 별도 후속 범위다.

```text
H5  fixed 16x16x16 exact-container batch registration와 Korean batch-only direct @ forms
H6  chat and microphone intent adapters
```

이 문장의 slice별 승인 요구는 H1-H7 역사 실행과 H5/H6 자체 범위의 provenance다. 현재 exact
GUI stabilization source/test/required clean build에는 문서 서두의 continuous override가 우선한다.
H1-H4 source, focused tests와 H7의
clean build/JAR 배포/단일 destination happy path 검증은 별도 승인에 따라 완료됐다.
H5와 H6는 H4/H7의 완료 조건에 포함하지 않는다. H5 pre-change 계약은 13절에서
2026-09-02 docs-only로 보강됐다. 현재 dirty worktree에는 별도 H5 Java/test source가 존재하지만
이 문서에서 검증된 H5 구현이나 build/runtime 완료로 승격하지 않는다. H6 방향은 24절에서 승인됐고,
이후 별도 사용자 승인에 따라 Python natural-language source와 LAVI-owned typed
terminal projection source가 현재 worktree에 구현됐다. 이 source 승인은 clean build,
runtime reproduction, public enable, commit 또는 push를 승인하지 않는다.

26절의 trusted-container-session-local manifest lifetime은 기존 H1-H4/H7 검증 이후의
별도 change unit이다. 이 문장은 26절 방향을 처음 작성했을 때의 docs-only 승인 이력을
기록한다. 현재 dirty source 존재와 미검증 상태는 23절 상태표가 별도로 소유한다. 이번
사용자 승인은 현재 문서 갱신에만 적용되며 추가 Java/test source 변경, test 실행,
clean build, 배포와 runtime reproduction을 승인하지 않는다.

## 23. 역사적 구현·검증 snapshot과 현재 dirty-worktree qualifier

아래 표의 build/test/JAR 수치는 2026-08-29 core/runtime artifact에 2026-09-02 H5 docs-only
qualifier를 덧붙인 역사적 snapshot이다. 이 표의 `243 TESTS`와 direct-test `NOT IMPLEMENTED`를
현재 최신 수치로 읽지 않는다. 이후 direct gates와 최신 기록 build는 §26.14의 `254 tests`가
대체하며, 현재 dirty H5 source와 향후 GUI-gate source는 어느 역사적 artifact로도 검증되지 않았다.

```text
exact trusted destination repository:            IMPLEMENTED; RUNTIME REGISTRY OBSERVED
trust / untrust / trusted_list commands:          IMPLEMENTED; RUNTIME COMMANDS OBSERVED
sequential manual trusted candidate execution:    IMPLEMENTED; ONE-CHEST HAPPY PATH VERIFIED
@store_home command:                              IMPLEMENTED; OPERATIONS 225/30351 RUNTIME OBSERVED
manual StoreHomeTask:                             IMPLEMENTED; TWO COMPLETED OPERATIONS OBSERVED
exact-slot manifest and executor:                 IMPLEMENTED; PAIRED DELTA VERIFIED
operation-wide early manifest lifetime:           HISTORICAL DEPLOYED BASELINE STRUCTURE
trusted-container-session-local manifest:         CLEAN BUILD/DEPLOY + ACTIVATION/PAIRED-DELTA RUNTIME VERIFIED
H5 fixed-volume batch registration:               2026-09-02 DIRECTION REVIEWED; CURRENT DIRTY JAVA/TEST SOURCE PRESENT; UNVERIFIED; EXECUTION PROHIBITED
chat/microphone STORE_HOME adapters:              SOURCE IMPLEMENTED; PUBLIC LIVE VALIDATION ENABLED
STORE_HOME Python intent/compiler/registry:        IMPLEMENTED; STORE_HOME CONTRACTS PASS; deposit_all CATALOG/REGISTRY PARITY BLOCKED
typed StoreHomeResult bridge projection:          BUILD/JUNIT AND CHAT RUNTIME VERIFIED
automatic manual-policy disable (historical H1):  IMPLEMENTED; UNIT/BUILD VERIFIED
automatic pressure current source:                RESTORED/ACTIVE; EVIDENCE OWNED BY §16 AND AUTOMATIC DOC
focused 1.20.1 Gradle tests (historical snapshot): PASS FOR EXECUTED ARTIFACT; 243 TESTS, 0 FAILURES, 0 ERRORS, 14 SKIPPED
clean forced Gradle build and deployed JAR (historical snapshot): VERIFIED
Minecraft runtime STORE_HOME reproduction:        OP225 >2400 DIRECT; OP30351 REPEAT COMPLETION; COMPLETE MATRIX OPEN
H6 matching-request typed terminal runtime:        VERIFIED THROUGH KOREAN CHAT
H6 Korean chat runtime:                            TWO COMPLETED AFTER ONE SAFE MANIFEST_STALE
H6 microphone runtime parity:                      NOT YET VERIFIED
H6 input safety and busy runtime matrix:            NOT YET VERIFIED
multi-destination/failure runtime matrix:          CAPACITY FALLBACK VERIFIED; COMPLETE MATRIX OPEN
transient MANIFEST_STALE exact mismatch cause:     UNPROVEN; HISTORICAL DIAGNOSTICS PLAN ONLY
count-only/reason-label artifact parity:           UNRESOLVED; SEPARATE EVIDENCE GATE
long-distance candidate timeout during active progress: PROVEN; DIRECT RUNTIME BOUNDARY OBSERVED
timeout/progress boundary diagnostics:              IMPLEMENTED; CLEAN BUILD/DEPLOYED FILE/RUNTIME EVENTS VERIFIED; COMPLETE SOURCE PARITY UNPROVEN
phase-scoped timeout behavior direction:            SOURCE/JUNIT/BUILD + OP225 OBSERVED >2400 HAPPY PATH VERIFIED
phase-scoped timeout focused JUnit:                  PASS IN CLEAN FORCED BUILD; NEW DIRECT SEMANTIC GATES NOT INCLUDED
StoreHomeTaskLifecycleController direct order test (at this snapshot): NOT IMPLEMENTED
StoreHomeTask shared state/timeout identity test (at this snapshot):    NOT IMPLEMENTED
submitted-root/termination/projector identity test (at this snapshot): NOT IMPLEMENTED
operation absolute timeout risk after candidate fix: NO-PROGRESS + EMERGENCY CAP IMPLEMENTED; FIRING MATRIX RUNTIME OPEN
UserBlockRangeTracker null-path NPE:                RUNTIME OBSERVED; SECONDARY CONTRIBUTION UNPROVEN
recurring JVM G1 Remark native crash:              SEPARATE INVESTIGATION; ROOT CAUSE UNPROVEN
```

§26.14 owns the later direct-test and 254-test clean-build record. Neither that
record nor this §23 snapshot covers the current independent dirty H5 source or a
future GUI-gate implementation. A build that consumes both must be reported as
mixed H5+GUI provenance, not as a GUI-only or H5-only verification result.

검증된 1.20.1 build JAR과 active CurseForge instance JAR은 모두 다음 SHA-256을
가졌다.

```text
file: chatclef-1.20.1-0.18.23.jar
size: 6,847,306 bytes
sha256: 8C6ECA6C29568094D7F4DA25D3D2C660DF914E8F93CD893A20E437A5DA2F83DD
```

H6 typed projection source를 포함한 후속 clean forced build와 active instance 배포
artifact는 다음과 같다.

```text
file: chatclef-1.20.1-0.18.23.jar
size: 6,853,544 bytes
sha256: D2ABB0C13186D825170707D826CC1023B892F0EF5A1038B8122853868393B61F
build: BUILD SUCCESSFUL; 171 actionable tasks executed
```

Session-local manifest source와 bounded STORE_HOME timeout diagnostics를 포함한 후속 clean
forced build, active instance 배포와 runtime code-source file identity는 다음과 같다.

```text
file: chatclef-1.20.1-0.18.23.jar
size: 7,007,701 bytes
sha256: B5C63F0D5D8D3043151A10107BB5C7CB213188651483F62CCFD6E1F147496A55
build: .\gradlew.bat clean build --rerun-tasks -> BUILD SUCCESSFUL
runtime: Fabric 1.20.1 launch and bounded STORE_HOME events observed
```

Build output과 active instance JAR의 file-level SHA-256은 일치했다. Runtime manifest는
repository/source/build-input identity가 완전히 공급되지 않아
`artifactParity=PARITY_UNPROVEN`을 유지했다. 따라서 exact current source provenance를
완전히 증명했다고 확대하지 않지만, 같은 runtime operation이 직접 출력한 counter,
decision, rejection과 terminal은 그 실행의 behavior evidence로 사용한다.

2026-08-29 최종 folderization과 phase-scoped timeout source를 포함한 clean forced build와
active instance artifact는 다음과 같다.

```text
command: .\gradlew.bat clean build --rerun-tasks
result: BUILD SUCCESSFUL in 2m 45s
tasks: 171 actionable tasks, 171 executed
1.20.1 JUnit XML: 243 tests, 0 failures, 0 errors, 14 skipped

file: chatclef-1.20.1-0.18.23.jar
size: 7,113,261 bytes
built/deployed SHA-256:
C939E5A859BB66535FADCB5522A4D9CD99AF2CAC7E90B6668A2945CA71AA7933
runtime code-source: active LAVI_TEST_Fabric01 mods JAR path observed
runtime manifest parity: PARITY_UNPROVEN
```

source JAR과 active CurseForge instance JAR의 크기와 SHA-256은 일치했고, 해당
launch에서 ChatClef 관련 MixinApplyError, InvalidMixin, InjectionError,
NoSuchMethodError 또는 NoSuchFieldError는 발견되지 않았다.

runtime에서는 세 trusted destination이 같은 world/dimension에 enabled 상태로
등록된 것이 확인됐다.

```text
-745, 69, 60
-746, 70, 59
-746, 70, 60
```

직접 `@store_home` happy path는 첫 destination을 열고 exact transfer를 수행해 다음
terminal을 기록했다.

```text
result=COMPLETED
storedItems=33
touchedStacks=32
remainingStacks=0
reason=paired_delta_confirmed
```

같은 배포 artifact에서 추가 direct command 실행도 모두 `COMPLETED`와
`remainingStacks=0`을 기록했다.

```text
storedItems=96,  touchedStacks=32
storedItems=455, touchedStacks=26
storedItems=11,  touchedStacks=11
```

사용자 확인 결과 주력 곡괭이, 도끼, 삽, 검과 reserve는 인벤토리에 정상 유지됐다.

이 증거는 direct command의 core 저장 동작을 검증한다. 당시 direct in-game
command에는 active LAVI command context가 없었으므로 bridge가 해당
TaskFinishedEvent를 무시한 것은 예상된 동작이다. 이후 H6 한국어 chat과
matching-request typed terminal은 23.2절에서 별도로 검증됐다.
full/missing/unreachable 후보
fallback, runtime cursor rejection, safety-chain resume, untrust during operation과
cross-dimension rejection은 focused tests 또는 source audit 근거만 있으며 전체
runtime acceptance matrix는 남아 있다.

구현은 commit `740aa61683ad4cab1f7b3d4dff3e2c86befec983`으로 기록되어
`minecraft-plugin-fix/alto-clef-infinite-loop` branch에 push됐다. 이 기록은 이후
변경의 commit 또는 push를 자동 승인하지 않는다.

### 23.1 JVM native crash 조사 분리

ChatClef multi-version project의 문서상 reference build SDK는 Temurin JDK 21이다.
이는 baseline/current artifact를 같은 환경에서 재현하기 위한 build SDK 표준이며,
Minecraft 1.20.1 JAR이 Java 21 runtime을 요구한다는 뜻이 아니다. 현재
`build.gradle`에서 Minecraft 1.20.1의 `sourceCompatibility`, `targetCompatibility`와
`JavaCompile.options.release`는 모두 Java 17이다.

```text
Temurin JDK 21로 Gradle 8.8 실행
    -> Minecraft 1.20.1을 --release 17로 compile
    -> Java 17 대상 JAR 생성
    -> Minecraft는 Java 17 runtime으로 실행
```

baseline/current JAR A/B의 current arm은 crash 회차에 실제 배포됐던 exact JAR
`D2ABB0C13186D825170707D826CC1023B892F0EF5A1038B8122853868393B61F`을 보존해 사용한다.
baseline arm은 별도 clean worktree에서 current artifact를 만들 때 사용한 것과 동일한
Temurin JDK 21 vendor와 exact patch, 동일 Gradle wrapper와 동일 build command로 생성한다.
artifact manifest에는 build JDK, Gradle version, JAR SHA-256과 Java 17 class target 증거를
남긴다. fresh current rebuild는 첫 A/B 전에 crash-run JAR을 대체하지 않으며, 나중에
수행한다면 reproducibility를 확인하는 별도 세 번째 artifact로 취급한다. 현재 dirty
worktree를 직접 checkout하거나 A/B 중간에 build SDK를 바꾸지 않는다.

H6 source를 포함한 clean forced multi-version build는 다음 evidence를 남겼다.

```text
JUnit XML aggregate: 1760 tests, 0 failures, 0 errors, 42 skipped
build/deployed JAR SHA-256:
D2ABB0C13186D825170707D826CC1023B892F0EF5A1038B8122853868393B61F
relevant Mixin/linkage signatures: 0
```

23절의 `156 TESTS, 0 FAILURES, 14 SKIPPED`는 focused 1.20.1 test 집계이고,
위 `1760 tests, 42 skipped`는 clean multi-version build의 전체 JUnit XML 집계다.
두 수치는 범위가 다르며 충돌하지 않는다. 이 evidence는 artifact identity와 build/test
성공을 증명하지만 JVM native crash가 발생할 수 없다는 증거로 확대하지 않는다.

2026-08-27 runtime은 직접 `@store_home` 완료 약 10.6초 뒤 다음 native signature로
종료됐다.

```text
EXCEPTION_ACCESS_VIOLATION
GCTaskThread
jvm.dll
G1PauseRemark
invalid read 0xC0
```

같은 broad crash family의 hs_err 기록 네 건이 H6와 current clean build보다 먼저
존재한다. 따라서 현재 evidence는 H6 source, typed projection 또는 clean build artifact가
이 crash family의 root cause라는 주장을 지지하지 않는다.

해당 회차의 한국어 `STORE_HOME` 입력 네 번은 모두 `result_status=rejected`,
`minecraft_command_routed=0`이었으므로 Java `StoreHomeTask`와 matching-request typed
projector를 실행하지 않았다. 다만 별도의 두 번째 직접 `@store_home`은 20:51:19에
시작해 20:51:27에 `COMPLETED`, `storedItems=180`, `touchedStacks=31`로 끝났고 약
10.6초 뒤 crash가 발생했다.

```text
H6 또는 clean build가 crash family의 root cause:
    NOT SUPPORTED

direct StoreHome workload가 해당 회차에서 기존부터 관찰된 crash family를 노출한 trigger:
    POSSIBLE; A/B PENDING
```

시간상 인접성만으로 trigger를 확정하지 않는다. operation의 `COMPLETED`는 해당 transfer의
기능 증거로 유지하지만 Minecraft JVM process의 장기 안정성까지 검증했다는 뜻은 아니다.

동일 current source를 다시 build하거나 Java source logging을 추가하기 전에 다음
JVM-level evidence를 먼저 확보한다.

```text
complete hs_err
Windows user-mode dump: full dump 우선, 불가능하면 minidump
정확히 대응하는 javaw.exe / jvm.dll version과 SHA-256
GC / concurrent marking / Remark / safepoint log
run manifest:
    test arm과 run ID
    Java vendor와 exact patch
    complete JVM flags, collector와 Xmx
    JAR SHA-256, mod/native module inventory
    world/inventory snapshot ID
    StoreHome 시작/종료 timestamp와 terminal
    관찰된 Remark exposure count
```

첫 재현은 가능하면 crash를 냈던 current JAR과 exact Java 17 runtime을 유지한 채 위
증거를 확보한다. 같은 current source의 반복 build는 이미 build/deployed SHA-256 identity가
확인됐으므로 우선순위가 낮다.

JVM evidence 확보 뒤 A/B는 다음 순서를 사용한다.

```text
1. commit 740aa616 baseline JAR 대 current H6 JAR
2. 동일 current JAR에서 StoreHome 실행 없음 대 직접 @store_home 두 번 실행
3. 동일 vendor의 현재 Java 17 patch 대 최신 지원 Java 17 patch
4. Xmx125728m 대 Xmx16g
5. G1 대 ParallelGC
6. diagnostics off 대 boundary
7. native injection을 한 항목씩 순차 격리
8. mod screening과 CPU/RAM 안정성 조사를 별도 phase로 수행
```

모든 A/B는 선언한 변수 하나 외의 JAR, runtime, JVM flags, evidence-capture 설정,
mod/native 구성, world/inventory snapshot과 workload를 동일하게 유지한다. 각 단계는
명시적으로 고정한 reference configuration을 사용하며, 한 단계에서 바꾼 값을 다음 단계에
자동 누적하지 않는다. G1을 사용하는 arm은 elapsed time만 비교하지 않고 Remark exposure
수도 함께 기록하며, exposure가 부족하면 no-crash를 반증으로 사용하지 않고
`INCONCLUSIVE`로 둔다. G1/ParallelGC collector A/B에서는 ParallelGC에 G1 Remark가 없는
것이 정상인 만큼 동일 workload, 관찰 시간과 collector별 GC-cycle evidence로 비교한다.
Java patch A/B에서는 vendor를 함께 바꾸지 않는다. mod binary search는 screening에만
사용하고 최종 판정은 단일 mod 제거 A/B로 확인한다. BIOS, microcode, XMP, CPU와 RAM은
하나의 변수로 묶지 않는다.

primary baseline/current와 workload A/B arm에는 새 Java source logging을 넣지 않는다.
새 log는 allocation과 GC timing을 바꿀 수 있다. 기존 boundary log, JVM log와 external
run manifest로 먼저 판정하고, 그래도 부족할 때만 source logging을 후속 별도 A/B 변수로
추가한다.

상세 confidence 분류, 반복 횟수와 arm별 판정 기준은
[ChatClef JVM G1 Remark Native Crash Investigation](chatclef-jvm-g1-remark-native-crash-investigation-2026-08-27.md)이 소유한다.

### 23.2 H6 한국어 chat runtime evidence

2026-08-28 사용자가 `public_korean_enabled=true`를 명시적으로 승인하고 LAVI를
재시작한 뒤, 같은 한국어 STORE_HOME 요청을 LAVI chat 경계로 세 번 제출했다. 각
입력은 독립 request ID를 가졌고 outer `command_accepted`와
`command_send_succeeded`는 요청당 각각 한 번만 관찰됐다.

| Request ID | Typed result | Stored items | Remaining stacks | Reason |
| --- | --- | ---: | ---: | --- |
| `lavi-input-ko-94619ab5e1024424bc1be631732b6141` | `MANIFEST_STALE` | 0 | 31 | `planned_fingerprint_changed:slot=8` |
| `lavi-input-ko-39c20092d9d1421d82cc9e4b179c502a` | `COMPLETED` | 180 | 0 | `paired_delta_confirmed` |
| `lavi-input-ko-e70c2184f69840febc907b1348fc0e76` | `COMPLETED` | 180 | 0 | `paired_delta_confirmed` |

첫 요청은 약 0.139초 뒤 stale로 안전 종료됐다. 두 번째 요청은 약 7.9초, 세 번째
요청은 약 8초 안에 완료됐다. 첫 실패를 자동 retry하거나 같은 request를 replay한
것이 아니라 사용자가 각각 새 입력을 제출했다. 따라서 이 증거는 다음을 확인한다.

```text
Korean deterministic STORE_HOME route: runtime observed
prefixless command bridge submission: one per request
matching-request typed terminal: runtime observed
trusted exact-slot transfer and paired delta: completed twice
automatic retry/replay: not observed
```

사용자 화면 확인에서도 여분 아이템은 trusted 상자에 저장됐고, 선택된 주력 곡괭이,
도끼, 삽, 검과 음식 reserve는 인벤토리에 남았다. 해당 실행 구간에서 Minecraft
`ERROR`/`FATAL` 또는 LAVI `ERROR`/`CRITICAL`은 관찰되지 않았다.

이 검증은 Korean chat happy path와 typed bridge까지만 증명한다. Microphone parity,
모호·부정·질문·유예 입력의 submission 0, busy, trusted fallback과 전체 failure matrix는
아직 증명하지 않는다. Chatbot 화면은 즉시 제출 응답만 표시했고 typed terminal의 최종
사용자 문구는 표시하지 않았으므로, terminal UI 연결은 별도 후속 범위다. 25절의
`MANIFEST_STALE` 진단 보강에 renderer나 Chatbot UI 변경을 섞지 않는다.

### 23.3 2026-08-28 장거리 `@store_home` candidate-timeout 조사

#### 23.3.1 초기 간접 증거

첫 matching request에서는 세 trusted destination ID가 로그에서 순차 관찰됐지만
container session은 활성화되지 않았고 약 360.1초 뒤 종료됐다.

```text
requestId:     lavi-input-ko-d85048c34d25438fafb4b126e9bcb0d3
correlationId: lavi-1eff03236c504552af685948b17e02f6
root start:         clientTickId=229087
terminal:           clientTickId=236289
clientTickId delta: 7202 ticks
result:        NO_USABLE_TRUSTED_DESTINATION
reason:        trusted_candidates_exhausted
storedItems:   0
touchedStacks: 0
remaining:     22 stacks
```

당시 read-only current dirty source의 `MAX_CANDIDATE_TICKS=2400`과 candidate 수 3을
대조하면
`2400 * 3 = 7200`이며 root 시작/terminal의 elapsed `clientTickId`와 2 tick만 차이
난다. 약 120.6초와 241.0초 지점에는 각각 다음 destination이 처음 관찰됐고, 세 후보
모두 sampled state가 `NAVIGATE_AND_OPEN`, `session=-1`이었다. 따라서 candidate
lifetime budget의 순차 소진 가설은 이번 terminal의 가장 강한 source-consistent 선행
설명이었다.

이 초기 log에는 Task-owned candidate counter 또는 후보별
`rejectionReason=candidate_timeout` event가 직접 없고 incident JAR과 current dirty
source의 complete parity도 미확정이다. Candidate ID의 순차 관찰은 candidate switch를
간접 지지하지만 후보별 start/reject decision event를 대체하지 않았다. 따라서 이
시점의 문서 판정은 `STRONGLY SUPPORTED`였고 direct timeout observation으로 확대하지
않았다.

#### 23.3.2 Confirmatory diagnostics runtime

후속 diagnostics-only source, clean forced build와 동일 JAR 배포가 각각 별도 승인으로
진행된 뒤 다음 matching request를 통제 재현했다.

```text
requestId:      lavi-input-ko-15758f4811b54e83a9d0949dab5ce117
correlationId:  lavi-1a9a2be198f5400ca649bc544e0d4745
operationId:    3781
runManifestId:  store-home-runtime-02563f05-02a4-487e-aadb-0dd010b8d78f
diagnostics:    BOUNDARY
```

| Candidate | Destination / target | Start | Timeout | Counter decision | Recent progress |
| ---: | --- | --- | --- | --- | ---: |
| 1 | `td_1bf...` / `-745,69,60` | `650,69,467`, distance² `2,111,674` | `488,62,260`, distance² `1,560,338` | candidate 2400=true, operation 2400=false | 12 ticks 전 |
| 2 | `td_085...` / `-746,70,60` | `488,62,260`, distance² `1,562,820` | `282,62,191`, distance² `1,074,009` | candidate 2400=true, operation 4800=false | 2 ticks 전 |
| 3 | `td_442...` / `-746,70,59` | `282,62,191`, distance² `1,074,272` | `88,62,94`, distance² `696,845` | candidate 2400=true, operation 7200=false | 1 tick 전 |

세 `STORE_HOME_CANDIDATE_TIMEOUT_DECISION`은 모두 기존 branch side effect 전에
`candidateTimeoutEvaluated=true`, `candidateTimeoutConditionMatched=true`,
`operationTimeoutEvaluated=true`, `operationTimeoutConditionMatched=false`를 기록했다.
이어지는 `STORE_HOME_CANDIDATE_REJECTED`는 실제 `candidate_timeout` 제거와 다음 후보 또는
queue exhaustion을 확인했다.

세 timeout 순간 모두 다음 상태였다.

```text
phase=NAVIGATE_AND_OPEN
baritonePathingActive=true
customGoalActive=true
goalMatchesCandidatePosition=true
screenName=none
exactBindingEverObserved=false
supportedHandlerEverObserved=false
containerSessionState=NO_ACTIVE_SESSION
pendingTransfer=false
```

마지막 candidate도 timeout 직전 tick에 이동과 best-distance improvement가 있었고 target까지
3차원 직선거리 기준 약 835 blocks가 남아 있었다. 따라서 candidate는 local interaction에
실패한 것이 아니라 계속 navigation 중이었다.

```text
operationTicks:   7201
result:           NO_USABLE_TRUSTED_DESTINATION
reason:           trusted_candidates_exhausted
storedItems:      0
touchedStacks:    0
remainingStacks:  23
bridge elapsed:   about 360.145 seconds
candidate activated events: 0
```

Terminal 뒤 matching TaskFinishedEvent가 한 번 발행되고 `IdleTask`로 정상 전환됐다.
이번 실행에는 Mixin/FATAL/Minecraft crash evidence가 없고 runtime은 terminal 뒤에도
계속 기록됐다. 따라서 다음 판정을 확정한다.

```text
direct candidate timeout cause: PROVEN
progress at timeout: PROVEN
Baritone stall as direct cause: NOT SUPPORTED
exact binding failure: NOT REACHED
operation timeout: FALSE IN OBSERVED RUN
IdleTask transition: NORMAL POST-TERMINAL LIFECYCLE
```

Build output과 active instance JAR은 7,007,701 bytes와 SHA-256
`B5C63F0D5D8D3043151A10107BB5C7CB213188651483F62CCFD6E1F147496A55`로 일치했다.
Runtime manifest 자체는 complete repository/source/build-input identity가 없어
`PARITY_UNPROVEN`을 유지했다. 이 qualifier는 current dirty source의 exact provenance를
추정하지 못하게 하지만, 같은 runtime operation이 직접 기록한 counter/decision/rejection을
그 실행의 behavior evidence로 사용하는 것을 막지 않는다.

#### 23.3.3 `@goto` 차이와 reviewed behavior direction

`@goto -745 60`은 `GetToXZTask`와 `GoalXZ`를 사용해 X/Z 일치만 완료 조건으로
본다. 같은 X/Z target의 자연 완료는 서로 다른 실행에서 601.682초, 881.165초와
980.684초 뒤 관찰됐다.
반면 `@store_home`은 후보마다 `InteractWithBlockTask`를 사용하고 parent가 실제
screen/handler와 exact trusted binding을 요구한다. 따라서 두 명령은 동일 completion
contract의 직접 A/B가 아니다. 그러나 confirmatory run에서는 stricter binding 조건을
검사하기 전에 parent `StoreHomeTask`의 candidate timeout이 먼저 발동했다. 따라서 이번
장거리 실행에서 두 명령 결과를 가른 직접 차이는 StoreHome-owned fixed candidate
lifetime이다.

최종 behavior 방향 검수 결과는 `PASS`다. Candidate timeout을 단순 상향하거나 제거하지
않고 다음처럼 phase-scoped 의미로 분리한다.

```text
NAVIGATE_TO_CANDIDATE
    -> recent actual movement OR best-distance improvement 기반 no-progress timeout

OPEN_AND_BIND_CANDIDATE
    -> interaction neighborhood 진입 뒤 bounded local interaction timeout

operation
    -> active progress를 자르지 않는 no-progress boundary
    -> 별도의 충분히 큰 emergency hard cap
```

Path/goal active flag만으로 progress를 인정하지 않고 actual movement와 best-distance를
함께 본다. 정상 우회 중 direct distance가 잠시 증가해도 actual movement가 있으면 즉시
stall로 판정하지 않는다. `MAX_OPERATION_TICKS=12000`은 이번 실행의 직접 원인이 아니지만
candidate timeout 수정 뒤 다음 장거리 제약이 될 수 있으므로 같은 의미 분리가 필요하다.
정확한 window, interaction-neighborhood predicate, local timeout과 emergency cap 숫자는
focused behavior tests와 별도 source 승인 전에 정한다.

초기 간접 회차(`requestId=lavi-input-ko-d85048c34d25438fafb4b126e9bcb0d3`,
`correlationId=lavi-1eff03236c504552af685948b17e02f6`) 중
`USER_BLOCK_RANGE_NULL_INPUT_OBSERVED` 뒤
`UserBlockRangeTracker.updateState`의 null dereference와 Baritone pathing exception이
한 번 관찰됐다. 이는 실제 별도 defect evidence지만 이후 path output이 계속됐고 세
후보의 sampled timing이 2,400-tick source budget과 정렬되는 현상을 단독으로 설명하지
못한다. NPE를 timeout의 주원인, 무해한 경고 또는 같은 behavior patch로 합치지 않는다.

§12의 같은 world/dimension trusted destination에 절대 거리 상한을 두지 않는 정책과
장거리 navigation 전체를 자르는 fixed candidate lifetime 사이의 implementation tension은
runtime에서 직접 확인됐다. 이를 고치는 owner는 parent `StoreHomeTask`와 candidate-local
state이며 generic `InteractWithBlockTask`, Baritone path/goal/input, candidate ordering,
exact binding, transfer, typed result와 IdleTask lifecycle은 그대로 둔다. 기존
`STORE_HOME_MANIFEST_STALE` event는 lifetime 책임이 다르므로 재사용하지 않는다.

상세 evidence 표, bounded event/field 계약, behavior-preservation gate, future
no-progress/local-interaction 분리 방향과 승인 gate는
[ChatClef STORE_HOME Long-Distance Timeout Investigation](chatclef-store-home-long-distance-timeout-investigation-2026-08-28.md)이
소유한다.

이 절의 최초 승인 범위는 docs-only였고 diagnostics source/build/deploy/runtime은 당시
완료된 historical evidence였다. 이후 별도 사용자 승인으로 phase-scoped behavior source,
focused tests, clean forced build, JAR 배포와 runtime 재현까지 수행됐다. 이 후속 이력도
`InteractWithBlockTask`, Baritone, `UserBlockRangeTracker`, commit 또는 push에 대한 standing
authorization은 아니다.

#### 23.3.4 2026-08-29 phase-scoped runtime verification

```text
operationId=225
    candidate 1 activation: candidateActiveTicks=7726
    candidate 1/2 rejection: trusted_gui_has_no_capacity, not timeout
    candidate 3 terminal: COMPLETED / paired_delta_confirmed
    storedItems=435, touchedStacks=23, remainingStacks=0

operationId=30351
    candidate 1 activation: candidateActiveTicks=794
    candidate 1 rejection: trusted_gui_has_no_capacity, not timeout
    candidate 2 terminal: COMPLETED / paired_delta_confirmed
    storedItems=155, touchedStacks=7, remainingStacks=0

candidate/operation timeout decisions in both operations: 0
```

첫 operation은 기존 `2,400` absolute candidate lifetime의 세 배가 넘는 active tick 뒤에도
같은 candidate를 유지해 exact activation에 도달했다. 따라서 observed long-distance
happy path에서는 진행 중인 navigation을 고정 lifetime으로 자르던 결함이 제거됐음을
직접 확인했다. Player2API connection refusal과 앞선 별도 `get melon 1` 흐름의 Baritone
`BlockOptionalMeta` NPE는 non-terminal이며 두 STORE_HOME operation의 timeout 또는 terminal
원인으로 합치지 않는다. Runtime code-source는 deployed JAR 경로를 기록했지만 manifest의
Git/source/build-input/runtime SHA fields는 `UNVERIFIED`이므로 strict artifact 판정은 계속
`PARITY_UNPROVEN`이다.

## 24. H6 chat/microphone STORE_HOME direction

H6 최종 1회 방향성 검수 판정은 `PASS`다. Core storage는 그대로 유지하고
LAVI-owned natural-language orchestration과 Fabric bridge result 경계만 확장한다.
24.1의 네 조건은 미결 방향이 아니라 public enable 전 충족해야 할 구현 gate다.
요구사항이 바뀌지 않는 한 추가 docs-only 방향성 검수 없이 이 절을 구현 기준으로
사용하며, 별도 source 승인에 따른 현재 구현도 이 경계를 따른다.
현재 source 구현 상태는 다음과 같다.

```text
ChatClefIntentType.STORE_HOME:              implemented
deterministic STORE_HOME rule:              implemented
input gate STORE_HOME candidate support:    implemented
prefixless store_home compiler branch:      implemented
translation intent/command validation:      implemented
Korean command registry entry:              implemented; public live validation enabled
STORE_HOME-specific response label:         implemented
typed StoreHomeResult bridge projection:    build/JUnit and Korean chat runtime verified
```

현재 `KoreanItemActionRuleParser`는 container 표현과 저장 행동이 함께 나오면
`DEPOSIT_ITEM`으로 판정한다. 따라서 STORE_HOME final 판정을 generic item-action
parser 뒤에 두면 집 전체 보관 문장이 개별 deposit으로 잘못 분류될 수 있다.

### 24.1 필수 조건

다음 네 조건은 구현 선택사항이 아니다.

1. `STORE_HOME` final 판정은 generic `DEPOSIT_ITEM` 판정보다 먼저 수행한다.
2. adapter는 Factory를 직접 호출하지 않고 prefixless `store_home`을 기존 bridge에
   제출한다.
3. Python command registry/readiness/public source 계약에 `store_home`을 추가한다.
4. generic Task completion을 저장 성공으로 간주하지 않고 17절의 typed
   `StoreHomeResult`를 terminal 응답에 전달한다.

이 네 조건 중 하나라도 준비되지 않으면 public Korean `STORE_HOME`을 enable하지
않는다.

### 24.2 판정 위치와 순서

권장 책임 흐름은 다음과 같다.

```text
chat or microphone final transcript
    -> shared input_component
    -> llm.receive_input
    -> MinecraftChatClefInputRouter
    -> MinecraftChatClefInputIntentGate: cheap candidate detection only
    -> KoreanChatClefRuleParser: deterministic final decision
    -> ChatClefIntentSchemaValidator
    -> ChatClefCommandCompiler
    -> ChatClefTranslationResultValidator
    -> existing submission precheck / request / reconciliation
    -> Fabric bridge
    -> Java @store_home normalization and dispatch
    -> existing StoreHomeCommand / StoreHomeTaskFactory / StoreHomeTask
```

`MinecraftChatClefInputIntentGate`는 Minecraft 번역기로 보낼 후보만 감지한다. 이
gate에서 실행을 승인하지 않는다. 현재 gate가 놓칠 수 있는 `집에 가져다 놔`,
`집에 가서 아이템 정리해` 같은 후보를 값싸게 감지할 수 있어야 한다.

`KoreanChatClefRuleParser`의 final 우선순위는 다음과 같다.

```text
1. STOP 등 명시적 control
2. negation / cancellation / question / hypothetical / deferred classification
3. STORE_HOME
4. existing GIVE / DEPOSIT / EQUIP
5. existing GET and remaining intents
6. UNKNOWN
```

gate와 parser에 STORE_HOME 어휘와 정규식을 복붙하지 않는다. 하나의 작고
결정론적인 규칙 소유자가 coarse candidate와 final classification에 필요한 의미를
제공하도록 한다. 정확한 class 수는 implementation audit에서 최소화한다.

V1에서는 LLM extractor가 `STORE_HOME`을 생성할 수 없다. 규칙이 확정하지 못한
문장은 사용자의 inventory를 변경할 실행 권한을 얻지 못한다.

### 24.3 결정론적 실행 조건

`STORE_HOME`은 다음 네 긍정 조건을 모두 만족할 때만 승인한다.

#### 집 목적지

V1에서 승인하는 목적지 표현:

```text
집
집에
집으로
집 상자
집 창고
```

`상자`, `창고`, `보관함`, `본진`, `기지`, `홈`, `베이스` 단독은 집의 trusted
storage인지 확정할 수 없으므로 승인하지 않는다. 후속 동의어는 실제 chat/STT
로그를 근거로 확장한다.

#### 저장 대상 범위

다음 중 하나가 명시되어야 한다.

```text
인벤토리
아이템
물건
짐
남는 것
남는 거
남는 물건
```

`전부`, `다`, `싹`, `정리`만으로는 무엇을 저장할지 알 수 없으므로 부족하다.

#### 저장 행동

강한 긍정 행동:

```text
넣어
저장해
보관해
가져다 놔
갖다 놔
옮겨 놔
```

`정리해`는 조건부 행동이다. 집 목적지와 저장 대상 범위가 모두 있을 때만
`STORE_HOME`으로 인정한다.

#### 현재 실행 직접 요청

명령형 또는 요청형이어야 한다. 설명, 과거 회상, 질문, 가정과 예약 표현은 현재
실행 요청이 아니다. 존댓말, 공백 차이와 마이크식 무구두점은 의미가 같아야 한다.

### 24.4 실행 금지와 모호성

다음 부정 표현은 긍정 단어보다 항상 우선한다.

```text
하지 마
넣지 마
저장하지 마
보관하지 마
정리하지 마
안 넣어
말고
취소
그냥 들고 있어
```

예약 실행이 없는 V1에서는 `나중에`, `이따`, `지금 말고`, `나중에 해줘`도
submission을 만들지 않는다.

질문과 가정은 물음표가 없어도 문장 형태로 감지한다. 예를 들어 `할까`,
`해도 돼`, `하면 될까`, `해야 하나`, `방법 알려줘`는 실행 요청이 아니다.

대표 판정은 다음과 같다. 표의 `AMBIGUOUS`, `NEGATED`, `DEFERRED`, `QUESTION`과
`AMBIGUOUS_COMPOUND`는 의미 분류이며, 구현에서는 기존 status와 안정적인
`reason_code` 조합으로 표현할 수 있다.

| 입력 | 판정 | submission |
| --- | --- | ---: |
| 지금 아이템 다 집에 가져다 놔 | `STORE_HOME` | 1 |
| 인벤토리 정리해서 집 상자에 넣어 | `STORE_HOME` | 1 |
| 남는 물건 전부 집에 보관해 | `STORE_HOME` | 1 |
| 집에 가서 아이템 정리해 | `STORE_HOME` | 1 |
| 집에 가 | `NO_MATCH` | 0 |
| 상자 열어 | `NO_MATCH` | 0 |
| 아이템 정리해 | `AMBIGUOUS` | 0 |
| 집에 가서 정리해 | `AMBIGUOUS` | 0 |
| 집 상자에 넣어 | `AMBIGUOUS` | 0 |
| 다이아 3개 상자에 넣어 | `DEPOSIT_ITEM` | 1 |
| 집에 아이템 넣지 마 | `NEGATED` | 0 |
| 나중에 아이템 집에 넣어 | `DEFERRED` | 0 |
| 집에 보관하면 될까 | `QUESTION` | 0 |
| 집에 보관하고 다이아 캐와 | `AMBIGUOUS_COMPOUND` | 0 |

V1은 독립 명령이 둘 이상인 복합 입력을 부분 실행하지 않는다. 단,
`집에 가서 아이템 정리해`의 `가서`는 별도 navigation command가 아니라 home
storage 목적지를 설명하는 연결 표현이므로 하나의 `STORE_HOME`으로 허용한다.

STORE_HOME 후보지만 정보가 부족한 입력은 일반 LLM이 실행 의미로 재해석하지
않고 deterministic no-submit 안내로 소비한다.

```text
[Minecraft] 집 보관 요청인지 확실하지 않아서 저장하지 않았어요.
"인벤토리 전부 집에 보관해"처럼 말해 주세요.
```

완전히 무관한 `UNKNOWN`만 기존 일반 대화 경로로 넘긴다. 명시적 부정, 질문과
유예도 command를 만들지 않으며 실행하지 않았다는 의미를 사용자에게 알린다.

### 24.5 Intent, compiler와 registry 계약

`STORE_HOME`은 item resolver를 사용하지 않는 zero-slot intent다.

```text
intent_type: store_home
source: rule
confidence: 1.0
item_phrase: ""
quantity: null
slots: {}

translation.status: validated
translation.executable: true
translation.command: store_home
```

compiler는 정확히 `store_home`만 생성한다. `@store_home`을 생성하지 않으며 item,
quantity, player 또는 coordinate를 붙이지 않는다. 원문 또는 slot에 줄바꿈, `;`,
`@`, 제어문자 같은 injection이 있으면 기존 command safety boundary에서 거절한다.
translation validator는 intent와 compiled command가 정확히 일치하는지 다시
검증한다.

권장 registry 계약:

```text
command_name: store_home
slot_schema: ()
resolver_domain: command_specific
lifecycle_kind: task
safety_tier: R2
confirmation_mode: none
allowed_input_sources:
  - lavi_chat_mic_router
  - direct_typed
serializer_id: prefixless_store_home
```

명확한 결정론적 직접 요청 자체를 실행 승인으로 사용하므로 별도 2차 확인은 두지
않는다. 다만 readiness는 실제 증거에 따라 다음 순서로 올린다.

```text
source_registered
    -> korean_parse_compile_ready
    -> python_admission_ready
    -> bridge_lifecycle_ready
    -> gameplay_effect_verifiable
    -> public_korean_enabled
```

typed terminal projection과 runtime 검증 전에는 `gameplay_effect_verifiable` 또는
`public_korean_enabled`를 true로 표시하지 않는 것이 기본 release gate다.
2026-08-28에는 사용자가 Korean chat live validation을 명시적으로 승인한 뒤 public
flag를 먼저 열었고, 같은 세션에서 matching typed terminal과 두 번의 happy path를
확인했다. 이는 제한된 live-validation 노출 기록이며 microphone parity와 전체 runtime
matrix 완료를 뜻하지 않는다.

### 24.6 Submission과 busy 계약

chat과 microphone은 공통 `lavi_chat_mic_router` source를 사용한다. 물리 입력
종류는 선택적 metadata로만 보존하고 intent 또는 execution policy를 바꾸지 않는다.
각 input event는 고유 request ID를 사용하며 다음 불변조건을 지킨다.

```text
translation: at most once
submission: at most once
automatic retry or replay: zero
UNKNOWN / AMBIGUOUS / NEGATED / QUESTION / DEFERRED submission: zero
terminal reconciliation: matching request ID only
reconnect: no automatic STORE_HOME replay
```

마이크 계층이 같은 final transcript를 중복 emit할 가능성은 공통 input event ID로
방지한다. adapter 자체 retry로 해결하지 않는다.

기존 bridge command가 active이면 chat/microphone `STORE_HOME`은
`minecraft_command_busy`로 거절하고 기존 Task를 선점하지 않는다. 게임에서 직접
입력한 `@store_home`은 기존 `runUserTask` 의미에 따라 현재 UserTask를 교체할 수
있다. H6에서 말하는 세 입력의 동일성은 admission 이후 같은 command/factory/storage
policy/terminal 의미를 사용한다는 뜻이며, admission 전 선점 정책까지 같다는 뜻은
아니다.

### 24.7 Terminal 사용자 응답

17절 typed payload가 matching request ID로 도착한 뒤에만 저장 결과를 확정한다.

| `StoreHomeResult` | 사용자 응답 의미 |
| --- | --- |
| `COMPLETED` | 집 보관 완료 |
| `PARTIAL_*` | 일부 저장, 남은 stack 있음 |
| `NO_USABLE_TRUSTED_DESTINATION` | 사용할 수 있는 trusted 상자 없음 |
| `NO_TRUSTED_CAPACITY` | trusted 상자에 빈 공간 없음 |
| `CURSOR_NOT_EMPTY` | cursor 안전 조건 때문에 거절 또는 중단 |
| `MANIFEST_STALE` | inventory 변경을 감지해 안전하게 중단 |
| `CONTEXT_CHANGED` | world/dimension 변경을 감지해 중단 |
| `TRANSFER_UNCONFIRMED` | paired transfer 확인 실패 |
| `INTERRUPTED` | 다른 작업으로 중단 |

generic outer status가 `completed`여도 typed payload가 없거나 모순되면 "집 보관
완료"라고 응답하지 않는다. `NO_TRUSTED_CAPACITY`와 같은 정상적인 Task 종료를
성공으로 오인하지 않는 것이 H6 public enable의 필수 gate다.

### 24.8 H6 테스트와 공개 순서

최소 test matrix:

| 범위 | 필수 증거 |
| --- | --- |
| positive parser | 대표 네 문장과 존댓말/공백/STT 무구두점 변형이 `STORE_HOME` |
| negative parser | 부정, 질문, 유예, 모호, 복합 문장의 submission 0 |
| intent priority | `인벤토리 전부 집 상자에 넣어`는 `STORE_HOME` |
| deposit regression | `다이아 3개 상자에 넣어`는 기존 `DEPOSIT_ITEM` |
| compiler/schema | zero-slot `store_home`, prefix 없음, injection 거절 |
| registry | R2/task/empty slots/allowed sources와 readiness 단계 검증 |
| router | input 1건당 translate 1회, submit 최대 1회, retry 0 |
| chat/mic parity | physical source metadata 외 intent, admission과 terminal 동일 |
| busy/disconnect | submission 0, 기존 Task 유지, reconnect replay 0 |
| terminal | 모든 `StoreHomeResult`가 서로 다른 의미로 projection/renderer에 전달 |
| runtime happy path | chat와 microphone 각각 command/task start 1회와 `COMPLETED` |
| runtime no-op | 모호·부정·질문 입력 후 이동/open/inventory mutation 0 |
| trusted fallback | A full 뒤 B 저장, 일반 상자 접근 0 |
| failure matrix | no trusted/full/unreachable/cross-dimension/cursor의 typed 결과 |
| no automatic trigger | 사용자 입력 없이 command/task start 0 |

runtime 로그에서는 최소한 다음 correlation을 확인한다.

```text
one input event
one validated translation
canonical command=store_home
request source=lavi_chat_mic_router
one Java normalized @store_home dispatch
one StoreHomeTask root
matching request ID terminal
typed StoreHomeResult
zero duplicate submission
```

공개 순서:

```text
1. parser/compiler/schema unit tests
2. registry and Python admission tests
3. bridge typed terminal tests
4. chat/microphone parity integration tests
5. authorized clean forced Fabric build and deployed JAR verification
6. authorized Minecraft runtime matrix
7. public_korean_enabled
```

현재 기록은 명시적 사용자 승인으로 7번을 live validation 용도로 먼저 적용한 뒤 Korean
chat에서 3번의 matching terminal을 확인한 상태다. 4번의 실제 microphone parity와
6번의 전체 failure/fallback matrix는 계속 open gate로 유지한다.

### 24.9 H6 범위 제외

H6 V1은 다음을 포함하지 않는다.

- LLM 기반 `STORE_HOME` 분류 또는 사용자 확인 없는 LLM 실행
- multi-turn 확인과 `그거 집에 넣어` 같은 문맥 참조
- 사용자별 동의어 학습
- `본진`, `기지`, `홈`, `베이스` 등 넓은 목적지 표현
- 영어 또는 한영 혼합 명령
- 예약 실행
- 자연어 명령의 active bridge Task 선점
- 자동 retry, replay 또는 reconnect 재실행
- loadout planner, manifest, repository 또는 transfer executor 변경
- 일반 상자 fallback 또는 inventory-pressure 자동 실행
- H5 fixed 16x16x16 direct-command batch registration와 Korean batch-only direct `@` forms
- 새 item-policy JSON, profile JSON 또는 pin JSON

H6로 인해 새 JSON item classification은 추가하지 않는다. 기존 exact trusted
destination registry JSON만 계속 사용한다.

### 24.10 Source ownership boundary

H6 source 구현에서 검토하고 사용한 LAVI-owned Python 경계는 다음과 같다.

```text
plugins/Minecraft/fabric/chatclef/input/
    minecraft_chatclef_input_intent_gate.py
    minecraft_chatclef_input_router.py
    routing/submission/result/submission_outcome_consistency.py
    routing/submission/reconciliation/submission_reconciliation_policy.py

plugins/Minecraft/fabric/chatclef/intent/
    chatclef_intent_type.py
    chatclef_intent_dto.py
    chatclef_intent_schema_validator.py
    korean_chatclef_rule_parser.py
    korean_item_action_rule_parser.py       # regression boundary; policy owner 아님
    chatclef_command_compiler.py
    chatclef_translation_result_validator.py
    chatclef_natural_language_service.py
    store_home/**

plugins/Minecraft/fabric/chatclef/command_registry/
    korean_command_registry.py
    admission/**

plugins/Minecraft/fabric/chatclef/extension/
    minecraft_fabric_chatclef_extension.py
    command/**
    natural_language/**

plugins/Minecraft/fabric/chatclef/result/
    store_home/**

plugins/Minecraft/fabric/chatclef/response/
    chatclef_command_response_renderer.py
    store_home/**
```

deterministic matcher는 `intent/store_home/`의 좁은 LAVI-owned collaborator로
구현해 gate와 parser가 함께 사용한다. gate와 parser에 어휘를 복제하지 않았고,
기존 class와 test pattern을 재사용했다.

Java 변경 후보는 existing command result `data` edge에 operation-specific typed
projection을 연결하는 최소 LAVI-owned 경계다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
    lavi/minecraft/task/container/home/command/**
    lavi/minecraft/task/container/home/execution/StoreHomeTask.java
    lavi/minecraft/task/container/home/execution/StoreHomeResult.java
    lavi/minecraft/task/container/home/result/**
    lavi/minecraft/fabric/chatclef/bridge/command/result/**
    lavi/minecraft/fabric/chatclef/bridge/command/execution/**
```

이 목록은 whole-package 수정 승인이 아니다. 실제 result ownership을 추적한 뒤
필요한 최소 파일과 hunk만 선택했다. `StoreHomeTask.outcome()`의 immutable typed
snapshot을 실제 matching Task 객체에서 읽으며, 사람이 읽는 문자열은 파싱하지
않는다.

다음 경계는 H6에서 의도적으로 변경하지 않는다.

```text
HomeLoadoutPlanner
HomeStorageManifest and exact-slot fingerprint policy
HomeStorageDestinationSelector and trusted JSON repository
HomeStorageTransferExecutor and paired-delta verification
automatic inventory-pressure policy
adris/** upstream-derived source
TaskRunner / UserTaskChain / global chain lifecycle
Baritone input, goal and path ownership
Forge / MineMind backend
```

### 24.11 H6 source implementation evidence

2026-08-27 별도 source 승인에 따라 다음을 현재 worktree에 구현했다.

```text
deterministic STORE_HOME classifier:       implemented; gate/parser shared owner
guarded no-submit classifications:         implemented; LLM fallback blocked
zero-slot intent/compiler/schema:          implemented; prefixless store_home
registry/source admission:                 implemented; R2/task/allowed sources
existing bridge submission path:           reused; Factory direct call 없음
typed Java StoreHomeOutcome projection:    implemented from matching Task object
typed Python validation/renderer:          implemented; contradictory payload fail-closed
extension responsibilities:                split into command and natural-language packages
public_korean_enabled:                     true; user-approved live validation
```

Registry/readiness의 현재 관찰 상태는 다음과 같다.

```text
SOURCE_REGISTERED=true
KOREAN_PARSE_COMPILE_READY=true
PYTHON_ADMISSION_READY=true
BRIDGE_LIFECYCLE_READY=true
GAMEPLAY_EFFECT_VERIFIABLE=true
PUBLIC_KOREAN_ENABLED=true
```

`BRIDGE_LIFECYCLE_READY`는 별도 승인된 clean forced build와 Java focused test가
통과하고 배포 JAR hash가 일치한 뒤 true로 올렸다.
`GAMEPLAY_EFFECT_VERIFIABLE`은 23.2절의 matching Korean chat request가 실제
`MANIFEST_STALE`와 `COMPLETED` typed outcome을 전달하고, 두 완료 요청이 trusted
상자에 paired-delta 저장한 증거를 근거로 한다. `PUBLIC_KOREAN_ENABLED`는 사용자의
명시적 live-validation 승인에 따른 현재 설정이다. 두 값이 true여도 microphone parity,
negative/busy runtime과 failure/fallback matrix가 완료됐다는 뜻은 아니다.

2026-08-27 H6 source 단계의 historical Python 검증은 관련 ChatClef suite에서 다음
결과를 기록했다.

```text
322 passed
2 skipped
2231 subtests passed
1 pre-existing catalog test deselected
```

deselect한 기존 test는 H6 source와 무관하게 source에 등록된 `DepositAllCommand`가
기존 command catalog fixture에 누락된 상태를 검사하는 항목이었다.

2026-08-29 commit-readiness 확인에서는 해당 test를 deselect하지 않고 실행했다. 최초
실행은 Java registration set에 있는 `DepositAllCommand`가 snapshot에 없어 실패했다.
Snapshot과 support matrix에 `deposit_all`을 `RAW_ONLY/java_only_task_command`로 반영한
뒤 재실행한 결과는 다음과 같다.

```text
37 collected
36 passed
1 failed

failure:
PythonKoreanCommandRegistryTests.
test_registry_contains_exactly_the_registered_chatclef_commands
```

현재 source-extracted snapshot과 support matrix는 22 commands이지만 production
`KoreanChatClefCommandRegistry`는 21 commands이며 `deposit_all` metadata가 없다. Test
fixture만 완화하거나 Java source registration을 숨기는 것은 금지한다. 올바른 closure는
canonical registry 문서에서 `deposit_all`의 raw-only metadata를 고정하고 production
registry와 expected count를 22로 동기화한 뒤 focused suite를 다시 실행하는 것이다.
이번 문서 작업에서는 production Python source를 수정하지 않았다.

Java focused test에는 matching `StoreHomeTask`, command-gate
`CURSOR_NOT_EMPTY`, 모든 terminal enum wire identity와 generic command 비오염
scenario를 추가했다. clean build 결과
`FabricChatClefStoreHomeResultProjectorTest` 4건과
`StoreHomeTaskInterruptionTest` 2건은 모두 failure/error 0으로 통과했다.

Python integration test는 chat 문장과 microphone식 무구두점 transcript의 canonical
route parity, direct-typed terminal payload 보존, busy/disconnect submission 0과 fresh
explicit input 전 replay 0을 추가로 고정했다. 실제 Minecraft Korean chat과 matching
request typed payload는 23.2절에서 runtime 확인됐다. 실제 microphone 입력과 전체
negative/busy/failure runtime matrix는 아직 미검증이다.

## 25. Historical operation-wide MANIFEST_STALE diagnostics-only direction

이 절은 26절의 session-local lifetime 방향이 문서화되기 전, operation 시작 때 만든
manifest를 root가 계속 검증하던 구현에 대한 당시 diagnostics-only 기록이다.
2026-08-28 당시 검토 판정은 `PASS - diagnostics-only 보강 후 통제 재현`이었다.
`MANIFEST_STALE`을 무시하거나 자동 복구할 근거는 없었으며, 첫 실패를 만든 exact
expected/current 상태를 operation당 한 번만 기록하는 것이 당시 다음 단계였다. 이 절은
진단 source, build, 배포, 재현, commit 또는 push를 승인하지 않는다.

이 기록의 request ID, typed result, event/field 계약, bounded payload와 통제 matrix를
삭제하거나 후속 behavior 방향이 이미 runtime에서 검증됐다는 증거로 바꾸지 않는다.

### 25.1 확인된 실패 경계와 배제 가능한 해석

당시 검토한 source와 23.2절의 typed reason으로 직접 확인된 사실은 다음뿐이다.

```text
manifest logical player main slot: 8
validation reason: planned_fingerprint_changed
proven difference: planned identity != live player.inventory.main[8] identity
root cause behind that difference: unproven
```

`StoreHomeTask`의 root tick은 candidate 선택, GUI binding과 executor 처리보다 앞에서
`HomeStorageManifestProgress.validate(...)`를 호출한다. 이 검증은 ScreenHandler
window slot을 resolve하지 않고 `player.getInventory().main`의 logical slot을 직접
읽는다. 따라서 이번 reason 자체는 `syncId` 또는 logical-slot-to-window-slot mapping
실패의 직접 증거가 아니다. Handler 전환은 live player inventory가 달라진 배후 trigger
후보일 수만 있다.

검증 순서는 empty, fingerprint, count다. Fingerprint는 count를 1로 정규화해 비교하고
count는 이후 별도로 검사한다. Count만 달라졌다면 `planned_count_changed`여야 하므로,
`planned_fingerprint_changed`는 item ID, damage 또는 count를 제외한 exact stack
metadata identity 중 적어도 하나가 달랐음을 뜻한다. Expected/actual count는 동시
변경 여부를 확인하기 위해 계속 기록한다.

`storedItems=0`과 `touchedStacks=0`은 QUICK_MOVE 요청이 0회였다는 증거가 아니다. 이
수치는 paired delta가 확인되어 `progress.confirm(...)`까지 끝난 전송만 집계한다.
Executor가 이전 tick에 click을 요청하고 pending verification 중이었을 가능성을
구분하려면 최소한 다음 네 필드가 필요하다.

```text
transferPending
pendingLogicalSlot
phaseBeforeFailure
currentManifestStepLogicalSlot
```

### 25.2 조사 우선순위

다음 순서는 원인 확정이 아니라 통제 재현의 우선순위다.

| 순위 | 후보 | 당시 의미 |
| ---: | --- | --- |
| 1 | GUI open 또는 직전 QUICK_MOVE 주변 client/server inventory synchronization | 일시 실패 뒤 같은 명령이 두 번 성공했으므로 먼저 관찰 |
| 2 | slot 8의 실제 damage 또는 metadata identity 변경 | 당시 reason과 직접 양립 가능 |
| 3 | 사용자, 다른 Task, mod 또는 server authoritative update의 실제 slot 교체 | expected/actual item ID로 판별 |
| 4 | fingerprint가 변동성 metadata를 identity로 취급한 false stale | 모든 비교 필드가 같을 때만 강해짐 |
| 5 | logical slot to handler window-slot mapping | 이번 root reason의 직접 원인은 아니며 context로만 관찰 |
| 6 | syncId 오류 | 이번 root reason의 직접 원인은 아니며 context로만 관찰 |

### 25.3 진단 책임과 삽입 경계

진단 구현은 operation-local collaborator를 사용하고 global mutable state를 만들지
않는다. 책임은 다음과 같이 제한한다.

| 경계 | 진단 책임 | 금지 사항 |
| --- | --- | --- |
| `StoreHomeTask` | operation ID, plan-time baseline, failure context와 단일 event emission 소유 | phase, terminal, retry 또는 cleanup 변경 |
| `HomeStorageManifestProgress` | 실제 mismatch 비교에 사용한 expected step과 actual stack snapshot을 같은 read에서 capture | 별도 재조회, request-aware logging 또는 판정 변경 |
| immutable manifest/step | existing expected identity 제공 | logging side effect 또는 mutable diagnostics state |
| fingerprint | 별도 bounded diagnostics digest에 필요한 normalized identity 제공 | `matches`, `equals`, `hashCode`, `summary` 의미 변경 |
| `HomeStorageScreenSlotResolver` | failure 시 read-only mapping inspection 제공 | resolver 결과, unique mapping 기준 또는 fallback 변경 |
| `HomeStorageTransferExecutor` | executor 내부 세 stale 경계에서 같은 helper에 context 전달 | normal polling/click/capacity 로그 또는 transfer 변경 |

실패 후 Task가 slot을 다시 읽어 actual 값을 만드는 방식은 사용하지 않는다. 조사 대상이
synchronization timing일 수 있으므로, mismatch 판정에 실제 사용한 stack을 같은 경계에서
즉시 defensive copy하거나 item ID/count/damage와 digest 입력을 immutable 값으로
materialize해야 한다. `Validation` 또는 executor result에 live `ItemStack` reference를
보관해 반환한 뒤 나중에 읽는 방식은 허용하지 않는다.

Executor의 기존 stale 경계도 같은 event contract를 사용하되 평상시에는 emit하지
않는다.

```text
logical_slot_mapping_unavailable       -> EXECUTOR_SLOT_RESOLUTION
exact_source_changed_before_click      -> EXECUTOR_PRE_CLICK_SOURCE_VALIDATION
source_fingerprint_changed_after_click -> EXECUTOR_POST_CLICK_SOURCE_VALIDATION
```

### 25.4 Event와 field 계약

새 detailed event는 하나로 통일한다.

```text
event=STORE_HOME_MANIFEST_STALE
failureStage=
    ROOT_MANIFEST_REVALIDATION |
    EXECUTOR_SLOT_RESOLUTION |
    EXECUTOR_PRE_CLICK_SOURCE_VALIDATION |
    EXECUTOR_POST_CLICK_SOURCE_VALIDATION
```

기존 diagnostics infrastructure의 trace, client tick과 task-run identity를 재사용한다.
다만 current `logBoundary(...)` event emitter는 command context를 자동 첨부하지 않으므로,
이 event의 field array는 `ChatClefDiagnostics.withCommandContextFields(...)`를 정확히 한 번
거쳐야 한다. 최소 command fields는 `commandContextAvailable`, `commandRequestId`,
`commandCorrelationId`, `commandSessionId`와 `commandConnectionGeneration`이다. Matching
LAVI request가 없으면 `commandContextAvailable=false`를 기록하며 가짜 request ID를
만들지 않는다. Manifest에 request context를 복사하지 않는다. `planRevision`은 planner
instance-local 값일 수 있으므로 단독 전역 ID로 사용하지 않고 항상 `operationId`와 함께
해석한다.

최소 StoreHome 전용 필드는 다음과 같다.

```text
operationId
planRevision
planCapturedClientTickId
failureClientTickId
elapsedClientTicks
phaseBeforeFailure
failureStage
validationReason

manifestStepCount
manifestStepIndex
logicalPlayerSlot
currentManifestStepLogicalSlot
expectedRemainingCount
transferPending
pendingLogicalSlot
confirmedItemCount
touchedStackCount

expectedPresent
expectedItemId
expectedCount
expectedDamage
expectedMetadataDigest
actualPresent
actualItemId
actualCount
actualDamage
actualMetadataDigest
itemIdEqual
countEqual
damageEqual
metadataDigestEqual
fullFingerprintEqual

planScreenClass
planHandlerClass
planHandlerIdentity
planSyncId
planHandlerSlotCount
failureScreenClass
failureHandlerClass
failureHandlerIdentity
failureSyncId
failureHandlerSlotCount
screenClassChanged
handlerIdentityChanged
syncIdChanged

mappingObservedForDiagnosticsOnly
mappingMatchCount
resolvedWindowSlot
resolvedSlotLogicalIndex
resolvedSlotPresent
resolvedSlotItemId
resolvedSlotCount
resolvedSlotDamage
resolvedSlotMetadataDigest
playerMainAndResolvedSlotEqual

cursorEmpty
cursorItemId
cursorCount
worldKey
dimension
activeDestinationPresent
trustedDestinationId
trustedPosition
exactTrustedBindingMatched

metadataDigestAlgorithm
metadataCanonicalizationVersion
diagnosticCaptureStatus
diagnosticErrorClass
```

Candidate 선택 전 root stale이면 destination 관련 값이 `none`인 것이 정상이다. Root
validation의 mapping snapshot은 terminal 판정에 사용하지 않고
`mappingObservedForDiagnosticsOnly=true`로 명시한다. Handler identity는
`ClassName@identityHashHex` 같은 run-local 값이며 JVM 실행 사이의 객체 identity로
해석하지 않는다. 특정 failure stage에서 관찰할 수 없는 mapping, binding 또는 equality
필드는 임의의 `false`, `0`이나 `minecraft:air`로 채우지 않고 명시적 `not_available`을
사용한다.

### 25.5 Bounded logging과 metadata 보호

`BOUNDARY`에서 실제 mismatch가 발생한 expected/actual stack 두 개만 계산하고,
operation당 detailed stale event는 최대 한 건이다. 성공 operation, cursor 사전 거절과
unchanged tick에는 새 event를 emit하지 않는다. Cross-operation dedupe는 금지하며 각
새 request와 operation은 독립 증거를 남긴다.

권장 dedupe key는 다음과 같다.

```text
operationId | planRevision | failureStage | logicalPlayerSlot | validationReason
```

기존 `HomeStorageStackFingerprint.summary()`의 hash는 item ID와 damage만 포함하므로
metadata 진단 hash로 사용하지 않는다. Existing matcher와 summary 의미는 그대로 둔다.
`expectedMetadataDigest`와 `actualMetadataDigest`는 별도로 기록한 canonical item ID, count와
damage를 제외한 metadata/component payload만 canonicalize한 diagnostics-only digest다.
기본 계약은 `SHA-256` 앞 128 bit, 32 hex characters,
`metadataCanonicalizationVersion=1`이다. Canonicalization은 구현 전에 test fixture로
고정하고 count와 damage-only 변경이 metadata digest에 들어가지 않음을 검증한다. Existing
matcher의 전체 결과는 별도 `fullFingerprintEqual`로 기록한다.

다음 값은 로그에 원문으로 남기지 않는다.

```text
full NBT or component serialization
custom name, lore or book text
container or shulker item contents
unbounded item metadata values
```

필요하면 `metadataPresent`, enchantment entry count, container payload presence와
serialized-size bucket 같은 bounded structural flag만 추가한다. Current diagnostics emitter와
value formatter에는 total payload cap이 없으므로 기존 logger가 잘라 줄 것이라고 가정하지
않는다. Diagnostics helper가 free-form field별 상한과 event 전체 상한을 직접 소유하며, 최종
formatted event는 최대 8 KiB로 제한한다. 상한에 도달하면 optional structural summary만
생략하거나 축약하고 `diagnosticCaptureStatus=partial`로 표시하되 item ID/count/damage,
metadata digest, failure stage와 correlation fields는 유지한다. Diagnostic formatter나 digest
자체의 formatting 실패만 `diagnosticCaptureStatus=partial`로 축약할 수 있다. 관찰 대상
engine, inventory, handler, transfer 또는 Task exception을 새로 catch하거나 억제하지 않는다.
진단 실패는 기존 result, reason 또는 control flow를 바꾸지 않는다.

### 25.6 통제 재현 matrix

같은 world copy, player position, trusted registry, slot 8 stack, 나머지 inventory,
JAR, Java runtime과 한국어 문장을 고정한다. 각 run은 새 request ID를 사용하고 이전
terminal 뒤에 시작하며, inventory/container 상태를 baseline으로 복원한다. 조건별 5회
screening 후 특정 조건에 stale가 모이면 그 조건을 10회로 늘린다.

| 조건 | 확인할 핵심 |
| --- | --- |
| inventory/container screen closed | handler 전환 없이도 stale가 나는지 |
| trusted large chest already open | 동일 handler/syncId에서 stale가 나는지 |
| another chest open | trusted handler 교체와 stale가 결합되는지 |
| slot 8 changed before acceptance | 새 snapshot이 변경 후 stack을 expected로 잡는지 |
| slot 8 changed immediately after acceptance | expected/actual 차이를 정확히 잡는 positive control |
| controlled server inventory update | player main과 resolved handler slot이 일시적으로 갈리는지 |
| cursor empty | 정상 완료 또는 stale detailed event |
| cursor non-empty | `CURSOR_NOT_EMPTY`, stale event 0, click 0 |
| repeated identical commands | request/operation별 event와 terminal이 독립적인지 |

Positive control은 item 교체, damage-only 변경과 metadata-only 변경을 분리한다. 단일
event로 sync race를 확정하지 않으며 같은 통제 조건에서 같은 패턴이 반복돼야 한다.

| 관찰 패턴 | 다음 판정 |
| --- | --- |
| expected/actual item ID 다름 | 실제 교체, slot 이동 또는 authoritative update |
| item ID 같고 damage 다름 | durability/damage mutation |
| item ID와 damage 같고 metadata digest 다름 | metadata identity mutation |
| fingerprint 같고 count만 다르며 `planned_count_changed` | count mutation detection |
| 모든 identity 필드가 같은데 fingerprint false | matcher 또는 diagnostic canonicalization 조사 |
| player main은 expected와 같고 mapping count가 0 또는 2+ | resolver mapping 조사 |
| player main은 expected와 같고 resolved handler stack만 다름 | handler-state 또는 client/server synchronization 조사 |
| handler 변경만 있고 player main identity 동일 | handler 전환은 stale의 직접 원인 아님 |
| `transferPending=true`, stored items 0 | click 요청 뒤 paired-delta confirm 전일 수 있음 |

### 25.7 절대 변경하지 않는 behavior

이번 diagnostics 단계에서는 다음을 변경하지 않는다.

- `HomeStorageStackFingerprint.matches()`와 fingerprint/count 비교 순서
- immutable manifest, logical slot과 manifest step 의미
- `MANIFEST_STALE` terminal, reason과 fail-closed 의미
- `MANIFEST_STALE` 후 automatic retry, same-GUI replan, same-item substitute 또는
  active manifest refresh 추가
- Task phase, timeout, safety-chain resume, interruption과 cleanup
- QUICK_MOVE, cursor, pending transfer와 paired-delta 검증
- trusted selection, candidate ordering, fallback, navigation과 dimension policy
- resolver unique mapping 기준 또는 임의 offset fallback
- `TaskRunner`, `UserTaskChain`, Baritone와 `adris/**`
- H6 intent, admission, Python renderer 또는 Chatbot terminal UI

매 tick inventory dump, 매 slot/matcher/resolver 호출 로그, full NBT, 별도 timer/thread와
로그를 위한 state-changing API 호출도 금지한다.

### 25.8 진단 구현 acceptance gate

당시 정의한 diagnostics-only source implementation acceptance gate는 다음과 같다.

```text
existing STORE_HOME behavior and terminal unchanged
BOUNDARY disabled: detailed stale event count = 0
BOUNDARY enabled + success operation: detailed stale event count = 0
BOUNDARY enabled + CURSOR_NOT_EMPTY: detailed stale event count = 0
BOUNDARY enabled + MANIFEST_STALE: detailed stale event count = exactly 1
any mode and any operation: detailed stale event count never exceeds 1
event uses an immutable defensive snapshot from the exact failed comparison
matching LAVI request includes existing command request/correlation context once
root and three executor failure stages are distinguishable
metadata digest is bounded, versioned, count-independent and damage-independent
damage-only positive control keeps metadataDigestEqual=true
formatted event obeys the diagnostics-owned 8 KiB cap
diagnostic capture failure cannot alter Task behavior
controlled positive tests classify item, damage and metadata changes
```

이 gate를 통과한 diagnostic JAR로 25.6절을 재현하는 것이 당시 root-cause
behavior-patch gate였다. Instrumented JAR은 별도 SHA-256으로 기록하며 23.1절의
JVM baseline/current A/B에서 보존해야 하는 exact current H6 crash-run JAR을 대체하지
않는다. 25절의 diagnostics 승인은 26절 behavior source, retry 또는 fingerprint policy
변경을 승인하지 않는다.

### 25.9 후속 제보와 source/artifact parity 미해결

후속 검수에 전달된 진단 요약은 navigation 중 cobblestone과 dirt count가 증가한
시점에 stale가 발생했고, item ID, damage와 표시된 metadata digest는 같았지만
`reason=planned_fingerprint_changed`였다고 설명한다. 이 문서 저장소에는 그 결과를
동일 실행에서 재구성할 수 있는 instrumented diagnostics JAR hash, exact source
revision과 runtime code-source tuple이 아직 기록돼 있지 않다. 따라서 이 요약은
`USER_REPORTED / ARTIFACT_PARITY_NOT_VERIFIED`로 분류한다.

현재 공개 source 의미상 fingerprint는 count를 1로 정규화하고 count를 별도로
검증하므로 정말 count만 달랐다면 다음이 예상된다.

```text
validationReason=planned_count_changed
fullFingerprintEqual=true
countEqual=false
damageEqual=true
metadataDigestEqual=true
```

실제 reason이 `planned_fingerprint_changed`였다면 최소한 다음 가능성을 분리한다.

```text
deployed diagnostics JAR source와 현재 branch source가 다름
metadata digest와 full fingerprint가 포괄하는 identity가 다름
diagnostic expected/actual capture가 실제 comparison과 다른 tick 또는 snapshot
reason branch 또는 build/deployed artifact parity가 다름
```

behavior source 변경 전에 다음 tuple을 하나의 evidence record로 고정해야 한다.

```text
exact source commit or tree identity
relevant dirty diff or build-input manifest
validate reason branch source
built diagnostics JAR SHA-256
deployed JAR SHA-256
runtime code-source identity
same defensive snapshot의 itemIdEqual
same defensive snapshot의 countEqual
same defensive snapshot의 damageEqual
same defensive snapshot의 metadataDigestEqual
same defensive snapshot의 fullFingerprintEqual
```

이 parity 문제는 26절의 manifest lifetime 방향과 별도다. 둘을 한 behavior patch,
한 원인 판정 또는 한 rollback unit으로 섞지 않는다.

## 26. Trusted-container-session-local manifest lifetime direction

최종 방향 판정은 `CONDITIONAL PASS`다. Exact manifest를 늦게 만드는 방향은
승인하지만, 아래 여섯 조건이 문서와 focused tests에 고정되고 별도 source 승인이
있기 전에는 구현하지 않는다.

```text
기존 nothing_to_store no-op 의미 보존
operation 누적 결과와 container-session-local progress 분리
transfer-authoritative fresh planning과 manifest activation을 exact GUI 경계로 제한
planner/baseline/disposition/manifest를 동일한 single-read immutable snapshot에서 파생
paired-delta confirmation을 overlay/progress/operation 누계에 exactly once commit하고 다음 revalidation 전 state 확정
count-only / planned_fingerprint_changed source-artifact 비정합 별도 확인
```

이 방향은 `MANIFEST_STALE` 안전 검사를 약화하지 않는다. 해결하려는 문제는
exact transfer 계약이 너무 엄격하다는 것이 아니라, transfer 계약이 아직 필요하지
않은 navigation phase부터 활성화돼 있었다는 수명 경계다.

### 26.1 Canonical operation 의미

정책 의미는 다음 문장으로 고정한다.

> `STORE_HOME`은 command 입력 순간 inventory를 동결해 보관하는 명령이 아니라,
> 같은 operation에서 trusted-container transfer session이 시작되는 시점의 현재
> inventory를 loadout 정책에 따라 정리하는 명령이다.

따라서 navigation 중 발생한 inventory 변화는 “기존 manifest의 허용 예외”가 아니다.
그 시점에는 authoritative transfer manifest 자체가 없어야 한다. Exact trusted GUI
activation 뒤에는 지금과 같은 fail-closed exactness를 유지한다.

### 26.2 소유권 분리

```text
STORE_HOME operation
    operationId and context snapshot
    one command request and one StoreHomeTask root
    candidate queue snapshot
    current candidate attempt and candidateTicks
    attempted/rejected destinations
    capacityFailures and unavailableFailures
    confirmedStoredItems
    confirmedTouchedLogicalSlots
    latestKnownRemainingStacks
    operationTicks
    one terminal result and reason

trusted-container session
    activeDestination
    containerSessionOrdinal
    fresh HomeStoragePlan
    immutable HomeStorageManifest
    immutable activation inventory baseline
    confirmed-delta expected-state overlay
    HomeStorageManifestProgress
    local planRevision
    pending transfer
```

Local manifest/progress가 교체돼도 operation ID, root identity, command request,
candidate snapshot, timeout과 누적 confirmed 결과를 초기화하지 않는다.
`candidateTicks`는 candidate selection/navigation attempt에서 시작해 후보가 바뀔 때
초기화하는 total-attempt 관찰값으로 유지할 수 있지만, navigation 중 fixed lifetime
rejection의 단독 predicate로 사용하지 않는다. Candidate-local last-progress clock과 local
interaction clock은 phase별 의미로 분리한다. 두 clock은 StoreHome root가 해당 phase에서
실제로 tick된 경우에만 진행하고 safety-chain 선점 중에는 소비하지 않는다. Candidate 교체,
path/goal 재제출과 calculation generation 변화는 그 자체로 semantic progress가 아니며
operation no-progress clock을 reset하지 않는다. Local interaction clock은 같은 candidate
attempt에서 interaction-neighborhood handoff가 처음 성립할 때 한 번 시작하고 screen/binding
flicker 또는 phase 경계 진동으로 다시 0이 되지 않는다. `containerSessionOrdinal`은 navigation
시작이 아니라 §10 activation gate를 모두 통과해 authoritative local plan/manifest를
활성화할 때만 증가한다.

### 26.3 권장 lifecycle

```text
1. ACCEPT_REQUEST
   cursor empty와 explicit request boundary 확인

2. SNAPSHOT_CONTEXT
   worldKey, dimension과 runtime context 고정

3. PRECHECK_CURRENT_SURPLUS
   기존 planner의 advisory result가 0 step이면 같은 root에서 nothing_to_store
   1 step 이상이면 plan/manifest를 전송에 사용하지 않고 폐기

4. BUILD_DESTINATION_QUEUE
   현재 trusted 후보 snapshot 고정
   operation 중 새 등록은 합류하지 않음

5. SELECT_DESTINATION
   현재 repository에서 trusted/enabled 재검증

6. NAVIGATE_AND_OPEN
   active plan/manifest/progress 없음
   cursor, context, UserTask와 후보 trust safety만 유지

7. VALIDATE_CONTAINER
   exact destination binding, handler/container, trust, cursor,
   world/dimension과 pending 없음 확인

8. PLAN_AT_TRUSTED_CONTAINER
   현재 player-held state를 one-read immutable activation snapshot으로 capture
   같은 snapshot에서 loadout/reserve, full baseline, disposition과 manifest 파생
   plan invariant 확인
   second live inventory read와 mutable ItemStack reference 보관 금지
   session ordinal과 plan revision을 가진 immutable local manifest 활성화

9. TRANSFER_EXACT_SLOTS
   active manifest strict revalidation과 exact logical-slot resolution

10. VERIFY_TRANSFER
    paired delta와 cursor empty 확인
    pending identity 기준 exactly-once confirmation commit
    overlay, local progress와 operation accumulator를 같은 Task transition에서 갱신
    다음 root revalidation/candidate transition 전 committed state 확정

11-A. LOCAL_MANIFEST_COMPLETE
    active progress remaining 0과 pending 없음이면 COMPLETED

11-B. CANDIDATE_NO_CAPACITY
    pending 없음과 operation 누계 반영을 확인
    local session 폐기 후 다음 candidate

11-C. CANDIDATE_UNAVAILABLE_OR_UNTRUSTED
    pending 없음이면 candidate-local 제외 후 다음 candidate
    pending이면 TRANSFER_UNCONFIRMED

11-D. TIMEOUT_BOUNDARY
    candidate_navigation_no_progress + pending 없음이면 현재 candidate 제외
    candidate_local_interaction_timeout + pending 없음이면 현재 candidate 제외
    operation_no_progress 또는 operation_emergency_hard_cap + pending 없음이면
        누적 결과에 따른 기존 no/partial terminal
    candidate-local reason을 operation-wide unusable 증거로 확대하지 않음
    어느 timeout이든 pending 결과가 불확실하면 TRANSFER_UNCONFIRMED

12. TERMINAL
    cumulative storedItems, latest/final remainingStacks와 typed result
```

모든 이름을 새 enum으로 추가할 필요는 없다. 기존 phase를 재배치하거나 최소 phase만
추가할 수 있지만, exact source와 test 변경은 별도 승인 뒤 결정한다.

### 26.4 `nothing_to_store`와 single-root 계약

Advisory preflight는 새 surplus 알고리즘을 만들지 않고 기존 planner를 재사용한다.
0 step 결과만 즉시 완료 판정에 authoritative하며 transfer manifest는 아니다.

```text
accepted request
    -> StoreHomeCommand creates exactly one StoreHomeTask
    -> runUserTask exactly once
    -> first StoreHomeTask lifecycle evaluates advisory preflight
    -> 0 step: COMPLETED / nothing_to_store
```

`StoreHomeCommand.call()`에서 Task를 만들지 않고 finish하거나 pre-existing
`IdleTask`를 command-owned root로 해석하지 않는다. 이는 기존 sync-finish
idle-root ownership 회귀를 피하기 위한 필수 조건이다. Non-empty preflight revision은
click, root manifest validation, destination session revision 또는 typed success에
사용하지 않는다.

### 26.5 Mutation과 fresh planning 경계

Manifest activation 전에는 변경 출처를 구분하지 않는다.

| Navigation 중 변화 | 다음 exact GUI activation에서의 처리 |
| --- | --- |
| 기존 stack count 증가 | 증가한 count로 fresh plan |
| 빈 slot에 새 stack 생성 | 새 logical slot을 fresh plan에 포함 |
| retained tool damage 증가 | 현재 durability로 주력 도구 재선정 |
| 음식·횃불·화살 소비 | 현재 수량으로 reserve 재계산 |
| logical slot swap | 현재 logical slot 기준 manifest |
| armor/offhand 변경 | 현재 장착 상태를 `KEEP_LOADOUT` |
| server authoritative update | 현재 snapshot에 반영 |

사용자, Baritone, 다른 mod 또는 server update인지 추적해 policy를 나누지 않는다.
Inventory mutation provenance를 붙이거나 upstream engine을 수정하지 않는다.

Manifest activation 뒤에는 immutable activation inventory baseline과 active
manifest/transfer context에 대해 다음 변경을 모두 fail-closed로 처리한다.

```text
main inventory occupied/empty state
item ID, count, damage and metadata/component/NBT fingerprint
logical slot and selected main-hand slot
armor and offhand state
activation-time disposition binding
logical source slot resolver unavailable or non-unique
cursor safety
world/dimension context
trusted destination binding
```

Fresh plan revision과 activation-time disposition binding은 이 baseline에 결합된
불변 기록이다. Activation 뒤 planner를 호출해 disposition을 재평가하지 않는다.
Live player-held state가 baseline + confirmed-delta overlay에서 계산한 expected
state와 달라지거나 manifest step이 그 revision의 `STORE_HOME` binding을 잃은
경우에만 fail-closed 판정을 한다.

Executor가 소유한 pending slot의 paired-delta-confirmed 감소만
`progress.confirm(...)` 경로로 session-local confirmed-delta overlay와 progress에
반영한다. Immutable activation baseline 자체는 절대 바꾸지 않는다. Validator는
baseline + overlay에서 exact expected current state를 만들며, Transfer manifest
schema는 계속 `STORE_HOME` source steps만 보유하고 KEEP/empty slot을 transfer
step으로 추가하지 않는다. 이 expected-state 검증이 planner-relevant player-held
state의 외부 mutation을 감지한다. 새 stack을 같은 session에 자동 추가하지 않으며
변경은 `MANIFEST_STALE`로 종료한다.

Transfer-authoritative fresh plan과 manifest activation이 허용되는 경계는 다음뿐이다.

```text
처음 exact trusted GUI에 결합하고 activation gate를 통과한 시점

OR

이전 candidate session이 pending 없이 안전하게 종료되고
다음 candidate의 exact trusted GUI에 새로 결합한 시점
```

§17과 §26.7의 terminal reporting용 final read-only plan은 click, retry, manifest
activation 또는 same-GUI recovery 권한이 없는 비권위적 관찰이므로 이 경계의 예외가
아니다.

다음 recovery는 금지한다.

```text
MANIFEST_STALE 뒤 같은 GUI replan
fingerprint mismatch step만 새로 찾기
같은 Item의 다른 logical slot 대체
TRANSFER_UNCONFIRMED 뒤 replan
cursor/context terminal 뒤 replan
pending click을 버리고 새 manifest 생성
safety-chain resume 뒤 active manifest를 fresh plan으로 교체
```

### 26.6 Trust, pending과 safety-chain

| 상태 | 계약 |
| --- | --- |
| A 이동 중 untrust, B 존재 | A 단방향 제외, B로 이동 |
| A 이동 중 untrust, B 없음 | confirmed 누계에 맞는 no/partial destination terminal |
| A GUI open, click 전 untrust, pending 없음 | A session 종료·제외, B exact GUI에서 fresh plan |
| active binding 상실, pending 없음 | session 종료·candidate 단방향 제외, 같은 GUI 재개/replan 금지 |
| trust/binding 상실, pending 있음 | `TRANSFER_UNCONFIRMED`, B 이동과 replan 금지 |
| navigation 중 safety-chain suspend/resume | active manifest 없이 context/cursor/trust 재검증 |
| transfer 중 suspend/resume, baseline + overlay expected state와 source/resolver contract 동일 | 같은 manifest와 session identity로 계속 |
| transfer 중 suspend/resume, activation inventory/source/resolver contract 변경 | `MANIFEST_STALE`, fresh replan 금지 |
| transfer 중 suspend/resume, cursor/context/trust/binding 변경 | 각 전용 terminal 또는 candidate-local 규칙 적용 |
| 새 UserTask가 root 교체 | `INTERRUPTED`, 자동 재개 없음 |

같은 candidate의 binding이 잠시 사라졌다가 돌아와도 해당 candidate는 현재 operation에서
이미 단방향 제외됐으므로 기존 manifest를 재개하거나 새 plan을 만들지 않는다.
Pending uncertainty가 있으면 항상 후보 전환보다 `TRANSFER_UNCONFIRMED` 안전 경계가
우선한다.

### 26.7 A에서 B로 이동할 때의 누적 의미

```text
A paired-delta confirmed item count
    -> operation.confirmedStoredItems에 즉시 누적

A capacity exhausted and no pending
    -> A local plan/progress/session 폐기
    -> capacity failure 기록

B exact GUI activation
    -> 현재 inventory fresh snapshot
    -> 새 plan revision과 local manifest
```

B plan에는 A에서 이미 저장된 item이 player inventory에 없으므로 다시 포함되지 않는다.
A remaining과 B remaining을 합산하지 않는다.

Operation metric은 다음 의미를 유지한다.

```text
storedItems
    = A confirmed + B confirmed + C confirmed

touchedStacks
    = confirmed transfer가 한 번 이상 있었던 distinct logical player slot
    = local touched count의 합이 아님

remainingStacks
    = 안전한 latest/final fresh read-only plan의 STORE_HOME step 수
    = unsafe terminal이면 마지막 authoritative reporting 값

goalSatisfied
    = COMPLETED
    AND pending 없음
    AND (
        authoritative advisory preflight remaining 0
        OR latest authoritative session progress remaining 0
    )
```

Capacity와 unavailable failure가 섞일 때의 terminal 우선순위는 기존
`StoreHomeResult` 의미를 유지하며 이번 lifetime 문서화에 몰래 바꾸지 않는다.

`latestKnownRemainingStacks`의 갱신 경계는 다음으로 제한한다.

```text
non-empty advisory preflight
    -> 최초 reporting baseline만 저장
    -> transfer 또는 manifest authority 없음

trusted-container session activation
    -> fresh plan의 STORE_HOME step 수로 교체

paired-delta confirmation
    -> current local progress remaining으로 갱신

capacity/destination exhaustion 직전의 안전한 final read-only plan
    -> terminal reporting 값으로 교체

MANIFEST_STALE / CONTEXT_CHANGED / INTERRUPTED / TRANSFER_UNCONFIRMED
    -> behavior replan 없이 마지막 authoritative reporting 값 보존

advisory baseline 전의 조기 rejection
    -> 기존 non-negative fallback 사용
    -> zero surplus 또는 goal satisfaction 증거 아님
```

### 26.8 최소 source 변경 경계 제안

향후 별도 승인이 있을 때 검토할 최소 범위는 다음이다.

```text
StoreHomeTask
    manifest 생성 시점 이동
    local manifest lifecycle
    operation accumulator 연결
    navigation root manifest validation 제거

StoreHomePhase
    existing phase 의미 재배치 또는 최소 phase 추가

small LAVI-owned operation progress helper
    필요할 때만 cumulative storedItems, touched logical slots,
    latest remaining과 candidate failures 소유

HomeStorageManifestProgress
    current trusted-container session progress로 의미 축소
    immutable activation inventory baseline과 confirmed-delta overlay 검증
```

Single-snapshot 계약은 기존 snapshot value를 확장하거나 작은 LAVI-owned immutable
value를 추가하는 방식 중 더 좁은 쪽을 선택할 수 있다. 별도 inventory reader,
manager/service framework 또는 planner와 baseline을 위한 이중 live read는 만들지 않는다.
Confirmed-delta commit도 같은 Task-owned transition 안에서 처리하며 executor의
public behavior나 새 동시성 체계를 요구하지 않는다.

다음 구성요소는 재사용하며 이번 방향 때문에 재설계하지 않는다.

```text
HomeLoadoutPlanner
HomeStorageInventorySnapshotReader
HomeStorageStackFingerprint policy
HomeStoragePlan / HomeStorageManifest / HomeStorageManifestStep
HomeStorageScreenSlotResolver
HomeStorageTransferExecutor
HomeStorageTransferDeltaVerifier
trusted repository
destination selector
candidate queue policy
StoreHomeResult
typed bridge
TaskRunner
UserTaskChain
PlayerInteractionFixChain
Baritone
adris/**
```

이 범위는 제안일 뿐 source 승인이나 파일 수 확정이 아니다. Engine-wide lifecycle,
Baritone block breaking/pickup/path behavior와 generic interaction을 변경하지 않는다.

### 26.9 Source/JAR parity gate

25.9절의 evidence tuple을 닫기 전에는 reason-label 문제를 lifetime behavior patch에
포함하지 않는다. 최소 판정은 다음을 구분한다.

```text
count-only mutation
    -> planned_count_changed
    -> fullFingerprintEqual=true
    -> countEqual=false

identity mutation
    -> planned_fingerprint_changed
    -> item/damage/metadata/full fingerprint 중 실제 차이 기록

all visible identity equal but full fingerprint false
    -> matcher/canonicalization/capture timing 조사

built, deployed and runtime artifact identity mismatch
    -> behavior 판정 중단
```

Commit hash만으로 dirty/untracked build input을 증명할 수 없다. Relevant source hash,
dirty diff 또는 build-input manifest와 built/deployed/runtime JAR identity를 함께
기록한다.

### 26.10 Focused regression-test contract

이 matrix는 필요한 test source의 계약이며 이번 docs-only 작업에서 구현하거나
실행하지 않는다. Test는 실제 Baritone 동작을 문자열로 추측하지 않고 client-tick
기반 deterministic fake/seam으로 root identity, planner invocation, active manifest,
pending, click, paired delta, accumulator와 terminal을 직접 관찰해야 한다.

#### A. Activation과 navigation

| Scenario | Expected |
| --- | --- |
| preflight 0 step | root 1, `nothing_to_store`, `remaining_stacks=0`, `goal_satisfied=true`, queue/navigation/open/click/active manifest 0 |
| preflight 1+ step | advisory planner 1회, preflight result 폐기, navigation active manifest/progress 0 |
| candidate-local binding/handler/trust activation 실패, pending 없음 | 해당 activation attempt의 active manifest 생성 0; candidate 제외 후 다음 candidate |
| activation 중 cursor/context 실패 | active manifest 생성 0; 각각 기존 cursor/context terminal |
| activation 진입 때 pending 결과 불확실 | active manifest 생성 0; `TRANSFER_UNCONFIRMED`, 다음 candidate/replan 0 |
| 모든 activation gate 통과 | preflight와 별도로 최신 snapshot planner 추가 1회, 새 session ordinal/revision과 local manifest 1개 |
| activation single-snapshot identity | planner input, full baseline, disposition binding과 manifest가 같은 capture에서 파생; second live read 0; live mutable `ItemStack` retention 0 |
| cobblestone 55→56 during navigation | stale 0, 도착 expected count 56 |
| dirt 4→5 during navigation | stale 0, 도착 expected count 5 |
| 새 stack, tool damage, reserve 소비, slot swap | 도착 fresh plan에 현재 상태 반영 |
| armor/offhand 변경 | 도착 actual equipped state `KEEP_LOADOUT` |
| navigation suspend 중 inventory 변화 | resume safety 재검증 뒤 다음 activation fresh plan에 반영 |

모든 navigation case는 root manifest validation과 QUICK_MOVE가 0이고 operation/root
identity가 유지돼야 한다. Lifetime patch가 새 global Baritone cancel, goal/path
cleanup 또는 input release를 호출하지 않는지도 확인하되, Baritone 자체의 정상적인
내부 path replacement/cancellation을 금지하는 assertion으로 확대하지 않는다.

#### B. Activation 뒤 strict stale와 executor-owned delta

| Scenario | Expected |
| --- | --- |
| active source count 외부 증가 또는 감소 | `MANIFEST_STALE`, 추가 click/replan 0 |
| active source item/damage/metadata 변경 | `MANIFEST_STALE`, substitute 0 |
| active source logical slot 이동 또는 교체 | `MANIFEST_STALE` |
| activation 때 empty였던 main slot에 새 stack 생성 | `MANIFEST_STALE`, same-session 편입 0 |
| KEEP/reserve stack, armor/offhand 또는 selected slot 변경 | `MANIFEST_STALE`, post-activation planner 0 |
| completed source slot에 같은 fingerprint stack 재등장 | `MANIFEST_STALE` |
| logical-slot resolver unavailable 또는 non-unique | `MANIFEST_STALE`, offset fallback 0 |
| 같은 Item이 다른 slot에 존재 | 대체 금지 |
| source 감소 + destination 증가 + cursor empty | paired delta confirm, immutable baseline 유지, overlay/progress와 operation 누계만 증가 |
| confirmed-delta commit ordering | overlay/progress/operation 누계를 같은 Task transition에서 exactly once 갱신; 다음 root validation은 새 expected state로 통과; 관찰 가능한 partial state 0 |
| duplicate 또는 replay된 confirmation | storedItems, touchedStacks, overlay와 local progress의 두 번째 증가 0 |
| source만 감소 또는 destination delta 없음 | 즉시 성공 0, 기존 WAITING과 bounded timeout/terminal 의미 유지 |
| pending 중 stale/context 상실 | pending을 버린 fresh manifest 0 |

#### C. A에서 B fallback

| Scenario | Expected |
| --- | --- |
| A capacity 0, B 여유 | A 제외, B exact GUI에서 fresh plan |
| A partial, B 여유 | A confirmed 누적, B fresh plan |
| A partial 뒤 B 이동 중 새 item | B plan에 새 item 반영 |
| A partial 뒤 B 이동 중 reserve 소비 | B reserve 재계산 |
| 같은 logical slot을 A/B에서 부분 전송 | storedItems 합산, touchedStacks operation당 한 번 |
| A session 뒤 B session 활성화 | `containerSessionOrdinal`과 `planRevision` 모두 변경, preflight revision 재사용 0 |
| A 종료 뒤 B fresh plan empty | pending 없으면 `COMPLETED` |
| A/B full, confirmed 있음 | `PARTIAL_TRUSTED_CAPACITY_EXHAUSTED` |
| A/B full, confirmed 0 | `NO_TRUSTED_CAPACITY` |
| A unavailable, B 성공 | 일반 container 접근 0 |
| capacity와 unavailable failure 혼합 | 기존 terminal precedence 유지; lifetime patch가 enum 의미를 변경하지 않음 |

Matching LAVI request의 모든 A→B case는 submission, root와 operation ID가 하나이며
operationTicks, confirmed metrics와 failure history가 초기화되지 않아야 한다.

#### D. Trust, pending과 lifecycle

| Scenario | Expected |
| --- | --- |
| A 이동 중 untrust, B 존재 | A 재시도 없이 B 이동 |
| A 이동 중 untrust, B 없음 | no/partial destination terminal |
| A GUI open, pending 없이 untrust | A session 폐기 후 B fresh plan |
| active binding loss, pending 없음 | A session 종료와 단방향 제외, same-candidate resume/replan 0 |
| pending 중 untrust 또는 binding loss | `TRANSFER_UNCONFIRMED`, B/replan/click 0 |
| operation 중 C 새 trusted 등록 | 현재 queue 합류 0 |
| navigation suspend/resume | manifest 없음, context/cursor/trust 재검증 후 계속 |
| transfer suspend/resume, baseline + overlay expected state와 source/resolver 동일 | same session/manifest 계속 |
| transfer suspend/resume, activation inventory/source/resolver contract 변경 | `MANIFEST_STALE`, replan 0 |
| transfer resume에서 cursor/context/trust 변경 | 각각 기존 cursor/context/trust-pending terminal 유지 |
| 새 UserTask | `INTERRUPTED`, 자동 재개 0 |
| world/dimension 변경 | `CONTEXT_CHANGED` |
| navigation 또는 transfer 중 cursor non-empty | 추가 open/click 0, 기존 cursor terminal |
| navigation no-progress 또는 local interaction timeout, pending 없음 | phase별 stable reason으로 candidate 제외 후 다음 후보 |
| safety-chain이 어떤 timeout window보다 오래 선점 | 모든 StoreHome-owned clock 미산입; resume 뒤 같은 attempt clock과 context 재검증 |
| path/goal recalculation/re-adoption/재제출, sub-threshold position jitter 또는 epsilon 미만 distance 변화만 있고 semantic progress 없음 | progress reset 0; no-progress 판정 유지 |
| candidate switch만 있고 actual progress 없음 | operation no-progress/emergency elapsed와 confirmed metrics/failure history reset 0; local clock은 handoff 전 UNSTARTED |
| interaction-neighborhood 경계 진동, screen/handler/binding flicker, 반복 click 또는 path/goal update | local elapsed silent reset 0; navigation 복귀 시 누적 elapsed 보존 또는 명시적 새 attempt generation |
| candidate phase timeout, pending 불확실 | `TRANSFER_UNCONFIRMED`; 다음 candidate/replan/추가 click 0 |
| operation no-progress, pending 없음 | `operation_no_progress`; 누적 결과에 따른 기존 no/partial terminal |
| emergency hard cap, pending 없음 | `operation_emergency_hard_cap` operation-level terminal; candidate-local rejection으로 기록 0 |
| operation timeout boundary, pending 불확실 | `TRANSFER_UNCONFIRMED`; 다음 candidate/replan/추가 click 0 |
| phase별 네 timeout 경로 | 네 lower-snake stable reason을 서로 합치지 않고 구분 |

#### E. 불변조건과 payload

| Scenario | Expected |
| --- | --- |
| explicit request 없음 | StoreHome Task/planner/navigation/open/click 0 |
| inventory pressure | automatic StoreHome 0 |
| 일반 상자만 존재 | fallback 0 |
| matching LAVI request 1건 | submission/root/operation/matching typed result 각각 1 |
| direct in-game `@store_home` | local root/operation/terminal 1; matching bridge typed result 요구 없음 |
| local manifest 교체 | storedItems, touchedStacks, timeout과 failures 보존 |
| A confirmed 40, B confirmed 12 | `stored_items=52`; click/source-only delta는 미포함 |
| same logical slot을 A와 B에서 touch | `touchedStacks=1` |
| terminal remaining | A+B local remaining 합산 금지; latest/final safe plan 사용 |
| stale/context/interrupted/unconfirmed | behavior planner 0, 마지막 authoritative remaining 보존 |
| advisory baseline 전 조기 rejection | 기존 non-negative fallback, `goal_satisfied=false` |
| stale/unconfirmed | automatic retry/replay/replan 0 |
| no-op completion | `stored_items=0`, `remaining_stacks=0`, `goal_satisfied=true` |

`nothing_to_store`는 두 경계로 나눠 검증한다.

```text
StoreHomeTask lifecycle focused test
    StoreHomeCommand creates one StoreHomeTask
    no-op completes inside that Task lifecycle
    candidate/open/click/session activation 0

matching LAVI bridge idle-root regression
    matching StoreHomeTask terminal만 typed COMPLETED로 투영
    pre-existing IdleTask를 command-owned root로 오인하지 않음
    후속 command가 false busy로 남지 않음
```

Command acceptance 때 cursor가 이미 non-empty인 기존 command-gate synchronous
`CURSOR_NOT_EMPTY` rejection은 별도 기존 계약으로 보존한다. `nothing_to_store`를
그 경로로 옮기지 않는다. `TaskRunner`, `UserTaskChain`, Baritone와 `adris/**`의
source 비변경은 runtime assertion이 아니라 diff/scope static gate로 검증한다.

### 26.11 승인과 중단 gate

이 절을 처음 문서화한 작업은 docs-only였다. 2026-08-28 후속 behavior 승인의 현재
상태는 §26.12에서 별도로 갱신한다. 아래 목록은 그 manifest-lifetime change unit의 역사
승인 경계다. 현재 exact GUI stabilization의 source/test/required clean build에 새 정지점을
만들지 않으며, 외부 배포·Minecraft runtime·commit·push에는 계속 별도 범위가 필요하다.

```text
focused test source 작성
Java behavior source 수정
diagnostics parity evidence 수집
test 또는 Gradle 실행
clean forced build
JAR 배포
Minecraft runtime reproduction
runtime log/crash review
commit
push
```

첫 behavior change는 LAVI-owned `StoreHomeTask` 경계에서 최소화한다. 다음 중 하나가
필요하면 구현하지 않고 실패 경계와 범위 확대 이유를 보고한다.

```text
adris/** behavior change
TaskRunner or UserTaskChain change
PlayerInteractionFixChain change
Baritone input, goal, path or pickup behavior change
generic container interaction change
same-GUI automatic replan or retry
fingerprint policy change mixed into lifetime patch
planner와 activation baseline을 동일 immutable capture에서 만들 수 없음
confirmed delta를 다음 revalidation/session transition 전에 overlay/progress/operation 누계에 exactly once commit할 수 없음
```

### 26.12 Phase-scoped timeout source implementation status

2026-08-28 후속 사용자 승인으로 고정 candidate lifetime timeout을 phase-scoped
timeout으로 교체하는 behavior source와 focused JUnit source를 현재 dirty worktree에
구현했다.

```text
NAVIGATE_TO_CANDIDATE
    no-progress window = 2,400 active-root ticks
    movement threshold = 0.5 block
    best-distance epsilon = 1.0 block
    progress metric = player Vec3d to candidate block center, 3D Euclidean blocks
    comparison = movement > 0.5 block OR best-distance improvement > 1.0 block

OPEN_AND_BIND_CANDIDATE
    local window = 2,400 active-root ticks
    handoff = exact binding OR existing InteractWithBlockTask current reach

operation_no_progress = 12,000 active-root ticks
operation_emergency_hard_cap = 120,000 active-root ticks
```

Timeout 책임은 `execution/timeout/**` 아래 policy, navigation, candidate, operation과
decision component로 분리했다. Candidate switch는 candidate-local clock만 초기화하며
operation clock, confirmed transfer 누계와 failure history는 보존한다. Safety-chain
선점 중에는 `StoreHomeTask.onTick()`이 호출되지 않으므로 어느 clock도 소비하지 않는다.

`execution/state/StoreHomeExecutionState`는 mutable state bag이 아니라 composition
root이며, 실제 mutable state는 `state/lifecycle`, `state/operation`, `state/context`,
`state/candidate`, `state/session`, `state/reporting`의 focused owner로 분리했다.
`StoreHomeTask`는 stable Task facade/API만 유지한다. Top-level lifecycle 순서는
`StoreHomeTaskLifecycleController`, terminal commit/report 순서는
`StoreHomeOperationTerminator`가 소유한다. Diagnostics는 event orchestration,
operation/candidate bookkeeping, progress snapshot, budget/emission,
boundary enablement와 diagnostic-only bookkeeping failure containment를 각각의
`diagnostics/.../timeout/**` component로 분리했다. Boundary action의 engine-state read
예외는 diagnostics가 catch하거나 suppress하지 않는다.

2026-08-29 후속 구조 작업에서는 `StoreHomeTask` 내부의 독립 stage도 composition으로
분리했다. 2차 구조 감사에서 root에 남았던 dependency assembly, lifecycle orchestration,
read-only result projection까지 각각 `execution/task/{composition,lifecycle,view}`로
추출했다. `StoreHomeTask`는 기존 FQN, 두 public constructor, 직접 선언된 `Task` override와
public result API만 유지하는 약 150줄의 thin facade다. Top-level tick ordering은 같은
state와 timeout clock을 공유하는 `StoreHomeTaskLifecycleController`가 소유한다.
`execution/{initialization,context}`, `execution/candidate/{selection,navigation,rejection,view}`,
`execution/session/{activation,flow,transfer}`, `execution/operation/{pending,reporting,terminal}`,
`execution/timeout/{candidate,decision}`의 focused collaborator가 각 단계 하나를 소유한다.
기존 공개 constructor/API, `state`·`timeoutLifecycle` reflection 경계, child Task 반환,
pending precedence, candidate rejection/activation/confirmed-commit/terminal 순서는 유지했다.
Upstream Task hierarchy나 Baritone lifecycle을 새로 상속·복제하지 않았다.

직접 영향을 받는 LAVI-owned slot/transfer/fingerprint 코드도 책임별로 분리했다.
행동 계약은 `execution/slot`과 `execution/transfer`, live observation과 stale evidence
조립은 `diagnostics/container/home/{slot,fingerprint,transfer}`가 소유한다.
`HomeStorageTransferExecutor`는 exact transfer 순서만 조정한다. Container/capacity,
QUICK_MOVE readiness/발행, pending lifecycle/검증, paired-delta 값 계약과 neutral failure
observation은 각각 `execution/transfer/{container,click,pending,delta,failure}`의 focused
type이 소유한다. Executor는 diagnostics DTO/import/호출을 소유하지 않으며 Task가 같은
tick에 neutral failure observation을 stale diagnostics snapshot으로 변환한다. 기존
status, reason과 click ordering은 불변이다.

Local handoff는 attempt당 한 번만 시작하고 reach/binding/screen flicker로 reset하지
않는다. Exact activation이 local timeout boundary와 같은 tick에 성립하면 activation을
먼저 확정해 candidate timeout을 비활성화한다. Transfer pending과 timeout이 겹치면
`TRANSFER_UNCONFIRMED`가 우선하며 다음 candidate, replan과 추가 click을 실행하지 않는다.
Emergency hard cap은 후보 선택과 child behavior 전에 판정한다. Pending ownership,
context와 cursor guard도 기존 선행 우선순위를 유지한다. Operation no-progress는 active
candidate가 있고 session/pending이 없는 좁은 경계에서만 같은-tick 현재 위치와 exact
activation을 먼저 관찰한 뒤 재판정한다. 이 경계에서는 full navigation/session step을
실행하지 않는다. Active session, pending transfer 또는 active candidate 부재 상태에서는
새 click/후보 선택보다 operation no-progress를 먼저 판정한다. 정상 navigation/session
step이 허용된 경로에서는 그 step 뒤 operation no-progress, 그 뒤 candidate timeout을
판정한다. 따라서 capacity/terminal/typed projection 전체가 모든 timeout보다 보편적으로
먼저라는 계약은 아니다.

Stable reason은 `candidate_navigation_no_progress`,
`candidate_local_interaction_timeout`, `operation_no_progress`,
`operation_emergency_hard_cap`으로 분리했다. Candidate reason은 rejection history에,
operation reason은 기존 typed terminal payload에 보존한다. 모든 candidate 소진의 기존
typed result/terminal precedence와 `trusted_candidates_exhausted` reason은 유지한다.

현재 분리된 증거 상태는
`CLEAN_BUILD_JUNIT_VERIFIED_DEPLOY_FILE_IDENTITY_VERIFIED_RUNTIME_BEHAVIOR_OBSERVED`다. 2026-08-29
folderization 뒤 clean forced build는 171/171 tasks executed와 1.20.1 JUnit 243 tests,
0 failures, 0 errors를 기록했다. Built/deployed JAR은 모두 7,113,261 bytes와 SHA-256
`C939E5A859BB66535FADCB5522A4D9CD99AF2CAC7E90B6668A2945CA71AA7933`로 일치했다.
두 runtime operation은 exact activation, capacity fallback, paired-delta transfer와
`COMPLETED`를 확인했으며 timeout decision은 0건이었다. `2,400` tick을 넘는 장거리
candidate 유지의 직접 증거는 operation 225이고, operation 30351은 반복 완료/no-timeout
관찰이다.

이 결과는 실제 장시간 safety-chain 선점, 네 timeout reason의 발동, pending timeout에서
container click 0회를 모두 재현했다는 뜻이 아니다. Runtime manifest의 complete
source/build-input identity도 없으므로 `PARITY_UNPROVEN`을 유지한다. Repository commit
readiness는 별도 Python `deposit_all` catalog/registry contract failure, 아래 direct-test
gap과 혼재된 dirty-worktree/change scope가 남아 있어 `NOT_READY`다. `adris/**`, TaskRunner,
UserTaskChain, `PlayerInteractionFixChain`과 Baritone behavior source 변경은 0이다.

### 26.13 2026-08-29 final cross-review correction

앞선 clean build의 243개 JUnit 성공은 실행 당시 artifact의 기존 suite 결과다. 다음
production semantic gate는 그 수치에 포함됐다고 주장하지 않으며 commit 전에 별도 direct
coverage로 닫아야 한다.

```text
StoreHomeTaskLifecycleController decision-order coverage
    emergency hard cap과 pending/context/cursor precedence 유지
    active candidate + no session + no pending에서만 current position과
      exact activation을 operation no-progress 재판정 전에 소비
    narrow boundary에서는 full navigation/session step 0
    normal step 뒤 operation no-progress, 그 뒤 candidate timeout
    pending은 TRANSFER_UNCONFIRMED 우선
    activation/rejection/terminal transition exactly once

StoreHomeTask shared object-graph identity coverage
    root/controller/view의 exact StoreHomeExecutionState assertSame
    root/controller/timeout collaborator의 exact StoreHomeTimeoutLifecycle assertSame

command lifecycle identity integration
    assertSame(submittedRootTask, terminationObservation.task())
    FabricChatClefStoreHomeResultProjector가 그 observation Task의 typed outcome을 읽음
```

`StoreHomeTaskAssembly`는 root Task를 생성하거나 저장하지 않는다. Controller와 view에도
Task back-reference가 없고 projector는 `terminationObservation.task()`를 읽는다. 따라서
존재하지 않는 `lifecycleController.task()` 또는 `resultProjector.task()`를 만들도록 요구하지
않는다. Shared state/clock wiring과 외부 command-lifecycle Task identity를 소유권별로
분리해 검증한다.

Python command catalog의 current 상태도 독립 gate다.

```text
Java registered-command snapshot: 22
support-matrix rows: 22
KoreanChatClefCommandRegistry: 21
missing production metadata: deposit_all
focused pytest: 37 collected, 36 passed, 1 failed
```

`deposit_all`은 raw-only shadow metadata로만 동기화하고 expected count를 22로 올려야 한다.
Public/parser/admission/bridge/gameplay-ready 집합은 모두 false, allowed sources는 empty로
유지한다. Exact-set 검사를 약화하거나 테스트를 deselect/retry해서는 안 된다. Registry
parity, 위 세 direct coverage gate와 zero-failure focused rerun이 모두 닫히기 전까지 최종
문서 방향은 `CONDITIONAL PASS`, merge/commit readiness는 `BLOCKED`다.

### 26.14 2026-08-29 final direct-gate and clean-build verification

§23의 243-test artifact/runtime 기록과 §26.13의 pending blocker 수치는 역사적 snapshot으로
보존한다. 그 뒤 Python registry closure와 세 STORE_HOME direct semantic gate를 다음 실제
결과로 닫았다.

```text
registered-command snapshot / support matrix / production registry: 22 / 22 / 22
focused Python: 38 tests passed, 179 subtests passed, 0 failures,
                no deselection or automatic retry
full focused Python closure: 404 passed, 2 pre-existing live-test skips,
                             2973 subtests passed, 0 failures

StoreHomeTaskLifecycleController decision-order direct tests: 8 passed
StoreHomeTask shared object-graph identity tests: 2 passed
command -> termination observation -> projector identity test: 1 passed
targeted Java aggregate: 8 classes, 32 tests, 0 failures, 0 errors, 0 skipped
```

기존 `.ps1`을 visible PowerShell 새 창에서 사용해 runtime root의 정확한
`clean build --rerun-tasks`를 실행한 결과는 다음과 같다.

```text
command: .\gradlew.bat clean build --rerun-tasks
result: BUILD SUCCESSFUL in 2m 45s; 171 actionable tasks, 171 executed
full 1.20.1 JUnit: 254 tests, 0 failures, 0 errors, 14 skipped
file: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
size: 7,115,508 bytes
sha256: D2175E8B0F8943388324929C6A6BAC16036EA8584848C25554440100156DFC8B
log: codex-build-logs/chatclef-fabric-1.20.1-build-20260829-140028.log
```

최신 254-test clean build는 앞선 243-test build와 그 artifact의 runtime evidence를
대체하거나 무효화하지 않는다. 최신 JAR은 active instance의
`C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar`에
같은 7,115,508-byte 크기와 SHA-256으로 배포됐다. `latest.log`는 이 code source의 로딩,
WebSocket handshake 수락, 관련 Mixin/FATAL 또는 새 crash report가 없음을 기록했다.
같은 artifact의 `store_home` operation은 `paired_delta_confirmed`로 `COMPLETED`됐고
`storedItems=137`, `remainingStackCount=0`, `capacityFailureCount=2`였다. LAVI application
log도 callback과 matching task-finished event로 같은 typed result를 확인했다.

이 file/runtime evidence는 runtime manifest의 repository/source/build-input provenance를
새로 만들지 않으므로 해당 필드는 계속 `UNVERIFIED`, `artifactParity`는
`PARITY_UNPROVEN`이다. STORE_HOME behavior, timeout 수치, Baritone, TaskRunner와
`InteractWithBlockTask`는 이번 closure에서 변경하지 않았다. 검증된 source/test는
`ab0355b1`, Windows clean-build helper는 `d0a23ea`에 각각 기록했으며 문서 commit과 push는
이 기록 뒤의 별도 Git 단계다.

## 27. 관련 문서

- [Minecraft Backend Separation](minecraft-backend-separation.md)
- [ChatClef / Carry On Integration Direction](chatclef-carryon-integration-direction.md)
- [ChatClef @deposit_all Ocean Loop Diagnostics Plan](chatclef-deposit-all-ocean-loop-diagnostics-plan-2026-08-26.md)
- [ChatClef Command Lifecycle And Threading](chatclef-command-lifecycle-and-threading.md)
- [Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md)
- [ChatClef Deposit Sync-Finish Idle Root Investigation](chatclef-deposit-sync-finish-idle-root-investigation-2026-08-21.md)
- [Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)
- [ChatClef JVM G1 Remark Native Crash Investigation](chatclef-jvm-g1-remark-native-crash-investigation-2026-08-27.md)
- [ChatClef STORE_HOME Long-Distance Timeout Investigation](chatclef-store-home-long-distance-timeout-investigation-2026-08-28.md)

이 문서는 `STORE_HOME`의 activation, item disposition과 destination policy에 대해
기존 automatic-deposit 문서보다 우선한다. 기존 문서의 runtime evidence,
`@deposit_all` 진단과 trusted registry 구현 기록은 역사적·기술적 근거로 유지한다.
