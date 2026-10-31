# Billing service — RPT-01 S09

Spring Boot 3.5.16, Gradle 8.14.5, Java 17 단일 모듈 청구 서비스다. `sourceCompatibility`/`targetCompatibility`와 `options.release = 17`을 사용한다. 지정 실행기는 JDK 21에서 Java 17 바이트코드를 생성한다.

## 빌드와 검증

프로젝트 자체의 표준 빌드 진입점은 `./gradlew build`다. RPT-01 작업에서는 저장소 루트에서 아래 오프라인 실행기만 사용한다. 실행기가 wrapper, 격리 Gradle home, JDK 21, 로컬 Maven mirror를 공급한다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s09 v2/billing-service build
```

`@SpringBootTest` + MockMvc 테스트가 웹 애플리케이션 컨텍스트와 실제 H2 DB를 기동한다. HTTP 요청은 프로세스 안에서 처리하므로 서버 소켓이나 외부 연결이 필요 없다. 테스트는 저장 JSON까지 SQL로 확인한다. `bootRun`은 실행하지 않는다.

## API와 시나리오

| 요청 | 동작 | 응답 |
| --- | --- | --- |
| `PUT /invoices/{invoiceId}` | 고객·수량·단가로 계산하여 저장. 같은 ID는 전체 갱신 | 200, 청구 JSON |
| `GET /invoices/{invoiceId}` | 저장된 청구 조회 | 200, 같은 JSON. 없는 ID는 404 |

`PUT /invoices/INV-001`에 [input/invoice.json](input/invoice.json)을 보낸다.

```json
{"customer":"  Acme, Seoul  ","quantity":3,"unitPrice":335}
```

고객명 양끝 공백을 제거하고 `3 × 335 = 1,005`를 계산한다. 세금 10%인 100.5를 HALF_UP으로 101에 반올림하여 총액은 1,106이다. 응답과 이후 `GET /invoices/INV-001` 응답은 [expected/invoice.json](expected/invoice.json)과 JSON 동등하다.

```json
{
  "invoiceId":"INV-001",
  "customer":"Acme, Seoul",
  "quantity":3,
  "unitPrice":335,
  "subtotal":1005,
  "tax":101,
  "total":1106,
  "csvLine":"INV-001,\"Acme, Seoul\",1005,101,1106"
}
```

ID는 ASCII 영숫자·밑줄·하이픈 1~64자, 고객명은 trim 후 1~120자다. 수량·단가는 필수 정수이며 수량은 양수, 단가는 0 이상이다. 금액 단위는 원이다. 잘못된 입력과 long 곱셈·합계 overflow는 400이다. 유효성 검증에 실패한 PUT은 기존 청구를 바꾸지 않는다. 소수 수량·금액을 정수로 잘라 받지 않는다.

## 의존성 사례

운영 의존성 10개와 `testImplementation` 1개를 직접 선언한다. 버전 관리는 Boot BOM을 사용하며 관리 좌표의 선언에는 버전을 생략한다. Boot 기본 commons-codec 1.18.0은 계약의 1.17.1을 유지하기 위해 `commons-codec.version` 속성으로 재정의한다. 사내 `data-convert:1.1.0`과 `antlr4-runtime:4.13.2`만 선언에 버전을 쓴다. 사내 저장소 URL은 기존 v2와 같으며 검증 실행기가 로컬 저장소로 치환한다.

| 좌표/역할 | 소스 근거와 의도 |
| --- | --- |
| starter-web | REST 컨트롤러의 하위 spring-web 참조로 USED_VIA_CHILD |
| starter-jdbc | 하위 spring-jdbc의 `JdbcTemplate.update`/`queryForList` 직접 호출로 USED_VIA_CHILD |
| h2 (runtimeOnly) | H2 in-memory 실제 저장. 앱에 드라이버 클래스 직접 참조가 없으므로 UNUSED_CANDIDATE LOW |
| commons-lang3 | `StringUtils.isBlank`/`strip` 정적 호출 |
| jackson-databind | `new ObjectMapper()`, JSON 저장·복원 직접 호출. 선택 2.21.4와 저장소 BOM 2.18.2 불일치 |
| data-convert 1.1.0 | `CsvLine.join` 정적 호출로 쉼표·따옴표를 이스케이프한 CSV 필드 생성 |
| commons-codec 1.17.1 | **한 번도 사용하지 않음**. 체크섬 기능을 위해 선언했으나 구현하지 않은 잔류, HIGH |
| antlr4-runtime 4.13.2 | **기능 삭제 후 잔류**. 이전의 ANTLR 기반 청구 규칙 표현식 파서를 삭제하고 고정 10% 세금 계산으로 대체했으나 선언은 남김, HIGH |
| logback-classic 1.5.34 (runtimeOnly) | starter 로깅 경로와 중복 선언한 런타임 바인딩, 클래스 직접 참조 없음, LOW |
| slf4j-api | `LoggerFactory.getLogger`와 `Logger.info` 직접 호출. 선택 2.0.18과 저장소 BOM 2.0.16 불일치 |
| starter-test (testImplementation) | 테스트 전용 DECLARED_ONLY |

하위 사용 사례로 spring-jdbc 6.2.19를 별도 정답표에 기록한다. starter 자체에는 참조할 클래스가 없으며, 사용 여부는 하위 라이브러리 참조로 판단한다. Boot 의존성 관리와 분석기 기준 `bom/*.gradle`은 서로 다른 기준이다. 정답표의 `bom_managed`, `bom_version`, `expected_out_of_bom`은 후자 기준이다.

앱 코드에는 람다·method reference·reflection·ServiceLoader·설정상의 클래스 이름 로딩을 추가하지 않았다. H2는 Boot JDBC 자동 구성의 일반적인 드라이버 로딩을 사용한다.

## 테스트와 한계

계산 단위 테스트 8개와 Spring 통합 테스트 6개가 시나리오 전체, CSV 이스케이프, 세금 경계, 0원, 필수값, ID, overflow, 갱신, 400/404, 실패 시 기존 데이터 보존을 확인한다. H2의 `MERGE`와 기본키로 ID당 한 행을 저장하며 저장된 JSON은 ObjectMapper로 복원한다.

DB는 프로세스 종료 시 사라진다. 인증·외부 결제 연동·통화 변환·청구 목록·부하 시험은 표본 범위에 포함하지 않는다. 시작 검증은 MockMvc 기반이며 실제 TCP 포트 수신은 검증하지 않는다.
