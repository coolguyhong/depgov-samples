# RPT-01 C — S09 Billing service 구현 결과

2026-10-31, Codex 제출. 상태: **구현·오프라인 빌드 완료, Claude 독립 검증 대기**.

## 제출 범위

- `v2/billing-service/**`: Java 17 단일 모듈, Spring Boot 3.5.16, Gradle 8.14.5 wrapper, Groovy DSL.
- `v2/answer-key-v2-s09.csv`: 기존 본체와 동일한 23열, **12개 사례 C49~C60**, holdout 전부 N.
- `v2/S09_RESULT.md`: 이 결과 문서.

운영 직접 선언은 계약 5.1의 **10개**(implementation 8 + runtimeOnly 2), `testImplementation`은 starter-test **1개**, 전체 직접 선언은 **11개**다. 정답표는 이 11개와 starter-jdbc 경유 spring-jdbc 하위 사용 1개를 포함한다. Gradle plugin 2개는 라이브러리 선언 수에 포함하지 않는다.

기존 서비스·libs·bom·maven-repo·정답표 본체는 변경하지 않았다. git add/commit/checkout/reset/clean, 네트워크 요청, 사용자 `~/.gradle` 접근을 하지 않았다. 빌드 산출물과 프로젝트 캐시는 서비스 내부 `.gitignore`로 제외했다.

## 구현

REST endpoint는 `PUT /invoices/{invoiceId}`와 `GET /invoices/{invoiceId}` 2개다. 고객명 공백 제거, 정수 수량 × 단가, 10% 세금 HALF_UP 반올림, overflow 검증, 사내 CsvLine CSV 이스케이프를 수행한다. JdbcTemplate의 `update`/`queryForList` 직접 호출로 H2 in-memory에 JSON을 저장·조회하고, ObjectMapper 생성·직접 호출로 직렬화·복원한다. 같은 ID의 PUT은 한 행을 갱신한다. 잘못된 입력은 400, 없는 ID 조회는 404다.

README, `input/invoice.json`, `expected/invoice.json`에 입력 → 예상 응답을 고정했다. 3 × 335 = 1,005, 세금 101, 총액 1,106이며 쉼표 포함 고객명의 CSV 따옴표도 검증한다.

commons-codec는 한 번도 구현되지 않은 체크섬 기능용 선언, antlr4-runtime은 삭제된 청구 규칙 표현식 파서의 잔류로 구분하여 build.gradle 주석과 README에 기록했다. LoggerFactory/getLogger와 Logger/info를 직접 호출하며 logback 클래스는 직접 참조하지 않는다. 앱 코드에 람다·method reference·reflection·ServiceLoader·설정상의 클래스 이름 로딩을 새로 넣지 않았다.

## 실행과 검증

모든 Gradle 실행은 저장소 루트에서 아래 지정 실행기만 사용했다. 실행기는 offline, 격리 Gradle home, 로컬 Maven mirror, Gradle 8.14.5, JDK 21을 사용한다. 컴파일은 `sourceCompatibility`/`targetCompatibility = 17`, `options.release = 17`이며 toolchain은 사용하지 않는다.

| 실행 명령 | exit | 결과 |
| --- | --- | --- |
| `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s09 v2/billing-service build` | 0 | 최초 빌드·14개 테스트 통과 |
| `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s09 v2/billing-service build dependencies --configuration runtimeClasspath --warning-mode all` | 0 | 소수 입력 거부 추가 후 14개 테스트 통과, 선택 버전 확인 |
| `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s09 v2/billing-service build --no-watch-fs --warning-mode all` | 0 | URL Groovy 대입 문법 수정 후 최종 빌드 성공, 7개 task UP-TO-DATE |
| `python3` 읽기 전용 검증 | 0 | wrapper 4개 파일 바이트 일치, 앱 클래스 6개 major 61, XML·CSV·근거 줄 검증 |

