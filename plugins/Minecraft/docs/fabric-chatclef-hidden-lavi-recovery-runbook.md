<!-- 20260816_kpopmodder: Added an evidence-bounded Windows runbook for identifying and manually recovering hidden LAVI processes without broad process termination. -->
<!-- 20260817_kpopmodder: Added a bounded, user-approved elevated retry procedure for antivirus-blocked read-only PowerShell probes. -->

# Fabric ChatClef Hidden LAVI Recovery Runbook

상태: 문서 전용 수동 운영 runbook.

이 문서는 Windows에서 화면에 보이지 않는 LAVI 프로세스가 Fabric ChatClef와
Gradio 포트를 계속 소유할 때, 운영자가 현재 identity를 다시 확인하고 정확한
프로세스만 수동으로 정리하는 절차를 정의한다.

이 문서 자체는 프로세스 종료, LAVI/Minecraft 실행, `@stop`, live mutating
command, 코드 수정, 빌드, commit 또는 push를 승인하지 않는다.

## 1. 적용 범위와 금지 사항

적용 범위:

- Fabric ChatClef backend
- Windows LAVI live runtime
- 숨은 LAVI와 fallback Gradio 중복 실행 조사
- LAVI가 시작하거나 재사용한 외부 자식 프로세스의 소유권 확인

금지 사항:

- `taskkill /IM python.exe /F`
- `taskkill /IM java.exe /F`
- `taskkill /IM javaw.exe /F`
- 최초 종료 시도에 `taskkill /T`
- PID 번호 하나만 보고 종료
- ancestor/descendant 목록을 자동 종료 목록으로 사용
- Minecraft Java 프로세스를 LAVI 포트 복구만을 이유로 종료
- 기존 dirty working tree의 reset, checkout 또는 clean

## 2. 증거와 현재 상태의 구분

### 2.1 2026-08-16 runtime snapshot

다음 값은 `2026-08-16 22:04:24 +09:00` 조사 당시 직접 관찰된 snapshot이며,
현재 종료 대상으로 재사용할 수 없다.

| 대상 | 당시 관찰 | 분류 |
| --- | --- | --- |
| Fabric ChatClef WebSocket | `4316 -> PID 25220` | `[runtime snapshot]` |
| Gradio | `47860 -> PID 25220` | `[runtime snapshot]` |
| Chess web server | `8790 -> PID 25220` | `[runtime snapshot]`, optional |
| fallback Gradio | `47861 -> LISTEN 없음` | `[runtime snapshot]` |
| GPT-SoVITS | `9880 -> PID 33336` | `[runtime snapshot]` |
| Minecraft | `javaw.exe PID 31952`, `LAVI_TEST_Fabric01` | `[runtime snapshot]` |

당시 LAVI listener owner는 다음 chain으로 관찰됐다.

```text
PID 30264
  venv\Scripts\python.exe -B main.py
  -> PID 25220
       base python.exe, 같은 LAVI argv
       LISTEN 4316, 47860, 8790
       -> PID 24112
            api_v2.py -a 127.0.0.1 -p 9880
            현재 9880 listener는 아님
            -> PID 28756 conhost.exe
```

`PID 24112`는 LAVI descendant이고 `api_v2.py` argv를 가졌지만 당시 `9880`
listener는 아니었다. 중복, 초기화 실패 또는 비정상 잔류 중 어느 상태인지는
`[unknown]`이며 자동 종료 대상으로 판정하지 않는다.

당시 `9880` listener `PID 33336`은 부모 `PID 24984`가 이미 사라진 상태였다.
생성 시각과 argv상 두 번째 LAVI가 시작했을 가능성이 높지만, 사전 process
snapshot이 없으므로 ownership은 `[inference]`다. 자동 종료 대상으로 판정하지
않는다.

### 2.2 사건 기록에만 남은 값

다음은 과거 로그에서 확인됐지만 위 snapshot 시각에는 더 이상 존재하지 않았다.

```text
두 번째 LAVI 시작: 약 20:12:31
Fabric ChatClef 4316 bind 실패
Gradio 47860 사용 중
Gradio 47861 시작
두 번째 LAVI 본체로 보이는 PID 24984: snapshot 시각에는 없음
```

`47861`이 비어 있어야 정상이라는 일반 규칙은 만들지 않는다. unrelated process가
해당 포트를 사용할 수 있다. 판정 대상은 "두 번째 LAVI가 fallback 범위의 다른
포트를 소유하는가"이다.

## 3. LAVI process identity 계약

### 3.1 승인된 production app entrypoint

