package com.portfolio.wallet.application.port.input;

import com.portfolio.wallet.domain.model.UserId;
import com.portfolio.wallet.domain.model.Wallet;

public interface GetMyWalletQuery {

    Wallet getWallet(UserId ownerId);
}
