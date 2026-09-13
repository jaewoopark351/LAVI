<!-- 20260913_kpopmodder: Keep the shared-block observation and post-cap checkpoint unit separate from gameplay repair. -->
# 공유 블록 목록·로그 한도 이후 관측 보강

<!-- 20260913_kpopmodder: Link the later observed overlap and separate protection-data repair plan. -->
22:12 후속 실행에서는 null 소비와 읽기·쓰기 구간 겹침, 예약 요약 32회 이후의 중단·진단 종료 출력을 확인했다. [후속 실행 근거](gold-mining-debugging-2026-09-13/evidence.md)와 [보호 데이터 수정 설계](gold-mining-debugging-2026-09-13/block-protection.md)를 별도로 둔다. 정확한 null 생성 write는 여전히 미확정이며, 이 문서의 관측 구현이 동시성 문제를 수정했다는 뜻은 아니다. 후속 수정 설계는 `PLAN_ONLY`다.

## 범위와 비교 기준

2026-09-13 사용자의 **로그 보강만** 요청에 따른 별도 관측 단위다. 기준 HEAD는 `dfe29ef8547ee85fa736b75b208b126fc73f0b8b`이며, 이미 있던 미커밋 채굴·보관·Builder 진단과 관찰자 등록 수정은 보존한다. 작업 전 관련 파일의 원문·SHA-256은 저장소 내부 `logs/diagnostics/shared-block-checkpoint-baseline-20260913-213146.json`에 기록했다. Git HEAD와의 전체 diff를 이번 단위의 diff로 간주하지 않는다.

공유 컬렉션의 동기화·교체·안전 복사·null 제거, 재시도, 도구/단축바 정책, 채굴·보관·자동방어·Task 판단은 변경하지 않는다. 게임 예외를 잡아 성공값이나 빈 목록으로 바꾸지 않는다. 별도 worker, logger, 네트워크 또는 백엔드를 만들지 않는다.

선행 기록은 [자원 관측 구현](chatclef-resource-observation-implementation-2026-09-13.md), [도구 반복·자동보관 계획](chatclef-gold-mining-tool-loop-auto-deposit-diagnostics-plan-2026-09-13.md), [등록 한도 사건](chatclef-diagnostic-observer-capacity-crash-plan-2026-09-13.md)이다. [캐시 점검 지침](chatclef-baritone-cache-troubleshooting.md)의 디스크 캐시와 이번 메모리 컬렉션 관측은 구분한다. 캐시 사용 자체는 원인 증거가 아니며 이번에는 캐시를 변경하지 않는다.

## 보강 전 실제 실행에서 확인한 사실

실제 인스턴스는 `C:/Users/jaewo/curseforge/minecraft/Instances/LAVI_TEST_Fabric01`이다. `logs/latest.log`, `logs/stdout-logs.txt`, `logs/instance_audit.txt`는 읽을 때 모두 0 byte가 아니었다. LAVI 기록은 `C:/Vtuber_Souorce_Code/LAVI/logs/20260913_211419_log.txt`다. 파일은 실행 중 증가하므로 아래 행 번호는 당시 읽은 기록을 가리킨다.

- 실행 JAR SHA-256: `7581b214eca74de730c948a5c93d02e113cc8f730a31650e71de0a193805932a`.
- 21:16:49 KST 명령: `금 10개 캐줘` → `get gold_ingot 10`.
- 요청: `lavi-input-ko-53a73c00d978486189791e3d05bcb079`, correlation: `lavi-82ca1a157a1d47b08cdd4602e6319287`.
- 세션: `fabric-chatclef-9e923d0858bf46c29e039d85c03628eb`, generation 1.

