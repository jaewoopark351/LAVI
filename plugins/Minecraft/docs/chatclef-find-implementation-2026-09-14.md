<!-- 20260915_kpopmodder: Preserve this historical record and point to the superseding name-boundary contract. -->
> 이름 해석 및 네이티브 명령 문법의 현재 기준은
> [Python JSON 이름 해석 이관 기록](chatclef-find-python-names-2026-09-15.md)이다.
> 아래 기록 당시의 Java 한국어 이름 해석/명령 예시는 이관 전 내용이며,
> 탐색·자동방어·접근 수명주기는 이번 이름 처리 변경으로 다시 설계하지 않았다.

<!-- 20260914_kpopmodder: Implementation and verification record for the uploaded baseline; not a deployment record. -->
# LAVI FIND 구현·검증 기록 — 2026-09-14

## 결과와 검증 경계

첨부 ZIP의 소스를 수정했다. FIND 실행, 한국어 입력, 타입화된 결과, 화면·TTS 응답 연결 및 회귀 테스트를 포함한다.
**배포 가능한 새 JAR를 만들었다고 주장하지 않는다. 실제 Minecraft 실행도 수행하지 않았다.**
Gradle wrapper 배포 파일을 내려받는 단계에서 `java.net.UnknownHostException: services.gradle.org`가 발생해 정식 빌드가 컴파일에 도달하지 못했다.
격리 Java 검사는 실제 FIND 소스를 사용하지만 Minecraft/Baritone/Task/Gson API를 테스트 대역으로 대체한다. 따라서 전체 모드 빌드, Mixin/remap, 게임 동작 증거가 아니다.

- 입력 파일: `LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (66).zip`
- ZIP comment의 기준 커밋: `2ecc1283a1e5cf48357dd0e263c2526e7bcb64e2`
- 수정 작업 경로: `/mnt/data/lavi_work/LAVI-minecraft-plugin-fix-alto-clef-infinite-loop`
- GitHub 원격 저장소, 사용자 PC, CurseForge 인스턴스는 수정·커밋·푸시·배포하지 않았다.
- Python 3.13 / pytest 9.0.2, OpenJDK 21.0.11 / `javac --release 17`로 아래 검사를 수행했다.
- 의존성·Java 타깃·Gradle·Fabric·Minecraft 버전은 변경하지 않았다.

## 사용법

LAVI 채팅 또는 마이크의 **최종 인식 결과**로 다음과 같이 요청한다.

```text
마을 주민 찾아줘
마을주민 찾아줘
좀비 찾아줘
상자 찾아줘
상자 블록 찾아줘
다이아몬드 블록 찾아줘
떨어진 다이아몬드 찾아줘
상자 위치만 알려줘
엔티티 minecraft:villager 찾아줘
떨어진 example:rare_gem 찾아줘
```

Minecraft의 네이티브 ChatClef 명령 또는 기존 직접 명령 GUI에서는 다음 형식을 사용한다.

```text
@find 마을 주민
@find entity minecraft:villager
@find entity minecraft:zombie
@find block minecraft:chest
@find item minecraft:diamond
@find item example:rare_gem report
@find player Steve report
```

문법은 `@find [auto|entity|block|item|player] 대상 [approach|report]`다.
기본은 **approach: 탐색 후 실제 대상에서 4블록 이내에 접근**이다. `report`는 좌표 확인만 하고 이동하지 않는다.
Java 탐색 실패나 모호한 이름은 한국어 실패 결과로 끝내며, 일반 Task 종료를 성공으로 간주하지 않는다.
LAVI 자동 채팅·음성 경로의 기존 한국어 eligibility 정책은 유지한다. **영문 DSL만 입력한 자동 채팅·음성은 새로 허용하지 않는다.**
ID를 이 경로에서 사용하려면 위의 `엔티티 minecraft:villager 찾아줘`처럼 한국어 요청으로 감싸면 된다.
원시 명령에 대한 한국어 화면 응답은 기존 직접 명령 GUI 경로를 따른다. Minecraft 네이티브 채팅 명령은 Java의 한국어 메시지를 출력하며, 네이티브 채팅 자체가 별도로 LAVI TTS 요청을 만들지는 않는다.

## 검색 범위와 의미

