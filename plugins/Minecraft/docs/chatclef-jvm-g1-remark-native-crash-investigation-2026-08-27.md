<!-- 20260827_openai: Separated the recurring JVM G1 Remark native-crash investigation from STORE_HOME feature policy. -->
<!-- 20260827_openai: Applied the final PASS review for exact-artifact preservation, evidence scope, and controlled A-B execution. -->
<!-- 20260827_openai: Added persistent CurseForge GC evidence capture and mandatory per-run validation gates. -->

# ChatClef JVM G1 Remark Native Crash Investigation

문서 상태: `DIRECTION_PASS_EVIDENCE_CAPTURE_IN_PROGRESS`

작성 기준일: 2026-08-27

최종 1회 방향성 검수: `PASS`

추가 방향성 검수 없이 JVM evidence 확보와 A/B 검증 단계로 진행할 수 있다. 이
판정은 아래 작업의 방향을 승인한 것이며, build, JVM option 변경, dump 설정,
Minecraft 실행, source 수정, commit 또는 push의 실행 승인은 아니다.

## 1. 목적과 승인 경계

이 문서는 2026-08-27 Minecraft 종료와 과거 native crash 기록을 근거로,
ChatClef build 검증과 JVM crash 조사를 분리하고 다음 검증 순서를 고정한다.

이 문서는 문서화만 수행한다. 다음 작업을 승인하지 않는다.

- Java 또는 Python source 수정
- diagnostics source 추가
- Gradle build 또는 JAR 배포
- Minecraft 실행이나 crash 재현
- Java, JVM option, launcher, mod 또는 native component 변경
- commit 또는 push

각 작업은 별도 사용자 승인이 필요하다.

## 2. 현재 판정

현재 증거는 이번 crash가 H6 한국어 `STORE_HOME` 또는 clean build가 새로 만든
결함이라기보다, 2026-07-28부터 반복된 HotSpot G1 Remark native-crash 계열의
재발임을 강하게 시사한다.

다만 최신 crash 약 10.6초 전에 직접 `@store_home` 작업이 완료됐다. 따라서
`StoreHomeTask` workload가 해당 회차에서 기존부터 관찰된 native crash family를
노출한 trigger였을 가능성은 남아 있다. 다음 두 명제를 구분한다.

```text
H6 또는 StoreHome이 native crash의 root cause다: 현재 근거로 지지되지 않음
StoreHome workload가 해당 회차의 trigger일 수 있다: 가능, A/B 필요
```

`StoreHomeTask COMPLETED`는 해당 transfer의 기능 증거다. Minecraft JVM process의
장기 안정성이 검증됐다는 뜻은 아니다.

지금 우선순위는 동일 current source의 반복 build나 Java source logging 추가가
아니다. 먼저 JVM crash evidence를 확보하고, baseline/current JAR과 StoreHome
workload 유무를 한 변수씩 비교한다.

## 3. Build JDK와 1.20.1 target 구분

ChatClef multi-version project README는 development SDK로 Temurin 21을 사용했다고
기록한다. 유일하게 허용되는 필수 JDK라는 선언은 아니다. 이 조사에서는 artifact
사이의 build 환경을 통제하기 위한 reference build SDK로 Temurin JDK 21을 사용한다.
Gradle wrapper는 8.8이며, Gradle 설정은 Minecraft version에 따라 compile target을
나눈다.

```text
controlled reference build SDK: Temurin JDK 21
Minecraft 1.20.1 source/target/release: Java 17
Minecraft 1.20.6 이상 source/target/release: Java 21
Minecraft 1.20.1 game runtime: Java 17
```

따라서 정상 경계는 `JDK 21로 Gradle 실행 -> Java 17 호환 1.20.1 JAR 생성`이다.
전체 multi-version build를 JDK 17로 실행하면 Java 21 target 때문에 실패할 수 있다.
이 실패는 1.20.1 JAR이 Java 17에서 실행될 수 없다는 증거가 아니다.

