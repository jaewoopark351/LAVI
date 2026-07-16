# P0 review and evidence checklist

이 문서는 P0-A/B/C 각 단계와 최종 P0 review에 사용한다. 실제 현재 명령과 approved matrix에 맞춰 실행하되, 실행하지 않은 항목은 미실행으로 보고한다.

## 1. baseline와 불변 파일

```powershell
Get-Location
git rev-parse --show-toplevel
git rev-parse HEAD
git branch --show-current
git status --short
git ls-files --error-unmatch modules.json
git hash-object modules.json
git diff --check
git diff --stat
git diff -- modules.json
```

필수:

- root `modules.json` tracked
- 실제 hash가 작업 시작 baseline과 동일
- pack 파일이나 tracked audit가 의도치 않게 변경되지 않음

## 2. P0-A installer/dependency

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install_windows.ps1 -Profile Core -Accelerator CPU
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install_windows.ps1 -Profile Core -Accelerator CPU
.\venv\Scripts\python.exe -m pip check
```

검증:

- 두 실행 모두 성공
- 같은 approved committed lock 사용
- Core/CPU에서 CUDA wheel 강제 설치 없음
- partial retry가 안전
- legacy switch script가 canonical installer와 충돌하지 않음
- approved cu130 path 회귀 없음

## 3. code quality와 test collection

```powershell
.\venv\Scripts\python.exe -m ruff check .
```

Core test 명령은 CI와 동일한 명시적 path/file list를 사용한다. optional profile top-level import가 Core collection에 들어오지 않아야 한다.

```powershell
.\venv\Scripts\python.exe -m pytest <approved-core-test-paths> -m "not gpu and not integration and not network and not slow"
```

## 4. Core offline smoke

CURRENT script interface를 사용한다. `python -m lavi`는 별도 승인 없이는 요구하지 않는다.

예시 target:

```powershell
.\venv\Scripts\python.exe .\scripts\preflight.py --profile Core --accelerator CPU --offline
.\venv\Scripts\python.exe .\scripts\smoke_startup.py --profile Core --modules-config .\config\modules.core.json --accelerator CPU --offline --timeout-sec 30
```

필수 결과:

```text
network_attempts = 0
download_attempts = 0
model_load_attempts = 0
external_process_attempts = 0
disabled_optional_imports = 0
disabled_optional_constructions = 0
disabled_optional_initializations = 0
shutdown_completed = true
```

`--offline` flag 존재만으로 통과가 아니다. 실제 guard/counter 증거가 필요하다.

## 5. Production configuration smoke

현재 tracked audit의 명령과 acceptance를 그대로 유지한다. 특히 `--production-config-smoke`가 정의되어 있다면 제거하거나 Core smoke로 대체하지 않는다.

필수:

- 사용자 override 없는 상태에서 root `modules.json` 선택
- module schema/ID 검증
- enabled module과 dependency/resource availability 진단
- 외부 resource 실제 start 없음
- root `modules.json` hash 불변
- production config를 `modules.core.json`으로 바꾸지 않음

## 6. config write safety

- 사용자 override fixture의 기존 값 보존
- invalid write 시 원본 보존
- atomic replace 검증
- root `modules.json` hash 불변
- example config production fallback 금지

## 7. CI와 docs

- Windows CI의 실제 명령이 로컬 review 명령과 일치
- Ruff와 pip check가 workflow에서 실제 실행
- Core/optional test collection 분리
- README CURRENT 명령과 installer 일치
- tracked audit의 production smoke/Git evidence가 유지

## 8. 최종 diff와 보고

```powershell
git status --short
git diff --check
git diff --stat
git diff -- modules.json
git hash-object modules.json
```

최종 보고에는 다음을 포함한다.

1. 변경 파일과 이유
2. 실행한 명령과 실제 exit/result
3. 실패 또는 미실행 항목
4. Windows/GPU/외부 앱 전용 미검증 항목
5. modules hash 전후
6. rollback
7. P1로 넘길 항목
8. commit/push를 하지 않았다는 확인
