package com.faisal.selenium.pages;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class InventoryPage extends BasePage {
    private final By pageTitle = By.cssSelector("[data-test='title']");
    private final By inventory = By.cssSelector("[data-test='inventory-list']");
    private final By inventoryItem = By.cssSelector("[data-test='inventory-item']");
    private final By productName = By.cssSelector("[data-test='inventory-item-name']");
    private final By productDescription = By.cssSelector("[data-test='inventory-item-desc']");
    private final By productPrice = By.cssSelector("[data-test='inventory-item-price']");
    private final By sortDropdown = By.cssSelector("[data-test='product-sort-container']");
    private final By cartBadge = By.cssSelector("[data-test='shopping-cart-badge']");
    private final By cartLink = By.cssSelector("[data-test='shopping-cart-link']");
    private final By menuButton = By.id("react-burger-menu-btn");
    private final By logoutLink = By.id("logout_sidebar_link");

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForPath("/inventory.html");
        waitForText(pageTitle, "Products");
        waitForVisible(inventory);
    }

    public String getPageTitle() {
        waitUntilLoaded();
        return getText(pageTitle);
    }

    public void addProductToCart(String productId) {
        waitUntilLoaded();
        int previousCount = getCartItemCount();
        click(By.id("add-to-cart-" + productId));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("remove-" + productId)));
        waitForCartItemCount(previousCount + 1);
    }

    public void removeProductFromCart(String productId) {
        waitUntilLoaded();
        int previousCount = getCartItemCount();
        click(By.id("remove-" + productId));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-to-cart-" + productId)));
        waitForCartItemCount(previousCount - 1);
    }

    public int getCartItemCount() {
        waitUntilLoaded();
        return readCartItemCount();
    }

    private int readCartItemCount() {
        List<WebElement> badges = driver.findElements(cartBadge);
        return badges.isEmpty() ? 0 : Integer.parseInt(badges.get(0).getText());
    }

    public void waitForCartItemCount(int expectedCount) {
        if (expectedCount < 0) {
            throw new IllegalArgumentException("Cart count cannot be negative");
        }
        wait.until(currentDriver -> {
            try {
                return readCartItemCount() == expectedCount;
            } catch (StaleElementReferenceException | NumberFormatException e) {
                return false;
            }
        });
    }

    public void openCart() {
        click(cartLink);
        new CartPage(driver).waitUntilLoaded();
    }

    public void logout() {
        click(menuButton);
        click(logoutLink);
        new LoginPage(driver).waitUntilLoaded();
    }

    public void sortProducts(String value) {
        waitUntilLoaded();
        new Select(waitForVisible(sortDropdown)).selectByValue(value);
        wait.until(currentDriver -> {
            try {
                return new Select(currentDriver.findElement(sortDropdown))
                        .getFirstSelectedOption().getAttribute("value").equals(value);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    public List<String> getProductNames() {
        waitUntilLoaded();
        return driver.findElements(productName).stream().map(WebElement::getText).toList();
    }

    public List<BigDecimal> getProductPrices() {
        waitUntilLoaded();
        return driver.findElements(productPrice).stream()
                .map(element -> new BigDecimal(element.getText().replace("$", ""))).toList();
    }

    public Map<String, BigDecimal> getCatalogPrices() {
        waitUntilLoaded();
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (WebElement item : driver.findElements(inventoryItem)) {
            prices.put(item.findElement(productName).getText(),
                    new BigDecimal(item.findElement(productPrice).getText().replace("$", "")));
        }
        return prices;
    }

    public void waitForProductNames(List<String> expectedNames) {
        wait.until(currentDriver -> {
            try {
                return getProductNames().equals(expectedNames);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    public void waitForProductPrices(List<BigDecimal> expectedPrices) {
        wait.until(currentDriver -> {
            try {
                return getProductPrices().equals(expectedPrices);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    public String getProductName(String productId) {
        return productRow(productId).findElement(productName).getText();
    }

    public String getProductDescription(String productId) {
        return productRow(productId).findElement(productDescription).getText();
    }

    public BigDecimal getProductPrice(String productId) {
        return new BigDecimal(productRow(productId).findElement(productPrice).getText().replace("$", ""));
    }

    public void openProductDetails(String productId) {
        WebElement link = productRow(productId).findElement(productName);
        wait.until(ExpectedConditions.elementToBeClickable(link)).click();
        new ProductDetailsPage(driver).waitUntilLoaded();
    }

    private WebElement productRow(String productId) {
        waitUntilLoaded();
        By row = By.xpath("//div[@data-test='inventory-item'][.//button[@id='add-to-cart-"
                + productId + "' or @id='remove-" + productId + "']]");
        return waitForVisible(row);
    }
}
