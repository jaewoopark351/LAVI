<!-- 20260817_kpopmodder: Documented and review-hardened the evidence-only investigation boundary for the hidden LAVI shutdown and Windows launch-ownership incident. -->

# Fabric ChatClef LAVI Shutdown and Launch Ownership Investigation Plan

상태: 문서 전용 evidence-only 조사 계획.

이 문서는 외부 launcher 또는 운영자가 보던 콘솔이 사라진 뒤에도 LAVI Python
runtime과 `4316`/`47860` listener가 계속 생존한 사건을 조사하기 위한 증거,
source lifecycle map, 재현 단계와 판정 기준을 정의한다.

이 문서 자체는 다음 작업을 승인하지 않는다.

- production 또는 test code 수정
- shutdown diagnostics 구현
- LAVI, Minecraft 또는 fixture 실행
- process 종료, `taskkill`, `Stop-Process`, `/T` 또는 `/F` 실행
- Minecraft command 전송
- build, dependency 변경, commit 또는 push
- production single-instance guard 구현

## 1. 문서 책임과 관련 문서

이 문서가 소유하는 책임:

- 2026-08-16~17 hidden-LAVI 사건의 shutdown/launch-ownership 증거 정리
- 현재 working tree의 LAVI shutdown source path 기록
- Windows 종료 사건을 서로 다른 lifecycle로 분리
- evidence-only 재현 계획과 결과 판정표 정의
- 향후 최소 shutdown 관측 지점 제안
- shutdown 문제와 production single-instance 문제 분리

이 문서가 소유하지 않는 책임:

- exact listener PID의 수동 복구 절차
- live mutating test 전 process/backend/instance/world preflight
- command terminal-result 계약
- Fabric Java runtime, ChatClef 또는 AltoClef lifecycle 변경

관련 문서:

- [`fabric-chatclef-live-runtime-process-lifecycle-plan.md`](./fabric-chatclef-live-runtime-process-lifecycle-plan.md)
- [`fabric-chatclef-hidden-lavi-recovery-runbook.md`](./fabric-chatclef-hidden-lavi-recovery-runbook.md)
- [`fabric-chatclef-live-runtime-preflight-plan.md`](./fabric-chatclef-live-runtime-preflight-plan.md)
- [`chatclef-korean-test-strategy.md`](./chatclef-korean-test-strategy.md)
- [`minecraft-backend-separation.md`](./minecraft-backend-separation.md)

증거 표기는 lifecycle documentation index의 다음 분류를 따른다.

- `[source]`: 현재 working-tree source에서 직접 확인
- `[config]`: 현재 로컬 설정 또는 명시된 설정 파일에서 확인
- `[runtime snapshot]`: 특정 시각 process, port 또는 log에서 직접 관찰
- `[official-platform behavior]`: 공식 Windows/Python/Gradio 자료에서 확인한 일반 동작
- `[inference]`: 직접 증거를 결합한 현재 사건 가설
- `[unknown]`: 현재 artifact로 확인할 수 없음
- `[proposed]`: 아직 실행 또는 구현되지 않은 계획

`[official-platform behavior]`는 현재 PC에서 같은 사건이 발생했다는 증거가 아니다.
현재 사건에 적용하려면 별도의 runtime snapshot 또는 재현 결과가 필요하다.

## 2. 현재 executive conclusion

### 2.1 가장 강한 현재 분류

`[inference]` 현재 사건은 다음과 같이 분류하는 것이 가장 정확하다.

> 외부 launcher 또는 운영자가 보던 콘솔은 사라졌지만, 실제 LAVI Python
> runtime에는 graceful shutdown 요청이 전달됐다는 증거가 없고 runtime은
> 강제 종료 직전까지 정상 serving을 계속했다.

primary classification:

```text
Python-level graceful shutdown 경로 미진입이 현재의 1차 조사 분류
```

이 분류 안에서 다음 두 competing sub-hypothesis를 분리한다.

```text
H1. launch/lifetime ownership
    outer launcher 또는 console host가 사라졌지만 base Python에 종료/control event가
    생성·전달되지 않았고, 두 수명이 결합되지 않음

H2. control-event delivery/handling boundary
    control event가 생성됐더라도 console/process-group/handler 경계에서 소비·무시되어
    Python-level KeyboardInterrupt와 shutdown marker까지 도달하지 않음
```

H1과 H2는 서로 배타적이라고 미리 가정하지 않지만, 같은 가설을 중복 계산하지도 않는다.
둘 다 "shutdown handler가 실행됐지만 component cleanup에서 실패했다"는 가설보다 현재
증거와 더 잘 맞는다. 강제 종료 전까지 같은 PID가 계속 serving했으므로 `00:28` 이전의
abrupt process termination은 사건 설명이 될 수 없다. 단순 marker flush 실패만으로
listener와 runtime의 지속 생존을 설명할 수도 없으므로, 로그 손실은 낮은 우선순위의
관측성 가설로만 남긴다.

### 2.2 `detached` 용어의 제한

이 문서에서 `detached runtime`은 운영자가 보던 launcher/console과 LAVI runtime의
수명이 분리된 **관찰 현상**을 뜻한다.

다음 Windows 생성 flag가 사용됐다는 뜻으로 사용하지 않는다.

```text
DETACHED_PROCESS
CREATE_NO_WINDOW
CREATE_NEW_PROCESS_GROUP
```

위 flag, console ownership, process group과 Job Object membership은 모두
`[unknown]`이며 재현에서 별도로 확인해야 한다.

### 2.3 현재 증거로 금지되는 결론

다음 결론은 아직 직접 증거가 없다.

- `RuntimeLifecycle.shutdown()`이 고장 났다.
- Gradio shutdown이 무한 대기했다.
- Fabric ChatClef WebSocket stop이 block됐다.
- background 또는 non-daemon thread가 shutdown 완료 후 process를 붙잡았다.
- Windows venv redirector가 base Python child 정리에 실패했다.
- console close와 `Ctrl+C`가 동일한 shutdown 경로를 사용한다.
- PID `10864`가 확정된 console owner 또는 lifetime owner였다.

## 3. 사건 evidence timeline

### 3.1 process와 port snapshot

