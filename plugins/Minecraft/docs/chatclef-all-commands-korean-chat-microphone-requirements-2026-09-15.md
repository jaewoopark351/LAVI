<!-- 20260915_kpopmodder: Record all-command Korean Chat/final-microphone requirements, current-source grammar gaps, and acceptance without implementing or running code. -->

# 전체 ChatClef 명령의 한국어 Chat·마이크 실행 요구사항

작성일: 2026-09-15

대상: Fabric ChatClef 1.20.1

상태: **요구사항 문서화 완료 / 이번 작업에서 구현·테스트·빌드 미실행**

## 1. 문서 목적과 현재 작업 범위

목표는 등록된 모든 기존 명령을 한국어로 해석하고, LAVI Chat과 신뢰된 마이크
최종 인식에서 기존 실행 경로를 거쳐 실제 상태·결과까지 전달하는 것이다.
명령 동사뿐 아니라 아이템·몹·블록·지원 구조물·설정값 등 번역 가능한 인자를
포함한다. 영어 명령을 외우거나 `@`를 발음할 필요가 없어야 한다.

이번 사용자의 실행 지시는 **문서화**이다. 제공 문서 안의 “실제 코드를 구현해줘”는
문서에 보존할 구현 요구사항이며, 이번 턴의 코드 변경·검증 실행 권한으로 적용하지
않는다. 이 문서를 읽는 것만으로 구현이나 게임 동작이 시작되지 않는다.
이후 사용자가 전체 구현을 요청하면 그 요청 범위의 구현·동반 로그·관련 테스트·
사용법 정리까지 수행해야 한다. 과거 문서의 단계 구분을 근거로 일부 별칭이나
플래그만 추가하거나 다시 문서 작성으로 축소해서는 안 된다.

| 작업 | 이번 문서화 범위 |
| --- | --- |
| 관련 문서·소스·Git 상태 읽기, Markdown 작성·연결·정합성 검사 | 포함 |
| Python/Java/bridge/DTO/리소스/설정/테스트 코드 변경 | 포함하지 않음 |
| 애플리케이션 import·실행, 기능 테스트, Java/Gradle 빌드 | 실행하지 않음 |
| 외부 인스턴스 배포, Minecraft 실행, 실월드 변경, 프로세스 제어 | 실행하지 않음 |
| rollback, 캐시 수동 변경, commit, push | 실행하지 않음 |

원문: 사용자 제공 `LAVI_all_commands_korean_Codex_request.md`와 대화의 보충 요구사항.
원문 파일 SHA-256:
`A94331885F8FFA3691430351A77C24ABDF851C6630FA25E93CD95CEC6CE72DBB`.
원문 파일은 수정하지 않는다.

## 2. 기존 문서와 책임 경계

