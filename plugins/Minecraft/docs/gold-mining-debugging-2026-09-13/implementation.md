<!-- 20260913_kpopmodder: Record the separately authorized behavior implementation and its verification boundaries. -->
# 구현과 검증 기록

2026-09-13 사용자가 [검수한 설계](README.md)의 네 가지 수정과 책임별 파일·폴더 분리를 명시적으로 요청했다. 이전 진단 변경이 섞인 작업 트리를 보존하면서 이번 변경만 별도 비교한다. 기존 [실행 근거](evidence.md)를 새 구현의 런타임 검증으로 사용하지 않는다.

현재 상태: `IMPLEMENTED_PENDING_RUNTIME`. 네 가지 소스 수정과 자동 검증을 완료했으며 실제 Minecraft 검증은 별도로 남아 있다.

## 비교 기준

- 저장소: `C:\Vtuber_Souorce_Code\LAVI`
- 시작 HEAD: `dfe29ef8547ee85fa736b75b208b126fc73f0b8b`
- 시작 branch: `minecraft-plugin-fix/alto-clef-infinite-loop`
- 보존 사본: `logs/gold_mining_implementation_20260913_233208/pre-implementation-inputs.zip`
- 파일별 시작 해시·기존 Git 변경 목록: 같은 폴더의 `baseline.json`
- 시작 사본 2,300개에는 runtime src, 주요 빌드 입력, AGENTS.md, 설계 문서가 포함된다. 기존 변경과 이번 변경을 HEAD diff 하나로 합쳐 소유권을 주장하지 않는다.
- 빌드 provenance: `MIXED_PROVENANCE`. 의존성·버전·외부 instance·캐시·커밋·푸시는 이번 소스 변경 단위에 포함하지 않는다.

## 구현 단위와 책임

| 단위 | 책임 경계 | 보존할 동작 |
| --- | --- | --- |
| 정확한 도구 장착 | `integration/toolselect/equip/`의 요청·검증·단일 실행·확인·진단 | 기존 후보 선택과 SAME_ITEM_TYPE_SKIP, generic 장착 API, 슬롯 입력 제한 |
| 단축바 공존 | `integration/mining/operation/hotbar/`의 배치·작업별 복구 상태 | TARGET/ACCESS 적격성, 아이템 보존, 방어·먹기·보관 선점 |
| 블록 데이터 | `blocks/scanner/`, `blocks/protection/`의 수집·작업 수명·계산·불변 공개 | 침대 주변 ±16의 기존 보호 대상, 자동방어, 기존 스캔 범위 |
| 명령 종료 | `integration/lifecycle/root/`, bridge `command/lifecycle/root/` 및 `observation/queue/` | 정상 완료·STOP·GOTO 결과 소유권, 현재 실행만 해제, 기존 재전송 |

새 코드는 composition과 작업별 상태로 나누며, upstream 클래스에는 최소 호출·조회 경계만 추가한다. upstream 클래스 이동·rename·상속을 통한 엔진 재작성은 없다. 모든 신규 연결은 Fabric 1.20.1 범위로 제한한다.

## 유한 처리와 입력 보존

- 장착 attempt의 SWAP은 최대 1회다. 반영 확인은 해당 owner의 활성 평가 최대 20회이며, 선점된 동안에는 추가 교환·역교환·확인 횟수 증가를 하지 않는다.
- 안전한 배치 부재의 복구는 금 작업의 활성 평가 최대 100회로 제한하고, 소진 결과를 요청에 결합한다. 로그 카운터와 별개의 행동 상태다.
- 보호 계산은 client tick당 최대 4,096좌표를 처리한다. 매 tick dirty만으로 계산을 재시작하지 않으며, 새 침대는 즉시 미완성 보호 영역으로 반영한다. scanner의 청크 자료 수집과 완료 결과 공개도 client tick에서 처리하고, worker는 불변 자료를 계산해 완료 mailbox에만 전달한다.
- 완료 이벤트 처리 한도는 tick당 32개를 유지한다. 교체 확인 시 이미 접수된 유한 구간을 고정하고, 뒤에 추가된 무관한 이벤트로 보류 기간을 연장하지 않는다.
- 실제 root 교체는 `failed / command_root_replaced`, root 제거는 `failed / command_root_removed`, root 유지 중 callback 소유권 교체는 `failed / command_callback_ownership_replaced`로 구분한다. 기존 명시 STOP만 해당 취소 계약을 따른다.
- root 완료는 확인했으나 callback/event 전달이 불완전하면 `unknown / root_completed_without_complete_event_handoff`로 종료한다. 완료의 중지 상태조차 읽지 못한 경우는 `unknown / root_completion_state_unavailable`이다. 목표 달성으로 만들어내거나 이전 명령을 무한 대기시키지 않는다.

