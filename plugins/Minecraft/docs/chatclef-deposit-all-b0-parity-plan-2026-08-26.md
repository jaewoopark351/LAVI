# ChatClef @deposit_all B0 복붙 패리티 계획

문서 상태: B0_CLEAN_BUILD_PASSED_RUNTIME_NOT_VERIFIED

작성 기준일: 2026-08-26

소스 기준 커밋:

~~~text
c865cb3a39b70a451915140804b15e2f9439ef3d
~~~

이 문서는 설계와 검증 순서만 고정한다. Java 구현, 빌드, Minecraft
실행, 배포, 커밋, 푸시를 승인하지 않는다.

2026-08-26 사용자 결정으로 DepositAllTask의 복제 원본은
StoreInAnyContainerTask 전체로 확정되었다. 같은 날 B0 Java 복제와 명령
등록만 승인되었으며, 빌드와 Minecraft 실행은 승인되지 않았다.

## 1. 목표

첫 구현 단계 B0의 목표는 새 기능을 만드는 것이 아니다.

~~~text
bare @deposit
    ==
bare @deposit_all
~~~

같은 시작 상태에서 두 명령이 같은 아이템을 선택하고, 같은 저장 Task
결정을 내리고, 같은 방식으로 컨테이너를 찾거나 준비하고, 같은 저장 및
종료 흐름을 보이는지를 먼저 확인한다.

기존 @deposit은 변경하지 않는다. @deposit_all은 이후 B1, B2, B3에서
독립적으로 변경할 수 있도록 새 Command 클래스와 새 Task 클래스에 둔다.

## 2. B0 원칙

B0에서는 복사 원본의 동작을 보존한다.

허용되는 변경은 다음 범위로 제한한다.

1. 새 파일명
2. 새 클래스명
3. 새 생성자명
4. command name의 deposit에서 deposit_all로의 변경
5. 새 클래스가 컴파일되기 위해 필요한 자기 타입 참조 변경
6. 필요한 import 변경
7. AltoClefCommands의 import 및 명령 등록 추가
8. DepositAllCommand가 DepositAllTask를 생성하도록 하는 참조 변경
9. 새 Java 파일에 필요한 프로젝트 이력 marker 추가

B0에서 하지 않는 작업:

- 함수 정리
- 조건문 개선
- helper 추출
- 공통 기반 클래스 도입
- 상속을 통한 코드 공유
- 아이템 보호 규칙 추가
- 컨테이너 거리 또는 검색 범위 변경
- ResourceTask 제한
- retry, timeout, fallback 또는 cleanup 변경
- 입력, Baritone goal 또는 path 소유권 변경
- 진단 이벤트 이름 정리
- 오탈자 정리
- 기존 @deposit 코드 변경
- Python 명령 변환, 한국어 별칭 또는 LAVI bridge 정책 변경
- Minecraft, Fabric, ChatClef, AltoClef, Baritone 또는 Gradle 버전 변경

리팩터링으로 중복을 줄이는 일은 B0의 목표와 충돌한다. B0의 중복은 이후
변경을 격리하고 원본과 비교하기 위한 의도적인 중복이다.

## 3. 현재 소스에서 확인된 사실

### 3.1 bare @deposit 호출 흐름

현재 DepositCommand의 실제 무인자 흐름은 다음과 같다.

~~~text
@deposit
  -> DepositCommand.call()
  -> parser.get(ItemList.class) == null
  -> getAllNonEquippedOrToolItemsAsTarget(mod)
  -> new StoreInAnyContainerTask(false, items)
  -> DepositCommandDiagnostics.logInvocation(...)
  -> mod.runUserTask(storeTask, this::finish)
~~~

bare @deposit의 아이템 선택 조건은 이름상 "모든 장비 제외"보다 좁고
구체적이다.

- PlayerSlot.ARMOR_SLOTS에 있는 스택은 제외한다.
- Item이 ToolItem인 스택은 제외한다.
- 빈 스택은 제외한다.
- 그 밖의 인벤토리 스택은 ItemTarget으로 만든다.

따라서 B0에서 "보호해야 할 것 같은 아이템"을 추가로 제외하면 패리티가
아니다. 그 변경은 B1에서 별도로 정의하고 검증한다.

StoreInAnyContainerTask에는 false가 전달되므로 저장 대상 아이템이 부족할
때 그 아이템을 추가로 획득하는 분기는 비활성화된다. 그러나 주변에 사용할
컨테이너가 없고 인벤토리에도 배치할 컨테이너 블록이 없으면 현재 Task는
TaskCatalogue.getItemTask(Items.CHEST, 1)을 반환해 상자를 구하려고 한다.
이 동작도 B0에서는 그대로 보존한다.

### 3.2 별도 DepositTask는 존재하지 않는다

