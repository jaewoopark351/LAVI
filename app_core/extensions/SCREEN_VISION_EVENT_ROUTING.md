#20260706_kpopmodder: Documented ScreenVision -> GameExtension event contract to avoid silent routing regressions.

## ScreenVision Event Routing Contract (one-direction)

### 1) Publisher

`plugins/ScreenVision/screen_vision_core/observation_memory_dispatch.py`
- `send_observation_to_llm()` builds payload via
  `ScreenObservationDispatchHelper.build_output_payload(...)`
- It publishes through `core.event_manager.event_manager.trigger(EventType.SCREEN_OBSERVATION, payload=payload)`.

### 2) Event key (fixed)

- `EventType`: `EventType.SCREEN_OBSERVATION` only.
- Dynamic string event names are not used for this path.

### 3) Payload contract (stable keys)

- Recommended primary keys (should be present on the payload):
  - `event_type`: `"screen_observation"`
  - `event_name`: `"SCREEN_OBSERVATION"`
  - `source`, `observation`, `text`
- Compatibility keys already carried by current pipeline:
  - `display_text`, `remember_history`, `metadata`
  - `payload` (nested duplicate payload)

### 4) Consumer path in StarCraft116 extension

- `app_core/extensions/starcraft116_game_extension.py`
  - subscribes: `EventType.SCREEN_OBSERVATION`
  - handler `_on_screen_observation_event` normalizes payload and enqueues command:
    - `{"action": "screen_observation", ...}`
- `app_core/extensions/starcraft116_worker.py`
  - dequeues command and passes to `StarCraft116Bridge.send_command`
  - updates worker status (`last_command`, `last_bridge_result`, `last_bridge_error`)
  - failure when bridge result is `{"ok": false, ...}` is logged and counted.
- `app_core/extensions/starcraft116_bridge.py`
  - recognizes `screen_observation` action and resolves handler method on plugin.
- `plugins/StarCraft116/starcraft116.py`
  - shim method `handle_screen_observation(...)` exists and returns success without changing legacy gameplay behavior.

### 5) Status exposure

- Extension status now includes worker status (`get_status()` in `StarCraft116Worker`).
- Useful fields:
  - `last_command`
  - `last_bridge_result`
  - `last_bridge_error`
  - `stats.failed`, `stats.processed`, `stats.last_processed_unix`

### 6) Compatibility behavior

- Existing direct ScreenVision flow is preserved.
- The event route is additive (shim), so old direct call paths remain untouched.