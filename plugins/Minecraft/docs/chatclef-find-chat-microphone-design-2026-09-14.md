<!-- 20260914_kpopmodder: Documented FIND Chat/final-microphone design proposals without authorizing or implementing runtime behavior. -->
<!-- 20260914_kpopmodder: Reconciled the FIND review with historical player scope, exact result fields, partial observations, scoped decisions, and companion-log delivery. -->
<!-- 20260914_kpopmodder: Recorded the forwarded follow-up direction: report by default, explicit approach to observed targets, dropped-item report, and no initial exploration. -->
<!-- 20260914_kpopmodder: Identified partial-found reporting as a pending contract reconciliation rather than a waiver of nearest and bounds-result obligations. -->

# Fabric ChatClef FIND — 채팅·마이크 설계 검토

Date: 2026-09-14

<!-- 20260914_kpopmodder: Preserve the documentation-only evidence and link the later implementation request. -->
## Current implementation follow-up

The sections below preserve the earlier documentation-only review and its exact
evidence snapshot. A later explicit user request authorizes repository-local FIND
implementation and verification, including initial player-name search. The current
behavior, fixed bounds, conservative approach, catalog and terminal schemas are
recorded in the [implementation contract and verification record](chatclef-find-implementation-contract-2026-09-14.md).
Its evidence is separate from the historical `NOT_IMPLEMENTED` ledger below.
Deployment and live Minecraft execution remain `NOT_RUN`.

## 1. 상태, 근거, 권한

사용자는 모든 몹·블록·아이템을 한국어 채팅과 마이크로 찾는 `@find`를 요구했다. 이번 작업의 승인은 **문서화만**이다. 사용자가 전달한 ChatGPT 후속 답변과 문서화 요청을 근거로 현행 설계 방향을 Section 2에 기록한다. 전달문은 설계 근거이며 그 안의 권고를 코드 실행 지시로 취급하지 않는다. 구체적인 수치·결과 schema·파일 배치는 여전히 구현 전 제안이고, 방향 문서화가 소스·빌드·실게임 실행 승인을 의미하지 않는다.

```text
DOCUMENT_STATUS: DIRECTION_DOCUMENTED / NOT_IMPLEMENTED
DIRECTION_BASIS: USER_FORWARDED_CHATGPT_FOLLOWUP_AND_DOCUMENTATION_REQUEST_2026_09_14
CURRENT_DOCUMENTED_DEFAULT: REPORT_FOR_DIRECT_CHAT_AND_FINAL_MICROPHONE
CURRENT_DOCUMENTED_APPROACH: EXPLICIT_REQUEST_TO_OBSERVED_TARGET_ONLY
CURRENT_DOCUMENTED_ITEM_SCOPE: DROPPED_ITEM_ENTITY_REPORT_ONLY
CURRENT_DOCUMENTED_EXPLORATION: EXCLUDED_FROM_INITIAL_SCOPE
BACKEND_SCOPE: FABRIC_CHATCLEF_1_20_1_ONLY
REVIEWED_REPOSITORY_ROOT: C:\Vtuber_Souorce_Code\LAVI
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: 2ecc1283a1e5cf48357dd0e263c2526e7bcb64e2
REVIEWED_WORKTREE_AT_INITIAL_SOURCE_REVIEW: CLEAN
CURRENT_FIND_COMMAND: ABSENT_IN_REVIEWED_SOURCE
ACTIVE_REQUEST_AUTHORIZES_DOCUMENTATION_EDIT: YES
ACTIVE_REQUEST_AUTHORIZES_SOURCE_OR_TEST_EDIT: NO
ACTIVE_REQUEST_AUTHORIZES_CONFIGURATION_OR_PROTOCOL_IMPLEMENTATION: NO
ACTIVE_REQUEST_AUTHORIZES_BUILD: NO
ACTIVE_REQUEST_AUTHORIZES_DEPLOYMENT_OR_EXTERNAL_INSTANCE_COPY: NO
ACTIVE_REQUEST_AUTHORIZES_GAME_OR_LIVE_WORLD_EXECUTION: NO
ACTIVE_REQUEST_AUTHORIZES_COMMIT_OR_PUSH: NO
FIND_AUTOMATED_TESTS: NOT_RUN / DOCUMENTATION_ONLY
FIND_BUILD: NOT_RUN / DOCUMENTATION_ONLY
FIND_DEPLOYMENT: NOT_RUN / DOCUMENTATION_ONLY
FIND_RUNTIME: NOT_RUN / NOT_IMPLEMENTED
```

ChatGPT 전달문 `C:\Users\jaewo\Downloads\LAVI_FIND_Codex_handoff_2026-09-14.md`를 읽기 전용으로 검토했다. 읽은 원본의 크기는 44,678 bytes, SHA-256은 `9DADDE65C199657CA19E996053B9AF637D54EB51398A318128D97D1A222D52FB`이다. 전달문이 인용한 ZIP의 해시와 archive comment는 전달자의 근거이며, 이번 작업에서 해당 ZIP을 독립 검증하지 않았다. 그 스냅샷이나 과거 테스트를 현재 FIND 실행 증거로 취급하지 않는다.

위 해시와 크기는 다운로드한 최초 전달문에만 해당한다. 새 방향의 근거는 사용자가 대화에 전달한 후속 답변이다. 최초 소스 검수의 clean 상태도 그 당시 스냅샷이며, 이번 갱신 시작 시에는 앞서 작성한 문서 3개의 변경이 있었다. 원본 전달문과 검수 HEAD는 그대로 보존한다.

우선순위와 관계:

