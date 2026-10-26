# partner-gateway — SMP-01 S4

Java 21 단일 모듈 외부 연계 표본이다. 요청 목록을 `PartnerClient.get(path)`으로 순서대로 조회하고 `JsonCodec.read/write`로 사내 응답 형식으로 변환한다. HTTP 클라이언트는 공통 라이브러리만 사용한다. C17/C18은 전이 의존이며 서비스 코드에 직접 참조가 없고, C19는 생성·get 호출·PartnerException catch, C20은 runtimeOnly 로깅 바인딩으로만 사용한다. 로그는 SLF4J API를 통해 콘솔 INFO 수준으로 출력한다.

저장소 루트에서 지정된 오프라인 실행기로 실행한다. wrapper 8.14.5의 네 파일은 `svc-01`에서 그대로 복사했다. 외부 다운로드나 서버는 필요하지 않다.

```sh
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s4 v2/partner-gateway build
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s4 v2/partner-gateway run --args='--stub input/requests.json /private/tmp/depgov-smp01/s4/responses.json'
cmp v2/partner-gateway/expected/responses.json /private/tmp/depgov-smp01/s4/responses.json
/Users/coolguyhong/workspace/depgov/tools/smp01/samplegradle.sh s4 v2/partner-gateway dependencies --configuration runtimeClasspath
```

`--stub`만 주면 입력은 `input/requests.json`, 출력은 `output/responses.json`이다. `--stub input output`으로 경로를 지정할 수 있다. Stub 파일은 입력 파일과 같은 디렉터리의 `stub-responses.json`을 읽는다. JDK `HttpServer`를 `127.0.0.1`의 임의 포트에 열고 처리 완료나 예외 발생 시 닫는다. 별도로 실행 중인 로컬 서버는 `--base-url http://127.0.0.1:PORT [input output]`으로 호출한다. 외부 네트워크 사용을 막기 위해 이 모드도 `http://127.0.0.1`만 허용한다. 리디렉션은 공통 클라이언트가 따르지 않는다.

요청은 `requestId`, `/`로 시작하는 `path`를 갖는 JSON 배열이다. Stub은 `path`, HTTP `status`(200~599), JSON `body`를 갖는 배열이다. 경로와 query를 정확히 일치시켜 응답하며 등록하지 않은 경로는 404다. 중복 stub 경로는 오류로 거부한다.

파트너 응답의 `id`는 사내 `partner.code`, `companyName`은 `partner.displayName`, `state`의 ACTIVE/INACTIVE는 `partner.enabled`의 true/false로 바뀐다. 성공 항목은 `status: SUCCESS`, `error: null`이다. HTTP 오류는 `status: FAILED`, `partner: null`, 공통 클라이언트 오류 메시지를 기록한다. 잘못된 응답 JSON이나 필수 값은 `INVALID_PARTNER_RESPONSE`로 기록한다. 실패 항목 뒤의 요청도 처리하고 입력 순서를 유지한다. 파일 전체의 잘못된 요청 형식이나 파일 입출력 오류는 실행을 실패시킨다. 개별 조회 실패를 결과에 정상 기록한 실행은 exit 0이다.

기본 fixture는 한글 이름의 활성 파트너, 404 실패, 비활성 파트너 세 건이다. UTF-8 JSON과 마지막 LF를 출력하며 `expected/responses.json`과 바이트 단위로 일치한다. JUnit 테스트는 실제 loopback HTTP를 통해 기본 시나리오, query 전달, HTTP 500/미등록 404, 잘못된 응답, 빈 입력, 외부 URL 거부, 입력 오류, 중복 stub을 확인한다. 결과와 의존성 트리는 [S4_RESULT.md](../S4_RESULT.md)에 기록한다.