엔티티 타입, 블록 타입, 아이템 타입은 실행 중인 `Registries.ENTITY_TYPE`, `Registries.BLOCK`, `Registries.ITEM` 전체를 순회하여 해석한다.
기본 몹 몇 종류만 열거하는 whitelist는 없다. 모드가 등록한 타입도 같은 방식으로 처리한다.
한국어·영어 리소스와 현재 언어 이름을 별칭으로 만들며, `마을 주민`/`마을주민`은 런타임에 주민이 등록되어 있을 때만 보조 별칭으로 추가한다.
언어 팩별 파일 우선순위를 유지하고 Reader를 닫는다. 한국어 번역이 없는 모드의 한국어 이름을 생성하거나 추측하지는 않는다. 이때 정확한 `namespace:id`를 사용한다.

검색에는 다음과 같은 의도적인 경계가 있다.

| 항목 | 구현된 범위 |
|---|---|
| 엔티티 / 드롭 아이템 | 요청 시작 위치 기준 반경 64블록, 현재 클라이언트에 로드된 대상, 최대 4,096개 검사 |
| 배치된 블록 | 시작 위치에서 X/Y/Z 각 축 ±32블록, 현재 로드된 청크만 검사 |
| 블록 검사 비용 | 틱당 최대 4,096좌표 및 약 2ms 슬라이스; 청크 생성·로딩 요청 금지 |
| 시간 제한 | 한 요청의 전체 경과 시간 90초, 방어 선점 시간도 포함, 재개로 초기화되지 않음 |
| 아이템 | 바닥에 떨어진 `ItemEntity`의 ItemStack 타입. 상자 안·플레이어 인벤토리·제작 재료 검색 아님 |
| 접근 | 기존 Baritone 목표로 이동. FIND가 블록을 캐거나 놓는 경로는 차단 |
| 월드 전체 탐색 | 미지원. 무한 배회·전역 스캔·새 청크 탐험으로 확대하지 않음 |
| 수집 / 처치 | 요청 동작에 포함하지 않음. 자동 제작·자원 획득·드롭 회수·처치 횟수 추적 없음 |

범위 안의 관측 대상 중 가까운 대상을 선택한다. 블록 탐색은 여러 틱에 걸친 관측이므로 월드 전체의 원자적 스냅샷은 아니다.
대상이 사라지거나 교체되거나 초기 범위를 벗어나면 같은 이름의 다른 대상으로 몰래 바꾸지 않고 `TARGET_LOST`로 종료한다.
`상자`처럼 BlockItem과 배치 블록이 같은 이름이면 기본은 배치 블록이다. 드롭된 상자는 `item` 또는 `떨어진 상자`로 지정한다.
다른 모드/종류 사이에 이름이 겹치면 최대 다섯 개 후보를 제시하고 정확한 종류·ID를 요구한다.
검색 결과가 없다는 말은 **관측한 로드 범위에서 없었다는 뜻**이며 월드 전체에 없다는 뜻이 아니다.

드롭 아이템에는 접근 금지 거리와 4블록 완료 거리를 사용하고 직접 줍기 동작을 만들지 않는다.
다만 서버의 자동 픽업, 이미 가까이 있는 플레이어, 움직이는 드롭, 방어 중 이동까지 전역적으로 금지하지는 않는다.
따라서 **어떤 상황에서도 아이템이 인벤토리에 들어오지 않는다는 보장은 아니다.**
도달 판정은 실제 좌표 거리이며 시야 확보나 대상과의 상호작용 성공을 요구하는 판정은 아니다.

## 코드 소유권과 기존 동작 보존

Java 구현은 `src/main/java/lavi/minecraft/task/find/` 아래로 분리했다.

- `FindCommand`: 네이티브 문법 검증, FIND Task 실행, 호출별 callback 격리.
- `FindRequest`: 엄격한 종류/이름/모드 문법.
- `FindNameIndex`, `FindRegistryResolver`: 런타임 등록 타입과 언어 리소스 기반 해석.
- `FindTask`: 범위·기한·대상 UUID/좌표·재검증·불변 결과 소유.
- `FindApproachTask`: 기존 `GoalFollowEntity` / `GoalBlock` 및 Baritone custom-goal process를 사용하는 이동 전용 자식.
- `FindOutcome`: 한국어 네이티브 메시지 및 타입화된 wire 결과.
- `FabricChatClefFindResultProjector`: 기존 matching-task/root-lifetime 검증 뒤에만 FIND 결과를 투영.

