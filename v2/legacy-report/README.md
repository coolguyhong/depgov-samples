# legacy-report — SMP-01 S6

고정폭 매출 레코드를 월별로 합산하는 단일 모듈 Java 8 애플리케이션이다. depgov 등록 시 `targetPath=v2/legacy-report`, `buildJdkMajor=8`을 지정한다. Gradle wrapper는 svc-01의 8.14.5를 그대로 사용한다. 로컬 실행 JVM은 지정 도구가 공급하는 JDK 21이며 source/target 1.8로 class major 52를 생성한다. 실제 JDK 8 javac 빌드와 실행은 Claude의 G lane 검증 대상이다.

## 입력과 출력

`input/sales.txt`는 UTF-8, 한 줄에 26자의 ASCII 레코드다. 헤더와 빈 줄은 허용하지 않는다.

| 위치(1부터) | 길이 | 값 |
| --- | --- | --- |
| 1–8 | 8 | 유효한 날짜 `yyyyMMdd` |
| 9–14 | 6 | 영문 대문자·숫자 지점 코드 1–6자, 오른쪽 공백 채움 |
| 15–26 | 12 | 0으로 왼쪽 채운 음이 아닌 원 단위 매출 |

지점에 관계없이 연월별 매출·건수를 합산하고 연월 오름차순으로 출력한다. `TAX`는 월 매출의 10%를 HALF_UP으로 원 단위 반올림한 금액이며 `TOTAL`은 매출+세액이다. 마지막 행은 각 월의 값들을 합산한다. 따라서 전체 매출에 세율을 한 번 적용한 값과 세액 합계가 다를 수 있다. 표본은 6건, 매출 4,230원, 월별 세액 합계 424원, 합계 4,654원이다. 금액은 천 단위 구분자 없이 출력하며 LF와 마지막 개행을 고정한다. 빈 입력은 합계 0을 출력한다. 잘못된 레코드는 줄 번호와 함께 실패하며 기존 보고서를 덮어쓰지 않는다.

인수 없이 실행하면 `input/sales.txt` → `output/report.txt`, 두 인수는 입력·출력 경로다. 출력 상위 디렉터리는 자동 생성한다. 저장소 루트에서 다음 명령만 사용한다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s6 v2/legacy-report build
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s6 v2/legacy-report run --args='input/sales.txt /private/tmp/depgov-smp01/s6/report.txt'
cmp v2/legacy-report/expected/report.txt /private/tmp/depgov-smp01/s6/report.txt
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s6 v2/legacy-report dependencies --configuration runtimeClasspath
```

## 분석 사례

- SMP-C25: `StringKit.pad`로 월 컬럼을 채우고 `Money.roundHalfUp`으로 세액을 계산한다. common-util 1.0.0의 정적 호출이다.
- SMP-C26: `LoggerFactory.getLogger` 및 SLF4J 1.7.36 호출. BOM의 2.0.16과 불일치하는 선언을 유지한다. 바인딩은 추가하지 않아 SLF4J의 NOP 안내가 출력된다.
- SMP-C27: `LogManager.getLogger("legacy.audit")`로 독립된 감사 로거를 만들고 보고서 저장 후 INFO 이벤트를 딱 한 번 출력한다. log4j-core 없이 API의 기본 SimpleLogger를 사용하며 `log4j2.simplelog.properties`에서 INFO를 활성화한다. 로깅 구현체 부재에 대한 Log4j 시작 안내는 감사 이벤트와 별개다. 좌표는 BOM 밖으로 유지한다.
- SMP-C28: 사용하지 않는 의존성은 build.gradle에만 선언하고 main/test 소스에서는 참조하지 않는다.
- commons-lang3 3.17.0: `StringUtils.leftPad/rightPad` 정적 호출로 숫자·제목 컬럼을 채운다.

테스트는 JUnit Jupiter 5.11.4 / Launcher 1.11.4를 사용하며 모든 테스트 소스도 Java 8 문법이다. 검증 명령·트리·jar 호환성 근거는 [S6_RESULT.md](../S6_RESULT.md)를 참조한다.
