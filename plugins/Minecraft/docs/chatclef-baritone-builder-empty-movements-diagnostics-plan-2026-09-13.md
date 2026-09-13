<!-- 20260913_kpopmodder: Record the BuilderProcess empty-movement crash and a behavior-preserving diagnostics plan; this change is documentation only. -->
<!-- 20260913_kpopmodder: Review path transformations, metadata lifetime, layered admission, and actual file evidence without changing runtime code. -->

# Baritone BuilderProcess 빈 이동 목록 크래시 — 로그 보강 계획

## 1. 목적과 작업 범위

2026-09-13 16:28:23 KST에 발생한 `BuilderProcess.updateMovement()` 크래시에 대해 **빈 이동 목록이 언제 만들어지고, 어떤 경로 채택·교체를 거쳐 소비됐는지** 확인할 로그를 설계한다. 직접 예외와 소비 위치는 확인됐으며, 빈 목록의 생성·유입 원인은 아직 확인되지 않았다.

초기 문서화 이후 사용자의 **로그 보강만 구현** 요청에 따라 관측 코드를 추가했다. 확정한 구현 수치와 검증 상태는 [구현 기록](chatclef-resource-observation-implementation-2026-09-13.md)을 따른다. 진단이 명령 실행이나 복구 정책을 결정하지 않도록 한다.

| 항목 | 상태 |
| --- | --- |
| 현재 소스 검토 기준 | `dfe29ef8547ee85fa736b75b208b126fc73f0b8b` |
| 이번 변경 | 문서 및 후속 로그 전용 구현 |
| 이 계획의 로그 보강 구현 | `IMPLEMENTED` — 검증 상태는 구현 기록 참조 |
| 생산 코드·테스트·설정·Mixin 변경 | 최소 관측 hook·helper·focused 테스트·Mixin 등록 |
| 테스트·빌드 / 배포·새 런타임 | 구현 기록 참조 / `NOT_RUN` |
| 후속 소스 작업의 요청 범위 | 로그 보강만, 기존 동작 유지 |
| 직접 예외와 소비 위치 | 크래시 로그 및 현재 배포 파일의 바이트코드로 확인 |
| 빈 경로의 생성·유입 원인 | `UNKNOWN` |

적용 대상은 Fabric ChatClef 1.20.1이다. [AGENTS.md](../../../AGENTS.md), [백엔드 분리](minecraft-backend-separation.md), [ChatClef 연동 방향](chatclef-carryon-integration-direction.md), [Task 진단 기준](chatclef-task-lifecycle-diagnostics.md)을 따른다. 이 사건의 로그 전용 범위는 사용자의 선택이며, 다른 기능 구현에 일반적인 진단 선행 의무를 새로 부과하는 문서가 아니다.

빈 목록을 발견해도 실행을 건너뛰는 `return`, 경로 재계산·취소, fallback, Task 교체, 재시도·timeout 변경, 기존 예외를 삼키는 처리는 포함하지 않는다. 명령 결과·Chat/TTS·STOP 처리·기존 자동방어와 생존 우선순위도 유지한다. 캐시 삭제·초기화, 버전 변경, 게임 실행·중지·월드 조작, 원복·커밋·푸시는 이번 문서 작업에 포함하지 않는다.

## 2. 사건 근거와 보존 위치

### 2.1 로그 파일

실제 인스턴스는 `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01`이다. 요청의 `instance\_audit.txt` 표기는 실제 파일 `logs\instance_audit.txt`로 확인했다.

| 근거 | 확인한 내용 |
| --- | --- |
| [crash-2026-09-13_16.28.24-client.txt](C:/Users/jaewo/curseforge/minecraft/Instances/LAVI_TEST_Fabric01/crash-reports/crash-2026-09-13_16.28.24-client.txt) | 예외·스택·Minecraft/Java 버전·당시 플레이어 위치 |
| [2026-09-13-6.log.gz](C:/Users/jaewo/curseforge/minecraft/Instances/LAVI_TEST_Fabric01/logs/2026-09-13-6.log.gz) | 크래시 당시 `latest.log`가 재실행 후 보관된 파일; 압축 해제 기준 9,177행 |
| `logs/latest.log`, `logs/stdout-logs.txt` | 최초 조사 당시 각각 14,356,956 / 15,995,205 bytes로 실제 내용 확인. stdout에서 동일 예외와 `[EXIT] code=-1, terminatedByApp=false` 확인 |
| `logs/instance_audit.txt` | 최초 조사 당시 2,969,254 bytes. 2026-09-13 06:14:22 UTC의 Minecraft 1.20.1/Fabric 실행 기록 확인 |
| [LAVI 20260913_151413_log.txt](C:/Vtuber_Souorce_Code/LAVI/logs/20260913_151413_log.txt) | 케이크 요청과 16:28:25 연결 해제. 파일 수정 시각만으로 마지막 기록 시각을 판단하지 않고 내부 타임스탬프 확인 |

