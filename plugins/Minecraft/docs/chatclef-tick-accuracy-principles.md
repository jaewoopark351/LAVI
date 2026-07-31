<!-- 20260731_kpopmodder: ChatClef / Carry On 화로 handoff 조사 뒤 tick 기반 정확도 원칙을 기록했다. -->

# ChatClef Tick 정확도 원칙

## 한 줄 요약

Minecraft AI 자동화는 속도보다 정확도가 우선이다. 각 Task는 이전 Task의 결과가 실제 Minecraft 상태에 반영된 것을 확인한 뒤 다음 Task로 넘어가야 하며, 기다림에는 반드시 bounded tick timeout을 둬서 무한루프와 무한대기를 막아야 한다.

## 핵심 원칙

ChatClef / AltoClef 자동화는 빠른 자동 클릭기가 아니라 tick 기반 state machine이어야 한다.

Minecraft는 정상 20 TPS 기준으로 1 tick이 약 50 ms다. 사람 눈에는 거의 동시에 일어난 것처럼 보여도, 실제로는 다음 상태들이 서로 다른 tick 경계에서 반영될 수 있다.

```text
Task onStop cleanup
input press / release
playerSneaking 상태
world block state
server -> client GUI open packet
currentScreenHandler 변경
Baritone pathing state
Carry On carrying state
```

따라서 AI는 다음 흐름을 목표로 해야 한다.

```text
이전 Task cleanup 확인
-> 게임 상태 안정화 확인
-> 다음 interaction 시도
-> 기대한 상태 변화 확인
-> terminal success 또는 bounded failure
```

반대로 다음 흐름은 위험하다.

```text
이전 Task가 끝났다고 판단
-> 즉시 다음 target 클릭
-> click result=SUCCESS를 실제 성공으로 간주
```

## 왜 비동기 멀티스레드가 아니라 main tick인가

Minecraft client 상태 대부분은 client main thread / client tick 흐름에서 안전하게 읽고 변경해야 한다.

```text
player 위치와 sneaking 상태
world block state
inventory / slot state
currentScreenHandler
input pressed / released 상태
server packet 처리 결과
Baritone pathing state
Carry On carrying state
```

이런 상태를 별도 background thread에서 동시에 읽거나 바꾸면 다음 문제가 더 쉽게 생긴다.

```text
아직 갱신되지 않은 상태를 읽음
반쯤 갱신된 상태를 읽음
서버 GUI open packet 처리 전 상태를 실패로 오해함
input release / press 순서가 꼬임
ScreenHandler가 null이거나 이전 값인 상태를 읽음
희귀 race, NPE, ConcurrentModificationException 발생
```

비동기로 빼도 되는 것은 Minecraft live state를 직접 건드리지 않는 작업이다.

```text
로그 파일 분석
외부 API 요청
명령 파싱
순수 계산
진단 결과 후처리
```

Minecraft 상태를 읽거나 바꾸는 핵심 자동화는 main tick 안에서 천천히, 정확하게 처리하는 편이 안전하다.

## Task 경계 원칙

어떤 Task가 input, path, goal, cleanup을 소유했다면, 다음 Task는 이전 Task가 끝났다는 이유만으로 그 소유 상태가 이미 정리됐다고 가정하면 안 된다.

민감한 경계에서는 다음 순서를 지킨다.

1. 이전 child Task가 기존 Task lifecycle을 통해 stop / cleanup 할 기회를 준다.
2. 로그로 증명된 경우, cleanup이 반영될 tick 경계를 보장한다.
3. cleanup 전 상태가 아니라 cleanup 후 게임 상태를 확인한다.
4. 추가 안정화가 필요하면 작은 bounded tick budget을 사용한다.
5. 사용자가 직접 키를 누르거나 다른 Task/mod가 상태를 잡고 있을 수 있으므로 무한히 기다리지 않는다.
6. budget 초과 시 terminal diagnostic을 1회 남기고, owning Task의 기존 lifecycle로 빠져나간다.

하지 말아야 할 것:

```text
전역 input 강제 release
전체 Baritone path / goal cancel
TaskRunner 전체 stop
무제한 retry
무제한 timeout
관측 실패를 success로 변환
```

