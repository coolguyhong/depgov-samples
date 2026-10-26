# SMP-01 S1 구현 결과

상태: **Claude 검증 대기**

- 기준 commit: `9c92ba21c25de888b7f7c051485a9bd4b88cb8c7` (`git rev-parse HEAD` 실행으로 확인).
- 작업 디렉터리: `/Users/coolguyhong/.codex/worktrees/smp01-s1/depgov-samples`, 지정 branch `codex/smp01-s1`, task-id `s1`.
- 구현: Gradle multi-project의 공통 라이브러리 5개 1.0.0, 공개 API와 JUnit 5 테스트, 로컬 Maven publish, README.
- 검증: build/publish/runtimeClasspath 명령 모두 exit 0. **35 tests, failures 0, errors 0, skipped 0**. JAR 5개, POM 5개, module metadata 5개, Maven metadata 5개 및 checksum 80개를 유지했다.
- 작업 시작 시 이미 `svc-01/.gradle/`, `svc-01/build/`가 untracked였다. 그대로 보존했으며 이번 신규 파일은 허용된 `v2/libs/**`, `v2/maven-repo/**`, 본 문서뿐이다.

## 1. 빌드·테스트·publish

저장소 루트에서 아래 3개 Gradle 명령을 각각 1회 실행했다. 전부 지정 실행기를 통해 오프라인, task별 GRADLE_USER_HOME, Temurin JDK 21, `--no-daemon`으로 실행했다. Gradle이 출력한 single-use JVM은 각 명령 종료 시 중단되는 실행기 기본 동작이며 지속 daemon은 사용하지 않았다.

| 실제 명령 | exit | 결과 |
| --- | --- | --- |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs build` | 0 | BUILD SUCCESSFUL in 6s; 21 tasks 실행; 35 tests 통과 |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs publish` | 0 | BUILD SUCCESSFUL in 2s; 26 tasks(15 실행, 11 up-to-date); 저장소 내 `v2/maven-repo/`에 publish |
| `/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs :ext-client:dependencies --configuration runtimeClasspath` | 0 | BUILD SUCCESSFUL in 2s; 1 task 실행 |

실제 shell에서는 각 명령 뒤에 `> /private/tmp/depgov-smp01/s1/libs-<종류>.log 2>&1; result=$?; cat <같은 로그>; exit "$result"`를 붙여 출력과 실제 exit를 보존했다. 로그명은 `libs-build.log`, `libs-publish.log`, `libs-runtimeClasspath.log`다.

| 모듈 | testcase 수 | failures / errors / skipped | 정상·경계 확인 |
| --- | ---: | --- | --- |
| common-util | 10 | 0 / 0 / 0 | pad/null/빈 값/길이 경계, isBlank, HALF_UP ±.5, 0, 중간 곱셈·결과 overflow |
| data-convert | 10 | 0 / 0 / 0 | JSON POJO 왕복/null/빈 값/오류, CSV trim/인용/escape/빈 필드/잘못된 따옴표/왕복 |
| ext-client | 8 | 0 / 0 / 0 | loopback GET/UTF-8/query/slash, 201/204, 404/500/302 예외, null/잘못된 URL |
| plugin-api | 2 | 0 / 0 / 0 | SPI 타입으로 id/render 정상 호출, 빈 map/빈 결과 |
| report-plugin | 5 | 0 / 0 / 0 | ServiceLoader 단일 provider/id, key 정렬/입력 보존, 빈 map/빈 값/Unicode/null |
| 합계 | **35** | **0 / 0 / 0** | 전체 통과 |

JUnit XML은 `v2/libs/<module>/build/test-results/test/TEST-*.xml`에서 집계했다. plugin-api는 구현 없는 SPI의 호출 계약을 테스트 provider로 확인한다. report-plugin 테스트는 구현체를 직접 생성하거나 reflection으로 로딩하지 않고 ServiceLoader만 사용한다. HTTP 테스트는 JDK 내장 HttpServer를 `127.0.0.1:0`에 bind하고 매 테스트 종료 시 닫는다.

JDK 21 javac의 Java 8 obsolete 경고와 sandbox의 `Could not start the FSEvents stream` 메시지가 있었으나 빌드·테스트·publish는 모두 정상 종료했다.

