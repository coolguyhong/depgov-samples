# answer-key-v2 — tag별 기대 변화와 정정 기록

`answer-key-v2.csv`는 tag `v2.0` 기준이다. 뒤 tag에서 기대가 달라지는 행만 적는다. 분석기 출력으로 정답을 바꾸지 않으며, 정정은 소스 근거와 함께 적는다.

## 정정 기록(2026-10-26, 구현·검증 중)

| 사례 | 변경 | 근거 |
| --- | --- | --- |
| C11 | 좌표 slf4j-api 2.0.13 → log4j-api 2.25.4 | ext-client 가 slf4j-api 를 외부 경로(httpclient5)로도 끌어와 project(:domain) 전용 경로 사례가 되지 않음. domain 로깅을 log4j-api 로 교체 |
| C15 | 좌표 HikariCP 7.0.2 → antlr4-runtime 4.13.2 | HikariCP 의 전이 slf4j-api 2.0.17 이 오프라인 저장소에 없어 L lane 해석 불가. 전이 없는 leaf 로 교체 |
| C30 | 기대 FAILED → DEGRADED/REDUCED(fail_stage build) | 분석기 계약: build 실패는 REDUCED 추출 후 DEGRADED. FAILED 는 clone 실패·예산 초과 |

## v2.1 (order-service 의존성 변경)

| 사례 | v2.0 | v2.1 기대 | 관측(L lane run 5) |
| --- | --- | --- | --- |
| C02 | jackson-databind 2.18.2, versionMismatch false | 2.21.4(선언=선택), versionMismatch **true**(BOM 2.18.2) | 일치 |
| C03 | jackson-core 2.18.2 | 2.21.4 REFERENCED, versionMismatch true | 일치 |
| C04 | commons-compress UNUSED_CANDIDATE HIGH | 행 없음(제거) — 전이 commons-codec 도 함께 사라짐 | 일치(result-diff 4→5: removed 5, added 3) |

## v2.2 (공통 라이브러리 1.1.0 채택: settlement·gateway·legacy, order 는 1.0.0 유지)

| 사례 | v2.2 기대 | 관측(L lane run 6) |
| --- | --- | --- |
| C12 | settle 의 jackson-databind 는 data-convert 1.1.0 경유 **2.21.4**, REFERENCED, versionMismatch true | 일치 |
| C19 | gateway ext-client **1.1.0**, versionMismatch true(BOM 1.0.0) | 일치 |
| C25 | legacy common-util **1.1.0**(padRight 호출), versionMismatch true(BOM 1.0.0) | 일치 |
| C31 | common-util 역조회: 1.0.0 → order 만, 1.1.0 → settle·legacy | 일치(`/api/libraries/{coordinate}` 1.0.0: order/app, 1.1.0: settlement-batch·legacy-report) |
| C32 | `StringKit#pad` 변경 영향: order(app) 만(1.0.0 사용자). settle·legacy 는 padRight 로 이행 | 일치(`/api/change-impact`: order-service OrderApp.java:38 만) |
| C33 | jackson-databind 2.18.2 포함: order 는 2.21.4 로 이미 교체(v2.1), settle·gateway 는 data-convert 1.1.0 으로 2.21.4 → **legacy(선언만)·svc-01 만 남음** | 일치(`/api/dependencies/{2.18.2}/impact`: svc-01, legacy-report, fail-jdk(REDUCED); `{2.21.4}`: order/app depth1, settle depth2 via data-convert 1.1.0, gateway TRANSITIVE_ONLY depth2) |
| 그 외 settle/gateway/legacy 의 data-convert·ext-client·common-util 행 | 선택 1.1.0, BOM 1.0.0 → versionMismatch true |

## v2.3 (BOM-v2 상향: 공통 라이브러리 기준 1.1.0, antlr4-runtime BOM 추가)

| 사례 | v2.3 기대 | 관측(L lane run 7) |
| --- | --- | --- |
| C07/C08/C09 | order 의 common-util·data-convert·ext-client 1.0.0 → versionMismatch **true**(BOM 1.1.0) | 일치 |
| C15 | antlr4-runtime outOfBom **false**(BOM 관리로 전환) | 일치 |
| settle/gateway/legacy 1.1.0 행 | versionMismatch false 로 복귀 | 일치(C12/C19/C25 의 남은 차이는 v2.0 기준 버전값뿐) |

## poc-3 규칙(2026-10-31, RPT-01 A — 분석 전에 기록)