- [AGENTS.md](../../../AGENTS.md)의 안전·권한·Minimal Complete Change Rule·동반 로그 계약을 따른다.
- [9월 9일 GOTO/FIND/전체 명령 입력 계약](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md)의 과거 상태·증거와 Section 17 결정 경계는 보존한다. 이 문서는 FIND 제안을 추가로 정리하며 기존 계약을 대체하거나 접근 모드를 승인하지 않는다.
- [Minecraft backend separation](minecraft-backend-separation.md), [ChatClef integration direction](chatclef-carryon-integration-direction.md), [Fabric bridge protocol v1](fabric-chatclef-bridge-protocol-v1.md)을 보존한다. FIND 프로토콜 필드는 이 문서의 초안만으로 활성화하지 않는다.
- GOTO 재설계, 기존 자동 보관 수정, 전체 명령 한국어화, Forge/MineMind, 의존성·버전 변경은 이번 범위 밖이다.

## 2. 현행 방향과 남은 제품·구현 결정

사용자 목표는 `@find`, 한국어 채팅/마이크 입력, 모든 몹·블록·아이템 종류의 대상 해석, 기존 ChatClef와 `@attack` 구조의 최대한 재사용이다. 후속 답변을 반영한 현행 문서 방향은 다음과 같다. 구현 완료나 실행 검증을 뜻하지 않는다.

- 직접 `@find`의 모드 생략과 한국어 “찾아줘”는 모두 위치 보고인 `report`를 기본으로 한다. 같은 표현은 Chat과 최종 마이크 입력에서 같은 동작으로 해석한다.
- “찾아서 가까이 가줘”처럼 이동 의도가 명시된 요청에만 선택적 `approach`를 연결한다. 초기에는 관측 가능한 범위에서 실제로 확인한 대상에게 접근한다. 미발견이면 확인 범위와 한계를 보고하고, 새로운 지역 탐험을 자동으로 시작하지 않는다.
- 초기 아이템 검색은 월드에 떨어진 실물 `ItemEntity`의 위치 보고다. 인벤토리·상자 내용물 조회와 획득 방법/장소 검색은 별도 확장 기능으로 둔다. item 접근·수집은 초기 범위에 포함하지 않는다.
- 실행 중인 레지스트리의 실제 대상에 한국어 이름과 검증된 별칭을 연결한다. 이름 해석과 실물 발견은 별도 증거이며 종류가 겹치면 짧게 명확화한다.
- 기존 엔진과 자동방어·회피·생존 우선순위를 유지한다. FIND는 대상 해석·관측·후보 선택·요청 충족 판단을 맡고, 기존 작업 실행 기반을 재사용한다. 방어 선점은 재개 시 대상 상태를 재검증하되 총 기한과 누적 한도를 초기화하지 않는다.
- 발견, 접근 완료, 로드 범위 미발견, 발견 후 접근 불가, 중단을 구분하고 한국어 UI와 음성에서 같은 의미로 전달한다.

최초 전달문의 “마을 주민 찾아줘 → 기본 접근”과 “미발견 시 탐험 지점 이동”은 이번 초기 방향에서 채택하지 않는다. 최초 문서·소스·실행 증거를 수정하는 것은 아니며, 아래 표와 예시는 현행 방향을 따른다.

| 결정 | 현행 방향 또는 남은 제안 | 현재 상태 |
|---|---|---|
| D1: 직접 명령 기본값 | 모드 생략 시 `report` | 현행 문서 방향. 명령은 아직 미구현 |
| D2: 자연어 기본값 | “찾아줘”와 “위치만 알려줘”는 `report`; 명시적인 “찾아서 가까이 가줘”만 선택적 `approach` | 현행 문서 방향. 정확한 허용 문법·접근 지원 조건은 구현 전 검증 |
| D3: 아이템의 의미 | 실제 월드의 떨어진 `ItemEntity`만, 초기에는 `report`만 | 현행 초기 범위. 소지품·보관소·획득 기능은 별도 확장 |
| D4: 대상 종류 경계 | 모든 몹·블록·등록 아이템 ID; 플레이어 이름·구조물·발사체·탈것 등 별도 비몹 기능 제외 | 플레이어 제외는 기존 Section 17.1의 초기 `player` 포함 범위를 축소하는 제안. 조정 미확정이며 기존 범위를 취소하지 않음 |
| D5: 성능·탐색 한도 | Section 6의 반경·시간·방문 수 | 실측 전 후보 수치 |
| D6: 안전·접수·결과 정책 | report의 safety tier·확인 방식·입력 소스, approach의 몹·블록별 안전 기준과 모드별 admission/lifecycle | 각 모드의 구현 범위에 필요한 결정·검증을 별도로 닫음 |
| D7: 카탈로그·wire 계약 | Fabric 전용 versioned 교환, 닫힌 양쪽 validator | 전송 schema·상한·무효화 조건 확정 필요 |

아이템 ID 전체를 해석한다는 것과 모든 위치에서 실물을 찾는다는 것은 다르다. 현행 D3에서는 인벤토리·상자 내용물·레시피·광석·몹의 드롭 테이블 등 획득 경로는 검색하지 않는다. 이미 월드에 떨어진 실물 `ItemEntity`는 검색 대상이다. “다이아몬드 찾아줘”는 아이템으로 해석된 경우 그 실물을 검색하며 다이아몬드 광석이나 GET으로 바꾸지 않는다. 모드에 한국어 번역이 없으면 등록 ID/사용 가능한 원어 이름과 검증된 별칭을 제공하며, 모든 모드 이름이 한국어로 준비됐다고 주장하지 않는다.

## 3. 명령 문법과 입력 흐름 — 초안

아래 문법은 전달문의 entity/block/item 제안이다. 기존 계약의 player 문법·identity·개인정보 경계는 D4에서 조정하기 전까지 보존하며, 아래 목록을 근거로 player를 삭제하거나 지원 완료로 표시하지 않는다.

```text
@find entity <namespace:id> [report|approach]
@find block  <namespace:id> [report|approach]
@find item   <namespace:id> [report]
```

