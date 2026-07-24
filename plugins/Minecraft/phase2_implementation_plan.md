<!-- 20260725_kpopmodder: Added the phase 2 implementation plan for the planned Minecraft ChatClef/LAVI integration. -->

# 2단계 구현 계획서

## 0. 목표

이 문서는 LAVI에 Minecraft 1.20.1 ChatClef/AltoClef 백엔드를 실제로 붙이기 위한 최소 구현 계획이다.

핵심 목표는 다음이다.

```text
LAVI가 Minecraft 행동을 직접 구현하지 않는다.
LAVI는 명령과 의사결정을 담당한다.
ChatClef/AltoClef는 기존 command/task/Baritone 경로로 Minecraft player를 조작한다.
```

초기 성공 기준은 다음이다.

```text
LAVI -> localhost bridge -> AltoClef @get iron_pickaxe 1 -> Minecraft player 실행
```

## 1. 구현 원칙

AGENTS.md 기준으로 다음 원칙을 지킨다.

```text
- 큰 리팩토링보다 작고 검증 가능한 변경을 우선한다.
- Player2 코드는 초기 단계에서 삭제하지 않는다.
- AltoClef task/crafting/mining/pathfinding 로직을 재구현하지 않는다.
- 새 기능은 책임별 파일과 폴더로 나눈다.
- public method, config key, fallback behavior를 함부로 바꾸지 않는다.
- 폴더 이동/이름 변경/삭제가 필요하면 먼저 대상 경로와 영향 범위를 제시하고 확인받는다.
- 새 Python/Java 파일에는 YYYYMMDD_kpopmodder 마커를 남긴다.
```

## 2. 제안 대상 구조

초기 구현 후 목표 구조는 다음이다.

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
      src/main/java/adris/altoclef/lavibridge/
        LaviBridgeServer.java
        LaviCommandAdapter.java
        LaviStateReader.java
        MinecraftThreadDispatcher.java
        LaviStopController.java
```

LAVI 게임 확장 쪽은 다음 위치를 권장한다.

```text
app_core/extensions/minecraft_core/
  minecraft_game_extension.py
```

기존 구조와 일관성을 위해 등록 파일은 다음을 수정 대상으로 본다.

```text
modules.json
app_core/optional_module_manifest.py
app_core/composition_core/optional_plugin_composition_service.py
app_core/extensions/extension_composition_core/game_extension_composition_service.py
```

## 3. 1차 구현 단위

### 3.1 ChatClef 1.20.1 빌드 타깃 확인

먼저 `runtime/chatclef_fabric_1.20.1`가 실제 Minecraft 1.20.1 Fabric jar를 만들 수 있는지 확인한다.

확인 항목은 다음이다.

```text
- Gradle task 목록
- versions/mainProject 값
- versions/1.20.1 물리 폴더 생성/전환 방식
- 1.20.1 target 선택 방식
- Java 17 사용 가능 여부
- Fabric Loader/Fabric API/Baritone dependency resolve 가능 여부
```

현재 정적 확인 결과는 다음이다.

```text
- settings.gradle.kts에는 1.20.1 project include가 있음
- root.gradle.kts에는 createNode("1.20.1", 12001, "yarn")가 있음
- build.gradle에는 1.20.1 Yarn/Fabric API 버전 매핑이 있음
- versions/mainProject 값은 1.21.1
- versions/1.20.1 폴더는 현재 없음
- versions/1.20.2 폴더에는 bin만 있음
- versions/1.20.6 폴더에는 src가 있음
```

이 단계에서 dependency 다운로드가 필요하면 사용자 확인 후 진행한다.

### 3.2 Java bridge 패키지 추가

새 Java 패키지를 추가한다.

```text
adris.altoclef.lavibridge
```

초기 클래스 책임은 다음이다.

```text
LaviBridgeServer
  127.0.0.1 전용 HTTP server를 시작/중지한다.

LaviCommandAdapter
  LAVI JSON command를 AltoClef command 문자열 또는 task 호출로 변환한다.

LaviStateReader
  player/world/inventory/current task 상태를 읽어 JSON 응답으로 만든다.

LaviActionRegistry
  action_id별 queued/running/succeeded/failed/cancelled 상태와 최근 오류를 관리한다.