2026-08-27 current artifact는 clean forced build, test, source/deployed hash 일치와
runtime Mixin/linkage 확인을 통과했다. 동일 source를 같은 방식으로 한 번 더
빌드하는 것만으로는 native crash 원인에 관한 새 증거를 거의 만들지 못한다.

## 4. 직접 확인된 증거

### 4.1 Build와 artifact

```text
build command:
  .\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon
  --stacktrace --offline

result: BUILD SUCCESSFUL
focused 1.20.1: 156 tests, 0 failures, 14 skipped
multi-version JUnit XML aggregate: 1760 tests, 0 failures, 0 errors, 42 skipped
artifact: chatclef-1.20.1-0.18.23.jar
size: 6,853,544 bytes
SHA-256: D2ABB0C13186D825170707D826CC1023B892F0EF5A1038B8122853868393B61F
source/deployed hash: identical
relevant Mixin/linkage error signatures: 0
```

focused 수치와 aggregate 수치는 집계 범위가 다르므로 충돌하지 않는다. 이 증거는
build output과 active instance JAR이 동일하고, 알려진 compile/test/Mixin 경계에서
실패하지 않았음을 뜻한다. 모든 runtime behavior나 JVM native safety를 증명하지는
않는다.

### 4.2 2026-08-27 command와 crash timeline

```text
20:50:34~20:51:02
  Korean STORE_HOME 입력 4회
  translation_status=validated
  result_status=rejected
  minecraft_command_routed=0

20:51:19
  Minecraft에서 직접 StoreHomeTask 시작
  active LAVI request 없음

20:51:27
  StoreHomeTask COMPLETED
  storedItems=180
  touchedStacks=31
  remainingStacks=0

20:51:37.817
  JVM native crash
```

한국어 입력 네 번은 Java `StoreHomeTask`를 시작하지 않았다. 직접 명령은 실제로
실행됐지만 active LAVI request가 없어서 새 typed result projector도 실행되지 않았다.

### 4.3 Native crash signature

최신 crash에서 확인된 경계는 다음과 같다.

```text
exception: EXCEPTION_ACCESS_VIOLATION (0xc0000005)
thread: GCTaskThread "GC Thread#13"
problematic frame: jvm.dll+0x31a47c
invalid read: 0x00000000000000c0
VM state: at safepoint
unfinished VM operation: G1PauseRemark
Java application exception: 없음
OOM or physical-memory exhaustion evidence: 없음
```

동일 instance의 확인된 hs_err 다섯 건은 모두 `GCTaskThread`, `jvm.dll` native
access violation과 `G1PauseRemark`를 공통으로 가진다. 이 중 네 건은 H6와 최신
build보다 먼저 발생했다. Microsoft OpenJDK와 Temurin은 모두 HotSpot 계열이므로
두 vendor 기록만으로 서로 독립적인 VM 구현 두 개에서 재현됐다고 해석하지 않는다.

### 4.4 첫 current screening과 JVM 인수 전달 확인

`current-exact-01`은 crash 당시 exact current JAR로 직접 `@store_home` happy path를
실행한 뒤 약 20.35분 관찰했다. 실행 중 `jcmd`로 GC logging을 활성화해 Young GC
112회를 확보했지만 G1 Remark, Concurrent Mark와 Full GC는 모두 0회였다. post-GC
heap, old region과 metaspace에서 Java heap leak을 지지하는 지속 증가도 관찰되지
않았다. 이 실행은 historical crash boundary인 Remark를 노출하지 못했으므로
`NO_CRASH_WITHOUT_REMARK_EXPOSURE`, native 원인은 `INCONCLUSIVE`다.

```text
run: logs/jvm-crash/runs/current-exact-01/
GC capture: dynamic jcmd, startup 이전 구간 없음
G1 Young cycles: 112
G1 Remark / Concurrent Mark / Full GC: 0 / 0 / 0
StoreHome: COMPLETED, storedItems=180, remainingStacks=0
```

