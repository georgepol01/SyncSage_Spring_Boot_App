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
    @Scope("prototype")
    public WebDriver webDriver() {

        System.setProperty("webdriver.chrome.driver", driverPath);
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-crash-reporter");
        options.addArguments("--disable-logging");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-browser-side-navigation");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--log-level=ALL");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-extensions");
        options.addArguments("user-data-dir=" + userDataDir);

        return new ChromeDriver(options);

    }

}
