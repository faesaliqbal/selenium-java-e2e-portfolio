package com.faisal.selenium.pages;

import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CartPage extends BasePage {
    private final By pageTitle = By.cssSelector("[data-test='title']");
    private final By cartList = By.cssSelector("[data-test='cart-list']");
    private final By cartItem = By.className("cart_item");
    private final By productName = By.cssSelector("[data-test='inventory-item-name']");
    private final By productPrice = By.cssSelector("[data-test='inventory-item-price']");
    private final By productQuantity = By.cssSelector("[data-test='item-quantity']");
    private final By checkoutButton = By.id("checkout");
    private final By continueShoppingButton = By.id("continue-shopping");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForPath("/cart.html");
        waitForText(pageTitle, "Your Cart");
        waitForVisible(cartList);
    }

    public int getCartItemCount() {
        waitUntilLoaded();
        return driver.findElements(cartItem).size();
    }

    public List<String> getProductNames() {
        waitUntilLoaded();
        return driver.findElements(productName).stream().map(WebElement::getText).toList();
    }

    public BigDecimal getProductPrice(String name) {
        return new BigDecimal(productRow(name).findElement(productPrice).getText().replace("$", ""));
    }

    public int getProductQuantity(String name) {
        return Integer.parseInt(productRow(name).findElement(productQuantity).getText());
    }

    private WebElement productRow(String name) {
        waitUntilLoaded();
        return driver.findElements(cartItem).stream()
                .filter(item -> item.findElement(productName).getText().equals(name))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Product not in cart: " + name));
    }

    public void waitForCartItemCount(int expectedCount) {
        if (expectedCount < 0) {
            throw new IllegalArgumentException("Cart count cannot be negative");
        }
        waitUntilLoaded();
        wait.until(ExpectedConditions.numberOfElementsToBe(cartItem, expectedCount));
    }

    public boolean isProductDisplayed(String productNameText) {
        waitUntilLoaded();
        return driver.findElements(productName).stream()
                .anyMatch(element -> element.isDisplayed() && element.getText().equals(productNameText));
    }

    public void removeProduct(String productId) {
        int previousCount = getCartItemCount();
        By removeButton = By.id("remove-" + productId);
        click(removeButton);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(removeButton));
        waitForCartItemCount(previousCount - 1);
    }

    public void continueShopping() {
        click(continueShoppingButton);
        new InventoryPage(driver).waitUntilLoaded();
    }

    public void clickCheckout() {
        click(checkoutButton);
        new CheckoutPage(driver).waitForInformation();
    }
}
