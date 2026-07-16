# P0 implementation contract — current-main aware

P0는 한 번에 수행하지 않는다. reconciliation에서 사용자가 승인한 delta와 ExecPlan을 기준으로 P0-A, P0-B, P0-C 순서로 진행한다. 각 단계가 review를 통과하기 전 다음 단계로 넘어가지 않는다.

공통 계약은 `HANDOFF_CONTRACT.md`를 따른다. ExecPlan은 저장소 `.agent/PLANS.md`가 있으면 그 규칙을, 없으면 pack-local `.agent/PLANS.md`를 따른다.

## P0 전제

- `prompts/00_RECONCILE_REVIEW_ONLY.txt` 결과가 제출되어야 한다.
- 현재 main의 tracked audit와 코드 기준 delta matrix가 있어야 한다.
- 사용자가 지원할 profile/accelerator matrix, dependency source, lock 도구를 승인해야 한다.
- root `modules.json`은 수정하지 않는다.
- 이번 구현 턴에서는 commit/push하지 않는다.

# P0-A — dependency와 canonical installer

## 목표

clean Windows checkout에서 최소한 Core/CPU를 한 명령으로 설치하고, 기존 cu130 경로를 깨뜨리지 않으며, 같은 명령을 두 번 실행해도 성공하게 한다.

## 1. 지원 matrix를 먼저 확정

다음 10개 조합을 근거 없이 모두 lock으로 만들지 않는다.

```text
5 profiles × 2 accelerators
```

reconciliation에서 다음을 조사한다.

- Python 3.14 wheel 지원
- Torch CPU/cu130 wheel과 index
- Voice/Vision/Games의 직접/전이 dependency
- profile 간 dependency 중복
- CI 비용과 lock 갱신 비용

최소 mandatory contract:

- `Core + CPU`: 새 배포 최소 경로
- 기존 production/cu130 경로: 현재 지원을 보존하는 approved 조합

Voice/Vision/Games/Full 조합은 실제 지원 가능성과 사용자 승인을 기준으로 추가한다. 지원하지 않는 조합은 조용히 다른 lock을 설치하지 말고 actionable error를 낸다.

## 2. dependency source of truth

하나의 사람이 수정하는 source와 generated lock을 구분한다.

권장 후보는 `requirements/profiles/*.in` 및 공통 constraints이지만, 현재 저장소와 Python 3.14에서 검증된 도구를 reconciliation에서 결정한다. `pip-tools`, `uv`, 표준 lock 중 하나를 선택하고 두 개를 동시에 canonical로 두지 않는다.

필수 계약:

- installer는 approved committed lock을 실제 사용
- direct import dependency 직접 선언
- legacy snapshot은 canonical source가 아님을 문서화
- lock refresh 명령과 drift 검증 제공
- hash lock을 사용한다면 실제 설치가 `--require-hashes` 또는 동등한 무결성 계약을 사용
- resolver가 hash lock을 만들 수 없으면 임의로 hash를 제거하지 말고 원인과 대안을 보고
- Python 3.14 및 기존 cu130 pin을 임의 변경하지 않음

## 3. canonical Windows installer

`scripts/install_windows.ps1`를 공식 진입점으로 정리한다.

목표 interface:

```powershell
.\scripts\install_windows.ps1 -Profile Core -Accelerator CPU
```

approved matrix의 다른 조합도 동일한 interface를 사용한다.

필수:

- profile/accelerator validation
- approved committed lock 선택과 출력
- 기존 venv를 이유 없이 삭제하지 않음
- 부분 실패 후 재실행 가능
- 같은 명령 두 번 성공
- Core/CPU에서 CUDA wheel 강제 설치 금지
- 기존 cu130 production 경로 보존
- 설치 후 `pip check`와 profile-aware preflight
- 오류 단계와 복구 명령 출력

`switch_CPU.bat`, `switch_GPU.bat`는 canonical installer wrapper 또는 명확한 deprecated guard로 만든다. Miniconda, Torch 2.2.2, cu121 별도 설치 흐름을 유지하지 않는다.

README의 CURRENT 명령은 실제 installer와 일치해야 한다.

## P0-A acceptance

- clean Windows Core/CPU install 성공
- 같은 명령 2회 성공
- installer log가 실제 committed lock 경로를 출력
- `pip check` 성공
- Core/CPU에 CUDA wheel 강제 설치 없음
- 기존 approved cu130 경로 회귀 없음
- legacy scripts가 별도 환경을 만들지 않음
- root `modules.json` hash 불변

# P0-B — Core runtime, config, smoke

## 1. 기존 구현을 먼저 재사용

다음은 새로 만들지 않는다.

- `core/profile_resolver.py`
- `config/modules.core.json`
- `plugins/NullInput`
- `plugins/NullLLM`
- `plugins/NullTTS`
- `plugins/NullVtuber`

다음을 검증하고 차이만 최소 수정한다.

