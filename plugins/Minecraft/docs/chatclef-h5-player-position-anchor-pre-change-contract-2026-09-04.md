<!-- 20260904_kpopmodder: Defined the H5 player-position anchor contract and recorded its worktree implementation verification. -->
<!-- 20260904_kpopmodder: Reconciled the current build, matching active-instance deployment, and partial live player-position registration evidence before commit. -->

# ChatClef H5 Player-Position Anchor Implementation Contract and Verification Record

## 1. Document status

```text
DOCUMENT_TYPE: IMPLEMENTATION_CONTRACT_AND_VERIFICATION_RECORD
DECISION_DATE: 2026-09-04
DECISION_STATUS: APPROVED_IMPLEMENTED_AND_RUNTIME_PARTIALLY_VERIFIED
DOCUMENT_REVIEW_STATUS: REVIEWED_2026-09-04; SOURCE_BUILD_DEPLOYMENT_RUNTIME_AND_CANONICAL_CROSS_CHECKED
PRE_CHANGE_SOURCE_ANCHOR: CROSSHAIR
CURRENT_WORKTREE_SOURCE_ANCHOR: PLAYER_BLOCK_POSITION
TARGET_BATCH_ANCHOR: PLAYER_BLOCK_POSITION
TARGET_VOLUME: FIXED_HALF_OPEN_16_X_16_X_16
SOURCE_CHANGE_IN_THIS_TASK: IMPLEMENTED_IN_WORKTREE
TEST_EXECUTION_IN_THIS_TASK: PASSED; 1.20.1=707_TESTS_0_FAILURES_0_ERRORS_1_SKIPPED; ALL_11_VERSION_TEST_TASKS_PASSED
BUILD_IN_THIS_TASK: PASSED; CLEAN_BUILD_RERUN_TASKS; 171_TASKS_EXECUTED
ARTIFACT_INSPECTION_IN_THIS_TASK: PASSED; FRESH_1.20.1_REMAPPED_JAR_AND_EXPECTED_CLASSES_PRESENT
ARTIFACT_SHA256_IN_THIS_TASK: 24DD4C8DEBA968D0206CA3D68A227C57D513E55769F1BB51F5943F549989CC27
DEPLOYMENT_IN_THIS_TASK: PASSED; ACTIVE_LAVI_TEST_FABRIC01_JAR_SHA256_MATCH
MINECRAFT_RUNTIME_IN_THIS_TASK: PARTIAL_PASS; PLAYER_POSITION_BATCH_UPDATED_REGISTRATION_OBSERVED
RUNTIME_ACCEPTANCE_REMAINING: NO_CHANGE_RERUN + PLAYER_MOVE_RANGE_SHIFT + EXPLICIT_EDGE_TESTS + NEWLY_ADDED_DESTINATION_SELECTION NOT_RUN
COMMIT_PUSH_IN_THIS_TASK: NOT_PERFORMED_AT_DOCUMENT_SNAPSHOT
```

이 문서는 H5 bulk trusted-destination registration의 구현 계약과 worktree 구현 결과를 함께
기록한다. 현재 Java source는 player feet `BlockPos`를 batch anchor로 사용한다. 생성된 JAR과
`LAVI_TEST_Fabric01` active JAR의 SHA-256이 일치하며, 한 번의 live batch registration에서
`PLAYER_BLOCK_POSITION`과 registry `13 -> 22` 변경이 확인됐다. 이 증거는 전체 runtime
acceptance나 downstream automatic transfer 성공으로 확대하지 않는다.

