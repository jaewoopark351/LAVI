<!-- 20260816_kpopmodder: Added an implementation-status-aware, fail-closed preflight plan for Fabric ChatClef live mutating tests and Minecraft latest.log fallback. -->
<!-- 20260817_kpopmodder: Documented the user-approved elevated retry boundary for antivirus-blocked read-only Windows probes. -->
<!-- 20260818_kpopmodder: Reconciled the live-test contract after restoration and separated terminal lifecycle observation from gameplay success. -->
<!-- 20260818_kpopmodder: Fixed audited-baseline terminology, deterministic fail-closed gates, explicit one-shot approval, and preflight/live-result separation. -->
<!-- 20260818_chatgpt: Added explicit expected/partial/prohibited gameplay-effect fields and evidence-complete E2E semantics. -->

# Fabric ChatClef Live Runtime Preflight Plan

상태: 문서 전용 test-only 구현 계획.

이 문서는 Fabric ChatClef live mutating test가 잘못된 LAVI, backend, Minecraft
instance 또는 world에 command를 보내지 않도록 read-only preflight 계약을
정의한다.

이 문서 자체는 프로세스 종료, LAVI/Minecraft 실행, live command 전송,
production DTO/Java payload/WebSocket protocol 변경, 빌드, commit 또는 push를
승인하지 않는다.

## 0. 2026-08-18 원복 후 audited implementation baseline

2026-08-18 read-only 재검토 시작 시점의 audited implementation baseline은
다음과 같다.

```text
branch: minecraft-plugin-fix/alto-clef-infinite-loop
audited implementation baseline: 4239c23
working tree at audit start: clean
```

이 문서에서 `audited baseline 4239c23`은 구현 상태를 판정한 고정 source
snapshot을 뜻한다. 이 문서를 docs-only로 commit하여 repository `HEAD`가 바뀌더라도
아래 구현 상태가 자동으로 갱신되는 것은 아니다. 이후 commit의 구현 상태를 현재
상태로 취급하려면 source와 test를 다시 read-only audit해야 한다.

Audited baseline 4239c23에는
`tests/minecraft_chatclef/runtime/test_lavi_gradio_runtime_lifecycle.py`가
존재한다. 이 test는 명시적 opt-in 아래 한국어 command를 Gradio submit API로 한 번
제출하고, 제출 응답의 `request_id`와 같은 terminal result가 나타날 때까지 status를
polling한다. stale request result는 인정하지 않으며 polling loop에서는 command를
재제출하지 않는다.

Audited baseline 4239c23 test method의 실제 통과 조건은 다음과 같다.

```text
matching request_id
+ terminal status
+ 같은 refresh snapshot에서 active_request_id == null
+ data가 dict이고 result_reason key가 존재하면 nonblank result_reason
```

다음 terminal status 중 어느 하나가 위 조건으로 관찰되어도 test method는
종료된다.

```text
completed
rejected
failed
cancelled
deadline_exceeded
unknown
```

따라서 audited baseline test 통과 의미는 submitted request와 matching된
`terminal lifecycle observed`다. 이것만으로 실제 이동, 블록 파괴,
인벤토리 증가 또는 command 성공을 증명하지 않는다.
`runtime-reported completion`은 최소한 matching `status == completed`를 별도로
요구하고, end-to-end success는 expected gameplay effect와 unexpected side effect를
직접 관찰하는 별도 E2E 계약을 요구한다.

반면 2026-08-16~17 판본과 관련 문서가
`implemented in current dirty working tree`로 기록한 backend/instance/world,
Windows process identity, same-PID, hidden-LAVI, `latest.log`, command 직전 TOCTOU
preflight 구현은 audited baseline 4239c23에 없다. 그 표현은 당시 미커밋
working-tree snapshot의 역사적 evidence일 뿐 현재 구현 상태가 아니다. 아래 기존
설계는 복구 대상 계약으로 유지하되, 구현 상태 해석에서는 이 section과 section 3의
표가 우선한다.

완전한 fail-closed preflight가 복구되고 offline fixture로 검증되기 전까지 audited
baseline mutating test를 무인 자동 실행 대상으로 취급하지 않는다. 실행이 필요하면
exact command/endpoint/backend/instance/world와 one-shot invocation에 대한 별도
사용자 승인을 받고, 운영자가 전용 Fabric instance와 disposable test world 및 아래
안전 조건을 모두 직접 확인한 supervised one-shot run으로만 취급한다.

## 1. 책임과 범위

preflight의 책임:

- 명시적 live/mutating opt-in 확인
- exact command, Gradio URL, backend, instance, world와 one-shot invocation에 묶인
  실행 승인 확인
- mutating mode에서 명시된 loopback Gradio URL과 approved command 확인
- 의도한 Gradio endpoint와 Fabric `4316` listener의 LAVI identity 확인
- backend, connection lifecycle와 idle 상태 확인
- expected Minecraft instance/world 확인
- runtime status에 없는 identity만 명시된 Minecraft `latest.log`로 보완
- command submit 직전 모든 mutable evidence 재확인
- 모든 조건이 맞기 전 command submission 차단
- terminal execution 결과와 분리된 구조화된 `PreflightDecision` 제공

preflight가 하지 않는 일:

- process 생성, 종료 또는 재시작
- port 자동 해제
- Minecraft Java process 제어
- 다른 CurseForge instance wildcard 검색
- production runtime에서 외부 Minecraft 로그 읽기
- timeout, disconnect 또는 submission outcome 불명확 후 command 자동 retry/replay
- IDE/CI/flaky plugin/Codex wrapper의 mutating test 자동 rerun 허용
- terminal result 계약 또는 gameplay-effect oracle 중복 정의
- `active_request_id == null`만으로 Minecraft task 종료나 side-effect 부재 주장

## 2. 관련 문서

- [`fabric-chatclef-hidden-lavi-recovery-runbook.md`](./fabric-chatclef-hidden-lavi-recovery-runbook.md)
- [`fabric-chatclef-bridge-protocol-v1.md`](./fabric-chatclef-bridge-protocol-v1.md)
- [`chatclef-korean-test-strategy.md`](./chatclef-korean-test-strategy.md)
- [`minecraft-backend-separation.md`](./minecraft-backend-separation.md)

Fabric ChatClef와 Forge MineMind는 독립된 sibling backend다. 이 계획은 Fabric
ChatClef에만 적용하며 Forge fallback, 공용 WebSocket server 또는 공용 reconnect
manager를 만들지 않는다.