`startup-validation-01`에서는 `launcher_profiles.json`에 `-XX:ErrorFile`과
`-Xlog:gc*,safepoint`를 넣고 Overwolf까지 cold restart했지만, 실제 Minecraft
`javaw.exe` command line에는 두 인수가 모두 없었고 GC와 error 디렉터리도 비어
있었다. CurseForge는 자체 `BuildLaunchOptionsAction`으로 인수를 구성하며, 실제
`minecraft-settings.additionalJavaArguments`에는 기존
`-Dlavi.chatclef.diagnostics=boundary`만 저장돼 있었다.

따라서 이 실행은 `FAILED_ARGUMENT_DELIVERY`이며 A/B 횟수나 no-crash 증거로 세지
않는다. `launcher_profiles.json` 직접 수정은 이 CurseForge 실행 경로의 JVM 인수
설정 방법으로 사용하지 않는다. Overwolf Chromium LevelDB를 직접 편집하지도 않는다.

```text
run: logs/jvm-crash/runs/startup-validation-01/
workload executed: false
actual -XX:ErrorFile: absent
actual -Xlog:gc*,safepoint: absent
classification: FAILED_ARGUMENT_DELIVERY
```

## 5. 증거 등급

### PROVEN

- 최신 종료는 Java exception이나 Fabric Mixin failure가 아니라 JVM native crash다.
- 한국어 `STORE_HOME` 네 요청은 Java command로 제출되지 않았다.
- current source JAR과 deployed JAR의 SHA-256은 일치한다.
- 같은 G1 Remark native-crash family가 H6와 current build 이전에도 존재했다.
- crash 시점에 heap 또는 물리 메모리가 고갈됐다는 증거가 없다.

### STRONGLY INDICATED

- 최신 crash는 기존 G1/native/runtime environment fault family의 재발이다.
- H6 typed projection과 clean Gradle build는 직접 root cause가 아니다.

### POSSIBLE

- 두 번째 직접 StoreHome workload가 최신 회차에서 기존 crash family를 노출한
  timing/allocation trigger였다.
- `-Xmx125728m`가 heap geometry를 바꾸는 amplifier였다.
- 오래된 HotSpot patch, 외부 native module, CPU/RAM 불안정 또는 이들의 조합이
  underlying fault에 관여했다.

### NOT SUPPORTED

- 한국어 입력이 crash 직전 Java `StoreHomeTask`를 실행했다.
- typed result projection이 crash 직전 payload를 생성했다.
- Gradle이 생성한 JAR이 `jvm.dll`을 손상시켰다.
- AVG, NVIDIA, Overwolf 또는 Korean patch 중 특정 하나가 현재 증거만으로 범인이다.
- `-Xmx125728m`가 단독 root cause다.
- WHEA event가 없으므로 CPU와 RAM이 정상임이 증명됐다.

## 6. 지금 수행할 진단 우선순위

### 6.1 현재 상태 보존

- crash 당시 deployed JAR, SHA-256, Java runtime binary와 launcher argument를 보존한다.
- 첫 current arm은 exact JAR
  `D2ABB0C13186D825170707D826CC1023B892F0EF5A1038B8122853868393B61F`을 사용한다.
- `public_korean_enabled=false`를 유지한다.
- 원인 비교 전에 behavior source나 diagnostics source를 변경하지 않는다.
- dirty worktree에서 baseline commit으로 destructive checkout하지 않는다.

### 6.2 JVM-level evidence 확보

다음 재현 전에 source log보다 아래 증거를 우선한다.

1. 같은 실행의 complete `hs_err_pid*.log`
2. Windows full user-mode dump 우선, 불가능하면 minidump
3. 정확히 같은 `javaw.exe`와 `jvm.dll`의 version 및 SHA-256
4. timestamp와 uptime이 포함된 GC, concurrent-marking, Remark와 safepoint log
5. 다음 항목을 포함한 run manifest

```text
test arm and run ID
Java vendor and exact patch
complete JVM flags, collector and Xmx
evidence-capture configuration
JAR SHA-256
mod and native-module inventory
world/inventory snapshot ID
StoreHome start/end timestamp and terminal
observed Remark exposure count
```

