package com.portfolio.wallet.application.port.input;

import java.util.UUID;

public interface RegisterUserUseCase {

    record RegisterCommand(String name, String email, String password) {}

    record UserResult(UUID id, String name, String email) {}

    UserResult register(RegisterCommand command);
}
