# P1 implementation contract — staged plugin hardening

P1은 P0가 실제 Windows와 CI에서 green이고 사용자가 명시적으로 승인한 뒤, 새 작업 브랜치와 새 Codex 스레드에서 시작한다. P1도 A/B/C로 나눈다.

공통 계약은 `HANDOFF_CONTRACT.md`를 따른다. ExecPlan은 저장소 `.agent/PLANS.md`가 있으면 그 규칙을, 없으면 pack-local `.agent/PLANS.md`를 따른다. root `modules.json`은 수정하지 않는다. same-process isolation만 구현한다. subprocess isolation은 P2다.

# P1-A — contract foundation과 대표 plugin 2개

## 목표

현재 loader/descriptor 구조를 조사해 세 번째 loader를 추가하지 않고, 최소 공통 metadata와 lifecycle foundation을 만든다. 대표 plugin은 우선 다음 두 개다.

- `VoiceInput`: model/device dependency 대표
- `VtubeStudio`: websocket/external service 대표

## 1. canonical metadata contract

기존 AST descriptor/registry가 있으면 확장한다. 별도 manifest 시스템을 중복 추가하지 않는다.

필수 metadata:

```text
id
display_name
api_version
category
entrypoint
dependency_group
capabilities
config_schema
required_python_packages
required_files
required_executables
required_services
supports_offline
supports_cpu
```

metadata 조회는 implementation import 없이 가능해야 한다. duplicate ID, invalid entrypoint, API version mismatch는 해당 plugin만 구조화된 상태로 격리한다.

## 2. lifecycle state

공통 상태:

```text
DISABLED
UNAVAILABLE
READY
STARTING
RUNNING
FAILED
STOPPED
```

- DISABLED: import/construct/init/start 0
- UNAVAILABLE: enabled지만 dependency/resource 부족, application 계속 실행
- READY: static availability 통과, resource start 전
- STARTING: init/start 진행
- RUNNING: 사용 가능
- FAILED: 해당 plugin의 init/start 실패, 다른 plugin 계속
- STOPPED: cleanup 완료, 반복 shutdown 안전

## 3. side-effect boundary

module import와 constructor에서는 다음을 금지한다.

- model load/download
- network/websocket
- subprocess/server/game start
- microphone/audio device open
- background thread start

실제 resource 작업은 availability check 이후 initialize/start에서 수행한다.

## P1-A acceptance

- disabled 대표 plugin import/construct/init/start 0
- missing model/device/service는 UNAVAILABLE
- Core/P0 smoke 회귀 없음
- API version mismatch actionable diagnostic
- stop/shutdown idempotent
- root `modules.json` hash 불변

# P1-B — UI lazy construction과 failure isolation

대표 plugin 한 개를 추가한다. current code 조사 후 `GPTSoVITS` 또는 `ScreenVision` 중 side-effect 종류와 테스트 가능성이 더 적합한 하나를 선택하고 사용자에게 이유를 보고한다.

## 1. metadata-only UI

provider 목록을 그리는 단계에서는 metadata와 availability만 사용한다.

```text
목록 표시 → instance 없음
사용자 선택 → availability check → construct → initialize/start
```

검증:

- UI 목록 조회 후 implementation module이 `sys.modules`에 없음
- constructor counter 0
- 선택 후 1회 construct
- UI rerender로 중복 instance 없음

## 2. failure isolation

- Plugin A import 실패 후 Plugin B READY/RUNNING 가능
- constructor 실패가 registry 전체를 손상하지 않음
- initialize/start 실패가 다른 plugin과 Core를 중단하지 않음
- shutdown 실패가 다른 cleanup을 중단하지 않음
- timeout과 구조화된 error list

## 3. actionable diagnostic

최소 필드:

```text
plugin_id
state
reason_code
human_readable_message
missing_python_packages
missing_files
missing_executables
missing_services
missing_environment_variables
suggested_install_profile
suggested_command
log_reference
```

credential 값은 로그에 출력하지 않는다.

## P1-B acceptance

- UI listing constructor 0
- Plugin A/B isolation test 통과
- init 실패 후 stop/shutdown 안전
- diagnostics에 사용자 조치 포함
- P0 smoke/production smoke green
- modules hash 불변

# P1-C — 단계적 rollout

P1-A/B review가 통과한 뒤, 사용자가 승인한 batch만 확장한다.

후보 batch:

1. 남은 Voice/Vision plugin
2. GPTSoVITS 또는 ScreenVision 중 미적용 plugin
3. Chess/StarCraft game plugin
4. 기타 optional TTS/LLM/chat plugin

각 batch마다 동일한 contract test와 P0 regression을 실행한다. 한 작업에서 “모든 plugin”을 자동 확대하지 않는다.

## sys.path 정책

plugin별 local mutation을 작은 diff로 제거할 수 있으면 P1 batch에서 처리할 수 있다. package 구조 변경, 대규모 파일 이동 또는 전체 import topology 변경이 필요하면 P2로 넘긴다.

## config compatibility

- root `modules.json` key/boolean 의미 유지
- 사용자 `config/modules.json` 값 보존
- root config migration 없음
- 새 metadata는 descriptor 또는 backward-compatible 필드
- config schema migration이 필요하면 별도 승인, backup, validation, opt-in

# P1 전체 금지

- 새로운 세 번째 loader
- root `modules.json` rewrite/migration
- 모든 plugin을 한 번에 전환
- constructor에서 resource 작업
- broad `except Exception: pass`
- P0 installer/lock/config contract 회귀
- subprocess isolation 구현
- package-wide restructuring
- 사용자 승인 전 commit/push