## 2. publish 산출물 경로와 SHA-256

경로는 저장소 루트 기준이다. 5개 artifact의 JAR/POM/module 15개 전체를 기재한다. 추가 checksum 파일 80개도 원본 바이트로 재계산해 모두 일치함을 확인했다. `maven-metadata.xml`은 5개 모두 release/version `1.0.0`이다. 1.1.0이나 sources JAR는 생성하지 않았다.

| publish 파일 경로 | SHA-256 |
| --- | --- |
| `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar` | `21c5df854a334350a676e1a6413cba629e2c205e9db5908598ea6b05e4cecbe7` |
| `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.module` | `cbc4cefbf24f2b4dd4286f8f3fb27f7a54a4f7797eee5a90c6dbaed879ee547a` |
| `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.pom` | `18b98a80ad4b2dc6f4a3113ed9ab30083733b5954ca38ed983bf5da2212ff71c` |
| `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar` | `362462ace2d59647598f7fe0235f699671a3f7fb74074b46a1dcbbe7fa46a504` |
| `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.module` | `ffb2d103dab4831dd2a76428ebf597d1a11d4f8dbe3145b9dfe1b3eca98004fb` |
| `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.pom` | `1e3d0a799fa7b2d2e099a165b65c85e2bba21ce4bfd9fb808819b1eb71eaa9fa` |
| `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.jar` | `f1c58d0d0e23e5619d3316e639b723086ddc466fbe572ebb8fb17dc93e22d13f` |
| `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module` | `bc79b09d8fe607126af893e891672a3a4ee86099b88d7d8fb4cb7f68b8dae7b0` |
| `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.pom` | `f778e81858b4b5feb3da100a72875e3f9c20175a5971a8a5b001267093ec1cee` |
| `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.jar` | `489f3d60988b570ce36e9e92ba6736da59051d326aa44ec72c5c34e94df716a6` |
| `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.module` | `8e1fa4ad84d9ffa0bc4d266cea20bf3541c5f6490f71e8fc99da08d5542fb5e9` |
| `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.pom` | `9f0bce95b97129dcc328c36dd60484457941b1712a5aeceb41eda9ad8ee1f2de` |
| `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.jar` | `f52932ee511950ea54894726465ca088b2f6716377d3bf083bca4e8317aeabe7` |
| `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.module` | `ccacaba596eb0794e797e2b1722acc830f56a4578ac6c4279e2f95b6c76edfaa` |
| `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom` | `feb4ca2e0cfc7d53f7c420c885c60a1d2988e32799bae8aec71e98099e923cc7` |

POM 및 module metadata의 공개/런타임 의존 범위를 검증했다. 테스트 의존은 publication에 없다.

| artifact | POM 의존(scope) |
| --- | --- |
| common-util | `org.apache.commons:commons-lang3:3.17.0` (compile) |
| data-convert | `com.fasterxml.jackson.core:jackson-databind:2.18.2` (compile), `org.apache.commons:commons-lang3:3.17.0` (runtime) |
| ext-client | `org.apache.httpcomponents.client5:httpclient5:5.6.1` (runtime), `org.apache.httpcomponents.core5:httpcore5:5.4` (runtime) |
| plugin-api | 없음 |
| report-plugin | `com.depgov.plugins:plugin-api:1.0.0` (runtime) |

report-plugin의 `implementation project(':plugin-api')`가 maven-publish 기본 동작으로 외부 좌표에 변환됐고, POM을 수동 수정하지 않았다. JAR 내부 SPI 파일의 바이트는 정확히 `com.depgov.plugins.report.ReportPlugin\n`이다. module의 apiElements/runtimeElements도 POM과 일치하며 목표 JVM은 각각 8/17/21/21/21이다.

## 3. JAR class major version

publish된 각 JAR에 `unzip -l`을 실행해 class 파일을 확인하고 **모든 class 8개**에 지정된 `javap -v -classpath <jar> <fqcn> | grep 'major version'`을 실행했다(exit 0). ZIP 내 class 헤더도 별도로 검증했다.