`[runtime snapshot]` 2026-08-17 관리자 read-only 조사에서 다음 chain이
관찰됐다.

```text
PID 30264
  C:\Vtuber_Souorce_Code\LAVI\venv\Scripts\python.exe
  argv: "...\venv\Scripts\python.exe" -B main.py
  parent PID: 10864, snapshot 시점에는 이미 없음

  -> PID 25220
       C:\Users\jaewo\AppData\Local\Python\pythoncore-3.14-64\python.exe
       같은 LAVI argv
       LISTEN 4316, 47860
```

이 snapshot의 PID는 과거 증거이며 이후 종료 대상이나 재현 기준으로 재사용하지
않는다. 로컬 조사 artifact는 `logs/codex/` 아래에 있으며 Git 문서 artifact로
취급하거나 commit하지 않는다.

### 3.2 시간순 증거

| 시각 | 관찰 | 근거 | 판정 |
| --- | --- | --- | --- |
| `2026-08-16 19:19:35` | PID `30264`와 `25220` 생성 | Windows process snapshot | `[runtime snapshot]` |
| `19:21:26` | Fabric bridge `4316` 시작 | `logs/20260816_191935_log.txt:123` | `[runtime snapshot]` |
| `19:21:26` | Gradio `47860` 시작 | 같은 파일 `:125` | `[runtime snapshot]` |
| `2026-08-17 00:03:43` | 두 번째 LAVI의 `4316` bind 실패, WinError 10048 | `logs/20260817_000301_log.txt:103` | `[runtime snapshot]` |
| `00:03:43` | 두 번째 LAVI가 사용 중인 `47860`을 피하고 `47861` 시작 | 같은 파일 `:104-105` | `[runtime snapshot]` |
| `00:15:04` | 재시작된 Minecraft가 기존 LAVI에 연결 | 기존 LAVI log `:8857`, Minecraft `latest.log:237` | `[runtime snapshot]` |
| `00:28:04` | 기존 LAVI에서 마지막으로 관찰된 runtime 활동 (`ScreenVision` screen-grab failure log) | 기존 LAVI log `:9250` | `[runtime snapshot]` |
| 약 `00:28` | 운영자가 identity를 재검증한 PID `25220`을 `Stop-Process -Force`로 종료 | 사용자 실행 기록 | `[runtime snapshot]` |
| `00:28:07` | Minecraft bridge가 `Connection reset` 관찰 | `latest.log:2080`, `stdout-logs.txt:6034` | `[runtime snapshot]` |
| `00:28:10` | Minecraft bridge 재연결 실패 | `latest.log:2082`, `stdout-logs.txt:6040` | `[runtime snapshot]` |

기존 LAVI log `:9250`은 error log이므로 해당 시각까지 process와 logging activity가
계속됐다는 생존 증거로만 사용한다. 정상 ScreenVision 동작의 증거로 사용하지 않는다.

### 3.3 log inventory

조사 시 확인한 `LAVI_TEST_Fabric01` log는 0 byte가 아니었다.

| 파일 | 조사 시 크기 | 확인된 역할 또는 경계 |
| --- | ---: | --- |
| `logs/latest.log` | `2,364,051` bytes 이상, live writer로 계속 증가 가능 | `[source]` `[config]` Minecraft/Fabric runtime log |
| `logs/instance_audit.txt` | `1,771,116` bytes | `[runtime snapshot]` instance-local audit 성격의 파일; exact writer는 `[unknown]` |
| `logs/stdout-logs.txt` | `2,699,264` bytes 이상, live writer로 계속 증가 가능 | `[runtime snapshot]` instance-local stdout capture 성격의 파일; exact writer는 `[unknown]` |
| LAVI `20260816_191935_log.txt` | `925,421` bytes | 기존 LAVI startup, reconnect, command, 마지막 활동 |
| LAVI `20260817_000301_log.txt` | `83,666` bytes | 두 번째 LAVI port collision과 fallback UI |

Minecraft `latest.log`와 `stdout-logs.txt`는 조사 당시 live writer가 계속 쓰고
있었으므로 그 크기와 hash는 고정 artifact가 아니다. 향후 증거 고정 단계에서는
snapshot 안정성과 write 시각을 함께 기록해야 한다. `instance_audit.txt`와
`stdout-logs.txt`의 writer는 파일명이나 내용만으로 확정하지 않는다.

### 3.4 forced termination의 해석

`Stop-Process -Id 25220 -Force` 이후 확인한 다음 현상은 forced termination의
negative control이다.

- LAVI cleanup marker 없음
- LAVI log의 갑작스러운 종료
- Minecraft `Connection reset`
- Minecraft reconnect 실패

이 결과를 graceful shutdown 구현 실패의 직접 증거로 사용하지 않는다.

## 4. current working-tree source lifecycle map

### 4.1 source snapshot 경계

다음 source line은 `2026-08-17` 조사 시점의 current working tree 기준이다.

```text
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 4a87e9018a9e2743764ec5d4215eb8923a401da4
working tree: dirty
```

이 line reference는 HEAD 완료 상태나 runtime 검증을 뜻하지 않는다. 특히
Minecraft extension 및 test 파일에 기존 사용자 변경이 있으므로 future audit는
line number와 diff를 다시 확인해야 한다.

### 4.2 top-level ownership

```text
main.py:4-5
  -> AppComposer().run()

app_core/app_composer.py:92-106
  -> configure/startup/build/lifecycle/Gradio 순서
  -> startup failure는 except Exception에서 cleanup

app_core/app_composer.py:363-374
  -> RuntimeLifecycle 생성 및 start()

app_core/app_composer.py:376-384
  -> GradioRuntimeLauncher.launch(...)

app_core/gradio_runtime_launcher.py:30-47
  -> port 선택
  -> interface.queue().launch(...)
  -> KeyboardInterrupt marker
  -> finally에서 RuntimeLifecycle.shutdown()
```

`[source]` `AppComposer.run()`은 `Exception`만 catch한다. `KeyboardInterrupt`,
`SystemExit`와 그 밖의 `BaseException` 경로는 이 startup-failure `except`가
소유하지 않는다. 각 경로에서 이미 등록된 `atexit` 또는 외부 harness가 무엇을
정리하는지는 단계별 재현으로 구분해야 한다.

