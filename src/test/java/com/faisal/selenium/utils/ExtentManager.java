package com.faisal.selenium.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public final class ExtentManager {
    private static ExtentReports extent;
    private static Path runDirectory;

    private ExtentManager() {
    }

    public static synchronized void startRun() {
        if (extent != null || runDirectory != null) {
            throw new IllegalStateException("A reporting run is already active");
        }
        runDirectory = Path.of("test-output", Instant.now().toString().replace(':', '-')
                + "-" + UUID.randomUUID()).toAbsolutePath();
    }

    public static synchronized Path getRunDirectory() {
        if (runDirectory == null) {
            startRun();
        }
        return runDirectory;
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            Path directory = getRunDirectory();
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create report directory: " + directory, e);
            }
            ExtentSparkReporter reporter = new ExtentSparkReporter(
                    directory.resolve("ExtentReport.html").toString());
            reporter.config().setDocumentTitle("Selenium E2E Automation Report");
            reporter.config().setReportName("SauceDemo Test Execution");
            ExtentReports newExtent = new ExtentReports();
            newExtent.attachReporter(reporter);
            newExtent.setSystemInfo("Project", "Selenium E2E Portfolio");
            newExtent.setSystemInfo("Application", "SauceDemo");
            newExtent.setSystemInfo("Framework", "Selenium + TestNG");
            newExtent.setSystemInfo("Java", System.getProperty("java.version"));
            newExtent.setSystemInfo("Browser", ConfigReader.get("browser"));
            extent = newExtent;
        }
        return extent;
    }

    public static synchronized void finishRun() {
        try {
            if (extent != null) {
                extent.flush();
                System.out.println("Extent report: " + getRunDirectory().resolve("ExtentReport.html"));
            }
        } finally {
            extent = null;
            runDirectory = null;
        }
    }
}
