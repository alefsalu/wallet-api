package com.portfolio.wallet.application.port.input;

public interface LoginUserUseCase {

    record LoginCommand(String email, String password) {}

    record TokenPairResult(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

    TokenPairResult login(LoginCommand command);
}