| 한국어 입력 | 제안된 canonical 명령 또는 응답 |
|---|---|
| 마을 주민 찾아줘 | `find entity minecraft:villager report` |
| 마을 주민 위치만 알려줘 | `find entity minecraft:villager report` |
| 마을 주민 찾아서 가까이 가줘 | `find entity minecraft:villager approach` — 지원된 명시적 접근에만 연결 |
| 상자 블록 찾아줘 | `find block minecraft:chest report` |
| 상자 블록 위치만 알려줘 | `find block minecraft:chest report` |
| 상자 블록 찾아서 가까이 가줘 | `find block minecraft:chest approach` — 지원된 명시적 접근에만 연결 |
| 다이아몬드 찾아줘 / 떨어진 다이아몬드 찾아줘 | `find item minecraft:diamond report` — 종류·이름이 유일하게 해석된 경우 |
| 떨어진 다이아몬드 찾아서 가까이 가줘 | 초기 item 접근 미지원 안내, 제출 0회 |
| 상자 찾아줘 | block/item 중의성 명확화, 제출 0회 |
| 인벤토리에서 다이아몬드 찾아줘 / 어느 상자에 다이아몬드 있어? / 다이아몬드는 어디서 얻어? | 소지품·보관소·획득 조회의 별도 범위라고 안내, FIND 제출 0회 |

직접 명령과 한국어 입력의 기본값은 모두 `report`다. 미지원 `approach` 요청을 `report`로 몰래 바꾸거나 보고 전용 구현을 전체 접근 기능 완료로 표시하지 않는다. report가 기존 작업과의 병렬 실행을 새로 보장하는 것은 아니며 기존 STOP/busy/trust/접수 정책을 유지한다.

```text
기존 신뢰된 Chat / 최종 ASR 수렴점
  -> FIND 후보 판정
  -> 종류·모드·한국어 대상 해석 또는 명확화
  -> typed validation + 기존 STOP/busy/trust/중복 방지
  -> @ 없는 canonical find 1회 제출
  -> 기존 Fabric bridge와 명령 소유 Task
  -> 관측 / 명시적으로 지원된 접근
  -> 후보 재검증 + 결과 1회 확정 + 기존 수명 증거
  -> Python 결과 검증·중복 방지
  -> 한국어 UI/TTS
```

partial ASR, 비신뢰 입력, TTS echo는 실행하지 않는다. “찾지 마”, “찾는 법 알려줘”, 외부 파일 검색, 복합 명령·개행·연결 문자를 구분한다. FIND로 판정했으나 해석에 실패한 요청을 GET/일반 대화/LLM 자동 보정 경로로 넘겨 실행하지 않는다. 명확화 후보는 최대 3개와 생략 수를 표시하는 안이며, 새 명확한 입력 전까지 제출하지 않는다.

## 4. 현재 소스와 최소 변경 경계

경로 약어:

```text
J = plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java
P = plugins/Minecraft/fabric/chatclef
B = J/lavi/minecraft/fabric/chatclef/bridge
```

| 현재 소스 | 재사용과 제한 |
|---|---|
| `J/adris/altoclef/AltoClefCommands.java`, `J/adris/altoclef/commands/AttackPlayerOrMobCommand.java` | 명령 인자 → `runUserTask` → 완료 callback 패턴 참고. 처치·드롭 수집·`ResourceTask`는 FIND에 가져오지 않음 |
| `J/adris/altoclef/commands/random/ScanCommand.java` | 좌표 보고 참고. `Blocks.class` reflection은 실제 레지스트리 전체/모드/한글 해석의 권위자가 아님 |
| `J/adris/altoclef/commands/BlockScanner.java` | 기존 `snapshotLocations(Block...)`의 world/player/dimension/revision과 불변 위치 목록을 재사용할 수 있음. 블록 종류당 캐시 상한 40개이며 관측 tick·검색 완료/영역 정보는 없음. 빈 스냅샷을 범위 전체 미발견으로 취급하지 않음 |
| `J/adris/altoclef/trackers/EntityTracker.java` | 기존 관측 경계 참고. 근접/적대/접근 불가 필터와 일부 착지 조건의 드롭 색인은 모든 대상 위치 보고를 보장하지 않음 |
| `J/adris/altoclef/tasksystem/Task.java` | 실제 Task 계약은 `onStart()`, `onTick()`, `onStop(Task)`, `isFinished()`, `isEqual(Task)`, `toDebugString()`. 일시 선점도 `onStop`을 호출하고 재개 시 시작 처리가 다시 실행될 수 있음 |
| `J/adris/altoclef/tasks/movement/GetToEntityTask.java` | `isFinished()` 오버라이드 없이 follow goal/Wander/unstuck을 사용. 시작·종료에 전역 `forceCancel()`, tick 중 공용 입력 해제·process `onLostControl()` 호출이 있음. 기존 Section 6.6에 따라 FIND 이동 자식으로 직접 재사용하지 않음. 유한 부모로 감싼 것만으로 입력/goal/path 정리 소유권을 증명하지 못함 |
| `P/input/minecraft_chatclef_input_router.py`, `P/input/gating/minecraft_chatclef_input_intent_gate.py`, `P/intent/korean_chatclef_rule_parser.py` | 현재 일반 FIND 분기 없음. 파서뿐 아니라 최초 Minecraft 라우팅도 연결해야 함 |
| `P/command_registry/korean_command_registry.py` | FIND 등록 없음. 명령 등록·노출·입력 소스·모드 정책·exact-set 검증을 함께 갱신해야 함 |
| `B/command/execution/FabricChatClefCommandResultFactory.java` | 명령 소유 유한 FIND root의 발견 성공에는 callback과 matching Task 종료 증거가 필요함. 기존 무Task 명령의 callback-only 완료, GOTO/STORE_HOME/general effect와 cautious unknown 경계를 보존하며 FIND 증거 projection 연결 |

최소 변경 후보는 다음과 같다. **실제 변경 manifest가 아니며 아직 파일을 생성하지 않았다.**