## 동작과 분리한 로그

전체 세션 상한 5,000개는 유지한다. 기존 critical 예약 940개에 정확한 장착 128개, 배치 종료 4개, 보호 공개 64개를 별도로 추가하여 총 예약 1,136개, 일반 출력 3,864개로 나눈다. 기존 최초 사건·요약·종료 예약량은 줄이지 않는다. 새 관찰자는 등록하지 않는다.

장착 로그는 이미 읽은 frame의 기대/관측 source·destination과 창 슬롯·선택 손·binding 일치 여부를 사용한다. 추가 슬롯 읽기나 클릭을 실행하지 않는다. 기록은 로컬 반영 관측이며 서버 ACK라고 표시하지 않는다. NBT 본문은 출력하지 않고 전체 값 비교에만 포함한다. 출력 한도·OFF/ON·출력 실패는 교환 횟수, 보호 판단, 명령 결과를 바꾸지 않는다.

## 검증 결과

| 검증 | 현재 결과 |
| --- | --- |
| Python 종료 연결 | 신규 6개 통과: 실패 문장·단일 claim·요청/세션/메시지/소켓 불일치의 실제 수신 거부 |
| 기존 Python STOP·종료·TTS 연결 | 관련 12개 테스트 통과, 4개 subtest 통과 |
| Java 신규 및 회귀 테스트 | **346/346 통과**, failed/aborted/skipped/containerFailures 모두 0 |
| 1.20.1 clean build | **PASS**, 아래 정확한 scoped 명령 사용 |
| 최종 JAR의 기존 진단 Mixin 변환 검사 | **8/8 통과**, 실제 packaged intermediary 입력 |
| 최종 JAR의 새 보호 갱신 Mixin 변환 검사 | **1/1 통과**, 실제 refmap 및 packaged intermediary 입력, handler와 보호 갱신 호출 각각 1회 |
| 외부 instance 배포·실제 Minecraft 실행 | `NOT_RUN` |

표준 Gradle test 연결은 공유 소스를 누락하거나 다른 버전의 컴파일을 끌어올 수 있으므로, [focused test 구성](../../runtime/chatclef_fabric_1.20.1/src/test/goldMining/gold-tests.init.gradle)의 `:1.20.1:goldFocusedTests`가 실제 1.20.1 클래스와 테스트를 명시적으로 실행한다. `NO-SOURCE`, skip, abort를 통과로 계산하지 않는다. 기존 다중 버전 그래프의 전처리 단계와 다른 버전의 Java 컴파일·패키징은 구분한다.

## 최종 1.20.1 산출물

2026-09-14 00:17:32~00:20:34 KST에 새 PowerShell 프로세스에서 저장된 `build-1201.ps1`을 실행했다. PC의 실행 정책을 변경하지 않고 기존 프로젝트 방식인 `ScriptBlock::Create(Get-Content ...)`로 실행했다. 프로젝트별 캐시·임시 경로와 읽기 전용 외부 의존성 캐시를 사용했다.

```powershell
.\gradlew.bat :1.20.1:clean :1.20.1:remapJar :1.20.1:goldFocusedTests --rerun-tasks --no-build-cache --no-daemon --stacktrace --warning-mode=summary --offline --init-script .gradle/codex-build-init.gradle --init-script src/test/goldMining/gold-tests.init.gradle
```