현재 소스에는 DepositTask.java 또는 DepositTask 클래스가 없다.
DepositCommand가 StoreInAnyContainerTask를 직접 생성한다.

그러므로 다음과 같은 일대일 복사 원본은 현재 존재하지 않는다.

~~~text
DepositTask -> DepositAllTask
~~~

사용자가 요구한 독립 DepositAllTask를 문자 그대로 만들 때 가장 가까운
복사 원본은 StoreInAnyContainerTask다.

~~~text
DepositCommand            -> DepositAllCommand
StoreInAnyContainerTask    -> DepositAllTask
~~~

이 매핑은 2026-08-26 사용자 결정으로 B0 복제 원본으로 확정되었다. 이전
DepositAllTask가 존재했다는 Git 이력은 현재 확인되지 않았다. 따라서 과거
구현을 복원한 것이 아니라 현재 기준선에서 새로 전체 복제한 작업으로
취급한다.

### 3.3 명령 등록 위치

현재 DepositCommand는 다음 파일에서 직접 등록된다.

~~~text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
adris/altoclef/AltoClefCommands.java
~~~

CommandExecutor는 Command.getName()을 키로 등록한다. 따라서 B0 등록 변경은
DepositAllCommand import와 new DepositAllCommand() 추가로 한정한다.

### 3.4 현재 진단 코드

DepositCommand와 StoreInAnyContainerTask에는 이미 LAVI의 bounded
diagnostics 호출이 들어 있다. 관련 진단 API는 구체
StoreInAnyContainerTask 타입이 아니라 Task 타입을 받으므로,
DepositAllTask 복제 시 타입 때문에 즉시 막히는 구조는 현재 확인되지
않았다.

B0에서는 진단 helper를 복제하거나 이벤트 이름을 바꾸지 않는다.
기존 진단 호출을 그대로 유지할 경우 일부 이벤트 이름에는 deposit 또는
store_in_any_container가 남을 수 있다. 이것은 B0의 동작 패리티를
지키기 위한 의도적인 제한이며, 진단 명칭 정리는 별도 단계다.

## 4. B0의 예상 파일 범위

B0에서 생성한 새 파일:

~~~text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
adris/altoclef/commands/DepositAllCommand.java

plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
adris/altoclef/tasks/container/DepositAllTask.java
~~~

B0에서 최소 수정한 파일:

~~~text
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java/
adris/altoclef/AltoClefCommands.java
~~~

의도적으로 변경하지 않을 기존 파일:

