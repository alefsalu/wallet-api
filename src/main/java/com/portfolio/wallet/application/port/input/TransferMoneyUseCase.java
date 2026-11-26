package com.portfolio.wallet.application.port.input;

import com.portfolio.wallet.domain.model.Money;
import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.WalletId;
import java.util.UUID;

public interface TransferMoneyUseCase {

    record TransferCommand(
            UserId sourceUserId,
            WalletId destinationWalletId,
            Money amount,
            String description,
            UUID correlationId) {}

    Transaction transfer(TransferCommand command);
}
