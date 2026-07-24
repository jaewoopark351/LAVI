<!-- 20260725_kpopmodder: Added the phase 1 investigation report for the planned Minecraft ChatClef/LAVI integration. -->

# 1단계 조사 보고서

## 0. 범위

이 문서는 LAVI에 ChatClef/AltoClef 기반 Minecraft 1.20.1 클라이언트 백엔드를 추가하기 전, 현재 구조와 최소 연결 지점을 정리한 조사 보고서다.

이번 단계에서는 코드 구현, 리팩토링, 폴더 이동, 삭제, 빌드는 수행하지 않는다. 라이선스 검토는 사용자 지시에 따라 이 문서의 판단 범위에서 제외한다.

## 1. 현재 LAVI 저장소 상태

확인된 작업 위치와 Git 루트는 모두 다음 경로다.

```text
C:\Vtuber_Souorce_Code\LAVI
```

현재 브랜치와 상태는 다음과 같다.

```text
branch: minecraft-plugin
HEAD: b9f8f767ba0bf303b164961eb54805c9ddd4b8f2
origin/main 대비: 0 ahead / 0 behind
working tree: ?? plugins/Minecraft/
```

`plugins/Minecraft/`는 아직 Git에 추적되지 않은 상태이며, 현재 내부에는 `runtime/` 폴더만 있다.

## 2. AGENTS.md 기준 작업 원칙

Minecraft 백엔드 추가 시 AGENTS.md의 리팩토링, 폴더화, 유지보수 규칙을 우선 적용해야 한다.

핵심 원칙은 다음과 같다.

```text
- 대규모 재작성보다 작고 안전한 변경을 우선한다.
- 기존 동작을 보존한다.
- public method name, config key, fallback path를 함부로 바꾸지 않는다.
- 새 기능은 책임별 폴더와 파일에 둔다.
- utils/helpers/common/misc 같은 임의 덤프 폴더를 만들지 않는다.
- 새 게임 연동은 GameExtensionInterface 구현체로 등록한다.
- ExtensionRegistry를 통해 등록한다.
- 파일 이동/이름 변경/삭제는 대상 경로와 영향 범위를 먼저 제시하고 확인받는다.
```

따라서 Minecraft 기능은 임시로 `AppComposer`에 직접 박는 방식이 아니라, 기존 StarCraft2/Chess처럼 플러그인 facade와 게임 확장 클래스로 분리하는 것이 맞다.

## 3. 현재 LAVI 플러그인 등록 구조

LAVI의 실행 및 플러그인 등록 흐름은 다음 구조를 따른다.

```text
main.py
-> app_core/app_composer.py
-> plugin_system/loader.py
-> app_core/optional_module_manifest.py
-> app_core/composition_core/optional_plugin_composition_service.py
-> app_core/extensions/extension_composition_core/game_extension_composition_service.py
-> app_core/extensions/extension_registry.py
```

현재 Minecraft는 `modules.json`, `optional_module_manifest.py`, `optional_plugin_composition_service.py`, `game_extension_composition_service.py`에 등록되어 있지 않다.

기존 게임 확장 구조를 기준으로 보면 Minecraft도 다음 조건을 만족해야 한다.

```text
- plugins/Minecraft/minecraft.py 같은 Python plugin facade를 가진다.
- app_core/extensions/... 아래 GameExtensionInterface 구현체를 가진다.
- ExtensionRegistry에 name 기반으로 등록된다.
- start/stop/handle_command/get_status 계약을 따른다.
```

## 4. Minecraft 폴더 권장 구조

사용자가 현재 지정한 경로는 다음과 같다.

```text
C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft
```

현재 ChatClef/Fabric 소스 위치는 다음과 같다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1
```

AGENTS.md 기준으로 이미 들어온 폴더를 임의로 옮기거나 이름 변경하지 않는 것이 안전하다. 1차 권장 구조는 다음이다.

```text
plugins/Minecraft/
  minecraft.py
  config/
    minecraft_config.json
  minecraft_core/
    minecraft_bridge_client.py
    minecraft_config.py
    minecraft_models.py
  runtime/
    chatclef_fabric_1.20.1/
```

`runtime/chatclef_fabric_1.20.1`는 ChatClef/Fabric 쪽 소스와 빌드 자산을 보관하는 경계로 보고, LAVI Python 플러그인 코드는 `runtime` 밖에 둔다.

## 5. ChatClef 1.20.1 소스/빌드 상태

확인된 ChatClef 복사본 위치는 다음이다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1
```

주요 파일과 폴더는 다음과 같다.

```text
src/
versions/
gradle/
build.gradle
gradle.properties
gradlew
gradlew.bat
root.gradle.kts
settings.gradle.kts
README.md
usage.md
CataloguedResources.txt
```

현재 확인된 빌드 관련 정보는 다음이다.

