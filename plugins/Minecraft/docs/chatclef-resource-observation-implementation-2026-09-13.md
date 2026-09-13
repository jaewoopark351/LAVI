<!-- 20260913_kpopmodder: Record the implemented gold/deposit and separate Builder diagnostics; no gameplay fix. -->
# 금 채굴·자동보관·Builder 관측 구현 기록

<!-- 20260913_kpopmodder: Preserve historical verification while linking the later four-unit behavior-repair design. -->
22:12 후속 실행과 독립 검수에서 확인한 장착 불일치·단축바 충돌·보호 좌표 예외·root 종료 대기는 [후속 설계와 근거](gold-mining-debugging-2026-09-13/README.md)에 정리했다. 그 문서의 동작 수정은 `PLAN_ONLY`이며, 아래 진단 구현·빌드·실게임 검증의 각 과거 결과를 변경하지 않는다.

## 범위

기준 HEAD는 `dfe29ef8547ee85fa736b75b208b126fc73f0b8b`다. 사용자의 후속 **로그 보강만 구현** 요청에 따라 아래 두 계획의 관측 코드를 추가했다. 기존 문서 변경을 보존했다.

- [금 채굴 도구 반복·자동보관 계획](chatclef-gold-mining-tool-loop-auto-deposit-diagnostics-plan-2026-09-13.md)
- [Builder 빈 이동 목록 계획](chatclef-baritone-builder-empty-movements-diagnostics-plan-2026-09-13.md)

도구 선택·장착·준비 조건, 채굴·길찾기·Task·보관 정책, 아이템 보호, 자동방어, 명령 결과와 Chat/TTS 동작은 변경 대상이 아니다. 크래시·반복·보관 미실행 원인을 해결했다고 주장하지 않는다. 최초 로그 구현 단계에서는 배포, Minecraft 실행·중지, 외부 인스턴스·월드·캐시 변경, 커밋·푸시를 수행하지 않았다. 후속 관찰자 등록 오류 수정의 배포·실행 상태는 아래에서 연결한 별도 기록을 따른다.

## 최신 후속 관측 — 21:16 금 채굴 반복과 공유 목록 예외

관찰자 등록 수정 후 사용자가 실행한 21:16 기록에서는 실제 도구 장착이 성공했고, 금 채굴 준비 재진입 117회·금 수량 무진척, 별도의 공유 블록 목록 NPE/AIOOBE, 일반 로그 예산 소진을 확인했다. **추가 로그 보강 단위와 검증 상태**는 [공유 블록 목록·예산 이후 관측 기록](chatclef-shared-block-read-checkpoint-diagnostics-2026-09-13.md)을 따른다. 아래 수치와 실행 결과는 각 시점의 기록으로 보존한다.

## 이전 실행 상태 — 20:19:38 크래시 확인

사용자가 마지막 19:48 JAR를 배포·실행한 뒤, 20:18:38 월드 진입을 통과했으나 **20:19:38 진단 관찰자 등록 한도 초과로 크래시**했다. 전체 등록 구성은 기존 15개에서 신규 관찰자 2개 추가로 17개가 됐고, registry 상한은 16개였다. `ToolEquipDiagnostics` 최초 초기화가 실패해 실제 도구 교체 전에 게임 tick으로 예외가 전파됐다. 로그 보강이 게임 동작을 방해하지 않아야 한다는 요구를 위반한 결함이다.

[사건 증거·후속 수정·검증 항목](chatclef-diagnostic-observer-capacity-crash-plan-2026-09-13.md)에 상세 기록한다. **후속 디버깅 요청에서 등록 상한·실패 격리 수정과 전체 등록 검사를 구현했다.** 최신 검증 상태는 링크 문서의 후속 구현 절을 따른다. 아래 19:48까지의 build/Mixin 통과 및 runtime `NOT_RUN`은 당시 기록으로 보존한다. 당시 Mixin 변환 검증은 전체 관찰자의 실제 지연 초기화를 검증하지 않았다. 수정 전 live-runtime 실패와 수정 후 자동 검증·실제 게임 검증은 구분한다.

