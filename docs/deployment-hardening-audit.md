# LAVI deployment hardening audit

작성일: 2026-07-16  
대상 저장소: `C:\Vtuber_Souorce_Code\LAVI`  
목표 상태: 새 Windows PC에서 한 명령으로 Core 프로필 설치 후, 네트워크, GPU, API 키, 게임, 모델 없이 smoke startup 성공

## 범위

이번 단계에서는 production code를 수정하지 않는다. 이 문서는 현재 저장소를 파일 기반으로 감사하고, 구현 전에 필요한 dependency, profile, installer, config, CI, smoke 기준을 정확하게 다시 정의한다.

확인한 사실:

- `Get-Location` 결과는 `C:\Vtuber_Souorce_Code\LAVI`였다.
- `git rev-parse --show-toplevel` 결과는 `C:/Vtuber_Souorce_Code/LAVI`였다.
- root `modules.json`은 Git tracked production configuration이다.
- 설치, 테스트, `run.bat`, `main.py`, GitHub Actions는 실행하지 않았다.
- Python 3.14, CUDA 13.0, Torch 버전은 변경하지 않았다.
- 이 단계에서 수정 대상은 `docs/deployment-hardening-audit.md` 하나뿐이다.

확인하지 않은 실행 결과는 성공으로 쓰지 않는다. Windows 전용 동작은 실제 Windows 실행 없이 실패로 단정하지 않고, 파일 근거가 있는 위험으로만 분류한다.

## modules.json 배포 계약

root `modules.json`은 legacy 파일이 아니다. 프로그램의 공식 production/deployment 기본 모듈 설정이며, 반드시 Git에 커밋 및 푸시되고 release/deployment artifact에 포함되어야 한다.

금지 사항:

- root `modules.json` 삭제 금지
- root `modules.json` 이름 변경 금지
- root `modules.json` 자동 migration 금지
- root `modules.json` 자동 rewrite 금지
- root `modules.json`을 Core-safe false-only 설정으로 교체 금지
- installer가 root `modules.json`을 생성하거나 덮어쓰기 금지
- Core 테스트를 green으로 만들기 위해 production module 값을 임의로 비활성화 금지

### 설정 파일 역할

| 파일 | 역할 | 정책 |
| --- | --- | --- |
| root `modules.json` | 공식 production/deployment 기본 모듈 설정 | 반드시 Git tracked 상태여야 하며 커밋/푸시/release artifact에 포함한다. 일반 실행에서 사용자 override가 없으면 이 파일을 사용한다. UI 설정 저장 대상으로 쓰지 않는다. Core 테스트를 위해 값을 모두 false로 바꾸지 않는다. |
| `config/modules.json` | 사용자 또는 장비별 optional override | 존재할 때 root `modules.json`보다 우선한다. 존재하지 않아도 프로그램은 root `modules.json`으로 실행되어야 한다. 사용자 설정 저장은 이 파일에 수행한다. 최초 생성 시 root `modules.json`을 기반으로 초기화할 수 있지만 root 파일은 수정하지 않는다. |
| `config/modules.core.json` | Core CI와 offline smoke 전용 설정 | 반드시 tracked 파일로 추가되어야 한다. 일반 실행에서 자동 선택하지 않는다. `--profile Core` 또는 `--modules-config`로 명시 선택할 때만 사용한다. production `modules.json`을 변경하지 않는다. |
| `config/modules.example.json` | 문서와 예시 전용 | production runtime fallback으로 사용하지 않는다. root `modules.json` 누락을 example 파일로 숨기지 않는다. |

### 설정 resolution 순서

일반 실행:

1. 명시적인 `--modules-config`
2. 사용자 `config/modules.json`
3. tracked root `modules.json`
4. 모두 없으면 actionable startup error

Core smoke:

1. 명시적인 `--profile Core`
2. tracked `config/modules.core.json`

Core profile이 선택되지 않은 일반 실행에서 `config/modules.core.json`을 자동으로 사용하면 안 된다. production 설정과 test 설정은 분리되어야 한다.

## 정정한 symbol과 경로

