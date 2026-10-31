# RPT-01 C S10 구현 결과

상태: **구현·오프라인 빌드 PASS, Claude 독립 검증 대기**. 서비스는 `v2/catalog-service`, 정답표 조각은 `v2/answer-key-v2-s10.csv`다. 기존 서비스·libs·bom·maven-repo·정답표 본체는 수정하지 않았다. git add/commit/checkout/reset/clean, 네트워크 요청, 사용자 `~/.gradle` 접근은 하지 않았다.

## 구성과 기능

- JDK 21, Spring Boot plugin 3.5.16, dependency-management plugin 1.1.7, Gradle 8.14.5 wrapper. wrapper는 기존 `v2/order-service`에서 복사했다. Groovy DSL과 sourceCompatibility/targetCompatibility 21을 사용하며 toolchain은 없다.
- 모듈 3개: app(REST·YAML·파일명), domain(JPA Product entity·검증), infra(JpaRepository·저장·조회). 프로젝트 의존은 app → domain/infra, infra → domain으로 총 3개다.
- REST 2개: `POST /catalog/products`(YAML 상품 등록·같은 SKU 갱신), `GET /catalog/products/{sku}`(정규화 SKU 조회). H2 in-memory DB를 Spring Data JPA로 실제 저장·조회한다.
- `input/product.yaml` → `expected/product.json` 시나리오를 README와 통합 테스트에 기록했다. 정상 200, 미등록 404, 잘못된 YAML·상품 400, 지원하지 않는 Content-Type 415를 확인했다.
- 직접 외부 선언은 app 8 + domain 1 + infra 2 = **11개**, app의 testImplementation 1개까지 **12개**다. 프로젝트 의존 3개와 빌드 plugin/BOM import는 이 외부 선언 수에 포함하지 않는다.
- 정답표 **13행(C62~C74)**: 직접 선언 12행 + infra의 starter 경유 spring-data-jpa 1행. REFERENCED 6, USED_VIA_CHILD 3, UNUSED_CANDIDATE 3(HIGH 2 / LOW 1), DECLARED_ONLY 1. outOfBom true 7행, versionMismatch true 1행(SnakeYAML). 모든 holdout은 N으로 제출했으며 최종 지정은 Claude 담당이다.

미사용 commons-collections4는 한 번도 사용하지 않은 선언이고, protobuf-java는 상품 protobuf 내보내기 기능을 삭제한 뒤 남긴 선언이다. 두 종류를 app/build.gradle 주석과 README에 구분했다. 실제 사용 라이브러리는 생성자·메서드·필드·상위 타입으로 직접 참조하며 새 method reference, ServiceLoader, reflection 로딩은 없다.

## 실행과 검증

모든 Gradle 명령은 저장소 루트에서 아래 prefix만 사용했다.

```sh
/Users/coolguyhong/workspace/depgov/tools/rpt01/samplegradle.sh s10 v2/catalog-service
```

| 전달한 인자 | exit | 결과 |
| --- | --- | --- |
| `build` (최초) | 0 | BUILD SUCCESSFUL, 15초, 11 task 실행, 테스트 16개 성공 |
| `:app:dependencies --configuration runtimeClasspath :domain:dependencies --configuration runtimeClasspath :infra:dependencies --configuration runtimeClasspath` | 0 | BUILD SUCCESSFUL, 3초, 선택 버전·프로젝트 경로 확인 |
| `build` (JSON 비교 deprecated API 교체 후 최종) | 0 | BUILD SUCCESSFUL, 8초, 2 task 실행 / 9 up-to-date, 테스트 16개 성공 |

최종 빌드 로그: `catalog-service/build/build-verification.log`. Gradle 클래스패스 확인: `catalog-service/build/runtime-classpaths.txt`. 두 파일은 빌드 산출물로 git에서 제외한다. Gradle 보고서는 선택 버전 검증에만 사용했으며 정답표 판정은 소스·계약·기존 등급 규칙으로 작성했다. 분석기는 실행하지 않았다.

| 테스트 클래스 | 수 | 실패 / 오류 / skip |
| --- | --- | --- |
| CatalogApplicationTest | 6 | 0 / 0 / 0 |
| CatalogYamlReaderTest | 4 | 0 / 0 / 0 |
| ProductTest | 6 | 0 / 0 / 0 |
| 합계 | **16** | **0 / 0 / 0** |

XML은 `catalog-service/app/build/test-results/test/TEST-*.xml`, HTML은 `catalog-service/app/build/reports/tests/test/index.html`이다. 통합 테스트 로그에서 JPA repository 1개 검색, `jdbc:h2:mem:catalog` 연결, 애플리케이션 컨텍스트 시작을 확인했다. MockMvc가 소켓 없이 요청을 처리하며 실제 JPA 저장·별도 조회와 같은 SKU 갱신을 검증한다. bootRun과 외부 HTTP 요청은 실행하지 않았다. domain 단위 테스트는 app 테스트 소스에서 실행하여 테스트 직접 선언을 한 개로 유지했다.

## 통합 시 확인할 사항과 한계

1. **H2 LOW 기대값의 계약 표 차이**: 계약 5.1 S10의 LOW 칸은 `—`이지만 H2는 runtimeOnly로 선언되어 있고 서비스 코드의 H2 클래스 직접 참조는 없다. 계약 1.1의 GradeRules 유지 원칙과 기존 `analyzer-core/src/main/resources/rules/runtime-loading-coordinates.txt`의 `com.h2database:h2` 항목에 따라 C64는 UNUSED_CANDIDATE / LOW로 작성했다. 실제 DB 사용은 통합 테스트로 입증되며 의도적인 미사용 잔류 2개와 구분된다. 이 차이는 Claude 독립 검증에서 확인해야 한다.
2. **SnakeYAML 버전**: Boot BOM 기본은 2.4이므로 root build.gradle의 `snakeyaml.version` 속성을 2.6으로 재정의하고 app 선언에서는 버전을 생략했다. 선택 2.6은 계약 표와 같으며 depgov BOM 2.3과는 다르므로 C66 versionMismatch=true다. 다른 Boot 관리 좌표도 버전을 생략했다.
3. **domain 컴파일 API 공급**: 직접 선언 domain 1개를 유지하기 위해 infra compileClasspath의 외부 artifact view에서 `jakarta.persistence`만 domain 컴파일 경로에 공급한다. G:A 직접 선언이나 domain → infra 프로젝트 의존을 추가하지 않는다. 런타임은 app/infra starter가 JPA API를 공급한다. Gradle 빌드에서 순환 없이 컴파일됨을 확인했다.
4. **BOM 반영 예정**: protobuf-java는 사용자 지시대로 Claude가 추가할 v2-libs BOM 4.34.2를 선반영하여 C68 bom_managed=true / outOfBom=false로 작성했다. 현재 이 worktree의 BOM은 수정하지 않았다. commons-collections4 4.4는 기존 product.gradle에도 있다. hold-out 지정과 본체 CSV 병합도 하지 않았다.
5. 표본의 DB는 메모리 전용이며 재시작 시 소실된다. 동시 SKU 갱신의 충돌 제어·외부 DB·운영 배포·분석기 판정 통합 검증은 범위 밖이다. 지정 좌표 중 오프라인 공급이 없어 제외한 의존성은 없다. Gradle 9 비호환 deprecation 안내는 있으나 지정된 8.14.5 빌드는 통과했다.