기존 `AttackPlayerOrMobCommand`는 읽고 비교했지만 수정하지 않았다.
그 Task는 처치 횟수·죽음 이벤트·드롭 회수가 묶여 있어 FIND에서 상속·복사하지 않았다.
기존 `GetToEntityTask`의 `TimeoutWanderTask` 경로에는 몹을 죽이는 복구 동작이 있으며,
`SafeRandomShimmyTask`는 왼쪽 클릭 입력을 강제한다. 따라서 이 래퍼들은 FIND에서 호출하지 않고 안전한 목표/경로 실행 계층만 재사용했다.
새 이동 알고리즘, 채굴 알고리즘, 자동방어 알고리즘을 만든 것은 아니다.

자동방어, KillAura, 무기 선택, 생존 Chain, 기존 @attack, @goto, 보관 및 GUI 슬롯 입력 코드는 변경하지 않았다.
FIND는 user task로 실행되며 우선순위/force-field 설정을 바꾸거나 자체적으로 선점을 거부하지 않는다.
선점·중단 시 FIND의 행동 정책을 pop하고 이동 자식을 종료한다. 재개 시 같은 대상과 같은 시간 예산을 재검증한다.
방어가 검색 중인 적대 몹을 죽일 수는 있다. 이 경우 FIND는 대상 소실로 처리하고, 처치를 FIND 성공으로 세지 않는다.
**방어 우선순위 자체와 실게임 선점 효과는 실제 게임에서 추가 확인해야 한다.**

Python은 별도 FIND parser, typed slots, 원문/번역 binding, 결과 decoder/evaluator/renderer를 추가했다.
기존 신뢰된 ingress receipt, final-only 음성, STOP, busy, 중복 방지, 전송 소유권, 시작 응답과 종료 응답의 순서를 유지한다.
부정문·질문형·복합 명령·명령 구분자·제어 문자는 임의로 실행 가능한 FIND로 고치지 않는다.
FIND가 새 명령이므로 현재 명령 목록/피드백/문법/수명주기 스냅샷은 26개에서 27개로 갱신했다. 과거 기록 문서나 다른 모듈의 개수를 일괄 변경하지 않았다.

## 관측과 실패 처리

FIND는 operation UUID를 소유하며 `start`, `resolved`, `selected`, `approach_start`, `yield_or_stop`, `resume`, `terminal`, `cleanup` 경계를 기록한다.
선점·재개 반복 로그는 첫 여덟 차례로 제한한다. 스캔 틱마다 좌표/엔티티 목록을 로그로 쏟지 않는다.
진단 로거의 실패가 실행 결정을 바꾸지 않도록 격리했다.

성공은 `FOUND`(report) 또는 `ARRIVED`(approach)뿐이다.
`NOT_FOUND`, `UNKNOWN_TARGET`, `AMBIGUOUS_TARGET`, `TARGET_LOST`, `NO_APPROACH`, `APPROACH_TIMEOUT`,
`SEARCH_LIMIT`, `SCOPE_CHANGED`, `PLAYER_UNAVAILABLE`, `INTERNAL_ERROR`는 실패로 구분한다.
취소·STOP·콜백만 도착한 경우·다른 요청/종류/대상·변조된 결과는 성공을 발표하지 않는다.
wire payload의 요청/종류/모드/UUID/좌표/범위/상태를 Python이 다시 검증한다.

## 수행한 검사

최종 합동 실행:

```bash
python -m tests.minecraft_chatclef.find.run_offline_tests -q   tests/minecraft_chatclef/find   tests/minecraft_chatclef/lavi_input   tests/minecraft_chatclef/command_lifecycle   tests/test_minecraft_chatclef_korean_rule_parser.py   tests/test_minecraft_chatclef_command_compiler.py   tests/test_minecraft_chatclef_natural_language_service.py   tests/test_minecraft_chatclef_korean_intent_schema.py   --disable-warnings --maxfail=8
```

결과: **598 passed, 11127 subtests passed**.
이 중 FIND 전용 실행은 **167 passed**였다. Java harness 두 개가 이 개수에 포함되므로 별도 테스트인 것처럼 중복 합산하지 않는다.
순수 Java 계약 35개 검사와 실제 FIND 소스를 실행하는 API 대역 기반 런타임 검사 54개가 통과했다.
Java가 실제로 출력한 terminal payload를 Python decoder로 읽는 교차 언어 계약 검사도 수행했다.