## 책임별 구현

| 책임 | 위치와 관측 |
| --- | --- |
| 도구 선택·장착·반복 | `diagnostics/mining/gold/`: 실제 후보 탈락 분기, 접근용 곡괭이의 단축바 이탈, 장착 전후 슬롯, 준비 재진입, 채굴 자식 시작·중단을 부모/요청에 연결 |
| 자동보관 판단 | `diagnostics/container/store/deposit/pressure/`: 실제 평가 입구·early return, 점유율, 정책·작업 보호·재실행 대기, 실행 계획, 기존 scheduler 결과 |
| 보관 결과의 출처 | 생성 시 고정한 operation 관측 핸들을 maintenance/trusted/recovery Task에 연결. 늦은 결과에 현재 명령을 새로 붙이지 않음 |
| Builder 소비 경계 | `diagnostics/baritone/builder/`: 실제 `List.get` receiver/index와 executor/path snapshot; 원래 접근은 한 번 실행하고 원래 반환값·예외 전파 보존 |
| 경로 출처 | `diagnostics/baritone/correlation/`: 요청 당시 출처, raw/postProcess, cutoff/splice, executor index 이관, current/next 채택·제거 |
| 공통 관측 | `diagnostics/observation/`: activation/scope, 제한된 evidence memory, 출력 예산·결과 누계, 필드 생성·물리 출력 책임 분리 |
| 출력 예약 | 기존 `diagnostics/session/` admission authority와 물리 logger 재사용. 별도 logger/thread/worker/backend 없음 |

upstream 파일은 최소 관측 hook만 둔다. `TaskRunner`는 원래 `isActive()` 반환값과 `getPriority()` 결과인 지역 변수를 전달한다. 도구 장착이나 자동방어를 실행할 수 있는 getter를 로그 목적으로 다시 호출하지 않는다.

금 부모가 자동보관·방어 때문에 잠시 중단되는 것과 실제 요청 종료를 구분한다. 같은 부모의 자식 재시작은 반복 누계를 초기화하지 않는다. 최초 장착→준비 재진입→자식 중단의 연결은 최근 32개 이력과 별도로 보존한다. `gold/root/`의 제한된 weak-root·assignment 결합은 `UserTaskChain`이 원래 root를 실제로 제거한 STOP·교체·정상 완료에서 해당 금 scope만 닫는다. pause·방어 yield는 종료로 처리하지 않는다.

자동보관은 `NOT_EVALUATED`, 평가 결과 false, 조회 불가를 구분한다. 이전 대기 진입 요청과 현재 평가 요청을 따로 남긴다. 계획한 빈칸과 실제 점유율 변화는 별도 필드이며, 부호 있는 순변화만으로 보관 전송의 효과를 확정하지 않는다.

## 확정한 출력·메모리 제한

계획의 초기 수치보다 아래 구현 수치가 우선한다. 기존 사건 로그의 일반 한도 4,936개는 과거 사실로 유지한다.

| 항목 | 구현값 |
| --- | --- |
| process diagnostic session 총 상한 | 5,000 슬롯 유지 |
| 일반 슬롯 | 4,540 |
| 기존 critical 슬롯 | 기존 64개와 각 하위 quota 유지 |
| 새 최초 슬롯 | mining/deposit/builder 각각 128개, 총 384개 |
| 새 scope 종료 슬롯 | 12개 |
| 수용 범위 | process diagnostic session 전체에서 domain당 최대 4개, 전체 최대 12개; OFF·새 월드로 quota 초기화하지 않음 |
| activation identity | 최대 8개, instance/world는 weak reference. mode epoch·월드 교체 시 무효화 |
| scope 최초 signature | `event + reason` 최대 32개. domain/scope가 owner를 구분하고 phase 차이는 event/reason에 포함 |
| 최근 이력 | 이벤트별 의미/fingerprint 변화만 반영, 최근 32개. 항목 최대 2 KiB |
| 최초 고정 기록 | 최대 4개, 각각 최대 2 KiB. 최근 이력이 밀려도 유지 |
| 일반 반복 출력 | scope당 최대 256개, 직전 시도 이후 200 tick **및** 10초 경과 시 요약 가능 |
| 출력 크기 | 이벤트 최대 8 KiB. 개별 값은 최대 240 UTF-8 bytes; 수집 필드는 최대 80쌍/합계 6 KiB |
| Builder 경로 metadata | 인스턴스당 최대 64개, current/next 고정 슬롯 2개와 최근 이력 32개 |
| 재시도 | 자동 재시도 0회. 같은 최초 signature는 한 번만 시도하고 이후 실패 상태를 요약 |

