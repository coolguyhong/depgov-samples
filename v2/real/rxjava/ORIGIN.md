# 출처 고정 (SMP-01 R-REAL #2, 2026-10-26)

- 원본: https://github.com/ReactiveX/RxJava, tag `v3.1.12`(최신 안정 3.x; v4.0.0-alpha 는 제외)
- 취득: GitHub archive zip, sha256 31eff4c603bba0cee6213225d11e80a783425a6dafcdb36653043d62e4c521e4, 3875862 bytes
- 변경: **없음**(원본 그대로). 이 ORIGIN.md 만 추가. 라이선스 Apache-2.0(원본 LICENSE).
- 빌드 특성: Gradle wrapper 8.14, Groovy DSL 단일 module, toolchain 11/8 선택(build.gradle 55~63행), sourceCompatibility 1.8, 외부 plugin 6개(plugins.gradle.org). 의존 선언이 `"${var}"` 보간이라 분석기가 선언으로 세지 않는다(계획 1절 한계) → reactive-streams 등은 "선언 없는 REFERENCED"로 나타날 것으로 예상.
- 분석 등록: targetPath `v2/real/rxjava`, buildJdkMajor 21(Gradle JVM; toolchain 은 worker registry 의 11/8 로 해석). G lane 전용.

## 제외한 후보

- mockito v5.24.0: `settings.gradle.kts`/`build.gradle.kts`(Kotlin DSL) → depgov 미지원(SETTINGS_KTS_UNSUPPORTED). zip sha256 8de0de884dc8c9e8c55a7c4e8dfa4a9329c72fd35464eefea0e850f51ae7e08f 만 기록하고 vendoring 하지 않음.