| artifact | 목표 release | class별 major version |
| --- | ---: | --- |
| common-util:1.0.0 | 8 | `StringKit: 52`, `Money: 52` |
| data-convert:1.0.0 | 17 | `CsvLine: 61`, `JsonCodec: 61` |
| ext-client:1.0.0 | 21 | `PartnerClient: 65`, `PartnerException: 65` |
| plugin-api:1.0.0 | 21 | `Plugin: 65` |
| report-plugin:1.0.0 | 21 | `ReportPlugin: 65` |

명령 출력 원본: `/private/tmp/depgov-smp01/s1/libs-class-versions.log`. common-util은 JDK 21의 `--release 8`, data-convert는 `--release 17`, 나머지는 `--release 21`이다. 실제 JDK 8/17 프로세스에서의 실행 검증은 하지 않았다.

## 4. ext-client runtimeClasspath 실제 출력

**httpcore5 선택 버전은 5.4로 선언 5.4와 같다.** httpclient5 5.6.1도 httpcore5/httpcore5-h2 5.4를 가져오므로 `->` 버전 교체가 없다. slf4j-api는 1.7.36이다. 다음은 지정 명령 출력 전체다.

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.5/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Task :ext-client:dependencies

------------------------------------------------------------
Project ':ext-client'
------------------------------------------------------------

runtimeClasspath - Runtime classpath of source set 'main'.
+--- org.apache.httpcomponents.client5:httpclient5:5.6.1
|    +--- org.apache.httpcomponents.core5:httpcore5:5.4
|    +--- org.apache.httpcomponents.core5:httpcore5-h2:5.4
|    |    \--- org.apache.httpcomponents.core5:httpcore5:5.4
|    \--- org.slf4j:slf4j-api:1.7.36
\--- org.apache.httpcomponents.core5:httpcore5:5.4

(*) - Indicates repeated occurrences of a transitive dependency subtree. Gradle expands transitive dependency subtrees only once per project; repeat occurrences only display the root of the subtree, followed by this annotation.

