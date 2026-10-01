# Selenium E2E Automation Portfolio

A Java Selenium/TestNG portfolio project that exercises the public [SauceDemo](https://www.saucedemo.com/) shopping application. It demonstrates Page Object Model design, meaningful positive and negative business checks, cross-browser execution, condition-based synchronization, and failure evidence handling.

## Technology stack

Versions configured in `pom.xml`:

| Component | Version |
| --- | --- |
| Java compiler release | 17 |
| Selenium Java | 4.49.0 |
| TestNG | 7.12.0 |
| ExtentReports / Spark HTML reporter | 5.1.2 |
| Maven Compiler Plugin | 3.16.0 |
| Maven Surefire Plugin | 3.6.0 |

Maven manages dependencies and builds. Surefire 3.6 runs TestNG through the JUnit Platform; its configuration in `pom.xml` registers the listener and disables parallel execution. `testng.xml` contains the corresponding six-class suite and listener for TestNG-compatible IDE/tools; the documented Maven commands use Surefire discovery and configuration.

## Getting started

Install JDK 17 and Maven (3.9+ recommended), and make `java` and `mvn` available on your PATH. Install Chrome and/or Firefox for the browser you intend to run. Internet access is needed for Maven dependency resolution, Selenium Manager driver resolution, and SauceDemo.

From the directory containing `pom.xml`:

```sh
java -version
mvn -version

# Resolve dependencies and compile the test project
mvn -B -ntp test-compile

# Optional: build and install the Maven artifact locally without running tests
mvn -B -ntp -DskipTests install

# Run the complete suite with the default browser (Chrome)
mvn test

# Explicit browser selection; quoting also works in PowerShell
mvn "-Dbrowser=chrome" test
mvn "-Dbrowser=firefox" test

# Run one login test when investigating the environment
mvn "-Dtest=LoginTest#verifyValidLogin" "-Dbrowser=chrome" test
```

No application installation or manually downloaded WebDriver executable is required. Standard `ChromeDriver` and `FirefoxDriver` constructors delegate driver discovery to Selenium Manager.

The framework opens normal browser windows. On Linux without a desktop display, provide a virtual display, for example:

```sh
xvfb-run -a --server-args="-screen 0 1920x1080x24" mvn -B -ntp "-Dbrowser=firefox" test
```

## Configuration

Defaults live in `src/test/resources/config.properties`. JVM system properties override file values, including the browser and timeouts.

| Property | Default | Purpose |
| --- | --- | --- |
| `browser` | `chrome` | Supported values: `chrome`, `firefox` |
| `baseUrl` | `https://www.saucedemo.com/` | Target demo application |
| `explicitWaitSeconds` | `10` | Page/state condition waits |
| `pageLoadTimeoutSeconds` | `30` | Browser navigation timeout |
| `scriptTimeoutSeconds` | `30` | WebDriver script timeout |

Timeouts must be positive integer seconds and are validated before browser launch:

```sh
mvn "-Dbrowser=chrome" "-DexplicitWaitSeconds=15" "-DpageLoadTimeoutSeconds=45" test
```

The `standard_user` / `secret_sauce` values in configuration are **public SauceDemo demo/test credentials**, not private application credentials. Login validation also uses the public `locked_out_user` account and synthetic invalid inputs. Checkout data is clearly synthetic: `Test`, `Shopper`, `TEST-00000`.

Chrome sessions apply only these password-manager preferences:

- `credentials_enable_service=false`
- `profile.password_manager_leak_detection=false`

They suppress password-manager UI and leak-detection prompts associated with the publicly shared demo credentials, including the native “Change your password” warning encountered during local testing. These preferences are Chrome-specific; Firefox receives no Chrome preferences. Browser behavior can change between releases.

## Framework architecture

- **BaseTest:** creates a fresh browser for each browser-based test invocation, validates configuration, applies timeouts, and releases the driver in an always-run teardown with a `finally` block.
- **Page objects:** `LoginPage`, `InventoryPage`, `ProductDetailsPage`, `CartPage`, and `CheckoutPage` encapsulate locators and reusable interactions. `BasePage` provides explicit waits and native WebDriver input/click helpers.
- **Tests:** own business assertions and expected results. TestNG DataProviders cover login validation, four sorting modes, required checkout fields, and cancellation stages without duplicating test classes.
- **Utilities:** `ConfigReader` loads configuration; `ExtentManager` owns the report lifecycle; `TestListener` records test/configuration outcomes and failure evidence.

Execution within each suite is serial, including DataProviders. Driver ownership has not been designed for parallel test execution.

Synchronization waits for destination paths, page readiness, exact field/text values, expected cart counts, removed elements, and actual sorted product lists where appropriate. Empty carts and absent badges are valid zero-item states. There are no fixed sleeps, automatic retries, or JavaScript-click workarounds.

## Implemented coverage

**24 business scenarios plus 3 framework reliability checks = 27 test invocations.** Counts include DataProvider rows, rather than only annotated test methods.

| Test class | Invocations | Checks |
| --- | ---: | --- |
| `HomePageTest` | 1 | Demo home/login page title |
| `LoginTest` | 7 | Valid login; invalid credentials; locked-out login; missing username, password, and both; logout and protected inventory access |
| `InventoryTest` | 7 | Adding products; partial/last-item removal; all four sorting modes with catalog preservation; backpack detail name, description, price, and return navigation |
| `CartTest` | 3 | Exact selected products, prices, and quantities; initially empty cart and last-item removal; remaining-item preservation after removal |
| `CheckoutTest` | 6 | Complete checkout; three required-field validation/correction cases; cancellation from information and overview stages with cart preservation |
| `FrameworkReliabilityTest` | 3 | Cleanup after screenshot failure; cleanup after reporting failure; rejection of invalid timeout values before browser launch |

Checkout verifies the selected backpack (`29.99`) and bike light (`9.99`), quantity one each, subtotal `39.98`, tax `3.20`, and total `43.18`. Assertions use `BigDecimal`, independent fixture prices, and the demo's 8% tax rule with cent rounding. Completion checks the confirmation and cleared cart.

The three framework reliability checks use simulated drivers/results and do not launch real browser sessions.

### Recorded local validation

The complete suite has been validated separately with **27/27 passing on Chrome and 27/27 passing on Firefox** (zero failures and zero skips in each run). Validation was performed on Windows with Java 17 and Maven 3.9.16, using Chrome 154.0.8037.59 and Firefox 157.0. These are recorded results, not a guarantee that future executions will pass.

## Reports and failure evidence

Each execution uses a unique timestamp/UUID directory:

```text
test-output/<run-id>/ExtentReport.html
test-output/<run-id>/screenshots/<class>-<method>-<UUID>.png
target/surefire-reports/
target/testng-reports/
```

Open `ExtentReport.html` in a local browser after execution; the console prints its path when the report is flushed. Keep its `screenshots` subdirectory alongside it so relative evidence links remain valid. Surefire provides XML/text results, and TestNG default listeners provide their reports.

The custom listener starts and finishes the reporting lifecycle, records passes/failures/skips and configuration failures, and labels DataProvider entries with their scenario names. It attempts failure screenshots before teardown, avoids duplicate attempts, and records unavailable evidence. Reporting exceptions are caught and logged; screenshot failures do not prevent the teardown's browser cleanup. A failed driver quit remains a TestNG configuration failure.

Generated reports and build output are excluded by `.gitignore`.

## Project structure

```text
.
├── .github/workflows/selenium.yml
├── .gitignore
├── LICENSE
├── README.md
├── pom.xml
├── testng.xml
└── src/test/
    ├── java/com/faisal/selenium/
    │   ├── base/       # Browser lifecycle
    │   ├── pages/      # BasePage and five page objects
    │   ├── tests/      # Five business test classes and reliability checks
    │   └── utils/      # Configuration, reports, and listener
    └── resources/config.properties
```

## GitHub Actions

The workflow runs on pushes to `main` and pull requests targeting `main`. Separate Chrome and Firefox matrix jobs use Ubuntu 24.04, Temurin Java 17, Maven dependency caching, and the existing `-Dbrowser` override. Xvfb supplies a display without changing browser configuration or test coverage. Selenium Manager remains responsible for driver resolution. The workflow sets [`SE_SKIP_DRIVER_IN_PATH=true`](https://www.selenium.dev/documentation/selenium_manager/#configuration) so runner-provided driver executables are not selected from PATH.

Each job runs the complete suite serially and attempts to upload Surefire, TestNG, and Extent evidence even after a test failure. Download and extract the browser-specific artifact to view reports locally; retain the directory structure for screenshot links. Artifact retention is 14 days.

This workflow has been statically validated but has not yet run on GitHub; the recorded passing results above are local browser results.

## Practices and limitations

The project demonstrates reusable page interactions, immutable page locators, condition-based explicit waits, data-driven negative checks, exact monetary assertions, synthetic checkout data, isolated browser sessions, and resilient failure evidence handling.

SauceDemo is an externally hosted public demo application. Availability, catalog values, tax behavior, and browser changes can affect runs. Coverage focuses on the implemented shopping flows; it does not exercise every demo user or product, all input boundaries, or persistence across separate sessions. There is no API, performance, accessibility, or visual testing in this project.

## License

[ISC](LICENSE) — Copyright (c) 2026 Faisal Iqbal.