신규 필수 사건은 `RESOURCE_OBSERVATION_MINING_FIRST`, `RESOURCE_OBSERVATION_DEPOSIT_FIRST`, `RESOURCE_OBSERVATION_BUILDER_FIRST`의 정확한 wrapper 이름으로 전용 quota에 연결된다. 원래 사건·사유는 `observedEvent`·`observedReason`에 남는다. 일반 요약은 `RESOURCE_OBSERVATION_DETAIL`로만 분류하여 최초 예약을 소비하지 않는다. 실제 진단 scope 종료만 `RESOURCE_OBSERVATION_TERMINAL`을 사용한다.

수용되지 않은 다섯 번째 scope에 대해 무제한 최초 출력을 보장하지 않는다. 수용 거부·stale scope·capture 실패는 제한된 누계와 최종 session snapshot에 남는다. 무효화된 activation의 world/instance 참조는 해제하고, 이미 수용한 최대 12개 scope의 동결된 증거·누계만 보존한다. 부모 소유의 순수 도구 상태는 다음 유효 bind에서 초기화하며 자식 Task 참조는 weak reference다.

필드는 합친 긴 문자열 하나로 출력하지 않는다. 요청 식별자와 사건 값은 별도 필드로 전달한다. 최초 pin·최근 이력의 부가 출력은 제한된 조각으로 나누고 잘린 여부를 표시한다. 물리 formatter의 `diagnosticCaptureStatus=partial`을 증거 완전성 확인으로 해석하면 안 된다.

`captured`, `attempted`, shared admission, `EMISSION_CALLS_RETURNED`, 출력 실패/승인 여부 미확정, 최종 파일 관측을 구분한다. 호출 성공은 Minecraft/launcher 파일에 영속 저장됐다는 증거가 아니며 `filePersistence=NOT_VERIFIED`로 남긴다. 출력 실패가 게임의 반환·예외·재시도·Task 선택을 바꾸지 않는다.

## 최초 구현 검증 기록 — 시작 크래시 수정 전

| 검증 | 상태 |
| --- | --- |
| 소스 구현 | `IMPLEMENTED`, 로그 전용 source diff 검수 완료 |
| focused 자동 검증 | 287개 실행, **286개 성공 / 실패 0개 / 기존 테스트 1개 중단** |
| 버전 지정 `:1.20.1:clean :1.20.1:build` | 기존 1.21.1 GOTO 소스 오류로 실패. 다중 버전 의존 작업이 `:1.21.1:compileJava`를 실행함 |
| 버전 미지정 전체 `clean build` | `NOT_RUN` — 사용자 1.20.1 한정 범위 |
| 1.20.1 clean 실행 JAR | `PASS` — `:1.20.1:clean :1.20.1:remapJar --rerun-tasks --no-build-cache --no-daemon`; 31 tasks 모두 실행 |
| 배포 / Minecraft 실행·재현 / 새 로그 파일 관측 | `NOT_RUN` |

검증 스크립트는 별도 PowerShell 창에서 실행한다. JDK 21로 빌드하고 1.20.1 target은 기존 release 17 설정을 유지한다. writable Gradle/temp는 저장소 내부, 기존 사용자 Gradle cache는 `GRADLE_RO_DEP_CACHE`로 읽기만 사용한다. 기존 offline/local Maven init 설정을 재사용하며 의존성·버전·시스템 설정을 바꾸지 않는다.

