package com.bingeboxed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BingeboxedApplication {

    public static void main(String[] args) {
        SpringApplication.run(BingeboxedApplication.class, args);
    }

}