## 3. 구현 상태

| 기능 | 상태 | 설명 |
| --- | --- | --- |
| live/mutating opt-in | implemented in audited baseline 4239c23 | 두 환경 변수가 모두 있어야 mutating method가 실행됨 |
| submitted request ID matching | implemented in audited baseline 4239c23 | `6d75c6e`부터 stale terminal result 차단 포함 |
| accepted-only 종료 방지 | implemented in audited baseline 4239c23 | matching terminal까지 기다리지만 runtime-reported completion을 뜻하지 않음 |
| Gradio submit API one-shot flow | implemented in audited baseline 4239c23 | test body의 submit 호출은 한 번이며 polling loop는 refresh만 수행 |
| lower adapter `command_request` exactly-once 증거 | unverified | audited baseline test의 submit call count만으로 server/adapter 전달 횟수를 증명하지 않음 |
| initial `active_request_id == null` 확인 | partially implemented in audited baseline 4239c23 | 제출 직전 전체 identity/status TOCTOU gate는 아님 |
| matching terminal과 same-snapshot active clear | implemented in audited baseline 4239c23 | matching terminal을 본 같은 refresh snapshot에서 `active_request_id == null`을 요구 |
| conditional `result_reason` nonblank check | implemented in audited baseline 4239c23 | `data`가 dict이고 key가 존재할 때만 검사; status별 필수 계약은 미확정 |
| explicit mutating `LAVI_GRADIO_URL` gate | absent from audited baseline 4239c23 | audited baseline test는 URL 미설정 시 `47860` default를 사용함 |
| exact approval tuple / command allowlist | absent from audited baseline 4239c23 | command override와 실행 대상을 preflight가 제한하지 않음 |
| backend/instance/world helper | absent from audited baseline 4239c23 | historical dirty-tree evidence만 존재; 복구와 fixture 검증 필요 |
| 명시된 `latest.log` fallback | absent from audited baseline 4239c23 | historical dirty-tree evidence만 존재; strict UTF-8/CP949 계약은 설계로 유지 |
| Windows listener/process identity | absent from audited baseline 4239c23 | read-only probe 복구 필요 |
| selected Gradio endpoint와 `4316` same-PID gate | absent from audited baseline 4239c23 | hidden second LAVI 차단 포함 복구 필요 |
| `connected`/`lifecycle_state` mutating direct gate | incomplete in audited baseline 4239c23 | sibling connection test와 mutating submission gate가 분리돼 있음 |
| command 직전 status/ownership 재확인 | absent from audited baseline 4239c23 | TOCTOU fail-closed gate 복구 필요 |
| submission outcome unknown reconciliation | absent from audited baseline 4239c23 | response loss 뒤 자동 replay 금지와 상태 조정 계약 구현 필요 |
| external automatic rerun 차단 | absent from audited baseline 4239c23 | IDE/CI/flaky/Codex wrapper policy와 fixture 필요 |
| mutating opt-in 뒤 `gradio_client` dependency 부재 처리 | audited baseline 4239c23은 skip; target contract는 fail | selected mutating run을 정상 미실행으로 숨기지 않도록 변경 필요 |
| replacement decode 자동 통과 금지 | absent from audited baseline 4239c23 | strict decode 계약은 설계로 유지 |
| gameplay effect 검증 | absent from audited baseline 4239c23 | 이동, 블록, 인벤토리 변화와 partial/unexpected effect를 직접 확인하지 않음 |
| status별 `result_reason` 계약 | unverified | test strategy에서 별도 확정 |
| production DTO/Java payload identity 확장 | out of scope | instance/world를 payload에 추가하지 않음 |

2026-08-16~17 판본과 관련 문서의
`implemented in current dirty working tree` 표현은 당시 미커밋 snapshot의 역사적
evidence다. audited baseline 4239c23의 구현 상태는 위 표를 따른다. 복구되는
working-tree code는 protocol, test strategy와 이 preflight 계약에 맞춰 offline
fixture로 다시 검증되어야 한다.

## 4. 2026-08-16 runtime/config evidence

2026-08-16 조사 당시:

- `[config]` Fabric ChatClef module enabled
- `[source default]` Fabric endpoint `127.0.0.1:4316`
- `[runtime snapshot]` `4316` connected
- `[source default]` Gradio start port `47860`, max attempts `100`
- `[config]` Gradio override section 없음
- `[runtime snapshot]` effective search range `47860..47959`, selected `47860`
- `[config]` Chess enabled, `127.0.0.1:8790`
- `[config]` GPT-SoVITS enabled, port `9880`

`47860..47959`는 현재 사용자 설정으로 고정한 범위가 아니라, override가 없어서
선택된 당시 코드 default다. 구현은 실제 config와 default를 읽어 effective range를
계산해야 한다.

read-only status snapshot은 다음 필드를 제공했다.

```text
backend_id: fabric_chatclef
endpoint: ws://127.0.0.1:4316
connected: true
lifecycle_state: connected
active_request_id: null
last_result: null
session_id: present
active_generation: 1
instance: absent
world: absent
correlation_id: absent in status snapshot
```

`StatusSnapshotDTO`와 Fabric status provider에는 instance/world 계약이 없다
(`status_snapshot_dto.py:15-79`,
`fabric_chatclef_websocket_server.py:110-143`). production DTO나 Java payload를
변경하지 않고, test-only fallback이 명시된 로그 경로를 fail closed로 읽는다.

## 5. 명시적 환경 및 실행 승인 계약

복구되는 자동 mutating preflight는 다음 값을 명시적으로 요구한다.

```powershell
$env:LAVI_MINECRAFT_RUNTIME_TESTS = '1'
$env:LAVI_MINECRAFT_RUNTIME_MUTATING = '1'
$env:LAVI_GRADIO_URL = 'http://127.0.0.1:47860'
$env:LAVI_MINECRAFT_RUNTIME_KOREAN_COMMAND = '돌 1개 가져와줘'
$env:LAVI_MINECRAFT_EXPECTED_BACKEND = 'fabric_chatclef'
$env:LAVI_MINECRAFT_EXPECTED_INSTANCE = 'LAVI_TEST_Fabric01'
$env:LAVI_MINECRAFT_EXPECTED_WORLD = '새로운 세계2'
$env:LAVI_MINECRAFT_INSTANCE_LOG_DIR = 'C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs'
```

