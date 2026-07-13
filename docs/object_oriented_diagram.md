# LAVI 객체지향 구조도

이 문서는 프로젝트가 직접 소유한 주요 런타임 클래스와 플러그인 관계를 현재 코드 기준으로 요약합니다.
외부 라이브러리의 내부 클래스, 모델 파일, 비활성화된 레거시 코드는 제외했습니다.

## 1. 전체 애플리케이션과 플러그인 구조

```mermaid
classDiagram
    direction LR

    class MainBootstrap {
        <<main.py>>
        +AppComposer().run()
    }
    class AppComposer {
        <<app_core.app_composer>>
        +run()
        +create core components
        +create optional modules
        +register game extensions
        +launch queue
    }
    class GradioLaunch {
        <<app_core.gradio_launch>>
        +find_available_port()
    }
    class ModuleConfig {
        <<app_core.module_config>>
        +module_enabled()
    }
    class OptionalPluginLoader {
        <<app_core.optional_plugin_loader>>
        +instantiate_optional_plugin()
        +import_optional_attribute()
    }
    class MemoryBootstrap {
        <<app_core.memory_bootstrap>>
        +bootstrap_memory()
    }
    class ScreenRouterBootstrap {
        <<app_core.screen_router_bootstrap>>
        +build_screen_question_router()
    }
    class RuntimeLifecycle
    class GPUDeviceManager
    class AudioDeviceManager
    class ExtensionRegistry {
        <<app_core.extensions>>
        +register()
        +initialize()
        +get()
        +stop_all()
    }
    class GameExtensionContext
    class GameExtensionInterface {
        <<interface>>
    }

    class PluginLoader
    class PluginSelectionBase
    class Provider
    class Input
    class LLM
    class Translate
    class TTS
    class Vtuber

    class ScreenVision
    class SongPlayer
    class Chess
    class StarCraft116
    class StarCraft2
    class StarCraftRemastered {
        <<optional module>>
    }
    class ChessGameExtension
    class StarCraft116GameExtension
    class StarCraft2GameExtension
    class StarCraft2Extension {
        <<passive log observer>>
    }

    class InputPluginInterface {
        <<interface>>
    }
    class LLMPluginInterface {
        <<interface>>
    }
    class TranslationPluginInterface {
        <<interface>>
    }
    class TTSPluginInterface {
        <<interface>>
    }
    class VtuberPluginInterface {
        <<interface>>
    }

    class VoiceInput
    class TwitchChatFetch
    class YoutubeChatFetch
    class Hybrid_OpenAI_LLM
    class ChatGPT_OpenAI
    class NoTranslate
    class LocalENToJA
    class GPTSoVITS
    class VtubeStudio

    PluginSelectionBase <|-- Input
    PluginSelectionBase <|-- LLM
    PluginSelectionBase <|-- Translate
    PluginSelectionBase <|-- TTS
    PluginSelectionBase <|-- Vtuber
    PluginSelectionBase *-- Provider
    Provider o-- InputPluginInterface
    Provider o-- LLMPluginInterface
    Provider o-- TranslationPluginInterface
    Provider o-- TTSPluginInterface
    Provider o-- VtuberPluginInterface

    InputPluginInterface <|.. VoiceInput
    InputPluginInterface <|.. TwitchChatFetch
    InputPluginInterface <|.. YoutubeChatFetch
    LLMPluginInterface <|.. Hybrid_OpenAI_LLM
    LLMPluginInterface <|.. ChatGPT_OpenAI
    TranslationPluginInterface <|.. NoTranslate
    TranslationPluginInterface <|.. LocalENToJA
    TTSPluginInterface <|.. GPTSoVITS
    VtuberPluginInterface <|.. VtubeStudio

    PluginLoader o-- InputPluginInterface : discovers
    PluginLoader o-- LLMPluginInterface : discovers
    PluginLoader o-- TranslationPluginInterface : discovers
    PluginLoader o-- TTSPluginInterface : discovers
    PluginLoader o-- VtuberPluginInterface : discovers
    PluginSelectionBase --> PluginLoader : selects provider

    MainBootstrap --> AppComposer : run
    AppComposer --> PluginLoader : load_plugins()
    AppComposer --> ModuleConfig : modules.json
    AppComposer --> OptionalPluginLoader : optional imports
    AppComposer --> MemoryBootstrap : memory services
    AppComposer --> ScreenRouterBootstrap : screen router
    AppComposer --> GradioLaunch : port probe
    AppComposer --> GPUDeviceManager : startup placement
    AppComposer *-- RuntimeLifecycle
    AppComposer *-- ExtensionRegistry
    AppComposer --> GameExtensionContext : shared runtime
    AppComposer *-- Input
    AppComposer *-- LLM
    AppComposer *-- Translate
    AppComposer *-- TTS
    AppComposer *-- Vtuber
    AppComposer --> AudioDeviceManager
    AppComposer o-- ScreenVision : optional direct module
    AppComposer o-- SongPlayer : optional direct module
    AppComposer o-- Chess : optional direct module
    AppComposer o-- StarCraft116 : optional direct module
    AppComposer o-- StarCraft2 : optional direct module
    AppComposer o-- StarCraftRemastered : optional direct module
    ExtensionRegistry o-- GameExtensionInterface
    GameExtensionInterface <|.. ChessGameExtension
    GameExtensionInterface <|.. StarCraft116GameExtension
    GameExtensionInterface <|.. StarCraft2GameExtension
    GameExtensionInterface <|.. StarCraft2Extension
    ChessGameExtension --> Chess
    StarCraft116GameExtension --> StarCraft116
    StarCraft2GameExtension --> StarCraft2
    StarCraft2Extension --> StarCraft2GameExtension : shared callback

    Input --> LLM : text event
    ScreenVision --> LLM : observation event
    LLM --> Translate : response event
    Translate --> TTS : translated text
    TTS --> Vtuber : mouth volume
    SongPlayer --> Vtuber : mouth and expression
    Chess --> LLM : text-only reaction
    Chess --> TTS : reaction speech
    StarCraft116 --> LLM : status/game reaction
    StarCraft116 --> TTS : reaction speech
    StarCraft2 --> TTS : telemetry reaction
```

