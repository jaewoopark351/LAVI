<!-- 20260904_kpopmodder: Defined the docs-only H5 Korean Chat/microphone natural-language exposure contract before Python source changes. -->
<!-- 20260905_kpopmodder: Clarified two-phase claim authorization, exact Java test scope, fallback preservation, and microphone ACK limits after cross-document review. -->
<!-- 20260905_kpopmodder: Recorded the implemented Python boundary, focused Java verification, and clean-build evidence. -->

# ChatClef H5 Auto-Deposit Trust Korean Chat/Microphone Pre-Change Contract

## 1. Document status

```text
DOCUMENT_TYPE: PRE_CHANGE_IMPLEMENTATION_CONTRACT
DECISION_DATE: 2026-09-04
DECISION_STATUS: IMPLEMENTED_AND_OFFLINE_VERIFIED
REPOSITORY_BASELINE: f301770d1885f8d7239846a97b187f4d51c05a55
FEATURE_SCOPE: LAVI_CHAT_AND_MICROPHONE_KOREAN_NATURAL_LANGUAGE_TO_EXISTING_H5_COMMAND

JAVA_H5_PRODUCTION_SOURCE: EXISTING_AND_PRESERVED
FUTURE_JAVA_VERIFICATION_SCOPE: ONE_NEW_FOCUSED_INTEGRATION_TEST_PLUS_ONE_EXISTING_REGISTRAR_TEST_HUNK_AND_REQUIRED_CLEAN_BUILD
PYTHON_NATURAL_LANGUAGE_SOURCE: IMPLEMENTED
PYTHON_INPUT_PROVENANCE: IMPLEMENTED_WITH_IMMUTABLE_TYPED_EVENTS_AND_EXACT_SOURCE_TUPLES
PUBLIC_ENABLEMENT_GATE: ENABLED_FOR_AUTO_DEPOSIT_TRUST_AFTER_OFFLINE_CONTRACT_VERIFICATION
MICROPHONE_ACK_DELIVERY: NOT_IMPLEMENTED; QUEUE_DRAIN_CURRENTLY_DISCARDS_GENERATOR_YIELDS
PYTHON_COMMAND_CATALOG: 26_SOURCE_BACKED_ROWS
TARGET_SOURCE_BACKED_COMMAND_SURFACE: 26_UNIQUE_NAMES_ON_COLLISION_FREE_NORMAL_ACTIVATION; SATISFIED

PRODUCTION_SOURCE_CHANGE_IN_THIS_TASK: PYTHON_ONLY; JAVA_PRODUCTION_UNCHANGED
DOCUMENTATION_CHANGE_IN_THIS_TASK: CONTRACT_IMPLEMENTATION_EVIDENCE_AND_CROSS_DOCUMENT_NORMATIVE_RECONCILIATION
TEST_EXECUTION_IN_THIS_TASK: PYTHON_AND_JAVA_OFFLINE_TESTS_PASSED; SEE_SECTION_16
JAVA_BUILD_IN_THIS_TASK: CLEAN_FORCED_BUILD_PASSED; SEE_SECTION_16
DEPLOYMENT_IN_THIS_TASK: NOT_PERFORMED
MINECRAFT_RUNTIME_IN_THIS_TASK: NOT_RUN
COMMIT_PUSH_IN_THIS_TASK: NOT_PERFORMED
```

이 문서는 이미 구현된 H5 player-position bulk trusted-destination registration을 LAVI의
Chat과 microphone final transcript에서 한국어로 요청할 수 있게 만드는 후속 Python 계약을
기록한다. 2026-09-04에는 구현 전 계약으로 작성되었고, 2026-09-05 Section 16에 이 계약을
구현하고 offline 검증한 결과를 추가했다. Java production behavior는 변경하지 않았으며 Python
입력·명령 계층과 Python/Java test source만 변경했다.

이 문서가 소유하는 것은 H5 한국어 후보 판정, zero-slot intent, prefixless compilation,
command-specific admission predicate, Chat/microphone 입력 출처 보존과 허용 정책, 사용자 응답의
증거 한계와 해당 테스트 계약이다. 공용 registry metadata/readiness axes와 activation-aware 22 -> 26
catalog truth의 canonical owner는 Python Korean Command Registry Plan이며 이 문서의 표는 H5 row mirror다.
Player anchor, scan volume, container allowlist, double-chest normalization과 repository mutation은
기존 H5 Java 문서가 계속 소유한다.

## 2. Authority and related documents

문서 책임은 다음처럼 나눈다.

