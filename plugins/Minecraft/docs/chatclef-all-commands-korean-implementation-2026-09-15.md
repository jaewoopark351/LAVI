<!-- 20260915_kpopmodder: Track authorized all-command implementation, exact boundaries, and independent verification evidence. -->
# 전체 명령 한국어 구현 기록

구현 당시 상태: 정규 명령 26개와 호환 이름의 공통 한국어 경로 구현·오프라인 검증 수행.
전체 빌드에는 기존 1.21.1 오류가 남았으며, 당시 실게임 검증은 수행하지 않았다.
이후 사용자 실게임 로그에서 일부 Chat 실행과 세 가지 보완 과제를 확인했다.
[실게임 로그 후속 보완 계획](#runtime-followup-20260915)은 문서 검수 후 사용자의 구현 요청에 따라
**후속 구현과 저장소 검증을 완료**했다. 마지막의 후속 구현 기록에 잔여 검사 실패 1개와
실게임 미검증 범위를 포함한 최신 결과를 구분한다.
아래 기존 테스트·빌드 기록은 이 후속 수정의 검증 결과가 아니다.
기준 HEAD: `1b65f980eaf6db44ba725bb1b93dc1971d9b16bd`.

요구사항: [검수된 전체 명령 요구사항](chatclef-all-commands-korean-chat-microphone-requirements-2026-09-15.md).
해당 문서의 문서화 당시 `NONE`/`NOT_RUN`은 역사적 snapshot이다. 이후 사용자가
전체 구현·관련 검증·Java clean build와 독립 책임 분리·폴더화를 요청했다.
시작 시 기존 Markdown 6개의 변경을 보존한다. 외부 배포·게임 실행·commit·push는 요청되지 않았다.

## 구조와 변경 소유권

모든 경로의 절대 기준은 `C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\`이다.

| 기존 책임 | 구현 경계 | 이유 |
| --- | --- | --- |
| `fabric/chatclef/intent/korean_chatclef_rule_parser.py`의 명령별 분기 | `intent/grammar/control/korean_control_rule_parser.py`, `intent/grammar/combat/korean_combat_rule_parser.py`, `intent/grammar/item/korean_item_list_rule_parser.py` | 명령군별 문법 소유권 분리 |
| schema/compiler의 타입별 처리 | `intent/grammar/validation/korean_command_request_validator.py`, `intent/grammar/compilation/korean_command_serializer.py` | 인자 검증과 문자열 생성 분리 |
| 이름·획득 카탈로그 처리 | `intent/names/runtime_command_name_catalog.py`, `intent/names/command_target_resolver.py` | 전체 이름 자료와 명령별 capability 분리 |
| 기존 XYZ 파서 | `intent/navigation/goto/korean_goto_variant_parser.py` 및 기존 파서·결과 DTO·입력 binding의 위임 | 기존 XYZ 보존, 명시 XZ/Y/차원 형태 추가 |
| 확인 메타데이터만 존재 | `input/confirmation/` 및 기존 route/admission의 연결 | 확인 상태와 신뢰·실행 정책의 소유권 분리 |
| Java registry·즉시 명령 결과 | Fabric 전용 bridge 경계 | 실제 등록·토큰·결과를 기존 Python 경로에 제공 |

파일 이동·이름 변경은 없다. 기존 facade/public import를 유지하고 새 책임 파일을
생성한다. imports, package exports, composition graph, tests, 문서를 함께 갱신한다.
Task·Baritone·이동·채굴·전투·보관 알고리즘, 의존성·버전 및 Forge 구현은 변경하지 않는다.

## GOTO 범위·증거 기록 — 구현 전

아래 기록은 한국어 입력 확장과 이후 전체 Java 빌드의 경계를 명시한다.
기존 navigation root cause를 수정하거나 증명하는 작업이 아니다.

```text
GOTO_INCIDENT_RECORD_READ: YES
REPRODUCTION_ID: UNKNOWN
ACTIVE_ARTIFACT_SHA256: UNKNOWN
COMMAND_CORRELATION_ID: UNKNOWN
OPERATION_ID: UNKNOWN
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
PARENT_TASK: UNKNOWN
CHILD_TASK: UNKNOWN
FINITE_COUNTER_OWNER: UNKNOWN
TERMINAL_DECISION_OWNER: UNKNOWN
DIAGNOSTIC_BUDGET_OWNER: GotoInputDiagnostics for input observations; navigation trace UNKNOWN
REQUIRED_CAUSAL_OWNER_BOUNDARY_SET: KoreanGotoCoordinateParser.parse -> input_guard VALID_XYZ/VALID_VARIANT/CLARIFY/NONCOMMAND; GotoTranslationBindingValidator -> original-input match/rejection; existing submission/result owners -> admission rejection/submission/terminal
REQUIRED_CAUSAL_OWNER_BOUNDARY_EMISSION_STATUS: UNKNOWN
REQUIRED_TERMINAL_EMISSION_STATUS: UNKNOWN
FIRST_REQUIRED_PHASE_TRANSITION_EMISSION_STATUS: UNKNOWN
DIAGNOSTIC_SUPPRESSION_REASON: UNKNOWN
PATH_OR_GOAL_OWNER: existing native GOTO Task; runtime instance UNKNOWN
LAST_SUCCESSFUL_BOUNDARY: UNKNOWN
FIRST_FAILING_BOUNDARY: UNKNOWN
TRIGGERING_STATE: trusted Korean coordinate/dimension request
EXPECTED_TRANSITION: validated original arguments -> existing canonical GOTO submission once
OBSERVED_TRANSITION: UNKNOWN
RETRY_OWNER: unchanged existing navigation; runtime UNKNOWN
WANDER_OWNER: unchanged existing navigation; runtime UNKNOWN
EXACT_FILES_AND_HUNKS_PROPOSED: intent/navigation/goto/{korean_goto_coordinate_parser.py:parse,goto_parse_result.py:typed result,goto_parse_decision.py:variant enum,korean_goto_variant_parser.py:new grammar}; input/routing/goto/{goto_translation_binding_validator.py:original tuple/dimension equality,goto_input_diagnostics.py:variant observation}; existing intent schema/compiler delegation and tests
EXACT_PROPOSED_FIX_BOUNDARY: Korean grammar and immutable original-input binding; no navigation fix
FAILED_IMPLEMENTATION_ROLLBACK_SCOPE: NONE
PROPOSED_CHANGE_EXACT_ROLLBACK_UNIT: inverse of only the new grammar/typed-result/binding/logging hunks and their tests in this request; preserve all pre-existing source and six Markdown changes
USER_APPROVED_EXACT_ROLLBACK_UNIT: NONE
PROPOSED_CHANGE_CLASSIFICATION: BEHAVIOR
ACTIVE_WORLD_IDENTITY: UNKNOWN
ACTIVE_WORLD_SAVE_PATH: UNKNOWN
WORLD_COPY_RESTORE_REPLACE_RENAME_HISTORY: UNKNOWN
BARITONE_CACHE_EVIDENCE: NOT_APPLICABLE
CACHE_TARGET_ABSOLUTE_PATH: NOT_APPLICABLE
CACHE_ACTION: NONE
TASK_STOPPED: NOT_APPLICABLE
TASK_STOPPED_EVIDENCE_STATUS: NOT_APPLICABLE
WORLD_EXITED: NOT_APPLICABLE
WORLD_EXITED_EVIDENCE_STATUS: NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED: NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED_EVIDENCE_STATUS: NOT_APPLICABLE
NOT_APPLICABLE_REASON: repository implementation and isolated tests/build only; no game/process/world/cache action
ACTIVE_REQUEST_AUTHORIZES_SOURCE_EDIT: YES
ACTIVE_REQUEST_AUTHORIZES_RECOVERY_ROLLBACK: NO
ACTIVE_REQUEST_AUTHORIZES_BUILD: YES
ACTIVE_REQUEST_AUTHORIZES_DEPLOYMENT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_LAUNCH: NO
ACTIVE_REQUEST_AUTHORIZES_EXTERNAL_INSTANCE_COPY: NO
ACTIVE_REQUEST_AUTHORIZES_RUNTIME_REPRODUCTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_EXECUTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_BLOCK_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_TASK_STOP: NO
ACTIVE_REQUEST_AUTHORIZES_WORLD_EXIT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_PROCESS_CONTROL: NO
ACTIVE_REQUEST_AUTHORIZES_MANUAL_CACHE_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_COMMIT: NO
ACTIVE_REQUEST_AUTHORIZES_PUSH: NO
```

## 한국어 문법·이름 처리 구현

기존 `KoreanChatClefRuleParser`는 기존 FIND/H5/STORE_HOME/GOTO의 구체적인 거절과
소유권을 유지하면서 명령군 파서에 위임한다. `grammar/control`은 설정·등록 및
SCAN/LOCATE_STRUCTURE, `grammar/combat`은 공격·지속 작업·food score,
`grammar/item`은 목록·명시 전체 보관·방어구 세트를 각각 소유한다. 공통 질문·부정문
검사는 기존 명령별 guard를 대체하지 않는다. 새 명령과 새 슬롯 형태는 schema에서
원문을 다시 파싱해 타입·인자·요청 결합을 확인하며 LLM의 거절 복구를 허용하지 않는다.

`names/RuntimeCommandNameCatalog`는 현재 세션의 실제 등록 snapshot만 색인한다.
`CommandTargetResolver`는 이름 해석과 해당 명령의 native 토큰·capability를 대조한다.
서로 다른 등록 ID가 같은 native 토큰을 공유하면 한국어 이름이 고유해도 실행을
거절한다. Java가 구별할 수 없는 요청을 이름 번역만으로 확정하지 않는다.
검증된 native 토큰은 256자 이내의 기존 문자열 문법(`:`, `.`, `/`, `-` 포함)을
보존하므로 모드 GIVE translation key와 정확한 ItemList 카탈로그 별칭을 전달할 수 있다.
기존 FIND 언어·별칭 자료는 이름 자료로만 재사용하고 언어 키로 등록 ID를 만들어
실제 등록 사실로 취급하지 않는다. translation의 `data.runtime_catalogue`에는
`session_id`와 `catalogue_sha256`, `data.target_labels`에는 검증한 토큰별 한국어
표시 이름을 전달한다. 실제 Butler 기본 사용자 요청에는 `butler_user_bound`도 남긴다.
재연결·리로드·사용자 변경 후 제출 권한은 기존 세션·admission 소유자가 재검증한다.

GET/EQUIP/명시 DEPOSIT/DEPOSIT_ALL의 Java ItemList 카탈로그 제한은 유지한다.
GIVE는 실제 등록 snapshot에 native inventory 토큰이 있으면 GET 카탈로그 밖도
해석한다. 모드 GIVE 토큰이 `item.example.crystal`처럼 전체 translation key인 경우도
그대로 검증해 생성한다. 모든 아이템을 GET의 지원 집합으로 제한하지 않는다.

목록은 각 항목 수량과 동일 대상 합산을 모두 Java 정수 범위에서 검사한다.
명시된 소수·중복 수량·잘못된 단위·overflow에는 기본 수량을 적용하지 않는다.
GAMMA는 native Double에 맞는 유한값만 받는다. GOTO는 기존 절대 XYZ를 보존하면서
명시 X/Z·높이 Y·차원·차원이 붙은 좌표를 같은 원문 binding으로 전달한다.
`100 20 좌표로 가줘`처럼 기존에 불완전 XYZ로 거절하던 문장은 계속 거절하고,
XZ는 `X 100 Z 20 좌표로 가줘`처럼 축을 명시한다.

### 26개 명령과 호환 이름 사용표

아래 Chat/마이크 표기는 **실제 Python 운영 입력 경로와 대역 bridge를 사용한 제출
검증**이다. 실게임 실행, 실제 마이크 하드웨어, 음성 재생이나 명령 효과의 성공을
뜻하지 않는다. 최종 결과·Java 빌드·실게임 상태는 아래 별도 검증 기록을 따른다.
수량의 `?`는 생략 가능성을 설명하는 표기이며 생성 명령에 포함하지 않는다.

| 명령 | 한국어 입력 예 | 인자·선택 형태와 생성 예 | Chat / 최종 마이크 | 확인 |
| --- | --- | --- | --- | --- |
| `attack` | 좀비 세 마리 공격해 줘 | 실제 entity 토큰, 처치 수 기본 1 → `attack zombie 3`; 플레이어 요청 거절 | 대역 제출 검증 / 대역 제출 검증 | R3 |
| `auto_deposit_trust` | 여기를 자동 보관 장소로 등록해 줘 | 단일 대상 `auto_deposit_trust`; 주변 16x16은 `auto_deposit_trust area 16x16` | 대역 제출 검증 / 대역 제출 검증 | 기존 R2 대상·claim 검사 |
| `auto_deposit_trusted_list` | 자동 보관 장소 목록 보여 줘 | 무인자 `auto_deposit_trusted_list` | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `auto_deposit_untrust` | 자동 보관 장소 test-id 등록 해제해 줘 | stable ID 지정 → `auto_deposit_untrust test-id`; ID 생략 시 기존 정확한 열린/조준 대상 | 대역 제출 검증 / 대역 제출 검증 | 기존 R2 대상 검사 |
| `chatclef` | 챗클레프 꺼 줘 / 켜 줘 | 필수 on/off → `chatclef off` / `chatclef on` | 대역 제출 검증 / 대역 제출 검증 | R4 |
| `deposit` | 철괴 16개 보관해 줘 | `deposit iron_ingot 16`; 명시 목록 지원. “장비와 도구를 제외한 아이템 전부 보관해 줘” → 무인자 `deposit` | 대역 제출 검증 / 대역 제출 검증 | 기존 R2 명시 수량·범위 |
| `deposit_all` | 인벤토리 전부 보관해 줘 | `deposit_all`; “철괴 두 개와 금괴 세 개 전체 보관해 줘” → `deposit_all [iron_ingot 2, gold_ingot 3]` | 대역 제출 검증 / 대역 제출 검증 | 기존 R2 명시 전체·목록 |
| `equip` | 다이아몬드 흉갑 장착해 줘 | `equip diamond_chestplate`; 개별 수량·목록 및 가죽/철/금/다이아몬드/네더라이트 방어구 세트 지원(`equip iron`) | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `find` | 철 골렘 찾아 줘 / 위치만 알려 줘 | `find entity minecraft:iron_golem approach` / `report`; 기존 auto/entity/block/item/player 종류 유지 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `follow` | Alex 따라가 줘 / 나 따라와 줘 | `follow Alex`; “나”는 현재 검증된 Butler 연결이 있을 때만 무인자 `follow` | 대역 제출 검증 / 대역 제출 검증 | R3 |
| `food` | 음식 10포인트만큼 모아 줘 | 필수 food score → `food 10`; 음식 개수·배고픔 아이콘 수로 바꾸지 않음 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `gamer` | 마인크래프트 엔딩까지 진행해 줘 | 무인자 `gamer` | 대역 제출 검증 / 대역 제출 검증 | R3 |
| `gamma` | 밝기를 1.5로 설정해 줘 | `gamma 1.5`; “밝기를 기본값으로 돌려 줘” → `gamma 1.0`; 유한 Double | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `get` | 철괴 16개 구해 줘 | `get iron_ingot 16`; “철괴 두 개와 금괴 세 개 구해 줘” → `get [iron_ingot 2, gold_ingot 3]`; 생략 수량 1 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `give` | Alex에게 철괴 세 개 줘 | 상대 지정은 반드시 `give Alex iron_ingot 3`; 현재 Butler 연결에서 “나에게 철괴 두 개 줘” → `give iron_ingot 2`; 한 아이템만 지원 | 대역 제출 검증 / 대역 제출 검증 | 기존 R2 상대·수량 검사 |
| `goto` | 500, 90, -928 좌표로 가 줘 | `goto 500 90 -928`; 명시 X/Z, 높이 Y, 오버월드/네더/엔드 및 차원+좌표 지원 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `hero` | 적대 몹 계속 정리해 줘 | 무인자 `hero`; 지속 작업 | 대역 제출 검증 / 대역 제출 검증 | R3 |
| `idle` | 가만히 있어 줘 | 무인자 `idle`; 지속 작업 | 대역 제출 검증 / 대역 제출 검증 | R3 |
| `locate_structure` | 엔드 요새 찾아가 줘 / 사막 사원 찾아가 줘 | `locate_structure stronghold` / `desert_temple`; “요새” 단독은 모호성 거절 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `meat` | 고기 10포인트만큼 모아 줘 | 필수 food score → `meat 10`; 기존 보유 전체 food score 가산 의미 유지 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `overlay` | 오버레이 꺼 줘 / 켜 줘 | 필수 on/off → `overlay off` / `overlay on` | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `reload_settings` | 마인크래프트 설정 다시 불러와 줘 | 무인자 `reload_settings` | 대역 제출 검증 / 대역 제출 검증 | R4 |
| `resetmemory` | 챗클레프 대화 기록 초기화해 줘 | 무인자 `resetmemory`; ChatClef 대화 기록 범위 | 대역 제출 검증 / 대역 제출 검증 | R4 |
| `scan` | 다이아몬드 원석 블록 위치 스캔해 줘 | 실제 `Blocks` 필드 토큰으로 `scan <token>`; “스캔해 줘”는 `scan`, 기존 DIRT 기본값 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `stop` | 멈춰 줘 / 중지해 줘 | 무인자 `stop`; 기존 최우선 제어 경로 | 대역 제출 검증 / 대역 제출 검증 | 없음 |
| `store_home` | 아이템 집에 정리해 줘 | 무인자 `store_home`; 기존 cursor/trusted-home 보호 | 대역 제출 검증 / 대역 제출 검증 | 기존 R2 보호 |
| `자동보관등록` 호환 이름 | 주변 16x16 자동 보관 장소 등록해 줘 | 한국어 경로는 `auto_deposit_trust area 16x16`으로 합침. 기존 raw `@자동보관등록 영역 16x16` / `반경 16x16` 보존 | 위 정규 명령에 포함 | 기존 H5 검사 |

R3/R4는 원래 요청에 묶인 한국어 확인을 받은 뒤 현재 조건을 재검증한다. 취소·만료·
중복 확인은 제출하지 않는다. 실제로 확인을 받아도 게임 명령의 성공까지 보증하지 않는다.

### 전체 이름 자료의 실제 측정값

측정 시점: 2026-09-15, 로컬 읽기 전용 1.20.1 언어 asset과 저장소 자료.
FIND vocabulary SHA-256:
`0feef311b6b6f4fb2a169792689c858814e5c2bd44f48ea671b00f16fcc69ffe`.
초기 측정의 `asset_status=local_asset_loaded`, 읽기 경고 0, 추가 FIND 별칭은 14개였다.
아래 실제 registry 검사에서 TNT 별칭 2개를 추가한 이후 값과 구분한다.

| 자료 집합 | 이름 항목 수 | 종류를 지정했을 때 같은 ID로 해석 | 모호한 항목 | 해석 실패/다른 ID |
| --- | ---: | ---: | ---: | ---: |
| FIND entity vocabulary | 124 | 124 | 0 | 0 |
| FIND block vocabulary | 1,275 | 1,275 | 0 | 0 |
| FIND item vocabulary | 1,820 | 1,748 | 72 (서로 충돌하는 이름 14종) | 0 |
| 합계 | 3,219 | 3,147 | 72 | 0 |

이 표는 **이름 후보 자료의 전수 해석 검사**이다. 언어 파일에는 표시용·과거 키가
있고 BlockItem 후보도 포함되므로 3,219를 실제 등록 대상 수나 실행 지원 수로
사용하지 않는다. 한글을 포함한 표시 문자열은 entity 124, block 1,274, item
1,819개이다. 나머지 두 항목의 ASCII 표시 이름까지 한글 음성 별칭 지원으로
계산하지 않는다. 실제 활성 registry와 mod 전체 수·번역 누락·실행 가능 수는
해당 세션 snapshot을 받기 전에는 `UNKNOWN`이다.

GET 카탈로그는 591개이다. 기존 고정 별칭 564개가 고유 카탈로그 대상 553개를
가리키고, 고정 별칭만으로 연결되지 않은 대상은 38개이다. 이것은 고정 별칭의
범위이며 재질 조합·문맥별 기본값·runtime 이름 해석까지 포함한 최종 지원율이 아니다.
기존 정책 자료의 분류는 DIRECT_ITEM 173, DIRECT_BLOCK 377,
CANONICALIZED_LEGACY 15, GENERIC_GROUP 20, UNSUPPORTED 6이다.
따라서 38개를 모두 새 구현 누락 또는 모두 실행 불가로 분류하지 않는다.

같은 정책 자료에서 translation key와 연결되는 고유 vanilla 이름 552개를 추출해
새 runtime 이름 색인/명령 resolver를 전수 검사했다. 모든 행이 동일 ID 후보에
포함되고, 고유한 이름은 해석되며 충돌은 거절된다. 한글 포함 표시 이름은 551개,
충돌은 `벽돌`(`brick`/`bricks`)과 `네더 벽돌`(`nether_brick`/`nether_bricks`)의
4개 대상이다. 이 검사는 자료 기반 대역 snapshot 검증이며 실게임 registry 수집
증거로 바꾸어 보고하지 않는다.

이름 해석 성공 뒤에도 기존 기능 한계가 적용된다. 일반 ItemList는 TaskCatalogue,
EQUIP는 실제 ArmorItem/방패 처리 범위, SCAN은 실제 `Blocks` 필드,
LOCATE_STRUCTURE는 두 구조물, GIVE는 상대·보유/획득 조건을 따른다.
자동방어·생존·KEEP_LOADOUT·working-set·trusted-container 및 Task 알고리즘은
이 이름 처리의 성공 여부로 해제하지 않는다.

### 실제 vanilla 등록 목록을 이용한 추가 전수 검사

<!-- 20260915_kpopmodder: Separate real bootstrap registry evidence from vocabulary candidates and live-game acceptance. -->

Java bootstrap 테스트가 실제 1.20.1 registry에서 생성한
[검증 JSON](../../../test/test_Isolation/all_commands_java_pass_20260915-182702-057/vanilla-runtime-catalogue.json)을
[Python 전수 검사](../../../test/test_Isolation/all_commands_parser_vanilla_20260915.py)로 읽어
[결과 JSON](../../../test/test_Isolation/all_commands_parser_vanilla_20260915_results.json)에 기록했다.
자료 SHA-256은 `b66ec85ba260830ed04213e0f3820c509ebff408197fd97bf0dc3b785e0ef3b7`이다.
이 자료는 실제 vanilla registry와 설치된 한국어 asset을 사용한 오프라인 증거이다.
실게임의 모드 목록·서버 상태·명령 executor 등록 집합·명령 작업 성공의 증거는 아니다.

| 실제 등록 종류 | 대상 수 | 한글 이름 또는 검증 별칭 있음 | 한국어 이름으로 고유 ID 확정 | 추가 구분 필요 |
| --- | ---: | ---: | ---: | ---: |
| item | 1,255 | 1,255 | 1,212 | 43 |
| block | 1,003 | 1,003 | 939 | 64 |
| entity | 124 | 124 | 124 | 0 |
| 합계 | 2,382 | 2,382 | 2,275 | 107 |

공식 한국어 locale 문자열은 2,382개 모두 존재했다. 그중 item/block TNT 두 항목은
표기가 ASCII이므로 기존 `korean_find_aliases.json`에 실제 `minecraft:tnt` ID를
가리키는 `티엔티` 별칭을 추가했다. 다른 대상의 전체 이름 처리를 샘플 별칭으로
대신한 것이 아니다. 기존 FIND alias 수는 14개에서 16개가 됐다.
동일한 실제 ID에 더 구체적인 공식 vocabulary 이름이 있으면 추가 이름으로 재사용한다.

기본 이름 충돌은 53종, 139개 ID였다(item 5종/43개, block 48종/96개).
동일 ID의 기존 공식 이름으로 block 32개를 추가로 구분했다. 남은 107개는 임의로
하나를 선택하지 않고 후보를 안내한다. item 충돌은 벽돌·네더 벽돌 각각 2개,
현수막 무늬 6개, 음반 16개, 대장장이 형판 17개이다. 형판 17개는 native GIVE 토큰도
모두 `smithing_template`이므로 한국어 이름이나 명시 ID만 바꿔도 원래 GIVE가
종류를 구분할 수 없다. 이 경우 `ambiguous_native_command_token`으로 거절한다.

모호한 이름 107개가 남아 있으므로 전체 아이템의 고유한 한국어 접근이 완료됐다고
표현하지 않는다. 명령별로 중복 집계한 모호 요청 125건에 대해 표시된 실제 ID를
이름 대신 넣어 다시 파싱했을 때 **108건은 canonical 생성**, **17건은 native GIVE
형판 토큰 충돌로 거절**됐다. 새 후보 번호 대화 상태는 만들지 않았다.
같은 이름에 관한 추가 구분과 native 명령 자체가 구분하지 못하는 한계를 나눠 안내한다.

한국어 후보 안내는 live 입력 신뢰 증명, 닫힌 모호성 reason, runtime fingerprint,
DTO 스키마, 원문 전체의 deterministic 재파싱 결과가 일치한 경우에만 기존 거절
경로에서 전달한다. 자유로운 `translation.message`를 그대로 출력하지 않으며,
검증된 등록 ID 최대 5개로 한국어 문장을 다시 만든다.
예를 들어 `벽돌 두 개 구해 줘`에는
`같은 한국어 이름의 대상이 여러 개야. 원하는 대상의 ID를 이름 대신 넣어 다시 말해 줘.
후보(최대 5개): minecraft:brick, minecraft:bricks`라고 안내한다.
`minecraft:brick 두 개 구해 줘`처럼 다시 요청하면 명시 대상을 검증한다.
형판의 native 토큰 충돌에는 기존 명령이 구분하지 못한다는 별도 안내를 한다.

[후보 전달 운영 검사](../../../tests/minecraft_chatclef/lavi_input/all_commands/test_ambiguous_name_delivery.py)는
GET 단일/목록, SCAN, native GIVE 토큰 충돌의 Chat·최종 마이크 입력 8건에서
화면과 음성 수신 경로에 동일 한국어 문장 1회, bridge 제출 0회, LLM 호출 0회를
확인한다. 악성·중복 후보와 원문·신뢰 증명 위조 검사 4건도 통과했다.
실제 음성 장치 재생은 이 검증에 포함하지 않는다.

전체 Python suite 실행 뒤 마지막 동반 로그 보완으로 기존 feature admission
projector가 이미 결정된 두 모호성 reason을 관측하고 기존 sanitizer가 그 두 값만
허용하도록 했다. 후보 ID·원문은 이 로그에 넣지 않는다. 실제 UTF-8 FileHandler로
두 결정×Chat/마이크 4건의 이벤트가 각각 한 번 기록됨을 검사하고, 로그 sink가
예외를 던져도 후보 응답 1회·제출 0회·LLM 호출 0회가 유지됨을 확인했다.
이 로그 보완 후 관련 후보/generic crafting/기존 Chat·마이크 검사는
**72 passed / 181 subtests passed**이다. 전체 suite 시점과 이 후속 로그 검증을 구분한다.

| native capability | 대상 수 | 한국어 해석·검증·canonical 생성 | 모호해 제출하지 않음 | 구현 오류 |
| --- | ---: | ---: | ---: | ---: |
| GET | 645 | 639 | 6 | 0 |
| DEPOSIT | 645 | 639 | 6 | 0 |
| DEPOSIT_ALL | 645 | 639 | 6 | 0 |
| EQUIP | 21 | 21 | 0 | 0 |
| GIVE | 1,255 | 1,212 | 43 | 0 |
| SCAN | 1,003 | 939 | 64 | 0 |
| ATTACK | 123 | 123 | 0 | 0 |
| 합계(명령별 중복 포함) | 4,337 | 4,212 | 125 | 0 |

ATTACK의 124개 entity 중 player 1개는 한국어 공격 capability에서 제외한다.
EQUIP 21개는 현재 native 장비 동작이 처리하는 ArmorItem/방패와 정확한
TaskCatalogue 대상이 모두 확인된 집합이다. 현재 보유·장착·획득 성공을 뜻하지 않는다.

앞의 static GET 591개는 카탈로그 이름 수이며 runtime 645개는 실제 single-item ID 수다.
645개 ID에는 정확한 카탈로그 별칭 659개가 연결돼 있고, 이 중 95개는 static 591개
자료에 없다. 반대로 static 자료 27개는 runtime의 exact single-item 별칭 집합 밖에
있다. 이는 그룹·과거 별칭 등 별도 문법 판정 대상이므로 같은 단위의 지원율로
합산하거나 모두 획득 불가라고 보고하지 않는다.

전수 검사에서 `아이템 액자`와 `발광 아이템 액자`의 DEPOSIT가 광역 STORE_HOME
거절 규칙에 잡히는 실제 누락을 발견해 고쳤다. 명시적인 집 보관 검사는 먼저 유지하고,
정확한 item 명령을 일반 인벤토리 표현보다 먼저 처리한다. 이후 전수 검사 구현 오류는 0건이다.

### parser·자료 검사 증거

- [전체 명령 문법·목록·runtime 이름·수량 검사](../../../tests/test_minecraft_chatclef_all_command_grammar.py):
  최초 10개 테스트와 기존 [카탈로그 별칭 전수 검사](../../../tests/minecraft_chatclef/catalog_coverage/test_runtime_alias_catalog_coverage_snapshot.py)를
  합쳐 18 passed / 616 subtests passed. pytest의 이미 import된 anyio rewrite 경고
  1개가 있었고 테스트 실패는 없었다.
- 기존 rule/schema/item resolver/acquisition/extension 통합을 포함한 focused
  검사에서 45 passed를 확인했다. 이후 추가한 전수 자료 검사는 위 별도 결과다.
- FIND request/input/name/alias/asset/output 경계와 GOTO input binding은
  277 passed / 12 subtests passed. UTF-8 formatter·실제 파일 sink 검사도 포함한다.
  기존 공용 pytest temp의 ACL 오류 21건은 저장소 `test/test_Isolation` 아래의
  요청별 fixture 경로로 재실행하여 해결했다. 시스템 권한이나 외부 파일을 변경하지 않았다.
- `intent/**`, `input/routing/goto/**`, 새 grammar 테스트에 Ruff 검사 통과.
- 추가 검수에서 기존 전각 숫자·원문 정규화, 잘못된 수량의 generic crafting 거절 소유권을
  보존했다. 모드 토큰 길이·문법과 native 토큰 충돌 검사를 추가해 grammar 테스트는
  12개가 됐다. 당시 grammar/schema/integration/generic crafting/전체 명령 운영 행렬을
  함께 실행한 결과는 **287 passed / 715 subtests passed**이다.
- TNT와 액자 회귀를 추가한 현재 grammar 테스트는 13개이며, STORE_HOME/H5/단일 등록/
  전체 명령 Chat·마이크/generic crafting/FIND 이름 경계를 함께 실행해
  **586 passed / 1,007 subtests passed**를 확인했다. 저장소 내 독립 임시 fixture를
  사용하여 기존 공용 pytest temp ACL에 영향을 받지 않도록 했다.
- 운영 제출 행렬은 [명령 예제](../../../tests/minecraft_chatclef/lavi_input/all_commands/command_cases.py)와
  [실제 Python 입력 경로 검사](../../../tests/minecraft_chatclef/lavi_input/all_commands/test_all_command_submission.py)에 있다.
  기본 26×2=52, 선택 형태 24×2=48, 등록 집합 1개로 101 passed를 확인했다.
  최종 결과/lifecycle 통합은 아래 최신 검증 기록에 별도로 기록한다.

## 검증 기록

최신 통합 결과는 문서 마지막의 **최종 검증·미검증 범위**에 기록한다.
아래의 초기 실패·중간 통과 수치는 발견과 수정의 이력이며 최종 결과를 대신하지 않는다.

## Python 입력·결과 연결의 변경 소유자

| 파일/폴더 | 변경 이유와 검증된 경계 |
| --- | --- |
| `fabric/chatclef/command_registry/korean_command_registry.py` | 정규 명령 26개의 문법·확인·공개 입력 정보를 실제 Java와 대조. parser/admission/lifecycle의 독립 readiness 축 유지 |
| `fabric/chatclef/intent/grammar/`, `intent/names/`, `intent/navigation/goto/` | 명령군별 해석·인자 검증·직렬화·실제 이름/capability 처리 분리 |
| `fabric/chatclef/input/confirmation/` | 원본 명령·인자·출처·세션·generation에 묶인 확인/취소/60초 만료와 일회용 receipt |
| `fabric/chatclef/input/auto_deposit_trust/single_registration/` | 단일 등록 권한을 기존 H5 영역 등록과 구분. 원문·canonical·세션·generation 재검증 |
| `fabric/chatclef/input/routing/ordinary/`, `input/gating/`, `input/stop/`, `command_registry/admission/` | 기존 신뢰 검사·중복·busy·STOP·제출 경로에 연결. 질문/부정 STOP이 대기 확인을 취소하지 않음 |
| `fabric/chatclef/session/catalogue/` | 압축·JSON 검증과 불변 snapshot 보관을 별도 책임으로 분리. 세션 종료/재접속/자료 변경 때 오래된 이름 무효화 |
| `fabric/chatclef/transport/server/client_session/catalogue/`, 기존 handshake/server 구성 | 활성 socket·session의 자료만 수신. 상태 화면에는 압축 payload 대신 요약만 공개 |
| `fabric/chatclef/transport/command_submission/admission/korean_submission_context_validator.py` | 기존 제출 lock 안에서 확인/등록 generation, catalogue hash, Butler 사용자를 다시 검사. metadata 누락도 필요한 명령에서는 거절 |
| `fabric/chatclef/result/instant/` | Java가 결정한 실제 설정값·검색·등록 결과를 엄격하게 검증하고 불변 DTO로 보관 |
| `fabric/chatclef/transport/command_feedback/lifecycle/descriptor/trusted_translation/` | 인자 검증·목록 투영·한국어 표시 이름 분리. 중복 목록 합산 후 실제 수량과 대상 이름 유지 |
| `fabric/chatclef/transport/command_feedback/lifecycle/evidence/instant/`, 기존 result/terminal 소유자 | request/command/profile/status가 일치하는 결과만 한국어 상세 안내에 사용. 실패·미확인을 성공과 분리 |
| `fabric/chatclef/response/command_lifecycle/terminal/instant/` | 설정·SCAN·신뢰 목록/등록 결과를 화면과 TTS의 같은 문장으로 제공 |
| `fabric/chatclef/diagnostics/instant_command/`, 기존 제출·수신 진단 | 실제 결정 이후 상관 ID·이유·상태를 유한한 한 줄로 기록. 출력 실패가 결정이나 재시도를 바꾸지 않음 |

확인은 출처별 하나씩 최대 2개만 보관하며, STOP은 기존 중지 경로를 먼저 사용한다.
Chat과 마이크의 명령 사전·실행기를 각각 만들지 않는다. 마이크 중간 인식은 제출하지 않는다.
단일 등록, 영역 등록, DEPOSIT, DEPOSIT_ALL, STORE_HOME은 서로 다른 기존 소유자를 유지한다.

최종 검수에서 확인·단일 등록 receipt가 임의 `_owner`의 메서드를 호출할 수 있는
경계를 보완했다. 실제 발급 owner의 정확한 타입과 원래 클래스의 메서드를 사용하고,
발급 map의 객체 동일성·현재 proof·세션·요청 결합을 다시 검사한다. 가짜 owner,
subclass, instance 메서드 덮어쓰기로 direct admission이나 commit을 우회할 수 없으며,
commit 거절 뒤에도 receipt를 소비한다. 형식이 잘못된 가짜 owner의 빈 슬롯은 먼저
거절해 잘못된 권한 자료 때문에 입력 경로가 예외로 끝나지 않도록 했다.

### 결과 안내와 자료 보호

- GAMMA는 Java `finish()`와 실제 값 읽기를 연결하고, 제출 수락만으로 terminal을 소비하지 않는다.
- CHATCLEF OFF의 기존 disable/cancel 부작용을 보존하면서 독립 Fabric 결과 전달 및 ON 경로를 유지한다.
- RELOAD_SETTINGS는 기존 loader가 파일별 실패를 모두 노출하지 않아 `unknown`으로 안내한다.
  “설정 다시 읽기 요청은 처리됐어. 각 설정 파일이 모두 적용됐는지는 확인하지 못했어.”가 정확한 결과다.
- 일반 Task 명령은 기존 작업 종료와 실제 효과 검증을 구분한다. 획득·보관·장착·전달을
  단순 callback만으로 성공했다고 말하지 않는다. 지속 작업은 계속 실행 중으로 다룬다.
- 한국어 목록은 최대 64개, native token은 최대 256자로 검증한다. 중복 대상은 수량을
  합산하고 Java 정수 범위를 재검사한다. 합산 뒤 단일 항목이 되어도 수량/한국어 이름을 잃지 않는다.
- 한국어 검증 완료 목록의 feedback lexer 상한은 32,768자로 두어 허용된 전체 목록을 수용한다.
  기존 raw GUI lexer의 512자 경계는 유지한다. 선택한 DEPOSIT 목록을 전체 인벤토리로 안내하지 않는다.
- 호환 별칭 `자동보관등록 영역/반경 16x16`의 Java canonical 결과 이름도 정확히 연결한다.
- Java native 토큰 자체가 여러 등록 ID를 가리키면 모호성으로 거절한다. 이름 번역만으로
  원래 명령이 구분할 수 없는 대상을 구분했다고 주장하지 않는다.

### 진단 출력 증거

새 확인·단일 등록·catalogue 갱신·즉시 결과 검사는 저장소 `test/test_Isolation` 아래의
실제 UTF-8 로그 파일을 `FileHandler`와 formatter로 작성하고 다시 읽어 검증한다.
명령/request/event, session generation, 채택/거절 이유, native 결과 code의 출력과
중복 terminal 억제를 확인한다. throwing/no-op sink가 실행 횟수·확인 결정·terminal을
바꾸지 않는 검사도 포함한다. 이는 **오프라인 테스트의 출력 증거**이며 실게임 trace 증거가 아니다.

## Java 결과·메타데이터 경계 (2026-09-15)

- 등록/번역 수집은 `bridge/catalogue/registry`, `catalogue/names`, 실제 ItemList
  토큰 어댑터는 `catalogue/capability`, 압축은 `catalogue/encoding`, 재적재 무효화는
  `catalogue/lifecycle`, 갱신 전달은 `bridge/transport/catalogue`가 각각 소유한다.
  기존 entrypoint·handshake·bridge client는 구성과 위임만 연결한다.
- `CLIENT_STARTED`와 리소스 변경 뒤 client tick에서만 게임 데이터를 읽는다.
  불변 snapshot을 handshake metadata와 기존 `event` envelope로 전달한다.
  Butler 사용자가 바뀌면 등록 정보를 다시 수집하지 않고 문맥 snapshot만 갱신한다.
- 압축 Base64 768 KiB, 전체 envelope 1 MiB, 해제된 JSON 16 MiB, 항목 65,536개
  상한을 적용한다. 상한 초과나 수집 실패는 일부 목록 성공으로 포장하지 않고
  snapshot 전체를 `available=false`로 표시한다.
- 즉시 명령의 결과 관측은 `lavi/minecraft/command/result/instant`에서 동기 실행
  범위 하나에 결합한다. 기존 호출과 finish 순서, Task 결과 소유자는 보존한다.
  결과 투영은 `bridge/command/result/instant`가 담당한다.

### 최소 upstream 변경과 독립적인 원복 단위

기준 HEAD는 `1b65f980eaf6db44ba725bb1b93dc1971d9b16bd`이다. 아래 기록은
소스 계약 차이의 증거이며 과거 게임 장애의 원인 증명은 아니다.

| 파일 / 메서드 | 변경 전 SHA-256 | 정확한 변경 / 역변경 단위 |
| --- | --- | --- |
| `adris/altoclef/commands/SetGammaCommand.java` / `call` | `251B85B4FEA1460EEFF5B13BFCD288392FCB5ED79A00EB7658AA34ED272F5C18` | 기존 `changeGamma` 뒤 `finish()` 한 번을 추가. 원복은 해당 주석과 호출만 제거 |
| `adris/altoclef/commands/random/ScanCommand.java` / `call` | `A5F2F1DB5C59DB186219A676510307E2D9038D524B445221AD6BBEE332E8A374` | 기존 선택 결과·좌표·미발견 분기에 결과 관측 호출만 추가. 원복은 각 관측 호출만 제거 |
| `adris/altoclef/commands/GiveCommand.java` / `call` | `C02C59DE782C1F20FAB7B004C3DC173C08E3F8FFA5E1E4052744B811AB67CF85` | 기존 사용자 없음·상대 미발견·아이템 미발견 조기 종료에 실패 관측만 추가. 원복은 관측 호출만 제거 |
| `adris/altoclef/commands/FollowCommand.java` / `call` | `AAC2E97A7A06968E0C269CC8C05BAB0CE315ADC85C9F2D6AFF312E98E6E73504` | 기존 Butler 사용자 없음 조기 종료에 실패 관측만 추가. 원복은 관측 호출만 제거 |
| `adris/altoclef/commands/AttackPlayerOrMobCommand.java` / `call`, nested Task 생성·predicate·`isEqualResource` | `70E974E9D58CB4A5A164651EECA47612AEB50DFFDF7C097E6A88E893B4E158ED` | 한국어 bridge 요청의 mob-only 권한을 기존 Task에 고정하고 모든 PlayerEntity를 제외. 원복은 3인자 생성·flag 필드·predicate guard·동등성 flag 비교와 관련 주석만 제거 |

한국어 ATTACK의 native token은 같은 이름의 플레이어도 일치시킬 수 있으므로
입력 시점의 플레이어 목록 검사만으로 권한을 보존할 수 없다. 이 최소 경계는
요청의 한국어 출처에서 플레이어 제외 정책을 한 번 고정하고, 기존 Task predicate가
나중에 관측하는 플레이어에도 적용한다. 원래 영어 요청은 기존 플레이어 공격
문법을 유지한다. 자동방어·KillEntitiesTask·드롭 획득 알고리즘은 변경하지 않는다.
다른 권한의 기존 Task와 동등하게 합쳐지지 않도록 frozen flag도 동등성에 포함한다.

GAMMA의 동기 setter가 반환된 뒤 Command callback을 알리지 않는 gap은
`CommandExecutor.executeRecursive`와 bridge의 `waiting_for_callback_without_user_task`
조건으로 확인했다. bridge가 임의로 callback을 꾸며내거나 모든 명령의 종료 규칙을
바꾸지 않도록 해당 명령의 기존 `finish()` 계약을 완성한다. SCAN의 reflection과
검색 알고리즘은 바꾸지 않는다. 조회 결과 관측은 기존 분기가 결정한 값만 읽는다.

즉시 설정 결과와 기존 idle root의 관계는 명령별 동기 결과 관측 및 기존 finish
증거로만 투영한다. Task 실행 명령의 root 선택·중지·재시도·정리에는 관여하지 않는다.
`chatclef off`의 기존 AI disable·user task cancel·TaskRunner disable 부작용을
보존하며 Fabric bridge의 독립적인 결과 전달·재활성화 경로를 유지한다.

이 경계의 1.20.1 compile 및 아래 Java 집중 테스트는 통과했다. 전체 clean build는
별도 최종 빌드 기록을 따른다. 런타임 재현·배포는 `NOT_RUN`이다.

### Java 구조·검증 보완

SCAN의 실제 `Blocks` 필드 조회는 등록 목록 투영과 책임이 달라
`catalogue/capability/FabricChatClefBlockCommandTokens.java`로 분리했다.
리소스 무효화와 snapshot 수집은 같은 capture 소유자의 동기화 경계로 직렬화한다.
따라서 먼저 시작한 수집이 나중의 리소스 무효화 결과를 덮어쓰지 않는다.

기존 `AttackPlayerOrMobCommand`의 private nested Task는 upstream 명령 소유자에
그대로 둔다. 이번 요구에 필요한 변경은 기존 predicate에 적용하는 권한 하나의 고정과
동등성 구분뿐이다. 새로운 권한 실행 범위는 `command/attack`에 분리했으며 기존 전투
Task를 다른 폴더로 옮기거나 별도 전투 실행기를 만들지 않는다. 상위 엔진의 기존
이동·전투·드롭 획득 책임을 보존하기 위한 경계이며, 독립 책임을 새로 혼합하지 않는다.

- 첫 quick compile에서 `ConversationHistory.size()` 연결 오류를 발견해 실제 기존 API인
  `getListJSON().size()`로 수정했다. 기억 지우기의 기본 system prompt 보존을 반영해
  사용자 대화 항목 수만 관측하며 대화 내용은 기록하지 않는다.
- 다음 1.20.1 compile은 통과했다. 같은 실행의 26개 JUnit 중 23개가 통과했고,
  3개 native ATTACK 테스트는 게임 없는 JVM의 client 초기화 누락으로 실패했다.
  기존 `HeadlessMinecraftClientSession` fixture를 재사용해 보완했다.
- 실제 등록 목록 전체와 ArmorItem/방패 capability를 검사하는 2개 테스트를 추가했다.
  vanilla 한국어 자료는 설치된 asset index 5의 `ko_kr.json`
  SHA-1 `6c71b9f0d3ce56e9c80d6d6b6fff6732583ec620`를 읽기만 사용한다.
  생성되는 검증 JSON은 저장소 `test/test_Isolation` 아래에 두며, 모드가 로드된 실게임
  registry 결과와 구분한다. 테스트 실행 자체는 Minecraft를 시작하지 않는다.

### Java 최종 집중 검증 결과

- 실행: `:1.20.1:allCommandsFocusedTests`, 기존 offline init 및
  `src/test/autoDeposit/auto-deposit-tests.init.gradle`, 새
  `src/test/allCommands/all-commands-tests.init.gradle`을 함께 사용했다.
  process JDK는 Temurin 21.0.12.1+1, Gradle 8.8이다. 저장소 안 기존 Gradle home과
  임시 폴더를 사용하고 외부 Gradle dependency cache는 읽기 전용으로 참조했다.
- 결과: **29 started / 29 successful / 0 failed / 0 aborted / 0 skipped /
  0 container failures**, `BUILD SUCCESSFUL in 3m 18s`. 같은 실행의
  `:1.20.1:compileJava`도 통과했다.
- 로그: [Java 집중 검증 로그](../../../test/test_Isolation/all_commands_java_pass_20260915-182702-057/tests.log).
- 설정 결과와 기존 idle root 분리, callback 없는 완료 차단, 실패·UNKNOWN 구분,
  stale/cross-thread 관측 배제, 실제 formatter·stdout sink 및 sink 실패 비간섭,
  세션·snapshot admission, 한국어 ATTACK의 실제 native predicate·Task 동등성을 검증했다.
- 기존 Overlay/AutoDeposit/StoreHome registrar가 첫 client tick에 등록하는 순서를
  확인했다. 명령 이름 목록 변화와 Butler 사용자 변화는 항목 전체 재수집 없이 문맥만
  갱신한다. 늦은 등록 갱신, 동일 문맥의 중복 snapshot 생성 방지, entries 객체 재사용,
  리소스 무효화의 unavailable 전환을 실제 capture owner 테스트로 검증했다.
- [전체 vanilla 등록 자료](../../../test/test_Isolation/all_commands_java_pass_20260915-182702-057/vanilla-runtime-catalogue.json):
  item **1,255**, block **1,003**, entity **124**, 합계 **2,382**. 각 행의 실제 translation
  key에 연결된 한국어 이름은 **2,382개**, 이 자료의 번역 누락은 **0개**다.
  파일 SHA-256은 `B66EC85BA260830ED04213E0F3820C509EBFF408197FD97BF0DC3B785E0EF3B7`이다.
- 같은 자료에서 native single-ID token이 존재하는 수는 GET/DEPOSIT/DEPOSIT_ALL 각
  **645**, EQUIP **21**, GIVE **1,255**, SCAN **1,003**, ATTACK **123**(player 제외),
  FIND **2,382**다. 이는 단일 대상 문법 capability이며 획득·작업 완료 성공 수가 아니다.
  generic group alias와 모드 실등록 범위는 별도다. 기존 Python 정책 자료 591개와
  이 실제 Java 수집 수치를 같은 카탈로그 상한으로 취급하지 않는다.
- 전체 자료의 `registered_commands`는 이 격리된 vanilla exporter에서 빈 목록이다.
  운영 snapshot은 실제 Java executor 목록을 읽는다. 이 자료만으로 모드 등록 명령,
  모드 번역, 실연결·실게임 결과를 검증했다고 주장하지 않는다.

### 전체 빌드에서 확인한 Identifier 호환 수정

첫 전체 clean build의 기본 1.21.1 컴파일에서 신규 코드 두 곳의
`Identifier(String, String)` 생성자가 비공개인 오류를 확인했다.
`FabricChatClefCatalogueReloadListener.getFabricId`와
`FabricChatClefKoreanLanguageReader.read`의 Identifier 생성 표현식만 기존 프로젝트
`IdentifierVer`와 같은 `MC >= 12100` 전처리 패턴으로 바꿨다. 1.21 이상에서는
`Identifier.of`를, 이전 버전에서는 기존 생성자를 사용한다. 의존성이나 버전을
변경하지 않았으며 두 표현식 외의 기능도 변경하지 않았다.

위 29개 통과 기록은 이 두 호환 표현식을 수정하기 전의 1.20.1 검증이다.
수정 후 동일 소스의 전체 clean build 및 29개 재검증 결과는 아래 최종 빌드 기록을
따른다. 첫 전체 빌드 로그는
[Full build 로그](../../../test/test_Isolation/all_commands_20260915/build/20260915-183206-252-Full/build.log)에 남긴다.

## 최종 검증·미검증 범위

### Python 통합 검증

권한 경계와 동반 로그 보완 후 최종 전체 검사에서
**2,209 passed / 15,837 subtests passed / 2 skipped / 1 failed**를 확인했다(94.05초).
범위는 `tests/minecraft_chatclef/`,
`tests/test_minecraft_chatclef_*.py`, `tests/test_llm_minecraft_input_router.py`다.
[최종 전체 로그](../../../test/test_Isolation/all_commands_20260915/full_python_offline_20260915_authority_final.log)와
[저장소 전용 runner](../../../test/test_Isolation/all_commands_20260915/run_tests.py)에 실행 증거를 남겼다.

- 실패 1개는 이번 변경이 없는 `FabricChatClefCommandDispatcher.java`의 **HEAD hash**를
  과거 기준과 동일하게 요구하는 기존 검사다. 기준 hash는 `5afbb6d9…`, HEAD hash는
  `f89a1f6a…`이며, 기존 committed STOP/detach 변경과 맞지 않는 단정이다.
  `test_java_contract_sources_have_not_drifted_at_head`의 실패를 숨기려고 기준값을 바꾸지 않았다.
- 생략 2개는 `LAVI_MINECRAFT_RUNTIME_TESTS=1`이 필요한 실제 LAVI/Minecraft 검사다.
  이번 요청의 게임 실행 범위 밖이므로 실행하지 않았다.
- 26×Chat/최종 마이크 기본 52개, 선택 형태 48개, 등록 집합 검사를 포함한
  실제 Python 입력 행렬이 통과했다. 확인·취소·만료·STOP·신뢰·중복·busy·세션 교체,
  GET/FIND/GOTO/자동보관 및 기존 영어/GUI 계약을 포함한다.
- 후보 응답은 모호명칭 8개 Chat/최종 마이크 사례와 위조/악성 자료 4개에서 검증했다.
  명령 제출과 LLM 우회 실행은 0회이며 화면·음성 안내는 한 번이다.
- 실제 서버 구성의 즉시 결과·오류·미확인·중복·stale socket·UTF-8 파일 로그 검사를 포함한다.
  전체 lifecycle 별도 검수에서도 347 tests / 10,735 subtests가 통과했다.
- 확인·단일 등록 receipt의 위조 권한 10개 회귀를 추가하고 관련 기존 경로까지
  **461 tests / 362 subtests**를 검증했다.
  [권한 경계 회귀 로그](../../../test/test_Isolation/all_commands_20260915/receipt_authority_final_regression.log).
- 최종 변경 Python 148개 파일의 Ruff와 `git diff --check` 검사는 통과했다. 테스트 runner는 프로젝트 venv와
  요청별 임시 폴더만 사용했으며 외부 패키지·설정·폴더 권한을 바꾸지 않았다.

### Java 빌드와 산출물

사용자 요청에 따라 새 Windows PowerShell 창에서 저장소 안의
[run-build.ps1](../../../test/test_Isolation/all_commands_20260915/build/run-build.ps1)을 실행했다.
JDK·Gradle home·TEMP는 프로세스 범위에서만 지정했다. 시스템 설정·전역 환경 변수는
변경하지 않았으며 외부 언어 asset과 dependency cache는 읽기만 했다.

| 검증 | 결과와 증거 |
| --- | --- |
| runbook의 전체 `clean build --rerun-tasks` | **실패**, exit 1. 최종 실행은 `:1.21.1:compileJava`의 기존 오류 7개에서 종료. [결과](../../../test/test_Isolation/all_commands_20260915/build/20260915-183827-918-Full/result.json), [전체 로그](../../../test/test_Isolation/all_commands_20260915/build/20260915-183827-918-Full/build.log) |
| 이후 1.20.1 강제 재실행 | **성공**, exit 0, 35 tasks executed. `allCommandsFocusedTests`, `remapJar`, `validateAccessWidener`를 `--rerun-tasks --no-build-cache`로 실행. [결과](../../../test/test_Isolation/all_commands_20260915/build/20260915-184013-207-TargetArtifact/result.json), [전체 로그](../../../test/test_Isolation/all_commands_20260915/build/20260915-184013-207-TargetArtifact/build.log) |
| 동일 최종 Java 소스 집중 테스트 | **29/29 통과**, 실패·생략 0개. 앞의 Identifier 호환 수정 후 재검증 |
| 게임 배포·Minecraft 실행·실월드 작업 | **NOT_RUN** |

전체 clean 실패가 남은 원본 파일은 `MinecraftToolEquipPort.java`(2개),
`FindBlockSearchTask.java`(2개), `GotoMaterialInventory.java`(2개),
`GotoMaterialSources.java`(1개)다. 모두 이번 작업에서 수정하지 않은 파일이며,
작업 시작 시 Java 소스가 clean이었고 최종 해당 파일의 diff도 없음을 확인했다.
이번 신규 Identifier 오류 2개는 수정 후 전체 빌드에서 사라졌다. 기존 1.21.1 문제를
해결하려고 다른 엔진·GOTO 소스를 변경하지 않았다.

1.20.1 성공은 **지정한 테스트·산출물 생성 그래프의 성공**이다. 전체 clean build 성공,
실게임 호환성, Mixin 주입, 명령 효과 성공을 뜻하지 않는다. 실행별 소스·리소스·빌드
입력 hash와 HEAD는 각 결과 폴더의 `build-inputs.json`에 기록했다.

생성 산출물:

- [chatclef-1.20.1-0.18.23.jar](../runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar)
- 크기: **8,964,741 bytes**
- 생성 시각: **2026-09-15 18:43:34 KST**
- SHA-256: `ED643D2E0FF0A8517F68236E1DD6334812BD4D566015F8F7723EAD2E95F8C4DD`
- 프로젝트 1.20.1 Java release target: **17**

프로젝트 `lavi/`·`adris/` 클래스 2,328개는 모두 bytecode major 61(Java 17)이며
신규 필수 클래스 20개가 JAR에 포함돼 있다. 포함 라이브러리의 bytecode는 다르며,
Jackson의 `META-INF/versions/19` 아래 major 63 클래스 3개도 있어 “JAR의 모든
클래스가 major 61”이라고 표현하지 않는다. Minecraft/모드 버전과 intermediary
mapping metadata를 확인했다. `validateAccessWidener`는 **NO-SOURCE**였으므로
실제 Access Widener 내용을 검증했다는 증거로 사용하지 않는다.

기존 Gradle 폐기 예정 API, 전처리기의 Windows DLL fallback 및 Mixin 대상 경고가
남아 있다. 전자는 이 실행을 중단시키지 않았고, Mixin 대상 경고 3개는 실제 런타임
주입 성공 여부가 미검증이다.

### 남은 검증과 원래 기능 한계

- 실제 마이크 인식 품질, 실제 Gradio 브라우저 표시, 음성 재생, 연결된 모드팩의 등록·번역
  자료, 게임 세션 재접속/리소스 리로드, 실제 명령 효과와 자동방어·생존 우선순위는
  실게임에서 검증하지 않았다. 오프라인에서는 기존 경계와 대역 상태·결과를 검증했다.
- 새 Java 메타데이터와 상세 즉시 결과를 사용하는 범위는 이 Java 변경이 포함된 backend와
  연결해야 한다. 저장소 JAR 생성만으로 현재 실행 중인 외부 인스턴스가 갱신되지는 않는다.
- 이름 해석은 작업 성공의 보장이 아니다. ItemList의 TaskCatalogue, 장비 종류, 로드된
  블록/상대, 보유량, 신뢰 컨테이너·장비 보존 규칙은 원래 Java 명령의 조건을 따른다.
- `locate_structure`는 원래 지원하는 stronghold/desert_temple만, `scan`은 원래 로드된
  범위와 Blocks 필드만 처리한다. 기존 Java가 구분하지 못하는 GIVE 형판은 거절한다.
- RELOAD_SETTINGS는 파일별 실제 적용 결과가 없어 미확인으로 안내한다. 일반 작업도
  정확한 효과 증거가 없으면 시작·종료와 실제 획득·보관 성공을 구분한다.
- 외부 인스턴스 복사·배포, 게임/월드/캐시 조작, commit·push는 수행하지 않았다.

## 직접 확인할 Chat·마이크 문장

아래는 사용자가 추후 연결된 게임에서 확인할 문장이다. 이번 작업에서 실행한
실월드 테스트 기록이 아니다. Chat에 한 번 입력하고, 같은 문장을 마이크로 말해
**최종 인식이 나온 뒤** 비교한다. 실행 중인 작업이 끝나거나 STOP 결과가 나온 뒤
다음 작업을 요청한다. 확인이 필요한 요청에는 같은 입력 출처에서 `확인` 또는
`취소`라고 답한다.

| 순서 | 문장 | 확인할 점 |
| --- | --- | --- |
| 1 | 오버레이 꺼 줘 / 오버레이 켜 줘 | 실제 설정값에 맞는 결과, 이후 busy가 남지 않음 |
| 2 | 밝기를 1.5로 설정해 줘 | 값 적용 안내, 실제 완료와 제출 안내 구분 |
| 3 | 철괴 두 개와 금괴 세 개 구해 줘 | `get [iron_ingot 2, gold_ingot 3]` 한 번 |
| 4 | 철괴 두 개와 금괴 세 개 보관해 줘 | `deposit` 선택 목록 유지, 전부 보관으로 바뀌지 않음 |
| 5 | 다이아몬드 흉갑 장착해 줘 | 실제 장비 이름과 결과, 획득 가능 여부는 원래 기능 조건 |
| 6 | Alex에게 철괴 세 개 줘 | 상대·수량 유지, 상대/아이템이 없으면 실패 안내 |
| 7 | 좀비 세 마리 공격해 줘 → 취소 | 확인 전과 취소 후 제출 0회 |
| 8 | Alex 따라가 줘 → 확인 → 멈춰 줘 | 확인 후 한 번 시작, STOP 결과 한 번 |
| 9 | 철 골렘 위치만 알려 줘 | 기존 FIND report 모드 유지 |
| 10 | 다이아몬드 원석 블록 위치 스캔해 줘 | FIND/GET으로 바뀌지 않고 SCAN 좌표 또는 미발견 안내 |
| 11 | 자동 보관 장소 목록 보여 줘 | 한국어 상태·좌표 또는 빈 목록 안내 |
| 12 | 챗클레프 꺼 줘 → 확인 → 챗클레프 켜 줘 → 확인 | OFF 결과 전달 뒤 ON 요청·확인·결과가 가능 |
| 13 | 벽돌 세 개 구해 줘 | 모호한 이름의 후보 안내, 대상 확정 전 제출 0회 |
| 14 | 철괴 0개 구해 줘 / 철괴 구하지 마 / 철괴 구해 줄 수 있어? | 수량 오류·부정문·질문이 실행되지 않음 |

GOTO는 위 전체 명령 표의 `500, 90, -928 좌표로 가 줘`, `X 500 Z -928 좌표로 가 줘`,
`높이 90으로 가 줘`, `네더로 가 줘`로 형태를 구분한다. 실제 이동과 블록 변경은
별도의 실게임 확인 범위이며 이번에 수행하지 않았다. 전체 26개 기본 문장과 선택
형태는 위 사용표 및 `command_cases.py`에 함께 남겼다.

<a id="runtime-followup-20260915"></a>

## 2026-09-15 실게임 로그 후속 보완 계획 — 문서화만

<!-- 20260915_kpopmodder: Record observed spelling fallthrough, stable-ID filtering damage, and missing EQUIP effect evidence without implementing fixes. -->

### 현재 요청과 근거의 범위

사용자는 세 문제의 해결 방향을 설명받은 뒤 **코드를 수정하지 않는 문서화 작업**을
요청했다. 이 절은 확인된 현상, 제안하는 구현 경계, 추후 검증 기준을 기록한다.
Python·Java·테스트·설정·별칭 자료·실행 스크립트는 이 문서화에서 변경하지 않는다.
이 문서는 빌드·배포·Minecraft 실행·실월드 변경·commit·push를 실행하라는 지시가 아니다.

주요 근거는 [LAVI 실행 로그](../../../logs/20260915_191746_log.txt)와 사용자 첨부
인벤토리 사진이다. 로그의 시각은 2026-09-15 KST이며, 아래 줄 번호는 해당 파일 기준이다.
Minecraft 측 대조 자료는 읽기 전용으로 확인한
`C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log`,
`instance_audit.txt`, `stdout-logs.txt`다. 로그는 실행 중 증가할 수 있으므로 재검수할 때
파일명·시각·이벤트 내용도 함께 대조한다. 외부 로그나 사진을 저장소에 복사하지 않았다.

| 항목 | 확인된 사실 | 근거와 해석의 한계 |
| --- | --- | --- |
| 제어 명칭의 철자 변형 | 19:25:39 `챗클래프 꺼줘`가 `fallthrough / no_minecraft_trigger` 뒤 일반 LLM 대화로 넘어감 | 550–558줄. 이후 `챗클레프 꺼 줘`는 확인을 거쳐 19:26:08 OFF, 19:26:15 ON 실행. 앞의 대화 응답은 게임 실행 결과가 아님 |
| 아이템 명칭의 철자 변형 | 19:27:27 `다이아 레겡스 장착해줘`가 `fallthrough / unclassified_internal_reason` 뒤 일반 LLM 대화로 넘어감 | 883–891줄. 이후 `다이아 바지 장착해줘`의 `equip diamond_leggings`는 19:27:44 종료. 내부 원인 코드만으로 모든 오탈자에 같은 원인이 있다고 단정하지 않음 |
| 보관 장소 ID 출력 | 19:24:47 SafetyFilter 결과와 TTS 입력에서 ID 내부 `2c8`이 치환됨 | 450줄 필터 감지, 465줄 원본 ID, 488줄 치환 ID, 498줄 TTS 분할 입력. UI는 499줄 큐 등록만 확인되며 실제 화면 표시·복사 문자열은 미검증. 등록 데이터 변경의 증거는 없음 |
| EQUIP 결과 검증 | 흉갑·투구·부츠·레깅스 Task가 각각 종료됐지만 응답은 실제 장착을 확인하지 못했다고 안내 | 762–763, 787–788, 860–861, 940–941줄. 사진에는 네 부위 착용이 보이지만 각 요청 전후 상태와 원인을 사진만으로 증명하지 않음 |

손상된 식별자의 정확한 재현 자료:

```text
원본: td_be0062c83f14cb8bd68b64b9
출력: td_be006검열됨3f14cb8bd68b64b9
```

이 로그 구간의 일반 명령은 모두 `lavi_chat_ui` 출처였다. 실제 마이크 최종 입력의
동작은 이 자료로 확인되지 않았다. 세 문제의 수정 후 효과도 아직 검증하지 않았다.
목록의 최종 결과는 500줄에서 TTS 큐에 등록됐지만, 534줄의 19:25:21 재생 결과는
`delivered=false / interrupted`다. 511줄의 `played=true`는 별도 시작 안내에 대한
기록이다. 따라서 훼손된 ID가 끝까지 낭독됐거나 전체 목록 재생이 완료됐다고 단정하지 않는다.

### 1. 철자 변형과 해석 실패의 처리

목표는 의미가 확실한 표기 변형을 해석하고, Minecraft 명령으로 판정할 근거가 있는
실패 요청에 대상 확인 안내를 제공하는 것이다.

- `챗클래프`는 `챗클레프`, `레겡스`는 `레깅스`의 검증된 표기 변형으로 기존
  명령·아이템 이름 해석기에 연결한다. 자유 문장 전체를 일괄 치환하거나 유사도만으로
  대상을 선택하지 않는다. 실제 등록 ID와 명령별 capability 검사는 그대로 적용한다.
- 명령 후보 감지와 인자 해석 실패 처리를 함께 검토한다. `no_minecraft_trigger`로
  일찍 탈락하는 경우와, 장착 동작을 감지한 뒤 대상 해석·거절 소유권을 놓치는 경우를
  구분한다. 파서의 성공 예시 두 개만 늘리는 것으로 완료하지 않는다.
- 신뢰된 Minecraft 요청 문맥과 기존 명령 소유권에 근거가 있을 때만 실패 안내를
  처리한다. 일상 대화의 `장착`, 질문·부정문·인용문을 실행 명령으로 바꾸지 않는다.
  Minecraft와 관계없는 일반 대화의 기존 경로도 보존한다.
- 미등록·미지원·모호한 대상은 실행하지 않고 이유나 검증된 후보를 안내한다.
  후보가 없으면 이름을 다시 입력하도록 안내한다. 임의 후보를 만들거나 일반 LLM의
  추측을 실행 권한으로 삼지 않는다.
- 대상을 재입력하면 기존 입력 신뢰·원문 binding·인자 검증·admission을 다시 거친다.
  `chatclef off/on` 등 기존 확인 대상은 여전히 원래 요청에 묶인 확인이 필요하다.
  불명확한 요청에 단순히 `네`라고 답한 사실만으로 새 명령을 실행하지 않는다.
- Chat과 최종 마이크는 동일한 owner를 사용하며 중간 인식은 제출하지 않는다.
  별도 음성 사전·실행기·확인 상태를 만들지 않는다.

기존 검토 경계는 `MinecraftChatClefInputIntentGate`, `KoreanChatClefRuleParser`,
명령군별 파서, `CommandTargetResolver`, `OrdinaryItemTranslationRejectionOwner`와
기존 확인·제출 경로다. 정확한 수정 함수와 자료 위치는 구현 시 현재 소스를 다시
대조해 확정한다. 모든 명령에 범용 오탈자 자동 교정을 약속하는 계획은 아니다.

현재 소스를 읽기 전용으로 대조한 사실과 런타임 추론은 다음처럼 구분한다.
아래 파일의 기준 경로는 `plugins/Minecraft/fabric/chatclef/`다.

| 기존 소유 파일 | 소스로 확인한 사실 | 이번 계획에서의 의미 |
| --- | --- | --- |
| `intent/grammar/control/korean_control_rule_parser.py` | 제어 명칭 정규식에 `챗클레프`, `채클레프`, `chatclef`가 있고 `챗클래프`는 없음 | 기존 제어 문법에 표기 변형을 연결할 지점 |
| `intent/resources/korean_equipment_aliases.json` | `레깅스`, `바지`는 `leggings`에 연결되고 `레겡스`는 없음 | 검증된 표기 변형과 장비 계열 인식의 일관성을 확인할 자료 |
| `input/ownership/item_command/item_command_translation_rejection_evidence.py` 및 `item_command_resolver_family_matcher.py` | 거절 소유권의 근거에 장비 계열 또는 명시적 Minecraft 표시를 사용 | 별칭 추가 후에도 실패 요청의 소유권이 유지되는지 검증할 경계 |
| `input/diagnostics/projection/minecraft_korean_controlled_reason_sanitizer.py` | 허용 목록 밖의 사유를 `unclassified_internal_reason`으로 축약 | 883줄만으로 원래 내부 분기를 특정할 수 없음 |

두 번째 사례의 내부 실패 분기는 **UNKNOWN**이다. 장비 계열 인식·거절 소유권의 누락은
소스에 근거한 검토 가설이며, 해당 런타임 분기가 직접 관측됐다는 뜻이 아니다.
후속 구현에서는 기존 진단 소유자가 결정한 제한된 사유를 구분해 기록하고, 실제 동일 입력
경로를 검증한다. 로그의 사유 문자열만 바꿔 라우팅 문제가 해결됐다고 판단하지 않는다.

### 2. 보관 장소 ID의 출력·음성 전달

목표는 실제 보관 목록의 검증된 ID를 화면에서 정확히 표시·복사할 수 있도록 보존하고,
음성 전달에서 손상된 ID를 사용하지 않는 것이다. 현재 확인된 손상은 필터 결과와 TTS
입력까지이며, 화면 출력은 별도 경로의 보존·검증 대상이다.

- 보관 목록의 구조화된 결과에서 **설명 문장과 식별자**를 구분한다. ID 보호 여부는
  현재 유효한 명령 결과와 목록 항목에 대한 검증으로 결정한다. `td_` 접두사나 정규식에
  맞는 문자열이라는 이유만으로 사용자·LLM 텍스트를 필터에서 제외하지 않는다.
- 보호 정보는 기존 결과 소유자가 검증한 **동일 요청·세션의 목록 snapshot과 정확한 ID
  항목**에서만 생성한다. `source`, 이벤트 ID, 문자열 타입 또는 사용자 제공 보호 표시만으로
  출처를 신뢰하지 않는다. 전달 중 연결이 누락·변조되면 보호 예외를 적용하지 않으며,
  그 경우 정확한 ID가 전달됐다고 보고하지 않는다.
- 기존 애플리케이션 출력 필터 소유자를 재사용한다. 주변 설명에는 기존 SafetyFilter를
  적용하고, 검증된 식별자만 원문 그대로 안전하게 화면에 표시한다. 표시 문자열의
  escaping과 길이 제한은 유지한다. Minecraft가 전체 출력 필터를 우회하지 않는다.
- 음성은 긴 ID 대신 목록 번호·좌표·상태 중심으로 읽는 방향을 권장한다. 번호는 표시
  보조 수단이며 `auto_deposit_untrust`가 받는 stable ID를 대체하는 새 인자는 아니다.
  화면에는 복사 가능한 실제 ID를 유지한다. 이 음성 요약은 아직 구현되지 않았다.
- 목록의 범위·개수·상태와 기존 순서를 보존한다. `UNKNOWN_OR_STALE` 항목을 지금
  접근 가능한 보관 장소로 안내하지 않는다. registry나 저장 파일의 ID는 변경하지 않는다.
- 전체 출력 파이프라인을 통과한 최종 화면 문자열에서 원본과 동일한지 확인한다.
  표시 ID를 복사해 기존 등록 해제 입력에 넣는 왕복 검사에서도 동일 대상을 해석하고
  원래의 신뢰·대상 검사를 거쳐야 한다. 세션·요청 binding 불일치나 없는·모호한 ID는
  기존 정책에 따라 처리한다. `UNKNOWN_OR_STALE` 상태만으로 유효한 ID의 등록 해제를
  막는 새 정책은 추가하지 않는다. ID를 지정한 기존 `unregisterById` 의미를 보존한다.

출력 손상의 관측 지점은 저장소 루트 [SafetyFilter](../../../safety_filter.py)다.
필터에 전달되기 전 이미 문자열로 합쳐지는 위치와 그 이후 재필터링 지점까지 확인하여
최소한의 구조화 전달 경계를 정한다. 임의 텍스트의 전역 필터 예외는 추가하지 않는다.
현재 결과 발행은 `llm_core/routed_response/routed_external_response_publisher.py`,
UI 전달은 `llm_core/routed_response/presentation/ui/routed_response_ui_sink_delivery.py`의
`deliver`가 각각 맡는다. UI는 `request.text`를 별도로 렌더링해 callback으로 전달하며,
큐 등록 성공이 실제 브라우저 표시·복사값의 증거는 아니다.

현재 명령 TTS 경로는 `tts_core/tts_component.py`의 `receive_input` →
`prepare_string_input_items` → `tts_core/text_processor.py`의 `normalize_text_item`에서
`clean_text`를 적용한다. 이후 `TtsLifecycleResponseInputCoordinator.receive`는
`current_plugin.synthesize`를 직접 전달한다. 별도 수동 TTS UI는
`tts_component.py`의 `wrapper_synthesize`에서 필터를 호출한다. 이를 명령 lifecycle의
공통 재필터링 경로로 혼동하지 않고, 변경이 닿는 각 경로를 따로 검증한다.
큐·재생·중복 방지는 기존
`tts_core/delivery/lifecycle_response/tts_lifecycle_response_delivery_adapter.py`가 소유한다.

### 3. 실제 장비 슬롯에 근거한 EQUIP 결과

현재 Java `FabricChatClefCommandEffectTrackerFactory`는 GET 효과 관측만 선택하고
나머지는 `FabricChatClefNoEffectTracker`로 처리한다. Python
`CommandTerminalEvidenceProfileRegistry`의 EQUIP도 `cautious_terminal`이다.
따라서 Task 종료만으로 실제 착용을 단정하지 않는 현재 응답은 결과 계약에 부합한다.
후속 목표는 성공 문구를 강제로 바꾸는 것이 아니라, 부족한 장착 효과 증거를 보완하는 것이다.
`EquipArmorTask.isFinished()` 자체는 이미 착용 상태를 검사한다. 부족한 것은 그 슬롯
증거를 요청에 결합해 bridge와 Python에 구조화하여 전달하는 경계다. 엔진이 착용 여부를
전혀 검사하지 않는다는 의미가 아니다.

1. 기존 Fabric 전용 효과 관측 경계에서 요청에 결합된 대상·예상 장비 슬롯과 실행 전
   상태를 기록한다. 게임 데이터는 기존 Minecraft 소유 스레드에서 읽고, 불변 snapshot을
   결과 전달에 사용한다. WebSocket 수신 스레드에서 장비 슬롯을 조회하지 않는다.
2. 기존 작업의 종료 시점과 슬롯 갱신 순서를 확인해 실제 종료 후 관측을 결합한다.
   요청·Task·세션·월드·플레이어의 일치 여부를 검사하고 늦은 결과·교체된 세션의
   snapshot을 배제한다. 검증 때문에 Task 종료를 지연하거나 재장착·재시도하지 않는다.
   기존 `FabricChatClefCommandOutcomeClassifier`의 dispatch 반환, 결합된 root Task,
   일치하는 종료 이벤트, 명령 callback, 중지 상태 확인을 보존한다. callback만 도착하거나
   다른 Task가 종료됐다는 이유로 정상 종료·착용 성공을 확정하지 않는다.
3. Java의 typed effect 결과를 기존 bridge envelope와 Python의 명령별 증거 평가·한국어
   결과 렌더링에 연결한다. 구버전 Java, 누락·잘못된 payload, 관측 불가는 계속 미확인으로
   처리한다. EQUIP 공개 플래그나 성공 분류만 바꾸지 않는다.
4. 개별 방어구 네 부위뿐 아니라 기존 EQUIP의 목록·세트·방패·대체 대상 문법과 실제
   지원 조건을 대조한다. 수량을 착용 가능한 슬롯 수로 임의 변환하지 않고, 충돌하는
   슬롯 요청이나 일부만 충족된 목록을 전체 성공으로 안내하지 않는다.
   `EquipArmorTask.armorTestAll`의 의미대로 **모든 ItemTarget을 충족(AND)**하되,
   각 target의 `getMatches()` 대체품 중 **하나를 착용하면 그 target을 충족(OR)**한다.
   검증 대상은 요청에 결합된 후보 집합이며, 임의 대표 아이템 하나로 축소하지 않는다.

현재 단일 아이템 capability는 `FabricChatClefItemCommandTokens`가 실제 catalogue
토큰과 `Equipment` 검사를 만족하고 `ArmorItem` 또는 바닐라 `Items.SHIELD`인 대상에
제공한다. 명령의 `Equipment` 검사 통과만으로 겉날개·호박·머리 아이템이나 모든 모드
방패까지 지원한다고 확대하지 않는다. 목록·별칭의 대체품은 실제 Task의 처리 조건을
별도로 대조한다. 원래 지원하지 않는 장비의 획득·장착을 이번 결과 검증에서 새로 만들지 않는다.

| 관측 상태 | 결과 안내 기준 |
| --- | --- |
| 정상 종료 후 모든 대상이 허용 대체품과 해당 슬롯 기준으로 충족됨 | 실제 확인한 장비의 착용을 안내 |
| 실행 전부터 같은 장비가 있고 정상 종료 후에도 충족됨 | 이미 착용 중임을 안내; 새 장착 효과가 발생했다고 주장하지 않음 |
| 정상 종료 후 읽을 수 있는 슬롯이 요청과 불일치 | 확인된 미장착 또는 부분 충족 상태를 안내; 원인을 추측하지 않음 |
| 슬롯 관측 불가, binding 불일치, 효과 자료 없음 | 실제 장착 확인 불가를 안내 |
| STOP·취소·실패·연결 종료 | 기존 종료 사유를 보존; 우연히 장비가 보인다는 이유로 성공으로 덮어쓰지 않음 |

관측 자료에는 실제 조회 시점과 출처를 명시한다. 클라이언트 슬롯 관측은 해당 시점의
착용 상태 증거이며, 별도 근거 없이 서버가 특정 요청을 승인했다는 증거로 표현하지 않는다.
기존 장착·획득 알고리즘, TaskRunner, 자동방어·생존 및 장비 보호 정책은 변경 대상이 아니다.

### 책임 분리와 동반 로그 계획

입력 해석, 애플리케이션 출력 필터, Fabric의 게임 상태 관측, Python 결과 평가는 각각
기존 소유자를 재사용한다. 사용자의 책임 분리·폴더화 요구는 추후 구현에도 적용한다.
독립 책임을 한 파일에 합치지 않고, 새 EQUIP 관측이 필요하면 기존 Fabric 효과 관측
폴더 아래 최소 단위로 둔다. 기존 upstream Task를 이동하거나 범용 실행 프레임워크를
추가하지 않는다. Fabric 구현을 공통 Minecraft 계층이나 Forge 쪽에 넣지 않는다.

| 관측 경계 | 기존 logger에 기록할 최소 결정 증거 | 실제 출력 검증 지점 |
| --- | --- | --- |
| 한국어 명령 해석·거절·제출 | 입력 이벤트 상관 ID, 출처·최종 인식 여부, 적용한 표기 규칙, 결정된 대상, 거절·확인 사유, 제출 횟수 | 공통 route의 formatter·필터·sink를 거친 로그와 제출 수 |
| 보관 목록 결과 표시 | 요청 상관 ID, 검증된 ID 항목 수, 문장 필터 적용·ID 보존 여부, 단계별 전달 결과 | UI 큐·화면 이력/표시, TTS 정규화 입력·큐·재생 결과를 각각 확인 |
| EQUIP 효과 평가·종료 | 요청·작업·세션 상관값, 실제 조회한 슬롯·대상·전후 상태, 관측 시점·출처, 판정과 관측 불가 사유 | Java 결과 전송, Python 증거 평가 로그, 최종 화면·음성 응답 |

새 상태·판정·거절·종료의 첫 의미 있는 경계는 유한한 trace 범위와 예약 용량으로
보존하고, 반복 슬롯·tick 로그는 제한한다. 기존 AGENTS.md Section 21에 따라 OFF와
BOUNDARY의 관측 범위, 용량 부족·필터·sink 실패를 구분한다. 로그 실패나 진단 예산이
명령 실행·취소·성공 판정을 바꾸면 안 된다. 원문 음성·대화·전체 NBT는 추가 수집하지 않는다.
구체적인 유한 상한·경계 signature·출력 경로는 실제 구현 owner에 맞춰 구현 전에 확정한다.

### 추후 검증과 완료 기준

아래는 **수정 구현 후 수행할 검사 목록**이다. 이번 문서화에서 실행한 테스트가 아니다.

| 분야 | 재현 입력·상태 | 합격 기준 |
| --- | --- | --- |
| 표기 변형 | `챗클래프 꺼줘` → 기존 확인 | `chatclef off` 한 번, 확인 전 제출 0회; ON 재활성화와 결과 전달 유지 |
| 표기 변형 | `다이아 레겡스 장착해줘` | 실제 대상 검증 후 `equip diamond_leggings` 한 번 |
| 실패 요청 | 미등록 장비명, 모호한 이름, 잘못된 수량 | 명령 소유권이 확인된 입력에 이유·후보 안내; 임의 실행·LLM 복구 실행 0회 |
| 비실행 입력 | `다이아 레겡스 장착하지 마`, 질문·인용·일상 대화 | 자동 제출 0회, 관계없는 일반 대화 경로 유지 |
| 확인·중복 | 취소·만료·STOP, 같은 event ID 재전달, 같은 문장의 새 event ID | 취소·만료 제출 0회, 재전달 중복 실행 방지; 새 요청과 중복 이벤트 구분 |
| ID 왕복 | 위 `2c8` 포함 ID의 목록 출력과 복사·재입력 | 필터 이후 ID 원문 일치, 동일 항목 해석, 기존 등록 해제 보호 유지 |
| 필터 경계 | 정상 금칙어 문장, `td_` 유사 문자열, 위조 보호 정보, 다른 요청의 ID 혼입, 세션 교체·구조화 정보 유실 | 검증된 동일 요청의 목록 ID 이외에는 필터 예외가 생기지 않음; 화면 escaping 유지 |
| 목록 상태 | 빈 목록·복수 항목·오래된 항목·세션 교체 | 개수·상태 정확, 잘못된 대상 실행 0회, 음성 요약과 화면 내용 일치 |
| 장비 효과 | 네 방어구 슬롯, 이미 착용, 목록·세트·바닐라 방패·허용 대체품 | 모든 target 충족·target 내 대체품 허용, 실제 슬롯에 맞는 전체/부분 결과 |
| 장비 경계 | 동일 슬롯 충돌, 수량 2 이상·중복 목록, 미지원 Equipment·모드 방패 | 수량만으로 착용 수를 늘리거나 대표 아이템만 비교하지 않음; 기존 지원 한계와 미충족·미확인 결과를 구분 |
| 관측 실패 | 종료 시 불일치·관측 불가·구버전 payload·stale 결과·취소, callback만 도착·다른 Task 종료 | 미장착·미확인·취소 구분, 기존 정상 종료 조건 유지, 예전 결과가 새 요청의 성공으로 사용되지 않음 |
| 입력·전달 통합 | 모든 재현 입력을 Chat과 최종 마이크 경로에 각각 전달 | 공통 신뢰·admission 적용, 중간 인식 제출 0회, 동일 응답의 UI·TTS 중복 큐 등록 방지 |
| 표시·재생 | 목록 응답의 UI callback·화면 이력 반영, TTS 정규화·큐 등록·분할 재생·중단·실패 | 실제 화면·복사 문자열과 음성 입력을 별도 검사; 큐 등록을 재생 완료로 보고하지 않음 |
| 회귀·진단 | GET·FIND·GOTO·자동보관·영어·GUI, 로그 OFF/BOUNDARY·예산·sink 실패 | 기존 소유권·보호·STOP 우선순위 유지, 로그 조건이 동작 판정을 바꾸지 않음 |

실제 마이크 인수에서는 원음과 최종 인식문을 구분해 확인한다. 최종 인식문이 달라졌다면
그 사실을 기록하고, STT 오류와 명령 해석 오류를 구분한다. 같은 문장을 다시 말하는
새 요청과 하나의 이벤트 재전달도 구분한다. 화면 1회와 음성 1회 전달을 중복 실행으로
계산하지 않으며, 시작 안내와 최종 결과 안내도 서로 다른 lifecycle 응답이다.
응답 생성, UI 큐 등록, 화면 이력 반영, 브라우저의 실제 표시·복사, TTS 큐 등록,
전체 재생 완료는 서로 다른 검증 지점이다. 테스트가 확인한 지점까지만 결과에 기록하고,
분할 문장 일부 재생·시작 안내 재생·사용자 중단을 최종 결과 전체 재생 완료로 합치지 않는다.
문서화 단계에서는 이 입력들을 게임에 제출하거나 실제 마이크를 실행하지 않는다.

Java 변경을 구현하는 후속 요청에서는
[Fabric 1.20.1 빌드 검증 runbook](chatclef-fabric-build-verification.md)을 적용하고,
요청된 1.20.1 범위와 실제 Gradle task graph를 대조한다. 기존 전체 빌드 오류와
1.20.1 산출물 생성 성공, 새 수정의 검사 결과를 구분한다. 이전 29개 Java 테스트나
Python 결과를 새 EQUIP 관측·필터·철자 변형 수정의 합격 증거로 재사용하지 않는다.
빌드·JAR 일치·배포·실게임 효과 확인은 각각 별도로 보고한다.

### 이번 문서화 결과

- 변경 파일: 이 구현 기록 Markdown 한 개. 상단의 구현 당시 상태와 이번 후속 계획을
  구분하고, 세 문제의 근거·소유권·수정 방향·완료 기준을 추가했다.
- 문서 검수: 로그 시각·줄 번호·식별자 원문과 기존 소유자를 대조했다. UTF-8 읽기,
  후속 절의 내부 링크 3개·절 anchor, 추가 내용의 공백·코드 블록 검사를 통과했다.
  최초 문서화에서는 상단 상태 설명 외 기존 본문을 보존했고, 이번 재검수 수정은
  이 후속 절 안에서 수행한다.
- 세 문제의 코드 수정: **미구현**. 동반 로그 추가: **NOT_APPLICABLE** — 문서만 바뀌어
  실행 경계가 없으며, 위 표는 추후 구현 시 필요한 관측 계획이다.
- 수정 후 자동 테스트·Java 빌드·실게임 검증: **NOT_RUN** — 이번 요청은 문서화만이다.
- 외부 파일 변경·배포·게임/월드/캐시 조작·commit·push: **수행하지 않음**.
- 남은 일: 후속 구현 시 세 경계를 현재 소스와 대조해 적용하고 위 검사로 검증한다.
  현재 기록만으로 세 문제 해결이나 전체 마이크 지원의 실게임 검증 완료를 선언하지 않는다.

### 문서 재검수 반영 — 2026-09-15

- ID 손상의 증거를 SafetyFilter 결과·TTS 입력으로 한정하고, 실제 화면 표시·복사값과
  최종 결과 전체 재생은 미검증으로 정정했다. 목록 결과의 재생 중단 기록도 반영했다.
- 명령 TTS와 수동 합성 UI의 필터 호출 경로를 구분했다. ID 보호에 필요한 동일 요청·세션·
  목록 항목의 출처 검증과 위조·유실 검사를 구체화했다.
- EQUIP의 기존 착용 검사, 모든 target과 각 대체품의 판정, 실제 지원 장비 범위,
  정상 종료 조건을 명시하고 빠진 경계 사례를 검증 표에 추가했다.
- 철자 변형의 실제 별칭 자료와 거절 소유자를 기록했다. 축약된 로그 사유로는
  두 번째 입력의 정확한 내부 실패 분기를 특정할 수 없다는 한계를 명시했다.
- 소스·로그 대조는 읽기 전용으로 수행했으며, 수정한 파일은 이 Markdown 한 개다.
  기존 구현·테스트 결과와 후속 계획의 미구현 상태를 유지한다.

## 후속 구현 기록 — 2026-09-15

사용자가 검수된 세 후속 계획의 실제 구현·책임 분리·폴더화·관련 검증과 Java 1.20.1
빌드를 요청했다. 위 문서화 단계의 미구현/NOT_RUN은 당시 기록이며 구현 허용 범위를
제한하는 새 요청이 아니다. 외부 배포·게임 실행·commit·push는 이번에도 수행하지 않는다.

기준: HEAD `1b65f980eaf6db44ba725bb1b93dc1971d9b16bd`, branch
`minecraft-plugin-fix/alto-clef-infinite-loop`. 기존 변경과 8,463개 파일 hash는
`test/test_Isolation/korean_followup_20260915/baseline-20260915-200333-238736.json`에 기록했다.
기존 전체 한국어 구현과 기타 dirty 파일을 보존하므로 빌드는 `MIXED_PROVENANCE`다.

### 책임 배치 — 구현 전

아래 경로의 기준은 `C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\fabric\chatclef\`다.

- `result/equip/equip_effect_payload.py`: 불변 장착 효과 자료.
- `result/equip/equip_effect_payload_decoder.py`: bounded wire 자료와 슬롯/대체품 의미 검사.
- `transport/command_feedback/lifecycle/evidence/equip/equip_terminal_evidence_evaluator.py`:
  기존 요청·세션·정상 종료와 효과 자료의 결합 검증.
- `response/command_lifecycle/terminal/equip/korean_equip_terminal_renderer.py`: 검증 결과의 한국어 표현.
- `diagnostics/equip_command/equip_command_diagnostic_observer.py`: 이미 결정된 결과의 bounded 로그.
- 각 폴더의 `__init__.py`와 기존 evaluator/registry/coordinator/fact/renderer/server graph의
  위임·import·등록·테스트를 함께 갱신한다. 기존 파일 이동·이름 변경은 없다.

Java는 Fabric `bridge/command/result/effect/equip`에서 profile, slot observation,
binding, evidence, payload, diagnostics의 독립 책임을 분리한다. factory는 EQUIP tracker를
선택하고 ExecutionState는 context 전달을 보완한다. upstream EquipCommand/Task와 GOTO는 보존한다.
입력은 기존 명령군 파서·장비 별칭·거절 소유자를 확장한다. 목록은 검증된 terminal fact에서
별도 speech projection을 생성하고 기존 앱의 화면/TTS 전달 소유자에 연결한다.

### 공유 결과 연결·빌드의 GOTO 범위 및 증거

아래 ledger는 공유 결과 위임과 빌드에 포함되는 기존 GOTO의 경계를 기록한다.
GOTO의 이동 동작을 수정하거나 과거 원인을 증명하는 작업이 아니다.

```text
GOTO_INCIDENT_RECORD_READ: YES
REPRODUCTION_ID: UNKNOWN
ACTIVE_ARTIFACT_SHA256: UNKNOWN
COMMAND_CORRELATION_ID: UNKNOWN
OPERATION_ID: UNKNOWN
NAVIGATION_ROOT_CAUSE_STATUS: UNKNOWN
PARENT_TASK: UNKNOWN
CHILD_TASK: UNKNOWN
FINITE_COUNTER_OWNER: UNKNOWN
TERMINAL_DECISION_OWNER: existing FabricChatClefCommandOutcomeClassifier; GOTO runtime owner UNKNOWN
DIAGNOSTIC_BUDGET_OWNER: existing backend diagnostics; GOTO trace UNKNOWN
REQUIRED_CAUSAL_OWNER_BOUNDARY_SET: UNKNOWN for GOTO; no new navigation transition
REQUIRED_CAUSAL_OWNER_BOUNDARY_EMISSION_STATUS: UNKNOWN
REQUIRED_TERMINAL_EMISSION_STATUS: UNKNOWN
FIRST_REQUIRED_PHASE_TRANSITION_EMISSION_STATUS: UNKNOWN
DIAGNOSTIC_SUPPRESSION_REASON: UNKNOWN
PATH_OR_GOAL_OWNER: existing navigation; runtime UNKNOWN
LAST_SUCCESSFUL_BOUNDARY: UNKNOWN
FIRST_FAILING_BOUNDARY: UNKNOWN
TRIGGERING_STATE: UNKNOWN
EXPECTED_TRANSITION: existing GOTO lifecycle and projection preserved
OBSERVED_TRANSITION: UNKNOWN
RETRY_OWNER: unchanged; runtime UNKNOWN
WANDER_OWNER: unchanged; runtime UNKNOWN
EXACT_FILES_AND_HUNKS_PROPOSED: new EQUIP payload/evaluator/renderer/diagnostics and closed EQUIP delegation in existing result owners; existing GOTO delegates unchanged; Java effect factory/context additions only
EXACT_PROPOSED_FIX_BOUNDARY: Korean input rejection; trusted list speech; EQUIP effect projection; no GOTO behavior fix
FAILED_IMPLEMENTATION_ROLLBACK_SCOPE: NONE
PROPOSED_CHANGE_EXACT_ROLLBACK_UNIT: inverse only of new followup files and exact owner delegation hunks relative to baseline-20260915-200333-238736.json; retain all pre-existing dirty changes
USER_APPROVED_EXACT_ROLLBACK_UNIT: NONE
PROPOSED_CHANGE_CLASSIFICATION: NONE
ACTIVE_WORLD_IDENTITY: UNKNOWN
ACTIVE_WORLD_SAVE_PATH: UNKNOWN
WORLD_COPY_RESTORE_REPLACE_RENAME_HISTORY: UNKNOWN
BARITONE_CACHE_EVIDENCE: NOT_APPLICABLE
CACHE_TARGET_ABSOLUTE_PATH: NOT_APPLICABLE
CACHE_ACTION: NONE
TASK_STOPPED: NOT_APPLICABLE
TASK_STOPPED_EVIDENCE_STATUS: NOT_APPLICABLE
WORLD_EXITED: NOT_APPLICABLE
WORLD_EXITED_EVIDENCE_STATUS: NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED: NOT_APPLICABLE
MINECRAFT_PROCESS_CLOSED_EVIDENCE_STATUS: NOT_APPLICABLE
NOT_APPLICABLE_REASON: repository implementation/tests/build only; no runtime/world/process/cache action
ACTIVE_REQUEST_AUTHORIZES_SOURCE_EDIT: YES
ACTIVE_REQUEST_AUTHORIZES_RECOVERY_ROLLBACK: NO
ACTIVE_REQUEST_AUTHORIZES_BUILD: YES
ACTIVE_REQUEST_AUTHORIZES_DEPLOYMENT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_LAUNCH: NO
ACTIVE_REQUEST_AUTHORIZES_EXTERNAL_INSTANCE_COPY: NO
ACTIVE_REQUEST_AUTHORIZES_RUNTIME_REPRODUCTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_EXECUTION: NO
ACTIVE_REQUEST_AUTHORIZES_LIVE_WORLD_BLOCK_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_TASK_STOP: NO
ACTIVE_REQUEST_AUTHORIZES_WORLD_EXIT: NO
ACTIVE_REQUEST_AUTHORIZES_MINECRAFT_PROCESS_CONTROL: NO
ACTIVE_REQUEST_AUTHORIZES_MANUAL_CACHE_MUTATION: NO
ACTIVE_REQUEST_AUTHORIZES_COMMIT: NO
ACTIVE_REQUEST_AUTHORIZES_PUSH: NO
```

위 `NONE`은 GOTO 동작 수정의 분류다. 사용자 요청에 따른 한국어 후속 입력·출력·장착
증거 처리는 구현 범위에 포함되며, 기존 이동 알고리즘에는 새 동작을 추가하지 않았다.

### 후속 구현 내용과 책임 분리

아래 경로는 저장소 루트 기준이다. 이동·이름 변경·삭제 없이 기존 소유자의 위임을
유지하고, 이번 요청에 필요한 독립 책임을 기존 기능 폴더 아래에 분리했다.

| 변경 경계 | 구현한 책임·이유 |
| --- | --- |
| `plugins/Minecraft/fabric/chatclef/intent/grammar/control/korean_control_rule_parser.py`, `intent/resources/korean_equipment_aliases.json` | 검증된 `챗클래프`, `레겡스` 표기를 기존 제어 명칭·장비 이름 해석에 연결한다. 임의 유사도 자동 실행은 추가하지 않는다 |
| `intent/names/command_target_resolver.py`, 기존 item phrase resolver·validator | 이름을 알아들은 뒤 실제 runtime row의 명령 capability를 재검사한다. 기존 별칭 fallback도 단일 대상 capability를 우회할 수 없게 한다 |
| `input/routing/ordinary/rejection/name/`, 기존 ordinary rejection 소유자 | 신뢰된 입력의 재해석, Minecraft 명령 의도 확인, 불명확·미지원 이름 안내를 나눈다. 후보가 없을 때 정확한 이름을 다시 요청하며 질문·부정문·인용·일반 대화 경계는 보존한다 |
| `intent/chatclef_intent_dto.py`, `input/diagnostics/` | 실제 선택한 제한된 `parse_rule_id`와 거절 사유를 기존 상관관계 로그에 연결한다. 진단 필드는 실행 권한을 부여하지 않는다 |
| `response/command_lifecycle/terminal/instant/list_output/` | 목록의 표시·음성 문장 생성과 검증된 terminal fact의 speech projection을 각각 분리한다 |
| `llm_core/routed_response/presentation/routed_response_text.py`, 기존 request·payload adapter·publisher | 불변 화면/음성 텍스트를 전달한다. 기존 UI 큐·Chat history는 화면 원문, 기존 output/SafetyFilter/TTS는 음성 문장을 받는다. 일반 응답의 기본 경로는 동일하다 |
| `input/routing/trusted_korean/response/coalesced/`, `input_core/input_event/provenance/trusted_user_ingress/routed_response_deferred_selection.py` | 완료가 시작 안내보다 먼저 도착할 때 원래 발급된 START 권한을 정확한 소유자의 최종 응답에 한 번만 연결한다. backend 전용 결과 선택과 backend 중립 권한 자료를 분리한다 |
| 기존 capability issuer·consumer·ready acknowledgement | 폐기된 입력 proof를 다시 사용하지 않는다. 최종 화면·음성·응답 종류·이벤트·소유자와 취소 상태를 함께 검증하고, 선택 실패나 발급 후 delegate 교체는 원래 START 전달로 복구하지 않는다 |
| Fabric Java `bridge/command/result/effect/equip/` | profile, observation, binding, evidence, payload, diagnostics를 각각 분리한다. 외부에서 쓰는 target/match/slot record도 개별 파일에 둔다 |
| Python `result/equip/`, `transport/command_feedback/lifecycle/evidence/equip/`, `response/command_lifecycle/terminal/equip/`, `diagnostics/equip_command/` | 불변 자료·wire 해독·요청 결합 검사·한국어 안내·진단 출력의 독립 책임을 분리한다 |
| 기존 Java effect factory·ExecutionState, Python evidence registry·result coordinator·fact·renderer·server graph | 기존 실행/결과 소유자에 EQUIP 분기와 observer를 등록한다. Task 종료 판정과 실제 착용 판정을 구분한다 |

표의 Minecraft Python 축약 경로는 `plugins/Minecraft/fabric/chatclef/` 아래다.
import/export, 기존 서버 구성, 응답 publication, 테스트 fixture와 focused Gradle source list를
함께 갱신했다. 기존 facade 경로·설정 키·명령 실행기·영어 문법은 유지한다. 새 Forge 구현,
공통 Java bridge, 의존성·버전 변경, Task·Baritone·이동·채굴·전투·보관 알고리즘 변경은 없다.

#### 보관 장소 ID와 음성

- `td_be0062c83f14cb8bd68b64b9`를 포함한 검증된 목록의 ID는 UI 큐와 Chat history 원문에
  정확히 남는다. 복사·재입력할 대상 문자열을 기존 `auto_deposit_untrust` 문법으로 연결한다.
- 음성은 번호·차원·좌표·기록된 상태로 안내한다. 원본 ID를 금칙어 예외로 등록하거나
  사용자/LLM의 `td_...` 문자열을 보호하는 범용 예외는 만들지 않았다.
- 기존 SafetyFilter는 그대로 적용한다. 긴 목록은 완전한 행 단위로 음성 길이를 제한하고,
  전체 개수·음성 안내 개수·나머지 화면 확인 문구를 제공한다. 화면 목록의 64개 항목은 보존한다.
- `UNKNOWN_OR_STALE`은 현재 상태 미확인으로 표현한다. 유효한 ID의 등록 해제 권한을
  이 상태만으로 새로 제한하지 않는다.
- 즉시 완료의 기존 START 권한 불일치는 실제 publication 경로를 사용한 테스트에서
  재현되어 함께 수정했다. 빠른 완료를 별도 실행기나 우회 publisher로 보내지 않는다.

#### 장착 결과의 의미

기존 Java `EquipArmorTask` 실행 전·정상 종료 후에 클라이언트 스레드에서 head/chest/legs/
feet/offhand를 읽는다. 같은 world/player와 request/session/connection/task 결합을 확인해
`equip_armor_slots_v1` payload를 기존 bridge 결과에 첨부한다. Python은 schema·정확한
bool/int·대상/수량·대체품·슬롯·시간 순서·callback/task 종료 증거를 다시 검사한다.

| 관측 | 안내 원칙 |
| --- | --- |
| 모든 요청 대상이 종료 후 슬롯에 있음 | `다이아 바지 착용을 확인했어.` |
| 실행 전·후 모두 충족하고 슬롯 snapshot도 동일 | `다이아 바지는 이미 착용하고 있어.` |
| 여러 대상 중 일부만 충족 | 전체 대상 수와 확인된 대상 수를 안내 |
| 읽은 슬롯에 요청 장비가 없음 | 작업 종료와 미착용을 함께 안내 |
| 이전 JAR의 payload 없음, 읽기 실패, 잘못된 결합·자료 | 기존 `실제로 장착됐는지는 확인하지 못했어` 안내 유지 |
| 취소·실패 또는 늦게 도착한 다른 요청의 결과 | 기존 terminal 사유·소유권·중복 차단 유지 |

목록은 **모든 대상 AND / 한 대상의 허용 대체품 ANY**다. Java ItemList가 동일 native
token의 수량을 합치고 입력과 다른 순서로 반환하는 것도 검증한다. 수량을 착용 개수나
획득 성공으로 해석하지 않는다. 재료 세트는 실제 네 부위로 확장하여 검사한다. 기존 명령이
지원하지 않는 장비를 새로 장착하거나, 장비 획득·착용 동작을 교체하지 않았다.

#### 직접 확인할 문장

Chat과 마이크 최종 인식에 같은 문장을 사용한다. 아래 표는 저장소 내 입력·결과 테스트의
기대이며, 실제 게임 실행·마이크 하드웨어 검증을 완료했다는 뜻은 아니다.

| 입력 | 기대 |
| --- | --- |
| `챗클래프 꺼줘` → `확인` | 기존 확인을 거친 뒤 `chatclef off` 한 번 제출 |
| `챗 클래프 켜 줘` → `확인` | 기존 확인을 거친 뒤 `chatclef on` 한 번 제출 |
| 위 확인 대기 중 `취소` 또는 `멈춰줘` | 대기 취소; 뒤늦은 `확인`으로 원 요청 실행하지 않음 |
| `다이아 레겡스 장착해줘` | `equip diamond_leggings`; 실제 슬롯 증거에 따라 안내 |
| `다이아 투구 한 개와 다이아 바지 한 개 장착해 줘` | `equip [diamond_helmet 1, diamond_leggings 1]`; 두 대상 모두 확인해야 착용 확인 |
| `다이아 방어구 세트 장착해 줘` | `equip diamond`; 네 부위 검사 |
| `자동 보관 장소 목록 보여 줘` | 화면 원본 ID, 음성 번호·좌표·상태; 빠른 결과도 한 번 전달 |
| `자동 보관 장소 td_be0062c83f14cb8bd68b64b9 등록 해제해 줘` | 해당 ID가 실제 목록에 있는 테스트에서 같은 ID의 `auto_deposit_untrust`로 연결 |

직접 등록 해제를 시험할 때는 자신의 실제 목록에 나온 테스트용 장소 ID를 사용한다.
마이크 중간 인식은 실행 대상이 아니다. 수량 없는 `다이아 투구와 다이아 바지 장착해 줘`의
붙은 `와` 목록 문법은 기존 제한으로 남는다. 각 수량을 명시하거나 `및`으로 연결한 문장을
사용한다. 이는 이번 철자 변형·결과 전달 수정과 별개인 목록 문법 확장 범위다.

### 후속 검증 기록

후속 세 부분의 구현과 저장소 검증을 완료했다. 최종 Python 전체 검사는
**2,591 passed / 15,910 subtests passed / 2 skipped / 1 failed**(114.96초)다.
Java와 실게임의 증거 수준은 아래처럼 구분한다.

#### Python·정적 검사와 변경 목록

- [최종 전체 로그](../../../test/test_Isolation/korean_followup_20260915/full-python-final-20260915-204550-606.log):
  `tests/minecraft_chatclef/`, `tests/test_minecraft_chatclef_*.py`,
  `tests/test_llm_minecraft_input_router.py`, `tests/input_core/`,
  `tests/llm_core/{routed_response,input_routing,chat_input,input_queue}/`,
  `tests/tts_core/delivery/`를 저장소 venv·전용 runner로 실행했다.
  Chat/최종 마이크, 확인·취소·STOP·busy·중복, 전체 기존 명령 행렬,
  GET/FIND/GOTO/자동보관 및 앱 입력 권한·화면·음성 전달을 포함한다.
- 남은 실패 1개는 기존 `test_java_contract_sources_have_not_drifted_at_head`의
  Dispatcher HEAD hash 단정이다. 기대 `5afbb6d9…`, 실제 committed HEAD `f89a1f6a…`가
  이미 이전 전체 검사에서도 불일치했다. 해당 Java 파일과 hash 기대값은 이번에 수정하지
  않았다. 이 실패를 제외하고 모두 통과했지만 전체 suite의 exit code는 **1**이다.
- 생략 2개는 별도 opt-in이 필요한 실제 LAVI/Minecraft runtime 검사다. 외부 게임 실행
  미허용 범위를 단위 테스트 성공으로 대체하지 않는다.
- 첫 후속 전체 실행에서 드러난 빈 `parse_rule_id` 직렬화로 인한 기존 자동보관 metadata
  모양 변경은 기본값 생략으로 고쳤다. 실제 규칙 값은 유지·검증한다. EQUIP의 의도된
  verified rollout 기대와 새 파일 날짜·tuple export 구조 검사를 맞추고 최종 전체 검사를
  다시 실행했다. 과거 날짜를 새 파일에 허위로 넣거나 기존 자동보관 기대값을 바꾸지 않았다.
- 별도 집중 증거: 장착 효과 60 tests, 목록·권한·필터·재생 receipt 57 tests,
  입력 metadata 최종 회귀 102 tests / 49 subtests. 전체 검사와 겹치는 수치이므로 합산하지 않는다.
- [Ruff](../../../test/test_Isolation/korean_followup_20260915/ruff-85-final.log): 변경 Python
  **85개 파일 PASS**(운영·테스트 83개와 검증 스크립트 2개). AST parse와 변경 production module당 최대 한 top-level class 검사,
  staged/unstaged `git diff --check` 모두 통과했다. Git의 기존 LF→CRLF 경고는 파일을
  변환했다는 뜻이 아니며, whitespace 오류 출력은 없다.
- [정확한 source/test/script 목록·전후 hash](../../../test/test_Isolation/korean_followup_20260915/final-change-inventory.json):
  **108개 경로(신규 54, 기존 54)**. 운영·테스트 104개와 빌드·검증 스크립트 4개이며,
  이 Markdown 한 개는 별도다. 변경한 input_core와
  app composition의 기존 dirty path가 없음을 초기 Git 상태와 대조했고, 추가 capability
  [원문 근거](../../../test/test_Isolation/korean_followup_20260915/list_output/cap_source_provenance.json)도 보존했다.
  일부 capability 파일의 원문 근거는 수정 전 파일 복사본이 아니라 HEAD와 해당 수정 diff다.
- [최종 정적·보존 검사](../../../test/test_Isolation/korean_followup_20260915/static-final.json):
  source inventory 이후 hash 변경 0, 초기 snapshot 8,463개 중 파일 누락 0,
  upstream `adris` 459개 Java 파일 변경 0. 파일 이동·삭제는 하지 않았다.

#### 동반 로그와 전달 범위

로그는 행동이나 성공을 선택하지 않고 소유자가 이미 결정한 값을 관측한다. 별도 전역
진단 상태·재시도·무제한 tick 로그를 추가하지 않았다.

| 경계/소유자 | 실제 출력과 제한 |
| --- | --- |
| 기존 feature admission + `MinecraftKoreanInterpretationRuleProjector` | event/source와 실제 parse rule·거절 사유; 규칙 없는 기존 입력의 metadata 형태는 동일 |
| `TrustedKoreanCoalescedResponseBinding._observe` | `command_coalesced_output_authority`: event_id, old/new kind, accepted, selected·selection_owner_changed·terminal_event_mismatch·render_failed 등 실제 사유 |
| 기존 capability 소비·publication receipt | 같은 event의 `command_feedback_delivery`, 응답 종류와 authorization_rejected·큐/출력 전달 상태; 취소와 한 번 소비를 기존 owner lock/permit 안에서 검사 |
| `InstantCommandDiagnosticObserver` | 검증된 list_total/list_validated/list_truncated; 원본 ID의 필터 예외를 로그에서 생성하지 않음 |
| `RoutedResponseTextProjectionFormatter` | `routed_response_text_projection`: event/kind, display_chars/speech_chars, 기존 필터 정책; ID·발화 전문을 추가로 로깅하지 않음 |
| Java `EquipEffectDiagnostics` | 요청별 capture/terminal 최대 두 경계; request/session/server·socket generation/task, 실제 target/slot/status/reason. target 세부 정보는 8개×match 4개로 제한하고 omitted 수 기록 |
| Python `EquipCommandDiagnosticObserver` | 기존 한 번 terminal claim에서 `equip_command_evidence`: request/event/session/generation, 검증 사유·관측 outcome·대상/충족 수·관측 시각 |

실제 formatter와 메모리/stdout/FileHandler sink를 통해 상관관계가 포함된 출력이 기록되는
것을 검사했다. coalesced 선택 로그의 같은 event 한 번 기록, Java terminal 재직렬화 시
관측·로그 반복 없음, sink 예외에도 결과·출력 정책이 바뀌지 않는 회귀가 통과했다.
UI queue·TTS queue와 모든 음성 조각의 playback receipt는 별도 이벤트이며, 한 조각의
재생 성공이나 큐 등록을 전체 재생 완료로 취급하지 않는다.

#### Java 1.20.1 산출물

사용자 요청에 따라 `.ps1`을 새 visible PowerShell 창에서 실행했다. 실행 파일은
`test/test_Isolation/korean_followup_20260915/build-1.20.1.ps1`이고 기존 build recorder를
호출한다. user scope인 **1.20.1만** 컴파일하도록 아래 그래프를 사용했다.

```powershell
.\gradlew.bat :1.20.1:clean :1.20.1:allCommandsFocusedTests :1.20.1:remapJar :1.20.1:validateAccessWidener --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline --init-script .gradle/codex-build-init.gradle --init-script src/test/autoDeposit/auto-deposit-tests.init.gradle --init-script src/test/allCommands/all-commands-tests.init.gradle
```

실제 추가 `-P` 인자, JDK/Gradle JVM, source hash, 전체 로그는
[최종 결과](../../../test/test_Isolation/korean_followup_20260915/20260915-203626-543-TargetCleanArtifact/result.json)와
[build log](../../../test/test_Isolation/korean_followup_20260915/20260915-203626-543-TargetCleanArtifact/build.log)에 있다.

- 최종 실행: **exit 0 / BUILD SUCCESSFUL / 52 tests successful / 0 failed·skipped**.
  36 tasks 모두 실행했다. production/test의 Java 컴파일 대상은 1.20.1이며, 상위 버전은
  기존 source preprocess chain만 통과한다.
- 첫 실행의 생성자 lambda overload 모호성, 두 번째의 package-private 테스트 seam 접근
  오류를 수정한 뒤 focused 재검증과 최종 clean 그래프를 통과했다. production public API를
  넓히지 않고 동일 package의 테스트 전용 fixture로 접근을 해결했다.
- 이 저장소의 `:1.20.1:build`도 기존 default test 의존성을 통해 1.21.1 컴파일에 도달한
  이전 증거가 있어, 사용자 버전 제한에 맞춘 위 그래프를 사용했다. **canonical 다중 버전
  `clean build` 성공이나 모든 버전 호환성 검증으로 보고하지 않는다.**
- 산출물: `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`.
  **8,991,315 bytes**, 생성 시각 2026-09-15 20:38:38 KST.
  SHA-256: `6D5601C2607F4C8523105F78CDE685A5068A846DFA50307BA067D0DBE90EB70E`.
- `lavi/adris` 프로젝트 클래스 2,339개 모두 major 61(Java 17), 신규 EQUIP 11개 포함,
  폐기된 nested record/test class 없음. Mixin 선언 42개 class와 refmap 포함, ZIP 중복 없음.
  기존 Jackson multi-release 라이브러리에는 Java 19 클래스가 있으므로 JAR 전체가 모두
  major 61이라는 표현은 사용하지 않는다. Access Widener 미선언으로 해당 task는 `NO-SOURCE`다.
- 최종 build input 2,501개 재해시 불일치 0, 당시 source 추가·누락 0. 기존 전체 한국어 구현과
  dirty source가 포함된 `MIXED_PROVENANCE` 산출물이다.
- 실제 Java result factory → EQUIP effect → Gson JSON을 Python DTO/evaluator/한국어 renderer로
  읽은 [양쪽 입력 source 검증](../../../test/test_Isolation/korean_followup_20260915/20260915-203626-543-TargetCleanArtifact/python-equip-parity.json)이 통과했다.
  이것은 headless 테스트의 합성 슬롯 fixture이며 실게임 착용 증거가 아니다.

#### 실게임·출력 증거의 한계

코드/단위·통합 테스트, JAR 생성, 외부 배포, 실게임 확인은 별개다. 이번 후속의 외부 배포,
Minecraft 실행, 실월드/캐시 변경, commit/push는 **NOT_RUN**이다. 이전 로그의 장착 사진과
기존 배포 JAR을 새 코드의 성공 증거로 사용하지 않는다.

UI 큐 등록·Chat history 문자열, TTS 큐 등록, 모든 음성 조각의 재생 완료/중단 receipt는
저장소 테스트에서 구분한다. 실제 브라우저 렌더·OS clipboard 복사·마이크 인식·스피커 전체
재생과 실게임 슬롯은 아직 검증하지 않았다. 새 JAR을 외부에 배포하지 않았으므로 현재 켜진
게임이 새 장착 증거를 보내고 있다고 주장하지 않는다.
