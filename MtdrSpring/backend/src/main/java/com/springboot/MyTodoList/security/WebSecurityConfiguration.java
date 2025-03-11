package com.springboot.MyTodoList.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    @Autowired
    private OciUserDetailsService userDetailsService; // Your custom UserDetailsService

    @Autowired
    private JwtTokenFilter jwtTokenFilter; // Your custom JWT Filter

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 1. Disable CSRF (common for stateless REST APIs)
                // Simplified: .disable() covers everything, no need to ignore h2 path
                // specifically
                .csrf(csrf -> csrf.disable())

                // 2. Set session management to stateless (correct for JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Set permissions on endpoints
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints:
                        .requestMatchers(new AntPathRequestMatcher("/auth/login")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/auth/register")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher(HttpMethod.OPTIONS.name(), "/**")).permitAll() // KEEP:

                        .anyRequest().authenticated() // KEEP: All other requests require authentication
                )

                // 4. Add your JWT token filter before the standard authentication filter
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class); // KEEP: Integrates your
                                                                                              // JWT validation

        // 5. REMOVED: H2 console specific header configuration
        // .headers(headers -> headers.frameOptions(frameOptions ->
        // frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        // Ensure the provider uses the secure password encoder defined below
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // IMPORTANT CHANGE: Use a strong, secure password encoder like BCrypt
        // Ensure passwords stored in your database are also encoded with BCrypt
        return new BCryptPasswordEncoder();
    }

    // UPDATED: More modern way to expose the AuthenticationManager bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}