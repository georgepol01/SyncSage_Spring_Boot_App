package com.syncsage.syncsage.service;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingSyncService {

    @Autowired
    private WebDriver driver;

    public void syncPricesAndAvailability(String listingId, double price, Date availabilityDate) {
        try {
            // Step 1: Navigate to Airbnb and update prices
            driver.get("https://www.airbnb.com");

            // Simulate human-like behavior
            simulateHumanBehavior();

            // Example: Update price on Airbnb
            WebElement priceInput = driver.findElement(By.id("price-input"));
            performHumanLikeInput(priceInput, String.valueOf(price));

            // Example: Update availability on Airbnb
            WebElement availabilityInput = driver.findElement(By.id("availability-input"));
            performHumanLikeInput(availabilityInput, availabilityDate.toString());

            // Step 2: Navigate to Booking.com and update prices
            driver.get("https://www.booking.com");

            // Simulate human-like behavior
            simulateHumanBehavior();

            // Example: Update price on Booking.com
            WebElement priceInputBooking = driver.findElement(By.id("price-input-booking"));
            performHumanLikeInput(priceInputBooking, String.valueOf(price));

            // Example: Update availability on Booking.com
            WebElement availabilityInputBooking = driver.findElement(By.id("availability-input-booking"));
            performHumanLikeInput(availabilityInputBooking, availabilityDate.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void simulateHumanBehavior() throws InterruptedException {
        // Random delay before actions (between 2 to 5 seconds)
        int delaySeconds = ThreadLocalRandom.current().nextInt(2, 6);

        // Convert delaySeconds to Duration
        Duration delayDuration = Duration.ofSeconds(delaySeconds);

        // Wait for the duration
        WebDriverWait wait = new WebDriverWait(driver, delayDuration);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@id, 'random-id')]"))); // Replace with actual XPath

        // Mimic mouse movements (move to a random element)
        Actions actions = new Actions(driver);
        WebElement randomElement = driver.findElement(By.xpath("//*[contains(@id, 'random-id')]")); // Replace with actual XPath
        actions.moveToElement(randomElement).perform();

        // Scroll the page
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,300)");

        // Wait for JavaScript to load
        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
    }

    private void performHumanLikeInput(WebElement element, String text) throws InterruptedException {
        // Clear existing text (if any)
        element.clear();

        // Type text with human-like typing speed and minor typos
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            // Random delay between typing each character (10 to 50 milliseconds)
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 51));
        }
    }
}
