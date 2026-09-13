<!-- 20260913_kpopmodder: Recorded Korean Chat/final-microphone XYZ routing without changing the working GOTO movement implementation. -->
<!-- 20260913_kpopmodder: Reviewed raw-input and rejection ownership, actual route order, and Prepared-versus-legacy result scope. -->
<!-- 20260913_kpopmodder: Narrowed the requested implementation to Python Korean input using existing goto; excluded go aliases and Java result changes. -->
<!-- 20260913_kpopmodder: Reconciled direction review with existing routing, live-proof lifecycle, guarded rejection, and original-XYZ binding. -->
<!-- 20260913_kpopmodder: Clarified whitespace and delimiter acceptance without weakening existing command-safety checks. -->
<!-- 20260913_kpopmodder: Implemented the fresh Python-only input unit and recorded offline versus live verification separately. -->
<!-- #20260913_kpopmodder: Append one user-run Chat arrival observation and link the separate result-response contract without changing the original input implementation scope. -->

# 한국어 좌표 GOTO: 마이크·Chat 입력 연결 계약

Date: 2026-09-13

## 1. 요구사항과 현재 상태

**LAVI Chat에 한국어로 입력해도, 마이크로 한국어를 말해도 같은 좌표 이동이 실행되어야 한다.**
사용자가 `@goto`를 직접 입력해야만 동작하는 것으로 이 요구사항을 충족했다고 보지 않는다.
예를 들어 `500, 90, -928 좌표로 가줘`를 두 입력 경로 모두에서 해석하고,
기존 GOTO에 한 번 전달하는 것이 목표다. 마이크는 최종 음성 인식 결과만 실행 후보로 삼는다.

이 문서는 입력 연결의 구현 전 공백과 구현·검증 계약을 기록한다.
기존 `goto` 명령 자체가 미구현이라는 뜻이 아니며, 문서 작성으로 기능이 구현되지는 않는다.
이후 사용자의 별도 구현 요청으로 Python 입력 연결을 작성했다. **구현 시점의 결과는 9절**이며,
후속 사용자 실행의 Chat 1건과 도착 화면·종료 응답 관측은 **10절**에 추가했다.
실제 마이크 음성 인식과 전체 문법·이동 경로의 live acceptance는 여전히 미검증이다.

당시 사용자가 승인한 **Python 입력 구현 대상은 한국어 입력 연결뿐**이었다.
`@go`는 LAVI Chat와 Minecraft 게임 채팅 어느 쪽에도 추가하지 않는다. 직접 명령은 기존
`@goto`를 사용한다. Java의 이동·재료 확보·결과 처리와 기존 JAR은 그대로 두며,
정확한 도착·실패 결과를 새로 연결하는 작업도 제외한다. 이전 초안의 별칭·Java 결과 연결
계획과 그에 따른 빌드·검증 요구는 이 결정으로 폐기한다.

구현 전 문서 검토에서는 사용자가 전달한 방향성 검토 의견을 당시 소스의 주요 호출 경계와 대조해
반영했다. 목표는 **입력 구현 진행을 전제로 최소 연결 방법을 구체화**하는 것이며, 구현 여부를
다시 결정하거나 과거 navigation 사고를 이유로 전체 입력 구현을 무기한 보류하는 검토가 아니다.
실제 결함·안전 규칙 충돌은 정확한 파일·경계와 최소 해결안으로 보고하고, 범위 밖 개선과
자료가 부족한 개별 판단을 분리한다. 이 원칙은 AGENTS의 적용 규칙을 무시하거나 필요한
근거를 생략하라는 뜻이 아니다. 당시 문서화 요청에서는 구현하지 않았고, 아래 소스 변경은
그 이후의 별도 구현 요청으로 수행했다. 기존 navigation 원인을 새로 입증한 것은 아니다.

다음 상태 블록은 9절의 초기 구현 완료 시점 기록이다. 후속 사용자 실행 증거는 10절에서
별도로 갱신하며, 당시 build·배포·runtime 미실행 기록을 소급해 바꾸지 않는다.

```text
DOCUMENT_TYPE: FOCUSED_INPUT_CONTRACT_WITH_IMPLEMENTATION_EVIDENCE
REVIEW_DATE: 2026-09-13
REVIEWED_HEAD: 99af65f2cb0609de31fae24d2f1704aa6853f659
REVIEWED_BRANCH: minecraft-plugin-fix/alto-clef-infinite-loop
REVIEWED_WORKTREE_AT_START: CLEAN
BACKEND_SCOPE: FABRIC_CHATCLEF_1_20_1_ONLY
IMPLEMENTATION_TARGET: PYTHON_KOREAN_INPUT_ONLY
CANONICAL_COMMAND: goto
KOREAN_CHAT_AND_FINAL_MIC_REQUIREMENT: REQUIRED
EXISTING_GOTO_COMMAND_AND_COMPILER: SOURCE_PRESENT
IMPLEMENTATION_DIRECTION_REVIEW: STATIC_SOURCE_RECONCILED_NOT_RUNTIME_VERIFIED
REQUESTED_INPUT_EXPANSION_STATUS: IMPLEMENTED_VERIFIED_OFFLINE_LIVE_NOT_RUN
GO_ALIAS_IMPLEMENTATION_SCOPE: NONE_USER_EXCLUDED
JAVA_SOURCE_TEST_BUILD_PACKAGING_SCOPE: NONE
NEW_TERMINAL_RESULT_PROJECTION_SCOPE: NONE
EXACT_USER_INPUT_RUNTIME_FAILURE_BOUNDARY: UNKNOWN
END_TO_END_KOREAN_CHAT_ACCEPTANCE: OFFLINE_TRUSTED_INGRESS_PASSED_LIVE_NOT_RUN
END_TO_END_FINAL_MIC_ACCEPTANCE: OFFLINE_FINAL_TRANSCRIPT_PASSED_ACTUAL_STT_LIVE_NOT_RUN
PRODUCTION_TEST_CONFIGURATION_CHANGE: PYTHON_INPUT_AND_TESTS_ONLY_NO_CONFIGURATION_CHANGE
SOURCE_CHANGE_SCOPE: FRESH_PYTHON_INPUT_ONLY_NAVIGATION_UNCHANGED
IMPLEMENTATION_WORKTREE_AT_START: THREE_EXISTING_DOCUMENT_CHANGES
PYTHON_TESTS: SEE_SECTION_9
JAVA_TESTS_AND_BUILD: NOT_RUN
DEPLOYMENT_AND_MINECRAFT_RUNTIME: NOT_RUN
COMMIT_AND_PUSH: NOT_RUN
```

위 `REVIEWED_WORKTREE_AT_START: CLEAN`은 최초 문서 작성 직전의 소스 검토 snapshot이다.
초기 입력 구현 전 문서 검수 시작 시에는 이 문서·기존 한국어 계약·README의 문서 변경 3건이 이미 있었다.
입력 구현 시작 시에도 HEAD는 같았다. 해당 Python 소스·오프라인 테스트 결과와 과거
navigation/런타임 증거는 별개이며, 오프라인 결과만으로 실제 마이크·게임 검증 상태를 승격하지 않는다.
이후 사용자 Chat 실행 한 건의 제한된 증거는 10절을 따른다.

### 관련 계약과 우선 범위

- [AGENTS.md](../../../AGENTS.md)의 현행 안전 규칙·GOTO gate를 변경하지 않는다.
- [기존 한국어 GOTO/FIND/전체 명령 계약](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md)의
  과거 HEAD·mixed-worktree·실패 로그 기록은 그대로 보존한다. 현재 요청의 입력 문법,
  두 필수 입력 경로, Python 전용 범위와 현행 소스 상태는 이 문서에서 구체화한다.
  `@go`, FIND 및 전체 명령 공개 확대는 포함하지 않는다.
- 이동 동작은 [현행 V3.1 frozen-target handoff 계약](chatclef-goto-upward-material-preflight-contract-2026-09-12.md)의
  `Current contract`를 따른다. 과거 admission Y 차이 기반 선채굴 규칙을 복원하지 않는다.
- [GOTO 사고 기록](chatclef-goto-diagnostics-before-behavior-incident-2026-09-11.md)의 역사적
  navigation 원인과 이번 입력 해석 문제를 같은 사건으로 취급하지 않는다.
- [backend 분리](minecraft-backend-separation.md)와
  [upstream 보존·Task 소유권](chatclef-carryon-integration-direction.md)을 유지한다.

## 2. 구현 전 소스에서 확인한 연결과 공백

아래는 위 HEAD에서 읽기 전용으로 확인한 사실이다. 사용자 원문 이벤트와 동일한
request/correlation의 런타임 추적을 이 문서 작업에서 수행하지 않았으므로,
개별 문장 실패의 실제 최초 경계를 런타임 확정으로 표기하지 않는다.
이 절의 정규식·gate 공백은 구현 전 snapshot이며, 변경 결과는 9절을 따른다.