MinecraftThreadDispatcher
  HTTP thread에서 받은 작업을 Minecraft client thread로 안전하게 넘긴다.

LaviStopController
  기존 AltoClef.stop(), cancelUserTask(), Baritone forceCancel 경로를 안전하게 사용한다.
```

### 3.3 최소 HTTP API

초기 API는 `/v1` prefix를 사용하고, read-only 조회 API를 먼저 만든 뒤 action API를 붙인다.

읽기 전용 1차 API는 다음이다.

```text
GET /v1/health
GET /v1/status
GET /v1/inventory
GET /v1/actions/current
```

쓰기/행동 2차 API는 다음이다.

```text
POST /v1/actions/get-item
POST /v1/actions/stop
```

`POST /v1/actions/get-item` request/response 초안은 다음이다.

```json
{
  "item": "iron_pickaxe",
  "count": 1
}
```

```json
{
  "ok": true,
  "action_id": "local-001",
  "accepted": true,
  "message": "get iron_pickaxe 1"
}
```

초기에는 action whitelist를 좁게 둔다.

```text
허용 후보:
- get-item
- stop
- status
- inventory

나중 후보:
- goto
- follow
- equip
- give
- deposit
```

### 3.4 AltoClef command 실행 방식

초기에는 기존 command executor를 사용하는 방향을 우선한다.

```text
CommandExecutor.executeWithPrefix("get iron_pickaxe 1")
```

직접 task를 생성하는 방식은 나중에 필요할 때만 검토한다.

```text
초기 우선순위:
1. 기존 command 문자열 실행
2. 기존 Command class 재사용
3. 직접 Task 생성은 최후순위
```

이렇게 해야 `@get`, `TaskCatalogue`, crafting, smelting, mining, Baritone 경로가 그대로 유지된다.

### 3.5 제어 모드

bridge에는 최소 세 가지 제어 모드를 둔다.

```text
MANUAL
  사용자가 직접 조작하며 AltoClef 자동 작업이 없는 상태.

AI
  LAVI 명령에 따라 AltoClef가 플레이어를 조작하는 상태.

PAUSED
  자동 작업이 정지 또는 중단된 상태.