| 책임 | 후보 위치 / 기존 연결 |
|---|---|
| FIND 등록·인자 검증 | `J/lavi/minecraft/find/`의 command/registrar; 기존 엔진 준비·충돌 검사 패턴 재사용. entrypoint가 필요하면 `fabric.mod.json`에 최소 연결 |
| 실제 ID·번역 카탈로그 | 같은 FIND 소유 영역의 resolver/catalog; Fabric 전용 bridge catalog 교환과 resource/session 소유자에 필요한 연결만 |
| 관측·Task 수명·결과 | 보고 Task와 필요한 관측 협력자. 접근은 승인된 경우에만 별도 소유자/이동 경계를 추가 |
| 결과 검증·직렬화 | `B/command/result/effect/find/` 후보; 기존 execution state/result factory에 명령 소유 root 연결과 projection만 |
| 한국어 FIND 해석 | `P/intent/find/` 후보; 기존 gate/parser/compiler/translator/admission에 필요한 위임만 |
| 카탈로그 수신·무효화 | `P/transport/catalog/` 후보; 기존 active dispatcher/session 조립·disconnect cleanup 재사용 |
| UI/TTS와 명령 metadata | 기존 descriptor/evidence/phrase/terminal renderer 및 command registry에 FIND의 닫힌 profile만 추가 |

Minimal Complete Change Rule에 따라 기존 owner, 함수, typed contract와 테스트 경계를 먼저 재사용한다. 전달문의 모든 DTO·서비스·adapter·패키지를 기계적으로 만들지 않는다. 새 독립 상태·정책·수명·백엔드 경계가 필요한 경우만 최소 소유자를 추가한다. `AttackPlayerOrMobCommand`, `ScanCommand`, `Task`, `TaskRunner`, 방어 체인, 전역 Baritone 정책의 변경은 제안된 기본 구현 범위에 포함하지 않는다. FIND 때문에 GOTO를 복구·교체하거나 generic dispatcher를 새로 만들지 않는다.

## 5. 대상 해석과 카탈로그 계약 — 초안

Java의 현재 `Registries.ENTITY_TYPE`, `Registries.BLOCK`, `Registries.ITEM`이 ID 권위자다. ID 형식과 실제 등록 여부를 각각 검사하고 default registry 값으로 없는 대상을 대체하지 않는다. `(kind, namespace:path)`를 identity로 사용한다. 몹은 실제 관측된 `MobEntity`와 요청 `EntityType`의 일치를 확인한다. 타입 판별을 위해 엔티티를 생성·스폰하지 않는다. 관측 전 자격이 불명확한 모드 타입은 알려진 비몹과 구분한다.

한국어 이름은 등록 ID/translation key와 현재 리소스의 명시적 `ko_kr` 번역을 결합한다. UI 언어가 영어여도 한국어 입력을 유지한다. 리소스 팩의 우선순위와 키 병합 규칙을 적용하고 읽은 리소스를 닫는다. 번역명·별칭의 정규화 후 충돌을 유지하며, fuzzy/LLM 결과는 자동 실행의 권위자가 아니다.

| 계약 | 허용 내용과 검증 |
|---|---|
| FIND 요청 | `kind`: entity/block/item, 현재 등록된 canonical ID, `mode`: report/approach. 기본은 report, approach는 명시적으로 요청한 지원 종류에만 허용. 초기 item+approach는 거부. 확정 DTO 필드만 허용 |
| 카탈로그 레코드 | kind, canonical ID, translation key, 선택적 ko/en label, label/alias source, 명시적 eligibility 상태 |
| 카탈로그 스냅샷 | version/digest/resource generation, 세션·connection generation binding, 정렬·페이지 순서·총수·전체 크기 검증 |
| 카탈로그 교환 | 기존 Fabric 세션 검증을 재사용하는 versioned payload 후보. 작은 capability만 handshake에 추가. 전체 목록을 handshake/heartbeat에 넣지 않음 |

페이지당 최대 256개 및 64 KiB 중 먼저 도달하는 한도는 제안이다. 전체 크기·레코드 수·라벨 길이·교환 기한도 구현 전 확정해야 한다. 완전히 검증된 스냅샷만 원자적으로 교체하고 부분 페이지를 이름 해석에 사용하지 않는다. 재연결·리소스 재로드·월드 교체의 무효화 조건을 확정한다. 이전 요청 자동 재제출은 없다. registry/label/resource 구현은 Minecraft common 계층에 넣지 않는다.

모든 전송 페이지가 도착했어도 원본 열거·번역 읽기 한도로 잘린 목록은 전체 카탈로그가 아니다. 원본 수집의 완전성도 검증하고, 불완전한 자료를 완전 스냅샷으로 게시하거나 전체 한국어 이름 커버리지로 주장하지 않는다. 직접 Java canonical-ID 명령은 Python 이름 카탈로그 준비 여부와 별도로 현재 Java registry에서 검증할 수 있으며 기존 명령 접수·수명 계약은 그대로 적용한다. 요청 표의 player 생략 역시 D4의 제안이며 기존 player identity 계약을 대체하지 않는다.

등록 ID 커버리지, 한국어 이름 커버리지, 실제 인스턴스 관측 커버리지는 별도로 보고한다. 공기 블록과 빈 아이템 스택처럼 도메인 경계가 다른 값도 명시적으로 처리한다. 정확한 wire key/enum/버전과 처리기는 향후 [bridge protocol](fabric-chatclef-bridge-protocol-v1.md)에 기록하고 양쪽 validator에서 일치시켜야 하며 현재 지원된다고 주장하지 않는다.

## 6. 관측과 접근의 한도 — 실측 전 제안

