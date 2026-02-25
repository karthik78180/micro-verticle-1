# JUnit 5 → JUnit 6 Migration Guide
### Gradle · Java 21 · Kotlin · Devin AI Context Document

> **Purpose:** This document is a self-contained, offline reference designed for batch migration sessions with Devin AI. It covers every category of change needed to migrate from JUnit 5.x to JUnit 6.x in a Gradle project that uses Java and/or Kotlin tests. Each section provides the exact before/after transformation so Devin can apply changes package-by-package with full context and no internet access required.

---

## Table of Contents

1. [What Changed in JUnit 6 — Executive Summary](#1-what-changed-in-junit-6--executive-summary)
2. [Version Baseline & Prerequisites](#2-version-baseline--prerequisites)
3. [Gradle Build File Changes](#3-gradle-build-file-changes)
   - [Single-Module Project (Groovy DSL)](#single-module--groovy-dsl)
   - [Single-Module Project (Kotlin DSL)](#single-module--kotlin-dsl)
   - [Multi-Module Project](#multi-module-project)
   - [Gradle 9 Gotcha — junit-platform-launcher is Now Required](#gradle-9-gotcha)
4. [Dependency Version Reference](#4-dependency-version-reference)
5. [What Did NOT Change](#5-what-did-not-change)
6. [Breaking Changes & What to Fix](#6-breaking-changes--what-to-fix)
   - [Removed: junit-platform-runner](#61-removed-junit-platform-runner)
   - [Removed: junit-platform-jfr](#62-removed-junit-platform-jfr)
   - [Deprecated: junit-jupiter-migrationsupport](#63-deprecated-junit-jupiter-migrationsupport)
   - [Deprecated: JUnit Vintage Engine](#64-deprecated-junit-vintage-engine)
   - [Unified Versioning — Platform version is now the same as Jupiter/Vintage](#65-unified-versioning)
   - [CSV Parsing Changed to FastCSV](#66-csv-parsing-changed-to-fastcsv)
   - [ConsoleLauncher Subcommand Required](#67-consolelauncher-subcommand-required)
   - [JSpecify Nullability Added to All APIs](#68-jspecify-nullability-added-to-all-apis)
7. [Feature-by-Feature Migration](#7-feature-by-feature-migration)
   - [Basic Test Annotations](#71-basic-test-annotations)
   - [Parameterized Tests](#72-parameterized-tests)
   - [Dynamic Tests](#73-dynamic-tests)
   - [Nested Tests](#74-nested-tests)
   - [Extensions (@ExtendWith)](#75-extensions-extendwith)
   - [Custom Extensions](#76-custom-extensions)
   - [Lifecycle Annotations](#77-lifecycle-annotations)
   - [Assertions (AssertJ / Hamcrest)](#78-assertions-assertj--hamcrest)
   - [Tags and Filtering](#79-tags-and-filtering)
   - [Test Ordering](#710-test-ordering)
   - [Parallel Execution](#711-parallel-execution)
8. [Mockito Integration (Java)](#8-mockito-integration-java)
9. [MockK Integration (Kotlin)](#9-mockk-integration-kotlin)
10. [Testcontainers Integration](#10-testcontainers-integration)
11. [Kotlin-Specific Migrations](#11-kotlin-specific-migrations)
    - [Native Suspend Test Support](#111-native-suspend-test-support)
    - [Kotlin DSL Gradle Config](#112-kotlin-dsl-gradle-config)
    - [MockK with Coroutines](#113-mockk-with-coroutines)
12. [Vert.x Test Integration](#12-vertx-test-integration)
13. [OpenRewrite Automated Migration](#13-openrewrite-automated-migration)
14. [Package-by-Package Migration Workflow (Devin Batch Guide)](#14-package-by-package-migration-workflow-devin-batch-guide)
15. [Known Issues & Common Pitfalls](#15-known-issues--common-pitfalls)
16. [Quick-Reference Cheat Sheet](#16-quick-reference-cheat-sheet)

---

## 1. What Changed in JUnit 6 — Executive Summary

JUnit 6.0 was released on **September 30, 2025** (current stable: **6.0.3**, released February 15, 2026). It is the first major version since JUnit 5 launched in 2017. The Jupiter programming model (annotations, lifecycle, extensions) is **largely unchanged**. Your existing `@Test`, `@BeforeEach`, `@AfterEach`, `@Nested`, and `@ExtendWith` code will compile and run as-is. What changed is the infrastructure underneath:

| Category | What Changed | Action Required |
|---|---|---|
| **Java baseline** | Minimum Java 17 (was Java 8) | None — you're on Java 21 |
| **Kotlin baseline** | Minimum Kotlin 2.2 (was 1.8) | Update Kotlin plugin if below 2.2 |
| **Versioning** | Platform, Jupiter, Vintage all use the same version number | Remove separate platform version overrides |
| **Removed modules** | `junit-platform-runner`, `junit-platform-jfr` | Remove dependencies if present |
| **Deprecated** | `junit-jupiter-migrationsupport`, Vintage engine | Remove or note deprecation warnings |
| **CSV parsing** | Switched from univocity-parsers to FastCSV | Fix `#`-delimited CSV sources |
| **Kotlin suspend** | `@Test suspend fun` is now natively supported | Remove `runBlocking` wrappers |
| **Nullability** | JSpecify `@Nullable`/`@NonNull` across all APIs | Optional: clean up null-safety warnings |
| **Gradle 9** | `junit-platform-launcher` must be explicit on runtime classpath | Add `testRuntimeOnly` dependency |

---

## 2. Version Baseline & Prerequisites

Before starting any migration batch, confirm the following in the project:

```
Java: 21 (compatible — JUnit 6 requires ≥ 17)
Gradle: 9.3.1
Kotlin: ≥ 2.2 (required for Kotlin test code)
JUnit 6 target version: 6.0.3 (latest stable as of Feb 2026)
Mockito: 5.x (compatible — see Section 8)
MockK: 1.14+ (required for Kotlin 2.2 compat)
Testcontainers: 2.0.x (compatible)
AssertJ: 3.26+ (compatible — no changes needed)
Hamcrest: 2.2+ (compatible — no changes needed)
```

Check current JUnit version in any module:
```bash
./gradlew dependencies --configuration=testRuntimeClasspath | grep junit
```

---

## 3. Gradle Build File Changes

### Single-Module — Groovy DSL

**Before (`build.gradle` — JUnit 5):**
```groovy
dependencies {
    // JUnit 5 — old style: separate platform version
    testImplementation platform('org.junit:junit-bom:5.11.4')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.junit.jupiter:junit-jupiter-params'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine'
    // Note: junit-platform-launcher was NOT always required in Gradle 8
}

test {
    useJUnitPlatform()
    testLogging {
        events 'passed', 'skipped', 'failed'
    }
}
```

**After (`build.gradle` — JUnit 6):**
```groovy
dependencies {
    // JUnit 6 — unified version, platform is same as Jupiter/Vintage
    testImplementation platform('org.junit:junit-bom:6.0.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.junit.jupiter:junit-jupiter-params'
    // REQUIRED in Gradle 9: launcher must be explicit on runtime classpath
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    // If you still have JUnit 4 tests (use Vintage as bridge, note: deprecated)
    // testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'
}

test {
    useJUnitPlatform()
    testLogging {
        events 'passed', 'skipped', 'failed'
        exceptionFormat 'full'
    }
}
```

### Single-Module — Kotlin DSL

**Before (`build.gradle.kts` — JUnit 5):**
```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
}
```

**After (`build.gradle.kts` — JUnit 6):**
```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    // REQUIRED in Gradle 9 — no longer auto-provided
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
```

### Multi-Module Project

In a multi-module build, use a shared BOM in the **root** `build.gradle(.kts)` and let submodules inherit it.

**Root `build.gradle.kts`:**
```kotlin
// Applies to all subprojects
subprojects {
    plugins.withType<JavaPlugin> {
        dependencies {
            // Apply BOM platform constraint — all submodules get consistent JUnit 6 versions
            "testImplementation"(platform("org.junit:junit-bom:6.0.3"))
            // Gradle 9 requires this explicitly on every subproject's runtime classpath
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
```

**Submodule `build.gradle.kts` (only declare what that module actually uses):**
```kotlin
dependencies {
    // Version omitted — resolved from BOM in root
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params") // only if needed
}
```

### Gradle 9 Gotcha

> **This is the most common reason tests silently fail after upgrading.**

Gradle 9 no longer leaks internal dependencies into test runtime classpaths. In Gradle 8 and earlier, `junit-platform-launcher` was provided transitively by Gradle's internal test infrastructure. In Gradle 9, it must be declared explicitly. The error you will see if it is missing:

```
Execution failed for task ':test'.
> Could not start Gradle Test Executor: Failed to load JUnit Platform.
  Please ensure that the JUnit Platform is available on the test runtime classpath.
```

**Fix:** Add `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to every module that runs tests.

---

## 4. Dependency Version Reference

Use this table as the canonical reference for all dependency versions in the migration. The BOM manages JUnit versions automatically — you only need to pin versions for external libraries.

| Dependency | Group ID | Artifact ID | Version |
|---|---|---|---|
| JUnit BOM | `org.junit` | `junit-bom` | `6.0.3` |
| JUnit Jupiter (API + Engine) | `org.junit.jupiter` | `junit-jupiter` | _(from BOM)_ |
| JUnit Jupiter Params | `org.junit.jupiter` | `junit-jupiter-params` | _(from BOM)_ |
| JUnit Platform Launcher | `org.junit.platform` | `junit-platform-launcher` | _(from BOM)_ |
| JUnit Platform Suite | `org.junit.platform` | `junit-platform-suite-api` | _(from BOM)_ |
| JUnit Vintage (deprecated bridge) | `org.junit.vintage` | `junit-vintage-engine` | _(from BOM)_ |
| Mockito Core | `org.mockito` | `mockito-core` | `5.18.0` |
| Mockito JUnit Jupiter | `org.mockito` | `mockito-junit-jupiter` | `5.18.0` |
| MockK | `io.mockk` | `mockk` | `1.14.0` |
| Testcontainers Core | `org.testcontainers` | `testcontainers` | `2.0.2` |
| Testcontainers JUnit Jupiter | `org.testcontainers` | `testcontainers-junit-jupiter` | `2.0.2` |
| AssertJ | `org.assertj` | `assertj-core` | `3.27.3` |
| Hamcrest | `org.hamcrest` | `hamcrest` | `2.2` |

> **Note on Mockito and JUnit 6:** As of January 2026, the Mockito team is tracking JUnit 6 compatibility. `mockito-junit-jupiter` works with JUnit 6 because it uses the JUnit Platform Extension API, which has not changed. The `@ExtendWith(MockitoExtension.class)` approach is fully compatible. If you encounter issues, verify you are on Mockito 5.14+ and check https://github.com/mockito/mockito/issues/3779 for the latest status.

---

## 5. What Did NOT Change

The following things are **identical** between JUnit 5 and JUnit 6. Code using these does not need to be touched:

- All standard annotations: `@Test`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, `@Disabled`
- `@DisplayName`, `@Tag`, `@Timeout`
- `@Nested` class structure
- `@ExtendWith` mechanism
- `Assertions.*` methods (`assertEquals`, `assertThrows`, `assertAll`, etc.)
- `Assumptions.*` methods
- `@TempDir`, `@TestInfo`, `@TestReporter`
- `@ParameterizedTest` annotation itself
- `@ValueSource`, `@EnumSource`, `@MethodSource`, `@ArgumentsSource`
- `@CsvSource` (mostly — see Section 6.6 for a `#` delimiter edge case)
- `@CsvFileSource`
- `DynamicTest`, `DynamicContainer`, `TestFactory`
- Extension API interfaces: `BeforeEachCallback`, `AfterEachCallback`, `BeforeAllCallback`, etc.
- `ExtensionContext`, `Store`, `Namespace`
- `@RegisterExtension`
- AssertJ, Hamcrest usage — zero changes needed
- `@Testcontainers`, `@Container` annotations

In short: if a test file only uses Jupiter annotations and standard assertions, **it does not need to be changed at all**. The only required change is in the Gradle build file.

---

## 6. Breaking Changes & What to Fix

### 6.1 Removed: junit-platform-runner

This module provided a JUnit 4 `@RunWith(JUnitPlatform.class)` runner that allowed running JUnit Platform tests inside JUnit 4 environments. It has been **completely removed** from JUnit 6.

**Before (JUnit 5 — do not use in JUnit 6):**
```groovy
// build.gradle — REMOVE THIS
testImplementation 'org.junit.platform:junit-platform-runner'
```

```java
// Java — REMOVE THIS annotation
@RunWith(JUnitPlatform.class)
public class MyTestSuite { ... }
```

**After (JUnit 6 — use Suite API instead):**
```java
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.example.mypackage")
public class MyTestSuite { }
```

```groovy
// build.gradle — add this instead
testImplementation 'org.junit.platform:junit-platform-suite-api'
testRuntimeOnly 'org.junit.platform:junit-platform-suite-engine'
```

### 6.2 Removed: junit-platform-jfr

The `junit-platform-jfr` module (Java Flight Recorder events for test discovery/execution) has been **removed as a separate artifact**. Its functionality is now built directly into `junit-platform-launcher`.

**Before (JUnit 5):**
```groovy
// REMOVE THIS
testImplementation 'org.junit.platform:junit-platform-jfr'
```

**After (JUnit 6):** No action needed. JFR integration is automatic when `junit-platform-launcher` is on the classpath.

### 6.3 Deprecated: junit-jupiter-migrationsupport

`junit-jupiter-migrationsupport` provided JUnit 4 `@Rule` support inside JUnit 5. It is now **deprecated** and will be removed in JUnit 7.

**Symptom:** You will see a compilation warning or INFO-level test discovery message if this artifact is on your classpath.

**Action:** Remove the dependency. Replace any `@Rule` usages with proper JUnit 5/6 extensions.

```groovy
// REMOVE THIS
testImplementation 'org.junit.jupiter:junit-jupiter-migrationsupport'
```

**If you have `@Rule` usages still in the codebase:** Convert them to `@ExtendWith` based extensions. See Section 7.5 for examples.

### 6.4 Deprecated: JUnit Vintage Engine

The Vintage engine (which runs JUnit 3/4 tests) is **still present in JUnit 6 but is now deprecated**. When Vintage finds at least one JUnit 4 test, it will emit an INFO-level discovery message:

```
INFO: JUnit Vintage engine is deprecated and will be removed in a future version.
```

This is not a failure — existing JUnit 4 tests will still run. But the intent is clear: migrate away from Vintage during the JUnit 6 adoption window.

**Recommended action per batch:** When migrating a package, check if any classes extend `junit.framework.TestCase` or use `@RunWith`. Migrate those to Jupiter as part of the JUnit 6 upgrade batch.

### 6.5 Unified Versioning

In JUnit 5, the Platform artifacts used a **different version number** from Jupiter and Vintage:

```
// JUnit 5 — old confusion:
org.junit.jupiter:junit-jupiter:5.11.4
org.junit.platform:junit-platform-commons:1.11.4  // DIFFERENT!
org.junit.platform:junit-platform-engine:1.11.4   // DIFFERENT!
```

In JUnit 6, **all artifacts share the same version**:

```
// JUnit 6 — unified:
org.junit.jupiter:junit-jupiter:6.0.3
org.junit.platform:junit-platform-commons:6.0.3   // SAME
org.junit.platform:junit-platform-engine:6.0.3    // SAME
```

**Action:** Remove any explicit version pins on `org.junit.platform:*` artifacts. The BOM handles everything.

```groovy
// REMOVE explicit platform version pins like these:
// force 'org.junit.platform:junit-platform-commons:1.11.4'
// force 'org.junit.platform:junit-platform-engine:1.11.4'
```

### 6.6 CSV Parsing Changed to FastCSV

JUnit 6 replaced `univocity-parsers` with `FastCSV` for `@CsvSource` and `@CsvFileSource`. This is a **breaking change for tests using `#` as a CSV delimiter**, because `#` is the default comment character in FastCSV.

**Before (worked in JUnit 5):**
```java
@ParameterizedTest
@CsvSource(value = {"Alice#24", "Bob#30"}, delimiter = '#')
void testWithHashDelimiter(String name, int age) {
    assertNotNull(name);
}
```

**After (JUnit 6 — will fail with default FastCSV config):**

The `#` character triggers a parse exception in JUnit 6.0.0. This was patched in JUnit 6.0.1 — the exception no longer occurs, but behavior around `#` as both delimiter and comment character may still be inconsistent.

**Recommended fix:** Avoid `#` as a delimiter. Switch to a different delimiter character:

```java
// Option 1: Use a different delimiter
@ParameterizedTest
@CsvSource(value = {"Alice|24", "Bob|30"}, delimiter = '|')
void testWithPipeDelimiter(String name, int age) {
    assertNotNull(name);
}

// Option 2: Use default comma (preferred)
@ParameterizedTest
@CsvSource({"Alice, 24", "Bob, 30"})
void testWithCommaDelimiter(String name, int age) {
    assertNotNull(name);
}
```

**Kotlin equivalent:**
```kotlin
@ParameterizedTest
@CsvSource("Alice, 24", "Bob, 30")
fun `test with csv source`(name: String, age: Int) {
    assertNotNull(name)
}
```

### 6.7 ConsoleLauncher Subcommand Required

If you use `junit-platform-console-standalone` directly in CI scripts, note that running it without a subcommand is no longer supported. Also, non-conventional options like `--h` (instead of `-h`) have been removed.

**Before:**
```bash
java -jar junit-platform-console-standalone.jar --class-path ... --scan-classpath
```

**After:**
```bash
java -jar junit-platform-console-standalone.jar execute --class-path ... --scan-classpath
```

### 6.8 JSpecify Nullability Added to All APIs

JUnit 6 annotates all API methods with JSpecify `@Nullable`, `@NonNull`, and `@NullMarked`. This means static analyzers and Kotlin's null-safety system can now detect null-safety violations in test code.

**Impact on Java:** Warnings from tools like IntelliJ IDEA or SpotBugs may increase. No runtime impact.

**Impact on Kotlin:** If you call JUnit API methods from Kotlin and pass `null` where the API is now `@NonNull`, you will get Kotlin compile-time warnings or errors depending on your compiler configuration.

**Example — Kotlin potential new warning:**
```kotlin
// This may produce a null-safety warning in JUnit 6 because
// ExtensionContext.getDisplayName() is now annotated @NonNull
val name: String? = context.displayName  // May warn: always non-null
val name: String = context.displayName   // Now correct
```

---

## 7. Feature-by-Feature Migration

### 7.1 Basic Test Annotations

**No changes required.** All standard lifecycle annotations are identical.

```java
// Java — unchanged between JUnit 5 and JUnit 6
import org.junit.jupiter.api.*;

class UserServiceTest {

    @BeforeAll
    static void setupAll() { /* runs once before all tests */ }

    @BeforeEach
    void setup() { /* runs before each test */ }

    @Test
    @DisplayName("should create user successfully")
    void createUser() {
        // test body
    }

    @Test
    @Disabled("pending implementation")
    void pendingTest() { }

    @AfterEach
    void teardown() { }

    @AfterAll
    static void teardownAll() { }
}
```

```kotlin
// Kotlin — unchanged between JUnit 5 and JUnit 6
import org.junit.jupiter.api.*

class UserServiceTest {

    @BeforeEach
    fun setup() { }

    @Test
    @DisplayName("should return user by id")
    fun `get user by id`() {
        // test body
    }

    @AfterEach
    fun teardown() { }
}
```

### 7.2 Parameterized Tests

**Most scenarios are unchanged.** The only action item is the `#` delimiter issue described in Section 6.6.

**Java — unchanged:**
```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class CalculatorTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void isPositive(int number) {
        assertTrue(number > 0);
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void isDayOfWeek(DayOfWeek day) {
        assertNotNull(day);
    }

    @ParameterizedTest
    @MethodSource("provideStrings")
    void testWithMethodSource(String input, int expected) {
        assertEquals(expected, input.length());
    }

    static Stream<Arguments> provideStrings() {
        return Stream.of(
            Arguments.of("hello", 5),
            Arguments.of("world!", 6)
        );
    }

    // CsvSource — use comma, not '#' as delimiter
    @ParameterizedTest
    @CsvSource({"Alice, 30", "Bob, 25", "Carol, 35"})
    void testUserAge(String name, int age) {
        assertTrue(age > 0);
    }

    // CsvFileSource — unchanged
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
    void testFromFile(String name, int age) {
        assertNotNull(name);
    }
}
```

**Kotlin — unchanged:**
```kotlin
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*

class CalculatorTest {

    @ParameterizedTest
    @ValueSource(strings = ["hello", "world"])
    fun `string is not empty`(value: String) {
        assertFalse(value.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    fun `test with method source`(input: String, expected: Int) {
        assertEquals(expected, input.length)
    }

    companion object {
        @JvmStatic
        fun provideArguments() = listOf(
            Arguments.of("hello", 5),
            Arguments.of("kotlin", 6)
        )
    }
}
```

**New in JUnit 6 — `@ParameterizedClass` (promoted from experimental):**

JUnit 6 promotes `@ParameterizedClass` to stable. It allows a test class itself to be parameterized rather than individual methods.

```java
// NEW in JUnit 6 — class-level parameterization
@ParameterizedClass
@MethodSource("databases")
class DatabaseIntegrationTest {

    private final String dbUrl;  // injected via constructor

    DatabaseIntegrationTest(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @BeforeParameterizedClassInvocation  // NEW lifecycle callback
    static void setupForDb(String dbUrl) {
        // called once per parameter set before any test methods
    }

    @Test
    void canConnect() {
        assertNotNull(dbUrl);
    }

    @AfterParameterizedClassInvocation  // NEW lifecycle callback
    static void cleanupForDb(String dbUrl) { }

    static Stream<String> databases() {
        return Stream.of("jdbc:h2:mem:test", "jdbc:postgresql://localhost/test");
    }
}
```

### 7.3 Dynamic Tests

**No changes required.** Dynamic tests using `@TestFactory` work identically.

```java
// Java — unchanged
import org.junit.jupiter.api.*;
import java.util.stream.Stream;

class DynamicTestExamples {

    @TestFactory
    Stream<DynamicTest> dynamicTests() {
        return Stream.of("Alice", "Bob", "Carol")
            .map(name -> DynamicTest.dynamicTest(
                "test for " + name,
                () -> assertNotNull(name)
            ));
    }

    @TestFactory
    DynamicContainer dynamicContainerExample() {
        return DynamicContainer.dynamicContainer("grouped tests",
            Stream.of(
                DynamicTest.dynamicTest("first", () -> assertTrue(true)),
                DynamicTest.dynamicTest("second", () -> assertTrue(true))
            )
        );
    }
}
```

```kotlin
// Kotlin — unchanged
import org.junit.jupiter.api.*

class DynamicTestExamples {

    @TestFactory
    fun `dynamic tests`(): Stream<DynamicTest> =
        listOf("Alice", "Bob", "Carol")
            .stream()
            .map { name ->
                DynamicTest.dynamicTest("test for $name") {
                    assertNotNull(name)
                }
            }
}
```

### 7.4 Nested Tests

**No changes required.**

```java
// Java — unchanged
@DisplayName("Order Service")
class OrderServiceTest {

    @Nested
    @DisplayName("when creating an order")
    class CreateOrder {

        @BeforeEach
        void setup() { /* nested setup */ }

        @Test
        void shouldCreateOrderWithValidItems() { }

        @Test
        void shouldRejectEmptyOrder() { }

        @Nested
        @DisplayName("with discount")
        class WithDiscount {

            @Test
            void shouldApplyDiscount() { }
        }
    }

    @Nested
    @DisplayName("when cancelling an order")
    class CancelOrder {

        @Test
        void shouldCancelPendingOrder() { }
    }
}
```

```kotlin
// Kotlin — unchanged
@DisplayName("Order Service")
class OrderServiceTest {

    @Nested
    @DisplayName("when creating an order")
    inner class CreateOrder {

        @Test
        fun `should create order with valid items`() { }

        @Nested
        @DisplayName("with discount applied")
        inner class WithDiscount {

            @Test
            fun `should apply percentage discount`() { }
        }
    }
}
```

> **Kotlin note:** Use `inner class` for `@Nested` classes in Kotlin so they can access the outer class's members. This is unchanged from JUnit 5.

### 7.5 Extensions (@ExtendWith)

**No changes required.** Extension wiring via `@ExtendWith` is identical.

```java
// Java — unchanged
@ExtendWith(MockitoExtension.class)
@ExtendWith(MyCustomExtension.class)
class ServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void testUserLookup() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Alice")));
        var result = userRepository.findById(1L);
        assertTrue(result.isPresent());
    }
}
```

**Multiple extensions (order matters — applied in declaration order):**
```java
@ExtendWith({MockitoExtension.class, TestcontainersExtension.class, AuditLogExtension.class})
class IntegrationTest { ... }
```

```kotlin
// Kotlin — unchanged
@ExtendWith(MockitoExtension::class)
class ServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Test
    fun `should find user by id`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(User(1L, "Alice")))
        val result = userRepository.findById(1L)
        assertTrue(result.isPresent)
    }
}
```

### 7.6 Custom Extensions

Custom JUnit 5 extensions written against the Jupiter Extension API are **fully compatible with JUnit 6**. The extension SPI interfaces have not changed.

```java
// Custom extension — no changes needed for JUnit 6 compatibility
import org.junit.jupiter.api.extension.*;

public class DatabaseCleanupExtension
        implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // context.getDisplayName() now returns @NonNull (JSpecify)
        String testName = context.getDisplayName(); // safe to treat as non-null
        // setup logic
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // cleanup logic
    }
}
```

**Custom extension with Store (unchanged):**
```java
public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Namespace NAMESPACE = Namespace.create(TimingExtension.class);

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getStore(NAMESPACE).put("start", System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        long start = context.getStore(NAMESPACE).remove("start", long.class);
        long duration = System.currentTimeMillis() - start;
        System.out.printf("[%s] took %d ms%n", context.getDisplayName(), duration);
    }
}
```

**Kotlin custom extension — unchanged:**
```kotlin
import org.junit.jupiter.api.extension.*

class AuditExtension : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        val testName = context.displayName  // @NonNull — no null check needed
        // setup
    }

    override fun afterEach(context: ExtensionContext) {
        // cleanup
    }
}
```

### 7.7 Lifecycle Annotations

**No changes required.** All four lifecycle hooks are identical.

One JUnit 6 enhancement: in Kotlin, lifecycle methods **can now be `suspend` functions** (see Section 11.1).

```java
// Java — @BeforeAll still requires static in PER_METHOD lifecycle (default)
@BeforeAll
static void setUpDatabase() {
    // runs once before all test methods
}

// Per-class lifecycle removes the static requirement
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest {
    @BeforeAll
    void setUpDatabase() { // non-static allowed with PER_CLASS
        // runs once before all test methods
    }
}
```

```kotlin
// Kotlin — PER_CLASS is often preferred to avoid companion object boilerplate
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest {

    @BeforeAll
    fun setupOnce() {
        // no companion object needed with PER_CLASS
    }
}
```

### 7.8 Assertions (AssertJ / Hamcrest)

**No changes required** for either library.

```java
// AssertJ — unchanged
import static org.assertj.core.api.Assertions.*;

assertThat(user.getName()).isEqualTo("Alice");
assertThat(list).hasSize(3).contains("item1");
assertThatThrownBy(() -> service.process(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("input must not be null");
```

```java
// Hamcrest — unchanged
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

assertThat(user.getName(), equalTo("Alice"));
assertThat(list, hasSize(3));
assertThat(value, both(greaterThan(0)).and(lessThan(100)));
```

```kotlin
// AssertJ in Kotlin — unchanged
assertThat(user.name).isEqualTo("Alice")
assertThat(list).hasSize(3).contains("item1")
```

### 7.9 Tags and Filtering

**No changes required.** Tag-based filtering in Gradle works identically.

```java
@Test
@Tag("integration")
@Tag("database")
void integrationTest() { }
```

```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        includeTags("unit")
        excludeTags("integration", "slow")
    }
}
```

### 7.10 Test Ordering

**No changes required.** `@TestMethodOrder` is identical.

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderedTests {

    @Test
    @Order(1)
    void firstTest() { }

    @Test
    @Order(2)
    void secondTest() { }
}
```

### 7.11 Parallel Execution

**No changes required.** Configuration via `junit-platform.properties` is identical.

```properties
# src/test/resources/junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

**JUnit 6 enhancement — new thread pool strategy:** JUnit 6 adds a `WorkerThreadPoolHierarchicalTestExecutorService` backed by a regular thread pool instead of a ForkJoinPool, which can improve behavior in certain environments.

```properties
# JUnit 6 — opt into new thread pool implementation
junit.jupiter.execution.parallel.hierarchical.worker.thread-pool.enabled=true
```

---

## 8. Mockito Integration (Java)

Mockito 5.x is compatible with JUnit 6. The `MockitoExtension` uses the JUnit Platform Extension API, which has not changed between JUnit 5 and JUnit 6.

**Dependencies (`build.gradle.kts`):**
```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}
```

**Standard Mockito test — no changes needed:**
```java
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        var request = new PaymentRequest("card-123", 99.99);
        when(paymentGateway.charge(anyString(), anyDouble())).thenReturn(PaymentResult.SUCCESS);

        // When
        var result = paymentService.process(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(auditLogger).logPayment(request);
        verifyNoMoreInteractions(auditLogger);
    }

    @Test
    void shouldThrowOnGatewayFailure() {
        when(paymentGateway.charge(anyString(), anyDouble()))
            .thenThrow(new GatewayException("timeout"));

        assertThatThrownBy(() -> paymentService.process(new PaymentRequest("card-456", 50.0)))
            .isInstanceOf(GatewayException.class)
            .hasMessageContaining("timeout");
    }
}
```

**Static mocking — unchanged:**
```java
@Test
void shouldUseMockedStatic() {
    try (var mocked = mockStatic(UUIDUtil.class)) {
        mocked.when(UUIDUtil::generate).thenReturn("fixed-uuid");
        var result = service.createEntity();
        assertThat(result.getId()).isEqualTo("fixed-uuid");
    }
}
```

**Argument captors — unchanged:**
```java
@Captor
private ArgumentCaptor<EmailRequest> emailCaptor;

@Test
void shouldCaptureEmailArgument() {
    service.registerUser("alice@example.com");
    verify(emailService).send(emailCaptor.capture());
    assertThat(emailCaptor.getValue().getTo()).isEqualTo("alice@example.com");
}
```

---

## 9. MockK Integration (Kotlin)

MockK 1.14+ is required for Kotlin 2.2 compatibility. MockK works with JUnit 6 via standard `@ExtendWith`.

**Dependencies (`build.gradle.kts`):**
```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.mockk:mockk:1.14.0")
}
```

**Standard MockK test — no changes needed:**
```kotlin
import io.mockk.*
import org.junit.jupiter.api.*
import org.assertj.core.api.Assertions.*

class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()
    private val notificationService: NotificationService = mockk(relaxed = true)
    private val orderService = OrderService(orderRepository, notificationService)

    @AfterEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun `should create order and send notification`() {
        // Given
        val order = Order(id = "ORD-001", amount = 150.0)
        every { orderRepository.save(any()) } returns order

        // When
        val result = orderService.createOrder(order)

        // Then
        assertThat(result.id).isEqualTo("ORD-001")
        verify { notificationService.send(any()) }
    }

    @Test
    fun `should throw when repository fails`() {
        every { orderRepository.save(any()) } throws RuntimeException("DB error")

        assertThatThrownBy { orderService.createOrder(Order(id = "ORD-002", amount = 50.0)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("DB error")
    }
}
```

**MockK annotation style with `@ExtendWith(MockKExtension::class)`:**
```kotlin
import io.mockk.impl.annotations.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    lateinit var userRepository: UserRepository

    @InjectMockKs
    lateinit var userService: UserService

    @Test
    fun `should find user by id`() {
        every { userRepository.findById("user-1") } returns User("user-1", "Alice")
        val user = userService.findById("user-1")
        assertThat(user.name).isEqualTo("Alice")
    }
}
```

**Static/object mocking — unchanged:**
```kotlin
@Test
fun `should mock object function`() {
    mockkObject(TimeProvider)
    every { TimeProvider.now() } returns LocalDateTime.of(2026, 1, 1, 12, 0)

    val result = service.getTimestamp()
    assertThat(result.year).isEqualTo(2026)

    unmockkObject(TimeProvider)
}
```

---

## 10. Testcontainers Integration

Testcontainers 2.0.x is fully compatible with JUnit 6. The `@Testcontainers` extension and `@Container` annotation work identically.

> **Important:** In some versions of Testcontainers, the `testcontainers-junit-jupiter` module brings a transitive dependency on JUnit 4 (via the older Testcontainers core). This is harmless but will cause the Vintage engine deprecation warning if Vintage is on the classpath. Either suppress the warning or exclude the JUnit 4 transitive dependency.

**Dependencies (`build.gradle.kts`):**
```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.2")
    testImplementation("org.testcontainers:postgresql:2.0.2") // or your DB module

    // Optional: suppress JUnit 4 transitive from Testcontainers if Vintage is unwanted
    // testImplementation("org.testcontainers:testcontainers:2.0.2") {
    //     exclude(group = "junit", module = "junit")
    // }
}
```

**Standard Testcontainers test — no changes needed:**
```java
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
class UserRepositoryIntegrationTest {

    // Shared container (started once, reused across all tests in the class)
    @Container
    private static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private UserRepository repository;

    @BeforeEach
    void setup() {
        repository = new UserRepository(postgres.getJdbcUrl(), "test", "test");
    }

    @Test
    void shouldPersistAndRetrieveUser() {
        var user = new User("alice@example.com");
        repository.save(user);

        var found = repository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }
}
```

**Per-test container lifecycle:**
```java
@Testcontainers
class IsolatedContainerTest {

    // Instance field — new container per test method
    @Container
    private final GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Test
    void shouldConnectToRedis() {
        assertThat(redis.isRunning()).isTrue();
    }
}
```

**Kotlin Testcontainers — no changes needed:**
```kotlin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.*

@Testcontainers
class UserRepositoryTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
    }

    @Test
    fun `should find user in database`() {
        assertThat(postgres.isRunning).isTrue()
    }
}
```

---

## 11. Kotlin-Specific Migrations

### 11.1 Native Suspend Test Support

This is the **most significant Kotlin-specific improvement in JUnit 6**. Previously, testing coroutine code required wrapping test bodies in `runBlocking`. JUnit 6 natively understands `suspend` on test and lifecycle methods.

**Before (JUnit 5 — with runBlocking):**
```kotlin
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*

class UserApiTest {

    private val userApi = UserApiClient()

    @BeforeEach
    fun setup() = runBlocking {
        userApi.connect()          // ← runBlocking wrapper required
    }

    @Test
    fun `should fetch user`() = runBlocking {
        val user = userApi.getUser("user-1")   // ← runBlocking wrapper required
        assertNotNull(user)
    }

    @AfterEach
    fun teardown() = runBlocking {
        userApi.disconnect()       // ← runBlocking wrapper required
    }
}
```

**After (JUnit 6 — native suspend support):**
```kotlin
import org.junit.jupiter.api.*

class UserApiTest {

    private val userApi = UserApiClient()

    @BeforeEach
    suspend fun setup() {          // ← no runBlocking needed
        userApi.connect()
    }

    @Test
    suspend fun `should fetch user`() {   // ← no runBlocking needed
        val user = userApi.getUser("user-1")
        assertNotNull(user)
    }

    @AfterEach
    suspend fun teardown() {       // ← no runBlocking needed
        userApi.disconnect()
    }
}
```

**Migration rule for Devin:** In any Kotlin test file, if a test method or lifecycle method has the pattern `fun methodName() = runBlocking { ... }` or `fun methodName() { runBlocking { ... } }`, it can be converted to `suspend fun methodName() { ... }` with the `runBlocking` wrapper removed.

> **Important constraint:** Suspend lifecycle methods only work in JUnit 6 when using `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` OR when the suspend method is on a class-level lifecycle function. Per-method `@BeforeEach` suspend works out of the box. Test engines prior to JUnit 6 will not discover suspend test methods.

### 11.2 Kotlin DSL Gradle Config

**Complete Kotlin DSL example for a module with both Java and Kotlin tests:**

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.2.0"
    java
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // JUnit 6 BOM — covers all junit artifacts
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockito for Java tests
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")

    // MockK for Kotlin tests
    testImplementation("io.mockk:mockk:1.14.0")

    // Testcontainers (optional)
    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.2")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}
```

### 11.3 MockK with Coroutines

MockK has first-class support for coroutine mocking via `coEvery`, `coVerify`, and related DSL functions. These are unchanged in JUnit 6.

```kotlin
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()
    private val orderService = OrderService(orderRepository)

    // JUnit 6: can use suspend @Test directly for simple cases
    @Test
    suspend fun `should fetch order asynchronously`() {
        coEvery { orderRepository.findById("ORD-001") } returns Order("ORD-001", 100.0)

        val order = orderService.getOrder("ORD-001")

        assertNotNull(order)
        coVerify { orderRepository.findById("ORD-001") }
    }

    // Use runTest for tests that need TestCoroutineScheduler control
    @Test
    fun `should handle timeout`() = runTest {
        coEvery { orderRepository.findById(any()) } coAnswers {
            delay(10_000)  // simulate slow DB
            Order("ORD-002", 50.0)
        }

        assertThrows<TimeoutException> {
            withTimeout(1_000) { orderService.getOrder("ORD-002") }
        }
    }
}
```

---

## 12. Vert.x Test Integration

If you use `vertx-junit5` for testing Vert.x verticles, the integration continues to work with JUnit 6 because it is built on the Jupiter Extension API.

**Dependencies:**
```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.vertx:vertx-junit5:4.5.x") // check latest compatible version
}
```

**Vert.x test — no changes needed:**
```java
import io.vertx.core.Vertx;
import io.vertx.junit5.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MyVerticleTest {

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MyVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    void shouldHandleRequest(Vertx vertx, VertxTestContext testContext) {
        vertx.createHttpClient()
            .request(HttpMethod.GET, 8080, "localhost", "/api/health")
            .compose(req -> req.send())
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertThat(response.statusCode()).isEqualTo(200);
                testContext.completeNow();
            })));
    }
}
```

**Kotlin Vert.x test — no changes needed:**
```kotlin
import io.vertx.core.Vertx
import io.vertx.junit5.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class MyVerticleTest {

    @BeforeEach
    fun deploy(vertx: Vertx, testContext: VertxTestContext) {
        vertx.deployVerticle(MyVerticle(), testContext.succeeding { testContext.completeNow() })
    }

    @Test
    fun `should respond to health check`(vertx: Vertx, testContext: VertxTestContext) {
        // test body
        testContext.completeNow()
    }
}
```

---

## 13. OpenRewrite Automated Migration

OpenRewrite has a published recipe specifically for JUnit 5 → JUnit 6 migration. For large repos, running this first automates the mechanical changes before Devin handles the non-automatable ones.

> **Recipe:** `org.openrewrite.java.testing.junit6.JUnit5to6Migration`  
> **Library:** `org.openrewrite.recipe:rewrite-testing-frameworks:3.27.0`

### Apply to a Single Module (Groovy DSL)

Add temporarily to `build.gradle`, then remove after running:

```groovy
// Temporary — add to build.gradle, run, then remove
plugins {
    id 'org.openrewrite.rewrite' version 'latest.release'
}

rewrite {
    activeRecipe('org.openrewrite.java.testing.junit6.JUnit5to6Migration')
    setExportDatatables(true)
}

dependencies {
    rewrite('org.openrewrite.recipe:rewrite-testing-frameworks:3.27.0')
}
```

```bash
./gradlew rewriteRun
git diff  # review changes
```

### Apply to a Multi-Module Build (via init.gradle)

This approach avoids touching module build files permanently. Create `init.gradle` at repo root:

```groovy
// init.gradle — place in repo root, delete after migration
initscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2" }
    }
    dependencies {
        classpath("org.openrewrite:plugin:7.26.0")
    }
}

rootProject {
    plugins.apply(org.openrewrite.gradle.RewritePlugin)
    dependencies {
        rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:3.27.0")
    }
    rewrite {
        activeRecipe("org.openrewrite.java.testing.junit6.JUnit5to6Migration")
        setExportDatatables(true)
    }
    afterEvaluate {
        if (repositories.isEmpty()) {
            repositories { mavenCentral() }
        }
    }
}
```

```bash
./gradlew rewriteRun
git diff
```

### What OpenRewrite Handles Automatically

The `JUnit5to6Migration` recipe handles:
- Version bumps in build files (5.x → 6.x)
- Removing `junit-platform-runner` dependency
- Adding `junit-platform-launcher` to `testRuntimeOnly`
- Updating unified versioning (removing separate platform version pins)
- Converting `@RunWith(JUnitPlatform.class)` to `@Suite`
- Removing `junit-jupiter-migrationsupport` dependency

### What OpenRewrite Does NOT Handle (Manual work for Devin)

- Converting `runBlocking` wrappers to `suspend fun` in Kotlin tests
- Fixing `#` delimiter in `@CsvSource` — must be manually reviewed
- Migrating JUnit 4 `@Rule` usages to proper extensions
- Custom extension API changes (JSpecify null annotations)
- Testcontainers module package renames if using older Testcontainers versions
- Business logic or test design improvements

---

## 14. Package-by-Package Migration Workflow (Devin Batch Guide)

This section defines the exact procedure for each batch migration session.

### Step 0: Confirm Context

At the start of each Devin session, confirm:
1. Which package or module is being migrated in this batch
2. Whether OpenRewrite has already been run on this module
3. Current test pass/fail status: `./gradlew :module-name:test` should be green before starting

### Step 1: Update Build File (Once Per Module)

For each module, perform these build file changes exactly once:

```
1. Replace `org.junit:junit-bom:5.x.x` with `org.junit:junit-bom:6.0.3`
2. Remove any line: testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine'
   (it is now included transitively via junit-jupiter)
3. Add: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
4. Remove any dependency on: junit-platform-runner, junit-platform-jfr
5. Remove any force/constraint pins on org.junit.platform:* artifacts
```

### Step 2: Per-File Scan Checklist

For each test file in the target package:

```
[ ] Does the file use @RunWith(JUnitPlatform.class)?
    → Replace with @Suite (Section 6.1)

[ ] Does the file use junit-jupiter-migrationsupport imports?
    → Remove dependency, migrate @Rule to @ExtendWith (Section 6.3)

[ ] Does the file use @CsvSource with '#' as delimiter?
    → Change delimiter to '|' or ',' (Section 6.6)

[ ] Is this a Kotlin file with = runBlocking { } in test/lifecycle methods?
    → Convert to suspend fun (Section 11.1)

[ ] Does the file extend junit.framework.TestCase?
    → Migrate to @ExtendWith based test (JUnit 4 → JUnit 6)

[ ] Does the file use @RunWith(MockitoJUnitRunner.class)?
    → Replace with @ExtendWith(MockitoExtension.class) (Section 8)

[ ] Does the file import org.junit.Test (JUnit 4)?
    → Replace with org.junit.jupiter.api.Test

[ ] Does the file use @Before / @After (JUnit 4)?
    → Replace with @BeforeEach / @AfterEach
```

### Step 3: Verify After Each Package

After migrating each package:

```bash
# Run only the tests in the migrated package
./gradlew :module-name:test --tests "com.example.targetpackage.*"

# If passing, commit and proceed to next package
git add -p
git commit -m "chore: migrate com.example.targetpackage tests to JUnit 6"
```

### Step 4: Module-Level Verification

After all packages in a module are done:

```bash
# Full module test run
./gradlew :module-name:test

# Check for deprecation warnings in test output (Vintage engine)
./gradlew :module-name:test --info 2>&1 | grep -i "vintage\|deprecated\|junit4"
```

### Step 5: Cross-Module Dependency Check

After migrating a module, verify that other modules depending on it still compile and test:

```bash
./gradlew :dependent-module:test
```

---

## 15. Known Issues & Common Pitfalls

### Pitfall 1: Missing junit-platform-launcher in Gradle 9

**Symptom:** `Failed to load JUnit Platform` error when running tests.

**Fix:** Add `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to every module.

---

### Pitfall 2: Version Conflict Between BOM and Explicit Platform Pins

**Symptom:** Dependency resolution warnings about version conflicts on `org.junit.platform:*` artifacts.

**Fix:** Remove all explicit `force` declarations and version overrides for JUnit Platform artifacts. Let the BOM manage everything.

```kotlin
// REMOVE patterns like:
configurations.all {
    resolutionStrategy.force("org.junit.platform:junit-platform-commons:1.11.4")
}
```

---

### Pitfall 3: Kotlin Tests Not Discovered

**Symptom:** Kotlin test classes exist but no tests are run.

**Causes and fixes:**
- Missing `useJUnitPlatform()` in the test task
- Kotlin plugin version below 2.2 — update to Kotlin 2.2+
- Test class is `object` instead of `class` — Jupiter requires class instances

---

### Pitfall 4: Vintage Deprecation Noise in Logs

**Symptom:** INFO messages about Vintage engine being deprecated appearing in CI logs.

**Cause:** Either JUnit 4 tests still exist, or Testcontainers is pulling in JUnit 4 transitively.

**Fix options:**
1. Migrate all JUnit 4 tests (preferred)
2. Suppress by excluding the JUnit 4 transitive dependency from Testcontainers:
```kotlin
testImplementation("org.testcontainers:testcontainers:2.0.2") {
    exclude(group = "junit", module = "junit")
}
```

---

### Pitfall 5: @CsvSource with # Delimiter Fails Silently

**Symptom:** Parameterized tests with `delimiter = '#'` pass zero arguments or throw a parse exception.

**Fix:** Change delimiter to `|` or use comma (default). See Section 6.6.

---

### Pitfall 6: Kotlin Suspend Tests With Per-Method Lifecycle

**Symptom:** `@Test suspend fun` does not run — no test discovered.

**Context:** Native suspend support requires Kotlin 2.2+ AND JUnit 6. Verify both versions. Also check that the Kotlin plugin is `kotlin("jvm")` version 2.2.0+, not just the stdlib.

---

### Pitfall 7: MockK Version Below 1.14 with Kotlin 2.2

**Symptom:** MockK fails to compile or throws `NoSuchMethodError` at runtime.

**Fix:** Upgrade MockK to 1.14.0 or later, which supports Kotlin 2.2's new compiler behavior.

---

### Pitfall 8: Multi-Module — launcher dependency missing on leaf modules

In multi-module Gradle builds, if `testRuntimeOnly` for the launcher is only declared in the root `build.gradle.kts` but a subproject does not inherit it correctly, tests in that subproject will fail. Verify by running `./gradlew :subproject:dependencies --configuration=testRuntimeClasspath | grep launcher` for each module.

---

## 16. Quick-Reference Cheat Sheet

### Dependency Replacements

| Remove (JUnit 5) | Add (JUnit 6) |
|---|---|
| `junit-bom:5.x.x` | `junit-bom:6.0.3` |
| `junit-platform-runner` | `junit-platform-suite-api` + `suite-engine` |
| `junit-platform-jfr` | _(nothing — built into launcher)_ |
| `junit-jupiter-migrationsupport` | _(nothing — migrate @Rule usages)_ |
| `junit-platform-commons:1.x.x` (forced) | _(remove force — BOM handles it)_ |
| _(nothing)_ | `junit-platform-launcher` (testRuntimeOnly) |

### Annotation Changes

| JUnit 5 (unchanged in JUnit 6) | Notes |
|---|---|
| `@Test` | No change |
| `@BeforeEach` / `@AfterEach` | No change |
| `@BeforeAll` / `@AfterAll` | No change |
| `@Disabled` | No change |
| `@ExtendWith` | No change |
| `@Nested` | No change |
| `@Tag` | No change |
| `@ParameterizedTest` | No change |
| `@TestFactory` | No change |
| `@TestMethodOrder` | No change |

### JUnit 4 → JUnit 6 (if still present in codebase)

| JUnit 4 | JUnit 6 |
|---|---|
| `@RunWith(MockitoJUnitRunner.class)` | `@ExtendWith(MockitoExtension.class)` |
| `@RunWith(JUnitPlatform.class)` | `@Suite` |
| `@Before` | `@BeforeEach` |
| `@After` | `@AfterEach` |
| `@BeforeClass` | `@BeforeAll` |
| `@AfterClass` | `@AfterAll` |
| `@Ignore` | `@Disabled` |
| `import org.junit.Test` | `import org.junit.jupiter.api.Test` |
| `@Rule public ExpectedException` | `assertThrows()` |
| `@Rule public TemporaryFolder` | `@TempDir` |

### Kotlin-Specific

| Before (JUnit 5) | After (JUnit 6) |
|---|---|
| `fun test() = runBlocking { ... }` | `suspend fun test() { ... }` |
| `fun setup() = runBlocking { ... }` | `suspend fun setup() { ... }` |
| `fun teardown() = runBlocking { ... }` | `suspend fun teardown() { ... }` |

---

*Last updated: February 2026*  
*JUnit version: 6.0.3 (February 15, 2026)*  
*Gradle version: 9.3.1*  
*Kotlin version: 2.2.0*
