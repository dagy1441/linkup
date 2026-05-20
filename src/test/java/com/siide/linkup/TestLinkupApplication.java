package com.siide.linkup;

import org.springframework.boot.SpringApplication;

public class TestLinkupApplication {

    public static void main(String[] args) {
        SpringApplication.from(LinkupApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