~~~text
adris/altoclef/commands/DepositCommand.java
adris/altoclef/tasks/container/StoreInAnyContainerTask.java
adris/altoclef/tasks/container/StoreInContainerTask.java
adris/altoclef/tasks/ResourceTask.java
adris/altoclef/TaskCatalogue.java
adris/altoclef/commandsystem/Command.java
adris/altoclef/commandsystem/CommandExecutor.java
lavi/minecraft/diagnostics/**
~~~

## 5. 복제 시 필요한 최소 자기 참조 변경

DepositAllCommand에서는 최소한 다음 참조가 달라졌다.

- 파일명과 public class 이름
- 생성자 이름
- super에 전달되는 command name
- StoreInAnyContainerTask import
- 지역 변수의 Task 타입과 생성 대상
- AltoClefCommands의 import와 생성자 호출

DepositAllTask를 StoreInAnyContainerTask에서 복제하면서 최소한 다음
자기 참조가 달라졌다.

- 파일명과 public class 이름
- 생성자 이름
- isEqual의 instanceof 대상
- isEqual에서 비교하는 지역 변수 타입

특히 isEqual의 자기 타입 변경은 리팩터링이 아니라 새 클래스가 독립된
Task 정체성을 갖고 컴파일되기 위한 필수 변경이다. onStart, onTick,
isFinished, onStop의 분기와 반환 Task는 B0에서 변경하지 않는다.

## 6. 무인자 계약과 인자 계약의 구분

DepositCommand를 그대로 복제하고 command name만 바꾸면
DepositAllCommand도 ItemList 인자를 파싱하게 된다. 그 결과 소스 구조상
@deposit_all diamond 2 같은 형태도 받아들일 가능성이 있다.

현재 사용자가 요구한 B0 승인 대상은 오직 다음 호출이다.

~~~text
@deposit_all
~~~

@deposit_all에 명시적 아이템 인자를 허용할지, 거부할지, 문서에 노출할지는
아직 결정되지 않았다. B0에서 인자 문법을 제거하면 단순 이름 복제 범위를
넘는 별도 동작 변경이 된다. 구현 전에 이 계약을 명시적으로 확정해야 한다.

## 7. B0 정적 패리티 게이트

런타임 실행 전에 다음을 확인한다.

1. 기존 DepositCommand.java의 내용이 변경되지 않았다.
2. 기존 StoreInAnyContainerTask.java의 내용이 변경되지 않았다.
3. DepositAllCommand의 정규화 diff가 허용된 이름과 참조 변경만 포함한다.
4. DepositAllTask의 정규화 diff가 허용된 자기 타입 변경만 포함한다.
5. AltoClefCommands 변경은 import와 등록 추가에 한정된다.
6. dependency, Gradle, Mixin, protocol, Python 파일 변경이 없다.
7. @deposit과 @deposit_all의 command name이 충돌하지 않는다.

문서 작성 시점의 비교 기준 SHA-256:

~~~text
DepositCommand.java
D4D598C6D1F2F3465A18F0A150B8294F621F3248C35DBCD97896AC75209F09B5

StoreInAnyContainerTask.java
7F54FE3FACE7DE1D5DFE8D45B9330A97B3B0AD80A47A7071EF4F1CBEDBC612BC

AltoClefCommands.java
DC03102ECB1659F1ED0D0FC55852864C887541F6D5287F5AD39FD7AED563D5F7
~~~

이 해시는 B0 복제 원본을 식별하기 위한 문서 기준점이다. 구현 시작 시
HEAD가 달라졌다면 먼저 새 diff와 provenance를 확인하고 기준을 갱신한다.

## 8. B0 빌드 및 런타임 게이트

빌드와 Minecraft 실행은 별도 사용자 승인을 받은 뒤에만 수행한다.

빌드 검증은 chatclef-fabric-build-verification.md의 clean forced build
절차를 따른다. 증분 빌드 성공만으로 B0를 통과시키지 않는다.

런타임 비교는 동일한 시작 상태를 준비해야 한다. 첫 명령이 인벤토리와
컨테이너를 변경하므로, @deposit 실행 직후 같은 월드에서 @deposit_all을
연속 실행하는 방식은 패리티 증거가 아니다. 각 명령 전에 다음 조건을
동일하게 복원해야 한다.

- 플레이어 위치와 차원
- 인벤토리 슬롯별 아이템과 수량
- 장착 방어구와 도구
- 주변 컨테이너 위치와 내용물
- 컨테이너 위 블록과 접근 가능 상태
- BlockScanner 및 관련 캐시 상태
- 활성 UserTask와 Baritone 상태

월드 복원 또는 교체 후에는
chatclef-baritone-cache-troubleshooting.md 절차를 먼저 적용한다.

## 9. B0 런타임 합격 기준

같은 시작 상태에서 bare @deposit과 bare @deposit_all을 각각 실행해 다음을
비교한다.

- 두 명령이 모두 등록되어 실행된다.
- @deposit의 기존 동작이 회귀하지 않는다.
- 선택된 ItemTarget의 항목과 수량이 같다.
- armor slot 제외 결과가 같다.
- ToolItem 제외 결과가 같다.
- getIfNotPresent가 모두 false다.
- 기존 컨테이너 탐색 여부가 같다.
- 같은 조건에서 OPEN_EXISTING, PLACE_CONTAINER_NEARBY,
  OBTAIN_CHEST 중 같은 분기를 선택한다.
- 동일한 종류의 child Task 흐름을 만든다.
- 저장된 아이템과 남은 아이템 결과가 같다.
- 정상 완료, interruption, 실패 시점이 기능적으로 같다.
- 새 crash report와 Mixin 또는 injection 오류가 없다.

경로 계산에는 실행 시점의 캐시와 월드 상태가 영향을 줄 수 있으므로
블록 단위로 완전히 같은 이동 궤적만을 단독 합격 기준으로 사용하지 않는다.
대신 같은 입력 상태, 같은 Task 분기, 같은 대상 컨테이너 정책, 같은 저장
결과와 종료 결과를 함께 확인한다.

B0 합격 전에는 B1 변경을 섞지 않는다.

## 10. 단계별 진행 순서

~~~text
B0  복붙 패리티
    -> 정적 diff 확인
    -> clean forced build
    -> 배포 JAR hash 확인
    -> Minecraft 독립 재현
    -> @deposit 회귀 확인
    -> B0 기준점 기록

B1  아이템 보호
    -> 보호 대상 계약 확정
    -> 한 종류의 정책 변경만 적용
    -> build 및 독립 재현

B2  bounded container
    -> 거리, 후보 종류, fallback 계약 확정
    -> 한 종류의 범위 제한만 적용
    -> build 및 독립 재현

B3  ResourceTask 제한
    -> 차단할 정확한 호출과 허용할 호출 확정
    -> 소유 Task와 terminal reason 확정
    -> 한 종류의 제한만 적용
    -> build 및 독립 재현
~~~

각 단계는 이전 단계의 통과 증거를 보존하고 별도 rollback 단위를 가진다.
한 단계가 실패하면 다음 단계로 넘어가지 않는다.

## 11. 아직 확정되지 않은 사항

다음 항목은 문서 작성 시점에 추측으로 확정하지 않는다.

1. @deposit_all의 명시적 ItemList 인자를 허용할지 여부
2. B0에서 기존 DepositCommandDiagnostics 명칭을 그대로 사용할지 여부
3. B1에서 보호할 정확한 아이템과 슬롯
4. B2의 거리 기준, 컨테이너 종류, 새 상자 배치 허용 여부
5. B3에서 ResourceTask 전체를 제한할지, 상자 획득 경로만 제한할지
6. 각 단계의 정확한 runtime fixture와 reset 절차
7. Java 등록 목록을 고정한 Python command catalog 계약의 동기화 시점

## 12. 구현 승인 전 중단 조건

다음 중 하나라도 발생하면 임의로 코드를 만들지 않고 보고한다.

- 복제 원본이 구현 시작 시점에 변경되어 문서 해시와 달라짐
- 새 Task를 위해 기존 StoreInAnyContainerTask 동작 변경이 필요함
- TaskRunner, UserTaskChain, ResourceTask 또는 TaskCatalogue 변경이 필요함
- 기존 @deposit 동작을 바꿔야만 등록 가능함
- 진단 helper나 protocol 변경이 필수로 보임
- B0에 B1 이상의 보호 또는 제한 정책이 필요해짐

이 경우 B0 구현을 확대하지 않고, 확인된 사실과 필요한 결정만 사용자에게
보고한다.

## 13. B0 소스 적용 결과

2026-08-26에 사용자 승인을 받은 B0 Java 소스 복제를 적용했다.

적용 결과:

- DepositAllCommand.java 생성
- DepositAllTask.java 생성
- AltoClefCommands.java에 import와 등록 한 줄씩 추가
- DepositCommand.java SHA-256 보존
- StoreInAnyContainerTask.java SHA-256 보존
- 필수 marker를 제거하고 이름을 원복한 Command 정규화 비교 통과
- 필수 marker를 제거하고 이름을 원복한 Task 정규화 비교 통과
- deposit과 deposit_all import 및 등록이 각각 정확히 한 번임을 확인
- trailing whitespace 없음

새 파일 SHA-256:

~~~text
DepositAllCommand.java
5E05443FBCD422064B9A7DDC7107FAC20782EB36DE338C3EFA7631127CA90B5E

DepositAllTask.java
59E9152D0788611B1602D4B5E9F4523F2D5ECB7DE5AA94E0C15B5AA1C1035432
~~~

기존 Java command catalog 계약 테스트 결과:

~~~text
6 tests run
5 passed
1 failed

failure:
DepositAllCommand exists in AltoClefCommands but is not yet present in
chatclef_registered_commands.snapshot.json
~~~

이 실패는 Java 복제 패리티 실패가 아니라 Java 등록 목록과 Python command
catalog 사이의 계약 미동기화다. B0의 Python 명령 변환, 한국어 별칭,
bridge 정책 무변경 범위를 지키기 위해 이번 적용에서는 Python registry,
snapshot, support matrix, 테스트 기대 개수를 변경하지 않았다.

## 14. B0 클린 빌드 결과

2026-08-26에 사용자가 빌드를 별도로 승인한 뒤, 일반 권한의 새 Windows
PowerShell 창에서 다음 명령을 실행했다. AVG 설정, 예외, 실시간 보호는
변경하지 않았다.

~~~powershell
.\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline
~~~

빌드 증거:

~~~text
runId: 20260826-001928
exitCode: 0
BUILD SUCCESSFUL in 2m 40s
171 actionable tasks: 171 executed
~~~

생성된 Minecraft 1.20.1 런타임 JAR:

~~~text
path:
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar

size:
6494699 bytes

modified:
2026-08-26 00:21:40 +09:00

SHA-256:
DF290A6D5F43F87BC0307D3D292C1D4F595D5661512A6241625E6076AAFE4FD7
~~~

JAR 엔트리 검사에서 다음 두 클래스가 모두 포함된 것을 확인했다.

~~~text
adris/altoclef/commands/DepositAllCommand.class
adris/altoclef/tasks/container/DepositAllTask.class
~~~

전체 로그와 결과 JSON은 각각 다음 위치에 있다.

~~~text
logs/build/chatclef-fabric-clean-build-20260826-001928.log
logs/build/chatclef-fabric-clean-build-20260826-001928.result.json
~~~

이 결과는 clean forced build와 JAR 패키징 성공만 증명한다. CurseForge
인스턴스 배포, Minecraft 실행, 런타임 Mixin 확인, @deposit 회귀 확인,
@deposit_all 런타임 패리티 확인은 수행하지 않았다. 따라서 현재 판정은
`BUILD_PASSED_RUNTIME_NOT_VERIFIED`다.
