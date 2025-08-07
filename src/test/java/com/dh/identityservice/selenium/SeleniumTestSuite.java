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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SeleniumTestSuite {

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
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testCompleteUserJourney() {
        // Test the complete user journey: Register -> Login -> Home -> Logout
        String uniqueUsername = "selenium" + System.currentTimeMillis();
        String password = "testPassword123";

        // Step 1: Navigate to register page
        driver.get(getBaseUrl() + "/register");
        Assertions.assertEquals("Register - Identity Service", driver.getTitle());

        // Step 2: Fill registration form
        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement firstName = driver.findElement(By.id("firstName"));
        WebElement lastName = driver.findElement(By.id("lastName"));
        WebElement dob = driver.findElement(By.id("dob"));
        WebElement registerBtn = driver.findElement(By.id("registerButton"));

        username.sendKeys(uniqueUsername);
        passwordField.sendKeys(password);
        firstName.sendKeys("Selenium");
        lastName.sendKeys("Test");
        dob.sendKeys("1990-01-01");
        registerBtn.click();

        // Step 3: Should redirect to login page with success message
        wait.until(ExpectedConditions.urlContains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("login"));

        // Step 4: Login with newly created account
        WebElement loginUsername = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement loginPassword = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.id("loginButton"));

        loginUsername.clear();
        loginUsername.sendKeys(uniqueUsername);
        loginPassword.sendKeys(password);
        loginBtn.click();

        // Step 5: Should redirect to home page
        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/home"));
        Assertions.assertEquals(getBaseUrl() + "/home", driver.getCurrentUrl());
        Assertions.assertEquals("Home - Identity Service", driver.getTitle());

        // Step 6: Logout
        WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']")));
        logoutBtn.click();

        // Step 7: Should redirect back to login page
        wait.until(ExpectedConditions.urlContains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("login"));
    }

    @Test
    public void testLoginValidation() {
        driver.get(getBaseUrl() + "/login");

        // Test with existing admin user
        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement password = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.id("loginButton"));

        username.sendKeys("admin");
        password.sendKeys("admin");
        loginBtn.click();

        // Should redirect to home page for valid credentials
        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/home"));
        Assertions.assertEquals(getBaseUrl() + "/home", driver.getCurrentUrl());
    }

    @Test
    public void testInvalidLogin() {
        driver.get(getBaseUrl() + "/login");

        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement password = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.id("loginButton"));

        username.sendKeys("nonexistentuser");
        password.sendKeys("wrongpassword");
        loginBtn.click();

        // Should stay on login page with error
        wait.until(ExpectedConditions.urlContains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("error") ||
                             driver.getPageSource().contains("Invalid username or password"));
    }

    @Test
    public void testPageNavigation() {
        // Test navigation between pages
        driver.get(getBaseUrl() + "/login");

        // Navigate to register page
        WebElement registerLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Register here")));
        registerLink.click();

        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/register"));
        Assertions.assertEquals(getBaseUrl() + "/register", driver.getCurrentUrl());

        // Navigate back to login page
        WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login here")));
        loginLink.click();

        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/login"));
        Assertions.assertEquals(getBaseUrl() + "/login", driver.getCurrentUrl());
    }

    @Test
    public void testFormFieldsPresent() {
        // Test login page elements
        driver.get(getBaseUrl() + "/login");

        WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("loginButton"));

        Assertions.assertTrue(usernameField.isDisplayed());
        Assertions.assertTrue(passwordField.isDisplayed());
        Assertions.assertTrue(loginButton.isDisplayed());

        // Test register page elements
        driver.get(getBaseUrl() + "/register");

        WebElement regUsernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement regPasswordField = driver.findElement(By.id("password"));
        WebElement firstNameField = driver.findElement(By.id("firstName"));
        WebElement lastNameField = driver.findElement(By.id("lastName"));
        WebElement dobField = driver.findElement(By.id("dob"));
        WebElement registerButton = driver.findElement(By.id("registerButton"));

        Assertions.assertTrue(regUsernameField.isDisplayed());
        Assertions.assertTrue(regPasswordField.isDisplayed());
        Assertions.assertTrue(firstNameField.isDisplayed());
        Assertions.assertTrue(lastNameField.isDisplayed());
        Assertions.assertTrue(dobField.isDisplayed());
        Assertions.assertTrue(registerButton.isDisplayed());
    }
}
