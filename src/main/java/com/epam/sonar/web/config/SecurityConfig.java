package com.epam.sonar.web.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 🔴 S4502 - CSRF protection disabled
    // 🔴 S4834 - All requests permitted without authentication
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)          // ❌ CSRF disabled
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(PathRequest.toH2Console()).permitAll()
//                        .requestMatchers("/h2-console/**").permitAll()
//                        .requestMatchers("/error").permitAll()
                        .anyRequest().permitAll()       // ❌ No auth required anywhere
                )
//                .csrf(csrf -> csrf
//                        .ignoringRequestMatchers(toH2Console()).disable()
//                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );
        return http.build();
    }

//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> web.ignoring().requestMatchers("/h2-console/**");
//    }
}