현재 소스에서 확인된 entrypoint는 다음과 같다.

| entrypoint | source evidence |
| --- | --- |
| `python main.py` / venv Python의 `main.py` | `main.py:4-9` |
| `run.bat` | `run.bat:5-28` |
| `run_lav_dev.cmd` | `run_lav_dev.cmd:2-6` |
| `python -m lavi` | `lavi/__main__.py:1-6`, `lavi/cli.py:36-49,65-69` |
| `python -m lavi app` | `lavi/cli.py:36-49,65-69` |

따라서 `CommandLine`에 `main.py`가 반드시 있어야 한다는 규칙은 금지한다.
`python -m lavi`와 `python -m lavi app`을 놓칠 수 있기 때문이다.

### 3.2 최소 identity 근거

LAVI 후보를 수동 종료 대상으로 승인하려면 다음 근거가 서로 일치해야 한다.

1. 승인된 entrypoint 중 하나와 argv가 일치한다.
2. repository 절대 경로, repository venv argv 또는 venv launcher parent가
   확인된다.
3. 생성 시각이 launch log 또는 운영자가 시작한 시각과 일치한다.
4. `4316`과 의도한 Gradio endpoint의 listener owner가 일치한다.
5. read-only runtime status가 `backend_id=fabric_chatclef`를 보고한다.
6. 상대경로 `main.py`인 경우 working directory 또는 launch 기록으로 repository
   identity를 보강한다.

PID, process name 또는 `ExecutablePath` 하나만으로는 충분하지 않다. Windows의
venv launcher가 base Python을 시작하면 실제 listener PID의 `ExecutablePath`는
repository venv가 아니라 base Python으로 보일 수 있다.

확인 권한이 없거나 argv, 경로, listener ownership이 모순되면 `[확인 불가]`로
보고하고 종료하지 않는다.

## 4. Fabric disconnect와 AltoClef 작업

LAVI 연결 해제와 AltoClef 작업 종료는 동일한 사건이 아니다.

현재 소스 경로:

- `FabricChatClefBridgeEntrypoint.java:13-18`
- `FabricChatClefBridgeClient.java:141-178`
- `FabricChatClefCommandDispatcher.java:42-80,83-139,175-182`
- `FabricChatClefCommandExecutionState.java:90-109`
- `FabricChatClefCommandQueue.java:114-132`
- `AltoClef.java:669-671`
- `UserTaskChain.java:63-90`
- `SingleTaskChain.java:68-78`
- `Task.java:169-203`

[source] WebSocket `onClose()` 또는 `onError()`는 detach event를 queue에 넣는다.
실제 detach 처리는 다음 Minecraft client tick에서 수행된다.

[source] 다음 조건이 맞을 때만 owned user task에 `cancelUserTask()`를 시도한다.

- 동일 connection generation의 active request가 존재
- terminal result send가 진행 중이지 않거나 defer 후 재처리됨
- current task가 bound root와 같은 Java object instance
- AltoClef user task chain을 사용할 수 있음

다음 경우 취소가 생략, 연기 또는 미처리될 수 있다.

- Minecraft client tick이 진행되지 않음
- generation 불일치
- active request 없음
- terminal send in flight
- current task 조회 실패
- bound root ownership 불일치
- user task chain 없음

정확한 운영 표현은 다음과 같다.

> LAVI 연결이 끊기면 Java는 client tick과 ownership 조건이 만족되는 active
> user task에 best-effort 취소를 시도한다. 작업이 반드시 계속되거나 반드시
> 취소된다고 보장할 수 없다.

[source] detach 처리 자체는 새 terminal result를 Python에 보장하지 않으며,
reconnect 시 원래 command를 자동 replay하지 않는다.

따라서 가능한 경우 종료 전에 read-only status에서 `active_request_id`가 비어
있는지 확인한다. active request가 있다면 단순 포트 정리를 위해 즉시 종료하지
않고, 운영자가 승인한 정상 stop/cancel 절차와 terminal 상태 확인을 우선한다.
비상 external/forced termination은 terminal delivery가 사라질 수 있음을 기록한다.

## 5. 종료 방식의 분류

| 방식 | 분류 | 보장 |
| --- | --- | --- |
| LAVI를 실행한 같은 전경 콘솔에서 `Ctrl+C` | graceful shutdown | `KeyboardInterrupt`와 runtime shutdown 경로를 기대할 수 있음 |
| `taskkill /PID <exact-pid>` | external termination | Ctrl+C와 같은 Python cleanup을 보장하지 않음 |
| `taskkill /PID <exact-pid> /F` | forced termination | `finally`/`atexit`/child cleanup을 기대하지 않음 |
| 브라우저 또는 Gradio 탭 닫기 | 종료 아님 | LAVI process는 계속 실행될 수 있음 |

