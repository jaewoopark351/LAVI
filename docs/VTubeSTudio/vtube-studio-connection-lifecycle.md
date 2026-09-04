# VTube Studio 연결 생명주기와 자동 재연결 계약

<!-- 20260904_kpopmodder: VTube Studio 미실행 상태에서도 LAVI가 시작되고 런타임 재연결을 수행해야 하는 계약을 기록했다. -->

## 1. 문서 상태

- 작성일: 2026-09-04
- 적용 범위: `plugins/VtubeStudio/**`, VTube Studio provider 선택 경계, 관련 시작 및 종료 테스트
- 기준 엔드포인트: `ws://localhost:8001`
- 현재 상태: `IMPLEMENTED_FOCUSED_TESTS_PASSED_TRANSPORT_AND_AUTH_LIVE_PASSED_AVATAR_LIVE_NOT_RUN`
- 구현일: 2026-09-04
- 구현 여부: 정적 시작 조건 분리, attempt 기반 재연결, 단계 기반 인증,
  interruptible shutdown, credential-safe bounded log 및 avatar worker 종료를
  구현했다. 실제 VTube Studio transport handshake, 전체 LAVI 인증, 연결 해제
  후 재연결 및 재인증을 검증했으며, avatar controller의 별도 live 검증은
  수행하지 않았다.
- 관찰 로그: `logs/20260904_122149_log.txt`,
  `logs/20260904_150202_log.txt`, `logs/20260904_150443_log.txt`,
  `logs/20260904_151346_log.txt`, `logs/20260904_151953_log.txt`,
  `logs/20260904_152143_log.txt`

이 문서는 VTube Studio 연결을 LAVI의 시작 필수 조건이 아닌 런타임 연결 상태로 취급하기 위한 기준 문서다. 향후 VTube Studio 연결, 인증, 재시도, 종료 또는 plugin availability 동작을 변경할 때 이 계약을 먼저 확인한다.

## 2. 핵심 결정

다음 동작을 보존해야 한다.

```text
VTube Studio가 실행되지 않음
또는
VTube Studio Plugin API 포트가 아직 열리지 않음
    -> LAVI는 정상적으로 시작한다.
    -> VtubeStudio provider 객체를 초기화한다.
    -> 연결 상태는 DISCONNECTED_WAIT로 남는다.
    -> 기존 연결 소유자가 3초 간격으로 재연결을 시도한다.
    -> VTube Studio가 나중에 준비되면 같은 provider가 연결 및 인증을 진행한다.
```

`localhost:8001`의 연결 거부, 타임아웃 또는 일시적 미응답은 LAVI 전체의 시작 실패 사유가 아니다.

Windows에서 configured endpoint의 `localhost`가 `::1`을 먼저 반환하더라도
VTube Studio의 IPv4 listener에 도달해야 한다. 따라서 사용자 설정과 상태 로그의
endpoint는 `ws://localhost:8001`로 유지하고, low-level transport만
`ws://127.0.0.1:8001`로 정규화한다. 이 처리는 명시적인 IPv4 주소나 원격 host를
변경하지 않는다.

다음과 같은 정적 결함과 런타임 연결 실패를 구분한다.

```text
websocket Python 패키지 누락
    -> VtubeStudio provider 사용 불가
    -> 명확한 정적 dependency 진단

잘못된 plugin metadata, import 실패 또는 interface 불일치
    -> VtubeStudio provider 사용 불가
    -> 명확한 contract 또는 load 진단

VTube Studio 포트 미연결
    -> VtubeStudio provider 사용 가능
    -> 런타임 연결 대기 및 3초 재시도
    -> LAVI 시작 계속
```

## 3. 사용자 관점의 기대 동작

사용자는 다음 순서 중 어느 쪽을 사용해도 된다.

### 3.1 VTube Studio를 먼저 실행한 경우

1. LAVI가 VtubeStudio provider를 초기화한다.
2. WebSocket 연결을 즉시 시도한다.
3. VTube Studio가 API 요청을 허용하면 인증한다.
4. 연결된 상태에서 표정, 입 모양 및 자세 제어를 시작한다.

### 3.2 LAVI를 먼저 실행한 경우

1. LAVI가 VtubeStudio provider를 초기화한다.
2. 첫 연결이 실패해도 LAVI UI와 다른 구성요소는 계속 시작한다.
3. VtubeStudio provider는 연결 대기 상태를 표시한다.
4. 연결 소유자는 3초 뒤 다시 시도한다.
5. 사용자가 VTube Studio를 나중에 실행하고 Plugin API를 허용하면 자동으로 연결 및 인증한다.

### 3.3 연결 중 VTube Studio가 종료된 경우

