package com.syncsage.syncsage.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SeleniumConfig {

    @Bean
    @Scope("singleton")
    public WebDriver webDriver() {
        // Configure Chrome driver path
        String driverPath = System.getProperty("user.dir") + "/src/main/resources/drivers/chromedriver.exe";
        System.setProperty("webdriver.chrome.driver", driverPath);

        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run Chrome in headless mode
        options.addArguments("user-data-dir=" + System.getProperty("user.dir") + "/src/main/resources/chrome-profile"); // Use a custom profile to persist session data

        // Initialize WebDriver
        return new ChromeDriver(options);
    }
}
