package com.syncsage.syncsage.service;

import com.syncsage.syncsage.config.SeleniumConfig;
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

import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMonitoringService.class);
    private WebDriver driver;
    private final ExtractionService extractionService;
    private final AirbnbSyncService airbnbSyncService;
    private final SeleniumConfig seleniumConfig;  // Inject the SeleniumConfig

    @Autowired
    public EmailMonitoringService(WebDriver driver, ExtractionService extractionService, AirbnbSyncService airbnbSyncService, SeleniumConfig seleniumConfig) {
        this.driver = driver;
        this.extractionService = extractionService;
        this.airbnbSyncService = airbnbSyncService;
        this.seleniumConfig = seleniumConfig;  // Initialize the SeleniumConfig
    }

    @Value("${gmx.email}")
    private String gmxEmail;

    @Value("${gmx.password}")
    private String gmxPassword;

    @Value("${email.sender}")
    private String emailSender;

    @Value("${email.provider.url}")
    private String emailProviderUrl;

//    @PreDestroy
//    public void tearDown() {
//        if (driver != null) {
//            try {
//                driver.quit();
//            } catch (NoSuchSessionException e) {
//                logger.warn("WebDriver session already terminated", e);
//            } catch (WebDriverException e) {
//                logger.error("Error while closing WebDriver", e);
//            }
//        }
//    }

    private void restartDriver() {
        try {
            if (driver != null) {
                driver.quit(); // Quit the current driver
            }
        } catch (Exception e) {
            logger.error("Error while quitting WebDriver", e);
        } finally {
            // Reinitialize the driver using the existing SeleniumConfig bean
            this.driver = seleniumConfig.webDriver(); // Call the method to create a new driver
            logger.info("WebDriver restarted successfully.");
        }
    }


    @Scheduled(fixedDelay  = 30000) // Check every 1 minute (60000 ms)
    public void monitorBookingEmails() {

        List<Object[]> bookingInfoList = new ArrayList<>();

        if (driver == null) {
            logger.warn("WebDriver is not initialized. Skipping email monitoring.");
            return;
        }

        try {
            // Open GMX Mail login page
            driver.get(emailProviderUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            // Locate and click the login button
            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("login-button")));
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
                        email.click();

                        // Switch to the specific iframe
                        WebElement msgIframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mail-detail")));
                        driver.switchTo().frame(msgIframe);

                        // Extract details from email content
                        WebElement emailContent = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
                        String contentText = emailContent.getText();

                        // Extract listing name, check-in, and check-out dates
                        String listingName = extractionService.extractListingName(contentText);
                        String[] bookingDates = extractionService.extractBookingDates(contentText);

                        // Check if the second date is greater than the current date
                        if (bookingDates != null && bookingDates.length > 1) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            try {
                                LocalDate checkoutDate = LocalDate.parse(bookingDates[0], formatter);
                                LocalDate currentDate = LocalDate.now();

                                if (!checkoutDate.isAfter(currentDate)) {
                                    bookingDates = null;
                                }
                            } catch (DateTimeParseException e) {
                                // Handle the case where the date format is invalid
                                bookingDates = null;
                                logger.error("Invalid date format in booking dates: ", e);
                            }
                        } else {
                            bookingDates = null;
                        }

                        // Block dates on Airbnb or Booking.com using extracted details
                        if (listingName != null && bookingDates != null) {
                            bookingInfoList.add(new Object[]{listingName, bookingDates});
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

            blockDatesOnPlatform(bookingInfoList);

        } catch (TimeoutException e) {
            logger.error("Timeout occurred while waiting for an element", e);
            restartDriver();
        } catch (WebDriverException e) {
            logger.error("WebDriver error occurred", e);

            // Check if the cause of the WebDriverException is a SocketException
            if (e.getCause() instanceof java.net.SocketException) {
                logger.error("Socket connection reset error occurred", e);
            }

            restartDriver();
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }
    }

    private void blockDatesOnPlatform(List<Object[]> bookingInfoList) {
        airbnbSyncService.blockDates(bookingInfoList);
    }

}