| 기존 표현 | 실제 코드 기준 정정 | 근거 |
| --- | --- | --- |
| `app_core/module_config.py::ModuleConfig._module_files` | `ModuleConfig` class와 `_module_files` symbol은 없다. module-level `module_enabled(module_name, default=True, current_module_directory=None)`가 내부 `modules_paths` list를 만든다. | `app_core/module_config.py:9`, `app_core/module_config.py:12-16` |
| `plugin_system/loader.py::PluginLoader._module_files` | `_module_files` symbol은 없다. `PluginLoader.__init__`가 `plugin_setting_path`, `legacy_plugin_setting_path`, `plugin_setting_example_path`를 만든다. 이 중 `legacy_plugin_setting_path`는 현재 코드 변수명일 뿐, 정책상 root `modules.json`을 legacy로 분류하지 않는다. `_load_module_settings()`가 현재 순서대로 읽는다. | `plugin_system/loader.py:152-154`, `plugin_system/loader.py:161-174` |
| descriptor discovery가 module import 기반이라는 표현 | 현재 discovery는 `_load_plugins_from_directory()`가 `.py` 파일을 찾고 `_register_descriptors_from_file()`이 AST로 class descriptor를 등록한다. 실제 import는 selected handle construction 때 `import_descriptor_module()`에서 일어난다. | `plugin_system/loader.py:223-230`, `plugin_system/loader.py:239-278`, `plugin_system/loader.py:311-332` |
| `plugin_system/selection.py::PluginSelector` | 실제 class명은 `PluginSelectionBase`다. | `plugin_system/selection.py:64` |
| `PluginSelector.create_plugin_ui()` | 실제 경로는 `plugin_system/selection.py::PluginSelectionBase.create_plugin_ui()`다. 이 함수가 provider 전체에 대해 `_ensure_provider_constructed()`를 호출한다. | `plugin_system/selection.py:119-126` |
| `PluginSelector._ensure_provider_constructed()` | 실제 경로는 `plugin_system/selection.py::PluginSelectionBase._ensure_provider_constructed()`다. | `plugin_system/selection.py:210-224` |
| VTube Studio UI 생성 시 바로 websocket 시작 | 두 경로를 구분해야 한다. `init() -> authenticate() -> connection.start()`는 startup init 경로이고, UI 버튼은 `create_ui() -> on_authenticate_click() -> authenticate()` 경로다. | `plugins/VtubeStudio/VtubeStudio.py:179`, `plugins/VtubeStudio/VtubeStudio.py:302`, `plugins/VtubeStudio/VtubeStudio.py:368-377`, `plugins/VtubeStudio/vtube_studio_core/vtube_studio_connection.py:34-55` |
| ScreenVision analyzer 경로 `plugins/ScreenVision/vision_analyzer.py` | 실제 경로는 `plugins/ScreenVision/screen_vision_core/vision_analyzer.py`다. | `plugins/ScreenVision/screen_vision_core/vision_analyzer.py:24`, `plugins/ScreenVision/screenVision.py:288-300` |
| `TTS.py` 자체가 Core에서 import되면 안 된다는 기준 | 잘못된 기준이다. `TTS.py`는 현재 `PluginSelectionBase` wrapper이며 `AppComposer.create_core_components()`가 `TTS()`를 생성한다. 막아야 하는 것은 optional TTS plugin import/init, ffmpeg download, network/model/process side effect다. | `app_core/app_composer.py:212-233`, `TTS.py:48-56`, `TTS.py:108-109` |

## 현재 상태 표

