package com.faisal.selenium.utils;

import com.aventstack.extentreports.ExtentTest;
import com.faisal.selenium.base.BaseTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.IConfigurationListener;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener, IInvokedMethodListener,
        IConfigurationListener, IExecutionListener {
    private static final String REPORT_ENTRY = "extentReportEntry";

    @Override
    public void onExecutionStart() {
        safely(ExtentManager::startRun);
    }

    @Override
    public void onExecutionFinish() {
        safely(ExtentManager::finishRun);
    }

    @Override
    public void onTestStart(ITestResult result) {
        safely(() -> entryFor(result));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        // Capture before @AfterMethod quits the browser, including failed setup methods.
        if (result.getStatus() == ITestResult.FAILURE && result.getInstance() instanceof BaseTest baseTest) {
            safely(() -> baseTest.captureFailureScreenshot(result));
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        safely(() -> entryFor(result).pass("Test passed successfully"));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        safely(() -> recordFailure(result));
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        safely(() -> recordFailure(result));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        safely(() -> {
            ExtentTest entry = entryFor(result);
            if (result.getThrowable() == null) {
                entry.skip("Test skipped");
            } else {
                entry.skip(result.getThrowable());
            }
        });
    }

    private ExtentTest entryFor(ITestResult result) {
        Object existing = result.getAttribute(REPORT_ENTRY);
        if (existing instanceof ExtentTest entry) {
            return entry;
        }
        String className = result.getMethod().getRealClass().getSimpleName();
        String name = className + "." + result.getName();
        if (result.getParameters().length > 0) {
            // Data providers use a non-sensitive scenario label as their first parameter.
            name += " [" + result.getParameters()[0] + "]";
        }
        if (!result.getMethod().isTest()) {
            name += " (configuration)";
        }
        ExtentTest entry = ExtentManager.getInstance().createTest(name);
        result.setAttribute(REPORT_ENTRY, entry);
        return entry;
    }

    private void recordFailure(ITestResult result) throws Exception {
        ExtentTest entry = entryFor(result);
        if (result.getThrowable() == null) {
            entry.fail("Execution failed without an exception");
        } else {
            entry.fail(result.getThrowable());
        }
        Object screenshot = result.getAttribute(BaseTest.SCREENSHOT_PATH);
        if (screenshot instanceof Path path && Files.isRegularFile(path)) {
            String relativePath = ExtentManager.getRunDirectory().relativize(path).toString()
                    .replace('\\', '/');
            entry.addScreenCaptureFromPath(relativePath);
        }
        Object error = result.getAttribute(BaseTest.SCREENSHOT_ERROR);
        if (error != null) {
            entry.warning("Failure screenshot unavailable: " + error);
        }
    }

    private void safely(ReportAction action) {
        try {
            action.run();
        } catch (Exception e) {
            // Reporting must not mask the original test/configuration failure or prevent cleanup.
            System.err.println("Reporting failure: " + e);
        }
    }

    @FunctionalInterface
    private interface ReportAction {
        void run() throws Exception;
    }
}
