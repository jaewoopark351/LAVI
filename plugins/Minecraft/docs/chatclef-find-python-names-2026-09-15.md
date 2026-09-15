<!-- 20260915_kpopmodder: Record the Python-owned FIND name boundary, language data coverage, and verification limits. -->
# FIND 한국어 이름을 Python·JSON으로 이관

## 기준과 변경 범위

입력은 `LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (67)(1).zip`이다.
ZIP comment의 기준 커밋은 `faad78953bd2a0e51cb72fcb07df71f283f4ef6c`이다.
GitHub 원격, 사용자 PC, 게임 인스턴스, 실행 JAR는 수정하지 않았다.
이 기록은 소스 변경과 격리 검증 기록이며 배포 또는 실게임 합격 기록이 아니다.

`@get`과 동일한 책임 경계를 적용한다. Python은 한국어 요청과 이름 데이터를 해석하고,
Java는 실제 게임 레지스트리 ID를 검증하여 기존 FIND Task를 실행한다.
`@get`의 허용 획득 목록이나 TaskCatalogue를 FIND 검색 허용 목록으로 사용하지 않는다.
GET/ATTACK 명령, Baritone, 자동방어, TaskRunner, GUI 슬롯 입력 및 STOP 정책은 변경하지 않았다.

```text
LAVI Chat / 마이크 최종 인식
  → 기존 신뢰·문장·부정문·복합 명령 검사
  → Python 한국어 이름 / 별칭 JSON
  → find entity minecraft:iron_golem approach
  → 기존 원문 일치·busy·중복·전송 검사
  → Java의 현재 ENTITY_TYPE / BLOCK / ITEM 검증
  → 기존 탐색 / 접근 / 방어 양보
  → 타입화된 실제 결과 검증
  → Python 한국어 화면 / TTS 리스너 응답
```

## 실행 예시

| LAVI 채팅/최종 음성 요청 | Java에 전달되는 명령 |
|---|---|
| 철골램 찾아줘 | `find entity minecraft:iron_golem approach` |
| 철 골렘 찾아줘 | `find entity minecraft:iron_golem approach` |
| 아이언 골렘 위치만 알려줘 | `find entity minecraft:iron_golem report` |
| 소 찾아줘 | `find entity minecraft:cow approach` |
| 양 찾아줘 | `find entity minecraft:sheep approach` |
| 돼지 찾아줘 | `find entity minecraft:pig approach` |
| 상자 찾아줘 | `find block minecraft:chest approach` |
| 떨어진 상자 찾아줘 | `find item minecraft:chest approach` |
| 떨어진 다야 찾아줘 | `find item minecraft:diamond approach` |
| 엔티티 example:new_mob 찾아줘 | `find entity example:new_mob approach` |

네이티브 Minecraft 채팅과 직접 명령 GUI는 영어 ID를 사용한다.

```text
@find entity minecraft:iron_golem report
@find entity iron_golem
@find entity cow
@find block minecraft:chest
@find item minecraft:diamond
@find player Steve
```

짧은 ID는 `minecraft:`를 붙이는 의미다. `iron_golem`의 밑줄을 지우지 않는다.
모드에는 정확한 `namespace:id`를 사용한다. `example:new_mob`는 예시 ID이며 설치를 보장하지 않는다.
Java에 한국어를 직접 보내는 예전 `@find 주민` 문법은 더 이상 지원하지 않는다.
LAVI Chat에 입력한 `@find entity 철골램`은 Python 번역 대상이므로 ID로 변환된다.
영어로만 된 자동 Chat/음성 입력에 한국어 신뢰 승인을 새로 발급하지 않는다. 기존 경계를 유지한다.

## Python 데이터 파일

위치: `plugins/Minecraft/fabric/chatclef/intent/resources/`

### korean_find_aliases.json

오타·줄임말·모드 별칭을 관리하는 사용자 편집 파일이다.
`schema_version`, `minecraft_version`, `asset_index_paths`, `entity`, `block`, `item` 필드를 유지한다.
예를 들어 `entity` 안의 다음 항목이 `철골램`, `철 골램`, `아이언골렘`을 처리한다.

```json
"minecraft:iron_golem": ["철 골램", "아이언 골렘"]
```

공백을 제거한 이름 비교는 Python에서만 한다. ID 비교에는 적용하지 않는다.
모드에 이름을 추가하는 예시는 다음과 같다. 반드시 실제 설치된 모드의 ID를 확인한다.

```json
"example:giant_cow": ["거대 소", "큰 소"]
```

