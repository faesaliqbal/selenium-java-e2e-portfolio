package com.faisal.selenium.tests;

import com.faisal.selenium.base.BaseTest;
import com.faisal.selenium.pages.CartPage;
import com.faisal.selenium.pages.CheckoutPage;
import com.faisal.selenium.pages.InventoryPage;
import com.faisal.selenium.pages.LoginPage;
import com.faisal.selenium.utils.ConfigReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CheckoutTest extends BaseTest {
    private static final Map<String, BigDecimal> EXPECTED_PRODUCTS = Map.of(
            "Sauce Labs Backpack", new BigDecimal("29.99"),
            "Sauce Labs Bike Light", new BigDecimal("9.99"));

    private InventoryPage loginToInventory() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.get("username"), ConfigReader.get("password"));
        InventoryPage inventoryPage = new InventoryPage(driver);
        inventoryPage.waitUntilLoaded();
        return inventoryPage;
    }

    private CheckoutPage startCheckout() {
        InventoryPage inventoryPage = loginToInventory();
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.addProductToCart("sauce-labs-bike-light");
        inventoryPage.openCart();
        new CartPage(driver).clickCheckout();
        return new CheckoutPage(driver);
    }

    private void enterSyntheticInformation(CheckoutPage checkoutPage) {
        checkoutPage.enterCheckoutInformation(ConfigReader.get("firstName"),
                ConfigReader.get("lastName"), ConfigReader.get("postalCode"));
    }

    private void assertSelectedCartProducts(CartPage cartPage) {
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should contain two products");
        Assert.assertEquals(cartPage.getProductNames(),
                List.of("Sauce Labs Backpack", "Sauce Labs Bike Light"), "Cart must contain exactly the selected products");
        EXPECTED_PRODUCTS.forEach((name, price) -> {
            Assert.assertEquals(cartPage.getProductPrice(name), price, name + ": wrong cart price");
            Assert.assertEquals(cartPage.getProductQuantity(name), 1, name + ": wrong cart quantity");
        });
    }

    @Test
    public void verifyCompleteCheckoutFlow() {
        InventoryPage inventoryPage = loginToInventory();
        Assert.assertEquals(inventoryPage.getPageTitle(), "Products", "Inventory page was not displayed");
        Assert.assertEquals(inventoryPage.getProductPrice("sauce-labs-backpack"),
                EXPECTED_PRODUCTS.get("Sauce Labs Backpack"));
        Assert.assertEquals(inventoryPage.getProductPrice("sauce-labs-bike-light"),
                EXPECTED_PRODUCTS.get("Sauce Labs Bike Light"));
        inventoryPage.addProductToCart("sauce-labs-backpack");
        inventoryPage.addProductToCart("sauce-labs-bike-light");
        Assert.assertEquals(inventoryPage.getCartItemCount(), 2);

        inventoryPage.openCart();
        CartPage cartPage = new CartPage(driver);
        assertSelectedCartProducts(cartPage);
        cartPage.clickCheckout();
        CheckoutPage checkoutPage = new CheckoutPage(driver);
        enterSyntheticInformation(checkoutPage);
        checkoutPage.clickContinue();
        Assert.assertEquals(checkoutPage.getOverviewTitle(), "Checkout: Overview",
                "Checkout Overview page was not displayed");
        Assert.assertEquals(checkoutPage.getProductNames(),
                List.of("Sauce Labs Backpack", "Sauce Labs Bike Light"),
                "Overview must contain exactly the selected products");
        EXPECTED_PRODUCTS.forEach((name, price) -> {
            Assert.assertEquals(checkoutPage.getProductPrice(name), price, name + ": wrong overview price");
            Assert.assertEquals(checkoutPage.getProductQuantity(name), 1, name + ": wrong overview quantity");
        });

        // Independent fixture prices and the demo's 8% tax rule form the financial oracle.
        BigDecimal expectedSubtotal = EXPECTED_PRODUCTS.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedTax = expectedSubtotal.multiply(new BigDecimal("0.08"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = expectedSubtotal.add(expectedTax);
        Assert.assertEquals(checkoutPage.getSubtotal(), new BigDecimal("39.98"), "Wrong exact subtotal");
        Assert.assertEquals(checkoutPage.getSubtotal(), expectedSubtotal, "Subtotal must equal selected line prices");
        Assert.assertEquals(checkoutPage.getTax(), new BigDecimal("3.20"), "Wrong exact tax");
        Assert.assertEquals(checkoutPage.getTax(), expectedTax, "Tax must use the demo rate and cent rounding");
        Assert.assertEquals(checkoutPage.getTotal(), new BigDecimal("43.18"), "Wrong exact total");
        Assert.assertEquals(checkoutPage.getTotal(), expectedTotal, "Total must equal subtotal plus tax");

        checkoutPage.clickFinish();
        Assert.assertEquals(checkoutPage.getConfirmationMessage(), "Thank you for your order!",
                "Order confirmation message was incorrect");
        checkoutPage.backToProducts();
        Assert.assertEquals(inventoryPage.getCartItemCount(), 0, "Completed order should clear the cart badge");
        inventoryPage.openCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 0, "Completed order should clear the cart contents");
    }

    @DataProvider(name = "requiredCheckoutFields", parallel = false)
    public Object[][] requiredCheckoutFields() {
        return new Object[][] {
            {"missing first name", "", "Shopper", "TEST-00000", "Error: First Name is required"},
            {"missing last name", "Test", "", "TEST-00000", "Error: Last Name is required"},
            {"missing postal code", "Test", "Shopper", "", "Error: Postal Code is required"}
        };
    }

    @Test(dataProvider = "requiredCheckoutFields")
    public void verifyRequiredCheckoutFieldValidation(String scenario, String firstName, String lastName,
            String postalCode, String expectedError) {
        CheckoutPage checkoutPage = startCheckout();
        checkoutPage.enterCheckoutInformation(firstName, lastName, postalCode);
        checkoutPage.submitInformation();

        Assert.assertEquals(checkoutPage.getErrorMessage(), expectedError, scenario + ": wrong validation message");
        Assert.assertEquals(URI.create(driver.getCurrentUrl()).getPath(), "/checkout-step-one.html",
                scenario + ": invalid information must not advance to overview");
        checkoutPage.waitForInformation();
        // Correct the input in the same session to ensure validation does not trap the user.
        enterSyntheticInformation(checkoutPage);
        checkoutPage.clickContinue();
        Assert.assertEquals(checkoutPage.getOverviewTitle(), "Checkout: Overview",
                scenario + ": corrected information should allow checkout to continue");
        Assert.assertEquals(checkoutPage.getProductNames(),
                List.of("Sauce Labs Backpack", "Sauce Labs Bike Light"),
                scenario + ": validation must preserve the selected products");
    }

    @DataProvider(name = "checkoutCancellation", parallel = false)
    public Object[][] checkoutCancellation() {
        return new Object[][] {{"information stage"}, {"overview stage"}};
    }

    @Test(dataProvider = "checkoutCancellation")
    public void verifyCheckoutCancellationPreservesCart(String stage) {
        CheckoutPage checkoutPage = startCheckout();
        enterSyntheticInformation(checkoutPage);
        if (stage.equals("overview stage")) {
            checkoutPage.clickContinue();
        }
        checkoutPage.cancel();

        if (stage.equals("information stage")) {
            Assert.assertEquals(URI.create(driver.getCurrentUrl()).getPath(), "/cart.html",
                    "Cancelling checkout information should return to the cart");
        } else {
            Assert.assertEquals(URI.create(driver.getCurrentUrl()).getPath(), "/inventory.html",
                    "Cancelling overview should return to inventory");
            InventoryPage inventoryPage = new InventoryPage(driver);
            Assert.assertEquals(inventoryPage.getCartItemCount(), 2, "Cancellation must preserve the cart badge");
            inventoryPage.openCart();
        }
        assertSelectedCartProducts(new CartPage(driver));
    }
}
