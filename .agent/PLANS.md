# LAVI ExecPlan 작성 및 실행 규칙

버전: 2026-07-16-safe-handoff-v2.2  
적용 대상: LAVI의 P0/P1처럼 여러 파일과 검증 단계를 포함하는 작업

## 이 파일의 역할

이 문서는 복잡한 작업을 시작하기 전에 Codex가 작성하고, 구현 중 계속 갱신해야 하는 **self-contained ExecPlan**의 규칙이다.

적용 우선순위:

1. 현재 저장소에 실제 `C:\Vtuber_Souorce_Code\LAVI\.agent\PLANS.md`가 존재하면 그 파일을 우선한다.
2. 저장소 파일이 없다면 이 pack-local 파일인 `logs/LAVI_Codex_Handoff_Pack_v2/.agent/PLANS.md`를 fallback으로 사용한다.
3. 이 pack-local 파일을 저장소 루트로 자동 복사하거나 tracked 파일로 추가하지 않는다. 저장소에 영구 반영하려면 사용자의 별도 승인이 필요하다.
4. 사용자의 최신 명시적 요청, 적용되는 `AGENTS.md`, 실제 코드/tests/CI, tracked 감사 문서와 충돌하면 임의로 해석하지 말고 충돌을 보고하고 멈춘다.

## ExecPlan이 필요한 작업

다음 중 하나라도 해당하면 구현 전에 ExecPlan을 작성한다.

- 여러 디렉터리 또는 세 개 이상의 파일을 수정하는 작업
- dependency, installer, config, CI, plugin lifecycle처럼 cross-cutting한 작업
- 30분 이상 걸릴 가능성이 있는 작업
- migration, rollback, compatibility가 필요한 작업
- Windows 전용 검증과 CI 검증을 함께 요구하는 작업
- P0-A, P0-B, P0-C, P1-A, P1-B, P1-C 단계

단순 오탈자 수정처럼 범위가 명확하고 되돌리기 쉬운 한 파일 변경은 별도 ExecPlan 없이 진행할 수 있지만, 사용자가 계획을 요구하면 항상 작성한다.

## 계획 파일 위치

### Read-only reconciliation 단계

파일을 만들지 않는다. ExecPlan 초안은 Codex 응답에만 제시한다.

### 승인된 구현 단계

사용자가 별도 위치를 지정하지 않으면 다음 ignored 작업 경로에 작성한다.

```text
logs/codex/execplans/<phase>-<YYYYMMDD-HHMMSS>.md
```

예:

```text
logs/codex/execplans/P0-A-20260716-143000.md
```

- tracked 문서나 source file에 계획을 섞지 않는다.
- plan 파일을 commit하지 않는다.
- commit/push 승인 전까지 plan도 포함해 저장소 추적 상태를 바꾸지 않는다.
- `logs/`가 ignored인지 시작 시 확인한다. ignored가 아니라면 파일을 쓰지 말고 응답 안에서만 계획을 유지한다.

## 핵심 원칙

### 1. Self-contained

새로운 개발자나 다른 Codex 세션이 이전 대화를 보지 않아도 다음을 이해할 수 있어야 한다.

- 무엇을 왜 바꾸는지
- 현재 구현은 무엇인지
- 이미 존재하는 파일과 재사용할 구현
- 정확한 변경 범위와 금지 범위
- 완료를 증명할 명령
- 실패 시 되돌리는 방법
- 사용자 결정이 필요한 사항

“앞서 논의한 대로”, “필요한 파일”, “적절히 수정” 같은 모호한 표현을 쓰지 않는다.

### 2. Living document

ExecPlan은 작성 후 고정되는 문서가 아니다. 작업 중 다음 섹션을 실제 사실에 맞게 갱신한다.

- Progress
- Surprises & Discoveries
- Decision Log
- Verification Results
- Outcomes & Retrospective

코드가 계획과 달라지면 계획을 먼저 수정하고 이유를 Decision Log에 기록한다.

### 3. 사실과 가정 분리

모든 항목을 다음 중 하나로 분류한다.

- `CONFIRMED`: 파일 또는 실제 명령으로 확인
- `ASSUMPTION`: 아직 확인하지 못한 가정
- `TARGET`: 이번 단계에서 구현할 계약
- `FUTURE`: 이후 단계 또는 별도 승인 사항

실행하지 않은 테스트를 통과했다고 쓰지 않는다. Windows에서만 확인 가능한 항목을 Linux 결과로 대체하지 않는다.

### 4. 최소 diff와 단계 경계

- 이미 존재하는 구현을 중복 생성하지 않는다.
- acceptance와의 차이만 최소 수정한다.
- P0 작업에서 P1 manifest/lifecycle 전면 개편을 시작하지 않는다.
- P1에서 package-wide restructuring 또는 subprocess isolation을 시작하지 않는다.
- 단계가 review를 통과하기 전 다음 단계로 넘어가지 않는다.

### 5. Stop-and-report

다음 상황에서는 임의로 계속하지 말고 상태와 선택지를 보고한다.

