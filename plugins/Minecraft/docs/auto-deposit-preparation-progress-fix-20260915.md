# 자동 보관 준비·접근 진척 판정 수정

## 변경 범위와 기준

2026-09-15. `LAVI-deposit-fix-full-20260915.zip`을 기준으로 수정했다. 이 기준본은 사용자 `(69).zip`에 이전 채팅·마이크 `deposit_all` 및 자동 보관 진입 조건 수정 13개 파일을 반영한 스냅샷이다. 현재 사용자의 Codex 로컬 변경이나 실행 JAR와 동일하다고 보증하는 버전 정보는 아니다. 배포 담당자는 패키지 `MANIFEST.json`의 before SHA-256 또는 `PATCH.diff`로 로컬 차이를 확인해야 한다.

작업은 ChatGPT 컨테이너의 별도 압축 해제 디렉터리에서 수행했다. `pwd`와 `git rev-parse --show-toplevel`을 확인했으며, 업로드 아카이브에는 `.git`이 없었다. Windows 저장소를 변경하거나 실제 Git diff/status가 있다고 주장하지 않는다. 커밋·푸시·게임 실행·JAR 빌드·배포는 하지 않았다. 이 문서의 diff는 기준 ZIP과 수정 파일을 비교해 생성한 unified diff다.

## 직접 근거

원본 Java 로그 `0d8f9269-86db-4f1b-bdf2-22a31efc4c07.txt`의 1-based 행 번호:

- 2610~2613: 34/36 슬롯에서 일반 상자 보관 6단계가 계획됨. 첫 대상은 흙 64개.
- 2865~2868: 지하 Y=-2에서 상자 확보 경로. 아직 보관 전송 없음.
- 7896~7905: tick 2131에 기존 상자 `(-4051,63,-947)` 선택. 플레이어 Y=53.
- 8355~8361: tick 2410에도 같은 상자에 접근 중. 플레이어 Y≈62.42, 흙 저장량 0/64.
- 8436~8445: 최종 `BUDGET_EXHAUSTED`, 확인 저장량 0, 점유 슬롯 34→36. 실행 2,400틱, 연속 무진척 2,400틱, `NO_PROGRESS_LIMIT`.
- 8448~8469: `WAIT_FOR_REARM / SAME_FAILURE`. 최종 결과와 다른 fallback `UNEXPECTED_STOP`이 상태 전환 사유에 표시됨.

이 자료는 상자 슬롯 전송 버그 또는 모든 상자 만석을 확정하지 않는다. Python 로그의 자연어 ScreenVision 묘사는 빌드나 명령 실행 증거로 사용하지 않았다.

## 책임과 연결

기존 `AutoDepositRunLedger`가 실행 예산 판정을 계속 소유한다. `AutoDepositMaintenanceTask`는 자신의 실제 유지 중인 일반 보관 자식만 읽기 포트로 노출한다. `DepositAllTask`에는 기존 선택 목표의 불변 좌표를 반환하는 getter만 추가했다. 이동·제작·GUI 실행은 기존 자식과 Baritone가 그대로 수행한다.

새 `auto/progress`의 세 파일은 독립된 이유가 있다. `AutoDepositProgressSample`은 불변 사실 계약, `AutoDepositProgressReader`는 client-thread 게임 상태 읽기, `AutoDepositProgressTracker`는 순수 Java의 유한한 누적 진척 판정이다. 테스트나 예산 코드가 Minecraft 객체를 보관하거나 진단 로그를 제어 입력으로 읽지 않도록 이 경계만 분리했다. 새 manager, scheduler, event bus, 전역 상태, 백엔드 간 연결은 추가하지 않았다.

연결 순서:

```text
현재 maintenance root의 onTick
  → context 일치와 종료 상태 확인
  → 기존 ledger 실행 tick observer
  → 실제 현재 general child의 준비 사실 읽기
  → 실제 전송 증가 / 준비 진척을 각각 계산
  → 기존 유한 실행 예산에 한 번 반영
  → 한도 초과 시 구체적 budget status로 종료 후보 확정
  → 이미 결정된 값을 기존 진단 채널에 전달
```

## 인정하는 진척

### 접근

이미 선택한 상자 또는 현재 자식의 `MineOrCollectTask.miningPos()`가 활성 native goal과 일치해야 한다. 알려진 고정 블록 goal인 정확한 `GoalBlock`, `GoalNear`, `GoalTwoBlocks` 클래스만 받는다. goal의 렌더 좌표는 게임 목표 값으로 읽지만, 진단 전용 캐시·출력 문자열·이벤트 수를 제어 입력으로 쓰지 않는다. native interaction의 `target.up()`은 같은 소유 목표 좌표로 정규화한다.

