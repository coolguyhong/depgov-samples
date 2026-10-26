# Order service — SMP-01 S2

Java 21, Gradle 8.14.5의 `app` + `domain` multi-project 주문 표본이다. `app`은 `project(':domain')`을 사용하며 공통 라이브러리는 공개 Maven 좌표의 1.0.0 artifact로 사용한다.

프로젝트 루트(`v2/order-service`)에서 실행한다.

```sh
./gradlew build
./gradlew :app:run --args='input/orders.json output/summary.json'
```

SMP-01 로컬 작업에서는 저장소 루트에서 지정 실행기만 사용한다. 이 실행기는 Java 21, 격리된 Gradle home, 사전 공급된 배포판 및 로컬 Maven mirror를 사용하며 다운로드하지 않는다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service build
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s2 v2/order-service :app:run --args='input/orders.json /private/tmp/depgov-smp01/s2/summary.json'
```

`run`의 working directory는 프로젝트 루트이므로 입력·출력 상대 경로도 그곳을 기준으로 해석한다. CLI는 `<input.json> <output.json> [--partner-url <baseUrl>]` 형식이다. 옵션이 있으면 각 주문의 원래 고객 ID로 `GET <baseUrl>/partners/<customerId>`를 수행하고 응답 본문을 결과의 `partnerResponse`에 저장한다. 옵션이 없으면 파트너 호출을 건너뛰고 해당 필드를 JSON null로 쓴다. 테스트는 JDK 내장 HttpServer를 `127.0.0.1` 임의 포트에 띄워 주문 하나의 GET 한 번을 검증한다.

입력은 `orders` 배열이며 각 주문에는 `orderId`, `customerId`, `lines`와 선택적 `attachments`가 있다. 품목의 `qty`는 양의 정수, `unitPrice`는 음수가 아닌 정수 금액이다. domain은 필수 ID와 품목을 검증하고 `Σ(qty × unitPrice)`를 계산한다. app은 주문마다 `Money.roundHalfUp(subtotal, 10)`으로 10% 세금을 HALF_UP 반올림하고 총액을 더한다. 전체 요약은 주문별 금액을 합산한다. 덧셈·곱셈 overflow는 오류로 처리한다.

`input/orders.json`은 품목 수 2·1·3개, 고객 ID 길이 2·8·14의 주문 3건이다. 짧은 고객 ID는 `StringKit.pad(customerId, 8)`로 오른쪽 공백을 채우고 긴 ID는 보존한다. 첨부 이름은 확장자 목록으로 변환한다. 출력은 `expected/summary.json`과 JSON 동등하며 합계 16,801, 세금 1,681, 총액 18,482다. 1,005의 세금 100.5가 101로 반올림되는 사례를 포함한다.

ObjectMapper로 입력을 읽고, JsonCodec.write/read로 요약 DTO를 JSON 왕복한 뒤 ObjectMapper로 읽기 좋은 결과 파일을 쓴다. 별도로 JsonFactory/createGenerator로 아래 요약 한 줄을 표준 출력에 streaming 출력한다. 옵션이 없는 기본 실행에서 세 경로가 모두 실행된다.

```json
{"orderCount":3,"subtotal":16801,"tax":1681,"total":18482}
```

| 사례 | module / 파일 | 사용 방식 |
| --- | --- | --- |
| SMP-C01 | app / OrderApp.java | StringUtils.isBlank 정적 호출 |
| SMP-C02 | app / OrderReader.java, SummaryWriter.java | new ObjectMapper(), readValue / writeValue |
| SMP-C03 | app / SummaryWriter.java | 직접 선언 없는 전이 JsonFactory 생성, createGenerator streaming |
| SMP-C04 | app / build.gradle | commons-compress 선언만 유지, 소스·테스트·자원 참조 없음 |
| SMP-C05 | app / PartnerLookup.java | javax.annotation.Nullable을 메서드 파라미터 annotation으로만 사용 |
| SMP-C06 | app / AttachmentNames.java | 확장자 추출은 method reference로만 사용 |
| SMP-C07 | app / OrderApp.java | StringKit.pad, Money.roundHalfUp 정적 호출 |
| SMP-C08 | app / SummaryWriter.java | JsonCodec.write/read 정적 호출 |
| SMP-C09 | app / PartnerLookup.java | 옵션이 있을 때만 new PartnerClient(baseUrl).get(...) |
| SMP-C10 | domain / OrderCalculator.java | StringUtils / Validate 정적 호출 |
| SMP-C11 | domain / OrderCalculator.java | domain에서만 LogManager.getLogger; app에 직접 선언·참조 없음 |

분석기 계약의 annotation / invokedynamic 수집 한계(C05/C06)와 project 경유 포함 경로 한계(C11)를 관찰하기 위한 소스 형태를 유지한다. SLF4J provider는 추가하지 않았으므로 실행 시 provider 부재 경고가 나올 수 있다. app의 선택 runtime 버전은 domain의 2.0.13이 ext-client 전이의 1.7.36보다 우선한다.

app 테스트는 임시 디렉터리에서 main을 호출하여 기대 JSON 전체, streaming 한 줄, 로컬 파트너 응답, HALF_UP 경계, 옵션 생략, 잘못된 인자·빈 주문 거부를 확인한다. domain 테스트는 품목 합산, 무료 품목, 잘못된 ID·수량·단가·빈 품목 및 overflow를 검증한다.
