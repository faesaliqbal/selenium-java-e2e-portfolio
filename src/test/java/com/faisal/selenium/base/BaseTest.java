package com.faisal.selenium.base;

import com.faisal.selenium.utils.ConfigReader;
import com.faisal.selenium.utils.ExtentManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    public static final String SCREENSHOT_PATH = "failureScreenshotPath";
    public static final String SCREENSHOT_ERROR = "failureScreenshotError";
    private static final String SCREENSHOT_ATTEMPTED = "failureScreenshotAttempted";

    protected WebDriver driver;

    @BeforeMethod
    public void setUp() {
        if (driver != null) {
            throw new IllegalStateException("Previous browser session was not cleaned up");
        }
        // Validate configuration before launching a browser.
        var pageLoadTimeout = ConfigReader.getTimeout("pageLoadTimeoutSeconds");
        var scriptTimeout = ConfigReader.getTimeout("scriptTimeoutSeconds");
        ConfigReader.getTimeout("explicitWaitSeconds");
        String browser = ConfigReader.get("browser").trim().toLowerCase(Locale.ROOT);
        // Default driver constructors delegate driver discovery to Selenium Manager.
        driver = switch (browser) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                // Public demo credentials must not trigger Chrome's native password-manager UI.
                options.setExperimentalOption("prefs", Map.of(
                        "credentials_enable_service", false,
                        "profile.password_manager_leak_detection", false));
                yield new ChromeDriver(options);
            }
            case "firefox" -> new FirefoxDriver();
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser
                    + ". Supported values: chrome, firefox. Select with -Dbrowser=<name>.");
        };
        // A setup failure is captured by the listener, then alwaysRun teardown releases the session.
        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        driver.manage().timeouts().scriptTimeout(scriptTimeout);
        if (browser.equals("chrome") && "true".equals(System.getenv("GITHUB_ACTIONS"))) {
            // Chrome's maximize command can fail under the CI runner's Linux/Xvfb display.
            driver.manage().window().setSize(new Dimension(1920, 1080));
        } else {
            driver.manage().window().maximize();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                captureFailureScreenshot(result);
            }
        } finally {
            WebDriver session = driver;
            driver = null;
            if (session != null) {
                // Let TestNG report a quit failure as a configuration failure.
                session.quit();
            }
        }
    }

    public final void captureFailureScreenshot(ITestResult result) {
        if (result.getAttribute(SCREENSHOT_ATTEMPTED) != null || driver == null) {
            return;
        }
        result.setAttribute(SCREENSHOT_ATTEMPTED, true);
        try {
            if (!(driver instanceof TakesScreenshot screenshotDriver)) {
                throw new IllegalStateException("Driver does not support screenshots");
            }
            byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
            Path directory = ExtentManager.getRunDirectory().resolve("screenshots");
            Files.createDirectories(directory);
            String label = (getClass().getSimpleName() + "-" + result.getName())
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destination = directory.resolve(label + "-" + UUID.randomUUID() + ".png");
            Files.write(destination, screenshot, StandardOpenOption.CREATE_NEW);
            result.setAttribute(SCREENSHOT_PATH, destination);
            System.out.println("Failure screenshot: " + destination);
        } catch (IOException | RuntimeException e) {
            result.setAttribute(SCREENSHOT_ERROR, e.toString());
            System.err.println("Unable to capture failure screenshot: " + e);
        }
    }
}