동일한 소유 목표까지의 실제 거리 기록이 이전 인정 최단 거리보다 최소 1블록 더 줄었을 때만 인정한다. 첫 관측, 목표 객체 교체, phase 변경, 경로 재계산, 단순 왕복, 제자리 흔들림은 인정하지 않는다. 작은 전진은 같은 기준에 누적되어 1블록 감소를 넘었을 때 인정한다. 목표가 없거나 다른 체인에 양보한 구간 뒤에는 다시 기준만 잡아 해당 구간 이동을 보관 진척으로 소급 인정하지 않는다.

직선 거리는 제한된 진척 신호이지 완전한 경로 비용 모델이 아니다. 긴 우회로처럼 목표 직선 거리가 오랫동안 줄지 않는 경우에는 계속 보수적으로 무진척 제한을 적용한다. 원인 없이 제한을 풀거나 이동 알고리즘을 바꾸지 않았다.

### 준비 재료

기존 활성 ResourceTask 자식들의 현재 output ItemTarget만 읽는다. main inventory 수량이 그 목표의 이전 최고 수량을 넘는 경우에만 인정한다. 필요한 수량과 해당 목표의 최초 필요한 수량으로 상한을 제한한다. 아이템 그룹 키는 레지스트리 ID 집합을 정렬하여 고정한다. 무관한 블록 줍기, 수량 소비 후 동일 수량 복구, 스스로 필요한 수량을 늘리는 행동은 새로운 진척이 아니다.

상자 접근 phase에 들어간 뒤 남은 재료 수집 자식은 준비 진척으로 세지 않는다. trusted 보관 및 working-set recovery의 실행 방식은 변경하지 않았고, 이들의 기존 확인된 전송/회수 진척은 그대로 인정한다.

## 유한성, 보호, 실패

- 준비 진척은 연속 무진척 카운터만 초기화한다. 실제 저장 수량, 완료된 작업 수 또는 보관 성공 판정을 만들지 않는다.
- 총 실행 기본 한도 12,000틱, 연속 무진척 2,400틱, 기본 완료 단위 3, 관련 조건 변화 재시도 2회, 관련 기회당 6,000틱은 기존 그대로다. 기존 rearm owner만 제한된 추가 기회를 부여할 수 있다.
- 누적 실행·누적 무진척·완료 단위·실패 이력은 task 재생성으로 지워지지 않는다. 준비 기록은 같은 budget 객체 수명에 귀속된다.
- 의미 있는 조건 변화의 종류와 `SAME_FAILURE` 규칙 자체는 변경하지 않았다. 가득 찼다는 이유만으로 실패 이력을 제거하거나 무조건 재시작하지 않는다.
- 목표 64개, 재료 그룹 64개까지 기록하며, 가득 차면 새 키를 거절하고 기존 키를 제거하지 않는다. 따라서 오래된 왕복 목표를 기록에서 밀어내고 다시 점수를 받을 수 없다.
- reader는 자식 최대 64개, ResourceTask당 검사 출력 최대 16개, 샘플 출력 총 16개, 그룹당 아이템 64개, 그룹 키 4,096자까지 제한한다.
- STOP, 기존 자동방어 선점 및 양보, 수동 보관 충돌 검사, KEEP_LOADOUT, working-set, 아이템 우선순위, 상자 안전 필터, exact GUI binding은 유지했다.
- 방어 등으로 active root가 정산되고 다시 만들어져도 기준 수명은 보존하되 관측 연속성을 끊는다. 보관 root가 tick하지 않는 대기 시간은 기존대로 보관 실행 예산에 넣지 않는다.

## 동반 진단

`AutoDepositRearmDiagnostics.executionProgress`가 실제 판정 직후 값을 기존 채널로 전달한다. formatter/sink 실패는 게임 동작이나 종료 결정을 바꾸지 않는다. 예산 소진을 처리하는 terminate 호출은 이 진단 try/catch 바깥에 있다.

- `AUTO_DEPOSIT_EXECUTION_PROGRESS`: phase 진입, 실제 확인 전송/회수 증가, `APPROACH_ADVANCED`, `PREPARATION_RESOURCE_INCREASED`, 기록 용량 초과, budget 종료.
- `AUTO_DEPOSIT_BUDGET_LIMIT`: 기존 일반 lifecycle 채널의 유한한 실제 종료 기록. 세부 진단 OFF에서도 호출한다. 종료 후보가 잡힌 root는 maintenance의 다음 tick 진입에서 걸러 동일 root가 매 tick 반복 종료 기록을 만들지 않는다.
- `AUTO_DEPOSIT_ROOT_SETTLED`: 확정된 `effectiveTerminalReason`과 호출 fallback `callbackReason`을 함께 기록. 상태 전환에는 확정된 결과 사유를 사용한다.

