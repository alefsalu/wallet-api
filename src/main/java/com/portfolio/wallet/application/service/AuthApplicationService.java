package com.portfolio.wallet.application.service;

import com.portfolio.wallet.application.port.input.LoginUserUseCase;
import com.portfolio.wallet.application.port.input.LogoutUserUseCase;
import com.portfolio.wallet.application.port.input.RefreshTokenUseCase;
import com.portfolio.wallet.application.port.input.RegisterUserUseCase;
import com.portfolio.wallet.application.port.output.PasswordHasherPort;
import com.portfolio.wallet.application.port.output.RefreshTokenPort;
import com.portfolio.wallet.application.port.output.TokenServicePort;
import com.portfolio.wallet.application.port.output.UserRepositoryPort;
import com.portfolio.wallet.application.port.output.WalletRepositoryPort;
import com.portfolio.wallet.domain.exception.EmailAlreadyRegisteredException;
import com.portfolio.wallet.domain.exception.UnauthorizedException;
import com.portfolio.wallet.domain.model.User;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService
        implements RegisterUserUseCase, LoginUserUseCase, RefreshTokenUseCase, LogoutUserUseCase {

    private final UserRepositoryPort userRepository;
    private final WalletRepositoryPort walletRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort jwtTokenService;
    private final RefreshTokenPort refreshTokenService;

    public AuthApplicationService(
            UserRepositoryPort userRepository,
            WalletRepositoryPort walletRepository,
            PasswordHasherPort passwordHasher,
            TokenServicePort jwtTokenService,
            RefreshTokenPort refreshTokenService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public UserResult register(RegisterCommand command) {
        String cleanEmail = command.email().toLowerCase().trim();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new EmailAlreadyRegisteredException("Email " + command.email() + " is already registered");
        }

        String hashedPassword = passwordHasher.hash(command.password());
        User user = User.create(command.name().trim(), cleanEmail, hashedPassword);
        User savedUser = userRepository.save(user);

        // Atomically initialize user's wallet
        Wallet wallet = Wallet.create(UserId.of(savedUser.id()));
        walletRepository.save(wallet);

        return new UserResult(savedUser.id(), savedUser.name(), savedUser.email());
    }

    @Override
    @Transactional
    public TokenPairResult login(LoginCommand command) {
        String cleanEmail = command.email().toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtTokenService.generateAccessToken(
                user.id(),
                user.email(),
                List.of(user.role()));

        String refreshToken = refreshTokenService.createRefreshToken(user.id());

        return new TokenPairResult(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenService.getAccessTokenExpirationSeconds());
    }

    @Override
    @Transactional
    public TokenPairResult refresh(String rawRefreshToken) {
        RefreshTokenPort.RotatedToken rotated = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String accessToken = jwtTokenService.generateAccessToken(
                user.id(),
                user.email(),
                List.of(user.role()));

        return new TokenPairResult(
                accessToken,
                rotated.rawToken(),
                "Bearer",
                jwtTokenService.getAccessTokenExpirationSeconds());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }
}
