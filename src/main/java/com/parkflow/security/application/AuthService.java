package com.parkflow.security.application;

import com.parkflow.security.api.dto.AuthRequest;
import com.parkflow.security.api.dto.AuthResponse;
import com.parkflow.security.api.dto.RegisterRequest;
import com.parkflow.security.domain.AppUser;
import com.parkflow.security.infra.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        var user = new AppUser(
                UUID.randomUUID(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                "USER"
        );
        
        repository.save(user);
        
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
                
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(
                jwtToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