Graceful shutdown의 source path:

```text
Ctrl+C
  -> app_core/gradio_runtime_launcher.py:35-47
  -> RuntimeLifecycle.shutdown()
  -> app_core/runtime_lifecycle.py:63-96
  -> ExtensionRegistry reverse-order stop
  -> Fabric ChatClef adapter/server stop
```

다음 로그가 실제로 있을 때만 Python cleanup이 실행됐다고 판정한다.

```text
[Gradio] KeyboardInterrupt received; shutting down.
[Shutdown] cleanup started.
[Shutdown] cleanup finished.
```

`atexit`은 정상 interpreter 종료의 보조 경로이며 external/forced termination에서
보장되지 않는다.

## 6. 외부 자식 프로세스 경계

| 구성요소 | 현재 source 동작 | 종료 경계 |
| --- | --- | --- |
| GPT-SoVITS | 기존 `9880` 서버가 응답하면 재사용하거나 `api_v2.py` 직접 실행 | `terminate()` 후 wait/kill 없이 handle 제거 가능 |
| LC0 | 직접 실행 | `quit`/wait, terminate/wait, timeout 시 kill; 최종 kill 후 wait는 없음 |
| StarCraft 1.16 | 기본 `CREATE_NO_WINDOW`, 선택 시 새 console | terminate만 하고 wait하지 않음 |
| StarCraft 2 launchers | 직접 실행 | terminate/wait 후 timeout이면 kill/wait |
| VoiceVox/Silero | fire-and-forget 실행 가능 | process handle을 보존하지 않는 경로가 있음; 당시 module은 disabled |

Windows subprocess 기본 flag는 별도 지정이 없으면 `CREATE_NO_WINDOW`일 수 있다
(`core/process.py:13-16,26-57`). 따라서 LAVI 본체를 전경에서 실행해도 모든
외부 자식이 창으로 보인다고 가정하지 않는다.

종료 전에는 direct child만이 아니라 다음을 snapshot한다.

- listener owner의 전체 ancestor chain
- listener owner의 전체 descendant tree
- descendant와 외부 listener의 argv, 생성 시각, listener port
- LAVI launch log에서 확인되는 child start 기록

이 snapshot은 자동 kill list가 아니다. 부모가 사라진 프로세스는 PID reuse와
사전 증거 부재 때문에 현재 argv만으로 ownership을 확정할 수 없다.

## 7. 읽기 전용 사전 조사

### 7.1 현재 listener 확인

실제 설정을 먼저 확인한다. 2026-08-16 조사에서는 Gradio 설정 section이 없어
코드 default `47860`, max attempts `100`이 적용되어 effective range가
`47860..47959`였다. 이 범위를 영구 상수로 간주하지 않는다.

```powershell
# 현재 조사 snapshot의 default range 예시.
# 실제 config/source default를 다시 확인한 뒤 값을 사용한다.
$fabricPort = 4316
$gradioPorts = 47860..47959
$optionalPorts = 8790, 9880
$ports = @($fabricPort) + @($gradioPorts) + @($optionalPorts)

Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in $ports } |
    Sort-Object LocalPort |
    Format-Table LocalAddress, LocalPort, State, OwningProcess -AutoSize
```

### 7.2 process record

```powershell
$laviPid = [int](Read-Host '현재 검증한 LAVI listener PID')

Get-CimInstance Win32_Process -Filter "ProcessId=$laviPid" |
    Select-Object Name, ProcessId, ParentProcessId, CreationDate,
        ExecutablePath, CommandLine |
    Format-List
```

### 7.3 ancestor chain

```powershell
function Get-ProcessAncestors {
    param([Parameter(Mandatory)][int]$ProcessId)

    $seen = @{}
    $current = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId" -ErrorAction SilentlyContinue

    while ($null -ne $current -and -not $seen.ContainsKey([int]$current.ProcessId)) {
        $seen[[int]$current.ProcessId] = $true
        $current | Select-Object Name, ProcessId, ParentProcessId,
            CreationDate, ExecutablePath, CommandLine

        if ([int]$current.ParentProcessId -le 0) { break }
        $current = Get-CimInstance Win32_Process -Filter "ProcessId=$($current.ParentProcessId)" -ErrorAction SilentlyContinue
    }
}

Get-ProcessAncestors -ProcessId $laviPid | Format-List
```