**재실행 후 `latest.log`·`stdout-logs.txt`는 새 세션 로그로 바뀌었고 audit에는 실행 이력이 추가됐다.** 새 `latest.log`·`stdout-logs.txt`의 행 번호를 과거 크래시 근거로 사용하지 않는다. 아래 Minecraft 행 번호는 보관된 `2026-09-13-6.log.gz`를 메모리에서 읽은 텍스트 기준이다. 원본 로그를 수정하거나 압축을 파일로 풀어 덮어쓰지 않았다.

보존 파일 SHA-256:

```text
crash-2026-09-13_16.28.24-client.txt (14,154 bytes)
50f38e23223357bd2205383a2d0b6adb1ec9e7c1243cb679916ec28eb02505a4

2026-09-13-6.log.gz (517,341 bytes; 압축 파일 자체 해시)
259bfbba473cdbd4b501b4db364b0bfa0ecbadc3d09d547f9a649c9575ae4d2f
```

### 2.2 확인된 시간 순서

| KST | 근거 | 확인 사실 |
| --- | --- | --- |
| 15:56:00 | LAVI 1122행 | 두 번째 `get cake 1` 실행 시작 |
| 16:27:18 | Minecraft 9030행 | 자동보관 후보 설치 기록 |
| 16:27:26 | Minecraft 9045–9046행 | `MOVEMENT_PROGRESS_FAILED`, 동등 후보 폐기·기존 실행 자식 유지 |
| 16:27:29 | Minecraft 9057–9059행 | 다시 이동 진행 실패, 기존 자식 stop 시작, 다른 후보 설치 |
| 16:28:19 | Minecraft 9136행 | 루트는 케이크 `CraftInTableTask`, 선택 체인은 `DepositAllInventoryPressureChain`, 완료 대기 중 |
| 16:28:23 | Minecraft 9137행 이후 | Render thread에서 `IndexOutOfBoundsException` 발생 |
| 16:28:24 | crash report / Minecraft 9169–9172행 | 보고서 생성, 서버 chunk 저장 완료 기록 |
| 16:28:25 | LAVI 1556–1557행 | `ConnectionClosedError: no close frame received or sent`, 해당 세션 연결 해제 |

연결에 사용한 사건 식별값:

```text
command: get cake 1
requestId: lavi-input-ko-865723c9909a42ab965daee6e33b77ce
correlationId: lavi-1aeb0ce4d31144c49a5e464a0a449226
sessionId: fabric-chatclef-31112fd1128b4040ae38cfc068705a5b
connectionGeneration: 1
boundRootIdentity: 1b07238d
```

자동보관의 실패·교체는 직전 상황이며 빈 경로의 생성 원인이라는 증거는 아니다. 크래시 시 선택된 체인이 과거에 그 경로를 만든 Task라는 보장도 없다. LAVI의 연결 오류는 Minecraft 예외 이후 관측됐다. ScreenVision의 화면 요약은 원인 판단 근거로 사용하지 않는다.

### 2.3 직접 예외와 파일 대조

```text
java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
  at baritone.process.BuilderProcess.updateMovement(BuilderProcess.java:801)
  at baritone.process.BuilderProcess.onTick(BuilderProcess.java:792)
  at baritone.process.BuilderProcess.onTick(BuilderProcess.java:601)
```

현재 배포 파일의 바이트코드에서 해당 소비는 다음과 같다.

```java
// 읽기 검토로 재구성한 기존 호출. 이 문서에서 코드를 추가한 것이 아니다.
exec.getPath().movements().get(exec.getPosition())
```

당시 **목록 길이 0에 인덱스 0을 적용**했다는 사실은 예외로 확인된다. `exec`는 해당 Baritone의 `PathingBehavior.getCurrent()`에서 얻은 local 객체다. Builder 전용 임시 executor를 새로 만드는 경계는 아니다.

선행 검사는 executor의 존재·`finished()`·`failed()`를 확인한다. 현재 `finished()`는 `pathPosition >= path.length()`이고 `IPath.length()`는 `positions().size()`를 사용한다. 따라서 좌표 목록 기준 완료 검사와 이동 목록 접근은 서로 다른 수를 사용한다. **사건 당시 실제 좌표 개수는 로그에 없으며 1이라고 가정하지 않는다.**

2026-09-13 문서 작성 시점의 파일 대조 결과:

