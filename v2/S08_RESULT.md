# RPT-01 C — S08 구현 결과

상태: 구현·자체 검증 완료, **Claude 독립 검증 대기**. 분석기는 실행하지 않았으며 정답표는 계약·소스 참조·BOM 기준으로 작성했다.

## 구성과 수량

- 경로: `v2/inventory-service`, 모듈 `app`(Boot 실행)·`domain`(순수 Java). `app → project(':domain')`.
- JDK 21, Gradle wrapper 8.14.5, Boot plugin 3.5.16, dependency-management plugin 1.1.7, Groovy DSL. toolchain 미사용.
- 외부 운영 의존성 직접 선언: app 9개 + domain 2개 = **11개**. 테스트 전용 `testImplementation starter-test` 1개를 더해 **12개**. project 의존성·plugin 선언은 이 수에 포함하지 않는다.
- 정답표: `v2/answer-key-v2-s08.csv`, 기존 헤더와 동일한 23열, **C35~C48 14행**, holdout 전부 N. 운영 선언 11개 + 지정 starter 하위 직접 사용 2개 + 테스트 선언 1개.
- 기대 판정: REFERENCED 6, USED_VIA_CHILD 3, UNUSED_CANDIDATE 4(HIGH 1·MEDIUM 1·LOW 2), DECLARED_ONLY 1.

## 구현

`POST /api/inventory`는 SKU와 이름을 정규화하고 `Validator.validate`로 검증한 뒤 JpaRepository를 통해 H2에 저장한다. `GET /api/inventory/{sku}`는 저장된 재고를 조회한다. 수량으로 재고 상태를 계산하고 common-util 1.1.0의 `StringKit.padRight`로 라벨을 만든다. JSON은 직접 생성한 ObjectMapper로 출력한다. README에 요청과 기대 응답을 기록했다.

domain은 commons-lang3 3.17.0과 jackson-annotations 2.18.2를 명시한다. Jackson annotation만 붙이는 대신 `JsonInclude.Value.construct`를 호출하여 실제 코드 참조를 만들고, app의 ObjectMapper에 전달한다. app은 Boot BOM 관리 좌표의 버전을 생략했다.

의도적 잔류는 commons-text(한 번도 안 씀), starter-cache(과거 SKU별 조회 캐시 삭제), PostgreSQL(한 번도 안 쓴 도입 후보)이다. build.gradle 주석과 README에 구분했다. starter-cache는 `transitive = false`로 클래스 없는 starter artifact만 남겼다. 기본 전이 경로의 공통 Boot 코드 참조가 미사용 캐시 사례와 섞이는 것을 막는 구성이다.

## 실행과 검증

아래 명령은 모두 저장소 루트에서 실행했다. Gradle은 지정 실행기만 사용했다.

| 명령 | exit | 결과 |
| --- | --- | --- |
| `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s08 v2/inventory-service build` | 0 | 초기 빌드·테스트 통과 |
| `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s08 v2/inventory-service build :app:dependencies --configuration runtimeClasspath :domain:dependencies --configuration runtimeClasspath --warning-mode all` | 0 | cache 전이 분리 후 빌드 통과, 두 모듈의 선택 버전 확인 |
| `/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s08 v2/inventory-service clean build --warning-mode all` | 0 | 최종 소스에서 11개 task 실행, BUILD SUCCESSFUL |
| Python CSV·소스·wrapper 정적 검사 | 0 | 헤더·14행·C35~C48·holdout N·근거 줄 범위·main 소스 비정적 사용 금지 확인; wrapper 4개 파일이 order-service와 바이트 동일 |
| Python JUnit XML 합산 검사 | 0 | 17 tests, 0 failures, 0 errors, 0 skipped |
| `git diff --check` | 0 | 추적 파일 공백 오류 없음; 신규 소스 공백은 별도 Python 검사 |

JUnit 결과:

| 테스트 클래스 | 실행 수 | 내용 |
| --- | --- | --- |
| InventoryApplicationTest | 12 | @SpringBootTest + MockMvc; H2 실제 저장·조회, SKU/이름 정규화, 수량 0, 중복 409와 원본 보존, 미존재 404, 잘못된 입력 7개, 깨진 JSON |
| InventoryPolicyTest | 5 | domain SKU 정규화·최대 길이·null/잘못된 문자, 수량 경계, JsonInclude 정책 |

보고서: `v2/inventory-service/app/build/reports/tests/test/index.html`.
XML: `v2/inventory-service/app/build/test-results/test/TEST-com.depgov.sample.inventory.app.InventoryApplicationTest.xml`, `TEST-com.depgov.sample.inventory.app.InventoryPolicyTest.xml`.
실행 jar: `v2/inventory-service/app/build/libs/app-1.0.0.jar`.

선택 버전은 app commons-lang3 3.17.0, common-util 1.1.0, commons-text 1.12.0, H2 2.3.232, PostgreSQL 42.7.11, jackson-databind 2.21.4, jackson-annotations 2.21, jakarta.validation-api 3.0.2다. domain 자체 runtimeClasspath의 jackson-annotations는 2.18.2다. 사내 기준 BOM과 다른 PostgreSQL·databind의 versionMismatch를 true로 기록했다.

## 한계와 인계

- MockMvc가 Boot 웹 컨텍스트와 DB 경로를 검증한다. 실제 TCP 리스닝·외부 HTTP 호출·bootRun·PostgreSQL 연결은 실행하지 않았다. DB는 in-memory이고 종료 후 보존하지 않는다.
- H2는 실제 DB로 사용하지만 서비스 소스가 드라이버 클래스를 참조하지 않으므로 계약의 런타임 로딩 규칙상 UNUSED_CANDIDATE LOW를 기대한다. PostgreSQL도 런타임 로딩 목록으로 LOW다.
- C48은 production runtime에 없는 테스트 전용 선언이므로 선택 버전을 비웠다(test classpath에서는 3.5.16). outOfBom은 사용자 지정의 G:A 부재 규칙에 따라 true로 기록했다. 현재 분석기 소스의 DECLARED_ONLY 분기는 outOfBom=false를 고정하므로 Claude 독립 검증에서 이 규칙 차이를 확인해야 한다. 분석기·정답표 본체는 변경하지 않았다.
- macOS FSEvents 감시 시작 실패와 JVM CDS 안내가 출력되었지만 빌드·테스트는 성공했다. 최종 빌드에는 Gradle deprecation 경고가 없다. Gradle 9 호환성은 검증 범위가 아니다.
- 누락되어 대체하거나 새로 요청한 의존성은 없다. 네트워크 호출, 사용자 ~/.gradle 접근, git add/commit/checkout/reset/clean은 수행하지 않았다. 변경은 지정된 서비스·CSV 조각·이 결과 문서에만 남겼다.