필드에는 현재 목표, 실제 거리, 이전 인정 거리, 재료 증가 전/후/상한, 인정 종류, 현재 phase, 기록 수, 소비 실행/연속 무진척/누적 무진척, 마지막 인정 실행 tick 및 종류, confirmed storage/recovery 수량, 세부 budget status가 포함된다. `lastMeaningfulProgressTick`은 client tick이 아니라 해당 budget에서 소비한 실행 tick 기준이다.

상세 채널은 기존 `AutoDepositBoundaryDiagnostics → ObservationScope`를 그대로 사용한다. 안정된 event/reason을 의미 서명으로 사용하고 좌표·숫자·task ID를 서명에 넣지 않는다. 기존 first/detail/session 한도와 반복 억제가 적용되므로 모든 세부 이벤트의 실제 파일 출력을 보장하지 않는다. 일반 종료와 테스트에서의 sink 호출 검증을 실제 게임 로그 파일 영속화 증거로 혼동하지 않는다.

## 실행한 검증

1. 실제 production tracker, sample, budget, rearm, ledger, reader, 진단 코드를 `javac --release 17`로 컴파일하고 오프라인 회귀 56케이스 통과.
   - 핵심 판정/정책 30케이스
   - 실제 ledger 및 종료/진단 연결 13케이스
   - 실제 reader의 명시적 게임 경계 대체 객체 테스트 13케이스
2. 수정·추가한 production 9파일과 test 2파일을 JDK compiler parser로 Java 17 구문 검사: 11파일, 오류 0.
3. 기존 생산 Java 중 선언한 6파일만 변경되었고, 이전 Fabric ChatClef Python 파일과 native Task/TaskRunner/이동/보관 실행 및 재시도 조건 소스는 기준본과 동일함을 바이트 비교.

56은 JUnit 실행 횟수가 아니라 이름이 붙은 오프라인 회귀 케이스 수다. `src/test/java`의 JUnit wrapper는 추가했지만 Gradle/JUnit runner로 실행하지 않았다. `test/test_Isolation/.../fixtures`의 Minecraft, 일부 Task/maintenance, Baritone 및 로그 sink는 명시적 대체 객체다. 따라서 maintenance와 pressure chain의 전체 native 실행, mapped classpath compile, 전송 서버 응답, GUI 바인딩, 실제 방어 동작까지 검증한 결과가 아니다. 긴 접근 케이스는 로그 흐름을 모델링한 합성 입력 테스트이며 게임 로그 전체 재생 또는 실게임 재현이 아니다.

실행 근거는 같은 패키지의 `test/test_Isolation/minecraft_auto_deposit_progress/evidence/`에 있다. 생성된 `.class`와 실행 임시 폴더는 패키지에서 제외했다.

## Codex 인수와 실게임 확인

로컬 변경을 백업하고 기준 해시 또는 diff를 대조한 뒤 source patch를 병합한다. 이 패키지는 이전 13파일 수정 위에 추가하는 패치이며 Python 수정본을 다시 덮어쓰지 않는다. 전체 원본 프로젝트가 아니다.

오프라인 재검증은 프로젝트 루트에서:

```powershell
python test/test_Isolation/minecraft_auto_deposit_progress/run_offline_tests.py
```

Codex가 AGENTS.md와 기존 빌드 절차를 읽고 Fabric ChatClef 1.20.1 대상의 실제 Gradle 테스트·컴파일·JAR 생성을 수행해야 한다. 사용자 요청대로 이 작업에서는 JAR를 생성하지 않았다. 다른 Minecraft 버전, dependencies, wrapper, shared engine을 임의 변경하지 말 것. 기존 JAR를 새 파일로 교체한 뒤 게임을 완전히 재시작해야 이전 WAIT_FOR_REARM 메모리 상태 및 클래스가 남지 않는다. 생성 JAR와 배포 JAR의 SHA-256을 비교한다. 커밋·푸시 권한은 없다.

실게임 합격은 움직였다는 사실이 아니라 기존 confirmedStoredItems 증가와 실제 점유 슬롯 감소로 판단한다. 재현 환경에서 준비 목표 진척 → 상자 접근 → 검증된 GUI → 전송 → 작업 후 보호조건 확인까지 이어져야 한다. 정지·접근 불가·상자 만석 시 유한 종료, 동일 실패 반복 차단, 자동방어 양보 및 STOP 보존도 확인한다.