| 대상 | SHA-256 |
| --- | --- |
| 인스턴스 `mods/chatclef-1.20.1-0.18.23.jar` | `4492eaa802cf900dcc2b5cc3f1817658691acac1e34d99f23a893b35a7202872` |
| processed·내장·로컬 비교 Baritone JAR | `807a467dbddede6769d9b666f176225694f8586d9237e0b62713fbda39fe0b27` |
| processed·내장 `baritone/process/BuilderProcess.class` | `415d91aa53f115fd521006a1942751bba9e2275d8e9ffc1a3be11545e728e0b3` |

비교 위치는 인스턴스 `.fabric/processedMods/baritone-1.10.1-9-geace2ad1-dirty-67ed795d8f1963fb.jar`, ChatClef 내부 `META-INF/jars/baritone-unoptimized-fabric-1.20.1.jar`, 로컬 runtime의 `versions/1.20.1/build/processIncludeJars/baritone-unoptimized-fabric-1.20.1.jar`다. 추출 파일 생성 없이 읽었다. 크래시 스택의 processed 파일명과도 일치한다.

이는 **문서 작성 시점의 디스크 파일과 바이트코드 대조 결과**다. 크래시 당시 메모리에 로드된 클래스의 해시를 별도로 수집한 것은 아니며, 이 파일 일치를 원인 해결이나 새 진단의 실행 증거로 해석하지 않는다.

## 3. 새 로그가 답해야 할 질문

1. 경로가 조립 완료될 때부터 이동 목록이 비었는가, 아니면 절단·연결·교체 뒤 다른 경로가 현재 경로로 들어왔는가?
2. 같은 path를 사용한 executor의 인덱스는 생성·채택·전환·소비 단계에서 어떻게 바뀌었는가?
3. 경로 요청 당시 Task·목적지와 소비 당시 Task·체인은 각각 무엇인가?
4. 자동보관 후보 교체·기존 취소·Builder 제어 상실과 이 경로 변경 사이에 실제 객체 연결이 있는가?
5. 보강 로그가 실행되지 않았는가, 관측했지만 상관관계를 잃었는가, 출력 제한 또는 파일 출력 실패로 누락됐는가?

경로 생성, 경로 선택, Task 성공/실패는 기존 소유자가 결정한다. 로그는 결정된 사실을 관측하며 새로운 실패 이유나 명령 결과를 만들지 않는다.

## 4. 보강 경계

아래 이벤트 이름은 제안이며 wire protocol 또는 현재 구현된 새 이벤트라는 뜻이 아니다. 기존 이벤트가 같은 책임을 이미 갖고 있으면 필드와 연결을 보완한다. 신규 Mixin의 정확한 descriptor·호출 ordinal·local capture·오류 경로는 후속 구현 시 대상 1.20.1 바이트코드와 맞춘다. 소스 줄 번호만으로 주입하지 않는다.

| 경계 | 현재 확인한 위치·재사용 후보 | 추가 관측과 목적 |
| --- | --- | --- |
| 요청·제어 시작/상실 | 실제 `build`/`clearArea`/goal 요청과 기존 `onLostControl`·취소 호출; 자동보관의 자식 선택·교체 경계 | 호출한 Task, 목적지, process identity, operation·후보·자식 세대, 기존 호출 전후 상태를 연결 |
| 경로 구성 | `Path` 생성, 기존 `BaritonePathBuildDiagnostics.logEnter/logReturn` | raw path identity, 시작·끝·좌표 수, 생성 단계. 조립 전 상태를 소비 오류와 구분 |
| 이동 단계 조립 | `Path.postProcess()`/`assembleMovements()`, 기존 `BaritonePathPostProcessDiagnostics.logEnter/logReturn` | 입력/반환 path identity, 조립 진입·정상 반환·예외 이탈 구분, 조회 가능한 시점의 이동 수 |
| 계산 중 경로 절단 | `AbstractNodeCostSearch.calculate()`의 `cutoffAtLoadedChunks()`·`staticCutoff()` | 절단 전후 path class/identity, 좌표·이동 수, 절단 범위와 반환 상태. 조립 반환 객체와 최종 계산 결과의 차이 추적 |
| 계산 결과 | `AbstractNodeCostSearch.calculate()`, 기존 `BaritonePathfinderLifecycleDiagnostics` | 계산 세대, 결과 종류, 실제 반환 path, 취소·실패·예외 여부와 조립 경계 연결 |
| 최초 채택 | `PathingBehavior`의 `findPathInNewThread` worker, 기존 `BaritonePathAdoptionDiagnostics.logBeforeClear` | 계산 결과가 `current`/`next` 중 어디에 채택됐는지, 폐기됐는지, `inProgress` 해제와 연결 |
| 실행 중 변환·교체 | `PathExecutor.trySplice()`·`cutIfTooLong()`, `PathingBehavior.tickPath()`의 next 승격·current/next 대입·제거 | `CutoffPath`/`SplicedPath`의 입력·출력, 새 executor 생성과 인덱스 이관 후 실제 채택 상태를 구분 |
| 실제 소비 | **`BuilderProcess.updateMovement()`의 `List.get(int)` 직전** | 실제 local executor·path·movement 목록·인덱스의 동일 관측, 기존 완료/실패 결과, 크기와 범위 관계 |