복구 대상 contract에서는 audited baseline test의 `LAVI_GRADIO_URL` default
fallback을 안전 근거로 사용하지 않는다. URL은 preflight가 시작되기 전에
명시되어야 하며, local Windows listener/process ownership을 검증하는 이 계획에서는
`127.0.0.1` 또는 검증된 `::1` loopback endpoint만 허용한다. non-loopback URL과
해석이 모호한 hostname은 지원하지 않고 command submission 전에 `fail`한다.

사용자 승인은 다음 exact tuple과 한 번의 invocation에 묶인다.

```text
command text
Gradio URL
backend
Minecraft instance
world/save directory
one-shot invocation
```

기본 smoke command candidate는 `돌 1개 가져와줘`다. 이 default도 invocation별
fresh approval을 대체하지 않는다. command text, endpoint, backend, instance 또는
world가 바뀌거나 command override를 사용하면 기존 승인을 재사용하지 않고 별도
사용자 승인을 받아야 한다.

두 opt-in 환경 변수는 mutating run을 선택하는 기술적 gate일 뿐 사용자 승인
evidence가 아니다. 사용자 승인 자체는 source, environment 또는 runtime status에서
자동 추론할 수 없다. 구현 전에 exact tuple을 runner에 전달하고 observed values와
대조할 별도의 test-only approval input 또는 supervised approval record를 하나로
고정해 offline fixture로 검증한다. 그 mechanism이 고정되기 전에는 환경 변수가
채워졌다는 이유만으로 승인을 추정하지 않으며 supervised one-shot 경로만 사용한다.

판정은 deterministic하게 적용한다.

- live opt-in 또는 mutating opt-in이 없음: `skip`; mutating submit 호출 없음
- 두 opt-in이 모두 설정된 뒤 required value 또는 `gradio_client` dependency가
  없거나 unsupported임: `fail`
- 두 opt-in이 모두 설정된 뒤 identity/evidence가 unknown, inaccessible, stale,
  unstable 또는 ambiguous임: `fail`
- observed identity mismatch 또는 command/approval tuple mismatch: `fail`
- 모든 required evidence가 일치함: `ok`

`skip`은 mutating live run이 선택되지 않은 경우에만 사용한다. 실행이 선택된 뒤의
증거 부족을 정상적인 미실행으로 숨기지 않는다. 운영 UI가 이를 `blocked`라고
표시할 수는 있지만 구조화된 preflight status는 `fail`이며 Gradio submit call
count는 `0`이어야 한다.

CI와 일반 offline test에서는 opt-in 값을 `0` 또는 미설정으로 유지한다. mutating
method는 test framework retry, flaky rerun, CI job retry, IDE failed-test rerun 또는
Codex wrapper automatic rerun 대상에서 제외한다.

## 6. preflight 실행 순서

```text
explicit live/mutating opt-in
  -> exact approval tuple과 approved command 확인
  -> explicit loopback LAVI_GRADIO_URL 확인
  -> effective Fabric/Gradio port 설정 계산
  -> selected Gradio URL listener identity
  -> 4316 listener와 selected Gradio listener가 같은 intended LAVI인지 확인
  -> fallback range에 두 번째 LAVI candidate가 없는지 확인
  -> runtime status backend == fabric_chatclef
  -> connected == true
  -> lifecycle_state == connected
  -> active_request_id == null
  -> runtime status의 instance/world 사용 가능 여부 확인
  -> 누락된 값만 명시된 Minecraft latest.log fallback으로 보완
  -> expected instance/world와 정확히 비교
  -> approved command와 tuple을 immutable live-run ticket으로 고정
  -> one-shot guard claim
  -> actual gateway URL, status, listener ownership, approval tuple과 ticket을 재검증
  -> 모두 통과한 직후 ticket.command로 /on_submit_korean_command_click 1회 호출
```

<!-- 20260819_kpopmodder: Closed the guard-to-submit TOCTOU and gateway identity gap. -->

최종 재검증은 guard claim 뒤에 수행한다. guard 또는 다른 callback이 mutable
environment를 바꾸더라도 submit은 environment의 command를 다시 읽지 않고 ticket에
고정된 command만 사용한다. actual gateway object가 가리키는 loopback URL도 승인된
URL과 initial/final 두 경계에서 같아야 한다. 이 최종 재검증과 submit 사이에는 다른
callback, approval normalization 또는 command translation을 삽입하지 않는다.

Audited baseline 4239c23 test가 직접 보장하는 one-shot 범위는 test body의 Gradio
submit API 호출 한 번이다. 그 아래 server/adapter가 Fabric `command_request`를
정확히 한 번 전달했는지는 별도 instrumentation 또는 correlation evidence로
관찰해야 하며 submit call count만으로 추정하지 않는다.

Submit request가 server에 도착했을 가능성이 있지만 accepted response를 받기 전에
timeout, connection reset 또는 malformed response가 발생하면
`submission_outcome_unknown`으로 기록한다. 이 경우 원래 command를 자동 재제출하지
않고 `active_request_id`, `last_result`, request/correlation evidence와 Minecraft
state를 운영자가 reconcile하기 전 다음 mutating run을 차단한다.

Command submission 이후 lifecycle, runtime-reported completion과 gameplay effect
판정은 `chatclef-korean-test-strategy.md`가 소유한다.

## 7. Windows listener/process identity preflight

Audited baseline 4239c23 상태: `[absent; historical dirty-tree implementation evidence only]`.

이 probe는 process를 종료하지 않고 다음 정보만 읽는다.

- Fabric port `4316` listener PID
- `LAVI_GRADIO_URL`의 실제 listener PID
- effective Gradio fallback range의 listener PID
- candidate process의 Name, PID, PPID, CreationDate, ExecutablePath, CommandLine
- candidate의 ancestor chain
- candidate의 parsed invocation mode와 primary operand
- exact resolved entrypoint와 repository provenance
- 승인 launcher가 필요한 경우 launcher PID, CreationDate, ExecutablePath,
  resolved path
- read-only runtime status

Production app은 repository `main.py`, `python -m lavi`, `python -m lavi app`,
`run.bat`, `run_lav_dev.cmd`를 통해 시작될 수 있다. 그러나 mutating preflight의
승인은 실제 probe가 provenance를 증명한 실행 형식으로 더 좁게 제한한다.

Mutating preflight에서 승인되는 app entrypoint:

- Python의 primary script operand가 exact absolute
  `<repository_root>\main.py`인 direct invocation
