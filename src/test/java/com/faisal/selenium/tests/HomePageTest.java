package com.faisal.selenium.tests;

import com.faisal.selenium.base.BaseTest;
import com.faisal.selenium.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HomePageTest extends BaseTest {
    @Test
    public void verifyHomePageLoads() {
        new LoginPage(driver).open();
        Assert.assertEquals(driver.getTitle(), "Swag Labs", "Home page title is incorrect");
    }
}
