# SMP-01 S5 — plugin-host

Java 21 기반 `host` 하나로 구성한 Gradle multi-project다. YAML 설정에서 활성 플러그인 id, handler 클래스 이름, handlerMethod, 입력 문자열 map을 읽고 보고서를 만든다.

| 사례 | 좌표 | 실제 사용 방식 | 분석기의 기대 판정 |
| --- | --- | --- | --- |
| SMP-C21 | report-plugin 1.0.0 (`runtimeOnly`) | `ServiceLoader.load(Plugin.class)`로만 발견하고 인터페이스로 실행 | UNUSED_CANDIDATE / MEDIUM, LIM-SPI 오탐 (hold-out) |
| SMP-C22 | commons-codec 1.17.1 | YAML의 클래스 이름을 `Class.forName`에 전달하고 reflection으로 digest 계산 | UNUSED_CANDIDATE / HIGH, LIM-REFLECTION 오탐 |
| SMP-C23 | snakeyaml 2.6 | `new Yaml().load(...)` 직접 호출 | REFERENCED, BOM 2.3과 versionMismatch |
| SMP-C24 | plugin-api 1.0.0 | `ServiceLoader<Plugin>`, `Plugin.id()`, `Plugin.render(...)` | REFERENCED, 사내 라이브러리 |

플러그인 구현과 handler 구현의 클래스 이름은 Java 소스(테스트 포함)에 두지 않는다. handler 클래스 이름은 `input/plugins.yml`에만 있다. 클래스 이름을 설정에서 읽고 `getMethod("sha256Hex", String.class).invoke(null, ...)`로 호출한다. `handlerMethod`는 `sha256Hex`만 허용하며 다른 값은 오류다.

## 입력과 출력

`input/plugins.yml`의 `active` 순서로 발견된 플러그인을 실행한다. 존재하지 않는 id와 중복 id는 오류다. `input`은 문자열 key/value map이어야 하며 숫자처럼 보이는 값도 YAML에서 인용한다. 빈 값 문자열과 빈 map은 허용한다.

digest 입력은 입력 key의 자연 순서로 정렬한 `key=value`를 LF로 연결한 문자열이며 마지막 LF는 없다. 기본 fixture의 digest 입력은 `amount=12500\ncustomer=C-1001`이다. 활성 플러그인의 render 결과 뒤에 `digest=<SHA-256 소문자 hex>` 한 줄을 붙인다. 출력 파일은 UTF-8이며 각 줄 끝은 LF, 마지막 줄에도 LF가 있다. expected 파일은 Python 표준 라이브러리의 SHA-256으로 독립 계산했다.

인수가 없으면 `input/plugins.yml` → `output/report.txt`를 사용한다. 인수가 있으면 입력 경로와 출력 경로 두 개를 받는다. 필요한 출력 상위 디렉터리를 생성하고 결과를 파일과 표준 출력에 쓴다. Gradle `run`과 `test`의 작업 디렉터리는 이 프로젝트 루트다.

## 오프라인 검증

저장소 루트에서 지정 실행기만 사용한다. wrapper 네 파일은 `svc-01/`의 Gradle 8.14.5 파일을 그대로 복사했다. 실행기는 JDK 21과 task 전용 Gradle home을 사용하며 모든 저장소를 로컬 mirror로 치환한다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host build
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host :host:run --args='input/plugins.yml /private/tmp/depgov-smp01/s5/report.txt'
cmp v2/plugin-host/expected/report.txt /private/tmp/depgov-smp01/s5/report.txt
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s5 v2/plugin-host :host:dependencies --configuration runtimeClasspath
```

`HostAppTest`는 SPI provider 발견·실행, 실제 reflection digest와 JDK SHA-256 비교, 전체 출력과 expected의 바이트 비교, 비활성 플러그인 제외, 변경된 UTF-8 입력의 digest, 잘못된 설정·누락 provider/handler 실패를 확인한다. 실행 기록과 runtimeClasspath는 [S5_RESULT.md](../S5_RESULT.md)에 기록한다. 분석기의 실제 판정 검증은 Claude 검증 단계에서 수행한다.
