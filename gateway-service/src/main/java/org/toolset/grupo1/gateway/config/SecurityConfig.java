package org.toolset.grupo1.gateway.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/movement/**", "/api/seismic/**", "/api/temperature/**", "/api/access/events", "/api/door-open/**")
                        .hasAnyRole("SENSOR", "OPERATOR", "ADMIN")
                        .requestMatchers("/api/access/check", "/api/alerts").hasAnyRole("OPERATOR", "ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                );

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