- primary script operand가 relative `main.py`이고 exact repository
  `run.bat` 또는 `run_lav_dev.cmd` ancestor가 working-root provenance를
  증명하며 그 launcher identity까지 같은 observation에 결합된 invocation

`python -c ... main.py`의 trailing argument, repository descendant의 다른
`tmp\main.py`, 다른 checkout의 `main.py`, 임의 argv 위치의 launcher 이름은
거절한다. 현재 process probe는 resolved module path를 관찰하지 않으므로
`python -m lavi`와 `python -m lavi app`은 production launch form이더라도
module provenance 구현 전까지 mutating preflight에서 fail closed한다.

통과 조건:

1. `4316`과 selected Gradio endpoint를 같은 검증된 LAVI process가 소유한다.
2. candidate argv와 repository/venv/ancestor evidence가 승인 entrypoint와 맞는다.
3. selected Gradio URL의 runtime status가 `backend_id=fabric_chatclef`를 반환한다.
4. effective fallback 범위에 별도 LAVI candidate가 없다.

다음은 단독 실패 조건이 아니다.

- `47861`을 unrelated process가 사용함
- Chess가 비활성이라 `8790`이 없음
- GPT-SoVITS가 별도 PID로 `9880`을 사용함

`8790`과 `9880`은 LAVI identity의 필수 조건으로 사용하지 않는다.

access denied, process disappearance, PID reuse 또는 source evidence 모순은
fail closed한다. process probe는 실제 사용자 Python/Java process를 종료하지
않으며, offline test가 만든 loopback fixture만 test teardown에서 정확한 PID로
정리할 수 있다.

### 7.1 AVG 또는 권한 차단 시 운영자 인계

Windows listener/process probe가 AVG Behavior Shield 탐지(예:
`IDP.HELU.PSE88`) 또는 접근 거부로 실행되지 않으면 자동 preflight는 다음 계약을
따른다.

1. 자동으로 관리자 권한을 요청하거나 elevated child process를 생성하지 않는다.
2. AVG, Behavior Shield 또는 다른 보안 기능을 끄지 않으며 광범위한 예외를 추가하지
   않는다.
3. mutating opt-in 전에는 해당 live method를 `skip`할 수 있다. 두 opt-in이 모두
   설정된 뒤 probe evidence를 얻지 못하면 `process_identity` 단계에서 `fail`하고
   Gradio submit API를 호출하지 않는다.
4. 차단된 실행 파일, 탐지 이름, 읽기 전용 명령의 목적과 종료 상태만 운영자에게
   보고한다. encoded payload 전체를 증거로 재사용하거나 자동 allowlist 대상으로
   만들지 않는다.
5. 사용자가 exact read-only probe 재시도를 명시적으로 승인한 경우에만 recovery
   runbook의 "AVG 또는 권한 차단 시 관리자 read-only 재시도" 절차로 한 번
   인계한다.

관리자 권한은 Windows 조회 권한 문제를 구분하기 위한 운영자 승인 재시도 수단일
뿐, 백신 탐지를 우회한다는 보장이 아니다. 관리자 창에서도 AVG가 다시 차단하면
추가 재시도하지 않고 중단한다. 이 실패를 supervised live command 승인으로
대체하거나 자동 preflight `ok`로 승격하지 않는다. PowerShell 의존성을 제거하는
구현은 별도 코드 변경 승인과 검증이 필요한 후속 작업이며 이 문서는 그 변경을
승인하지 않는다.

## 8. bridge와 idle gate

Audited baseline 4239c23 상태:
`[partial initial-idle check only; full preflight absent]`.

runtime status에서 최소한 다음을 요구한다.

```text
backend_id == fabric_chatclef
connected == true
lifecycle_state == connected
active_request_id == null
```

command submission 직전에 다시 조회한다. preflight 시작 때 idle이었어도 그 사이
다른 요청이 시작되거나 listener ownership이 바뀔 수 있기 때문이다.

`active_request_id`가 존재하면 `fail`하고 새로운 mutating command를 보내지 않는다.
진행 중 request를 자동 cancel하지 않으며, 운영자가 recovery runbook과 정상
stop/cancel 정책에 따라 처리한다.

`active_request_id == null`은 Python-side lifecycle이 idle 또는 clear로 보인다는
증거다. 이것만으로 Java/ChatClef task가 확실히 중단됐거나 Minecraft 이동과
partial world/inventory mutation이 남지 않았다고 판단하지 않는다. timeout,
disconnect, `unknown` 또는 `submission_outcome_unknown` 뒤에는 runtime status와
Minecraft state를 reconcile하기 전 다음 mutating run을 차단한다.

## 9. Minecraft `latest.log` 소유 관계

`latest.log`는 LAVI 애플리케이션 로그가 아니라 다음 인스턴스가 생성한
**Minecraft/Fabric runtime log**다.

```text
C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs\latest.log
```

[source/config] 해당 Minecraft client는 `client-1.12.xml`의
`RollingRandomAccessFile` appender를 사용한다.

```text
fileName = logs/latest.log
filePattern = logs/%d{yyyy-MM-dd}-%i.log.gz
rotation = TimeBasedTriggeringPolicy + OnStartupTriggeringPolicy
size-based rotation = 현재 설정에 없음
```

`instance_audit.txt`와 `stdout-logs.txt`는 같은 Minecraft instance `logs` 폴더에
있지만 writer 구현은 `[unknown]`이다. 자동 instance/world fallback 입력으로
사용하지 않는다.

## 10. 2026-08-16 표본에서 확인된 marker와 한계

2026-08-16 현재 `latest.log` 표본:

- 약 1.03 MiB
- 실제 한글 byte sequence는 CP949와 일치
- strict UTF-8 표본이 아님
- 일반 `ReadAllBytes`는 live writer와 sharing violation 발생
- `FileShare.ReadWrite | FileShare.Delete` snapshot 읽기는 성공
- 확인 시점 마지막 byte는 newline이었지만 항상 보장되지 않음

### 10.1 sample evidence

현재 표본에서 확인된 line shape:

```text
Starting integrated minecraft server version 1.20.1
...\Instances\LAVI_TEST_Fabric01\saves\새로운 세계2\...
```

여기서 `새로운 세계2`는 GUI display name 계약이 아니라 save-directory 이름이다.
같은 directory 이름을 재사용해 다른 월드 내용으로 바꾸면 `latest.log`만으로
내용 identity를 구분할 수 없다.

현재 표본에는 `Stopping server` 또는 `Saving worlds` marker가 없었다. 안정적인
server stop marker는 `[unverified]`다.