우선순위는 소비 지점의 정확한 snapshot, 생성·조립·채택·교체의 연결, Task/자동보관 연결 순이다. 소비 지점만 추가하고 생성·채택 경계가 연결되지 않은 상태를 원인 규명 완료로 기록하지 않는다.

### 4.1 소비 직전 관측

제안 이벤트 `BARITONE_BUILDER_MOVEMENT_ACCESS`는 실제 접근 대상의 `executorIdentity`, `pathIdentity`, `movementListIdentity`, `pathPosition`, `positionsCount`, `movementsCount`, 기존 `finished/failed` 관측을 담는다. 첫 범위 초과는 `BARITONE_BUILDER_MOVEMENT_ACCESS_INVALID` 같은 별도 진단 사유로 구분하되 기존 `get` 실행과 예외를 바꾸지 않는다.

`movementListIdentity`는 해당 `List.get` 호출의 실제 receiver인 view 객체의 일회성 식별값이다. 현재 `Path.movements()`는 `Collections.unmodifiableList(movements)`를 반환하므로 내부 목록이 같아도 조회마다 wrapper identity가 바뀔 수 있다. 이 값의 변화만으로 목록 교체를 판정하지 않는다. 장기 연결은 path/executor/계산 세대를 기준으로 하며, backing list identity가 필요하면 검증한 read-only 내부 필드 관측을 별도 이름으로 구분한다.

이미 선택한 local `exec`와 실제 호출에 쓰이는 목록·인덱스를 관측한다. 로그를 만들려고 전역 `current`를 다시 조회한 값을 실제 소비 객체라고 쓰지 않는다. 재조회 값이 필요하면 `observedCurrentAtCapture`처럼 별도 필드로 표시한다. 서로 다른 시점의 값으로 “목록과 인덱스가 일치했다”는 결론을 만들지 않는다.

실제 피연산자 캡처가 확인되지 않은 후속 구현은 관측 정확성 미확인으로 남긴다. 로그용 조회가 대상 목록을 조립·변경하거나, 기존 getter/실행 메서드를 추가 호출하여 실행 상태를 진행시키면 안 된다.

### 4.2 조립 전 빈 목록과 조회 불가

현재 `Path` 생성자는 빈 movement 목록으로 시작한다. `postProcess()`가 이동 단계를 조립한다. 조립 전 공개 `movements()`는 `Path not yet verified` 예외를 낼 수 있으므로, 조회 실패를 `movementsCount=0`으로 바꾸지 않는다.

`verified=true`는 조립 호출 **전에** 설정되므로 이 값만으로 정상 조립 완료를 주장할 수도 없다. 진입/정상 반환/예외 관측과 별도로 기록한다.

```text
RAW_CREATED: 아직 이동 조립 전일 수 있음
POST_PROCESS_ENTER: 조립 결과 미확정
POST_PROCESS_RETURN: 반환 객체·목록 관측
POST_PROCESS_EXCEPTION: 실제 원래 예외를 관측했을 때만 기록
EXECUTOR_ADOPTED / EXECUTOR_REPLACED: 실행 대상으로 채택된 실제 객체
BUILDER_MOVEMENT_ACCESS: 실제 목록·인덱스 소비
```

공개 조회 불가, 관측 실패, 아직 조립 전, 실제 빈 목록을 각각 구분한다. 내부 필드 접근이 필요하면 정확한 필드·read-only 접근 방식을 먼저 확인한다. 원래 조립 함수를 진단 목적으로 다시 호출하지 않는다. 예외 이탈 로그를 구현하더라도 원래 예외의 종류·객체·전파를 보존하고 정상 반환으로 전환하지 않는다.

### 4.3 기존 진단의 연결 보완

현재 [자동보관 Baritone 요약](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/container/store/deposit/event/terminal/StoreDepositTerminalEventFields.java)은 `baritoneContextImplemented=false`, `baritone_generation_context` 미구현을 명시한다. Task 로그의 양을 늘리는 것만으로 이 연결이 생기지는 않는다.

기존 계산·조립·채택 진단과 [executor snapshot](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritoneExecutorDiagnosticSnapshot.java)을 재사용하되, 단순 `pathSummary` 대신 숫자·객체 식별·조회 상태를 구조화한다. helper가 존재한다는 사실과 실제 주입·호출·출력 성공은 따로 확인한다. 연결을 구현하지 않은 상태에서 기존 coverage 필드를 `true`로 바꾸지 않는다.