```text
Gradle wrapper: 8.8
local Java: OpenJDK 17.0.19
Fabric Loader: 0.16.2
Minecraft 1.20.1 mappings: yarn 1.20.1+build.10
Fabric API 1.20.1: 0.92.2+1.20.1
```

주의할 점은 `versions/mainProject`가 현재 `1.21.1`을 가리킨다는 점이다. 또한 `settings.gradle.kts`와 `root.gradle.kts`에는 `1.20.1` 프로젝트/노드가 선언되어 있지만, 현재 복사본에는 `versions/1.20.1` 물리 폴더가 없다. `versions/1.20.2`에는 `bin`만 있고, `versions/1.20.6`에는 `src`가 있다.

따라서 폴더명은 `chatclef_fabric_1.20.1`이지만, 실제 1.20.1 빌드를 위해 어떤 Gradle task 또는 version switch 절차를 써야 하는지 2단계 초반에 확인해야 한다.

이번 조사에서는 빌드를 실행하지 않았다.

## 6. `@get iron_pickaxe` 실행 경로

`@get iron_pickaxe`는 이미 AltoClef의 기존 task 시스템으로 처리된다. 확인된 흐름은 다음과 같다.

```text
AltoClefCommands
-> GetCommand
-> ArgParser
-> ItemList
-> ItemTarget
-> TaskCatalogue.getItemTask(...)
-> AltoClef.runUserTask(...)
-> UserTaskChain
-> TaskRunner
-> ResourceTask / CataloguedResourceTask
-> CraftInInventoryTask / CraftInTableTask / SmeltInFurnaceTask / MineAndCollectTask
-> Baritone pathing / mining / movement
```

즉, LAVI가 다음 로직을 재구현하면 안 된다.

```text
- iron_pickaxe 제작법
- 철 채광 조건
- 나무/막대기/조약돌/화로 재료 수급
- 제련 처리
- crafting table 사용
- Baritone 이동/pathfinding
```

LAVI는 구조화된 명령만 만들고, 실제 Minecraft 행동은 기존 AltoClef/Baritone 실행 경로에 맡기는 것이 맞다.

## 7. Player2와 AltoClef의 경계

최종 목표에서는 Player2의 LLM/STT/TTS를 사용하지 않는다.

Player2 의존 영역은 다음 쪽에 집중되어 있다.

```text
adris.altoclef.player2api.AICommandBridge
adris.altoclef.player2api.Player2APIService
adris.altoclef.player2api.ConversationHistory
adris.altoclef.player2api.ChatclefConfigPersistantState
adris.altoclef.ui.ChatclefToggleButton
adris.altoclef.ui.PlayerModeToggleButton
adris.altoclef.ui.STTfeedback
```

반대로 반드시 살려야 하는 AltoClef 핵심 영역은 다음이다.

```text
CommandExecutor
GetCommand / StopCommand / GotoCommand 등 command 계층
TaskCatalogue
Task / TaskRunner / UserTaskChain
ResourceTask 계열
Baritone 연동 계층
StorageHelper / Tracker 계열
```

따라서 초기 구현에서는 Player2 코드를 삭제하지 않고, 새로운 `lavibridge` 계층을 별도로 붙이는 편이 안전하다.

## 8. LAVI Bridge 최소 삽입 지점

가장 작은 Java 쪽 삽입점은 `AltoClef` 초기화 이후다.

권장 흐름은 다음이다.

```text
AltoClef initialized
-> LaviBridgeServer starts on 127.0.0.1
-> LaviCommandAdapter receives command
-> MinecraftThreadDispatcher dispatches to client thread
-> CommandExecutor.executeWithPrefix("get iron_pickaxe 1")
-> 기존 AltoClef task system 실행
```

초기 bridge endpoint는 `/v1` prefix를 붙이고, read-only 조회를 먼저 붙인 뒤 action endpoint를 최소로 확장하는 것이 좋다.

```text
GET  /v1/health
GET  /v1/status
GET  /v1/inventory
GET  /v1/actions/current

POST /v1/actions/get-item
POST /v1/actions/stop
```

스레드 경계가 중요하다. HTTP 서버 스레드에서 직접 Minecraft client/world/player 상태를 만지지 않고, client thread dispatcher를 거쳐야 한다.

Action 상태 추적은 별도 `LaviActionRegistry` 책임으로 분리하는 것이 좋다. 최소 상태는 `queued`, `running`, `succeeded`, `failed`, `cancelled`이며, `LaviBridgeServer`가 이 상태 관리를 직접 떠안지 않게 해야 한다.

제어 모드는 최소 세 가지가 필요하다.

```text
MANUAL
  사용자가 직접 조작하며 AltoClef 자동 작업이 없는 상태.

AI
  LAVI 명령에 따라 AltoClef가 플레이어를 조작하는 상태.

PAUSED
  자동 작업이 정지 또는 중단된 상태.
```

