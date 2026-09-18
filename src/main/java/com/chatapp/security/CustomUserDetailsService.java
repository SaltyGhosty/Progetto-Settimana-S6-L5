package com.chatapp.security;

import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Ponte tra Spring Security e il nostro database: dato un'email cerca lo User
// corrispondente e lo avvolge in un CustomUserPrincipal. Questo bean viene usato
// dal DaoAuthenticationProvider definito in SecurityConfig.
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for email: " + email));
        return new CustomUserPrincipal(user);
    }
}
