<!-- 20260816_kpopmodder: Added an implementation-status-aware, fail-closed preflight plan for Fabric ChatClef live mutating tests and Minecraft latest.log fallback. -->

# Fabric ChatClef Live Runtime Preflight Plan

상태: 문서 전용 test-only 구현 계획.

이 문서는 Fabric ChatClef live mutating test가 잘못된 LAVI, backend, Minecraft
instance 또는 world에 command를 보내지 않도록 read-only preflight 계약을
정의한다.

이 문서 자체는 프로세스 종료, LAVI/Minecraft 실행, live command 전송,
production DTO/Java payload/WebSocket protocol 변경, 빌드, commit 또는 push를
승인하지 않는다.

## 1. 책임과 범위

preflight의 책임:

- 명시적 live/mutating opt-in 확인
- 의도한 Gradio endpoint와 Fabric `4316` listener의 LAVI identity 확인
- backend, connection lifecycle와 idle 상태 확인
- expected Minecraft instance/world 확인
- runtime status에 없는 identity만 명시된 Minecraft `latest.log`로 보완
- 모든 조건이 맞기 전 command submission 차단
- 구조화된 `ok | skip | fail` 결과 제공

preflight가 하지 않는 일:

- process 생성, 종료 또는 재시작
- port 자동 해제
- Minecraft Java process 제어
- 다른 CurseForge instance wildcard 검색
- production runtime에서 외부 Minecraft 로그 읽기
- timeout/disconnect 후 command 자동 retry/replay
- terminal result 계약을 중복 정의

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
| live/mutating opt-in | implemented in current dirty working tree | HEAD 포함 또는 테스트 완료를 뜻하지 않음 |
| submitted request ID matching | implemented in current dirty working tree | stale terminal result 차단 포함 |
| backend/instance/world helper | partially implemented | current dirty runtime test 기준 |
| 명시된 `latest.log` fallback | partially implemented | strict decode와 session 계약 보강 필요 |
| Windows listener/process identity | planned | process를 종료하지 않는 read-only probe |
| selected Gradio endpoint와 `4316` same-PID gate | planned | current dirty helper에는 없음 |
| `connected`/`lifecycle_state` direct gate | planned | status는 제공하지만 preflight에서 완전 사용하지 않음 |
| `active_request_id == null` idle gate | partially implemented | command 전송 직전 재확인 필요 |
| replacement decode 자동 통과 금지 | known implementation gap | diagnostics 전용으로 제한해야 함 |
| status별 `result_reason` 계약 | unverified | test strategy에서 별도 확정 |
| production DTO/Java payload identity 확장 | out of scope | instance/world를 payload에 추가하지 않음 |

`implemented in current dirty working tree`는 확정된 source of truth가 아니다.
확정 계약은 protocol과 test strategy이며, working-tree code는 그 계약에 맞춰
검증되어야 한다.

## 4. 현재 runtime/config evidence

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

## 5. 명시적 환경 계약

현재 전용 live test는 다음 값을 명시한다.

```powershell
$env:LAVI_MINECRAFT_RUNTIME_TESTS = '1'
$env:LAVI_MINECRAFT_RUNTIME_MUTATING = '1'
$env:LAVI_GRADIO_URL = 'http://127.0.0.1:47860'
$env:LAVI_MINECRAFT_EXPECTED_BACKEND = 'fabric_chatclef'
$env:LAVI_MINECRAFT_EXPECTED_INSTANCE = 'LAVI_TEST_Fabric01'
$env:LAVI_MINECRAFT_EXPECTED_WORLD = '새로운 세계2'
$env:LAVI_MINECRAFT_INSTANCE_LOG_DIR = 'C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01\logs'
```

값이 없으면 추측하지 않는다.

- live/mutating opt-in 없음: `skip`
- expected backend/instance/world 없음: `skip`
- log fallback이 필요한데 log directory 없음: `skip`
- observed identity mismatch: command submission 전 `fail`
- identity를 확인할 수 없음: `skip` 또는 fail-closed `fail`

CI와 일반 offline test에서는 opt-in 값을 `0` 또는 미설정으로 유지한다.

## 6. preflight 실행 순서

```text
explicit live opt-in
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
  -> command submit 직전 status와 listener ownership 재검증
  -> 모두 통과한 뒤에만 command_request 1회 전송
```

command submission 후 success 판정은
`chatclef-korean-test-strategy.md`의 terminal result 계약을 따른다.

## 7. Windows listener/process identity preflight

상태: `[planned]`.

이 probe는 process를 종료하지 않고 다음 정보만 읽는다.

- Fabric port `4316` listener PID
- `LAVI_GRADIO_URL`의 실제 listener PID
- effective Gradio fallback range의 listener PID
- candidate process의 Name, PID, PPID, CreationDate, ExecutablePath, CommandLine
- candidate의 ancestor chain
- candidate의 approved entrypoint
- read-only runtime status

승인된 app entrypoint:

- repository `main.py`
- `python -m lavi`
- `python -m lavi app`
- 위 entrypoint를 시작하는 `run.bat` 또는 `run_lav_dev.cmd` chain

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