현재 [CalculationDiagnosticRegistry.complete()](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/calculation/CalculationDiagnosticRegistry.java)는 raw/postProcess/result path의 연결을 제거한다. 따라서 채택 시 이 정리보다 **먼저** 필요한 불변 metadata를 현재/다음 경로의 제한된 진단 슬롯으로 넘겨야 한다. 소비 시점에 기존 registry를 다시 조회하는 것만으로 과거 계산을 복원할 수 없다. `recordFor()`로 새 세대를 만들어 과거 세대인 것처럼 붙이거나 기존 전역 registry를 무기한 유지하지 않는다.

### 4.4 절단·연결과 인덱스 이관

변환 로그는 입력·출력의 `pathClass`, identity, 좌표/이동 수와 조회 상태를 함께 기록한다. `CutoffPath`는 원본과 절단 범위·보정량을, `SplicedPath`는 **두 입력 경로**와 출력을 연결한다. 하나의 `parentPathIdentity`로 연결 경로의 두 출처를 덮어쓰지 않는다.

`trySplice()`가 호출되거나 반환했다고 연결 성공으로 기록하지 않는다. 같은 객체 유지, 연결 안 됨, 절단, 실제 연결을 기존 분기와 반환 객체로 구분한다. 새 `PathExecutor`는 생성 후 기존 인덱스를 복사하거나 절단량만큼 보정하므로, 생성자 직후의 기본 인덱스와 이관 완료 후 채택된 인덱스를 별도 단계로 기록한다. 문서 작성 시 대조한 바이트코드의 splice `617→618`, cutoff `636→637`행은 이 구분의 근거이며 주입 식별자는 아니다. 동일 executor identity를 유지한 인덱스 변경도 실제 소비값과 연결한다.

## 5. 공통 상관관계와 snapshot 계약

| 정보 묶음 | 필요한 필드 |
| --- | --- |
| 사건·순서 | diagnostic session/activation, 단조 증가 event sequence, 발생 thread, client tick 또는 unavailable, monotonic capture 시각 |
| 명령·Task | request/correlation/session/connection generation, root·실제 요청 Task identity, operation·후보·route-child identity |
| 요청 당시 상태 | 요청 Task·원래 목적지·월드/차원·process identity를 경로 요청 시점에 결합 |
| 소비 당시 상태 | 당시 root·선택 체인·현재 process·플레이어 위치, 요청 당시 정보와 다른 필드로 보존 |
| 계산·경로·실행기 | calculation generation, 원본/변환 path class·identity, 두 splice 입력 또는 cutoff 원본·범위, current/next executor identity, 소비 view의 일회성 identity, 생성/이관/채택 단계와 변경 전후 index |
| 목록·목표 | positions/movements count, 각 값의 조회 상태, goal 종류·제한된 목적지 요약, 시작·끝 위치 |
| 관측 완전성 | capture status, 연결 근거/미연결 이유, coverage, 누락·예산·eviction 수와 이유 |

비동기 계산 worker에서 “지금 활성 명령”을 읽어 과거 요청의 소유자로 붙이지 않는다. 요청 시 확보한 불변 상관관계를 계산 세대·결과 path에 전달하고, 알 수 없으면 `UNBOUND`로 남긴다. 연결에 Task/월드 전체 객체를 장기 보관하지 않는다.

identity 문자열의 우연한 재사용을 피하도록 진단 세션·계산/전환 세대와 함께 해석한다. 로그 순서만 보고 객체의 인과관계를 추정하지 않는다. 단일 원본 변환은 `parentPathIdentity`, 두 경로 연결은 두 입력 identity를 보존하고 `replacementReason`을 함께 기록한다. 읽기와 출력 사이에 바뀔 수 있는 mutable 상태는 snapshot에 동결하고 formatter에서 다시 조회하지 않는다.

새 연결 상태는 Baritone 인스턴스와 diagnostic activation generation에 귀속한다. 진단 활성화 확인 뒤 제한된 metadata/최근 이력을 **출력 admission 전에** 갱신하고 snapshot을 동결한다. 기존 lazy fields supplier는 예산 소진 시 호출되지 않을 수 있으므로, 그 안에서만 이력을 갱신하면 안 된다. supplier/formatter는 이미 동결된 값만 읽는다.