- baseline branch, commit 또는 working tree가 예상과 다름
- root `modules.json` hash drift
- 적용되는 지침 간 충돌
- 지원 dependency/wheel이 없어 승인된 matrix를 구현할 수 없음
- 테스트를 통과시키려면 범위 확대 또는 요구 약화가 필요함
- 기존 사용자 변경과 작업 diff가 섞일 위험
- destructive Git 또는 저장소 밖 변경이 필요함
- commit/push가 필요하지만 별도 승인이 없음

## LAVI 불변 계약

모든 P0/P1 ExecPlan에 다음을 그대로 포함한다.

### root `modules.json`

- 공식 production/deployment default
- Git tracked 상태 유지
- 삭제, 이동, 이름 변경 금지
- `config/modules.json`으로 migration 금지
- installer, smoke, UI, migration에 의한 자동 rewrite 금지
- Core-safe 값으로 전체 교체 금지
- 사용자 설정 저장 대상으로 사용 금지
- 사용자 override가 없으면 일반 실행이 이 파일을 사용
- 작업 전후 `git hash-object modules.json` 비교

운영자 제공 기준 hash:

```text
ddffc5475ef92bd40e5b0e08bce42e0c6b2b1019
```

실제 시작 hash가 다르면 baseline drift로 보고하고 수정하지 않는다.

### Core 설정

- Core는 tracked `config/modules.core.json`을 명시적으로 사용
- `config/modules.example.json`은 production fallback이 아님
- 일반 실행 resolution은 명시적 config → 사용자 `config/modules.json` → root `modules.json` → actionable error

### Git 안전

- `git reset --hard` 금지
- `git clean` 금지
- 기존 사용자 변경 삭제/덮어쓰기 금지
- 저장소 밖 파일 수정 금지
- commit/push는 사용자의 별도 명시 승인 전까지 금지
- 테스트 실패를 skip, broad exception, assertion 약화로 숨기지 않음

### 명령 상태

모든 검증 명령에 다음 라벨을 붙인다.

- `CURRENT`: 지금 checkout에서 실행 가능
- `TARGET`: 이 단계에서 구현해야 실행 가능
- `FUTURE`: P2 또는 별도 승인 이후

현재 `lavi/__main__.py`가 없다면 `python -m lavi doctor/smoke`를 CURRENT로 표시하지 않는다.

## 필수 ExecPlan 구조

아래 제목을 유지한다. 해당 사항이 없으면 “없음”과 이유를 쓴다.

```markdown
# <Phase>: <작업 제목> ExecPlan

## Status
## Purpose and observable outcome
## Baseline and current state
## Scope
## Out of scope
## Invariants
## Existing implementation to reuse
## Assumptions and open questions
## Milestones
## Verification plan
## Progress
## Surprises & Discoveries
## Decision Log
## Risks and mitigations
## Rollback plan
## Verification Results
## Outcomes & Retrospective
```

### Status

최소 필드:

```text
phase:
state: DRAFT | APPROVED | IN_PROGRESS | BLOCKED | COMPLETE
created_at:
updated_at:
baseline_branch:
baseline_commit:
working_tree_at_start:
modules_json_hash_at_start:
approval_source:
```

승인되지 않은 계획은 `DRAFT`다. 사용자가 명시적으로 승인한 뒤에만 `APPROVED` 또는 `IN_PROGRESS`로 바꾼다.

### Purpose and observable outcome

구현 세부가 아니라 사용자가 관찰할 수 있는 완료 상태를 적는다.

예:

```text
clean Windows checkout에서 Core/CPU installer를 같은 명령으로 두 번 실행해 성공하고,
installer가 승인된 committed lock을 실제 사용하며 root modules.json hash가 변하지 않는다.
```

### Baseline and current state

실제 read-only 명령과 결과를 기록한다.

필수 후보:

```powershell
Get-Location
git rev-parse --show-toplevel
git rev-parse HEAD
git branch --show-current
git status --short
git ls-files --error-unmatch modules.json
git hash-object modules.json
```

파일 존재만으로 동작을 확인했다고 간주하지 않는다. 기존 구현은 “present but unverified”로 분류할 수 있다.

### Scope

- 수정할 정확한 파일 또는 디렉터리
- 허용되는 새 파일
- 각 파일에서 변경할 symbol/책임
- 이번 단계에 포함되는 테스트와 문서

### Out of scope

다음 단계로 넘길 항목을 구체적으로 적는다.

예:

```text
- P1 공통 lifecycle enum
- 전 plugin manifest 전환
- python -m lavi package entrypoint
- subprocess plugin isolation
- package-wide sys.path 제거
```

### Invariants

최소한 다음을 기록한다.

- `modules.json` hash 및 불변 계약
- Python 3.14 및 승인된 cu130 경로
- 기존 사용자 config 보존
- no commit/push
- no destructive Git
- P0/P1 경계

### Existing implementation to reuse

현재 main에 존재하는 파일과 재사용 전략을 적는다.

예:

