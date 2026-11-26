package com.portfolio.wallet.application.port.output;

import com.portfolio.wallet.domain.model.Transaction;
import com.portfolio.wallet.domain.model.WalletId;
import java.util.List;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    record PagedResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    PagedResult<Transaction> findByWalletId(WalletId walletId, int page, int size);
}