| 실제 관측 | 의미와 한계 |
| --- | --- |
| 21:16:56, `latest.log` 1451~1462: 삽 장착 성공 → 접근 곡괭이 단축바 이탈 → 준비 재진입 → 채굴 자식 중단 | 이전 관찰자 초기화 크래시를 넘어서 실제 도구 교체가 실행됐다. 이 반복은 아래 첫 NPE보다 먼저 시작됐다. |
| 21:18:17, 6091행: 준비 재진입 117회, 채굴 자식 실행 119회, 같은 목표 `919,26,-1808`, raw gold 순변화 0, 수량 불변 1,750 tick | 해당 시점까지 반복과 무진척이 기록됐다. 이후 로그 한도 때문에 이 숫자를 최종 반복 횟수로 단정하지 않는다. |
| 21:16:58 및 21:17:06: `UserBlockRangeTracker`가 null 위치를 소비하다 NPE | 첫 NPE 직전에 `USER_BLOCK_RANGE_NULL_INPUT_OBSERVED`도 존재한다. 보호 범위 목록의 null 소비이며 금 목표 좌표가 null이라는 뜻은 아니다. |
| 21:17:05: `HashMap.keysToArray → HashSet.toArray → LinkedList.addAll → BlockScanner.getKnownLocationsIncludeUnreachable:93`에서 AIOOBE | 공유 목록 읽기 경계는 확인됐다. 어느 변경과 겹쳤는지는 기존 로그로 확인되지 않았다. 동시 변경은 조사 가설이며 확정 원인이 아니다. |
| 21:16:49, 1106행: 점유 28/36, 빈칸 8, `inventory_at_or_below_low_water`, ARMED 유지 | 이 평가에서는 자동보관이 시작될 공간 조건이 아니었다. 검사하지 않은 후속 조건은 `NOT_EVALUATED`다. 이후 시점의 인벤토리도 같다고 추정하지 않는다. |
| 21:18:18, 6147행: `DIAGNOSTIC_SESSION_CAP_REACHED`, 일반 4,540개 소진, 최초 억제 `TOOL_EQUIP_REQUEST` | 반복 횟수와 보관 판단을 계속 확인할 별도 제한 요약이 필요하다. |

21:20까지 후속 경로 로그가 이어졌다. 이번 관측은 게임 프로세스 종료 크래시와 구분한다. 이전 Builder 이동 목록/인덱스 문제 및 20:19 관찰자 한도 크래시와도 같은 원인으로 묶지 않는다.

## 추가 관측 경계와 책임 분리

`diagnostics/blocks/collection/`에 진입 API, `state/`에 제한된 구간·세대·목록 출처 메타데이터, `state/output/`에 최초/요약 예산과 출력 회계, `format/`에 불변 snapshot의 필드 구성, `context/`에 요청 맥락, `emission/`에 출력 호출 책임을 둔다. `BlockCollectionLedger`는 구간 추적과 이 책임들의 호출을 조정한다. upstream의 `BlockScanner`와 `UserBlockRangeTracker`에는 실제 호출 주변의 최소 hook만 둔다.

1. `trackedBlocks`의 기존 등록·초기화·clear/add/addAll/put과 `getKnownLocationsIncludeUnreachable`의 기존 읽기 진입/정상·비정상 반환을 관측한다. 원래 컬렉션 호출 횟수·인자·순서는 유지한다. 로그를 위해 컬렉션을 추가 순회하거나 크기를 읽지 않는다.
2. scanner/컬렉션 식별값, thread, 읽기 번호, write 진입 세대, 관측 구간의 겹침을 연결한다. 읽기에 겹친 변경의 제한된 메타데이터를 비정상 반환/null 소비에도 붙여, 최초 일반 겹침 사건이 이미 억제돼도 해당 구간을 설명한다. 진단 메타데이터의 짧은 잠금은 원래 게임 연산을 둘러싸지 않는다. write 진입 세대는 실제 변경 성공 횟수가 아니다. 구간 겹침 역시 실제 메모리 접근 충돌의 확정 증거가 아니다. 요청 정보는 컬렉션 진입 시점의 활성 명령 맥락이며, worker를 원래 제출한 요청의 소유권 증거로 간주하지 않는다.
3. 원래 반환 목록을 제한된 weak reference로 읽기 출처에 연결한다. `UserBlockRangeTracker`의 기존 null 관측 지점에서 이 출처를 붙인다. 목록을 교체하거나 null을 제거하지 않는다. 추적 범위를 넘은 출처는 누락 상태로 남긴다.
4. `try/finally`의 정상 반환 여부는 진단 메타데이터다. 원래 게임 예외는 그대로 전파한다. 진단 자체의 `RuntimeException`/`LinkageError`는 진단 경계에서만 격리한다.