### 7.4 descendant tree

```powershell
function Get-ProcessDescendants {
    param([Parameter(Mandatory)][int]$RootProcessId)

    $all = @(Get-CimInstance Win32_Process)
    $frontier = @([pscustomobject]@{ ProcessId = $RootProcessId; Depth = 0 })
    $result = @()
    $seen = @{}

    while ($frontier.Count -gt 0) {
        $next = @()
        foreach ($node in $frontier) {
            if ($seen.ContainsKey([int]$node.ProcessId)) { continue }
            $seen[[int]$node.ProcessId] = $true

            $children = @($all | Where-Object {
                [int]$_.ParentProcessId -eq [int]$node.ProcessId
            })

            foreach ($child in $children) {
                $record = [pscustomobject]@{
                    Depth = [int]$node.Depth + 1
                    Name = $child.Name
                    ProcessId = [int]$child.ProcessId
                    ParentProcessId = [int]$child.ParentProcessId
                    CreationDate = $child.CreationDate
                    ExecutablePath = $child.ExecutablePath
                    CommandLine = $child.CommandLine
                }
                $result += $record
                $next += [pscustomobject]@{
                    ProcessId = [int]$child.ProcessId
                    Depth = [int]$node.Depth + 1
                }
            }
        }
        $frontier = $next
    }

    $result
}

$descendantsBefore = @(Get-ProcessDescendants -RootProcessId $laviPid)
$descendantsBefore | Sort-Object Depth, ProcessId | Format-List
```

### 7.5 AVG 또는 권한 차단 시 관리자 read-only 재시도

7.1~7.4의 읽기 전용 PowerShell 조사가 AVG Behavior Shield 탐지(예:
`IDP.HELU.PSE88`) 또는 접근 거부로 실행되지 않을 때에만 이 절차를 사용한다.

1. 차단된 명령이 listener, process identity, ancestor 또는 descendant를 읽기만
   하는지 다시 확인한다. `taskkill`, `Stop-Process`, `/T`, `/F`, 파일 쓰기, 설정
   변경, registry 변경 또는 보안 기능 변경이 포함되면 이 절차를 사용하지 않는다.
2. 실행할 평문 명령과 조회 목적을 사용자에게 먼저 보여 주고 관리자 실행에 대한
   명시적 승인을 받는다.
3. 승인 후 보이는 관리자 PowerShell 창을 한 번 열고, `-EncodedCommand` 대신
   검토된 동일 read-only 명령을 평문으로 한 번만 실행한다.
4. UAC 승인 여부, AVG 탐지 이름, 명령의 성공 또는 차단 결과와 관찰한 PID/port만
   기록한다. 관리자 실행을 process 종료, Minecraft command 전송 또는 다른 작업의
   포괄 승인으로 해석하지 않는다.
5. AVG가 관리자 창에서도 다시 차단하면 즉시 중단한다. AVG/Behavior Shield를
   비활성화하거나 PowerShell 폴더 전체를 예외 처리하거나 반복 재시도하지 않는다.

자동 test/preflight가 스스로 권한을 올리는 것은 금지한다. 이 절차는 사용자가
그 시점의 정확한 읽기 전용 명령을 확인하고 승인한 수동 운영 절차이며, 관리자
권한으로 실행했다는 사실만으로 조회 결과의 LAVI identity가 증명되지는 않는다.

### 7.6 종료 직전 TOCTOU 재검증

처음 조사와 실제 종료 사이에 PID가 재사용될 수 있다. 종료 명령 직전에 다음을
다시 비교한다.

- ProcessId
- CreationDate
- ExecutablePath
- CommandLine
- ParentProcessId와 ancestor chain
- `4316` 및 의도한 Gradio endpoint listener ownership
- runtime status의 `backend_id`, `connected`, `lifecycle_state`,
  `active_request_id`

하나라도 달라졌거나 확인할 수 없으면 종료하지 않는다.

## 8. 수동 복구 절차

### 8.1 우선순위

1. 소유한 전경 콘솔이 있으면 그 콘솔에서 `Ctrl+C`를 사용한다.
2. 숨은 고아 LAVI라 콘솔 신호를 전달할 수 없고 사용자가 종료를 승인한 경우에만
   검증된 listener owner PID 하나에 external termination을 사용한다.
3. 같은 PID가 계속 남고 사용자가 forced termination을 별도로 승인한 경우에만
   `/F`를 사용한다.