| 문서 | 계속 재사용할 책임 |
| --- | --- |
| [AGENTS.md](../../../AGENTS.md) | 최소 완결 변경, 안전·소유권·로그·검증·행동 권한 |
| [Workstream C](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md#7-workstream-c--korean-input-for-every-registered-command) | 명령별 한국어 노출, 확인, 공통 실행 경로, 지원율의 구분 |
| [한국어 명령 레지스트리 계획](chatclef-python-korean-command-registry-plan.md) | 등록 집합, 인자·대상, lifecycle, 안전 등급, 확인, 출처, readiness |
| [아이템 동작·별칭 V2 계획](chatclef-korean-item-action-alias-v2-plan.md) | 아이템 이름, 표시 이름, 모호성, 명령별 capability |
| [한국어 테스트 전략](chatclef-korean-test-strategy.md) | 검증 계층, 전체 데이터 검사, 회귀, 증거와 실게임 한계 |
| [명령 lifecycle·스레딩](chatclef-command-lifecycle-and-threading.md) | active ownership, 결과 상관관계, 스레드와 종료 책임 |
| [Fabric bridge 프로토콜](fabric-chatclef-bridge-protocol-v1.md) | 기존 전송·메시지·DTO 계약 |
| [Backend separation](minecraft-backend-separation.md) | Fabric/Forge 구현·상태·설정·진단·테스트·패키징 분리 |
| [ChatClef / Carry On 통합 방향](chatclef-carryon-integration-direction.md) | upstream 엔진 보존, 기존 Task/입력/정리 소유권 |
| [Fabric 빌드 검증 runbook](chatclef-fabric-build-verification.md) | clean 강제 빌드, JDK·입력·JAR 식별, 배포·런타임 증거 분리 |
| [현재 FIND 이름·결과 경계](chatclef-find-python-names-2026-09-15.md) | Python 이름 해석, Java registry 검증, 현재 report/approach와 결과 전달 |

이 문서는 최신 전체 명령 한국어화 요구사항과 읽기 전용 소스 확인을 연결한다.
기존 문서가 소유한 안전·프로토콜·테스트 계약을 별도 체계로 복제하지 않는다.
과거 20/22/26개 명령 수, “FIND 미구현”, “접근 비활성”은 해당 날짜의 기록이다.
현재 FIND/GOTO를 되돌리거나 FIND 등록·접근을 다시 비활성화하는 근거가 아니다.
반대로 현재 FIND가 존재한다는 사실도 모든 명령의 한국어 실행 완료를 뜻하지 않는다.

이전 Python-only 설계는 이번 목표에 꼭 필요한 최소 Java/bridge 메타데이터·인자
어댑터·결과 연결을 영구 금지하는 근거가 아니다. 향후 실제 수정은 기존 owner의
계약으로 해결할 수 없는 구체적 공백을 확인한 경우에만 최소 범위로 한다.
한국어화와 무관한 Task·Baritone·이동·채굴·전투·보관 알고리즘 및 의존성·버전은
변경 대상이 아니다. Forge/MineMind 구현이나 placeholder도 만들지 않는다.

## 3. 읽기 전용으로 확인한 현재 기준

```text
REPOSITORY: C:\Vtuber_Souorce_Code\LAVI
BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_HEAD: 1b65f980eaf6db44ba725bb1b93dc1971d9b16bd
INITIAL_DOCUMENTATION_WORKTREE: CLEAN
REVIEW_START_WORKTREE: SIX_MARKDOWN_CHANGES_FROM_DOCUMENTATION
EVIDENCE: SOURCE_READ_ONLY
IMPLEMENTATION_ACTION: NONE
TEST_EXECUTION: NOT_RUN
BUILD_EXECUTION: NOT_RUN
DEPLOYMENT: NOT_RUN
LIVE_RUNTIME: NOT_RUN
```

### 3.1 명령 수와 준비 상태

근거: [KoreanChatClefCommandRegistry](../fabric/chatclef/command_registry/korean_command_registry.py)의
`_REGISTERED_COMMANDS`, `_PUBLIC_KOREAN_COMMANDS`, `_PARSER_READY_COMMANDS`,
`_PYTHON_ADMISSION_READY_COMMANDS`, `_BRIDGE_LIFECYCLE_READY_COMMANDS`.

| 소스상의 집합 | 수 | 해석 |
| --- | ---: | --- |
| 등록 이름 | 27 | 정규 명령 26개 + `자동보관등록` 호환 이름 1개 |
| 의미상 명령 | 26 | 사용자 지정 25개 + 기존 FIND; 호환 이름 중복 계산 금지 |
| public Korean 메타데이터 | 11 | 공개 플래그이며 실제 Chat·마이크 실행 성공 수가 아님 |
| parser-ready 메타데이터 | 13 | 위 11개에 `follow`, `idle` 추가 |
| admission-ready / bridge-lifecycle-ready 메타데이터 | 각각 11 | 파서 준비와 별도 축 |
| 이번 작업에서 Chat·마이크 전체 경로를 실행 검증한 수 | 0 | 문서화 범위이므로 `NOT_RUN`; 지원 불가 수라는 뜻은 아님 |

공개 메타데이터 11개는 `auto_deposit_trust`, `deposit`, `equip`, `find`, `food`,
`get`, `give`, `goto`, `meat`, `stop`, `store_home`이다.
`follow`·`idle`은 파서 준비 표시는 있으나 공개·admission 집합에는 없다.
R3/R4 행의 `direct_typed_only` 및 확인 계약을 한국어 확인 구현 없이 단순 제거하지 않는다.

Java 등록 근거는 다음을 함께 조사한 소스 집합이다. 실제 실행 중인 클라이언트의
활성 등록 상태를 관찰한 것은 아니며, 향후 구현 시 activation 조건도 재확인한다.

- [AltoClefCommands](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClefCommands.java)
- [AutoDepositTrustedCommandRegistrar](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/deposit/auto/trusted/command/AutoDepositTrustedCommandRegistrar.java)
- [StoreHomeCommandRegistrar](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/container/home/command/StoreHomeCommandRegistrar.java)
- [OverlayCommandRegistrar](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/overlay/command/OverlayCommandRegistrar.java)

`inventory`, `list`, `status`, `pause`, `unpause`, `stash`, `cycle`, `dummy` 등의
클래스 존재만으로 등록 명령에 포함하지 않는다. 현재 활성 등록문이 없는 클래스나
주석 처리된 등록을 자동으로 활성화하는 요청이 아니다. LAVI의 기존 문맥 상태 질문
경로도 별도 Java `@status` 등록과 혼동하지 않는다.

### 3.2 실제 문법과 메타데이터의 확인된 차이

- `gamer`: Python 슬롯에는 `target`이 있으나 Java 명령은 무인자이다.
- `get`·`equip`·`deposit`·`deposit_all`: 단일 슬롯만 보지 말고 Java `ItemList`의
  단일/목록 문법과 기본 수량을 조사해야 한다. EQUIP에는 재질별 방어구 세트 형태도 있다.
- `give`: 명시 상대는 **`give Alex iron_ingot 3`처럼 세 인자를 모두 출력**해야 한다.
  `ArgParser`의 username 기본값 조건 때문에 `give Alex iron_ingot`을
  “상대 지정 + 기본 수량”으로 해석하면 안 된다. 원래 한두 인자 형태는
  `give item [count]`이며 현재 butler 사용자를 요구한다.
- `follow`의 생략 가능한 사용자, `gamma`의 기본값, `scan`의 기본 블록,
  GOTO의 여러 좌표/차원 형태가 단순 Python 슬롯 표만으로는 충분히 표현되지 않는다.
- `SetGammaCommand.call()`은 설정 변경 후 `finish()` 호출이 보이지 않는다.
  즉시 명령의 결과·active 해제 검증 대상으로 기록한다. 현재 busy 잔류가 실제로
  재현됐다는 주장이나 이 메서드만으로 원인을 확정한 주장은 아니다.

공통 문법 근거: [ItemList](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/ItemList.java),
[ArgParser](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/ArgParser.java),
[GotoTarget](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commandsystem/GotoTarget.java),
[명령 구현 디렉터리](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/).
이 확인은 문법·기존 호출 경계 읽기이며 GOTO 이동 동작 평가나 변경 제안이 아니다.

## 4. 전체 명령 요구사항 표

한국어 문장은 **구현·인수검증 목표 예시**이며, 현재 모두 동작한다는 사용법 보증이
아니다. 모든 행의 목표 출처는 **Chat + 마이크 최종 인식**이다. 두 출처의 실제
실행·응답 검증은 이번 문서화에서 모두 `NOT_RUN`이다.

`공개`는 현재 Python public 메타데이터만 뜻한다. `확인`은 현재 R3/R4 정책을
한국어 요청·확인 경로로 연결해야 함을 뜻하며, 나머지 행도 기존 명령별 보호와
명시 인자 정책을 유지한다. 네이티브 문법의 생략 가능성이 한국어 입력에서의
암묵적 보관·대상 추정을 허용하지 않는다.

표의 `[count=1]` 같은 표기는 생략 가능한 인자와 기본값을 뜻하며 그대로 출력하는
문자가 아니다. 별도로 `ItemList`의 목록을 감싸는 `[]`와 항목 사이 쉼표는 실제
입력 문자이다. 예를 들어 `get iron_ingot 2`는 단일 형태이고,
`get [iron_ingot 2, gold_ingot 3]`는 목록 형태이다. 각 항목의 생략 수량은 1이다.
기존 목록 파서는 같은 항목의 수량을 합산하므로, 반복 목록 항목과 잘못된 중복
수량 표현을 구분하고 개별 수량뿐 아니라 합산 결과의 범위도 검증한다.
모든 이름을 무조건 `namespace:id`로 출력하지 말고 해당 Java 명령이 실제
소비하는 토큰으로 검증·변환한다.

| 명령 | 한국어 목표 예시 | 읽은 Java 문법·보존할 의미 | 공개 | 확인 |
| --- | --- | --- | :---: | :---: |
| `attack` | 좀비 세 마리 공격해 줘 | `attack name [count=1]`; 수량은 처치 목표 수이며 타격 횟수가 아님; entity untranslated name/플레이어 비교, 7.1절 플레이어 대상 보호 | 아니오 | R3 |
| `auto_deposit_trust` | 여기를 자동 보관 장소로 등록해 줘 / 주변 16x16 자동 보관 장소 등록해 줘 | 무인자 단일 대상 또는 `area 16x16` / `반경 16x16`; 두 의미 분리 | 예 | 기존 R2 |
| `auto_deposit_trusted_list` | 자동 보관 장소 목록 보여 줘 | 무인자, 신뢰 목적지 읽기 전용 목록 | 아니오 | 기존 R0 |
| `auto_deposit_untrust` | 자동 보관 장소 `<등록 ID>` 등록 해제해 줘 | `[destinationId]`; 생략은 기존 열린/조준 대상, 지정은 stable ID | 아니오 | 기존 R2 |
| `chatclef` | 챗클레프 켜 줘 / 꺼 줘 | `on` / `off` 필수; 종료 결과와 사용자 재활성화 경로 | 아니오 | R4 |
| `deposit` | 철괴 16개 보관해 줘 | 선택 `ItemList`는 보관할 대상·수량; 무인자는 기존 비장착/비도구 선택 경로 | 예 | 기존 R2·명시 슬롯 |
| `deposit_all` | 인벤토리 전부 보관해 줘 | 선택 `ItemList`도 보관할 대상·수량이며 제외 목록이 아님; 무인자는 기존 inventory selector | 아니오 | 기존 R2 |
| `equip` | 다이아몬드 흉갑 장착해 줘 / 철 방어구 세트 장착해 줘 | `ItemList` 또는 단독 `leather/iron/gold/diamond/netherite`; 명령 인자 검사와 실제 장착 Task capability를 각각 확인 | 예 | 기존 R1 |
| `find` | 철 골렘 찾아 줘 / 철 골렘 위치만 알려 줘 | `[auto/entity/block/item/player] target [approach/report]`; 기본 auto/approach, 기존 canonical은 종류·ID·모드 명시 | 예 | 기존 R1 |
| `follow` | Alex 따라가 줘 | `[username]`; 생략은 기존 butler 사용자 연결이 있을 때만 | 아니오 | R3 |
| `food` | 음식 10포인트만큼 모아 줘 | 정수 `count` 필수; food score 단위, 기존 보유 food score를 더하는 의미 | 예 | 기존 R1 |
| `gamer` | 마인크래프트 엔딩까지 진행해 줘 | 무인자 `gamer`; 기존 `BeatMinecraftTask` | 아니오 | R3 |
| `gamma` | 밝기를 1.5로 설정해 줘 | `[value=1.0]`; native 범위 제한은 확인되지 않음, 유한값·허용 범위와 종료 연결 확인 | 아니오 | 기존 R0 |
| `get` | 철괴 16개 구해 줘 / 다이아몬드 곡괭이 하나 만들어 줘 | `ItemList`; 기존 획득 지원과 보유량을 더하는 의미 보존 | 예 | 기존 R1 |
| `give` | Alex에게 철괴 세 개 줘 | 명시 상대는 `player item count`; 생략 상대는 `item [count=1]`, 검증된 butler 연결 필요 | 예 | 기존 R2·명시 슬롯 |
| `goto` | 500, 90, -928 좌표로 가 줘 | `x y z [dimension]`, `x z [dimension]`, `y [dimension]`, `dimension`; overworld/nether/end 문법 대조 | 예 | 기존 R1 |
| `hero` | 적대 몹 계속 정리해 줘 | 무인자, 기존 지속 전투와 STOP·방어 정책 | 아니오 | R3 |
| `idle` | 가만히 있어 줘 | 무인자, 기존 대기·STOP·방어 정책 | 아니오 | R3 |
| `locate_structure` | 엔드 요새 찾아가 줘 / 사막 사원 찾아가 줘 | 현재 `stronghold` / `desert_temple`만 지원; 모든 구조물 탐색으로 표시 금지 | 아니오 | 기존 R1 |
| `meat` | 고기 10포인트만큼 모아 줘 | 정수 `count` 필수; 현재 인벤토리 전체 food score를 더한 목표를 Task에 전달, 고기 개수 아님 | 예 | 기존 R1 |
| `overlay` | 오버레이 켜 줘 / 꺼 줘 | `on` / `off`; UI 설정과 즉시 결과 | 아니오 | 기존 R0 |
| `reload_settings` | 마인크래프트 설정 다시 불러와 줘 | 무인자; 기존 설정·butler whitelist/blacklist 재로딩 범위 | 아니오 | R4 |
| `resetmemory` | 챗클레프 대화 기록 초기화해 줘 | 무인자; ChatClef AI 대화 기록만, LAVI의 다른 기억·파일 제외 | 아니오 | R4 |
| `scan` | 다이아몬드 원석 블록 위치 스캔해 줘 | `[block=DIRT]`; `Blocks` 필드명 대소문자 무시 비교, 전체 mod registry 검색이 아님 | 아니오 | 기존 R0 |
| `stop` | 멈춰 줘 / 중지해 줘 | 무인자, 기존 STOP 우선 경로·terminal-only 응답 | 예 | 기존 R0 |
| `store_home` | 아이템 집에 정리해 줘 | 무인자; 기존 cursor gate, trusted home, 실제 terminal 검증 | 예 | 기존 R2 |

별도 호환 이름 `@자동보관등록 영역 16x16` / `@자동보관등록 반경 16x16`은
일괄 등록 전용이다. 무인자 단일 등록 별칭으로 취급하지 않는다. 이 raw 별칭의
parser는 영어 `area` 토큰을 받지 않는다. 한국어 Chat·마이크에서는 같은 의미를
`auto_deposit_trust area 16x16`으로 정규화할 수 있지만, 기존 raw 문법을 변경하거나
별칭을 별도 기능으로 세어 지원율을 늘리지 않는다.

## 5. 전체 대상 이름과 명령 capability

### 5.1 공통 이름 데이터와 슬롯별 검증

1. 실제 ITEM/BLOCK/ENTITY_TYPE 등록 정보, translation key, Minecraft 1.20.1
   한국어 언어 데이터의 관계로 이름을 처리한다. 언어 파일만 보고 존재하지 않는
   ID를 만들지 않는다. 구조물·설정값은 해당 명령의 실제 지원 집합으로 한정한다.
2. 공통 이름 데이터는 재사용하되 아이템·장비·몹·플레이어·블록·구조물·설정·
   목적지 ID 슬롯의 타입·지원 여부는 해당 명령이 소유한다.
3. GET의 TaskCatalogue/CataloguedResources는 획득 지원 자료이다. Python에서
   이를 모든 명령의 공통 이름 허용 목록으로 강제하지 않는다.
4. **현재 Java `ItemList` 자체의 TaskCatalogue 검사도 별도로 기록한다.**
   GET·일반 EQUIP·명시 목록 DEPOSIT/DEPOSIT_ALL은 이 native 제한을 공유한다.
   GIVE에는 카탈로그 외 inventory 항목을 찾는 경로가 있다. 영어도 못 하는 작업은
   기존 기능 한계이며, 영어가 가능한데 한국어만 막히는 경우는 한국어 경로 누락이다.
   이 구분 없이 새 획득·장착·보관 알고리즘을 만들거나 제한을 없애지 않는다.
5. ATTACK의 untranslated name, SCAN의 `Blocks` 필드명, FIND의 종류별 registry ID,
   LOCATE_STRUCTURE의 enum, GIVE의 inventory 경로 등 실제 소비 문법에 맞춘다.
   특히 GIVE의 카탈로그 외 inventory 비교는 `ItemHelper.stripItemName()` 결과와
   정확히 일치해야 한다. 이 함수는 `item.minecraft.` / `block.minecraft.`만
   제거하므로 모드 아이템은 전체 translation key가 토큰일 수 있다.
   registry ID와 실제 명령 토큰을 별도로 검증한다.
6. “이름 해석 성공”, “그 명령이 지원하는 대상”, “요청 접수”, “실제 효과·완료”를
   별도로 기록한다. 이름을 이해했지만 획득법이 없으면 이름 인식 실패나 성공으로
   위장하지 않고 기존 기능 한계로 안내한다.
7. EQUIP의 `Equipment` 검사 통과만으로 실제 장착 지원을 확정하지 않는다.
   현재 `EquipArmorTask`는 방패를 별도 처리하고 나머지 대상의 첫 match를
   `ArmorItem`으로 캐스팅한다. 명령 인자 수용과 Task의 실제 지원·미검증 범위를
   구분하고, 한국어화 요청을 기존 장착 알고리즘 확장으로 바꾸지 않는다.

위의 세부 근거는 [GiveCommand](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GiveCommand.java),
[ItemHelper](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/util/helpers/ItemHelper.java),
[EquipCommand](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/EquipCommand.java),
[EquipArmorTask](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/tasks/misc/EquipArmorTask.java)이다.

모드 대상은 실제 runtime 등록을 검증한 뒤 제공 한국어 번역 또는 검증된 한국어
사용자 별칭으로 접근한다. 번역·별칭이 없어 영어 ID만 가능한 대상은 한국어 지원
완료에 포함하지 않는다. 별칭은 종류·namespace·버전/데이터 출처를 구분하고,
없는 대상·충돌·잘못된 자료를 안전하게 처리한다.

공백 차이와 승인된 흔한 별칭은 처리하되 위험한 근사 매칭으로 대상을 바꾸지 않는다.
동명이면 종류·namespace 등으로 구분할 수 있게 안내하고 선택 전에는 제출하지 않는다.
플레이어 이름·등록 ID는 원문 대소문자와 식별자를 보존한다. “나 따라와”의 `나`는
실제 사용자와 게임 플레이어의 확인된 연결이 있을 때만 해석한다.

로컬 정적 자료만으로 runtime 전체 대상을 알 수 없다면 필요한 최소 읽기 전용
registry/capability 메타데이터를 기존 Fabric bridge 경계로 전달한다. 연결 세션과
자료 출처·버전·유효성을 확인하고, disconnect/reload 이후 오래된 자료로 검증하지
않는다. 새 연결 프레임워크나 외부 인스턴스 파일 변경은 만들지 않는다.

### 5.2 지원율과 누락 보고

각 명령·대상 종류·동일한 runtime 자료 snapshot별로 아래 값을 따로 집계한다.

| 항목 | 집계 의미 |
| --- | --- |
| 전체 등록 대상 수 | 실제 해당 종류 registry/지원 enum에서 확인한 고유 대상 |
| 한국어 번역으로 해석 가능한 수 | 검증된 ID와 연결되고 모호하지 않은 한국어 이름 |
| 사용자 별칭으로 추가 해석 가능한 수 | 번역 수와 중복하지 않는 검증된 한국어 별칭 대상 |
| 번역 누락·모호성·잘못된 ID 수 | 원인과 충돌 후보를 별도로 기록 |
| 명령의 실제 지원 대상 수 | native capability 제한을 적용한 집합 |
| 한국어 전체 경로로 연결된 지원 대상 수 | 이름·검증·admission·제출·결과 연결을 통과한 집합 |
| 영어 ID만 가능한 수 | 별도 호환 지표; 한국어 지원 분자에 넣지 않음 |

현재 문서화에서는 runtime 전체 아이템·모드 대상 수 및 실행 지원 수를
`UNKNOWN`으로 둔다. 수집하지 않은 수를 0으로 대신하거나 샘플 테스트 수를 전체
대상 수로 쓰지 않는다. 번역 자료만 있는 ID도 분자에 넣지 않는다.
누락은 **원래 기능 한계 / 한국어 구현 누락 / 자료 누락 / 모호성 / 미검증**으로 구분한다.

## 6. Chat·마이크의 공통 실행 계약

```text
신뢰된 LAVI Chat 또는 마이크 최종 인식
  → 기존 입력 출처·소유권 증명과 STOP/문맥 상태/확인 우선순위
  → 한국어 의도 및 명령별 인자 해석
  → 타입·이름·capability·수량·권한·필요한 확인 검증
  → 기존 admission / busy / 중복 방지
  → prefix 없는 canonical command 생성
  → 기존 Fabric bridge에 정확히 한 번 제출
  → 기존 Java 명령 실행
  → 실제 lifecycle·typed 결과 검증
  → 기존 한국어 화면 / 출력 / TTS 전달
```

기존 owner의 실제 호출 순서를 조사해 이 의미상의 계약을 연결한다. Chat과
마이크에 별도 명령 사전·별도 executor를 만들지 않는다. 출처별 신뢰 검사는
보존하고 이후 해석·검증·컴파일은 기존 공통 경로를 재사용한다.

- 마이크 중간 인식·위조 출처·허용되지 않은 GUI 문자열은 실행하지 않는다.
- 재전달된 동일 이벤트는 한 번만 처리한다. 같은 문자열의 나중 새 요청을
  영구 차단하지 않는다. 확인·응답 중복도 문자열만으로 판단하지 않는다.
- parser, `public_korean_enabled`, `allowed_input_sources`, gate, compiler,
  admission 중 한 곳만 바꿔 전체 지원이라고 하지 않는다.
- `@` prefix 책임을 보존하고 이중 prefix를 붙이지 않는다. 원문이나 LLM 출력
  문자열을 무검증 raw 명령으로 전송하지 않는다.
- 질문·설명·인용·부정문·모호한 복합 요청의 일부를 임의 실행하지 않는다.
  기존 문맥 상태 질문 경로는 보존하며 게임 명령을 새로 제출하지 않는다.
- 수량은 숫자와 승인된 한국어 형태(`한 개`, `하나`, `세 마리`)를 명령별 단위로
  해석한다. 명시된 잘못된 수량·잘못된 중복 수량 표현·범위 초과는 기본값 실행으로
  바꾸지 않는다. 목록의 동일 항목 합산도 정수 overflow 없이 검증한다.
  실제 생략일 때만 기존 한국어 안전 계약과 Java 의미에 맞는 기본값을 적용한다.
- GAMMA는 NaN/무한/overflow를 거부하고 근거 있는 유한값 범위를 정한다.
  현재 native 범위 제한이 확인되지 않았다는 이유로 임의 숫자 범위를 문서에
  사실처럼 넣지 않는다. GOTO는 기존 음수·쉼표·정수 범위를 유지한다.
- 현재 명령의 선택 인자·목록·모드·하위 문법도 대상으로 삼는다. 문법상 가능한
  형태가 실제 기능 한계나 기존 안전 정책으로 제한되면 행별로 이유를 기록한다.

canonical 예시는 `attack zombie 3`, `get iron_ingot 16`, `deposit iron_ingot 16`,
`equip diamond_chestplate`, `give Alex iron_ingot 3`, `overlay off`,
`goto 500 90 -928`, `gamer`, `stop`이다. 이들은 인자 변환·확인·admission 이후의
목표 출력이며 현재 한국어 경로의 성공 결과로 제시하는 것이 아니다.

## 7. 안전성·확인·명령 의미 보존

### 7.1 명령을 다른 동작으로 치환하지 않기

- GET / FIND / SCAN / LOCATE_STRUCTURE를 구분한다. “상자 찾아 줘”를 상자
  획득·제작으로 바꾸지 않는다. 기존 FIND report/approach 및 탐색을 보존한다.
- DEPOSIT / DEPOSIT_ALL / STORE_HOME을 구분한다. “전부”는 기존 보호 정책을
  해제하는 단어가 아니다. KEEP_LOADOUT, working-set, trusted-container,
  cursor gate, 목적지·Task 소유권을 유지한다.
- 단일 신뢰 컨테이너 등록과 고정 `area 16x16` 일괄 등록을 구분한다.
  해제는 기존 stable ID 또는 정확한 대상 선택 계약을 지킨다.
- FOOD/MEAT 수량을 아이템 개수나 배고픔 아이콘 수로 바꾸지 않는다.
- ATTACK의 플레이어 대상은 Workstream C의 기존 기본 거절 정책을 유지한다.
  별도로 승인된 정확한 플레이어 공격 계약 없이 R3 확인만으로 허용하지 않는다.
  FIND/FOLLOW의 플레이어 이름 지원도 공격 권한으로 해석하지 않는다.
- 기존 영어 `@명령`, 네이티브 채팅, 직접 명령 GUI의 문법·동작을 보존한다.
- 자동방어·생존·회피·기존 무기 선택은 해당 작업 중에도 유지한다.
  특히 자동방어가 자동보관보다 우선하는 정책을 유지한다. 대기·추적·탐색을
  이유로 방어를 끄거나 새 전투 알고리즘을 만들지 않는다.

### 7.2 한국어 확인·취소

현재 R3/R4 등 확인이 필요한 정책을 우회하지 않고 Chat과 최종 음성 각각에서
한국어 확인 후 실행할 수 있게 연결한다. 이 경로가 없으면 전체 지원 미완료이다.
기존에 확인이 불필요한 모든 명령에 새 확인을 일괄 추가하지 않는다.

확인은 정확한 출처·세션·세대·요청·정규화된 명령·대상·수량에 결합하고, 유효기간과
한 번 소비를 검증한다. 확인 전 제출은 0회이다. 확인 소비 시에도 현재 출처 신뢰·
세션·명령별 권한·대상·busy/admission을 재검증하며, 모두 통과한 요청만 1회 제출한다.
확인 대기 중 조건이 달라져 거절된 요청은 0회 제출하고 한국어로 이유를 전달한다.
취소·만료·중복 확인·무관한 발화·다른 세션/요청의 확인·마이크 중간 인식은
실행을 발생시키지 않는다. STOP은 기존 우선순위를 유지하며 pending 확인과
활성 작업의 처리 관계를 기존 소유권에 맞게 검증한다.

“공격하지 마”, “기억 초기화가 뭐야?”에서 부작용이 없어야 한다.
무검증 다중 명령 실행, 자동 체이닝·재시도·replay를 추가하지 않는다.

`chatclef off`는 LAVI 종료와 구분한다. disable 전후 한 번의 결과 전달과 기존
독립 사용자 `@chatclef on` 복구 경로를 보존하고, 한국어 on/off 진입 가능 여부도
검증한다. native 복구만 남은 상태를 한국어 전체 지원으로 계산하지 않는다.
`resetmemory`는 ChatClef 대화 기록에만 한정하며 다른 LAVI 기억·파일을 지우지 않는다.

## 8. lifecycle·한국어 피드백·동반 로그

즉시 설정·조회 명령, 일회성 Task, 지속 Task, STOP을 동일한 완료 규칙으로
처리하지 않는다. 제출/접수/시작을 실제 완료로 말하지 않는다.

| 유형 | 검증할 결과 계약 |
| --- | --- |
| 즉시 설정·조회 | authoritative 결과와 active/busy 해제를 한 번 연결; 다음 정상 명령을 막지 않음 |
| 일회성 Task | 기존 실제 완료·실패·취소 증거와 정확히 연계; 접수만으로 효과 성공 주장 금지 |
| FOLLOW/IDLE/HERO 등 지속 Task | 실행 중 상태를 유지하고 STOP·중단·종료와 연결; 시작을 완료로 표시하지 않음 |
| STOP | 기존 terminal-only 화면·음성 정책; 보이는 START 0회, 검증된 terminal 최대 1회 |

지원하지 않는 대상, 모호한 이름, 잘못된 수량, 확인 대기·취소, 접근 실패,
실제 완료를 한국어로 구분한다. 증거가 부족한 legacy 결과에는 기존 신중한 표현을
유지한다. UI/출력/TTS는 기존 이벤트 상관관계와 소비 owner로 중복을 막고
한국어 표시 이름·조사·수량·맞춤법을 검수한다.

동반 로그는 기존 backend logger를 재사용한다. 입력 출처·안전한 상관 ID,
선택 명령, 실제 검증값·거절 이유, 확인 소비, 제출·결과·종료/정리 경계를
제한된 payload로 연결한다. 원문 전체 대화·음성·API 키·불필요한 개인정보는 남기지
않는다. logger 호출과 실제 formatter/sink 출력을 구분하여 검증한다.

진단 OFF/BOUNDARY, 예산·억제·출력 실패가 행동·성공·재시도·정리·terminal을
선택하지 않아야 한다. AGENTS.md Section 21의 유한 trace 범위, 필수 최초 경계와
terminal 예약 용량, 전달 불가 시 미검증 표기를 적용한다. 같은 이벤트를 매 tick
출력하거나 무제한 flush/retry를 추가하지 않는다.

## 9. 구현 시 재사용할 owner와 작업 순서

아래는 **향후 구현 시 읽고 추적할 기존 경계**이다. 파일명만 보고 전부 수정하거나
새 manager/coordinator/프레임워크를 만드는 지시가 아니다.

| 책임 | 기존 확인 시작점 (`fabric/chatclef/` 기준) |
| --- | --- |
| 명령·readiness·확인·출처 | `command_registry/korean_command_registry.py`, `command_registry/admission/` |
| 입력 신뢰·자격·분기·제출 | `input/minecraft_chatclef_input_router.py`, `input/eligibility/`, `input/gating/`, `input/routing/` |
| 한국어 의도·typed 검증 | `intent/korean_chatclef_rule_parser.py`, `intent/chatclef_intent_type.py`, `intent/chatclef_intent_schema_validator.py`, `intent/natural_language/` |
| canonical 생성·안전 검사 | `intent/chatclef_command_compiler.py`, `intent/chatclef_command_safety.py` |
| 이름·자료·명령별 대상 | `intent/korean_item_phrase_resolver.py`, `intent/chatclef_target_catalog.py`, `intent/resources/`, `intent/navigation/find/` |
| 결과·화면·TTS | `result/`, `response/command_lifecycle/`, `response/trusted_korean/`, `transport/`의 기존 결과 소유자 |
| 필요한 Java 연동 | 실제 command/registrar, 기존 Fabric bridge의 registry/capability·인자·즉시 결과 경계 |

향후 명시적 구현 요청에서의 순서:

1. 당시 HEAD·dirty 변경·Java 활성 등록·문법·호출부를 재확인하고 사용자 변경을 보존한다.
2. 이 문서의 26개 의미상 명령 표에 실제 인자·대상·확인·lifecycle·출처별 공백을 채운다.
   gamer/ItemList/GIVE/즉시 명령처럼 메타데이터와 실제 동작이 다른 경계를 먼저 맞춘다.
3. 기존 한국어 입력·이름·검증·컴파일·admission·결과 owner에서 가장 작은 완결 단위로
   구현하고 동반 로그와 해당 테스트를 함께 완성한다. 필요한 Java 연동만 최소 보완한다.
4. 명령별 조건을 만족한 뒤 공개 readiness를 연결한다. 나머지를 영구 비공개로 두고
   전체 완료라고 하지 않으며, 사용자 요청 범위의 모든 명령까지 이어서 진행한다.
5. 전체 대상 데이터와 Chat/최종 마이크·영어 회귀를 검증하고 아래 완료 기준을 채운다.
6. Java 변경 시 runbook에 따른 clean build와 JAR 식별을 별도 기록한다.
   배포·게임 실행 등은 해당 시점의 별도 사용자 요청 범위에만 따른다.

이 문서화 자체에는 위 구현 단계의 source action이 없다. GOTO를 읽거나 언급했다는
이유로 동작 변경·복원·빌드를 수행하지 않으며, 이후 실제 변경 경계에 적용되는
AGENTS.md의 범위·증거·행동 권한을 각각 따른다.

## 10. 필수 검증과 완료 기준

### 10.1 명령별 전체 경로 검사

각 명령을 **Chat / 마이크 최종** 두 행으로 나누고 다음 열을 갖는 지원·검증 표를
완성한다. 영어 native/직접 GUI 호환도 별도 회귀 축으로 둔다.

```text
command / semantic_id / source / Korean input / resolved kind and target
Java grammar and defaults / canonical command / safety and confirmation
parser / source trust / admission / submitted count / lifecycle identity
result evidence / UI count / output count / TTS count / active-owner cleanup
test environment / actual outcome / evidence location / remaining limitation
```

| 검사군 | 최소 인수 조건 |
| --- | --- |
| 정상·선택·목록·모드 | 실제 Java 문법과 일치, 생략 기본값·모든 지원 하위 형태·동일 목록 항목 합산, 승인된 요청 제출 1회 |
| 잘못된 입력 | 개별/합산 수량 오류·overflow·모호 이름·잘못된 ID·부정·질문·인용·부분 복합 실행은 제출 0회 |
| 출처 | Chat/최종 마이크 각각 신뢰 검사 통과; 위조/중간 인식은 제출 0회 |
| 확인 | 확인 전 0회; 유효 확인 소비 시 현재 신뢰·세션·권한·대상·busy/admission 통과 후 1회; 취소/만료/중복/교차 요청/재검증 거절 0회 |
| 이벤트 재전달 | 동일 이벤트 제출·응답 중복 없음; 새 이벤트의 같은 문장은 정상 재요청 가능 |
| busy·STOP | 기존 거절·소유권·STOP 우선 경로, 지속 Task 종료와 pending 확인 관계 |
| 즉시 명령·disable | 결과·active 해제·다음 admission, chatclef off 결과와 재활성화 |
| 결과·화면·음성 | 잘못된 상관 ID/지연/중복 결과 처리, 접수와 완료 구분, 실제 sink 출력 경계 |
| 전체 이름 자료 | 모든 대상 순회, 누락·충돌·잘못된 ID·모드 번역/별칭·종류별 capability 차이 |
| 회귀 | GET/FIND/GOTO, H5 등록, STORE_HOME, KEEP_LOADOUT/working-set/trusted-container, 자동방어 우선 |
| 진단 비간섭 | 같은 입력에서 로그 모드·예산·출력 실패만 바꿔도 기능 결정과 정리가 동일 |

테스트는 파서 호출만으로 끝내지 않고 실제 운영 입력 진입점의 신뢰 검사부터
제출·결과·UI/TTS 연결을 포함한다. 대역 bridge·게임 API·리스너와 실제 하드웨어,
실게임 관찰을 구분한다. 이전 FIND 또는 응답 profile 테스트 통과 수를 전체 명령
실행 검증 수로 재사용하지 않는다.

### 10.2 나중에 사용자가 따라 할 Chat·마이크 인수 문장

4절의 모든 행을 두 출처에서 각각 검사한다. 아래는 특히 놓치기 쉬운 목표 시나리오다.
**지금 실행할 지시가 아니며**, 필요한 확인·테스트 환경·실월드 권한을 갖춘 뒤 시행한다.

- `좀비 세 마리 공격해 줘` → 정확한 대상·수량으로 확인 대기 → 같은 요청 확인 후 1회 제출.
- `공격하지 마`, `기억 초기화가 뭐야?` → 해당 게임 명령 제출 0회.
- `Alex에게 철괴 세 개 줘` → 상대 원문 보존, `give Alex iron_ingot 3` 한 번.
- `철괴 두 개와 금괴 세 개 구해 줘` → 기존 지원 ItemList 한 명령; 여러 명령 체이닝 아님.
- `밝기를 1.5로 설정해 줘` → 즉시 결과 후 active 해제; 다음 명령이 오래된 busy로 막히지 않음.
- `챗클레프 꺼 줘` → 필요한 확인·종료 결과 전달; 다시 켜는 사용자 경로와 한국어 on 검증.
- `자동 보관 장소 목록 보여 줘` → bounded 목록, 등록/해제 부작용 없음.
- `주변 16x16 자동 보관 장소 등록해 줘` → 단일 등록과 구분되는 기존 범위 등록.
- `인벤토리 전부 보관해 줘` → DEPOSIT_ALL과 기존 보호 정책; STORE_HOME으로 치환하지 않음.
- `철 골렘 위치만 알려 줘` / `철 골렘 찾아 줘` → 기존 FIND report / approach 구분 보존.
- `Alex 따라가 줘`, `가만히 있어 줘`, `멈춰 줘` → 기존 확인·busy 정책 아래 지속/STOP 상태 구분.
- 동일 최종 음성 이벤트 재전달 → 제출 1회 유지; 새로운 최종 이벤트로 같은 요청 → 영구 차단 없음.

### 10.3 빌드와 실제 게임 검증의 분리

향후 Java 변경 시 적용할 runbook 명령은 다음과 같다. **이번 문서화에서는 실행하지 않는다.**

```powershell
Set-Location -LiteralPath 'C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1'
.\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace
```

빌드 JDK·Gradle JVM·전체 입력과 dirty 변경 단위·명령·exit code·실패 경계·실행 Task,
fresh 1.20.1 JAR 절대 경로·크기·시각·SHA-256을 기록한다. 특정 버전의 remapJar만
성공한 경우 전체 다중 버전 clean build 성공으로 보고하지 않는다.
소스 검사·격리 테스트·실제 로그 sink·빌드·JAR 식별·배포·게임/Mixin·실제 효과 검증은
각각 별도 결과이다. 미실행은 이유와 함께 `NOT_RUN`, 근거 없는 값은 `UNKNOWN`이다.

외부 CurseForge 배포, Minecraft 실행, 실월드 변경, commit, push는 별도 요청 없이
수행하지 않는다. 실제 게임을 실행하지 못했다면 게임 성공으로 표시하지 않는다.

### 10.4 최종 구현 보고의 필수 항목

1. 변경 파일·기존 owner·변경 이유와 필요한 최소 Java 경계.
2. 전체 26개 의미상 명령 및 호환 이름의 한국어 표현·인자·canonical·Chat/최종
   마이크 지원·확인·검증 상태 표. 모든 인자 형태와 원래 기능 한계 포함.
3. 명령별 전체 대상·한국어 해석·누락·모호성·실제 capability·경로 연결 수와 자료 출처.
4. 실제 실행한 테스트 명령·결과·출력 증거, 전체 빌드와 대상 JAR·배포·런타임의 개별 상태.
5. 사용자가 그대로 따라 할 문장과 예상 제출/확인/화면/음성 결과.
6. 남은 문제의 영향·근거·미구현·미검증 범위. 기존 기능 한계와 이번 구현 누락 구분.

부분 명령, 샘플 별칭, 문서, 플래그, 영어 ID fallback, 파서 통과만으로
**“전체 한국어 지원 완료”라고 표시하지 않는다.** 명령별 Chat·최종 마이크 실행
연결과 실제 완료 검증의 차이까지 표에서 드러나야 한다.

## 11. 이번 문서화 변경 기록

이 문서를 새로 작성하고 Minecraft README 및 기존 Workstream C·registry·alias·
test strategy 문서에 연결·상태 주석을 보완했다. current-source 표는 읽기 전용
문법/등록 확인이며 production 코드, JSON 이름 자료, 테스트 코드, AGENTS.md는
수정하지 않는다. 문서 링크·표·경로·diff만 검사하며 기능 테스트·빌드·배포·
Minecraft 실행·commit·push 결과를 새로 만들지 않는다.

| 변경 문서 | 변경 이유 |
| --- | --- |
| [본 요구사항 문서](chatclef-all-commands-korean-chat-microphone-requirements-2026-09-15.md) — 신규 | 사용자 요구, 현재 소스 문법·공백, 26개 명령, 안전·전체 경로·검증·완료 기준 통합 |
| [Minecraft README](../README.md) | 사용자 진입 링크와 요구사항/미구현 상태 안내 |
| [Workstream C](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md) | 현재 27개 이름·FIND 보존 반영, 과거 상태와 최신 범위 구분 |
| [레지스트리 계획](chatclef-python-korean-command-registry-plan.md) | 공개·파서·admission 차이와 실제 Java 문법 기준 연결 |
| [아이템·별칭 계획](chatclef-korean-item-action-alias-v2-plan.md) | 전체 이름·명령별 native 제한·한국어 누락의 구분 연결 |
| [테스트 전략](chatclef-korean-test-strategy.md) | 두 입력 출처의 전체 경로·모든 대상·회귀·증거 구분 연결 |

최초 문서화의 정적 검사에서 26개 정규 명령과 호환 이름, 공개 11개 행을 소스 AST와
대조했다. 로컬 링크·Workstream C anchor·UTF-8·표·code fence·공백·원문 SHA-256을
검사했으며 변경 범위가 Markdown 6개뿐임을 확인했다. 이는 애플리케이션 import나
기능 테스트 실행이 아니다.

### 11.1 2026-09-15 문서 재검수

첨부 원문·현재 Java 소스·연결 문서를 다시 대조하여 다음을 보완했다.

- Workstream C의 GOTO 인자 범위와 한국어 `chatclef on` 복구 검증을 최신 요구와 맞췄다.
- ATTACK의 처치 수, MEAT의 보유 food score 가산, GIVE의 실제 모드 아이템 토큰,
  EQUIP의 인자 수용과 장착 Task 한계를 명확히 했다.
- ItemList의 실제 목록 문자·반복 항목 합산·합산 overflow 검증을 구분했다.
- 확인 소비 시 현재 조건의 재검증과 기존 플레이어 공격 보호를 명시했다.
- 최초 작성 기준과 재검수 시작 시점의 Git 상태를 구분했다.

재검수 수정 대상은 본 문서와 Workstream C 두 Markdown 파일이다. 앞서 작성한
나머지 네 문서는 보존했다. 소스 AST와 명령 표 26행·공개 11행을 다시 대조했고,
본 문서의 로컬 링크 30개와 anchor, 표·UTF-8·code fence·공백·원문 SHA-256 검사를
통과했다. `git diff --check`도 통과했으며 전체 dirty 범위는 Markdown 6개이다.
수정 후 독립 정합성 검토에서 추가 수정이 필요한 충돌은 발견하지 못했다.
코드는 변경하지 않았고 기능 테스트·빌드·런타임 검증은 계속 `NOT_RUN`이다.
