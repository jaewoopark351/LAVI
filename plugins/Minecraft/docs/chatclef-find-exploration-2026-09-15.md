<!-- 20260915_kpopmodder: Preserve this historical record and point to the superseding name-boundary contract. -->
> 이름 해석 및 네이티브 명령 문법의 현재 기준은
> [Python JSON 이름 해석 이관 기록](chatclef-find-python-names-2026-09-15.md)이다.
> 아래 기록 당시의 Java 한국어 이름 해석/명령 예시는 이관 전 내용이며,
> 탐색·자동방어·접근 수명주기는 이번 이름 처리 변경으로 다시 설계하지 않았다.

# FIND 탐색 이동 및 자동방어 수정 기록

## 적용 기준과 상태

기준은 `LAVI-minecraft-plugin-fix-alto-clef-infinite-loop (66).zip`에 `LAVI_find_implementation_20260914.zip`과 `LAVI_find_exploration_defense_20260915.zip`의 `changed_files`를 순서대로 적용한 소스다. 후속 지속 탐색 패치는 `LAVI_find_until_found_20260915.patch`다. 이 문서는 2026-09-14 전달본의 단발 검색 설명과 직전 전달본의 90초 전체 탐색 기한을 대체한다. 실제 Windows 작업 트리, GitHub, 게임 모드 폴더는 수정하지 않았다.

지속 탐색 수정과 격리 회귀 검사는 완료했다. 이 후속 패치의 정식 Gradle 빌드, JAR 배포 및 실게임 검증은 실행하지 않았다. 직전 전달본의 Gradle DNS 오류는 이전 실행의 기록이며 이번 빌드 결과로 재사용하지 않는다. 새 배포 JAR는 포함하지 않는다. Java 검사는 실제 Task 소스와 게임/API 대역을 사용하며 실제 장시간 청크 수신·이동·전투의 증거를 대체하지 않는다.

## 원인과 변경 동작

이전 FindTask는 초기에 로드된 엔티티 목록이나 블록 범위를 한 번 조회하고 후보가 없으면 NOT_FOUND로 끝났다. 주민 실패 로그는 한국어 이름이 minecraft:villager로 해석된 후 이동 없이 종료된 사례였다. 이번 수정은 기본 approach 모드에 기존 ChatClef의 가장 가까운 대상 선택과 이동 탐색 반복을 연결한다.

- 엔티티, 플레이어, 드롭 아이템: FindEntitySearchTask가 기존 DoToClosestEntityTask를 상속한다. 실제 EntityTracker가 갱신한 후보에 런타임 레지스트리 ID/드롭의 ITEM ID/플레이어 이름 조건을 적용한다.
- 블록: FindBlockSearchTask가 기존 DoToClosestBlockTask와 BlockScanner를 사용한다. 캐시 후보도 실제 로드된 월드 상태로 다시 검증한다. 네이티브 스캐너가 의도적으로 생략하는 air 상태 타입은 런타임 기본 블록 상태의 isAir()로 구분하고 작은 전용 FindUnindexedAirProbe에서 틱 분할 관측한다. 이름 화이트리스트를 추가하지 않는다.
- 공통: AbstractDoToClosestObjectTask가 대상 선택, 무효화, 재관측 및 wander 전환을 소유한다. 후보가 없으면 FIND 전용 이동 leaf가 기존 Baritone ExploreProcess.explore(x, z)를 실행한다. 별도 길찾기 알고리즘이나 마을 위치 추측, 서버 /locate, 월드 시드 접근은 추가하지 않는다.
- TimeoutWanderTask 전체를 호출하지 않는다. 그 클래스에는 막혔을 때 주변 몹을 죽이는 복구와 강제 클릭, 인벤토리 커서 처리, 전역 취소가 포함돼 있다. 탐색 primitive만 재사용하여 FIND 자체의 명시적 공격/처치/드롭 획득을 추가하지 않는다.
- 발견하면 기존 GoalFollowEntity 또는 안전한 인접 위치의 GoalBlock으로 접근한다. 움직이는 엔티티는 현재 위치로 재계획한다. 살아 있는 동일 대상과 현재 거리가 실제로 충족될 때만 ARRIVED를 낸다.
- 후보 부재는 approach에서 즉시 실패하지 않는다. report 모드는 이전처럼 이동하지 않는 조회이며, 조회가 끝나고 없을 때만 NOT_FOUND가 가능하다.

