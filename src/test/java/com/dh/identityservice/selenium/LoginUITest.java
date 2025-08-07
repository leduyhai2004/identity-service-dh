package com.dh.identityservice.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoginUITest {

    private WebDriver driver;
    private WebDriverWait wait;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/identity";
    }

    @BeforeEach
    public void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode for CI/CD
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testLoginPageLoads() {
        driver.get(getBaseUrl() + "/login");

        // Verify page title
        Assertions.assertEquals("Login - Identity Service", driver.getTitle());

        // Verify form elements are present
        WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("loginButton"));

        Assertions.assertTrue(usernameField.isDisplayed());
        Assertions.assertTrue(passwordField.isDisplayed());
        Assertions.assertTrue(loginButton.isDisplayed());
    }

    @Test
    public void testLoginWithValidCredentials() {
        driver.get(getBaseUrl() + "/login");

        // Fill in the login form
        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement password = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.id("loginButton"));

        username.sendKeys("admin");
        password.sendKeys("admin");
        loginBtn.click();

        // Wait for navigation and verify redirect to home page
        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/home"));
        Assertions.assertEquals(getBaseUrl() + "/home", driver.getCurrentUrl());
    }

    @Test
    public void testLoginWithInvalidCredentials() {
        driver.get(getBaseUrl() + "/login");

        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement password = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.id("loginButton"));

        username.sendKeys("invaliduser");
        password.sendKeys("wrongpassword");
        loginBtn.click();

        // Should redirect back to login page with error
        wait.until(ExpectedConditions.urlContains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("login"));
    }

    @Test
    public void testNavigateToRegisterPage() {
        driver.get(getBaseUrl() + "/login");

        // Click on register link
        WebElement registerLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Register here")));
        registerLink.click();

        // Verify navigation to register page
        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/register"));
        Assertions.assertEquals(getBaseUrl() + "/register", driver.getCurrentUrl());
        Assertions.assertEquals("Register - Identity Service", driver.getTitle());
    }
}