기존 canonical H5 계약은
[ChatClef Manual Trusted Home Storage Direction §13](chatclef-manual-trusted-home-storage-direction-2026-08-27.md#13-trusted-등록-ux와-json)이
소유한다. 이 문서는 그중 batch anchor와 그에 직접 연결된 scanner precondition, result field,
zero-destination `NO_CHANGE` reason과 test acceptance만 후속 결정으로 대체·확장한다. 기존 문서의
historical crosshair 설명은 pre-change source와 배포 전 runtime baseline 기록으로 보존한다.

Canonical 문서와 이 문서의 적용 우선순위는 다음과 같이 고정한다.

| Canonical H5 clause | Worktree implementation status |
|---|---|
| §13.3의 “batch anchor는 player position이 아니다” | 이 문서 §6의 `PLAYER_BLOCK_POSITION`으로 대체 |
| §13.3의 “anchor 자체가 exact allowlist container여야 한다” | 폐기; 이 문서 §8의 geometric center/candidate 분리로 대체 |
| §13.3의 exact-open/last-interaction/nearest fallback 금지 | 유지; player position은 fallback이 아니라 유일한 batch anchor |
| §13.3의 half-open 16×16×16과 loaded-only coverage | 유지 |
| §13.4–§13.6의 allowlist, topology, transaction과 side-effect 경계 | 유지 |
| §13.5의 generic non-mutating `NO_CHANGE` | 유지하되 empty input은 `no_supported_destinations_found`로 구분 |
| §13.7 acceptance item 5의 crosshair-only/player-position-0 조건 | 이 문서 §15의 player-anchor와 single-regression items로 대체 |
| 무인자 English single-registration의 exact-open/crosshair 계약 | 유지 |

이 표는 현재 worktree source와 hash가 일치하는 active-instance deployment에 적용되는 scoped
override다. 다른 external instance는 그 instance의 실제 JAR identity와 runtime evidence로 판정한다.

## 2. User intent and exact interpretation

사용자 의도는 다음과 같다.

```text
캐릭터가 바라보는 상자를 중심으로 등록하지 않는다.
명령 실행 시 캐릭터가 서 있는 위치를 중심으로 주변 범위를 계산한다.
그 범위에서 발견한 모든 지원 storage container를 trusted destination으로 등록한다.
```

이번 변경의 **외부 behavior scope는 anchor semantics 변경으로만 제한**한다. 이를 안전하게
구현하기 위한 scanner precondition 분리, result/formatter 갱신과 focused type 추가는 포함될 수 있다.
`16x16` command token의 기존 의미는 변경하지 않는다. 따라서 scan은 X/Z 16×16 footprint와
Y 16-level window를 합친 최대 16×16×16, 즉 4096 BlockPos를 사용한다. 전 세계 높이 전체나
2차원 X/Z column 전체를 검색하지 않는다.

`주변 16×16`은 원형 반경, Manhattan distance, 캐릭터로부터 각 방향 16블록 또는
32×32 footprint를 뜻하지 않는다.

이 문서에서 `모든 상자`는 기존 H5 allowlist인 vanilla chest, trapped chest와 barrel을 뜻한다.
Shulker, ender chest, processing/utility container, entity inventory와 modded container까지 범위를
확장한다는 의미가 아니다.

## 3. Pre-change and implemented behavior

| Boundary | Pre-change source baseline | Implemented worktree source |
|---|---|---|
| Batch anchor | 안전한 crosshair block hit | 캐릭터의 현재 feet `BlockPos` |
| Aim requirement | 지원 container를 8블록 이내에서 조준해야 함 | 조준 불필요, crosshair state 무시 |
| Anchor block kind | anchor 자체가 지원 container여야 함 | 위치 기준점일 뿐이며 air/비-container 허용 |
| Screen dependency | crosshair safety가 no-screen/ChatScreen을 검사 | player anchor acquisition은 screen을 읽지 않음 |
| Post-scan validation | 같은 crosshair block인지 재확인 | 같은 player `BlockPos`와 world provenance인지 재확인 |
| Volume | anchor 기준 16×16×16 | player `BlockPos` 기준 같은 16×16×16 |
| Container allowlist | chest, trapped chest, barrel | 변경 없음 |
| Repository mutation | one all-or-none transaction | 변경 없음 |

기존 배포 runtime에서 `anchorSource=CROSSHAIR`와
`reason=safe_crosshair_anchor_unavailable`이 출력될 수 있다는 기록은 pre-change baseline이다.
현재 배포 runtime에서는 `anchorSource=PLAYER_BLOCK_POSITION`이 확인됐다. 이후 current artifact의
batch form에서 crosshair anchor 값이 다시 출력되면 regression이다.

## 4. Scope and preservation boundary

이 계약은 Fabric ChatClef 1.20.1의 LAVI-owned H5 bulk registration에만 적용한다.

```text
LAVI -> Fabric adapter -> Fabric mod -> ChatClef / AltoClef
```

다음은 변경하지 않는다.

- Forge/MineMind backend, placeholder, config, protocol 또는 test를 추가하지 않는다.
- `plugins/Minecraft/common/**`에 Minecraft runtime 구현을 넣지 않는다.
- ChatClef/AltoClef upstream-derived Task, TaskRunner, chain 또는 Baritone lifecycle을 수정하지 않는다.
- GUI open, click, input, path, goal, Carry On state 또는 container transfer lifecycle을 H5에 연결하지 않는다.
- Minecraft, Fabric, Loader, Loom, Gradle, Java, ChatClef, AltoClef, Baritone 또는 Carry On version을 바꾸지 않는다.
- dependency와 Gradle wrapper를 추가·교체하지 않는다.

H5는 계속 loaded block-state scan과 trusted repository mutation만 소유한다. GUI lifecycle,
container open lifecycle과 automatic storage transfer lifecycle은 각각 기존 owner에 남는다.

## 5. Command grammar boundary

Player-position anchor는 다음 네 batch form에만 적용한다.

```text
@auto_deposit_trust area 16x16
@auto_deposit_trust 반경 16x16
@자동보관등록 영역 16x16
@자동보관등록 반경 16x16
```

네 form은 모두 완전히 같은 player-centered snapshot을 만든다. English/Korean alias에 따라
anchor, volume, allowlist, transaction 또는 logging 의미가 달라지지 않는다.

다음 무인자 English command는 기존 single-registration 계약을 유지한다.

```text
@auto_deposit_trust
    -> exact-open binding 우선, 없으면 안전한 crosshair target 사용
```

따라서 기존 single target resolver와 crosshair safety는 제거하거나 player-position으로 바꾸지
않는다. 무인자 `@자동보관등록`을 새 single alias로 만들지 않으며 malformed, mixed,
unsupported 또는 extra argument의 fail-closed 문법도 그대로 유지한다.

Python chat/microphone natural-language route와 Fabric wire protocol 변경은 이 계약의 범위가 아니다.

## 6. Player anchor contract

### 6.1 Acquisition

Bulk command는 Minecraft client thread에서 다음을 한 번 읽어 immutable initial anchor를 만든다.

```text
required live state:
    AltoClef instance
    MinecraftClient instance
    current ClientWorld
    current ClientPlayerEntity

required production checks:
    client.isOnThread() == true
    client.world != null
    client.player != null
    client.player.getWorld() == client.world

defensive integration checks:
    client.world == mod.getWorld()
    client.player == mod.getPlayer()

anchor position:
    player.getBlockPos().toImmutable()

anchor source:
    PLAYER_BLOCK_POSITION
```

Player reader는 `AutoDepositBulkAnchorReadResult` typed outcome을 반환한다. 상태는 `AVAILABLE`,
`UNAVAILABLE`, `PLAYER_WORLD_MEMBERSHIP_MISMATCH`이며 `AVAILABLE`일 때만 immutable
`AutoDepositBulkPlayerAnchor`가 존재한다. Anchor value는 player `BlockPos`, opaque initial player
object identity와 opaque captured client-world identity를 보관한다. 마지막 값은 world가 A→B→A로
교체되는 경우에도 다른 world에서 읽은 player anchor를 이전 provenance와 결합하지 않기 위한
operation-local ABA 방지 값이다.

Existing world view는 world key, dimension identity와 client-world object identity를
`AutoDepositBulkWorldProvenance`로 제공한다. Bulk service는 두 값을 한 uninterrupted client-thread
acquisition phase에서 읽고 service-owned immutable registration snapshot으로 결합한다. Reader가
world provenance를 중복 조회하거나 내부 mutable field/global/static state에 initial identity를
숨기지 않는다.

Acquisition 순서는 다음과 같다.

```text
create one operation-local world view
-> read initial world provenance
-> read player anchor while validating client/mod world and player identity
-> read world provenance again
-> require both provenance values to be sameWorld(...)
-> require playerAnchor.belongsToWorld(secondProvenance)
-> create immutable registration snapshot
```

두 provenance가 다르거나 어느 state도 얻지 못하면 scan을 시작하지 않는다.

첫 provenance read가 exception이면 그것이 즉시 terminal failure이며 player reader를 호출하지 않는다.
다만 `provenance()==Optional.empty()`는 world-key 부재뿐 아니라 wrong client thread나 client/mod world
불일치도 나타낼 수 있는 합성 신호다. 이 경우에만 player reader를 정확히 한 번 호출해 원인을
분류한다.

```text
first provenance throws
    -> SCAN_COVERAGE_INCOMPLETE
       / world_provenance_read_failed_during_anchor_acquisition
       / anchorPos=unavailable

first provenance is empty
    -> read player anchor exactly once; do not retry provenance
       ├─ UNAVAILABLE or PLAYER_WORLD_MEMBERSHIP_MISMATCH
       │  -> INVALID_ANCHOR / player_position_anchor_unavailable
       │     / anchorPos=unavailable
       ├─ AVAILABLE
       │  -> SCAN_COVERAGE_INCOMPLETE
       │     / world_provenance_unavailable_during_anchor_acquisition
       │     / anchorPos=<captured feet BlockPos>
       └─ reader throws
          -> INVALID_ANCHOR / player_position_anchor_read_failed
             / anchorPos=unavailable
```

이 분류 단계는 scanner, topology 또는 repository를 호출하지 않으며 새 provenance를 current truth로
채택하거나 자동 retry하지 않는다. 두 provenance가 `sameWorld(...)`여도 captured player world가
second provenance의 opaque world identity와 다르면 snapshot 생성을 거절한다.

```text
reason=world_provenance_changed_during_anchor_acquisition
boundedFirstConflict=player_world_provenance_mismatch
```

`player.getBlockPos()`는 eye position이나 rounded coordinate가 아니라 플레이어의 feet 위치를
Minecraft block-coordinate floor semantics로 변환한 값이다. 특히 음수 소수 좌표는 0 방향 절삭이
아니라 floor되어야 한다.

여기서 feet block은 플레이어 발 좌표가 들어 있는 block이다. 플레이어가 밟고 있는 지면 block인
`player.getBlockPos().down()`을 사용하지 않는다. 예를 들어 Y=64인 full ground block의 top face
coordinate는 Y=65이고, 그 위에 선 player의 feet coordinate와 anchor Y는 모두 65다. Ground block
position Y=64를 anchor로 사용하지 않는다.

예시:

```text
player position = (-0.20, 64.90, -8.01)
anchor BlockPos = (-1, 64, -9)
```

Anchor acquisition은 다음을 읽거나 요구하지 않는다.

- `MinecraftClient.crosshairTarget`
- 현재 조준 대상의 block kind
- player-to-crosshair distance
- exact-open container binding
- last interaction position
- nearest container
- current screen type

Client live world/player 또는 안전한 immutable `BlockPos`를 얻을 수 없으면 status는
`INVALID_ANCHOR`이고 repository read, save, publish와 revision mutation은 모두 0이다. Live world가
존재하지만 operation-local world view 또는 provenance를 만들 수 없는 경우는 아래
`SCAN_COVERAGE_INCOMPLETE` reason을 따른다.

Canonical failure status/reason은 다음으로 고정한다.

```text
missing client/world/player, wrong client thread, client/mod identity mismatch
or player-world membership mismatch:
    INVALID_ANCHOR / player_position_anchor_unavailable

player-position reader/API exception:
    INVALID_ANCHOR / player_position_anchor_read_failed

world-view factory returns null or throws:
    SCAN_COVERAGE_INCOMPLETE / world_view_unavailable

initial provenance unavailable from an otherwise present world view:
    SCAN_COVERAGE_INCOMPLETE / world_provenance_unavailable_during_anchor_acquisition

first or second initial provenance read throws:
    SCAN_COVERAGE_INCOMPLETE / world_provenance_read_failed_during_anchor_acquisition

two successful initial provenance reads are not sameWorld(...), or the captured player world
does not match the second provenance world identity:
    SCAN_COVERAGE_INCOMPLETE / world_provenance_changed_during_anchor_acquisition
```

예외 객체, stack trace, raw player object 또는 unbounded state를 한 줄 결과에 넣지 않는다.
필요하면 bounded exception class name만 `boundedFirstConflict`에 기록한다.

### 6.2 Pre-scan binding gate

Registration snapshot의 provenance와 scanner가 실제로 읽은 initial provenance는 build-height,
bounds, chunk 또는 block read 전에 `sameWorld(...)`여야 한다. Service는 expected provenance를
scanner에 전달하고 scanner는 자체 initial provenance read 직후 다른 world state를 읽기 전에 비교한다.

```text
expected provenance matches scanner initial provenance
    -> loaded coverage evaluation 계속

scanner initial provenance read throws
    -> SCAN_COVERAGE_INCOMPLETE / world_provenance_read_failed

scanner initial provenance is unavailable
    -> SCAN_COVERAGE_INCOMPLETE / world_provenance_unavailable

scanner initial provenance exists but does not match expected provenance
    -> SCAN_COVERAGE_INCOMPLETE / world_provenance_changed_before_scan
```

세 failure 모두 `coverageComplete=false`, `scannedPositionCount=0`,
`physicalSupportedBlockCount=0`, `registryReadStatus=NOT_READ`이며 save, publish와 revision mutation이
0이다. 기존 scanner-start read-failure/unavailable reason은 보존하고 expected-snapshot mismatch에만
새 `world_provenance_changed_before_scan` reason을 사용한다.

이 gate는 old-player coordinates로 replacement world를 읽은 뒤 commit에서만 실패하는 것을 막는다.
Mismatch를 발견한 뒤 새 provenance를 current truth로 채택하거나 player anchor를 자동으로 다시
snapshot하지 않는다. Retry는 이 command invocation의 책임이 아니다.

### 6.3 Stability before commit

Initial player `BlockPos`와 world/dimension provenance는 operation-local immutable snapshot이다.
Scan과 topology normalization 뒤 repository transaction 직전에 먼저 current world provenance를
재확인하고, 같은 world일 때만 current player identity와 `BlockPos`를 다시 읽는다.

```text
same client/player identity + same BlockPos + same world/dimension/object provenance
    -> commit evaluation 계속

current world provenance read throws
    -> SCAN_COVERAGE_INCOMPLETE / world_provenance_read_failed_before_commit

world/dimension/object provenance is unavailable or changed
    -> SCAN_COVERAGE_INCOMPLETE / world_provenance_changed_before_commit

same world, but client thread or player state unavailable
    -> INVALID_ANCHOR / player_position_anchor_unavailable_before_commit

same world, but player reader/API throws
    -> INVALID_ANCHOR / player_position_anchor_read_failed_before_commit

same world, but current player object is not the initial player
    -> INVALID_ANCHOR / player_identity_changed_during_scan

same world and player identity, but player.getWorld() is not current world
    -> INVALID_ANCHOR / player_world_membership_changed_during_scan

same world, player identity and membership, but different BlockPos
    -> INVALID_ANCHOR / player_position_anchor_changed_during_scan
```

Player가 같은 block 안에서 소수 좌표만 이동한 것은 같은 anchor다. 다른 block으로 이동했다면
`INVALID_ANCHOR / player_position_anchor_changed_during_scan`으로 끝내고 save, publish와 revision
mutation을 수행하지 않는다. World/dimension/object provenance 변경은 기존
`SCAN_COVERAGE_INCOMPLETE` 계열 reason으로 fail closed한다.

Pre-commit revalidation 안에서 player와 world가 동시에 달라졌다면 world provenance failure가
우선한다. 이 우선순위는 이미 발생한 scan/topology failure를 덮어쓰라는 의미가 아니다.

`coverageComplete`는 base volume과 필요한 double-chest partner observation이 완료됐는지를 나타내는
scan evidence다. 따라서 complete scan 뒤 발생한 player/world pre-commit failure나 repository
failure에서는 `coverageComplete=true`일 수 있다. 이는 operation/commit 성공을 뜻하지 않는다.
반대로 §6.2 pre-scan mismatch처럼 block observation 전에 거절한 결과는 반드시 false다.

이 재확인은 범위를 움직이는 live-follow 기능이 아니다. 한 invocation은 최초 snapshot의 한 volume만
소유하며 자동 rescan, retry 또는 새 Task를 만들지 않는다.

### 6.4 Whole-operation failure precedence

Operation은 다음 순서로만 진행한다. 최초 확정 failure가 terminal이며 뒤 단계가 앞 failure를
덮어쓰거나 더 높은 우선순위로 바꾸지 않는다. §6.1의 first-provenance empty는 여러 live-state 원인을
합친 provisional outcome이므로, 그 경우의 player-reader 1회 분류가 끝난 뒤에 terminal failure가
확정된다. Exception 또는 이미 typed된 failure에는 이 예외를 적용하지 않는다.

```text
exact command grammar
-> initial world + player anchor acquisition snapshot
-> snapshot versus scanner-start world provenance
-> anchor build-height validation and half-open bounds
-> required chunk coverage, base/partner observation and scan-end provenance
-> double-chest topology normalization
-> pre-commit current-world provenance
-> pre-commit current-player availability, identity, membership and BlockPos
-> strict repository read, merge, conditional persist and publish
```

Complete scan 뒤 pre-commit 단계에서 실패하면 `registryReadStatus=NOT_READ`이고 repository counts와
revision fields는 unavailable sentinel을 유지한다. Scan/topology counts와 `coverageComplete` evidence는
이미 관찰된 값을 보존한다.

## 7. Exact fixed volume

Initial player anchor를 `(playerX, playerY, playerZ)`라고 할 때 requested half-open bounds는 다음과
같다.

```text
X: [playerX - 8, playerX + 8)   -> playerX-8 ... playerX+7
Y: [playerY - 8, playerY + 8)   -> playerY-8 ... playerY+7
Z: [playerZ - 8, playerZ + 8)   -> playerZ-8 ... playerZ+7
```

짝수 길이에는 하나의 block을 기준으로 완전히 대칭인 integer set이 없으므로 low side 8칸,
high side 7칸과 anchor block을 포함하는 기존 half-open 규칙을 보존한다.

먼저 player anchor 자체가 `bottomY <= anchorY < topYExclusive`인지 검증한다. 이 조건을 통과한
뒤에만 requested Y window와 유효 `[bottomY, topYExclusive)`의 교집합을 계산한다. World
build-height 밖의 Y는 버리고, 반대편으로 window를 밀어 16층을 보충하지 않는다.

```text
normal effective scan count: 16 * 16 * 16 = 4096
build-height edge:           16 * 16 * effectiveYLevelCount
```

`requestedRange`와 `effectiveRange`는 계속 별도로 기록한다. X/Z는 항상 요청한 16×16이고,
Y만 build-height에 따라 좁아질 수 있다.

Player anchor Y 자체가 world build height 밖이면 window에 일부 유효 Y가 남더라도
`INVALID_ANCHOR / anchor_outside_build_height`로 끝낸다. 방어적으로 requested Y와 build height의
교집합이 비어 있는 경우도 같은 결과다. 이때 scan observation, repository read, save, publish와
revision mutation은 모두 0이다.

이 검증 책임은 기존 bounds value owner인 `AutoDepositBulkScanBounds.around(...)`에 둔다. 이 factory는
clipping 전에 `bottomY <= anchorY < topYExclusive`를 검사하고, 위반 또는 empty effective Y range를
명시적인 `IllegalArgumentException`으로 거절한다. Scanner는 pre-scan provenance gate를 통과한 뒤 이
bounds factory를 호출하고, 해당 bounds-construction failure만
`INVALID_ANCHOR / anchor_outside_build_height`로 변환한다. Scanner 안에 같은 Y predicate를 별도로
복제하지 않는다.

## 8. Scan-center and container-candidate separation

Player anchor는 오직 volume 계산의 geometric center다. Anchor block 자체는 다음 어느 것이어도
유효하다.

```text
air
solid ground
water
unsupported block
supported storage container
```

따라서 pre-change scanner의 `anchor_not_exact_supported_container` precondition은 목표 계약과
양립하지 않았다. 현재 worktree 구현은 다음 책임을 분리했다.

```text
bulk anchor source
    -> player BlockPos 획득과 재확인

scan bounds
    -> player BlockPos에서 fixed half-open volume 계산

bulk scanner
    -> loaded coverage와 각 BlockPos observation

container classifier
    -> existing world-view classifier가 각 block의 exact allowlist kind를 관찰
```

Scanner는 center position도 다른 scan position과 동일하게 관찰할 수 있지만, 그 block이 지원
container가 아니라는 이유로 operation을 거절하지 않는다. Center가 container라면 일반 발견
candidate 한 개로 계산할 뿐 특별한 우선순위를 주지 않는다.

새 classifier abstraction은 만들지 않는다. Existing `MinecraftAutoDepositBulkWorldView.kindOf(...)`와
`AutoDepositBulkContainerKind`를 그대로 사용하고, scanner의 special anchor pre-observation과
`anchor_not_exact_supported_container` rejection만 제거한다. Center는 normal 4096-position loop에서
정확히 한 번 관찰한다.

## 9. Loaded-only and fail-closed coverage

기존 loaded-only 계약을 그대로 유지한다.

- H5는 chunk를 load, generate 또는 pathfind하지 않는다.
- Requested X/Z footprint가 걸치는 모든 chunk를 scan 전에 확인한다.
- Double-chest partner가 volume 밖에 있으면 그 partner chunk와 reciprocal observation도 요구한다.
- Scan 뒤 required chunks가 계속 loaded인지 재확인한다.
- Block observation 하나라도 불가능하거나 world provenance가 바뀌면 partial result를 commit하지 않는다.

Scanner body 또는 scan-end provenance read가 예외를 던지면 기존
`SCAN_COVERAGE_INCOMPLETE / world_read_failed`를 유지한다. Scan-end provenance가 없거나 initial
provenance와 다르면 기존 `world_provenance_changed_during_scan`을 유지한다. 둘 다 scanner success가
아니므로 `coverageComplete=false`다.

Player-centered footprint는 player가 chunk 경계 근처에 있을 때 최대 네 X/Z chunk에 걸칠 수 있다.
그중 하나라도 unloaded면 loaded subset만 등록하지 않고 `SCAN_COVERAGE_INCOMPLETE`로 전체 operation을
무변경 종료한다.

## 10. Exact supported destination set

`모든 상자`는 임의의 `Inventory` 또는 모든 container-like block을 뜻하지 않는다. 이번 변경은
기존 H5 exact vanilla allowlist를 유지한다.

```text
included:
    Blocks.CHEST
    Blocks.TRAPPED_CHEST
    Blocks.BARREL

excluded:
    all shulker boxes
    ender chest
    furnace / smoker / blast furnace
    hopper / dispenser / dropper
    minecart and other entity inventory
    modded container
    tag-based expansion
```

Fixed volume 안에서 검증된 logical destination은 임의의 64개 registration limit 없이 모두
repository transaction input에 포함한다. Downstream automatic operation의 기존 최대 64 candidate
snapshot은 별도 정책이며 변경하지 않는다.

### 10.1 Zero supported destination result

Player center block은 container일 필요가 없으므로 complete volume에 지원 destination이 0개인 경우는
정상적인 결과다. 이를 invalid anchor나 scan failure로 바꾸지 않는다.

Complete scan과 topology normalization 뒤 strict repository read와 published-snapshot consistency
check는 그대로 수행한다. Strict snapshot이 usable하고 현재 published state와 일치하며 logical
destination이 0개일 때만 결과는 다음과 같다.

```text
status=NO_CHANGE
success=true
reason=no_supported_destinations_found
registryReadStatus=<valid strict snapshot status>
coverageComplete=true
scannedPositionCount=<bounds.effectivePositionCount()>
physicalSupportedBlockCount=0
logicalDestinationCount=0
newlyRegisteredCount=0
reenabledCount=0
alreadyRegisteredCount=0
doubleChestCollapsedHalfCount=0
repositoryRevisionBefore=<current revision>
repositoryRevisionAfter=<same revision>
totalRegistryCountBefore=<current count>
totalRegistryCountAfter=<same count>
downstreamAutomaticReevaluationPossible=false
```

Repository save와 publish call은 0이다. Empty logical input을 `all_destinations_already_enabled`로
보고하지 않는다. 그 reason은 하나 이상의 logical destination이 발견됐고 모두 이미 enabled인
경우에만 사용한다. Strict registry read가 실패하면 destination 0을 근거로 false `NO_CHANGE`
success를 만들지 않고 기존 `REGISTRY_READ_FAILED` 계약을 적용한다. Strict snapshot과 current
published state가 다르면 zero input이어도 기존
`EXTERNAL_MODIFICATION_CONFLICT / published_registry_state_differs_from_strict_snapshot`이 우선한다.

## 11. Double-chest and repository guarantees

Anchor 변경은 double-chest normalization과 repository transaction 의미를 바꾸지 않는다.

- Vanilla double chest는 reciprocal partner, 같은 kind/facing과 complementary LEFT/RIGHT를 검증한다.
- 두 physical half는 logical destination 하나로 collapse한다.
- 한 half가 volume 밖에 있어도 loaded reciprocal partner가 검증되면 logical destination을 포함한다.
- 새 pair는 lexicographically smaller position을 대표로 사용한다.
- 기존 한 half entry의 exact destination ID와 position은 보존한다.
- 두 half가 이미 별도 entry면 조용히 merge/delete하지 않고 whole transaction을 거절한다.
- Registry read, parse, persistence 또는 external-conflict 실패는 mutation 0이다.
- Effective multi-entry mutation은 save 최대 1회, publish 최대 1회, revision 정확히 +1이다.
- 모든 대상이 이미 enabled면 `NO_CHANGE`이며 save/publish/revision increment는 0이다.

Player position은 persistent trusted destination schema에 저장하지 않는다. Persistent area,
scan receipt 또는 player-follow subscription도 만들지 않는다.

## 12. Result and logging contract

기존 bounded terminal summary 필드 이름은 유지하되 의미를 다음처럼 바꾼다.

```text
anchorSource=PLAYER_BLOCK_POSITION
anchorPos=<initial immutable player BlockPos>
requestedRange=<player BlockPos based half-open range>
effectiveRange=<world build-height intersection>
```

Configured/attempted batch source는 anchor acquisition 성공 여부와 관계없이 항상
`PLAYER_BLOCK_POSITION`으로 기록한다. 따라서 player/world unavailable 또는 reader exception처럼
position을 얻기 전의 failure도 다음 형태를 사용한다.

```text
anchorSource=PLAYER_BLOCK_POSITION
anchorPos=unavailable
```

Formatter가 `CROSSHAIR`를 상수로 출력해서는 안 된다. Result가 operation에서 실제 사용한
configured source와 available position을 소유하고 formatter는 그 immutable result만 직렬화한다.

최소 terminal fields는 계속 다음과 같다.

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

Count와 coverage field의 정확한 범위는 다음과 같다.

```text
scannedPositionCount:
    effective base volume에서 관찰을 완료한 BlockPos 수
    volume 밖 double-chest partner observation은 제외

physicalSupportedBlockCount:
    effective base volume 안의 allowlisted physical block 수
    volume 밖 partner는 제외

logicalDestinationCount:
    topology normalization 결과 수
    volume-edge double chest의 representative coordinate는 volume 밖일 수 있음

coverageComplete:
    required base volume과 partner observation 완료 뒤
    scanner-owned post-scan required-chunk + provenance revalidation까지 통과했는지 나타내는 scan evidence
    command success 또는 repository commit success와 동일하지 않음
```

따라서 full scan 뒤의 player/world pre-commit failure, registry read failure, persistence failure 또는
external conflict에서도 `coverageComplete=true`일 수 있다. Pre-scan provenance mismatch와
incomplete chunk/block coverage, `world_read_failed`와 `world_provenance_changed_during_scan`에서는
false다. Scanner success 뒤 service-owned revalidation에서 발생하는
`world_provenance_read_failed_before_commit` 또는 `world_provenance_changed_before_commit`은 true를
유지한다.

Crosshair가 null이거나 다른 방향을 가리키는 것은 failure reason이나 conflict가 아니다. Batch 결과에
`safe_crosshair_anchor_*`, `crosshair_anchor_*` 또는 `anchor_not_exact_supported_container`가 나오면
목표 구현이 완성되지 않은 것이다.

전체 destination 목록, 전체 scanned BlockPos 목록 또는 per-block success log는 남기지 않는다.

## 13. Responsibility and package direction

현재 H5 code는 LAVI-owned source다. 다음 구현도 upstream-derived `adris/**`를 이동·분해하지 않고
LAVI-owned package 안에서만 최소 책임 분리를 사용한다.

권장 dependency direction은 다음과 같다.

```text
English/Korean command facade
    -> AutoDepositTrustedBulkRegistrationOperation port

command form parser
    -> AutoDepositTrustCommandForm immutable value

AutoDepositTrustedBulkRegistrationCommandOperation
    -> implements the existing operation port
    -> calls command-emission-free registration service
    -> formats Result and emits log/logWarning

AutoDepositTrustedBulkRegistrationService
    -> execute(...), immutable Result-returning orchestration only
    -> consumes the form value
    -> bulk/anchor player-position reader
    -> existing bulk/scan world view, provenance and scanner
    -> existing bulk/topology normalizer
    -> existing trusted repository bulk transaction
```

### 13.1 Mandatory command emission/orchestration split

Pre-change `AutoDepositTrustedBulkRegistrationService` owned two independent responsibilities:
result-producing anchor/scan/topology/repository orchestration and command-facing formatter/severity/log
emission. The worktree implementation split those responsibilities instead of adding another branch to the
mixed unit.

```text
.../trusted/bulk/AutoDepositTrustedBulkRegistrationService.java
    -> no longer implements the command operation port
    -> public execute(AltoClef, AutoDepositTrustCommandForm)
       returns AutoDepositTrustedBulkRegistrationResult
    -> owns anchor, scan, topology and repository orchestration only

.../trusted/command/AutoDepositTrustedBulkRegistrationCommandOperation.java
    -> implements AutoDepositTrustedBulkRegistrationOperation
    -> invokes the command-emission-free registration service
    -> passes Result to the existing bounded formatter
    -> success => mod.log(...)
    -> failure => mod.logWarning(...)
```

기존 `trusted/command` package가 이미 command adapter 책임 경계이므로 한 파일만을 위한 추가
`command/bulk` 폴더는 만들지 않는다. Existing operation interface는 compatibility port로 같은 경로에
유지한다. Formatter도 pure result serialization 책임으로 `trusted/bulk`에 유지한다.

Default composition은 `AutoDepositTrustedCommandRegistrar`와 `AutoDepositTrustCommand` public
convenience constructor에서 raw service 대신 command operation adapter를 만든다. Registrar가 만든
동일 adapter instance를 English와 Korean facade에 계속 주입한다. Parser, facade dispatch, Korean
batch-only restriction과 English no-arg single branch behavior는 변경하지 않는다.

신규 또는 대체되는 anchor 역할은 의미 있는 bulk 전용 package에 한 primary type씩 둔다.

```text
.../trusted/bulk/anchor/AutoDepositBulkPlayerAnchor.java
    -> immutable BlockPos + initial player identity reference
       + captured client-world identity reference

.../trusted/bulk/anchor/AutoDepositBulkAnchorKind.java
    -> canonical result kind; PLAYER_BLOCK_POSITION

.../trusted/bulk/anchor/AutoDepositBulkAnchorReader.java
    -> bulk service가 의존하는 좁은 read port

.../trusted/bulk/anchor/AutoDepositBulkAnchorReadStatus.java
    -> AVAILABLE / UNAVAILABLE / PLAYER_WORLD_MEMBERSHIP_MISMATCH

.../trusted/bulk/anchor/AutoDepositBulkAnchorReadResult.java
    -> typed read outcome; AVAILABLE일 때만 player anchor 소유

.../trusted/bulk/anchor/AutoDepositBulkPlayerAnchorFactory.java
    -> captured thread/player/world inputs를 검증하고 immutable player anchor를 만드는 pure seam

.../trusted/bulk/anchor/MinecraftAutoDepositPlayerPositionAnchorReader.java
    -> MinecraftClient state를 한 번 capture하고 pure factory에 전달하는 adapter

.../trusted/bulk/AutoDepositBulkRegistrationSnapshot.java
    -> service-owned configured kind + player anchor
       + existing AutoDepositBulkWorldProvenance composition
```

Player/world identity는 reference-equivalence validation에만 쓰는 operation-local opaque 값이다.
Formatter, persistent JSON, equality key 또는 unbounded object string에 노출하지 않는다. Existing
`AutoDepositBulkWorldProvenance`는 현재 `bulk/scan` package에 그대로 두고 이번 anchor 변경을 이유로
이동하지 않는다. Service-owned registration snapshot만 player-anchor value와 provenance value를
compose하여 pre-scan과 pre-commit에 동일 world/player를 결정적으로 검증한다.

Bulk service는 snapshot 성공 여부와 독립적인 non-null configured
`AutoDepositBulkAnchorKind.PLAYER_BLOCK_POSITION`을 소유한다. 모든 result factory에 이 kind를
전달하므로 player-anchor snapshot이 만들어지기 전의 failure도 source field를 잃지 않는다.

Production adapter의 `MinecraftClient.getInstance()` access를 deterministic test가 직접 바꾸지
않는다. Thread/state/identity/membership 조합은 pure player-anchor factory 또는 injected reader port로
검증한다. Snapshot 비교는 record/default `equals()`나 identity hash string에 맡기지 않고, player는
reference `==`, provenance끼리는 `AutoDepositBulkWorldProvenance.sameWorld(...)`, captured player
world와 provenance는 `AutoDepositBulkWorldProvenance.matchesWorldIdentity(...)` 및
`AutoDepositBulkPlayerAnchor.belongsToWorld(...)`를 사용하는 explicit method가 소유한다.

현재 구현은 위 이름과 package를 사용하며 다음 책임 경계를 유지한다.

- Player reader가 scan, topology 또는 repository mutation을 소유하지 않는다.
- Scanner가 player, crosshair, screen 또는 command grammar를 직접 읽지 않는다.
- Formatter가 source state를 다시 읽거나 anchor source를 hard-code하지 않는다.
- Command operation adapter만 formatter 호출과 log severity emission을 소유한다.
- Command-emission-free bulk service가 result-producing orchestration과 all-or-none commit gate를 소유한다.
- Single-registration crosshair reader/safety는 single 경로에 남는다.

현재 `AutoDepositBulkScanner` 안의 special anchor pre-observation과 anchor-container precondition만
작은 LAVI-owned hunk로 제거한다. Existing world-view classifier와 normal scan loop를 재사용한다.
Scanner 전체 rewrite, provenance type 이동, 새 classifier layer 또는 unrelated cleanup은 필요하지 않다.

## 14. Implemented source scope

구현 전에 exact dirty-worktree diff와 ownership을 확인했다. 구현된 source 범위는 다음과 같다.

```text
new LAVI-owned bulk/anchor types:
    AutoDepositBulkPlayerAnchor
    AutoDepositBulkAnchorKind
    AutoDepositBulkAnchorReader
    AutoDepositBulkAnchorReadStatus
    AutoDepositBulkAnchorReadResult
    AutoDepositBulkPlayerAnchorFactory
    MinecraftAutoDepositPlayerPositionAnchorReader
AutoDepositBulkRegistrationSnapshot
AutoDepositTrustedBulkRegistrationCommandOperation
AutoDepositTrustedBulkRegistrationService
AutoDepositBulkScanner
AutoDepositBulkScanBounds
AutoDepositBulkWorldProvenance
    -> opaque captured-world reference matching method only
MinecraftAutoDepositBulkWorldView
    -> client-thread-first live-world gate; existing exact allowlist classifier preserved
AutoDepositTrustedBulkRegistrationResult
AutoDepositTrustedBulkRegistrationFormatter
AutoDepositTrustedDestinationRepository
    -> zero-target NO_CHANGE reason과 non-empty already-enabled reason만 구분
AutoDepositTrustedCommandRegistrar
    -> default composition에서 하나의 command adapter를 만들고 두 facade가 공유
AutoDepositTrustCommand
    -> public convenience constructor wiring만 adapter composition으로 변경
corresponding focused tests, including AutoDepositBulkScanBoundsTest
```

다음 source는 behavior-preserving regression 확인 대상이지만 player-centered batch 구현을 이유로
변경하지 않는다.

```text
AutoDepositTrustCommandForm parsers
AutoDepositTrustCommand parser와 single branch logic
AutoDepositTrustedTargetResolver
AutoDepositCrosshairAnchorReader / Safety
AutoDepositSingleCrosshairTargetReader
repository persistence schema
automatic-deposit selector and transfer tasks
adris/** upstream-derived source
```

`AutoDepositTrustCommand` 파일은 constructor composition hunk만 예상 수정 범위이며 parser와 single
branch logic은 regression-preserved 영역이다. `AutoDepositKoreanBulkTrustCommand`는 existing operation
port를 계속 받으므로 behavior/source 변경 없이 같은 adapter instance를 사용할 수 있어야 한다.

Upstream engine, global Task lifecycle 또는 common backend로 범위를 확장하지 않았다. 이후 그러한
확장이 필요해지면 이 구현 범위를 벗어나므로 별도 근거와 사용자 요청이 필요하다.

## 15. Deterministic acceptance tests

Worktree 구현은 실제 3D world 전체나 real-time movement에 의존하지 않는 focused fixtures로 다음을
검증한다.

1. Player가 air/비-container 위치에 있어도 주변 allowlisted containers를 정상 등록한다.
2. Crosshair가 null, MISS, entity, unsupported block 또는 먼 block이어도 같은 player anchor 결과가 나온다.
3. Current screen이 null, ChatScreen 또는 다른 screen이어도 bulk anchor 값은 screen에 의존하지 않는다.
4. `anchorSource=PLAYER_BLOCK_POSITION`과 initial feet `BlockPos`가 result에 기록된다.
5. Anchor reader를 호출하지 않았거나 typed anchor를 얻지 못한 failure도
   `anchorSource=PLAYER_BLOCK_POSITION`, `anchorPos=unavailable`을 기록한다. First provenance empty를
   분류한 결과 anchor가 AVAILABLE이면 provenance failure에도 captured `anchorPos`를 보존한다.
6. Pure factory/injected reader로 wrong thread, null world/player와 player-world membership mismatch를 검증한다.
7. Production-reader tests는 global `MinecraftClient` singleton을 mutate하지 않는다.
8. Initial acquisition provenance unavailable/read-exception/change와 scanner-start unavailable/read-exception/mismatch를 각각 exact reason으로 구분한다. First provenance empty는 player reader를 정확히 한 번 호출하고 scanner-start failure는 build-height/chunk/block read가 모두 0이다.
9. Feet `getBlockPos()`를 사용하고 ground block인 `getBlockPos().down()`을 사용하지 않는다.
10. 음수 fractional player coordinate가 Minecraft floor semantics의 BlockPos로 변환된다.
11. X/Y/Z low edge `anchor-8`은 포함하고 high edge `anchor+8`은 제외한다.
12. 일반 높이에서 exactly 4096 base positions를 관찰하고 build-height edge에서는 effective Y만 줄인다.
13. Anchor Y 유효성을 clipping보다 먼저 검사하며, build height 밖 anchor는 overlap이 남아도 실패한다.
    이 검증은 `AutoDepositBulkScanBounds.around(...)`가 소유하고 scanner는 exact failure로 투영한다.
14. Center block이 unsupported여도 거절하지 않으며, center가 supported면 normal loop에서 한 번만 센다.
15. Volume 밖 double-chest partner observation은 scanned/physical count에서 제외하되 coverage에 포함한다.
16. Logical destination representative가 volume 밖일 수 있는 edge-pair metric 의미를 검증한다.
17. Complete scan의 supported destination이 0개면 consistent strict snapshot 확인 뒤 `success=true`, effective scanned count와 exact zero counts를 가진 `NO_CHANGE`를 반환한다.
18. Empty logical input과 non-empty all-already-enabled reason을 구분하고 published-state conflict가 zero-input `NO_CHANGE`보다 우선한다.
19. Initial snapshot 뒤 같은 block 안의 소수 이동은 허용하고 다른 BlockPos 이동은 mutation 0이다.
20. Commit 전 player unavailable와 reader exception을 각각 exact pre-commit reason으로 구분한다.
21. Initial player object 교체, player-world membership 변경과 same-player BlockPos 이동을 서로 구분한다.
22. Pre-commit world-provenance read exception과 unavailable/change를 exact reason으로 구분하고, world와 player가 함께 바뀌면 world failure가 player failure보다 우선한다.
23. 앞선 scan/topology failure를 pre-commit 검사 결과가 덮어쓰지 않는다.
24. Scanner의 post-scan chunk/provenance revalidation failure는 `coverageComplete=false`, scanner success 뒤 service pre-commit/repository failure는 true인 scan-evidence 경계를 검증한다. Mid-scan 또는 post-scan read exception도 이미 완료된 base scanned/physical count를 보존한다.
25. Player/world unavailable, world/dimension/object change는 partial save/publish/revision 없이 fail closed한다.
26. Requested chunk 또는 double-chest partner chunk가 unloaded면 전체 mutation 0이다.
27. Chest, trapped chest, barrel만 포함하고 기존 excluded set을 등록하지 않는다.
28. Double-chest pair와 volume-edge partner를 logical destination 하나로 normalize한다.
29. Effective multi-entry change는 one transaction과 revision +1, repeated identical run은 `NO_CHANGE`다.
30. 네 batch grammar가 같은 player anchor path를 사용하고 malformed form은 operation 호출 전 거절된다.
31. 무인자 English single command의 exact-open/crosshair precedence와 distance/screen safety가 회귀하지 않는다.
32. Batch path가 Task, input, click, screen open, Baritone goal/path 또는 Carry On state에 접근하지 않는다.
33. Command-emission-free bulk service는 result를 반환하고 formatter 또는 `mod.log*`를 호출하지 않는다.
34. Command operation adapter만 success/failure에 맞춰 `mod.log`/`mod.logWarning`을 한 번 호출한다.
35. Registrar의 English/Korean facade가 같은 adapter instance를 공유하고 mutable parser state는 공유하지 않는다.
36. Formatter가 crosshair source/reason을 hard-code하지 않고 bounded output을 유지한다.

## 16. Clean build and runtime acceptance

이 worktree 구현은 repository의
[Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)에 따라 focused tests와
전체 1.20.1 tests 뒤 다음 clean forced build로 검증한다. 정확한 결과는 §1 status와 아래 verification
record에만 기록한다.

```powershell
.\gradlew.bat clean build --rerun-tasks
```

### 16.1 Worktree verification record

```text
VERIFICATION_DATE: 2026-09-04
JAVA_HOME: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
FINAL_COMMAND: .\gradlew.bat clean build --rerun-tasks
FINAL_RESULT: BUILD_SUCCESSFUL_IN_5M_4S
GRADLE_TASKS: 171_EXECUTED

1.20.1_TEST_SUITES: 203
1.20.1_TESTS: 707
1.20.1_FAILURES: 0
1.20.1_ERRORS: 0
1.20.1_SKIPPED: 1
ALL_CONFIGURED_VERSION_TEST_TASKS: PASSED

ARTIFACT: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
ARTIFACT_SIZE_BYTES: 8112941
ARTIFACT_LAST_WRITE: 2026-09-04 21:16:47 +09:00
ARTIFACT_SHA256: 24DD4C8DEBA968D0206CA3D68A227C57D513E55769F1BB51F5943F549989CC27
EXPECTED_PLAYER_ANCHOR_AND_COMMAND_ADAPTER_CLASSES_PRESENT: true

DEPLOYMENT: PASSED
ACTIVE_INSTANCE_JAR: C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\mods\chatclef-1.20.1-0.18.23.jar
ACTIVE_INSTANCE_JAR_SIZE_BYTES: 8112941
ACTIVE_INSTANCE_JAR_SHA256: 24DD4C8DEBA968D0206CA3D68A227C57D513E55769F1BB51F5943F549989CC27
BUILD_DEPLOYED_ARTIFACT_IDENTITY: MATCH
MINECRAFT_RUNTIME: PARTIAL_PASS; PLAYER_POSITION_BATCH_UPDATED_REGISTRATION_OBSERVED
COMMIT_PUSH: NOT_PERFORMED_AT_DOCUMENT_SNAPSHOT

RUNTIME_EVIDENCE_DATE: 2026-09-04
RUNTIME_EVIDENCE_SOURCE: LAVI_TEST_Fabric01/logs/stdout-logs.txt
RUNTIME_EVIDENCE_LINE: 3450
RUNTIME_EVENT_TIME: 21:32:57 KST
COMMAND_FORM: @auto_deposit_trust area 16x16
STATUS: UPDATED
REASON: bulk_registration_updated
REGISTRY_READ_STATUS: VALID_POPULATED
ANCHOR_SOURCE: PLAYER_BLOCK_POSITION
ANCHOR_POS: -743,69,65
REQUESTED_RANGE: [-751,-735)x[61,77)x[57,73)
EFFECTIVE_RANGE: [-751,-735)x[61,77)x[57,73)
COVERAGE_COMPLETE: true
SCANNED_POSITION_COUNT: 4096
PHYSICAL_SUPPORTED_BLOCK_COUNT: 44
LOGICAL_DESTINATION_COUNT: 22
NEWLY_REGISTERED_COUNT: 9
REENABLED_COUNT: 0
ALREADY_REGISTERED_COUNT: 13
DOUBLE_CHEST_COLLAPSED_HALF_COUNT: 22
REPOSITORY_REVISION: 1_TO_2
TOTAL_REGISTRY_COUNT: 13_TO_22
DOWNSTREAM_AUTOMATIC_REEVALUATION_POSSIBLE: true
DOWNSTREAM_AUTOMATIC_EXECUTION_PROVEN: false
NO_CHANGE_RERUN: NOT_RUN
PLAYER_MOVE_RANGE_SHIFT: NOT_RUN
EXPLICIT_LOW_HIGH_EDGE_RUNTIME_TEST: NOT_RUN
NEWLY_ADDED_DESTINATION_SELECTION: NOT_RUN
```

Configured versions `1.17.1`, `1.18`, `1.18.2`, `1.19.4`, `1.20.1`, `1.20.2`,
`1.20.4`, `1.20.5`, `1.20.6`, `1.21`, `1.21.1`은 각 203 suites / 707 tests를
실행했고 failure와 error는 모두 0이었다. Version별 skipped는 0 또는 1이며 build failure가 아니다.
Gradle deprecation, existing `Unsafe`/Mixin target warning과 preprocess native-filesystem fallback warning은
출력됐지만 최종 command는 성공했다.

현재 live run은 아래 item 4-6을 직접 확인했다. Item 7은 persisted registry의 total 22로 보조
확인됐지만 `trusted_list` exact-set 실행 증거는 아니다. Item 1-3과 8-10의 전체 절차는 완료되지
않았으므로 별도 runtime acceptance로 남는다.

Full runtime acceptance의 최소 절차는 다음이다.

1. Player의 feet `BlockPos`와 배치된 boundary containers를 기록한다.
2. Crosshair를 하늘이나 범위 밖 block으로 향하게 한다.
3. `@auto_deposit_trusted_list`로 before exact set을 기록한다.
4. 승인된 batch form 하나를 정확히 한 번 실행한다.
5. Terminal summary의 `anchorSource=PLAYER_BLOCK_POSITION`과 `anchorPos=player BlockPos`를 확인한다.
6. `requestedRange`, `effectiveRange`, scan count와 before/after registry delta를 검증한다.
7. `@auto_deposit_trusted_list`로 after exact set을 확인한다.
8. Crosshair 방향만 바꾸고 같은 위치에서 재실행하여 `NO_CHANGE`와 동일 anchor를 확인한다.
9. Player를 다른 block으로 옮긴 뒤 새 범위가 새 player `BlockPos`를 중심으로 계산되는지 확인한다.
10. 새 destination을 downstream `store_home`이 사용할 수 있는지는 별도 lifecycle evidence로 기록한다.

Registration 성공은 downstream automatic execution이나 transfer 성공을 뜻하지 않는다. 두 결과는
서로 다른 owner와 correlation으로 판정한다.

## 17. Failure and rollback boundary

목표 구현은 다음 조건에서 release하면 안 된다.

- Player가 container 위에 서 있지 않다는 이유로 `INVALID_ANCHOR`가 된다.
- Crosshair가 없거나 바뀌었다는 이유로 batch가 실패한다.
- Formatter가 계속 `anchorSource=CROSSHAIR`를 출력한다.
- Scanner가 player center block을 exact supported container로 요구한다.
- 일부 loaded subset만 저장한다.
- Player anchor 변경이 무인자 single-registration 동작까지 바꾼다.
- Registration을 이유로 Task, click, input 또는 path를 직접 시작한다.

Rollback unit은 bulk player-anchor adapter, bulk service wiring, scanner의 center/candidate 분리,
result/formatter source projection과 해당 focused tests다. 기존 single-registration crosshair path,
repository schema와 upstream engine은 rollback 대상에 포함되지 않아야 한다.

## 18. Completion definition

현재 worktree source 구현 완료는 다음을 의미한다.

```text
player-centered intent documented
exact BlockPos and half-open volume semantics fixed
pre-change-vs-worktree behavior separated
scanner container-anchor precondition removed
ownership/package split implemented
fail-closed and bounded logging contract recorded
deterministic acceptance covered by automated tests
```

Test, clean build, artifact inspection, deployment와 runtime verification은 서로 다른 완료 상태다.
각 결과가 실제로 존재할 때만 §1 status와 verification record를 갱신한다. Worktree source 구현이나
build 성공을 배포 또는 Minecraft runtime 검증으로 간주하지 않는다.