| 한도 | report 제안 | approach 제안 |
|---|---|---|
| 중심·차원 | operation 시작 시 고정 | 최초 관측 중심·차원 유지, 이동으로 검색 범위 확장 금지 |
| 반경 | entity/item 64, block 32; 3차원 유클리드 거리 | 최초 제한 관측에서 확인한 후보만. 허용 접근 거리·경로 한도는 별도 확정 |
| 전체 기한 | monotonic 경과 5초 | 120초, 선점/재개에도 초기화 금지 |
| 엔티티 처리 | 실제 방문 최대 4,096개 | 관측 단계별·누적 한도 추가 확정 필요 |
| 블록 처리 | 총 300,000개, tick당 4,096개와 약 2ms 소프트 예산 | 관측 단계별·누적 한도 추가 확정 필요 |
| 후보 유지 | 가까운 후보 최대 64개 | 실제 후보 및 재선택 정책 확정 필요 |
| 이동 탐색 | FIND 소유 이동 없음 | 미발견 대상 탐험 지점 0개. 발견 대상 접근·후보 소실/재선택 한도는 별도 확정 |
| 사용자 결과 | 기본 1개 | 발견과 안전한 접근 완료를 구분 |

2ms는 월드 API 호출을 강제 중단하는 보장이 아니라 반복 사이에서 확인할 작업 예산이다. 성능 측정으로 수치를 확정한다. 방문 수, 일치 수, 후보 보관 수, 시간·반경·로그 한도는 각각 다른 개념이다. 전체 목록을 먼저 복사하고 마지막에 자르는 구현으로 처리 비용이 제한됐다고 주장하지 않는다.

최초 전달문의 접근 탐험 반경 128, 탐험 지점 8개, 재선택 3회는 역사적 제안이다. 초기 기능에 새로운 지역을 돌아다니며 미발견 대상을 찾는 행동을 포함하는 근거로 사용하지 않는다. 위 접근 기한 120초도 실측 전 제안이며 기한·안전 거리·후보 소실 정책은 별도로 닫아야 한다.

클라이언트 스레드에서 현재 로드된 엔티티/블록만 읽고 필요한 불변 scalar/identity를 보관한다. 살아 있는 iterator나 mutable 엔티티 객체를 tick 사이의 관측 자료로 넘기지 않는다. 블록은 고정 위치 커서로 분할 관측하며 로드 여부 확인 없는 `getChunk`와 강제 청크 로딩은 없다. 기존 스캐너 위치는 현재 블록 상태로 재검증한다.

착지 여부나 경로의 접근 불가는 발견 자체의 필터가 아니다. 벽 너머/지하의 로드된 블록도 관측될 수 있으므로 기본 발견을 시야 확인으로 표현하지 않는다. 후보 유지 상한에 도달해도 처음 만난 후보를 전체 최근접으로 주장하지 않는다. 시작 중심 거리와 UUID/블록 좌표의 안정된 tie-break를 사용하고 음수 좌표는 `floor` 처리한다. 거리 순위는 관측한 후보와 그 관측 시점의 자료에 한정하며, 여러 tick의 관측을 월드 전체의 동시 스냅샷이나 완전한 최근접으로 표현하지 않는다.

기존 `ClientChunkSnapshotCapture`의 클라이언트 스레드·로드 청크 확인 경계를 우선 검토한다. 현재 캡처는 한 호출에서 청크의 모든 section을 복사하므로 그 사용만으로 FIND의 tick당 방문·2ms·메모리 한도를 증명하지 못한다. 분할 커서와 처리 비용은 별도로 검증한다. 블록 위치 스냅샷은 lock 아래에서 일관되게 복사된 후보 자료지만 영역 검사 완료 증거는 아니다. 엔티티의 exact-class bucket이나 interaction-range 목록도 전체 로드된 엔티티 관측을 대신하지 않는다.

초기 approach는 최초 제한 관측에서 실제로 확인한 대상에게만 유한하게 접근한다. 그 범위에서 발견하지 못하면 확인 범위와 한계를 보고하고 탐험 이동을 시작하지 않는다. 이동으로 청크가 자연스럽게 로드되더라도 이를 새 지역 탐색이나 검색 반경 확장의 권한으로 사용하지 않는다. 같은 후보가 실제로 유효하며 모드별 허용 거리·가시성·안전 조건을 충족한 뒤 성공한다. FIND가 공격·GET·상호작용·채굴·설치 fallback을 요청하지 않도록 실제 이동 호출 체인을 검토한다. 전역 설정/키/goal을 덮어써 보장하지 않는다. 국소 소유권과 허용 동작을 증명할 수 없는 경로는 접근 실패이며, 모든 몹에 같은 거리가 안전하다고 표현하지 않는다. 세부 이동 API와 제어 자원은 아직 `UNKNOWN`이다.

## 7. Task ownership과 종료 경계

| 경계 | FIND 소유자가 해야 할 일 | 보존할 경계 |
|---|---|---|
| 명령 접수 | 기존 admission 이후 원래 kind/ID/mode와 request/root를 연결 | STOP/busy/trust/중복 방지 |
| 최초 `onStart()` | operation, world/dimension, 중심·시작 시간·총 예산을 한 번 고정 | 명령 소유 root |
| `onTick()` | 같은 operation의 제한 관측/허용된 이동, 후보·한도 재검증 | 클라이언트 스레드·기존 engine Task 계약 |
| 방어 `interrupt()` / `onStop(Task)` | 일시 양보·자기 제어만 정리, 종료 성공/실패로 단정하지 않음 | 자동방어·회피·생존 우선순위 |
| 재개 `onStart()` | 원래 operation·기한·누적 예산 유지, 현재 binding과 대상 재검증 | 재제출·새 명령 초기화 금지 |
| 사용자 STOP/root 교체/세계 교체 | 기존 authoritative lifecycle 근거로 구분 | 늦은 완료가 STOP/다른 root를 덮어쓰지 않음 |
| 결과 확정 / `isFinished()` | 단일 결과 owner가 immutable outcome을 한 번 확정 | callback 완료와 FOUND 구분 |
| cleanup 및 projection | 자식 정지·소유 정리와 matching task 종료 증거 확인 | 정상 cleanup이 확정 outcome을 덮어쓰지 않음 |