### 10.2 proposed parser patterns

다음 pattern은 현재 표본에서 도출한 제안이며 반복 실행으로 검증된 장기 계약이
아니다.

```text
start marker:
  Starting integrated minecraft server version <version>

instance/world path:
  ...\Instances\<instance>\saves\<world>\...
```

제안 regex 예시는 다음과 같다.

```regex
(?i)[\\/]Instances[\\/](?P<instance>[^\\/]+)[\\/]saves[\\/](?P<world>[^\\/]+)[\\/]
```

stop marker와 multi-session 선택 규칙은 fixture로 검증되기 전 자동 통과 계약으로
사용하지 않는다.

## 11. fail-closed log fallback 계약

Audited baseline 4239c23 상태:
`[absent; historical dirty-tree implementation evidence only]`.

1. `LAVI_MINECRAFT_INSTANCE_LOG_DIR`가 명시되지 않으면 fallback을 사용하지 않고,
   mutating opt-in 후에는 `fail`한다.
2. wildcard로 다른 CurseForge instance를 검색하지 않는다.
3. 지정된 directory 바로 아래의 `latest.log`만 읽는다.
4. directory parent instance 이름과 log에서 관찰한 instance가 일치해야 한다.
5. runtime status가 instance/world를 제공하면 log fallback을 사용하지 않는다.
6. live file은 sharing을 허용한 snapshot read로 연다.
7. 읽기 전후 size, last-write 또는 file identity가 바뀌면 unstable snapshot으로
   간주하고 제한된 횟수만 read-only 재시도한다. 안정된 snapshot을 얻지 못하면
   `fail`하고 command를 보내지 않는다.
8. incomplete final line과 multibyte 중간 절단은 자동 identity 통과에 사용하지
   않는다.
9. strict UTF-8 decode를 시도하고 실패하면 strict CP949를 시도한다.
10. 두 strict decoder가 모두 실패하면 `fail`하고 identity를 만들지 않는다.
11. `errors="replace"` 결과는 사람이 보는 diagnostics에만 사용할 수 있으며,
    instance/world 자동 통과 근거로 사용하지 않는다.
12. expected와 observed 값이 다르면 command submission 전에 `fail`한다.
13. multiple session 또는 stop 상태를 안정적으로 판정할 수 없으면 automated
    preflight는 `ok`를 반환하지 않는다. mutating opt-in 후에는 `fail`한다.

2026-08-16~17 dirty working-tree의 test-only parser는 전체 snapshot에 strict
UTF-8을 먼저 적용하고 실패하면 strict CP949를 적용한 것으로 기록됐다. 그 구현은
audited baseline 4239c23에 없으므로, 복구 시 두 decoder가 모두 실패하면 identity를
만들지 않고 replacement decode 결과를 자동 통과 근거로 사용하지 않는 계약을
offline fixture로 다시 검증한다.

## 12. bounded policy 값

2026-08-16~17 dirty working-tree test code에는 다음 값이 구현된 것으로 기록됐다.
Audited baseline 4239c23에는 해당 test-only preflight constant가 없다.

```text
maximum age: 15 minutes
maximum size: 128 MiB
```

두 값의 의미:

- production 계약 아님
- Minecraft/Log4j 요구값 아님
- 실제 표본에서 도출된 필수값 아님
- command 오발송을 막기 위한 조정 가능한 fail-closed test policy

문서와 test constant를 함께 관리하고 offline fixture 및 운영 경험으로만 조정한다.
Mutating opt-in 후 limit를 초과하면 더 큰 범위를 임의로 읽거나 limit를 자동
완화하지 않고 `fail`한다. limit 초과는 command submission count `0`으로 끝나야
한다.

## 13. session 판정 정책

2026-08-16 sample 하나로 stop marker와 반복 session의 장기 안정성을 증명하지
못했다. 따라서 단계적으로 적용한다.

### 단계 A: fixture 검증 전

- runtime status의 backend/connected/idle 검증
- 명시된 `latest.log`에서 final integrated-server start segment와 instance/world
  path를 읽음
- stop 또는 session 종료를 신뢰성 있게 판정할 수 없으면 automated preflight는
  `ok`를 반환하지 않음
- mutating opt-in 후 자동 판정이 불가능하면 `fail`
- preflight 복구 전 별도 승인된 supervised one-shot run에서는 운영자가 실제 월드
  진입을 직접 확인하되, 그 수동 확인을 automated preflight 통과 증거로 재사용하지
  않음

### 단계 B: validated stop/session fixture 후

- 검증된 exact start/stop marker로 session을 분리
- 마지막 active session만 선택
- 해당 session 내부의 instance/world path만 사용
- 마지막 stop이 마지막 start보다 최신이면 `fail`
- rotation 직후 identity가 아직 기록되지 않았으면 `fail`

복구된 parser가 이 단계보다 앞서 stop marker 또는 session 정책을 가정하면
`implementation gap`으로 기록한다.

운영자의 실제 월드 확인은 supervised one-shot run을 위한 별도 수동 evidence다.
unverified marker를 자동 preflight `ok`로 바꾸지 않으며, manual override를
무인 runner나 이후 invocation에 재사용하지 않는다.

## 14. terminal result 계약 참조

상세 계약은 `chatclef-korean-test-strategy.md`가 소유한다. 이 문서는 audited
baseline의 관찰 의미와 preflight 경계만 요약한다.

Audited baseline 4239c23의 mutating test flow:

- Gradio submit API는 test body에서 한 번만 호출
- submitted request ID와 동일한 terminal result만 lifecycle 종료 후보
- 이전 request의 stale terminal result는 무시
- matching terminal을 본 같은 refresh snapshot에서
  `active_request_id == null`을 요구
- `data`가 dict이고 `result_reason` key가 존재하면 nonblank인지 확인
- missing `result_reason` 자체는 audited baseline test에서 실패 조건이 아님

Audited baseline 4239c23과 향후 strategy 모두 다음 no-replay 원칙을 따른다.

- observer timeout, disconnect, malformed response 또는 connection reset 뒤 원래
  command 자동 retry/replay 금지
- accepted response 수신 여부가 불명확하면 `submission_outcome_unknown`으로 기록
- `submission_outcome_unknown`을 `not submitted`로 간주하지 않음
- active request, last result, request/correlation evidence와 Minecraft state를
  reconcile하기 전 다음 mutating run 금지