당시 빌드 로그: `logs/build/resource-observation-1201-20260913-180453.log` (`BUILD SUCCESSFUL`, 1m 50s). 산출물은 `versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`, 8,597,005 bytes, SHA-256 `10209018d24f367358a1472592595c0ca1f090776d3f7100e453f058c8ed30a7`였다. class major version 61(Java 17), 새 관측 클래스와 네 신규 Mixin 등록이 JAR에 포함됨을 확인했다. 이 검증은 실제 Mixin 변환을 포함하지 않았으며, 후속 사용자 실행에서 아래 시작 크래시가 확인됐다.

앞선 `175309`/`175557` 빌드는 offline 캐시 설정 누락으로 컴파일 전에 실패했고, `175644`의 build 경로는 기존 다른 버전 GOTO 오류로 실패했다. 이를 성공으로 덮지 않는다. 실행 JAR을 만드는 검증 성공은 전체 build/test 경로 성공과 구분한다.

focused 검증은 `test/test_Isolation/minecraft/resource_observation/verify_resource_observation.ps1`와 분리된 classpath/input helper로 수행했다. 최종 결과는 `output/f326eb2eb44f40c484d7603c90457c41/tests.log`와 같은 폴더의 `inputs.txt`에 보존한다. shared observation 8/8, gold 7/7, pressure 6/6, Builder 13/13이 통과했다. 기존 session/채굴/보관 회귀 검증을 포함한 전체 성공 수는 286개다.

중단된 기존 테스트는 `AutoDepositCategoryReservePolicyTest.allocatesFuelAsOneCategoryTotalInsteadOfPerItem()`이다. 일반 named Minecraft 테스트 환경에서 `SimpleRegistry`가 `RegistryEntry.Reference.setRegistryKey`에 접근할 때 발생하는 `IllegalAccessError`로 bootstrap되지 못했다. 이 테스트는 통과로 계산하지 않으며 테스트를 지우거나 게임/정책 동작을 바꿔 우회하지 않았다. 앞선 실행의 테스트 작업 폴더 문제 10건은 harness에서 해결한 뒤 재검증했다.

일반 cap 후 최초 사유·종료 예약, 80회 자식 반복, 최초 pin/최근 이력 분리, 서로 다른 early return, 원래 scheduler 호출 횟수와 예외, 늦은 worker/요청, OFF/월드 교체, 원래 `List.get` 피연산자·반환·예외를 검증한다. build 성공만으로 새 Mixin의 실제 Minecraft 주입 또는 사건 직전 파일 출력이 확인됐다고 기록하지 않는다.

## 19:30 시작 크래시 원인과 후속 수정

사용자가 배포한 위 SHA-256의 JAR에서 2026-09-13 19:25:22, 19:28:02, 19:30:07 KST에 같은 시작 실패가 발생했다. 읽기 전용으로 확인한 마지막 증거는 `C:/Users/jaewo/curseforge/minecraft/Instances/LAVI_TEST_Fabric01/crash-reports/crash-2026-09-13_19.30.07-client.txt`의 `Initializing game` 스택과 `logs/latest.log`의 다음 오류다.

```text
InvalidMixinException: @Shadow field baritone was not located in the target class baritone.process.BuilderProcess.
```

이는 새 `BuilderProcessDiagnosticMixin`의 필드 연결 오류다. `baritone` 필드는 `BuilderProcess`가 아니라 부모 `BaritoneProcessHelper`에 선언돼 있다. 게임 진입 전 Mixin 적용에서 실패했으므로 기존 채굴 반복·빈 이동 목록 접근의 원인이 확인된 것으로 해석하지 않는다. 실제 processed Baritone와 실행 JAR 내부 Baritone의 SHA-256은 모두 `807a467dbddede6769d9b666f176225694f8586d9237e0b62713fbda39fe0b27`로 일치했다.

