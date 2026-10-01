package com.faisal.selenium.pages;

import com.faisal.selenium.utils.ConfigReader;
import java.net.URI;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, ConfigReader.getTimeout("explicitWaitSeconds"));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        if (driver instanceof FirefoxDriver) {
            // Gecko's element click scrolls off-screen controls into view before clicking.
            element.click();
        } else {
            // Preserve the native pointer sequence used by the Chrome suite.
            new Actions(driver).moveToElement(element).click().perform();
        }
    }
    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        // Keyboard deletion emits input events for controlled forms, including empty values.
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        wait.until(currentDriver -> "".equals(currentDriver.findElement(locator).getDomProperty("value")));
        element.sendKeys(text);
        wait.withMessage("Expected entered value for " + locator)
                .until(ExpectedConditions.refreshed(currentDriver ->
                        text.equals(currentDriver.findElement(locator).getDomProperty("value"))));
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText();
    }

    protected void waitForText(By locator, String text) {
        wait.withMessage("Expected text '" + text + "' at " + locator)
                .until(ExpectedConditions.textToBe(locator, text));
    }

    protected void waitForPath(String path) {
        wait.withMessage("Expected destination path: " + path)
                .until(currentDriver -> path.equals(URI.create(currentDriver.getCurrentUrl()).getPath()));
    }
}