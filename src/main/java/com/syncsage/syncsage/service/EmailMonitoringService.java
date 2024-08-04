package com.syncsage.syncsage.service;

import jakarta.annotation.PreDestroy;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class EmailMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMonitoringService.class);
    private final WebDriver driver;
    private final ExtractionService extractionService;
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    public EmailMonitoringService(WebDriver driver, ExtractionService extractionService) {
        this.driver = driver;
        this.extractionService = extractionService;
    }

    @Value("${gmx.email}")
    private String gmxEmail;

    @Value("${gmx.password}")
    private String gmxPassword;

    @Value("${email.sender}")
    private String emailSender;

    @Value("${email.provider.url}")
    private String emailProviderUrl;

    @PreDestroy
    public void tearDown() {
        lock.lock();
        try {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (NoSuchSessionException e) {
                    logger.warn("WebDriver session already terminated", e);
                } catch (WebDriverException e) {
                    logger.error("Error while closing WebDriver", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedRate = 60000) // Check every 1 minute (60000 ms)
    public void monitorBookingEmails() {
        if (driver == null) {
            logger.warn("WebDriver is not initialized. Skipping email monitoring.");
            return;
        }

        lock.lock();
        try {
            // Open GMX Mail login page
            driver.get(emailProviderUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Locate and click the login button
            WebElement loginButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-button")));
            loginButton.click();

            // Find the email input field and enter the email address
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));
            usernameField.sendKeys(gmxEmail);

            // Find the password input field and enter the password
            WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-password")));
            passwordField.sendKeys(gmxPassword);

            // Find the login button and click to submit the login form
            WebElement loginSubmitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn.btn-block.login-submit")));
            loginSubmitButton.click();

            // Wait for inbox to load after login
            Thread.sleep(4000);
            driver.switchTo().defaultContent();

            // Switch to the specific iframe
            WebElement iframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("thirdPartyFrame_mail")));
            driver.switchTo().frame(iframe);

            // Locate unread email elements
            List<WebElement> emails = driver.findElements(By.cssSelector("#mail-list tbody tr.new"));
            for (WebElement email : emails) {
                try {
                    WebElement nameElement = email.findElement(By.cssSelector(".name"));
                    if (nameElement.getText().trim().equalsIgnoreCase(emailSender)) {
                        // Click the email
                        email.click();
                        // Wait for email content to load
                        Thread.sleep(2000);

                        // Switch to the specific iframe
                        WebElement msgIframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mail-detail")));
                        driver.switchTo().frame(msgIframe);

                        // Extract details from email content
                        WebElement emailContent = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
                        String contentText = emailContent.getText();

                        // Extract listing name, check-in, and check-out dates
                        String listingName = extractionService.extractListingName(contentText);
                        String[] bookingDates = extractionService.extractBookingDates(contentText);

                        // Block dates on Airbnb or Booking.com using extracted details
                        if (listingName != null && bookingDates != null) {
                            blockDatesOnPlatform(listingName, bookingDates);
                        }

                        // Go back to the email list
                        driver.switchTo().defaultContent();
                        driver.switchTo().frame(iframe);
                    }
                } catch (NoSuchElementException e) {
                    logger.warn("Element not found during email processing", e);
                } catch (Exception e) {
                    logger.error("Error while processing email", e);
                }
            }
        } catch (TimeoutException e) {
            logger.error("Timeout occurred while waiting for an element", e);
        } catch (WebDriverException e) {
            logger.error("WebDriver error occurred", e);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        } finally {
            lock.unlock();
        }
    }

    private void blockDatesOnPlatform(String listingName, String[] bookingDates) {
        logger.info("Blocking dates for listing: " + listingName);
        logger.info("Check-in: " + bookingDates[0]);
        logger.info("Check-out: " + bookingDates[1]);
    }
}