report에는 FIND 소유 이동 자원이 없다. 자동방어가 report 중 이동·공격할 수는 있으며, 이는 FIND가 해당 행동을 요청했다는 증거가 아니다. 방어가 접근 대상 좀비를 죽이면 FIND 처치 성공이 아니라 대상 소실이다. 기존 UserTaskChain의 공통 정리를 제거하지 않고 FIND가 전역 `AltoClef.stop()`/`forceCancel()`/키 해제를 중복 호출하지 않는다.

기본 report Task는 FIND 소유 자식·retry·탐험 없이 기존 유한 관측 계약을 따른다. 표의 이동 자식 정리는 선택적 approach에만 해당한다. `isEqual(Task)`과 `toDebugString()`도 기존 상속 ledger에 포함하며, 진단 correlation/랜덤 ID만으로 Task 교체를 강제하지 않는다. 일시 방어 선점의 `onStop` 호출과 authoritative lifecycle이 증명한 영구 `INTERRUPTED` 종료를 구분한다.

## 8. 결과 계약과 한국어 응답 — 초안

matching command-owned root의 종료 증거와 한 번 확정한 FIND outcome을 함께 요구한다. callback만 끝났거나 다른 Task가 끝난 결과를 FOUND로 승격하지 않는다. 기존 GOTO/STORE_HOME/GET 결과·root retirement·STOP 처리는 보존한다.

matching은 접수 때 binding한 같은 Task 객체 참조와 해당 root lifetime에 한정한다. 클래스명·debug 문자열·hash 일치로 다른 root의 종료를 가져오지 않는다. 이 요구는 FIND의 강한 발견 증거에 적용하며 기존 무Task 명령의 완료 방식을 바꾸지 않는다.

| 결과 의미 | 한국어 응답 예시 / 성공 조건 |
|---|---|
| report 발견 | “마을 주민을 찾았어. 확인한 위치는 X 500, Y 90, Z -928이야.” 최종 후보 재검증과 `find_satisfied=true` |
| 부분 관측 중 발견 — 미확정 확장 제안 | “확인한 대상 중 마을 주민을 찾았어. 위치는 X 500, Y 90, Z -928이야. 탐색 범위를 전부 확인하지는 못했어.” 재검증된 발견을 별도로 보고하는 최초 전달문의 제안. 전체 최근접 보장은 없으며 기존 계약 조정과 partial scope·닫힌 payload/mapping 확정 전에는 활성화하지 않음 |
| 로드 범위 미발견 | “불러온 범위에서는 마을 주민을 확인하지 못했어.” 약속한 로드 범위 검사 완료, 만족=false |
| 관측 한도 | “탐색 한도에 도달해서 전부 확인하지는 못했어.” 전체 부재·최근접 보장 없음 |
| 후보 재검증 불가/소실 | “확인하던 대상을 지금은 확인할 수 없어.” 과거 캐시 좌표를 성공 결과로 대체하지 않음 |
| approach 완료 | “마을 주민을 찾아서 가까이 도착했어.” 유효한 같은 대상과 승인된 접근 기준·정리·종료 증거가 모두 충족 |
| approach 요청의 최초 관측 미발견/한도, 유효 후보 없음 | 확인 범위·한계를 보고. 새로운 지역 탐험을 시작하지 않으며 접근 완료 문구 금지 |
| 발견했지만 접근 실패 | “상자는 발견했지만 허용된 이동 방식으로 가까이 갈 수는 없었어.” 발견/접근 결과를 분리 |
| STOP | 기존 STOP의 “멈췄어”만 한 번 전달 |
| 증거 불충분·잘못된 세션·중복 | 기존 unknown/거부/무시 경계, 성공 문구나 새 동작 생성 금지 |

부분 관측의 발견 보고와 관측 후보만의 거리 순위는 기존 계약 Sections 6.4–6.5의 완전성·최근접 선택 및 Section 6.8의 한도 초과 문구와 함께 D7에서 명시적으로 조정해야 한다. 이번 기본 보고 방향만으로 그 의무를 면제하지 않는다. `OBSERVATION_BOUNDS_EXHAUSTED`로 확정된 결과를 FOUND로 승격하거나 성공 좌표를 붙이지 않는다. 부분 발견을 채택하려면 어떤 범위를 검사했는지, 최근접을 주장할 수 있는 조건, 후보 증거·scope·결과 매핑·UI/TTS 문구를 기존 계약과 protocol 및 양쪽 validator에서 일치시켜야 한다.

기본 결과 이유는 기존 Section 6.6의 `FOUND_AND_REPORTED`, `INVALID_TARGET`, `NOT_OBSERVED_IN_LOADED_SCOPE`, `OBSERVATION_BOUNDS_EXHAUSTED`, `TARGET_LOST`, `CANDIDATE_NOT_REVALIDATABLE`, `STOPPED`, `INTERRUPTED`, `INTERNAL_ERROR` 구분을 보존한다. 초기 선택적 approach에는 `FOUND_AND_IN_SAFE_RANGE`, `ALREADY_IN_SAFE_RANGE`, `UNREACHABLE`, `TIMEOUT`의 서로 다른 조건이 필요하다. 기존 `SEARCH_BOUNDS_EXHAUSTED`는 탐험 확장의 역사적 후보이며 초기 접근에 탐험 owner나 wire placeholder를 추가하는 근거가 아니다. 전달문의 `APPROACH_FAILED` 후보로 구체적인 원인 차이를 합치지 않으며, 채택하려면 정확한 원인·기존 이유와의 매핑을 함께 닫는다. 세계/세션 변경도 기존 수명 및 오류 계약과 맞춘다. 이 목록은 wire enum 추가 승인이 아니다.

