<!-- 20260817_kpopmodder: Maintained the stable lifecycle index and added the shutdown/launch-ownership investigation document and snapshot boundary. -->
<!-- 20260818_kpopmodder: Added the clean-HEAD post-restore snapshot and separated historical dirty-tree evidence from current implementation. -->
<!-- 20260818_kpopmodder: Bound current-status claims to audited baseline 4239c23 and indexed the explicit approval, no-replay, and observation contracts. -->
<!-- 20260818_kpopmodder: Indexed evidence-complete gameplay E2E semantics without duplicating the authoritative observation schema. -->

# Fabric ChatClef Live Runtime Process Lifecycle Documentation Index

상태: 문서 전용 인덱스. 이 문서 자체는 프로세스 종료, LAVI/Minecraft 실행,
live mutating test, 코드 수정, 빌드, commit 또는 push를 승인하지 않는다.

기존의 단일 `Fabric ChatClef Live Runtime Process Lifecycle Plan`은 사건 복구와
테스트 설계를 한 파일에 함께 담고 있었다. 책임을 분리하기 위해 본문을 다음
문서로 나누고, 이 파일은 기존 경로를 보존하는 안내 문서로 유지한다.

## 문서 구성

1. [`fabric-chatclef-hidden-lavi-recovery-runbook.md`](./fabric-chatclef-hidden-lavi-recovery-runbook.md)
   - Windows에서 숨은 LAVI를 식별하고 수동으로 정리하는 운영 runbook
   - listener PID, ancestor chain, descendant tree와 외부 자식 프로세스 경계
   - graceful, external, forced termination 구분
   - LAVI와 Minecraft의 안전한 재실행 순서

2. [`fabric-chatclef-live-runtime-preflight-plan.md`](./fabric-chatclef-live-runtime-preflight-plan.md)
   - Fabric ChatClef live mutating test 전용 read-only preflight 설계
   - exact command/URL/backend/instance/world/one-shot approval tuple
   - intended LAVI listener ownership, bridge 상태, idle 상태 검증
   - 명시된 Minecraft instance의 `logs/latest.log` fallback 계약
   - deterministic skip/fail, submission uncertainty와 preflight/live-result 분리
   - offline fixture와 manual live validation 계획

3. [`chatclef-korean-test-strategy.md`](./chatclef-korean-test-strategy.md)
   - command submission과 terminal result의 상세 테스트 계약
   - submitted request ID와 동일한 terminal result만 lifecycle 종료 후보
   - terminal lifecycle, runtime completion, gameplay effect와 E2E 판정 분리
   - complete observation, expected/partial effect, prohibited-effect absence 판정
   - stale result, 자동 replay와 외부 automatic rerun 금지
   - `result_reason`은 이 문서에서 추측하지 않고 status별 계약이 확정된 뒤
     test strategy에서 관리

4. [`fabric-chatclef-lavi-shutdown-launch-ownership-investigation-plan.md`](./fabric-chatclef-lavi-shutdown-launch-ownership-investigation-plan.md)
   - 외부 launcher/console 소멸 후 생존한 LAVI runtime 사건의 증거 정리
   - graceful shutdown source path와 미확정 Windows ownership 경계
   - forced termination negative control, 재현 matrix와 판정 기준
   - shutdown/launch ownership과 production single-instance defect 분리

## 증거 표기

분리된 문서에서는 다음 표기를 사용한다.

- `[source]`: 해당 문서가 고정한 current working-tree source에서 직접 확인
- `[config]`: 현재 로컬 설정 또는 명시된 설정 파일에서 확인
- `[runtime snapshot]`: 특정 시각 Windows/HTTP/파일에서 직접 관찰
- `[official-platform behavior]`: 공식 Windows/Python/Gradio 자료의 일반 동작
- `[inference]`: 여러 증거를 결합한 판단이며 자동 종료 근거로 사용할 수 없음
- `[unknown]`: 현재 증거로 확인할 수 없음
- `[proposed]`: 아직 구현 또는 반복 검증되지 않은 문서상 정책

`[runtime snapshot]`의 PID, port와 생성 시각은 과거 증거다. 복구 작업에서는
반드시 현재 상태를 다시 조회하며, 문서에 적힌 PID를 종료 대상으로 재사용하지
않는다.

## 문서별 조사 snapshot

### 2026-08-16 recovery/preflight 조사

```text
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 6d75c6e8c68dc36d60d00ee70768f9104a0fc7bc
working tree: dirty
기존 combined lifecycle plan: untracked
```

### 2026-08-17 shutdown/launch-ownership 조사

