# AGENTS.md

This file contains project-specific instructions for AI coding agents such as Codex.

The goal of this file is to keep the project stable, prevent accidental dependency/version changes, and make future AI-assisted coding work consistent.

---

## 0. Critical Safety Gate for Codex

These rules override all other instructions in this file.

### Active repository root

For this project, the active repository root is:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Codex must treat this path as the default and primary write boundary.

Before editing, moving, deleting, generating, or cleaning up files, Codex must verify both the current directory and the Git repository root:

```bat
cd
git rev-parse --show-toplevel
```

If the current directory or Git root is not the intended repository root, Codex must stop and ask the user.

### Write permission boundary

By default, Codex may only modify files inside:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Do not modify files outside this repository unless the user explicitly provides an exact target path and confirms the operation.

The following folders are read-only by default:

```bat
C:\Program Files (x86)\StarCraft II
C:\Users
C:\Users\*\Desktop
C:\Users\*\Documents
C:\Users\*\Downloads
C:\Users\*\Pictures
C:\Users\*\Videos
C:\Windows
C:\Program Files
C:\ProgramData
```

`C:\Program Files (x86)\StarCraft II` may be inspected for integration or debugging, but must not be modified, cleaned up, moved, renamed, or deleted unless the user explicitly requests a specific file-level change.

### Test isolation directory

The default directory for isolated testing, reproduction scripts, exploratory checks, temporary test fixtures, and test-generated artifacts is:

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation
```

Repository-relative form:

```text
test\test_Isolation
```

When creating a new ad-hoc test or a temporary test-only file, Codex must use this directory unless:

* the repository already has an established test location for that specific component, or
* the user explicitly provides a different path.

Do not automatically move existing tests into this directory.
Do not place production runtime code, model files, secrets, or permanent user configuration in this directory.
Do not treat this directory as an automatic cleanup target. All deletion and cleanup safety rules still apply.

### Forbidden destructive commands

Never run broad destructive commands.

Forbidden commands include, but are not limited to:

```bat
git clean -fdx
git clean -xdf
git reset --hard
rm -rf
del /s
rmdir /s
rd /s
Remove-Item -Recurse
Remove-Item -Recurse -Force
robocopy /MIR
```

Never run deletion commands against:

```bat
C:\
C:\Users
Desktop
Documents
Downloads
AppData
Program Files
Windows
any parent folder of the active repo
```

### Cleanup policy

Repo cleanup means updating `.gitignore` first.

Do not mass-delete files just because they match patterns such as:

```text
__pycache__/
*.pyc
tmp_pycache/
plugins/**/bin/
plugins/**/obj/
*.pdb
pretrained_models/**/.cache/
pretrained_models/**/hub/
pretrained_models/**/snapshots/
pretrained_models/**/blobs/
```

If cleanup seems necessary, Codex must first print the exact candidate list and stop.

Do not delete, unlink, or remove symlinks, junctions, shortcuts, model caches, build outputs, or DLL files without explicit user confirmation.

### Runtime DLL preservation

Do not delete DLL files required for game integration.

Examples that may be required:

```text
BWAPI.dll
BWAPIClient.dll
BWTA.dll
StormLib.dll
Monster.dll
```

If unsure whether a DLL is required, keep it and ask the user.

### Future game folders

For future Minecraft, Helltaker, StarCraft2, or other game folders, Codex must not invent paths.

If a task requires a new game folder path, Codex must ask:

```text
This task requires a path outside the current approved repository boundary.
Please provide the exact folder path and confirm whether it should be added to AGENTS.md before I modify it:

<target path>
```

### Required behavior before risky actions

Before any delete, move, rename, cleanup, reset, or mass file operation, Codex must do this instead:

1. Print the exact target paths.
2. Explain why the operation is needed.
3. Confirm that every path is inside the active repo.
4. Stop and ask the user for confirmation.

Do not perform broad cleanup automatically.

---


## 1. Project Overview

This repository is a Windows-based Live AI Vtuber Interface project.

Main components:

* Python application
* Gradio UI
* Transformers Whisper / PyTorch based STT
* Local LLM using GGUF / GGML models
* llama-cpp-python CUDA backend
* GPT-SoVITS based TTS
* VTube Studio integration
* Audio playback, interrupt, and queue control logic

Primary goal:

* Keep the project runnable and stable on Windows.
* Prefer small, safe, incremental changes.
* Avoid large rewrites unless explicitly requested by the user.
* Preserve working behavior over theoretical code cleanliness.

---

## 2. Important Project Policy

Do not assume that the latest package version is better.

This project has fragile dependencies involving:

* Python version
* CUDA Toolkit
* Visual Studio C++ Build Tools
* PyTorch
* llama-cpp-python
* Gradio
* GPT-SoVITS
* Transformers Whisper / PyTorch STT
* audio playback libraries
* VTube Studio integration

Do not upgrade or downgrade these dependencies unless the user explicitly asks.

Always follow `README.md` for the current installation and runtime instructions.

---

## 3. Repository Location

Expected local project path:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Primary entry point:

```bat
python main.py
```

Do not assume Linux paths unless the user explicitly asks for Linux support.

## 3.1 Repository Boundary / Filesystem Safety Rules

The default writable project boundary is the active repository root:

```bat
C:\Vtuber_Souorce_Code\LAVI
```

Codex may edit files inside this repository only.

`C:\Vtuber_Souorce_Code` is a parent workspace folder, not a broad cleanup target.

`C:\Program Files (x86)\StarCraft II` is read-only by default. It may be inspected for debugging or integration, but Codex must not modify, delete, move, rename, or clean up files there unless the user explicitly provides an exact file path and confirms the change.

Do not treat the entire `C:\Program Files`, `C:\Program Files (x86)`, `C:\Users`, Desktop, Documents, Downloads, AppData, Windows, or any parent folder of the repo as editable.

Before editing, deleting, moving, renaming, or generating any file, Codex must verify that the resolved absolute target path is inside the active repository root.

If a requested task requires editing a path outside the active repository root, Codex must stop and ask the user before making any change.

Required message:

```text
This task requires editing a path outside the active repository root.
Please confirm the exact target path and whether it should be added to AGENTS.md before I modify it:

<target path>
```

For future Minecraft, Helltaker, StarCraft2, or other game folders, ask the user for the exact folder path first. Do not invent or add new paths automatically.


---

## 4. Operating System Rules

This project primarily targets Windows.

When giving commands, prefer Windows CMD or PowerShell examples.

CMD examples are preferred when dealing with:

* `vcvars64.bat`
* Visual Studio C++ environment
* CUDA path setup
* llama-cpp-python build/install commands
* venv activation

Use PowerShell only when it is clearly better or when the user asks for it.

---

## 5. Python Environment Rules

Use the existing project virtual environment unless instructed otherwise.

Typical venv path:

```bat
venv\
```

Do not delete or recreate the virtual environment unless explicitly requested.

Do not commit virtual environment files.

Never modify files inside:

```bat
venv\
```

unless the user specifically asks for debugging inside installed packages.

---

## 6. Dependency Rules

Do not casually modify:

```text
requirements.txt
requirements_full.txt
pyproject.toml
setup.py
```

Do not add heavy dependencies unless necessary.

Do not upgrade core packages without explicit approval.

Fragile packages include but are not limited to:

```text
torch
torchvision
torchaudio
llama-cpp-python
gradio
gradio_client
fastapi
starlette
uvicorn
transformers
tokenizers
huggingface_hub
numpy
scipy
librosa
soundfile
onnxruntime
speechbrain
GPT-SoVITS dependencies
```

If a dependency change is required:

1. Explain why.
2. Show the exact package change.
3. Warn about possible breakage.
4. Ask for user confirmation before applying it.

---

## 7. CUDA / llama-cpp-python Rules

CUDA behavior is fragile.

Do not change CUDA-related settings unless explicitly requested.

Do not change these casually:

```bat
CUDA_PATH
CUDA_PATH_V13_2
PATH
CMAKE_ARGS
FORCE_CMAKE
CMAKE_CUDA_ARCHITECTURES
CMAKE_CUDA_FLAGS
CL
CC
CXX
```

Do not reinstall `llama-cpp-python` unless the user explicitly asks.

When CUDA verification is needed, use a small test first.

Example:

```bat
python -c "from llama_cpp import llama_cpp; print(llama_cpp.llama_print_system_info().decode())"
```

A successful CUDA check should show CUDA device information.

---

## 8. Visual Studio C++ Build Tools Rules

This project may require Visual Studio C++ build environment for CUDA-related Python package builds.

Do not assume `cl.exe` is available until `vcvars64.bat` has been called.

Typical command:

```bat
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
```

If the user has multiple Visual Studio versions installed, do not switch versions without asking.

Prefer the Visual Studio version documented in `README.md`.

---

## 9. Git Rules

Before editing files, check the current Git state.

Recommended commands:

```bat
git status
git branch
```

Do not commit automatically unless explicitly asked.

Do not push automatically unless explicitly asked.

When the user asks for a commit message, provide a clear conventional-style message.

Examples:

```text
docs: update Windows CUDA setup guide
fix: stabilize TTS interrupt handling
refactor: split GPT-SoVITS controller logic
chore: update gitignore for model artifacts
```

---

## 10. Files That Must Not Be Committed

Never commit:

```text
venv/
__pycache__/
*.pyc
*.pyo
*.log
*.tmp
*.bak
*.wav
*.mp3
*.pth
*.ckpt
*.gguf
*.ggml
*.safetensors
*.onnx
*.bin
.cache/
logs/
outputs/
models/
```

Be especially careful with:

* GPT-SoVITS model files
* LLM model files
* Whisper model files
* generated audio files
* temporary debug logs
* local config files containing secrets or tokens

---

## 10.1 Cleanup and Ignore Rules

Cleanup must be handled in this order:

1. Update `.gitignore` first.
2. Check what is already tracked.
3. Print cleanup candidates.
4. Stop and ask the user before removing anything.

Recommended inspection commands:

```bat
git status --short
git ls-files
```

Do not use broad cleanup commands.

Forbidden:

```bat
git clean -fdx
git clean -xdf
rm -rf
del /s
rmdir /s
rd /s
Remove-Item -Recurse
Remove-Item -Recurse -Force
robocopy /MIR
```

If files should no longer be tracked, prefer a precise `git rm --cached` only after user confirmation.

Example safe pattern:

```bat
git rm --cached path\to\specific_file.pyc
```

Do not run recursive delete commands for these patterns:

```text
__pycache__/
*.pyc
*.pyo
tmp_pycache/
plugins/**/bin/
plugins/**/obj/
*.pdb
pretrained_models/**/.cache/
pretrained_models/**/hub/
pretrained_models/**/snapshots/
pretrained_models/**/blobs/
```

For these patterns, add or update `.gitignore` first.

Do not delete runtime DLL files unless the user explicitly confirms the exact file.

Preserve possible runtime DLLs such as:

```text
BWAPI.dll
BWAPIClient.dll
BWTA.dll
StormLib.dll
Monster.dll
```

If unsure whether a file is a build artifact or a runtime dependency, keep it and ask the user.

---

## 11. Code Editing Rules

Always inspect existing code before editing.

Prefer minimal patches.

Do not rewrite a whole file when a small change is enough.

Do not remove working fallback logic unless the user confirms it is no longer needed.

Do not rename classes, files, or config keys unless explicitly requested.

Preserve existing Korean comments when they explain project-specific behavior.

Preserve comments marked with:

```text
#YYYYMMDD_kpopmodder
```

If code marked with `#YYYYMMDD_kpopmodder` must be removed or disabled, do not hard-delete it.