```

초기 구현에서는 제어 모드를 상태 응답에 포함하되, 키보드/마우스 입력 제어를 새로 만들지 않는다. 사용자 입력과 AltoClef 자동 조작 충돌 처리는 기존 ChatClef/AltoClef 동작을 확인한 뒤 최소 변경으로 다룬다.

## 4. LAVI Python 구현 단위

### 4.1 Minecraft bridge client

새 파일 후보:

```text
plugins/Minecraft/minecraft_core/minecraft_bridge_client.py
```

책임은 다음이다.

```text
- localhost bridge health check
- /v1/status GET
- /v1/inventory GET
- /v1/actions/current GET
- /v1/actions/get-item POST
- /v1/actions/stop POST
- timeout/retry/error normalization
```

새 heavy dependency는 추가하지 않는다. 가능하면 Python 표준 라이브러리 `urllib.request` 또는 이미 프로젝트에 있는 HTTP 유틸을 먼저 검토한다.

### 4.2 Minecraft config

새 파일 후보:

```text
plugins/Minecraft/config/minecraft_config.json
plugins/Minecraft/minecraft_core/minecraft_config.py
```

초기 config 초안은 다음 정도가 적절하다.

```json
{
  "bridge_host": "127.0.0.1",
  "bridge_port": 4316,
  "request_timeout_sec": 3,
  "minecraft_version": "1.20.1",
  "enabled": true
}
```

Player2가 4315를 사용하므로 LAVI bridge 기본 포트는 4316 후보가 안전하다.

### 4.3 Minecraft plugin facade

새 파일 후보:

```text
plugins/Minecraft/minecraft.py
```

책임은 다음이다.

```text
- config load
- bridge client 생성
- LAVI UI 또는 extension에서 호출할 단순 method 제공
- health/status/inventory/current-action/get-item/stop wrapping
```

StarCraft2 plugin처럼 UI/런타임 조립이 커지면 `minecraft_core` 아래 factory/service로 분리한다.

## 5. LAVI GameExtension 연결

새 파일 후보:

```text
app_core/extensions/minecraft_core/minecraft_game_extension.py
```

`GameExtensionInterface` 구현은 다음 계약을 따른다.

```text
name
start()
stop()
handle_command(command)
get_status()
```

초기 `handle_command`는 LAVI 내부 command dict를 bridge API로 넘기는 얇은 wrapper로 둔다.

예상 흐름은 다음이다.

```text
LAVI game extension command
-> MinecraftGameExtension.handle_command(...)
-> Minecraft plugin facade
-> MinecraftBridgeClient
-> ChatClef LaviBridgeServer
-> AltoClef CommandExecutor
```

## 6. 등록 작업

등록 수정 후보는 다음이다.

```text
modules.json
app_core/optional_module_manifest.py
app_core/composition_core/optional_plugin_composition_service.py
app_core/extensions/extension_composition_core/game_extension_composition_service.py
```

예상 등록 개념은 다음이다.

```text
module name: Minecraft
module path: plugins.Minecraft.minecraft
class name: Minecraft
capabilities: game_extension, minecraft
config path: plugins/Minecraft/config/minecraft_config.json
```

`modules.json`에는 기본적으로 `Minecraft: true` 또는 `Minecraft: false` 중 하나를 선택해야 한다. 초기 개발 중에는 `false`로 두고 수동 활성화하는 방식이 더 안전하다.

## 7. 테스트 계획

테스트는 작은 순서로 진행한다.

```text
1. ChatClef Gradle target 확인
2. ChatClef 1.20.1 jar 빌드
3. Minecraft Fabric client에서 mod load 확인
4. 원본 상태에서 @get iron_pickaxe 수동 확인
5. 원본 상태에서 stop/cancel 수동 확인
6. GET /v1/health 단독 확인
7. GET /v1/status 단독 확인
8. GET /v1/inventory 단독 확인
9. GET /v1/actions/current 단독 확인
10. POST /v1/actions/get-item item=oak_log, count=1 확인
11. POST /v1/actions/stop 확인
12. LAVI Python MinecraftBridgeClient 단독 확인
13. MinecraftGameExtension get_status 확인
14. LAVI에서 get iron_pickaxe 1 end-to-end 확인
```

Python 쪽 기본 문법 확인은 다음을 사용한다.

```bat
python -m py_compile plugins\Minecraft\minecraft.py
python -m py_compile plugins\Minecraft\minecraft_core\minecraft_bridge_client.py
python -m py_compile app_core\extensions\minecraft_core\minecraft_game_extension.py
```

단, Minecraft 실제 행동은 syntax check만으로 검증할 수 없다. Fabric client에서 runtime 확인이 필요하다.

## 8. 오류 처리 기준

초기 bridge와 LAVI client는 다음 오류를 명확히 구분해야 한다.

```text
- Minecraft client가 실행되지 않음
- bridge가 열려 있지 않음
- world/player가 아직 로드되지 않음
- 이미 task가 실행 중임
- action이 whitelist에 없음
- AltoClef command parse 실패
- action은 accepted 되었지만 task 수행 중 실패
- stop 요청 실패
- status/inventory/current action 읽기 실패
```

LAVI 쪽은 오류가 발생해도 전체 앱이 죽지 않도록 `get_status()`와 `handle_command()`에서 실패를 dict 형태로 반환해야 한다.

## 9. 유지보수 기준

폴더와 책임은 다음처럼 유지한다.

```text
plugins/Minecraft/runtime/
  ChatClef/Fabric/Java mod source only

plugins/Minecraft/minecraft_core/
  LAVI Python bridge/config/model only

app_core/extensions/minecraft_core/
  LAVI GameExtension wrapper only
```

피해야 할 구조는 다음이다.

```text
- Minecraft 관련 Python code를 app_composer.py에 직접 추가
- ChatClef Java code와 LAVI Python code를 같은 폴더에 섞기
- Player2 code를 바로 삭제하면서 bridge 구현
- 여러 책임을 minecraft.py 한 파일에 몰아넣기
- 새 공용 utils 폴더를 근거 없이 만들기
```

## 10. 예상 수정 영향

초기 구현에서 기존 LAVI 파일 수정은 등록 계층에 한정하는 것이 좋다.

```text
낮은 위험:
- 새 plugins/Minecraft Python 파일 추가
- 새 app_core/extensions/minecraft_core 파일 추가
- optional module manifest에 Minecraft 추가