1. 기존 WebSocket 연결을 연결 해제로 기록한다.
2. 인증 상태를 초기화한다.
3. 다른 LAVI 구성요소를 종료하지 않는다.
4. 3초 간격의 재연결 상태로 돌아간다.
5. VTube Studio가 다시 준비되면 연결 및 인증을 복구한다.

### 3.4 LAVI를 종료한 경우

1. 새로운 연결 시도를 금지한다.
2. 현재 WebSocket을 닫는다.
3. 재시도 대기를 즉시 깨운다.
4. 연결 스레드가 제한된 시간 안에 종료됐는지 확인한다.
5. 종료가 완료된 뒤에는 재연결 로그나 네트워크 접근이 발생하지 않는다.

## 4. 수정 전 회귀 원인 기록

### 4.1 관찰된 증상

2026-09-04 실행에서 다음 순서가 관찰됐다.

```text
12:21:45  VTube Studio 프로세스 시작
12:21:52  LAVI plugin discovery가 VtubeStudio를 검사
12:23:10  vtuber provider 선택
12:23:10  No usable provider for required plugin category vtuber
12:23:10  AppComposer startup 실패
```

오류의 `available=['VtubeStudio']`는 READY 상태를 의미하지 않는다. 현재 선택 구현은 provider 목록에 들어 있는 이름을 `available`로 출력하므로, 이미 `UNAVAILABLE`인 provider도 이 목록에 나타난다.

관찰 당시 다음 항목은 정상임을 확인했다.

- `modules.json`에서 `VtubeStudio`가 활성화됨
- `websocket-client` 패키지가 가상환경에 설치됨
- VtubeStudio module import 가능
- `VtuberPluginInterface` 구현 정상
- plugin metadata와 lifecycle contract 검증 정상
- pose 설정 JSON 존재 및 파싱 가능
- 같은 날 이전 실행에서 VtubeStudio provider 초기화 성공 기록 존재

최신 확인 시 동일한 TCP availability probe는 `localhost:8001`에 연결할 수
있었다. 이 probe는 IPv6 실패 후 IPv4로 넘어갈 수 있으므로, 그 결과만으로
1초 제한을 사용하는 websocket-client transport까지 정상이라고 판단하지 않는다.
12시대의 시작 실패는 VTube Studio 프로세스가 시작된 직후 plugin discovery가
먼저 실행되어 API 포트가 아직 준비되지 않았거나 짧은 probe 제한 안에 응답하지
못한 시작 시점 경쟁으로 분류한다.

분류:

```text
HIGH_CONFIDENCE_TRANSIENT_SERVICE_PREFLIGHT_REGRESSION
```

### 4.2 런타임 연결 transport 회귀

시작 조건을 분리한 뒤의 `150202`, `150443` 실행에서는 provider가 `RUNNING`으로
초기화되고 단일 worker가 attempt 32 이상까지 재시도했다. 그러나 모든 attempt가
약 1초 뒤 `connection_timeout`으로 끝났으며 `connection_established`와 인증 로그는
한 번도 발생하지 않았다.

동일 시점의 로컬 상태와 직접 handshake 비교는 다음과 같았다.

```text
VTube Studio listener     -> 0.0.0.0:8001 (IPv4)
localhost 첫 해석         -> ::1 (IPv6)
ws://localhost:8001       -> TimeoutError
ws://127.0.0.1:8001       -> WebSocket handshake 성공
```

신규 session의 1초 connect timeout이 IPv6 시도에서 모두 소비되고,
websocket-client가 `TimeoutError`에서 다음 IPv4 주소로 넘어가지 않는 것이
직접적인 원인이었다.

분류:

```text
HIGH_CONFIDENCE_IPV6_FIRST_CONNECT_TIMEOUT_REGRESSION
```

교정은 configured endpoint와 상태 로그의 `localhost`를 유지하면서 low-level
localhost transport만 `127.0.0.1`로 정규화하는 것이다. timeout을 늘리는 방식은
매 연결마다 불필요한 IPv6 대기를 추가하므로 사용하지 않는다.

초기 `UNAVAILABLE` 상세 사유가 시작 로그에 직접 출력되지 않았으므로, 과거 시점의 포트 상태 자체는 사후에 완전하게 재현할 수 없다. 이 한계는 향후 진단 로그 요구사항에 반영한다.

### 4.3 수정 전 실패 경로

현재 실패 경로는 다음과 같다.