Instead, comment out the old code using the language-appropriate comment syntax, keep the original `#YYYYMMDD_kpopmodder` marker visible, and add a short reason using the same comment style.

Example:

```python
#YYYYMMDD_kpopmodder: Disabled old CUDA path handling because Transformers local LLM no longer uses llama-cpp-python.
# old_code = load_llama_cpp_cuda_path()  #YYYYMMDD_kpopmodder
```

For multi-line removed blocks, comment out the smallest necessary block only.

Do not leave large obsolete sections unless they explain an important compatibility or rollback decision.

When adding a risky or important change, add a comment using the same style.

Example:

```python
#YYYYMMDD_kpopmodder: Keep CUDA path explicit because llama-cpp-python may fail to load CUDA DLLs on Windows.
```

When adding a new class, new module, or new file, include a `#YYYYMMDD_kpopmodder` marker comment near the top of the new code.
This marker is required even if the class or module is otherwise straightforward, because it records project history and AI-agent changes.

Recommended Python examples:

```python
#YYYYMMDD_kpopmodder: Added this module to isolate MemoryRouter preflight behavior.


#YYYYMMDD_kpopmodder: Added this class to keep GPU preflight checks separate from runtime startup.
class GpuPreflightRunner:
    ...
```

For non-Python files, use the equivalent language-appropriate comment syntax while keeping the marker text visible.

Do not spam comments on obvious code outside these required new class/module/file markers.

Use comments only for fragile behavior, compatibility notes, rollback points, or project-specific decisions.

---

## 12. Stability First Rule

This project values runtime stability more than ideal architecture.

Before refactoring, consider:

* Does the current code already work?
* Is this change necessary?
* Could this break audio playback?
* Could this break interrupt behavior?
* Could this break GPT-SoVITS?
* Could this break CUDA loading?
* Could this break Gradio UI state?

If the risk is high, propose the change first instead of directly applying it.

---

## 13. STT / Whisper Rules

STT behavior is fragile.

Be careful with:

* microphone input
* silence detection
* VAD
* interrupt detection
* AI voice feedback filtering
* speaker recognition
* hallucinated text during silence
* `condition_on_previous_text`
* `initial_prompt`
* `no_speech_prob`
* phrase time limits
* cooldown timing

Do not casually change Whisper parameters unless asked.

If the user reports ghost text during silence, check:

* VAD settings
* silence threshold
* `initial_prompt`
* `condition_on_previous_text`
* no-speech filtering
* microphone energy threshold

---

## 14. TTS / GPT-SoVITS Rules

GPT-SoVITS is the main TTS backend.

Be careful with:

* TTS queue
* audio playback locks
* interrupt handling
* sentence splitting
* API server startup
* port 9880
* reference audio
* GPT weight path
* SoVITS weight path
* mouth movement sync
* VTube Studio state reset

Do not re-enable RVC casually.

RVC has historically caused instability, latency, skipped audio, and crashes.

If modifying TTS logic, preserve:

* queue behavior
* interrupt cooldown
* playback cleanup
* mouth close behavior after interruption
* fallback behavior when audio fails

---

## 15. LLM Rules

This project may use local GGUF / GGML LLMs.

Be careful with:

* context length
* GPU layer count
* VRAM usage
* llama-cpp-python backend
* CUDA loading
* prompt formatting
* streaming response behavior
* interruption during generation

Do not increase context length, GPU layers, or model size unless the user explicitly asks.

Large model changes can cause VRAM exhaustion.

---


## 15.1 Memory System Rules

Memory behavior is fragile and can easily pollute LLM answers.

Do not replace the existing keyword-based memory retrieval completely.
Keep the existing keyword trigger logic as a safe fallback path.

When modifying memory retrieval:

* Add a MemoryRouter / MemoryNeedClassifier before memory DB search.
* The router must decide whether stored memory is needed before retrieval.
* The router must not generate user-facing answers.
* The router must return JSON only.
* If the router fails, times out, or returns invalid JSON, recover safely.
* Router failure must not crash the main conversation flow.
* If router parsing fails, fallback to the existing keyword-based memory retrieval.
* If fallback is disabled or also fails, continue without memory instead of crashing.

MemoryRouter output should follow this general shape:

```json
{
  "intent": "search",
  "need_memory": true,
  "reason": "ongoing_project_context",
  "queries": ["LAV Whisper microphone setting"],
  "memory_scope": ["working", "derived", "long_term"],
  "max_items": 5
}
```

Do not treat every Korean word containing "기억" as a search trigger.

Examples:

* "기억나?", "아까", "전에", "그때" may require memory search.
* General programming or command questions should usually avoid memory search.

Prefer this retrieval order for real-time answers:

1. working memory
2. derived_memory.sqlite3
3. long_term_memory.json

Treat raw_events as source material for consolidation, summarization, promotion, or derived memory rebuild.
Do not use raw_events as the primary real-time retrieval store unless explicitly requested.

Before injecting memory into the main LLM prompt:

* Deduplicate results.
* Limit Top-K results.
* Avoid long raw logs.
* Convert relevant memory into short bullet summaries.
* Keep memory context concise.

Example prompt context:

```text
[Relevant Memory]
- The user uses Whisper large-v3 in LAVI.
- AudioDeviceManager and VoiceInput had an input_device_index synchronization issue.
- input_device_index_callback returning None should fallback to the system default input device.
```

Memory router config should be configurable when possible.

Recommended config keys:

```json
{
  "memory_router_enabled": true,
  "memory_router_timeout_sec": 3,
  "memory_router_max_items": 5,
  "memory_router_fallback_to_keyword": true,
  "memory_router_provider": "auto"
}
```

Do not hardcode router behavior if the existing config system can support it.

When adding tests for memory routing, include at least:

* General question returns need_memory=false.
* Previous-context project question returns need_memory=true.
* LAV / Whisper / ScreenVision project-context question returns need_memory=true.
* Invalid JSON falls back safely.
* Timeout or router exception falls back safely.
* need_memory=true with empty queries uses the user input as fallback query.
* max_items is clamped to the configured limit.

---

## 16. Gradio UI Rules

Gradio behavior may break between versions.

Do not upgrade Gradio unless explicitly requested.

When editing UI code:

* Preserve existing event flow.
* Preserve queue behavior unless testing queue issues.
* Avoid changing component names unnecessarily.
* Avoid breaking live textbox behavior.
* Avoid breaking audio device dropdown behavior.
* Avoid breaking refresh/fallback behavior.

If Gradio warnings appear but the app works, do not over-fix them without user approval.

---

## 17. Audio Device Rules

Audio device handling is important for this project.

Be careful with:

* input device selection
* output device selection
* default fallback
* device refresh
* device index changes
* Windows audio device names
* USB audio devices

When modifying audio device code, preserve fallback to default device.

---

## 18. VTube Studio Rules

VTube Studio integration must remain stable.

Be careful with:

* token authentication
* websocket connection
* mouth open / close state
* expression state
* interrupt cleanup
* reconnect behavior

If TTS is interrupted, the mouth should close properly.

Do not change token handling unless explicitly requested.

Do not commit local VTube Studio tokens.

---

## 19. Config Rules

Project configuration files are part of the repository and must be tracked in Git by default.

Configuration files required to install, run, test, reproduce, or maintain the project must be committed and uploaded to the remote Git repository.

This includes, but is not limited to:

```text
config/
plugins/**/config.json
*.config.json
default_config.json
settings.default.json
configuration schemas
configuration migration files
configuration documentation
```

Do not add the entire `config/` directory or broad configuration-file patterns to `.gitignore` merely because settings may differ between computers.

When creating or modifying a configuration file, Codex must:

1. Keep the configuration structure, keys, safe defaults, and documentation in Git.
2. Verify that the configuration file is tracked unless it contains secrets or private credentials.
3. Replace secrets with empty values, safe placeholders, or environment-variable references before staging or committing.
4. Preserve backward compatibility for existing configuration keys and loading behavior when possible.
5. Check `.gitignore`, `git status --short`, and `git ls-files` to ensure required configuration files are not accidentally excluded.
6. Report any required configuration file that is currently ignored or untracked.

Safe tracked values may include:

```json
{
  "memory_router_enabled": true,
  "memory_router_timeout_sec": 3,
  "memory_router_max_items": 5,
  "openai_api_key": "",
  "huggingface_token": "",
  "external_model_path": ""
}
```

Safe placeholders include:

```text
""
null
"YOUR_API_KEY_HERE"
"${OPENAI_API_KEY}"
"${HUGGINGFACE_TOKEN}"
```

The following values must not be committed:

```text
real API keys
access tokens
passwords
private credentials
VTube Studio authentication tokens
personal access tokens
private server credentials
sensitive private network addresses
private user information embedded in paths or values
```

When a configuration file requires secrets or private machine-specific values, use a tracked base configuration and a local override structure where practical:

```text
config/config.json          # tracked project configuration
config/config.schema.json   # tracked configuration schema
config/local_config.json    # ignored only when it contains private values
.env.example                # tracked environment-variable template
.env                        # ignored secret values
```

A configuration file must not be excluded from Git solely because it is a configuration file.

Only a secret-bearing or privacy-sensitive local override may remain untracked. When such an override exists, the repository must still contain a tracked default, template, schema, or example that preserves every required key and explains how to configure the project.

If an existing configuration file mixes shareable settings with secrets, Codex must not ignore the entire configuration system. Instead, Codex must:

1. Keep shareable settings and the complete configuration structure in a tracked file.
2. Move secrets to environment variables or an explicitly ignored local override.
3. Preserve existing runtime behavior and compatibility where practical.
4. Update `.gitignore`, README, and configuration-loading logic when required.
5. Explain which files are tracked and which values remain local.

Do not overwrite user configuration files casually.

When editing configuration defaults:

* Explain the runtime impact.
* Preserve existing configuration keys when possible.
* Maintain backward compatibility.
* Do not replace real user values with defaults during application startup.

Be careful with files under:

```text
config/
plugins/
voices/
```

Do not hardcode private absolute local paths.

Repository-internal paths should use repository-relative paths.

External paths may use absolute paths when required by runtime behavior, but tracked defaults should use safe examples, empty values, or documented placeholders when the real value contains private information.

---

## 19.1 Internal and External Path Rules

Runtime resource paths and configuration paths must follow these rules:

* Files located inside the active LAVI repository must be stored as paths relative to the repository root.
* Files located outside the active LAVI repository must be stored as absolute paths.
* Relative paths must always be resolved from the active repository root, not from the current working directory.
* Do not use `os.getcwd()` or the process launch directory as the base for project resource paths.
* Existing absolute paths must remain readable for backward compatibility.
* When saving or rewriting configuration:

  * Convert a path to a relative path only when its resolved target is inside the active repository root.
  * Preserve the absolute path when its resolved target is outside the active repository root.
* Do not blindly convert every absolute path to a relative path.
* Repository safety boundaries, approved external application locations, and Codex write boundaries must remain absolute paths.

Examples:

```text
# Inside the LAVI repository: store as relative paths
config/config.json
plugins/GPTSoVITS/config.json
voices/LAVI/reference.wav
pretrained_models/whisper-large-v3

# Outside the LAVI repository: store as absolute paths
C:\Program Files (x86)\StarCraft II
D:\AI_Models\external_model.gguf
C:\GPT-SoVITS\GPT_SoVITS\pretrained_models
```

Path loading must support both forms:

```text
relative path -> resolve from the LAVI repository root
absolute path -> use the absolute path directly
```

Path normalization must be centralized in an existing path/config utility when possible. Do not add independent path-resolution implementations to multiple plugins.

---

## 20. Model File Rules

Model files are large and should not be committed.

Do not move, rename, delete, or regenerate model files unless explicitly requested.

Model file extensions include:

```text
.pth
.ckpt
.gguf
.ggml
.safetensors
.onnx
.bin
```

If a model path is broken, suggest a config/path fix first.

---

## 21. Logging Rules

Logs are useful for debugging but should not be committed.

When adding logs:

* Use existing logger utilities if available.
* Avoid printing secrets.
* Avoid excessive spam in real-time audio loops.
* Prefer clear messages that help locate the failing module.

Do not remove useful debug logs during active troubleshooting unless the user asks.

---

## 22. Testing Rules

After code changes, suggest the smallest relevant test first.

For isolated or ad-hoc testing, use the repository-relative directory:

```bat
test\test_Isolation
```

Resolve it from the active repository root as:

```bat
C:\Vtuber_Souorce_Code\LAVI\test\test_Isolation
```

Testing behavior:

* Put new reproduction scripts, exploratory tests, temporary fixtures, test logs, and test-generated outputs in this directory by default.
* Keep existing established unit or integration tests in their current project-defined locations unless the user explicitly requests migration.
* Do not make production code depend on files under `test\test_Isolation`.
* When practical, configure test output, cache, and temporary paths to stay inside this directory.
* A test isolation directory is not permission for automatic deletion; print exact cleanup candidates and ask for confirmation first.

Basic syntax check:

```bat
python -m py_compile main.py
```

Run application:

```bat
python main.py
```

CUDA backend check:

```bat
python -c "from llama_cpp import llama_cpp; print(llama_cpp.llama_print_system_info().decode())"
```

Dependency consistency check:

```bat
pip check
```

Do not assume the app is fixed just because syntax checks pass.

For audio/TTS/STT changes, runtime testing is required.

---

## 23. README Rules

`README.md` is for human users.

Keep it practical and copy-paste friendly.

When editing README:

* Prefer Windows CMD examples.
* Clearly separate PowerShell and CMD commands.
* Do not include unnecessary advanced theory.
* Include warnings for fragile CUDA / Visual Studio / llama-cpp-python steps.
* Keep Korean and English sections consistent when both exist.
* Do not mention unsupported features as if they are stable.

---

## 24. AGENTS.md Rules

This file is for AI coding agents.

When project rules change, update this file.

Do not remove safety rules unless the user explicitly asks.

If a rule conflicts with the user's latest instruction, follow the user's latest explicit instruction and note the conflict.

---

## 25. Branching Rules

Use branches for risky work.

Recommended branch names:

```text
fix/...
docs/...
refactor/...
test/...
stabilize/...
```

Examples:

```text
docs/update-cuda-readme
fix/tts-interrupt-cleanup
refactor/gpt-sovits-controller
stabilize/audio-device-fallback
```

Do not merge branches unless the user explicitly asks.

---

## 26. Commit Message Rules

Use clear commit messages.

Preferred format:

```text
type: short summary
```

Common types:

```text
fix
docs
refactor
test
chore
build
style
```

Examples:

```text
docs: add AGENTS.md for Codex project rules
fix: prevent TTS queue crash during interrupt
refactor: split Whisper transcription helper
build: document CUDA llama-cpp-python install steps
chore: update gitignore for model files
```

For Korean commit messages, concise Korean is acceptable when the editor and terminal are confirmed to use UTF-8.

To avoid console encoding issues, keep examples in this file ASCII-only.

If Korean text appears broken in Codex, CMD, PowerShell, or logs, do not assume the file is corrupted. Verify UTF-8 decoding first.

Example:

```text
docs: add Codex work rules
```

---

## 27. Push Rules

Do not push automatically.

Before push:

1. Check changed files.
2. Check staged files.
3. Confirm commit message.
4. Confirm branch.
5. Push only when the user asks.

Recommended commands:

```bat
git status
git log --oneline -5
git push
```

VS Code GUI push is acceptable.

---

## 28. VS Code Rules

VS Code GUI is acceptable for:

* reviewing diffs
* staging files
* committing
* pushing
* checking changed files

Before committing with VS Code GUI, verify that unwanted files are not staged.

Especially check for:

```text
venv/
logs/
models/
*.pth
*.ckpt
*.gguf
*.ggml
__pycache__/
```

---

## 29. Refactoring Rules

Refactoring is allowed only when it improves maintainability without breaking behavior.

Mandatory responsibility split trigger:

* If a class, module, source file, service, manager, controller, adapter, or facade has two or more independent responsibilities, Codex must split those responsibilities into separate focused units before adding more behavior.
* Do not postpone this split as a future cleanup when the current task touches that code.
* Keep each extracted unit focused on one primary responsibility, with a name that states that responsibility clearly.
* Preserve public APIs, config keys, runtime behavior, and compatibility paths unless the user explicitly approves a breaking change.
* If the split cannot be done safely within the current task, stop expanding the mixed-responsibility code and report the blocker instead of adding more logic to it.

Readability-first separation preference:

* Prefer more small, explicit classes/files over fewer dense classes when that makes the code easier to read, test, and extend.
* Do not avoid a split merely because it increases the class or file count.
* Optimize for clear responsibility boundaries first; performance, allocation count, and class-count efficiency are secondary unless there is a measured runtime problem or a user explicitly asks for optimization.
* Avoid clever consolidation that hides responsibilities behind generic utility classes, broad managers, or overloaded facades.
* If in doubt, choose the structure that a future maintainer can understand fastest.

Preferred refactoring style:

* Extract small helper class.
* Extract small helper function.
* Keep old behavior intact.
* Preserve public method names.
* Preserve config keys.
* Add fallback path if needed.

Avoid:

* large rewrites
* speculative abstractions
* dependency injection frameworks
* changing too many files at once
* replacing stable code only for style reasons