- IDE/CI/flaky plugin/Codex wrapper에 의한 mutating test 자동 rerun 금지
- observer timeout을 command terminal, failure 또는 cancellation으로 추정하지 않음
- observer timeout 뒤 자동 `@stop` 또는 자동 cancel을 전송하지 않음

판정 용어를 다음처럼 분리한다.

```text
terminal lifecycle observed:
  matching request_id
  + terminal status
  + active request clear

runtime-reported completion:
  terminal lifecycle observed
  + status == completed

gameplay effect observed:
  terminal status와 독립적으로 expected, partial 또는 unexpected Minecraft state/effect를 관찰

end-to-end success:
  runtime-reported completion
  + gameplay observation complete
  + expected gameplay effect verified
  + prohibited effect absence verified
```

`failed`, `rejected`, `cancelled`, `deadline_exceeded` 또는 runtime terminal status
`"unknown"`은 lifecycle 종료일 뿐 runtime-reported completion이 아니다. 여기서
runtime value `"unknown"`은 observation 자체가 불가능한 tri-state `unknown`과
구분한다. 실패 또는 취소 전에 이동, 블록 파괴, 인벤토리 변화 같은 partial side
effect가 이미 발생할 수 있으므로 `gameplay_effect_observed`,
`partial_gameplay_effect_observed`와 `unexpected_effect_observed`는 terminal status와
독립적으로 기록한다.

`prohibited effect not observed`는 observation source가 불완전할 때 부재 증거가
아니다. end-to-end success에는 command별 oracle이 필요한 state surface를 모두
관찰했고 `prohibited_effect_absence_verified == true`라는 적극적인 부재 검증이
필요하다.

Audited baseline 4239c23은 matching terminal과 active clear가 같은 refresh
snapshot에 있어야 통과한다. 향후 bounded follow-up refresh window를 허용하려면
test strategy에서 별도 계약과 timeout을 명시하고 offline fixture로 검증해야 하며,
그 follow-up window에서도 command를 재제출하지 않는다.

현재 generic DTO의 `data`가 dict라는 이유만으로 모든 status의
`result_reason`을 optional 또는 required라고 확정하지 않는다. 다음 status별 생성
경로를 추적한 뒤 strategy에서 required/optional/status-specific을 결정한다.

```text
completed
rejected
failed
cancelled
deadline_exceeded
unknown
```

그 전까지 status별 `result_reason` 계약 상태는 `[unverified]`다.

## 15. 구조화된 결과

Preflight와 command 실행 관찰을 하나의 상태 구조에 섞지 않는다. preflight `ok`는
command 성공이 아니며, live result가 preflight 판정을 소급해서 바꾸지 않는다.

```text
PreflightDecision:
  status: ok | skip | fail
  selected_mutating_run: true | false
  stage:
    opt_in
    approval
    endpoint
    port_configuration
    port_ownership
    process_identity
    bridge
    idle
    instance
    world
    log_snapshot
    pre_submit_recheck
  reason: 사람이 바로 조치할 수 있는 한 줄 설명
  observed:
    gradio_url
    approved_command_is_default
    approved_command_fingerprint
    approval_source
    approval_tuple_verified
    fabric_endpoint
    backend
    connected
    lifecycle_state
    active_request_id
    instance
    world
    identity_source
    listener_pid_by_port
    process_entrypoint
    intended_lavi_pid
    intended_lavi_creation_date
    intended_lavi_executable_path
    process_invocation_mode
    resolved_entrypoint_path
    entrypoint_provenance
    repository_root
    approved_ancestor: null | object
    approved_ancestor fields when present:
      process_id
      parent_process_id
      creation_date
      executable_path
      invocation_mode
      resolved_entrypoint_path
      entrypoint_provenance
    process_identity_fingerprint
    log_encoding
    log_snapshot_stable
    pre_submit_recheck_passed
```

`PreflightDecision.status == skip`은 `selected_mutating_run == false`일 때만 사용한다.
두 opt-in 뒤 required evidence가 부족하거나 모순되면 `fail`이며 submit call count는
`0`이다. `terminal`은 preflight stage가 아니다.

```text
LiveRunObservation:
  submission_outcome:
    not_attempted
    accepted
    submit_response_not_accepted
    submission_outcome_unknown
  gradio_submit_call_count: nonnegative integer | unknown
  adapter_command_request_count: nonnegative integer | unknown
  automatic_resubmit_count: nonnegative integer
  automatic_rerun_count: nonnegative integer
  submitted_request_id: value | absent | unknown
  connection_state_verified: true | false | unknown
  connection_state_error: controlled message | absent
  terminal_lifecycle_observed: true | false | unknown
  terminal_request_id: value | absent | unknown
  terminal_status:
    completed | rejected | failed | cancelled | deadline_exceeded | "unknown" | absent
  active_request_clear: true | false | unknown
  active_clear_observation:
    same_snapshot | bounded_followup | not_observed | unknown
  observer_timeout: true | false
  runtime_reported_completion: true | false | unknown
  gameplay_observation_complete: true | false | unknown
  gameplay_effect_observed: true | false | unknown
  expected_gameplay_effect_verified: true | false | unknown
  partial_gameplay_effect_observed: true | false | unknown
  unexpected_effect_observed: true | false | unknown
  prohibited_effect_absence_verified: true | false | unknown
  end_to_end_success: true | false | unknown
  reconciliation_required: true | false
```

Audited baseline 4239c23의 `active_clear_observation` 통과값은 `same_snapshot`뿐이다.
`bounded_followup`은 strategy와 fixture가 별도로 승인된 이후에만 사용할 수 있다.

Accepted submission 이후의 terminal observer도 각 status snapshot에서 하나의
unambiguous bridge object를 선택한 뒤 exact Fabric backend, enabled, connected 및
`lifecycle_state == connected`를 다시 확인한다. 이 연결 증거가 누락되거나
모순되면 `connection_state_verified == false`,
`reconciliation_required == true`로 남기고 같은 snapshot의 completed 값만으로
batch를 다음 단계로 진행하지 않는다.

`end_to_end_success == true`는 runtime-reported completion,
`gameplay_observation_complete`, expected gameplay effect와 prohibited-effect absence가
모두 `true`일 때만 허용한다. observation source가 불완전하면 false absence를
만들지 않고 관련 필드와 E2E를 `unknown`으로 유지한다.

`submission_outcome_unknown`, runtime terminal status `"unknown"`, observer timeout,
active-clear failure, partial/unexpected effect 또는 incomplete gameplay observation이
있으면 `reconciliation_required`를 `true`로 기록한다.

