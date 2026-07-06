package com.aiworkspace.backend.service;

import com.aiworkspace.backend.dto.AuthResponse;
import com.aiworkspace.backend.dto.LoginRequest;
import com.aiworkspace.backend.dto.RegisterRequest;
import com.aiworkspace.backend.dto.UserResponse;
import com.aiworkspace.backend.model.Organization;
import com.aiworkspace.backend.model.User;
import com.aiworkspace.backend.repository.OrganizationRepository;
import com.aiworkspace.backend.repository.UserRepository;
import com.aiworkspace.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        OrganizationRepository organizationRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }

        Organization organization = organizationRepository.findByNameIgnoreCase(request.organizationName().trim())
                .orElse(null);
        boolean isNewOrg = organization == null;
        if (isNewOrg) {
            organization = new Organization();
            organization.setName(request.organizationName().trim());
            organization.setCreatedAt(Instant.now());
            organization = organizationRepository.save(organization);
        }

        User user = new User();
        user.setEmail(email);
        user.setHashedPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        user.setOrgId(organization.getId());
        user.setRole(isNewOrg ? User.ROLE_ADMIN : User.ROLE_MEMBER);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
        }

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