## Code Structure / Refactoring Awareness Rule

When writing or modifying code, always consider refactoring opportunities and folder structure.

Do not only make the code “work”; also check whether the new code belongs in the correct module, class, or folder.

Before adding new logic:

* Check whether an existing module, helper, service, manager, controller, or utility file already handles similar behavior.
* Avoid putting too much logic into one large file or one large class.
* If a function, class, or file becomes too large, propose a small safe extraction instead of continuing to expand it.
* Keep related code grouped together by responsibility, feature, domain, or component.
* Evaluate folder and package placement during every production code change, not only during class or file separation.
* Create or reorganize folders when the current structure does not clearly represent responsibility, feature, domain, or component boundaries.
* Do not create arbitrary folders that add depth without establishing a meaningful ownership boundary.
* Prefer clear boundaries between UI, application logic, core logic, game integration, memory, audio, TTS, STT, vision, configuration, infrastructure, and adapters.
* Preserve existing behavior while improving structure.
* Refactor only in small, safe steps.

When refactoring:

* Preserve public method names unless the user explicitly approves a rename.
* Preserve config keys and file paths unless the user explicitly approves a change.
* Keep backward-compatible fallback paths when possible.
* Avoid large rewrites.
* Explain why the refactor improves maintainability, portability, or extensibility.


---

## 29.1 Folder and Package Organization Rule

Production files must be grouped into folders and packages by responsibility, feature, domain, or component boundary.

Folder organization is a continuous code-structure rule. It is not a subordinate task that applies only when classes are split into separate files.

Whenever Codex creates, modifies, splits, moves, or reorganizes production code, it must evaluate whether the affected files are located in the correct folder or package. If the current structure does not clearly represent ownership and responsibility, Codex must include the necessary folder organization in the same structural change, limited to the directly affected component.

### Core Principle

Use this structure principle:

```text
One primary class, type, or responsibility per file.
Related files grouped by responsibility, feature, domain, or component.
```

Files must not be grouped only by file extension, class suffix, or implementation type.

Prefer feature-oriented or component-oriented folders over global type-oriented folders.

Preferred:

```text
audio/
    devices/
        audio_device_manager.py
        input_device_resolver.py
    playback/
        audio_player.py
        playback_controller.py

memory/
    routing/
        memory_router.py
        memory_need_classifier.py
    storage/
        derived_memory_store.py
        long_term_memory_store.py

games/
    starcraft/
        extension/
            starcraft_extension.py
        bridge/
            starcraft_bridge.py
        adapters/
            bwapi_adapter.py
```

Avoid grouping unrelated code into global dumping-ground folders such as:

```text
classes/
managers/
controllers/
services/
helpers/
utils/
common/
misc/
```

A generic folder may be used only when its contents are genuinely shared across multiple features and the folder has one clearly explainable responsibility.

For example:

```text
audio/devices/audio_device_manager.py
memory/routing/memory_router.py
games/starcraft/bridge/starcraft_bridge.py
```

are preferred over:

```text
managers/audio_device_manager.py
services/memory_router.py
adapters/starcraft_bridge.py
```

### Required Folder Evaluation

During every production code change, Codex must check:

1. Which responsibility, feature, domain, or component owns each affected file.
2. Whether an appropriate existing folder or package already exists.
3. Whether the current folder mixes unrelated responsibilities.
4. Whether related files should be grouped into a clearer package boundary.
5. Whether a new folder would clarify ownership or merely add unnecessary depth.
6. Which imports, exports, tests, configuration references, dynamic import paths, build files, and documentation would be affected.
7. Whether the proposed structure would introduce circular dependencies.
8. Whether backward-compatible import or loading paths must be preserved.

Do not leave a file in an unrelated folder merely because moving it was not explicitly requested.

Do not perform unrelated repository-wide folder reorganization during a small feature, bug-fix, or maintenance task.

### When Folder Organization Is Required

Create or reorganize folders when one or more of the following conditions apply:

* Files with different responsibilities are mixed in the same folder.
* A new feature, domain, component, plugin, adapter, or external integration boundary is being introduced.
* Several related files form one independently understandable, testable, replaceable, removable, or extensible component.
* A flat folder has grown enough that related files are difficult to locate or understand.
* The folder name no longer accurately describes all files contained within it.
* UI, application logic, domain logic, infrastructure, and external integration code are mixed without clear boundaries.
* Multiple files share a responsibility or feature relationship that is not represented by the directory structure.
* New code would otherwise be added to an unrelated existing folder.
* A class, function, interface, event, adapter, worker, controller, configuration module, or other production unit is being separated and needs a clearer package boundary.

Folder organization must be performed when it is necessary for maintainability, portability, extensibility, or clear responsibility boundaries.

Do not postpone an obviously necessary folder separation merely because the current task is not explicitly named as a refactoring task.

### When a New Folder Must Not Be Created

Do not create a new folder merely because:

* One new file was added.
* One class was moved into its own file.
* Every individual class could technically have its own directory.
* More directories would make the repository appear organized.
* The folder would contain only one file without representing a meaningful component boundary.
* The new directory would increase navigation depth without clarifying ownership.
* An appropriate existing folder already represents the responsibility.

Folders represent groups of related responsibilities or components, not individual classes.

Avoid:

```text
memory_router/
    memory_router.py

audio_player/
    audio_player.py
```

unless each folder represents a real component with closely related implementation, interface, configuration, resource, or test files.

### Folder Boundary Guidelines

Recommended major responsibility boundaries may include:

```text
application/
core/
interfaces/
events/
plugins/
extensions/
audio/
stt/
tts/
vision/
memory/
games/
config/
ui/
infrastructure/
adapters/
```

These are examples, not mandatory fixed directories.

Inspect and reuse the repository's existing structure before creating a new boundary.

Do not create parallel folders with overlapping meanings, such as:

```text
audio/
audios/
sound/
playback_audio/
```

Choose one clear ownership boundary and place related files beneath it.

A folder must have one concise and explainable responsibility.

### Required Plan Before Folder Creation or Reorganization

Before creating a new production folder or moving existing production files, Codex must print:

```text
Current structure:
- <current path>: <responsibility>

Proposed structure:
- <proposed path>: <responsibility>

Files to create:
- <exact path>

Files to move:
- <current path> -> <new path>

References to update:
- imports
- package exports
- tests
- configuration paths
- dynamic imports
- plugin registration
- build or packaging files
- documentation

Reason:
- <why the proposed folder represents a clearer responsibility, feature, domain, or component boundary>
```

All source and destination paths must resolve inside the active repository root.

Creating an empty folder does not require separate approval, but moving or renaming existing files remains subject to the repository safety rules. Before any move or rename, print the exact source and destination paths and stop for user confirmation unless the user's latest instruction already explicitly approves those exact paths.

### Imports, Exports, and Compatibility

When creating or reorganizing folders, update all affected:

* Python imports
* Python `__init__.py` files
* package public exports
* relative and absolute imports
* dynamic import strings
* plugin and extension registration paths
* mock and patch target paths
* configuration module paths
* test imports
* C and C++ include paths
* Java package declarations
* build and packaging files
* documentation examples

Do not consider folder organization complete while broken or outdated references remain.

For Python packages, create `__init__.py` when required by the existing repository convention.

Use `__init__.py` for package initialization and deliberate public exports. Do not place unrelated runtime logic in it.

When an existing public import path may still be used, preserve compatibility where practical by re-exporting the moved type from the old path.

Example:

```python
#YYYYMMDD_kpopmodder: Compatibility export after moving MemoryRouter.
from memory.routing.memory_router import MemoryRouter

__all__ = ["MemoryRouter"]
```

Do not remove a compatibility path until repository-wide references have been checked and the user has approved its removal.

### Circular Dependency Prevention

Folder organization must not create circular dependencies.

When multiple components require the same contract, extract it into an appropriate independent module.

Shared contracts may include:

* interfaces
* protocols
* abstract base classes
* event types
* callback types
* DTOs
* enums
* shared data structures

Example:

```text
memory/
    interfaces/
        memory_store_interface.py
    routing/
        memory_router.py
    storage/
        derived_memory_store.py
```

Do not hide circular dependencies through duplicated types, unrelated utility modules, or scattered runtime imports when a clear shared contract can be extracted.

### Incremental Scope

Folder organization must be incremental.

Limit each structural change to the directly affected:

* feature
* package
* plugin
* extension
* game integration
* component
* responsibility group

Do not reorganize the entire repository in one uncontrolled change.

Do not combine folder reorganization with unrelated:

* dependency upgrades
* configuration format changes
* public API renames
* feature additions outside the affected component
* large architectural rewrites

A file or class split and the directly required folder organization may be performed together because they belong to the same structural change.

### Validation

After creating or reorganizing folders, Codex must:

1. List all created folders.
2. List all created and moved files.
3. Search for references to old paths.
4. Verify package exports.
5. Check for circular imports.
6. Run syntax or compile checks.
7. Run the smallest relevant tests.
8. Verify plugin, extension, and dynamic import paths.
9. Inspect `git diff`.
10. Report any remaining runtime validation.

Recommended Python checks:

```bat
python -m compileall <changed_package>
python -m pytest <smallest_relevant_test_path>
git diff --check
git status --short
```

Syntax checks alone are not sufficient when the affected code uses dynamic imports, plugin discovery, configuration-based loading, UI registration, or runtime extension loading.

### Forbidden Folderization Patterns

Do not create a folder containing unrelated classes:

```text
classes/
    audio_device_manager.py
    memory_router.py
    starcraft_worker.py
    gradio_controller.py
```

Do not organize the entire repository globally by class suffix:

```text
managers/
controllers/
workers/
services/
adapters/
```