보고 profile은 기존 Section 10의 정확한 초안 필드 이름과 중첩 위치를 기준으로 한다. 아래는 registry 대상의 FOUND shape이며 현재 구현된 wire 지원을 뜻하지 않는다.

```text
data.result_reason=matching_task_finished
data.result_fidelity=callback_plus_matching_user_task_event
data.effect_profile_id=fabric_chatclef_find_observation
data.effect_profile_version=1
data.effect_kind=find_observation
data.effect_payload:
  completion_mode=LOCATE_AND_REPORT
  target_kind=entity | block | item
  canonical_target_id=<actual registry ID>
  candidate_identity_digest=<validated candidate>
  dimension=<canonical dimension ID>
  x=<signed Java int>
  y=<signed Java int>
  z=<signed Java int>
  observation_scope=<closed kind-appropriate scope>
  find_result=FOUND_AND_REPORTED
  find_satisfied=true
  reason=<trimmed 1..256 character explanation>
```

`data.result_reason`은 수명 증거이며 `data.effect_payload.reason`은 별도의 제한된 설명이다. no-candidate 결과에는 candidate identity·좌표가 금지된다. GET/STORE_HOME/STOP 전용 키를 섞은 FIND payload는 거부한다. 기존 player shape는 D4 조정 전까지 보존하며 `player_identity_digest`와 registry용 `canonical_target_id`를 혼용하지 않는다. D7에서는 기존 정확한 이름을 임의로 재정의하지 않고 완전한 필수/금지 필드·partial scope·비성공 status 행렬을 protocol과 양쪽 validator에서 함께 닫는다. approach profile/version은 미확정이며 report 성공 payload를 재사용해 접근 완료로 가장하지 않는다.

외부 `status=completed`는 조회 절차 완료일 수 있으며, 만족=false인 미발견을 발견으로 표현하지 않는다. `completed/failed/cancelled/unknown`과 결과 reason의 정확한 행렬은 기존 v1 허용값 안에서 확정하고 임의 status를 추가하지 않는다. Python은 session/generation/request/message/root, kind/ID/mode/profile과 필드 집합·타입을 검증한다. `bool`을 좌표 정수로 받아들이지 않는다. 자유 텍스트를 그대로 TTS에 넘기지 않고 접수 시 확정한 표시명과 검증된 결과로 문장을 만든다. 기본 시작 1회·terminal 1회이며 기존 echo guard와 중복 방지를 유지한다.

## 9. 로그 coverage와 실제 출력 검증 계획

| 변경/관측 경계 | 사건과 실제 필드 | 출력·검증 경계 |
|---|---|---|
| input/admission | accepted/rejected, source/final/trust, request/message, mode, 실제 거부 이유 | 기존 Python logger/입력 결과, 제출 0/1회와 correlation 확인 |
| target/catalog | resolved/ambiguous/invalid, kind/ID, digest/resource/session generation, alias source | 해석 owner의 formatter·filter·출력 자료 |
| Task 시작 | operation/root, world/dimension, 원래 중심·기한·방문 예산 | 기존 Fabric logger의 필수 첫 경계 예약 |
| 관측/선택 | visited/matched/retained, revalidation, bounds, 실제 선택/거부 조건 | 상태 변경·제한된 요약; 슬롯/getter마다 출력하지 않음 |
| 접근·방어 선점·재개 | 대상·이동 소유자, 일시 선점 이유, 유지되는 operation·원래 잔여 예산 | 기존 수명 경계와 연결, 새 행동 선택 금지 |
| 결과·정리 | outcome/조건값, commit 여부, 자식 정지·자기 정리 결과, terminal reason | 보호된 terminal/필수 경계. OFF에서도 새 실패 이유·실제 결정값·operation·상세 trace 상태를 정상 로그 정책으로 기록 |
| projection/UI/TTS | matching 증거, profile/schema 검사, 중복/거부, 실제 확인한 전달 단계 | validator·renderer·queue/UI/audio 단계별 증거를 분리 |

AGENTS.md Section 21의 유한 trace 예약, 기존 더 엄격한 한도와 backend-local 로깅 경로를 재사용한다. 기본 후보는 같은 상태 요약 200tick/10초보다 자주 출력하지 않음, 상세 correlation당 256개, 진단 세션 전체 5,000개와 그 안의 terminal/exception/cap 예약 최소 32개다. 보호할 operation 수와 정확한 필수 경계 서명을 먼저 정하고 용량을 계산한다. 고정 32개만으로 FIND 전체 경계가 보호된다고 주장하지 않는다.

필수 서명은 owner + event family + 실제 transition/result/reason + 필요한 phase role로 정의한다. 사전에 보호 대상으로 접수된 각 operation의 필수 첫 경계·terminal·cleanup 결과는 공유 총 cap 안의 예약을 사용하며 반복 상세·요약·다른 operation이 예약을 소비하지 못한다. 의미가 다른 필수 경계를 하나의 family로 합치거나 요약으로 대체하지 않는다. `BOUNDARY`에서 필수 연결을 확인하며 `VERBOSE`만으로 원인 증거를 제공하지 않는다. `OFF`는 상세 인과 trace를 보장하지 않지만 기존 정상 로그와 새 실패 결정 기록을 숨기지 않는다. 설정 변경이나 overlay/HUD 제어로 출력을 강제하지 않는다.

기록 수뿐 아니라 message bytes·pending queue·fingerprint/correlation 저장 크기와 수명도 제한한다. trace 예약 부족은 진단 완전성/접수만 제한하며 실제 FIND 시작·재시도·종료를 거부하지 않는다. 식별자·표시명·예외 문자열은 민감정보·개행을 제거하고 필요한 안전한 correlation만 남긴다. 해시라는 이유만으로 개인정보를 안전하다고 간주하지 않는다.

