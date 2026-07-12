<!-- #20260701_kpopmodder: Added Korean README for the optional StarCraft Remastered plugin. -->
# StarCraft Remastered

이 플러그인은 사용자가 로컬에 설치한 StarCraft Remastered AI 스크립트 환경을 Samase로 실행하고, ScreenVision 기반 코칭 프롬프트와 SAIDA 전략 포팅용 BWAPI-compatible 어댑터 표면을 준비하는 선택형 플러그인입니다.

이 플러그인은 네이티브 BWAPI DLL, 메모리 인젝터, 유닛 직접 제어 봇, 강화학습 에이전트, 마우스 자동화 도구가 아닙니다. 1차 목표는 실행 보조, 상태 추적, 화면 관찰 기반의 짧은 훈수, 전략 로직 포팅을 위한 데이터 전용 호환 계약입니다.

## 하는 일

- 로컬 StarCraft Remastered, Samase, `aiscript.bin` 경로를 검증합니다.
- 설정된 mod argument로 Samase를 실행합니다. 기본 예시는 `samase.exe custom`입니다.
- LAV가 실행한 프로세스의 PID를 추적합니다.
- `main.py`에서 연결된 경우 최신 ScreenVision 관찰을 가져옵니다.
- UI 버튼을 눌렀을 때만 StarCraft 코칭 프롬프트를 기존 LAV LLM 파이프라인으로 보냅니다.
- SAIDA 같은 전략 코드를 옮길 수 있도록 `Command`, `GameState`, `Unit`, provider, event, BWAPI 스타일 wrapper 클래스를 제공합니다.
- C++ `bwapi_shim` 골격에 `BWAPI.h`, `BWAPI::Broodwar`, `AIModule`, `Unit`, `Player`, 안전한 `LAVBWAPIRM::Bridge` 계약을 포함합니다.
- SAIDA-style shim 코드가 게임 메모리를 건드리지 않고 읽을 수 있도록 `logs\starcraft_bwapi_rm_snapshot.json` snapshot을 씁니다.
- shim이 내보낼 명령을 위해 `logs\starcraft_bwapi_rm_commands.jsonl` queue를 예약합니다. Python 쪽에서는 아직 안전하게 로그/no-op 처리합니다.
- `mode`가 `single_player_only`가 아니거나 Battle.net/multiplayer 화면이 감지되면 향후 자동 조작이 중지되도록 guard를 둡니다.

## 포함하지 않는 것

- `samase.exe`를 포함하지 않습니다.
- BWMetaAI 또는 UEDAIP 파일을 포함하지 않습니다.
- `aiscript.bin`을 포함하지 않습니다.
- StarCraft 게임 파일, MPQ 파일, 제3자 바이너리를 포함하지 않습니다.
- 게임을 직접 조작하지 않습니다.
- 수정하지 않은 네이티브 SAIDA/BWAPI 바이너리 주입을 포함하지 않습니다.
- Remastered 메모리 후킹, 패킷 조작, 안티치트 우회, Battle.net 자동화를 포함하지 않습니다.
- 완전한 BWAPI binary ABI 대체품은 아직 아닙니다.
- SAIDA가 내린 명령을 Remastered에 실시간 실행하는 단계는 아직 아닙니다. command queue 처리는 보수적으로 유지합니다.

StarCraft Remastered, Samase, BWMetaAI 또는 UEDAIP는 사용자가 직접 설치한 뒤, 개인 설정 파일에서 로컬 경로만 지정해야 합니다.

## 설정 방법

1. 로컬 경로가 준비되기 전에는 `modules.json`에서 비활성 상태를 유지합니다.

```json
"StarCraftRemastered": false
```

2. 예시 config를 개인 config로 복사합니다.

```bat
copy plugins\StarCraftRemastered\config\starcraft_remastered_config.example.json plugins\StarCraftRemastered\config\starcraft_remastered_config.json
```

3. `plugins\StarCraftRemastered\config\starcraft_remastered_config.json`에서 로컬 설치 경로를 수정합니다.

   안전 기본값은 다음 상태를 유지합니다.

```json
{
  "mode": "single_player_only",
  "provider": "screen_input",
  "allow_battlenet": false,
  "allow_multiplayer": false,
  "auto_control": false
}
```

4. Launch 버튼으로 Samase를 실행할 준비가 되었을 때 개인 StarCraft config에서 `"enabled": true`로 바꿉니다.

5. 모듈을 활성화합니다.

```json
"StarCraftRemastered": true
```

6. LAV를 실행합니다.

```bat
venv\Scripts\python.exe main.py
```

## 참고

- 개인 config 파일은 gitignore 대상입니다.
- 상태 로그는 기본적으로 `logs\` 아래에 기록되며, 이 경로도 gitignore 대상입니다.
- LAV가 종료되어도 외부 StarCraft 프로세스를 자동으로 강제 종료하지 않습니다.
- 직접 게임 조작과 마우스 자동화는 향후 작업 범위로만 남겨둡니다.
- BWAPI-compatible 계층은 소스 수준 전략 포팅용이며, 수정하지 않은 네이티브 SAIDA DLL을 Remastered에 로드하는 기능은 아닙니다.
- 장기 목표 경로는 `SAIDA -> BWAPI-compatible shim -> LAV-BWAPI-RM bridge -> Samase single-player -> StarCraft Remastered`입니다.
- 현재 bridge 단계는 `ScreenVision observation -> StarCraftGameState -> BWAPI-RM snapshot JSON -> C++ FileBridge`입니다.