| 경계 | 소스에서 확인한 상태 | 필요한 처리 |
| --- | --- | --- |
| 한국어 명령 registry | `goto`는 이미 public / parser-ready / Python-admission-ready / bridge-ready | allowlist 전체를 다시 열지 않는다. 메타데이터가 런타임 성공 증거는 아니다. |
| Chat·마이크 신뢰성 | 정확한 Chat 제출 / VoiceInput 최종 이벤트와 소비된 ingress 증거, Hangul을 검사 | 기존 원본 이벤트·final·단일 소비·신뢰 검증을 보존한다. |
| public/source admission과 feedback | 출처 허용 목록은 live proof 검증이 아니며, ordinary는 feedback grant가 없다는 사실만으로 transport 호출을 차단하지 않음 | 자동 Chat/최종 마이크 GOTO 경계에서 기존 live-proof 검증을 명시적으로 재사용한다. 실제 무권한 실행의 런타임 증거로 해석하지 않는다. |
| 입력 후보 판별 | `좌표`, `이동`은 있지만 좌표 뒤의 `가줘`만으로는 trigger가 되지 않음 | 좌표와 이동 의도를 함께 판별하는 전용 규칙을 연결한다. |
| 한국어 좌표 파서 | 공백으로 구분된 signed 숫자 3개 + 선택적 조사 + 이동 동사 접두사를 `search`로 찾음 | `좌표로`, 축 이름, 제한된 음성 부호 표현과 전체 발화 검증을 처리한다. |
| 공통 정규화 | 쉼표뿐 아니라 마침표 등도 공백으로 바꿈 | 좌표 원문에서 소수·부호·구분자를 먼저 검증해 손실된 구두점으로 잘못 실행하지 않는다. |
| 명령 변환 | typed GOTO의 x/y/z를 Java int로 검증해 `goto x y z` 생성 | 기존 compiler와 제출 경로를 재사용한다. |
| 번역 결과 일치 | 기존 `ChatClefTranslationResultValidator`가 intent 재컴파일 결과와 실제 command를 비교 | 기존 검사를 복제하지 않고, 최초 원문에서 검증한 불변 XYZ와 최종 intent·command의 연결만 보완한다. |
| Java 이동 입구 | `GotoCommand.call()`이 1.20.1에서 기존 Task를 `PreparedGotoTask.forCommand()`에 전달 | 이 입구를 보존하고 Python에서 별도 이동 Task나 채굴 명령을 만들지 않는다. |
| 완료 응답 | 일반 Task 자연 종료는 completed 경로로 갈 수 있고, 한국어 GOTO 응답은 도착 미확인 문구 | 기존 분류·표현을 보존한다. 실제 도착 증거와 실패 이유의 새 연결은 이번 범위 밖이다. |

구체적인 차이:

- `500, 90, -928 좌표로 가줘`: `좌표`로 입력 후보가 될 수 있으나 현재 GOTO 정규식에는
  숫자 뒤 `좌표로`를 소비하는 문법이 없다.
- `500 90 -928로 가줘`: 파서 자체는 해석할 수 있지만, 다른 trigger가 없으면 앞단에서 제외될 수 있다.
- `500.5 90 -928로 이동해`: 현재 정규화가 `500 5 90 -928로 이동해`로 바꾸면 부분 검색이
  원문과 다른 `(5, 90, -928)`을 선택할 수 있다. 정적 소스상 가능한 경로이며 실제 발생 기록은 아니다.
- `@go`는 구현 대상에서 제외한다. 이를 지원하기 위한 ASCII 입력 경로,
  Hangul·신뢰 검사 예외 또는 source 허용 확대를 추가하지 않는다.
- 과거에 특정 한국어 입력의 번역이 성공했거나 직접 `@goto` 이동이 성공했다는 사실만으로
  현재 Chat·마이크의 모든 필수 문장이 지원된다고 판단하지 않는다.

## 3. 한국어 문법과 입력 소유권

### 3.1 필수 문장

다음 표의 한국어 문장은 **Chat와 최종 마이크 전사 양쪽에서** 같은 typed GOTO가 되어야 한다.
STT 모델의 정확도 보장이 아니라, 실제 도착한 최종 전사 문장의 처리 계약이다.

| 입력 예시 | 좌표 |
| --- | --- |
| `500 90 -928로 가줘` | `(500, 90, -928)` |
| `500,90,-928으로 가줘` | `(500, 90, -928)` |
| `500, 90, -928 좌표로 가줘` | `(500, 90, -928)` |
| `좌표 500, 90, -928로 이동해줘` | `(500, 90, -928)` |
| `x 500 y 90 z -928로 가줘` | `(500, 90, -928)` |
| `x=500, y=90, z=-928 좌표로 가줘` | `(500, 90, -928)` |
| `엑스 500 와이 90 제트 마이너스 928 좌표로 가줘` | `(500, 90, -928)` |
| `500 90 마이너스 928로 가줘` | `(500, 90, -928)` |
| `(500, 90, -928)으로 이동해` | `(500, 90, -928)` |

초기 문법은 절대 정수 XYZ이며 현재 차원을 사용한다. 축 표기는 X/Y/Z 순서로 각각 한 번만
허용하고, alias는 `x/엑스`, `y/와이`, `z/제트`로 제한한다. 숫자는 10진 숫자와 선택적
`+/-`, 또는 바로 앞의 `플러스/마이너스`를 허용한다. 두 부호를 겹쳐 쓰지 않는다.
마무리는 `가`, `가줘`, `가자`, `가주세요`, `이동`, `이동해`, `이동해줘`, `이동해주세요`처럼
명시한 이동 표현으로 제한한다. 공백·괄호·문장 끝 구두점 차이를 허용하되 의미를 버리지 않는다.
여기서 공백 차이는 기존 안전 검사와 양립하는 한 줄의 일반 공백 차이다. Chat/최종 마이크에서는
`LaviInputEvent.text`의 제어문자를 `.strip()`이나 공백 정규화 전에 검사한다. 탭·줄바꿈·제어문자를
삭제하거나 공백으로 치환해 실행 가능한 입력으로 만들지 않는다. 기존 공통 위험 문자 검사는
그대로 유지하며, 제어문자가 포함된 자동입력은 제출 0회다. 괄호는 예시처럼 XYZ 묶음을 감싸는
짝을 검증하며, 짝이 맞지 않는 괄호를 임의로 제거하거나 보정해 부분 좌표를 실행하지 않는다.

Chat/최종 마이크 자동실행 경계의 검증 원문은 신뢰된 `LaviInputEvent.text`다.
공유 좌표 파서는 원문 문자열을 해석하는 순수한 함수/객체이며, 이벤트의 신뢰 증거를 발급하거나
검증하는 책임을 맡지 않는다. parse 성공 자체도 실행 허가가 아니다. 기존 `direct_typed`와
`lavi_gui_korean` 진입점의 원문 전달·source/admission은 유지하고, 이들 경로에 Chat/마이크용
proof를 새로 요구하거나 위조하지 않는다. 기존 경로의 허용 범위를 확대하지도 않는다.
현재 rule parser의 `original` 변수와
그 값에서 만든 `intent.original_text`는 이미 구두점이 정규화됐으므로 원문 대신 쓰지 않는다.
원본 이벤트는 보존하고 별도 값에서 좌표 전용 정규화·검증을 수행한다. 숫자 사이 마침표,
부호, 인용·질문 표지를 검증하기 전에 제거하지 않는다. 허용된 이동 표현 전체를 소비해야 하며
`가` 접두사만 맞는다는 이유로 `가면`, `가지 마`를 명령으로 받지 않는다.
문장 끝 구두점 허용이 질문·인용의 실행 허용을 뜻하지 않는다.
기존 아이템 명령 등 다른 기능의 공통 정규화 동작을 함께 바꾸지 않는다.

`오백 구십 마이너스 구백이십팔` 같은 전부 한글인 수사 표현은 초기 보장 범위 밖이다.
전사가 이 형태이거나 부호가 불명확하면 한국어로 좌표를 다시 확인한다. STT가 언제나 숫자로
변환한다고 가정하지 않으며, 지원하지 않는 형태를 LLM이 추측해 실행하게 하지 않는다.

### 3.2 실행하지 않아야 하는 입력

- 좌표 누락, 4번째 숫자, 축 중복·잘못된 순서, 경쟁하는 두 좌표 묶음.
- 소수, NaN/무한대, Java signed int 범위 밖, 불명확한 천 단위 쉼표나 부호.
- `~`/`^` 상대·로컬 좌표, 명시적 다른 차원 요청. 기존 raw `@goto` 문법은 별개로 보존한다.
- `500 90 -928로 가지 마`, 질문·인용·가정·설명 속 좌표, 이동 뒤의 두 번째 행동 명령.
- 예: `500 90 -928로 가면 어떻게 돼?`, `"500 90 -928로 가줘"라고 말했어`,
  `500 90 -928로 가고 좀비 공격해`를 부분 문자열만 추출해 실행하지 않는다.
