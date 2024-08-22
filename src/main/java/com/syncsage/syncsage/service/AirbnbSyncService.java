package com.syncsage.syncsage.service;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AirbnbSyncService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMonitoringService.class);

    @Value("${airbnb.url}")
    private String airbnbUrl;

    @Value("${airbnb.email}")
    private String airbnbEmail;

    @Value("${airbnb.password}")
    private String airbnbPassword;

    public void blockDates(WebDriverWait wait, WebDriver driver, List<Object[]> bookingInfoList) {

        try {
            driver.get(airbnbUrl);

            WebElement menuButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='react-application']/div/div/div[1]/div/div[3]/div[2]/div/div/div/header/div/div[3]/nav/div[2]/div/button")));
            menuButton.click();

            WebElement listings = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"simple-header-profile-menu\"]/div/a[4]")));
            listings.click();

            for (Object[] bookingInfo : bookingInfoList) {
                String listingName = (String) bookingInfo[0];
                String[] bookingDates = (String[]) bookingInfo[1];

                WebElement calendar = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"react-application\"]/div/div/div[1]/div/div/div[1]/div/div/div/nav/div[1]/div/div[2]/div/div/div/div/div[2]")));
                calendar.click();

                WebElement chooseMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"calendarControlsChipGroup\"]/div[2]/div/div[2]/div[1]/div/div[1]/div/button")));
                chooseMenu.click();

                WebElement listingElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[normalize-space(text())='" + listingName + "']/ancestor::label//input[@type='radio']")));
                listingElement.click();

                WebElement submitListing = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[8]/div/div/section/div/div/div[2]/div/footer/div/button")));
                submitListing.click();

                Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(bookingDates[0]);
                Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(bookingDates[1]);

                List<Date> dateRange = getDateRange(startDate, endDate);

                logger.info("SYNC-Blocking dates for listing: " + listingName);
                logger.info("dateRange: " + dateRange);

                for (Date date : dateRange) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
                    scrollAndClickDate(wait, driver, dateStr);

                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-date='" + dateStr + "']")));
                }

                Thread.sleep(4000);

                WebElement blockDates = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"HOST-CALENDAR-SIDEBAR-CONTAINER\"]/div/div/div[2]/div/button[2]")));
                blockDates.click();

                logger.info("SYNC--Blocked dates for listing: " + listingName);
                logger.info("Check-in: " + bookingDates[0]);
                logger.info("Check-out: " + bookingDates[1]);
            }
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

    }

    private void scrollAndClickDate(WebDriverWait wait, WebDriver driver, String date) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement calendarContainer = driver.findElement(By.xpath("//*[@id=\"FMP-target\"]/div"));

        int scrollSteps = 50;
        long scrollWaitTime = 500;
        int maxRetryAttempts = 3;
        long retryDelay = 2000;
        boolean shouldLog = false;

        for (int i = 0; i < scrollSteps; i++) {
            boolean success = false;

            for (int attempt = 0; attempt < maxRetryAttempts; attempt++) {
                try {
                    WebElement dateButton;
                    try {
                        dateButton = driver.findElement(By.xpath("//button[@data-date='" + date + "']"));
                    } catch (NoSuchElementException e) {
                        if (i == scrollSteps - 1 && attempt == maxRetryAttempts - 1) {
                            logger.warn("Date button not found after maximum scrolling and attempts. Date: " + date);
                        }
                        js.executeScript("arguments[0].scrollBy(0, arguments[0].scrollHeight / " + scrollSteps + ");", calendarContainer);
                        Thread.sleep(scrollWaitTime);
                        continue; // Retry locating the element after scrolling
                    }
                    js.executeScript("arguments[0].scrollIntoView(true);", dateButton);
                    wait.until(ExpectedConditions.elementToBeClickable(dateButton));

                    dateButton.click();
                    Thread.sleep(500);
                    logger.info("Clicked on date: " + date);
                    success = true;
                    break;
                } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
                    if (attempt == maxRetryAttempts - 1) {
                        shouldLog = true;  // Log only after the final attempt fails
                    }
                    if (shouldLog) {
                        logger.warn("Click intercepted or element became stale. Retrying... Attempt " + (attempt + 1));
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread was interrupted during retry wait", ie);
                    }
                } catch (WebDriverException e) {
                    if (attempt == maxRetryAttempts - 1) {
                        shouldLog = true;  // Log only after the final attempt fails
                    }
                    if (shouldLog) {
                        logger.error("Connection issue or WebDriver exception. Retrying... Attempt " + (attempt + 1));
                    }
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Thread was interrupted during retry delay", ie);
                    }
                } catch (Exception e) {
                    logger.error("Unexpected error during clicking", e);
                    break;
                }
            }
            if (success) {
                return;
            }
            // Scroll down the calendar container if the date button was not found or not clickable
            js.executeScript("arguments[0].scrollBy(0, arguments[0].scrollHeight / " + scrollSteps + ");", calendarContainer);
            try {
                Thread.sleep(scrollWaitTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                logger.error("Thread was interrupted during scrolling", ex);
            }
        }
        logger.error("Date not found or clickable after all attempts: " + date);

    }


    private List<Date> getDateRange(Date startDate, Date endDate) {

        List<Date> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        while (calendar.getTime().before(endDate) || calendar.getTime().equals(endDate)) {
            dates.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;

    }

}
//    public void login() {
//
//        try {
//            // Step 1: Navigate to Airbnb and update prices
//            driver.get(airbnbUrl);
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            Thread.sleep(randomWait);
//
//            WebElement menuButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='react-application']/div/div/div[1]/div/div[3]/div[2]/div/div/div/header/div/div[3]/nav/div[2]/div/button")));
//            menuButton.click();
//
//            // Wait for the element to be visible
//            WebElement login = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='simple-header-profile-menu']/div/a[2]/div")));
//            login.click();
//
//            Thread.sleep(randomWait);
//
//            // Wait for the element to be visible
//            WebElement byEmail = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[10]/div/section/div/div/div[2]/div/div[2]/div/div[3]/div/div[4]/button")));
//            byEmail.click();
//
//            Thread.sleep(randomWait);
//
//            // Wait for the email input field to become visible and enter the email address
//            WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"email-login-email\"]")));
//            emailInput.sendKeys(airbnbEmail);
//
//            Thread.sleep(randomWait);
//
//            // Wait for the email input field to become visible and enter the email address
//            WebElement emailSubmit = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[10]/div/section/div/div/div[2]/div/div[2]/div/form/div[3]/button/span[1]")));
//            emailSubmit.click();
//
//            Thread.sleep(randomWait);
//
//            // Wait for the email input field to become visible and enter the email address
//            WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"email-signup-password\"]")));
//            passwordInput.sendKeys(airbnbPassword);
//
//            Thread.sleep(randomWait);
//
//            // Wait for the email input field to become visible and enter the email address
//            WebElement passwordSubmit = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[10]/div/section/div/div/div[2]/div/div[2]/div/div/form/div[3]/button/span[1]")));
//            passwordSubmit.click();
//
//        } catch (Exception e) {
//            logger.error("Unexpected error occurred", e);
//        }
//    }