중간 위험:
- game extension composition service에 Minecraft 등록 추가
- modules.json에 Minecraft 항목 추가

높은 위험:
- AppComposer 흐름 직접 변경
- Player2 코드 삭제
- ChatClef task/command/core logic 수정
- Gradle dependency 버전 변경
```

높은 위험 항목은 구현하지 않는 방향을 기본값으로 둔다.

## 11. 단계별 완료 기준

2단계 완료 기준은 다음이다.

```text
- ChatClef bridge가 127.0.0.1에서 /v1/health 응답을 준다.
- LAVI Python client가 health/status/inventory/current-action/get-item/stop 호출을 할 수 있다.
- MinecraftGameExtension이 LAVI extension registry에 등록된다.
- get iron_pickaxe 1 명령이 기존 AltoClef command path로 전달된다.
- 실패 시 LAVI 앱이 죽지 않고 상태 dict를 반환한다.
```

실제 월드에서 iron pickaxe 획득까지 성공하면 2단계 구현은 기능적으로 완료된 것으로 본다.

첫 번째 수직 성공 기준은 다음으로 고정한다.

```text
LAVI 측 테스트 코드
-> POST /v1/actions/get-item
-> item=iron_pickaxe, count=1
-> ChatClef가 기존 AltoClef Task를 실행
-> 새 생존 월드에서 철 곡괭이 확보
-> succeeded 상태 반환
-> 최종 인벤토리에서 iron_pickaxe 확인
```

## 12. 다음 결정 필요 사항

구현 전에 사용자가 결정하면 좋은 항목은 다음이다.

```text
1. Minecraft module 기본값을 modules.json에서 true로 둘지 false로 둘지
2. bridge 기본 포트를 4316으로 확정할지
3. Java bridge HTTP 구현에 JDK 내장 HttpServer를 사용할지
4. ChatClef 1.20.1 빌드 타깃 전환을 어떤 방식으로 할지
5. LAVI UI에 Minecraft 전용 탭을 바로 만들지, 먼저 extension command만 붙일지
6. 제어 모드 MANUAL/AI/PAUSED의 UI 노출을 언제 할지
```

권장 기본값은 다음이다.

```text
Minecraft module default: false
bridge host: 127.0.0.1
bridge port: 4316
HTTP server: JDK built-in HttpServer 우선 검토
UI: 초기에는 최소 상태/명령 전송만
control mode: 초기에는 status에 포함하고 UI 제어는 나중에
```

## 13. 요약

2단계 구현은 다음 순서로 진행하는 것이 가장 안전하다.

```text
1. ChatClef 1.20.1 빌드 가능 여부 확정
2. ChatClef Java lavibridge 추가
3. bridge 단독 endpoint 검증
4. LAVI Python client 추가
5. Minecraft plugin facade 추가
6. MinecraftGameExtension 추가
7. LAVI 등록 파일 수정
8. end-to-end 테스트
```

이 계획은 LAVI와 ChatClef의 책임을 분리하고, 기존 AltoClef/Baritone 동작을 최대한 보존하는 방향이다.

## 14. 2026-07-25 Python 플러그인 구현 기록

LAVI 쪽 Minecraft 플러그인 bridge 1차 구현을 완료했다.

추가한 production 파일은 다음과 같다.

```text
plugins/Minecraft/__init__.py
plugins/Minecraft/minecraft.py
plugins/Minecraft/minecraft_core/__init__.py
plugins/Minecraft/minecraft_core/chatclef_bridge_client.py
plugins/Minecraft/minecraft_core/minecraft_config.py
plugins/Minecraft/minecraft_core/minecraft_facade_service.py
app_core/extensions/minecraft_game_extension.py
config/minecraft_config.json
config/minecraft_config.example.json
```

수정한 등록 파일은 다음과 같다.

```text
modules.json
config/modules.example.json
config/modules.core.json
app_core/optional_module_manifest.py
app_core/composition_core/optional_plugin_composition_service.py
app_core/composition_core/optional_plugin_composition_result.py
app_core/composition_core/app_ui_composition_service.py
app_core/extensions/extension_composition_core/game_extension_composition_service.py
app_core/extensions/extension_composition_core/game_extension_composition_result.py
```

초기 module 상태는 다음과 같다.

```text
modules.json: Minecraft=false
config/minecraft_config.json: enabled=true, allow_actions=true, bridge=http://127.0.0.1:4316
```

LAVI 쪽에서 지원하는 초기 command는 다음과 같다.

```text
health
status
inventory
current_action
get_item / get-item
stop / cancel
reload
```

검증 결과는 다음과 같다.

```text
py_compile: OK
ruff check for new Minecraft Python files: OK
unittest discover test_minecraft_plugin.py: OK, 5 tests
unittest discover test_optional_plugin_composition.py: OK, 2 tests
unittest discover test_app_composer.py: OK, 17 tests
unittest discover test_active_plugin_combination_smoke.py: OK, 3 tests
live Python health call to http://127.0.0.1:4316/v1/health: OK
```

## 14. 진행 로그

### 2026-07-25

2단계 시작 시 문서를 최신 handoff 기준으로 보강했다.

반영한 내용은 다음이다.

```text
- API prefix를 /v1로 고정
- read-only bridge를 먼저 만들고 action endpoint를 뒤에 붙이는 순서로 정리
- LaviActionRegistry 책임 추가
- MANUAL / AI / PAUSED 제어 모드 추가
- 첫 번째 수직 성공 기준을 POST /v1/actions/get-item으로 고정
- ChatClef 1.20.1 빌드 타깃 정적 확인 결과 추가
```

Gradle 프로젝트 확인을 위해 다음 명령을 시도했다.

```bat
.\gradlew.bat projects --no-daemon
```

결과는 실패다.

```text
distributionUrl=https://services.gradle.org/distributions/gradle-8.8-bin.zip
failure: javax.net.ssl.SSLHandshakeException
reason: PKIX path building failed / unable to find valid certification path to requested target
```

전역 `gradle` 명령도 현재 PATH에서 발견되지 않았다. 따라서 다음 진행을 위해서는 Gradle wrapper 다운로드 인증서 문제를 해결하거나, Gradle 8.8 distribution을 안전하게 준비한 뒤 다시 `projects` 또는 `:1.20.1:tasks` 확인을 진행해야 한다.

사용자가 다음 경로에 Gradle 8.8을 준비했다.

```text
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\tools\gradle-8.8
```

다음 명령으로 Gradle 8.8 실행 자체는 성공했다.

```bat
set GRADLE_USER_HOME=C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\gradle_user_home
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\tools\gradle-8.8\bin\gradle.bat --version
```

확인 결과는 다음이다.

```text
Gradle 8.8
JVM: 17.0.19 Eclipse Adoptium
OS: Windows 11 amd64
```

이후 같은 Gradle로 프로젝트 확인을 다시 시도했다.

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\tools\gradle-8.8\bin\gradle.bat projects --no-daemon
```

