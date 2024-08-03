package com.syncsage.syncsage.service;

import jakarta.annotation.PreDestroy;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class EmailMonitoringService {

    @Autowired
    private WebDriver driver;

    @Autowired
    private ExtractionService extractionService;

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
        if (driver != null) {
            driver.quit();
        }
    }

    @Scheduled(fixedRate = 60000) // Check every 10 minutes (600000 ms)
    public void monitorBookingEmails() {

        try {
            // Open GMX Mail login page
            driver.get(emailProviderUrl);

            // Explicit wait to ensure the main iframe is loaded
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
            Thread.sleep(5000); // Adjust as necessary, or replace with a more precise wait
            driver.switchTo().defaultContent();

            // Switch to the specific iframe
            WebElement iframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("thirdPartyFrame_mail")));
            driver.switchTo().frame(iframe);

            // Locate unread email elements
            List<WebElement> emails = driver.findElements(By.cssSelector("#mail-list tbody tr.new"));
            for (WebElement email : emails) {
                // Find the name element and check if it matches "giorgos polizoids"
                WebElement nameElement = email.findElement(By.cssSelector(".name"));
                if (nameElement.getText().trim().equalsIgnoreCase("giorgos polizoids")) {
                    // Click the email
                    email.click();
                    Thread.sleep(2000); // Wait for email content to load

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
                    Thread.sleep(2000); // Wait for the email list to load again
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void blockDatesOnPlatform(String listingName, String[] bookingDates) {
        System.out.println("Blocking dates for listing: " + listingName);
        System.out.println("Check-in: " + bookingDates[0]);
        System.out.println("Check-out: " + bookingDates[1]);
    }
}