# P1 review and evidence checklist

P1 각 단계는 P0 regression부터 시작한다.

## 1. P0 baseline 재검증

- approved Core/CPU installer
- `pip check`
- Ruff
- Core tests
- Core offline smoke
- production configuration smoke
- root `modules.json` hash

P0가 green이 아니면 P1 review를 통과하지 않는다.

## 2. metadata discovery

검증:

- implementation import 없이 descriptor 목록 조회
- duplicate ID deterministic error
- invalid entrypoint가 해당 plugin만 격리
- API version mismatch가 actionable state
- category/capability/config schema validation

## 3. disabled no-import

각 대표 plugin에 대해:

```text
module in sys.modules = false
constructor_count = 0
initialize_count = 0
start_count = 0
state = DISABLED
```

## 4. enabled but unavailable

- missing Python package → UNAVAILABLE
- missing model/file → UNAVAILABLE
- missing executable/service → UNAVAILABLE
- Core/application process 계속 실행
- suggested profile/command 제공
- secret 값 로그 노출 없음

## 5. constructor side effects

constructor 호출 동안 다음 attempt가 0이어야 한다.

```text
network
model load/download
subprocess
websocket
microphone/audio device
background thread
```

## 6. UI lazy construction

- 목록 조회만으로 module import/constructor 0
- 선택 시 1회 construct
- rerender로 중복 instance 없음
- state와 diagnostic은 instance 없이 표시 가능

## 7. failure isolation

- Plugin A import/construct/start failure 후 Plugin B 정상
- failure가 registry 전체를 손상하지 않음
- shutdown 예외 후 다른 plugin cleanup 계속
- timeout 적용
- error list와 log reference 보존

## 8. shutdown idempotence

- initialize 전 stop/shutdown 안전
- partial initialize 후 안전
- stop/shutdown 2회 이상 안전
- thread/process/socket leak 없음

## 9. config와 modules.json

```powershell
git ls-files --error-unmatch modules.json
git hash-object modules.json
git diff -- modules.json
```

필수:

- root hash 불변
- false module no-import
- true + missing resource → UNAVAILABLE
- 사용자 override 값 보존
- Core config로 production config 대체 없음

## 10. 최종 보고

- 대표 plugin별 적용 범위
- 아직 미적용 plugin 목록
- 실행한 tests와 실제 결과
- P0 regression 결과
- modules hash
- rollback
- 다음 batch 제안
- commit/push를 하지 않았다는 확인