- 임시 음성 인식 결과, 자동입력에 필요한 유효한 신뢰 증거가 없는 입력, 다른 이벤트·owner의
  증거, 닫힌 proof 또는 같은 입력의 중복 재사용, 출처 문자열만 위조한 입력.

좌표 이동 요청으로 판별된 불완전·불명확 입력은 한 번의 한국어 확인/거절과 **제출 0회**로 끝낸다.
일반 대화·인용·질문·부정문은 Minecraft 실행 소유권을 획득하지 않고 바깥의 일반 대화로
돌린다. 기존 STOP/STATUS가 소유한 입력은 해당 제어 응답으로 끝낸다.
`가줘`라는 단어만으로 일상 대화를 가로채지 않는다.
좌표 후보 판별과 상세 해석은 동일한 문법 정의에 기반한다. 의미 결과 세 가지에 앞서
`GOTO 후보 아님`을 구별해, 다음 네 가지 라우팅 의미를 보존한다. 이름은 구현 시 확정하되
`None`이나 일반 `UNKNOWN` 하나로 서로 다른 처리 방향을 합치지 않는다.

| 판별 결과 | 예시 | 처리 |
| --- | --- | --- |
| GOTO 후보 아님 | 기존 아이템 명령, 좌표 이동과 무관한 일상 대화 | 기존 gate·ordinary 등 다른 입력 처리를 유지 |
| 명령 아님 | 좌표가 포함된 질문·인용·부정·가정 | Minecraft 라우터 자체를 `not_handled`로 종료하고 바깥 일반 대화로 반환 |
| 확인 필요 | 누락·소수·모호한 부호·지원하지 않는 한글 수사·복합 실행 요청 | 처리 완료된 확인/거절 응답 한 번, 제출 0회 |
| 유효한 XYZ | `500, 90, -928 좌표로 가줘` | 같은 원문·불변 XYZ를 보존해 기존 신뢰·번역·제출 경로로 진행 |

정상 문법만 후보로 받아서는 안 된다. 좌표·축·숫자열/수사열 단서와 이동 맥락이 있는
불완전·질문·부정 후보도 상세 검사를 거쳐야 한다. 반대로 `가`/`가줘`를 독립된 광역 trigger로
추가해 `집에 가줘` 같은 일상 문장을 좌표 명령으로 잡지 않는다. 후보 판별은 XYZ 확정,
proof 검사나 제출을 하지 않으며, 공유 문법의 단서를 사용하되 파서와 별개의 실행 문법을 만들지 않는다.

여기서 일반 대화로 반환한다는 것은 Minecraft ordinary translator, 기존 GOTO 정규식 또는
optional Minecraft LLM intent extractor에 다시 해석을 맡긴다는 뜻이 아니다. 현재
`search`는 `좌표 500 90 -928로 가면 어떻게 돼?`의 일부도 GOTO로 잡을 수 있고,
`UNKNOWN`은 optional extractor가 주입된 구성에서 재해석될 수 있다. 이는 소스상 가능한
경로이지, 해당 optional extractor가 이번 사용자 실행에 사용됐다는 런타임 증거가 아니다.
GOTO로 판별·검증한 입력의 거절/일반 대화 결정은 이 재해석 경로에서도 보존해야 한다.
`확인 필요`는 처리 완료된 typed rejection과 제출 0회로 종결하며, optional extractor의
주입 여부에 따라 실행 여부가 바뀌면 안 된다. 확인 응답으로 pending 명령이나 자동 승인
상태를 새로 만들지 않고, 보완 입력도 같은 원문·신뢰·문법 검증을 거친다.

## 4. 기존 GOTO 실행 연결과 보존 범위

### 4.1 마이크·Chat 공통 필수 경로

```text
trusted Chat submit / trusted final VoiceInput
  -> 기존 ingress 증거 단일 소비와 한국어 eligibility
  -> 기존 STOP / STATUS 및 다른 전용 입력 owner 보존
  -> 공통 한국어 GOTO 후보 판별·원문 좌표 검증
  -> typed GOTO {x, y, z}
  -> 기존 busy / public / source admission 및 compiler
  -> prefix 없는 goto x y z를 기존 제출 경로로 1회 전달
  -> Fabric bridge의 기존 client-thread dispatch
  -> GotoCommand.call()
  -> 기존 PreparedGotoTask / GetToBlockTask
```

이 도식은 책임 경계의 요약이며 기존 세부 admission/컴파일 순서를 재배치하는 계약이 아니다.
현재 route sequence는 STOP → STATUS → generic crafting을 먼저 검사하고,
gate에서 H5 전용 경로와 ordinary 경로를 구분한다. GOTO 입력 보완 때문에 이들 전용 owner의
조건·우선순위를 바꾸지 않는다. 이 경로에 존재하지 않는 `pending-confirmation` 단계를
추가하거나, 확인이 불필요한 기존 public GOTO에 새 승인 수명을 만들지 않는다.

#### 주 연결점과 기존 문자열 API

주 연결점은 `MinecraftInputRouteSequence.route()`의 H5 분기 다음, ordinary coordinator 호출
직전이다. gate에는 같은 GOTO 후보 판별을 연결해 `500 90 -928로 가줘` 같은 입력도 이 경계까지
도달하게 한다. Chat adapter·마이크 callback에 각각 별도의 GOTO 제출 분기를 만들지 않는다.

GOTO `명령 아님` 판정은 ordinary로 계속 진행하는 신호가 아니라 route 자체의 종료다.
현재 바깥 `LlmPredictionDispatchCoordinator.predict()`는 처리되지 않은 입력을 일반 대화
`response_pipeline.predict(...)`로 넘긴다. 그 반환과 Minecraft intent 추출기의 재호출을
구분해 보존한다. ASCII 따옴표가 있는 인용도 공통 `ChatClefTranslationInputGuard`가 먼저
위험 문자로 거절하기 전에 GOTO 앞단에서 일반 대화로 반환한다. 이를 위해 공통 위험 문자
검사를 완화하지 않는다. `확인 필요`는 일반 대화 반환과 다르게 처리 완료된 거절로 종결한다.

현재 ordinary translation은 문자열을 받아 기존 natural-language service에서 intent를
추출한다. 따라서 위 `typed GOTO` 단계가 기존 제출 API에 이미 직접 연결돼 있다는 뜻은 아니다.
기존 `ChatClefNaturalLanguageService.translate(text)` 문자열 API를 유지하고, 공유 rule parser의
GOTO 분기를 같은 원문 전용 순수 파서에 위임한다. GOTO 실패 후 기존 `_GOTO_RE.search()`로
돌아가는 fallback은 남기지 않는다. 다른 명령의 공통 정규화와 기존 진입점은 보존한다.
원문 숫자 문법을 검사해 정수로 만든 뒤에는 기존 `required_exact_int()`와 `java_int()` 검증을
재사용한다. 이 함수들이 원문 숫자 문법까지 해석한다고 간주하지 않는다.

검증된 XYZ는 명령별 불변 결과로 보존해 기존 intent validator/compiler에 연결한다.
같은 원문을 라우터와 공유 rule parser의 두 경계에서 순수 파싱하는 것은 허용한다.
파싱을 한 번으로 줄이기 위해 별도 typed 실행 API나 raw 우회 제출을 만들지 않는다.
기존 번역 경로에서도 같은 원문과 동일한 순수 좌표 문법만 사용하며,
손실된 정규화 문자열·과거 GOTO 부분 정규식·LLM으로 다시 좌표를 선택하지 않는다.

#### 최초 XYZ와 기존 번역 결과의 결합

기존 `ChatClefTranslationResultValidator.validate()`의 intent↔command 일치 검사는 유지한다.
추가 검사는 `OrdinaryMinecraftCommandRoutePipeline.route_locked()`가 번역 결과를 얻고
번역 단계 자체의 오류를 처리한 직후, 일반 rejection 검사 전에 배치하는 GOTO 전용 결합 검사다.
기존 coordinator의 잠금·reconciliation·precheck 순서를 바꾸지 않고 최초 판별 결과를 전달한다.

자동 Chat/최종 마이크 경로의 유효한 원문 GOTO에 대해서 최종 intent가 GOTO인지, XYZ가 처음
검증한 값과 같은지, command가 기존 compiler의 같은 `goto x y z`인지 확인한다.
원문은 `(500, 90, -928)`인데 intent와 command가 함께 `(501, 90, -928)`로 바뀌면 기존
둘 사이 검사만으로는 충분하지 않다. `ChatClefIntentDTO`의 x/y/z는 frozen 필드이지만 `slots`는
mutable dict이므로, 최초 XYZ를 그 dict에만 맡기지 않는다. 별도 불변 결과/정수 튜플로 보존한다.