## 8. bridge와 idle gate

상태: `[planned/partially implemented]`.

runtime status에서 최소한 다음을 요구한다.

```text
backend_id == fabric_chatclef
connected == true
lifecycle_state == connected
active_request_id == null
```

command submission 직전에 다시 조회한다. preflight 시작 때 idle이었어도 그 사이
다른 요청이 시작될 수 있기 때문이다.

`active_request_id`가 존재하면 새로운 mutating command를 보내지 않는다.
진행 중 request를 자동 cancel하지 않으며, 운영자가 recovery runbook과 정상
stop/cancel 정책에 따라 처리한다.

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

## 10. 현재 표본에서 확인된 marker와 한계

2026-08-16 현재 `latest.log` 표본:

- 약 1.03 MiB
- 실제 한글 byte sequence는 CP949와 일치
- strict UTF-8 표본이 아님
- 일반 `ReadAllBytes`는 live writer와 sharing violation 발생
- `FileShare.ReadWrite | FileShare.Delete` snapshot 읽기는 성공
- 확인 시점 마지막 byte는 newline이었지만 항상 보장되지 않음

### 10.1 current-sample evidence

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

상태: `[partially implemented / proposed hardening]`.

1. `LAVI_MINECRAFT_INSTANCE_LOG_DIR`가 명시되지 않으면 사용하지 않는다.
2. wildcard로 다른 CurseForge instance를 검색하지 않는다.
3. 지정된 directory 바로 아래의 `latest.log`만 읽는다.
4. directory parent instance 이름과 log에서 관찰한 instance가 일치해야 한다.
5. runtime status가 instance/world를 제공하면 log fallback을 사용하지 않는다.
6. live file은 sharing을 허용한 snapshot read로 연다.
7. 읽기 전후 size, last-write 또는 file identity가 바뀌면 unstable snapshot으로
   간주하고 제한된 횟수만 재시도한 뒤 `skip`한다.
8. incomplete final line과 multibyte 중간 절단은 자동 identity 통과에 사용하지
   않는다.
9. strict UTF-8 decode를 시도하고 실패하면 strict CP949를 시도한다.
10. 두 strict decoder가 모두 실패하면 `skip/fail closed`한다.
11. `errors="replace"` 결과는 사람이 보는 diagnostics에만 사용할 수 있으며,
    instance/world 자동 통과 근거로 사용하지 않는다.
12. expected와 observed 값이 다르면 command submission 전에 `fail`한다.
13. multiple session 또는 stop 상태를 안정적으로 판정할 수 없으면 자동 통과하지
    않는다.

현재 dirty working-tree code에 replacement decode가 자동 판정 경로로 남아 있다면
이는 `known implementation gap`이다. 이 문서는 코드 수정 승인이 아니며, 별도
구현 단계에서 strict fail-closed로 맞춰야 한다.

## 12. bounded policy 값

현재 dirty working-tree test code에는 다음 값이 구현되어 있다.

```text
maximum age: 15 minutes
maximum size: 128 MiB
```

두 값의 의미:

- production 계약 아님
- Minecraft/Log4j 요구값 아님
- 실제 표본에서 도출된 필수값 아님
- command 오발송을 막기 위한 조정 가능한 fail-closed test policy

문서와 test constant를 함께 관리하고 offline fixture 및 운영 경험으로만
조정한다. limit 초과 시 더 큰 범위를 임의로 읽지 않고 `skip`한다.

## 13. session 판정 정책

현재 sample 하나로 stop marker와 반복 session의 장기 안정성을 증명하지 못했다.
따라서 단계적으로 적용한다.

### 단계 A: fixture 검증 전

- runtime status의 backend/connected/idle 검증
- 명시된 `latest.log`에서 final integrated-server start segment와 instance/world
  path를 읽음
- stop 또는 session 종료를 신뢰성 있게 판정할 수 없으면 live 자동 통과 금지
- manual validation에서 운영자가 실제 월드 진입을 확인

### 단계 B: validated stop/session fixture 후

- 검증된 exact start/stop marker로 session을 분리
- 마지막 active session만 선택
- 해당 session 내부의 instance/world path만 사용
- 마지막 stop이 마지막 start보다 최신이면 `skip`
- rotation 직후 identity가 아직 기록되지 않았으면 `skip`

현재 dirty parser가 이 단계보다 앞서 stop marker 또는 session 정책을 가정하면
`implementation gap`으로 기록한다.

## 14. terminal result 계약 참조

상세 계약은 `chatclef-korean-test-strategy.md`가 소유한다. 이 문서는 다음만
요약한다.

- command는 preflight 통과 후 한 번만 제출
- submitted request ID와 동일한 terminal result만 success 후보
- 이전 request의 stale terminal result는 무시
- timeout 또는 disconnect 후 원래 command 자동 retry/replay 금지
- terminal result 이후 `active_request_id == null` 확인
- `result_reason`은 확정된 status별 strategy 계약을 따름

현재 generic DTO의 `data`가 dict라는 이유만으로 모든 status의
`result_reason`을 optional이라고 확정하지 않는다. 다음 status별 생성 경로를
추적한 뒤 strategy에서 required/optional/status-specific을 결정한다.