`[source]` `GradioRuntimeLauncher.launch()`의 port 선택과 starting log는
`try` 바깥인 `30-35`행에 있고, Gradio `launch()`만 `36-47`행의 `try/finally`
안에 있다. 따라서 Gradio `try` 진입 전 interrupt 경로는 별도 재현 대상이다.
정상 interpreter 종료에서 등록된 `atexit`가 보조할 수 있지만 abrupt 또는
external termination에서는 이를 보장하지 않는다.

### 4.3 RuntimeLifecycle shutdown

`[source]` `app_core/runtime_lifecycle.py`의 현재 경로:

```text
44-52  start(), shutdown hook 등록, component 시작, global update 시작
54-61  atexit.register(self.shutdown)
63-68  boolean reentry-suppression flag (`app_shutdown_done`) 확인 및 설정
69     [Shutdown] cleanup started.
71-77  global update timer cancel
79-95  managed component shutdown, component별 exception 기록 후 계속 진행
96     [Shutdown] cleanup finished.
206-244 global update timer 생성, daemon=True
```

따라서 cleanup 구현과 시작/완료 marker는 source에 존재한다. 현재 사건에서 marker가
없다는 사실은 `shutdown()` 내부 component hang보다 shutdown 미진입을 먼저
조사해야 한다는 근거다.

`[source]` 현재 guard는 `app_shutdown_done=True`를 timer cancel과 component cleanup보다
먼저 설정한다. 후속 invocation은 같은 flag만 보고
`[Shutdown] already in progress or completed.`를 남긴 뒤 반환한다. 따라서 이 flag는
**cleanup 진행 중**과 **cleanup 완료**를 구분하지 않으며, 후속 호출이 억제됐다는 사실만으로
첫 cleanup이 끝났다고 판정할 수 없다. `[Shutdown] cleanup finished.` marker는
`shutdown()` method body가 끝까지 도달했다는 method-level 증거다. 전체 graceful process
종료를 판정하려면 이 marker와 별도로 listener 해제 및 redirector/base PID 종료를
확인해야 한다.

`[source]` guard 주변에는 별도의 lock이나 atomic state machine이 보이지 않는다. 순차적인
재호출 억제는 확인되지만 여러 thread가 거의 동시에 `shutdown()`을 호출할 때의 단일-owner
보장은 `[unknown]`이다. 관측 보강을 위해 lock을 새로 추가하거나 state machine으로 바꾸는
작업은 diagnostics-only가 아니라 behavioral change다.

`[source]` global update cancel과 각 component shutdown 중 발생한 `KeyboardInterrupt`는
현재 메서드 내부에서 기록한 뒤 다음 cleanup 단계로 진행한다. 따라서 cleanup 중 두 번째
`Ctrl+C`가 곧바로 process 강제 종료를 뜻한다고 가정하지 않으며, 별도 fixture subscenario로
관찰해야 한다.

`[source]` RuntimeLifecycle 자체에는 managed component별 공통 timeout이 없다.
cleanup 진입 이후 특정 component가 반환하지 않는 별도 사건은 가능하지만, 이번
사건의 1차 증거는 아니다.

### 4.4 extension과 Fabric bridge shutdown

```text
app_core/extensions/extension_registry.py:78-90
  -> extension을 역순으로 stop()

plugins/Minecraft/fabric/chatclef/extension/
  minecraft_fabric_chatclef_extension.py:41-53
  -> start()는 adapter.start()
  -> stop()은 adapter.stop() 후 stopped event 기록

plugins/Minecraft/fabric/chatclef/adapter/
  fabric_chatclef_adapter.py:41-48
  -> adapter.stop()은 WebSocket server.stop()

plugins/Minecraft/fabric/chatclef/transport/
  fabric_chatclef_websocket_server.py:80-108
  -> event-loop thread daemon=True
  -> async close future에 bounded wait
  -> thread join에 bounded wait

  fabric_chatclef_websocket_server.py:282-301
  -> event loop run_forever(), finally close_async(), loop.close()

  fabric_chatclef_websocket_server.py:316-325
  -> server.close(), wait_closed(), session/ownership clear
```

`[source]` Fabric WebSocket thread와 RuntimeLifecycle periodic timer는 daemon
thread다. 이 두 thread만으로 interpreter가 shutdown 완료 후 계속 살아 있었다고
판정하지 않는다.

### 4.5 Gradio ownership의 확인 범위

`[source]` 현재 call은 `interface.queue().launch(...)`에 `server_name`,
`server_port`, `share`, `inbrowser`만 전달하며 `prevent_thread_lock`을 명시하지
않는다.

`[source]` 현재 repository의 `main.py`, `app_core/**`, `ui_core/**`와
`plugins/Minecraft/**` 검색에서는 다음 app-wide action을 찾지 못했다.

- LAVI 전체 종료 UI button
- app-wide shutdown endpoint
- Gradio interface/server handle을 보존한 명시적 `close()` 호출

이 검색은 지정 scope의 current working tree에 대한 source evidence일 뿐, 설치된
Gradio 내부 lifecycle의 증거는 아니다.

`[unknown]` 설치된 Gradio 버전에서 해당 `launch()` call의 exact blocking,
thread와 signal 동작은 installed source 또는 official documentation 및 격리된
fixture로 확인해야 한다.

### 4.6 source-only worker inventory gap

현재 source map에서 daemon 여부와 bounded stop이 직접 확인된 것은 Fabric WebSocket
thread와 RuntimeLifecycle periodic timer다. 다음 영역은 아직 repository-wide
worker inventory가 끝나지 않았으므로 process 생존 원인으로 확정하거나 배제하지
않는다.

| 영역 | 현재 확인 범위 | source-only audit에서 남은 질문 |
| --- | --- | --- |
| Gradio | launch argument와 LAVI 측 `close()` 부재 | installed version의 server handle, non-daemon thread, signal/close ownership |
| ScreenVision | 사건 로그에서 반복 활동 확인 | worker 생성 위치, daemon 여부, stop/join/timeout |
| audio/STT/TTS | 미조사 | recording/playback worker, executor, native thread와 shutdown 경계 |
| plugin workers | Fabric 일부만 확인 | 각 plugin의 background thread/timer/executor와 shutdown 순서 |
| asyncio | Fabric event loop 일부만 확인 | process-wide loop, pending task와 loop owner |
| subprocess monitor | 일부 child process manager 문서만 존재 | monitor thread, pipe reader와 child wait ownership |