`entity`, `block`, `item`은 서로 다른 사전이다. 설치 블록을 찾는 요청과 드롭 아이템을 찾는 요청을 섞지 않는다.
이름이 여러 대상으로 겹치면 자동 실행하지 않고 종류/ID 후보를 안내한다.
알려지지 않은 한글을 비슷한 몹으로 추측하거나 LLM으로 임의 보정하지 않는다.
JSON 문법 오류·중복 키·잘못된 ID·제어 문자·초과 크기는 조용히 무시하지 않고 실패 처리한다.
편집 후 LAVI Python 프로세스를 다시 시작한다. JAR 재빌드는 별칭 편집 자체에는 필요하지 않다.
실행 중 JSON 핫 리로드는 보장하지 않으며, 진행 중인 요청의 사전을 덮어쓰지 않는다.

### korean_find_entity_names.json

Minecraft 1.20.1 바닐라 엔티티 타입 124개의 기본 한국어 이름이다. 소·양·돼지·철 골렘뿐 아니라
해당 버전의 전체 엔티티 타입 ID를 포함한다. 이는 스폰 가능성이나 실제 발견 성공을 보장하는 목록이 아니다.

레지스트리 목록 출처는 `misode/mcmeta` 저장소의 `1.20.1-registries` ref,
`entity_type/data.json`, Git blob `fef0b3bb2820bfabc26825426ae867e499e8cabd`이다.
한국어 대조 자료는 `InventivetalentDev/minecraft-assets`의 `1.20.1` ref,
`assets/minecraft/lang/ko_kr.json`, blob `fdb0c864b2b1a5fe636f0cf71f5885adb1ee2537`이다.
테스트는 포함된 ID 124개를 정렬·직렬화한 Git blob 해시가 출처와 정확히 같은지도 확인한다.
`killer_bunny`처럼 번역 키만 있고 별도 레지스트리 타입은 아닌 이름을 임의의 ID로 등록하지 않는다.

### 전체 바닐라 아이템·블록 이름의 출처

기존 `chatclef_item_command_target_policy.json`의 **이름 데이터만** 기본 오프라인 자료로 재사용한다.
이 파일의 GET 획득 정책이나 카탈로그 허용 여부를 FIND에 적용하지 않는다.
이 기본 자료만으로 모든 바닐라 아이템·블록의 한국어 이름이 포함됐다고 주장하지 않는다.

전체 로컬 이름은 Python이 설치된 Minecraft 1.20.1의 `ko_kr.json` asset을 읽어서 보충한다.
기본 탐색 위치는 다음과 같다.

```text
%USERPROFILE%/curseforge/minecraft/Install/assets/indexes/5.json
%APPDATA%/.minecraft/assets/indexes/5.json
```

다른 위치를 사용하는 경우 `korean_find_aliases.json`의 `asset_index_paths`에 실제 경로를 설정한다.
JSON 경로에는 `/` 또는 이스케이프한 `\\`를 사용한다.

```json
"asset_index_paths": ["D:/Minecraft/assets/indexes/5.json"]
```

`objects["minecraft/lang/ko_kr.json"].hash`로 가리키는 로컬 asset만 읽는다.
파일의 SHA-1을 확인하며 파일당 최대 4 MiB, 명시 경로 최대 8개로 제한한다.
네트워크 다운로드, 게임 파일 수정, 모드/리소스팩 설정 변경, 월드 쓰기는 하지 않는다.
표시 전용 하위 번역 키나 일부 남은 구버전 키는 후보에 사용하지 않는다.
블록 번역을 공유하는 드롭 아이템 이름은 어휘 후보로 추가하되, 실제 ITEM 등록 여부는 Java가 검증한다.
따라서 번역 JSON에 키가 있다는 이유만으로 그 ID가 게임에 존재한다고 인정하지 않는다.

언어 asset을 찾지 못하면 기본 이름+별칭으로 동작하고 `asset_status=bundled_names_only`가 기록된다.
로드에 성공하면 `asset_status=local_asset_loaded`가 기록된다.
미번역 모드 이름 및 활성 리소스팩의 별도 한국어 이름은 이 이관에서 자동 수집하지 않는다.
모드의 한국어 별칭은 JSON에 추가하고, 정확한 모드 ID는 Python 사전에 없어도 Java에 전달할 수 있다.
Minecraft 버전 변경에 따른 사전/asset-index 자동 갱신은 범위 밖이다.

## Java 변경과 결과 연결

`FindRegistryResolver`의 언어 리소스 읽기와 주민 하드코딩 별칭을 제거했다.
`FindNameIndex`는 정확한 ID 및 종류만 비교한다. `auto`의 실제 BlockItem/설치 블록 관계 처리는 유지한다.
`FindRequest`는 네이티브/bridge 대상으로 영어 ID 또는 플레이어 닉네임만 허용한다.
현재 등록된 ID가 없으면 `UNKNOWN_TARGET`, 동일 ID의 종류가 모호하면 `AMBIGUOUS_TARGET`이다.
`language_warnings` wire 필드는 호환성을 위해 남기되 새 Java resolver에서는 0이다.

