<!-- #20260701_kpopmodder: Tracks which BWAPI pieces are ready for SAIDA compatibility. -->
# SAIDA Compatibility Matrix

This matrix keeps the Remastered BWAPI work honest. `Stubbed` means the symbol exists but does not yet represent live Remastered state.

| Area | Current Status | Notes |
| --- | --- | --- |
| `BWAPI::AIModule` | Stubbed | Callback names exist for source-level compile probes. |
| `BWAPI::Broodwar` | Stubbed | Backed by `LAVBWAPIRM::Bridge`; no native game attachment. |
| `Game::isConnected` | Stubbed | Reads bridge snapshot. |
| `Game::isInGame` | Stubbed | Reads bridge snapshot. |
| `Game::getFrameCount` | Stubbed | Reads bridge snapshot. |
| `Game::self` / `enemy` | Stubbed | Uses snapshot players. |
| `Game::neutral` / `getPlayers` | Stubbed | Provides safe player wrappers for source-level compile/runtime probes. |
| `Game::getAllUnits` | Stubbed | Rebuilds unit wrappers from snapshot units. |
| `Game::getMinerals` / `getNeutralUnits` | Stubbed | Filters snapshot neutral/resource units. |
| `Game::getUnitsInRadius` | Stubbed | Local filter over snapshot units. |
| `Game::canBuildHere` | Stubbed | Validates tile position and building type only. |
| Draw/debug methods | Stubbed | No-op source compatibility helpers. |
| `Player::getUnits` | Stubbed | Uses per-frame snapshot ownership. |
| `Player::allUnitCount` / `completedUnitCount` / `visibleUnitCount` | Stubbed | Counts snapshot units. |
| `Unit::getPlayer` | Stubbed | Returns snapshot owner wrapper. |
| `Unit::exists` / `getDistance` / `getTilePosition` | Stubbed | Local calculations over snapshot data. |
| Python snapshot writer | Scaffolded | ScreenVision observation is parsed into `logs/starcraft_bwapi_rm_snapshot.json`. |
| Samase read-only state file | Scaffolded | `SamaseProvider` can read `samase_state_path` JSON and expose it as `StarCraftGameState`; no input control. |
| Samase read-only state writer | Scaffolded | `SamaseReadonlyStateWriter` writes the safe JSON contract and can mirror it to the BWAPI-RM snapshot for local probes. |
| Samase state relay loop | Scaffolded | `provider=samase` can poll `samase_state_path` and refresh `bwapi_snapshot_path`; no game attachment. |
| Native Samase plugin heartbeat | Scaffolded | Rust `cdylib` exports `Initialize`, `samase_plugin_init`, and a manual test export; `Initialize` is now the confirmed `SAMASE_MORE_DLLS` InitializeBridge v3 entry and writes a synthetic heartbeat thread until the real PluginApi path is active. |
| InitializeBridge v3 diagnostics | Scaffolded | Adds `bridge.schema=lav_initialize_bridge_v3`, v2/v1 compatibility schema names, StarCraft/client SDK/Samase temp DLL fingerprints, PE section maps, verified section-bounded scan ranges, and deferred in-game/resource/unit pointer candidates without reading or dereferencing game memory. |
| In-game detector v1 | Scaffolded | Adds `bridge.in_game_detector` using PluginApi when available, otherwise a tiny `VirtualQuery`-guarded allowlist of fixed legacy primitive reads; no unit/resource pointer dereference. |
| SCR offset discovery v2 | Scaffolded | Adds `bridge.offset_discovery` bounded read-only scans over verified `.data` windows for candidate `small_u32_nonzero`, `bool_u8_true`, readable-pointer-like offsets, 4KB window-profile hashes, and focused 256-byte chunk profiles; zero-valued primitive noise is suppressed and pointers are not followed. |
| Python command queue reader | Scaffolded | Can parse command JSONL, but live game execution remains disabled. |
| `GameStateProvider` | Implemented | Interface boundary for mock state and future SCR/Samase state. |
| `MockGameStateProvider` | Implemented | Supplies deterministic Terran opening state, minerals/gas/supply, and units. |
| `MockBridge` | Implemented | Logs commands and never controls StarCraft. |
| `CompatRunner` | Implemented | Calls `onStart()` and repeated `onFrame()` callbacks. |
| `mock_runtime_probe` | Implemented | Exercises mock reads plus `train`, `gather`, `build`, `move`, and `attack` logs. |
| `scr_readonly_runtime` | Implemented | Reads `lav_bwapi_rm_snapshot_v1` JSON through `FileBridge` and prints a state summary; commands disabled. |
| `BWAPI/Client.h` | Stubbed | Exists so SAIDA headers that include `BWAPI/Client.h` can compile without BWAPIClient runtime use. |
| `saida_mock_runtime` | Scaffolded | Optional CMake target that links local SAIDA source to `lav_bwapi_rm`; native compile errors drive the next API additions. |
| `Unit::train` | Stubbed | Sends bridge command; `NullBridge` returns false. |
| `Unit::build` | Stubbed | Sends bridge command; `NullBridge` returns false. |
| `Unit::move` | Stubbed | Sends bridge command; `NullBridge` returns false. |
| `Unit::attack` | Stubbed | Sends bridge command; `NullBridge` returns false. |
| `Unit::rightClick` | Stubbed | Sends bridge command; `NullBridge` returns false. |
| `Unit::gather` / `repair` | Stubbed | Sends bridge command; `NullBridge` returns false. |
| `LAVBWAPIRM::FileBridge` | Scaffolded | Reads snapshot JSON and can append command JSONL only when a queue path is configured; no game attachment. |
| `TechType` / `UpgradeType` | Placeholder | Names only. |
| BWAPI binary ABI | Not implemented | Required only if running unmodified native binaries. |
| Remastered memory hooks | Out of scope | Do not add under this plugin policy. |
| DLL injection | Out of scope | Do not add under this plugin policy. |
| Battle.net / multiplayer automation | Out of scope | Must remain blocked. |

## First Real SAIDA Pass

The next useful implementation step is to place SAIDA source or headers in a local, uncommitted analysis folder and extract the actual compile errors/API calls. Then fill only the missing source-level API surface needed by SAIDA.