```text
PluginLoader discovery
    -> required_services의 localhost:8001 TCP probe
    -> probe 실패
    -> PluginHandle 상태를 UNAVAILABLE로 생성

PluginSelectionBase
    -> VtubeStudio를 이름 기준 기본 provider로 선택
    -> PluginHandle.construct() 호출
    -> UNAVAILABLE 상태이므로 재검사 없이 None 반환
    -> 대체 provider 없음
    -> PluginStartupError

결과
    -> VtubeStudio.init() 미호출
    -> VTubeStudioConnection.start() 미호출
    -> 3초 재연결 스레드 미생성
    -> LAVI 전체 시작 중단
```

## 5. 회귀가 생긴 변경 경계

Git 기록으로 확인된 관련 변경은 다음과 같다.

### 5.1 `a6f9db4946d037194fa07de353e28bafac375323`

```text
Date: 2026-07-16 20:12:45 +09:00
Subject: refactor: add plugin lifecycle metadata foundation
```

VtubeStudio metadata에 다음 항목이 추가됐다.

- `required_python_packages = ['websocket']`
- `required_services = ['VTube Studio websocket ws://localhost:8001']`

### 5.2 `b065fc92d26b97e04102e784b4c6c3e904f3efd0`

```text
Date: 2026-07-16 22:48:30 +09:00
Subject: Fix P1 plugin startup regressions
```

이 변경에서 loader가 `required_services`를 실제로 probe하고, discovery 중 진단 결과가 있으면 처음부터 `PluginState.UNAVAILABLE`인 handle을 만들도록 변경됐다.

이 변경 전에는 VTube Studio 서비스가 metadata에 선언되어 있어도 연결 포트를 정적 availability 조건으로 검사하지 않았다. 따라서 provider가 생성되고 `init() -> authenticate() -> connection.start()` 경로에 들어가 기존 3초 재연결이 동작할 수 있었다.

### 5.3 후속 상태 고정 동작

다음 후속 변경에서 `UNAVAILABLE` provider는 명시적인 `refresh_availability(force=True)` 없이는 READY로 돌아오지 않는 계약이 정리됐다.

- `325fe4ebb0c90387892a9d6ebb224cb15feb311c`
- `566dd69608550569ab7c28ab17bca8f2c8ae53db`

이 때문에 discovery와 실제 provider 선택 사이에 VTube Studio가 준비되더라도 자동 복구되지 않는다.

## 6. 배포 안정화 목표와의 관계

`docs/deployment-hardening-audit.md`의 원래 목표는 다음과 같다.

- Core/offline smoke에서 네트워크, 모델 및 외부 프로세스 side effect를 발생시키지 않는다.
- missing dependency 또는 resource를 사용자 조치 가능한 `UNAVAILABLE` 상태로 표시한다.
- Production 설정에서 외부 자원이 없는 활성 module이 전체 프로그램을 종료시키지 않는다.

현재 VTube Studio 경로는 마지막 목표와 충돌한다. VTube Studio 포트가 없을 때 `UNAVAILABLE`을 표시하는 데 그치지 않고 required `vtuber` category의 유일한 provider를 사용할 수 없게 만들어 전체 앱을 종료한다.

Core/offline profile에서 `NullVtuber`를 선택해 네트워크 접근을 막는 정책과, Production profile에서 선택된 VtubeStudio가 런타임 재연결을 수행하는 정책은 서로 다른 계약이다. Core smoke의 network attempt 0 요구사항을 지키기 위해 Production VtubeStudio의 재연결을 제거해서는 안 된다.

## 7. 책임과 소유권

### 7.1 PluginLoader

소유 책임:

- plugin metadata 파싱
- API version, entrypoint 및 interface contract 검증
- Python package, 파일 및 executable 같은 정적 availability 진단
- provider descriptor와 handle 등록

소유하지 않는 책임:

- VTube Studio의 지속적인 연결 상태
- WebSocket 재시도 간격
- 인증 상태
- 실행 중 연결 복구
- VTube Studio connection thread 종료

VTube Studio 포트의 일시적 미연결을 정적 plugin 결함으로 확정해서는 안 된다.

### 7.2 PluginSelectionBase

소유 책임:

- 설정과 runtime requirement에 맞는 provider 선택
- provider construct/init/start 호출
- 정적 결함이 있는 provider 격리
- 실제 대체 provider가 있을 때 fallback 선택

선택 단계는 VTube Studio의 런타임 연결 성공을 provider 객체 생성의 전제조건으로 사용하지 않는다.

### 7.3 VtubeStudio

소유 책임:

- VTube Studio 기능의 plugin lifecycle
- 인증 관리자와 각 avatar controller 조합
- 연결 객체 시작
- LAVI shutdown을 connection shutdown으로 전달
- 연결되지 않은 동안 avatar mutation을 수행하지 않도록 보호

### 7.4 VTubeStudioConnection

소유 책임:

