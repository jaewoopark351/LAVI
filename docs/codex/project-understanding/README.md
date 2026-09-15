<!-- #20260915_kpopmodder: Group source-backed project understanding documents without changing code or root instructions. -->

# 프로젝트 이해 · 계정/세션 인수인계

LAVI의 구조와 개발 맥락을 다시 파악하기 위한 문서 폴더다.
소스 구조, 확인된 검증 결과와 후속 작업 범위를 남긴 기록이다.

## 문서 목록

| 문서 | 기준 소스 | 담긴 내용 |
| --- | --- | --- |
| [2026-09-15 프로젝트 이해](2026-09-15.md) | `b3a6c9d2` | 앱 시작·설정, 입력/LLM/기억/TTS, 플러그인·게임별 경계, 테스트/CI, 최근 검증·남은 작업 |
| [2026-09-16 자동 보관 검증 인수인계](minecraft-auto-deposit/2026-09-16-verification-handoff.md) | 문서 `4c1e8988` / 빌드 `eaf71458` + 당시 변경 | 패치·빌드 근거, 실게임 미검증 범위, 가까운 상자의 자동 보관 1회(A) 우선, 재발동 정책(D) 분리 |
| [2026-09-16 자동 보관 실행 근거](minecraft-auto-deposit/2026-09-16-runtime-evidence.md) | `4c1e8988` / 기존 실행 PID `46980` | 실제 로그 수집, 클라이언트 전송 124개와 카운터 0·비정상 종료, cleanup, D 소스 확인, 통제된 A 준비 |
| [2026-09-16 슬롯 이벤트 연결 수정](../../../plugins/Minecraft/docs/slot-click-counter-fix-20260916/implementation.md) | `4c1e8988` + 이번 수정 | 1.20.1 hook/native 호출 수정, 책임 분리, 이벤트·카운터 회귀와 실제 Mixin 적용 검증, 빌드·실게임 상태 |
| [2026-09-16 아이템 분류 변경·구현 기록](minecraft-auto-deposit/2026-09-16-item-classification-requirements.md) | `4c1e8988` + 변경 / 정책 revision 2 | 돌 5종 일반 보관, 사탕수수 유지, 청금석 귀중품·신뢰 상자 전용 적용. **291/291 검사·1.20.1 clean 빌드·JAR 정책 일치 확인**, 배포·실게임 미검증 |

## 읽는 순서

1. 현재 [AGENTS.md](../../../AGENTS.md)와 이번 사용자 요청.
2. 위 이해 문서의 기준 HEAD·범위와 현재 Git 상태 대조.
3. 해당 영역의 코드 진입점·호출자·기능 계약·테스트.
4. 과거 검증과 이번에 확인한 사실을 구분한 뒤 현재 요청 수행.

이 폴더는 개발 인수인계 기록이며 `memory_core`의 앱 기억과 별개다.
저장돼 있다는 사실만으로 새 계정·세션에서 자동으로 읽힌 것은 아니다.
과거 문서에 적힌 작업·승인을 지금의 실행 요청으로 취급하지 않는다.

## 다음 대화 시작 문장

> docs/codex/project-understanding/README.md와 이번 작업에 해당하는 날짜별 인수인계를 읽고 현재 Git 상태와 대조해줘. 이번 작업은 [원하는 작업]이야.

## 기록을 더 남길 때

새 조사·인수인계는 날짜를 붙인 새 문서로 추가하고, 사용자 요청 범위에서 이 목록을 갱신한다.
기준 HEAD, 확인한 범위, 결정 이유, 실제 검증, 미실행/미확인, 남은 작업을 적는다.
비밀 설정값이나 원문 대화는 넣지 않는다.

[기존 7월 P0/P1 handoff pack](../handoff/README.md)은 별도 참고 자료로 유지한다.