후속 사용자의 코드 수정·1.20.1 빌드 요청에 따라 다음 최소 단위로 수정했다.

- `BuilderProcessDiagnosticMixin`: 잘못된 inherited-field `@Shadow`를 제거하고 기존 process 객체를 관측 함수에 전달한다.
- `BaritoneProcessHelperDiagnosticAccessor`: 필드가 실제 선언된 부모 클래스에 읽기 전용 accessor를 연결한다. `altoclef.mixins.json`에 등록한다.
- `BuilderProcessOwnerView`: observer가 사용하는 단일 책임의 owner 조회 인터페이스다.
- `BuilderProcessObserver`: BOUNDARY 모드에서 기존 owner를 읽고 실패는 관측 실패 누계로 처리한다. 기존 경로·Task 동작을 호출하거나 바꾸지 않는다.

upstream 클래스의 상속 구조·필드·생성자와 원래 `List.get` 호출은 유지한다. 도구 선택·자동보관 조건·자동방어는 이 수정의 대상이 아니다.

### 수정 후 1.20.1 빌드 및 회귀 검증

새 **표시된 PowerShell 창**에서 `logs/build/verify-resource-observation-1201-20260913.ps1`를 실행했다. 실행 명령은 다음과 같다.

```powershell
.\gradlew.bat :1.20.1:clean :1.20.1:remapJar --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline --init-script .gradle/codex-build-init.gradle
```

2026-09-13 19:39:40~19:41:26 KST에 `BUILD SUCCESSFUL in 1m 45s`, 31 tasks 전부 실행, exit code 0을 확인했다. 로그는 `logs/build/resource-observation-1201-20260913-193939.log`와 같은 이름의 `.result.json`이다. 다른 버전은 기존 전처리·mapping 의존 작업만 실행했고 Java 컴파일·JAR 생성·remap은 **1.20.1만** 실행했다. 알려진 다중 버전 `build` 의존성을 성공으로 기록하지 않는다.

이 빌드의 산출물은 `versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`, **8,598,115 bytes**, SHA-256 **`d8206c5435763acdc88eddf46a49cb6ad9a18ca78609ef65c9a6386ef4fcbee2`**였다. 수정한 클래스 4개, 새 accessor 등록, class major version 61(Java 17), 기존 Baritone SHA-256 유지까지 확인했다.

fresh named 1.20.1 클래스로 focused suite를 재실행한 결과는 **287개 실행 / 286개 성공 / 실패 0개 / 기존 bootstrap 중단 1개**다. 증거는 `test/test_Isolation/minecraft/resource_observation/output/f5c1e6daecca45bf8a0e267b88dc4b84/tests.log`와 같은 폴더의 `inputs.txt`다. 위에서 설명한 `AutoDepositCategoryReservePolicyTest` 중단이 동일하게 남아 wrapper는 exit code 1로 끝났으므로 전체 suite를 PASS로 표시하지 않는다.

기존 검증 누락을 보완하기 위해 게임을 실행하지 않는 별도 JVM에서 **실제 Sponge MixinTransformer**로 대상 바이트코드를 변환했다. 이전 배포 JAR에서는 원래 shadow 오류가 재현됐고, 수정한 named 클래스에서는 부모 accessor를 포함한 대상 7개가 모두 변환됐다. 이어 `AbstractNodeCostSearch`까지 포함한 대상 8개를 위 remapped JAR 자체·그 내부 Baritone·기존 intermediary Minecraft 1.20.1 바이트코드로 검증했다. 8개 모두 변환됐으며 `test/test_Isolation/minecraft/mixin_application/output/6a348599e66f48209dc70b50dd931a61/`에 기록했다.

동일한 packaged/intermediary 조건에서 이전 깨진 JAR을 입력한 비교 검사는 `output/3e1430520cd3440f9896efd4e70f3cd4/smoke.log`에 원래 missing-shadow 오류가 재현됐음을 남긴다. 정확한 이전 오류가 재현될 때만 비교 검사를 통과시키며, 깨진 JAR이 정상이라는 뜻이 아니다. 기존 외부 인스턴스의 파일은 읽기만 했다.