사용자 입력과 AltoClef 자동 조작이 충돌하는 영역은 기존 ChatClef/AltoClef 동작을 먼저 확인해야 하며, 임의로 키보드/마우스 제어 코드를 새로 만들지 않는다.

## 9. 포함/제외 후보

포함해야 할 후보는 다음이다.

```text
- AltoClef command 계층
- TaskCatalogue
- TaskRunner/UserTaskChain
- ResourceTask, crafting, smelting, mining task
- Baritone integration
- inventory/world/entity tracker
- Fabric mod entrypoint와 build scripts
```

초기 구현에서 우회 또는 제외할 후보는 다음이다.

```text
- Player2 LLM 호출
- Player2 STT/TTS 호출
- Player2 heartbeat
- Player2 conversation history
- Player2 character selection
- Player2 전용 UI 토글
```

단, 제외 후보라고 해서 바로 삭제하지 않는다. 삭제/이동/이름 변경은 별도 확인이 필요하다.

## 10. 예상 신규 파일

LAVI Python 쪽 예상 파일은 다음이다.

```text
plugins/Minecraft/minecraft.py
plugins/Minecraft/config/minecraft_config.json
plugins/Minecraft/minecraft_core/minecraft_bridge_client.py
plugins/Minecraft/minecraft_core/minecraft_config.py
plugins/Minecraft/minecraft_core/minecraft_models.py
app_core/extensions/minecraft_core/minecraft_game_extension.py
```

ChatClef Java 쪽 예상 파일은 다음이다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/lavibridge/LaviBridgeServer.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/lavibridge/LaviCommandAdapter.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/lavibridge/LaviStateReader.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/lavibridge/LaviActionRegistry.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/lavibridge/MinecraftThreadDispatcher.java
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/lavibridge/LaviStopController.java
```

추가 수정 예상 파일은 다음이다.

```text
modules.json
app_core/optional_module_manifest.py
app_core/composition_core/optional_plugin_composition_service.py
app_core/extensions/extension_composition_core/game_extension_composition_service.py
```

## 11. 최소 구현 단계

권장 구현 순서는 다음이다.

```text
1. ChatClef/Fabric 1.20.1 빌드 타깃 확인
2. 원본 ChatClef 상태에서 @get iron_pickaxe 수동 검증
3. 원본 ChatClef 상태에서 stop/cancel 수동 검증
4. ChatClef Java 쪽 read-only localhost bridge 추가
5. /v1/health, /v1/status, /v1/inventory, /v1/actions/current 단독 테스트
6. /v1/actions/get-item 하나만 연결
7. /v1/actions/stop 하나만 연결
8. 독립 테스트 클라이언트로 bridge 호출
9. LAVI Python bridge client 추가
10. Minecraft plugin facade와 MinecraftGameExtension 추가
11. optional module/extension registry 등록
12. LAVI UI 또는 command dispatch 경로 연결
13. get iron_pickaxe 1 end-to-end 테스트
```

첫 번째 수직 성공 기준은 다음이다.

```text
LAVI 측 테스트 코드
-> POST /v1/actions/get-item
-> item=iron_pickaxe, count=1
-> ChatClef가 기존 AltoClef Task를 실행
-> 새 생존 월드에서 철 곡괭이 확보
-> succeeded 상태 반환
-> 최종 인벤토리에서 iron_pickaxe 확인
```

## 12. 리스크와 중단 조건

라이선스를 제외한 기술적 중단 조건은 다음이다.

```text
- ChatClef 1.20.1 빌드 타깃이 확정되지 않은 경우
- Java 17/21 요구사항이 현재 로컬 환경과 맞지 않는 경우
- Fabric/Baritone dependency 다운로드 또는 빌드가 실패하는 경우
- Minecraft client thread dispatch 없이 world/player 상태 접근이 필요한 경우
- Player2 제거 범위가 불명확한 상태에서 기존 코드를 삭제해야 하는 경우
- plugins/Minecraft/runtime 폴더를 이동/정리해야 하는 경우
```

이 조건에 걸리면 구현을 멈추고 사용자 확인을 받아야 한다.

## 13. 결론

가장 안전한 방향은 다음이다.

```text
LAVI = AI 판단, 명령 생성, 상태 해석
ChatClef/AltoClef = Minecraft 실제 행동 실행
Baritone = 이동/pathfinding
```

즉, LAVI는 `get iron_pickaxe 1` 같은 구조화 명령을 보내고, ChatClef 쪽 bridge는 기존 AltoClef `CommandExecutor`와 task system을 호출해야 한다. 이 방식이 AGENTS.md의 리팩토링/폴더화/유지보수 원칙에도 가장 잘 맞는다.
