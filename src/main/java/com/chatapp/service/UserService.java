package com.chatapp.service;

import com.chatapp.dto.ContactDTO;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Registrazione utenti e query di supporto (lookup, lista contatti).
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Lanciata quando qualcuno prova a registrarsi con un'email già in uso.
    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("An account with email '" + email + "' already exists");
        }
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new EmailAlreadyUsedException(request.getEmail());
        }
        // La password viene sempre salvata come hash BCrypt, mai in chiaro.
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    public User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    // Tutti gli altri utenti del sistema, usati per popolare la lista "inizia una nuova chat".
    public List<ContactDTO> listOtherUsers(Long currentUserId) {
        return userRepository.findByIdNotOrderByFullNameAsc(currentUserId).stream()
                .map(u -> new ContactDTO(u.getId(), u.getFullName(), u.getEmail()))
                .toList();
    }
}
