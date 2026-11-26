package com.portfolio.wallet.application.port.input;

public interface RefreshTokenUseCase {

    LoginUserUseCase.TokenPairResult refresh(String refreshToken);
}
