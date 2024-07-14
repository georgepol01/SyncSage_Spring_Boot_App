package com.syncsage.syncsage.service;

import jakarta.annotation.PostConstruct;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailMonitoringService {

    @Autowired
    private WebDriver driver;

    @Value("${gmx.email}")
    private String gmxEmail;

    @Value("${gmx.password}")
    private String gmxPassword;

    @Value("${airbnb.listing.names}")
    private String listingNames;

    private Set<Cookie> cookies;

    private String[] listingNameArray;

    @PostConstruct
    public void initialLogin() {
        try {
            // Perform initial login and save cookies
            driver.get("https://www.gmx.com/");

            // Initialize WebDriverWait with a timeout of 10 seconds
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Wait until the login button is clickable and then click it
            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("login-button")));
            loginButton.click();

            // Find the email input field and enter the email address
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-email")));
            usernameField.sendKeys(gmxEmail);

            // Find the password input field and enter the password
            WebElement passwordField = driver.findElement(By.id("login-password"));
            passwordField.sendKeys(gmxPassword);

            // Find the login button and click to submit the login form
            WebElement loginSubmitButton = driver.findElement(By.cssSelector(".btn.btn-block.login-submit"));
            loginSubmitButton.click();

            // Wait for inbox to load after login
            Thread.sleep(5000); // Adjust as necessary, or replace with a more precise wait

            // Save cookies for session persistence
            cookies = driver.manage().getCookies();

            // Initialize the listing name array
            listingNameArray = listingNames.split(",");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 60000) // Check every minute (60000 ms)
    public void monitorBookingEmails() {
        try {
            // Load GMX Mail with cookies
            driver.get("https://mail.gmx.com/");
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    driver.manage().addCookie(cookie);
                }
                driver.navigate().refresh(); // Refresh to apply cookies
            }

            Thread.sleep(5000); // Wait for inbox to load

            // Search for unread emails from georgepol01@yahoo.com
            WebElement searchBox = driver.findElement(By.id("id5")); // Locate the search input field
            searchBox.sendKeys("is:unread from:georgepol01@yahoo.com"); // Enter the search query for unread emails
            WebElement searchButton = driver.findElement(By.cssSelector("input[data-webdriver='mailSearchButton']")); // Locate the search button
            searchButton.click(); // Click the search button
            Thread.sleep(5000); // Wait for search results to load

            // Parse email content and trigger actions for each unread email
            List<WebElement> emails = driver.findElements(By.cssSelector(".mail-list-item.new")); // Locate unread email elements
            for (WebElement email : emails) {
                email.click();
                Thread.sleep(2000); // Wait for email content to load

                // Extract details from email content
                WebElement emailContent = driver.findElement(By.cssSelector(".msg-body"));
                String contentText = emailContent.getText();

                // Extract listing name, check-in, and check-out dates
                String listingName = extractListingName(contentText);
                String[] bookingDates = extractBookingDates(contentText);

                // Block dates on Airbnb or Booking.com using extracted details
                if (listingName != null && bookingDates != null) {
                    blockDatesOnPlatform(listingName, bookingDates);
                }

                // Mark the email as read (optional, if needed)
                markEmailAsRead(email);

                // Go back to the email list
                driver.navigate().back();
                Thread.sleep(2000); // Wait for the email list to load again
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markEmailAsRead(WebElement emailElement) {
        try {
            WebElement markAsReadButton = emailElement.findElement(By.cssSelector(".mail-read-mark"));
            markAsReadButton.click();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractListingName(String emailContent) {
        for (String listingName : listingNameArray) {
            if (emailContent.contains(listingName.trim())) {
                return listingName.trim();
            }
        }
        return null;
    }

    private String[] extractBookingDates(String emailContent) {
        try {
            // Define regular expressions to match check-in and check-out date patterns in the email content
            Pattern checkInPattern = Pattern.compile("Check-in\\s+\\w+,\\s+\\w+\\s+\\d+\\s+\\d+:\\d+\\s+\\w+");
            Pattern checkOutPattern = Pattern.compile("Checkout\\s+\\w+,\\s+\\w+\\s+\\d+\\s+\\d+:\\d+\\s+\\w+");

            // Create matcher objects to find patterns in the email content
            Matcher checkInMatcher = checkInPattern.matcher(emailContent);
            Matcher checkOutMatcher = checkOutPattern.matcher(emailContent);

            // Check if both check-in and check-out patterns are found in the email content
            if (checkInMatcher.find() && checkOutMatcher.find()) {
                // Extract the full matched strings for check-in and check-out
                String checkInString = checkInMatcher.group();
                String checkOutString = checkOutMatcher.group();

                // Define the input date format based on the email content (e.g., "Sun, Aug 4 3:00 PM")
                SimpleDateFormat inputFormat = new SimpleDateFormat("E, MMM d h:mm a", Locale.ENGLISH);

                // Define the output date format (e.g., "yyyy-MM-dd")
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

                // Extract and format the check-in date
                String checkInDate = outputFormat.format(
                        inputFormat.parse(
                                // Concatenate the relevant parts of the matched string to form a valid date-time string
                                checkInString.split("\\s+")[1] + " " +
                                        checkInString.split("\\s+")[2] + " " +
                                        checkInString.split("\\s+")[3]
                        )
                );

                // Extract and format the check-out date
                String checkOutDate = outputFormat.format(
                        inputFormat.parse(
                                // Concatenate the relevant parts of the matched string to form a valid date-time string
                                checkOutString.split("\\s+")[1] + " " +
                                        checkOutString.split("\\s+")[2] + " " +
                                        checkOutString.split("\\s+")[3]
                        )
                );

                // Return the formatted check-in and check-out dates as an array of strings
                return new String[]{checkInDate, checkOutDate};
            }
        } catch (Exception e) {
            // Print stack trace in case of an exception (e.g., parsing error)
            e.printStackTrace();
        }
        // Return null if the dates couldn't be extracted or formatted
        return null;
    }

    private void blockDatesOnPlatform(String listingName, String[] bookingDates) {
        // Implement the logic to block dates on Airbnb or Booking.com
        // This can involve using Selenium to navigate to the respective platform and update the listing
        System.out.println("Blocking dates for listing: " + listingName);
        System.out.println("Check-in: " + bookingDates[0]);
        System.out.println("Check-out: " + bookingDates[1]);

        // Example logic to navigate to Airbnb/Booking.com and block dates
        // driver.get("https://www.airbnb.com/");
        // ... additional Selenium code to update the listing ...
    }
}
