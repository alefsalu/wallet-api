package com.portfolio.wallet.application.port.input;

import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import java.util.UUID;

public interface DepositUseCase {

    Transaction deposit(UserId userId, Money amount, UUID correlationId);
}