후속 source-only audit는 각 worker에 대해 `creation file:line`, owner, daemon 여부,
stop/shutdown, join/wait, timeout, process 생존 가능성과 사건 직접 증거 여부를 표로
남겨야 한다. 이 inventory가 끝나기 전에는 "background thread가 원인이다" 또는
"background thread는 무관하다"고 단정하지 않는다.

## 5. 확인된 사실과 미확정 ownership

### 5.1 확인된 사실

- `[runtime snapshot]` outer parent로 기록된 PID `10864`는 조사 시 이미 없었다.
- `[runtime snapshot]` venv Python PID `30264`와 listener PID `25220`은 함께
  생존했다.
- `[runtime snapshot]` PID `25220`은 강제 종료 직전까지 `4316`과 `47860`을
  소유했다.
- `[runtime snapshot]` cleanup 진입 marker가 LAVI log에 없었다.
- `[source]` cleanup method, marker, atexit hook과 Gradio finally는 존재한다.
- `[source]` 브라우저 탭을 닫는 동작을 process shutdown으로 연결하는 app-wide
  action은 조사한 source scope에서 발견되지 않았다.

### 5.2 아직 확인할 수 없는 값

- PID `10864`의 executable, argv와 정확한 launch 역할
- 세 PID의 console attachment와 console owner
- process group 관계와 `CREATE_NEW_PROCESS_GROUP` 사용 여부
- Job Object membership과 lifetime owner
- `CREATE_NO_WINDOW` 또는 `DETACHED_PROCESS` 사용 여부
- stdin/stdout/stderr handle과 terminal host 관계
- 종료 사건이 발생했는지와 어느 PID가 받았는지
- `CTRL_C_EVENT`, `CTRL_BREAK_EVENT`, `CTRL_CLOSE_EVENT` 처리 경로
- installed CPython venv redirector의 exact build/source 동작
- installed Gradio의 non-daemon thread와 server close ownership
- plugin, audio, executor, subprocess monitor의 non-daemon thread inventory
- shutdown 시 pending asyncio task
- process exit code

부모 PID가 존재하거나 사라졌다는 사실만으로 console owner, Job owner 또는
lifetime owner를 판정하지 않는다.

## 6. Windows lifecycle 시나리오 분리

| ID | 종료 사건 | 현재 분류 | 핵심 질문 |
| --- | --- | --- | --- |
| W1 | 같은 visible foreground console의 `Ctrl+C` | graceful 후보 | base Python이 SIGINT/KeyboardInterrupt를 받고 cleanup marker를 남기는가 |
| W2 | classic Console Host X, Windows Terminal/ConPTY close, VS Code terminal close | host별 close 사건 묶음 | 각 host가 실제로 어떤 control event·pipe close·process termination을 만들고 Python cleanup으로 이어지는가 |
| W3 | 브라우저/Gradio 탭 닫기 | client lifecycle | server 또는 process shutdown 요청이 실제 발생하는가 |
| W4 | outer launcher만 종료 | 이번 사건의 primary 재현 후보 | Python chain에 control event 또는 lifetime 종료가 전달되는가 |
| W5 | venv redirector 종료 | launcher/job fixture | base Python child와 Job ownership은 어떻게 동작하는가 |
| W6-a | explicit force flag가 없는 base Python exact-PID external termination | semantics 조사 | 사용한 tool/API가 실제로 어떤 종료 경로와 marker 결과를 만드는가 |
| W6-b | base Python forced termination | forced negative control | finally/atexit/cleanup marker 부재를 graceful 결함으로 오인하지 않는가 |

W1~W6을 서로 같은 종료 방식으로 합치지 않는다. W6-a와 W6-b도 별도 결과로 기록하며
forced negative control은 W6-b에만 적용한다. W2는 표의 한 행으로 묶었을 뿐,
재현에서는 classic Console Host, Windows Terminal/ConPTY와 VS Code terminal을 별도
subscenario로 기록한다. runtime 또는 공식 platform 근거 없이 Windows Terminal/VS Code
host close를 `CTRL_CLOSE_EVENT`라고 표기하지 않는다.

## 7. evidence-only 재현 계획

이 절의 모든 항목은 `[proposed]`이며 이번 문서 작업으로 실행이 승인되지 않는다.

### Phase A. 격리된 최소 Python fixture

실제 LAVI, Minecraft, GPT-SoVITS와 production port를 사용하지 않는다. repository
production source를 수정하지 않고 승인된 isolated test 위치에서 disposable fixture와
그 fixture를 소유하는 external harness의 조합을 계획한다.

fixture 내부 기록:

```text
PID, PPID
sys.executable, sys._base_executable
sys.prefix, sys.base_prefix
argv, cwd
Python에서 직접 확인 가능한 console attachment 상태
Python-level signal/exception
raw Win32 control event는 별도 승인된 fixture handler가 실제로 관찰한 경우에만 기록
atexit와 finally 진입
thread name, daemon, native_id
```

external harness 기록:

```text
process tree와 CreationDate
terminal/console host 종류와 external harness가 실행한 exact action
console attachment와 console client process set
console host identity는 확인 가능한 경우에만 기록
관찰한 control event와 host action에서 추론한 event를 구분
launch-time process-group flag/relationship은 확인 가능한 범위만 기록
Job Object membership은 query 가능한 범위만 기록
stdin/stdout/stderr handle 관계
listener ownership과 port 해제 시각
redirector/base Python 종료 시각
final process exit code
forced teardown 수행 여부
```

종료된 process의 final exit code를 fixture 자신의 마지막 log에 의존하지 않는다.
console host, process group 또는 Job membership을 조회하지 못한 경우 `none`으로 해석하지
않고 `[unknown]`으로 남긴다.

### Phase B. 최소 Gradio fixture

임시 loopback port에서 다음을 구분한다.

