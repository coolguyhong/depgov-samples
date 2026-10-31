# Catalog service — RPT-01 S10

상품 YAML을 정규화하여 H2에 저장하고 SKU로 조회하는 Spring Boot 3.5.16 표본이다. JDK 21, Gradle 8.14.5 wrapper, Groovy DSL을 사용한다.

| 모듈 | 역할 | 외부 직접 선언 수(테스트 제외) |
| --- | --- | --- |
| app | REST, YAML 파싱, 첨부 파일명 처리, Boot 실행 | 8 |
| domain | JPA Product entity, SKU·이름·가격 검증 | 1 |
| infra | ProductRepository extends JpaRepository, 저장·조회 | 2 |

프로젝트 의존은 `app → project(':domain'), project(':infra')`, `infra → project(':domain')`이다. domain의 JPA annotation 컴파일에는 infra starter의 외부 artifact 중 `jakarta.persistence` API만 재사용한다(`domain/build.gradle`의 artifact view). domain의 G:A 직접 선언은 commons-lang3 하나이며 JPA API 런타임은 app/infra starter가 공급한다. 프로젝트 artifact를 이 view에서 제외하므로 domain jar를 자기 컴파일 입력으로 삼지 않는다.

Boot BOM 관리 좌표는 선언에서 버전을 생략한다. SnakeYAML의 `snakeyaml.version` BOM 속성은 계약의 2.6으로 재정의했다(Boot 기본 2.4). commons-lang3는 Boot 관리 버전 3.17.0이다. depgov 기준 BOM(`bom/*.gradle`)은 버전 비교 기준이며 서비스 빌드에 import하지 않는다.

저장소 루트에서 아래 실행기만 사용한다. 네트워크 없이 공급된 배포판·Maven mirror·JDK 21과 격리 Gradle home을 사용한다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s10 v2/catalog-service build
```

일반 프로젝트의 빌드 진입점은 `v2/catalog-service`에서 `./gradlew build`이다. 이번 구현에서는 위 지정 실행기로만 Gradle을 실행했다. `bootRun`은 실행하지 않았으며 `@SpringBootTest`와 MockMvc가 웹 애플리케이션 컨텍스트, JPA repository, 실제 H2 저장·조회까지 확인한다. 테스트는 외부 HTTP 요청이나 소켓 리스너를 만들지 않는다.

## API와 시나리오

| 요청 | 동작 | 응답 |
| --- | --- | --- |
| `POST /catalog/products` | YAML 상품 등록. 동일한 정규화 SKU는 한 행을 갱신 | 200, 상품 JSON |
| `GET /catalog/products/{sku}` | SKU 공백 제거·대문자 변환 후 조회 | 200, 상품 JSON / 미등록 404 |

POST Content-Type은 `application/yaml`, `text/yaml`, `text/plain`이다. YAML 키는 `sku`, `name`, `price`, `attachment` 네 개만 허용한다. 빈 값, 추가·누락 키, 중복 키, 임의 객체 태그, 잘못된 YAML은 400이다. 정수 가격만 허용하며 음수·소수·문자열·64비트 초과 값은 거부한다. SKU는 정규화 후 영문·숫자로 시작하는 영문·숫자·하이픈 1~32자, 이름은 연속 공백 정리 후 1~100자, 첨부 파일명은 1~120자다.

입력 파일 `input/product.yaml`을 POST 본문으로 전달한다.

```yaml
sku: " cat-001 "
name: "  Travel   Mug  "
price: 12500
attachment: "uploads/2026/mug.png"
```

POST 응답과 후속 `GET /catalog/products/cat-001` 응답은 모두 `expected/product.json`과 JSON 동등하다.

```json
{"sku":"CAT-001","name":"Travel Mug","price":12500,"attachmentName":"mug.png"}
```

`CatalogApplicationTest.scenarioRegistersAndReadsPersistedProduct`가 위 두 파일로 POST→JPA 저장→별도 GET 전체를 검증한다. 첨부 경로는 Commons IO `FilenameUtils.getName`으로 파일명만 추출하며 파일이나 URL을 열지 않는다. YAML은 `new Yaml(new SafeConstructor(options)).load(...)`, 공통 유틸은 `StringKit.isBlank`, domain·infra 정규화는 `StringUtils`의 직접 호출을 사용한다. infra의 `ProductRepository extends JpaRepository`가 starter 경유 spring-data-jpa 참조 근거다. 새 method reference, ServiceLoader, reflection 로딩은 없다.

## 의도적으로 남긴 선언

- commons-collections4 4.4: 한 번도 사용하지 않은 선언. 소스·테스트·자원에 사용 코드가 없다.
- protobuf-java 4.34.2: 과거 상품 protobuf 내보내기 기능을 삭제한 뒤 남긴 선언. 현재 API는 YAML 입력·JSON 응답만 제공한다.

두 선언 모두 `app/build.gradle`에 구분 주석이 있으며 HIGH 미사용 후보다. `spring-boot-starter-test`는 app의 유일한 테스트 직접 선언이고 정답표에서는 DECLARED_ONLY다. domain 단위 테스트도 app 테스트 소스에서 실행하여 다른 모듈에 테스트 의존성을 추가하지 않았다.

H2는 runtimeOnly로 실제 DB 접근에 사용하지만 서비스 코드에서 H2 클래스를 직접 참조하지 않는다. 기존 등급 규칙의 런타임 로딩 목록에 H2가 있으므로 정답표에는 UNUSED_CANDIDATE / LOW로 기록했다. 계약 5.1 S10의 LOW 칸은 `—`이지만 H2에 대한 기존 규칙을 우선 적용한 부분이며 독립 검증 대상으로 남긴다.

테스트 16개: Spring Boot·MockMvc 통합 6개(정상 저장·조회, 404, 같은 SKU 갱신, 음수 거부, YAML 오류 거부, 415), YAML reader 단위 4개(Windows 경로·최대 가격, 문서 구조, 가격 타입·overflow, 빈 값·파일명), domain 단위 6개(정규화·무료 상품, 최대 길이, SKU·이름·가격·첨부 경계). 테스트 XML과 HTML은 `app/build/test-results/test/`, `app/build/reports/tests/test/`에 생성된다.

H2는 in-memory이며 재시작 시 데이터가 사라진다. 외부 DB, 운영 배포, 동시 SKU 갱신의 충돌 제어는 이 표본의 범위 밖이다. 정답표는 `../answer-key-v2-s10.csv`, 실행 결과와 통합 주의점은 `../S10_RESULT.md`에 있다.