- WebSocket 객체
- 연결 스레드
- 현재 연결 상태
- 재시도 허용 상태
- 3초 재시도 대기
- 연결 및 연결 해제 상태 전이
- WebSocket close
- 연결 스레드의 제한된 종료

연결과 재시도에 대한 mutable state는 이 객체가 소유해야 하며 loader나 전역
static 상태에 복제하지 않는다. 구현 내부에서는 이 소유권을 유지하면서 다음
집중 구성요소에 세부 책임을 위임한다.

- `VTubeStudioConnectionAttemptState`: attempt/socket/authentication 상태 전이
- `VTubeStudioConnectionWorker`: worker 하나와 3초 interruptible retry cadence
- `VTubeStudioConnectionSender`: send 직렬화와 identity-safe 실패 무효화
- `VTubeStudioWebSocketSession`: 단일 transport connect/receive/cancel
- `VTubeStudioWebSocketLaunchCoordinator`: connect 및 callback과 shutdown 선형화
- `VTubeStudioSocketCloser`: bounded close와 미완료 socket ledger

### 7.5 인증과 runtime

- `VTubeStudioAuthManager`: attempt별 인증 protocol state machine
- `VTubeStudioAuthenticationPhase`: 요청과 응답의 허용 순서
- `VTubeStudioTokenStore`: token 읽기와 staged atomic publish
- `VTubeStudioEventLogLimiter`: 인증·연결 반복 이벤트별 session hard cap
- `VTubeStudioRuntime`: 연결·인증·종료 orchestration
- `VTubeStudioAvatarControllerGroup`: controller 시작·rollback·종료
- `VTubeStudioInterruptSubscription`: interrupt 구독과 retryable unsubscribe

## 8. 런타임 상태 모델

개념적인 상태는 최소한 다음을 구분한다.

```text
NOT_STARTED
    connection.start()가 아직 호출되지 않음

CONNECTING
    WebSocket 연결 시도 중

DISCONNECTED_WAIT
    연결 실패 또는 연결 해제 후 다음 재시도를 기다림

CONNECTED
    WebSocket이 열렸지만 인증 완료 전일 수 있음

AUTHENTICATING
    token 요청 또는 authentication 요청 처리 중

AUTHENTICATED
    VTube Studio API 인증 완료

STOPPING
    새 재시도를 차단하고 socket/thread를 정리 중

STOPPED
    socket이 닫히고 연결 스레드가 종료됨
```

구현이 반드시 동일한 enum을 만들어야 한다는 뜻은 아니다. 다만 진단과 테스트에서 이 의미들을 서로 혼동해서는 안 된다.

허용되는 주요 전이는 다음과 같다.

```text
NOT_STARTED -> CONNECTING
CONNECTING -> CONNECTED
CONNECTING -> DISCONNECTED_WAIT
CONNECTED -> AUTHENTICATING
AUTHENTICATING -> AUTHENTICATED
AUTHENTICATING -> DISCONNECTED_WAIT
AUTHENTICATED -> DISCONNECTED_WAIT
DISCONNECTED_WAIT -> CONNECTING
any active state -> STOPPING -> STOPPED
```

`STOPPING` 또는 `STOPPED`에서 `CONNECTING`으로 돌아가는 전이는 허용하지 않는다.

## 9. 재연결 계약

- 첫 연결 시도는 provider initialization 후 즉시 시작한다.
- 초기 연결 실패는 예외를 AppComposer까지 전파하지 않는다.
- 현재 기준 재시도 간격은 3초다.
- 재시도 루프는 connection instance당 하나만 존재한다.
- `start()`가 반복 호출되어도 두 번째 connection thread를 만들지 않는다.
- 연결 성공 후 루프는 `run_forever()`가 끝날 때까지 추가 연결을 만들지 않는다.
- 연결 해제 후 인증 상태를 초기화하고 3초 뒤 같은 endpoint로 재시도한다.
- 재시도 간격 또는 backoff 정책 변경은 별도의 명시적 결정으로 취급한다.
- token 오류를 해결한다는 이유로 `token.txt`를 자동 삭제하거나 덮어쓰지 않는다.
- VTube Studio 프로그램을 LAVI가 자동 설치하거나 강제로 실행하지 않는다.

## 10. 종료 계약

구현은 connection instance가 daemon worker, `threading.Event`, 현재 WebSocket,
attempt identity와 상태 전이를 소유한다. 재시도 대기는
`stop_event.wait(3.0)`을 사용하며 `VtubeStudio.shutdown()`은 runtime을 통해
connection과 avatar worker의 제한된 종료를 수행한다.