## 자동방어 계약

기존 MobDefenseChain, KillAura, TaskRunner, 공격/회피/무기 선택 및 생존 설정은 수정하지 않는다. FIND를 자동방어의 제외 대상으로 등록하지 않으며 방어 자체를 끄지 않는다. 기존 설정에서 활성화된 자동방어가 네이티브 체인 우선순위로 FIND를 선점한다. 글로벌 방어 설정이 이미 꺼져 있다면 이를 강제로 켜는 패치는 아니다.

방어 우선순위는 TaskRunner가 평가한 결과만 사용한다. FIND는 getPriority()를 추가 호출하지 않는다. 포스필드 공격/방패가 우선순위 평가 과정에서 입력을 사용하는 경우에도 기존 isToolInputClaimed()를 읽고 이동 child를 양보한다.

방어가 찾던 좀비를 죽이는 것은 허용된다. 방어 해제 후 현재 월드, 플레이어, 대상 ID/UUID/생존/로드 상태를 확인한다. 사라진 대상은 성공도 즉시 최종 실패도 아니며 같은 요청의 탐색으로 돌아간다. 탐색에는 전체 경과시간에 의한 종료가 없으며, 소실된 대상의 접근 예산만 초기화된다. 새 대상을 관측·접근해야 성공할 수 있다. 이 정책은 어떤 장비/상황에서도 캐릭터가 죽지 않는다는 보장이 아니다.

## 이동과 정리의 소유권

부모 FIND는 전역 입력을 해제하거나 경로를 강제 취소하지 않는다. 실제로 실행 중인 FindMovementTask leaf가 자신이 획득한 process와 제한된 BotBehaviour frame만 정리한다. 네이티브 Task의 child 교체/중단 순서를 사용하므로 방어 Task 시작 전에 FIND leaf가 정리된다.

CustomGoalProcess는 자신이 설치한 Goal 객체가 아직 유지되는 경우에만 onLostControl()을 호출한다. 다른 Goal이 활성 상태이면 기다린다. ExploreProcess는 자기 활성화 여부를 기록하고 native 단일 Task 소유권 전환 시 해제한다. 외부 모드가 같은 ExploreProcess를 프로토콜 밖에서 동시에 덮어쓰는 경우까지 중재하는 전역 잠금은 추가하지 않았다.

이전 전달본의 전체 채굴/설치 금지 frame은 제거했다. 탐색과 이동은 기존 Baritone 이동 권한과 안전 정책을 따른다. 선택한 FIND 대상 블록 자체의 파괴/덮어쓰기만 막고, 드롭 아이템에는 근접 줍기를 줄이는 좁은 이동 제한만 둔다. 해당 제한은 FIND leaf가 양보할 때 해제된다. 따라서 기존 설정이 허용하면 경로상 다른 블록의 채굴/발판 설치가 일어날 수 있다. 자동 획득 Task를 추가하지 않아도 기본 게임의 아이템 줍기가 일어날 수 있다.

## 범위와 기한

레지스트리 이름 지원은 월드 전체에서 반드시 찾아낸다는 보장이 아니다. 엔티티 관측 반경 64블록, 블록 관측 각 축 32블록 조건은 접근 모드에서 현재 플레이어 위치를 기준으로 평가한다. 최초 좌표의 고정 범위에 탐색을 묶지 않는다. report는 시작 위치의 범위를 유지한다.

기본 approach의 미발견 탐색은 90초 전체 경과시간만으로 종료하지 않는다. 동일 요청과 operationId를 유지하면서 기존 탐색 이동과 재관측을 계속한다. 새 명령을 반복 전송하거나 매번 탐색을 다시 시작하는 외부 재시도는 추가하지 않는다. 주민 전용 예외가 아니라 엔티티·블록·드롭 아이템·플레이어 FIND에 같은 정책을 적용한다.

