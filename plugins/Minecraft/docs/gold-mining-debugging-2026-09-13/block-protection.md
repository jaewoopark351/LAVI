<!-- 20260913_kpopmodder: Separate consistent block collection reads from world-bound immutable protection publication. -->
# 공유 블록 목록과 보호 범위의 안전한 전달

상태: `IMPLEMENTED_PENDING_RUNTIME`. 후속 구현·자동 검증은 [구현 기록](implementation.md)을 따른다. 수정 전 [원인·관측 한계](evidence.md)의 정확한 null 생성 write는 여전히 미확정이며, 후보의 동시성 검증을 원본 원인의 재현으로 바꾸지 않는다.

## 현재 소스에서 검토할 경계

| 파일·메서드 | 소스에서 확인한 책임·위험 |
| --- | --- |
| [BlockScanner](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/commands/BlockScanner.java) `addBlock`, `reset`, `getKnownLocationsIncludeUnreachable`, `scanCloseBlocks` | 공유 map·내부 set 변경과 LinkedList 복사; cached clear/addAll 사이의 중간 상태 |
| 같은 파일 `tick`, `rescan`, `scanChunk`, `getFirstFewPositions` | 세계 변경 확인 전 근거리 스캔·조기 반환, worker의 scannedBlocks/scannedChunks 변경, forceStop 뒤 worker reset |
| [UserBlockRangeTracker](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/trackers/UserBlockRangeTracker.java) `isNearUserTrackedBlock`, `updateState`, `reset` | ensureUpdated와 보호 집합 clear/add는 contains의 잠금 밖; worker에서 live world 조회 가능 |
| [Tracker](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/trackers/Tracker.java), [TrackerManager](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/trackers/TrackerManager.java) | 일반 boolean dirty와 지연 갱신, tick의 dirty 설정 |
| [AltoClef](../../runtime/chatclef_fabric_1.20.1/src/main/java/adris/altoclef/AltoClef.java) `onClientTick` | scanner·보호 결과 갱신과 Task 실행의 명시적인 순서가 필요한 연결 지점 |

위 소스상 위험을 모두 이번 실행에서 입증한 race라고 부르지 않는다. 외부 map만 concurrent로 바꾸거나 dirty만 volatile로 바꾸는 것으로 이 전체 경계가 해결된다고 판단하지 않는다.

이 tracker의 기존 정책은 **침대 표시 주변 각 축 ±16 범위에서 COBBLESTONE과 LOG 대상 블록을 보호**하는 것이다. 현재 `WorldHelper.scanRegion`은 양 끝을 포함해 표시 하나당 최대 `33³ = 35,937` 좌표를 순회한다. 영역 안의 모든 블록을 영구 보호하거나 상자를 새 표시로 추가하는 정책 변경은 포함하지 않는다. 다른 엔진 보호 규칙은 별도로 유지한다.

## 1. 일관된 데이터 수집과 복사

map 조회·내부 set 복사·모든 관련 writer에 같은 일관성 규칙을 적용한다. 복사 후 불변으로 감싸는 것과 복사 과정 자체가 안전한 것은 별개다. reader만 잠그고 writer를 그대로 두면 충분하지 않다.

스캔 작업 시작 시 world/player/차원 수명과 generation을 결합한다. 종료 결과는 현재 세대와 일치할 때만 반영한다. 옛 worker가 새 세대의 map·reset 상태를 덮어쓰지 못하도록 작업 소유권을 확인한다. 현재 스캔이 참조하는 live world 접근도 실제 스레드 경계에서 검토한다. 과거 좌표에 새 world 이름만 붙여 공개하지 않는다.

월드 변경에 따른 무효화는 근거리 스캔과 주기/`scanning` 조기 반환보다 먼저 적용할 경계를 정한다. 결과의 세대 확인과 공개는 하나의 일관성 경계로 묶어 확인 직후 월드가 바뀌는 틈을 막는다. 이전 worker의 종료·reset·`scanning/forceStop` 정리도 해당 run의 소유권을 확인한 뒤 수행하며, 새 작업 상태를 과거 worker가 해제하지 않게 한다.

잠금 또는 결과 교체는 일관된 자료 수집·공개에 필요한 짧은 경계로 제한한다. 범위 스캔·월드 조회 전체를 큰 공유 잠금 안에 넣지 않는다. 구체적인 잠금/불변 교체 선택과 전체 writer 목록은 후속 구현에서 소스에 맞춰 확정하며, 진단 관찰용 generation을 내용 revision으로 재사용하지 않는다.

## 2. 보호 결과의 계산과 공개

보호 범위는 클라이언트 스레드에서 검증 가능한 월드 상태로 완성한다. 경로 계산 worker는 완성된 불변 결과를 조회하며 ensureUpdated·범위 스캔·live world 조회를 시작하지 않는다.

| 공개 결과에 필요한 의미 | 목적 |
| --- | --- |
| world/접속 수명, 차원 | 재접속·동일 이름 월드 교체·차원 이동 구분 |
| 원천 스캔 revision과 보호 데이터 revision | 서로 다른 시점의 목록·보호 조건을 혼합하지 않음 |
| 준비 상태 | 확인된 빈 집합과 미준비·실패·stale 결과 구분 |
| 완성된 불변 보호 좌표 집합 | clear→add 중간 상태·사후 변경 노출 방지 |