scanner 관측 입장은 프로세스 전체에서 최대 2개, 입장당 활성 구간 16개·반환 목록 weak 연결 16개·최초 의미 사건 32개·반복 요약 16개로 제한한다. OFF/teardown은 게임 객체 연결과 활성 구간을 무효화하지만 입장 횟수와 고정 누계를 초기화하지 않는다. OFF→ON을 반복해도 예약이 늘지 않으며, 입장/요약 소진·출력 거절은 최종 회계에 남긴다. 최초 의미는 작업 종류/결과/겹침 등 고정된 의미로 구분한다. thread ID, tick, 좌표, 매번 바뀌는 sequence를 최초 사건 키로 사용하지 않는다. 반복 요약은 200 tick 및 10초 조건을 모두 충족해야 한다. 신규 lifecycle 관찰자는 1개만 등록하며 기존 typed 등록 실패·OFF·teardown·늦은 콜백 차단을 사용한다.

## 일반 예산 소진 이후 자원 요약

자원 관측은 실제 `DETAIL` 출력이 일반/공유 총 한도로 거절된 뒤에만 별도 checkpoint 상태를 활성화한다. 단순 포맷 오류·sink 실패·OFF를 예산 소진으로 취급하지 않는다. 같은 scope의 후속 사건에서 200 tick 및 10초마다 제한 요약을 허용한다. 별도 타이머가 아니므로 후속 사건이 없으면 주기 출력도 없다.

checkpoint는 scope당 최대 32개다. 최초 사건과 실제 종료 사건의 우선순위·예약은 유지한다. 금 채굴의 후속 관측 후보에는 현재 누계와 최초 연결을 함께 실어, 다음 사건이 준비 단계가 아니라 도구 선택/장착이어도 최근 누계를 확인할 수 있게 한다. 예약 요약 소진도 종료/최종 회계에서 구분한다.

| 프로세스 진단 예산 | 값 |
| --- | --- |
| 전체 상한 | 5,000 유지 |
| 일반 출력 | 4,060 (이전 4,540) |
| 기존 critical | 64 유지 |
| 자원 최초 사건 | 채굴/보관/Builder 각각 128, 총 384 유지 |
| 자원 종료 | 12 유지 |
| 신규 자원 요약 | 각 영역 128 = 4 scopes × 32, 총 384 |
| 신규 공유 블록 최초/요약 | 64 / 32 |
| 총 예약 | 940 |

`RESOURCE_OBSERVATION_<DOMAIN>_SUMMARY`, `BLOCK_COLLECTION_FIRST`, `BLOCK_COLLECTION_SUMMARY`를 기존 admission authority에 연결한다. 일반 상세 로그 한도는 더 일찍 닫히며 그만큼 후속 요약 공간이 확보된다. 무제한 출력이나 전체 상한 증가는 아니다. 자원 scope 입장 상한은 기존 영역당 4개/총 12개이며, 초과 scope에 최초 사건·요약 출력을 보장하지 않는다.

각 tier의 capture/시도/승인/출력 함수 반환/실패 및 거절 사유를 구분한다. `filePersistence=NOT_VERIFIED`를 유지한다. 함수 반환이나 자동 테스트만으로 실제 Minecraft 파일 기록을 확인했다고 하지 않는다.

## 검증과 인수 조건

source candidate 검증은 `test/test_Isolation/minecraft/resource_observation/verify_resource_observation.ps1 -CompileDiagnosticCandidate`로 수행한다. 바뀐 production source를 별도 출력 폴더에 새로 컴파일하고 이를 기존 named 1.20.1 의존 클래스보다 앞에 둔다. 세션 패키지와 상수 사용 emitter도 함께 재컴파일해 예전 inlined 한도가 섞이지 않게 한다. canonical build 클래스와 JAR는 덮어쓰지 않는다.