기본 transport는 websocket-client의 저수준 연결을 감싼 단일-use session이다.
session의 취소 신호는 sticky 상태이므로 `run_forever()` 진입 직전 또는 connect
진행 중 shutdown이 발생해도 이후 반환된 transport를 callback 전에 즉시 닫는다.
socket close는 별도 bounded closer가 수행하고, 완료되지 않은 모든 close 대상은
유실하지 않고 cleanup pending 상태로 보존한다.

`start()`의 worker 등록·실행과 `shutdown()`은 같은 lifecycle 경계에서
선형화한다. shutdown은 socket detach, close 등록, worker join 및 terminal 상태
발행을 하나의 직렬화된 transaction으로 처리한다. 진행 중 send/connect/close가
있으면 `STOPPING`을 유지하고, 마지막 소유 작업이 끝난 시점에 자동으로
`STOPPED`로 승격한다.

종료 동작은 다음을 만족해야 한다.

- 여러 번 호출해도 안전한 idempotent shutdown
- shutdown 시작과 동시에 새 재시도 금지
- 현재 WebSocket이 있으면 해당 connection 소유자가 close
- interruptible wait를 사용해 3초 대기를 즉시 해제
- connection thread에 제한된 join 적용
- thread 종료 성공 또는 제한시간 초과를 명확히 기록
- 종료 후 `websocket_thread_started`, `connected`, 인증 상태가 모순되지 않음
- shutdown 후 callback이 도착해도 새 controller thread나 재연결을 시작하지 않음
- 소유하지 않은 다른 plugin, input, process 또는 global runtime을 종료하지 않음

## 11. 오류 분류

| 조건 | 분류 | LAVI 시작 | VtubeStudio 처리 |
| --- | --- | --- | --- |
| `websocket` Python package 누락 | 정적 dependency 누락 | fallback 정책에 따름 | provider `UNAVAILABLE`, 설치 안내 |
| metadata/API version/interface 오류 | plugin contract 오류 | fallback 정책에 따름 | provider `FAILED` |
| 설정 JSON 읽기 실패 | plugin 설정 오류 | 기존 safe-default 또는 명시적 init 정책에 따름 | 오류 원인 기록 |
| `localhost:8001` 연결 거부 | 일시적 런타임 연결 실패 | 계속 | 3초 뒤 재시도 |
| `localhost:8001` timeout | 일시적 런타임 연결 실패 | 계속 | 3초 뒤 재시도 |
| WebSocket handshake 실패 | 런타임 연결 실패 | 계속 | 상태 초기화 후 재시도 |
| 인증 거부 또는 token 문제 | 사용자 조치 가능한 인증 실패 | 계속 | 원인 표시, credential 비공개 유지 |
| 연결 후 VTube Studio 종료 | 런타임 연결 해제 | 계속 | 3초 뒤 재시도 |
| LAVI shutdown | 정상 종료 | 종료 진행 | 재시도 중단, socket/thread 정리 |

`NullVtuber`는 Core/offline profile 또는 명시적 fallback을 위한 별도 provider다. `NullVtuber` 활성화만으로 VtubeStudio의 자동 재연결 계약을 대체하지 않는다.

## 12. 진단 및 로그 계약

최소한 다음 경계를 로그로 구분한다.

- provider initialized
- connection thread started
- connection attempt started
- connection established
- authentication started
- authentication succeeded
- authentication rejected
- connection closed
- reconnect scheduled
- shutdown started
- WebSocket closed
- connection thread stopped
- shutdown timeout

connection lifecycle 로그에 구현된 공통 필드:

```text
[VtubeStudio]
event=<state-change event>
state=<state>
attempt=<bounded attempt counter>
endpoint=ws://localhost:8001
retry_delay_sec=3
reason=<typed reason>
error_type=<exception class name>
thread_alive=<true|false>
cleanup_pending=<true|false>
```

로그 제한:

- 인증 token과 credential 내용을 출력하지 않는다.
- 알 수 없는 WebSocket `messageType` 원문을 출력하지 않는다.
- WebSocket payload 전체를 기본 INFO 로그에 출력하지 않는다.
- 연결 attempt 경계와 인증 오류는 sampling 후 session hard cap을 적용한다.
- 정상적인 연결 대기와 실제 plugin load/contract 실패를 다른 reason code로 구분한다.
- `available`이라는 단어를 provider가 발견됐다는 뜻과 READY라는 뜻으로 동시에 사용하지 않는다.
- 시작 실패 메시지에는 provider 이름뿐 아니라 실제 state와 reason code를 포함한다.

## 13. 구현 경계

향후 구현은 우선 다음 소유 경계를 사용한다.