| 항목 | 확인된 상태 | 배포 준비도 영향 | 목표 상태와의 차이 |
| --- | --- | --- | --- |
| dependency source of truth | `requirements.txt`는 grouped files를 include하고, `requirements_full.txt`, `requirements_org.txt`, `requirements_cuda13_success.txt`, `requirements/constraints-windows-py314-cu130.txt`도 존재한다. Python lock은 없고, repo 안의 lock은 StarCraftRemastered Rust `Cargo.lock`뿐이다. | 새 Windows PC에서 같은 dependency set을 재현하기 어렵다. | 사람이 수정하는 canonical source와 installer가 실제 사용하는 committed lock이 필요하다. |
| `.gitignore`와 lock | `.gitignore:150`에 `*.lock`가 있다. | `.lock` 확장자를 Python lock에 쓰면 커밋에서 빠질 수 있다. | Python lock은 `requirements/locks/*.txt`로 두거나 `.gitignore`에 명시적 예외가 필요하다. |
| installer/preflight | `scripts/install_windows.ps1`는 cu130 Torch exact pin을 설치하고 `requirements.txt`를 설치한다. `scripts/preflight.py`도 Python 3.14와 cu130 Torch exact version을 검사한다. | Core CPU 설치도 CUDA/Torch와 Full dependency 흐름에 묶일 수 있다. | `-Profile`과 `-Accelerator`를 받아 lock을 선택해야 한다. |
| switch scripts | `switch_CPU.bat`는 `torch==2.2.2`, `switch_GPU.bat`는 cu121 index의 최신 Torch를 설치한다. | canonical installer와 충돌한다. | switch scripts는 canonical installer로 위임하거나 deprecated guard를 가져야 한다. |
| module config resolution | `module_enabled()`와 `PluginLoader._load_module_settings()` 모두 현재 코드상 `config/modules.json`, root `modules.json`, `config/modules.example.json` 순서다. | example fallback이 production missing error를 숨길 수 있고 Core smoke와 production config가 명시 분리되어 있지 않다. | 일반 실행은 root `modules.json`을 production default로 사용하고, Core smoke는 `config/modules.core.json`을 명시 사용해야 한다. |
| root `modules.json` | Git tracked production/deployment default다. 현재 root 파일에는 Voice, GPTSoVITS, VTubeStudio, OpenAI, Games 계열 활성 key가 포함된다. | 활성 module이 외부 자원 없이 startup을 종료시키면 production smoke가 실패할 수 있다. | 값을 Core-safe로 바꾸는 것이 아니라, enabled module이 dependency/profile과 `UNAVAILABLE` 진단 contract를 가져야 한다. |
| `config/modules.example.json` | example도 `VoiceInput`, `VtubeStudio`, `ChatGPT_OpenAI`, `Hybrid_OpenAI_LLM`, `GPTSoVITS`, `Local_EN_to_JA`가 true다. | example을 runtime fallback으로 쓰면 production default 누락을 숨긴다. | 문서/예시 전용으로 제한하고 runtime fallback에서 제외해야 한다. |
| provider 기본값 | `temp_default`는 `NoTranslate`, `gpt_sovits`, `VoiceInput`, `AyaLLM`이고, LLM builtin default는 `Hybrid_OpenAI_LLM`이다. | API-key-free LLM provider가 Core 계획에서 빠지면 Core startup contract가 비어 있다. | `NullLLM` 또는 `EchoLLM`이 필요하다. |
| provider construction | `PluginSelectionBase.__init__()`는 선택 provider를 load/init하고, `create_plugin_ui()`는 모든 provider를 construct해 UI를 만든다. | 선택하지 않은 optional provider도 construction될 수 있다. | Core에서는 선택된 null provider만 construct/init되어야 한다. |
| startup network/model/process side effect | ffmpeg download, Hugging Face `from_pretrained`, GPTSoVITS local server start, VTube websocket, game process/download 경로가 존재한다. | 네트워크/GPU/model/game/API key 없는 Core smoke 목표와 충돌한다. | Core offline smoke가 네트워크/model/process attempt 0을 강제해야 한다. Production smoke는 resource 누락을 `UNAVAILABLE`로 검증해야 한다. |
| CI/test collection | CI는 base/windows/dev만 설치하고 `tests` 전체를 marker로 실행한다. optional test files는 module top-level에서 VoiceInput/GPTSoVITS 등을 import한다. | marker deselection 전에 collection import failure가 날 수 있다. | test path 또는 file list로 Core/optional collection을 분리해야 한다. |
| 직접 import dependency | `VTuber.py`와 `LAV_utils.py`는 `tqdm`, `VTuber.py`와 `tts_core/mouth_animator.py`는 `pydub`, `Local_EN_to_JA.py`는 `pysbd`를 직접 import한다. grouped requirements에는 누락되어 있고 frozen snapshots에는 있다. | fresh install import failure 위험이 있다. | direct import dependency는 canonical source에 profile별로 들어가야 한다. |
| sys.path mutation | `app_core/app_composer.py`, `plugins/Hybrid_OpenAI_LLM/Hybrid_OpenAI_LLM.py`, 일부 plugin/test/script에서 `sys.path`를 수정한다. | 장기적으로 import 재현성과 package metadata 전환을 어렵게 한다. | P1/P2에서 제거 계획이 필요하다. |

## dependency source-of-truth 결정

단순히 requirements를 정리하지 않는다. P0에서 다음 구조를 canonical로 선택한다.

| 결정 항목 | 선택 |
| --- | --- |
| 사람이 수정하는 canonical dependency source | `requirements/profiles/*.in`과 `requirements/accelerators/*.in` |
| profile source 파일 | `requirements/profiles/core.in`, `voice.in`, `vision.in`, `games.in`, `full.in` |
| accelerator source 파일 | `requirements/accelerators/cpu.in`, `requirements/accelerators/cu130.in` |
| lock 생성 도구 | `pip-tools`의 `pip-compile` |
| lock 생성 명령 | `.\venv\Scripts\python.exe -m piptools compile --generate-hashes --resolver=backtracking --output-file requirements\locks\windows-py314-<profile>-<accelerator>.txt requirements\profiles\<profile>.in requirements\accelerators\<accelerator>.in` |
| cu130 lock 생성 명령 추가 옵션 | `--extra-index-url https://download.pytorch.org/whl/cu130` |
| installer가 설치할 파일 | raw requirements가 아니라 `requirements/locks/windows-py314-<Profile>-<Accelerator>.txt` |
| installer install 명령 | `python -m pip install --require-hashes -r requirements\locks\windows-py314-<profile>-<accelerator>.txt` |
| lock 파일명 | `requirements/locks/windows-py314-core-cpu.txt`, `core-cu130.txt`, `voice-cpu.txt`, `voice-cu130.txt`, `vision-cpu.txt`, `vision-cu130.txt`, `games-cpu.txt`, `games-cu130.txt`, `full-cpu.txt`, `full-cu130.txt` |
| `.gitignore` 처리 | 현재 `*.lock`가 있으므로 Python lock은 `.txt` 확장자를 사용한다. 만약 `.lock` 확장자로 바꾸면 `!requirements/locks/*.lock` 예외를 추가해야 한다. |
| `requirements.txt` 정책 | compatibility entrypoint로만 유지한다. canonical installer는 사용하지 않는다. |
| `requirements_full.txt` 정책 | historical frozen snapshot으로 보존한다. 새 설치 source나 lock 생성 source로 쓰지 않는다. |
| `requirements_org.txt` 정책 | upstream/original snapshot으로 보존한다. 새 설치 source나 lock 생성 source로 쓰지 않는다. |
| `requirements_cuda13_success.txt` 정책 | compatibility pointer로 보존하되 README에서 `requirements/locks/*-cu130.txt`로 대체되었음을 명시한다. |

