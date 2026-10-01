package com.faisal.selenium.pages;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CheckoutPage extends BasePage {
    private final By firstName = By.id("first-name");
    private final By lastName = By.id("last-name");
    private final By postalCode = By.id("postal-code");
    private final By continueButton = By.id("continue");
    private final By cancelButton = By.id("cancel");
    private final By errorMessage = By.cssSelector("[data-test='error']");
    private final By pageTitle = By.cssSelector("[data-test='title']");
    private final By finishButton = By.id("finish");
    private final By completeHeader = By.cssSelector("[data-test='complete-header']");
    private final By cartItem = By.className("cart_item");
    private final By productName = By.cssSelector("[data-test='inventory-item-name']");
    private final By productPrice = By.cssSelector("[data-test='inventory-item-price']");
    private final By productQuantity = By.cssSelector("[data-test='item-quantity']");
    private final By subtotal = By.cssSelector("[data-test='subtotal-label']");
    private final By tax = By.cssSelector("[data-test='tax-label']");
    private final By total = By.cssSelector("[data-test='total-label']");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public void waitForInformation() {
        waitForPath("/checkout-step-one.html");
        waitForText(pageTitle, "Checkout: Your Information");
        waitForVisible(firstName);
    }

    public void enterCheckoutInformation(String firstNameText, String lastNameText, String postalCodeText) {
        waitForInformation();
        type(firstName, firstNameText);
        type(lastName, lastNameText);
        type(postalCode, postalCodeText);
    }

    public void submitInformation() {
        click(continueButton);
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

    public void clickContinue() {
        submitInformation();
        waitForOverview();
    }

    private void waitForOverview() {
        waitForPath("/checkout-step-two.html");
        waitForText(pageTitle, "Checkout: Overview");
        waitForVisible(finishButton);
    }

    public String getOverviewTitle() {
        return getText(pageTitle);
    }

    public List<String> getProductNames() {
        waitForOverview();
        return driver.findElements(productName).stream().map(WebElement::getText).toList();
    }

    public BigDecimal getProductPrice(String name) {
        return new BigDecimal(productRow(name).findElement(productPrice).getText().replace("$", ""));
    }

    public int getProductQuantity(String name) {
        return Integer.parseInt(productRow(name).findElement(productQuantity).getText());
    }

    private WebElement productRow(String name) {
        waitForOverview();
        return driver.findElements(cartItem).stream()
                .filter(item -> item.findElement(productName).getText().equals(name))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Product not in overview: " + name));
    }

    public BigDecimal getSubtotal() {
        return readAmount(subtotal);
    }

    public BigDecimal getTax() {
        return readAmount(tax);
    }

    public BigDecimal getTotal() {
        return readAmount(total);
    }

    private BigDecimal readAmount(By locator) {
        waitForOverview();
        String text = getText(locator);
        return new BigDecimal(text.substring(text.indexOf('$') + 1));
    }

    public void cancel() {
        String currentPath = URI.create(driver.getCurrentUrl()).getPath();
        if (!currentPath.equals("/checkout-step-one.html") && !currentPath.equals("/checkout-step-two.html")) {
            throw new IllegalStateException("Cancellation is unavailable at " + currentPath);
        }
        click(cancelButton);
        if (currentPath.equals("/checkout-step-one.html")) {
            new CartPage(driver).waitUntilLoaded();
        } else {
            new InventoryPage(driver).waitUntilLoaded();
        }
    }

    public void clickFinish() {
        click(finishButton);
        waitForPath("/checkout-complete.html");
        waitForText(completeHeader, "Thank you for your order!");
    }

    public String getConfirmationMessage() {
        return getText(completeHeader);
    }

    public void backToProducts() {
        click(By.id("back-to-products"));
        new InventoryPage(driver).waitUntilLoaded();
    }
}
