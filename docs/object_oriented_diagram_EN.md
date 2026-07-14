# LAVI Object-Oriented Architecture

This document summarizes the main project-owned runtime classes and plugin
relationships based on the current codebase. Internal classes from external
libraries, model files, and disabled legacy code are excluded.

## 1. Application and Plugin Architecture

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
    class GameExtensionCompositionService {
        <<app_core.extensions>>
        +compose()
    }
    class ExtensionRegistry {
        <<app_core.extensions>>
        +register()
        +initialize()
        +get()
        +stop_all()
    }
    class GameExtensionContext
    class GameRuntimeContextRegistry
    class GameEventBus
    class GameCommandDTO
    class GameStatusDTO
    class GameResultDTO
    class GameExtensionInterface {
        <<interface>>
        +normalize_command()
        +record_result()
        +record_status()
        +publish_event()
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
    AppComposer *-- GameExtensionCompositionService
    AppComposer *-- GameRuntimeContextRegistry
    AppComposer *-- GameEventBus
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
    GameExtensionCompositionService --> ExtensionRegistry : register and initialize
    ExtensionRegistry o-- GameExtensionInterface
    GameExtensionContext --> GameRuntimeContextRegistry
    GameExtensionContext --> GameEventBus
    GameExtensionInterface --> GameCommandDTO : command contract
    GameExtensionInterface --> GameStatusDTO : status contract
    GameExtensionInterface --> GameResultDTO : result contract
    GameExtensionInterface --> GameEventBus : observer events
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

`main.py` is not a separate application class. It is now a thin entry point
that calls `AppComposer().run()`. `AppComposer` owns startup assembly, plugin
loading, Gradio UI construction, optional module loading, game extension
registration, lifecycle startup, and Gradio launch. `MainBootstrap`,
`MemoryBootstrap`, `ScreenRouterBootstrap`, `ModuleConfig`, and `GradioLaunch`
are diagram-only module roles for files/functions.

`AppComposer` delegates game extension construction and registration to
`GameExtensionCompositionService`. The shared game-extension layer also exposes
`GameCommandDTO`, `GameStatusDTO`, `GameResultDTO`,
`GameRuntimeContextRegistry`, and `GameEventBus`, so command/status/result/event
handoffs can move away from ad-hoc dicts without forcing every game plugin to
change at once.

<!-- #20260715_kpopmodder: Document game extension composition service and shared contracts. -->

`ScreenVision`, `SongPlayer`, `Chess`, `StarCraft116`, `StarCraft2`, and
`StarCraftRemastered` are not `PluginSelectionBase` providers. They are
optional AppComposer components gated by `modules.json` and loaded through
`app_core.optional_plugin_loader`. In the current `modules.json`, `StarCraft2`,
`StarCraft116`, `Chess`, `ScreenVision`, and `SongPlayer` are enabled, while
`StarCraftRemastered` is disabled.

`Hybrid_OpenAI_LLM` is shown as the current default LLM provider.
`ChatGPT_OpenAI` may still be available as an LLM provider, but the built-in
default and `PluginSelection` settings prefer `Hybrid_OpenAI_LLM`.
<!-- #20260704_kpopmodder: Updated optional direct-module docs for StarCraft116 and optional_plugin_loader. -->

## 2. Core Runtime, Memory, and Screen Routing

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

The memory layer keeps `raw_events.jsonl` as the recoverable source of truth.
`raw_events.sqlite3` is a query mirror, and `derived_memory.sqlite3` is an
optional derived search index. `MemoryRouter` and `ScreenQuestionRouter` do not
answer the user directly; they only decide whether memory or screen context is
needed.

## 3. Internal Architecture of Major Provider Plugins

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

## 4. Internal Architecture of Direct `main.py` Optional Modules

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

`SongPlayer`, `Chess`, and `StarCraft116` are selectable modules with their own
Gradio tabs and controllers, not provider-selector plugins. `SongPlayer` keeps
playback separate from the TTS queue. `Chess` embeds a local web board in
Gradio through an iframe. `StarCraft116` manages BWAPI profile setup, launch
commands, status polling, exported game events, and optional LLM/TTS reactions
without merging that path into the generic LLM provider system.

The current default GPU placement is documented through `GPUDeviceManager`:
VoiceInput/Whisper, ScreenVision, and GPT-SoVITS are described as GPU 1 /
`cuda:1`-family placements. Startup preflight logs re-check this placement.
<!-- #20260630_kpopmodder: Mirror current GPU preflight ownership. -->

## 5. StarCraft2 Extension and Engine Architecture

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
        +subscribe_status_events(callback)
        +on_local_human_vs_changeling_click(...)
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
    class StarCraft2FacadeService {
        <<orchestrator>>
        +start(config_overrides)
        +stop()
        +get_status()
        +on_local_human_vs_changeling_click(...)
    }
    class StarCraft2LocalMatchService {
        <<domain service>>
        +start_local_match(command)
        +stop_local_match()
        +get_local_match_status()
    }
    class StarCraft2EventBus {
        <<observer channel>>
        +subscribe(callback)
        +emit(event)
    }
    class StarCraft2Contracts {
        <<DTO/dataclass>>
        +StartResultDTO
        +StopResultDTO
        +LocalMatchRuntimeStatusDTO
        +StarCraft2Event
    }
    class StarCraft2EngineEventService
    class StarCraft2LadderProxyEventService
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
    StarCraft2GameExtension --> StarCraft2EventBus : subscribes
    StarCraft2GameExtension --> StarCraft2ReactionRuntime : event callback
    StarCraft2ReactionRuntime --> StarCraft2ReactionPolicy : speak/log policy
    StarCraft2ReactionRuntime *-- StarCraft2ReactionTTSAdapter
    StarCraft2ReactionRuntime *-- StarCraft2ReactionMemoryRecorder
    StarCraft2ReactionTTSAdapter --> TTS : cancel/speak
    StarCraft2ReactionMemoryRecorder --> MemoryStore : raw event memory

    StarCraft2 *-- StarCraft2EngineRegistry
    StarCraft2 *-- StarCraft2FacadeService
    StarCraft2 *-- StarCraft2LocalMatchService
    StarCraft2 *-- StarCraft2EventBus
    StarCraft2 *-- SC2LadderProxyLauncher
    StarCraft2 *-- SC2ObservationTracker
    StarCraft2FacadeService --> StarCraft2EngineRegistry : start/stop/status
    StarCraft2FacadeService --> StarCraft2LocalMatchService : local match flow
    StarCraft2FacadeService --> StarCraft2Contracts : typed results
    StarCraft2LocalMatchService --> SC2LadderProxyLauncher : launch/stop/status
    StarCraft2LadderProxyEventService --> SC2ObservationTracker : telemetry deltas
    StarCraft2LadderProxyEventService --> StarCraft2EventBus : stdout/game events
    StarCraft2EngineEventService --> StarCraft2EventBus : engine events
    StarCraft2EventBus --> StarCraft2Contracts : StarCraft2Event
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
    StarCraft2Extension --> StarCraft2GameExtension : shared status callback
```

`StarCraft2` is now the UI and assembly surface: it builds the Gradio tab,
holds runtime references, and delegates execution to `StarCraft2FacadeService`.
`StarCraft2FacadeService` is the orchestration boundary for start/stop/status
and the Local Human vs AI button flow. Local match command construction,
runtime preflight, ladder-proxy launch, stdout/game-event parsing, and reaction
TTS/memory handling remain in domain services.

`StarCraft2LocalMatchService`, `StarCraft2EngineEventService`, and
`StarCraft2LadderProxyEventService` are the public service names in code.
The underscore-prefixed names remain as compatibility aliases only.

`StarCraft2EventBus` is the single live event channel for SC2 stdout-derived
events, engine events, and telemetry observations. UI/game extensions subscribe
to it; they do not parse ladder stdout directly. `StarCraft2Extension` is still
intentionally passive: it observes ProBots/Changeling logs, parses events, and
reuses the shared StarCraft2 status callback instead of controlling the main
game facade. LAN Lobby remote-human code is archived/commented out in the
current source and is not part of the live diagram.
<!-- #20260713_kpopmodder: Document current StarCraft2 facade/service/event split and archived LAN Lobby status. -->
<!-- #20260715_kpopmodder: Keep public SC2 service names and legacy aliases documented with source. -->

## Relationship Symbols

- `<|--`: Class inheritance
- `<|..`: Interface implementation
- `*--`: Composition; the owning object controls the component lifecycle
- `o--`: Aggregation; an object is externally supplied or shared
- `-->`: Event, callback, or general dependency
