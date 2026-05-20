package com.siide.linkup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan("com.siide.linkup")
@EnableScheduling
public class LinkupApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkupApplication.class, args);
    }

}
