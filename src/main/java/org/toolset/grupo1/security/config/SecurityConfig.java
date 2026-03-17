package org.toolset.grupo1.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/ws/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/sensors/**").hasAnyRole("SENSOR", "OPERATOR", "ADMIN")
                        .requestMatchers("/api/access/**").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers("/api/alerts/**").hasAnyRole("OPERATOR", "ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails sensor = User.withUsername("sensor-node")
                .password("{noop}sensor-pass")
                .roles("SENSOR")
                .build();
        UserDetails operator = User.withUsername("operator")
                .password("{noop}operator-pass")
                .roles("OPERATOR")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password("{noop}admin-pass")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(sensor, operator, admin);
    }
}

