package com.springboot.MyTodoList.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private OciUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Disable CSRF for APIs
        http.csrf().disable();

        // Set session management to stateless
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // Set permissions on endpoints
        http.authorizeRequests()
            // Public endpoints
            .antMatchers("/auth/login").permitAll()
            .antMatchers("/h2-console/**").permitAll()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // Private endpoints
            .anyRequest().authenticated();

        // Add JWT token filter
        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console frames
        http.headers().frameOptions().sameOrigin();
    }
}