두 결과에 포함하지 않는 값:

- 전체 환경 변수 dump
- credential, token 또는 개인 대화 내용
- Minecraft Java 전체 command line
- `latest.log` 전체 원문
- unrelated process 전체 목록

allowlist 밖의 arbitrary user command를 diagnostics에 그대로 저장하지 않는다. 별도
승인된 command가 필요한 경우 supervised run record에는 approval tuple과 대조할 수
있는 stable fingerprint만 남긴다.

## 16. offline test 계획

### 16.1 opt-in, approval와 bridge

- live opt-in 없음 -> skip, submit 0회
- mutating opt-in 없음 -> skip, submit 0회
- 두 opt-in 뒤 `LAVI_GRADIO_URL` 없음 -> fail; default endpoint 사용 금지
- 두 opt-in 뒤 required `gradio_client` dependency 없음 -> fail; skip 금지
- non-loopback 또는 해석이 모호한 Gradio URL -> fail
- expected backend/instance/world 없음 -> fail
- log fallback이 필요한데 log directory 없음 -> fail
- default smoke command candidate -> fresh approval tuple이 일치하면 통과 후보
- command override인데 별도 exact approval 없음 -> fail
- approval tuple의 endpoint/backend/instance/world가 observed와 다름 -> fail
- backend mismatch -> fail
- disconnected -> fail
- lifecycle not connected -> fail
- active request 존재 -> fail, command 미전송
- command 직전 status, listener ownership 또는 approval tuple이 바뀜 -> fail,
  command 미전송

### 16.2 Windows process identity

- 대상 port가 LISTEN하지 않음 -> fail
- `4316`과 selected Gradio endpoint가 같은 verified LAVI PID -> 통과 후보
- 두 port의 owner PID가 다름 -> fail
- `4316`을 unrelated process가 소유 -> fail
- selected Gradio endpoint가 unrelated process -> fail
- fallback range에 두 번째 LAVI candidate 존재 -> fail
- unrelated process만 `47861` 사용 -> 단독 실패 아님
- candidate가 조회 사이 사라지는 PID reuse/TOCTOU -> fail
- process command line access denied -> opt-in 뒤 fail
- AVG/권한 차단 뒤 승인된 elevated read-only 재시도 1회
- elevated 재시도도 차단됨 -> fail, 추가 재시도 없음
- exact absolute repository `main.py` primary operand -> 통과 후보
- relative `main.py`인데 approved launcher provenance 없음 -> fail
- `python -c "..." main.py` -> fail
- repository descendant `tmp\main.py` -> fail
- 다른 checkout의 `main.py` -> fail
- `main.py` 또는 launcher path가 trailing argument일 뿐임 -> fail
- resolved module provenance 없는 `-m lavi`, `-m lavi app` -> fail
- exact approved launcher ancestor + relative primary `main.py` -> 통과 후보
- 같은 PID/CreationDate라도 executable, invocation mode, resolved entrypoint,
  repository root 또는 launcher provenance가 바뀜 -> fail
- required structured identity field가 누락됨 -> fail
- Chess 비활성으로 `8790`이 없음 -> 단독 실패 아님
- GPT-SoVITS 별도 PID가 `9880` 사용 -> 단독 실패 아님

### 16.3 `latest.log` fixtures

- 정상 CP949 world enter -> 통과 후보
- strict UTF-8 fixture -> 통과 후보
- 다른 instance -> fail
- 다른 world -> fail
- final segment에 identity 없음 -> fail
- 마지막 start 이후 검증된 stop -> fail
- stop 이후 새 start와 exact identity -> 통과 후보
- stale log -> fail
- 128 MiB 초과 -> fail
- final line truncated -> fail
- CP949 multibyte 중간 절단 -> fail
- 두 strict decoder 모두 실패 -> fail
- replacement decode에서만 identity 문자열이 보임 -> fail
- stat과 open 사이 rotation -> bounded read-only retry 뒤 fail
- snapshot read 중 file size 변화 -> bounded read-only retry 뒤 fail
- startup rotation 직후 identity 미기록 -> fail
- 같은 파일의 ambiguous multiple world session -> fail
- directory parent instance와 내부 path mismatch -> fail
- 다른 instance log가 정답이어도 wildcard 검색 금지

### 16.4 submission, lifecycle와 gameplay observation

- `PreflightDecision.status != ok`이면 Gradio submit API call count == 0
- successful preflight 뒤 test body의 Gradio submit API call count == 1
- Gradio submit API call count > 1 -> no-replay contract violation
- adapter-side `command_request` count는 별도 instrumentation이 있을 때만 assert
- observable adapter-side `command_request` count > 1 -> correlation/replay violation
- accepted response와 nonblank submitted request ID
- submit response가 accepted가 아니거나 request ID가 blank ->
  `submit_response_not_accepted`, 자동 replay 0회
- server 수신 가능성이 있지만 response timeout/connection reset ->
  `submission_outcome_unknown`, 자동 retry 0회
- submitted ID와 matching terminal result
- stale terminal result
- matching terminal과 same-snapshot active request clear
- terminal 뒤 다음 refresh에서만 clear됨 -> audited baseline에서는 통과 아님
- `data`가 dict이고 `result_reason` key가 존재하지만 blank -> audited baseline failure
- `result_reason` key가 없음 -> audited baseline에서는 이 이유만으로 실패하지 않음;
  status별 requiredness는 계속 `[unverified]`
- observer timeout -> terminal/failure/cancel 추정 금지, 자동 stop/replay 0회
- IDE/CI/flaky/Codex wrapper automatic rerun count == 0
- `completed` + gameplay observation complete + expected effect verified + prohibited
  effect absence verified -> end-to-end success
- `completed`지만 expected effect가 false -> runtime-reported completion만 true,
  end-to-end success false
- failed/rejected/cancelled/deadline_exceeded/`"unknown"` -> lifecycle 종료만 기록
- failed/cancelled/deadline_exceeded/`"unknown"` 뒤 partial gameplay effect 관찰 ->
  partial effect true, end-to-end success false, reconciliation required
- expected effect 외 block/inventory/movement mutation -> unexpected effect true
- prohibited effect가 보이지 않았지만 observation source가 불완전 ->
  prohibited-effect absence와 E2E를 `unknown`으로 유지
- effect observation source가 없거나 불완전 -> gameplay observation complete false 또는
  unknown, expected/prohibited evidence와 E2E를 `unknown`으로 유지