주요 확인 항목은 한국어/ID 문법, mod 등록 타입, 이름 충돌, BlockItem/블록 구분, 안전하지 않은 입력 차단,
원문/번역 일치, 신뢰된 채팅·최종 음성에서 정확히 한 번 제출, 소비된 음성 receipt 재사용 차단,
로드되지 않은 청크 제외, entity cap, 대상 교체·소실·범위 이탈, 기한 유지, 행동 정책 복구,
callback 분리, 로거 실패 격리, 잘못된 요청/중단 결과의 성공 차단, UI와 TTS 리스너에 한 번 전달 등이다.

Linux에서는 기존 Windows 전용 `winsound` import 때문에 일부 integration 모듈이 수집 단계에서 실패한다.
새 `run_offline_tests.py`는 비-Windows에서 import만 허용하는 **테스트 전용 shim**을 명시적으로 설치한다.
실제 `PlaySound` 호출은 AssertionError가 나므로 재생 성공을 흉내내지 않는다.
프로덕션 음성 코드는 수정하지 않았다. 여기서 확인한 것은 화면/출력/TTS 리스너로의 텍스트 전달이며,
실제 마이크 인식 품질·음성 합성 서버·스피커 재생은 확인하지 않았다.

처음 실행에서 발견된 명령 개수 스냅샷 불일치는 신규 명령에 맞게 갱신했다.
영문만 있는 자동 입력에 한국어 proof가 발급되지 않는 것도 확인하고 기존 제한을 유지했다.
격리 Java NO_APPROACH 테스트의 잘못된 바닥 fixture를 수정했다. 모두 이전 실패 기록에 남아 있다.
정식 Gradle 실패는 해결된 것으로 표시하지 않았다.

## 정식 빌드와 배포 전 확인

저장소의 `plugins/Minecraft/docs/chatclef-fabric-build-verification.md`를 기준으로 외부 PowerShell에서 실행한다.

```powershell
cd C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1
java -version
.\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace
```

Gradle 실행은 기존 JDK 21 개발 환경을 사용하고, Minecraft 1.20.1의 Java 17 타깃은 바꾸지 않는다.
`BUILD SUCCESSFUL` 이후 **1.20.1용 remapped runtime JAR**를 확인해야 한다.
이 프로젝트의 1.20.1 서브프로젝트 출력 경로는 `versions/1.20.1/build/libs/`다.
실제 생성 파일과 해시를 확인하고, `sources`, `dev`, 잘못된 버전 또는 이전 JAR를 런타임 파일로 착각하지 않는다.
이 전달물에는 새 JAR가 없으므로 Python 파일만 적용하고 기존 JAR를 그대로 두면 네이티브 FIND가 생기지 않는다.

실게임에서는 안전한 시험 환경에서 다음 순서로 확인한다.
1. `@find entity minecraft:villager report`로 움직이지 않는 좌표 보고를 확인한다.
2. 주민 접근, 상자 블록, 바닥의 다이아몬드, 미발견, 모호한 이름을 각각 확인한다.
3. 한국어 Chat와 최종 마이크 입력에서 각 한 번 실행 및 한국어 종료 응답을 확인한다.
4. 대상 소실/언로드, STOP, 방어 선점과 재개, 기한 종료에서 거짓 성공과 정책 잔류가 없는지 확인한다.
5. mod 한국어 번역·정확한 ID·리소스 팩 우선순위, 인벤토리 변화, 예상하지 않은 공격/채굴/배치 여부를 확인한다.

API 시그니처 확인에 사용한 1.20.1 공식 Yarn 문서:

```text
https://maven.fabricmc.net/docs/yarn-1.20.1+build.10/net/minecraft/resource/ResourceManager.html
https://maven.fabricmc.net/docs/yarn-1.20.1+build.10/net/minecraft/resource/Resource.html
https://maven.fabricmc.net/docs/yarn-1.20.1+build.10/net/minecraft/client/world/ClientChunkManager.html
```

## 전달 파일

`changed_files/`에는 변경·추가한 소스와 테스트, 이 기록만 담았다. 원본 전체 ZIP이나 기존 바이너리를 재포장하지 않는다.
`manifest.json`에는 모든 적용 파일의 원본/수정 SHA-256과 기준 커밋이 들어 있다.
unified patch는 원본 ZIP에서 꺼낸 파일들에 `git apply --check` 및 실제 적용 후 해시 대조로 검증한다.
검사 임시 파일·클래스 파일·캐시·개인 로그·폰트·기존 JAR는 적용 파일에 넣지 않는다.
