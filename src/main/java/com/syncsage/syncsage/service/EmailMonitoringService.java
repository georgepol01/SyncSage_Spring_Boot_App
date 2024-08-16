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

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMonitoringService.class);
    private final WebDriver driver;
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

//    private void restartDriver() {
//        try {
//            if (driver != null) {
//                driver.quit(); // Quit the current driver
//            }
//        } catch (Exception e) {
//            logger.error("Error while quitting WebDriver", e);
//        } finally {
//            // Reinitialize the driver using the existing SeleniumConfig bean
//            this.driver = seleniumConfig.webDriver(); // Call the method to create a new driver
//            logger.info("WebDriver restarted successfully.");
//        }
//    }


    @Scheduled(fixedDelay = 30000)
    public void monitorBookingEmails() {
        List<Object[]> bookingInfoList = new ArrayList<>();

        if (driver == null) {
            logger.warn("WebDriver is not initialized. Skipping email monitoring.");
            return;
        }

        try {
            driver.get(emailProviderUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

            loginToEmail(wait);

            driver.switchTo().defaultContent();
            WebElement iframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("thirdPartyFrame_mail")));
            driver.switchTo().frame(iframe);

            List<WebElement> emails = driver.findElements(By.cssSelector("#mail-list tbody tr.new"));
            Thread.sleep(2000);

            for (WebElement email : emails) {
                if (processEmail(wait, email, bookingInfoList)) {
                    // After processing each email, refresh the email list
                    driver.switchTo().defaultContent();
                    iframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("thirdPartyFrame_mail")));
                    driver.switchTo().frame(iframe);
                }
            }
            processBookingInfoList(bookingInfoList);

        } catch (TimeoutException e) {
            logger.error("Timeout occurred while waiting for an element", e);
        } catch (WebDriverException e) {
            logger.error("WebDriver error occurred", e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }
    }

    private void loginToEmail(WebDriverWait wait) {
        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("login-button")));
        loginButton.click();

        WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));
        usernameField.sendKeys(gmxEmail);

        WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-password")));
        passwordField.sendKeys(gmxPassword);

        WebElement loginSubmitButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn.btn-block.login-submit")));
        loginSubmitButton.click();

        logger.info("Logged in to GMX Mail successfully.");
    }

    private boolean processEmail(WebDriverWait wait, WebElement email, List<Object[]> bookingInfoList) {
        try {
            WebElement nameElement = email.findElement(By.cssSelector(".name"));
            if (nameElement.getText().trim().equalsIgnoreCase(emailSender)) {
                email.click();
                Thread.sleep(2000);

                // Switch to email content iframe
                WebElement msgIframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mail-detail")));
                driver.switchTo().frame(msgIframe);

                WebElement emailContent = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
                String contentText = emailContent.getText();

                // Extract listing name, check-in, and check-out dates
                String listingName = extractionService.extractListingName(contentText);
                String[] bookingDates = extractionService.extractBookingDates(contentText);

                // Validate and add to booking info list
                if (validateBookingDates(bookingDates)) {
                    bookingInfoList.add(new Object[]{listingName, bookingDates});
                }

//                logger.info("EMAIL-Blocking dates for listing: " + listingName);
//                logger.info("Check-in: " + bookingDates[0]);
//                logger.info("Check-out: " + bookingDates[1]);

                // Go back to the main frame and email list
                driver.switchTo().defaultContent();
                return true;
            }
        } catch (StaleElementReferenceException e) {
            logger.warn("Element became stale during email processing", e);
        } catch (NoSuchElementException e) {
            logger.warn("Element not found during email processing", e);
        } catch (Exception e) {
            logger.error("Error while processing email", e);
        }
        return false;
    }

    private boolean validateBookingDates(String[] bookingDates) {
        if (bookingDates == null || bookingDates.length < 2) return false;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            LocalDate checkoutDate = LocalDate.parse(bookingDates[1], formatter);
            LocalDate currentDate = LocalDate.now();
            return checkoutDate.isAfter(currentDate);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format in booking dates: ", e);
            return false;
        }
    }

    private void processBookingInfoList(List<Object[]> bookingInfoList) {
        for (Object[] bookingInfo : bookingInfoList) {
            String listingName = (String) bookingInfo[0];
            String[] bookingDates = (String[]) bookingInfo[1];

            logger.info("EMAIL-Blocking dates for listing: " + listingName);
            if (bookingDates != null && bookingDates.length > 1) {
                logger.info("Check-in: " + bookingDates[0]);
                logger.info("Check-out: " + bookingDates[1]);
            } else {
                logger.warn("Booking dates are not available for listing: " + listingName);
            }
        }

        blockDatesOnPlatform(bookingInfoList);
    }

    private void blockDatesOnPlatform(List<Object[]> bookingInfoList) {
        airbnbSyncService.blockDates(bookingInfoList);
    }
}