가능하면 같은 Java build의 symbols를 함께 확보한다. 현재 `jvm.dll+offset`만으로는
실제 HotSpot C++ function과 source line을 확정할 수 없다. JVM option이나 Windows
dump 설정 변경은 별도 승인과 사전 검증 후 수행한다.

#### 6.2.1 CurseForge 상시 GC evidence 설정 계약

CurseForge는 기본 상태에서 상세 GC, concurrent-marking, Remark와 safepoint log를
별도 파일로 저장하지 않는다. `latest.log`, `stdout-logs.txt`, CurseForge log와
`hs_err_pid*.log`는 이 unified GC log를 대체하지 않는다.

native crash 조사가 끝날 때까지 모든 CurseForge Minecraft 진단 실행은
`CurseForge 설정 > Minecraft > Additional Java Arguments(추가 Java 인수)`의 공식
입력 경계를 사용해 다음 두 인수를 지속 적용한다. 기존
`-Dlavi.chatclef.diagnostics=boundary`와 같은 입력란에 공백으로 구분해 추가한다.

```text
-XX:ErrorFile=C:/Vtuber_Souorce_Code/LAVI/logs/jvm-crash/live/errors/hs_err_pid%p.log
-Xlog:gc*,safepoint:file=C:/Vtuber_Souorce_Code/LAVI/logs/jvm-crash/live/gc/gc-safepoint-%t-%p.log:time,uptime,level,tags:filecount=5,filesize=64M
```

이 설정에는 다음 제한을 적용한다.

- `live/errors`와 `live/gc` 디렉터리는 Minecraft 시작 전에 존재해야 한다.
- CurseForge의 이 설정은 특정 instance가 아니라 모든 CurseForge Minecraft 실행에
  적용될 수 있다. manifest에 instance와 PID를 반드시 기록한다.
- `%t`와 `%p`로 실행별 파일을 분리한다. 동일 PID의 rotation은 64MB 파일 5개로
  제한하지만 여러 실행의 총 보존량은 자동 제한되지 않으므로 별도 retention 검사가
  필요하다.
- 정상 조사 모드는 `info` 수준의 `gc*,safepoint`다. `debug`, `trace`, NMT와 다른
  verbose 옵션을 이 상시 설정에 섞지 않는다.
- full process command line에는 launcher credential이 포함될 수 있으므로 저장하지
  않는다. 필요한 JVM option만 sanitize해 기록하고 전체 command line은 SHA-256만
  남긴다.
- `launcher_profiles.json`이나 Overwolf LevelDB를 직접 편집하지 않는다.
- root cause 조사 종료 후 이 상시 설정의 유지 여부는 별도 판정한다.

`%p`가 가리키는 exact GC log와 같은 PID의 `hs_err` 또는 dump를 run directory에
결합하고 hash를 manifest에 기록하기 전에는 해당 파일을 정리하지 않는다. 로그
정리는 증거 보존이 끝난 실행에만 수행하며 기존 증거를 자동 덮어쓰지 않는다.

#### 6.2.2 매 실행 필수 확인 gate

GC 파일 생성과 GC 분석은 서로 다른 단계다. 로그 생성은 JVM이 자동 수행하지만,
각 진단 실행의 유효성 확인과 종료 후 분석은 반드시 별도로 수행한다.

Minecraft 시작 직후, world 진입이나 `@store_home` 실행 전에 다음을 확인한다.

1. exact game PID와 game directory가 대상 instance와 일치한다.
2. sanitize한 실제 JVM option에 `-XX:ErrorFile`과
   `-Xlog:gc*,safepoint`가 모두 존재한다.
3. exact PID의 GC log가 생성됐고 0바이트가 아니며 startup uptime 구간을 포함한다.
4. collector, Java patch, Xms, Xmx와 ChatClef JAR SHA-256이 reference manifest와
   일치한다.
5. 하나라도 실패하면 workload를 실행하지 않고 `FAILED_ARGUMENT_DELIVERY` 또는
   `INVALID_RUN_CONFIGURATION`으로 종료한다.