## 화로 / Carry On 사례

이번 화로 문제는 속도가 정확도를 이긴 순간에 생긴 race였다.

실패 흐름:

```text
PlaceBlockNearbyTask가 화로 설치
-> stopPlacing / onStop에서 SNEAK release
-> 같은 tick 경계에서 다음 container Task가 바로 화로 우클릭
-> ChatClef는 shiftClick=false인 정상 우클릭을 요청
-> 하지만 클릭 직전 playerSneaking=true가 아직 남아 있음
-> Carry On이 sneaking + right-click을 container open이 아니라 block pickup으로 처리
-> FurnaceScreenHandler가 열리지 않음
-> 설치한 화로 위치가 minecraft:air로 바뀜
-> ChatClef는 화로가 없다고 판단
-> 다시 화로 만들기 / 구하기 루프로 진입
```

중요한 구분:

```text
interactBlock result=SUCCESS
```

는 GUI가 열렸다는 뜻이 아니다.

container workflow에서 성공 조건은 실제 Minecraft 상태여야 한다.

```text
player.currentScreenHandler instanceof FurnaceScreenHandler
```

또는 해당 버전에 맞는 ScreenHandler open 상태를 확인해야 한다.

목표 흐름:

```text
화로 설치 완료
-> 한 tick 경계에서 PlaceBlockNearbyTask.onStop cleanup 실행
-> SNEAK release가 반영됐는지 확인
-> playerSneaking / rawSneakKeyPressed 안정화 확인
-> 안정화되면 normal right-click
-> FurnaceScreenHandler open 확인
-> 요리 workflow 진행
-> N tick 안에 GUI가 열리지 않으면 terminal diagnostic 기록 후 무한루프 방지
```

## Timeout 원칙

정확도를 위해 기다림을 추가할 때는 반드시 명시적인 제한이 있어야 한다.

위험한 형태:

```text
while playerSneaking:
    return null
```

안전한 방향:

```text
POST_PLACE_HANDOFF
-> WAIT_FOR_STABILITY for at most N ticks
-> stable이면 PROCEED
-> budget 초과면 TIMEOUT
```

timeout은 성공이 아니다.

timeout은 현재 상태를 보존하고, 한 번만 terminal diagnostic을 남긴 뒤, 가장 작은 owning Task 경계로 제어를 돌려야 한다.

## 진단 로그 원칙

진단은 timing boundary를 증명해야지, timing boundary 자체를 교란하면 안 된다.

기본 로그는 다음 정도만 남긴다.

```text
operation start
실제 state transition
first failure
terminal success 또는 terminal failure
```

고빈도 진단은 기본 OFF 또는 강한 rate limit이 필요하다.

```text
매 tick PRE/POST snapshot
매 Task tick log
매 block update log
모든 input 요청의 caller stack
heartbeat log
정상 흐름 WARN log
```

이번 문제처럼 tick 경계 race를 조사할 때는 로그 자체가 tick 처리 시간과 입력 적용 시점을 바꿀 수 있다. 따라서 진단은 bounded, lazy, opt-in이어야 한다.

## 새 Minecraft AI Task 체크리스트

Task를 만들거나 수정하기 전에 다음을 확인한다.

```text
무엇이 실제 성공 상태인가?
click result가 아니라 어떤 Minecraft state를 확인해야 하는가?
각 input은 어느 Task가 소유하는가?
timeout은 어느 Task가 소유하는가?
retry count와 terminal reason은 누가 소유하는가?
다음 interaction 전에 어떤 cleanup이 먼저 실행돼야 하는가?
기다리는 상태가 사용자, 다른 Task, 다른 mod에 의해 계속 유지될 수 있는가?
bounded tick budget은 몇 tick인가?
budget 초과 시 무엇을 기록하고 어디로 빠져나가는가?
Carry On 같은 optional mod가 없거나 incompatible이면 어떻게 동작하는가?
diagnostics OFF 상태에서도 비용이 발생하는가?
```

기본 결론:

```text
빠르게 추측해서 진행하지 말고,
조금 느리더라도 게임 상태가 확정된 것을 확인한 뒤 진행한다.
```
