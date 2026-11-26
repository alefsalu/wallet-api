package com.portfolio.wallet.application.port.input;

public interface LogoutUserUseCase {

    void logout(String refreshToken);
}
