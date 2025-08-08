package com.dh.identityservice.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoginPageTest {
    private WebDriver driver;

    @BeforeEach
    void setUp() {

        WebDriverManager.chromedriver().clearDriverCache().setup();
        driver = new ChromeDriver();
    }

    @Test
    void testLoginSuccess() {
        driver.get("http://localhost:8080/identity/login");

        // Fill username
        WebElement usernameInput = driver.findElement(By.id("username"));
        usernameInput.sendKeys("admin");

        // Fill password
        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys("admin");

        // Click login
        WebElement loginButton = driver.findElement(By.id("loginButton"));
        loginButton.click();

        // Wait for redirect or message
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/home"));

        assertTrue(driver.getCurrentUrl().contains("/home"));
    }

    @Test
    void testLoginFailure() {
        driver.get("http://localhost:8080/identity/login");

        driver.findElement(By.id("username")).sendKeys("wronguser");
        driver.findElement(By.id("password")).sendKeys("wrongpass");
        driver.findElement(By.id("loginButton")).click();

        WebElement errorMessage = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.className("error")));

        assertEquals("Invalid username or password", errorMessage.getText());
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