Do not create meaningless dumping grounds:

```text
utils/
helpers/
common/
misc/
temp/
```

Do not create excessive directory depth without a real responsibility boundary:

```text
src/core/modules/components/services/managers/
```

Do not mix unrelated responsibilities inside a feature folder:

```text
games/starcraft/
    starcraft_extension.py
    memory_router.py
    audio_player.py
    gradio_ui_controller.py
```

### Required Completion Report

After folder organization, Codex must report:

```text
Created folders:
- <folder path>: <responsibility>

Created files:
- <file path>: <responsibility>

Moved files:
- <old path> -> <new path>

Updated references:
- imports
- exports
- tests
- configuration
- dynamic loading
- build or packaging files

Compatibility preserved:
- <old import or API path>

Validation completed:
- <command>: <result>

Runtime validation still required:
- <remaining manual or integration test>
```

Folder organization is not complete merely because directories and files were created.

It is complete only after responsibility boundaries, references, backward compatibility, tests, and runtime loading paths have been verified.

---

## 29.2 File and Type Separation Rule

Production code must follow a one-primary-type-or-responsibility-per-file rule.

The purpose of this rule is to keep classes, types, and modules independently maintainable without changing existing runtime behavior.

### Common Rules

* One source file must contain only one primary class, type, or module responsibility.
* Independently reusable classes or types must be placed in separate files.
* The file name must correspond to the primary class or type name, following the language and existing repository naming convention.
* Do not place multiple independent classes in one file merely to reduce the number of files.
* When adding a new independent class or type, create a new file instead of appending it to an unrelated existing file.
* When splitting a file, update all affected imports, includes, package declarations, namespaces, public exports, build files, and tests.
* If a split introduces a circular dependency, extract the shared interface, data type, callback type, or constant into a separate file.
* Do not create a giant static class, utility class, or miscellaneous helper file to avoid proper separation.
* Preserve existing public APIs, class names, config keys, runtime behavior, and compatibility paths unless the user explicitly approves a change.

### Python

Python production code must follow the one-class-per-file rule.

* A `.py` file may define at most one top-level project class.
* If a module already contains a class, do not add a second top-level class to that module.
* Move each additional class into its own `.py` file and import it where needed.
* Use `PascalCase` for class names and `snake_case.py` for module names.
* Functions, constants, and type aliases that do not belong to a class may remain in a class-free module organized by responsibility.
* A small class must still be separated when it has independent state or responsibility.

Examples:

```text
PluginManager       -> plugin_manager.py
ExtensionRegistry   -> extension_registry.py
MemoryRouter        -> memory_router.py
GameWorker          -> game_worker.py
```

For this rule, Python classes include:

* regular classes
* `@dataclass` classes
* abstract base classes
* protocol classes
* enums declared as classes
* exception classes
* helper classes
* compatibility wrapper classes

When creating a new Python class file, keep the required project-history marker near the top:

```python
#YYYYMMDD_kpopmodder: Added this module to keep one project class per Python file.
```

### C

C does not provide class syntax, so one `.h` / `.c` file pair must represent one primary structure or one cohesive module responsibility.

* One `.h` / `.c` pair must own one primary `struct` or one clearly defined module.
* Declare the primary structure and its public API in the `.h` file.
* Place the corresponding implementation in the matching `.c` file.
* Do not declare multiple independent primary structures in one header.
* Do not collect unrelated global functions in one `.c` file.
* Small `enum` values, constants, callback typedefs, and private helper types directly owned by the primary module may remain with that module.
* Shared `enum` values, constants, typedefs, callback contracts, or data structures used by multiple modules must be placed in separate headers.
* Keep private implementation helpers `static` inside the owning `.c` file when they are not independently reusable.

Examples:

```text
audio_manager.h
audio_manager.c        -> AudioManager structure and related functions

event_bus.h
event_bus.c             -> EventBus structure and related functions
```

When creating a new C header or source file, keep the required project-history marker near the top:

```c
//YYYYMMDD_kpopmodder: Added this module to isolate one C structure or module responsibility.
```

### C++

One `.h` / `.hpp` and `.cpp` file pair must represent one primary class or independently reusable type.

* Give each primary class its own header and implementation file.
* The header and implementation base names must correspond to the primary class or type name.
* Do not declare multiple independent classes in one header.
* Do not implement member functions for multiple independent classes in one `.cpp` file.
* Interfaces, abstract classes, independently reusable `enum class` types, and public helper classes must be placed in separate files.
* A small nested class, private implementation type, or internal helper may remain with the owning class only when it is not referenced independently.
* A PImpl implementation class may remain private inside the owning `.cpp` file.
* Template implementation may remain in the owning header or matching implementation header when required by C++ compilation rules.

Examples:

```text
AudioManager.h
AudioManager.cpp       -> AudioManager

PluginRegistry.h
PluginRegistry.cpp     -> PluginRegistry

EventBus.h
EventBus.cpp           -> EventBus
```

When creating a new C++ header or source file, keep the required project-history marker near the top:

```cpp
//YYYYMMDD_kpopmodder: Added this class file to keep one primary C++ class per file pair.
```

### Java

Java production code must follow the one-top-level-type-per-file rule.

* A `.java` file may contain only one primary top-level project type.
* The file name must exactly match the primary `class`, `interface`, `enum`, `record`, or `@interface` name.
* Do not place multiple independent package-private top-level classes in one file.
* Each independent `class`, `interface`, `enum`, `record`, or annotation type must have its own `.java` file.
* A small nested class may remain inside the owning class only when it is used exclusively by that class and has no independent responsibility.
* Move a nested class into its own file when it grows, is referenced externally, or becomes independently testable or reusable.

Examples:

```text
AudioManager.java      -> AudioManager
PluginRegistry.java    -> PluginRegistry
EventBus.java           -> EventBus
Plugin.java             -> Plugin interface
PluginState.java        -> PluginState enum
```

When creating a new Java type file, keep the required project-history marker near the top:

```java
//YYYYMMDD_kpopmodder: Added this type file to keep one primary Java type per file.
```

### Allowed Exceptions

The following exceptions are allowed only when the additional type has no independent responsibility and is not independently referenced:

* Python `__init__.py` files used only for imports and public exports
* modules containing functions, constants, or type aliases without a class
* short nested classes used only by their owning class
* C++ PImpl implementation types
* C or C++ private helper types used only inside one implementation file
* language-required template implementation kept with its owning C++ template
* lambdas, anonymous classes, and local classes
* small test fixtures or local test doubles tightly coupled to one isolated test
* generated code
* vendored or third-party code that should remain unmodified

Do not use an exception merely because a class or type is small.
Do not use an exception to hide a second independent responsibility in an existing file.

### Refactoring Existing Files

When an existing file contains multiple independent classes or primary types:

1. List every class or primary type and its current file.
2. Propose the exact new file paths before moving code.
3. Split one package, component, or responsibility group at a time.
4. Preserve public names, behavior, configuration, and compatibility paths.
5. Move each independent class or type into its own file or matching header/source pair.
6. Update imports, includes, package declarations, namespaces, exports, build settings, and tests.
7. Check for circular dependencies and extract shared contracts when necessary.
8. Run syntax or compile checks and the smallest relevant tests.
9. Inspect `git diff` and report any runtime testing that remains necessary.

Do not perform a repository-wide class split in one uncontrolled change.
Do not combine file separation with unrelated feature work or large architectural redesign.

---


## 29.3 Inheritance and Common Base Class Rule

Inheritance must be actively considered when multiple project classes share the same stable responsibility, lifecycle, validation flow, execution sequence, or error-handling template.

Do not repeatedly copy the same control flow into sibling classes when the differences can be expressed as small subclass-specific steps.

### Mandatory Inheritance Evaluation Trigger

Before adding or modifying a second class with behavior similar to an existing class, Codex must compare the classes and determine whether they share:

* the same public operation or lifecycle
* the same ordered execution steps
* the same validation, permission, retry, logging, cleanup, or error-handling flow
* the same constructor dependencies
* the same result conversion or response-building flow
* differences limited to an action name, endpoint, command prefix, strategy hook, payload type, or one small execution step

If two or more concrete classes share a stable algorithm or control-flow skeleton, Codex must evaluate a common abstract base class before adding more duplicated logic.

If three or more classes already repeat the same skeleton, Codex must not add another copied implementation. It must first extract or extend a common base class unless doing so would violate substitutability, create an unsafe dependency, or break compatibility.

This evaluation is required even when the current duplicated methods are individually short.

### Preferred Inheritance Pattern: Template Method

Use the Template Method pattern when the overall algorithm must remain consistent but one or more steps vary by implementation.

The base class should:

* own the stable public execution method
* enforce shared ordering and invariants
* perform common validation, permission checks, logging, retry, cleanup, and response conversion
* expose only the smallest necessary protected or abstract hooks
* keep subclass-specific logic out of the shared algorithm

The subclass should:

* declare its domain-specific identity clearly
* override only the variable step or steps
* avoid reimplementing the complete public algorithm
* remain substitutable anywhere the base type is accepted

Python example:

```python
from abc import ABC, abstractmethod
from typing import ClassVar


class MinecraftVerifiedItemActionBase(ABC):
    action_name: ClassVar[str]

    def run(self, item: str, count: int):
        blocked = self._permission_guard.blocked_response(self.action_name)
        if blocked is not None:
            return blocked

        validation = self._validator.validate(self.action_name, item, count)
        if validation is not None:
            return validation

        return self._verified_runner.run(
            action=self.action_name,
            item=item,
            count=count,
            submit=lambda: self._submit(item, count),
        )

    @abstractmethod
    def _submit(self, item: str, count: int):
        raise NotImplementedError
```