한국어 원문과 원래 slots는 Python intent에 그대로 보존한다.
입력 binding 및 피드백 descriptor는 원문 slots를 다시 ID로 컴파일하여 전송 명령과 비교한다.
번역 데이터에 들어 있는 ID나 표시 이름만 믿고 검증을 우회하지 않는다.
종류, 대상 ID, 모드가 바뀌거나 최종 입력 proof가 만료되면 실행하지 않는다.
명확한 FIND 요청의 이름 해석이 실패하면 일반 대화/LLM으로 넘기지 않고 한국어 거절 응답으로 소비한다.
이 거절 응답도 원문을 다시 해석해서 만들며, 외부 번역 데이터의 오류 문장이나 후보를 그대로 신뢰하지 않는다.
결과 decoder는 반환 registry_id가 명령의 정확한 ID와 일치하는지도 검증한다.
시작/종료 화면 및 TTS용 이름은 Python 사전에서 렌더링한다.

FIND의 탐색/접근/자동방어 선점 동작, 슬롯 입력, 리소스 획득 코드는 변경하지 않았다.
`FindTask`에는 `resolved` 진단 경계의 ID 출처 필드만 추가했다.
아이템 검색은 드롭된 `ItemEntity` 대상이다. 상자 내용물 검색, 자동 수집, 제작 또는 채굴 명령이 아니다.
검색은 기존에 로드·관측되는 월드 범위와 이동 가능한 영역을 사용한다. 이름 지원과 실제 발견 성공은 별개다.

## 진단

기존 입력 로거로 요청당 하나의 `[LAVI FIND Input]` JSON 메시지를 기록한다.
원문 query, 최종 kind/registry_id/mode, source, asset_status, asset_warnings,
name_count, alias_count, vocabulary_sha256, event_id 및 검증 reason을 남긴다.
외부 거절 데이터도 필드 타입/길이를 제한한다. 문자열 최대 128자, reason 최대 64자이며 목록을 덤프하지 않는다.
Java의 기존 `resolved` 경계에는 `resolutionSource=registry_id`, `requestedKind`, `query`를 추가한다.

대표적인 정상 해석은 다음과 같다.

```text
query=철골램
kind=entity
registry_id=minecraft:iron_golem
source=json_alias
reason=original_find_matched
```

진단 테스트는 Python의 실제 `core.logger.log_print`/formatter/UTF-8 파일 출력을 확인한다.
Java는 실제 진단 emitter/mode/admission/formatter의 stdout을 확인한다.
사용자 PC의 실제 게임 로그 파일 영속화나 스피커 재생까지 검증했다는 의미는 아니다.

## 검증 결과와 미검증 사항

환경: Python 3.13.5, OpenJDK 21.0.11, Java harness 컴파일 타깃 `--release 17`.
변경 전 FIND 전용 기준 실행은 183 passed였다.
최종 이름·binding·리스너·게임 API 대역 검사를 포함한 FIND 전용 실행은 402 passed였다.
확장 회귀 실행은 `909 passed, 13401 subtests passed, 2 failed`이다.

남은 두 실패는 동일한 기존 테스트 안의 SHA-256 비교다.
`CataloguedResources.txt`와 `korean_item_aliases.json`의 snapshot 기대값이 실제 파일과 다르다.
두 파일은 이 패치에서 변경하지 않았고, 원본 ZIP을 별도 풀어 동일 실패 2건을 재현했다.
범위 밖의 snapshot 기대값을 수정하여 통과 처리하지 않았다.

```text
CataloguedResources.txt
expected: e22b898c88f5687afa82ddfc83896333d9d1b2454a86771ff3bb2a66452e5825
actual:   4591ae83151dd2643943dfdcbb2e89a53d26de393c583902370c3f4d3c57188a

korean_item_aliases.json
expected: 4d371eef8e4161b0a064f57daf29794b6615577890c204ab0a73461c15c3695c
actual:   fe395f2d13b5b8f5537b6413a9e5c6c3bed80a5bcc44b428863bc969a5586080
```

기존 FIND Java harness는 실제 Task/TaskRunner/선택/이동 소스를 실행하지만 Minecraft·Baritone API는 대역이다.
전체 게임 실행, 실제 철 골렘 발견, 실제 경로 도달, 실제 음성 인식/합성/재생은 수행하지 않았다.

정식 `gradlew clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace`를 시도했으나
Gradle 8.8 배포 파일 다운로드에서 `UnknownHostException: services.gradle.org`로 실패했다.
Java 컴파일 이전 실패이며, 이 환경에서 새 배포용 JAR는 생성되지 않았다.
사용자 환경에서는 기존 `chatclef-fabric-build-verification.md`에 따라 clean forced build,
1.20.1 JAR 확인, 실제 인스턴스 JAR 해시 대조 및 게임 검증을 별도로 수행해야 한다.

테스트 명령과 원본 실패/최종 출력/Gradle 실패 로그는 전달 패키지의 verification에 포함한다.
