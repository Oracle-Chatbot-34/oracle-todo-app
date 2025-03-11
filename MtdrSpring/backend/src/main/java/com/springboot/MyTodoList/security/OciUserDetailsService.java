package com.springboot.MyTodoList.security;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class OciUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.security.user.name:admin}")
    private String defaultUsername;

    @Value("${spring.security.user.password:}")
    private String defaultPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up the user in the database
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(), // The password should already be encoded
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        }

        // For fallback/development, we use the default user if it matches
        if (username.equals(defaultUsername)) {
            return new org.springframework.security.core.userdetails.User(
                    defaultUsername,
                    defaultPassword,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}