Gradle wrapper 다운로드 문제는 우회됐지만, 다음 build plugin resolution 문제로 실패했다.

```text
Plugin [id: 'fabric-loom', version: '1.7-SNAPSHOT', apply: false] was not found
searched coordinate: fabric-loom:fabric-loom.gradle.plugin:1.7-SNAPSHOT
searched repositories: maven.fabricmc.net, Maven Central, Gradle Plugin Portal, jitpack.io
```

추가로 Gradle은 `settings.gradle.kts`에 포함된 일부 project directory가 실제로 없다고 경고했다.

```text
missing project directories:
- versions/1.20.1
- versions/1.21
- versions/1.18
- versions/1.19.4
- versions/1.17.1
- versions/1.20.4
- versions/1.20.5
```

다음 조치는 `root.gradle.kts`의 Fabric Loom snapshot 버전을 안정 버전으로 고정할지 검토하는 것이다. Fabric Maven에는 `1.7-SNAPSHOT`과 안정 버전 `1.7.4`가 모두 존재하므로, 최소 후보 변경은 다음이다.

```text
root.gradle.kts
id("fabric-loom") version "1.7-SNAPSHOT" apply false
-> id("fabric-loom") version "1.7.4" apply false
```

이 변경은 build dependency version 변경이므로 사용자 승인 후에만 적용한다.