유효한 원문 GOTO가 다른 명령·UNKNOWN으로 바뀌거나, 이 자동입력 경로에서 유효한 원문 GOTO
결과 없이 번역기가 GOTO를 만들어낸 경우도 제출하지 않는다. 자동입력 대상 여부와 원문 판별
결과를 명시적으로 전달해, 단순한 optional 값 부재가 기존 GUI/direct 진입점과 혼동되지 않게 한다.
이 검사는 원문과 번역의 결합만 맡고 기존 compiler·result validator를 복제하지 않는다.
최종 제출할 canonical 명령이 정확히 `goto x y z`이고 세 정수가 검증된 XYZ와 일치해야 한다.
값 변경·누락·추가 인자 또는 불일치는 제출 0회로 끝내며, 원문을 고쳐 재시도하지 않는다.

#### 공유 파서·composite·admission의 재해석 차단

앞단 guard로 끝나지 않고 기존 번역 서비스를 사용하는 다른 진입점에서도 GOTO 거절을
보존해야 한다. 기존 H5/store-home의 거절 전달 방식처럼 GOTO 재해석 금지 표식을 intent에
전달하고, `CompositeChatClefIntentExtractor.extract()`는 그 표식이 있으면 optional Minecraft
LLM을 호출하지 않는다. 표식은 실행 권한이나 proof가 아니다.

표식 존재 판정과 내용 유효성 검사를 구분한다. 잘못된 표식도 일반 UNKNOWN으로 되돌려
재해석하지 않고, `ChatClefIntentAdmissionStage.inspect()`에서 non-executable 거절로 처리한다.
유효한 거절 표식은 기존 rejection DTO·응답 경로에 연결한다. GOTO 전용 거절이 ordinary의
아이템 소유권/일반 UNKNOWN fallback으로 흘러가지 않게 하고, 같은 입력에 앞단과 공유 번역이
각각 확인 응답을 발행하지 않는다. 앞단에서 종결한 입력은 번역 API를 호출하지 않는다.

#### 정상 consumed ingress와 live proof 재사용

자동 Chat/최종 마이크 GOTO guard는 기존
`TrustedKoreanInputRouteCoordinator.is_live_proof(proof, event)`를 재사용한다.
현재 router composition도 같은 validator를 ordinary coordinator에 주입하고 있다.
`KoreanPublicCommandSubmissionPolicy`의 public/source 허용 목록은 proof 검증을 대신하지 않으며,
feedback `prepare(...)`에서 grant가 없다는 사실도 transport 제출 금지와 같지 않다.
proof 누락·다른 이벤트/owner·닫힘·유효성 상실은 해당 자동 GOTO 실행 경계에서 제출 0회로 끝내고,
source 문자열만 확인하는 ordinary fallback으로 통과시키지 않는다.

정상 흐름 자체가 `ConsumedIngressEvidence`를 사용한다. 기존 ingress 소비 → eligibility의
단일 claim과 proof 발급 → 같은 이벤트의 live proof로 처리 → 기존 소유자의 close 순서를
보존한다. **정상 consumed evidence에 기초한 live proof는 허용**하며, 소비됨이라는 이유만으로
거절하지 않는다. 재발급/중복 재제출 시도와 정상 수명 안의 검증을 구분한다.
새 guard는 proof를 발급·재소비·조기 종료하거나 별도 시간 만료 정책을 만들지 않는다.
기존 validator가 판단하는 타입·이벤트·owner·원문 결합과 수명을 그대로 사용한다.
이 요건을 기존 `direct_typed`/`lavi_gui_korean` 경로로 확대하지 않고, 순수 파서에도 넣지 않는다.

원래 source, event ID, 신뢰 증거와 request/correlation/session/root 연결을 보존한다.
`voice_input_final`을 `direct_typed`로 바꿔 통과시키거나, proof를 새로 꾸며내지 않는다.
STT·소켓 callback에서 Minecraft 상태를 직접 변경하지 않는다.

중복 방지는 기존 이벤트 identity·소비 수명에 연결한다. 같은 문장을 사용자가 나중에 새로
말한 정상 요청까지 문자열만으로 영구 차단하지 않는다. 하나의 입력을 한국어 경로와
raw/일반 LLM 경로가 각각 제출하지 않는다. busy는 기존 정책대로 제출 0회이며,
작업 종료 후 대기열 실행·재접속 replay·자동 재시도를 새로 넣지 않는다.
`submit_once()`라는 이름 자체가 이벤트 중복 제거를 보장하는 것은 아니다. 현재 request factory는
호출마다 request UUID를 만들므로 두 경로에서 호출하면 서로 다른 요청이 될 수 있다.
기존 ingress/proof 수명 → 기존 ordinary 잠금 → 기존 제출 지점 한 곳의 관계를 보존하고,
새 GOTO guard는 제출하지 않는다. 정상 자동입력은 기존 번역 API 한 번과 adapter 제출 한 번을
검증한다. 전체 파서 호출 횟수나 문자열 캐시로 단일 제출을 대신하지 않는다.

문법적으로 유효하다는 사실은 실행 환경의 준비 완료를 뜻하지 않는다. 플러그인 비활성,
bridge 미연결, 월드/플레이어 미준비, busy는 기존 admission 정책으로 처리하며 자동 설정 변경,
게임 실행 또는 나중의 재전송으로 보완하지 않는다. 한국어 XYZ는 차원 인자를 추가하지 않고
기존 nullable dimension 요청을 보존한다. Python에서 live Y/차원을 추정하거나 좌표를 임의로
보정하지 않는다. Java int 범위 통과도 실제 지형에서의 도달 가능성 보장은 아니다.

### 4.2 `@go`는 추가하지 않고 기존 `@goto` 사용

- 한국어 Chat/최종 마이크 입력은 기존 compiler가 만드는 prefix 없는 `goto x y z`로
  연결한다. 사용자가 `@goto`라고 말하거나 입력할 필요는 없다.
- 직접 명령을 지원하는 기존 Minecraft 게임 채팅·raw 명령 입력 경로에서는 `@goto`를
  그대로 사용한다. LAVI 일반 Chat의 ASCII 직접 명령을 새로 지원하는 작업은 포함하지 않는다.
- `@`는 문서 예시다. 기존 설정의 command prefix와 bridge의 prefix 부착 처리를 유지한다.
  `@goto`의 원래 XYZ/XZ/Y/차원 문법, Java 등록·도움말·명령명 목록은 변경하지 않는다.
- `@go` 등록, `go`를 `goto`로 바꾸는 Python 별칭 변환, 별칭용 ASCII adapter,
  source 확대·한국어 eligibility 예외를 만들지 않는다. 언어 scope-out은 실행 허가가 아니다.
- Python에서 Java Task를 직접 구성하거나 기존 bridge를 우회하지 않는다. 기존 GOTO가
  포함된 JAR을 사용하므로 이 입력 연결을 위해 Java 수정·테스트·재빌드·JAR 교체를 하지 않는다.

### 4.3 이동·재료 확보는 변경하지 않는다

한국어 입력 범위에는 같은 높이·하강 XYZ도 포함된다. 다만 `PreparedGotoTask.forCommand()`는
유효한 플레이어·월드에서 차원 전환 없는 **상승 XYZ**에만 부모를 붙인다. 비상승 XYZ는
기존 `GetToBlockTask`를 사용하며, 다른 원래 GOTO 형태도 기존 Task 선택을 유지한다.
한국어 입력 연결이 이 적용 조건을 넓혀서는 안 된다.

현행 V3.1의 native-first 이동, 필요할 때의 제한된 로컬 재료 확보, frozen target,
자식 cleanup, 같은 operation의 기존 이동 재개와 도착 판정을 그대로 사용한다.
한국어를 연결한다는 이유로 접수 시 Y 차이만큼 선채굴하는 과거 방식, 새로운 길찾기,
기둥 쌓기, timeout 채굴 trigger, 재채굴 루프, 전역 입력/Baritone 취소를 추가하지 않는다.
이 문서의 입력 지원은 모든 지형에서 경로 성공을 보장하는 기능이 아니다.

한 번 실행한다는 것은 수락한 입력 한 건당 명령 dispatch·`runUserTask`·root 등록 한 번,
LAVI 입력에서는 submit 한 번을 뜻한다. 부모·채굴 자식·최초 이동 자식·확보 후의 새 이동
자식까지 모두 합쳐 Task 객체 하나만 생성하라는 뜻이 아니다. 기존 정상 자식 교체는 보존하고,
같은 입력을 다시 제출하거나 두 번째 root operation을 만드는 것을 금지한다.

## 5. 한국어 응답 보존과 결과 연결 제외