A web-based, searchable dependency report is available by adding the --scan option.

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
```

## 5. 사례 ID별 S1 제공 위치와 사용 방식

아래는 S1 라이브러리가 제공하는 API/provider와 계약 사례의 연결이다. 해당 서비스 구현 및 depgov 분석 결과를 검증했다는 의미는 아니다. 모든 Java 소스·테스트 첫 줄에 관련 `SMP-Cnn`과 사용 방식을 적었다.

| 사례 ID | 구현 위치(저장소 루트 기준) | 사용 방식 / S1 확인 범위 |
| --- | --- | --- |
| SMP-C07 | `v2/libs/common-util/src/main/java/com/depgov/common/util/StringKit.java:10`, `Money.java:11` | 서비스 정적 호출용 공개 API. 내부 StringUtils 정적 호출; pad/isBlank와 금액 계산 테스트 |
| SMP-C08 | `v2/libs/data-convert/src/main/java/com/depgov/common/convert/JsonCodec.java:13`, `CsvLine.java:14` | 정적 JSON/CSV API; 공유 ObjectMapper 하나, 따옴표 CSV 지원 |
| SMP-C09 | `v2/libs/ext-client/src/main/java/com/depgov/common/ext/PartnerClient.java:16`, `PartnerException.java:4` | 생성·GET 호출 API; loopback HTTP 2xx/404 등 테스트 |
| SMP-C12 | `v2/libs/data-convert/build.gradle:2`, `src/main/java/com/depgov/common/convert/JsonCodec.java:4` | jackson-databind api 공개로 서비스의 전이 직접 참조를 지원; 서비스 쪽 참조는 S3 소관 |
| SMP-C17 | `v2/libs/ext-client/build.gradle:2`, `src/main/java/com/depgov/common/ext/PartnerClient.java:8` | HttpClient 직접 사용 및 runtime 전이 경로 제공 |
| SMP-C18 | `v2/libs/ext-client/build.gradle:3`, `src/main/java/com/depgov/common/ext/PartnerClient.java:11` | httpcore5 5.4 명시 implementation 및 EntityUtils 직접 호출 |
| SMP-C19 | `v2/libs/ext-client/src/main/java/com/depgov/common/ext/PartnerClient.java:25` | PartnerClient.get 호출용 1.0.0 API; 1.1.0 변경은 미구현 |
| SMP-C21 | `v2/libs/report-plugin/src/main/java/com/depgov/plugins/report/ReportPlugin.java:10`, `v2/libs/report-plugin/src/main/resources/META-INF/services/com.depgov.plugins.spi.Plugin:1`, `v2/libs/report-plugin/src/test/java/com/depgov/plugins/report/ReportPluginTest.java:14` | SPI provider 및 등록; 테스트는 ServiceLoader 전용. 실제 host의 SPI 전용 사용은 S5 소관 |
| SMP-C24 | `v2/libs/plugin-api/src/main/java/com/depgov/plugins/spi/Plugin.java:6`, `v2/libs/report-plugin/src/main/java/com/depgov/plugins/report/ReportPlugin.java:10` | Plugin 타입 계약 및 구현; host가 직접 참조할 별도 plugin-api artifact 제공 |
| SMP-C25 | `v2/libs/common-util/src/main/java/com/depgov/common/util/StringKit.java:10`, `Money.java:11` | Java 8 class 형식의 정적 호출용 API 제공 |
| SMP-C31 | `v2/libs/common-util/build.gradle:2`, `v2/libs/build.gradle:5` | common-util 1.0.0 외부 좌표를 publish; 사용 서비스 역조회 API 검증은 범위 밖 |
| SMP-C32 | `v2/libs/common-util/src/main/java/com/depgov/common/util/StringKit.java:10` | 1.0.0의 pad 멤버 유지; 제거 예정 계약만 README에 기록, 변경 영향 API 검증은 범위 밖 |
| SMP-C33 | `v2/libs/data-convert/build.gradle:2` | jackson-databind 2.18.2 좌표 고정; 취약점 관측·서비스 영향 분석 미실행 |

그 외 사례는 서비스·BOM·분석 검증 담당 범위이며 S1에서 구현하지 않았다. S1에 annotation 전용·method reference 전용·reflection 전용 사용 사례를 임의 추가하지 않았다.

## 6. 미확보·미실행·amendment 요청

- 미확보 좌표: **없음**. 각 모듈의 모든 문자열 리터럴 의존은 `/private/tmp/depgov-smp01/repository-gavs.txt` 안에 있으며 유일한 project 의존은 계약의 report-plugin → plugin-api다.
- 1.1.0: 생성·publish하지 않음. README의 예정 변경 요약은 계약 3.3절을 그대로 옮겼다. ext-client의 1.1.0 변경 방식은 현재 선택 버전 5.4를 근거로 Claude가 추후 확정한다.
- 미실행: 실제 JDK 8/17 실행, 서비스 빌드/분석, 공용 오프라인 mirror 복사, GKE·DB/MQ·OSV/BlackDuck·외부 HTTP·사용 서비스 역조회/변경 영향 검증. S1 요청 범위 밖이다.
- L lane의 commons-lang3 3.17.0은 공급 저장소에 POM 없이 JAR만 있어 실행기의 `artifact()` metadata source로 해석됐다(계획 2절). 합성 POM은 만들지 않았다. 해당 leaf의 외부 의존 트리에는 추가 전이 노드가 없다.
- amendment 요청: **없음**. 계획의 일반 공유파일 소유권보다 이번 S1 작업 지시의 `v2/maven-repo/**` publish 허용을 우선 적용했고, 해당 worktree의 저장소에만 publish했다. 공용 mirror의 artifact 내용은 수정하지 않았다.

## 7. 보호 규칙 준수

- 외부 host 접속 **0**, 다운로드 **0**. 웹 도구·외부 API·외부 Maven/Gradle URL 접속을 실행하지 않았다. 설정 및 Gradle 출력에 URL 문자열은 있으나 네트워크 요청은 아니다. 테스트 HTTP는 `127.0.0.1` 임의 포트만 사용했다.
- git 쓰기 **0**. add/commit/checkout/reset/clean/stash 없이 HEAD·status·diff만 읽었다.
- 수정 파일은 허용 경로로 한정. `svc-01/`, `svc-02/`, `svc-03/`, `bom/`, `fixtures/`, `.idea/`, depgov 저장소는 수정하지 않았다. 기존 wrapper를 읽어 그대로 복사했다.
- `.env` 및 비밀값은 읽거나 출력하거나 기록하지 않았다. docker/kubectl/gcloud·DB/MQ 접속을 실행하지 않았다.
- Gradle은 지정 실행기 3회만 사용했다. 사용자 `~/.gradle`·`~/.depgov-gradle`은 사용하지 않았다. 외부 plugin, catalog, map/변수/공통 subprojects 의존 선언, platform, sources JAR를 추가하지 않았다.
- 테스트 assertion 완화·삭제·skip 없음. 공통 루트에는 의존 선언이 없고 각 모듈 build.gradle에는 dependencies 블록만 있다.

## 8. 수정·신규 파일 목록(파일별 한 줄)

build/ 및 .gradle/은 프로젝트 .gitignore로 제외되는 로컬 실행 산출물이다. 아래 목록은 제출할 신규 파일 전체이며 시작 전 기존 untracked 산출물은 포함하지 않는다.

- `v2/S1_RESULT.md` — S1 제출 결과·검증 근거
- `v2/libs/.gitignore` — build/·.gradle/ 산출물 제외
- `v2/libs/README.md` — artifact 계약·사용 경계·publish 절차·1.1.0 예정 변경
- `v2/libs/build.gradle` — 공통 group/version·java-library·release·테스트·maven-publish 설정
- `v2/libs/common-util/build.gradle` — 해당 module의 문자열 리터럴 의존 선언(계약의 project 의존 예외 포함)
- `v2/libs/common-util/src/main/java/com/depgov/common/util/Money.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/common-util/src/main/java/com/depgov/common/util/StringKit.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/common-util/src/test/java/com/depgov/common/util/MoneyTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/common-util/src/test/java/com/depgov/common/util/StringKitTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/data-convert/build.gradle` — 해당 module의 문자열 리터럴 의존 선언(계약의 project 의존 예외 포함)
- `v2/libs/data-convert/src/main/java/com/depgov/common/convert/CsvLine.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/data-convert/src/main/java/com/depgov/common/convert/JsonCodec.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/data-convert/src/test/java/com/depgov/common/convert/CsvLineTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/data-convert/src/test/java/com/depgov/common/convert/JsonCodecTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/ext-client/build.gradle` — 해당 module의 문자열 리터럴 의존 선언(계약의 project 의존 예외 포함)
- `v2/libs/ext-client/src/main/java/com/depgov/common/ext/PartnerClient.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/ext-client/src/main/java/com/depgov/common/ext/PartnerException.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/ext-client/src/test/java/com/depgov/common/ext/PartnerClientTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/gradle/wrapper/gradle-wrapper.jar` — svc-01에서 바이트 그대로 복사한 Gradle 8.14.5 wrapper
- `v2/libs/gradle/wrapper/gradle-wrapper.properties` — svc-01에서 바이트 그대로 복사한 Gradle 8.14.5 wrapper
- `v2/libs/gradlew` — svc-01에서 바이트 그대로 복사한 Gradle 8.14.5 wrapper
- `v2/libs/gradlew.bat` — svc-01에서 바이트 그대로 복사한 Gradle 8.14.5 wrapper
- `v2/libs/plugin-api/build.gradle` — 해당 module의 문자열 리터럴 의존 선언(계약의 project 의존 예외 포함)
- `v2/libs/plugin-api/src/main/java/com/depgov/plugins/spi/Plugin.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/plugin-api/src/test/java/com/depgov/plugins/spi/PluginTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/report-plugin/build.gradle` — 해당 module의 문자열 리터럴 의존 선언(계약의 project 의존 예외 포함)
- `v2/libs/report-plugin/src/main/java/com/depgov/plugins/report/ReportPlugin.java` — 계약 공개 API 구현·사례 ID/사용 방식 주석
- `v2/libs/report-plugin/src/main/resources/META-INF/services/com.depgov.plugins.spi.Plugin` — ServiceLoader provider 클래스 1줄 등록
- `v2/libs/report-plugin/src/test/java/com/depgov/plugins/report/ReportPluginTest.java` — JUnit 5 정상·경계 테스트
- `v2/libs/settings.gradle` — 고정 rootProject.name 및 5개 module include
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar` — publish JAR
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.jar.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.module` — publish Gradle module metadata
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.module.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.module.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.module.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.module.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.pom` — publish Maven POM
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.pom.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.pom.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.pom.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/common-util/1.0.0/common-util-1.0.0.pom.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/common-util/maven-metadata.xml` — publish Maven version metadata
- `v2/maven-repo/com/depgov/common/common-util/maven-metadata.xml.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/common-util/maven-metadata.xml.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/common-util/maven-metadata.xml.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/common-util/maven-metadata.xml.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar` — publish JAR
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.jar.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.module` — publish Gradle module metadata
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.module.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.module.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.module.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.module.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.pom` — publish Maven POM
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.pom.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.pom.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.pom.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/data-convert/1.0.0/data-convert-1.0.0.pom.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/data-convert/maven-metadata.xml` — publish Maven version metadata
- `v2/maven-repo/com/depgov/common/data-convert/maven-metadata.xml.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/data-convert/maven-metadata.xml.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/data-convert/maven-metadata.xml.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/data-convert/maven-metadata.xml.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.jar` — publish JAR
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.jar.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.jar.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.jar.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.jar.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module` — publish Gradle module metadata
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.pom` — publish Maven POM
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.pom.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.pom.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.pom.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.pom.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/common/ext-client/maven-metadata.xml` — publish Maven version metadata
- `v2/maven-repo/com/depgov/common/ext-client/maven-metadata.xml.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/common/ext-client/maven-metadata.xml.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/common/ext-client/maven-metadata.xml.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/common/ext-client/maven-metadata.xml.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.jar` — publish JAR
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.jar.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.jar.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.jar.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.jar.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.module` — publish Gradle module metadata
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.module.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.module.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.module.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.module.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.pom` — publish Maven POM
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.pom.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.pom.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.pom.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/1.0.0/plugin-api-1.0.0.pom.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/maven-metadata.xml` — publish Maven version metadata
- `v2/maven-repo/com/depgov/plugins/plugin-api/maven-metadata.xml.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/maven-metadata.xml.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/maven-metadata.xml.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/plugin-api/maven-metadata.xml.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.jar` — publish JAR
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.jar.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.jar.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.jar.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.jar.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.module` — publish Gradle module metadata
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.module.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.module.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.module.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.module.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom` — publish Maven POM
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom.sha512` — publish sha512 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/maven-metadata.xml` — publish Maven version metadata
- `v2/maven-repo/com/depgov/plugins/report-plugin/maven-metadata.xml.md5` — publish md5 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/maven-metadata.xml.sha1` — publish sha1 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/maven-metadata.xml.sha256` — publish sha256 checksum
- `v2/maven-repo/com/depgov/plugins/report-plugin/maven-metadata.xml.sha512` — publish sha512 checksum

## 9. 실제 실행 명령과 exit 보충 기록

주요 Gradle 3개는 1절, JAR 검사 명령은 아래에 적었다. 읽기 조사 중 존재하지 않는 경로·sandbox의 프로세스 목록 조회 거부로 exit 1인 명령이 있었으며 모두 기록했다. 이들은 구현·빌드 실패가 아니다. Python 실행은 모두 `/opt/homebrew/bin/python3 -I -B`로 수행했다. 편집은 apply_patch와 본 문서 생성을 위한 Python을 사용했다.

### 명령 1 — exit 0

기준 commit·초기 상태.

```sh
pwd && git rev-parse HEAD && git status --short
```

### 명령 2 — exit 0

AGENTS 위치 검색; 장시간 실행 후 세션 회수, 종료 출력 exit 0.

```sh
rg --files -g AGENTS.md -g '!**/build/**' /Users/coolguyhong/.codex /Users/coolguyhong/workspace/depgov/tools/smp01 /Users/coolguyhong/workspace/depgov/docs 2>/dev/null
```

### 명령 3 — exit 0

계약 전체 읽기.

```sh
cat /Users/coolguyhong/workspace/depgov/docs/implementation/SMP-01_PLAN.md
```

### 명령 4 — exit 0

기존 구성·허용 좌표·오프라인 실행기 읽기; v2는 당시 없음.

```sh
rg --files svc-01 svc-02 bom v2 2>/dev/null; cat /private/tmp/depgov-smp01/repository-gavs.txt; cat /Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh
```

### 명령 5 — exit 1

상위 AGENTS 확인(.codex/AGENTS.md 빈 파일); 작업 트리 AGENTS 검색 일치 없음.

```sh
for p in /AGENTS.md /Users/AGENTS.md /Users/coolguyhong/AGENTS.md /Users/coolguyhong/.codex/AGENTS.md /Users/coolguyhong/.codex/worktrees/AGENTS.md /Users/coolguyhong/.codex/worktrees/smp01-s1/AGENTS.md AGENTS.md; do if [ -f "$p" ]; then printf '\n%s\n' "$p"; cat "$p"; fi; done; rg --files --hidden -g AGENTS.md -g '!**/.git/**' -g '!**/.gradle/**' -g '!**/build/**' .
```

### 명령 6 — exit 1

계약 4절·기존 표본 읽기; svc-02/web와 v2가 존재하지 않음.

```sh
sed -n '120,260p' /Users/coolguyhong/workspace/depgov/docs/implementation/SMP-01_PLAN.md; cat svc-01/settings.gradle svc-01/build.gradle svc-01/gradle/wrapper/gradle-wrapper.properties svc-02/settings.gradle svc-02/build.gradle svc-02/core/build.gradle svc-02/web/build.gradle bom/*.gradle; rg --files svc-01/src svc-02/core/src svc-02/web/src; ls -ld v2 /private/tmp/depgov-smp01/maven-repo 2>/dev/null
```

### 명령 7 — exit 1

기존 소스·로컬 POM 확인; 마지막 ps만 sandbox에서 거부, 프로세스 변경 없음.

```sh
cat svc-01/src/main/java/com/depgov/sample/svc01/App.java svc-02/app/build.gradle svc-02/common/build.gradle svc-02/core/src/main/java/com/depgov/sample/svc02/core/CoreService.java; rg --files svc-02/app/src svc-02/common/src; ls -l svc-01/gradlew svc-01/gradle/wrapper/*; sed -n '1,180p' /private/tmp/depgov-smp01/repository/org/apache/httpcomponents/client5/httpclient5/5.6.1/httpclient5-5.6.1.pom; ps -axo pid,command | rg 'rg --files -g AGENTS'
```

### 명령 8 — exit 0

wrapper 복사.

```sh
mkdir -p v2/libs/gradle/wrapper && cp -p svc-01/gradlew svc-01/gradlew.bat v2/libs/ && cp -p svc-01/gradle/wrapper/* v2/libs/gradle/wrapper/
```

### 명령 9 — exit 0

build·test.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs build > /private/tmp/depgov-smp01/s1/libs-build.log 2>&1; result=$?; cat /private/tmp/depgov-smp01/s1/libs-build.log; exit "$result"
```

### 명령 10 — exit 0

참고 소스·신규 파일 확인.

```sh
cat svc-02/app/src/main/java/com/depgov/sample/svc02/app/AppMain.java svc-02/common/src/main/java/com/depgov/sample/svc02/common/CommonUtil.java; rg --files --hidden v2/libs -g '!**/.gradle/**' -g '!**/build/**'; git diff --stat -- v2/libs v2/maven-repo v2/S1_RESULT.md
```

### 명령 11 — exit 0

publish.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs publish > /private/tmp/depgov-smp01/s1/libs-publish.log 2>&1; result=$?; cat /private/tmp/depgov-smp01/s1/libs-publish.log; exit "$result"
```

### 명령 12 — exit 0

runtime 트리.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s1 v2/libs :ext-client:dependencies --configuration runtimeClasspath > /private/tmp/depgov-smp01/s1/libs-runtimeClasspath.log 2>&1; result=$?; cat /private/tmp/depgov-smp01/s1/libs-runtimeClasspath.log; exit "$result"
```

### 명령 13 — exit 0

unzip·javap, class 8개.

```sh
아래 JAR 검사 실제 shell 코드 블록 전체
```

### 명령 14 — exit 0

POM·module 읽기, wrapper 바이트 일치, SHA-256.

```sh
cat v2/maven-repo/com/depgov/plugins/report-plugin/1.0.0/report-plugin-1.0.0.pom v2/maven-repo/com/depgov/common/ext-client/1.0.0/ext-client-1.0.0.module; for file in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties; do cmp "svc-01/$file" "v2/libs/$file" || exit 1; done; shasum -a 256 v2/maven-repo/com/depgov/common/*/1.0.0/*.{jar,pom,module} v2/maven-repo/com/depgov/plugins/*/1.0.0/*.{jar,pom,module}
```

### 명령 15 — exit 0

검증 항목·집계는 본 절 마지막 문단; JSON 출력.

```sh
/opt/homebrew/bin/python3 -I -B - <<'PY' (publication 검증 heredoc)
```

### 명령 16 — exit 0

보호 경로 tracked diff 없음; 사례 위치 확인.

```sh
git status --short && git diff --check -- v2/libs v2/maven-repo v2/S1_RESULT.md && git diff --name-only -- svc-01 svc-02 svc-03 bom fixtures .idea; /opt/homebrew/bin/python3 -I -B - <<'PY' (Java 공개 API·테스트 메서드 줄 번호 출력 heredoc)
```

### 명령 17 — exit 0

검증 JSON·runtime 로그를 문서에 삽입하고 placeholder·파일 수 검증.

```sh
/opt/homebrew/bin/python3 -I -B - <<'PY' (본 문서의 집계 표·SHA·전체 파일 목록·명령 이력 생성 heredoc)
```

### 명령 18 — exit 1

문서에 삽입한 Gradle 원본 로그의 끝 공백을 발견해 최종 공백 검사가 실패했다. 코드·테스트·publish 산출물에는 영향이 없으며 명령 19에서 문서 공백을 제거하고 명령 20에서 재검사했다.

```sh
git status --short && git diff --check -- v2/libs v2/maven-repo v2/S1_RESULT.md && git diff --name-only -- svc-01 svc-02 svc-03 bom fixtures .idea; /opt/homebrew/bin/python3 -I -B - <<'PY' (최종 파일 경계·문서·checksum·공백·wrapper 검사 heredoc)
```

### 명령 19 — exit 0

문서 원본 로그의 끝 공백 제거, 실패 검사 및 재검사 이력 기록.

```sh
/opt/homebrew/bin/python3 -I -B - <<'PY' (문서 원본 로그의 끝 공백 제거·실패 검사 기록·명령 이력 갱신 heredoc)
```

### 명령 20 — exit 0

명령 18과 동일한 shell 및 Python 표준입력으로 최종 검사를 재실행했다.

```sh
git status --short && git diff --check -- v2/libs v2/maven-repo v2/S1_RESULT.md && git diff --name-only -- svc-01 svc-02 svc-03 bom fixtures .idea; /opt/homebrew/bin/python3 -I -B - <<'PY' (최종 파일 경계·문서·checksum·공백·wrapper 검사 heredoc)
```

JAR 검사 실제 shell:

```sh
for jar in v2/maven-repo/com/depgov/common/*/1.0.0/*.jar v2/maven-repo/com/depgov/plugins/*/1.0.0/*.jar; do unzip -l "$jar"; for entry in $(unzip -Z1 "$jar" | rg '\.class$'); do class_name=${entry%.class}; class_name=${class_name//\//.}; printf '%s: ' "$class_name"; /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/javap -v -classpath "$jar" "$class_name" | grep 'major version'; done; done > /private/tmp/depgov-smp01/s1/libs-class-versions.log 2>&1; result=$?; cat /private/tmp/depgov-smp01/s1/libs-class-versions.log; exit "$result"
```

exit 0. publication 검증용 Python의 표준입력은 POM 좌표·의존 scope, module의 JVM·의존 variant, Maven metadata version, ZIP class header, SPI 등록 완전 일치, JUnit XML의 test/failure/error/skip 수, GAV allowlist, 전체 checksum을 assert하고 `/private/tmp/depgov-smp01/s1/libs-verification.json`에 집계를 기록했다. 결과: `TOTAL: 35 tests; 100 published files; POM/module/SPI/checksums/GAV allowlist OK`.

状態: **Claude 검증 대기**