`main.py`는 별도 애플리케이션 클래스가 아니라 `AppComposer().run()`을 호출하는 얇은 진입점입니다.
`AppComposer`가 시작 조립, 플러그인 로딩, Gradio UI 구성, 선택 모듈 로딩, 게임 확장 등록, lifecycle 시작, Gradio launch를 소유합니다.
`MainBootstrap`, `MemoryBootstrap`, `ScreenRouterBootstrap`, `ModuleConfig`, `GradioLaunch`는 파일/함수 역할을 표현하기 위한 도식상의 모듈입니다.
`ScreenVision`, `SongPlayer`, `Chess`, `StarCraft116`, `StarCraft2`, `StarCraftRemastered`는 `PluginSelectionBase` provider가 아니라 `modules.json`으로 켜고 끄는 `AppComposer` 선택 구성 요소입니다.
이 직접 모듈들은 `app_core.optional_plugin_loader`를 통해 선택적으로 import/생성됩니다.
현재 `modules.json` 기준으로 `StarCraft2`, `StarCraft116`, `Chess`, `ScreenVision`, `SongPlayer`는 활성화되어 있고 `StarCraftRemastered`는 비활성화되어 있습니다.

`Hybrid_OpenAI_LLM`은 현재 기본 LLM provider로 표시합니다. `ChatGPT_OpenAI`는 활성화 가능한 LLM provider이지만 기본 선택은 `PluginSelection` 설정과 내장 기본값에 의해 `Hybrid_OpenAI_LLM`이 우선됩니다. <!-- #20260630_kpopmodder: Update architecture docs for app_core bootstrap, lazy provider loading, and direct optional modules. -->
<!-- #20260704_kpopmodder: Updated optional direct-module docs for StarCraft116 and optional_plugin_loader. -->

## 2. 핵심 실행 객체와 메모리/화면 라우팅 구조

