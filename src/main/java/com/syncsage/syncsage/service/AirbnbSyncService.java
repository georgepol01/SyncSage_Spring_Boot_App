package com.syncsage.syncsage.service;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

@Service
public class AirbnbSyncService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMonitoringService.class);
    private final WebDriver driver;
    @Autowired
    public AirbnbSyncService(WebDriver driver) {
        this.driver = driver;
    }
    @Value("${airbnb.url}")
    private String airbnbUrl;

    @Value("${airbnb.email}")
    private String airbnbEmail;

    @Value("${airbnb.password}")
    private String airbnbPassword;

    public void blockDates(List<Object[]> bookingInfoList) {
        try {
            driver.get(airbnbUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

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
                    scrollAndClickDate(dateStr);

//                    // Ensure the UI is updated after clicking the date
//                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//button[@data-date='" + dateStr + "']")));
//                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-date='" + dateStr + "']")));
                }

                Thread.sleep(7000);

                WebElement blockDates = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"HOST-CALENDAR-SIDEBAR-CONTAINER\"]/div/div/div[2]/div/button[2]")));
                blockDates.click();

                logger.info("SYNC--Blocked dates for listing: " + listingName);
                logger.info("Check-in: " + bookingDates[0]);
                logger.info("Check-out: " + bookingDates[1]);

//                // Refresh the page after blocking dates
//                driver.navigate().refresh();
//
//                // Wait for the page to refresh and be ready
//                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='react-application']")));
            }

        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }
    }

    private void scrollAndClickDate(String date) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement calendarContainer = driver.findElement(By.xpath("//*[@id=\"FMP-target\"]/div"));

        int scrollSteps = 150;
        long scrollWaitTime = 200; // Reduced wait time for smoother scrolling

        for (int i = 0; i < scrollSteps; i++) {
            try {
                WebElement dateButton = driver.findElement(By.xpath("//button[@data-date='" + date + "']"));
                js.executeScript("arguments[0].scrollIntoView(true);", dateButton);
                wait.until(ExpectedConditions.elementToBeClickable(dateButton));

                // Retry clicking if the initial attempt fails
                for (int attempt = 0; attempt < 3; attempt++) {
                    try {
                        dateButton.click();
                        logger.info("Clicked on date: " + date);
                        return;
                    } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
                        logger.warn("Click intercepted or element became stale. Retrying... Attempt " + (attempt + 1));
                        // Wait for a short time and recheck the element's clickability
                        Thread.sleep(500); // Brief wait before retrying

                        // Re-find the dateButton in case it became stale
                        dateButton = driver.findElement(By.xpath("//button[@data-date='" + date + "']"));
                        js.executeScript("arguments[0].scrollIntoView(true);", dateButton); // Re-scroll into view
                        wait.until(ExpectedConditions.elementToBeClickable(dateButton));
                    }
                }
            } catch (NoSuchElementException | InterruptedException e) {
                js.executeScript("arguments[0].scrollBy(0, arguments[0].scrollHeight / " + scrollSteps + ");", calendarContainer);
                try {
                    Thread.sleep(scrollWaitTime);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("Thread was interrupted during scrolling", ex);
                }
            }
        }

        logger.error("Date not found or clickable: " + date);
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