```text
plugins/VtubeStudio/VtubeStudio.py
    -> runtime service를 시작 필수 정적 dependency로 분류하지 않음
    -> AST discovery용 metadata와 기존 공개 facade 유지

plugins/VtubeStudio/vtube_studio_core/connection/
    -> connection coordinator: 하위 연결 구성요소와 shutdown transaction 소유
    -> attempt state: socket identity와 상태 전이 소유
    -> worker: 단일 thread와 3초 retry wait 소유
    -> sender: 직렬화된 send와 실패 처리 소유
    -> websocket session: bounded connect/read와 sticky transport 취소 소유
    -> launch coordinator: connect/callback admission 선형화 소유
    -> socket closer: bounded close와 미완료 close 대상 collection 소유

plugins/VtubeStudio/vtube_studio_core/authentication/
    -> 인증 protocol phase, token 저장 및 bounded log 책임 분리

plugins/VtubeStudio/vtube_studio_core/diagnostics/
    -> 반복 lifecycle·polling 오류의 sampling 및 session hard cap

plugins/VtubeStudio/vtube_studio_core/runtime/
    -> component factory: 구성요소 조립만 소유
    -> runtime: 인증 후 controller 시작과 전체 shutdown orchestration
    -> avatar/: controller group 시작·rollback·종료 소유
    -> events/: interrupt 구독과 해제 소유

plugins/VtubeStudio/vtube_studio_core/controllers/mouth/
    -> MouthOpen 메시지 동작과 worker lifecycle 분리

plugins/VtubeStudio/vtube_studio_core/configuration/
    -> pose 기본값, 검증 및 JSON loading 분리

plugins/VtubeStudio/vtube_studio_core/ui/
    -> Gradio 인증 UI만 소유

connection / authentication / controllers.mouth package exports
    -> 책임별 구현의 canonical import 경로

이전 vtube_studio_connection.py / vtube_studio_auth_manager.py /
vtube_studio_mouth_controller.py compatibility re-export
    -> 저장소 전체 import 전환과 사용자 승인 완료 후 제거됨

plugin_system/**
    -> 변경 없음; 다른 plugin의 required service는 기존 fail-closed 유지
```

VTube Studio만 고치기 위해 모든 plugin의 service availability 의미를 느슨하게 만들지 않는다. 공통 계약 변경이 필요하다면 다음을 명시적으로 구분해야 한다.

```text
startup_required_service
runtime_reconnectable_service
diagnostic_only_service
```

현재 오류만 가리기 위해 `PluginStartupError`를 전역적으로 제거하거나 모든 required category를 fail-open으로 변경하지 않는다.

## 14. 테스트 및 완료 기준

### 14.1 정적 dependency 테스트

- `websocket` package가 없으면 VtubeStudio가 명확한 reason code로 `UNAVAILABLE`이 된다.
- metadata, entrypoint, API version 또는 interface가 잘못되면 기존 contract failure가 유지된다.
- 정적 결함을 포트 재시도 대상으로 오인하지 않는다.

### 14.2 초기 연결 실패 테스트

- `localhost:8001` 연결이 거부된 상태에서 LAVI core composition이 성공한다.
- VtubeStudio provider가 생성되고 `init()`이 완료된다.
- connection thread가 정확히 하나 시작된다.
- 첫 연결 실패가 AppComposer까지 예외로 전파되지 않는다.
- 상태가 `DISCONNECTED_WAIT` 의미로 관찰된다.
- 3초 후 다음 연결 시도가 발생한다.

### 14.3 지연 시작 복구 테스트

- LAVI를 먼저 시작한다.
- 초기 연결을 실패시킨다.
- 이후 fake VTube Studio endpoint를 준비한다.
- 다음 재시도에서 연결된다.
- 인증 경로가 같은 connection instance에서 진행된다.
- LAVI core component가 재생성되지 않는다.

### 14.4 연결 해제 복구 테스트

- 연결 및 인증 완료 후 endpoint 연결을 끊는다.
- 인증 상태가 초기화된다.
- 다른 LAVI 구성요소는 계속 실행된다.
- 3초 후 재연결한다.
- avatar controller가 인증 전 mutation을 전송하지 않는다.

### 14.5 중복 시작 테스트

- `authenticate()` 또는 `connection.start()`를 반복 호출한다.
- connection thread가 하나만 존재한다.
- 중복 호출은 상태를 손상시키지 않는다.

### 14.6 종료 테스트

- `CONNECTING`, `DISCONNECTED_WAIT`, `CONNECTED`, `AUTHENTICATED` 각각에서 shutdown한다.
- 새 연결 시도가 즉시 금지된다.
- 3초 대기가 즉시 해제된다.
- WebSocket close는 소유 connection에만 적용된다.
- 제한된 시간 안에 thread가 종료된다.
- shutdown을 두 번 호출해도 예외나 추가 close가 발생하지 않는다.
- 종료 후 reconnect 로그가 추가되지 않는다.

