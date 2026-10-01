package com.faisal.selenium.tests;

import com.faisal.selenium.base.BaseTest;
import com.faisal.selenium.pages.CartPage;
import com.faisal.selenium.pages.InventoryPage;
import com.faisal.selenium.pages.LoginPage;
import com.faisal.selenium.pages.ProductDetailsPage;
import com.faisal.selenium.utils.ConfigReader;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class InventoryTest extends BaseTest {
    private InventoryPage loginToInventory() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.get("username"), ConfigReader.get("password"));
        InventoryPage inventoryPage = new InventoryPage(driver);
        inventoryPage.waitUntilLoaded();
        return inventoryPage;
    }

    @Test
    public void verifyMultipleProductsCanBeAddedToCart() {
        InventoryPage inventoryPage = loginToInventory();
        Assert.assertEquals(inventoryPage.getPageTitle(), "Products", "Inventory page was not displayed");
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.addProductToCart("sauce-labs-bike-light");
        Assert.assertEquals(inventoryPage.getCartItemCount(), 2, "Cart should contain two products");
    }

    @Test
    public void verifyInventoryRemovalPreservesOtherItemsAndClearsLastBadge() {
        InventoryPage inventoryPage = loginToInventory();
        Assert.assertEquals(inventoryPage.getCartItemCount(), 0,
                "A new session should have no cart badge/items");
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.addProductToCart("sauce-labs-bike-light");
        inventoryPage.removeProductFromCart("sauce-labs-backpack");
        Assert.assertEquals(inventoryPage.getCartItemCount(), 1,
                "Removing one product must preserve the other");

        inventoryPage.openCart();
        CartPage cartPage = new CartPage(driver);
        Assert.assertEquals(cartPage.getProductNames(), List.of("Sauce Labs Bike Light"),
                "Only the unremoved item should reach the cart");
        Assert.assertFalse(cartPage.isProductDisplayed("Sauce Labs Backpack"));
        cartPage.continueShopping();
        inventoryPage.removeProductFromCart("sauce-labs-bike-light");
        Assert.assertEquals(inventoryPage.getCartItemCount(), 0,
                "Removing the last product should remove the badge");
        inventoryPage.openCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 0, "Inventory removal should empty the cart too");
    }

    @DataProvider(name = "sortModes", parallel = false)
    public Object[][] sortModes() {
        return new Object[][] {
            {"name ascending", "az", false, false},
            {"name descending", "za", false, true},
            {"price low to high", "lohi", true, false},
            {"price high to low", "hilo", true, true}
        };
    }

    @Test(dataProvider = "sortModes")
    public void verifyProductSorting(String scenario, String mode, boolean byPrice, boolean descending) {
        InventoryPage inventoryPage = loginToInventory();
        List<String> originalNames = inventoryPage.getProductNames();
        List<BigDecimal> originalPrices = inventoryPage.getProductPrices();
        Map<String, BigDecimal> originalCatalog = inventoryPage.getCatalogPrices();
        Assert.assertEquals(originalNames.size(), 6, "The demo catalog should contain all six products");
        Assert.assertEquals(originalCatalog.size(), originalNames.size(), "Product names should be unique");

        // A-Z is the initial default: change away first so this case tests a real transition.
        if (mode.equals("az")) {
            inventoryPage.sortProducts("za");
            inventoryPage.waitForProductNames(originalNames.stream().sorted(Comparator.reverseOrder()).toList());
        }

        inventoryPage.sortProducts(mode);
        if (byPrice) {
            Comparator<BigDecimal> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
            List<BigDecimal> expectedPrices = originalPrices.stream().sorted(order).toList();
            inventoryPage.waitForProductPrices(expectedPrices);
            Assert.assertEquals(inventoryPage.getProductPrices(), expectedPrices, scenario + ": wrong price order");
        } else {
            Comparator<String> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
            List<String> expectedNames = originalNames.stream().sorted(order).toList();
            inventoryPage.waitForProductNames(expectedNames);
            Assert.assertEquals(inventoryPage.getProductNames(), expectedNames, scenario + ": wrong name order");
        }
        Assert.assertEquals(inventoryPage.getProductNames().size(), originalNames.size(),
                scenario + ": sorting must not lose or duplicate products");
        Assert.assertEquals(inventoryPage.getCatalogPrices(), originalCatalog,
                scenario + ": sorting must preserve each product's identity and price");
    }

    @Test
    public void verifyProductDetailsMatchInventory() {
        InventoryPage inventoryPage = loginToInventory();
        String productId = "sauce-labs-backpack";
        String name = inventoryPage.getProductName(productId);
        String description = inventoryPage.getProductDescription(productId);
        BigDecimal price = inventoryPage.getProductPrice(productId);
        Assert.assertEquals(name, "Sauce Labs Backpack", "Wrong product selected");
        Assert.assertFalse(description.isBlank(), "Product description must not be empty");
        Assert.assertEquals(price, new BigDecimal("29.99"), "Backpack price should match the demo catalog");

        inventoryPage.openProductDetails(productId);
        ProductDetailsPage detailsPage = new ProductDetailsPage(driver);
        Assert.assertEquals(detailsPage.getProductName(), name, "Detail name differs from inventory");
        Assert.assertEquals(detailsPage.getProductDescription(), description,
                "Detail description differs from inventory");
        Assert.assertEquals(detailsPage.getProductPrice(), price, "Detail price differs from inventory");
        detailsPage.backToProducts();
        Assert.assertEquals(inventoryPage.getProductName(productId), name,
                "Returning from details should preserve the inventory product");
    }
}