OFF 전환·activation 교체·인스턴스/월드 teardown 때 해당 진단 상태를 해제한다. 늦게 끝난 worker는 시작 때 캡처한 activation generation이 현재와 일치할 때만 같은 owner를 갱신하며 새 activation에 과거 기록을 재삽입하지 않는다. worker와 Render thread 사이의 진단 registry 갱신·snapshot 복사는 필요한 짧은 진단용 동기화로 묶고, 게임 getter 호출과 출력은 그 lock 밖에서 수행한다. 엔진 상태 전체를 잠그거나 기존 pathing lock 순서를 바꾸지 않는다. 여러 엔진 필드를 관측한 snapshot을 전역적으로 원자적인 월드 상태라고 주장하지 않는다.

## 6. 로그 출력·예산·동작 보존

기존 boundary 진단과 로그 파일 경로를 활용하며 별도 무제한 logger·worker·통신 채널을 만들지 않는다. 일반 반복 로그가 핵심 사건의 출력 예산을 소비하지 않도록 세 범위를 분리한다.

1. **필수 최초 경계:** 조립 결과, 계산 결과, current/next 채택, 경로 교체·제거, 소비 이상을 owner + event + 의미 있는 전환/결과로 식별한다. 무작위 ID만 다른 반복과 구분한다. 최초 범위는 같은 Baritone 인스턴스의 diagnostic activation이며, 해당 최초 경계와 최초 소비 이상을 일반 detail 상한과 별도로 출력하도록 예약한다. 아래 admission 연결과 파일 검증 전에는 보장 상태를 `NOT_VERIFIED`로 둔다.
2. **반복 detail:** 같은 상태는 요약하고 기존 기준인 200 game ticks 또는 10초보다 자주 반복 요약하지 않는다. 경로당 전 tick snapshot을 출력하지 않는다.
3. **최근 전환 보존:** 장시간 실행 후 발생한 이상도 추적할 수 있도록 현재/다음 경로의 생성·조립·채택 metadata와 제한된 최근 전환을 유지한다. 최초 이상 로그를 먼저 출력하고 관련 최근 이력을 제한된 크기로 뒤따라 출력한다. 크래시 뒤의 종료 callback에만 의존하지 않는다.

후속 구현에서 사용할 초기 상한 제안은 다음과 같다. 기존 공유 진단 admission과 대조해 더 엄격한 제약이 있으면 그 범위 안에서 수치와 계산식을 확정하고 문서·검증을 함께 갱신한다.

현재 [BaritoneDiagnosticEmitter](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritoneDiagnosticEmitter.java)는 일반 `emitLazyWithFallback` 경로를 사용한다. 채굴 진단의 local budget과 `ChatClefDiagnostics`의 shared admission은 별도 제한이며, 기존의 critical reserve가 새 Builder 이벤트를 위해 확보된 전용 용량은 아니다. helper 재사용이나 fallback key만 추가해서 최초 이상 출력이 보장됐다고 기록하지 않는다.

후속 구현에서는 유한한 최초 signature 목록, 실제 event-family 분류, local/shared 예약 소유자와 필요한 서브쿼터를 함께 확정한다. 일반 detail·반복 요약·history dump·다른 operation이 최초 소비 이상 예약을 소비하지 못하게 연결한다. 일반 상한뿐 아니라 양쪽 shared/critical 예산 소진 상황까지 검증한다. 예약을 얻으려고 비종료 이벤트를 `terminal=true`로 위장하거나 새로운 fallback key/activation을 매번 만들지 않는다. 출력 실패 재시도는 무한 반복하지 않으며 시도·실제 출력 성공·미확보를 구분한다.

| 항목 | 초기 제안 |
| --- | --- |
| 새 이벤트 직렬화 크기 | 이벤트당 최대 8 KiB, 핵심 identity/개수/관측 상태 우선 |
| 자유 형식 문자열 | 값당 최대 256자; 전체 경로 배열·인벤토리·raw 입력 제외 |
| 최근 전환 이력 | Baritone 인스턴스당 32개, 항목당 최대 2 KiB의 불변 값만 보관 |
| 추가 계산 상관관계 보관 | 인스턴스당 최대 64개, 항목당 최대 2 KiB; 현재/다음 경로 metadata는 같은 크기의 별도 고정 슬롯 2개로 유지 |
| 일반 detail 물리 출력 | 진단 operation당 최대 64개; 이후 요약. operation 미연결 시 같은 세션의 제한된 fallback scope 사용 |
| 필수 경계·최초 이상 | 인스턴스/activation의 유한 signature별 최초. local/shared 양쪽의 명시적 예약과 서브쿼터는 구현 때 확정·검증 |

정상 종료 시 더 이상 사용하지 않는 진단 상태를 정리하되, 아직 current/next executor가 참조하는 경로의 출처를 명령 종료만으로 없애지 않는다. 이 경우 원래 요청을 종료된 출처로 표시한 제한된 metadata만 경로 제거 또는 activation 종료까지 유지한다. OFF·세션/월드 교체의 해제 규칙은 5절을 따른다. 보관 한도 초과나 연결 정보 소실은 `coverageComplete=false`와 명시적인 이유로 기록한다. eviction된 정보가 필요했던 사건을 인과관계 완전 확인으로 보고하지 않는다.