```mermaid
classDiagram
    direction TB

    class RuntimeLifecycle
    class EventManager
    class EventSubscription

    RuntimeLifecycle o-- Input : shutdown
    RuntimeLifecycle o-- LLM : shutdown and idle check
    RuntimeLifecycle o-- Translate : shutdown and idle check
    RuntimeLifecycle o-- TTS : shutdown and idle check
    RuntimeLifecycle o-- ScreenVision : optional shutdown
    RuntimeLifecycle o-- SongPlayer : optional idle check
    RuntimeLifecycle o-- Chess : optional shutdown
    RuntimeLifecycle o-- StarCraft116 : optional shutdown
    EventManager *-- EventSubscription

    class LLM
    class LLMContextManager
    class LLMEventDispatcher
    class LLMInputQueueWorker
    class LLMResponsePipeline
    class LLMStreamingChunker
    class LLMInteractionContext
    class LLMMemoryBridge
    class ScreenQuestionRouter
    class ScreenQuestionDecision
    class OpenAIScreenQuestionRouterProvider

    LLM *-- LLMContextManager
    LLM *-- LLMEventDispatcher
    LLM *-- LLMInputQueueWorker
    LLM *-- LLMResponsePipeline
    LLM *-- LLMStreamingChunker
    LLM o-- ScreenQuestionRouter
    LLMResponsePipeline *-- LLMMemoryBridge
    LLMResponsePipeline *-- LLMInteractionContext
    LLMResponsePipeline o-- ScreenQuestionRouter
    ScreenQuestionRouter --> ScreenQuestionDecision
    ScreenQuestionRouter o-- OpenAIScreenQuestionRouterProvider : optional AI route

    class MemoryContextBuilder
    class MemoryStore
    class MemoryRetriever
    class MemoryRouter
    class MemoryRouteDecision
    class OpenAIMemoryRouterProvider
    class MemoryConsolidator
    class RawEventSQLiteStore
    class DerivedMemorySQLiteStore
    class DerivedMemoryBuilder
    class RawEventsJsonl {
        <<file>>
    }
    class LongTermMemoryJson {
        <<file>>
    }

    LLMMemoryBridge o-- MemoryContextBuilder
    MemoryContextBuilder o-- MemoryStore
    MemoryContextBuilder o-- MemoryRetriever
    MemoryContextBuilder o-- MemoryRouter
    MemoryRouter --> MemoryRouteDecision
    MemoryRouter o-- OpenAIMemoryRouterProvider : optional AI route
    MemoryRetriever *-- MemoryConsolidator
    MemoryRetriever o-- DerivedMemorySQLiteStore : optional reference index
    MemoryStore *-- RawEventSQLiteStore
    MemoryStore --> RawEventsJsonl : source of truth
    MemoryStore --> LongTermMemoryJson : manual long-term memory
    RawEventSQLiteStore --> RawEventsJsonl : mirrors JSONL rows
    DerivedMemoryBuilder --> RawEventsJsonl : rebuild source
    DerivedMemoryBuilder --> DerivedMemorySQLiteStore : writes derived index

    class TTS
    class TTSTextProcessor
    class TTSQueueWorker
    class TTSInterruptController
    class TTSMouthAnimator
    class WinSoundAudioPlayer

    TTS *-- TTSTextProcessor
    TTS *-- TTSQueueWorker
    TTS *-- TTSInterruptController
    TTS *-- TTSMouthAnimator
    TTS *-- WinSoundAudioPlayer
    WinSoundAudioPlayer --> TTSMouthAnimator
    TTSInterruptController --> EventManager : INTERRUPT

    class ScreenVision
    class ScreenCapture
    class VisionAnalyzer
    class ObservationPolicy
    class ScreenObservationProcessor
    class AutoWatchController

    ScreenVision *-- ScreenCapture
    ScreenVision *-- VisionAnalyzer
    ScreenVision *-- ObservationPolicy
    ScreenVision *-- ScreenObservationProcessor
    ScreenVision *-- AutoWatchController
    ScreenVision o-- MemoryStore
    ScreenObservationProcessor --> ObservationPolicy
    AutoWatchController --> ScreenCapture : callbacks

    class AudioDeviceManager
    class AudioDeviceRegistry
    class AudioDeviceConfigStore
    class AudioPlaybackController

    AudioDeviceManager *-- AudioDeviceRegistry
    AudioDeviceManager *-- AudioDeviceConfigStore
    AudioDeviceManager *-- AudioPlaybackController
```