이는 전체 버전의 `clean build` 실행과 구분한다. 실제 `compileJava` 실행 대상은 1.20.1 하나였으며, 전처리 그래프의 다른 버전 노드는 변환에만 사용됐다. Java SDK는 Temurin 21.0.12, LAVI·AltoClef 출력 클래스 2,246개의 major version은 모두 61(Java 17)이다. 기존 Jackson JAR에서 온 `META-INF/versions/19/` 항목 3개는 별도 라이브러리 항목이며 새 소스의 타깃 버전과 구분한다.

- JAR: `plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar`
- 크기: 8,773,970 bytes
- SHA-256: `5ae4159a2af60714ff3cc6373dd7fa0047f02a9ced8353a1e7ba58abe48a38a1`
- 내부 Baritone SHA-256: `807a467dbddede6769d9b666f176225694f8586d9237e0b62713fbda39fe0b27` — 기존 근거의 JAR과 동일
- 빌드 로그·입력 해시·결과: `logs/gold_mining_implementation_20260913_233208/build-20260914-001729.*`
- 실제 packaged 진단 Mixin 8개 결과: `test/test_Isolation/minecraft/mixin_application/output/bf325f497b084f4cba8b1419109e8618/`
- 실제 packaged 보호 Mixin 결과: `test/test_Isolation/minecraft/gold_mixin_application/output/3f904eccc4c7474b80b5ae1207ac927e/`
- named 보호 Mixin 별도 검사: `test/test_Isolation/minecraft/gold_mixin_application/output/5ac79b71d73c477c99062b0a1f8834ff/` — 1/1 통과, artifact 검사와 별도 집계
- 비교 파일 목록·정확한 이전/이후 diff: 같은 구현 로그 폴더의 `changes.json`, `implementation.diff`

시작 사본과 비교하여 기존 2,300개 파일을 보존했고 삭제는 없다. `AGENTS.md`와 과거 근거 로그는 변경하지 않았다. 기존 dirty 변경과 이번 변경이 함께 들어간 `MIXED_PROVENANCE` 산출물이며 커밋·푸시·외부 instance 복사는 수행하지 않았다.

검증 중 실패한 실행도 보존한다. 초기 `.ps1 -File` 호출은 실행 정책으로 시작되지 않았고, 첫 표준 test 연결은 1.21.1 컴파일까지 포함해 실패했다. 다음 실행의 전처리 주석 오류와 테스트 도우미 누락을 수정한 뒤 위 최종 명령이 통과했다. 이 중간 실행이나 Mixin compile-only 결과는 최종 성공 횟수에 포함하지 않는다.

새 보호 smoke의 첫 artifact 실행은 테스트 설정이 refmap을 읽지 않아 실패했다. 선택한 JAR의 실제 `altoclef.mixins.json`과 `chatclef-refmap.json`을 사용하도록 검증기만 수정하고 다시 실행했다. `named:intermediary`에서 `onBlockChanged`가 `class_1937.method_19282`로 해석된 후 실제 Sponge 변환과 단일 호출 검사를 통과했다. 이 테스트 설정 변경은 운영 소스·최종 JAR를 변경하지 않았고, build 입력 manifest와의 테스트 파일 차이는 별도로 보존한다.

Mixin 검사는 격리 JVM에서 선택한 클래스에 실제 변환을 적용한 결과다. 실제 Fabric 부팅, 다른 모드와의 전체 조합, 라이브 블록 callback, 실게임 로그 출력 확인은 `NOT_RUN`이다.

## 실제 게임 확인 순서

최종 JAR의 해시와 실제 instance의 로드된 JAR를 대조한 후, 기존 조건으로 `금 10개 캐줘`를 실행한다. 여러 삽 중 선택·실제 장착 일치, 곡괭이와 삽 공존, 지하 접근·광석 파괴·회수·제련·금괴 10개 확보를 각각 확인한다. 준비 반복 해소만으로 금 명령 전체 성공을 선언하지 않는다.

피격 시 공격·회피·무기 선택과 이후 채굴 복귀, 월드 변경 후 이전 보호 결과 폐기, 정상 완료·idle 교체·STOP·새 명령의 종료 응답도 확인한다. 자동보관은 ARMED 상태의 31/32/33칸 경계와 실행 중 유지·실제 전송·공간 확보를 별도로 검증한다. 이 문서의 자동 테스트와 JAR 빌드는 실게임 결과를 대신하지 않는다.