변환 harness의 Java 코드·Mixin 설정·service 등록은 `src/test/mixinApplication/`에 둔다. 이는 일반 JUnit의 메서드 존재 여부나 wrapper 직접 호출 검사와 구분한다. 실제 적용 대상 8개는 `BaritoneProcessHelper`, `BuilderProcess`, `Path`, `CutoffPath`, `SplicedPath`, `PathExecutor`, `PathingBehavior`, `AbstractNodeCostSearch`다. 게임 클래스는 리소스로만 읽으며 초기화하지 않는다. 설치된 Sponge `0.17.3+mixin.0.8.7`와 MixinExtras `0.5.4`를 기존 위치에서 읽고 production 의존성을 바꾸지 않는다.

이 검사는 선택한 Mixin의 실제 변환 증거다. 다른 모든 모드와의 조합, 게임 callback 실행, 실제 로그 파일 출력을 증명하지 않는다. 당시 배포·Minecraft 시작·월드 진입·실제 관측 파일 출력은 `NOT_RUN`이었다. 이후 사용자의 실행에서 확인한 별도 크래시는 문서 상단의 최신 상태를 따른다.

### 당시 산출물 — 19:48 빌드·격리 검증 완료

최종 검수에서 `BuilderProcessObserver`의 진단 모드 조회도 기존 `try` 안으로 옮겼다. 진단 클래스 초기화 실패를 같은 `RuntimeException | LinkageError` 관측 실패 처리 범위에 포함하기 위한 보완이다. 같은 `.ps1`를 새 PowerShell 창에서 다시 실행하여 이 한 줄 변경까지 새로 컴파일·패키징했다.

| 항목 | 최종 증거 |
| --- | --- |
| 빌드 | 19:46:15~19:48:03 KST, `BUILD SUCCESSFUL in 1m 48s`, exit 0, 31 tasks 모두 실행 |
| 범위 | `:1.20.1:clean :1.20.1:remapJar --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline --init-script .gradle/codex-build-init.gradle`; Java 컴파일·JAR 생성은 1.20.1만 |
| 로그 | `logs/build/resource-observation-1201-20260913-194614.log` 및 `.log.result.json` |
| 최종 JAR | `versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`, **8,598,123 bytes** |
| SHA-256 | **`ac7a63d40897af585a3e8da121223d56de48be9e4136012b2dee694d24cf54e6`** |
| 패키징 확인 | 수정 클래스 4개·accessor 등록 포함, major 61(Java 17), 내부 Baritone SHA-256 유지 |
| 실제 Mixin 변환 | 최종 JAR 자체와 그 내부 Baritone를 사용해 **8/8 통과**, launcher exit 0 |
| 변환 증거 | `test/test_Isolation/minecraft/mixin_application/output/22c744a8e55f4f4e8cac816eab1eb2d2/{smoke.log,inputs.txt}` |
| focused 재검증 | **287개 실행 / 286개 성공 / 실패 0개 / 기존 bootstrap 중단 1개**, wrapper exit 1 |
| focused 증거 | `test/test_Isolation/minecraft/resource_observation/output/185a3f07e4644cf290ca2e40804704d5/{tests.log,inputs.txt}` |
| 당시 배포·Minecraft 실행·월드 진입 | 19:48 검증 당시 `NOT_RUN`; 이후 사용자 실행의 20:19 크래시는 별도 최신 기록 참조 |

변환 검사의 canonical 실행기는 Git 추적 가능한 `src/test/mixinApplication/scripts/verify_mixin_application.ps1`다. [실행법과 검증 한계](../runtime/chatclef_fabric_1.20.1/src/test/mixinApplication/README.md)를 함께 보존한다. 이전 ignored isolation 경로의 실행기는 이 파일에 위임하며, 출력만 기존 isolation 폴더에 남긴다.