- visible CMD/PowerShell/Windows Terminal/VS Code terminal의 `Ctrl+C`
- 브라우저 탭만 닫기
- classic Console Host 창의 X 닫기
- Windows Terminal tab close와 terminal application/window close
- VS Code integrated terminal kill/close
- outer launcher만 종료
- venv redirector 종료
- explicit force flag가 없는 base Python exact-PID external termination
- base Python forced termination
- Gradio launch return/exception
- cleanup marker와 listener 해제
- redirector/base Python 종료 여부

production port `4316`, `47860`, `8790`, `9880`은 사용하지 않는다.

### Phase C. 최소 LAVI runtime

Phase A/B가 통과하고 사용자가 별도 승인한 뒤에만 실행을 검토한다.

조건:

- visible foreground console
- Minecraft 미연결
- live/mutating command 없음
- 외부 GPT-SoVITS, Chess, StarCraft 등은 가능한 test-only 방식으로 비활성
- production port와 충돌하지 않는 test port
- 독립 log

검증:

- `Ctrl+C`
- Gradio KeyboardInterrupt marker
- RuntimeLifecycle cleanup start/finish marker
- test listener 해제
- venv/base Python 종료
- orphan child 없음

설정 또는 source 변경이 필요하면 구현하지 않고 exact test-only override 제안과
별도 승인을 먼저 받는다.

### Phase D. command-free Minecraft integration

Phase C 통과 후 별도 승인하에 실행을 검토한다.

여기서 `command-free`는 Minecraft command와 world mutation을 수행하지 않는다는 뜻이다.
LAVI `Ctrl+C`, 선택적인 LAVI 재시작과 Minecraft 재연결이 포함될 수 있으므로 process
lifecycle 관점의 read-only 단계로 표현하지 않는다.

- Fabric Minecraft 연결만 수행
- Minecraft command 전송 없음
- `active_request_id == null`
- visible console에서 `Ctrl+C`
- LAVI cleanup marker 확인
- Minecraft disconnect 기록 확인
- LAVI listener 해제
- idle disconnect 자체가 unsolicited `command_request`를 만들지 않는지 확인
- LAVI 재시작과 Minecraft 재연결까지 별도 승인된 subscenario에서는 idle reconnect도
  unsolicited `command_request`를 만들지 않는지 확인

이 단계는 command를 보내지 않으므로 "이전에 실행한 command가 replay되지 않는다"는
mutating replay 계약까지 검증하지 않는다. 재시작·재연결 subscenario도 active command
replay 검증이 아니라 idle 연결 lifecycle 검증으로 한정한다. 실제 replay 계약은 별도
승인된 command-lifecycle fixture 또는 기존 protocol/test evidence로만 다룬다.
Java/Fabric runtime과 protocol은 변경하지 않는다.

## 8. 필수 시나리오 matrix

다음 표의 `통과`는 특정 shutdown 정책이 옳다는 뜻이 아니라, 해당 사건을 재현하고
marker/process/listener 증거로 분류할 수 있다는 뜻이다. 모든 exact-PID 종료는
실제 LAVI가 아닌 disposable fixture에만 적용하며 별도 사용자 승인이 필요하다.

| ID | 목적·실행 환경 | 종료 사건 | 기대 marker | 기대 process/listener | 통과 기준과 실패 분류 | 범위·위험·승인 |
| --- | --- | --- | --- | --- | --- | --- |
| S1 | classic Console Host의 visible CMD에서 venv Python fixture | `Ctrl+C` | Python-level signal/`KeyboardInterrupt`, `finally`; raw Win32 event는 승인된 fixture handler가 있을 때만 기록하고 `atexit`는 보조 관측 | redirector/base PID 종료, 임시 listener 해제 | primary marker와 종료가 모두 관찰되면 통과; `atexit` 부재만으로 실패시키지 않고 미전달·cleanup hang·PID leak을 분리 | fixture only, 낮음, 실행 승인 필요 |
| S2 | classic Console Host의 visible PowerShell에서 같은 fixture | `Ctrl+C` | S1과 동일 | S1과 동일 | shell 차이를 S1과 비교 | fixture only, 낮음, 실행 승인 필요 |
| S3 | Windows Terminal/ConPTY에서 같은 fixture | `Ctrl+C` | terminal host와 Python이 관찰한 event | redirector/base PID와 listener 결과 | ConPTY 경로를 S1/S2와 분리 | fixture only, 낮음, 실행 승인 필요 |
| S4 | VS Code integrated terminal에서 같은 fixture | `Ctrl+C` | VS Code terminal host와 Python marker | PID/listener 종료 여부 | host-specific event 전달 여부 분류 | fixture only, 낮음, 실행 승인 필요 |
| S5 | 최소 Gradio fixture | 브라우저/탭만 닫음 | process shutdown marker는 원칙적으로 기대하지 않고 실제 결과 기록 | server PID와 listener의 지속 여부 | client lifecycle인지 process lifecycle인지 분류 | fixture only, 낮음, 실행 승인 필요 |
| S6 | host별 독립 fixture; 아래 S6-a~S6-d로 분리 | console/terminal host close | 관찰 가능한 host/control marker와 마지막 flush | PID/listener가 종료·잔류하는 실제 결과 | graceful/abrupt/잔류 중 하나로 분류; host 간 결과를 일반화하지 않음 | fixture only, 중간, 각 close 승인 필요 |
| S7 | helper outer launcher가 venv fixture를 시작 | outer launcher exact PID만 종료 | child에 전달된 event 유무 | redirector/base PID와 listener의 잔류·종료 | lifetime coupling과 stdio/console 관계를 분류 | fixture only, 중간, exact-PID 승인 필요 |
| S8 | venv redirector와 base child를 식별한 fixture | redirector exact PID 종료 | base child가 받은 event 유무 | Job/child PID와 listener 결과 | redirector/job ownership을 분류 | fixture only, 중간, exact-PID 승인 필요 |
| S9 | base Python fixture | explicit force flag 없는 exact-PID external termination; 사용한 tool/API의 실제 동작을 함께 기록 | cleanup marker는 보장하지 않고 실제 결과 기록 | base PID 종료, redirector/port 결과 | graceful과 external termination을 구분하되 “soft termination”으로 가정하지 않음 | fixture only, 중간, exact-PID 승인 필요 |
| S10 | base Python fixture | forced termination | cleanup marker 부재를 negative control로 기록 | 즉시 PID 종료와 listener 해제/reset | forced 기준선을 확보; graceful 결함으로 해석 금지 | fixture only, 높음, destructive 승인 필요 |
| S11 | AppComposer 이전 단계 test double | `RuntimeLifecycle` 생성 전 `KeyboardInterrupt` | lifecycle marker 부재 가능, outer `finally`/harness marker | 이미 만든 fixture resource의 잔류 여부 | startup ownership gap과 정상 무자원 종료를 분류 | test double, 낮음~중간, 설계·실행 승인 필요 |
| S12 | partial component start test double | `RuntimeLifecycle.start()` 도중 interrupt | hook 등록, started component와 cleanup marker 순서 | partial resource/listener 잔류 여부 | rollback ownership 또는 leak을 분류 | test double, 중간, 설계·실행 승인 필요 |
| S13 | Gradio port 선택/starting-log 경계 test double | launcher `try` 진입 직전 interrupt | Gradio KeyboardInterrupt marker 유무, `atexit`/lifecycle marker | PID/listener 결과 | `try` 외부 경계와 atexit 보조 여부 분류 | test double, 중간, 설계·실행 승인 필요 |
| S14 | 최소 Gradio fixture, 이후 승인된 최소 LAVI | serving 중 `Ctrl+C` | Gradio KeyboardInterrupt, cleanup start/end, main return; `atexit`는 보조 관측 | 모든 test listener 해제, redirector/base PID 종료 | primary marker와 port/PID 종료를 steady-state graceful 기준선으로 사용하며 `atexit` 부재만으로 실패시키지 않음 | fixture 우선; 최소 LAVI는 별도 승인 |
| S15-a | 반환하지 않는 fake component | cleanup 진입 후 component hang | cleanup start와 component begin, component end와 cleanup finish 부재 | 사전 고정한 harness timeout 동안 PID 생존 | 실제 non-returning hang classifier 검증 | test double only, 중간, 강제 fixture 정리 승인 필요 |
| S15-b | `KeyboardInterrupt`를 발생시키는 fake component | 같은 `shutdown()` invocation 안의 component interrupt | component-interrupted marker와 다음 component begin/end | cleanup이 다음 component로 진행하고 finish에 도달하는지 관찰 | component interrupt 소비와 process 종료를 구분 | test double only, 중간, 설계·실행 승인 필요 |
| S15-c | cleanup을 제어된 지점에서 대기시키고 독립된 call site/thread가 `shutdown()`을 다시 호출하는 fake setup | secondary shutdown invocation/reentry | 두 invocation의 ID/thread/guard evidence | 첫 cleanup의 진행·완료 상태와 후속 호출의 suppression 또는 다중 진입 | 두 번째 `Ctrl+C`와 두 번째 `shutdown()` 호출을 동일시하지 않고 reentry/concurrency를 분류 | test double only, 중간, 설계·실행 승인 필요 |
| S16 | 해제 가능한 fake non-daemon thread | cleanup 완료 후 thread 생존 | cleanup finish 뒤 thread inventory | listener는 해제되지만 PID는 thread 해제 전 생존 | process-leak classifier가 non-daemon 원인을 식별 | test double only, 중간, 실행 승인 필요 |