최초 소비 이상은 메모리 buffer나 큐 등록 성공만으로 기록 완료로 보지 않는다. 기존 출력 경계를 통해 사건 직전에 남는지 검증한다. 현재 `logBoundaryWithPhysicalOutcome()`의 성공은 진단 emission 경계의 완료이며, `System.out.println` 뒤 appender/launcher의 파일 저장 완료까지 증명하지 않는다. **진단 admission, emitter/sink 호출 완료, 하위 큐 등록, 실제 파일에서의 관측**을 구분하고, 파일 관측 여부는 사후 검증 전 `UNKNOWN`으로 둔다.

payload cap, dedup, local/shared admission, observer 오류, 출력 실패를 각각 구분한다. 원래 예외 뒤에만 실행되는 종료 callback을 최초 이상 기록의 유일한 경계로 쓰지 않는다. 파일 증거를 보장한다는 이유로 원래 소비를 막고 flush/drain·파일 polling·별도 동기 대기를 추가하지 않는다. 파일 출력 자체가 실패하면 증거 미확보로 보고하며, 이를 이유로 게임을 중단·대기·재시도시키지 않는다.

동작 보존 검수에는 다음을 포함한다.

- 기존 반환값·분기·Task 선택·timeout·재시도 횟수·목표·입력·경로 취소와 호출 순서를 유지한다.
- 빈 목록을 정상 취급하거나 가짜 movement를 넣지 않는다. 소비 예외를 catch해서 성공·실패 응답으로 변환하지 않는다.
- 진단 코드의 오류만 격리하며 원래 코드의 예외는 그대로 전파한다. 진단용 lock을 보유한 채 게임/Baritone 실행 함수를 호출하지 않는다.
- 진단 활성화 여부나 로그 예산이 gameplay 동작의 조건이 되지 않는다. 기존 자동방어·회피·반격·생존 정책을 변경하지 않는다.
- 기존 bridge wire 키·상태·Python DTO와 Chat/TTS 의미를 유지한다. 추가 필드는 log-only다.

## 7. 파일·폴더 책임과 구현 연결 후보

새 코드는 진단 수집·상관관계 저장·출력 책임을 분리한다. 기존 upstream 클래스 이동·rename·분해나 Baritone 전체 소스 fork는 포함하지 않는다. 아래는 초기 설계 후보이며 실제 구현 위치는 구현 기록에서 확인한다.

| 책임 | 기존 또는 제안 위치 |
| --- | --- |
| 소비 직전 얇은 관측 연결 | 기존 1.20.1 Mixin 구성에 맞춘 `BuilderProcess` 진단 연결부; 정확한 주입 방법은 바이트코드로 확정 |
| 소비 snapshot·범위 관계 표시 | 새 `lavi/minecraft/diagnostics/baritone/builder/` 아래 snapshot/observer |
| 경로 전환 연결 | 기존 `diagnostics/mining/baritone/calculation/` helper와 별도 executor 전환 observer |
| 제한된 identity·최근 이력 | 새 `diagnostics/baritone/correlation/` 또는 기존 연결 저장소의 focused 확장 |
| 필드 형식·예산·물리 출력 | 새 책임별 diagnostic emitter/admission 협력자, 기존 공통 진단 출력 재사용 |
| 자동보관 연결 | 기존 `diagnostics/container/store/deposit/`의 operation/candidate/route-child 진단과 경로 요청 연결 |

재사용할 실제 파일:

- [BaritonePathCalculationDiagnostics](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritonePathCalculationDiagnostics.java): 계산·조립·채택·취소 진단 facade.
- [BaritonePathBuildDiagnostics](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/calculation/BaritonePathBuildDiagnostics.java): 생성 진입·반환.
- [BaritonePathPostProcessDiagnostics](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/calculation/BaritonePathPostProcessDiagnostics.java): 조립 진입·반환.
- [BaritonePathAdoptionDiagnostics](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/calculation/BaritonePathAdoptionDiagnostics.java): 계산 결과와 current/next 채택 연결.
- [PathingBehaviorDiagnosticMixin](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/mixins/diagnostics/PathingBehaviorDiagnosticMixin.java): 기존 진단 연결 방식 참고. 존재만으로 새 전환의 주입 성공을 가정하지 않음.
- [BaritoneDiagnosticEmitter](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritoneDiagnosticEmitter.java): 기존 bounded 출력과 fallback 연결 확인.