로그 admission·ID·formatter·cap·sink 실패가 탐색·재시도·성공·방어·cleanup을 바꾸지 않는다. 출력 불가/trace 제외는 가능한 남은 경로에서 제한적으로 보고하며 실제 유실은 unverified로 남긴다. 자동 session reset이나 정상 로그로의 재분류로 cap을 우회하지 않는다. 원문 음성·전체 대화·카탈로그·월드·NBT dump는 없다. runtime ID/tick을 새 필수 예약 서명으로 만들지 않는다.

logger 호출·queue 적재·파일 저장·UI 표시·실제 음성 재생은 다른 증거다. actual formatter/filter/gate/budget/appender를 통과한 출력으로 검증하고 mock 호출 수·내부 카운터·빌드 성공만으로 출력 완료를 주장하지 않는다. 이번 문서 작업에는 새 실행 경계가 없어 runtime log implementation은 `NOT_APPLICABLE / DOCUMENTATION_ONLY`; 향후 구현의 로깅 요구는 그대로 의무다.

## 10. 구현 순서와 검증 계획

향후 구현 요청에서는 기존 Section 17처럼 해당 작업에 필요한 결정만 닫는다. D1–D3의 현행 방향을 입력·대상 계약으로 검증하고, 기본 report에는 D4의 남은 대상 범위, D5의 report 한도, D6의 report 접수 정책, D7의 기본 카탈로그·결과 계약과 정확한 변경 manifest를 닫는다. D2의 정확한 접근 표현과 D5/D6/D7의 접근 전용 결정, 구조물·탐험 확장은 독립적으로 닫힌 report 구현을 막지 않는다.

ID/카탈로그·report 관측·결과 전달 기반을 먼저 검증하고, 지원하기로 결정한 approach에만 이동·수명·안전 계약을 추가한다. command-name 단위 lifecycle registry가 두 모드를 같은 유한 수명으로 안전하게 다루는지 증명하거나 기존 계약에 맞는 validated descriptor-mode dispatch를 검토한다. report만 구현하면 보고 기능 완료로만 표시하고 미지원 접근 요청은 명시적으로 안내한다. 이 순서는 이번 문서화에 소스/테스트/빌드 권한을 추가하지 않는다.

| 자동화 후보 | 필요한 검증 |
|---|---|
| Chat/final ASR 입력 | 같은 표현의 동일 명령, 모드 생략·“찾아줘”=report, 명시적 접근만 approach, item 접근 미지원·종류 충돌 명확화·소지품/보관소/획득 조회의 FIND 제출 0회, partial/echo/비신뢰 0회, 중복 final 1회, STOP/busy/기존 GOTO/GET/보관 보존 |
| 해석·카탈로그 | 모든 실제 등록 ID round-trip, 모드 ID/한글 번역 부재·충돌, ko_kr 우선순위, stale/partial/digest/세션 오류, 전송 완료지만 원본 수집이 잘린 카탈로그 거부 |
| 관측·한도 | 공중 ItemEntity, 캐시 밖/접근 불가 블록, 미로드 확인, visited와 retained 구분, timeout/부분 관측 중 재검증된 발견/tie-break/음수 floor |
| 선택적 접근 | 최초 관측 미발견·한도 시 탐험 이동 0회, 발견 대상만 접근, 경로 중 새 청크를 검색 확장에 사용하지 않음, 대상 소실·안전 거리·자기 자원 정리 |
| 수명·방어 | 선점→재개 원래 기한 유지, STOP/늦은 완료/root 교체 경쟁, 대상 소실, outcome 1회와 자식 정리 |
| 결과·출력 | 정확한 kind/ID/mode/profile/schema, 실패 성공 문구 금지, bool 좌표 거부, UI/TTS 중복 방지 |
| 로깅·성능 | OFF/BOUNDARY/VERBOSE, 필수 예약·공유 cap·sink 실패 비간섭, actual output, tick 비용·메모리·payload 상한 |

기존 Java 테스트 루트는 runtime의 `src/test/java/`, Python은 `tests/`와 `tests/minecraft_chatclef/`다. FIND 전용 `src/test/java/lavi/minecraft/find/`와 `tests/minecraft_chatclef/find/`는 아직 없는 신규 후보 경로이며 기존 소유 영역이라고 주장하지 않는다. 기존 명령·수명 회귀 테스트를 재사용하고 새 ad-hoc 재현 산출물은 AGENTS.md의 `test/test_Isolation` 경계를 따른다. 경로 이름만으로 테스트를 생성하거나 실행하지 않는다.

향후 직접 구현 요청에는 요청 범위의 동반 로그·focused test·[build verification runbook](chatclef-fabric-build-verification.md)의 required clean build를 같은 작업으로 수행하며 단계별 재확인을 만들지 않는다. 이번 문서 검수에서는 실행하지 않는다. 배포·외부 instance 변경·실게임 실행은 그 행동을 포함한 활성 요청이 필요하다. 실제 Minecraft acceptance는 fresh artifact/loaded JAR/Mixin/log 증거와 실제 주민·동물·적대/모드 몹, 바닐라/모드 블록, 지상·수중·공중 아이템, 로드 경계·소실·방어·접근 실패, 실제 UI/TTS·성능으로 확인한다. source, 기능 테스트, 로깅 출력, build, deployment, live game은 각각 보고한다. 이전 자동 보관 테스트 267개 통과는 FIND 검증이 아니다.

## 11. 이번 문서화의 완료 기준

설계 문서와 기존 계약/README의 방향 안내만 갱신한다. 상대 링크 존재, diff 공백 검사, 변경 경로가 문서 3개뿐인지 확인한다. 사용자 변경과 원본 전달문을 보존한다. D1–D3은 현행 문서 방향으로 기록하고 D4–D7의 남은 범위·수치·안전·전송 계약은 미확정으로 유지한다. 기존 계약의 역사·해시·실패 증거와 구현 상태는 바꾸지 않는다. 소스·테스트·설정·프로토콜 구현, 빌드·게임·외부 파일·커밋·푸시는 수행하지 않는다.