- status별 `result_reason` key requiredness는 strategy 확정 후 fixture 추가

offline fixture는 테스트가 직접 만든 loopback/file/process만 사용한다. 실제 사용자
LAVI, Minecraft, Python 또는 Java process를 종료하지 않는다.

## 17. manual live validation

1. 운영자가 exact command, Gradio URL, backend, instance, world와 one-shot
   invocation에 대한 fresh approval tuple과 approval source를 확인한다.
2. 두 opt-in은 실행 선택일 뿐 사용자 승인 자체가 아님을 확인한다.
3. IDE/CI/flaky-test plugin/wrapper/Codex automatic rerun이 비활성인지 확인한다.
4. explicit loopback `LAVI_GRADIO_URL`이 설정됐고 default fallback을 사용하지 않는지
   확인한다.
5. 운영자가 visible foreground LAVI를 한 개만 실행한다.
6. `4316`과 selected Gradio endpoint가 같은 verified PID인지 확인한다.
7. fallback 범위에 두 번째 LAVI candidate가 없는지 확인한다.
8. status가 `fabric_chatclef`, connected, lifecycle connected, idle인지 확인한다.
9. 명시된 `latest.log`가 Minecraft/Fabric runtime log인지 확인한다.
10. strict decode와 stable snapshot으로 expected instance/world가 확인되는지 본다.
11. stop/session marker가 unverified이면 운영자가 실제 월드 진입을 별도 확인한다.
12. gameplay effect를 판정하려면 command 전 inventory/world/location baseline과
    prohibited effect를 먼저 기록한다.
13. one-shot guard claim 뒤 immutable ticket 기준으로 actual gateway URL, status,
    listener ownership과 approval tuple을 다시 확인한다.
14. mutable environment를 다시 읽지 않고 ticket의 approved command를 Gradio submit
    API로 한 번만 제출한다.
15. accepted response를 받기 전 timeout, disconnect 또는 malformed response가
    발생하면 `submission_outcome_unknown`으로 기록하고 재제출하지 않는다.
16. submission outcome unknown이면 active request, last result, correlation evidence와
    Minecraft state를 reconcile하기 전 다음 mutating run을 하지 않는다.
17. accepted response를 받으면 같은 request ID의 terminal result만 기다린다.
18. matching terminal과 같은 refresh snapshot에서 `active_request_id == null`인지,
    `result_reason` key가 존재하면 nonblank인지 audited baseline 동작과 대조한다.
19. observer timeout은 terminal이 아니다. 자동 `@stop`, cancel 또는 replay하지 않고
    현재 task와 effect를 운영자가 확인한다.
20. terminal lifecycle observed, runtime-reported completion, gameplay observation
    completeness, expected/partial/unexpected effect, prohibited-effect absence와
    end-to-end success를 서로 다른 판정으로 기록한다.
21. non-completed terminal에서도 partial gameplay effect가 남았는지 확인한다.
22. stale result가 인정되지 않았고 automatic resubmit과 IDE/CI/Codex automatic
    rerun이 모두 `0`인지 확인한다.
23. 종료할 경우 recovery runbook의 graceful 경로와 cleanup marker를 확인한다.

## 18. 완료 기준

- 구현 상태는 시간 의존적인 unqualified `HEAD`가 아니라 audited baseline commit에
  묶여 기록된다.
- exact command/Gradio URL/backend/instance/world와 one-shot invocation 승인이
  별도 approval source로 확인되며, 두 opt-in만으로 승인을 추정하지 않는다.
- mutating mode에서 explicit loopback `LAVI_GRADIO_URL` 없이 default endpoint를
  사용하지 않는다.
- approved command 외 override는 별도 사용자 승인 없이 제출되지 않는다.
- live run이 선택되지 않았을 때만 `skip`하고, opt-in 뒤 evidence 부족/모순은
  deterministic `fail`로 처리한다.
- intended LAVI listener identity가 read-only로 검증된다.
- `4316`과 selected Gradio endpoint의 owner mismatch에서 command가 나가지 않는다.
- fallback port 자체가 아니라 두 번째 LAVI candidate를 판정한다.
- backend, connected, lifecycle, idle이 command 직전에 확인된다.
- expected backend, instance, world가 모두 일치해야 command가 나간다.
- log fallback은 명시된 한 directory의 `latest.log`에만 제한된다.
- `latest.log`를 Minecraft/Fabric runtime log로 정확히 표현한다.
- live file sharing, rotation, truncation과 strict encoding을 fail closed로 처리한다.
- replacement decode가 자동 identity 통과에 사용되지 않는다.
- 15분/128 MiB를 test policy로만 표현하고 limit 초과를 자동 완화하지 않는다.
- stop/session marker의 미검증 상태를 숨기지 않는다.
- `PreflightDecision`과 `LiveRunObservation` 책임이 분리되고 preflight stage에
  `terminal`을 넣지 않는다.
- audited baseline의 test pass 의미는 matching terminal, same-snapshot active clear와
  conditional nonblank `result_reason` 검사로 정확히 기록된다.
- terminal lifecycle observed와 runtime-reported completion을 구분한다.
- failed/rejected/cancelled/deadline_exceeded/runtime `"unknown"`을 성공으로
  기록하지 않고, runtime status와 observation meta-state를 구분한다.
- gameplay observation completeness와 expected/unexpected/partial effect를 terminal
  status와 독립적으로 기록한다.
- end-to-end success는 matching `completed`, complete gameplay observation, expected
  effect verified와 prohibited-effect absence verified를 요구한다.
- observation source가 불완전하면 `not observed`를 prohibited-effect absence evidence로
  사용하지 않는다.
- submit response가 불명확하면 `submission_outcome_unknown`으로 기록하고 자동
  retry/replay하지 않으며 `reconciliation_required == true`로 남긴다.
- observer timeout을 terminal로 추정하거나 자동 stop/cancel하지 않는다.
- `active_request_id == null`만으로 Minecraft task 종료 또는 partial side-effect 부재를
  증명하지 않는다.
- mutating test가 IDE/CI/flaky plugin/Codex wrapper에서 자동 rerun되지 않는다.
- selected mutating run에서 required `gradio_client` dependency 부재를 `skip`으로
  숨기지 않는다.
- test가 process를 자동 종료하거나 Minecraft Java process를 건드리지 않는다.
- production DTO, Java payload와 WebSocket protocol은 변경하지 않는다.