이 설계에서 lock을 만들고 installer가 사용하지 않는 상태는 허용하지 않는다. P0 acceptance는 committed lock을 실제 installer가 설치하는지로 판단한다.

## Core provider 구성 결정

Core는 required category마다 외부 자원 없는 provider를 가져야 한다.

| Required category | Core provider | 이유 |
| --- | --- | --- |
| Input | `ManualTextInput` 또는 `NullInput` | microphone, sounddevice, Whisper 없이 startup 가능해야 한다. |
| LLM | `EchoLLM` 또는 `NullLLM` | API key와 local model 없이 startup 가능해야 한다. 현재 `Hybrid_OpenAI_LLM` 기본값은 Core-safe가 아니다. |
| Translation | 기존 `NoTranslate` | 외부 model 없이 동작 가능한 기존 provider 후보가 있다. |
| TTS | `NullTTS` | GPTSoVITS, ffmpeg, audio playback 없이 startup 가능해야 한다. |
| Vtuber | `NullVtuber` | VTube Studio websocket 없이 startup 가능해야 한다. |

`config/modules.example.json`은 Core-safe하지 않다. `VoiceInput`, `VtubeStudio`, `ChatGPT_OpenAI`, `Hybrid_OpenAI_LLM`, `GPTSoVITS`, `Local_EN_to_JA`가 true이기 때문이다. 따라서 P0에서는 `config/modules.core.json`을 별도로 정의한다. active profile과 modules config는 loader마다 추측하지 않고 하나의 중앙 resolver에서 결정한다.

## 주요 side effect 근거

| 위험 | 파일과 symbol | 근거 줄 |
| --- | --- | --- |
| ffmpeg startup download | `TTS.py::TTS.__init__`, `tts_core/ffmpeg_manager.py::ensure_ffmpeg_exists`, `download_ffmpeg` | `TTS.py:108-109`, `tts_core/ffmpeg_manager.py:19-31`, `tts_core/ffmpeg_manager.py:40-45` |
| VoiceInput model load | `VoiceInput.init`, `TransformersWhisperBackend._load_pipeline` | `plugins/VoiceInput/voiceInput.py:154`, `plugins/VoiceInput/voice_input_core/stt_backends/transformers_whisper_backend.py:25-54` |
| GPTSoVITS server/process | `GPTSoVITS.init`, `GPTSoVITSTTS.init`, `GPTSoVITSServerManager.start_server` | `plugins/GPTSoVITS/GPTSoVITS.py:21`, `plugins/GPTSoVITS/GPTSoVITS_TTS.py:125-130`, `plugins/GPTSoVITS/gpt_sovits_core/gpt_sovits_server_manager.py:34-96` |
| VTube Studio websocket | `VtubeStudio.init`, `authenticate`, `VTubeStudioConnection.start`, `websocket_thread` | `plugins/VtubeStudio/VtubeStudio.py:179`, `plugins/VtubeStudio/VtubeStudio.py:302`, `plugins/VtubeStudio/VtubeStudio.py:376-377`, `plugins/VtubeStudio/vtube_studio_core/vtube_studio_connection.py:34-55` |
| VTube Studio UI button path | `create_ui`, `on_authenticate_click`, `authenticate` | `plugins/VtubeStudio/VtubeStudio.py:368-377` |
| ScreenVision auto watch and model | `ScreenVision.create_ui`, `set_auto_watch`, `VisionAnalyzer.load` | `plugins/ScreenVision/screenVision.py:131-190`, `plugins/ScreenVision/screenVision.py:619`, `plugins/ScreenVision/screen_vision_core/vision_analyzer.py:43-88` |
| StarCraft2 runtime download | `StarCraft2RuntimeDownloader._download`, `_snapshot_download` | `plugins/StarCraft2/starcraft2_core/starcraft2_runtime_downloader.py:209-233` |
| game/external process launch | Chess, StarCraft116, StarCraft2 launchers | `plugins/Chess/chess_core/lc0_uci_engine.py:78`, `plugins/StarCraft116/starcraft116_core/starcraft116_launch_executor.py:48`, `plugins/StarCraft2/starcraft2_core/sc2_ladder_proxy_launcher.py:133` |

## P0 실행 계획

