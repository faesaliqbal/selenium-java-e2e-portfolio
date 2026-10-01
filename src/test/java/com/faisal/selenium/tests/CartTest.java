package com.faisal.selenium.tests;

import com.faisal.selenium.base.BaseTest;
import com.faisal.selenium.pages.CartPage;
import com.faisal.selenium.pages.InventoryPage;
import com.faisal.selenium.pages.LoginPage;
import com.faisal.selenium.utils.ConfigReader;
import java.math.BigDecimal;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CartTest extends BaseTest {
    private InventoryPage loginToInventory() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.get("username"), ConfigReader.get("password"));
        InventoryPage inventoryPage = new InventoryPage(driver);
        inventoryPage.waitUntilLoaded();
        return inventoryPage;
    }

    @Test
    public void verifyProductsAddedToCart() {
        InventoryPage inventoryPage = loginToInventory();
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.addProductToCart("sauce-labs-bike-light");
        inventoryPage.openCart();
        CartPage cartPage = new CartPage(driver);

        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should contain two products");
        Assert.assertEquals(cartPage.getProductNames(),
                List.of("Sauce Labs Backpack", "Sauce Labs Bike Light"), "Cart should contain exactly the selected products");
        Assert.assertEquals(cartPage.getProductPrice("Sauce Labs Backpack"), new BigDecimal("29.99"));
        Assert.assertEquals(cartPage.getProductPrice("Sauce Labs Bike Light"), new BigDecimal("9.99"));
        Assert.assertEquals(cartPage.getProductQuantity("Sauce Labs Backpack"), 1);
        Assert.assertEquals(cartPage.getProductQuantity("Sauce Labs Bike Light"), 1);
    }

    @Test
    public void verifyEmptyCartAndLastProductRemoval() {
        InventoryPage inventoryPage = loginToInventory();
        inventoryPage.openCart();
        CartPage cartPage = new CartPage(driver);
        Assert.assertEquals(cartPage.getCartItemCount(), 0, "A new cart should be empty");
        Assert.assertFalse(cartPage.isProductDisplayed("Sauce Labs Backpack"),
                "Product lookup should return false for an empty cart");

        cartPage.continueShopping();
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.openCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 1);
        cartPage.removeProduct("sauce-labs-backpack");
        Assert.assertEquals(cartPage.getCartItemCount(), 0, "Removing the last product should leave zero items");
        Assert.assertFalse(cartPage.isProductDisplayed("Sauce Labs Backpack"),
                "Removed product should no longer be displayed");
        cartPage.continueShopping();
        Assert.assertEquals(inventoryPage.getCartItemCount(), 0,
                "Cart removal must also clear the inventory badge");
    }

    @Test
    public void verifyCartRemovalPreservesRemainingProduct() {
        InventoryPage inventoryPage = loginToInventory();
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.addProductToCart("sauce-labs-bike-light");
        inventoryPage.openCart();
        CartPage cartPage = new CartPage(driver);
        cartPage.removeProduct("sauce-labs-backpack");

        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Only one item should be removed");
        Assert.assertEquals(cartPage.getProductNames(), List.of("Sauce Labs Bike Light"));
        Assert.assertFalse(cartPage.isProductDisplayed("Sauce Labs Backpack"));
        Assert.assertEquals(cartPage.getProductPrice("Sauce Labs Bike Light"), new BigDecimal("9.99"),
                "Removing another item must not alter the remaining price");
        Assert.assertEquals(cartPage.getProductQuantity("Sauce Labs Bike Light"), 1);
        cartPage.continueShopping();
        Assert.assertEquals(inventoryPage.getCartItemCount(), 1,
                "The inventory badge should agree with cart contents");
    }
}
