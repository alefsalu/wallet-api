package com.portfolio.wallet.interfaces.rest.controller;

import com.portfolio.wallet.application.port.input.LoginUserUseCase;
import com.portfolio.wallet.application.port.input.LogoutUserUseCase;
import com.portfolio.wallet.application.port.input.RefreshTokenUseCase;
import com.portfolio.wallet.application.port.input.RegisterUserUseCase;
import com.portfolio.wallet.interfaces.rest.dto.LoginRequest;
import com.portfolio.wallet.interfaces.rest.dto.RefreshTokenRequest;
import com.portfolio.wallet.interfaces.rest.dto.RegisterRequest;
import com.portfolio.wallet.interfaces.rest.dto.TokenPairResponse;
import com.portfolio.wallet.interfaces.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh and logout")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user and initialize digital wallet")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserUseCase.UserResult result = registerUserUseCase.register(
                new RegisterUserUseCase.RegisterCommand(request.name(), request.email(), request.password()));
        return new UserResponse(result.id(), result.name(), result.email());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive RS256 token pair")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        LoginUserUseCase.TokenPairResult result = loginUserUseCase.login(
                new LoginUserUseCase.LoginCommand(request.email(), request.password()));
        return new TokenPairResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresIn());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new RS256 access token")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginUserUseCase.TokenPairResult result = refreshTokenUseCase.refresh(request.refreshToken());
        return new TokenPairResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresIn());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke refresh token and end session")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logoutUserUseCase.logout(request.refreshToken());
    }
}