P0 목표: clean Windows checkout에서 Core CPU가 canonical lock으로 설치되고, 같은 명령을 두 번 실행해도 성공하며, Core offline smoke가 production `modules.json`을 변경하지 않고 네트워크/model/external process attempt 0으로 종료된다. 별도의 production configuration smoke는 root `modules.json`을 공식 production default로 검증한다.

| P0 항목 | 변경할 정확한 파일 | 작업 | Acceptance criteria |
| --- | --- | --- | --- |
| P0-1 dependency source of truth와 generated lock | 새 파일 `requirements/profiles/core.in`, `voice.in`, `vision.in`, `games.in`, `full.in`; 새 파일 `requirements/accelerators/cpu.in`, `cu130.in`; 새 폴더 `requirements/locks/`; `requirements/dev.txt`; `requirements.txt`; `requirements_full.txt`; `requirements_org.txt`; `requirements_cuda13_success.txt`; `.gitignore` | `pip-tools`를 lock 생성 도구로 고정하고, profile/accelerator별 committed lock을 생성한다. `requirements.txt`는 compatibility entrypoint로 격하한다. historical requirements는 snapshot으로 문서화한다. | installer가 raw requirements가 아니라 committed lock을 `--require-hashes`로 설치한다. `*.lock` ignore와 충돌하지 않는다. Python 3.14과 기존 cu130 pin은 보존된다. |
| P0-2 canonical Windows installer | `scripts/install_windows.ps1`; `scripts/preflight.py`; `run.bat`; `README.md`; `README_EN.md`; `switch_CPU.bat`; `switch_GPU.bat` | installer에 `-Profile Core|Voice|Vision|Games|Full`과 `-Accelerator CPU|cu130`을 추가한다. switch scripts는 canonical installer로 위임하거나 deprecated guard를 건다. | `.\scripts\install_windows.ps1 -Profile Core -Accelerator CPU`가 clean checkout에서 성공한다. 같은 명령을 두 번 연속 실행해도 성공한다. switch scripts가 canonical installer와 충돌하지 않는다. installer 전후 root `modules.json` 내용이 동일하다. |
| P0-3 중앙 config/profile resolver와 production/test config 분리 | 새 파일 `core/profile_resolver.py`; `app_core/module_config.py`; `plugin_system/loader.py`; 새 파일 `config/modules.core.json`; `core/paths.py`; `core/config_manager.py`; `tests/test_repository_contract.py`; `tests/test_plugin_system_imports.py`; 검증 대상 `modules.json`; 검증 대상 `config/modules.example.json` | 중앙 resolver를 추가한다. 일반 실행은 root `modules.json`을 production default로 사용한다. 사용자 override는 `config/modules.json`에 저장한다. Core smoke는 `config/modules.core.json`을 명시적으로 사용한다. production 설정과 test 설정을 분리한다. root `modules.json`은 삭제, rewrite, migrate하지 않는다. | `git ls-files --error-unmatch modules.json` 성공. fresh checkout에 root `modules.json` 존재. `config/modules.json`이 없는 일반 실행에서 root `modules.json` 선택. Core smoke는 production 파일을 변경하지 않고 `config/modules.core.json`을 명시 선택. `config/modules.example.json`은 runtime fallback으로 사용되지 않는다. |
| P0-4 root `modules.json` production contract 검증 | 검증 대상 `modules.json`; `tests/test_repository_contract.py`; 새 테스트 후보 `tests/core/test_modules_contract.py`; dependency profile files | root `modules.json`의 모든 key가 실제 module ID와 일치하는지 검증한다. true로 활성화된 module의 Python dependency가 production/full dependency profile에 선언되는지 검증한다. 실제 module key 오류를 수정해야 한다면 변경할 정확한 key와 이유를 별도 PR/패치에 명시한다. Core-safe 설정으로 전체 교체하지 않는다. | release/deployment artifact에 root `modules.json` 포함. installer가 root `modules.json`을 삭제하거나 생성하지 않음. UI에서 module 설정을 변경해도 root `modules.json`은 변경되지 않고 사용자 override는 `config/modules.json`에 기록. |
| P0-5 Core null providers | 새 파일 `plugins/ManualTextInput/manual_text_input.py` 또는 `plugins/NullInput/null_input.py`; 새 파일 `plugins/EchoLLM/echo_llm.py` 또는 `plugins/NullLLM/null_llm.py`; 기존 `plugins/NoTranslate`; 새 파일 `plugins/NullTTS/null_tts.py`; 새 파일 `plugins/NullVtuber/null_vtuber.py`; `plugin_system/selection.py`; `config/modules.core.json`; `tests/test_plugin_system_imports.py` | Core required categories를 외부 자원 없는 provider로 채운다. `PluginSelectionBase.create_plugin_ui()`가 선택되지 않은 provider를 eager construction하지 않게 한다. | 선택된 null provider만 construct/init된다. optional TTS plugin module은 Core에서 import되지 않는다. `TTS.py` wrapper import 자체는 실패 조건이 아니다. |
| P0-6 startup network/model/process side effect 제거 | `TTS.py`; `tts_core/ffmpeg_manager.py`; `plugins/VoiceInput/voiceInput.py`; `plugins/GPTSoVITS/GPTSoVITS.py`; `plugins/VtubeStudio/VtubeStudio.py`; `plugins/ScreenVision/screenVision.py`; `plugins/StarCraft2/starcraft2_core/starcraft2_runtime_downloader.py`; `tests/core/test_offline_smoke_guards.py` | Core startup에서 ffmpeg download, Hugging Face load/download, websocket, requests, external process launch가 호출되지 않도록 lazy/status 경계를 둔다. Production config에서는 외부 프로그램, API key, model이 없는 활성 module이 전체 프로그램을 종료하지 않고 actionable `UNAVAILABLE` 상태를 표시하게 한다. | Core offline smoke에서 network attempt 0, model load 0, external process launch 0, FFmpeg download call 0. Production smoke에서 resource 누락은 plugin별 `UNAVAILABLE`로 표시되고 process는 종료되지 않는다. |
| P0-7 doctor/profile-aware preflight와 실제 smoke | `scripts/preflight.py`; 새 파일 `scripts/smoke_startup.py`; `app_core/app_composer.py`; `run.bat`; `tests/core/test_offline_smoke.py`; `tests/core/test_offline_smoke_guards.py`; 새 테스트 후보 `tests/core/test_production_modules_smoke.py` | `preflight.py --profile Core --accelerator CPU --offline`와 `smoke_startup.py`를 만든다. Core smoke는 `config/modules.core.json`을 명시 사용한다. Production configuration smoke는 사용자 override 없는 상태에서 root `modules.json`을 선택한다. | Core offline smoke 정상 종료와 shutdown 완료. Production configuration smoke가 root module key/schema와 enabled module/dependency profile 일치를 검증. production 설정을 Core 설정으로 대체하지 않는다. |
| P0-8 profile별 Windows CI, Ruff, pip check, idempotence | `.github/workflows/windows-ci.yml`; `pyproject.toml`; `requirements/dev.txt`; `tests/core/`; `tests/profiles/voice/`; `tests/profiles/vision/`; `tests/profiles/games/`; `tests/profiles/full/` | Core CI와 optional CI를 path 단위로 분리한다. Core CI는 Core lock만 설치하고 Ruff, pip check, Core tests, Core offline smoke, production configuration smoke를 실행한다. optional CI는 missing dependency를 skip으로 숨기지 않는다. | Core test collection과 tests가 성공한다. `ruff check` 성공, `pip check` 성공. optional profile CI는 해당 lock 설치 후 해당 tests만 실행한다. |