```python
class MinecraftCraftAction(MinecraftVerifiedItemActionBase):
    action_name = "craft"

    def _submit(self, item: str, count: int):
        return self._client.craft(item, count)
```

In this structure, permission checks, validation, verified execution, and result handling belong to the base class. Only the bridge operation belongs to the subclass.

### ABC, Interface, and Protocol Selection

Choose the contract type deliberately.

Use an abstract base class when:

* subclasses have a real `is-a` relationship
* shared implementation or state is required
* a stable execution template must be enforced
* subclasses must override one or more well-defined hooks
* duplicated behavior would otherwise exist in every implementation

Use a `Protocol`, interface, or pure abstract contract when:

* only a callable or behavioral contract is required
* implementations may be structurally unrelated
* shared implementation is not appropriate
* dependency inversion across plugins, adapters, extensions, or infrastructure boundaries is the primary goal

A Python `Protocol` provides a structural contract but does not remove duplicated implementation by itself.
If sibling classes implement the same `Protocol` and also duplicate the same algorithm, keep the `Protocol` for the external contract and add an abstract base class for the shared implementation when appropriate.

Example relationship:

```text
MinecraftCommandHandler Protocol
    <- external handler contract

MinecraftItemCommandHandlerBase ABC
    <- shared parsing, validation, and response flow

MinecraftCraftCommandHandler
MinecraftEquipCommandHandler
    <- domain-specific implementations
```

### Inheritance Versus Composition Decision Rule

Inheritance and composition are both valid, but they solve different problems.

Prefer inheritance when:

* the subtype is a specialized form of the base type
* the shared algorithm must not be reordered by each implementation
* subclass identity is meaningful in logs, registration, testing, or public APIs
* most behavior is invariant and only a small hook varies
* preserving named domain classes improves readability and traceability

Prefer composition, delegation, or strategy objects when:

* behavior must be selected or replaced at runtime
* collaborators have independent lifecycles
* there is no true `is-a` relationship
* only a utility function or isolated operation is shared
* multiple independent capabilities must be combined
* inheritance would expose unrelated protected state or methods

Do not use composition merely to avoid inheritance when it causes every sibling class to duplicate the same orchestration flow.

Do not use inheritance merely to share a few unrelated helper functions. Extract a focused function or collaborator instead.

### Preserve Domain-Specific Classes

Do not collapse several meaningful domain classes into one overly generic configurable class solely to reduce the class count.

Prefer thin subclasses when separate class identities are useful for:

* dependency injection wiring
* command or action registration
* plugin discovery
* logging and diagnostics
* tests and mocks
* public imports
* future subclass-specific behavior
* human-readable architecture

For example, these named classes may remain separate:

```text
MinecraftCraftAction
MinecraftGetItemAction
MinecraftGetAndEquipAction
```

while inheriting their common execution flow from:

```text
MinecraftVerifiedItemActionBase
```

A generic class such as `ConfiguredAction(action_name, callback)` is acceptable only when the objects have no meaningful independent domain identity and the generic form is clearly easier to understand.

### Base Class Design Rules

A shared base class must have one clear responsibility.

Required rules:

* Give the base class a name that describes the shared domain role, not a vague name such as `Base`, `Common`, `HelperBase`, or `ManagerBase`.
* Prefer names such as `MinecraftVerifiedItemActionBase`, `MinecraftPrefixCandidateSelectorBase`, or `GameExtensionBase`.
* Keep public behavior in the base class and variable behavior behind protected or abstract methods.
* Keep abstract hooks small and purpose-specific.
* Do not make subclasses override a method only to copy most of the base implementation.
* Do not require subclasses to know the internal ordering of the template algorithm.
* Do not expose mutable base-class state unless subclasses genuinely require it.
* Prefer constructor injection for shared dependencies.
* Document invariants that subclasses must preserve.
* Use `final` or language-equivalent restrictions when a template method must not be overridden and the language/project supports it safely.
* Do not add unrelated responsibilities to a base class because multiple subclasses happen to need them.

### Substitutability Rule

Every subclass must obey the behavioral contract of its base class.

A subclass must not:

* weaken required validation or permission checks
* change the meaning of an inherited public method
* return an incompatible result shape
* require callers to perform subtype-specific preconditions not required by the base type
* silently skip shared logging, cleanup, retry, or error handling
* raise new unexpected exceptions for normal inputs accepted by the base contract

If a proposed subclass cannot follow the base contract, it is not a valid subtype. Use composition or a separate hierarchy instead.

### Language-Specific Inheritance Rules

Apply the same design rule using the language's native mechanism.

* Python: use `ABC` and `@abstractmethod` when shared implementation and enforced hooks are required. Keep `Protocol` for structural contracts.
* TypeScript: use an `abstract class` for shared implementation and `protected abstract` hooks. Use an `interface` when only the external contract is required.
* Java: use an `abstract class` for the template implementation and abstract methods for variable steps. Mark the template method `final` when subclasses must not replace the algorithm.
* C++: use an abstract base class with pure virtual hooks and a virtual destructor. Keep ownership explicit and avoid deep virtual hierarchies.
* C: C has no class inheritance. Use a focused interface struct with function pointers, an explicit context pointer, and shared non-virtual functions for the stable execution template.

TypeScript example:

```typescript
//YYYYMMDD_kpopmodder: Added this abstract action to centralize verified item-action execution.
abstract class MinecraftVerifiedItemActionBase {
  protected abstract readonly actionName: string

  public async run(item: string, count: number): Promise<ActionResult> {
    const blocked = this.permissionGuard.blockedResponse(this.actionName)
    if (blocked !== null) {
      return blocked
    }

    const validation = this.validator.validate(this.actionName, item, count)
    if (validation !== null) {
      return validation
    }

    return this.verifiedRunner.run({
      action: this.actionName,
      item,
      count,
      submit: () => this.submit(item, count),
    })
  }

  protected abstract submit(item: string, count: number): Promise<ActionResult>
}
```

Do not simulate inheritance with repeated wrapper classes that each copy the same public method.
Do not replace a useful abstract base class with an interface plus duplicated implementations merely to claim that the code uses composition.

### Inheritance Depth and Multiple Inheritance

Keep inheritance shallow.

Preferred structure:

```text
interface or Protocol
    -> abstract base class
        -> concrete implementation
```

Prefer one abstract implementation level and one concrete subclass level.
Do not add deeper inheritance chains without a clear written justification.

Avoid multiple inheritance for production behavior.
Multiple inheritance is allowed only for small, stateless, clearly named mixins whose responsibilities do not overlap and whose method-resolution order is obvious.

Do not use mixins to bypass the one-primary-responsibility rule or to assemble a hidden large class from many unrelated behaviors.

### File and Folder Placement

A reusable abstract base class is an independent project type and must follow the one-class-per-file rule.

Python examples:

```text
plugins/Minecraft/minecraft_core/actions/base/
    minecraft_verified_item_action_base.py

plugins/Minecraft/minecraft_core/actions/
    minecraft_craft_action.py
    minecraft_get_item_action.py
    minecraft_get_and_equip_action.py
```

Folder placement must follow the existing component structure.
Do not create a `base/` folder automatically when the component has only one base class and an existing responsibility-oriented folder is already clear.

Acceptable alternatives include:

```text
plugins/Minecraft/minecraft_core/actions/minecraft_verified_item_action_base.py
```

or a focused contract folder when multiple related contracts exist:

```text
plugins/Minecraft/minecraft_core/actions/contracts/
    minecraft_item_action.py
    minecraft_verified_item_action_base.py
```

Do not create repository-wide dumping grounds such as:

```text
base_classes/
abstracts/
inheritance/
common_bases/
```

### Refactoring Existing Duplicate Implementations

When converting existing sibling classes to inheritance, Codex must:

1. List the duplicate public flow shared by the classes.
2. Separate invariant steps from subclass-specific steps.
3. Explain why the subclasses satisfy an `is-a` relationship.
4. Propose the exact base class and subclass file paths.
5. Preserve existing public class names, methods, constructor compatibility, imports, registration keys, configuration keys, and response shapes.
6. Move only the shared behavior into the base class.
7. Keep specialized behavior in the concrete subclass.
8. Avoid unrelated cleanup or renaming in the same change.
9. Add or update tests for both the shared base behavior and every subclass hook.
10. Search for direct construction, mocks, patch targets, registries, and dynamic imports that reference the original classes.

If constructor compatibility cannot be preserved directly, use a small compatibility adapter or staged migration rather than breaking all call sites at once.

### Required Tests for Inheritance Refactoring

At minimum, test:

* shared validation runs for every subclass
* shared permission checks cannot be bypassed
* the correct subclass hook is called
* action names, command names, endpoints, or payload types remain correct
* shared exceptions and cleanup behavior remain unchanged
* each subclass remains usable through the base contract or interface
* existing registration and dynamic loading still resolve the concrete class
* no old duplicated public flow remains in concrete subclasses

Recommended Python checks:

```bat
python -m compileall <changed_package>
python -m pytest <smallest_relevant_test_path>
git diff --check
```

### Forbidden Inheritance Patterns

Do not:

* create an empty base class with no contract or shared behavior
* create a base class only because two class names share a suffix
* move unrelated helpers into a base class
* use inheritance to access another class's internal state
* override a template method and replace the entire shared algorithm
* duplicate the base algorithm inside each subclass
* create subclass flags that produce many hidden execution branches in the base class
* use `isinstance` chains in the base class to detect concrete subclasses
* create a deep hierarchy that makes runtime behavior difficult to trace
* replace clear domain classes with one large inheritance tree for speculative future reuse
* introduce inheritance into stable third-party, vendored, or generated code