각 실행은 exact command, PID/PPID, console/Job/process-group 관찰 방법, 시작·종료
시각, log 경로와 fixture teardown을 사전에 고정한다. 한 시나리오의 성공을 다른
terminal host, console-close 사건 또는 production LAVI의 성공으로 일반화하지 않는다.

각 fixture 설계는 readiness deadline, shutdown settle deadline, post-marker observation
window와 강제 teardown deadline을 실행 전에 고정한다. `PID가 남음` 또는 `listener가
남음` 판정은 settle deadline 뒤에도 같은 identity가 유지될 때만 허용한다. listener는
endpoint와 owner PID를 다시 조회하고, process는 PID뿐 아니라 CreationDate, executable과
argv를 함께 비교하여 PID 재사용이나 다른 process의 listener를 같은 runtime으로 오인하지
않는다.

S6은 최소한 다음 host-specific subscenario를 별도로 기록한다.

```text
S6-a  classic Console Host 창의 X 닫기
S6-b  Windows Terminal tab 닫기
S6-c  Windows Terminal window/application 종료
S6-d  VS Code integrated terminal kill/close
```

각 subscenario는 같은 Windows close semantics를 사용한다고 미리 가정하지 않는다.
특히 terminal tab close와 terminal application 종료도 동일 사건으로 합치지 않는다.
CMD나 PowerShell이라는 shell 이름만으로 classic Console Host라고 판정하지 않는다.
Windows 기본 terminal 설정으로 Windows Terminal에 redirect됐다면 actual host evidence에
따라 S3/S6-b/S6-c로 재분류한다. W6도 explicit force flag가 없는 external termination과
forced termination을 각각 S9/S10 결과로 분리해 기록한다.

## 9. 향후 최소 shutdown 관측 보강안

이 절은 `[proposed]`이며 아직 구현을 승인하지 않는다.

### 9.1 in-process identity

```text
[Lifecycle] process started
pid, ppid
sys.executable, sys._base_executable
argv, cwd
console handle/stdio handle의 관찰 가능한 상태
```

process group, Job Object membership과 실제 console owner는 Python 내부 추정값만으로
확정하지 않고 9.6의 외부 harness 관찰과 함께 기록한다.

### 9.2 shutdown invocation evidence

```text
[Lifecycle] shutdown invoked
invocation_id=<monotonic or correlation id>
invoker=gradio_launcher_finally | atexit_hook | startup_failure_cleanup |
        explicit_ui | windows_console_handler | unknown
trigger=keyboard_interrupt | gradio_launch_return | gradio_launch_exception |
        interpreter_exit | startup_exception | explicit_ui |
        win32_console_close | unknown
thread_name=<name>
thread_native_id=<id>
guard_flag_before=false | true | unknown
outcome=entered_cleanup | suppressed_by_existing_guard | unknown
first_entered_invocation_id=<id or unknown>
```