```text
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 4a87e9018a9e2743764ec5d4215eb8923a401da4
working tree: dirty
```

### 2026-08-18 원복 후 audited implementation baseline 재검토

```text
branch: minecraft-plugin-fix/alto-clef-infinite-loop
audited implementation baseline: 4239c23
working tree at audit start: clean
```

Audited baseline 4239c23에는 기본 opt-in live lifecycle test와 submitted
request-ID matching이 남아 있다. 2026-08-16~17 dirty working tree에만 있었다고
문서화된
backend/instance/world, listener/process identity, same-PID, hidden-LAVI,
`latest.log` 및 command 직전 TOCTOU preflight 구현은 audited baseline에 없다.

이 baseline은 고정 source snapshot이다. docs-only commit으로 `HEAD`가 바뀌어도
구현 상태가 자동 갱신되지 않으며, 이후 source/test commit을 현재 상태로 설명하려면
다시 read-only audit해야 한다.

각 snapshot은 해당 시점의 line reference와 관찰을 해석하기 위한 경계다. 업로드 ZIP,
다른 날짜의 working tree 또는 현재 local repository와 동일하다는 증명이 아니며,
한 snapshot의 구현 상태를 다른 snapshot에 자동으로 이월하지 않는다.

## 구현 상태 요약

다음 표는 `2026-08-17` 문서 갱신 시점에 Codex가 기록한 current dirty working-tree
상태 요약이다. 이 인덱스만으로 실제 코드 구현이나 test 통과를 독립 검증하지 않으며,
세부 근거는 각 책임 문서와 repository diff를 따른다.

| 기능 | 상태 | 비고 |
| --- | --- | --- |
| live/mutating 명시적 opt-in | implemented in current dirty working tree | HEAD 포함 여부와 별개 |
| submitted request ID matching | implemented in current dirty working tree | stale result 차단 포함 |
| backend/instance/world preflight | implemented in current dirty working tree | test-only runner 기준; HEAD 포함 여부와 별개 |
| 지정된 `latest.log` fallback | implemented in current dirty working tree | test-only shared snapshot과 strict UTF-8/CP949 판정 |
| Windows listener/process identity preflight | implemented in current dirty working tree | read-only probe만 구현; 자동 종료 기능은 범위 밖이며 수동 종료는 recovery runbook에서 별도 승인 필요 |
| bridge `connected`/`lifecycle_state` 직접 gate | implemented in current dirty working tree | command 직전 idle과 함께 재확인 |
| replacement decode 자동 통과 금지 | implemented in current dirty working tree | 두 strict decoder가 모두 실패하면 fail closed |
| status별 `result_reason` 계약 | unverified | strategy에서 별도 확정 필요 |
| LAVI shutdown/launch ownership | source/log audit documented; reproduction planned | shutdown source path와 사건 evidence는 정리됐으나 root cause는 미검증 |
| production single-instance guard | out of scope | 별도 설계와 승인 필요 |

`implemented in current dirty working tree`는 HEAD baseline 또는 검증 완료를 뜻하지
않는다. 현재 수정된 working-tree implementation이 존재한다는 의미뿐이다.

## 2026-08-18 원복 후 audited baseline 구현 상태

이 표는 위 역사적 dirty-tree 표를 현재 repository 상태로 해석할 때 우선한다.

| 기능 | audited baseline 4239c23 상태 | 비고 |
| --- | --- | --- |
| live/mutating 명시적 opt-in | implemented | 현재 runtime lifecycle test에 존재 |
| submitted request ID matching | implemented | `6d75c6e`부터 stale result 차단 포함 |
| Gradio submit one-shot | implemented | test body submit API 1회; lower adapter `command_request` 횟수 증거는 별도 필요 |
| matching terminal lifecycle 대기 | implemented | submitted ID 일치, terminal status, same-snapshot active clear를 요구 |
| conditional `result_reason` 검사 | implemented | `data`가 dict이고 key가 있을 때 nonblank; status별 requiredness는 미확정 |
| runtime-reported completion 검증 | not implemented | matching `status == completed`를 필수로 요구하지 않음 |
| gameplay/end-to-end effect 검증 | not implemented | complete observation, expected/partial effect와 verified prohibited-effect absence를 확인하지 않음 |
| explicit mutating `LAVI_GRADIO_URL` gate | absent | audited baseline은 URL 미설정 시 default endpoint 사용 |
| exact approval tuple | absent | command/URL/backend/instance/world/one-shot 승인 근거 필요 |
| backend/instance/world preflight | absent | historical dirty-tree implementation을 복구·재검증해야 함 |
| 지정된 `latest.log` fallback | absent | strict UTF-8/CP949 및 stable snapshot 계약만 문서에 남음 |
| Windows listener/process identity preflight | absent | selected Gradio와 `4316` same-PID gate 포함 |
| hidden second LAVI 차단 | absent | effective fallback range identity 검증 필요 |
| command 직전 connected/lifecycle/idle/ownership 재확인 | absent | current initial idle assert만으로 대체할 수 없음 |
| submission outcome unknown reconciliation | absent | 응답 유실 가능 시 자동 재제출 금지와 다음 run 차단 필요 |
| external automatic rerun 차단 | absent | IDE/CI/flaky/Codex wrapper rerun 0회 계약 필요 |
| status별 `result_reason` 계약 | unverified | strategy에서 별도 확정 필요 |