```text
completed
rejected
failed
cancelled
deadline_exceeded
unknown
```

그 전까지 `result_reason` 계약 상태는 `[unverified]`다.

## 15. 구조화된 결과

```text
status: ok | skip | fail
stage:
  opt_in
  port_configuration
  port_ownership
  process_identity
  bridge
  idle
  instance
  world
  log_snapshot
  terminal
reason: 사람이 바로 조치할 수 있는 한 줄 설명
observed:
  gradio_url
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
  log_encoding
  log_snapshot_stable
```

결과에 포함하지 않는 값:

- 전체 환경 변수 dump
- credential, token 또는 개인 대화 내용
- Minecraft Java 전체 command line
- `latest.log` 전체 원문
- unrelated process 전체 목록

## 16. offline test 계획

### 16.1 opt-in과 bridge

- live opt-in 없음 -> skip
- mutating opt-in 없음 -> skip
- backend mismatch -> fail
- disconnected -> fail/skip according to strategy
- lifecycle not connected -> fail/skip
- active request 존재 -> fail/skip, command 미전송
- command 직전 status가 바뀜 -> command 미전송

### 16.2 Windows process identity

- 대상 port가 LISTEN하지 않음
- `4316`과 selected Gradio endpoint가 같은 verified LAVI PID
- 두 port의 owner PID가 다름
- `4316`을 unrelated process가 소유
- selected Gradio endpoint가 unrelated process
- fallback range에 두 번째 LAVI candidate 존재
- unrelated process만 `47861` 사용
- candidate가 조회 사이 사라지는 PID reuse/TOCTOU
- process command line access denied
- `main.py`, `-m lavi`, `-m lavi app` 각각의 승인 entrypoint
- base Python listener + venv launcher parent
- Chess 비활성으로 `8790`이 없음
- GPT-SoVITS 별도 PID가 `9880` 사용

### 16.3 `latest.log` fixtures

- 정상 CP949 world enter
- strict UTF-8 fixture
- 다른 instance
- 다른 world
- final segment에 identity 없음
- 마지막 start 이후 검증된 stop
- stop 이후 새 start
- stale log
- 128 MiB 초과
- final line truncated
- CP949 multibyte 중간 절단
- 두 strict decoder 모두 실패
- replacement decode에서만 identity 문자열이 보임
- stat과 open 사이 rotation
- snapshot read 중 file size 변화
- startup rotation 직후 identity 미기록
- 같은 파일의 multiple world session
- directory parent instance와 내부 path mismatch
- 다른 instance log가 정답이어도 wildcard 검색 금지

### 16.4 terminal result

- submitted ID와 matching terminal result
- stale terminal result
- terminal result 뒤 active request clear
- timeout/disconnect 후 submit call이 한 번뿐임
- status별 `result_reason` 계약은 strategy 확정 후 fixture 추가

offline fixture는 테스트가 직접 만든 loopback/file만 사용한다. 실제 사용자
LAVI, Minecraft, Python 또는 Java process를 종료하지 않는다.

## 17. manual live validation

1. 운영자가 visible foreground LAVI를 한 개만 실행한다.
2. `4316`과 selected Gradio endpoint가 같은 verified PID인지 확인한다.
3. fallback 범위에 두 번째 LAVI candidate가 없는지 확인한다.
4. status가 `fabric_chatclef`, connected, lifecycle connected, idle인지 확인한다.
5. 명시된 `latest.log`가 Minecraft/Fabric runtime log인지 확인한다.
6. strict decode와 stable snapshot으로 expected instance/world가 확인되는지 본다.
7. stop/session marker가 unverified이면 운영자가 실제 월드 진입을 별도 확인한다.
8. command submit 직전에 status와 listener ownership을 다시 확인한다.
9. command를 한 번만 제출한다.
10. 같은 request ID의 terminal result를 기다린다.
11. stale result와 자동 replay가 없었는지 확인한다.
12. 종료할 경우 recovery runbook의 graceful 경로와 cleanup marker를 확인한다.

## 18. 완료 기준

- intended LAVI listener identity가 read-only로 검증된다.
- `4316`과 selected Gradio endpoint의 owner mismatch에서 command가 나가지 않는다.
- fallback port 자체가 아니라 두 번째 LAVI candidate를 판정한다.
- backend, connected, lifecycle, idle이 command 직전에 확인된다.
- expected backend, instance, world가 모두 일치해야 command가 나간다.
- log fallback은 명시된 한 directory의 `latest.log`에만 제한된다.
- `latest.log`를 Minecraft/Fabric runtime log로 정확히 표현한다.
- live file sharing, rotation, truncation과 strict encoding을 fail closed로 처리한다.
- replacement decode가 자동 identity 통과에 사용되지 않는다.
- 15분/128 MiB를 test policy로만 표현한다.
- stop/session marker의 미검증 상태를 숨기지 않는다.
- success는 matching terminal result strategy를 따른다.
- test가 process를 자동 종료하거나 Minecraft Java process를 건드리지 않는다.
- production DTO, Java payload와 WebSocket protocol은 변경하지 않는다.
