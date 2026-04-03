package com.epam.sonar.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.random.RandomGenerator;

@Slf4j
@RestController
@RequestMapping("/hello")
public class HelloController {

    private static final RandomGenerator SHARED_RANDOM = RandomGenerator.getDefault();

    // NOTE: http://localhost:8181/api/hello
    @GetMapping
    public ResponseEntity<String> helloEpam() {
        int id = SHARED_RANDOM.nextInt(10_000);
        log.info("Processing hello request. Generated ID: '{}'", id);

        String response = "Hello Epammer with ID %d!".formatted(id);
        log.debug("Returning response: '{}'", response);

        return ResponseEntity.ok(response);
    }
}