메모리 계층은 `raw_events.jsonl`을 복구 가능한 원본으로 두고, `raw_events.sqlite3`는 조회용 미러, `derived_memory.sqlite3`는 선택적인 파생 검색 인덱스로 사용합니다.
`MemoryRouter`와 `ScreenQuestionRouter`는 사용자에게 직접 답하지 않고, 검색/화면 컨텍스트가 필요한지만 판단합니다.

## 3. 주요 provider 플러그인의 내부 구조

```mermaid
classDiagram
    direction LR

    class VoiceInput
    class VoiceInputState
    class MicrophoneRecorder
    class WhisperTranscriber
    class SpeakerIdentifier
    class SpeakerService
    class InterruptController
    class OpenMicController
    class VoiceInputRuntimeController
    class VoiceInputUiController
    class VoiceInputHotkeyController

    VoiceInput *-- VoiceInputState
    VoiceInput *-- MicrophoneRecorder
    VoiceInput *-- WhisperTranscriber
    VoiceInput *-- SpeakerIdentifier
    VoiceInput *-- SpeakerService
    VoiceInput *-- InterruptController
    VoiceInput *-- OpenMicController
    VoiceInput *-- VoiceInputRuntimeController
    VoiceInput *-- VoiceInputUiController
    VoiceInput *-- VoiceInputHotkeyController
    SpeakerService --> SpeakerIdentifier
    InterruptController --> MicrophoneRecorder
    InterruptController --> SpeakerService
    OpenMicController --> MicrophoneRecorder
    OpenMicController --> SpeakerService

    class Hybrid_OpenAI_LLM
    class RouterFirstHybridEngine
    class HybridOpenAISettings
    class CommandOverrideRouter
    class OpenAIRouteProvider
    class MemoryRouterProvider_OpenAI
    class OpenAIChatProvider
    class DisabledLocalLightChatProvider

    Hybrid_OpenAI_LLM *-- RouterFirstHybridEngine
    Hybrid_OpenAI_LLM *-- HybridOpenAISettings
    Hybrid_OpenAI_LLM *-- CommandOverrideRouter
    Hybrid_OpenAI_LLM *-- OpenAIRouteProvider
    Hybrid_OpenAI_LLM *-- MemoryRouterProvider_OpenAI
    Hybrid_OpenAI_LLM *-- OpenAIChatProvider
    Hybrid_OpenAI_LLM *-- DisabledLocalLightChatProvider
    RouterFirstHybridEngine --> OpenAIChatProvider : openai_chat
    RouterFirstHybridEngine --> DisabledLocalLightChatProvider : disabled fallback

    class GPTSoVITS
    class GPTSoVITSTTS
    class TTSSynthesisService
    class GPTSoVITSSettingsController
    class GPTSoVITSConfigManager
    class GPTSoVITSServerManager
    class GPTSoVITSModelManager
    class GPTSoVITSApiClient

    GPTSoVITS *-- GPTSoVITSTTS
    GPTSoVITS *-- TTSSynthesisService
    GPTSoVITS *-- GPTSoVITSSettingsController
    GPTSoVITSTTS *-- GPTSoVITSConfigManager
    GPTSoVITSTTS *-- GPTSoVITSServerManager
    GPTSoVITSTTS *-- GPTSoVITSModelManager
    GPTSoVITSTTS *-- GPTSoVITSApiClient

    class VtubeStudio
    class VTubeStudioAuthManager
    class VTubeStudioConnection
    class VTubeStudioMouthController
    class VTubeStudioBlinkController
    class VTubeStudioSmileController
    class VTubeStudioSongExpressionController

    VtubeStudio *-- VTubeStudioAuthManager
    VtubeStudio *-- VTubeStudioConnection
    VtubeStudio *-- VTubeStudioMouthController
    VtubeStudio *-- VTubeStudioBlinkController
    VtubeStudio *-- VTubeStudioSmileController
    VtubeStudio *-- VTubeStudioSongExpressionController
    VTubeStudioBlinkController --> VTubeStudioSongExpressionController : override check
    VTubeStudioSmileController --> VTubeStudioSongExpressionController : override check
```