이후 사용자가 Temurin JDK 21을 설치하고 현재 PowerShell 세션의 `JAVA_HOME`/`Path`를 JDK 21로 설정했다.

확인 결과는 다음이다.

```text
openjdk version "21.0.11" 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-21.0.11+10
```

JDK 21 적용 후 다음 명령이 성공했다.

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\tools\gradle-8.8\bin\gradle.bat projects --no-daemon
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\tools\gradle-8.8\bin\gradle.bat :1.20.1:tasks --no-daemon
```

확인된 내용은 다음이다.

```text
- Gradle projects 성공
- :1.20.1:tasks 성공
- Root project altoclef 아래 :1.20.1 project가 인식됨
- :1.20.1 build, remapJar, runClient 등 Fabric task 확인됨
- Gradle 실행 중 비어 있던 versions/* project directory가 생성됨
- root.gradle.kts의 fabric-loom 1.7-SNAPSHOT은 현재 Loom 1.7.4로 해석되어 동작함
```

따라서 현재 시점에는 `root.gradle.kts`의 Fabric Loom 버전 변경이 필수는 아니다. 다음 단계는 JDK 21 세션을 유지한 채 `:1.20.1:build`를 실행해 실제 1.20.1 jar 생성 여부를 확인하는 것이다.

이후 사용자가 다음 명령을 실행했고 성공했다.

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation\tools\gradle-8.8\bin\gradle.bat :1.20.1:build --no-daemon
```

결과는 다음이다.

```text
BUILD SUCCESSFUL in 2m 20s
44 actionable tasks: 39 executed, 5 from cache
```

생성된 jar는 다음이다.

```text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23-all.jar
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23-sources.jar
```

`jar tf`로 확인한 결과 `chatclef-1.20.1-0.18.23.jar`와 `chatclef-1.20.1-0.18.23-all.jar` 모두 `fabric.mod.json`과 `adris/altoclef/AltoClef.class`를 포함한다. Gradle 설정상 `remapJar`가 `shadowJar`를 입력으로 삼으므로, Fabric 클라이언트에 넣어 수동 검증할 1차 후보는 classifier 없는 다음 파일이다.

```text
chatclef-1.20.1-0.18.23.jar
```

다음 단계는 이 jar를 Minecraft 1.20.1 Fabric client의 `mods` 폴더에 넣고, 원본 상태에서 `@get iron_pickaxe`와 stop/cancel을 수동 검증하는 것이다.

이후 ChatClef Java 쪽에 최소 LAVI bridge를 구현했다.

추가된 파일은 다음이다.

```text
src/main/java/adris/altoclef/lavibridge/MinecraftThreadDispatcher.java
src/main/java/adris/altoclef/lavibridge/LaviActionRegistry.java
src/main/java/adris/altoclef/lavibridge/LaviStateReader.java
src/main/java/adris/altoclef/lavibridge/LaviStopController.java
src/main/java/adris/altoclef/lavibridge/LaviCommandAdapter.java
src/main/java/adris/altoclef/lavibridge/LaviBridgeServer.java
```

`AltoClef.java`에는 `Settings.load(...)` 이후 `LaviBridgeServer`를 한 번만 시작하도록 최소 연결만 추가했다.

구현된 endpoint는 다음이다.

```text
GET  /v1/health
GET  /v1/status
GET  /v1/inventory
GET  /v1/actions/current
POST /v1/actions/get-item
POST /v1/actions/stop
```

`POST /v1/actions/get-item`은 새 제작/채굴 로직을 만들지 않고, 기존 AltoClef `CommandExecutor`를 통해 `@get <item> <count>` 경로를 사용한다. `POST /v1/actions/stop`은 기존 `AltoClef.stop()` 경로를 사용한다.

검증 결과는 다음이다.

```text
Gradle command: :1.20.1:build --no-daemon
Result: BUILD SUCCESSFUL in 1m 21s
Final jar: versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
Jar contains: adris/altoclef/lavibridge/* and adris/altoclef/AltoClef.class
```
