<!-- 20260913_kpopmodder: Preserve the reviewed runtime snapshot, exact tool mismatch, and separate uncertainty boundaries. -->
# 실행 근거와 미확정 범위

[설계 목차](README.md)로 돌아간다. 아래 행 번호는 고정한 ZIP의 원문 기준이다. 이후 재실행으로 바뀐 live `latest.log`에 그대로 적용하지 않는다.

## 비교 자료와 출처

| 항목 | 식별 |
| --- | --- |
| 저장소 | `C:\Vtuber_Souorce_Code\LAVI` |
| HEAD / branch | `dfe29ef8547ee85fa736b75b208b126fc73f0b8b` / `minecraft-plugin-fix/alto-clef-infinite-loop` |
| 실제 instance | `C:\Users\jaewo\curseforge\minecraft\Instances\LAVI_TEST_Fabric01` |
| 실행 버전 | Minecraft 1.20.1 / Fabric Loader 0.19.3 / ChatClef 0.18.23 |
| command | `금 10개 캐줘` → `get gold_ingot 10` |
| request | `lavi-input-ko-9b1df4e2b6fc4244928726e45334ad89` |
| correlation | `lavi-46f108d745114f1a967d9688e7669117` |
| session | `fabric-chatclef-cd27390addae4ddea1cef62b64750f1f` |
| context generation | 진단 맥락 1 / bridge ownership 2. 출처가 다른 두 필드이며 임의 통합하지 않음 |

고정 자료는 [ChatGPT 전달 ZIP](../../../../logs/chatgpt_handoff/gold_mining_20260913_223205/ChatGPT_full_evidence_final.zip)이다. ZIP SHA-256은 `5c889dc1cfb7b503601c7e13d2dc59033c1ab535621bde4c366112a7dcf8723c`이다. `logs/`의 로컬 산출물이므로 Git clone에 따라오지 않을 수 있다. 이 문서에는 핵심 수치와 해시를 따로 남긴다.

ZIP의 `repository/`는 기존 변경이 섞인 작업 트리 사본이다. 이전 빌드 입력 manifest에 존재하는 첨부 파일 1,975개가 해시 일치했고, 후속 검수에서는 현재 운영 Java 1,964개가 ZIP 사본과 일치했다. 모든 변환·로딩 클래스까지 그 비교만으로 증명한 것은 아니다. 출처가 확인되지 않은 다른 ZIP이나 HEAD만을 실행 소스로 대체하지 않는다.

검수 의견 원문은 사용자가 제공한 `C:\Users\jaewo\.codex\attachments\ad24f145-3c72-421b-ba4f-2dee7dafe1e7\pasted-text.txt`다. 의견 자체를 런타임 증거로 취급하지 않고 아래 로그·소스와 대조했다.

## 로그 snapshot

2026-09-13 22:32:06~07 KST에 파일별 open 당시 길이까지 실제 읽었다. 파일 간 원자적 동시 snapshot은 아니다. 디렉터리 metadata의 과거 크기·수정 시각으로 0 byte나 기록 종료를 판단하지 않았다.

| ZIP `evidence/runtime/current/` 파일 | bytes / 행 | 원문 SHA-256 |
| --- | --- | --- |
| `latest.log` | 8,703,957 / 14,711 | `5a98779747ee25198621f82c66b57ab7bc8c07f7b0d37db7e2aefd3a3fe1cde6` |
| `stdout-logs.txt` | 11,266,329 / 43,937 | `6096031440ec1220143815cbf478a1772dbc9a677262b60f76a2c7bd9126cae5` |
| `instance_audit.txt` | 3,037,077 / 599 | `885e4bb5ebc9f27a5b4a14ceea6b0a0e0a6ae10570c481b4051917b1dca04ba8` |
| `LAVI_20260913_211419_log.txt` | 544,482 / 1,741 | `32822d860b32266d1f51bdb960232c6645ef9aad9493855a748949df6e3a5226` |

실제 audit 파일명은 `logs/instance_audit.txt`이다. `latest.log` 원문은 CP949이고 ZIP의 `normalized_utf8/`에는 UTF-8 변환 사본을 별도로 넣었다. stdout·LAVI는 UTF-8이다. 감사 로그 13:11:37 UTC는 22:11:37 KST에 해당한다. ScreenVision 문장은 게임 상태·원인 증거로 사용하지 않는다.

## 시간순 관측

