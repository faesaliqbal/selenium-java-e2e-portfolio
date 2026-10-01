package com.faisal.selenium.tests;

import com.faisal.selenium.base.BaseTest;
import com.faisal.selenium.utils.TestListener;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.internal.TestResult;

public class FrameworkReliabilityTest {
    @Test
    public void verifyBrowserClosesWhenScreenshotFails() {
        AtomicBoolean quitCalled = new AtomicBoolean();
        WebDriver failingScreenshotDriver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                new Class<?>[] {WebDriver.class, TakesScreenshot.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getScreenshotAs")) {
                        throw new WebDriverException("Simulated screenshot failure");
                    }
                    if (method.getName().equals("quit")) {
                        quitCalled.set(true);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        CleanupProbe probe = new CleanupProbe(failingScreenshotDriver);
        TestResult failure = TestResult.newEmptyTestResult();
        failure.setStatus(ITestResult.FAILURE);
        probe.tearDown(failure);

        Assert.assertTrue(quitCalled.get(), "Screenshot failure must not prevent driver.quit()");
        Assert.assertTrue(probe.isDriverReleased(), "Driver ownership must be released after teardown");
        Assert.assertNotNull(failure.getAttribute(BaseTest.SCREENSHOT_ERROR),
                "Screenshot failure should remain available to the reporter");
    }

    @Test
    public void verifyInvalidTimeoutsFailBeforeBrowserLaunch() {
        String key = "explicitWaitSeconds";
        String previousValue = System.getProperty(key);
        try {
            for (String invalid : new String[] {"0", "-1", "not-a-number", ""}) {
                System.setProperty(key, invalid);
                CleanupProbe probe = new CleanupProbe(null);
                Assert.expectThrows(IllegalArgumentException.class, probe::setUp);
                Assert.assertTrue(probe.isDriverReleased(), "Invalid timeouts must not launch a browser");
            }
        } finally {
            if (previousValue == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previousValue);
            }
        }
    }

    @Test
    public void verifyReportingFailureDoesNotPreventCleanup() {
        AtomicBoolean quitCalled = new AtomicBoolean();
        WebDriver session = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[] {WebDriver.class}, (proxy, method, args) -> {
                    if (method.getName().equals("quit")) {
                        quitCalled.set(true);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        ITestResult brokenReportResult = (ITestResult) Proxy.newProxyInstance(
                ITestResult.class.getClassLoader(), new Class<?>[] {ITestResult.class},
                (proxy, method, args) -> {
                    throw new IllegalStateException("Simulated reporting failure");
                });
        new TestListener().onTestFailure(brokenReportResult);
        CleanupProbe probe = new CleanupProbe(session);
        TestResult success = TestResult.newEmptyTestResult();
        success.setStatus(ITestResult.SUCCESS);
        probe.tearDown(success);
        Assert.assertTrue(quitCalled.get(), "Reporting failure must not prevent browser cleanup");
        Assert.assertTrue(probe.isDriverReleased());
    }

    private static final class CleanupProbe extends BaseTest {
        private CleanupProbe(WebDriver session) {
            driver = session;
        }

        private boolean isDriverReleased() {
            return driver == null;
        }
    }
}