### P0 검증 명령

아래 명령은 향후 구현 후 실행할 명령이다. 이번 감사에서는 실행하지 않았다.

```powershell
Get-Location
git rev-parse --show-toplevel
git status --short
git ls-files --error-unmatch modules.json
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install_windows.ps1 -Profile Core -Accelerator CPU
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install_windows.ps1 -Profile Core -Accelerator CPU
.\venv\Scripts\python.exe .\scripts\preflight.py --profile Core --accelerator CPU --offline
.\venv\Scripts\python.exe -m pip check
.\venv\Scripts\python.exe -m ruff check .
.\venv\Scripts\python.exe -m pytest tests\core tests\test_repository_contract.py tests\test_plugin_system_imports.py -m "not gpu and not integration and not network and not slow"
.\venv\Scripts\python.exe .\scripts\smoke_startup.py --profile Core --accelerator CPU --offline --modules-config config\modules.core.json --timeout-sec 30
.\venv\Scripts\python.exe .\scripts\smoke_startup.py --profile Full --accelerator CPU --production-config-smoke --timeout-sec 30
```

P0 최종 acceptance:

- clean Windows checkout에서 Core CPU 설치 성공
- 같은 설치 명령 두 번 연속 성공
- installer가 committed lock을 실제 사용
- `pip check` 성공
- Ruff 성공
- Core test collection 및 tests 성공
- Core offline smoke 성공
- network/model/external process attempt 0
- disabled optional modules import/construct/init 0
- `git ls-files --error-unmatch modules.json` 성공
- fresh checkout에 root `modules.json` 존재
- `config/modules.json`이 없는 일반 실행에서 root `modules.json` 선택
- Core smoke는 production 파일을 변경하지 않고 `config/modules.core.json`을 명시 선택
- installer 전후 root `modules.json` 내용 동일
- installer가 root `modules.json`을 삭제하거나 생성하지 않음
- release/deployment artifact에 root `modules.json` 포함
- UI module 설정 변경 시 root `modules.json`은 변경되지 않고 사용자 override는 `config/modules.json`에 기록
- root `modules.json`의 모든 key가 실제 module ID와 일치
- true로 활성화된 module의 Python dependency가 production/full dependency profile에 선언됨
- 외부 프로그램, API key, model이 없는 활성 module은 전체 프로그램을 종료하지 않고 actionable `UNAVAILABLE` 상태 표시
- `config/modules.example.json`은 runtime fallback으로 사용되지 않음
- Core test를 green으로 만들기 위해 production module 값을 임의로 비활성화하지 않음
- 기존 `config.ini`를 덮어쓰지 않음
- Python 3.14 및 기존 cu130 pin 보존
- switch script 경로가 canonical installer와 충돌하지 않음