### Required Planning Report

Before introducing or changing a production inheritance hierarchy, Codex must report:

```text
Shared contract:
- <public operation and expected behavior>

Invariant flow:
- <steps owned by the base class>

Variable hooks:
- <steps implemented by subclasses>

Proposed base class:
- <exact path>: <responsibility>

Concrete subclasses:
- <exact path>: <specialized behavior>

Why inheritance is valid:
- <is-a relationship and substitutability explanation>

Compatibility to preserve:
- constructors
- public imports
- registration keys
- config keys
- response types
- dynamic loading paths

Validation:
- <tests and commands>
```

If inheritance is rejected after evaluation, Codex must briefly state why composition, delegation, a strategy, a shared function, or a protocol-only contract is safer.

---

## 29.4 Game Extension Migration Rule

New games must be added as `GameExtension` implementations and integrated through `ExtensionRegistry`.

* Do not add future game-specific behavior as direct ad-hoc plugin wiring in startup or UI paths.
* Each new game must provide a `GameExtensionInterface` implementation (`name`, `initialize`, `start`, `stop`, `handle_command`, `get_status`).
* Register and lookup games by logical name via `ExtensionRegistry` (for example: `chess`, `starcraft116`).
* Add game runtime execution/observation/command split through `GameBridge`/`GameWorker` or equivalent adapter layers.
* Keep existing legacy game modules working during migration by using shim/wrapper compatibility paths.
* Update this rule when any new game (Minecraft, Helltaker, StarCraft2, etc.) is added.

For any new game:

1. Add extension wrapper first.
2. Register through AppComposer/registry layer.
3. Preserve old behavior as fallback until new path is verified.
4. Document migration status in extension adapter and project docs.

## 30. Error Handling Rules

Prefer safe fallback behavior.

When adding exception handling:

* Log enough information to debug.
* Do not hide critical errors silently.
* Do not crash the whole app for recoverable audio/device errors.
* Preserve user-visible behavior when possible.

For audio playback and TTS, cleanup should happen even after exceptions.

Use `finally` blocks where appropriate.

---

## 30.1 Null, Nil, Undefined, and Invalid Reference Safety Rule

Null-reference failures are not limited to Java.

Different languages use different names, but the underlying defect is usually the same: code assumes that a value, object, pointer, callback, collection element, dependency, configuration value, or external response exists when it may actually be absent, invalid, uninitialized, or already disposed.

Common manifestations include:

```text
Java                  NullPointerException
Kotlin                NullPointerException or failed `!!`
C#                    NullReferenceException
Python                AttributeError on None, TypeError from None, or UnboundLocalError
JavaScript/TypeScript TypeError when reading or calling a property of null/undefined
C/C++                 null pointer dereference, access violation, segmentation fault, or undefined behavior
Go                    panic caused by nil pointer dereference or nil interface misuse
Rust                  panic from unwrap/expect on None, or unsafe raw-pointer misuse
Swift                 fatal error from unexpectedly unwrapping nil
Objective-C           silent nil behavior or invalid pointer access
```

Codex must treat all of these as one broad defect category:

```text
Null / nil / None / undefined / invalid-reference safety
```

### Core Rule

Do not assume that a value exists merely because:

* a type annotation says it should exist
* a constructor parameter is normally supplied
* a dictionary or map key usually exists
* a configuration key has a documented default
* an external API normally returns an object
* a callback normally returns a value
* a dependency injection container normally supplies an implementation
* a parser normally succeeds
* a UI component normally exists
* a collection is normally non-empty
* a background operation normally reaches an assignment
* validation occurred in another layer
* a test mock normally behaves like the real implementation

At every trust boundary, determine:

1. Can the value be absent?
2. Can it be present with the wrong type?
3. Can creation fail before assignment?
4. Can reload, shutdown, disposal, callback, async work, or another thread invalidate it?
5. Does absence mean optional, not found, disabled, pending, failure, or invalid input?
6. Should the correct behavior be fallback, typed failure, validation error, or fail-fast exception?

Do not add a null check without deciding the correct semantic behavior.

### Mandatory Review Trigger

Perform an explicit null-safety review whenever code involves:

* optional configuration values
* JSON, YAML, TOML, environment variables, or external settings
* HTTP, WebSocket, database, subprocess, file, plugin, game bridge, or model responses
* callbacks and event handlers
* dependency injection, registries, factories, and plugin loading
* parser or extractor results
* dictionary, map, object, or payload lookups
* collection indexing or first/last element access
* UI component maps
* lazy initialization
* reload, reconnect, shutdown, disposal, or lifecycle transitions
* background threads, async tasks, futures, queues, or timers
* variables assigned inside `try`, conditional, loop, callback, or asynchronous branches
* C/C++ pointers, function pointers, handles, COM pointers, or API output pointers
* force unwraps, non-null assertions, casts, and untrusted numeric or enum conversions

Review both explicit null-like values and wrong-type values that cause the same failure at the use site.

### Validate at Boundaries

Validate nullable and untrusted values where they enter the owning component.

Preferred boundaries include:

```text
configuration loader
HTTP or bridge transport
database or repository adapter
parser-result boundary
composition root
plugin or action registry
UI component builder
callback adapter
thread or task entry point
C/C++ API wrapper
```

Preferred flow:

```text
untrusted value
    -> validate and normalize once
    -> typed domain value or explicit failure
    -> internal code
```

Avoid passing raw nullable values through several services and scattering `.get()`, casts, default values, and defensive checks across unrelated modules.

### Absence Must Have One Meaning

Do not use one null-like value to represent several unrelated states.

Avoid using `None`, `null`, `nil`, or `undefined` interchangeably to mean:

```text
not configured
not found
not initialized
operation failed
operation pending
feature disabled
empty result
permission denied
transport disconnected
invalid input
```

Use a result type, status enum, error object, or explicit state when callers must distinguish these cases.

Do not return null for an error when the caller requires an error reason.

### Configuration Null Semantics

Configuration handling must distinguish between:

```text
key absent
key present with null
key present with an invalid value
key present with a valid false, zero, or empty value
```

Do not automatically convert explicit null to false, zero, an empty string, or an empty collection unless the configuration contract defines that behavior.

Do not use truthiness fallback when false, zero, or an empty value is meaningful.

Avoid:

```python
count = int(value or 1)
```

Prefer deliberate handling:

```python
if value is None:
    count = 1
else:
    count = parse_count(value)
```

Use a default only when explicit null is documented to mean “use the default.” Otherwise return a validation error.

### Python Rules

Python equivalents commonly appear as:

```text
AttributeError: 'NoneType' object has no attribute ...
TypeError caused by None in int(), len(), iteration, arithmetic, or context-manager use
UnboundLocalError when assignment occurred only in a failed branch
KeyError or IndexError caused by unsafe assumptions
```

Required practices:

* Use `Optional[T]` or `T | None` when absence is part of the contract.
* Narrow optional values before dereferencing them.
* Do not use `typing.cast()` to hide a real nullability problem.
* Do not use `assert value is not None` for public or external runtime validation.
* Initialize variables before conditional or `try` branches when used afterward, or return immediately from failure branches.
* Validate values returned from callbacks, mocks, transports, plugins, registries, and dynamic imports.
* Validate dictionary values, not only key presence.
* Catch `TypeError` and `ValueError` narrowly around untrusted conversions when a typed failure is appropriate.
* Do not catch `Exception` merely to hide a `None` defect.

Unsafe:

```python
try:
    result = extension.handle_command(command)
except Exception as error:
    message = format_error(error)

notify(message)

if result.get("pending"):
    watch(result)
```

Safe:

```python
try:
    result = extension.handle_command(command)
except Exception as error:
    notify(format_error(error))
    return

notify(format_result(result))

if is_pending(result):
    watch(result)
```

### Java and Kotlin Rules

Java:

* Do not catch `NullPointerException` as normal control flow.
* Validate required arguments with `Objects.requireNonNull()` when immediate failure is appropriate.
* Use `Optional<T>` only when absence is a meaningful return result.
* Do not return null for collections when an empty immutable collection is the defined result.
* Validate nullable intermediate values in chained calls.
* Preserve useful context in null-related errors.

Kotlin:

* Prefer nullable types and explicit handling.
* Avoid `!!` unless the invariant is locally proven and documented.
* Validate Java platform types.
* Use `requireNotNull`, `checkNotNull`, safe calls, Elvis handling, or typed results according to the contract.
* Do not use `?: default` when null should be invalid.

### C# Rules

* Enable and respect nullable reference types when supported.
* Do not use the null-forgiving operator `!` merely to silence diagnostics.
* Validate required constructor arguments and dependency injection results.
* Use `ArgumentNullException.ThrowIfNull()` or the project equivalent for required public inputs.
* Use `?.` and `??` only when the fallback is semantically correct.
* Distinguish absence from operation failure.

### JavaScript and TypeScript Rules

Typical failures include:

```text
TypeError: Cannot read properties of null
TypeError: Cannot read properties of undefined
TypeError: value is not a function
```

Required practices:

* Enable and respect `strictNullChecks` when supported.
* Do not use non-null assertion `!` merely to silence the compiler.
* Do not use optional chaining to hide a missing required dependency.
* Validate JSON, DOM, IPC, HTTP, plugin, environment-variable, and callback boundaries.
* Use discriminated unions for success, failure, pending, disabled, and not-found states.
* Validate both property existence and property type.
* Check `Map.get()` results before dereferencing.
* Do not use `value || default` when `0`, `false`, or `""` is valid.
* Use `value ?? default` only when nullish fallback is the documented behavior.

### C and C++ Rules

A null pointer dereference may cause an access violation, segmentation fault, memory corruption, or undefined behavior.

