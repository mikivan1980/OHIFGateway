package com.mikivan.OHIFGateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class runOHIFGateway {

    public static void main(String[] args) {

        System.out.println("[START...........OHIFGateway]");

        SpringApplication.run(runOHIFGateway.class, args);

        System.out.println("[STARTED]");
    }
}
