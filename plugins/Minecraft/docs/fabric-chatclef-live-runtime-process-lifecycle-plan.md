<!-- 20260816_kpopmodder: Split the combined Fabric ChatClef live-runtime lifecycle plan into a recovery runbook, a preflight plan, and this stable index. -->

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
   - intended LAVI listener ownership, bridge 상태, idle 상태 검증
   - 명시된 Minecraft instance의 `logs/latest.log` fallback 계약
   - offline fixture와 manual live validation 계획

3. [`chatclef-korean-test-strategy.md`](./chatclef-korean-test-strategy.md)
   - command submission과 terminal result의 상세 테스트 계약
   - submitted request ID와 동일한 terminal result만 성공 후보
   - stale result 및 자동 replay 금지
   - `result_reason`은 이 문서에서 추측하지 않고 status별 계약이 확정된 뒤
     test strategy에서 관리

## 증거 표기

분리된 문서에서는 다음 표기를 사용한다.

- `[source]`: 현재 repository 소스에서 직접 확인
- `[config]`: 현재 로컬 설정 또는 명시된 설정 파일에서 확인
- `[runtime snapshot]`: 특정 시각 Windows/HTTP/파일에서 직접 관찰
- `[inference]`: 여러 증거를 결합한 판단이며 자동 종료 근거로 사용할 수 없음
- `[unknown]`: 현재 증거로 확인할 수 없음
- `[proposed]`: 아직 구현 또는 반복 검증되지 않은 문서상 정책

`[runtime snapshot]`의 PID, port와 생성 시각은 과거 증거다. 복구 작업에서는
반드시 현재 상태를 다시 조회하며, 문서에 적힌 PID를 종료 대상으로 재사용하지
않는다.

## 조사 기준 snapshot

다음 값은 2026-08-16 evidence-only 조사 당시 repository 상태다.

```text
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 6d75c6e8c68dc36d60d00ee70768f9104a0fc7bc
working tree: dirty
기존 combined lifecycle plan: untracked
```

이 snapshot은 업로드 ZIP과 현재 local repository가 동일하다는 증명이 아니며,
이후 변경된 working tree의 source of truth로 사용할 수 없다.

## 구현 상태 요약

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
| production single-instance guard | out of scope | 별도 설계와 승인 필요 |

`implemented in current dirty working tree`는 HEAD baseline 또는 검증 완료를 뜻하지
않는다. 현재 수정된 working-tree implementation이 존재한다는 의미뿐이다.

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

## 범위 밖 변경

다음 변경은 이 문서 분리로 승인되지 않는다.

- `main.py` 또는 `AppComposer`에 process-wide mutex 추가
- `4316` bind 실패를 전체 LAVI fatal error로 변경
- Gradio 자동 포트 증가 정책 변경
- `run.bat`에 자동 process kill 추가
- production runtime에서 외부 Minecraft 로그 읽기
- Fabric/Forge backend 경계 통합
- status별 `result_reason`을 증거 없이 필수 또는 선택으로 확정
