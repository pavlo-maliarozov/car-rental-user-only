package com.example.rental.controller;

import com.example.rental.config.jwt.JwtService;
import com.example.rental.dto.auth.AuthResponse;
import com.example.rental.dto.auth.LoginRequest;
import com.example.rental.dto.auth.SignupRequest;
import com.example.rental.model.User;
import com.example.rental.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService, AuthenticationManager am, JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = am;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
        User u = userService.signup(req.email(), req.password());
        String token = jwtService.generate(u.getEmail(), Map.of("role", "USER"));
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        String token = jwtService.generate(req.email(), Map.of("role", "USER"));
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