## 4. `main.py` 직접 선택 모듈의 내부 구조

```mermaid
classDiagram
    direction LR

    class SongPlayer
    class SongManifest
    class SongEntry
    class SongPlaybackController
    class SongMouthAnimator
    class SongRhythmAnimator
    class EventManager
    class VtubeStudio

    SongPlayer *-- SongManifest
    SongManifest o-- SongEntry : parsed manifest
    SongPlayer *-- SongPlaybackController
    SongPlaybackController *-- SongMouthAnimator
    SongPlaybackController *-- SongRhythmAnimator
    SongPlayer --> EventManager : interrupt before play
    SongMouthAnimator --> VtubeStudio : mouth volume
    SongRhythmAnimator --> VtubeStudio : rhythm expression
    SongPlaybackController --> VtubeStudio : loud-note expression

    class Chess
    class ChessGameController
    class ChessWebServer
    class LC0UCIEngine
    class ChessReactionRuntime
    class ChessReactionPolicy
    class LLM
    class TTS

    Chess *-- ChessGameController
    Chess *-- ChessWebServer
    Chess o-- LC0UCIEngine : optional configured engine
    ChessWebServer --> ChessGameController : local HTTP API
    ChessGameController o-- LC0UCIEngine : UCI bestmove
    ChessGameController --> ChessReactionRuntime : ai_move_applied
    ChessReactionRuntime --> ChessReactionPolicy : prompt and fallback text
    ChessReactionRuntime --> LLM : generate_text_only()
    ChessReactionRuntime --> TTS : receive_input()

    class StarCraft116
    class StarCraft116Config
    class StarCraft116ExporterManager
    class StarCraft116Launcher
    class StarCraft116StatusReader
    class StarCraft116RuntimeState
    class StarCraft116GameEventTailer
    class StarCraft116ReactionRuntime
    class StarCraft116ReactionPolicy

    StarCraft116 *-- StarCraft116Config
    StarCraft116 *-- StarCraft116ExporterManager
    StarCraft116 *-- StarCraft116Launcher
    StarCraft116 *-- StarCraft116StatusReader
    StarCraft116 *-- StarCraft116RuntimeState
    StarCraft116 *-- StarCraft116GameEventTailer
    StarCraft116 --> StarCraft116ReactionRuntime : status callback
    StarCraft116GameEventTailer --> StarCraft116ReactionPolicy : game events
    StarCraft116ReactionRuntime --> StarCraft116ReactionPolicy : prompt and fallback text
    StarCraft116ReactionRuntime --> LLM : generate_text_only()
    StarCraft116ReactionRuntime --> TTS : receive_input()
```

`SongPlayer`, `Chess`, `StarCraft116`은 provider selector에 등록되는 플러그인이 아니라, Gradio 탭과 자체 컨트롤러를 가진 선택 모듈입니다.
`SongPlayer`는 TTS 큐와 분리된 재생 흐름을 사용하고, `Chess`는 Gradio 안에 로컬 웹 보드를 iframe으로 붙입니다.
`StarCraft116`은 BWAPI 프로필 설정, 실행 명령, 상태 조회, 게임 이벤트 tailing, 선택적 LLM/TTS 반응을 관리합니다.

현재 기본 GPU 배치는 `GPUDeviceManager` 기준으로 VoiceInput/Whisper, ScreenVision, GPT-SoVITS를 GPU 1 / `cuda:1` 계열로 설명합니다. 시작 시 preflight 로그가 이 배치를 다시 확인합니다. <!-- #20260630_kpopmodder: Mirror current GPU preflight ownership. -->

## 5. StarCraft2 확장과 엔진 구조

