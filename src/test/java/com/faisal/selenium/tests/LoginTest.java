package com.faisal.selenium.tests;

import com.faisal.selenium.base.BaseTest;
import com.faisal.selenium.pages.InventoryPage;
import com.faisal.selenium.pages.LoginPage;
import com.faisal.selenium.utils.ConfigReader;
import java.net.URI;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {
    @Test
    public void verifyValidLogin() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.get("username"), ConfigReader.get("password"));

        InventoryPage inventoryPage = new InventoryPage(driver);
        inventoryPage.waitUntilLoaded();
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/inventory.html"),
                "User was not redirected to the inventory page");
        Assert.assertEquals(inventoryPage.getPageTitle(), "Products",
                "Inventory page was not ready after login");
    }

    @DataProvider(name = "loginValidation", parallel = false)
    public Object[][] loginValidation() {
        return new Object[][] {
            {"invalid credentials", "invalid_test_user", "invalid_test_password",
                "Epic sadface: Username and password do not match any user in this service"},
            {"locked-out account", "locked_out_user", ConfigReader.get("password"),
                "Epic sadface: Sorry, this user has been locked out."},
            {"missing username", "", ConfigReader.get("password"), "Epic sadface: Username is required"},
            {"missing password", ConfigReader.get("username"), "", "Epic sadface: Password is required"},
            {"both fields empty", "", "", "Epic sadface: Username is required"}
        };
    }

    @Test(dataProvider = "loginValidation")
    public void verifyLoginValidation(String scenario, String username, String password, String expectedError) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(username, password);

        Assert.assertEquals(loginPage.getErrorMessage(), expectedError, scenario + ": wrong login error");
        Assert.assertEquals(URI.create(driver.getCurrentUrl()).getPath(), "/",
                scenario + ": rejected login must not grant inventory access");
        loginPage.waitUntilLoaded();
    }

    @Test
    public void verifyLogoutRevokesInventoryAccess() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(ConfigReader.get("username"), ConfigReader.get("password"));
        InventoryPage inventoryPage = new InventoryPage(driver);
        inventoryPage.waitUntilLoaded();
        inventoryPage.logout();

        Assert.assertEquals(URI.create(driver.getCurrentUrl()).getPath(), "/",
                "Logout should return to the login page");
        driver.get(URI.create(ConfigReader.get("baseUrl")).resolve("inventory.html").toString());
        loginPage.waitUntilLoaded();
        Assert.assertEquals(URI.create(driver.getCurrentUrl()).getPath(), "/",
                "A logged-out session must not access inventory via its URL");
        Assert.assertEquals(loginPage.getErrorMessage(),
                "Epic sadface: You can only access '/inventory.html' when you are logged in.",
                "Protected-page access should explain why the session was rejected");
    }
}