## Core smoke와 production configuration smoke

| Smoke | modules config | 목적 | 성공 조건 |
| --- | --- | --- | --- |
| Core offline smoke | `--profile Core`와 tracked `config/modules.core.json` 명시 사용 | 최소 Core profile이 외부 자원 없이 시작/종료 가능한지 검증 | network/model/process attempt 0, FFmpeg download 0, 선택 null provider만 construct/init, production `modules.json` 비변경, 정상 shutdown |
| Production configuration smoke | 사용자 override가 없는 상태에서 tracked root `modules.json` 선택 | 실제 production/deployment default가 schema와 dependency contract를 만족하는지 검증 | root module key/schema 검증, enabled module과 production/full dependency profile 일치, 외부 resource 누락은 plugin별 actionable `UNAVAILABLE`, production 설정을 Core 설정으로 대체하지 않음 |

## P1 실행 계획

| P1 항목 | 변경할 정확한 파일 | 목표 |
| --- | --- | --- |
| plugin lifecycle/state contract | `plugin_system/loader.py`; `plugin_system/selection.py`; `plugin_system/registry.py`; `app_core/optional_plugin_loader.py`; 관련 tests | `READY`, `DISABLED`, `UNAVAILABLE`, `BROKEN`, `RUNNING` 상태와 construct/init/start/shutdown contract를 명확히 한다. |
| actionable UNAVAILABLE diagnostics | VoiceInput, GPTSoVITS, VTubeStudio, ScreenVision, game plugins; `app_core/app_composer.py`; UI status tests | missing dependency/resource를 crash가 아니라 사용자 조치 가능한 `UNAVAILABLE` status로 표시한다. |
| API version/capability contract | `plugin_system/interfaces.py`; plugin descriptors; plugin tests | plugin이 지원하는 capability와 required resources를 metadata로 노출한다. |
| remaining external plugin lazy initialization | optional plugins 전체 | Core 외 profile에서도 model/server/process는 사용자가 기능을 켤 때까지 지연한다. |
| sys.path mutation 제거 | `app_core/app_composer.py`; `plugins/Hybrid_OpenAI_LLM/Hybrid_OpenAI_LLM.py`; `plugins/vitsTTS/__init__.py`; scripts/tests의 import setup | package metadata와 import 경계를 정리한다. |

## P2 실행 계획

| P2 항목 | 변경할 정확한 파일 | 목표 |
| --- | --- | --- |
| 정식 package metadata와 release artifact | `pyproject.toml`; 새 build/release scripts; README | editable/local path 의존을 줄이고 release artifact를 만든다. |
| 대형 파일 구조 분리 | model/cache/output 관련 docs와 `.gitignore`; plugin resource docs | 모델, cache, generated output, native binary의 위치와 추적 정책을 분리한다. |
| subprocess plugin isolation | GPTSoVITS, game launchers, local model servers | external process plugin을 격리하고 lifecycle/timeout/log contract를 통일한다. |
| 장기 architecture cleanup | `app_core`, `plugin_system`, `core`, plugin folders | profile, plugin, config, UI composition의 장기 구조를 단순화한다. |

## CI collection 경계

pytest marker만으로 optional test collection import를 막는 설계는 부족하다. 현재 `tests/test_voice_input_imports.py`는 module top-level에서 VoiceInput core modules를 import하고, `tests/test_gpt_sovits_imports.py`는 module top-level에서 GPTSoVITS modules를 import한다.

근거:

- `tests/test_voice_input_imports.py:24-35`
- `tests/test_gpt_sovits_imports.py:14-26`
- `.github/workflows/windows-ci.yml:12`
- `.github/workflows/windows-ci.yml:30-31`
- `pyproject.toml:3`
- `pyproject.toml:16`

선택한 방향:

- test tree를 `tests/core`, `tests/profiles/voice`, `tests/profiles/vision`, `tests/profiles/games`, `tests/profiles/full`로 분리한다.
- Core CI는 optional test path를 수집하지 않는다.
- optional CI는 해당 profile lock을 설치하고 해당 profile path를 실행한다.
- optional dependency missing을 단순 skip으로 숨기지 않는다. 해당 profile CI에서는 missing dependency가 실패다.

Core CI 명령:

```powershell
python -m pip install --require-hashes -r requirements\locks\windows-py314-core-cpu.txt
python -m pip install -r requirements\dev.txt
python -m ruff check .
python -m pytest tests\core tests\test_repository_contract.py tests\test_plugin_system_imports.py -m "not gpu and not integration and not network and not slow"
python -m pip check
python scripts\smoke_startup.py --profile Core --accelerator CPU --offline --modules-config config\modules.core.json --timeout-sec 30
python scripts\smoke_startup.py --production-config-smoke --timeout-sec 30
```

