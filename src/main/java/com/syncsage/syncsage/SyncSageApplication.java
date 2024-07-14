package com.syncsage.syncsage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyncSageApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncSageApplication.class, args);
    }

}
