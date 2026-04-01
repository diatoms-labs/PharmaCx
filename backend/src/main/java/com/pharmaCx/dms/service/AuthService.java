package com.pharmaCx.dms.service;

import com.pharmaCx.dms.api.dto.AuthResponse;
import com.pharmaCx.dms.api.dto.LoginRequest;
import com.pharmaCx.dms.api.dto.RegisterRequest;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.model.OrganizationalUnit;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.domain.repository.OrganizationalUnitRepository;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.exception.ValidationException;
import com.pharmaCx.dms.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository userRepo;
    private final OrganizationalUnitRepository orgUnitRepo;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepo,
                       OrganizationalUnitRepository orgUnitRepo,
                       JwtTokenProvider tokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.orgUnitRepo = orgUnitRepo;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername();
        AppUser user = userRepo.findByUsername(identifier)
                .or(() -> userRepo.findByEmail(identifier))
                .orElseThrow(() -> new ValidationException("Invalid credentials"));

        if (!user.isActive()) {
            throw new ValidationException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid credentials");
        }

        OrganizationalUnit unit = user.getUnitId() != null ? orgUnitRepo.findById(user.getUnitId()).orElse(null) : null;
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), user.getUnitId(),
                unit != null ? unit.getCode() : null,
                unit != null ? unit.getDisplayName() : null);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new ValidationException("Username already exists");
        }
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new ValidationException("Email already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setUnitId(request.getUnitId());
        user.setActive(true);
        user.setMustChangePassword(false);
        user.setEditorPermissions(new AppUser.EditorPermissions());

        user = userRepo.save(user);

        OrganizationalUnit unit = user.getUnitId() != null ? orgUnitRepo.findById(user.getUnitId()).orElse(null) : null;
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), user.getUnitId(),
                unit != null ? unit.getCode() : null,
                unit != null ? unit.getDisplayName() : null);
    }

    public AppUser getUserById(String userId) {
        return findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public java.util.Optional<AppUser> findUserById(String userId) {
        return userRepo.findById(userId);
    }

}