이 절은 LAVI Chat/마이크 등 기존 LAVI 요청 수명에 결합된 응답을 다룬다.
LAVI를 거치지 않은 native 명령의 UI/TTS 중계는 이 입력 계약의 완료 조건이 아니다.

입력 접수, 실제 실행 시작, 재료 확보, 이동 재개, 도착은 서로 다른 사실이다.
기존 상태·응답 수명에 연결하고 같은 단계의 답변을 UI/TTS에 중복 발행하지 않는다.

- 제출이 수락됐을 때: `500, 90, -928 좌표로 이동할게.`
- 좌표가 모호할 때: `X, Y, Z 좌표 세 개를 다시 알려줘.` — 제출하지 않는다.
- busy/STOP: 기존 검증된 활성 작업·중지 응답을 유지한다.
- 종료 응답: 기존 `도착했는지는 확인하지 못했어` 수준을 유지한다.
- 기존 경로가 실패/STOP/unknown을 전달했을 때: 기존 분류와 한국어 표현을 보존한다.
  Python에서 새 의미를 추정하거나 STOP을 재료 실패로 바꾸지 않는다.

현재 `PreparedGotoTask`는 성공·실패 모두 `isFinished()`가 참일 수 있으며,
`arrived()` / `failureReason()` 조회점이 있어도 기존 bridge의 일반 자연 종료 분기와
한국어 도착 증거에 연결된 것은 아니다. 따라서 입력 문법만 수정하고 정확한 도착 응답까지
완료했다고 보고하면 안 된다.

이 미연결 상태는 알려진 제한이지 이번 Python 입력 구현의 추가 작업이나 완료 전제조건이 아니다.
Java typed 종단 결과 투영, protocol/payload 확대, Python terminal evidence profile/evaluator
승격, 새 `도착했어` 문구 연결은 모두 범위에서 제외한다. 기존 완료 상태를 새 실패/unknown/도착으로
재분류하지 않으며, 로그 문자열·child FINISHED·접수 성공으로 ARRIVED를 만들어내지 않는다.
좌표 해석의 typed 결과와 Java int 범위 검증은 계속 필요하다. 제외하는 것은 Java 종단 결과
연결이지 입력 좌표의 타입·안전 검증이 아니다.

## 6. 책임 분리와 변경 단위

아래는 구현 전에 정한 책임 설계다. 실제 생성 파일·연결 hunk는 9절에서 구분한다.

| Python 책임 | 제안 파일과 재사용 경계 | 검증 경계 |
| --- | --- | --- |
| 후보 판별 | `intent/navigation/goto/korean_goto_candidate_detector.py`: gate와 파서가 공유하는 좌표 이동 후보 단서 | 네 판별 결과의 의미 일관성, 다른 전용 owner 보존 |
| 좌표 파서 | `intent/navigation/goto/korean_goto_coordinate_parser.py`: 원문 전체 문법과 XYZ; 기존 정수 검증 재사용 | 정상 XYZ, 불명확·부정·인용·소수 거절; 부수 효과 없음 |
| 불변 결과 | `intent/navigation/goto/goto_parse_result.py`: 판별 결과와 최초 XYZ 보관 | 다른 입력·명령 상태와 혼합하거나 mutable slots에만 보관하지 않음 |
| 거절 표식 변환 | `intent/navigation/goto/goto_guard_intent_codec.py`: 기존 intent 경로에 재해석 금지 판정 전달 | malformed 표식도 fallback 금지; 실행 권한 부여 없음 |
| 입력 라우팅 guard | `input/routing/goto/goto_input_route_guard.py`: 기존 live-proof 검증과 계속/일반 대화/확인 종결 분기 | 자동 Chat/final mic 보호, 기존 source 보존, 자체 제출 없음 |
| 원문·번역 결합 검사 | `input/routing/goto/goto_translation_binding_validator.py`: 최초 XYZ와 기존 번역 결과의 연결 | intent/command 동시 변조·다른 명령·원문 GOTO 없는 번역 GOTO 제출 0회 |
| 관찰·검증 | 기존 Python bounded 진단과 focused 테스트를 해당 책임별로 배치 | 입력 경계 추적, 중복·busy·미준비·STOP 회귀; 로그가 실행을 결정하지 않음 |

