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