Required practices:

* Validate nullable raw pointers before dereferencing them.
* Initialize pointers and handles to a defined invalid state.
* Check API return codes before using output pointers or handles.
* Validate function pointers before calling them.
* Use RAII and explicit ownership in C++.
* Prefer references or an established non-null contract for required dependencies.
* Use `std::optional<T>` for absent values that do not require pointer identity.
* Preserve the distinction between null pointers, invalid OS handles, empty buffers, and failed operations.
* Do not return pointers to expired objects.
* Inspect lifetime and ownership, not only nullness.

A null check does not fix:

```text
dangling pointer
use-after-free
uninitialized pointer
invalid handle
data race
buffer lifetime error
```

### Go Rules

* Check errors before using returned values.
* Do not use a value when `err != nil`.
* Account for interfaces that are non-nil while containing a typed nil pointer.
* Validate nil maps, slices, channels, functions, and pointers according to their language behavior.
* Do not call a nil function.
* Do not recover from a nil panic merely to continue with invalid state.

### Rust Rules

* Use `Option<T>` for meaningful absence.
* Do not use `unwrap()` or `expect()` on runtime input, configuration, external responses, plugins, or lifecycle state unless failure is deliberately fatal and documented.
* Handle `Option` and `Result` with pattern matching, `?`, or explicit error mapping.
* In `unsafe` or FFI code, validate raw pointers and document ownership and lifetime.

### Swift and Objective-C Rules

Swift:

* Avoid force unwrap `!` for runtime or external values.
* Use optional binding, `guard let`, or typed errors.
* Do not use implicitly unwrapped optionals for values that may remain absent after initialization.

Objective-C:

* Sending a message to `nil` can silently return zero-like values and hide defects.
* Explicitly validate required objects when silent nil behavior would corrupt state.
* Distinguish deliberate optional receivers from missing required dependencies.

### Dependency Injection and Registry Rules

Composition roots, registries, factories, plugin loaders, and tests must reject invalid dependencies before runtime use.

Do not permit:

```text
action name -> null
parser list item -> null
service dependency -> null
callback -> null
UI component key -> null
plugin object without its required method
```

Validate that:

```text
the object exists
required methods exist
required methods are callable
result types match the contract
missing registration returns a named error
duplicate registration is handled deliberately
```

Prefer a precise composition-time error over a later unrelated attribute or method failure.

### External Response Normalization

External systems may return:

```text
null
an empty body
a scalar instead of an object
an object missing required fields
a field present with null
a field with the wrong type
a stale or partially initialized object
```

Transport and adapter boundaries must normalize malformed responses into the project’s standard failure result.

Do not let raw malformed data pass through multiple service layers.

A method that promises a dictionary, object, or DTO must not return null without declaring that possibility.

### Collection and Mapping Rules

Before using a collection or mapping:

* distinguish missing from empty
* validate the collection type
* validate required keys
* validate values associated with those keys
* check length before indexing
* check map lookup results before dereferencing
* account for concurrent invalidation when relevant

This is not sufficient for untrusted data:

```python
count = int(payload.get("count", 0))
```

The key may exist with `None` or an invalid string.

### Lifecycle, Reload, and Concurrency Rules

Null and invalid-reference defects often occur during:

```text
startup
partial initialization
reload
reconnect
shutdown
plugin replacement
thread cancellation
async failure
UI teardown
resource disposal
```

Required practices:

* Do not publish partially initialized dependencies.
* Build and validate replacement state before swapping it into live state.
* On failed reload, retain the previous valid state.
* Do not clear a shared dependency while active users may still access it without a defined synchronized lifecycle.
* Ensure callbacks cannot observe half-updated state.
* Return from failure branches before using values that require success.
* After disposal, return an explicit unavailable result rather than dereferencing cleared state.
* Add synchronization only when an actual shared-state race is identified.

### Null Object Pattern

A Null Object is allowed only when “do nothing successfully” is valid domain behavior.

Potentially valid examples:

```text
no-op logger
disabled metrics sink
optional notification sink
```

Do not use a Null Object to hide:

```text
missing required bridge client
missing model
failed plugin initialization
invalid configuration
missing action handler
permission denial
transport failure
```

A Null Object must obey the full contract and must not report misleading success.

### Exception and Fallback Rules

Do not catch a null-reference failure and continue without repairing or rejecting the invalid state.

Avoid:

```text
catch NullPointerException and ignore
catch NullReferenceException and return success
catch AttributeError and assume every case means None
catch TypeError around unrelated code
recover from panic without identifying the invalid state
```

When a value is required, reject it early with a precise error.

When it is optional, handle absence explicitly.

When an external system is malformed, return a typed failure and preserve the last valid internal state.

When continuing would corrupt state, fail the current operation safely rather than inventing a default.

### Required Tests

When nullable values, optional results, dependency composition, boundary validation, or lifecycle state are changed, consider tests for:

* required dependency missing
* optional dependency absent
* dictionary key absent
* dictionary key present with null
* wrong value type
* callback returns null
* callback raises before later assignment
* transport returns null
* transport returns empty or malformed data
* parser returns no match
* invalid registry entry
* empty collection
* configuration key absent
* configuration key explicitly null
* false, zero, or empty string used as valid values
* reload preparation fails before swap
* callback fails during reload
* shutdown occurs before a late callback
* background operation fails before result assignment
* previous valid state remains usable after failure

Do not weaken tests to accept a silent default unless that default is part of the documented contract.

### Static Analysis and Compiler Support

Use existing project tools when available:

```text
Python       mypy, pyright, Ruff, pylint, pytest
TypeScript   strictNullChecks, tsc, ESLint
Java         compiler warnings, SpotBugs, Error Prone, NullAway
Kotlin       compiler null-safety
C#           nullable reference types and analyzers
C/C++        compiler warnings, clang-tidy, sanitizers, static analyzers
Go           go vet, staticcheck, tests
Rust         compiler, clippy, tests, Miri where appropriate
Swift        compiler warnings and tests
```

Do not add or upgrade analysis dependencies without following the dependency rules.

Static-analysis success does not replace runtime boundary tests.

### Required Review Report

For null-safety investigation or remediation, report:

```text
Nullable or invalid-reference sources:
- <path and value>

Possible failure:
- <language-specific exception, panic, access violation, or undefined behavior>

Current contract:
- <required, optional, pending, disabled, not found, or failure>

Chosen behavior:
- <fallback, validation error, typed result, or fail-fast exception>

Files changed:
- <exact paths>

Tests added:
- missing value
- explicit null
- wrong type
- failure before assignment
- lifecycle or reload failure when relevant

Validation:
- <commands and results>

Remaining runtime risk:
- <manual or integration validation>
```

### Forbidden Patterns

Do not:

* add broad exception handling only to hide a null defect
* use force unwraps or non-null assertions without a locally proven invariant
* replace every null with zero, false, an empty string, or an empty object
* use truthiness when false, zero, or an empty value is valid
* return success after a required dependency is missing
* publish partially initialized objects
* dereference external responses before type and field validation
* assume a mapping value is valid because its key exists
* assume a variable was assigned inside a branch that may fail
* use null checks as a substitute for C/C++ ownership and lifetime analysis
* silently convert malformed responses into normal empty results
* spread unrelated defensive checks throughout internal code instead of validating the owning boundary
* weaken types to `Any`, `object`, raw pointers, or broad nullable unions merely to silence diagnostics

The goal is not to eliminate every optional value.

The goal is to make absence explicit, validate it at the correct boundary, preserve valid state after failures, and prevent language-specific null-reference crashes or silent state corruption.

---

## 31. Security Rules

Do not commit secrets.

Do not print secrets.

Sensitive values include:

* OpenAI API keys
* Hugging Face tokens
* VTube Studio tokens
* local credentials
* private server addresses
* personal access tokens

If a secret appears in a file, warn the user.

---

## 32. User Communication Rules

When explaining changes to the user:

* Be direct.
* Explain what changed.
* Explain why it changed.
* Mention risky areas.
* Provide Windows commands when useful.
* Do not over-explain obvious Git basics unless asked.

The user prefers practical guidance and copy-paste-ready commands.

---

## 33. Korean Project Notes

The user may use Korean comments and Korean documentation.

Do not delete Korean comments simply because they are not English.

Korean comments often contain important project history and risk notes.

Preserve comments that explain why a workaround exists.

---

## 34. Known Fragile Areas

Treat these as high-risk areas:

```text
TTS queue
TTS interrupt
audio playback
VTube Studio mouth control
Whisper silence handling
speaker recognition
GPT-SoVITS API startup
CUDA DLL loading
llama-cpp-python installation
Gradio queue/event behavior
audio device selection
model path configuration
memory retrieval
MemoryRouter / MemoryNeedClassifier
derived_memory.sqlite3
long_term_memory.json
raw_events consolidation
memory context injection
```

Any change in these areas should be small and carefully explained.

---

## 35. Preferred Workflow for AI Agents

Before editing:

1. Inspect the relevant files.
2. Summarize the planned change.
3. Make the smallest safe patch.
4. Show what changed.
5. Suggest a test command.
6. Before any cleanup, deletion, move, rename, reset, or mass file operation, print the exact target list and stop for user confirmation.

After editing:

1. Check syntax if possible.
2. Check Git diff.
3. Warn about runtime tests that still need to be done.
4. Do not commit or push unless asked.
5. If cleanup candidates were found, report them only. Do not delete them automatically.

---

## 36. Final Principle

This project is already working in many areas.

Do not break working behavior for unnecessary cleanup.

When in doubt:

* preserve existing behavior
* make smaller changes
* document fragile assumptions
* ask before changing versions
* prioritize Windows runtime stability