대상을 실제로 선택한 뒤의 접근에는 별도의 90초 활성 접근 예산을 유지한다. 해당 대상이 선택된 이후 FIND가 실행 가능한 틱 사이의 시간을 누적하며, 앞선 미발견 탐색 시간은 포함하지 않는다. 네이티브 체인 선점은 onStop에서 시계를 정지하고, 포스필드/방패 입력 점유는 기존 isToolInputClaimed 분기에서 정지한다. 같은 대상에 재개하면 이미 사용한 접근 시간은 보존하되 방어 대기 간격은 더하지 않는다. 대상이 소실되거나 다른 대상으로 선택되면 그 대상의 접근 예산을 초기화한다. 실제 도착을 확인하면 ARRIVED, 활성 접근 예산을 소진하면 APPROACH_TIMEOUT이다. 이것은 ‘무진척일 때만 증가하는 시간’이 아니며 새 정체 감지 알고리즘은 추가하지 않았다.

report는 이동하지 않는 한 번의 조회이며 기존 90초 보고용 기한과 관측량 제한을 유지한다. 전체 탐색 기한 제거가 모든 종료 조건의 제거를 뜻하지 않는다. STOP/작업 취소, 월드·플레이어 교체, 플레이어 불가, 명시적 실행 오류, 관측당 엔티티·블록 검사 상한, NO_APPROACH는 기존 처리대로 남는다. 대상이 존재하지 않는 월드에서는 탐색이 계속될 수 있고 발견이나 생존을 보장하지 않는다.

scanned는 마지막 관측 쿼리/슬라이스의 검사 수이며 누적 방문 청크 수나 주민 수가 아니다. scan_complete는 해당 관측의 완료 상태이며 전체 월드 탐색 완료가 아니다. 결과 DTO 스키마는 유지한다.

## 한국어 결과 전달

CommandFeedbackResultCoordinator의 FAILED 처리 대상에 find를 추가했다. 엄격한 FIND 결과 검증기를 통과한 실패만 상세 사유를 화면과 TTS에 전달한다. 식별자가 다른 요청, 누락/위조된 필드 및 실패에 섞인 성공 관측은 구체적 이유로 투영하지 않는다. 취소 우선, 연결/요청 상관관계 검증, 중복 최종 결과 방지 및 Chat/최종 음성 입력 경계는 유지한다.

## 진단 로그

기존 ChatClefDiagnostics facade와 emitter를 사용한다. 모든 신규 FIND 로그는 findOperationId, phase, elapsedMs를 포함한다. scan_complete는 observationPass, scanned, scanComplete, candidatePresent, scanCenter를 추가한다. 주요 reason은 no_candidate_explore, explore_started, selected, approach_goal, defense_yield, defense_resume, yield_or_stop, resume, target_lost_research, movement_released 및 terminal이다. 후속 패치에서 start에 searchLifetime, reportTimeoutMs, approachActiveTimeoutMs를 추가한다. selected/양보/종료에는 approachActiveMs를 기록한다. 보고용 기한과 활성 접근 예산의 종료 이유는 각각 report_time_limit와 approach_time_limit로 구분한다. limitMs와 approachActiveMs는 판단 지점의 값을 사용하고 진단을 위해 판단 함수를 다시 실행하지 않는다.

상세 이벤트 이름은 FIND다. 종료는 FIND_TERMINAL로 기존 NON_STORE_TERMINAL family를 사용하고, 오류는 FIND_EXCEPTION으로 기존 exception family를 사용한다. 공유 admission 정책이나 한도는 바꾸지 않았다. phase/reason별 로컬 반복은 8회로 제한하고 종료에 suppressedDiagnostics를 남긴다. 일반 상세 로그 한도 소진 후 종료용 여유 슬롯이 사용되는 것을 검사했다. OFF 또는 공유 critical 한도까지 소진된 상황에서 출력이 보장되지는 않는다.

실제 emitter, UTF-8 field encoder, mode controller, shared admission authority, stdout 출력/캡처를 사용하는 테스트를 추가했다. 게임 의존 facade는 테스트 어댑터다. Minecraft의 실제 facade 초기화, 로그 파일 보존 및 디스크 flush까지 검증한 것은 아니다. 로깅 비활성/예외로 탐색 결과나 경로 정리가 달라지지 않는지 별도 검사했다.

## 검증 결과

합동 회귀 명령:

```bash
python -m tests.minecraft_chatclef.find.run_offline_tests -q \
  tests/minecraft_chatclef/find \
  tests/minecraft_chatclef/lavi_input \
  tests/minecraft_chatclef/command_lifecycle \
  tests/test_minecraft_chatclef_korean_rule_parser.py \
  tests/test_minecraft_chatclef_command_compiler.py \
  tests/test_minecraft_chatclef_natural_language_service.py \
  tests/test_minecraft_chatclef_korean_intent_schema.py \
  --disable-warnings --maxfail=8
```