| 테스트 클래스 | 수 | 실패/오류/skip | 주요 검증 |
| --- | --- | --- | --- |
| `BillingApplicationTest` | 6 | 0/0/0 | SpringBootTest + MockMvc, 실제 H2 SQL 저장값, PUT→GET 시나리오, 갱신, 400/404, malformed JSON·누락·소수 입력, 실패 시 기존 데이터 보존 |
| `InvoiceCalculatorTest` | 8 | 0/0/0 | HALF_UP 경계, 0원, CSV 따옴표, 고객·수량·단가·ID 유효성, 곱셈 및 총액 overflow |
| 합계 | **14** | **0/0/0** | 모두 PASS |

XML: `v2/billing-service/build/test-results/test/TEST-com.depgov.sample.billing.{BillingApplicationTest,InvoiceCalculatorTest}.xml`.
HTML: `v2/billing-service/build/reports/tests/test/index.html`.
실행 artifact: `v2/billing-service/build/libs/billing-service-1.0.0.jar`.

첫 두 빌드의 FSEvents stream 경고는 비치명적이며 마지막 실행은 `--no-watch-fs`로 제거했다. JVM CDS 경고도 시험 성공에 영향이 없었다. 저장소 URL의 Groovy 대입 문법 경고는 수정했고 마지막 실행에는 deprecation 경고가 없었다.

## 버전과 정답표 근거

선택 버전은 starter-web/jdbc/test 3.5.16, h2 2.3.232, commons-lang3 3.17.0, jackson-databind 2.21.4, data-convert 1.1.0, commons-codec 1.17.1, antlr4-runtime 4.13.2, logback-classic 1.5.34, slf4j-api 2.0.18, spring-jdbc 6.2.19다. 필요한 좌표는 모두 오프라인 공급본에 있어 추가 의존성이나 네트워크 공급은 필요하지 않았다.

Boot BOM 관리 좌표의 버전은 선언에서 생략했다. commons-codec의 Boot 기본 1.18.0은 계약의 1.17.1을 유지하도록 `ext['commons-codec.version'] = '1.17.1'`로 재정의했다. 정답표의 `declared_version`은 버전이 없는 선언에는 빈 값이며, 해당 속성과 기대 선택값을 note로 기록했다.

정답표는 계약과 실제 소스·build.gradle 줄을 읽어 사전 작성했으며 분석기를 실행하거나 그 출력으로 역생성하지 않았다. Gradle의 runtimeClasspath 보고서는 선택 버전 확인에만 사용했다. `bom_managed`·`bom_version`은 Boot BOM이 아니라 읽기 전용 `bom/*.gradle` 전체 기준이다. 기준과 선택이 다른 jackson-databind(2.18.2 → 2.21.4), slf4j-api(2.0.16 → 2.0.18)의 versionMismatch만 true다.

예상 판정은 REFERENCED 5, USED_VIA_CHILD 2, UNUSED_CANDIDATE 4(HIGH 2 + LOW 2), DECLARED_ONLY 1이다. H2는 DB 접근에 실제 사용되지만 앱에서 드라이버 클래스를 참조하지 않으므로 규칙의 런타임 드라이버 목록에 따라 LOW로 기록했다. logback도 클래스 참조 없는 런타임 바인딩으로 LOW다.

CSV 검증: 헤더 일치, 12개 좌표 중복 없음, C49~C60 연속, 모든 evidence_file 존재·줄 범위 유효, 직접 11개 + 전이 1개, holdout N. wrapper 4개 파일은 `v2/order-service`의 원본과 바이트 동일하며, 생성된 앱 클래스 6개는 모두 Java 17 major version 61이다.

## 한계와 후속 검증

기동은 SpringBootTest의 MOCK 웹 환경으로 검증했다. `bootRun`과 실제 TCP 포트 수신은 실행하지 않았다. H2는 프로세스 종료 시 사라지며 SQL MERGE는 H2에 맞춘 표본이다. 인증·외부 결제·동시 요청 부하 시험은 범위 밖이다. 분석기의 poc-3 판정, 정답표 병합·최종 hold-out 배정은 Claude 독립 검증 단계다.