수명·revision·준비 상태·좌표는 하나의 snapshot으로 안전하게 공개하고 reader는 그 참조를 한 번 읽어 사용한다. `volatile`/atomic 참조 또는 일관된 잠금처럼 스레드 사이의 가시성을 보장하는 공개 수단이 필요하다. 불변 set과 별도의 mutable 상태 필드를 따로 읽어 서로 다른 세대의 값을 조합하지 않는다. 이는 map·내부 set을 안전하게 복사하는 계약을 대체하지 않는다.

계산 중인 부분 집합은 완성된 결과로 공개하지 않는다. 같은 월드의 이전 snapshot이라도 새로운 보호 대상이 추가됐다면 그것만으로 충분하지 않을 수 있다. 미준비·실패·월드 불일치를 ‘보호 없음’으로 바꾸지 않고, 확인되지 않은 블록의 파괴 판단에 한정한 보수적 처리를 정의한다. 전체 입력·MobDefenseChain 공격을 막는 전역 정지는 도입하지 않는다.

원래 범위 계산은 큰 영역을 조회한다. 매 tick 클라이언트에 그대로 옮기기 전에 비용을 측정한다. 분할 계산이 필요하면 작업량·완료 시점·원천 revision 변화를 관리하고 전체 완료 후 한 번 공개한다. 갱신 중 revision 변경과 연속 무효화 시의 처리·유한한 작업 예산을 구현 설계에서 확정해, 미준비 상태에 영구적으로 머물지 않게 검증한다.

현재 `TrackerManager.tick()`의 매 tick `setDirty()`는 내용 변경 증거가 아니다. 이를 매번 새 내용 revision으로 바꿔 분할 계산을 처음부터 재시작하면 완료할 수 없다. 재평가 요청과 실제 관련 데이터 변경·월드 무효화를 구분하고, 표시뿐 아니라 계산 대상 범위의 관련 블록 변화도 최종 유효성에 반영한다. 완성 여부는 기존 스캔 범위에서 확보한 자료를 기준으로 정의하며 월드 전체 탐색 기능으로 확대하지 않는다.

## 책임 분리와 동작 영향

새 LAVI 구현은 scanner 결과 수집/검증, world-bound 보호 결과 값, 클라이언트 계산, 공개·수명 무효화를 focused type과 의미 있는 패키지로 분리한다. upstream에는 필요한 최소 위임과 실제 호출 경계만 둔다. 새로운 일반-purpose Minecraft 공유 backend나 별도 네트워크·logger를 만들지 않는다.

null을 필터로 지워 빈 성공 목록을 반환하거나 NPE를 잡고 보호 없음으로 계속 진행하지 않는다. 동일 월드/완성된 빈 집합의 정상 결과는 그대로 지원한다. 기존 보호 대상과 보호 범위 정책, 일반 길찾기 알고리즘은 별도 변경하지 않는다.

## 원인 가설 검증과 회귀 테스트

`HashSet.toArray()`가 복사 배열을 준비한 뒤 set이 축소되면 비어 있는 배열 요소가 결과에 들어올 수 있다는 검수 의견은 구체적인 가설이다. 실제 게임 JVM의 구현과 원래 공유 writer/copy 경계를 대조한다. sleep만 반복하는 확률적 테스트나 임의로 null을 반환하는 가짜 컬렉션만으로 원본 원인을 재현했다고 보고하지 않는다.

| 자동 테스트 | 기대 결과 |
| --- | --- |
| 실제 writer와 복사 순서를 barrier/latch로 통제 | 원래 구현의 실패 재현과 후보의 일관된 결과를 구분해서 기록 |
| add/clear/addAll/reset/전경·배경 스캔 | map과 내부 set 전체의 불완전 복사·세대 혼입 없음 |
| 보호 결과 계산 중 reader | 이전 유효 결과 또는 명시적 미준비 정책, 부분 집합 노출 없음 |
| snapshot 공개 직전 월드 변경 / metadata·집합 동시 읽기 | 이전 세대 공개·서로 다른 snapshot 필드 혼합 없음 |
| 새 보호 표시 추가·제거 / source revision 변경 | 낡은 결과로 새 보호 대상을 허용하지 않음 |
| 재접속·차원·월드 교체와 늦은 worker 종료 | 다른 수명의 결과 폐기, 새 상태 reset/덮어쓰기 없음 |
| 준비 실패 / 확인된 빈 결과 | 서로 다른 상태 유지, 실패를 보호 없음으로 처리하지 않음 |
| 계산 비용·분할 처리 | client tick 지연 측정, 부분 공개·영구 미준비 없음 |
| 매 tick dirty이나 실제 관련 데이터 변화 없음 | 분할 계산이 반복 초기화되지 않고 완료됨 |
| 침대 주변 ±16 경계·COBBLESTONE/LOG·일반 블록 | 기존 범위·대상 유지, 범위 내 모든 블록으로 보호 정책 확대 없음 |
| 자동방어·STOP | 블록 파괴 판단의 보수적 처리가 공격이나 전체 입력을 막지 않음 |

완료 기준은 NPE 부재와 기존 보호 동작 유지의 동시 충족이다. 실제 보호 블록, 일반 접근 블록, 월드 변경을 각각 검증한다. companion 로그는 source/공개 revision, 준비 상태, 무효화·폐기 사유를 기존 bounded 출력으로 남기되 어떤 로그도 보호 판단의 입력으로 삼지 않는다. 후속 소스·자동 테스트 결과는 [구현 기록](implementation.md), 실게임은 `NOT_RUN`이다.