`invoker`는 **어느 call site가 `shutdown()`을 호출했는지**, `trigger`는 **그 call site에
도달하게 한 사건이 무엇인지**를 구분한다. 예를 들어 serving 중 `Ctrl+C`에서는
`invoker=gradio_launcher_finally`, `trigger=keyboard_interrupt`가 될 수 있다. interpreter
종료 시 보조 호출은 `invoker=atexit_hook`, `trigger=interpreter_exit`로 기록한다. 한 개의
`source` 필드에 caller와 trigger를 섞지 않는다.

현재 source는 `atexit.register(self.shutdown)`으로 bound method를 직접 등록한다. 따라서
`atexit_hook` invoker를 식별하려면 별도 wrapper 또는 call-site instrumentation이 필요하다.
그 변경은 현재 승인되지 않았으며, 향후 구현 시 callback 순서·예외·return semantics를
바꾸지 않는지 별도 검수해야 한다.

현재 `app_shutdown_done`은 boolean reentry-suppression flag다. 순차 재호출은 억제하지만
**in-progress와 completed를 구분하지 않고**, concurrent caller에 대한 thread-safe 단일-owner
보장도 source에서 확인되지 않는다. 따라서 `suppressed_by_existing_guard`는 다음 둘을 모두
포함할 수 있다.

```text
- 첫 cleanup이 아직 진행 중인 상태의 재호출
- cleanup finished 이후의 정상적인 후속 재호출
```

둘의 구분은 `[Shutdown] cleanup finished.` marker와 외부 PID/listener 증거로 한다.
`first_entered_invocation_id`도 동시 호출이 없다는 증거가 있을 때만 확정한다. diagnostics를
위해 lock이나 새로운 shutdown state machine을 추가하지 않는다.

`atexit`은 사용자 또는 OS가 보낸 shutdown request가 아니라 interpreter 종료 시의
fallback invocation이다. graceful 종료의 primary 성공 조건은 cleanup marker와
listener/PID 종료이며, `atexit` marker는 보조 관측으로만 사용한다. 확인되지 않은
invoker/trigger를 추측해서 기록하지 않는다. `explicit_ui`와 `windows_console_handler`는
현재 구현됐다는 뜻이 아니라, 해당 경로가 실제로 추가·관찰된 경우에만 사용할 reserved
value다.

`trigger=win32_console_close`는 직접 등록·관찰한 Win32 console-close event에만 사용한다.
Windows Terminal 또는 VS Code host 종료가 같은 event로 매핑됐다는 근거가 없으면 external
harness의 host-close 관찰로만 기록하고 이 trigger 값을 재사용하지 않는다. 이 값을 얻기
위해 production LAVI에 새 Win32 console-control handler를 등록하는 작업은 event semantics와
종료 시간을 바꿀 수 있으므로 diagnostics-only 변경으로 취급하지 않는다. 그런 handler는
먼저 별도 승인된 isolated fixture에서만 검증하고, production 적용은 별도 behavioral approval을
요구한다.

### 9.3 Gradio boundary

```text
[Gradio] launch entered
[Gradio] launch returned
[Gradio] launch raised: <exception type>
[Gradio] server close started
[Gradio] server close finished
```

현재 LAVI source에서는 Gradio server handle을 보존한 명시적 `close()` 경로가 확인되지
않았다. 따라서 `server close started/finished`는 기존 또는 별도 승인된 close 경계가
실제로 존재할 때만 기록하는 reserved marker다. diagnostics-only 작업에서 marker를
남기기 위해 새 close 호출이나 shutdown 동작을 추가하지 않는다.

### 9.4 component cleanup

```text
[Shutdown] component begin: <type>
[Shutdown] component end: <type> elapsed_ms=<value>
[Shutdown] component failed: <type> <exception type>
```

component 로그는 실제 호출 순서를 보존해야 하며 exception을 새로 suppress하거나
timeout, cleanup order 또는 return value를 변경하면 diagnostics-only가 아니다.

### 9.5 in-process thread와 async boundary

```text
thread name, daemon, native_id, alive
  - 최소한 모든 non-daemon thread 포함
asyncio loop 상태와 pending task 개수
[Shutdown] cleanup finished.
[Lifecycle] main returning
[Lifecycle] atexit entered
```

기존 `[Shutdown] cleanup finished.` marker와 의미가 겹치는 새
`[Lifecycle] cleanup finished` marker를 추가하지 않는다.

### 9.6 external harness observation

다음 값은 종료되는 process가 자기 log에 남기는 값이 아니라, 소유 launcher 또는
fixture harness가 외부에서 기록해야 한다.

```text
process tree와 CreationDate
terminal/console host 종류와 external harness가 실행한 exact action
console attachment와 console client process set
console host identity는 확인 가능한 경우에만 기록
관찰한 control event와 host action에서 추론한 event를 구분
launch-time process-group flag/relationship은 확인 가능한 범위만 기록
Job Object membership은 query 가능한 범위만 기록
stdin/stdout/stderr handle 관계
listener owner와 port 해제 시각
redirector/base Python 종료 시각
process exit code
강제 teardown 수행 여부
```

console host, process group, Job membership 또는 control event를 직접 확인하지 못한 경우
`none`이나 특정 event로 추정하지 않고 `[unknown]`으로 남긴다. host action과 실제 관찰된
control event는 별도 필드로 기록한다.

관측 보강은 boundary/state-change 중심이어야 하며 thread 목록이나 pending task를
주기적으로 polling하여 unbounded log를 만들지 않는다.

## 10. 재현 결과 판정표