```text
- core/profile_resolver.py: 새로 만들지 않고 config consumer 통합 여부만 검증
- config/modules.core.json: schema와 실제 선택 여부 검증
- plugins/NullInput/NullLLM/NullTTS/NullVtuber: 중복 provider 생성 금지
```

### Assumptions and open questions

각 가정에 확인 방법과 결정권자를 적는다.

| 항목 | 상태 | 확인 방법 | 결정 필요 |
| --- | --- | --- | --- |
| Core/CPU에서 Torch 자체가 필요한가 | ASSUMPTION | imports와 wheel matrix 조사 | 사용자/기술 결정 |

미해결 결정이 구현 방향을 바꾸면 먼저 사용자 승인을 요청한다.

### Milestones

각 milestone은 독립적으로 검증하고 되돌릴 수 있어야 한다.

필수 형식:

```markdown
### Milestone N — 이름

- objective:
- files:
- exact changes:
- acceptance:
- verification commands:
- rollback:
- stop conditions:
```

“전체를 수정한다” 같은 큰 milestone을 만들지 않는다. 한 milestone이 실패하면 다음 milestone을 시작하지 않는다.

### Verification plan

각 명령에 상태와 실행 환경을 표시한다.

```text
[CURRENT][Windows][Local]
[TARGET][Windows][CI]
[FUTURE][Packaging]
```

각 검증에 다음을 포함한다.

- command
- expected observation
- timeout
- 실패 의미
- 외부 자원 필요 여부
- 작업 전후 `modules.json` hash 확인 시점

Core smoke와 production configuration smoke를 분리한다.

### Progress

체크박스와 timestamp를 사용한다.

```markdown
- [x] 2026-07-16 14:30 KST — baseline 확인
- [ ] P0-A milestone 1 구현
- [ ] 검증 실행
```

완료 표시에는 실제 증거 위치 또는 명령 결과 요약을 함께 적는다.

### Surprises & Discoveries

계획과 달랐던 사실을 즉시 기록한다.

예:

```text
- requirements lock 생성 중 Python 3.14 wheel이 없는 package 발견.
  Evidence: <command/output summary>
  Impact: approved matrix 재결정 필요.
```

### Decision Log

중요한 선택을 시간순으로 남긴다.

```markdown
- 2026-07-16 — Core/CPU lock 도구로 X를 선택.
  - reason:
  - alternatives considered:
  - approved by:
  - consequences:
```

사용자 승인이 필요한 결정은 승인 전 구현하지 않는다.

### Risks and mitigations

기술 위험뿐 아니라 범위, rollback, 환경 차이도 포함한다.

| 위험 | 가능성 | 영향 | 완화 | 중단 조건 |
| --- | --- | --- | --- | --- |

### Rollback plan

- milestone별 되돌릴 파일
- 생성 파일 제거 방법
- 사용자 config와 `modules.json` 보존 확인
- destructive Git 없이 되돌리는 명령
- rollback 후 재검증 명령

`git reset --hard`와 `git clean`을 rollback으로 사용하지 않는다.

### Verification Results

계획이 아니라 실제 실행 결과를 기록한다.

| 명령 | 환경 | 결과 | 증거/요약 | 상태 |
| --- | --- | --- | --- | --- |

상태는 다음 중 하나다.

```text
PASS
FAIL
NOT RUN
BLOCKED
```

`NOT RUN`과 `BLOCKED`를 PASS로 표현하지 않는다.

### Outcomes & Retrospective

- observable outcome 달성 여부
- 변경 파일 요약
- 남은 위험
- P1/P2로 넘긴 항목
- rollback 가능 상태
- commit/push 미수행 확인 또는 별도 승인 결과
- 다음 단계 시작 전 필요한 사용자 결정

## 실행 절차

1. 적용 지침과 authoritative sources를 읽는다.
2. baseline을 read-only로 확인한다.
3. delta를 분류한다: already verified / present but unverified / missing / out of scope.
4. ExecPlan 초안을 작성한다.
5. 계획을 사용자에게 보고하고 승인 경계에서 멈춘다.
6. 승인된 milestone만 구현한다.
7. milestone마다 계획의 Progress, Discoveries, Decision Log를 갱신한다.
8. 검증을 실행하고 실제 결과를 Verification Results에 기록한다.
9. 실패하면 요구를 약화하지 말고 수정하거나 BLOCKED로 보고한다.
10. 단계 review 후 멈춘다.
11. commit/push는 별도 승인 메시지를 받은 뒤에만 수행한다.

## 완료 기준

ExecPlan은 다음을 모두 만족해야 완료다.

- baseline commit, branch, working tree, modules hash 기록
- scope/out-of-scope와 P0/P1 경계 명확
- 기존 구현 재사용 계획 명확
- milestone별 acceptance와 rollback 존재
- CURRENT/TARGET/FUTURE 명령 구분
- 실행한 명령의 실제 결과 기록
- 실패, skip, 미실행 항목 투명하게 표시
- 계획 변경과 중요한 결정 기록
- modules hash 전후 비교
- 남은 위험과 다음 승인 지점 명시
- commit/push 상태 명시
