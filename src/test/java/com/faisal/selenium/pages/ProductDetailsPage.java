package com.faisal.selenium.pages;

import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProductDetailsPage extends BasePage {
    private final By productName = By.cssSelector("[data-test='inventory-item-name']");
    private final By productDescription = By.cssSelector("[data-test='inventory-item-desc']");
    private final By productPrice = By.cssSelector("[data-test='inventory-item-price']");
    private final By backButton = By.id("back-to-products");

    public ProductDetailsPage(WebDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForPath("/inventory-item.html");
        waitForVisible(productName);
        waitForVisible(productDescription);
        waitForVisible(productPrice);
    }

    public String getProductName() {
        return getText(productName);
    }

    public String getProductDescription() {
        return getText(productDescription);
    }

    public BigDecimal getProductPrice() {
        return new BigDecimal(getText(productPrice).replace("$", ""));
    }

    public void backToProducts() {
        click(backButton);
        new InventoryPage(driver).waitUntilLoaded();
    }
}
