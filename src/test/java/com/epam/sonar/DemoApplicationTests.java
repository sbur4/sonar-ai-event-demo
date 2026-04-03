package com.epam.sonar;

import com.epam.sonar.web.HelloController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@DisplayName("Application Fast Unit Tests")
class DemoApplicationTests {

    @Autowired
    private HelloController helloController;

    @Test
    @DisplayName("Should instantiate HelloController")
    void shouldInstantiateHelloController() {
        assertThat(helloController)
                .as("HelloController should be instantiated")
                .isNotNull();
    }
}
