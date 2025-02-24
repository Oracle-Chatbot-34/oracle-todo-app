package com.springboot.MyTodoList.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class OciUserDetailsService implements UserDetailsService {

    @Value("${spring.security.user.name:admin}")
    private String defaultUsername;
    
    @Value("${spring.security.user.password:}")
    private String defaultPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // For local development, we use a simple user store
        // In production, this would verify against OCI IAM
        if (username.equals(defaultUsername)) {
            return new User(
                defaultUsername,
                defaultPassword, // In production, we'd use OCI auth instead
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }
        
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