각 실행 종료 후에는 다음 항목을 확인하고 run result에 기록한다.

```text
run duration and GC log time range
Young GC count and first/last GC ID
Concurrent Start, Concurrent Mark, Remark and Cleanup count
Full GC count and reason
maximum GC pause and maximum safepoint duration
first/last post-GC heap and old-region trend
metaspace and humongous-region trend
same-PID hs_err and dump presence
Remark exposure count
PASS / WARN / CRASH / INCONCLUSIVE classification
```

시작 로그가 없는 동적 `jcmd` capture는 부분 증거로 보존할 수 있지만 startup-complete
A/B 실행으로 세지 않는다. GC log가 없거나 Remark exposure가 0인 무크래시 실행도
root cause 반증으로 사용하지 않는다. 자동 요약 도구를 나중에 추가하더라도 이
문서의 증거 필드와 gate를 바꾸지 않으며, Java/Python runtime source 변경과 분리한다.

### 6.3 Source logging 보류 기준

지금 Java source에 로그를 추가하지 않는다. JVM이 GC worker에서 native crash하면
application log는 crash frame을 설명하지 못하며, 추가 allocation이 A/B timing을
바꿀 수 있다.

baseline/current A/B에서 current JAR에만 crash가 반복 집중되거나, 기존 로그로
구분할 수 없는 마지막 성공 경계와 첫 실패 경계가 드러난 경우에만 bounded
diagnostics-only source change를 검토한다. 그때도 다음을 지킨다.

- Task start, terminal, transfer summary 같은 boundary event만 기록
- unchanged per-tick 또는 per-slot polling log 금지
- return value, Task selection, retry, timeout, click, cursor, cleanup과 pathing 변경 금지
- behavior fix와 diagnostics를 같은 patch에 포함하지 않음

## 7. 최소 A/B 순서

모든 arm은 동일 world snapshot, player 위치, inventory, trusted container 내용,
mod list, background app 상태, Java runtime, JVM option, evidence-capture 설정과 실행
시각을 사용한다. 지정한 변수 하나만 바꾼다. 각 A/B는 고정된 reference
configuration을 명시하며, 한 단계에서 변경한 값을 다음 단계에 자동 누적하지 않는다.
실행 순서는 가능하면 `A-B-B-A`로 교차한다.

각 arm의 모든 run은 6.2.2 startup gate를 먼저 통과해야 한다. GC log 생성 실패,
실제 JVM option 불일치 또는 startup 구간 누락이 있으면 해당 run에서는 workload를
실행하지 않고 arm 횟수에서도 제외한다.

screening 기준은 arm별 5회, run당 최소 20분 또는 G1 Remark exposure 3회다. 같은
signature가 한 번이라도 나오면 arm별 10회까지 확대한다. G1 arm에서 Remark
exposure가 부족한 no-crash는 반증이 아니라 `INCONCLUSIVE`로 기록한다. 두 arm 모두
0회여도 무죄가 아니라 재현 실패다.

### A/B 1: baseline JAR과 current JAR

```text
A: commit 740aa616의 H1-H4 baseline JAR
B: crash 당시 exact current H6 JAR
   D2ABB0C13186D825170707D826CC1023B892F0EF5A1038B8122853868393B61F
```

baseline만 사용자 변경을 보존하는 별도 clean worktree에서 build한다. current
artifact를 만들 때 사용한 것과 동일한 Temurin JDK 21 vendor와 exact patch, Gradle
wrapper와 build command를 사용한다. current arm은 새로 build하지 않고 crash 당시
exact JAR을 보존해 사용한다. fresh current rebuild가 필요하면 첫 A/B 뒤 reproducibility
확인용 별도 세 번째 artifact로 취급한다.

artifact manifest에는 build JDK vendor/version, Gradle version, JAR size와 SHA-256,
Java 17 class target evidence를 기록한다. 두 arm은 동일 Java 17 runtime에서 같은
직접 `@store_home` workload를 실행한다.