### 14.7 로그 테스트

- 연결 상태 변경 로그에 endpoint, state 및 reason code가 포함된다.
- token 값이 로그에 포함되지 않는다.
- 동일한 연결 거부 로그가 무제한으로 폭주하지 않는다.
- plugin discovery 당시 `UNAVAILABLE`이면 실제 missing static dependency가 진단에 노출된다.
- required service를 실제로 선언한 다른 plugin의 fail-closed 진단은 유지된다.

### 14.8 회귀 테스트

아래 사용자 시나리오가 최종 acceptance다.

```text
Given VTube Studio가 완전히 종료되어 있고 localhost:8001이 닫혀 있다.
When 사용자가 LAVI를 실행한다.
Then LAVI UI와 다른 core components는 정상적으로 시작한다.
And VtubeStudio는 연결 대기 상태다.
When 사용자가 나중에 VTube Studio를 실행하고 Plugin API를 허용한다.
Then LAVI를 재시작하지 않아도 VtubeStudio가 자동 연결 및 인증된다.
```

## 15. 비범위

이 계약만으로 다음 변경을 승인하지 않는다.

- VTube Studio 설치 또는 자동 실행
- VTube Studio Plugin API 설정 자동 변경
- Windows 방화벽 또는 시스템 설정 변경
- 기본 포트 8001 변경
- 인증 오류를 이유로 한 token 파일 삭제·이동·임의 덮어쓰기
- 모든 plugin availability 정책의 전면 재설계
- 모든 required category의 fail-open 전환
- `NullVtuber`의 Production 기본값 강제 설정
- 재시도 간격 또는 backoff 정책 변경
- VTube Studio UI 전면 개편

## 16. 문서 연결 계획

이 문서를 기준 문서로 사용하고 다른 문서에는 요약과 링크만 둔다.

- `AGENTS.md`: VTube Studio 미실행이 LAVI 시작을 막아서는 안 된다는 보존 규칙과 이 문서 링크
- `README.md`: 사용자용 자동 재연결 동작 및 문제 해결 안내
- `README_EN.md`: 동일한 영문 사용자 안내
- `docs/deployment-hardening-audit.md`: runtime-reconnectable service와 static dependency를 구분한다는 정정 기록
- `docs/object_oriented_diagram.md`: 실제 구현 후 connection stop/state ownership 반영
- `docs/object_oriented_diagram_EN.md`: 동일한 영문 구조 반영

이 문서의 내용을 임시 handoff 문서나 README 여러 곳에 복제하지 않는다. 세부 동작 계약은 이 파일을 단일 기준으로 유지한다.

## 17. 구현 전 확인 목록

- 현재 작업 폴더와 Git root 확인
- 기존 사용자 변경 보존
- VtubeStudio와 plugin loader의 현재 ownership 재확인
- 정적 dependency와 runtime service 분류 기록
- connection thread, WebSocket 및 retry state 소유자 기록
- shutdown entry point와 cleanup 순서 기록
- 기존 3초 재시도 계약을 변경하는지 확인
- 관련 focused test 파일과 integration acceptance 기록
- 실제 VTube Studio 실행 검증은 별도 명시적 요청이 있을 때만 수행

## 18. 현재 결론

VTube Studio WebSocket은 연결 시점이 변할 수 있는 런타임 서비스다. 해당 포트가 닫혀 있다는 이유만으로 VtubeStudio provider 객체 생성을 막으면 provider 내부의 재연결 기능은 실행될 수 없다.

따라서 향후 수정의 기준은 다음 한 문장이다.

> VTube Studio가 꺼져 있거나 아직 준비되지 않았더라도 LAVI는 정상적으로 시작하며, VtubeStudio가 연결 생명주기를 소유하고 종료될 때까지 3초 간격으로 안전하게 재연결한다.

## 19. 2026-09-04 구현 기록

적용 결과:

- `required_python_packages = ["websocket"]`는 유지했다.
- `required_services`에서 `localhost:8001`을 제거하고 endpoint 설정은 유지했다.
- 공용 `plugin_system/**` availability 및 `PluginStartupError` 정책은 변경하지 않았다.
- connection은 worker 하나, 단조 증가 attempt ID, 현재 socket identity,
  `threading.Event` 기반 3초 wait와 제한된 join을 소유한다.
- connection 내부의 attempt state, reconnect worker, sender, launch coordinator,
  session 및 socket closer를 각각 단일 책임 파일로 분리했다.
- 기본 WebSocket session은 connect timeout과 짧은 receive poll을 사용하고,
  websocket-client의 전역 반복 오류 로그에 의존하지 않는다.