| 관측 결과 | 허용되는 판정 |
| --- | --- |
| shutdown marker 없음 + 사전 고정한 observation deadline 뒤에도 같은 PID/port identity가 계속 생존 | shutdown request 미생성·미전달, Python handler 미도달 또는 launch/console ownership 불일치 가능성 |
| cleanup started 후 특정 component begin에서 사전 고정한 harness deadline을 초과 | 해당 component shutdown hang 가능성 |
| cleanup finished 후 settle deadline 뒤에도 같은 owner의 Gradio port가 계속 LISTEN | Gradio lifecycle ownership 또는 close 누락 가능성; listener owner identity 재검증 필요 |
| cleanup finished 후 port는 해제됐지만 settle deadline 뒤에도 같은 process identity가 생존 | non-daemon thread, 다른 blocking wait 또는 아직 진행 중인 종료 callback 가능성 |
| cleanup marker 없이 PID 즉시 종료 | external/forced termination 또는 marker 전 abrupt exit |
| `cleanup finished` 이후 `atexit_hook` invocation이 `suppressed_by_existing_guard`로 기록됨 | 정상적인 순차 재호출 가능성; 중복 cleanup 실패로 판정하지 않음 |
| `cleanup finished` 없이 후속 invocation이 `suppressed_by_existing_guard`로 기록됨 | 최초 cleanup이 진행 중이거나 부분 종료된 상태일 수 있음; cleanup 성공 증거가 아니며 완료 상태는 `[unknown]` |
| 둘 이상의 invocation이 `entered_cleanup`으로 보이거나 순서가 모순됨 | concurrent guard race 또는 instrumentation 오류 가능성; 현재 boolean guard의 thread-safe 단일-owner 보장은 미검증 |
| redirector 종료 후 settle deadline 뒤에도 같은 base child identity가 생존 | venv launcher/Job ownership 조사 필요 |
| outer launcher만 종료되고 observation deadline 뒤에도 같은 Python chain identity가 생존 | launch ownership, console attachment 또는 process-group 문제 가능성 |
| Ctrl+C에서 primary marker와 port/PID 종료가 사전 고정한 deadline 안에 모두 확인 | graceful shutdown steady-state 기본 경로 통과 |

한 번의 성공이 모든 terminal host와 console-close 경로를 증명하지 않는다.

## 11. production single-instance guard 분리

다음 두 defect를 합치지 않는다.

### Defect A: shutdown/launch ownership

- 종료 사건이 실제 base Python runtime에 전달되는가
- `RuntimeLifecycle.shutdown()`이 호출되는가
- cleanup이 완료되는가
- listener와 process가 종료되는가
- outer launcher 또는 console이 사라질 때 runtime 수명은 누가 소유하는가

### Defect B: production single-instance

- 기존 LAVI가 살아 있을 때 두 번째 LAVI를 어디서 차단하는가
- `4316` bind 실패 후 전체 startup을 중단해야 하는가
- Gradio fallback port의 partial startup을 허용해야 하는가
- 기존 instance를 사용자에게 어떻게 표시하는가

이번 조사에서는 다음을 제안하거나 적용하지 않는다.

- `main.py` 또는 `AppComposer` process-wide mutex
- `4316` bind 실패의 전체 LAVI fatal 처리
- Gradio 자동 port 증가 제거
- `run.bat` 자동 process kill
- 기존 PID 자동 종료

single-instance guard는 shutdown 원인 조사 후 별도 문서와 별도 승인을 요구한다.

## 12. 외부 자료 사용 기준

Windows, CPython 또는 Gradio 동작 확인이 필요하면 다음 authoritative source만
사용한다.

- Microsoft Windows Console, Process와 Job Object documentation
- Python 3.14 official documentation
- 실제 설치 build와 일치하는 CPython source
- 실제 설치 버전과 일치하는 Gradio source 또는 official documentation

외부 문서의 일반 동작을 현재 사건의 runtime evidence처럼 표현하지 않는다.

```text
[source] repository source
[config] 현재 로컬 설정 또는 명시된 설정
[runtime snapshot] 이번 PC에서 관찰한 값
[official-platform behavior] 공식 자료의 일반 동작
[inference] 현재 사건에 대한 추론
[unknown] 필요한 사건 artifact 없음
[proposed] 아직 실행·구현되지 않은 조사 또는 정책
```

## 13. 조사 완료 기준

이 절은 **조사 전체의 완료 기준**이며 Markdown 문서 반영 승인 기준이 아니다.
문서 자체는 fixture 실행, process 종료, diagnostics 구현 또는 production 변경 없이
반영할 수 있다. 아래 항목의 실제 수행은 각각 14절의 별도 사용자 승인을 요구한다.

- outer launcher identity를 확인하거나 `[unknown]`으로 명시했다.
- console, process group, Job Object와 stdio ownership을 확인 가능한 범위에서 분리해
  기록하고, 조회할 수 없는 값은 `none`이 아니라 `[unknown]`으로 유지했다.
- source-only worker inventory를 완료하거나 미조사 영역을 `[unknown]`으로 유지했다.
- 별도 승인된 fixture에서 W1~W6의 서로 다른 semantics를 재현·분류했고, W2는
  S6-a~S6-d의 host-specific close 결과를 별도로 기록했다.
- S11~S16 failure classifier를 production component가 아닌 test double로 검증했다.
- forced termination을 graceful failure 증거에서 제외했다.
- shutdown invoker와 trigger를 분리하고 invocation/thread identity를 함께 기록할 수 있다.
- boolean guard가 in-progress와 completed를 구분하지 않는 현재 한계를 숨기지 않으며,
  suppressed reentry를 cleanup 완료 증거로 오인하지 않는다.
- concurrent invocation의 단일-owner 여부를 증명하지 못하면 `[unknown]`으로 유지한다.
- shutdown trigger와 cleanup begin/end가 관찰 가능하다.
- component별 begin/end와 elapsed time을 관찰할 수 있다.
- shutdown 전후 thread/async inventory를 bounded하게 비교했다.
- 외부 harness가 listener, venv/base PID와 exit code를 종료 후 확인했다.
- root cause를 증거로 판정하거나 명확히 `[unknown]`으로 유지했다.
- shutdown과 production single-instance를 별도 defect로 유지했다.
- Java/Fabric runtime과 Minecraft protocol을 변경하지 않았다.

실제 LAVI 또는 Minecraft integration 재현은 이 조사 문서의 작성 완료 조건이 아니다.
Phase C/D는 앞선 fixture 증거와 별도 사용자 승인이 있을 때만 추가한다.

## 14. 다음 단계 승인 경계

이 문서 완료 후에도 자동으로 승인되는 작업은 없다.

1. source-only lifecycle audit
2. isolated fixture 파일 생성
3. fixture 실행과 exact-PID 종료 시나리오
4. 최소 LAVI runtime 실행
5. command-free Minecraft integration
6. shutdown 관측 log 구현
7. behavioral fix
8. production single-instance 설계/구현
9. commit/push

각 단계는 앞 단계 증거와 exact scope를 제시하고 별도 사용자 승인을 받아야 한다.
