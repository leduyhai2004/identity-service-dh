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
public class RegisterUITest {

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
    public void testRegisterPageLoads() {
        driver.get(getBaseUrl() + "/register");

        // Verify page title
        Assertions.assertEquals("Register - Identity Service", driver.getTitle());

        // Verify form elements are present
        WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement firstNameField = driver.findElement(By.id("firstName"));
        WebElement lastNameField = driver.findElement(By.id("lastName"));
        WebElement dobField = driver.findElement(By.id("dob"));
        WebElement registerButton = driver.findElement(By.id("registerButton"));

        Assertions.assertTrue(usernameField.isDisplayed());
        Assertions.assertTrue(passwordField.isDisplayed());
        Assertions.assertTrue(firstNameField.isDisplayed());
        Assertions.assertTrue(lastNameField.isDisplayed());
        Assertions.assertTrue(dobField.isDisplayed());
        Assertions.assertTrue(registerButton.isDisplayed());
    }

    @Test
    public void testRegisterWithValidData() {
        driver.get(getBaseUrl() + "/register");

        // Generate unique username to avoid conflicts
        String uniqueUsername = "testuser" + System.currentTimeMillis();

        // Fill in the registration form
        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement password = driver.findElement(By.id("password"));
        WebElement firstName = driver.findElement(By.id("firstName"));
        WebElement lastName = driver.findElement(By.id("lastName"));
        WebElement dob = driver.findElement(By.id("dob"));
        WebElement registerBtn = driver.findElement(By.id("registerButton"));

        username.sendKeys(uniqueUsername);
        password.sendKeys("password123");
        firstName.sendKeys("John");
        lastName.sendKeys("Doe");
        dob.sendKeys("1990-01-01");
        registerBtn.click();

        // Should redirect to login page with success message
        wait.until(ExpectedConditions.urlContains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("login"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("message"));
    }

    @Test
    public void testRegisterWithExistingUsername() {
        driver.get(getBaseUrl() + "/register");

        // Try to register with admin username (assuming it exists)
        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement password = driver.findElement(By.id("password"));
        WebElement firstName = driver.findElement(By.id("firstName"));
        WebElement lastName = driver.findElement(By.id("lastName"));
        WebElement dob = driver.findElement(By.id("dob"));
        WebElement registerBtn = driver.findElement(By.id("registerButton"));

        username.sendKeys("admin");
        password.sendKeys("password123");
        firstName.sendKeys("John");
        lastName.sendKeys("Doe");
        dob.sendKeys("1990-01-01");
        registerBtn.click();

        // Should redirect back to register page with error
        wait.until(ExpectedConditions.urlContains("register"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("register"));
        Assertions.assertTrue(driver.getCurrentUrl().contains("error"));
    }

    @Test
    public void testNavigateToLoginPage() {
        driver.get(getBaseUrl() + "/register");

        // Click on login link
        WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Login here")));
        loginLink.click();

        // Verify navigation to login page
        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/login"));
        Assertions.assertEquals(getBaseUrl() + "/login", driver.getCurrentUrl());
        Assertions.assertEquals("Login - Identity Service", driver.getTitle());
    }

    @Test
    public void testRegisterFormValidation() {
        driver.get(getBaseUrl() + "/register");

        // Try to submit empty form
        WebElement registerBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("registerButton")));
        registerBtn.click();

        // Check if browser validation prevents submission (required fields)
        WebElement usernameField = driver.findElement(By.id("username"));
        Assertions.assertTrue(usernameField.getAttribute("validationMessage") != null ||
                             usernameField.getAttribute("required") != null);
    }

    @Test
    public void testCompleteUserFlow() {
        // Test complete flow: Register -> Login -> Home
        String uniqueUsername = "flowtest" + System.currentTimeMillis();
        String password = "testpass123";

        // Step 1: Register
        driver.get(getBaseUrl() + "/register");

        WebElement username = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement firstName = driver.findElement(By.id("firstName"));
        WebElement lastName = driver.findElement(By.id("lastName"));
        WebElement dob = driver.findElement(By.id("dob"));
        WebElement registerBtn = driver.findElement(By.id("registerButton"));

        username.sendKeys(uniqueUsername);
        passwordField.sendKeys(password);
        firstName.sendKeys("Test");
        lastName.sendKeys("User");
        dob.sendKeys("1995-05-15");
        registerBtn.click();

        // Step 2: Should be redirected to login page
        wait.until(ExpectedConditions.urlContains("login"));

        // Step 3: Login with the newly created account
        WebElement loginUsername = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement loginPassword = driver.findElement(By.id("password"));
        WebElement loginBtn = driver.findElement(By.id("loginButton"));

        loginUsername.sendKeys(uniqueUsername);
        loginPassword.sendKeys(password);
        loginBtn.click();

        // Step 4: Should be redirected to home page
        wait.until(ExpectedConditions.urlToBe(getBaseUrl() + "/home"));
        Assertions.assertEquals(getBaseUrl() + "/home", driver.getCurrentUrl());
        Assertions.assertEquals("Home - Identity Service", driver.getTitle());
    }
}