Optional CI 예:

```powershell
python -m pip install --require-hashes -r requirements\locks\windows-py314-voice-cpu.txt
python -m pip install -r requirements\dev.txt
python -m pytest tests\profiles\voice -m "not gpu and not integration and not network and not slow"
python -m pip check
```

## offline smoke 강제 방식

`--offline` flag만 추가하는 것은 acceptance가 아니다. smoke command는 다음 접근을 감지하면 실패해야 한다.

- `socket.socket`, `socket.create_connection`
- `requests`, `urllib`, `websocket.WebSocketApp`
- `transformers` / Hugging Face `from_pretrained`
- `huggingface_hub.snapshot_download`
- `tts_core.ffmpeg_manager.download_ffmpeg`
- `subprocess.Popen`
- game server/process start

smoke에는 timeout과 정상 shutdown 검증이 있어야 한다. 성공 조건은 "예외 없이 시작"이 아니라 "attempt counters가 모두 0이고 shutdown 완료"다.

## Rollback 전략

이번 감사 문서 rollback:

```powershell
git restore -- docs/deployment-hardening-audit.md
```

향후 구현 rollback:

- installer/lock 변경은 `scripts/install_windows.ps1`, `scripts/preflight.py`, `requirements/profiles`, `requirements/accelerators`, `requirements/locks` 단위로 되돌린다.
- profile resolver/config 변경은 `core/profile_resolver.py`, `app_core/module_config.py`, `plugin_system/loader.py`, `config/modules.core.json` 단위로 되돌린다.
- root `modules.json`은 rollback 대상이 아니라 검증 대상이다. 실제 key 오류 수정이 필요한 경우에도 별도 근거와 정확한 key 단위로만 다룬다.
- null provider 변경은 새 provider plugin files와 `plugin_system/selection.py` 단위로 되돌린다.
- offline smoke 변경은 `scripts/smoke_startup.py`, smoke tests, startup side-effect patches 단위로 되돌린다.
- root `modules.json`, `config.ini`, 사용자 config는 삭제하거나 덮어쓰지 않는다.

## 변경하면 안 되는 영역

- root `modules.json` 삭제, 이동, 이름 변경, 자동 migration, 자동 rewrite
- root `modules.json`을 Core-safe false-only 값으로 교체
- Python 3.14 기준 자체
- CUDA 13.0 / cu130 Torch pin 자체
- 기존 user `venv`
- 모델 cache와 대형 model files
- GPT-SoVITS 외부 설치 경로와 weight files
- StarCraft native 실행 파일, DLL, ladder proxy binary
- 사용자 API key, VTube Studio token, local credential
- 광범위 cleanup, recursive delete, `git reset`, `git clean`

## 실제 Git 증거

`git status --short`:

```text
?? docs/deployment-hardening-audit.md
```

`git diff --stat`:

```text
<no output>
```

`git diff -- docs/deployment-hardening-audit.md`:

```text
<no output>
```

참고: `docs/deployment-hardening-audit.md`는 아직 untracked 파일이므로 일반 `git diff` 출력에는 포함되지 않는다. 문서 하나만 untracked인지 확인하기 위해 `git status --short`를 함께 사용했다.

`git ls-files --error-unmatch modules.json`:

```text
modules.json
```

## 미확인 사항과 이유

| 항목 | 이유 |
| --- | --- |
| clean Windows install 실제 성공 여부 | installer를 실행하지 않았다. |
| dependency lock 생성 가능 여부 | `pip-tools`를 설치하거나 실행하지 않았다. |
| `run.bat` startup 실제 결과 | Gradio/runtime side effect를 실행하지 않았다. |
| Core offline smoke 실제 결과 | smoke script가 아직 구현되지 않았다. |
| production configuration smoke 실제 결과 | production smoke command가 아직 구현되지 않았다. |
| 네트워크 차단 상태 동작 | 네트워크 차단 환경을 만들지 않았다. |
| GPU 없는 PC 결과 | 현재 장비 조건을 변경하지 않았다. |
| GitHub Actions 결과 | CI를 실행하지 않았다. |
| root `modules.json` 모든 key와 dependency profile 일치 | 이번 단계에서는 계약을 문서화했고, 전체 검증 테스트는 아직 구현/실행하지 않았다. |
| pip resolver 결과 | dependency install을 실행하지 않았다. |

## 조사에 사용한 주요 명령

```powershell
Get-Location
git rev-parse --show-toplevel
git status --short
git ls-files --error-unmatch modules.json
Select-String -Path docs/deployment-hardening-audit.md -Encoding UTF8 -Pattern "legacy|migration|오염|제외|migrate|modules\.json|modules.core|modules.example"
rg -n "modules\.json|deployment hardening|read-only audit" C:\Users\jaewo\.codex\memories\MEMORY.md
```