위 경로는 `plugins/Minecraft/fabric/chatclef/` 아래이며,
[기존 계약의 책임 분리안](chatclef-korean-goto-find-and-all-command-coverage-pre-change-contract-2026-09-09.md#91-python-ownership)을
구체화한 후보 구성이다. 정확한 파일명과 기존 helper 재사용은 구현 전 소유권 검토에서 확정하며,
위 여섯 파일을 기계적으로 생성하거나 이미 생성됐다고 간주하지 않는다. 공통 문법 상수가
필요하면 같은 기능 폴더에서 작게 분리하고, gate/parser/composition은 얇게 연결한다.
새 LAVI-owned 코드에 독립된 책임이 둘 이상이면 파일·의미 있는 폴더로 분리한다.
빈 프레임워크, 통합 manager, mutable static/global 상태를 만들지 않는다.
upstream ChatClef/AltoClef 클래스를 이동·분해·광범위하게 리팩터링하지 않는다.

후속 Python 소스 단위는 현 HEAD·dirty 변경·owner·정확한 hunk·독립적인 되돌림 범위를 먼저
확인한다. 기존 `goto`를 호출하는 입력 연결과 Java dispatch·Task·결과·packaging 변경을
같은 작업으로 취급하지 않는다. Java 소스/테스트/리소스/Gradle, protocol, JAR과 dependency는
수정하지 않는다. Java 변경 없이는 충족할 수 없는 별도 문제가 드러나면 이번 범위에 섞지 않고
근거와 한계를 보고한다. 이 문서는 AGENTS의 해당 gate를 완화하거나 새 승인 예외를 만들지 않는다.
미해결 이동 원인을 문법 테스트로 입증했다고 쓰지 않는다.

### 6.1 구현 순서

각 연결 단계의 bounded Python 진단은 적용되는 AGENTS 순서에 따라 해당 결정 경계와 함께
계획·구성한다. 아래 마지막 단계는 최종 통합 검증이며, 필요한 진단을 그때까지 미루라는 뜻이 아니다.

1. 원문 기반 후보 판별·순수 파서·불변 결과를 책임별로 구성하고 정상 9개 문장과 거절 문법을
   Python 단위 테스트로 고정한다.
2. 기존 rule parser의 GOTO 부분 검색을 같은 순수 파서로 교체하고, 거절 뒤 옛 정규식으로
   돌아가지 않게 한다. 기존 문자열 translate와 다른 명령의 정규화는 보존한다.
3. composite와 intent admission에 GOTO 거절 표식을 연결해 optional Minecraft LLM 주입 여부와
   무관하게 비실행 결정을 유지한다.
4. gate와 기존 owner 다음·ordinary 이전의 route guard를 연결한다. 기존 live proof를 재사용하고,
   일반 대화 반환과 처리 완료된 확인 응답을 구별한다.
5. 자동입력의 최초 판별/불변 XYZ를 기존 ordinary coordinator·pipeline으로 전달하고 번역 직후
   결합을 검증한다. 제출은 기존 한 곳에서만 수행하며 잠금·precheck·feedback 수명을 유지한다.
6. 기존 신뢰된 Chat/최종 마이크 fixture를 확장해 정상 제출 1회와 거절·중복·busy 제출 0회를
   검증한다. 각 단계에 동반한 bounded Python 로그의 ON/OFF 회귀도 확인한다.

위 순서는 후속 Python 구현의 작업 목록이지 별도의 새 실행 API, Java 진단·재빌드,
모든 과거 navigation 원인 규명이나 새 도착 응답 구현을 선행 조건으로 추가하는 목록이 아니다.
실제 변경이 AGENTS의 다른 보호 경계에 닿는다면 그 정확한 경계와 더 좁은 대안을 보고한다.

## 7. 필수 검증과 관찰 기준

정상 입력의 제출 1회 기준은 신뢰된 새 이벤트이고 연결·월드·사용자 Task admission이
허용되는 조건에서 검사한다. 거절·미준비 조건의 제출 0회 검증은 별도로 수행한다.

| 검증 대상 | 합격 기준 |
| --- | --- |
| 표의 모든 한국어 정상 문장 | Chat와 final mic 각각 같은 XYZ, 같은 canonical command, 제출 1회 |
| 숫자 검증 | 음수·0·양수·부호 표현을 보존하고 Java int 경계를 정확히 검사; 소수/범위 초과 제출 0회 |
| 공백·구분자 검증 | 허용된 일반 공백과 짝이 맞는 XYZ 괄호는 처리; 자동입력 원문의 탭·CR/LF·기타 제어문자 또는 불일치 괄호를 정규화로 보정해 실행하지 않고 제출 0회 |
| 불완전·모호한 명령 | 확인/거절 1회, 제출 0회; optional Minecraft LLM extractor 주입 ON/OFF 모두 보정 실행 없음 |
| 일반 대화·인용·부정·질문 | GOTO 후보인 비명령은 route 자체에서 일반 대화로 반환; ordinary 번역·Minecraft LLM·제출 없음 |
| 두 번째 행동을 포함한 복합 실행 요청 | 확인/거절 한 번, 제출 0회; 명령 일부만 추출해 실행하지 않음 |
| 마이크 interim → final → 중복 전달 | interim 제출 0회, 동일 최종 이벤트 제출 최대 1회 |
| 신뢰·출처·busy·연결 끊김 | forged/replayed 증거 거부, busy 제출 0회, 자동 replay 없음; Hangul·source 검사 확대 없음 |
| 정상 증거 수명 | 정상 consumed ingress에서 발급된 live proof는 허용; 다른 이벤트/owner·닫힌 proof·중복 claim은 거절; feedback grant 유무를 실행 권한으로 대체하지 않음 |
| 명령 이름·입구 회귀 | canonical `goto x y z`와 기존 제출 경로 유지; `go` 별칭 변환·등록 없음; 기존 직접 `@goto` 경로 보존 |
| 공유 파서·기존 입력 호환 | 파서는 원문 문자열만 해석하고 실행·proof 발급 없음; 기존 direct_typed/lavi_gui_korean source·admission과 원문 전달 보존 |
| 검증 좌표와 최종 명령 일치 | command 단독 변조와 intent/command 동시 변조, 다른 명령·go·UNKNOWN, 추가 인자, 자동입력의 원문 GOTO 없이 생성된 번역 GOTO 모두 제출 0회 |
| 공유 번역 진입점의 거절 | 앞단을 거치지 않는 기존 번역에서도 GOTO 표식·malformed 표식은 optional Minecraft LLM ON/OFF 모두 재해석 금지; 기존 거절 응답 보존 |
| Java 비변경 범위 | 기존 GotoCommand 입구·상승 Prepared/비상승 legacy 선택·재료 확보·cleanup 코드를 수정하지 않음; Java/Gradle/JAR 변경 없음 |
| Python 입력 연결 통합 | 정상 9개 문장을 실제 신뢰 fixture의 Chat/final mic 양쪽에서 원문 → typed GOTO → canonical goto → 기존 submit/admission까지 검사; source/event 보존, 번역 API·adapter 제출 각 1회 |
| 종단 결과·응답 회귀 | 기존 completed/failed/STOP/unknown 분류와 신중한 한국어 종료 표현 보존; 새 도착 문구·증거 승격·응답 중복 없음 |

### 7.1 기존 Python 테스트 재사용 위치

- [신뢰된 Chat/최종 마이크 통합 fixture](../../../tests/minecraft_chatclef/lavi_input/test_trusted_korean_chat_voice_integration.py):
  `_llm_harness`, `_chat_coordinator`, `_voice_provider`와 기록용 adapter를 확장한다.
  기존 ingress registry·registrar·queue 경계를 통과시켜 양쪽 입력의 source/event와 최종 제출을 확인한다.
- [기존 rule parser 테스트](../../../tests/test_minecraft_chatclef_korean_rule_parser.py): 정상 문법과 부분 검색 회귀.
- [기존 자연어 서비스 테스트](../../../tests/test_minecraft_chatclef_natural_language_service.py): 문자열 API·거절 표식·번역 검증 재사용.
- [오탐 제출 방지 테스트](../../../tests/minecraft_chatclef/input_gate/test_korean_false_positive_submission_guard.py): 후보/비명령 구분과 재해석 차단.
- [입력부터 제출까지의 테스트](../../../tests/minecraft_chatclef/lavi_input/test_korean_input_to_chatclef_submission_path.py): 원문 XYZ 결합·단일 제출·busy 회귀.
- [기존 한국어 GUI 테스트](../../../tests/test_minecraft_fabric_chatclef_korean_gui.py): 공유 parser 변경 후 기존 진입점/source/admission 보존.

source 문자열만 지정한 `router.route(...)` 테스트는 파서·라우팅 단위 검증에는 사용할 수 있지만
신뢰된 최종 마이크 검증 완료의 증거가 아니다. 위 통합 fixture도 실제 음성 인식·실게임 도착의
증거는 아니다. 바깥 일반 대화 LLM으로 반환되는 것과 Minecraft 명령 추출 LLM을 재호출하는 것은
구별해 검사하며, 기존 fixture를 재사용하고 새 테스트 프레임워크는 만들지 않는다.

### 7.2 진단과 별도 런타임 확인

진단은 기존 bounded logger와 correlation 체계를 우선 재사용한다. 최초 후보 판정,
typed 해석 결과/거절 이유, admission, 제출, root binding, 종단을 연결하되 매 tick·원문 음성·
전체 대화 내용을 반복 기록하지 않는다. 새로운 필드가 필요하면 기존 schema와 대조해 이름과
한도를 먼저 정한다. 로그 ON/OFF·누락·억제가 실행·재시도·도착 판정을 바꾸면 안 된다.
새 진단은 Python 입력 결정 경계에 한정한다. Java root/종단은 기존 증거를 대조하며,
관측이 부족하다는 이유로 이번 범위에 Java 진단·payload 변경을 추가하지 않는다.

별도로 허용된 실제 확인에서는 기존 JAR을 그대로 사용해 Chat 1건과 실제 마이크 1건을
검사한다. Chat 원문 / 마이크 STT 최종 문자열의 좌표·부호 → request → 기존 bound root →
이동 결과를 연결해 확인한다.
도달 가능한 시험 목표의 실제 도착은 기존 게임 상태·로그로 확인하되, 새 Java 결과 payload나
강한 한국어 `도착했어` 문구를 합격 조건으로 요구하지 않는다. 같은 명령의 증거가 부족하면
런타임 판정을 미확인으로 남긴다. 기존 경로 실패는 별도 조사 사항이며 입력 변환 실패와 구분한다.

Python 통합 검증, bridge 접수, 직접 `@goto` 성공, 실제 마이크·Chat 이동 성공을 서로 대체하지
않는다. 오프라인 검증만 했다면 `OFFLINE_PASSED / LIVE_NOT_RUN`처럼 구분해 보고한다.
Python 전용 입력 구현에 Java 빌드·배포를 필수 단계로 붙이지 않는다. 이번 별도 구현 요청으로
Python 오프라인 테스트를 수행했으며, Minecraft 실행·월드/블록/cache 변경은 수행하지 않았다.
실제 마이크·게임 확인은 별도로 허용된 요청에서 수행한다.

## 8. 소스 근거 위치

아래 링크는 검토 HEAD의 책임 위치다. 이후 수정 시 줄 번호 대신 명시된 클래스/메서드와
실제 diff를 다시 확인한다.

- [KoreanCommandRegistry](../fabric/chatclef/command_registry/korean_command_registry.py): 공개·준비 상태와 source 허용.
- [KoreanChatMicrophoneEligibilityAdmission.issue](../fabric/chatclef/input/eligibility/korean_chat_microphone_eligibility_admission.py): trusted Chat/final microphone·Hangul·단일 소비.
- [MinecraftChatClefInputIntentGate.inspect](../fabric/chatclef/input/gating/minecraft_chatclef_input_intent_gate.py): 현재 trigger 조건.
- [KoreanChatClefRuleParser.parse](../fabric/chatclef/intent/korean_chatclef_rule_parser.py): 현재 GOTO 정규식과 typed intent.
- [KoreanTextNormalizer.normalize](../fabric/chatclef/intent/korean_text_normalizer.py): 구두점 정규화.
- [LaviInputEventNormalizer.normalize](../../../input_core/input_event/normalization/lavi_input_event_normalizer.py): typed 원본 이벤트 보존.
- [CompositeChatClefIntentExtractor.extract](../fabric/chatclef/intent/composite_chatclef_intent_extractor.py): UNKNOWN의 optional LLM 재해석 경계.
- [ChatClefCommandCompiler](../fabric/chatclef/intent/chatclef_command_compiler.py): `goto x y z` 변환.
- [ChatClefNumericConstraints](../fabric/chatclef/intent/chatclef_numeric_constraints.py): 기존 exact-int/Java int 범위 검증.
- [ChatClefIntentDTO](../fabric/chatclef/intent/chatclef_intent_dto.py): frozen x/y/z 필드와 mutable slots의 구분.
- [ChatClefTranslationResultValidator.validate](../fabric/chatclef/intent/chatclef_translation_result_validator.py): 이미 존재하는 intent↔command 일치 검사.
- [ChatClefNaturalLanguageService.translate](../fabric/chatclef/intent/chatclef_natural_language_service.py): 문자열 입력의 intent 추출·기존 검증/컴파일 연결.
- [ChatClefTranslationInputGuard.inspect](../fabric/chatclef/intent/natural_language/chatclef_translation_input_guard.py): extractor보다 앞선 위험 문자 거절.
- [ChatClefCommandSafetyValidator](../fabric/chatclef/intent/chatclef_command_safety.py): 탭·CR/LF·제어문자 등을 금지하는 기존 공통 검사.
- [ChatClefIntentAdmissionStage.inspect](../fabric/chatclef/intent/natural_language/policy/chatclef_intent_admission_stage.py): 기존 guard 거절과 일반 UNKNOWN 처리 구분.
- [LegacyNaturalLanguageCommandCoordinator](../fabric/chatclef/extension/natural_language/legacy_natural_language_command_coordinator.py): 기존 문자열 입력의 번역·제출 경로.
- [MinecraftInputRouteSequence.route](../fabric/chatclef/input/routing/orchestration/minecraft_input_route_sequence.py): 실제 STOP/STATUS·전용 owner·gate 순서.
- [TrustedKoreanInputRouteCoordinator.route](../fabric/chatclef/input/routing/trusted_korean/trusted_korean_input_route_coordinator.py): proof 없는 scope-out과 fallback 경계.
- [TrustedKoreanProofValidator.is_live](../fabric/chatclef/input/routing/trusted_korean/trusted_korean_proof_validator.py): coordinator의 `is_live_proof()`가 재사용하는 live 검증.
- [KoreanChatMicrophoneProofLifecycle](../fabric/chatclef/input/eligibility/proof/korean_chat_microphone_proof_lifecycle.py): 이벤트·owner·원문 결합과 close 수명.
- [MinecraftChatClefRouterComponentGraph](../fabric/chatclef/input/routing/composition/minecraft_chatclef_router_component_graph.py): 기존 live-proof validator 주입 위치.
- [KoreanPublicCommandSubmissionPolicy.inspect](../fabric/chatclef/command_registry/admission/korean_command/public_command_submission_policy.py): public/source 허용 검사와 proof 검증의 구분.
- [OrdinaryMinecraftCommandRouteCoordinator](../fabric/chatclef/input/routing/ordinary/ordinary_minecraft_command_route_coordinator.py): 기존 잠금과 pipeline 전달.
- [OrdinaryMinecraftCommandRoutePipeline](../fabric/chatclef/input/routing/ordinary/ordinary_minecraft_command_route_pipeline.py): 기존 번역·제출 연결.
- [OrdinarySubmissionResultStage.submit](../fabric/chatclef/input/routing/ordinary/submission/ordinary_submission_result_stage.py): feedback 준비와 transport 제출의 별도 책임.
- [MinecraftChatClefSubmissionRequestFactory.build](../fabric/chatclef/input/routing/submission/submission_request_factory.py): 제출 호출별 request ID 생성과 원본 이벤트 전달.
- [LlmPredictionDispatchCoordinator.predict](../../../llm_core/input_routing/llm_prediction_dispatch_coordinator.py): 처리되지 않은 입력의 바깥 일반 대화 반환.
- [GotoCommand.call](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/GotoCommand.java): 기존 이동 입구.
- [AltoClefCommands](../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClefCommands.java): native 명령 등록.
- [PreparedGotoTask](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/task/movement/gotopreflight/PreparedGotoTask.java): 현행 이동·확보·도착/실패 조회점.
- [FabricChatClefCommandOutcomeClassifier](../runtime/chatclef_fabric_1.20.1/src/main/java/lavi/minecraft/fabric/chatclef/bridge/command/lifecycle/FabricChatClefCommandOutcomeClassifier.java): callback/root/STOP 및 자연 종료 분류.
- [Command terminal evidence profiles](../fabric/chatclef/transport/command_feedback/lifecycle/evidence/command_terminal_evidence_profile_registry.py): GOTO cautious 상태.
- [KoreanCommandTerminalRenderer](../fabric/chatclef/response/command_lifecycle/grammar/boundary/korean_command_terminal_renderer.py): 한국어 완료/실패 표현.

## 9. 2026-09-13 Python 구현 결과

### 9.1 구현 범위와 provenance

사용자의 별도 구현 요청으로 **한국어 Chat·최종 마이크 전사 → 기존 `goto x y z` 입력 연결**을
구현했다. 처음 문서화 때의 `DOCUMENTED_NOT_IMPLEMENTED` 상태를 대체한 구현 완료 기록이다.
그 구현 작업에서 Java 이동·채굴 기능을 새로 구현하거나 실제 음성 인식·게임 도착을 검증하지는 않았다.
이후 사용자 실행의 관측은 10절에 별도로 기록한다.

- 작업 기준 HEAD: `99af65f2cb0609de31fae24d2f1704aa6853f659`.
- 로컬 비교점: `92760206652ce155b482f3ddc85505702f9d811e`. 구현 전 두 commit의
  `plugins/Minecraft/fabric/chatclef` 전체 diff는 없었고 직접 변경할 파일의 blob도 같았다.
  최신 remote 검증을 주장하지 않는다.
- 따라서 현재 Python baseline 위에 새 입력 코드를 작성하는 단위로 분리했다. 과거 실패한
  navigation 구현을 복원·재사용하거나 그 일반 경로 실패 원인을 규명한 것으로 표시하지 않는다.
- 시작 시 기존 변경은 README·과거 한국어 계약·이 문서 3개였다. 과거 한국어 계약과
  `AGENTS.md`는 변경하지 않았고, README에서는 이번 기능 상태 단락만 갱신했다.
- Java/runtime, actual submission transport/admission, Task/경로/채굴, terminal projection,
  protocol, 설정, 의존성, Gradle/JAR 파일은 변경하지 않았다.
- 독립 역패치 범위는 아래 새 Python 파일과 기존 파일의 이번 입력 연결·테스트 hunk뿐이다.
  이는 변경 단위 기록이며 실제 원복·삭제 승인이 아니다. 파일 이동·이름 변경·삭제는 없었다.

### 9.2 실제 책임 분리와 연결

새 production 파일은 `plugins/Minecraft/fabric/chatclef/` 아래에만 생성했다.

```text
intent/navigation/__init__.py
intent/navigation/goto/
  __init__.py                           공개 파서 API
  goto_grammar.py                       공유 불변 문법 정의
  goto_parse_decision.py                네 판별 상태
  goto_parse_result.py                  불변 판별 결과·XYZ
  korean_goto_candidate_detector.py     좌표 이동 후보 단서
  korean_goto_coordinate_parser.py      원문 전체 파싱·정수 검증
  guard/
    __init__.py                        공개 거절 표식 API
    goto_guard_fields.py               불변 표식 규격
    goto_guard_marker_detector.py      표식 존재 판별
    goto_guard_intent_encoder.py       비실행 판정 인코딩
    goto_guard_intent_decoder.py       strict 표식 검증·malformed 거절
input/routing/goto/
  __init__.py                           공개 입력 경계 API
  goto_input_binding.py                 자동입력 적용 범위·원문 판별 결과 DTO
  goto_input_route_guard.py             앞단 proof 검사·일반 대화/확인/진행 분기
  goto_input_decision_factory.py        기존 응답 DTO의 GOTO 거절 생성
  goto_input_diagnostics.py             동작에 관여하지 않는 유한 진단
  goto_translation_binding_validator.py 최초 XYZ와 번역 결과 일치 검사
  goto_translation_binding_stage.py     잠금 안 번역 후 결합/proof 재검증 단계
```

기존 production 파일의 이번 변경은 다음으로 한정한다.

| 기존 경로 (`fabric/chatclef/` 기준) | 변경 hunk |
| --- | --- |
| `intent/korean_chatclef_rule_parser.py` | GOTO만 순수 원문 파서로 위임. 옛 부분 검색은 이력 주석으로 보존하되 실행 fallback 없음 |
| `intent/composite_chatclef_intent_extractor.py` | GOTO 표식이 있으면 optional LLM 재해석 금지 |
| `intent/natural_language/policy/chatclef_intent_admission_stage.py` | 유효·malformed 표식을 기존 non-executable rejection으로 변환 |
| `input/gating/minecraft_chatclef_input_intent_gate.py` | 공유 후보 판별 연결; H5 선행 유지 |
| `input/routing/orchestration/minecraft_input_route_sequence.py` | H5 다음 앞단 guard, 불변 binding 전달, 정상 자동 GOTO 원문 유지 |
| `input/routing/orchestration/minecraft_input_route_ordering_coordinator.py` | guard 의존성 전달 |
| `input/routing/orchestration/minecraft_input_route_ordering_component_graph.py` | sequence에 guard 주입 |
| `input/routing/composition/minecraft_chatclef_router_component_graph.py` | 기존 `is_live_proof()`와 logger로 guard 구성 |
| `input/routing/ordinary/ordinary_minecraft_command_route_coordinator.py` | 선택적 typed binding 전달; 기존 호출 API·잠금 보존 |
| `input/routing/ordinary/ordinary_minecraft_command_route_pipeline.py` | 번역 오류 처리 다음, 일반 rejection 이전의 GOTO 결합 검사 |
| `input/routing/ordinary/composition/ordinary_minecraft_command_route_component_graph.py` | 결합 검사 단계 구성 |
| `extension/natural_language/natural_language_command_input.py` | 순수 파서로 유효한 XYZ인 경우만 raw 문자열 유지; 다른 요청의 기존 `.strip()` 보존 |

원문 앞뒤 일반 공백도 extension/service 번역 API 인자와 요청의 `original_text`에 보존한다.
서비스 내부의 기존 input guard는 앞뒤 공백을 제거할 수 있으므로 `intent.original_text`는
여전히 신뢰 원문 owner가 아니다. 최초 좌표는 `LaviInputEvent.text`에서 먼저 검사한 불변
`GotoInputBinding.parse_result.xyz`로 검증한다. 제어문자를 공백처럼 제거해 통과시키지 않는다.
입력 변환기 등 여러 경계에서 동일 순수 파서를 다시 호출할 수 있지만 번역 API와 실제 제출은
각각 한 번이다. 공통 정규화·compiler·intent↔command 검증·source admission은 대체하지 않았다.

전역 mutable 명령 상태·문자열 중복 캐시·새 proof 발급·대기열·재시도는 추가하지 않았다.
상수는 불변 값/tuple/frozenset/read-only mapping이며 요청별 좌표는 frozen DTO다.
새 클래스는 파일별 한 개로 분리하고, 기존 import/API·STOP/STATUS·H5·제작 owner를 보존했다.

진단은 `event=korean_goto_input`의 `input_guard` 및 `translation_binding` 경계다.
입력당 앞단 최대 1건, 번역 결합 최대 1건이며, 결정·이유·source·기존 event ID·검증된 XYZ만
기록한다. 문자열 token은 64자로 제한하고 원문 대화·음성·인증 정보는 기록하지 않는다.
logger OFF/예외가 발생해도 실행 여부와 제출 횟수는 바뀌지 않는다.

### 9.3 검증 결과와 남은 범위

확장/추가 테스트:

- [원문 파서 테스트](../../../tests/test_minecraft_chatclef_korean_rule_parser.py)
- [공유 번역·거절 표식 테스트](../../../tests/test_minecraft_chatclef_natural_language_service.py)
- [실제 trusted-ingress Chat/최종 전사 fixture](../../../tests/minecraft_chatclef/lavi_input/test_trusted_korean_chat_voice_integration.py)
- [GOTO binding·진단·기존 입력 변환 회귀](../../../tests/minecraft_chatclef/lavi_input/test_goto_input_boundary.py)

프로젝트 `venv\Scripts\python.exe -B -m pytest -p no:cacheprovider`로 실행했다.
기존 `tests/conftest.py`의 저장소 내부 임시 출력 규칙을 유지하고 새 테스트 프레임워크나
의존성을 설치하지 않았다.

| 검사 | 결과 |
| --- | --- |
| 변경 전 parser/service/trusted 입력 기준선 | 20 passed, 63 subtests passed |
| 최종 관련 입력·구조·GUI·parser/service 회귀 | **151 passed, 474 subtests passed** |
| 더 넓은 Minecraft Python 회귀 | **1445 passed, 14958 subtests passed; 기존 HEAD 해시 검증 1건 실패; live 검사 2건 skip** |
| 변경 Python 35개 파일 구문·신규 marker·파일별 class 검사 | PASS; 새 production 19개 파일, class/marker 위반 0 |
| 이 문서·README와 diff 검수 | 상대 링크 61개 중 깨짐 0; UTF-8·공백·fence·`git diff --check` PASS |
| 실제 마이크 STT, live Chat·Minecraft 이동 | NOT_RUN |
| Java 테스트 실행·Gradle build·JAR 교체·배포 | NOT_RUN |
| 커밋·푸시 | NOT_RUN |

관련 회귀 실행 명령:

```powershell
& .\venv\Scripts\python.exe -B -m pytest -p no:cacheprovider tests/minecraft_chatclef/input_gate tests/minecraft_chatclef/lavi_input tests/minecraft_chatclef/structure tests/test_minecraft_fabric_chatclef_korean_gui.py tests/test_minecraft_chatclef_natural_language_service.py tests/test_minecraft_chatclef_korean_rule_parser.py -q
```

확장 회귀는 `tests/minecraft_chatclef` 및 `tests`의 `test_minecraft_chatclef*.py`,
`test_minecraft_fabric_chatclef*.py`를 대상으로 실행했다. 2개 skip은 실제 게임 확인용
`LAVI_MINECRAFT_RUNTIME_TESTS=1` opt-in이 없는 기존 runtime 테스트다.

확장 회귀의 실패는
`test_minecraft_chatclef_java_item_command_contract.py::test_java_contract_sources_have_not_drifted_at_head`
내 `FabricChatClefCommandDispatcher.java`의 **Git HEAD blob** 비교다. 기대 SHA-256은
`5afbb6d923a453f7888efb46957920bf3e665ad415f3f492af8980d2a764d8fc`, 실제 HEAD 값은
`f89a1f6a9014a861162bce95d4d4da71683425fa8b5ead350ebd6cf3dc6c30ae`다.
테스트는 worktree가 아닌 committed HEAD를 읽고, 이번 작업에서 HEAD·해당 테스트·Java 파일은
변경하지 않았다. 기존 불일치로 분리하며 이를 통과시키기 위해 Java나 기대 해시를 수정하지 않았다.
따라서 확장 회귀 전체가 통과했다고 주장하지 않는다. 기존 `websockets.legacy` 폐기 예정 경고도
남아 있으며 의존성 버전을 변경하지 않았다.

검수 중 발견한 원문 공백 손실과 `이동해주지 마`/`가주지 마`의 부정문 분류 누락은
각각 좁은 입력 변환 hunk와 공유 문법으로 보완하고 회귀를 추가했다. 신뢰된 정상 9문장은 양쪽
입력에서 canonical command/source/event를 보존하고 번역·제출 각 1회가 확인됐다.
잘못된 좌표, raw 제어문자, 질문·부정·인용, stale/foreign proof, replay, busy, 미연결,
intent/command 동시 변조, 원문 GOTO 없는 번역 GOTO 및 로그 ON/OFF/예외도 검증했다.

9절 작성 시점의 완료 상태는 **Python 입력 연결 구현 및 오프라인 검증 완료 / 실제 음성·게임 검증 미실행**이다.
이 결과가 현재 Java의 모든 지형 이동 성공이나 정확한 도착 응답을 보증하지 않는다.

## 10. 후속 사용자 Chat 실행과 도착 응답 공백

2026-09-13 12:52:49~12:53:03(KST) 사용자가 LAVI Chat에서 `500 90 -928로 가줘`를
실행했다. 기존 로그에서 원문 XYZ 결합, 같은 요청의 dispatch·root·자연 종료·결과 수신을
확인했다. 사용자는 도착을 보고했고 첨부 F3 화면은 `Block: 500 90 -928`을 표시했다.
이 문서화 작업에서 새로 게임을 실행하거나 이동 명령을 전송하지 않았다.

| 항목 | 후속 확인 상태 |
| --- | --- |
| 위 한국어 Chat 한 문장의 입력→명령 전달 | VERIFIED_RUNTIME |
| 물리적 목표 블록 도착 | USER_REPORTED_AND_SCREENSHOT_OBSERVED |
| 같은 요청의 Java 결과→LAVI | `completed / matching_task_finished`, 일반 자연 완료 수신 확인 |
| 확정 `ARRIVED` 결과의 전달 | UNKNOWN; 현재 수신 결과에 도착 증거 없음 |
| UI/TTS 종료 문장 | 도착 미확인 문장을 생성·전달, TTS 재생 완료 확인 |
| 실제 마이크 STT 및 전체 정상/거절 live matrix | NOT_RUN_IN_REVIEWED_CASE |
| 실행 JAR와 검토 소스의 동일성 | UNKNOWN |
| 신규 도착 결과 연결 구현·build·배포 | NOT_RUN |

세부 로그 경로·행 번호·상관 ID, 현재 결과 연결 공백과 후속 구현/검증 계약은
[GOTO 도착 결과와 한국어 종료 응답 연결 계약](chatclef-goto-arrival-result-and-tts-response-pre-change-contract-2026-09-13.md)에 기록했다.
그 문서는 별도 후속 작업의 계획이며 5절의 Python 입력 구현 범위를 소급해 확대하지 않는다.
이번 한 건의 도착 관측으로 모든 경로의 성공, Task의 `ARRIVED` 내부값, 과거 navigation 실패
원인이나 실제 마이크 지원 완료를 주장하지 않는다. 출발 TTS 지연도 결과 의미 연결과 분리한다.
