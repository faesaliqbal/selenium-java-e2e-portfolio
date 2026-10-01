package com.faisal.selenium.pages;

import com.faisal.selenium.utils.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {
    private final By username = By.id("user-name");
    private final By password = By.id("password");
    private final By loginButton = By.id("login-button");
    private final By errorMessage = By.cssSelector("[data-test='error']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(ConfigReader.get("baseUrl"));
        waitUntilLoaded();
    }

    public void waitUntilLoaded() {
        wait.until(ExpectedConditions.titleIs("Swag Labs"));
        waitForVisible(username);
        waitForVisible(password);
        wait.until(ExpectedConditions.elementToBeClickable(loginButton));
    }

    public void login(String user, String pass) {
        type(username, user);
        type(password, pass);
        click(loginButton);
    }

    public String getErrorMessage() {
        return wait.until(currentDriver -> {
            try {
                var errors = currentDriver.findElements(errorMessage);
                if (errors.isEmpty() || !errors.get(0).isDisplayed()) {
                    return null;
                }
                String message = errors.get(0).getText();
                return message.isBlank() ? null : message;
            } catch (StaleElementReferenceException e) {
                return null;
            }
        });
    }
}
