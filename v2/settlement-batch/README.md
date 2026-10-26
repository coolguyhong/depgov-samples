# SMP-01 S3 정산 Batch

Java 17 문법·API를 사용하는 단일 모듈이다. Gradle DSL에는 toolchain/release를 선언하지 않는다. 지정된 오프라인 실행기의 기본 JVM은 JDK 21이며 실제 JDK 17 검증 결과와 의존성 공급 한계는 [S3_RESULT.md](../S3_RESULT.md)에 기록한다.

현재 공급본에는 antlr4-runtime 7.0.2가 전이로 요구하는 `org.slf4j:slf4j-api:2.0.17`이 없어 아래 Gradle build/run은 의존성 해석 단계에서 실패한다. 요구된 의존 선언은 유지했다. 별도 JDK 17 컴파일·JUnit 28건·golden 비교는 통과했으며, 확보된 2.0.18 constraint를 추가하는 계약 보완 여부를 요청한 상태다.

저장소 루트에서 실행한다. Gradle은 다음 지정 실행기로만 실행하며, Maven 저장소 URL은 실행기의 로컬 mirror로 치환된다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch build
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch run
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch run --args='input/transactions.csv input/config.json /private/tmp/depgov-smp01/s3/settlement.json'
cmp v2/settlement-batch/expected/settlement.json /private/tmp/depgov-smp01/s3/settlement.json
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s3 v2/settlement-batch dependencies --configuration runtimeClasspath
```

인자 0개이면 모듈 디렉터리의 `input/transactions.csv`, `input/config.json`을 읽어 `output/settlement.json`을 쓴다. 인자를 지정할 때는 CSV·설정·출력 경로 3개를 모두 전달한다. 파일은 UTF-8이며 출력 부모 디렉터리는 자동 생성한다.

CSV 첫 줄은 `merchantId,transactionId,amount,feeRatePercent,status`다. 금액은 음수가 아닌 정수(long), 수수료율은 0~100 정수 퍼센트다. `CsvLine.split`으로 쉼표·큰따옴표 escape·한글 필드를 지원하며 필드 양끝 공백을 정리한다. 레코드 하나는 물리적 한 줄이고 빈 데이터 줄은 무시한다. 거래 ID는 파일 전체에서 유일해야 한다. 상태는 `APPROVED`, `CANCELLED`, `DECLINED` 중 하나이며 승인 거래만 정산에 포함한다.

`config.json`의 `maxFeeRatePercent`는 0~100 정수다. 거래별 수수료는 `Money.roundHalfUp(amount, min(feeRatePercent, maxFeeRatePercent))`다. 거래마다 먼저 HALF_UP 반올림하고, 가맹점별 승인 건수·원금·수수료·정산액(원금−수수료)을 합산한다. 결과의 가맹점은 ID의 Java 문자열 자연 순서로 정렬하며 승인 거래가 없는 가맹점은 제외한다. 마지막에 전체 합계를 기록한다. 잘못된 설정·입력, 중복 ID, 합계 overflow는 실패하며 결과 파일을 쓰기 전에 검증한다.

기본 입력은 승인 6건, 취소 1건, 거절 1건이다. 5% 거래에 상한 3%가 적용되고, 1,050원의 1%는 11원, 50원의 1%는 1원으로 반올림된다. 결과는 `expected/settlement.json`과 바이트 단위로 일치하며 요약은 다음 한 줄이다.

```text
SETTLEMENT  merchants=3 approved=6 gross=5033 fees=105 net=4928
```

| 사례 | 구현 방식 | 기대 분석 신호 |
| --- | --- | --- |
| SMP-C12 | Jackson 좌표 선언 없이 data-convert 전이 ObjectMapper를 타입으로 참조하고 `readTree(File)` 직접 호출 | REFERENCED, data-convert 경유 |
| SMP-C13 | commons-lang3 **3.16.0** 선언, `StringUtils.isBlank` 정적 호출; 공통 라이브러리가 3.17.0을 요구 | REFERENCED, `3.16.0 -> 3.17.0`, versionMismatch |
| SMP-C14 | 지정된 드라이버 좌표를 선언하고 소스에서는 참조·로딩하지 않음 | UNUSED_CANDIDATE / LOW |
| SMP-C15 | 지정된 커넥션 풀 좌표를 선언하고 소스에서는 참조·로딩하지 않음 | UNUSED_CANDIDATE / HIGH |
| SMP-C16 | `FileUtils.readLines`, `FileUtils.writeStringToFile` 정적 호출 | REFERENCED, outOfBom |

공통 라이브러리도 `CsvLine.split`, `JsonCodec.write`, `Money.roundHalfUp`, `StringKit.pad`를 직접 정적 호출한다. 테스트는 golden 파일·main 요약, 거래별 반올림·상한, 승인 필터·정렬·인용 필드, 0/100% 경계, 빈 정산, 잘못된 입력·설정, 중복 ID, overflow, 파일/JSON 오류와 CLI 인자를 검증한다. depgov 분석기 자체 검증은 Claude 검증 범위다.