한 LAVI 파일에 독립 책임이 둘 이상 생기면 후속 구현 작업 안에서 파일·폴더로 나누고 참조를 함께 갱신한다. 기존 helper가 채굴 폴더에 있다는 이유로 자동보관을 채굴 Task로 위장하거나, 관련 없는 진단 폴더 전체를 이동하지 않는다. 새 bridge·백엔드 공통 실행 계층이나 Forge placeholder도 만들지 않는다.

## 8. 후속 구현·검증 순서

1. 실제 대상 클래스·Mixin 설정·기존 helper 호출 경로를 대조하고 정확한 관측 hunk, 읽을 값의 순수성, 대상 artifact를 기록한다. 특히 `List.get`의 실제 피연산자와 path 변환 경계의 식별이 먼저다.
2. 소비 snapshot, 생성·조립·채택·교체 연결, Task/자동보관 연결을 책임별로 구현한다. 기존 판단을 그대로 통과시키는 logging-only diff인지 검수한다.
3. 필수 이벤트 signature와 예산·누락 집계를 구현한다. 사건 직전 물리 출력과 일반 반복 억제를 각각 검증한다.
4. focused 자동 검증 후 [Fabric 1.20.1 clean forced build](chatclef-fabric-build-verification.md)를 해당 구현 요청 범위에서 수행한다. Gradle 성공과 실제 Mixin 실행을 구분한다.
5. 배포·게임 검증이 요청된 범위에서는 활성 인스턴스 JAR 일치와 같은 요청/경로의 이벤트 연결을 확인한다. 새 로그로 확인한 생성·유입 원인을 기록하며, 크래시가 안 났다는 사실만으로 동작 수정이나 해결을 주장하지 않는다.

| 검증 | 확인할 내용 |
| --- | --- |
| 조립 단계 | raw 상태의 빈 목록/조회 불가와 postProcess 반환을 구분. getter 조회 실패를 0으로 바꾸지 않음 |
| 소비 | 빈 목록/index 0, 음수·범위 밖 index, 정상 목록을 구분하고 원래 접근 결과·예외를 보존 |
| 경로 전환 | current/next 채택, next 승격, splice, 제거·취소 전후 identity가 이어짐 |
| 절단·인덱스 이관 | 계산 단계 cutoff와 실행 단계 cutoff, 두 splice 입력, 같은 객체 반환, 새 executor 생성 후 index 복사/보정을 구분 |
| 목록 view | 같은 backing list의 새 wrapper를 경로·목록 교체로 오판하지 않고 실제 소비 receiver를 기록 |
| 상관관계 | 요청 교체·worker 지연·동일 종류 Task에서 과거 경로가 현재 명령으로 잘못 연결되지 않음 |
| 상태 수명 | 계산 registry 정리 전에 출처 이관, detail 상한 뒤에도 최근 이력 유지, OFF/activation 교체 뒤 늦은 worker 재삽입 방지 |
| 동작 불변 | diagnostics on/off, observer 예외, 예산 소진에서 기존 호출·반환·원래 예외·입력/취소 횟수가 같음 |
| 출력 | local/shared 일반·critical 예산 경합에서 전용 예약을 확인. emitter 성공을 파일 저장과 혼동하지 않고 실제 파일 관측·미확보를 별도 검증 |
| 구조·호환 | 책임별 분리, 기존 이벤트/bridge 의미 보존, Minecraft 1.20.1 대상 주입·remap 확인 |

위 표는 초기 검증 계획이다. 후속 구현·자동 검증·빌드와 배포/런타임의 실제 상태는 구현 기록에서 별도로 관리한다.

## 9. 남겨 둘 불확실성과 완료 기준

아직 모르는 것은 사건 경로의 실제 positions 수, 조립 결과, 채택·교체 이력, 경로를 만든 Task, 목록이 비게 된 최초 경계다. 현재 파일 해시 일치와 직접 예외 확인으로 이 항목들을 채우지 않는다.

[캐시 문제 및 과거 BlockOptionalMeta NPE](chatclef-baritone-cache-troubleshooting.md)는 별도 사건/가설이다. 같은 세션의 과거 NPE나 긴 경로 계산, 자동보관 실패가 있다는 이유만으로 이번 빈 목록의 원인으로 연결하지 않는다. 캐시 상태나 월드 복원 이력이 필요한 후속 분석에서는 실제 근거를 확인하며 수동 캐시 변경은 하지 않는다.

로그 보강의 완료 기준은 **같은 실행 경로의 생성·조립·채택·교체·소비와 Task 연결을 검증 가능한 범위로 남기고, 관측하지 못한 부분을 명시하며, 기존 gameplay 동작을 보존하는 것**이다. 원인 수정이나 크래시 방지는 이 logging-only 작업의 완료 주장에 포함하지 않는다.