```mermaid
classDiagram
    direction LR

    class AppComposer
    class ExtensionRegistry
    class GameExtensionInterface {
        <<interface>>
        +initialize(context)
        +start()
        +stop()
        +handle_command(command)
        +get_status()
    }
    class GameExtensionContext

    class StarCraft2 {
        <<plugins.StarCraft2.starcraft2>>
        +create_ui()
        +start(config_overrides)
        +stop()
        +get_status()
        +set_status_event_callback(callback)
        +set_tts(tts)
    }
    class StarCraft2GameExtension {
        <<app_core.extensions>>
        +start()
        +stop()
        +handle_command(command)
        +get_status()
    }
    class StarCraft2Bridge
    class StarCraft2Worker
    class StarCraft2EngineRegistry
    class StarCraft2EngineInterface {
        <<interface>>
        +start(config, event_callback)
        +stop()
        +get_status()
    }
    class InternalLAVBotEngine
    class AresSC2BotEngine
    class MicroMachineBotEngine
    class ExternalExeBotEngine
    class ExternalJarBotEngine
    class HumanVsBotLauncher
    class SC2LadderProxyLauncher
    class SC2ObservationTracker

    class StarCraft2Extension {
        <<passive log observer>>
        +start()
        +stop()
        +handle_command(command)
        +get_status()
    }
    class ProBotsLauncher
    class ProBotsLogWatcher
    class SC2EventParser
    class SC2TTSBridge
    class StarCraft2ReactionRuntime
    class StarCraft2ReactionPolicy
    class StarCraft2ReactionTTSAdapter
    class StarCraft2ReactionMemoryRecorder
    class TTS
    class MemoryStore

    AppComposer *-- ExtensionRegistry
    AppComposer --> GameExtensionContext
    ExtensionRegistry o-- GameExtensionInterface
    GameExtensionInterface <|.. StarCraft2GameExtension
    GameExtensionInterface <|.. StarCraft2Extension

    StarCraft2GameExtension *-- StarCraft2Bridge
    StarCraft2GameExtension *-- StarCraft2Worker
    StarCraft2GameExtension --> StarCraft2 : facade/plugin
    StarCraft2GameExtension --> StarCraft2ReactionRuntime : status callback
    StarCraft2ReactionRuntime --> StarCraft2ReactionPolicy : speak/log policy
    StarCraft2ReactionRuntime *-- StarCraft2ReactionTTSAdapter
    StarCraft2ReactionRuntime *-- StarCraft2ReactionMemoryRecorder
    StarCraft2ReactionTTSAdapter --> TTS : cancel/speak
    StarCraft2ReactionMemoryRecorder --> MemoryStore : raw event memory

    StarCraft2 *-- StarCraft2EngineRegistry
    StarCraft2 *-- SC2LadderProxyLauncher
    StarCraft2 *-- SC2ObservationTracker
    StarCraft2EngineRegistry o-- StarCraft2EngineInterface
    StarCraft2EngineInterface <|.. InternalLAVBotEngine
    StarCraft2EngineInterface <|.. AresSC2BotEngine
    StarCraft2EngineInterface <|.. MicroMachineBotEngine
    StarCraft2EngineInterface <|.. ExternalExeBotEngine
    StarCraft2EngineInterface <|.. ExternalJarBotEngine
    StarCraft2EngineInterface <|.. HumanVsBotLauncher

    StarCraft2Extension *-- ProBotsLauncher
    StarCraft2Extension *-- ProBotsLogWatcher
    StarCraft2Extension *-- SC2EventParser
    StarCraft2Extension *-- SC2TTSBridge
    StarCraft2Extension --> StarCraft2GameExtension : shared status callback
```

`StarCraft2GameExtension`은 현재 StarCraft2 facade와 worker 명령 경로를 소유하는 활성 lifecycle adapter입니다. `StarCraft2Extension`은 ProBots/Changeling 로그를 관찰하고 이벤트를 파싱한 뒤 공유 StarCraft2 status callback을 재사용하는 수동 관찰자입니다. 현재 소스에서 LAN Lobby remote-human 코드는 보관용으로 주석 처리되어 있으며 live 다이어그램에는 포함하지 않습니다.
<!-- #20260713_kpopmodder: Document current StarCraft2 facade/observer split and archived LAN Lobby status. -->

## 관계 기호

- `<|--`: 클래스 상속
- `<|..`: 인터페이스 구현
- `*--`: 객체가 구성 요소의 생명주기를 소유하는 합성
- `o--`: 외부에서 전달받거나 공유하는 집약
- `-->`: 이벤트, 콜백 또는 일반 의존 관계