Audited baseline 4239c23 test는 `one-shot lifecycle correlation live test`로
분류한다. 완전한
fail-closed preflight가 복구되기 전에는 무인 mutating E2E runner로 분류하거나
실행하지 않는다.

실제 mutating run의 exact approval tuple, explicit loopback URL, opt-in 뒤
deterministic `fail`, `submission_outcome_unknown`, observer timeout, 자동 rerun 금지,
`PreflightDecision`/`LiveRunObservation` 분리는
[`fabric-chatclef-live-runtime-preflight-plan.md`](./fabric-chatclef-live-runtime-preflight-plan.md)가
소유한다. terminal lifecycle observed, runtime-reported completion, gameplay effect
observed와 end-to-end success의 판정 및 command별 oracle은
[`chatclef-korean-test-strategy.md`](./chatclef-korean-test-strategy.md)가 소유한다.
특히 strategy는 `gameplay_observation_complete`, expected-effect verification,
partial-effect observation과 prohibited-effect absence verification의 의미를 소유하며,
이 인덱스는 해당 필드 스키마를 중복 정의하지 않는다.

## 공통 안전 불변조건

1. 테스트 코드가 LAVI, Minecraft 또는 외부 자식 프로세스를 자동 종료하지 않는다.
2. `taskkill /IM python.exe`, `taskkill /IM java.exe`,
   `taskkill /IM javaw.exe`를 사용하지 않는다.
3. PID 번호나 `python.exe`라는 이름 하나만으로 LAVI를 판정하지 않는다.
4. `/T`, image-name kill 또는 ancestor/descendant 일괄 kill을 사용하지 않는다.
5. Minecraft Java 프로세스는 LAVI 포트 복구만을 이유로 종료하지 않는다.
6. 실제 live runtime은 운영자가 볼 수 있는 전경 콘솔에서 한 번만 실행한다.
7. `4316` 충돌 상태에서는 추가 LAVI를 계속 실행하지 않는다.
8. production DTO, Java payload, WebSocket protocol에 test-only log fallback을
   추가하지 않는다.
9. 모든 preflight가 통과하기 전에 mutating `command_request`를 전송하지 않는다.
10. 확인할 수 없는 identity 또는 process ownership은 fail closed한다.
11. shutdown fixture, exact-PID 종료 재현과 실제 LAVI/Minecraft integration은 단계별
    사용자 승인 없이 실행하지 않는다.
12. matching terminal 관찰을 실제 command 성공 또는 gameplay effect 성공으로
    표현하지 않는다.
13. observer timeout, disconnect 또는 accepted response 유실 가능성 뒤 원래 command를
    자동 retry/replay하거나 자동 `@stop`/cancel하지 않는다.
14. `active_request_id == null`만으로 Minecraft Task 종료나 partial/unexpected effect
    부재를 주장하지 않는다.
15. 관찰 범위가 불완전하면 prohibited effect를 보지 못했다는 사실을 verified
    absence 또는 end-to-end success로 승격하지 않는다.
15. mutating test를 IDE, CI, flaky-test plugin 또는 Codex wrapper가 자동 rerun하지
    않는다.

## 범위 밖 변경

다음 변경은 이 문서 분리로 승인되지 않는다.

- `main.py` 또는 `AppComposer`에 process-wide mutex 추가
- `4316` bind 실패를 전체 LAVI fatal error로 변경
- Gradio 자동 포트 증가 정책 변경
- `run.bat`에 자동 process kill 추가
- production runtime에서 외부 Minecraft 로그 읽기
- Fabric/Forge backend 경계 통합
- status별 `result_reason`을 증거 없이 필수 또는 선택으로 확정
- Windows console handler, shutdown diagnostics 또는 behavioral fix 추가
- evidence-only 계획만으로 fixture/LAVI/Minecraft 재현 실행
