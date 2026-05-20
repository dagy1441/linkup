package com.siide.linkup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LinkupApplicationTests {

    @Test
    void contextLoads() {
    }

}