```text
A와 B에서 비슷하게 crash: H6 Java diff 원인설 약화
B에만 반복 집중: current diff가 trigger 확률을 높였을 가능성
A에만 crash: current diff 원인설 기각 방향
둘 다 0: 재현 실패, 결론 보류
```

동일 current source의 반복 build보다 baseline artifact 하나를 통제된 환경에서
만드는 것이 현 단계에서 의미 있는 build다.

### A/B 2: StoreHome workload 유무

동일 exact current JAR로 control arm은 idle 또는 고정 workload를 수행하고, test
arm은 같은 시각에 직접 `@store_home`을 두 번 실행한다. test arm에 crash가 반복
집중될 때만 StoreHome core workload를 trigger 후보로 올린다. control arm에서도
G1 Remark exposure를 기록해 exposure 부족과 무크래시를 구분한다.

### A/B 3 이후

앞 단계 결과와 관계없이 한 번에 하나만 바꾼다.

1. 동일 vendor의 crash-run Java 17 patch와 현재 지원되는 최신 Java 17 patch
2. `-Xmx125728m`와 `-Xmx16g`
3. G1과 ParallelGC
4. diagnostics `off`와 `boundary`
5. Overwolf/GEP, NVIDIA overlay, Korean patch, AVG hook 등 native injection 순차 격리
6. suspect mod screening 후 단일 mod 제거 confirm
7. 별도 세션의 BIOS, microcode, Intel default profile, CPU와 RAM 안정성 검사

Java patch A/B에서 vendor를 함께 바꾸지 않는다. Java patch와 heap 크기를 동시에
바꾸지 않으며, 각 단계는 정해 둔 reference configuration으로 돌아가 시작한다.
정상 운용에는 125GB heap을 유지할 이유가 거의 없지만, 인과 검증에서는 변경 순서를
분리한다.

G1과 ParallelGC 비교에서 ParallelGC에 G1 Remark가 없는 것은 정상이다. 이 단계는
동일 workload와 관찰 시간, collector별 GC-cycle evidence로 비교한다. Native
component는 한 번에 하나씩 격리한다. Mod binary search는 screening에만 사용하고
단일 mod 제거 A/B로 확인한다. BIOS, microcode, XMP, CPU와 RAM도 하나의 변수로
묶지 않는다.

## 8. 판정 gate

```text
current JAR에만 동일 signature 반복
  -> current diff의 trigger 가능성 조사
  -> exact boundary의 bounded source diagnostics 검토 가능

baseline/current 모두 유사하게 반복
  -> H6와 build artifact 원인설 약화
  -> Java patch, heap, collector와 native environment 조사로 이동

StoreHome workload arm에만 반복
  -> core workload trigger 조사
  -> root cause로 표현하지 않음

모든 arm에서 재현 실패
  -> 해결 또는 무죄로 판정하지 않음
  -> run duration, Remark exposure와 evidence quality 기록

GC evidence startup gate 실패
  -> A/B 결과로 사용하지 않음
  -> FAILED_ARGUMENT_DELIVERY 또는 INVALID_RUN_CONFIGURATION
  -> 공식 CurseForge 설정 경계를 바로잡은 뒤 새 run ID로 다시 시작
```

H6 checkpoint source를 보존하는 것과 public release 준비 완료 선언은 별개다.
Matching-request typed terminal, chat/microphone parity와 failure/fallback runtime
matrix가 끝나기 전에는 `gameplay_effect_verifiable`과 public gate를 열지 않는다.

## 9. 관련 문서와 증거

- [Manual Trusted Home Storage Direction](chatclef-manual-trusted-home-storage-direction-2026-08-27.md)
- [Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)
- [ChatClef Task Lifecycle Diagnostics](chatclef-task-lifecycle-diagnostics.md)
- repository evidence bundle: `logs/share/chatgpt-minecraft-jvm-crash-review-20260827-205917/`

이 문서는 native crash 조사 순서에 한해 trusted-home 방향 문서보다 구체적이다.
`STORE_HOME` item policy, trusted destination, exact-slot transfer와 public activation
정책은 trusted-home 방향 문서가 계속 소유한다.