- Windows에서 `localhost`가 `::1`을 먼저 선택해 1초 connect timeout을 모두
  소비하는 회귀를 막기 위해, localhost transport만 IPv4 loopback으로
  정규화한다. configured endpoint와 원격 host 동작은 유지한다.
- session의 sticky cancel과 bounded socket closer로 shutdown 직전 실행 경쟁,
  전송 중 graceful-close 정지 및 미완료 close 대상 유실을 차단한다.
- 이전 attempt의 open/message/error/close callback과 send 실패는 현재 연결을
  변경할 수 없다.
- worker start/shutdown, 동시 shutdown, in-flight send/connect/close 및 늦은
  close 완료를 선형화하고, cleanup 완료 시 `STOPPED`를 자동 발행한다.
- 연결, 인증 중, 인증 완료, 재시도 대기, 종료 중 및 종료 완료 상태를 구분한다.
- 인증 로그에서 token과 전체 WebSocket 응답을 제거했다.
- 인증 응답은 현재 attempt뿐 아니라 요청 phase도 일치해야 하며, 요청하지 않은
  token 응답이나 중복 인증 성공은 거부한다.
- token은 sibling 임시 파일에 stage한 뒤 shutdown과 직렬화된 현재-attempt
  확인을 통과한 경우에만 원자적으로 publish하며, crash 잔여 stage 파일은
  VTube Studio 전용 `.gitignore` 규칙으로 credential commit에서 제외한다.
- token publish와 shutdown은 같은 인증 lock에서 순서를 결정한다. shutdown이
  먼저 이기면 staged token을 폐기하고, publish가 먼저 이기면 파일 publish를
  끝낸 뒤 shutdown이 메모리의 token·인증·attempt 상태를 초기화한다.
- 기존 빈 token 파일은 삭제하거나 임의로 덮어쓰지 않고 새 token 요청으로
  복구한다. 정상 `AuthenticationTokenResponse`로 발급된 token 저장 동작은
  기존 계약대로 유지한다.
- 손상되었거나 UTF-8로 읽을 수 없는 기존 token 파일도 삭제·덮어쓰기 없이
  provider 시작 안내에서 예외를 전파하지 않는다. 실제 인증 시도는
  `token_read_failed`로 닫고 재연결 대기로 돌아가며, 기존 파일을 자동 복구하거나
  새 token으로 덮어쓰지 않는다.
- mouth polling은 별도 worker로 분리했고 stop event와 bounded join을 추가했다.
- runtime lock은 상태 전이에만 사용하며 token I/O와 WebSocket send 동안 잡지 않는다.
- avatar controller group과 interrupt subscription을 runtime orchestration에서
  분리하고, 늦은 controller start rollback 및 unsubscribe 재시도를 보존한다.
- interrupt event subscription을 보관하고 shutdown에서 해제한다. 해제가 실패하면
  `STOPPING`을 유지하고 다음 shutdown 호출에서 같은 subscription 정리를 재시도한다.
- connection/auth/mouth import는 책임별 package export로 통일했으며, 명시적으로
  승인된 이전 root-level compatibility module 3개는 제거했다.
- 기존 facade callback 인자 수와 공개 method/property 이름은 유지한다. 다만
  lifecycle setter와 반환값은 shutdown 이후 socket/worker를 되살리지 않도록
  안전한 admission 결과를 반영한다.

검증은 fake WebSocket과 제어 가능한 Event를 사용해 실제 포트 연결 및 실제
3초 sleep 없이 수행한다. 검증 범위에는 정적 dependency 분류, 즉시 첫 시도,
정확한 3초 wait 요청, 지연 연결, stale callback, 중복·동시 start, 이전 send
실패, retry wait 중 shutdown, active socket 단일 close, token 로그 비공개,
빈 token 복구, 손상 token의 비파괴적 시작·실패 처리, 요청 phase가 없는 응답 거부,
token publish와 shutdown의
양방향 선형화,
mouth worker 종료·재시작, event unsubscribe 재시도, 늦은 인증 callback 차단,
blocked send/close의 bounded shutdown, 초기 연결 실패의 기존 error→close
callback 순서, 실제 facade callback 경로, 실제 provider
선택·구성 경로, 동일 runtime의 연결 해제 후 재인증이 포함된다.

추가로 localhost→IPv4 transport 정규화와 명시적 IPv4·원격 endpoint 보존을
검증한다.

자동 검증 결과: VTube Studio 집중 및 관련 구성 회귀 테스트
`135 passed, 5 subtests passed`.

실제 VTube Studio transport handshake: `PASSED` (`127.0.0.1:8001`).

전체 LAVI 인증, 연결 해제 후 재연결 및 재인증: `PASSED`.

avatar controller 별도 live 검증: `NOT_RUN`.