4. `/T`, image-name kill 또는 ancestor/descendant 일괄 kill은 사용하지 않는다.

### 8.2 exact listener PID 종료

```powershell
# 반드시 바로 앞 단계에서 identity를 다시 검증한 exact PID만 사용한다.
taskkill /PID $laviPid
```

이 명령을 graceful shutdown으로 기록하지 않는다. cleanup marker가 없다면
`cleanup 실행 여부 불명`으로 남긴다.

프로세스가 계속 존재하고 forced termination이 명시적으로 승인된 경우에만:

```powershell
taskkill /PID $laviPid /F
```

### 8.3 종료 후 검증

```powershell
Get-Process -Id $laviPid -ErrorAction SilentlyContinue

Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in $ports } |
    Sort-Object LocalPort |
    Format-Table LocalAddress, LocalPort, State, OwningProcess -AutoSize
```

판정 기준:

- exact listener PID가 사라졌는지
- `4316`과 이전 Gradio endpoint에서 LAVI listener가 사라졌는지
- `TIME_WAIT`가 아니라 `LISTEN` 소유권이 남았는지
- 이전 ancestor launcher가 자연 종료됐는지
- 종료 전에 기록한 descendant가 남았는지
- 남은 descendant 또는 orphan이 실제 listener를 소유하는지

ancestor launcher 또는 descendant가 남았더라도 자동 종료하지 않는다. argv,
생성 시각, listener ownership과 사전 snapshot을 다시 검토하고 정확한 PID에 대한
별도 사용자 승인이 있어야 한다.

## 9. 재실행 순서

### 9.1 Minecraft를 유지하는 경우

1. 기존 LAVI listener와 fallback LAVI 후보를 정리하고 LISTEN 상태를 확인한다.
2. 새 전경 PowerShell/CMD에서 LAVI를 한 번만 실행한다.
3. `4316`과 의도한 Gradio endpoint가 같은 새 LAVI PID인지 확인한다.
4. 다른 fallback port에 두 번째 LAVI candidate가 없는지 확인한다.
5. Fabric ChatClef의 `connected=true`, `lifecycle_state=connected`를 기다린다.
6. 기존 Minecraft가 reconnect하지 않을 때만 Minecraft 정상 재시작을 검토한다.

```powershell
Set-Location 'C:\Vtuber_Souorce_Code\LAVI'
.\run.bat
```

`8790`은 Chess가 활성화된 경우에만 관찰되는 optional port다. `9880`은 별도
GPT-SoVITS process 또는 재사용된 외부 서버일 수 있으므로 LAVI identity의 필수
조건으로 사용하지 않는다.

### 9.2 완전 재시작이 필요한 경우

1. Minecraft의 진행 중 작업을 운영자가 정상 중지하고 월드를 저장한다.
2. Minecraft를 정상 종료한다.
3. 숨은 LAVI와 port 상태를 위 runbook으로 정리한다.
4. LAVI를 전경에서 한 번 실행한다.
5. `4316`과 Gradio listener를 확인한다.
6. Fabric Minecraft instance를 한 번 실행한다.
7. handshake, connected 상태와 idle 상태를 확인한다.

## 10. 재발 방지

live runtime에서는 다음 실행 방식을 사용하지 않는다.

```text
Start-Process -WindowStyle Hidden
pythonw.exe
start /B
detached/background launch
종료 신호를 전달할 콘솔과 운영자가 없는 실행
```

기본 실행은 visible foreground `run.bat`이다. 종료 시 브라우저 탭이 아니라
실행한 콘솔에서 `Ctrl+C`를 사용하고 cleanup marker와 LISTEN 해제를 확인한다.

production process-wide single-instance guard, `4316` bind failure의 fatal 처리,
Gradio 자동 증가 정책 변경은 이 runbook의 범위가 아니며 별도 설계와 승인이
필요하다.

## 11. 완료 기준

- 현재 PID와 listener ownership을 종료 직전에 다시 확인했다.
- exact listener PID 외의 process를 일괄 종료하지 않았다.
- ancestor와 descendant를 증거로 snapshot했지만 자동 kill list로 사용하지 않았다.
- Minecraft Java process를 건드리지 않았다.
- 종료 방식을 graceful/external/forced 중 하나로 정확히 기록했다.
- cleanup marker의 실제 존재 여부를 기록했다.
- 종료 후 LAVI listener와 남은 external child/orphan을 다시 확인했다.
- 새 LAVI를 visible foreground에서 한 번만 실행했다.
- fallback 범위에서 두 번째 LAVI candidate가 없는지 확인했다.
