package com.portfolio.wallet.application.port.output;

import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;
import com.portfolio.wallet.domain.model.WalletId;
import java.util.Optional;

public interface WalletRepositoryPort {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(WalletId id);

    Optional<Wallet> findByOwnerId(UserId ownerId);
}