| KST / 파일·행 | 확인된 사실 |
| --- | --- |
| 22:12:21~22 / LAVI 1284~1296 | 금 명령 accepted·전송 후 running. running은 dispatch 시작이며 완료가 아님 |
| 22:12:24 / latest 1274~1275 | 잔디 `(919,67,-1809)`용으로 inventory 19, diamond_shovel, damage 0 선택 |
| 같은 tick / latest 1276~1277 | 같은 종류의 source 19·20을 교환; 실제 hotbar 1 및 main hand는 damage 1111, `postconditionExactStackMatched=false`; 기존 돌 곡괭이는 inventory 19로 이동 |
| 같은 tick / latest 1279~1285 | `accessToolReady=true`, hotbar 미노출, `MOVE_ACCESS_PICKAXE_TO_HOTBAR`; 준비 Task가 채굴 자식을 중단. 최종 광석 목표 `(919,26,-1809)` |
| 22:12:55 / latest 3191, 3194~3226 | 보호 좌표 null 소비와 NPE stack, 읽기 구간의 쓰기 겹침 관측 |
| 22:13:36 / latest 5489 | 일반 예산 4,060 소진. 이후 예약 요약 출력 지속 |
| 22:15:15 / latest 6699 | 준비 재진입 248회, 원금 수량 변화 0 |
| 22:19:03 / latest 9471 | 자동보관 마지막 주기 요약: 31/36, 빈칸 5, below_threshold; 요약 32회 소진 |
| 22:19:05 / latest 9497 | 채굴 마지막 주기 요약: 준비 재진입 584회, 원금 수량 변화 0; 요약 32회 소진 |
| 22:26:21 / latest 14652~14656 | idle 명령 경로의 user root 교체. 이전 root `2771e44f`, 새 IdleTask `25f3bd37` |
| 같은 시각 / latest 14654~14655 | `GOLD_PARENT_STOP_OBSERVED / ON_RESOURCE_STOP`: 준비 재진입·자식 실행·중단·준비 중단 각각 1,216회, 원금 변화 0, 무변화 16,788 tick. 다음 `RESOURCE_OBSERVATION_TERMINAL / USER_ROOT_REPLACED`도 실제 출력 |
| 교체 이후 / latest 14657 이후 | 이전 root stopped이지만 TaskFinishedEvent·callback을 기다리는 bridge 기록. 진단 scope 종료와 명령 terminal은 별개 |
| 22:27:51~53 / latest 후반, stdout 43937, LAVI 1574 | 저장·서버 종료, `[EXIT] code=0, terminatedByApp=false`, 같은 session generation 2 연결 종료 |

최초 도구 반복은 NPE보다 약 31초 앞선다. 후반 code 0 종료를 NPE 때문에 발생한 새 프로세스 크래시로 묶지 않는다. idle 경로는 확인됐지만 누가 어떤 UI에서 명령했는지는 이 기록만으로 단정하지 않는다. 금 수집 성공 증거는 없다.

## null 원인에 대한 한계

- reader `pool-8-thread-4`, `READ_QUERY=185002`, copiedSourceCount 1, write 관찰 generation `183771→183827`, 겹친 write 56회다.
- 최초 겹친 write는 Render thread의 `BlockScanner.scanCloseBlocks.cached.addAll`이다. query `collectionIdentity=UNAVAILABLE`, `writeCausalAttribution=NOT_PROVEN_CAUSE`이므로 정확한 null 생성자라고 확정할 수 없다.
- `nullSourceAttribution=UNPROVEN_MULTIPLE_COPY_SOURCES`는 실제 출력된 일반 문구다. 실제 copiedSourceCount 1과 구분한다.
- write 관찰 generation은 쓰기 진입 번호이며 컬렉션 내용 revision이 아니다.
- `HashSet.toArray()` 복사 중 set 축소로 비어 있는 배열 요소가 넘어왔다는 설명은 **재현 가설**이다. 실제 게임 JVM과 실제 공유 경계의 통제된 실행 순서로 확인해야 한다. 가짜 컬렉션이 null을 반환하는 테스트만으로 원본 race를 재현했다고 보고하지 않는다.

## 실행 JAR와 기존 검증

| 대상 | 기존 증거 |
| --- | --- |
| 실행 및 로컬 1.20.1 JAR | 8,683,405 bytes, SHA-256 `f69f0fc4269020dc60f13341b038386e069a1926a0ada59f40680f87ce6e9aa5` |
| 포함된 Baritone JAR | 1,808,159 bytes, SHA-256 `807a467dbddede6769d9b666f176225694f8586d9237e0b62713fbda39fe0b27` |
| ToolSet | ZIP `evidence/artifacts/ToolSet.javap.txt`의 실제 bytecode는 기본 getBestSlot에서 index 0~8 검사. ToolSetMixin은 전체 인벤토리 선택으로 확장하지 않음 |
| 기존 빌드 | 22:06:20~22:09:08, exit 0. `:1.20.1:clean :1.20.1:remapJar --rerun-tasks --no-build-cache --no-daemon --stacktrace --offline --init-script .gradle/codex-build-init.gradle` |
| 기존 focused runner | started 337 / successful 336 / failed 0 / aborted 1. AutoDepositCategoryReservePolicyTest의 기존 named Minecraft 초기화 제약 관련 중단과 runner AssertionError 포함. 전체 통과 아님 |

빌드 증거는 `logs/build/block-observation-1201-20260913-220600.log` 및 artifact/input manifest, 테스트 증거는 `test/test_Isolation/minecraft/resource_observation/output/469b311560264815ad4ca41add622710/`이며 ZIP에도 사본이 있다. 빌드 당시 deployment/runtime `NOT_RUN`과 이후 사용자 실행에서 읽은 증거는 다른 단계다. 이번 설계의 동작 수정·자동 테스트·새 빌드·새 실게임 검증은 아직 수행하지 않았다.