- 모든 config consumer가 같은 resolver를 사용하는지
- Core가 `config/modules.core.json`을 명시적으로 선택하는지
- 일반 실행이 사용자 override 없을 때 root `modules.json`을 선택하는지
- Null provider가 required category를 채우는지
- 선택되지 않은 optional provider import/construct/init가 0인지

## 2. config/path 저장 안전

- `core/config_manager.py`와 config consumer가 `core/paths.py` 정책을 따르는지 확인
- 사용자 `config/modules.json` 저장은 validation + temp file + atomic replace
- 실패 시 기존 사용자 파일 보존
- root `modules.json` 비변경
- root 파일 migration 없음
- example 파일 production fallback 없음

## 3. startup side effect 차단

Core 경로에서 다음 attempt가 0이어야 한다.

- FFmpeg download
- network/socket/requests/urllib/websocket
- Hugging Face model load/download
- external server/process/game launch
- microphone/audio device open

P0는 전체 plugin lifecycle을 전면 재설계하지 않는다. Core 경로의 unconditional side effect와 eager construction을 차단하는 최소 변경만 수행한다. 전 plugin 공통 state/diagnostic은 P1이다.

## 4. 두 종류의 smoke

### Core offline smoke

- `config/modules.core.json` 명시 사용
- UI 서버 실제 launch 불필요
- composition/startup/shutdown boundary 확인
- timeout
- network/download/model/process attempt counter
- disabled optional import/construct/init counter
- 정상 shutdown

CURRENT 구조를 우선한다.

```powershell
python .\scripts\preflight.py --profile Core --accelerator CPU --offline
python .\scripts\smoke_startup.py --profile Core --modules-config .\config\modules.core.json --accelerator CPU --offline --timeout-sec 30
```

실제 옵션은 current tracked audit와 구현을 기준으로 조정하되, `python -m lavi` package를 P0 범위로 새로 만들지 않는다.

### Production configuration smoke

현재 tracked audit의 `production configuration smoke` 및 `--production-config-smoke` 계약을 유지한다.

- 사용자 override가 없는 상태에서 root `modules.json` 선택
- module ID/schema/dependency 계약 검증
- 외부 resource를 실제로 시작하지 않음
- unavailable resource를 상태/진단으로 평가
- root `modules.json` hash 불변
- Core config로 production config를 대체하지 않음

## P0-B acceptance

- Core smoke 성공
- production configuration smoke 성공
- network/download/model/process attempts 0
- disabled optional import/construct/init 0
- selected Null provider만 필요한 범위에서 construct/init
- startup/shutdown timeout 내 완료
- FFmpeg 없음/네트워크 없음으로 Core가 실패하지 않음
- root `modules.json` 작업 전후 hash 동일
- 기존 사용자 config 비변경 또는 안전한 atomic write 증거

# P0-C — CI, tests, docs, repository contract

## 1. test collection 경계

marker만으로 optional top-level import를 막지 않는다. Core CI는 Core test path/file만 명시적으로 수집한다. Optional profile CI는 해당 dependency가 설치된 job에서 해당 path만 실행한다.

가능한 구조:

```text
tests/core/
tests/profiles/voice/
tests/profiles/vision/
tests/profiles/games/
tests/profiles/full/
```

대규모 파일 이동이 불필요하면 CI에서 명시적 파일 list를 사용할 수 있다. 가장 작은 안전한 diff를 선택한다.

## 2. CI 필수 증거

- approved Core/CPU lock 설치
- `pip check`
- `ruff check`
- Core tests
- Core offline smoke
- production configuration smoke
- installer idempotence를 검증하는 Windows job 또는 별도 reproducibility job
- root `modules.json` tracked/hash/비변경 contract

Optional profile CI에서는 해당 profile Python dependency 누락을 skip으로 숨기지 않는다. 외부 app/model이 없는 것은 `UNAVAILABLE` contract test로 다룬다.

## 3. docs

- README와 installer CURRENT 명령 일치
- target/future 명령을 현재 명령처럼 표시하지 않음
- legacy install 경로 명확히 deprecated
- tracked audit의 더 강한 acceptance와 Git evidence 유지
- pack 문서로 tracked audit를 교체하지 않음

## 4. deployment artifact contract

formal release artifact 생성이 아직 P2라면 새 packaging 체계를 P0에서 만들지 않는다. 다만 현재 존재하는 archive/release/install 경로에서 root `modules.json`이 누락되지 않는 repository contract test를 추가한다. 향후 artifact job의 필수 include로 문서화한다.

## P0-C acceptance

- Core CI green
- Ruff와 pip check 실제 실행
- Core optional collection failure 없음
- production smoke 유지
- README 명령과 workflow 명령 일치
- root `modules.json` tracked 및 기존 배포/archive 경로에서 포함
- tracked audit acceptance 약화 없음

# P0 전체 금지

- root `modules.json` 수정 또는 migration
- 기존 resolver/Null provider 중복 생성
- 근거 없는 10개 lock 생성
- P0를 위해 `lavi` package 구조 신설
- P1 lifecycle/manifest 전면 개편
- 테스트 skip/삭제/assertion 약화
- 광범위 formatting/rename
- 사용자 승인 전 commit/push
