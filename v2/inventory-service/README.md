# Inventory service — RPT-01 S08

JDK 21, Gradle 8.14.5, Spring Boot 3.5.16의 재고 REST 표본이다. `app`은 Boot 실행 모듈이며 `project(':domain')`의 SKU 정규화·재고 상태·JSON inclusion 정책을 사용한다. `domain`은 순수 Java 라이브러리다.

## 빌드와 검증

저장소 루트에서 지정된 오프라인 실행기만 사용한다. 실행기는 격리 Gradle home, JDK 21, 로컬 Maven mirror와 복사된 wrapper를 사용한다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s08 v2/inventory-service build
```

통상 프로젝트 빌드 진입점은 서비스 디렉터리의 `./gradlew build`다. 이 작업에서는 위 실행기를 통해서만 실행한다. `bootRun` 대신 `@SpringBootTest` + MockMvc로 Boot 웹 컨텍스트, JPA repository, H2 연결과 REST 요청을 함께 확인한다. 소켓 연결이나 외부 호출 없이 검증한다. 실행 jar는 `app/build/libs/app-1.0.0.jar`다.

## 입력 → 기대 출력

1. `POST /api/inventory`, `Content-Type: application/json`:

```json
{"sku":" sku-1 ","name":"  Blue   pen  ","quantity":12}
```

HTTP 201, JSON 본문:

```json
{"sku":"SKU-1","name":"Blue pen","quantity":12,"status":"IN_STOCK","stockCard":"SKU-1        | Blue pen"}
```

2. `GET /api/inventory/sku-1` → HTTP 200, 위와 같은 JSON. 조회는 저장된 H2 행을 읽는다. SKU는 ASCII 대문자로 바뀌며 양끝 공백을 제거한다. 이름의 공백은 하나로 합친다. `stockCard`는 `StringKit.padRight(sku, 12)`로 만든 고정 최소 너비 라벨이다.

수량 0은 `OUT_OF_STOCK`, 양수는 `IN_STOCK`이다. SKU는 정규화 후 1~32자의 영숫자·하이픈(첫 글자는 영숫자), 이름은 공백이 아닌 1~120자, 수량은 필수 비음수 정수다. 잘못된 입력은 400, 없는 SKU 조회는 404, 정규화한 SKU의 중복 등록은 409다. 데이터는 `jdbc:h2:mem:inventory`에 저장하며 프로세스 종료 후 보존하지 않는다.

## 고정 의존성 사례

| 모듈 | 선언·하위 좌표 | 사용·잔류 의도 |
| --- | --- | --- |
| app | starter-web | Spring MVC REST; 하위 jackson-databind의 `new ObjectMapper()`와 `writeValueAsString` 직접 호출 |
| app | starter-data-jpa | `JpaRepository` 상속과 `saveAndFlush`·`findById`로 H2 영속화 |
| app | starter-validation | `Validation.buildDefaultValidatorFactory()`·`Validator.validate` 직접 호출; factory는 컨텍스트 종료 시 닫음 |
| app | h2 (runtimeOnly) | H2 DB 드라이버의 런타임 로딩; 소스의 H2 클래스 직접 참조 없음 |
| app | commons-lang3 | `StringUtils.normalizeSpace` 호출 |
| app | common-util 1.1.0 | `StringKit.padRight` 호출 |
| app | commons-text 1.12.0 | 한 번도 사용하지 않은 도입 잔류 |
| app | starter-cache | 과거 SKU별 재고 조회 결과 캐시 기능을 삭제한 뒤 의존성만 남김. 캐시 annotation·설정·코드 없음 |
| app | postgresql (runtimeOnly) | 도입 후보였으나 한 번도 사용하지 않음. H2만 사용 |
| domain | commons-lang3 3.17.0 | `StringUtils.trimToEmpty` 호출 |
| domain | jackson-annotations 2.18.2 | `JsonInclude.Value.construct` 호출, app ObjectMapper의 inclusion 정책에 전달 |
| app | starter-test (testImplementation) | 테스트 전용, DECLARED_ONLY |

app은 Boot BOM 관리 좌표의 버전을 생략한다(commons-lang3 3.17.0, PostgreSQL 42.7.11 포함). domain은 Boot BOM을 가져오지 않으며 계약의 명시 버전으로 컴파일한다. app 런타임에서는 Boot BOM의 Jackson annotations 2.21, databind 2.21.4가 선택된다. 분석용 사내 `bom/*.gradle`는 빌드에 platform으로 적용하지 않는다. 사내 BOM의 Jackson 기준 2.18.2, PostgreSQL 기준 42.7.4와 선택 버전의 차이는 정답표에 기록한다.

직접 외부 선언 11개(app 9, domain 2)에 테스트 전용 선언 1개가 추가된다. 정답표는 이 12개와 starter 경유 하위 직접 사용 2개를 합쳐 14행이다. `project(':domain')`은 외부 좌표 수에 포함하지 않는다. domain 단위 테스트도 app의 테스트 소스에서 실행해 테스트 의존성을 한 곳에 둔다. 새 method reference·SPI·reflection 기반 사용은 없다.

H2처럼 런타임 로딩으로만 쓰이는 좌표는 실제 DB 사용과 별개로 바이트코드 직접 참조가 없어 `UNUSED_CANDIDATE LOW`를 기대한다. starter 자체는 클래스가 없고 하위 라이브러리를 참조하므로 `USED_VIA_CHILD`다. 사용하지 않는 starter-cache도 전이 Boot 공통 jar 가 다른 경로로 참조되어 `USED_VIA_CHILD`로 제외된다(잔류 starter 미탐, 알려진 한계 LIM-STARTER-SHARED-SUBTREE). 서비스 실행 jar에는 의도적인 미사용 잔류도 포함된다.

starter-cache는 자연스러운 선언 그대로 둔다(Claude 보완 2026-10-31: 분석기 결과를 맞추기 위한 `transitive = false` 구성을 제거).