| 문서 | 소유 책임 |
|---|---|
| [Manual Trusted Home Storage Direction §13](chatclef-manual-trusted-home-storage-direction-2026-08-27.md#13-trusted-등록-ux와-json) | canonical direct-command grammar, fixed volume, container allowlist, topology와 repository 계약 |
| [H5 Player-Position Anchor Contract](chatclef-h5-player-position-anchor-pre-change-contract-2026-09-04.md) | player anchor acquisition/revalidation, scanner precondition의 scoped override, anchor/result fields와 build/deploy/runtime evidence |
| [Python Korean Command Registry Plan](chatclef-python-korean-command-registry-plan.md) | source-backed command catalog, lifecycle, safety, readiness와 public exposure axes |
| [Python Command Orchestration Plan](chatclef-python-command-orchestration-plan.md) | single-pass translation, exactly-once submission, busy/reconciliation/no-replay와 response evidence |
| [Korean Test Strategy](chatclef-korean-test-strategy.md) | prefix ownership, offline/live test 계층과 artifact authority |
| [Korean Post-Review Merge Blockers](chatclef-korean-post-review-merge-blockers.md) | current catalog/public-exposure blocker와 merge-readiness snapshot |
| [Automatic Deposit Post-Checkpoint Direction](chatclef-automatic-deposit-post-checkpoint-direction-2026-08-31.md) | downstream automatic runtime/harness 경계와 historical H5 evidence |
| [Fabric ChatClef Bridge Protocol V1](fabric-chatclef-bridge-protocol-v1.md) | existing request/result envelope와 transport boundary |
| [Minecraft Backend Separation](minecraft-backend-separation.md) | Fabric-only ownership과 Forge/MineMind 분리 |

이 문서는 위 공용 계약을 바꾸지 않는다. 이 기능에 특화된 grammar, intent, admission과 테스트
vector만 좁게 추가한다. 기존 H6는 `STORE_HOME` natural-language adapter를 소유한다. 이 기능을
`STORE_HOME` H6 분기에 끼워 넣지 않고 H5의 sibling natural-language slice로 유지한다.

## 3. User intent and exact interpretation

사용자 의도는 다음과 같다.

```text
Minecraft에서 직접 입력 가능한 H5 16x16 batch registration을
LAVI Chat과 LAVI microphone의 한국어 명령으로도 실행한다.
```

대표 입력은 다음 의미를 가져야 한다.

```text
auto_deposit_trust area 16x16
auto_deposit_trust 반경 16x16
자동보관등록 영역 16x16
자동보관등록 반경 16x16
캐릭터 주변 16x16 범위의 상자를 자동 보관 대상으로 등록해
현재 위치 기준 반경 16 곱하기 16 상자를 자동 입고 대상으로 등록해
```

이 문서에서 `16x16`은 새 크기 parameter가 아니다. 기존 Java H5가 정한 다음 fixed volume을
호출하는 승인 토큰이다.

```text
X: [playerX - 8, playerX + 8)
Y: [playerY - 8, playerY + 8)
Z: [playerZ - 8, playerZ + 8)
maximum base positions: 16 * 16 * 16 = 4096
```

따라서 한국어 `반경`은 원형 radius, 각 방향 16 block 또는 32x32 footprint를 뜻하지 않는다.
`영역`, `범위`, `반경`, `주변`은 기존 fixed H5 batch form을 선택하는 자연어 alias일 뿐이다.

`모든 상자`는 사용자-facing shorthand다. 정확한 대상은 기존 H5가 지원하는 vanilla chest,
trapped chest와 barrel 보관함/컨테이너다. Shulker, ender chest, furnace 계열, entity inventory와
modded container를 새로 포함하지 않는다.

## 4. Current and target flow

현재 Python 입력은 `MinecraftChatClefInputRouter`에 도달하기 전에 출처가 보존되는 typed 경계로
합류하지 않는다. 실제 current-source 흐름은 다음과 같다.

```text
LAVI Chat
    -> Gradio ChatInterface
    -> LLM.predict_wrapper(raw str)

VoiceInput final transcript
    -> VoiceInput process_input(...)
    -> Input.send_output(raw str)
    -> llm.receive_input(raw str)
    -> LLM queue
    -> LLM.predict_wrapper(raw str)

TwitchChatFetch / YoutubeChatFetch / IdleThink / other loaded Input providers
    -> the same Input.send_output(raw str)
    -> the same llm.receive_input(raw str) and queue

ScreenVision direct callback
    -> llm.receive_input(structured dict with kind/source/observation/text/display_text/
       remember_history=false/metadata/payload)

StarCraft-related direct callback
    -> llm.receive_input(current plugin output object, commonly text)

all of the above
    -> _try_route_external_input(...)
    -> MinecraftChatClefInputRouter
    -> request factory synthesizes source=lavi_chat_mic_router

when the Minecraft extension is unavailable in current production wiring
    -> app wiring installs no input router
    -> an H5-like input can fall through to the ordinary LLM path
```

따라서 현재 `lavi_chat_mic_router`는 신뢰 가능한 물리 입력 출처가 아니라 router가 나중에 합성한
coarse route label이다. 이 값을 allowlist에 넣는 것만으로는 LAVI local Chat과 VoiceInput final
transcript를 Twitch/YouTube/IdleThink/ScreenVision/StarCraft/unknown input에서 분리할 수 없다. 사용자
요구는 local Chat과 microphone만이므로 이 상태에서 persistent R2 mutation을 public-enable하면 안 된다.

현재 H5에 대해 끊긴 경계는 다음과 같다.

```text
input gate: H5 candidate vocabulary 없음
intent enum: H5 semantic intent 없음
deterministic Korean parser: H5 classifier 없음
schema/compiler: H5 zero-slot validation과 serialization 없음
Python registry/admission: auto_deposit_trust public route 없음
catalog snapshot/support matrix: H5 registrar 4 names 없음
```

목표 흐름은 다음과 같다.

```text
trusted local Chat adapter or VoiceInput final-transcript adapter
    -> immutable typed input event
       text
       source = lavi_chat_ui | voice_input_final
       event_id = ingress-generated opaque id
       event_kind = chat_submit | final_transcript
       final = true
       provider_id = trusted producer identity
       fallback_payload = the unchanged original object for ordinary LLM handling
    -> source-preserving direct Chat call or LLM queue
    -> app wiring always installs MinecraftChatClefInputRouter with a nullable extension dependency
    -> shared Minecraft input router receives the same typed event
    -> one typed gate decision from one H5 candidate-detector call
       route_kind = H5_AUTO_DEPOSIT_TRUST | GENERIC | NONE
    -> H5 candidate source/final admission and exact event-id structural validation
    -> raw H5 CR/LF/TAB/control scan before any strip or normalization
    -> route lock and one-shot event claim issues an opaque in-process receipt
    -> H5 candidate failures remain handled even when extension/handler is unavailable
    -> pre-existing reconciliation check before translation
    -> exact trusted @ input adapter, when applicable
       -> preserves original text
       -> produces prefixless translation input
    -> one translation
       -> one authoritative deterministic H5 Korean classification
       -> AUTO_DEPOSIT_TRUST_AREA zero-slot intent
       -> schema validation and compiler
       -> exactly one prefixless command
          auto_deposit_trust area 16x16
    -> translation DTO validation and bridge precheck
    -> submit-once boundary
       -> coordinator extracts command_name from translation.command
       -> H5-specific registry/admission validates the matching claim receipt without consuming it
       -> request source is copied from the trusted event, never from user text
       -> side-effect-free request build without the receipt
       -> atomic receipt revalidation and one-time consumption
       -> immediate existing Fabric adapter/WebSocket submission with no intervening callback or await
    -> Java dispatcher adds the configured ChatClef prefix
    -> existing <configuredPrefix>auto_deposit_trust area 16x16
    -> existing H5 player-position registration operation
```

H5 candidate가 Twitch/YouTube/IdleThink/ScreenVision/StarCraft/unknown source에서 오거나
`final=false`이면 typed source rejection으로 소비하고 translation, submission과 일반 LLM 호출을
모두 0으로 만든다. 완전히 무관한 입력은 출처와 관계없이 기존 일반 대화 경로로 넘긴다. `source`,
`provider_id`, `event_id`, `event_kind`, `final`은 user text, LLM output, thread name 또는 selected/default provider에서
추론하지 않는다.

Typed wrapper는 기존 structured LLM input을 문자열로 축소하지 않는다. Router는 `event.text`만 H5
후보 판정에 읽지만, `GENERIC`/`NONE` 결과에서는 `event.fallback_payload`를 그대로
`response_pipeline.predict()`에 넘긴다. 특히 ScreenVision dict의 `kind`, `source`, `observation`, `text`,
`display_text`, `remember_history=false`, nested `metadata`와 `payload`는 보존되어
`LLMInteractionContext`의 screen-observation 기록과 history 제외 의미가 그대로 유지돼야 한다.

## 5. Scope and non-goals

첫 구현의 production behavior 범위는 다음 Python-owned 경계와 관련 tests/docs로 제한한다. Input
provenance는 H5 공개 전의 필수 선행 범위이며 optional metadata가 아니다. 다만
`BRIDGE_LIFECYCLE_READY`를 추정이 아니라 exact command 경로로 닫기 위한 focused Java integration
test source와 그 clean forced build는 검증 범위에 포함한다. 이는 Java production behavior 변경이
아니다.

```text
input_core/**
    -> immutable input event contract and provider-bound callback adapters
llm_core/** narrow input adapter/queue/predict boundary only
    -> local Chat wrapper stamps lavi_chat_ui; queue preserves the event;
       normal fallback passes fallback_payload unchanged and extracts text only at an existing text-only owner
app_core/** narrow event-listener/router wiring only
    -> connect typed callbacks without inventing or rewriting source
plugins/VoiceInput/voiceInput.py emission boundary only when source evidence requires it
    -> final transcript only; no STT/VAD behavior change
plugins/Minecraft/fabric/chatclef/input/**
plugins/Minecraft/fabric/chatclef/intent/**
plugins/Minecraft/fabric/chatclef/command_registry/**
plugins/Minecraft/fabric/chatclef/extension/minecraft_fabric_chatclef_extension.py narrow signature/delegation hunk only
plugins/Minecraft/fabric/chatclef/extension/natural_language/** narrow coordinator/input/request-factory changes only
plugins/Minecraft/fabric/chatclef/response/** when command-specific wording requires it
tests/minecraft_chatclef/**
plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/**
    -> exact H5 dispatcher/lifecycle integration test and missing English-trio collision registrar test only
plugins/Minecraft/docs/**
plugins/Minecraft/README.md
```

`AppComponentWiringService`는 Minecraft extension의 존재 여부와 관계없이 정확히 하나의
`MinecraftChatClefInputRouter`를 LLM에 설치한다. Extension과 submission handler는 nullable runtime
dependency로 router에 주입한다. `extension=None`인 H5 candidate는
`auto_deposit_trust_extension_unavailable` handled rejection이며 LLM 호출은 0이다. 같은 상태의 비-H5
입력은 기존 normal LLM path를 유지한다. 이 배선 계약은 router unit test가 아니라 app wiring integration
test로도 고정한다. Router module import 또는 construction 실패는 runtime extension-unavailable 상태와
다르다. 이를 catch하고 router 없이 계속 시작하지 말고 typed composition/startup error로 fail closed한다.
따라서 broken router installation에서 H5 text가 ordinary LLM으로 흐르는 상태는 만들지 않는다.

다음은 변경하지 않는다.

- H5 Java form parser, command facade, player anchor, scanner, topology와 repository
- Fabric bridge v1 DTO, schema, message type와 envelope shape
- `adris/**`, AltoClef, Task, TaskRunner, UserTaskChain과 Baritone
- automatic-deposit selection, navigation, GUI open 또는 transfer behavior
- microphone STT engine, model, VAD 또는 transcription behavior
- Forge/MineMind backend 또는 common runtime implementation
- Minecraft/Fabric/Loader/Loom/Gradle/Java/ChatClef/Baritone/Carry On version
- H5 bulk undo, untrust, list natural-language exposure 또는 scheduled execution
- human-readable Minecraft log parsing을 Python success oracle로 사용하는 기능

Java H5 command는 이미 필요한 behavior를 소유하므로 이 자연어 기능을 위해 Java behavior를
변경하지 않는다. Exact count/status를 typed result로 전달하는 기능은 §11의 별도 후속 범위다.

Provider callback은 raw text를 계속 emit할 수 있지만 `Input._sync_provider_listeners()`는 provider별
동일 callback을 공유하지 않는다. Provider descriptor/handle에 결합된 stable adapter callback을 하나씩
보관하고 sync/shutdown 때 그 exact callback을 제거한다. 이 adapter가 user text와 무관하게 source와
provider ID를 stamp한다. Raw `str`이 LLM 또는 router에 직접 들어오는 legacy path는
`source=untrusted_legacy`로 정규화하고 H5 권한을 갖지 않는다. 기존 provider의 일반 비-H5 대화는
text를 추출해 현 LLM path로 계속 전달한다.

## 6. Direct command and natural-language boundary

현재 Java가 지원하는 direct batch form은 다음 네 가지다.

```text
@auto_deposit_trust area 16x16
@auto_deposit_trust 반경 16x16
@자동보관등록 영역 16x16
@자동보관등록 반경 16x16
```

이 form은 Minecraft/ChatClef direct command entrypoint의 grammar다. Python natural-language
compiler output이 아니다.

사용자가 예시로 든 다음 두 문자열은 신뢰된 `lavi_chat_ui` 또는 `voice_input_final` event에서만
별도 exact whole-string allowlist로 수용한다.

```text
@auto_deposit_trust area 16x16
@auto_deposit_trust 반경 16x16
```

이 lane은 raw command passthrough가 아니다. Immutable raw `event.text`에서 CR/LF, TAB과 모든 control
character를 trim/normalization보다 먼저 검사해 하나라도 있으면 거절한다. 그 뒤 ASCII space outer
trim만 허용하고
문자열 전체가 둘 중 하나와 일치하며 suffix, extra option과 두 번째 command가 없을 때만 H5 fixed
semantic으로 판정한다. Wire에는 `@`를 제거한 canonical prefixless command만 보낸다. `@자동보관등록 ...`을 포함한 다른
literal direct form은 Minecraft/ChatClef direct-command entrypoint에만 남는다.

Python natural-language 안전 경계는 먼저 immutable raw text의 CR/LF, TAB과 모든 control character를
검사한다. 이 검사를 통과한 뒤에만 위 두 exact whole-string form을 분리하고, 나머지 lane의 `@`, `;`,
`#`, quote와 injection marker를 거절한다. 공용 dangerous-text 검사를 완화하지 않는다.
모든 승인된 exact/natural-language/spacing/STT 변형은 다음 ASCII prefixless command 하나로
정규화한다.

```text
auto_deposit_trust area 16x16
```

Java `FabricChatClefCommandDispatcher.normalizeCommand()`가 configured prefix를 붙여 기존 direct
command로 실행한다. Default/current test configuration의 prefix는 `@`지만 runtime evidence는 먼저
실제 configured prefix를 capture한다. `반경` 또는 Korean direct alias를 outbound wire command로
사용하지 않는다.
이렇게 해야 alias/encoding 분기 없이 한 compiler result와 한 bridge test vector만 유지할 수 있다.

Allowlist 밖의 H5-like literal `@` 입력은 outer candidate gate가 잡은 뒤
`dangerous_command_slot` handled rejection으로 소비해 translation, submission과 일반 LLM 호출을
모두 0으로 만든다. 입력 본문이 `source=lavi_chat_ui` 같은 문자열을 포함해도 출처가 승격되지 않는다.

현재 `ChatClefNaturalLanguageService`는 extractor보다 먼저 raw text의 `@`를 거절한다. 따라서 exact
allowlist 구현은 그 공용 dangerous-text 검사를 완화하거나 classifier에 도달하기를 기대하지 않는다.
Trusted source/final admission과 event-ID structural validation, raw control scan, event claim을 그
순서로 마친 뒤 service 호출 전에 별도
pre-translation adapter가 위 두 raw
whole-string만 `auto_deposit_trust area 16x16`으로 바꾼다. `original_text`는 raw form을 보존하고 실제
service input은 `metadata.natural_language.translation_input_text`로 별도 기록한다. Adapter는 문자열만
반환하지 않고 immutable `{original_text, translation_input_text}` result를 반환한다. Translation boundary는
두 번째 값을 사용하고 request factory는 첫 번째 값을 기존 `original_text` field에 넣어 기존 의미를
바꾸지 않는다.

## 7. Deterministic Korean classification

### 7.1 Positive conditions

실행 가능한 입력은 두 종류로 제한한다.

첫째, 신뢰된 LAVI Chat/VoiceInput final event에서 사용할 수 있는 exact shorthand는 다음 여섯 form뿐이다.

```text
auto_deposit_trust area 16x16
auto_deposit_trust 반경 16x16
자동보관등록 영역 16x16
자동보관등록 반경 16x16
@auto_deposit_trust area 16x16
@auto_deposit_trust 반경 16x16
```

둘째, 자연어 문장은 다음 의미가 모두 명확할 때만 실행 가능한 분류를 만든다.

```text
automatic/trusted storage purpose
+ nearby/area/range meaning
+ container/chest target meaning
+ explicit registration imperative
+ exact fixed 16x16 size token
+ current execution request
```

Initial positive examples:

| Input | Decision |
|---|---|
| `auto_deposit_trust area 16x16` | executable exact shorthand |
| `auto_deposit_trust 반경 16x16` | executable exact shorthand |
| `자동보관등록 영역 16x16` | executable exact shorthand |
| `자동보관등록 반경 16x16` | executable exact shorthand |
| `@auto_deposit_trust area 16x16` | executable exact whole-string allowlist; compiles without `@` |
| `@auto_deposit_trust 반경 16x16` | executable exact whole-string allowlist; compiles without `@` |
| `캐릭터 주변 16x16 범위의 상자를 자동 보관 대상으로 등록해` | executable |
| `현재 위치 기준 반경 16 곱하기 16 상자를 자동 입고 대상으로 등록해` | executable |
| `주변 16 X 16 상자 전부 자동보관 등록해 주세요` | executable |

공백, 존댓말과 microphone식 무구두점은 의미를 바꾸지 않는다. Size normalization은 명시적인
allowlist만 사용한다. 다만 raw text의 `?`, `？`, 질문형 종결, 부정, 유예와 compound marker는
punctuation/spacing normalization보다 먼저 검사한다. Normalizer가 `등록해?`를 `등록해`로 바꾸어
실행형으로 승격시키면 안 된다.

Execution normalizer는 전역 NFKC/casefold를 적용하거나 punctuation을 삭제하지 않는다. 전각
영문·숫자, 원문자 숫자와 shorthand 내부 punctuation은 exact form 또는 allowlisted size token으로
승격하지 않는다. Candidate detector만 NFKC를 detection-only로 사용할 수 있으며, 그 목적은
H5-like obfuscation을 일반 LLM으로 보내지 않고 guarded rejection lane에 남기는 것이다. 자연어 문장의
종결 `,`, `.`, `!`, `，`, `。`, `！`만 closed execution grammar가 명시적으로 허용한다. 실행용 spacing
정규화는 outer edge, token 사이와 `16 X 16`/`16 곱하기 16` size token 내부 모두 ASCII space만 다루며
NBSP, thin space, narrow no-break space, ideographic space 등 Unicode `Zs` separator를 ASCII 공백으로
승격하지 않는다.

```text
16x16
16 X 16
16×16
16 곱하기 16
```

`십육 곱하기 십육` 같은 한글 수사는 실제 production microphone transcript fixture에서 관찰한
뒤 exact test vector와 함께 추가한다. 임의 숫자 word parser나 범용 산술 parser를 만들지 않는다.

### 7.2 Guarded no-submit decisions

다음 H5 candidate는 실행하지 않고 typed rejection으로 소비한다.

| Input class | Example | Submission |
|---|---|---:|
| negation | `주변 상자를 자동 보관 대상으로 등록하지 마` | 0 |
| question | `주변 16x16 상자를 등록할까?` | 0 |
| hypothetical | `주변 상자를 등록하면 되나` | 0 |
| deferred | `나중에 주변 16x16 상자를 등록해` | 0 |
| compound | `주변 상자 등록하고 다이아 캐 와` | 0 |
| unsupported size | `주변 8x8 상자를 등록해` | 0 |
| mixed size | `주변 16x8 상자를 등록해` | 0 |
| extra option/number | `주변 16x16 상자 5개만 등록해` | 0 |
| missing fixed size | `주변 상자를 자동 보관 대상으로 등록해` | 0 |
| ambiguous purpose | `상자 등록해` | 0 |

부정, 질문, 가정, 유예와 복합 판정은 positive keyword보다 우선한다. H5 candidate인데 정보가
불완전한 문장을 일반 LLM이나 generic `DEPOSIT_ITEM`으로 넘기지 않는다. 부분 실행도 하지 않는다.
`주변에 상자가 많네`처럼 automatic/trusted registration 의도와 실행 동사가 없는 일반 대화는
`NO_MATCH`이며 기존 normal LLM path로 넘긴다.

`AutoDepositTrustIntentDecision`의 exact enum/value contract는 다음과 같다. 의미를 하나의 boolean으로
축소하거나 구현 시 이름을 다시 선택하지 않는다.

```text
NO_MATCH = "no_match"
AUTO_DEPOSIT_TRUST_AREA = "auto_deposit_trust_area"
NEGATED = "negated"
QUESTION = "question"
DEFERRED = "deferred"
AMBIGUOUS = "ambiguous"
AMBIGUOUS_COMPOUND = "ambiguous_compound"
UNSUPPORTED_SIZE = "unsupported_size"
MALFORMED = "malformed"
```

Guarded decision의 canonical reason code는 다음과 같다.

```text
NEGATED             -> auto_deposit_trust_area_negated
QUESTION            -> auto_deposit_trust_area_question
DEFERRED            -> auto_deposit_trust_area_deferred
AMBIGUOUS           -> auto_deposit_trust_area_ambiguous
AMBIGUOUS_COMPOUND  -> auto_deposit_trust_area_ambiguous_compound
UNSUPPORTED_SIZE    -> auto_deposit_trust_area_unsupported_size
MALFORMED           -> auto_deposit_trust_area_malformed
```

`NO_MATCH`에는 rejection reason을 만들지 않고 기존 parser 순서로 진행한다.
`AUTO_DEPOSIT_TRUST_AREA`만 executable이다. Guarded response는 실행하지 않았다는 사실과 reason만
말하며 등록·제출·완료를 암시하지 않는다.

## 8. Intent, schema, compiler and LLM authority

새 semantic intent 이름은 다음처럼 behavior를 직접 드러낸다.

```text
ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA
```

이 executable intent는 zero-slot이다.

```text
intent_type: auto_deposit_trust_area
source: rule
confidence: 1.0
item_phrase: ""
quantity: null
food_units: null
player_name: ""
x: null
y: null
z: null
slots: {}
```

`16x16`, `area`와 command name은 user-controlled slot으로 전달하지 않는다. Compiler-owned constant로
다음 exact string만 생성한다.

```text
auto_deposit_trust area 16x16
```

Schema는 executable intent에 대해 `source=rule`과 모든 item/quantity/player/coordinate/extra slot의
부재를 요구한다. Translation validator는 intent와 exact compiled command가 일치하는지 다시 검증한다.
Original text, slot 또는 compiled command에서 command injection이 발견되면 submission은 0이다.

Guarded classification은 executable H5 intent로 위장하지 않는다. 기존 `STORE_HOME` 선례와 같은
fail-closed transport를 사용한다.

```text
intent_type: UNKNOWN
source: rule_auto_deposit_trust_guard
slots:
  auto_deposit_trust_guarded: true
  auto_deposit_trust_decision: <exact guarded decision value>
  reason_code: <canonical reason code>
  message: <bounded non-execution response>
```

`AutoDepositTrustGuardFields`는 다음 constant만 소유한다.

```text
GUARD_SOURCE = "rule_auto_deposit_trust_guard"
GUARD_SLOT = "auto_deposit_trust_guarded"
DECISION_SLOT = "auto_deposit_trust_decision"
REASON_SLOT = "reason_code"
MESSAGE_SLOT = "message"
```

`KoreanChatClefRuleParser`는 candidate classification을 한 번만 받아 executable이면 zero-slot intent,
guarded이면 encoder로 위 UNKNOWN guard intent를 만든다. `CompositeChatClefIntentExtractor`는 marker
detector로 guard source 또는 guard slot이 하나라도 보이면 payload가 malformed여도 LLM fallback 전에
그대로 보존한다. `ChatClefNaturalLanguageService`는 generic UNKNOWN 처리 전에 decoder로 exact guard
source/slots를 검증한다. Valid guard는 canonical reason의 typed `INVALID` translation rejection,
invalid/contradictory guard marker는 `auto_deposit_trust_area_malformed_guard` rejection으로 소비한다.
어느 경우도 executable H5 intent나 일반 LLM으로 복구하지 않는다.

이 persistent registry mutation intent를 LLM이 생성할 권한은 없다.

```text
deterministic rule creates AUTO_DEPOSIT_TRUST_AREA: allowed
LLM payload requests AUTO_DEPOSIT_TRUST_AREA: rejected
LLM output is coerced into the intent after parsing: rejected
guarded deterministic candidate falls through to LLM: forbidden
```

Parser priority는 최소한 다음 관계를 보존한다.

```text
explicit control and global guard classification
-> H5 auto-deposit trust area registration
-> STORE_HOME
-> generic GIVE / DEPOSIT / EQUIP
-> GET and remaining intents
-> UNKNOWN
```

이 순서는 `자동 보관 대상으로 등록`이 item `DEPOSIT_ITEM` 또는 집 전체 `STORE_HOME`으로 잘못
분류되는 것을 막는다.

## 9. Responsibility and package direction

한 class에 input provenance, normalization, candidate detection, final grammar classification,
rejection contract와 compiler를 합치지 않는다. 둘 이상의 변경 이유를 갖는 타입은 구현 전에
분리하고, 새 LAVI-owned Python responsibility는 다음 하위 package에서 한 primary type/file 단위로
폴더화한다.

```text
input_core/input_event/
    __init__.py
    contracts/
        __init__.py
        lavi_input_event.py
            -> frozen envelope for text/source/event_id/event_kind/final/provider_id plus opaque fallback_payload
    provenance/
        __init__.py
        lavi_input_source.py
            -> canonical source constants only
        input_provider_source_resolver.py
            -> trusted Provider descriptor ID to immutable source/provider_id/event_kind/final policy only
    adapters/
        __init__.py
        provider_bound_input_event_adapter.py
            -> stable provider callback identity, trusted policy stamping and event ID creation only
        local_chat_input_event_adapter.py
            -> local Chat event creation and trusted provenance stamping only
        direct_callback_input_event_adapter.py
            -> ScreenVision/StarCraft callback identity stamping while preserving opaque fallback payload only
    normalization/
        __init__.py
        lavi_input_event_normalizer.py
            -> typed-event validation; raw/unknown object becomes untrusted_legacy without losing fallback payload
```

```text
llm_core/chat_input/
    __init__.py
    local_chat_prediction_entrypoint.py
        -> typed local Chat event to LLM generator delegation only
    local_chat_interface_factory.py
        -> validated Gradio streaming callback registration only
```

`Input`은 provider별 adapter callback을 보관·동기화·해제하고, queue는 immutable event를 변경 없이
운반한다. `LLM.predict_wrapper()`는 router에 event 전체를 전달한 뒤 normal LLM fallback에서는
`event.fallback_payload`를 원래 object 그대로 넘긴다. Local Gradio Chat은 raw `predict_wrapper`에 직접
연결하지 않고 전용 adapter를 사용한다. ScreenVision/StarCraft direct wiring도 source-bound adapter를
사용하되 non-H5 fallback payload를 재구성하거나 문자열화하지 않는다. `InputPluginInterface`, individual
Twitch/YouTube/Idle provider와 wire DTO를 provenance policy owner로 만들지 않는다.

Trusted policy는 adapter가 bind한 provider descriptor ID에서
`source/provider_id/event_kind/final` 네 필드를 한 번에 만든다. Provider output object가 같은 이름의
field를 포함해도 authorization field로 복사하거나 승격하지 않는다. Adapter는 새 ingress `event_id`를
생성하고 original object를 opaque `fallback_payload`로만 보존한다. Unknown/raw caller는
`source=untrusted_legacy`, `provider_id=unknown`, `event_kind=legacy`, `final=false`로 정규화되며 payload
안의 `source`, `provider_id`, `event_kind`, `final` 또는 `event_id` 문자열로 trusted tuple을 위조할 수 없다.

Ingress `event_id`의 exact representation은 built-in `str` 하나이며 adapter가
`secrets.token_hex(16)`으로 생성한 32-character lowercase ASCII hexadecimal 값이다. 즉 accepted form은
`^[0-9a-f]{32}$` 하나뿐이고 trim, case-fold, coercion 또는 provider-supplied ID reuse를 하지 않는다.
`None`, empty/whitespace, leading/trailing whitespace, uppercase, wrong-length, non-hex, control-character와
non-string/list/mapping 값은 claim registry에 넣기 전에
`auto_deposit_trust_input_provenance_invalid`로 거절한다. Valid ID 충돌은 새 ID로 고치거나 덮어쓰지 않고
duplicate event로 fail closed한다. Event ID 자체는 authorization이 아니며 matching opaque receipt가
별도로 필요하다.

```text
plugins/Minecraft/fabric/chatclef/intent/auto_deposit_trust/
    __init__.py
    contracts/
        __init__.py
            -> explicit contract exports; not an empty compatibility file
        auto_deposit_trust_intent_decision.py
            -> decision enum only
        auto_deposit_trust_intent_classification.py
            -> immutable classification and guarded metadata only
    normalization/
        __init__.py
            -> explicit normalizer export
        auto_deposit_trust_input_normalizer.py
            -> raw guard scan 뒤 allowlisted spacing and size-token normalization only; no NFKC or punctuation deletion
    candidate/
        __init__.py
            -> explicit detector export
        auto_deposit_trust_candidate_detector.py
            -> total, side-effect-free coarse H5 candidate boolean only
    classification/
        __init__.py
            -> explicit classifier export
        korean_auto_deposit_trust_intent_classifier.py
            -> deterministic final semantic decision only
    guard/
        __init__.py
            -> explicit guard-contract exports
        auto_deposit_trust_guard_fields.py
            -> guard source와 slot-name constants only
        auto_deposit_trust_guard_decode_result.py
            -> immutable NOT_GUARD / VALID / INVALID decode result only
        auto_deposit_trust_guard_marker_detector.py
            -> source/slot marker presence check only
        auto_deposit_trust_guard_intent_encoder.py
            -> guarded classification to UNKNOWN intent encoding only
        auto_deposit_trust_guard_intent_decoder.py
            -> exact guarded UNKNOWN validation and decode only
```

Candidate detector와 final classifier는 normalizer를 composition으로 사용한다. Candidate detector는
router가 validated event에서 꺼낸 string input에 대해 exception을 밖으로 내보내지 않는 total
boundary이며 final decision을 만들지 않는다. Classifier는 raw question/negation/deferred/compound
evidence를 normalization 전에 판정하고 router/LLM/submission을 호출하지 않는다.
각 `__init__.py`는 명시적 public export만 담고 빈 compatibility facade로 남기지 않는다.

Existing owners receive only narrow delegation or new intent branches.

```text
plugins/Minecraft/fabric/chatclef/input/gating/
    __init__.py
    contracts/
        __init__.py
        minecraft_chatclef_input_route_kind.py
            -> NONE / GENERIC / H5_AUTO_DEPOSIT_TRUST enum only
        minecraft_chatclef_input_gate_decision.py
            -> immutable consider/route_kind result only
    minecraft_chatclef_input_intent_gate.py
        -> delegates to AutoDepositTrustCandidateDetector exactly once
        -> returns typed gate decision; does not duplicate H5 regex or vocabulary

MinecraftChatClefInputRouter
    -> reuses the typed gate decision for source admission and every early return
    -> does not call the H5 detector a second time
    -> after H5 route identity is known, converts every route-local exception to a handled typed failure
    -> keeps existing GENERIC/NONE behavior outside this feature

KoreanChatClefRuleParser
    -> delegates to KoreanAutoDepositTrustIntentClassifier before generic storage/item rules
    -> maps executable classification to one zero-slot intent
    -> delegates guarded UNKNOWN serialization to AutoDepositTrustGuardIntentEncoder

CompositeChatClefIntentExtractor / ChatClefNaturalLanguageService
    -> marker detector preserves every guard marker before LLM
    -> decoder validates it before generic UNKNOWN
    -> does not invoke LLM fallback for H5 candidates

ChatClefLLMIntentExtractor
    -> explicitly denies H5 intent authority

ChatClefIntentSchemaValidator
    -> owns rule-only and zero-slot validation

ChatClefCommandCompiler
    -> owns one exact prefixless command string
```

현재 flat module인
`plugins/Minecraft/fabric/chatclef/input/minecraft_chatclef_input_intent_gate.py`는 위
`input/gating/minecraft_chatclef_input_intent_gate.py`로 이동한다. 모든 repository import와 test patch
target을 새 경로로 갱신한 뒤 old flat file은 삭제한다. 빈 compatibility file은 남기지 않는다. 외부
import 호환성이 실제로 source-backed requirement로 확인될 때만 deprecated re-export와 제거 시점을 담은
non-empty facade를 별도 승인 범위로 둔다.

H5 candidate의 typed provenance와 one-consumer claim은 semantic classifier나 generic LLM facade에
섞지 않는다.

```text
plugins/Minecraft/fabric/chatclef/input/auto_deposit_trust/
    __init__.py
    admission/
        __init__.py
        auto_deposit_trust_input_admission_decision.py
            -> immutable allow/reason/message result only
        auto_deposit_trust_input_admission.py
            -> exact source/provider/event_kind/final tuple validation only
    delivery/
        __init__.py
        contracts/
            __init__.py
            auto_deposit_trust_input_claim_receipt.py
                -> opaque event/source-bound capability value only
        auto_deposit_trust_input_event_claim_registry.py
            -> process-lifetime H5 event-id reservation, receipt state and duplicate refusal only
    safety/
        __init__.py
        auto_deposit_trust_raw_input_safety.py
            -> CR/LF/control scan on immutable raw event.text only; does not reject @
    exact_input/
        __init__.py
        auto_deposit_trust_exact_input_adaptation.py
            -> immutable original_text/translation_input_text result only
        auto_deposit_trust_exact_input_adapter.py
            -> two trusted @ whole-string forms to one adaptation result only
```

Exact trusted tuples are 다음 두 개뿐이다.

```text
source=lavi_chat_ui, provider_id=lavi_chat_ui, event_kind=chat_submit, final=true
source=voice_input_final, provider_id=VoiceInput, event_kind=final_transcript, final=true
```

Claim registry는 trusted tuple/event-ID admission과 raw CR/LF/TAB/control scan을 통과한 H5 candidate의
valid `event_id`를 route lock 안에서 translation 전에 한 번 reserve하고 receipt를 발급한다. 이 event-ID reservation과 뒤의 submission
receipt consumption은 서로 다른 상태 전이 단계다. Untrusted/invalid source는 claim count를 늘리지 않는다. 같은
event ID가 queue/router에 다시 전달되면 translation 0, submission 0이다. Registry는 process lifetime에
accepted ID를 evict하지 않으며 exact hard capacity는 4096 IDs다. Capacity에 도달하면 오래된 ID를
버려 재실행 가능하게 만들지 않고 새 H5 candidate를 fail closed한다. Provider가 동일한 발화를 별도
callback으로 두 번 emit해 서로 다른 event ID를 만든 경우는 서로 다른 사용자 입력으로 취급하며
text/time heuristic으로 합치지 않는다.

Claim은 registry 내부 lock에서 atomic해야 한다. `capacity - 1`과 `capacity`번째 unique ID는 claim되고,
그 뒤 unique ID는 capacity rejection이다. Claimed IDs는 app shutdown 때 registry object가 폐기되고 새
process/runtime composition이 만들어질 때만 reset한다. Runtime 중 clear/reload/reconnect가 claim을
지우면 안 된다.

Successful claim은 `event_id`, trusted source와 owning registry instance에 결합된 opaque receipt를 한 번만
발급한다. Receipt는 object identity와 registry-held nonce/state로 검증하며 string, mapping 또는
`metadata.input_event` field로 재구성할 수 없다. Router는 이를 별도 in-process keyword/context로
submission boundary까지 운반한다. Receipt를 translation DTO, `CommandRequestDTO`, JSON, logs 또는 wire
metadata에 넣지 않는다. Admission inspection은 같은 event/source/registry binding과 `ISSUED` state를
non-consuming 방식으로 확인한다. Side-effect-free request build가 끝난 뒤 command submitter 호출 직전에
같은 binding과 state를 registry lock 안에서 다시 검증하고 receipt를 `SPENT`로 원자 전이한다. 성공한
commit과 command submitter 호출 사이에는 await, callback 또는 외부 user code를 두지 않는다. Missing,
forged, mismatched 또는 spent receipt는 submit 0이다. Coordinator-owned request build 또는 commit 전 단계가 실패하면
coordinator는 receipt를 원자적으로 abandon해 `ISSUED -> SPENT`로 전이하고 다시 제출 가능한 상태로 남기지
않으며, event-ID reservation도 process lifetime 동안 유지한다. Commit 뒤 submit 결과가 예외/`UNKNOWN`이어도 receipt는 spent이고
retry/replay는 0이다.

`MinecraftFabricChatClefExtension` composition은 process/runtime마다 정확히 하나의
`AutoDepositTrustInputEventClaimRegistry`를 생성해 H5 admission/coordinator에 constructor-inject하고 narrow
read-only accessor로 router wiring에 제공한다. `AppComponentWiringService`는 extension이 있을 때 그 exact
instance를 router에 주입하며 별도 registry를 만들지 않는다. Extension이 없는 router는 submitter와
공유되지 않는 reject-only local registry를 사용할 수 있지만 receipt를 command submission으로 전달할 수
없다. Static/global registry는 금지한다. Object-graph test는 router, extension coordinator와 H5 authorizer가
같은 registry object를 `is` identity로 공유하는지 검증한다.

Claim 전 raw safety owner는 exact `@` form과 자연어 form을 구분하기 전에 immutable
`event.text`의 CR/LF, TAB, 모든 control character와 ASCII space가 아닌 Unicode `Zs` separator를
검사한다. 이 단계는 `@`를 거절하지 않으며 실패 시
claim count도 늘리지 않는다. 그다음 route lock 안에서 claim을 발급하고 exact adapter가 두 whole-string
form만 canonical prefixless text로 바꾸며, exact match가 아닌 lane은 기존 full
dangerous validator에서 `@`, `;`, `#`, quote와 injection marker를 거절한다. CR/LF/control evidence는
outer ASCII-space trim, exact-adapter handling 또는 spacing normalization
`NaturalLanguageCommandInput.from_text()` 호출보다 먼저 거절한다. H5 route
identity가 확정된 뒤 source/final/event-ID admission, raw safety, claim, extension/handler와
reconciliation 준비, exact adapter 또는 translation 준비에서
예외가 발생하면 router가 이를 catch해 handled failure로 소비하며 `_try_route_external_input()` 밖으로
예외를 내보내 일반 LLM fallback을 열지 않는다.

Pre-translation input rejection reason은 다음으로 고정한다.

```text
auto_deposit_trust_input_source_not_allowed
auto_deposit_trust_input_not_final
auto_deposit_trust_input_provenance_invalid
auto_deposit_trust_input_raw_control_not_allowed
auto_deposit_trust_duplicate_input_event
auto_deposit_trust_input_event_capacity_exhausted
auto_deposit_trust_extension_unavailable
auto_deposit_trust_handler_unavailable
auto_deposit_trust_input_internal_error
```

이 rejection은 handled이며 LLM call, translation과 submission이 모두 0이다.

Command-specific source/readiness admission은 generic registry lookup에 숨기지 않는다. `STORE_HOME`
admission precedent처럼 H5-specific admission collaborator를 사용한다.

```text
plugins/Minecraft/fabric/chatclef/command_registry/admission/
    auto_deposit_trust/
        __init__.py
        auto_deposit_trust_command_admission_decision.py
            -> immutable allow/reason/message result only
        auto_deposit_trust_command_admission.py
            -> composes source/readiness and claimed-submission decisions only
        auto_deposit_trust_source_readiness_admission.py
            -> request source, H5 readiness and public-exposure predicate only
        auto_deposit_trust_claimed_submission_authorizer.py
            -> non-consuming receipt inspection, atomic commit and abandon only
```

`KoreanCommandSubmissionAdmission`은 registry lookup 뒤 command-specific delegate가 있는지를 먼저
판정한다. H5는 이 collaborator가 request source를 먼저 검사한 뒤 H5 readiness/public predicate를
검사하므로 아래 `auto_deposit_trust_source_not_allowed` reason이 실제로 도달 가능하다. `STORE_HOME`도
기존 delegate와 `store_home_source_not_allowed` reason을 그대로 보존한다. 나머지 public command에는
generic `allowed_input_sources` 검사를 적용한다. Specialized admission이 allow를 반환한 뒤에도 shared
membership invariant를 방어적으로 확인하되, 실패 reason의 owner를 generic으로 바꾸지 않는다.

`KoreanCommandSubmissionAdmission.inspect()`의 target API는 caller mapping에서 claim metadata를 읽지
않고 router가 별도 전달한 opaque claim context를 optional argument로 받는다. 이 inspection은 H5
source/readiness와 receipt validity를 검사하지만 receipt를 소비하지 않는다. Inspection 시점에 receipt가
없거나 invalid/spent이면 request factory와 command submitter에 진입하지 않는다. Inspection 뒤의
동시 consume은 아래 commit에서 다시 검출한다.
따라서 `extension.handle_natural_language_command({"text": ..., "source": "lavi_chat_ui"})`, forged
`submit_translated_command()` mapping, Fabric Korean UI 또는 arbitrary internal caller가 source/metadata
문자열을 흉내 내도 H5를 실행할 수 없다. Existing non-H5 direct extension/UI behavior는 그대로 둔다.

Exact call path는 router submission boundary ->
`MinecraftFabricChatClefExtension.submit_translated_command(..., route_claim=...)` ->
`NaturalLanguageCommandCoordinator.submit_translated(..., route_claim=...)` -> H5 admission/authorizer다.
`route_claim`은 keyword-only in-process object이며 `NaturalLanguageCommandInput` mapping helper가 읽거나
만들지 않는다. Coordinator는 admission inspection을 통과한 뒤 receipt를 넘기지 않고
`TranslatedCommandRequestFactory`를 호출한다. Factory는 reserved H5 metadata인 `original_text`,
`translation_input_text`, validated translation과 input-event audit fields를 trusted route context에서
merge하고 caller metadata가 덮어쓰지 못하게 한다. Receipt 자체는 factory 입력 metadata에서 제거된
상태이며 `CommandRequestDTO`로 전달하지 않는다. Factory 성공 뒤 coordinator는 admission/authorizer의
commit API로 같은 receipt와 event/source/command binding을 lock 안에서 재검증하고 한 번 소비한다.
Commit 성공 직후에만 `_command_submitter(request)`를 동기 호출한다. Factory exception이면 abandon,
commit rejection이면 submit 0, commit 뒤 submitter exception이면 spent 유지와 retry 0이다. Direct extension
APIs가 route claim 없이 기존 signature로 호출되는 behavior는 non-H5에만 유지되고 H5 submit은 fail closed한다.

현재 coordinator의 admission -> request factory -> command submitter 순서를 보존하면서 two-phase
authorization을 다음처럼 삽입한다. `inspection`과 `route_claim`은 모두 in-process 전용이고 DTO가 아니다.

```text
try:
    inspection = admission.inspect(command, translation, route_claim)  # non-consuming
    if inspection rejected: return handled rejection; request build 0; submit 0

    try:
        request = request_factory.build(command, translation, trusted_route_context)
    except:
        return handled failure; submit 0

    commit = admission.commit(inspection, route_claim, request identity)  # locked recheck + SPENT
    if commit rejected: return handled rejection; submit 0

    try:
        return command_submitter(request)  # immediate synchronous call, no intervening callback/await
    except:
        return UNKNOWN; submit attempt 1; retry/replay 0
finally:
    admission.abandon_if_issued(route_claim)  # idempotent; ISSUED -> SPENT, committed SPENT -> no-op
```

Concurrent inspection이 둘 이상 provisional allow를 얻더라도 `commit`과 `abandon`은 같은 registry lock과
receipt state를 사용한다. 따라서 전체 경쟁에서 `ISSUED -> SPENT` 승자는 하나이고 command submitter 진입도
최대 하나다. Factory/commit 실패는 retry를 만들지 않으며 commit 뒤 발생한 submitter 예외는 outcome을
`UNKNOWN`으로 다룰 수 있지만 receipt를 되살리지 않는다.

Receipt lifecycle owner는 no-submit branch를 빠뜨리지 않는다. Router는 claim 발급부터 extension 호출
handoff까지 receipt를 소유하고, coordinator는 `submit_translated(..., route_claim=...)` 진입부터 return까지
소유한다. 두 owner 모두 자신의 `finally`에서 idempotent/no-throw `abandon_if_issued()`를 호출한다. 따라서
extension/handler unavailable, reconciliation/busy/quarantine, exact adapter/translation/DTO validation,
precheck/admission rejection 또는 exception, request factory failure, commit rejection/exception 중 어느 경로도
`ISSUED` receipt를 남기지 않는다. Commit이 이미 성공했거나 다른 경쟁자가 먼저 spend했다면 abandon은
상태를 바꾸지 않는 no-op이다. Coordinator commit API는 typed no-throw decision을 반환하고, 예상 밖 내부
예외도 finally abandon과 handled failure, submit 0으로 닫는다. Successful commit 뒤 submitter
exception/`UNKNOWN`에서는 이미 `SPENT`이므로 abandon은 no-op이고 retry/replay는 0이다.

이 source migration은 현재 coarse `lavi_chat_mic_router`를 실제 ingress source로 바꾸므로 기존 public
Korean command와 `STORE_HOME`도 local Chat/microphone behavior를 유지하도록 다음 exact source tuple로
함께 이관한다.

```text
lavi_chat_ui
voice_input_final
direct_typed  # only for the already separate direct-typed entrypoint
lavi_gui_korean  # preserve the existing Fabric ChatClef Korean UI for commands that already admit it
```

H5는 사용자 요구대로 앞의 두 source만 허용한다. 기존 public Korean command는 generic source
enforcement를 새로 켰을 때 기존 Fabric ChatClef Korean UI behavior가 사라지지 않도록
`lavi_gui_korean`을 해당 command의 registry tuple에 명시한다. `STORE_HOME`은 현재 이 UI source를
admit하지 않으므로 이를 새로 추가하지 않고 `lavi_chat_ui`, `voice_input_final`, `direct_typed`만으로
coarse source를 교체한다. Natural-language router request에는 앞의 두 값을 사용하고, `metadata.input_route`가
`minecraft_fabric_chatclef` router provenance를 계속 소유한다. `CommandRequestDTO.source`는 기존
string field를 사용하므로 common DTO, v1 schema와 Java payload shape는 바꾸지 않는다. Raw input은
`untrusted_legacy`이고 allowlist에 들어가지 않는다.

여기서 “기존 authorized behavior”는 local Chat, VoiceInput final, 이미 존재하는 `direct_typed`와
command별 기존 Fabric Korean UI만 뜻한다. 현재 raw-string collapse 때문에 우연히 가능했던 Twitch,
YouTube, IdleThink, ScreenVision, StarCraft 또는 unknown-source의 public Korean Minecraft auto-submit은
generic source enforcement 이후 의도적으로 차단된다. 이는 H5 한 row만의 변화가 아니라 기존 public
Korean command의 입력 보안 경계를 좁히는 compatibility change이므로 source별 regression matrix와
release note를 같은 구현 단위에 포함한다. Ordinary non-command LLM 대화는 차단하지 않는다.

H5 command-specific admission reason set은 다음 다섯 개로 고정한다.

```text
auto_deposit_trust_source_not_allowed
auto_deposit_trust_input_claim_required
auto_deposit_trust_input_claim_invalid
auto_deposit_trust_python_admission_not_ready
auto_deposit_trust_public_readiness_incomplete
```

마지막 reason은 public flag가 true인데 `BRIDGE_LIFECYCLE_READY`가 false일 때 사용한다.
`GAMEPLAY_EFFECT_VERIFIABLE=false`는 ACK-only 응답을 강제하지만 §10의 staged rollout 자체를 막는
admission predicate로 사용하지 않는다. Public flag가 false인 나머지 경우에는 기존 generic reason
`korean_command_not_public`을 사용한다.

Response interpretation과 wording이 둘 이상의 독립 분기를 갖게 되면
`response/auto_deposit_trust/` 아래 별도 validator/renderer로 분리한다. Submission ACK 한 문장만
필요하면 기존 generic renderer의 작은 intent label branch를 사용할 수 있으며 불필요한 hierarchy나
manager layer는 만들지 않는다.

## 10. Registry, catalog and admission contract

2026-09-04 pre-implementation snapshot의 production Python registry, registered-command snapshot과
support matrix는 22개 command를 기준으로 했다. Current Java source의 별도
`AutoDepositTrustedCommandRegistrar`는 다음 네
command name의 등록을 선언하고 시도한다. Collision-free normal activation path에서는 네 이름이
모두 H5 owner로 등록된다. Collision behavior는 atomic all-four rollback이 아니며 다음 두 branch로
구분한다.

```text
any English trio collision (trust/untrust/list):
  English trio registration all refused
  Korean alias registration not attempted

Korean alias collision after collision-free English trio:
  English trio remains registered by the H5 registrar
  Korean alias registration alone is refused
  pre-existing Korean alias and its owner remain
```

따라서 actual runtime effective set과 owner는 별도 runtime evidence 대상이고, 이름 수 26만으로 ownership
성공을 주장할 수 없다.

이 current-source 판정의 exact baseline은
`f301770d1885f8d7239846a97b187f4d51c05a55`이다.

```text
auto_deposit_trust
auto_deposit_untrust
auto_deposit_trusted_list
자동보관등록
```

2026-09-05 구현 뒤 현재 상태는 다음처럼 기록한다.

```text
Python registry/snapshot/support matrix: 26
collision-free normal activation source target including H5 registrar: 26
catalog parity: CLOSED_OFFLINE
```

구현된 test extractor는 H5 registrar activation도 source-backed authority에 포함한다.
Collision-free normal activation의 exact target set은 26이며 단순
expected-count 변경이나 test deselection으로 닫지 않는다. Existing registrar JUnit은 no-collision과
Korean-alias collision branch를 직접 다루지만 English-trio collision branch의 direct coverage는 없다.
Source inspection을 test pass로 바꾸어 말하지 말고 별도 direct test로 세 branch를 모두 고정한다.

26개 모두 source catalog metadata에는 존재해야 하지만 public natural-language exposure는 서로
다르다.

Snapshot과 support matrix의 exact target rows는 다음과 같다. `path`는 Java source root
상대 경로다.

| Command | Class | Owner | Path | Natural-language status | Support status |
|---|---|---|---|---|---|
| `auto_deposit_trust` | `AutoDepositTrustCommand` | `lavi_auto_deposit_trusted` | `lavi/minecraft/task/container/deposit/auto/trusted/command/AutoDepositTrustCommand.java` | `IMPLEMENTED` | `supported_korean` |
| `auto_deposit_untrust` | `AutoDepositUntrustCommand` | `lavi_auto_deposit_trusted` | `lavi/minecraft/task/container/deposit/auto/trusted/command/AutoDepositUntrustCommand.java` | `RAW_ONLY` | `java_only_user_or_dev_command` |
| `auto_deposit_trusted_list` | `AutoDepositTrustedListCommand` | `lavi_auto_deposit_trusted` | `lavi/minecraft/task/container/deposit/auto/trusted/command/AutoDepositTrustedListCommand.java` | `RAW_ONLY` | `java_only_user_or_dev_command` |
| `자동보관등록` | `AutoDepositKoreanBulkTrustCommand` | `lavi_auto_deposit_trusted` | `lavi/minecraft/task/container/deposit/auto/trusted/command/AutoDepositKoreanBulkTrustCommand.java` | `RAW_ONLY` | `java_only_user_or_dev_command` |

Production registry의 exact target metadata는 다음과 같다. 빈 `allowed_input_sources`와 false readiness는
source-backed shadow row만 유지하고 Python에서 submit하지 않는다는 뜻이다.

| Command | Slot schema | Lifecycle | Safety | Confirmation | Allowed input sources | Serializer |
|---|---|---|---:|---|---|---|
| `auto_deposit_trust` | `()` | `immediate` | R2 | `none` | `("lavi_chat_ui", "voice_input_final")` | `prefixless_auto_deposit_trust` |
| `auto_deposit_untrust` | `("destinationId?",)` | `immediate` | R2 | `none` | `()` | `prefixless_auto_deposit_untrust` |
| `auto_deposit_trusted_list` | `()` | `immediate` | R0 | `none` | `()` | `prefixless_auto_deposit_trusted_list` |
| `자동보관등록` | `()` | `immediate` | R2 | `none` | `()` | `prefixless_자동보관등록` |

네 row의 `resolver_domain`은 모두 `command_specific`이다. Final target readiness는 다음처럼 분리한다.

```text
auto_deposit_trust current production state after successful implementation and offline rollout:
  SOURCE_REGISTERED=true
  KOREAN_PARSE_COMPILE_READY=true
  PYTHON_ADMISSION_READY=true
  BRIDGE_LIFECYCLE_READY=true
  GAMEPLAY_EFFECT_VERIFIABLE=false
  PUBLIC_KOREAN_ENABLED=true

auto_deposit_untrust, auto_deposit_trusted_list, 자동보관등록:
  SOURCE_REGISTERED=true
  KOREAN_PARSE_COMPILE_READY=false
  PYTHON_ADMISSION_READY=false
  BRIDGE_LIFECYCLE_READY=false
  GAMEPLAY_EFFECT_VERIFIABLE=false
  PUBLIC_KOREAN_ENABLED=false
```

`자동보관등록` serializer ID는 현 registry convention을 따르는 inert identifier다. Allowed source가
없으므로 Python outbound Unicode command를 허용하지 않는다. 이 row의 disabled reason은
`raw_java_direct_alias_not_exposed_by_python`으로 문서화한다.

구현된 `auto_deposit_trust`의 실제 `ChatClefCommandSpec`은 다음으로 고정한다.

```text
command_name: auto_deposit_trust
slot_schema: ()
resolver_domain: command_specific
lifecycle_kind: immediate
safety_tier: R2
confirmation_mode: none
allowed_input_sources:
  - lavi_chat_ui
  - voice_input_final
serializer_id: prefixless_auto_deposit_trust
```

`IMMEDIATE`는 문서 taxonomy 이름이고 production `ChatClefCommandSpec.lifecycle_kind` encoding은
lowercase `immediate`로 고정한다. 다음 값은 registry dataclass field가 아니라 이 기능 문서가
소유하는 의미 제약이다.

```text
exposed_java_grammar_subset: area 16x16
effect: persistent_registry_mutation
```

Zero-slot metadata는 전체 raw Java grammar를 없앤다는 뜻이 아니다. Python natural-language route가
노출하는 fixed batch semantic에 사용자-controlled slot이 없다는 뜻이다. Minecraft direct entrypoint의
English no-arg single form과 네 exact batch form은 Java grammar owner에 그대로 남는다.

이전 registry source는 `_PARSER_READY_COMMANDS`와 `_PYTHON_ADMISSION_READY_COMMANDS`를
`_PUBLIC_KOREAN_COMMANDS` union으로 계산했다. 구현은 세 집합을 독립적인 source-backed memberships로
materialize했다. 기존 모든 command의 axis 값은 그대로 보존하고 H5만 다음
순서로 상태가 달라진다.

```text
pre-rollout implementation:
  KOREAN_PARSE_COMPILE_READY=true
  PYTHON_ADMISSION_READY=true
  PUBLIC_KOREAN_ENABLED=false

approved candidate rollout:
  KOREAN_PARSE_COMPILE_READY=true
  PYTHON_ADMISSION_READY=true
  PUBLIC_KOREAN_ENABLED=true

public-only rollback:
  KOREAN_PARSE_COMPILE_READY=true
  PYTHON_ADMISSION_READY=true
  PUBLIC_KOREAN_ENABLED=false
```

Readiness set은 public set에서 derive하지 않는다. 대신
`PUBLIC_KOREAN_ENABLED ⊆ KOREAN_PARSE_COMPILE_READY ∩ PYTHON_ADMISSION_READY` invariant를 별도
검사한다. H5 public candidate가 올라가기 전과 rollback 뒤에는 actual production registry에서 parse/admission
true, public false, H5 submit 0을 함께 증명한다.

명확한 fixed-size 명령형 요청 자체를 승인으로 사용하므로 별도 두 번째 확인 dialog는 만들지 않는다.
전체 submission gate는 책임별로 다음을 검사한다.

```text
typed H5 input admission before translation:
  source/final/provider_id/event_kind tuple is one of the two trusted ingress forms
  event_id is present, valid and claimed once for this process

translation/schema/command validator:
  rule-only zero-slot intent
  validated exact prefixless command

generic source + H5 command admission before adapter submission:
  request source is in registry allowed_input_sources
  source_registered
  korean_parse_compile_ready
  python_admission_ready
  bridge_lifecycle_ready for public exposure
  public_korean_enabled
  opaque claim receipt belongs to the same registry, event_id and source
  receipt state is ISSUED, checked without consumption

side-effect-free request construction:
  build the request without receiving or serializing the opaque receipt
  on failure, abandon the receipt and submit zero

claimed submission commit:
  revalidate the same registry/event/source/command binding and ISSUED state under the registry lock
  atomically transition the opaque receipt to SPENT exactly once
  remove the receipt from all request/metadata/wire/log payloads
  after successful commit, invoke command_submitter synchronously with no callback or await in between
  on commit rejection, submit zero; after submitter exception or UNKNOWN, keep SPENT and retry zero

router precheck/reconciliation owner:
  bridge enabled and connected
  no active command
  no reconciliation or quarantine owner
```

다음은 2026-09-04 documentation snapshot의 historical planned/provisional readiness다. 현재 production
registry는 `registry.spec("auto_deposit_trust")`를 제공하며 source/parser/admission/bridge/public=true,
gameplay-effect-verifiable=false다.

```text
SOURCE_REGISTERED: true
KOREAN_PARSE_COMPILE_READY: false
PYTHON_ADMISSION_READY: false
BRIDGE_LIFECYCLE_READY: false for this command-specific route until tests
GAMEPLAY_EFFECT_VERIFIABLE: false
PUBLIC_KOREAN_ENABLED: false
```

Public rollout은 순환 gate를 만들지 않도록 단계화한다.

```text
1. PUBLIC_KOREAN_ENABLED=false로 typed provenance, source migration, catalog/registry/parser/compiler/admission을 구현하고 parser/admission/public membership을 독립화한다.
2. injected registry metadata를 사용하는 offline catalog/router/bridge contract tests를 통과한다.
3. 기존 세 Java test는 보조 evidence로 유지한다. Existing registrar JUnit에 English trio의
   trust/untrust/list 각 이름 collision을 parameterized 세 subcase로 추가하고, 별도로 exact
   `auto_deposit_trust area 16x16` dispatch가 H5 callback과 같은 request의 terminal queue retirement로
   이어지는 dedicated Java integration test를 추가한다.
4. focused Java test와 required clean forced build를 통과해야 `BRIDGE_LIFECYCLE_READY=true`로 판정한다.
5. source-backed `KoreanChatClefCommandRegistry._PUBLIC_KOREAN_COMMANDS` candidate에 H5를 추가하고
   actual production registry로 전체
   focused/regression suite와 axis별 fail-closed matrix를 다시 실행한다.
6. 사용자가 exact external runtime 변경을 승인한 경우에만 그 candidate로 local Chat과 real
   VoiceInput final transcript를 각각 통제 검증한다.
7. 증거가 통과하면 true를 유지한다. 실패하면 즉시 false로 되돌리고 false-state smoke suite를 재실행한다.
```

2026-09-05 repository 범위에서는 위 1–5단계를 완료했고 public flag를 유지했다. 6단계의 active LAVI
restart와 실제 Chat/VoiceInput runtime은 승인되지 않아 `NOT_RUN`이며, 7단계의 live retain/revert 판단도
아직 하지 않는다.

즉 live Chat/microphone evidence는 false 상태에서 요구하지 않는다. Source-backed public flag candidate를
repository 안에서 만들고 test하는 단계는 완료됐지만, 그 source를 active LAVI process에 load하기 위한
restart와 실제 Chat/microphone runtime은 명시적으로 승인된 후속 범위다. 별도 runtime
config flag가 존재한다고 가정하지 않는다.
기존 세 test와 Python test만 연결한 compositional proof는 direct H5-to-bridge integration test가
아니므로 그것만으로 `BRIDGE_LIFECYCLE_READY=true`를 만들지 않는다. 추가된 dedicated Java integration
test는 exact H5 command name, normalized dispatch string, callback identity와 같은 request의 terminal
queue retirement를 한 fixture에서 증명한다. 그래도 gameplay effect는 증명하지 않는다.
`BRIDGE_LIFECYCLE_READY=true`의 의미는 request admission과 queue retirement에만 한정한다. Exact
integration test 또는 clean build가 실패하면 이 axis와 public flag를 false로 둔다. 승인된 candidate
runtime 검증을 실제 수행했는데 exact dispatch/terminal이 관찰되지 않은 경우에도 둘을 false로
되돌린다. Runtime이 승인되지 않아 `NOT_RUN`인 상태 자체는 test-proven bridge axis 실패가 아니다. Java production source 변경은 전제가
아니지만 focused Java test source와 build verification은 전제다.
`GAMEPLAY_EFFECT_VERIFIABLE`은 §11의 typed result 또는 승인된 Python-readable oracle 없이 true로
만들지 않는다. 이 독립 axis가 false여도 explicit fixed R2 command의 public flag는 통제 검증 후 true가
될 수 있지만, 사용자 응답은 계속 ACK-only여야 한다.

## 11. Submission, lifecycle and response evidence

Chat과 microphone은 같은 router와 command semantic을 사용하지만 source는 서로 다르다. Router label은
source가 아니라 metadata에 남긴다.

```text
local Chat:
  source: lavi_chat_ui
  metadata.input_event.provider_id: lavi_chat_ui
  metadata.input_event.event_kind: chat_submit

VoiceInput final transcript:
  source: voice_input_final
  metadata.input_event.provider_id: VoiceInput
  metadata.input_event.event_kind: final_transcript

both:
  metadata.input_event.event_id: <that event's unchanged ingress event_id>
  metadata.input_event.final: true
  metadata.input_route: minecraft_fabric_chatclef
  metadata.language: ko
  metadata.natural_language.language: ko
  metadata.natural_language.original_text: <original user text>
  metadata.natural_language.translation_input_text: <prefixless text passed to translation>
  metadata.natural_language.translation: <validated translation object>
  metadata.natural_language.translation.command: auto_deposit_trust area 16x16
request_id: lavi-input-ko-<unique-id>
```

구현된 typed-event 경계는 이 target을 충족한다. Source, event ID, provider ID, event kind와 final flag는
ingress에서 queue/router/request까지 그대로 보존하는
필수 authorization/audit data다. Microphone partial/interim transcript는 `final=false`로 H5 admission에서
거절하거나 command route에 넣지 않으며, 어떤 경우도 `voice_input_final` 권한으로 승격하지 않는다.

Side-effect-free router gate의 candidate check는 authoritative parse 전에 독립적으로 수행될 수 있다.
최종 의미 판정은 단 한 번의 translation 안에서 classifier가 한 번 소유한다. 각 trusted ingress
adapter callback invocation이 만든 immutable event 한 건은 다음 불변조건을 지킨다.

```text
authoritative final classification inside translation: once
translation: at most once
submission attempt: at most once
automatic retry/replay/resubmit: zero
pre-existing busy/disconnected/reconciliation/quarantine: current event submission zero
current submission outcome UNKNOWN: current event attempted once; retry/replay zero
after UNKNOWN ownership latch: later event submission zero until matching reconciliation
guarded rejection submission: zero
same event_id delivered again: translation zero; submission zero
reconnect replay: zero
transport result acceptance: same active websocket; current generation; matching session_id and request_id; correlation_id equals outgoing command_message_id
router UNKNOWN reconciliation: already-trusted snapshot with matching pending request_id/status
```

Identity 검증 경계는 하나로 뭉치지 않는다. Transport ingress acceptance는 같은 active WebSocket,
active command generation과 current generation의 일치, envelope `session_id`와 command session의
일치, result `request_id`와 command request의 일치, envelope `correlation_id`와 outbound
`command_message_id`의 일치를 검증한다. Incoming envelope 자체의 `message_id`를 expected ID와
matching한다고 주장하지 않는다. Router의 `UNKNOWN` reconciliation state는 pending `request_id`만
보존하며, already-trusted status snapshot의 `last_result.request_id/status`가 matching하는지를 직접
검사한다.

동일한 final transcript를 사용자가 실제로 다시 말한 것은 새 요청일 수 있으므로 text/time heuristic으로
임의 dedupe하지 않는다. 같은 `event_id`의 재전달만 duplicate delivery로 차단한다. Provider가 동일한
물리 발화를 두 callback으로 emit하면 서로 다른 event ID가 생기므로 provider duplicate utterance
suppression은 여전히 별도 범위다. Physical utterance -> provider callback exactly-once는 offline fixture로
증명할 수 없고 real microphone runtime에서 `INCONCLUSIVE` 또는 관찰 결과로 따로 기록한다.

Python envelope transport send가 성공한 뒤 local result factory가 만드는 pre-normalized
`CommandResultDTO`의 현재 성공 결과는 다음 사실만 증명한다.

```text
status=accepted
message=Fabric ChatClef command sent to Java bridge.
```

Router가 사용하는 normalized Python mapping에서는 같은 값을 다음 path로 검사한다.

```text
result.status.status=accepted
result.status.message=Fabric ChatClef command sent to Java bridge.
result.status.data.*=<fresh deep-copied equal-value data from the pre-normalized local DTO>
result.details.*=<a separate fresh deep-copied equal-value mirror produced by the router normalizer>
```

이 `accepted` 결과는 Java가 Python으로 보낸 wire `command_result`가 아니라 Python transport adapter의
local submission ACK다. Extension의 중간 payload는 `status.data`의 shallow mapping copy와 `details`의
original data mapping을 사용하므로 그 단계에는 deep-copy identity를 주장하지 않는다. Router normalizer가
두 값을 검증한 뒤 위 final mapping의 두 독립 deep copy를 만든다. Java가 이후 보내는
`running`/terminal wire result와 구분한다.

따라서 direct Gradio Chat에서 표시하는 즉시 사용자 응답은 다음 정도로 제한한다.

```text
[Minecraft] 주변 16×16 자동 보관 대상 등록 명령을 제출했어요.
```

VoiceInput은 queue worker가 response generator를 끝까지 실행하므로 command route 자체는 수행되지만,
현재 `LLMInputQueueRuntime.drain_queue()`는 yielded route response를 소비하고 버린다. 따라서 microphone
경로에서 위 ACK가 Chat UI, TTS 또는 별도 output listener에 전달된다고 주장하지 않는다. 이번 기능의
microphone acceptance는 final transcript가 정확히 한 H5 submission attempt로 이어지는 데까지이며
feedback delivery는 `NOT_IMPLEMENTED`다. Microphone ACK/TTS가 필요하면 별도 bounded output dispatcher,
delivery target과 exactly-once test를 새 범위로 설계한다.

Java H5 command는 새 user Task를 만들지 않고 repository operation과 bounded log emission 뒤
`finish()`한다. Existing bridge는 lifecycle 관찰 결과에 따라 나중에 generic terminal을 보낼 수 있다.
No-user-task 상태가 정확히 귀속되면 Java wire `command_result` payload가 다음 result를 보낼 수 있고,
Python은 이를 decode/validate해 `CommandResultDTO`로 보존한다.

```text
status=completed
data.result_reason=callback_completed_without_user_task
data.result_fidelity=callback_without_user_task
message=ChatClef command completed without starting a user task.
```

Normalized Python mapping assertion path는 `result.status.status=completed`,
`result.status.data.result_reason=callback_completed_without_user_task`와
`result.status.data.result_fidelity=callback_without_user_task`이며 같은 data는
`result.details.result_reason` / `result.details.result_fidelity`에도 mirror된다. Wire field와 normalized
wrapper path를 섞어 쓰지 않는다.

그러나 이 generic terminal은 H5 result의 `success`, `UPDATED`, `NO_CHANGE`, failure reason 또는 count를
포함하지 않는다. Current `AutoDepositTrustedBulkRegistrationCommandOperation`은 H5 result를 Minecraft
log/logWarning으로만 emit하며 bridge에는 `STORE_HOME` 전용 projector만 존재한다. 따라서 다음 표현은
금지한다.

```text
주변 상자 등록 완료
9개 등록 완료
UPDATED 성공
변경할 상자가 없었어요
등록 실패 이유는 ...
```

Pre-existing root/Idle ownership 때문에 no-user-task 귀속이 불분명하면 generic `unknown` 또는 다른
fail-closed terminal이 가능하다. Immediate-command lifecycle은 command-specific source/test에서 먼저
증명하며, 어떤 generic terminal도 H5 registry mutation 성공으로 승격하지 않는다.

Generic completed를 관찰해도 registry mutation 성공을 추측하지 않는다. Exact H5 결과가 필요하면
별도 scope로 다음을 설계한다.

```text
Java H5 typed result projection
-> existing command_result.data additive field
-> Python typed validator
-> matching-request reconciliation
-> command-specific response renderer
```

Human-readable `[AutoDepositBulkTrust]` log 문자열을 Python이 파싱해 public success response를 만드는
방식은 사용하지 않는다.

Existing Java source/test boundary는 다음 책임을 이미 분리해 둔다. 이 문서화 작업에서는 해당 test를
실행하지 않았으며 pass를 새로 주장하지 않는다.

| Existing test | Source-backed responsibility |
|---|---|
| [AutoDepositTrustCommandTest](../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/task/container/deposit/auto/trusted/command/AutoDepositTrustCommandTest.java) | H5 bulk operation 호출과 command completion callback의 one-shot 계약 |
| [FabricChatClefFinishCallbackLifecycleBoundaryTest](../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefFinishCallbackLifecycleBoundaryTest.java) | generic finish callback의 request/lifecycle 귀속 경계 |
| [FabricChatClefCommandResultFidelityTest](../runtime/chatclef_fabric_1.20.1/src/test/java/lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandResultFidelityTest.java) | bridge command-result envelope fidelity |

위 세 Java test는 각각의 경계를 증명하는 보조 evidence일 뿐 direct H5 -> bridge integration test는
아니다. 후속 구현에서는
`FabricChatClefAutoDepositTrustLifecycleIntegrationTest` 같은 dedicated focused test를 Java test package에
추가해 exact H5 command dispatch -> H5 completion callback -> matching request terminal send -> queue
retirement를 한 fixture에서 검증한다. 새 Python source-contract test는 registry metadata -> prefixless
compiler -> one-shot submit까지만 소유한다. Dedicated Java test와 required clean forced build가 모두
통과하기 전에는 `BRIDGE_LIFECYCLE_READY=false`다. Java production behavior source는 변경하지 않는다.

Dedicated Java fixture는 적어도 다음 두 exact terminal branch를 분리한다.

```text
NO_ROOT_VISIBLE + synchronous H5 finish callback:
  matching request_id
  status=completed
  data.result_reason=callback_completed_without_user_task
  data.result_fidelity=callback_without_user_task
  terminal send exactly once
  successful async terminal-send completion (`outcome.succeeded()=true`) 뒤 matching active queue entry retired

PREEXISTING_UNCHANGED_IDLE_ROOT:
  finishCallbackReceived=true
  finishCallbackFirstObservedBeforeDispatchReturn=true
  taskFinishedObservation=null
  preexistingIdleRootStabilityQualified=true
  matching request_id
  status=unknown
  data.result_reason=finish_callback_without_new_command_owned_root
  data.result_fidelity=callback_without_matching_user_task_event
  terminal send exactly once
  successful async terminal-send completion (`outcome.succeeded()=true`) 뒤 matching active queue entry retired
```

두 branch 모두 normalized dispatch가 exact configured-prefix H5 command인지 확인한다. `completed` branch도
registry mutation 성공이나 count를 증명하지 않는다.

이 readiness의 합격 의미는 queue retirement와 fail-closed terminal handling뿐이다. `NO_ROOT_VISIBLE`과
synchronous finish callback에서는 matching generic completed가 가능하고, unchanged pre-existing Idle
root 등 ownership이 모호한 경우에는 typed `UNKNOWN`이 가능해야 한다. 어느 경우도 H5 mutation success
증거로 승격하지 않는다.

## 12. Deterministic test contract

구현 시 새 feature test는 한 의미 있는 package 아래 책임별 파일로 분리한다.

```text
tests/minecraft_chatclef/auto_deposit_trust/
    provenance/
        test_lavi_input_event_contract.py
        test_input_provider_source_resolver_contract.py
        test_provider_bound_input_event_adapter_contract.py
        test_local_chat_input_event_adapter_contract.py
        test_direct_callback_input_event_adapter_contract.py
        test_lavi_input_event_queue_contract.py
        test_structured_llm_fallback_payload_contract.py
    candidate/
        test_auto_deposit_trust_candidate_detector_contract.py
    exact_input/
        test_auto_deposit_trust_exact_input_adapter_contract.py
    safety/
        test_auto_deposit_trust_raw_input_safety_contract.py
    normalization/
        test_auto_deposit_trust_input_normalizer_contract.py
    classification/
        test_korean_auto_deposit_trust_intent_classifier_contract.py
    guard/
        test_auto_deposit_trust_guard_marker_detector_contract.py
        test_auto_deposit_trust_guard_intent_encoder_contract.py
        test_auto_deposit_trust_guard_intent_decoder_contract.py
    contracts/
        test_auto_deposit_trust_intent_contract.py
    registry/
        test_auto_deposit_trust_registry_admission_contract.py
    routing/
        test_minecraft_chatclef_typed_gate_decision_contract.py
        test_auto_deposit_trust_input_admission_contract.py
        test_auto_deposit_trust_input_event_claim_registry_contract.py
        test_auto_deposit_trust_claimed_submission_authorizer_contract.py
        test_auto_deposit_trust_claim_registry_object_graph_contract.py
        test_auto_deposit_trust_early_failure_contract.py
        test_auto_deposit_trust_app_wiring_contract.py
        test_auto_deposit_trust_extension_bypass_contract.py
        test_auto_deposit_trust_router_contract.py
        test_auto_deposit_trust_source_to_request_contract.py
    response/
        test_auto_deposit_trust_response_contract.py

plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java/
    lavi/minecraft/fabric/chatclef/bridge/command/
        FabricChatClefAutoDepositTrustLifecycleIntegrationTest.java
    lavi/minecraft/task/container/deposit/auto/trusted/command/
        AutoDepositTrustedCommandRegistrarTest.java
            -> add only the missing any-English-trio-collision branch
```

필수 offline matrix:

1. Positive grammar 대표 문장과 spacing, 존댓말, 무구두점 STT 변형이 같은 intent가 된다.
2. `16x16`, `16 X 16`, `16×16`, `16 곱하기 16`이 같은 fixed form이 된다.
   전각 영문/숫자, 원문자 숫자, NFKC compatibility form, Unicode `Zs` 공백과 shorthand 내부 punctuation은 candidate로
   포착하되 executable form으로 승격하지 않는다.
3. 부정, 질문, 가정, 유예, 복합, missing/wrong/mixed size와 extra number는 submission 0이다.
   `등록해?`와 `등록해？`는 raw question evidence가 normalization 전에 우선되어야 한다.
4. Guarded H5 candidate는 LLM, `DEPOSIT_ITEM` 또는 `STORE_HOME`으로 fallback하지 않는다.
   Candidate detector는 string fixture 전체에서 no-throw total result를 반환하고, final classifier
   예외는 `translation_internal_error` handled rejection, submission 0, LLM call 0으로 fail closed한다.
5. 완전히 무관한 대화는 기존 normal LLM path를 유지한다.
6. Executable H5 intent는 `source=rule`, zero-slot이며 모든 item/player/coordinate field가 비어 있다.
   Guarded decision은 `intent_type=UNKNOWN`, exact guard source/slots/reason으로 운반되고 LLM 전에
   typed `INVALID` rejection으로 소비된다. Malformed/contradictory guard payload도 submission 0이다.
7. LLM extractor는 pre/post-validation 모두 H5 intent 생성을 거절한다.
8. Compiler output은 정확히 `auto_deposit_trust area 16x16` 한 문자열이고 `@`가 없다.
9. Trusted source의 두 exact `@auto_deposit_trust ...` whole-string form은 raw CR/LF/control/non-ASCII-Zs pre-scan을
    통과한 뒤 pre-translation adapter가 raw original을 보존하면서 canonical prefixless input으로 바꾼다.
    그 밖의 trusted `@`, semicolon, hash, quote와 arbitrary slot injection은 기존 full dangerous validator의
    `dangerous_command_slot` handled rejection이며 submission 0, LLM call 0이다. Leading/trailing CR/LF와
    `@...16x16\n`은 exact/natural-language 구분과 trim 전에
    `auto_deposit_trust_input_raw_control_not_allowed`로 거절한다. 이 raw-first scan은 exact `@`
    form뿐 아니라 자연어 H5 candidate에도 동일하게 적용되며 기존 `.strip()` owner에 들어가기 전에
    끝난다. Raw pre-scan이 허용 exact `@` 두 form 자체를 거절하면 안 된다.
10. Translation validator는 intent/command mismatch를 거절한다.
11. Source extraction이 H5 registrar 네 command를 포함하고, collision-free normal activation에서
    registry/snapshot/matrix exact target set과 owner는 26이다. Direct registrar tests는 no-collision,
    any-English-trio-collision과 Korean-alias-only-collision을 서로 다른 expected state로 검증한다.
    Any-English branch는 trust, untrust와 list의 각 이름 collision을 parameterized 세 subcase로 실행한다.
12. `untrust`, `trusted_list`, Korean Java alias는 raw-only/public false를 유지한다.
13. `auto_deposit_trust` R2, immediate lifecycle, persistent mutation effect, empty slot, exact allowed sources와 readiness metadata가 exact하다.
14. Immutable event field는 direct Chat/queue/router/request metadata까지 변하지 않는다. Local Chat은
    `lavi_chat_ui`, VoiceInput final은 `voice_input_final` source를 사용하며 intent와 compiled command만
    같다. Descriptor-bound policy가 `source/provider_id/event_kind/final` 전체를 만들고 adapter가 event ID를
    발급한다. Provider payload 안의 같은 이름 field와 user text의 source spoofing은 효과가 없다. Raw object는
    `untrusted_legacy`다. Non-H5 ScreenVision structured dict는 object와 모든 nested field를 그대로
    `response_pipeline.predict()`에 넘겨 screen-observation/history-exclusion side effect를 보존한다.
15. TwitchChatFetch, YoutubeChatFetch, IdleThink, NullInput, ScreenVision, StarCraft와 unknown provider의
    H5 candidate는 handled source rejection, translate 0, submit 0, LLM call 0이다. 같은 source의 비-H5
    ordinary input은 기존 normal LLM path를 유지한다. Voice partial/final=false도 H5 submit 0이며 이
    모든 pre-admission rejection은 event-claim count를 변경하지 않는다.
16. Repeated provider sync 뒤에도 provider당 exact bound callback은 하나이고 shutdown은 그 callback을
    제거한다. Local Chat adapter callback invocation과 VoiceInput final callback invocation은 각각 event
    하나만 만든다.
17. 같은 `event_id`를 두 번 route하거나 concurrent claim하면 최초 claim 하나만 성공한다. 4095번째와
    4096번째 unique ID는 허용되고 4097번째는 capacity rejection이다. Reload/reconnect는 claim을
    초기화하지 않고 process/runtime reconstruction만 초기화한다. 발급 receipt는 matching event/source와
    registry object에 결합되며 wire/metadata에 직렬화되지 않는다. Admission inspection은 이를 소비하지
    않고 검증한다. Factory 성공 뒤 atomic commit 하나만 `ISSUED -> SPENT` 전이에 성공하며 그 직후
    submitter가 한 번 호출된다. Concurrent inspection/commit에서는 request build가 둘 이상 일어날 수 있어도
    submitter 진입은 하나뿐이다. Factory failure는 receipt를 abandon하고 submit 0, commit failure는 submit 0,
    submitter exception/UNKNOWN은 spent 유지와 retry/replay 0이다. 어느 실패에서도 같은 event ID는 다시
    claim되지 않는다.
    Event ID는 exact built-in string `[0-9a-f]{32}`만 허용한다. Valid generated ID와 `None`, empty,
    whitespace, padded, uppercase, 31/33-character, non-hex, control-character, list와 mapping invalid vector를
    각각 검사한다. Invalid ID는 registry count 0, translation 0, submit 0이고 어떤 normalization도 받지 않는다.
    Extension/handler unavailable, reconciliation/busy/quarantine, exact adapter/translation/DTO/precheck/admission
    rejection 또는 exception, factory failure와 commit rejection/exception을 parameterize해 claim 뒤의 모든
    terminal no-submit path가 matching receipt를 `SPENT`로 만들고 live `ISSUED` count를 0으로 남기는지 검증한다.
    각 branch 뒤 같은 event ID 재전달도 claim/translation/submit 0이어야 한다.
18. Trusted ingress event delivery 하나당 translate 1회 이하, submit attempt 1회 이하이며 서로 다른
    event는 서로 다른 request ID다. Provider가 한 물리 발화를 두 callback으로 emit하는 문제는 이
    범위 밖이며 live evidence도 자동으로 exactly-once로 판정하지 않는다.
19. H5 candidate는 extension unavailable, translation handler unavailable 또는 submission handler
    unavailable에서도 handled rejection, translate 0, submit 0, LLM call 0이다. Candidate identity를
    generic early-return에서 잃어 `not_handled`로 바꾸면 안 된다. `extension=None`인 production app
    wiring도 router를 설치한 상태로 이 결과를 내고, 같은 배선의 비-H5 text는 normal LLM으로 간다.
    H5 route 판정 뒤 source/final/event-ID admission -> raw-safety -> claim ->
    extension/handler와 reconciliation 준비 -> exact-adapter/translation 준비 순서의 어느 지점에 주입한 예외도
    `auto_deposit_trust_input_internal_error` handled rejection이며 LLM call 0이다.
    Router import/construction failure는 app wiring이 삼키지 않고 startup/composition failure로 올리며
    router 없는 LLM fallback 상태를 만들지 않는다.
20. Pre-existing busy/disconnected/reconciliation/quarantine에서는 current event submit이 0이다. Current
    submit이 UNKNOWN으로 끝난 경우에는 attempt 1회, retry/replay 0이며 이후 event는 matching
    reconciliation 전까지 submit 0이다.
21. Generic source admission은 모든 public Korean command에 registry allowlist를 강제한다. Existing
    `STORE_HOME`과 public commands는 `lavi_chat_ui`, `voice_input_final`, separate `direct_typed` migration
    뒤에도 기존 authorized behavior를 유지한다. Fabric Korean UI를 이미 허용하던 command는
    `lavi_gui_korean`도 유지하지만 `STORE_HOME`과 H5에는 이를 새로 추가하지 않는다. Positive router
    fixtures는 typed event를 사용하고 raw string은 `untrusted_legacy`로 거절되며 coarse
    `lavi_chat_mic_router`는 target allowlist 어디에도 남지 않는다. 모든 untrusted source는 submit 0이다.
    Direct extension `handle_natural_language_command`/`submit_translated_command`, arbitrary mapping,
    fabricated `source`/`metadata`, Fabric Korean UI와 missing/forged/spent receipt도 H5 submit 0이다. 오직
    router claim registry가 발급해 같은 event/source에 묶인 opaque receipt를 한 번 소비한 route만 submit
    1회가 가능하다.
    Existing generic public command도 local Chat/VoiceInput/direct/Fabric UI allowlist에서는 기존 동작을
    유지하고 Twitch/YouTube/IdleThink/ScreenVision/StarCraft/unknown source의 command candidate는 typed
    source rejection과 submit 0이 된다. 같은 source의 ordinary non-command text는 normal LLM path를
    유지한다.
22. Source/parse/admission/bridge/public axis 중 필요한 하나라도 false이면 submit 0이다.
    `GAMEPLAY_EFFECT_VERIFIABLE=false`는 ACK-only wording을 강제하지만 별도 staged public gate다.
23. Public flag를 바꾼 candidate는 injected spec이 아니라 actual production registry/config로 전체
    focused/regression suite를 다시 통과한다. Registry readiness sets는 public membership에서 독립적이며,
    rollout 전과 rollback 뒤 actual production row가 parse/admission true, public false, submit 0인지 증명한다.
24. Direct Chat immediate response는 `제출`만 말하고 `완료`, count 또는 H5 terminal status를 단정하지
    않는다. Queue-driven microphone route는 submission을 수행하지만 current generator yield가 discard되므로
    user-visible/TTS ACK를 test success로 요구하거나 구현됐다고 표시하지 않는다.
    Wire result path와 normalized `status.status` / `status.data` / `details` path를 각각 검사한다.
25. Existing GET/DEPOSIT/STORE_HOME/control intent tests가 회귀하지 않는다.
26. Java bridge source contract는 prefixless input에 prefix를 한 번만 붙인다.
27. Existing registrar/H5 command/generic dispatcher-lifecycle tests는 보조 evidence로 유지한다. 별도
    exact Java integration test는 `auto_deposit_trust area 16x16` dispatch, H5 callback, matching request
    terminal send와 queue retirement를 한 fixture에서 검증한다. 이 test와 clean forced build가 통과하기
    전에는 `BRIDGE_LIFECYCLE_READY=false`다.
28. H5 no-user-task terminal에 typed bulk result/count가 없다는 현재 evidence boundary를 잠근다.

Catalog tests는 단순 count 26만 확인하지 않는다.

```text
actual registrar activation sources
exact command-name set
owner and registration path
collision-free target plus distinct English-trio and Korean-alias collision states
duplicate name count zero
Python registry exact-set parity
support matrix exact-set parity
per-command public/readiness classification
```

## 13. Runtime acceptance

Runtime 검증은 구현·tests가 통과하고 사용자가 정확한 external instance mutation을 승인한 뒤에만
수행한다. Chat과 microphone은 다음을 각각 한 번 검증한다.

```text
one final user input
one validated AUTO_DEPOSIT_TRUST_AREA intent
one prefixless canonical command
one request_id with source=lavi_chat_ui for Chat or voice_input_final for microphone
one Java normalized <configuredPrefix>auto_deposit_trust area 16x16 dispatch
captured configured prefix (expected `@` only when that runtime configuration proves it)
one bounded [AutoDepositBulkTrust] terminal log
anchorSource=PLAYER_BLOCK_POSITION
single isolated-run registry before/after evidence
zero duplicate dispatch
```

현재 `[AutoDepositBulkTrust]` log와 generic bridge terminal에는 같은 request/session/generation/correlation
identity가 모두 실리지 않는다. 따라서 위 registry before/after와 bounded log는 격리된 한 실행 창에서의
temporal/run-scoped 보조 증거일 뿐이다. Exact request -> H5 mutation 결합은 `INCONCLUSIVE`로 기록한다.
Local Python adapter ACK는 transport-send acceptance만 증명한다. Java가 나중에 보내는 wire `status=completed` /
`data.result_reason=callback_completed_without_user_task`와 그 normalized mirror
`status.status=completed` / `status.data.result_reason` / `details.result_reason`은 queue/lifecycle terminal만
증명하며 registry mutation success를 증명하지 않는다.

Chat run 뒤 같은 위치에서 microphone run을 하면 정상적으로 `NO_CHANGE`가 될 수 있다. 이를 false
failure로 해석하지 않는다. 두 input path에서 mutation happy-path 보조 evidence를 각각 관찰하려면
registry fixture를 안전하게 복원하거나 서로 다른 player range/test world snapshot을 사용한다.
Fixture 생성·복원과 external instance 변경은 별도 승인 범위다.

Runtime evidence도 다음을 분리한다.

```text
local transport submission accepted
generic Java command terminal
H5 bounded operation log
registry mutation/result
downstream automatic-deposit execution
```

H5 registration success는 downstream automatic deposit, navigation, container open 또는 item transfer
성공을 뜻하지 않는다.

## 14. Implementation and rollback gate

향후 구현 요청이 오면 다음 순서로 진행한다.

```text
1. repository/dirty-worktree and catalog baseline capture
2. H5 registrar source-extraction/catalog reconciliation
3. typed input provenance, stable provider callback binding and existing-public-command source migration
4. responsibility-split deterministic classifier and zero-slot intent
5. exact trusted @ adapter, schema/compiler/LLM-denial and guarded fallback wiring
6. registry/admission and bounded response wording
7. focused offline tests and existing regression suite with production metadata
8. missing English-trio registrar collision coverage, exact H5 Java dispatcher/lifecycle integration test,
   and required clean forced build
9. diff, catalog artifact and source-boundary inspection
10. explicitly authorized runtime validation when requested
```

Java production source를 변경하지 않는 구현에서는 Java H5 behavior rollback이 필요하지 않다. 새
focused Java test source는 독립 검증 unit이며 production behavior를 바꾸지 않는다.
Rollback은 다음 독립 unit으로 나눈다.

```text
catalog reconciliation unit:
  26-name source extraction and four H5 shadow rows are source truth
  public feature rollback after Java H5 remains must not remove them

input provenance hardening unit:
  immutable input event, provider callback binding, queue/UI adapters,
  source migration and their tests
  if this unit is reverted for its own defect, H5 PUBLIC_KOREAN_ENABLED remains false
  and INPUT_PROVENANCE_BLOCKED is restored explicitly

H5 feature unit:
  new intent/input-admission/exact-input packages
  narrow parser/gate/schema/compiler/LLM branches
  H5 command admission/response branch and focused tests
  removal leaves auto_deposit_trust as source-backed RAW_ONLY/public false metadata

Java verification-only test unit:
  one missing English-trio collision case in the existing registrar test
  one exact H5 dispatcher/lifecycle integration test
  production Java behavior remains unchanged; test hunks are independently revertible

rollout unit:
  parser/admission/public source memberships are independent; readiness is not derived from public
  remove auto_deposit_trust from source-backed _PUBLIC_KOREAN_COMMANDS first
  set only PUBLIC_KOREAN_ENABLED=false for a public-only rollback;
  preserve SOURCE/PARSE/ADMISSION/BRIDGE/GAMEPLAY axes according to their actual evidence
  lower another axis only when its owning source or verification unit is also reverted or invalidated
  run repository false-state source/axis/no-submit smoke after rollback
  if the candidate is loaded in an active LAVI process, obtain external-runtime approval,
  stop/restart that process with the disabled source, then prove public=false and submit=0
  no Java JAR rebuild/redeploy is required solely for this Python source-backed flag rollback
```

검증 실패 시 public flag를 열어 둔 채 parser만 되돌리거나, catalog에서 H5 command를 다시 숨기거나,
generic safety/LLM fallback을 느슨하게 만들지 않는다. Source-backed inventory, provenance hardening,
feature behavior와 rollout flag를 각각 independently reviewable/revertible하게 유지한다.

Rollback이 repository source에만 적용됐는지 active runtime에도 적용됐는지를 별도 상태로 기록한다.
이미 candidate를 실행 중이라면 source edit만으로 runtime이 비활성화됐다고 주장하지 않는다. 승인된
LAVI restart 뒤 production registry의 `public_korean_enabled=false`, H5 no-submit과 ordinary non-H5 LLM
smoke를 확인해야 active-runtime rollback이 완료된다. Java artifact는 production Java 변경이나 별도
deployment가 있었던 경우에만 그에 맞는 artifact rollback 절차를 추가한다.

Focused Java integration test를 추가하므로 구현 검증에는
[Fabric ChatClef 1.20.1 Build Verification](chatclef-fabric-build-verification.md)의 clean forced build를
적용한다. JAR deployment와 Minecraft runtime은 별도 승인 범위이며 각각의 상태를 별도로 기록한다.

## 15. Completion definition

이 문서화 작업의 완료는 다음만 의미한다.

```text
Korean Chat/microphone intent and safety semantics documented
typed Chat/VoiceInput provenance prerequisite and source-isolation policy documented
two exact trusted @ whole-string forms and all-other-dangerous-input rejection documented
two-phase claim inspection/atomic submission-commit ordering documented
existing Java H5 behavior preserved
prefixless canonical command fixed
catalog drift and target 26-name surface recorded
exact H5 bridge-lifecycle test gate and ACK versus actual H5 result boundary recorded
microphone command-execution boundary and NOT_IMPLEMENTED ACK delivery recorded separately
responsibility/package and test plan recorded
related documents linked
```

다음을 의미하지 않는다.

```text
Python feature implemented
public Korean command enabled
tests or build passed
JAR deployed
Chat or microphone runtime verified
registry mutation verified through LAVI response
commit or push completed
```

위 목록은 2026-09-04 문서화 단계의 완료 경계다. 이후 구현 결과와 현재 상태는 다음 Section 16이
우선한다.

## 16. 2026-09-05 implementation evidence

### 16.1 Implemented boundary

다음 구현이 이 계약에 따라 추가되었다.

```text
immutable LaviInputEvent and source-bound Chat/Voice/direct adapters
always-installed MinecraftChatClefInputRouter, including extension-unavailable fail-closed handling
typed H5 gate and deterministic rule-only AUTO_DEPOSIT_TRUST_AREA intent
closed natural-language execution grammar plus guarded no-submit classifications
two exact trusted @ forms with ASCII outer-space handling
raw Cc/Cf/Zl/Zp and non-ASCII Zs rejection plus detection-only NFKC candidate capture without execution folding
closed execution normalization that preserves command-keyword case, compatibility characters, and internal punctuation
process-lifetime 4096-entry no-eviction claim registry and non-serializable opaque receipt
non-consuming inspect -> request build -> atomic ISSUED-to-SPENT commit -> immediate synchronous submit
H5 translation revalidation at the Router and command-admission boundaries
26-row source-backed command catalog with only auto_deposit_trust newly public in Korean
fixed submission-only Chat acknowledgement wording
responsibility-based input/gating/admission/delivery/safety/exact-input/intent package split
```

기존 flat `input/minecraft_chatclef_input_intent_gate.py`는 실제 구현이 있는
`input/gating/minecraft_chatclef_input_intent_gate.py`로 이동했고 모든 production import를 새 경로로
갱신했다. 빈 compatibility Python 파일은 남기지 않았다.

### 16.2 Verified safety and delivery properties

Offline tests는 다음 경계를 고정한다.

```text
only lavi_chat_ui/lavi_chat_ui/chat_submit/true and
     voice_input_final/VoiceInput/final_transcript/true can enter H5 admission
invalid or non-built-in event IDs perform zero claim, translation, and submission
raw strings, payload source claims, untrusted providers, intermediate input, and direct API calls cannot gain H5 authority
negative, question, hypothetical, deferred, compound, wrong-size, extra-option, and ambiguous candidates submit zero commands
punctuation/control/Unicode marker obfuscation is retained in the H5 rejection lane instead of reaching the normal LLM
uppercase or mixed-case shorthand keywords remain guarded and cannot be normalized into an executable exact form
every tested post-claim rejection or exception leaves zero ISSUED receipts and blocks same-event replay
missing, foreign, forged, and spent receipts fail authorization
Chat and Voice paths both produce exactly one prefixless auto_deposit_trust area 16x16 request with exact metadata
ScreenVision structured fallback retains object identity for ordinary non-H5 LLM processing
```

최종 Python 검증 결과는 다음과 같다.

```text
H5 focused/affected suite: 126 passed, 452 subtests passed
repository regression suite: 1801 passed, 4 skipped, 3 deselected, 3821 subtests passed
uppercase/mixed-case shorthand audit: 46 Chat/Voice events; submit 0; issued claim 0
Unicode U+0000..U+10FFFF marker insertion: candidate bypass 0; executable 0; exception 0
NFKC/fullwidth/circled/NFD/NFKD/punctuation/Unicode-Zs corpus: 1,774 cases; submit 0; router bypass 0
```

Repository regression의 세 deselection은 기능 실패를 숨기기 위한 제외가 아니다. 두 항목은 이번
변경에서 제거한 legacy flat gate가 commit 전 Git index에는 tracked 상태로 남아 있어 발생하는
dirty-worktree inventory 검사이며, 한 항목은 sandbox 내부 Windows multiprocessing named-pipe 권한
제약을 받는 SQLite writer-lock 검사다. Writer-lock 검사는 허용된 외부 실행에서 독립적으로
`1 passed`를 확인했다. 실제 H5 및 영향 범위 테스트는 제외 없이 위 focused suite에서 통과했다.

### 16.3 Java verification and artifact

Java production source under `src/main/java` remained unchanged. Java changes are test-only:

```text
existing registrar test: English trust/untrust/list collision coverage
new focused integration test: dispatcher -> callback -> typed terminal -> successful queue retirement
AutoDepositTrustedCommandRegistrarTest: 5 tests; 0 failures; 0 errors; 0 skipped
FabricChatClefAutoDepositTrustLifecycleIntegrationTest: 2 tests; 0 failures; 0 errors; 0 skipped
```

The repository PowerShell build script ran the canonical command from the exact runtime root:

```text
.\gradlew.bat clean build --rerun-tasks
BUILD SUCCESSFUL in 4m 35s
171 actionable tasks: 171 executed
```

Fresh 1.20.1 artifact evidence:

```text
path: plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar
size: 8,112,941 bytes
SHA-256: 24DD4C8DEBA968D0206CA3D68A227C57D513E55769F1BB51F5943F549989CC27
```

### 16.4 Explicitly unverified boundaries

```text
CurseForge deployment: NOT_RUN
Minecraft launch/live-world mutation: NOT_RUN
Chat or microphone live runtime: NOT_RUN
H5 repository mutation observed through LAVI: NOT_VERIFIED
microphone user-visible/TTS acknowledgement: NOT_IMPLEMENTED
commit/push: NOT_PERFORMED
```

따라서 offline 구현·빌드 완료를 실제 월드의 상자 등록 성공으로 확대 해석하지 않는다. 실제 Chat과
microphone runtime 검증, JAR 배포, 테스트 월드 변경은 각각 별도 명시적 승인 범위다.