최초/요약/종료 예산 분리, sink 실패와 예산 거절 구분, 반복 누계, OFF/늦은 콜백, 구간 겹침과 제한, 원래 반환·예외 보존, 실제 관찰자 18개 구성과 ToolEquip 최후 초기화를 확인한다. 이 검사는 전체 Gradle 빌드나 실제 게임 재현을 대신하지 않는다.

| 단계 | 이번 단위 상태 |
| --- | --- |
| production source overlay | `PASS`: 수정 경계와 의존 상수 사용자를 포함한 88개 Java source를 `--release 17`로 새로 컴파일 |
| 기존·신규 통합 focused suite | **337개 실행 / 336개 성공 / 실패 0개 / 기존 초기화 중단 1개**; wrapper exit 1, 전체 PASS로 표시하지 않음 |
| 공유 블록 검사 | **16/16 성공**, 실패·중단·skip 0; 별도 JVM의 활성 진단 native 호출 포함 |
| 이번 신규 검사 | checkpoint 6개 + block 16개 = **22개 모두 성공**, 최종 통합 실행에 모두 포함 |
| 실제 production 관찰자 구성 | **18개 등록**, ToolEquip 마지막 초기화·역순·bridge 선행·상한 초과 시 진단 격리 검사 통과 |
| clean Gradle / 실행 JAR | `NOT_RUN` |
| 배포 / 실제 Mixin 적용 / Minecraft 재현 / 물리 로그 확인 | `NOT_RUN` |

최종 증거는 `test/test_Isolation/minecraft/resource_observation/output/469b311560264815ad4ca41add622710/`의 `candidate-compile.log`, `compile.log`, `tests.log`, `inputs.txt`다. 104개 test class·113개 test/support source를 사용했다. 책임 분리와 invalidated ledger의 늦은 출력 회계 차단을 반영한 source를 새로 컴파일한 결과다. 기존 중단은 `AutoDepositCategoryReservePolicyTest.allocatesFuelAsOneCategoryTotalInsteadOfPerItem()`의 named Minecraft bootstrap `IllegalAccessError`이며 이전 실행과 동일하다. 이를 통과로 세거나 보관 정책을 바꿔 우회하지 않았다.

앞선 중간 결과도 보존했다. `output/d9880f502ada466f91eeea1ff49f4f5b/`는 책임 분리 전 84개 source·334개 검사(333 성공/기존 중단 1)이며, `output/blocks-7bbfc4d385d34c7abc4a03483556c5f9/`는 해당 production source 84개의 SHA-256이 그대로임을 확인한 뒤 block test source 5개만 새로 컴파일한 16/16 결과다. 그 뒤 source 책임 분리가 있었으므로 최종 인수 증거는 위 `469b311560264815ad4ca41add622710` 실행을 따른다.

후속 검사에는 이미 일반 overlap 최초 로그가 소모된 뒤 다른 읽기에서 예외가 나도 해당 구간의 peer write/겹친 write 진입 횟수가 남는 사례, 목록 반환 뒤 다른 write가 발생해도 null 소비에 원래 snapshot이 유지되는 사례, 진단이 실제 활성화된 새 JVM에서 원래 `toArray` 1회·null 요소·동일 예외 객체·관측 누계를 확인하는 사례가 포함된다. 정상/실패 반환과 진단 sink 실패, process 입장 제한, OFF/늦은 토큰도 함께 검사했다. 실행 JAR와 canonical build 클래스는 수정하지 않았다.

이후 사용자가 새 JAR로 재현했을 때 읽기 출처→write 관측 구간→비정상 반환/null 소비가 연결되는지, 일반 예산 소진 뒤 요약에 반복 누계와 보관 판단이 남는지 확인한다. 기록이 없으면 OFF/상한/출력 실패/관측 밖 경계를 먼저 구분한다. 이번 소스만으로 공유 컬렉션 경합이나 채굴 무한 반복을 해결했다고 판정하지 않는다.
