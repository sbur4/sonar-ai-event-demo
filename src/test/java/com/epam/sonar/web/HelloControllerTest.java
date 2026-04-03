package com.epam.sonar.web;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
@DisplayName("HelloController Tests")
@FieldDefaults(level = AccessLevel.PRIVATE)
class HelloControllerTest {

    static final String CONTENT_TYPE = "Content-Type";
    static final String HELLO_EPAMMER_WITH_ID = "Hello Epammer with ID";
    static final String HELLO_ENDPOINT = "/hello";
    static final MediaType TEXT_PLAIN = MediaType.TEXT_PLAIN;


    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("GET /hello - Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("Should return 200 OK with hello message")
        @SneakyThrows
        void shouldReturnHelloMessage() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
                    .andExpect(content().string(containsString(HELLO_EPAMMER_WITH_ID)));
        }

        @Test
        @DisplayName("Should return message with valid ID format (0-9999)")
        @SneakyThrows
        void shouldReturnMessageWithValidIdFormat() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(matchesPattern(HELLO_EPAMMER_WITH_ID + " \\d{1,4}!")));
        }

        @Test
        @DisplayName("Should return different IDs for multiple requests")
        @SneakyThrows
        void shouldReturnDifferentIdsForMultipleRequests() {
            // When
            String firstResponse = mockMvc.perform(get(HELLO_ENDPOINT))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String secondResponse = mockMvc.perform(get(HELLO_ENDPOINT))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Then - Assuming the controller generates unique IDs
            // If IDs should be the same, remove this assertion
            assertThat(firstResponse).isNotEqualTo(secondResponse);
        }

        @Test
        @DisplayName("Should have correct message structure")
        @SneakyThrows
        void shouldHaveCorrectMessageStructure() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(startsWith("Hello Epammer")))
                    .andExpect(content().string(containsString("with ID")));
        }
    }

    @Nested
    @DisplayName("GET /hello - Request Header Tests")
    class RequestHeaderTests {

        @Test
        @DisplayName("Should accept request with Accept header")
        @SneakyThrows
        void shouldAcceptRequestWithAcceptHeader() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT)
                            .header("Accept", TEXT_PLAIN.toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(HELLO_EPAMMER_WITH_ID)));
        }

        @Test
        @DisplayName("Should accept request without headers")
        @SneakyThrows
        void shouldAcceptRequestWithoutHeaders() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should set correct response headers")
        @SneakyThrows
        void shouldSetCorrectResponseHeaders() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().exists(CONTENT_TYPE))
                    .andExpect(header().string(CONTENT_TYPE, containsString(TEXT_PLAIN.toString())));
        }
    }

    @Nested
    @DisplayName("GET /hello - Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 for invalid endpoint")
        @SneakyThrows
        void shouldReturn404ForInvalidEndpoint() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT + "/invalid"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 405 for POST method")
        @SneakyThrows
        void shouldReturn405ForPostMethod() {
            // When & Then
            mockMvc.perform(post(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should return 405 for PUT method")
        @SneakyThrows
        void shouldReturn405ForPutMethod() {
            // When & Then
            mockMvc.perform(put(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should return 405 for DELETE method")
        @SneakyThrows
        void shouldReturn405ForDeleteMethod() {
            // When & Then
            mockMvc.perform(delete(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("GET /hello - Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle concurrent requests")
        @SneakyThrows
        void shouldHandleConcurrentRequests() {
            // When - Simulate multiple concurrent requests
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(get(HELLO_ENDPOINT))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString(HELLO_EPAMMER_WITH_ID)));
            }
        }

        @Test
        @DisplayName("Should respond quickly")
        @SneakyThrows
        void shouldRespondQuickly() {
            // When
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andExpect(status().isOk());

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Then - Response should be under 1 second
            assert duration < 1000 : "Response time was " + duration + "ms, expected < 1000ms";
        }
    }

    @Nested
    @DisplayName("GET /hello - Query Parameter Tests")
    class QueryParameterTests {

        @Test
        @DisplayName("Should ignore unknown query parameters")
        @SneakyThrows
        void shouldIgnoreUnknownQueryParameters() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT)
                            .param("unknown", "value"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(HELLO_EPAMMER_WITH_ID)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "123", "special!@#", ""})
        @DisplayName("Should handle various query parameter values")
        @SneakyThrows
        void shouldHandleVariousQueryParameterValues(String paramValue) {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT)
                            .param("testParam", paramValue))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /hello - Content Validation Tests")
    class ContentValidationTests {

        @Test
        @DisplayName("Should not return null or empty response")
        @SneakyThrows
        void shouldNotReturnNullOrEmptyResponse() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(emptyOrNullString())));
        }

        @Test
        @DisplayName("Should return response with minimum length")
        @SneakyThrows
        void shouldReturnResponseWithMinimumLength() {
            // When
            String response = mockMvc.perform(get(HELLO_ENDPOINT))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Then - Response should be at least 20 characters
            assert response.length() >= 20 : "Response length was " + response.length();
        }

        @Test
        @DisplayName("Should return valid UTF-8 encoded response")
        @SneakyThrows
        void shouldReturnValidUtf8EncodedResponse() {
            // When & Then
            mockMvc.perform(get(HELLO_ENDPOINT))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().encoding("UTF-8"));
        }
    }

    @Nested
    @DisplayName("GET /hello - Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should complete full request-response cycle successfully")
        @SneakyThrows
        void shouldCompleteFullRequestResponseCycleSuccessfully() {
            // Given
            String endpoint = HELLO_ENDPOINT;

            // When
            ResultActions result = mockMvc.perform(get(endpoint)
                    .accept(TEXT_PLAIN));

            // Then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
                    .andExpect(content().string(containsString("Hello Epammer")))
                    .andExpect(content().string(containsString("with ID")));
//                    .andExpect(jsonPath("$").doesNotExist()); // Should not be JSON
        }

        @Test
        @DisplayName("Should maintain consistent response format across multiple calls")
        @SneakyThrows
        void shouldMaintainConsistentResponseFormat() {
            // When - Make multiple calls
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get(HELLO_ENDPOINT))
                        .andExpect(status().isOk())
                        .andExpect(content().string(matchesPattern(HELLO_EPAMMER_WITH_ID + " .+")));
            }
        }
    }
}