분석기 규칙 `poc-2 → poc-3`(invokedynamic bootstrap Handle·클래스 리터럴 참조 수집, `META-INF/services/X` 제공 jar 의 `USED_VIA_SPI` 판정). 결과 맞춤이 아니라 규칙 변경에 따른 사전 기대값이며, 아래 두 행만 바뀐다.

| 사례 | poc-2 기대 | poc-3 기대 | 근거 |
| --- | --- | --- | --- |
| C06 commons-io(order/app) | UNUSED_CANDIDATE HIGH, LIM-INVOKEDYNAMIC | **REFERENCED** | `AttachmentNames.java:12` `FilenameUtils::getExtension` 은 invokedynamic bootstrap Handle(H_INVOKESTATIC) → METHOD 참조 |
| C21 report-plugin(plugin/host, hold-out) | UNUSED_CANDIDATE MEDIUM, LIM-SPI | **USED_VIA_SPI** | `HostApp.java:29;31` 이 `com.depgov.plugins.spi.Plugin` 참조, report-plugin jar 가 `META-INF/services/com.depgov.plugins.spi.Plugin` 제공 |

C22(reflection, `Class.forName`)는 그대로 오탐(LIM-REFLECTION)이다. C05(annotation 전용)는 바뀌지 않는다(annotation 은 바이트코드 참조로 수집하지 않음).

## v2.4 (RPT-01 C — 업무형 표본 5개 추가, 2026-10-31)

`v2/inventory-service`(S08, C35~C48), `v2/billing-service`(S09, C49~C60), `v2/catalog-service`(S10, C62~C75), `v2/notify-service`(S11, C76~C89), `v2/audit-service`(S12, C90~C102). Spring Boot 3.5.16, H2, Codex `gpt-6.1-sol/high` 제작, Claude 독립 빌드·정답표 대조. hold-out 2건 추가: C68(protobuf-java), C98(postgresql). BOM-v2 에 commons-collections4 4.4·protobuf-java 4.34.2 추가(기존 좌표 불변). C61 은 결번.

정답표 작성 규칙(Claude 확정): DECLARED_ONLY 행은 분석기 계약대로 `expected_out_of_bom=false`(선언만 보존, BOM 대조 없음). `com.h2database:h2` runtimeOnly 는 실제 DB 로 쓰이지만 드라이버 클래스 참조가 없어 런타임 로딩 목록의 `UNUSED_CANDIDATE LOW` 로 기대(서비스마다 1행, LOW 는 정밀도 계산 제외). S08 starter-cache(C42)는 Codex 가 `transitive = false` 로 후보화했으나 Claude 보완으로 자연스러운 선언으로 되돌렸고, 전이 Boot 공통 jar 가 다른 경로로 참조되므로 `USED_VIA_CHILD` 로 제외된다 — 잔류 starter 미탐을 `LIM-STARTER-SHARED-SUBTREE` 로 기록한다. S10 domain 의 jakarta.persistence-api 는 Codex 의 artifactView 구성을 `compileOnly` 선언으로 바꿔 C75(DECLARED_ONLY)로 추가했다.

### 정정(2026-10-31, L lane run 1 뒤 — 소스·jar 근거로 정정, 결과 맞춤 아님)

| 사례 | 변경 | 근거 |
| --- | --- | --- |
| C60·C74·C89·C102 (starter-test, DECLARED_ONLY) | expected_selected_version 3.5.16 → 빈 값 | DECLARED_ONLY 는 runtimeClasspath 밖이라 선택 버전이 없다(분석기 계약, C48 과 동일). 작성 오류 |
| C83 log4j-api(notify) | expected_grade HIGH → MEDIUM | `log4j-api-2.25.4.jar` 안에 `META-INF/services/org.apache.logging.log4j.util.PropertySource` 가 있어 Table 51 "SPI 등록 정보" 조건 → MEDIUM. 작성 시 jar 내용을 확인하지 않은 오류. 코드가 PropertySource 를 참조하지 않으므로 poc-3 의 USED_VIA_SPI 에는 해당하지 않는다 |

L lane run 1(main 831c89e, 분석기 poc-3) 대조: 101행 중 PASS 82 → 정정 뒤 87, FAIL 10 + NOT_FOUND 1 은 모두 v2.1~v2.3 tag 변경의 기대 차이(위 절), MANUAL 4(C31~C34 흐름 사례). 신규 C35~C102 67행은 정정 뒤 전부 PASS. C06 REFERENCED·C21 USED_VIA_SPI·C42 USED_VIA_CHILD 관측.