결과: 614 passed, 11127 subtests passed. FIND 전용 183개 테스트가 이 합계에 포함된다. Java 하네스는 합동 검사의 두 pytest 항목 안에서 실행되며, 순수 계약 35개, 런타임 경계 58개, 실제 Task 엔진 110개, 실제 로그 출력 경로 19개의 assertion을 수행한다. 이 assertion들을 별도 pytest 테스트처럼 더하지 않는다. javac 21.0.11로 --release 17 컴파일했다.

실제 소스를 실행한 네이티브 구성 요소: Task, TaskRunner, TaskChain, ITaskCanForce, SingleTaskChain, AbstractDoToClosestObjectTask, DoToClosestEntityTask, DoToClosestBlockTask, GoalFollowEntity. 전투 요구·체력 변화, EntityTracker/BlockScanner의 월드 공급, Baritone 경로 실행과 네트워크 청크 도착은 명시적 대역이다. 방어 우선순위 65/80 요구를 모사해 실제 스케줄러가 FIND보다 먼저 실행하는 것을 검사했다. 실제 MobDefenseChain의 전투 판단을 게임에서 검증한 것은 아니다.

주요 회귀: 최초 미발견 시 탐색 시작, 초기 반경 밖에서 나중에 관측한 대상, 움직이는 대상, 주민/블록/드롭/air 상태, 동일 요청의 반복 방어 선점, 방어 처치 후 재탐색, aura/shield 입력 양보, 타 소유자의 Goal 보존, STOP 후 재생 금지, 월드/플레이어 교체, 90초 경과 후 새 주민 관측·접근, 장시간 방어 후 예산 보존, report 기한 및 관측 상한 보존, 부분 실행 실패 시 frame/process 해제, 한국어 UI/TTS 상세 실패 이유 및 중복 방지.

## 실게임 확인 항목

새 JAR와 Python 소스 적용 후 LAVI 및 Minecraft를 재시작한다. 주민을 초기에 관측할 수 없는 위치에서 `마을 주민 찾아줘`를 실행했을 때 탐색 이동이 시작되는지 확인한다. 90초를 넘어도 탐색이 유지되고, 그 이후 새로 로드된 주민을 발견한 뒤 실제로 접근해 ARRIVED가 나오는지 확인한다. 좀비를 찾는 동안 기존 자동방어가 전투/회피하며, 찾던 좀비를 죽여도 FIND가 재탐색하는지 확인한다. STOP 실행 후 이동이나 성공 응답이 뒤늦게 재생되지 않아야 한다. report는 계속 움직이지 않아야 한다.

확인 로그는 Minecraft latest.log와 LAVI 로그 두 개다. 새 구현의 탐색 진행은 동일 findOperationId의 no_candidate_explore → explore_started → selected → approach_goal → FIND_TERMINAL로 확인한다. 선점 시 yield_or_stop/resume 또는 defense_yield/defense_resume, 대상 사망 시 target_lost_research를 확인한다. 이동 경로의 실제 성공은 로그 존재만이 아니라 게임 관측과 함께 판단한다.

## 2026-09-15 후속 로그 근거

사용자 제공 `af1f16a9-09b3-468c-9408-adb28fd7bebd.txt`에서 FIND는 02:00:08에 시작해 FindEntitySearchTask → FindExploreTask로 연결됐다. 경로 실행기의 진행 번호도 증가했다. 02:01:38의 FIND_TERMINAL은 elapsedMs=90025, code=SEARCH_LIMIT, observationPass=1751, scanned=72, observed=false, interruptions=0이었다. 이는 전체 시간 제한 종료의 근거이며 주민의 월드 전체 부재나 실제 방어 전투 검증의 근거가 아니다. Python 로그 `77c5fcff-8b90-4f46-a27f-b6c8d592fc59.txt`에는 같은 요청의 약 90.122초 후 실패와 구체적인 한국어 결과 전달이 기록됐다.

소스 변경 소유자는 FindTask의 실행 수명 및 대상 접근 예산이다. 별도 manager, config, 전역 상태, 공통 엔진 수정은 필요하지 않았다. 공격·자동방어·이동 알고리즘·한국어 입력·프로토콜 구현은 그대로 유지했다.
