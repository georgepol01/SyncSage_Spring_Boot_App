package com.syncsage.syncsage.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class SeleniumConfig {

    @Value("${webdriver.chrome.driver}")
    private String driverPath;

    @Value("${chrome.user.data.dir}")
    private String userDataDir;

    @Bean
    @Scope("singleton")
    public WebDriver webDriver() {
        System.setProperty("webdriver.chrome.driver", driverPath);

        ChromeOptions options = new ChromeOptions();
        // Uncomment the following line to run Chrome in headless mode
        // options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--log-level=ALL");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-extensions");
        options.addArguments("user-data-dir=" + userDataDir);

        return new ChromeDriver(options);
    }

}
